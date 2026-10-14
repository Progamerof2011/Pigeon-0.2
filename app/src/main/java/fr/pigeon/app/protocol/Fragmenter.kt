package fr.pigeon.app.protocol

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Découpe un message en fragments qui tiennent dans une écriture BLE.
 *
 * Format d'un fragment :
 *   [16 octets : id du message] [2 octets : index] [2 octets : nombre total] [données]
 *
 * Résultat : la taille du message n'est pas limitée, seul le débit l'est.
 */
object Fragmenter {

    const val HEADER_SIZE = 20

    /**
     * @param chunkSize taille maximale d'un fragment entier (en-tête compris).
     */
    fun split(messageId: UUID, payload: ByteArray, chunkSize: Int): List<ByteArray> {
        val dataSize = chunkSize - HEADER_SIZE
        require(dataSize > 0) { "chunkSize trop petit: $chunkSize" }

        val total = ((payload.size + dataSize - 1) / dataSize).coerceAtLeast(1)
        require(total <= 0xFFFF) { "Message trop grand pour le protocole" }

        return (0 until total).map { index ->
            val start = index * dataSize
            val end = minOf(start + dataSize, payload.size)
            ByteBuffer.allocate(HEADER_SIZE + (end - start))
                .putLong(messageId.mostSignificantBits)
                .putLong(messageId.leastSignificantBits)
                .putShort(index.toShort())
                .putShort(total.toShort())
                .put(payload, start, end - start)
                .array()
        }
    }
}

/**
 * Réassemble les fragments reçus. Un message est rendu quand tous ses fragments sont là.
 * Les messages incomplets depuis trop longtemps sont abandonnés.
 */
class Reassembler(private val timeoutMs: Long = 60_000L) {

    private class Pending(val total: Int, val parts: Array<ByteArray?>, var lastSeen: Long)

    private val pending = HashMap<String, Pending>()

    /**
     * @param source adresse de l'appareil émetteur (évite les collisions d'identifiants).
     * @return le message complet (id, données) quand il est terminé, sinon null.
     */
    @Synchronized
    fun offer(source: String, frame: ByteArray): Pair<UUID, ByteArray>? {
        if (frame.size < Fragmenter.HEADER_SIZE) return null

        val buffer = ByteBuffer.wrap(frame)
        val msb = buffer.getLong()
        val lsb = buffer.getLong()
        val index = buffer.getShort().toInt() and 0xFFFF
        val total = buffer.getShort().toInt() and 0xFFFF
        if (total == 0 || index >= total) return null

        val id = UUID(msb, lsb)
        val key = "$source/$id"
        val now = System.currentTimeMillis()
        pending.entries.removeAll { now - it.value.lastSeen > timeoutMs }

        val entry = pending.getOrPut(key) { Pending(total, arrayOfNulls(total), now) }
        if (entry.total != total) return null

        if (entry.parts[index] == null) {
            entry.parts[index] = frame.copyOfRange(Fragmenter.HEADER_SIZE, frame.size)
        }
        entry.lastSeen = now

        if (entry.parts.any { it == null }) return null

        pending.remove(key)
        val out = ByteArrayOutputStream()
        entry.parts.forEach { out.write(it!!) }
        return id to out.toByteArray()
    }
}
