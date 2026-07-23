package ai.mayra.app.automation

import android.app.SearchManager
import android.bluetooth.BluetoothAdapter
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.Settings
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Calendar
import java.util.UUID

/** Android-safe automation surface. It favors public intents and settings panels over restricted APIs. */
enum class AutomationType {
    OPEN_APP, OPEN_URL, WEB_SEARCH, SHARE_TEXT,
    COMPOSE_SMS, COMPOSE_WHATSAPP, COMPOSE_EMAIL, DIAL_NUMBER,
    CREATE_ALARM, CREATE_TIMER, INSERT_CALENDAR_EVENT,
    OPEN_WIFI_SETTINGS, OPEN_BLUETOOTH_SETTINGS, OPEN_DND_SETTINGS,
    OPEN_APP_SETTINGS, OPEN_NOTIFICATION_SETTINGS,
    SET_FLASHLIGHT, CHANGE_MEDIA_VOLUME, SET_BRIGHTNESS,
    COPY_CLIPBOARD, READ_CLIPBOARD,
    OPEN_DOWNLOADS, OPEN_FILES, STORAGE_SUMMARY
}

enum class AutomationRisk { LOW, MEDIUM, HIGH }
enum class AutomationStatus { COMPLETED, USER_ACTION_REQUIRED, PERMISSION_REQUIRED, NOT_SUPPORTED, FAILED, DUPLICATE_SUPPRESSED }

data class AutomationRequest(
    val id: String = UUID.randomUUID().toString(),
    val type: AutomationType,
    val parameters: Map<String, String> = emptyMap(),
    val confirmed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

data class AutomationResult(
    val status: AutomationStatus,
    val message: String,
    val data: Map<String, String> = emptyMap(),
    val retryable: Boolean = false
)

data class AutomationAudit(
    val requestId: String,
    val type: AutomationType,
    val status: AutomationStatus,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class AutomationCapabilities(
    val cameraFlash: Boolean,
    val bluetoothAdapter: Boolean,
    val canWriteSystemSettings: Boolean,
    val downloadsDirectoryReadable: Boolean,
    val clipboardAvailable: Boolean,
    val supportedTypes: Set<AutomationType>
)

class AutomationSafetyPolicy {
    fun risk(type: AutomationType): AutomationRisk = when (type) {
        AutomationType.SET_FLASHLIGHT,
        AutomationType.CHANGE_MEDIA_VOLUME,
        AutomationType.SET_BRIGHTNESS,
        AutomationType.COPY_CLIPBOARD,
        AutomationType.READ_CLIPBOARD -> AutomationRisk.MEDIUM
        AutomationType.COMPOSE_SMS,
        AutomationType.COMPOSE_WHATSAPP,
        AutomationType.COMPOSE_EMAIL,
        AutomationType.DIAL_NUMBER -> AutomationRisk.HIGH
        else -> AutomationRisk.LOW
    }

    fun requiresConfirmation(type: AutomationType): Boolean = risk(type) == AutomationRisk.HIGH

    fun validate(request: AutomationRequest): AutomationResult? {
        if (System.currentTimeMillis() - request.createdAt > REQUEST_TTL_MS) {
            return AutomationResult(AutomationStatus.FAILED, "Automation request expired.")
        }
        if (requiresConfirmation(request.type) && !request.confirmed) {
            return AutomationResult(AutomationStatus.USER_ACTION_REQUIRED, "Please confirm this communication action first.")
        }
        if (request.parameters.size > MAX_PARAMETERS || request.parameters.any { it.key.length > 80 || it.value.length > MAX_VALUE_LENGTH }) {
            return AutomationResult(AutomationStatus.FAILED, "Automation parameters exceed safe limits.")
        }
        return null
    }

    companion object {
        const val REQUEST_TTL_MS = 2 * 60 * 1000L
        const val MAX_PARAMETERS = 20
        const val MAX_VALUE_LENGTH = 4_000
    }
}

class MayraAndroidAutomation(
    context: Context,
    private val safety: AutomationSafetyPolicy = AutomationSafetyPolicy(),
    private val now: () -> Long = System::currentTimeMillis
) {
    private val appContext = context.applicationContext
    private val audit = ArrayDeque<AutomationAudit>()
    private val fingerprints = ArrayDeque<Pair<String, Long>>()

    @Synchronized
    fun execute(request: AutomationRequest): AutomationResult {
        safety.validate(request)?.let { return record(request, it) }
        val fingerprint = fingerprint(request)
        pruneFingerprints()
        if (fingerprints.any { it.first == fingerprint }) {
            return record(request, AutomationResult(AutomationStatus.DUPLICATE_SUPPRESSED, "Duplicate automation was suppressed."))
        }
        rememberFingerprint(fingerprint)
        val result = runCatching { executeInternal(request) }
            .getOrElse { AutomationResult(AutomationStatus.FAILED, it.message ?: "Automation failed.", retryable = true) }
        return record(request, result)
    }

    fun capabilities(): AutomationCapabilities {
        val cameraManager = appContext.getSystemService(CameraManager::class.java)
        val flash = runCatching {
            cameraManager.cameraIdList.any { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
        }.getOrDefault(false)
        return AutomationCapabilities(
            cameraFlash = flash,
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter() != null,
            canWriteSystemSettings = Settings.System.canWrite(appContext),
            downloadsDirectoryReadable = downloadsDirectory()?.canRead() == true,
            clipboardAvailable = appContext.getSystemService(ClipboardManager::class.java) != null,
            supportedTypes = AutomationType.entries.toSet()
        )
    }

    @Synchronized
    fun recentAudit(limit: Int = 50): List<AutomationAudit> = audit.toList().takeLast(limit.coerceIn(1, MAX_AUDIT)).reversed()

    private fun executeInternal(request: AutomationRequest): AutomationResult = when (request.type) {
        AutomationType.OPEN_APP -> openApp(required(request, "package"))
        AutomationType.OPEN_URL -> launch(Intent(Intent.ACTION_VIEW, safeUri(required(request, "url"))), "Opening link.")
        AutomationType.WEB_SEARCH -> launch(Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, required(request, "query")), "Opening web search.")
        AutomationType.SHARE_TEXT -> shareText(required(request, "text"), request.parameters["subject"])
        AutomationType.COMPOSE_SMS -> composeSms(required(request, "number"), request.parameters["message"])
        AutomationType.COMPOSE_WHATSAPP -> composeWhatsApp(required(request, "number"), request.parameters["message"].orEmpty())
        AutomationType.COMPOSE_EMAIL -> composeEmail(required(request, "to"), request.parameters["subject"], request.parameters["body"])
        AutomationType.DIAL_NUMBER -> launch(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(required(request, "number"))}")), "Opening dialer.")
        AutomationType.CREATE_ALARM -> createAlarm(request.parameters)
        AutomationType.CREATE_TIMER -> createTimer(request.parameters)
        AutomationType.INSERT_CALENDAR_EVENT -> insertCalendar(request.parameters)
        AutomationType.OPEN_WIFI_SETTINGS -> launch(Intent(Settings.ACTION_WIFI_SETTINGS), "Opening Wi-Fi settings.")
        AutomationType.OPEN_BLUETOOTH_SETTINGS -> launch(Intent(Settings.ACTION_BLUETOOTH_SETTINGS), "Opening Bluetooth settings.")
        AutomationType.OPEN_DND_SETTINGS -> launch(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS), "Opening Do Not Disturb settings.")
        AutomationType.OPEN_APP_SETTINGS -> launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appContext.packageName}")), "Opening Mayra settings.")
        AutomationType.OPEN_NOTIFICATION_SETTINGS -> openNotificationSettings()
        AutomationType.SET_FLASHLIGHT -> setFlashlight(parseBoolean(required(request, "enabled")))
        AutomationType.CHANGE_MEDIA_VOLUME -> changeVolume(required(request, "operation"), request.parameters["steps"]?.toIntOrNull() ?: 1)
        AutomationType.SET_BRIGHTNESS -> setBrightness(required(request, "value").toIntOrNull() ?: -1)
        AutomationType.COPY_CLIPBOARD -> copyClipboard(required(request, "text"))
        AutomationType.READ_CLIPBOARD -> readClipboard()
        AutomationType.OPEN_DOWNLOADS -> launch(Intent(Intent.ACTION_VIEW).setType("resource/folder").addCategory(Intent.CATEGORY_DEFAULT), "Opening downloads.", fallback = Intent(Settings.ACTION_INTERNAL_STORAGE_SETTINGS))
        AutomationType.OPEN_FILES -> launch(Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE), "Opening file picker.")
        AutomationType.STORAGE_SUMMARY -> storageSummary()
    }

    private fun openApp(packageName: String): AutomationResult {
        val intent = appContext.packageManager.getLaunchIntentForPackage(packageName)
            ?: return AutomationResult(AutomationStatus.NOT_SUPPORTED, "Installed app could not be opened.")
        return launch(intent, "Opening app.")
    }

    private fun shareText(text: String, subject: String?): AutomationResult {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        subject?.takeIf(String::isNotBlank)?.let { send.putExtra(Intent.EXTRA_SUBJECT, it) }
        return launch(Intent.createChooser(send, "Share with"), "Opening share sheet.")
    }

    private fun composeSms(number: String, message: String?): AutomationResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(number)}"))
        message?.let { intent.putExtra("sms_body", it) }
        return launch(intent, "Opening SMS composer.")
    }

    private fun composeWhatsApp(number: String, message: String): AutomationResult {
        val digits = number.filter(Char::isDigit)
        val encoded = URLEncoder.encode(message, StandardCharsets.UTF_8.name())
        val uri = Uri.parse("https://wa.me/$digits?text=$encoded")
        return launch(Intent(Intent.ACTION_VIEW, uri).setPackage("com.whatsapp"), "Opening WhatsApp message.", fallback = Intent(Intent.ACTION_VIEW, uri))
    }

    private fun composeEmail(to: String, subject: String?, body: String?): AutomationResult {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${Uri.encode(to)}"))
        subject?.let { intent.putExtra(Intent.EXTRA_SUBJECT, it) }
        body?.let { intent.putExtra(Intent.EXTRA_TEXT, it) }
        return launch(intent, "Opening email composer.")
    }

    private fun createAlarm(p: Map<String, String>): AutomationResult {
        val hour = p["hour"]?.toIntOrNull() ?: return AutomationResult(AutomationStatus.FAILED, "Alarm hour is required.")
        val minute = p["minute"]?.toIntOrNull() ?: 0
        if (hour !in 0..23 || minute !in 0..59) return AutomationResult(AutomationStatus.FAILED, "Alarm time is invalid.")
        val intent = Intent(AlarmClock.ACTION_SET_ALARM)
            .putExtra(AlarmClock.EXTRA_HOUR, hour)
            .putExtra(AlarmClock.EXTRA_MINUTES, minute)
            .putExtra(AlarmClock.EXTRA_MESSAGE, p["label"].orEmpty())
            .putExtra(AlarmClock.EXTRA_SKIP_UI, false)
        return launch(intent, "Opening alarm setup.")
    }

    private fun createTimer(p: Map<String, String>): AutomationResult {
        val seconds = p["seconds"]?.toIntOrNull() ?: return AutomationResult(AutomationStatus.FAILED, "Timer duration is required.")
        if (seconds !in 1..86_400) return AutomationResult(AutomationStatus.FAILED, "Timer must be between 1 second and 24 hours.")
        return launch(Intent(AlarmClock.ACTION_SET_TIMER).putExtra(AlarmClock.EXTRA_LENGTH, seconds).putExtra(AlarmClock.EXTRA_MESSAGE, p["label"].orEmpty()).putExtra(AlarmClock.EXTRA_SKIP_UI, false), "Opening timer setup.")
    }

    private fun insertCalendar(p: Map<String, String>): AutomationResult {
        val begin = p["beginMillis"]?.toLongOrNull() ?: now()
        val end = p["endMillis"]?.toLongOrNull()?.coerceAtLeast(begin) ?: (begin + 60 * 60 * 1000L)
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, begin)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, end)
            .putExtra(CalendarContract.Events.TITLE, p["title"].orEmpty())
            .putExtra(CalendarContract.Events.DESCRIPTION, p["description"].orEmpty())
            .putExtra(CalendarContract.Events.EVENT_LOCATION, p["location"].orEmpty())
        return launch(intent, "Opening calendar event.")
    }

    private fun openNotificationSettings(): AutomationResult {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)
        } else Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${appContext.packageName}"))
        return launch(intent, "Opening notification settings.")
    }

    private fun setFlashlight(enabled: Boolean): AutomationResult {
        val manager = appContext.getSystemService(CameraManager::class.java)
        val id = manager.cameraIdList.firstOrNull { cameraId ->
            manager.getCameraCharacteristics(cameraId).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        } ?: return AutomationResult(AutomationStatus.NOT_SUPPORTED, "No flashlight is available.")
        manager.setTorchMode(id, enabled)
        return AutomationResult(AutomationStatus.COMPLETED, if (enabled) "Flashlight turned on." else "Flashlight turned off.")
    }

    private fun changeVolume(operation: String, steps: Int): AutomationResult {
        val audio = appContext.getSystemService(AudioManager::class.java)
        val direction = when (operation.lowercase()) {
            "increase", "up" -> AudioManager.ADJUST_RAISE
            "decrease", "down" -> AudioManager.ADJUST_LOWER
            "mute" -> AudioManager.ADJUST_MUTE
            "unmute" -> AudioManager.ADJUST_UNMUTE
            else -> return AutomationResult(AutomationStatus.FAILED, "Unknown volume operation.")
        }
        repeat(steps.coerceIn(1, 10)) { audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, direction, AudioManager.FLAG_SHOW_UI) }
        return AutomationResult(AutomationStatus.COMPLETED, "Media volume updated.")
    }

    private fun setBrightness(value: Int): AutomationResult {
        if (value !in 0..255) return AutomationResult(AutomationStatus.FAILED, "Brightness must be between 0 and 255.")
        if (!Settings.System.canWrite(appContext)) {
            launch(Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${appContext.packageName}")), "Opening system-setting permission.")
            return AutomationResult(AutomationStatus.PERMISSION_REQUIRED, "Allow Mayra to modify system settings, then try again.")
        }
        Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL)
        Settings.System.putInt(appContext.contentResolver, Settings.System.SCREEN_BRIGHTNESS, value)
        return AutomationResult(AutomationStatus.COMPLETED, "Brightness updated.")
    }

    private fun copyClipboard(text: String): AutomationResult {
        appContext.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Mayra", text))
        return AutomationResult(AutomationStatus.COMPLETED, "Copied to clipboard.")
    }

    private fun readClipboard(): AutomationResult {
        val manager = appContext.getSystemService(ClipboardManager::class.java)
        val text = manager.primaryClip?.getItemAt(0)?.coerceToText(appContext)?.toString().orEmpty()
        return if (text.isBlank()) AutomationResult(AutomationStatus.NOT_SUPPORTED, "Clipboard is empty or unavailable.")
        else AutomationResult(AutomationStatus.COMPLETED, "Clipboard text read.", mapOf("text" to text.take(4_000)))
    }

    private fun storageSummary(): AutomationResult {
        val root = appContext.filesDir
        val downloads = downloadsDirectory()
        val data = mapOf(
            "appFreeBytes" to root.freeSpace.toString(),
            "appTotalBytes" to root.totalSpace.toString(),
            "downloadsReadable" to (downloads?.canRead() == true).toString(),
            "downloadsItems" to (downloads?.listFiles()?.size ?: 0).toString()
        )
        return AutomationResult(AutomationStatus.COMPLETED, "Storage summary ready.", data)
    }

    private fun launch(intent: Intent, success: String, fallback: Intent? = null): AutomationResult {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val chosen = if (intent.resolveActivity(appContext.packageManager) != null) intent else fallback
        if (chosen == null || chosen.resolveActivity(appContext.packageManager) == null) {
            return AutomationResult(AutomationStatus.NOT_SUPPORTED, "No compatible Android app is available.")
        }
        chosen.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        appContext.startActivity(chosen)
        return AutomationResult(AutomationStatus.USER_ACTION_REQUIRED, success)
    }

    private fun safeUri(value: String): Uri {
        val clean = value.trim()
        val withScheme = if (clean.startsWith("http://") || clean.startsWith("https://")) clean else "https://$clean"
        return Uri.parse(withScheme)
    }

    private fun required(request: AutomationRequest, key: String): String = request.parameters[key]?.trim()?.takeIf(String::isNotBlank)
        ?: throw IllegalArgumentException("Missing parameter: $key")

    private fun parseBoolean(value: String): Boolean = when (value.lowercase()) {
        "true", "on", "1", "yes" -> true
        "false", "off", "0", "no" -> false
        else -> throw IllegalArgumentException("Invalid boolean value")
    }

    private fun downloadsDirectory(): File? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) appContext.getExternalFilesDir("downloads") else android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)

    private fun fingerprint(request: AutomationRequest): String = request.type.name + "|" + request.parameters.toSortedMap().entries.joinToString("|") { "${it.key}=${it.value.trim().lowercase()}" }

    private fun rememberFingerprint(value: String) {
        if (fingerprints.size >= MAX_FINGERPRINTS) fingerprints.removeFirst()
        fingerprints.addLast(value to now())
    }

    private fun pruneFingerprints() {
        val cutoff = now() - DEDUP_WINDOW_MS
        while (fingerprints.isNotEmpty() && fingerprints.first().second < cutoff) fingerprints.removeFirst()
    }

    private fun record(request: AutomationRequest, result: AutomationResult): AutomationResult {
        if (audit.size >= MAX_AUDIT) audit.removeFirst()
        audit.addLast(AutomationAudit(request.id, request.type, result.status, result.message, now()))
        return result
    }

    companion object {
        const val DEDUP_WINDOW_MS = 10_000L
        const val MAX_FINGERPRINTS = 60
        const val MAX_AUDIT = 300
    }
}
