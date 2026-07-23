package ai.mayra.app.platform.device

import ai.mayra.app.core.actions.AndroidDeviceActionSpecFactory
import ai.mayra.app.core.actions.DevicePermission
import ai.mayra.app.core.actions.PermissionSnapshot
import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.os.Build
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import java.util.Locale

/** Reads the current state of one Mayra device permission. */
fun interface DevicePermissionStateReader {
    fun isGranted(permission: DevicePermission): Boolean
}

/** Builds immutable permission snapshots for the device action safety gate. */
class DevicePermissionSnapshotProvider(
    private val stateReader: DevicePermissionStateReader,
    private val permanentlyDeniedReader: DevicePermissionStateReader = DevicePermissionStateReader { false }
) {
    fun snapshot(): PermissionSnapshot {
        val granted = DevicePermission.entries.filterTo(linkedSetOf(), stateReader::isGranted)
        val permanentlyDenied = DevicePermission.entries
            .filterTo(linkedSetOf(), permanentlyDeniedReader::isGranted)
            .minus(granted)
        return PermissionSnapshot(granted = granted, permanentlyDenied = permanentlyDenied)
    }
}

/** Android implementation used by production permission checks. */
class AndroidDevicePermissionStateReader(
    context: Context,
    private val alarmManager: AlarmManager? =
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
) : DevicePermissionStateReader {
    private val appContext = context.applicationContext

    override fun isGranted(permission: DevicePermission): Boolean = when (permission) {
        DevicePermission.QUERY_APPS -> true
        DevicePermission.POST_NOTIFICATIONS ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                hasPermission(Manifest.permission.POST_NOTIFICATIONS)
        DevicePermission.SCHEDULE_EXACT_ALARM ->
            Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
                alarmManager?.canScheduleExactAlarms() == true
        else -> AndroidDeviceActionSpecFactory.androidPermissionName(permission)
            ?.let(::hasPermission)
            ?: true
    }

    private fun hasPermission(permissionName: String): Boolean =
        ContextCompat.checkSelfPermission(appContext, permissionName) == PackageManager.PERMISSION_GRANTED
}

data class ContactPhone(
    val contactId: Long,
    val displayName: String,
    val phoneNumber: String,
    val normalizedPhoneNumber: String = PhoneNumberNormalizer.normalize(phoneNumber)
) {
    init {
        require(displayName.isNotBlank()) { "Contact name cannot be blank." }
        require(phoneNumber.isNotBlank()) { "Contact phone number cannot be blank." }
    }
}

fun interface ContactPhoneDataSource {
    fun search(query: String): List<ContactPhone>
}

sealed interface ContactResolution {
    data class Resolved(val contact: ContactPhone) : ContactResolution
    data class Ambiguous(val candidates: List<ContactPhone>) : ContactResolution
    data object NotFound : ContactResolution
}

class ContactResolver(
    private val dataSource: ContactPhoneDataSource
) {
    fun resolve(query: String): ContactResolution {
        val cleanQuery = normalizeLabel(query)
        if (cleanQuery.isBlank()) return ContactResolution.NotFound

        val unique = dataSource.search(query.trim())
            .filter { it.normalizedPhoneNumber.isNotBlank() }
            .distinctBy { it.normalizedPhoneNumber }

        if (unique.isEmpty()) return ContactResolution.NotFound

        val exact = unique.filter { normalizeLabel(it.displayName) == cleanQuery }
        val ranked = when {
            exact.isNotEmpty() -> exact
            else -> unique.filter { normalizeLabel(it.displayName).startsWith(cleanQuery) }
                .ifEmpty { unique.filter { cleanQuery in normalizeLabel(it.displayName) } }
        }

        return when (ranked.size) {
            0 -> ContactResolution.NotFound
            1 -> ContactResolution.Resolved(ranked.single())
            else -> ContactResolution.Ambiguous(ranked.sortedBy { normalizeLabel(it.displayName) })
        }
    }
}

class AndroidContactPhoneDataSource(
    context: Context,
    private val maxResults: Int = 25
) : ContactPhoneDataSource {
    private val resolver = context.applicationContext.contentResolver

    override fun search(query: String): List<ContactPhone> {
        if (query.isBlank()) return emptyList()

        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY,
            ContactsContract.CommonDataKinds.Phone.NUMBER
        )
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} LIKE ?"
        val args = arrayOf("%${query.trim()}%")
        val sort = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY} COLLATE NOCASE ASC"

        return resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            selection,
            args,
            sort
        )?.use { cursor -> cursor.readContacts(maxResults) }.orEmpty()
    }

    private fun Cursor.readContacts(limit: Int): List<ContactPhone> {
        val idIndex = getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
        val nameIndex = getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME_PRIMARY)
        val numberIndex = getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val contacts = ArrayList<ContactPhone>(minOf(count, limit))

        while (moveToNext() && contacts.size < limit) {
            val name = getString(nameIndex)?.trim().orEmpty()
            val number = getString(numberIndex)?.trim().orEmpty()
            if (name.isNotBlank() && number.isNotBlank()) {
                contacts += ContactPhone(
                    contactId = getLong(idIndex),
                    displayName = name,
                    phoneNumber = number
                )
            }
        }
        return contacts
    }
}

object PhoneNumberNormalizer {
    fun normalize(raw: String, defaultCountryCode: String = "+91"): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return ""

        val hasInternationalPrefix = trimmed.startsWith("+")
        val digits = trimmed.filter(Char::isDigit)
        if (digits.isBlank()) return ""

        return when {
            hasInternationalPrefix -> "+$digits"
            digits.length == 10 -> "${normalizeCountryCode(defaultCountryCode)}$digits"
            digits.length == 11 && digits.startsWith("0") ->
                "${normalizeCountryCode(defaultCountryCode)}${digits.drop(1)}"
            digits.length == 12 && digits.startsWith("91") -> "+$digits"
            else -> "+$digits"
        }
    }

    private fun normalizeCountryCode(value: String): String {
        val digits = value.filter(Char::isDigit)
        require(digits.isNotBlank()) { "Country code must contain digits." }
        return "+$digits"
    }
}

data class InstalledApp(
    val label: String,
    val packageName: String
) {
    init {
        require(label.isNotBlank()) { "App label cannot be blank." }
        require(packageName.isNotBlank()) { "Package name cannot be blank." }
    }
}

fun interface InstalledAppDataSource {
    fun loadLaunchableApps(): List<InstalledApp>
}

sealed interface AppResolution {
    data class Resolved(val app: InstalledApp) : AppResolution
    data class Ambiguous(val candidates: List<InstalledApp>) : AppResolution
    data object NotFound : AppResolution
}

class InstalledAppResolver(
    private val dataSource: InstalledAppDataSource
) {
    fun resolve(query: String): AppResolution {
        val cleanQuery = normalizeLabel(query)
        if (cleanQuery.isBlank()) return AppResolution.NotFound

        val apps = dataSource.loadLaunchableApps()
            .distinctBy(InstalledApp::packageName)

        apps.singleOrNull { it.packageName.equals(query.trim(), ignoreCase = true) }
            ?.let { return AppResolution.Resolved(it) }

        val exact = apps.filter { normalizeLabel(it.label) == cleanQuery }
        val ranked = when {
            exact.isNotEmpty() -> exact
            else -> apps.filter { normalizeLabel(it.label).startsWith(cleanQuery) }
                .ifEmpty { apps.filter { cleanQuery in normalizeLabel(it.label) } }
        }

        return when (ranked.size) {
            0 -> AppResolution.NotFound
            1 -> AppResolution.Resolved(ranked.single())
            else -> AppResolution.Ambiguous(
                ranked.sortedWith(compareBy({ normalizeLabel(it.label) }, InstalledApp::packageName))
            )
        }
    }
}

class AndroidInstalledAppDataSource(
    context: Context
) : InstalledAppDataSource {
    private val packageManager = context.applicationContext.packageManager

    override fun loadLaunchableApps(): List<InstalledApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(0L)
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        return resolved.mapNotNull { info ->
            val packageName = info.activityInfo?.packageName?.trim().orEmpty()
            val label = info.loadLabel(packageManager)?.toString()?.trim().orEmpty()
            if (packageName.isBlank() || label.isBlank()) null else InstalledApp(label, packageName)
        }.distinctBy(InstalledApp::packageName)
    }
}

private fun normalizeLabel(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace(Regex("\\s+"), " ")
