package ai.mayra.app.memory

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

interface MayraMemoryRecordProtector {
    fun protect(plaintext: String): String
    fun unprotect(payload: String): String?
    fun isProtected(payload: String): Boolean
}

/** AES-GCM record protection backed by the Android Keystore. */
internal class AndroidKeystoreMayraMemoryProtector(
    private val alias: String = KEY_ALIAS
) : MayraMemoryRecordProtector {
    override fun protect(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return listOf(ENVELOPE_VERSION, b64(cipher.iv), b64(ciphertext)).joinToString(DELIMITER)
    }

    override fun unprotect(payload: String): String? = runCatching {
        val parts = payload.split(DELIMITER)
        require(parts.size == 3 && parts[0] == ENVELOPE_VERSION)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_BITS, unb64(parts[1])))
        String(cipher.doFinal(unb64(parts[2])), Charsets.UTF_8)
    }.getOrNull()

    override fun isProtected(payload: String): Boolean = payload.startsWith("$ENVELOPE_VERSION$DELIMITER")

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE).apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun b64(value: ByteArray): String = Base64.encodeToString(value, Base64.NO_WRAP)
    private fun unb64(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    private companion object {
        const val KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "mayra.personal.memory.aes.v1"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val TAG_BITS = 128
        const val ENVELOPE_VERSION = "enc1"
        const val DELIMITER = "."
    }
}
