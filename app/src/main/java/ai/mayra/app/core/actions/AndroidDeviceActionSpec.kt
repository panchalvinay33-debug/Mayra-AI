package ai.mayra.app.core.actions

/**
 * Android-facing, framework-neutral description of the intent that should be launched.
 * The runtime adapter converts this value into android.content.Intent.
 */
data class AndroidIntentSpec(
    val action: String,
    val data: String? = null,
    val mimeType: String? = null,
    val packageName: String? = null,
    val categories: Set<String> = emptySet(),
    val extras: Map<String, String> = emptyMap(),
    val flags: Set<Int> = emptySet()
)

/**
 * Personal Alpha uses review-first handoffs. Mayra opens the dialer or message composer but does not
 * directly place a call or send an SMS. This keeps the final irreversible step visible to the owner.
 */
object AndroidDeviceActionSpecFactory {
    const val ACTION_MAIN = "android.intent.action.MAIN"
    const val ACTION_DIAL = "android.intent.action.DIAL"
    const val ACTION_SENDTO = "android.intent.action.SENDTO"
    const val ACTION_INSERT = "android.intent.action.INSERT"
    const val CATEGORY_LAUNCHER = "android.intent.category.LAUNCHER"
    const val EXTRA_TEXT = "android.intent.extra.TEXT"
    const val EXTRA_TITLE = "title"

    fun create(request: DeviceActionRequest): AndroidIntentSpec = when (request.type) {
        DeviceActionType.OPEN_APP -> AndroidIntentSpec(
            action = ACTION_MAIN,
            packageName = sanitizePackageName(request.metadata[PACKAGE_NAME_KEY] ?: request.target),
            categories = setOf(CATEGORY_LAUNCHER)
        )

        DeviceActionType.CALL_CONTACT -> AndroidIntentSpec(
            action = ACTION_DIAL,
            data = "tel:${sanitizeDialTarget(request.target)}"
        )

        DeviceActionType.SEND_MESSAGE -> AndroidIntentSpec(
            action = ACTION_SENDTO,
            data = "smsto:${sanitizeMessageTarget(request.target)}",
            extras = request.payload
                ?.let(::sanitizeMessageBody)
                ?.takeIf(String::isNotBlank)
                ?.let { mapOf(EXTRA_TEXT to it) }
                .orEmpty()
        )

        DeviceActionType.CREATE_REMINDER -> AndroidIntentSpec(
            action = ACTION_INSERT,
            data = CALENDAR_EVENTS_URI,
            extras = buildMap {
                put(EXTRA_TITLE, sanitizeVisibleText(request.target, 160))
                request.payload?.let { sanitizeVisibleText(it, 1_000) }
                    ?.takeIf(String::isNotBlank)
                    ?.let { put(EXTRA_TEXT, it) }
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

    internal fun sanitizeDialTarget(value: String): String {
        val clean = value.trim().take(120)
        require(clean.none { it == '\r' || it == '\n' || it == '\u0000' }) { "Invalid dial target." }
        require(!clean.contains(':') && !clean.contains('/') && !clean.contains('?') && !clean.contains('#')) {
            "Dial target contains unsupported URI characters."
        }
        val normalized = clean.filter { it.isDigit() || it in setOf('+', ' ', '-', '(', ')') }
        require(normalized.any(Char::isDigit)) { "Dial target must contain a phone number." }
        return normalized
    }

    internal fun sanitizeMessageTarget(value: String): String {
        val clean = value.trim().take(160)
        require(clean.none { it == '\r' || it == '\n' || it == '\u0000' }) { "Invalid message target." }
        require(!clean.contains(':') && !clean.contains('/') && !clean.contains('?') && !clean.contains('#')) {
            "Message target contains unsupported URI characters."
        }
        return clean
    }

    internal fun sanitizeMessageBody(value: String): String = sanitizeVisibleText(value, 4_000)

    private fun sanitizePackageName(value: String): String {
        val clean = value.trim().take(255)
        require(clean.matches(Regex("[A-Za-z0-9_]+(?:\\.[A-Za-z0-9_]+)+"))) {
            "App package name is invalid."
        }
        return clean
    }

    private fun sanitizeVisibleText(value: String, maxLength: Int): String = value
        .replace('\u0000', ' ')
        .replace(Regex("[\\r\\n\\t]+"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
        .take(maxLength)

    private const val PACKAGE_NAME_KEY = "packageName"
    private const val CALENDAR_EVENTS_URI = "content://com.android.calendar/events"
}
