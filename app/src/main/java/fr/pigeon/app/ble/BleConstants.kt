package fr.pigeon.app.ble

import java.util.UUID

object BleConstants {
    /** UUID du service Pigeon : tous les appareils de l'app l'annoncent et le scannent. */
    val SERVICE_UUID: UUID = UUID.fromString("7a1f0000-5b9e-4c1a-9d2e-70696765726f")

    /** Caractéristique en écriture sur laquelle les messages sont envoyés par fragments. */
    val MESSAGE_CHAR_UUID: UUID = UUID.fromString("7a1f0001-5b9e-4c1a-9d2e-70696765726f")

    /** MTU demandé. Android peut en négocier moins selon l'appareil. */
    const val REQUESTED_MTU = 517

    /** En dessous, un fragment ne peut pas contenir l'en-tête et des données utiles. */
    const val MIN_MTU = 64

    /** Surcharge ATT ajoutée par la couche BLE à chaque écriture. */
    const val ATT_OVERHEAD = 3
}
