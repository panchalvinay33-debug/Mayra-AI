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
 * so unit tests never need to perform a real device action. Calls and messages use review-first
 * Android surfaces; Mayra never claims the final call connection or message delivery.
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
        return handoffMessage(request)
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

    private fun handoffMessage(request: DeviceActionRequest): String = when (request.type) {
        DeviceActionType.OPEN_APP -> "Android accepted the request to open ${safeLabel(request.target)}."
        DeviceActionType.CALL_CONTACT ->
            "Dialer opened for ${safeLabel(request.target)}. The owner must start the call."
        DeviceActionType.SEND_MESSAGE ->
            "Message composer opened for ${safeLabel(request.target)}. The message has not been sent."
        DeviceActionType.CREATE_REMINDER ->
            "Reminder editor opened for ${safeLabel(request.target)}. Saving is still visible to the owner."
    }

    private fun safeLabel(value: String): String = value
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(120)
}
