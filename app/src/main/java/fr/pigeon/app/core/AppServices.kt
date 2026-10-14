package fr.pigeon.app.core

import fr.pigeon.app.ble.BleTransport
import fr.pigeon.app.data.MessageRepository

/**
 * Conteneur des services partagés entre les écrans.
 * Pour ajouter un service (NFC, stockage, etc.), le déclarer ici.
 */
data class AppServices(
    val ble: BleTransport,
    val repository: MessageRepository,
)
