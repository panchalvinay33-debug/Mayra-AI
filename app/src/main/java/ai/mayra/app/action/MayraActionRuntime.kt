package ai.mayra.app.action

import ai.mayra.app.core.actions.AndroidDeviceActionRunner
import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionSafetyGate
import android.content.Context

object MayraActionRuntime {
    @Volatile private var engine: MayraActionEngine? = null

    val installed: Boolean get() = engine != null

    fun install(context: Context): MayraActionEngine = synchronized(this) {
        engine ?: MayraActionEngine(
            coordinator = DeviceActionCoordinator(
                safetyGate = DeviceActionSafetyGate(),
                runner = AndroidDeviceActionRunner(context.applicationContext)
            )
        ).also { engine = it }
    }

    fun requireEngine(): MayraActionEngine = engine
        ?: error("Mayra action safety runtime is not installed.")

    fun stopAll() { engine?.stopAll() }
    fun resume() { engine?.resume() }
}
