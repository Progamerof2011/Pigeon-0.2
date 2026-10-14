package fr.pigeon.app.protocol

import org.json.JSONObject
import java.util.UUID

/**
 * Un message de chat. Pas de limite de taille côté modèle : la couche fragments s'en charge.
 *
 * Champs prévus pour les versions suivantes :
 *  - [ttl] : nombre de relais restants (relais multi-sauts, pas encore actif en 0.1) ;
 *  - [channel] : canal de discussion (seul "general" existe en 0.1).
 */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val channel: String = GENERAL,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val ttl: Int = DEFAULT_TTL,
) {
    fun toBytes(): ByteArray = JSONObject()
        .put("id", id)
        .put("c", channel)
        .put("s", sender)
        .put("t", text)
        .put("ts", timestamp)
        .put("ttl", ttl)
        .toString()
        .toByteArray(Charsets.UTF_8)

    companion object {
        const val GENERAL = "general"
        const val DEFAULT_TTL = 3

        fun fromBytes(bytes: ByteArray): ChatMessage {
            val json = JSONObject(String(bytes, Charsets.UTF_8))
            return ChatMessage(
                id = json.getString("id"),
                channel = json.optString("c", GENERAL),
                sender = json.getString("s"),
                text = json.getString("t"),
                timestamp = json.getLong("ts"),
                ttl = json.optInt("ttl", DEFAULT_TTL),
            )
        }
    }
}
