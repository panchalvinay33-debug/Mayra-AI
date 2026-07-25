package ai.mayra.app.action

import ai.mayra.app.core.actions.AndroidDeviceActionRunner
import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionSafetyGate
import ai.mayra.app.file.MayraFileGrantRegistry
import ai.mayra.app.file.MayraFileInventoryWorker
import ai.mayra.app.safety.MayraGlobalStopStore
import android.content.Context

object MayraActionRuntime {
    @Volatile private var engine: MayraActionEngine? = null
    @Volatile private var stopStore: MayraGlobalStopStore? = null
    @Volatile private var appContext: Context? = null

    val installed: Boolean get() = engine != null

    fun install(context: Context): MayraActionEngine = synchronized(this) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        val store = stopStore ?: MayraGlobalStopStore(applicationContext).also { stopStore = it }
        engine ?: MayraActionEngine(
            coordinator = DeviceActionCoordinator(
                safetyGate = DeviceActionSafetyGate(),
                runner = AndroidDeviceActionRunner(applicationContext)
            )
        ).also {
            engine = it
            if (store.isStopped()) {
                it.stopAll()
                MayraFileInventoryWorker.cancel(applicationContext)
            } else {
                it.resume()
                if (MayraFileGrantRegistry(applicationContext).list().isNotEmpty()) {
                    MayraFileInventoryWorker.schedulePeriodic(applicationContext)
                }
            }
        }
    }

    fun requireEngine(): MayraActionEngine = engine
        ?: error("Mayra action safety runtime is not installed.")

    fun stopAll(reason: String = "Owner used the emergency kill switch.") {
        val context = appContext
        stopStore?.stop(reason)
        engine?.stopAll()
        if (context != null) MayraFileInventoryWorker.cancel(context)
    }

    fun resume(reason: String = "Owner resumed Mayra actions.") {
        val context = appContext
        stopStore?.resume(reason)
        engine?.resume()
        if (context != null && MayraFileGrantRegistry(context).list().isNotEmpty()) {
            MayraFileInventoryWorker.schedulePeriodic(context)
        }
    }

    fun isGloballyStopped(): Boolean = stopStore?.isStopped() ?: engine?.isStopped() ?: false
}
