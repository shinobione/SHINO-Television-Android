package fr.shinobiwan.tvhome.data

import fr.shinobiwan.tvhome.core.isLanIpv4
import org.json.JSONObject

/** Configuration transférée volontairement depuis un fichier local/USB via le sélecteur Android. */
data class HubPairingConfig(
    val host: String,
    val port: String,
    val caPem: String,
    val certificateSha256: String,
    val readerToken: String
)

fun parseHubPairingConfig(raw: String): HubPairingConfig {
    require(raw.toByteArray(Charsets.UTF_8).size <= 24576) { "Fichier trop volumineux" }
    val root = JSONObject(raw.removePrefix("\uFEFF"))
    fun str(key: String): String =
        (root.get(key) as? String)?.trim() ?: throw IllegalArgumentException("Champ invalide : $key")

    require(str("format") == "SHINO_TV_HOME_HUB_PAIRING")
    require(root.get("version") == 1)
    val host = str("host")
    require(isLanIpv4(host)) { "Adresse privée invalide" }
    val portValue = root.get("port")
    require(portValue is Int && portValue in 1024..65535) { "Port invalide" }
    val caPem = str("caPem")
    val pin = str("certificateSha256").lowercase().replace(":", "").replace(" ", "")
    val token = str("readerToken")
    require(validReaderToken(token)) { "Jeton TV_HOME invalide" }
    // Reuse the exact CA and pin checks already used by the existing HUB client.
    hubTrustManager(caPem, pin)
    return HubPairingConfig(host, portValue.toString(), caPem, pin, token)
}
