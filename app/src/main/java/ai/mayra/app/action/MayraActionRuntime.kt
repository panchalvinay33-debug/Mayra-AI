package ai.mayra.app.action

import ai.mayra.app.core.actions.AndroidDeviceActionRunner
import ai.mayra.app.core.actions.DeviceActionCoordinator
import ai.mayra.app.core.actions.DeviceActionSafetyGate
import ai.mayra.app.safety.MayraGlobalStopStore
import android.content.Context

object MayraActionRuntime {
    @Volatile private var engine: MayraActionEngine? = null
    @Volatile private var stopStore: MayraGlobalStopStore? = null

    val installed: Boolean get() = engine != null

    fun install(context: Context): MayraActionEngine = synchronized(this) {
        val appContext = context.applicationContext
        val store = stopStore ?: MayraGlobalStopStore(appContext).also { stopStore = it }
        engine ?: MayraActionEngine(
            coordinator = DeviceActionCoordinator(
                safetyGate = DeviceActionSafetyGate(),
                runner = AndroidDeviceActionRunner(appContext)
            )
        ).also {
            engine = it
            if (store.isStopped()) it.stopAll() else it.resume()
        }
    }

    fun requireEngine(): MayraActionEngine = engine
        ?: error("Mayra action safety runtime is not installed.")

    fun stopAll(reason: String = "Owner used the emergency kill switch.") {
        stopStore?.stop(reason)
        engine?.stopAll()
    }

    fun resume(reason: String = "Owner resumed Mayra actions.") {
        stopStore?.resume(reason)
        engine?.resume()
    }

    fun isGloballyStopped(): Boolean = stopStore?.isStopped() ?: engine?.isStopped() ?: false
}
