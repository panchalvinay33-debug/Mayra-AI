package ai.mayra.app.context

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Reads only timing columns from Android's Calendar Provider and immediately reduces them to the
 * privacy-safe J6 calendar aggregate. Titles, attendees, locations, descriptions and account data
 * are never projected from the provider.
 */
fun collectCalendarContext(
    context: Context,
    capturedAt: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault()
): CalendarContextSnapshot {
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        return CalendarContextSnapshot(capturedAt, ContextValue.NotGranted)
    }

    val window = calendarQueryWindow(capturedAt.toLocalDate(), zoneId)
    val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
        .appendPath(window.first.toString())
        .appendPath((window.last + 1).toString())
        .build()

    val metadata: List<CalendarEventMetadata> = try {
        context.contentResolver.query(
            uri,
            arrayOf(CalendarContract.Instances.BEGIN, CalendarContract.Instances.END),
            null,
            null,
            "${CalendarContract.Instances.BEGIN} ASC"
        )?.use { cursor ->
            val beginIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            buildList<CalendarEventMetadata> {
                while (cursor.moveToNext()) {
                    val startsAt = epochMillisToLocalDateTime(cursor.getLong(beginIndex), zoneId)
                    val endsAt = epochMillisToLocalDateTime(cursor.getLong(endIndex), zoneId)
                    if (!endsAt.isBefore(startsAt)) {
                        add(CalendarEventMetadata(startsAt = startsAt, endsAt = endsAt))
                    }
                }
            }
        } ?: emptyList()
    } catch (_: SecurityException) {
        return CalendarContextSnapshot(capturedAt, ContextValue.NotGranted)
    } catch (_: RuntimeException) {
        return CalendarContextSnapshot(capturedAt, ContextValue.Unavailable)
    }

    return aggregateCalendarMetadata(metadata, capturedAt)
}

internal fun epochMillisToLocalDateTime(epochMillis: Long, zoneId: ZoneId): LocalDateTime =
    Instant.ofEpochMilli(epochMillis).atZone(zoneId).toLocalDateTime()

internal fun calendarQueryWindow(date: LocalDate, zoneId: ZoneId): LongRange {
    val start = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    val endExclusive = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    return start until endExclusive
}
