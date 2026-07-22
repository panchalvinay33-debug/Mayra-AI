package ai.mayra.app.platform.device

import android.content.Context
import ai.mayra.app.core.device.DeviceActionHandler
import ai.mayra.app.core.device.DeviceActionHandlerFactory

/** Composition root for Android-backed device actions. */
object AndroidDeviceActionModule {
    fun create(context: Context): DeviceActionHandler {
        val appContext = context.applicationContext
        return DeviceActionHandlerFactory.create(
            intents = AndroidIntentGateway(appContext),
            clipboard = AndroidClipboardGateway(appContext),
            notifications = AndroidNotificationGateway(appContext)
        )
    }
}
