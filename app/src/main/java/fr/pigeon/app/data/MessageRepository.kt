package fr.pigeon.app.data

import android.util.Log
import fr.pigeon.app.ble.BleTransport
import fr.pigeon.app.protocol.ChatMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Historique des messages. En 0.1 il est seulement en mémoire :
 * il sera remplacé par Room (base locale) dans une version suivante.
 */
class MessageRepository(
    private val ble: BleTransport,
    scope: CoroutineScope,
    val localName: String,
) {
    private val seenIds = HashSet<String>()
    private val ownIds = HashSet<String>()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    init {
        scope.launch {
            ble.incoming.collect { bytes ->
                runCatching { ChatMessage.fromBytes(bytes) }
                    .onSuccess { add(it) }
                    .onFailure { Log.w(TAG, "Message invalide ignoré", it) }
            }
        }
    }

    /** Envoie un message sur le canal général. */
    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        val message = ChatMessage(sender = localName, text = clean)
        ownIds.add(message.id)
        if (add(message)) ble.broadcast(UUID.fromString(message.id), message.toBytes())
    }

    fun isMine(message: ChatMessage): Boolean = message.id in ownIds

    /** Ajoute un message s'il n'a pas déjà été vu (anti-doublons). */
    @Synchronized
    private fun add(message: ChatMessage): Boolean {
        if (!seenIds.add(message.id)) return false
        _messages.value = (_messages.value + message).sortedBy { it.timestamp }
        return true
    }

    private companion object {
        const val TAG = "Pigeon-Repo"
    }
}
