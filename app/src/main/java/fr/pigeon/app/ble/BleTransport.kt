package fr.pigeon.app.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import fr.pigeon.app.protocol.Fragmenter
import fr.pigeon.app.protocol.Reassembler
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val TAG = "Pigeon-BLE"

/**
 * Couche Bluetooth Low Energy.
 *
 * Chaque appareil fait trois choses à la fois :
 *  - il annonce le service Pigeon (advertising) ;
 *  - il scanne et se connecte aux autres appareils Pigeon (client GATT) ;
 *  - il héberge une caractéristique en écriture qui reçoit les fragments (serveur GATT).
 *
 * Un message est envoyé en écrivant ses fragments sur chaque appareil connecté.
 * Les fragments reçus sont réassemblés puis émis dans [incoming].
 */
@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
class BleTransport(context: Context) {

    private val appContext = context.applicationContext
    private val adapter: BluetoothAdapter? =
        (appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter

    private val reassembler = Reassembler()
    private val links = ConcurrentHashMap<String, PeerLink>()

    private var server: BluetoothGattServer? = null
    private var advertiser: BluetoothLeAdvertiser? = null
    private var scanner: BluetoothLeScanner? = null
    private var running = false

    private val _incoming = MutableSharedFlow<ByteArray>(extraBufferCapacity = 64)

    /** Messages complets reçus, sous forme de JSON (voir ChatMessage). */
    val incoming: SharedFlow<ByteArray> = _incoming.asSharedFlow()

    private val _peers = MutableStateFlow<Set<String>>(emptySet())

    /** Adresses des appareils prêts à recevoir des messages. */
    val peers: StateFlow<Set<String>> = _peers.asStateFlow()

    private val _status = MutableStateFlow("Arrêté")

    /** Texte d'état lisible par l'interface. */
    val status: StateFlow<String> = _status.asStateFlow()

    @Synchronized
    fun start() {
        if (running) return
        val bt = adapter
        if (bt == null || !bt.isEnabled) {
            _status.value = "Bluetooth désactivé"
            return
        }
        try {
            running = true
            startServer()
            startAdvertising()
            startScanning()
            _status.value = "Actif"
        } catch (e: SecurityException) {
            running = false
            _status.value = "Autorisations Bluetooth manquantes"
            Log.e(TAG, "Permissions absentes", e)
        }
    }

    @Synchronized
    fun stop() {
        if (!running) return
        running = false
        runCatching { scanner?.stopScan(scanCallback) }
        runCatching { advertiser?.stopAdvertising(advertiseCallback) }
        links.values.forEach {
            it.gatt.disconnect()
            it.gatt.close()
        }
        links.clear()
        server?.close()
        server = null
        _peers.value = emptySet()
        _status.value = "Arrêté"
    }

    /** Envoie un message à tous les appareils actuellement connectés. */
    fun broadcast(messageId: UUID, payload: ByteArray) {
        links.values.forEach { it.enqueue(messageId, payload) }
    }

    // ---------------------------------------------------------------- Serveur GATT

    private fun startServer() {
        val manager = appContext.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        server = manager.openGattServer(appContext, serverCallback)?.also { gattServer ->
            val characteristic = BluetoothGattCharacteristic(
                BleConstants.MESSAGE_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE,
                BluetoothGattCharacteristic.PERMISSION_WRITE,
            )
            val service = BluetoothGattService(
                BleConstants.SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY,
            )
            service.addCharacteristic(characteristic)
            gattServer.addService(service)
        } ?: Log.w(TAG, "Impossible d'ouvrir le serveur GATT")
    }

    private val serverCallback = object : BluetoothGattServerCallback() {
        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?,
        ) {
            if (responseNeeded) {
                server?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, null)
            }
            val frame = value ?: return
            reassembler.offer(device.address, frame)?.let { (_, payload) ->
                _incoming.tryEmit(payload)
            }
        }
    }

    // ---------------------------------------------------------------- Publicité et scan

    private fun startAdvertising() {
        advertiser = adapter?.bluetoothLeAdvertiser
        if (advertiser == null) {
            Log.w(TAG, "Publicité BLE non supportée par cet appareil")
            return
        }
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(true)
            .build()
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "Échec de la publicité BLE: $errorCode")
        }
    }

    private fun startScanning() {
        scanner = adapter?.bluetoothLeScanner
        val filter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(BleConstants.SERVICE_UUID))
            .build()
        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        scanner?.startScan(listOf(filter), settings, scanCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val address = result.device.address
            if (links.containsKey(address)) return
            Log.d(TAG, "Appareil Pigeon trouvé: $address")
            val gatt = result.device.connectGatt(
                appContext, false, clientCallback, BluetoothDevice.TRANSPORT_LE,
            )
            links.getOrPut(address) { PeerLink(gatt) }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.w(TAG, "Échec du scan BLE: $errorCode")
        }
    }

    // ---------------------------------------------------------------- Client GATT

    private val clientCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.d(TAG, "Connecté à $address")
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Déconnecté de $address (status $status)")
                links.remove(address)
                gatt.close()
                updatePeers()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val found = gatt.getService(BleConstants.SERVICE_UUID)
                ?.getCharacteristic(BleConstants.MESSAGE_CHAR_UUID) != null
            if (status != BluetoothGatt.GATT_SUCCESS || !found) {
                gatt.disconnect()
                return
            }
            if (!gatt.requestMtu(BleConstants.REQUESTED_MTU)) gatt.disconnect()
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            val characteristic = gatt.getService(BleConstants.SERVICE_UUID)
                ?.getCharacteristic(BleConstants.MESSAGE_CHAR_UUID)
            if (characteristic == null || mtu < BleConstants.MIN_MTU) {
                Log.w(TAG, "MTU insuffisant (${mtu}) pour ${gatt.device.address}")
                gatt.disconnect()
                return
            }
            links.getOrPut(gatt.device.address) { PeerLink(gatt) }.markReady(mtu, characteristic)
            updatePeers()
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            status: Int,
        ) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                Log.w(TAG, "Écriture échouée vers ${gatt.device.address} (status $status)")
            }
            links[gatt.device.address]?.onWriteDone()
        }
    }

    private fun updatePeers() {
        _peers.value = links.values.filter { it.ready }.map { it.gatt.device.address }.toSet()
    }

    /**
     * Une connexion sortante vers un appareil. Garde une file de fragments à envoyer
     * et n'écrit qu'un fragment à la fois (Android n'accepte qu'une opération GATT en cours).
     */
    private inner class PeerLink(val gatt: BluetoothGatt) {
        var ready = false
            private set
        private var characteristic: BluetoothGattCharacteristic? = null
        private var chunkSize = 0
        private var busy = false
        private val waiting = ArrayDeque<Pair<UUID, ByteArray>>()
        private val frames = ArrayDeque<ByteArray>()

        @Synchronized
        fun enqueue(messageId: UUID, payload: ByteArray) {
            if (ready) splitInto(messageId, payload) else waiting.addLast(messageId to payload)
            pump()
        }

        @Synchronized
        fun markReady(mtu: Int, characteristic: BluetoothGattCharacteristic) {
            this.characteristic = characteristic
            chunkSize = mtu - BleConstants.ATT_OVERHEAD
            ready = true
            while (waiting.isNotEmpty()) {
                val (id, payload) = waiting.removeFirst()
                splitInto(id, payload)
            }
            pump()
        }

        @Synchronized
        fun onWriteDone() {
            busy = false
            pump()
        }

        private fun splitInto(messageId: UUID, payload: ByteArray) {
            frames.addAll(Fragmenter.split(messageId, payload, chunkSize))
        }

        private fun pump() {
            val char = characteristic ?: return
            if (!ready || busy || frames.isEmpty()) return
            val frame = frames.removeFirst()
            char.value = frame
            char.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
            busy = gatt.writeCharacteristic(char)
            if (!busy) frames.addFirst(frame) // réessayé au prochain événement
        }
    }
}
