package fr.shinobiwan.tvhome.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.map
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.settingsStore by preferencesDataStore("tvhome")
data class Settings(val city: String = "Lézignan-Corbières", val host: String = "192.168.1.195", val port: String = "8767", val pin: String = "", val encryptedToken: String = "",
    val order: List<String> = emptyList(), val hidden: Set<String> = emptySet(), val widgets: List<String> = listOf("codex", "media", "clock"),
    val mediaSource: String = "AUTO", val demo: Boolean = false, val protocol: String = "HUB_V2", val caPem: String = "")
class Preferences(private val context: Context) {
    private val city = stringPreferencesKey("city")
    private val host = stringPreferencesKey("host")
    private val port = stringPreferencesKey("port")
    private val pin = stringPreferencesKey("pin")
    private val token = stringPreferencesKey("encryptedToken")
    private val order = stringPreferencesKey("order")
    private val hidden = stringSetPreferencesKey("hidden")
    private val widgets = stringPreferencesKey("widgets")
    private val source = stringPreferencesKey("source")
    private val demo = booleanPreferencesKey("demo")
    private val protocol = stringPreferencesKey("protocol")
    private val ca = stringPreferencesKey("hubCaPem")
    val flow = context.settingsStore.data.map { p -> Settings(p[city] ?: "Lézignan-Corbières", p[host] ?: "192.168.1.195", p[port] ?: "8767", p[pin] ?: "", p[token] ?: "",
        p[order]?.split('|')?.filter { it.isNotBlank() } ?: emptyList(), p[hidden] ?: emptySet(), p[widgets]?.split('|')?.filter { it in setOf("codex","media","clock","telemetry") }?.take(3) ?: listOf("telemetry","media","codex"), p[source] ?: "AUTO", p[demo] ?: false,
        p[protocol] ?: if (p[host].isNullOrBlank()) "HUB_V2" else "V1", p[ca] ?: "") }
    suspend fun save(s: Settings) { context.settingsStore.edit { p ->
        p[city]=s.city; p[host]=s.host; p[port]=s.port; p[pin]=s.pin; p[token]=s.encryptedToken
        p[order]=s.order.joinToString("|"); p[hidden]=s.hidden; p[widgets]=s.widgets.take(3).joinToString("|"); p[source]=s.mediaSource; p[demo]=s.demo
        p[protocol]=s.protocol; p[ca]=s.caPem
    } }
    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("tvhome-pairing", null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("tvhome-pairing", KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    fun encrypt(secret: String): String {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key()) }
        return Base64.encodeToString(cipher.iv + cipher.doFinal(secret.toByteArray(Charsets.UTF_8)), Base64.NO_WRAP)
    }
    fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val bytes=Base64.decode(value, Base64.NO_WRAP)
            val cipher=Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, bytes.copyOfRange(0,12))) }
            String(cipher.doFinal(bytes.copyOfRange(12,bytes.size)), Charsets.UTF_8)
        } catch (_: Exception) { "" }
    }
}
