package pro.sihao.jarvis.data.encryption

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApikeyEncryption @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val encryptionPrefs = EncryptedSharedPreferences.create(
        context,
        "jarvis_encryption_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun encryptApiKey(apiKey: String): String {
        // For simplicity, we'll use a basic approach where we store the encrypted key
        // in encrypted shared preferences and return a reference to it
        val keyRef = "api_key_${System.currentTimeMillis()}_${apiKey.hashCode()}"
        encryptionPrefs.edit()
            .putString(keyRef, apiKey)
            .apply()
        return keyRef
    }

    fun decryptApiKey(keyRef: String?): String? {
        return if (keyRef.isNullOrBlank()) {
            null
        } else {
            encryptionPrefs.getString(keyRef, null)
        }
    }

    fun deleteApiKey(keyRef: String?) {
        if (!keyRef.isNullOrBlank()) {
            encryptionPrefs.edit()
                .remove(keyRef)
                .apply()
        }
    }
}