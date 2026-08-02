package ai.mayra.app.core

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Android Keystore-backed provider credential store.
 *
 * The API key is never written in plaintext. Only AES-GCM ciphertext and IV are stored in
 * SharedPreferences; the encryption key remains inside AndroidKeyStore. Corrupted or unreadable
 * ciphertext is cleared instead of being exposed or repeatedly retried.
 */
class AndroidMayraProviderCredentialStore(context: Context) : MayraProviderCredentialSource {
    private val preferences = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    override fun bearerToken(): String? = read()

    fun hasCredential(): Boolean = read()?.isNotBlank() == true

    fun write(token: String): Result<Unit> = runCatching {
        val clean = token.trim()
        require(clean.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH) { "Provider API key length is invalid." }
        require(clean.none(Char::isWhitespace)) { "Provider API key cannot contain spaces." }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(clean.toByteArray(Charsets.UTF_8))
        val iv = cipher.iv
        require(iv.size == EXPECTED_GCM_IV_BYTES) { "Unexpected encryption IV size." }

        check(
            preferences.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                .putString(KEY_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .commit()
        ) { "Unable to persist encrypted provider credential." }
    }

    fun clear(): Boolean = preferences.edit()
        .remove(KEY_CIPHERTEXT)
        .remove(KEY_IV)
        .commit()

    private fun read(): String? {
        val ciphertext = preferences.getString(KEY_CIPHERTEXT, null) ?: return null
        val ivValue = preferences.getString(KEY_IV, null) ?: return clearCorrupted()
        return runCatching {
            val iv = Base64.decode(ivValue, Base64.NO_WRAP)
            require(iv.size == EXPECTED_GCM_IV_BYTES) { "Invalid encrypted-key IV." }
            val encrypted = Base64.decode(ciphertext, Base64.NO_WRAP)
            require(encrypted.isNotEmpty()) { "Encrypted provider key is empty." }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
                .trim()
                .takeIf { it.length in MIN_TOKEN_LENGTH..MAX_TOKEN_LENGTH && it.none(Char::isWhitespace) }
                ?: error("Decrypted provider key is invalid.")
        }.getOrElse { clearCorrupted() }
    }

    private fun clearCorrupted(): String? {
        clear()
        return null
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
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val PREFS = "mayra_provider_credentials_v1"
        const val KEY_CIPHERTEXT = "ciphertext"
        const val KEY_IV = "iv"
        const val KEY_ALIAS = "mayra_provider_api_key_v1"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val EXPECTED_GCM_IV_BYTES = 12
        const val MIN_TOKEN_LENGTH = 12
        const val MAX_TOKEN_LENGTH = 512
    }
}
