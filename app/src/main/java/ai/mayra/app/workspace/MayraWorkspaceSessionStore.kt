package ai.mayra.app.workspace

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class MayraWorkspaceSessionStore(context: Context) {
    private val appContext = context.applicationContext
    private val atomicFile = AtomicFile(File(appContext.filesDir, FILE_NAME))

    fun save(session: MayraWorkspaceSession) {
        val plain = session.toJson().toString().toByteArray(Charsets.UTF_8)
        require(plain.size <= MAX_PLAINTEXT_BYTES) { "Workspace session is too large to autosave." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plain)
        val envelope = JSONObject()
            .put("schema", SCHEMA)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString()
            .toByteArray(Charsets.UTF_8)

        val stream = atomicFile.startWrite()
        try {
            stream.write(envelope)
            atomicFile.finishWrite(stream)
        } catch (error: Exception) {
            atomicFile.failWrite(stream)
            throw error
        } finally {
            plain.fill(0)
        }
    }

    fun load(): MayraWorkspaceSession? {
        if (!atomicFile.baseFile.exists()) return null
        return runCatching {
            val bytes = atomicFile.openRead().use { input -> input.readBytes() }
            require(bytes.size <= MAX_ENVELOPE_BYTES) { "Workspace autosave envelope is too large." }
            val envelope = JSONObject(String(bytes, Charsets.UTF_8))
            require(envelope.optString("schema") == SCHEMA) { "Unsupported workspace autosave schema." }
            val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
            require(iv.size == EXPECTED_GCM_IV_BYTES) { "Invalid workspace autosave IV." }
            val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
            require(ciphertext.isNotEmpty()) { "Workspace autosave ciphertext is empty." }
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(ciphertext)
            try {
                MayraWorkspaceSession.fromJson(JSONObject(String(plain, Charsets.UTF_8)))
            } finally {
                plain.fill(0)
            }
        }.getOrElse {
            clearCorrupted()
            null
        }
    }

    fun clear() {
        atomicFile.delete()
    }

    private fun clearCorrupted() {
        runCatching { atomicFile.delete() }
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val FILE_NAME = "mayra_workspace_session_v1.enc"
        const val SCHEMA = "mayra.workspace.session.v1"
        const val KEY_ALIAS = "mayra_workspace_session_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val EXPECTED_GCM_IV_BYTES = 12
        const val MAX_PLAINTEXT_BYTES = 1_000_000
        const val MAX_ENVELOPE_BYTES = 1_500_000
    }
}

private fun MayraWorkspaceSession.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("transcript", JSONArray(transcript))
    .put("tasks", JSONArray().apply { tasks.forEach { put(it.toJson()) } })
    .put("notes", notes)
    .put("table", table?.toJson())
    .put("activeTaskId", activeTaskId)
    .put("revision", revision)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)

private fun MayraWorkspaceTask.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("intent", intent.toJson())
    .put("state", state.name)
    .put("progress", progress)
    .put("statusMessage", statusMessage)
    .put("sources", JSONArray().apply { sources.forEach { put(it.toJson()) } })
    .put("resultSummary", resultSummary)
    .put("verified", verified)
    .put("updatedAt", updatedAt)

private fun MayraWorkspaceIntent.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("rawText", rawText)
    .put("action", action.name)
    .put("entities", JSONObject(entities))
    .put("requiresConfirmation", requiresConfirmation)
    .put("sensitive", sensitive)
    .put("createdAt", createdAt)

private fun MayraSourceReference.toJson(): JSONObject = JSONObject()
    .put("uri", uri)
    .put("displayName", displayName)
    .put("page", page)
    .put("confidence", confidence)
    .put("excerpt", excerpt)

private fun MayraWorkspaceTable.toJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("title", title)
    .put("columns", JSONArray(columns))
    .put("rows", JSONArray().apply { rows.forEach { put(JSONArray(it)) } })
    .put("revision", revision)

private fun MayraWorkspaceSession.Companion.fromJson(json: JSONObject): MayraWorkspaceSession = MayraWorkspaceSession(
    id = json.getString("id"),
    title = json.optString("title", "Mayra Workspace"),
    transcript = json.optJSONArray("transcript").toStringList(),
    tasks = json.optJSONArray("tasks").toObjectList { MayraWorkspaceTask.fromJson(it) },
    notes = json.optString("notes", ""),
    table = json.optJSONObject("table")?.let(MayraWorkspaceTable::fromJson),
    activeTaskId = json.optString("activeTaskId").takeIf(String::isNotBlank),
    revision = json.optLong("revision", 0L),
    createdAt = json.optLong("createdAt", System.currentTimeMillis()),
    updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
)

private fun MayraWorkspaceTask.Companion.fromJson(json: JSONObject): MayraWorkspaceTask = MayraWorkspaceTask(
    id = json.getString("id"),
    intent = MayraWorkspaceIntent.fromJson(json.getJSONObject("intent")),
    state = enumValueOrDefault(json.optString("state"), MayraWorkspaceTaskState.DRAFT),
    progress = json.optInt("progress", 0).coerceIn(0, 100),
    statusMessage = json.optString("statusMessage", "Draft"),
    sources = json.optJSONArray("sources").toObjectList { MayraSourceReference.fromJson(it) },
    resultSummary = json.optString("resultSummary").takeIf(String::isNotBlank),
    verified = json.optBoolean("verified", false),
    updatedAt = json.optLong("updatedAt", System.currentTimeMillis())
)

private fun MayraWorkspaceIntent.Companion.fromJson(json: JSONObject): MayraWorkspaceIntent = MayraWorkspaceIntent(
    id = json.getString("id"),
    rawText = json.getString("rawText"),
    action = enumValueOrDefault(json.optString("action"), MayraWorkspaceActionType.UNKNOWN),
    entities = json.optJSONObject("entities").toStringMap(),
    requiresConfirmation = json.optBoolean("requiresConfirmation", false),
    sensitive = json.optBoolean("sensitive", false),
    createdAt = json.optLong("createdAt", System.currentTimeMillis())
)

private fun MayraSourceReference.Companion.fromJson(json: JSONObject): MayraSourceReference = MayraSourceReference(
    uri = json.getString("uri"),
    displayName = json.optString("displayName", "Source"),
    page = json.optInt("page").takeIf { json.has("page") && !json.isNull("page") },
    confidence = json.optDouble("confidence").takeIf { json.has("confidence") && !json.isNull("confidence") },
    excerpt = json.optString("excerpt").takeIf(String::isNotBlank)
)

private fun MayraWorkspaceTable.Companion.fromJson(json: JSONObject): MayraWorkspaceTable = MayraWorkspaceTable(
    id = json.getString("id"),
    title = json.optString("title", "Untitled table"),
    columns = json.optJSONArray("columns").toStringList(),
    rows = json.optJSONArray("rows").toObjectList { it.toStringList() },
    revision = json.optLong("revision", 0L)
)

private fun JSONArray?.toStringList(): List<String> = if (this == null) emptyList() else buildList {
    for (index in 0 until length()) add(optString(index))
}

private fun <T> JSONArray?.toObjectList(transform: (JSONObject) -> T): List<T> = if (this == null) emptyList() else buildList {
    for (index in 0 until length()) optJSONObject(index)?.let { add(transform(it)) }
}

private fun JSONObject?.toStringMap(): Map<String, String> = if (this == null) emptyMap() else buildMap {
    keys().forEach { key -> put(key, optString(key)) }
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String, default: T): T =
    runCatching { enumValueOf<T>(value) }.getOrDefault(default)

private val MayraWorkspaceSession.Companion: MayraWorkspaceSession
    get() = MayraWorkspaceSession()
private val MayraWorkspaceTask.Companion: MayraWorkspaceTask
    get() = MayraWorkspaceTask(MayraWorkspaceIntent(rawText = "", action = MayraWorkspaceActionType.UNKNOWN))
private val MayraWorkspaceIntent.Companion: MayraWorkspaceIntent
    get() = MayraWorkspaceIntent(rawText = "", action = MayraWorkspaceActionType.UNKNOWN)
private val MayraSourceReference.Companion: MayraSourceReference
    get() = MayraSourceReference(uri = "", displayName = "")
private val MayraWorkspaceTable.Companion: MayraWorkspaceTable
    get() = MayraWorkspaceTable()
