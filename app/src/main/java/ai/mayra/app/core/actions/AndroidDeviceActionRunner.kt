package ai.mayra.app.core.actions

import android.content.Context
import android.content.Intent
import android.net.Uri

fun interface AndroidIntentStarter {
    fun start(intent: Intent)
}

/**
 * Android runtime implementation of [DeviceActionRunner].
 *
 * Intent construction is deterministic and the actual launch is hidden behind [AndroidIntentStarter]
 * so unit tests never need to perform a real device action.
 */
class AndroidDeviceActionRunner(
    private val starter: AndroidIntentStarter,
    private val specFactory: (DeviceActionRequest) -> AndroidIntentSpec =
        AndroidDeviceActionSpecFactory::create
) : DeviceActionRunner {

    constructor(context: Context) : this(
        starter = AndroidIntentStarter { intent -> context.startActivity(intent) }
    )

    override suspend fun run(request: DeviceActionRequest): String {
        val intent = buildIntent(specFactory(request))
        starter.start(intent)
        return successMessage(request)
    }

    fun buildIntent(spec: AndroidIntentSpec): Intent = Intent(spec.action).apply {
        spec.data?.let { data = Uri.parse(it) }
        spec.mimeType?.let { type = it }
        spec.packageName?.let(::setPackage)
        spec.categories.forEach(::addCategory)
        spec.extras.forEach { (key, value) -> putExtra(key, value) }
        spec.flags.forEach(::addFlags)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    private fun successMessage(request: DeviceActionRequest): String = when (request.type) {
        DeviceActionType.OPEN_APP -> "Opened ${request.target}."
        DeviceActionType.CALL_CONTACT -> "Started call to ${request.target}."
        DeviceActionType.SEND_MESSAGE -> "Opened message for ${request.target}."
        DeviceActionType.CREATE_REMINDER -> "Opened reminder for ${request.target}."
    }
}
