package ai.mayra.app.core.execution

import ai.mayra.app.core.planning.PlannedStep
import ai.mayra.app.core.planning.StepAction
import java.util.concurrent.atomic.AtomicBoolean

/** Cooperative cancellation signal shared by goal orchestration and device action adapters. */
class ExecutionCancellationToken {
    private val cancellationRequested = AtomicBoolean(false)

    val isCancellationRequested: Boolean
        get() = cancellationRequested.get()

    fun cancel(): Boolean = cancellationRequested.compareAndSet(false, true)

    fun throwIfCancellationRequested() {
        if (isCancellationRequested) throw StepExecutionCancelledException()
    }
}

class StepExecutionCancelledException(
    message: String = "Step execution was cancelled."
) : RuntimeException(message)

sealed interface StepProgressEvent {
    val step: PlannedStep

    data class Started(
        override val step: PlannedStep,
        val completedSteps: Int,
        val totalSteps: Int
    ) : StepProgressEvent

    data class Completed(
        override val step: PlannedStep,
        val completedSteps: Int,
        val totalSteps: Int,
        val output: String
    ) : StepProgressEvent

    data class Failed(
        override val step: PlannedStep,
        val completedSteps: Int,
        val totalSteps: Int,
        val error: Throwable
    ) : StepProgressEvent

    data class Cancelled(
        override val step: PlannedStep,
        val completedSteps: Int,
        val totalSteps: Int
    ) : StepProgressEvent
}

fun interface StepProgressListener {
    fun onProgress(event: StepProgressEvent)
}

/**
 * Decorates a [StepAction] with cooperative cancellation and deterministic progress events.
 *
 * Device actions should check the same [cancellationToken] during long-running work when possible.
 * The decorator also checks immediately before and after delegation so cancellation always prevents
 * subsequent plan steps from starting.
 */
class ControllableStepAction(
    private val delegate: StepAction,
    private val cancellationToken: ExecutionCancellationToken,
    private val totalSteps: Int,
    private val listener: StepProgressListener = StepProgressListener { }
) : StepAction {
    init {
        require(totalSteps > 0) { "totalSteps must be greater than zero." }
    }

    private var completedSteps = 0

    @Synchronized
    fun completedStepCount(): Int = completedSteps

    override suspend fun execute(step: PlannedStep): String {
        if (cancellationToken.isCancellationRequested) {
            listener.onProgress(
                StepProgressEvent.Cancelled(step, completedStepCount(), totalSteps)
            )
            throw StepExecutionCancelledException()
        }

        listener.onProgress(
            StepProgressEvent.Started(step, completedStepCount(), totalSteps)
        )

        return try {
            val output = delegate.execute(step)
            if (cancellationToken.isCancellationRequested) {
                listener.onProgress(
                    StepProgressEvent.Cancelled(step, completedStepCount(), totalSteps)
                )
                throw StepExecutionCancelledException()
            }

            val completed = incrementCompletedSteps()
            listener.onProgress(
                StepProgressEvent.Completed(step, completed, totalSteps, output)
            )
            output
        } catch (error: StepExecutionCancelledException) {
            throw error
        } catch (error: Throwable) {
            listener.onProgress(
                StepProgressEvent.Failed(step, completedStepCount(), totalSteps, error)
            )
            throw error
        }
    }

    @Synchronized
    private fun incrementCompletedSteps(): Int {
        completedSteps += 1
        return completedSteps
    }
}
