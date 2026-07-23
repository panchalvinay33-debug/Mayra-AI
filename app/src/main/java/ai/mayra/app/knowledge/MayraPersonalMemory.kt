package ai.mayra.app.knowledge

import android.content.Context
import java.util.UUID

enum class PersonalNoteType { NOTE, IDEA, SHOPPING_LIST, CHECKLIST, VOICE_TRANSCRIPT, PROJECT_NOTE, SECURE_REFERENCE }
enum class TimelineEventType { NOTE_CREATED, CALL, MESSAGE, MEETING, REMINDER, GOAL, TRIP, PURCHASE, SYSTEM, CUSTOM }
enum class TimelineImportance { LOW, NORMAL, HIGH, CRITICAL }

data class ChecklistItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val completed: Boolean = false,
    val completedAt: Long? = null
)

data class PersonalNote(
    val id: String = UUID.randomUUID().toString(),
    val type: PersonalNoteType = PersonalNoteType.NOTE,
    val title: String,
    val body: String = "",
    val tags: Set<String> = emptySet(),
    val linkedEntityIds: Set<String> = emptySet(),
    val checklist: List<ChecklistItem> = emptyList(),
    val priority: Int = 1,
    val pinned: Boolean = false,
    val sensitive: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val archivedAt: Long? = null
) {
    init {
        require(title.isNotBlank())
        require(priority in 1..5)
        if (type == PersonalNoteType.SECURE_REFERENCE) require(sensitive) { "Secure references must be sensitive" }
    }
}

data class TimelineEvent(
    val id: String = UUID.randomUUID().toString(),
    val type: TimelineEventType,
    val title: String,
    val description: String = "",
    val occurredAt: Long = System.currentTimeMillis(),
    val linkedEntityIds: Set<String> = emptySet(),
    val linkedNoteId: String? = null,
    val importance: TimelineImportance = TimelineImportance.NORMAL,
    val source: String = "local",
    val metadata: Map<String, String> = emptyMap(),
    val sensitive: Boolean = false
)

data class PersonalMemoryHit(
    val id: String,
    val kind: String,
    val title: String,
    val preview: String,
    val score: Double,
    val timestamp: Long,
    val sensitive: Boolean
)

data class PersonalMemoryDiagnostics(
    val notes: Int,
    val activeNotes: Int,
    val checklistItems: Int,
    val completedChecklistItems: Int,
    val timelineEvents: Int,
    val sensitiveItems: Int,
    val pinnedNotes: Int
)

class MayraPersonalMemory(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun saveNote(note: PersonalNote, addTimeline: Boolean = true): PersonalNote {
        val normalized = note.copy(
            title = sanitize(note.title), body = sanitize(note.body),
            tags = note.tags.map(::sanitize).filter(String::isNotBlank).take(MAX_TAGS).toSet(),
            linkedEntityIds = note.linkedEntityIds.take(MAX_LINKS).toSet(),
            checklist = note.checklist.take(MAX_CHECKLIST).map { it.copy(text = sanitize(it.text)) },
            updatedAt = System.currentTimeMillis()
        )
        writeNotes(notes(includeArchived = true).filterNot { it.id == normalized.id } + normalized)
        if (addTimeline && notes(includeArchived = true).none { it.id == normalized.id }) {
            appendEvent(TimelineEvent(type = TimelineEventType.NOTE_CREATED, title = normalized.title, linkedEntityIds = normalized.linkedEntityIds, linkedNoteId = normalized.id, sensitive = normalized.sensitive))
        }
        return normalized
    }

    fun note(id: String): PersonalNote? = notes(includeArchived = true).firstOrNull { it.id == id }

    fun notes(includeArchived: Boolean = false): List<PersonalNote> = preferences.getStringSet(KEY_NOTES, emptySet()).orEmpty()
        .mapNotNull(::decodeNote)
        .filter { includeArchived || it.archivedAt == null }
        .sortedWith(compareByDescending<PersonalNote> { it.pinned }.thenByDescending { it.priority }.thenByDescending { it.updatedAt })

    @Synchronized
    fun toggleChecklist(noteId: String, itemId: String, completed: Boolean): PersonalNote? {
        val current = note(noteId) ?: return null
        if (current.checklist.none { it.id == itemId }) return null
        return saveNote(current.copy(checklist = current.checklist.map {
            if (it.id == itemId) it.copy(completed = completed, completedAt = if (completed) System.currentTimeMillis() else null) else it
        }), addTimeline = false)
    }

    @Synchronized
    fun archiveNote(noteId: String): PersonalNote? {
        val current = note(noteId) ?: return null
        return saveNote(current.copy(archivedAt = System.currentTimeMillis()), addTimeline = false)
    }

    @Synchronized
    fun deleteNote(noteId: String): Boolean {
        val all = notes(includeArchived = true)
        if (all.none { it.id == noteId }) return false
        writeNotes(all.filterNot { it.id == noteId })
        return true
    }

    @Synchronized
    fun appendEvent(event: TimelineEvent): TimelineEvent {
        require(event.title.isNotBlank())
        val normalized = event.copy(
            title = sanitize(event.title), description = sanitize(event.description),
            linkedEntityIds = event.linkedEntityIds.take(MAX_LINKS).toSet(),
            metadata = event.metadata.entries.take(MAX_METADATA).associate { sanitize(it.key) to sanitize(it.value) },
            source = sanitize(event.source)
        )
        writeEvents((timeline(includeSensitive = true) + normalized).sortedByDescending { it.occurredAt }.take(MAX_EVENTS))
        return normalized
    }

    fun timeline(
        from: Long? = null,
        to: Long? = null,
        entityId: String? = null,
        types: Set<TimelineEventType> = emptySet(),
        includeSensitive: Boolean = false,
        limit: Int = MAX_EVENTS
    ): List<TimelineEvent> {
        require(limit > 0)
        return preferences.getStringSet(KEY_EVENTS, emptySet()).orEmpty().mapNotNull(::decodeEvent)
            .asSequence()
            .filter { includeSensitive || !it.sensitive }
            .filter { from == null || it.occurredAt >= from }
            .filter { to == null || it.occurredAt <= to }
            .filter { entityId == null || entityId in it.linkedEntityIds }
            .filter { types.isEmpty() || it.type in types }
            .sortedByDescending { it.occurredAt }
            .take(limit)
            .toList()
    }

    fun search(query: String, includeSensitive: Boolean = false, limit: Int = 25): List<PersonalMemoryHit> {
        require(limit > 0)
        val terms = tokens(query)
        if (terms.isEmpty()) return emptyList()
        val noteHits = notes().asSequence().filter { includeSensitive || !it.sensitive }.mapNotNull { note ->
            val corpus = tokens(note.title) + tokens(note.body) + note.tags.flatMap(::tokens) + note.checklist.flatMap { tokens(it.text) }
            val matches = terms.count { term -> corpus.any { it.contains(term) } }
            if (matches == 0) null else PersonalMemoryHit(
                note.id, "note", note.title, note.body.take(180),
                score = score(matches, terms.size, note.priority / 5.0, if (note.pinned) 1.0 else 0.0),
                timestamp = note.updatedAt, sensitive = note.sensitive
            )
        }
        val eventHits = timeline(includeSensitive = includeSensitive).asSequence().mapNotNull { event ->
            val corpus = tokens(event.title) + tokens(event.description) + event.metadata.flatMap { tokens(it.key) + tokens(it.value) }
            val matches = terms.count { term -> corpus.any { it.contains(term) } }
            if (matches == 0) null else PersonalMemoryHit(
                event.id, "timeline", event.title, event.description.take(180),
                score = score(matches, terms.size, event.importance.ordinal / 3.0, 0.0),
                timestamp = event.occurredAt, sensitive = event.sensitive
            )
        }
        return (noteHits + eventHits).sortedWith(compareByDescending<PersonalMemoryHit> { it.score }.thenByDescending { it.timestamp }).take(limit).toList()
    }

    @Synchronized
    fun prune(noteLimit: Int = 300, eventLimit: Int = 1200, archiveRetentionMs: Long = DEFAULT_ARCHIVE_RETENTION, now: Long = System.currentTimeMillis()) {
        require(noteLimit > 0 && eventLimit > 0 && archiveRetentionMs >= 0)
        val retainedNotes = notes(includeArchived = true)
            .filter { it.archivedAt == null || now - it.archivedAt <= archiveRetentionMs }
            .sortedWith(compareByDescending<PersonalNote> { it.pinned }.thenByDescending { it.priority }.thenByDescending { it.updatedAt })
            .take(noteLimit)
        writeNotes(retainedNotes)
        writeEvents(timeline(includeSensitive = true, limit = Int.MAX_VALUE).take(eventLimit))
    }

    fun diagnostics(): PersonalMemoryDiagnostics {
        val allNotes = notes(includeArchived = true)
        val events = timeline(includeSensitive = true)
        val checklist = allNotes.flatMap { it.checklist }
        return PersonalMemoryDiagnostics(
            notes = allNotes.size, activeNotes = allNotes.count { it.archivedAt == null },
            checklistItems = checklist.size, completedChecklistItems = checklist.count { it.completed },
            timelineEvents = events.size,
            sensitiveItems = allNotes.count { it.sensitive } + events.count { it.sensitive },
            pinnedNotes = allNotes.count { it.pinned }
        )
    }

    private fun writeNotes(items: List<PersonalNote>) { preferences.edit().putStringSet(KEY_NOTES, items.map(::encodeNote).toSet()).apply() }
    private fun writeEvents(items: List<TimelineEvent>) { preferences.edit().putStringSet(KEY_EVENTS, items.map(::encodeEvent).toSet()).apply() }

    private fun encodeNote(n: PersonalNote): String = listOf(
        n.id, n.type.name, sanitize(n.title), sanitize(n.body), encodeSet(n.tags), encodeSet(n.linkedEntityIds),
        encodeChecklist(n.checklist), n.priority, n.pinned, n.sensitive, n.createdAt, n.updatedAt, n.archivedAt ?: -1L
    ).joinToString(FIELD)

    private fun decodeNote(raw: String): PersonalNote? {
        val p = raw.split(FIELD)
        if (p.size != 13) return null
        return runCatching { PersonalNote(p[0], PersonalNoteType.valueOf(p[1]), p[2], p[3], decodeSet(p[4]), decodeSet(p[5]), decodeChecklist(p[6]), p[7].toInt(), p[8].toBooleanStrict(), p[9].toBooleanStrict(), p[10].toLong(), p[11].toLong(), p[12].toLong().takeIf { it >= 0 }) }.getOrNull()
    }

    private fun encodeEvent(e: TimelineEvent): String = listOf(
        e.id, e.type.name, sanitize(e.title), sanitize(e.description), e.occurredAt, encodeSet(e.linkedEntityIds), e.linkedNoteId.orEmpty(),
        e.importance.name, sanitize(e.source), encodeMap(e.metadata), e.sensitive
    ).joinToString(FIELD)

    private fun decodeEvent(raw: String): TimelineEvent? {
        val p = raw.split(FIELD)
        if (p.size != 11) return null
        return runCatching { TimelineEvent(p[0], TimelineEventType.valueOf(p[1]), p[2], p[3], p[4].toLong(), decodeSet(p[5]), p[6].ifBlank { null }, TimelineImportance.valueOf(p[7]), p[8], decodeMap(p[9]), p[10].toBooleanStrict()) }.getOrNull()
    }

    private fun encodeChecklist(items: List<ChecklistItem>): String = items.joinToString(LIST) { listOf(it.id, sanitize(it.text), it.completed, it.completedAt ?: -1L).joinToString(SUBFIELD) }
    private fun decodeChecklist(raw: String): List<ChecklistItem> = if (raw.isBlank()) emptyList() else raw.split(LIST).mapNotNull {
        val p = it.split(SUBFIELD); if (p.size != 4) null else runCatching { ChecklistItem(p[0], p[1], p[2].toBooleanStrict(), p[3].toLong().takeIf { v -> v >= 0 }) }.getOrNull()
    }
    private fun encodeSet(values: Set<String>): String = values.joinToString(LIST) { sanitize(it) }
    private fun decodeSet(raw: String): Set<String> = raw.split(LIST).filter(String::isNotBlank).toSet()
    private fun encodeMap(values: Map<String, String>): String = values.entries.joinToString(LIST) { "${sanitize(it.key)}$MAP${sanitize(it.value)}" }
    private fun decodeMap(raw: String): Map<String, String> = raw.split(LIST).mapNotNull { val i = it.indexOf(MAP); if (i <= 0) null else it.substring(0, i) to it.substring(i + MAP.length) }.toMap()
    private fun sanitize(value: String): String = value.replace(FIELD, " ").replace(LIST, " ").replace(SUBFIELD, " ").replace(MAP, " ").take(MAX_FIELD)
    private fun tokens(value: String): Set<String> = value.lowercase().split(Regex("[^\\p{L}\\p{N}]+" )).filter { it.length >= 2 }.toSet()
    private fun score(matches: Int, total: Int, importance: Double, pin: Double): Double = (matches.toDouble() / total * 0.7 + importance * 0.2 + pin * 0.1).coerceAtMost(1.0)

    private companion object {
        const val FILE_NAME = "mayra_personal_memory"
        const val KEY_NOTES = "notes"
        const val KEY_EVENTS = "events"
        const val FIELD = "\u001A"
        const val LIST = "\u001B"
        const val SUBFIELD = "\u001C"
        const val MAP = "\u001D"
        const val MAX_FIELD = 3000
        const val MAX_TAGS = 20
        const val MAX_LINKS = 30
        const val MAX_CHECKLIST = 100
        const val MAX_METADATA = 30
        const val MAX_EVENTS = 1500
        const val DEFAULT_ARCHIVE_RETENTION = 90L * 24 * 60 * 60 * 1000
    }
}
