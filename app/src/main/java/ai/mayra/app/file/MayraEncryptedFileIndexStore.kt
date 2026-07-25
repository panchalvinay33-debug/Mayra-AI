package ai.mayra.app.file

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

class MayraEncryptedFileIndexStore(context: Context) {
    private val atomicFile = AtomicFile(File(context.applicationContext.filesDir, FILE_NAME))

    @Synchronized
    fun read(): MayraFileIndexSnapshot {
        if (!atomicFile.baseFile.exists()) return MayraFileIndexSnapshot()
        return runCatching {
            val envelopeBytes = atomicFile.openRead().use { it.readBytes() }
            require(envelopeBytes.size <= MAX_ENVELOPE_BYTES) { "File index envelope is too large." }
            val envelope = JSONObject(String(envelopeBytes, Charsets.UTF_8))
            require(envelope.optString("schema") == SCHEMA) { "Unsupported file index schema." }
            val iv = Base64.decode(envelope.getString("iv"), Base64.NO_WRAP)
            require(iv.size == 12) { "Invalid file index IV." }
            val ciphertext = Base64.decode(envelope.getString("ciphertext"), Base64.NO_WRAP)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val plain = cipher.doFinal(ciphertext)
            try { decode(JSONObject(String(plain, Charsets.UTF_8))) } finally { plain.fill(0) }
        }.getOrElse {
            atomicFile.delete()
            MayraFileIndexSnapshot()
        }
    }

    @Synchronized
    fun write(snapshot: MayraFileIndexSnapshot) {
        val bounded = snapshot.copy(
            files = snapshot.files.sortedByDescending { it.modifiedAt }.take(MAX_FILES),
            grants = snapshot.grants.take(MAX_GRANTS)
        )
        val plain = encode(bounded).toString().toByteArray(Charsets.UTF_8)
        require(plain.size <= MAX_PLAINTEXT_BYTES) { "File index is too large." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal(plain)
        val envelope = JSONObject()
            .put("schema", SCHEMA)
            .put("iv", Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .put("ciphertext", Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .toString().toByteArray(Charsets.UTF_8)
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

    @Synchronized
    fun merge(files: List<MayraIndexedFile>, grants: List<MayraFileGrant>? = null): MayraFileIndexSnapshot {
        val current = read()
        val merged = current.files.associateBy { it.uri }.toMutableMap()
        files.forEach { candidate ->
            val old = merged[candidate.uri]
            if (old == null || old.fingerprint != candidate.fingerprint || candidate.indexedAt >= old.indexedAt) {
                merged[candidate.uri] = candidate
            }
        }
        val next = current.copy(
            files = merged.values.toList(),
            grants = grants ?: current.grants,
            generation = current.generation + 1,
            updatedAt = System.currentTimeMillis()
        )
        write(next)
        return next
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").run {
            init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build())
            generateKey()
        }
    }

    private fun encode(snapshot: MayraFileIndexSnapshot) = JSONObject()
        .put("generation", snapshot.generation)
        .put("updatedAt", snapshot.updatedAt)
        .put("grants", JSONArray().apply { snapshot.grants.forEach { grant -> put(JSONObject()
            .put("treeUri", grant.treeUri).put("label", grant.label).put("grantedAt", grant.grantedAt)
            .put("lastScanAt", grant.lastScanAt).put("enabled", grant.enabled)) } })
        .put("files", JSONArray().apply { snapshot.files.forEach { file -> put(JSONObject()
            .put("uri", file.uri).put("displayName", file.displayName).put("mimeType", file.mimeType)
            .put("sizeBytes", file.sizeBytes).put("modifiedAt", file.modifiedAt)
            .put("sourceKind", file.sourceKind.name).put("relativeLocation", file.relativeLocation)
            .put("fingerprint", file.fingerprint).put("state", file.state.name)
            .put("extractedText", file.extractedText).put("indexedAt", file.indexedAt)
            .put("failure", file.failure)) } })

    private fun decode(json: JSONObject): MayraFileIndexSnapshot = MayraFileIndexSnapshot(
        files = json.optJSONArray("files").objects().mapNotNull { item -> runCatching {
            MayraIndexedFile(
                uri = item.getString("uri"), displayName = item.getString("displayName"),
                mimeType = item.nullable("mimeType"), sizeBytes = item.optLong("sizeBytes"),
                modifiedAt = item.optLong("modifiedAt"),
                sourceKind = enumOr(item.optString("sourceKind"), MayraIndexedSourceKind.SAF_DOCUMENT),
                relativeLocation = item.nullable("relativeLocation"), fingerprint = item.getString("fingerprint"),
                state = enumOr(item.optString("state"), MayraIndexState.METADATA_ONLY),
                extractedText = item.nullable("extractedText"), indexedAt = item.optLong("indexedAt"),
                failure = item.nullable("failure")
            )
        }.getOrNull() },
        grants = json.optJSONArray("grants").objects().mapNotNull { item -> runCatching {
            MayraFileGrant(item.getString("treeUri"), item.optString("label", "Folder"),
                item.optLong("grantedAt"), item.optLong("lastScanAt"), item.optBoolean("enabled", true))
        }.getOrNull() },
        generation = json.optLong("generation"), updatedAt = json.optLong("updatedAt")
    )

    private fun JSONArray?.objects(): List<JSONObject> = if (this == null) emptyList() else buildList {
        for (i in 0 until length()) optJSONObject(i)?.let(::add)
    }
    private fun JSONObject.nullable(key: String): String? = if (!has(key) || isNull(key)) null else optString(key).takeIf { it.isNotBlank() }
    private inline fun <reified T : Enum<T>> enumOr(value: String, fallback: T): T = runCatching { enumValueOf<T>(value) }.getOrDefault(fallback)

    private companion object {
        const val FILE_NAME = "mayra_file_index_v1.enc"
        const val SCHEMA = "mayra.file.index.v1"
        const val KEY_ALIAS = "mayra_file_index_key_v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val MAX_FILES = 20_000
        const val MAX_GRANTS = 64
        const val MAX_PLAINTEXT_BYTES = 12_000_000
        const val MAX_ENVELOPE_BYTES = 17_000_000
    }
}
