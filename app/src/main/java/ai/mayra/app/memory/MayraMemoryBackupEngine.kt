package ai.mayra.app.memory

import ai.mayra.app.knowledge.ChecklistItem
import ai.mayra.app.knowledge.MayraPersonalMemory
import ai.mayra.app.knowledge.PersonalNote
import ai.mayra.app.knowledge.PersonalNoteType
import ai.mayra.app.knowledge.TimelineEvent
import ai.mayra.app.knowledge.TimelineEventType
import ai.mayra.app.knowledge.TimelineImportance
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable password-protected backup format for owner-controlled Mayra memory exports. */
object MayraMemoryBackupEngine {
    private const val ENVELOPE_HEADER = "MAYRA_ENCRYPTED_BACKUP_V1"
    private const val PAYLOAD_HEADER = "MAYRA_MEMORY_PAYLOAD_V1"
    private const val ITERATIONS = 210_000
    private const val KEY_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val MAX_ENVELOPE_CHARS = 5_000_000
    private const val MAX_RECORDS = 2_000
    private val base64 = Base64.getEncoder()
    private val decoder = Base64.getDecoder()

    data class BackupPayload(
        val notes: List<PersonalNote>,
        val events: List<TimelineEvent>,
        val generatedAt: Long
    )

    data class ImportPreview(
        val notesInBackup: Int,
        val eventsInBackup: Int,
        val newNotes: Int,
        val duplicateNotes: Int,
        val newEvents: Int,
        val duplicateEvents: Int,
        val generatedAt: Long
    )

    data class RestoreResult(
        val notesAdded: Int,
        val notesSkipped: Int,
        val eventsAdded: Int,
        val eventsSkipped: Int
    )

    fun requireStrongPassword(password: CharArray) {
        require(password.size >= 8) { "Use at least 8 characters for the backup password" }
        require(password.any(Char::isLetter) && password.any(Char::isDigit)) {
            "Backup password must contain at least one letter and one number"
        }
    }

    fun export(memory: MayraPersonalMemory, now: Long = System.currentTimeMillis()): BackupPayload = BackupPayload(
        notes = memory.notes(includeArchived = true).filterNot { it.sensitive }.take(MAX_RECORDS),
        events = memory.timeline(includeSensitive = false, limit = MAX_RECORDS),
        generatedAt = now
    )

    fun encrypt(payload: BackupPayload, password: CharArray, random: SecureRandom = SecureRandom()): String {
        requireStrongPassword(password)
        val plaintext = serialize(payload).toByteArray(Charsets.UTF_8)
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val key = deriveKey(password, salt, ITERATIONS)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(ENVELOPE_HEADER.toByteArray(Charsets.UTF_8))
        val encrypted = cipher.doFinal(plaintext)
        password.fill('\u0000')
        return buildString {
            appendLine(ENVELOPE_HEADER)
            appendLine("iterations=$ITERATIONS")
            appendLine("salt=${base64.encodeToString(salt)}")
            appendLine("iv=${base64.encodeToString(iv)}")
            append("ciphertext=${base64.encodeToString(encrypted)}")
        }
    }

    fun decrypt(envelope: String, password: CharArray): BackupPayload {
        require(envelope.length in 1..MAX_ENVELOPE_CHARS) { "Backup file is empty or too large" }
        requireStrongPassword(password)
        val fields = envelope.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        require(fields.firstOrNull() == ENVELOPE_HEADER) { "Unsupported Mayra backup format" }
        val values = fields.drop(1).mapNotNull { line ->
            val index = line.indexOf('=')
            if (index <= 0) null else line.substring(0, index) to line.substring(index + 1)
        }.toMap()
        val iterations = values["iterations"]?.toIntOrNull() ?: error("Backup iteration count is missing")
        require(iterations in 100_000..1_000_000) { "Unsafe or unsupported backup iteration count" }
        val salt = decodeExact(values["salt"], SALT_BYTES, "salt")
        val iv = decodeExact(values["iv"], IV_BYTES, "initialization vector")
        val encrypted = decode(values["ciphertext"], "ciphertext")
        require(encrypted.size >= 16) { "Backup ciphertext is invalid" }
        val key = deriveKey(password, salt, iterations)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        cipher.updateAAD(ENVELOPE_HEADER.toByteArray(Charsets.UTF_8))
        val plaintext = runCatching { cipher.doFinal(encrypted) }
            .getOrElse { throw IllegalArgumentException("Wrong password or modified backup file", it) }
        password.fill('\u0000')
        return deserialize(plaintext.toString(Charsets.UTF_8))
    }

    fun preview(payload: BackupPayload, memory: MayraPersonalMemory): ImportPreview {
        val noteIds = memory.notes(includeArchived = true).mapTo(mutableSetOf()) { it.id }
        val eventIds = memory.timeline(includeSensitive = true, limit = Int.MAX_VALUE).mapTo(mutableSetOf()) { it.id }
        return ImportPreview(
            notesInBackup = payload.notes.size,
            eventsInBackup = payload.events.size,
            newNotes = payload.notes.count { it.id !in noteIds },
            duplicateNotes = payload.notes.count { it.id in noteIds },
            newEvents = payload.events.count { it.id !in eventIds },
            duplicateEvents = payload.events.count { it.id in eventIds },
            generatedAt = payload.generatedAt
        )
    }

    /** Adds only missing IDs. Existing Mayra memory is never silently overwritten or deleted. */
    fun restore(payload: BackupPayload, memory: MayraPersonalMemory): RestoreResult {
        val noteIds = memory.notes(includeArchived = true).mapTo(mutableSetOf()) { it.id }
        val eventIds = memory.timeline(includeSensitive = true, limit = Int.MAX_VALUE).mapTo(mutableSetOf()) { it.id }
        var notesAdded = 0
        var eventsAdded = 0
        payload.notes.filterNot { it.sensitive }.forEach { note ->
            if (note.id !in noteIds) {
                memory.saveNote(note, addTimeline = false)
                noteIds += note.id
                notesAdded++
            }
        }
        payload.events.filterNot { it.sensitive }.forEach { event ->
            if (event.id !in eventIds) {
                memory.appendEvent(event)
                eventIds += event.id
                eventsAdded++
            }
        }
        return RestoreResult(
            notesAdded = notesAdded,
            notesSkipped = payload.notes.size - notesAdded,
            eventsAdded = eventsAdded,
            eventsSkipped = payload.events.size - eventsAdded
        )
    }

    internal fun serialize(payload: BackupPayload): String = buildString {
        appendLine(PAYLOAD_HEADER)
        appendLine("generatedAt=${payload.generatedAt}")
        payload.notes.take(MAX_RECORDS).forEach { appendLine(encodeNote(it)) }
        payload.events.take(MAX_RECORDS).forEach { appendLine(encodeEvent(it)) }
    }

    internal fun deserialize(raw: String): BackupPayload {
        val lines = raw.lineSequence().filter(String::isNotBlank).toList()
        require(lines.firstOrNull() == PAYLOAD_HEADER) { "Backup payload version is unsupported" }
        val generatedAt = lines.getOrNull(1)?.substringAfter("generatedAt=", "")?.toLongOrNull()
            ?: error("Backup generation time is missing")
        val notes = mutableListOf<PersonalNote>()
        val events = mutableListOf<TimelineEvent>()
        lines.drop(2).take(MAX_RECORDS * 2).forEach { line ->
            when {
                line.startsWith("N|") -> decodeNote(line)?.let(notes::add)
                line.startsWith("E|") -> decodeEvent(line)?.let(events::add)
            }
        }
        return BackupPayload(notes, events, generatedAt)
    }

    private fun encodeNote(note: PersonalNote): String = listOf(
        "N", e(note.id), note.type.name, e(note.title), e(note.body), e(note.tags.joinToString("\u001F")),
        e(note.linkedEntityIds.joinToString("\u001F")), e(encodeChecklist(note.checklist)), note.priority.toString(),
        note.pinned.toString(), note.createdAt.toString(), note.updatedAt.toString(), (note.archivedAt ?: -1L).toString()
    ).joinToString("|")

    private fun decodeNote(line: String): PersonalNote? = runCatching {
        val p = line.split('|')
        require(p.size == 13)
        PersonalNote(
            id = d(p[1]), type = PersonalNoteType.valueOf(p[2]), title = d(p[3]), body = d(p[4]),
            tags = splitSet(d(p[5])), linkedEntityIds = splitSet(d(p[6])), checklist = decodeChecklist(d(p[7])),
            priority = p[8].toInt(), pinned = p[9].toBooleanStrict(), sensitive = false,
            createdAt = p[10].toLong(), updatedAt = p[11].toLong(), archivedAt = p[12].toLong().takeIf { it >= 0 }
        )
    }.getOrNull()

    private fun encodeEvent(event: TimelineEvent): String = listOf(
        "E", e(event.id), event.type.name, e(event.title), e(event.description), event.occurredAt.toString(),
        e(event.linkedEntityIds.joinToString("\u001F")), e(event.linkedNoteId.orEmpty()), event.importance.name,
        e(event.source), e(event.metadata.entries.joinToString("\u001E") { "${it.key}\u001D${it.value}" })
    ).joinToString("|")

    private fun decodeEvent(line: String): TimelineEvent? = runCatching {
        val p = line.split('|')
        require(p.size == 11)
        TimelineEvent(
            id = d(p[1]), type = TimelineEventType.valueOf(p[2]), title = d(p[3]), description = d(p[4]),
            occurredAt = p[5].toLong(), linkedEntityIds = splitSet(d(p[6])), linkedNoteId = d(p[7]).ifBlank { null },
            importance = TimelineImportance.valueOf(p[8]), source = d(p[9]), metadata = decodeMap(d(p[10])), sensitive = false
        )
    }.getOrNull()

    private fun encodeChecklist(items: List<ChecklistItem>): String = items.joinToString("\u001E") {
        listOf(e(it.id), e(it.text), it.completed, it.completedAt ?: -1L).joinToString("\u001D")
    }

    private fun decodeChecklist(raw: String): List<ChecklistItem> = if (raw.isBlank()) emptyList() else raw.split("\u001E").mapNotNull { item ->
        runCatching {
            val p = item.split("\u001D")
            require(p.size == 4)
            ChecklistItem(d(p[0]), d(p[1]), p[2].toBooleanStrict(), p[3].toLong().takeIf { it >= 0 })
        }.getOrNull()
    }

    private fun splitSet(raw: String): Set<String> = raw.split("\u001F").filter(String::isNotBlank).toSet()
    private fun decodeMap(raw: String): Map<String, String> = if (raw.isBlank()) emptyMap() else raw.split("\u001E").mapNotNull {
        val index = it.indexOf("\u001D")
        if (index <= 0) null else it.substring(0, index) to it.substring(index + 1)
    }.toMap()

    private fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        val encoded = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        spec.clearPassword()
        return SecretKeySpec(encoded, "AES")
    }

    private fun decodeExact(value: String?, expected: Int, label: String): ByteArray = decode(value, label).also {
        require(it.size == expected) { "Backup $label is invalid" }
    }

    private fun decode(value: String?, label: String): ByteArray = runCatching { decoder.decode(value ?: error("Backup $label is missing")) }
        .getOrElse { throw IllegalArgumentException("Backup $label is invalid", it) }

    private fun e(value: String): String = base64.encodeToString(value.toByteArray(Charsets.UTF_8))
    private fun d(value: String): String = decoder.decode(value).toString(Charsets.UTF_8)
}
