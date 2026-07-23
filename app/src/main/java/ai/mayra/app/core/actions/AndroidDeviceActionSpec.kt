package ai.mayra.app.core.actions

/**
 * Android-facing, framework-neutral description of the intent that should be launched.
 * The actual Android adapter converts this value into android.content.Intent.
 */
data class AndroidIntentSpec(
    val action: String,
    val data: String? = null,
    val mimeType: String? = null,
    val packageName: String? = null,
    val extras: Map<String, String> = emptyMap(),
    val flags: Set<Int> = emptySet()
)

object AndroidDeviceActionSpecFactory {
    const val ACTION_MAIN = "android.intent.action.MAIN"
    const val ACTION_CALL = "android.intent.action.CALL"
    const val ACTION_SENDTO = "android.intent.action.SENDTO"
    const val ACTION_INSERT = "android.intent.action.INSERT"
    const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
    const val EXTRA_TEXT = "android.intent.extra.TEXT"
    const val EXTRA_TITLE = "title"

    fun create(request: DeviceActionRequest): AndroidIntentSpec = when (request.type) {
        DeviceActionType.OPEN_APP -> AndroidIntentSpec(
            action = ACTION_MAIN,
            packageName = request.metadata[PACKAGE_NAME_KEY] ?: request.target,
            extras = mapOf(CATEGORY_KEY to CATEGORY_LAUNCHER)
        )

        DeviceActionType.CALL_CONTACT -> AndroidIntentSpec(
            action = ACTION_CALL,
            data = "tel:${request.target.trim()}"
        )

        DeviceActionType.SEND_MESSAGE -> AndroidIntentSpec(
            action = ACTION_SENDTO,
            data = "smsto:${request.target.trim()}",
            extras = request.payload
                ?.trim()
                ?.takeIf(String::isNotBlank)
                ?.let { mapOf(EXTRA_TEXT to it) }
                .orEmpty()
        )

        DeviceActionType.CREATE_REMINDER -> AndroidIntentSpec(
            action = ACTION_INSERT,
            data = CALENDAR_EVENTS_URI,
            extras = buildMap {
                put(EXTRA_TITLE, request.target.trim())
                request.payload?.trim()?.takeIf(String::isNotBlank)?.let { put(EXTRA_TEXT, it) }
            }
        )
    }

    fun androidPermissionName(permission: DevicePermission): String? = when (permission) {
        DevicePermission.QUERY_APPS -> null
        DevicePermission.CALL_PHONE -> "android.permission.CALL_PHONE"
        DevicePermission.READ_CONTACTS -> "android.permission.READ_CONTACTS"
        DevicePermission.SEND_MESSAGES -> "android.permission.SEND_SMS"
        DevicePermission.POST_NOTIFICATIONS -> "android.permission.POST_NOTIFICATIONS"
        DevicePermission.SCHEDULE_EXACT_ALARM -> "android.permission.SCHEDULE_EXACT_ALARM"
    }

    private const val PACKAGE_NAME_KEY = "packageName"
    private const val CATEGORY_KEY = "category"
    private const val CALENDAR_EVENTS_URI = "content://com.android.calendar/events"
}
