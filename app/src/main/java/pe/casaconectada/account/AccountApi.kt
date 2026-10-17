package pe.casaconectada.account

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Solo para compilaciones debug con `adb reverse tcp:8787 tcp:8787`. */
@Singleton
class AccountApi @Inject constructor() {
    private val baseUrl = "http://127.0.0.1:8787"

    suspend fun request(method: String, path: String, payload: JSONObject? = null, token: String? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection)
            try {
                connection.requestMethod = method
                connection.connectTimeout = 5000
                connection.readTimeout = 10000
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Cache-Control", "no-store")
                if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
                if (payload != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                val result = try { JSONObject(text) } catch (_: Exception) { JSONObject() }
                if (status !in 200..299) {
                    val code = result.optString("error", "http_$status")
                    throw AccountApiException(status, code)
                }
                result
            } finally {
                connection.disconnect()
            }
        }
}

class AccountApiException(val status: Int, val code: String) : Exception(code)

/** El bearer token se cifra mediante AES-GCM con una clave no exportable de Android Keystore. */
@Singleton
class AccountTokenStore @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("cc_account_debug_v1", Context.MODE_PRIVATE)
    private val alias = "cc_account_debug_session_aes_v1"

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build())
        }.generateKey()
    }

    fun save(token: String) {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val iv = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        val content = Base64.encodeToString(cipher.doFinal(token.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
        check(prefs.edit().putString("session", "$iv:$content").commit()) { "No se pudo guardar la sesión" }
    }

    fun load(): String? {
        val value = prefs.getString("session", null) ?: return null
        return try {
            val parts = value.split(':', limit = 2)
            if (parts.size != 2) return null
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, Base64.decode(parts[0], Base64.NO_WRAP)))
            val token = String(cipher.doFinal(Base64.decode(parts[1], Base64.NO_WRAP)), Charsets.UTF_8)
            token.takeIf { it.matches(Regex("[a-f0-9]{64}")) }
        } catch (_: Exception) {
            clear()
            null
        }
    }

    fun clear() { prefs.edit().remove("session").commit() }
}

data class AccountProfile(val id: String, val email: String, val name: String,
    val username: String, val phone: String, val bio: String)
data class AccountFamily(val id: String, val name: String, val role: String)
data class AccountMember(val name: String, val role: String)

data class AccountSnapshot(val profile: AccountProfile, val families: List<AccountFamily>)

fun parseAccount(json: JSONObject): AccountSnapshot {
    val p = json.optJSONObject("profile") ?: throw IllegalStateException("Perfil inválido")
    fun field(key: String) = if (p.isNull(key)) "" else p.optString(key)
    val profile = AccountProfile(field("id"), field("email"), field("displayName"),
        field("username"), field("phone"), field("bio"))
    if (profile.id.isBlank()) throw IllegalStateException("Perfil sin identificación")
    val families = mutableListOf<AccountFamily>()
    val array = json.optJSONArray("families") ?: JSONArray()
    for (i in 0 until array.length()) {
        val f = array.optJSONObject(i) ?: continue
        families.add(AccountFamily(f.optString("id"), f.optString("name"), f.optString("role")))
    }
    return AccountSnapshot(profile, families)
}
