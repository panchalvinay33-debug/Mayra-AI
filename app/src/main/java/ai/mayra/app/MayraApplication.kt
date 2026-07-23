package ai.mayra.app

import ai.mayra.app.core.ActionDispatcher
import ai.mayra.app.core.LocalCommandEngine
import ai.mayra.app.core.LocalMayraAssistant
import ai.mayra.app.core.MayraAssistant
import ai.mayra.app.platform.device.AndroidActionExecutor
import android.app.Application

class MayraApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        MayraRuntime.assistant = LocalMayraAssistant(
            LocalCommandEngine(
                actionDispatcher = ActionDispatcher(
                    AndroidActionExecutor(applicationContext)
                )
            )
        )
    }
}

/** Small application-level service container; avoids recreating pending action state on recomposition. */
object MayraRuntime {
    @Volatile
    var assistant: MayraAssistant = LocalMayraAssistant()
}
