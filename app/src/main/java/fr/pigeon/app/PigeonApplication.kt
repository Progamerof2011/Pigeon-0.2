package fr.pigeon.app

import android.app.Application
import android.os.Build
import fr.pigeon.app.ble.BleTransport
import fr.pigeon.app.core.AppServices
import fr.pigeon.app.data.MessageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.CoroutineScope

/**
 * Point d'entrée de l'application. Les services sont créés ici (et non dans l'Activity)
 * pour survivre à la rotation de l'écran.
 */
class PigeonApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    val services: AppServices by lazy {
        val ble = BleTransport(this)
        AppServices(
            ble = ble,
            repository = MessageRepository(ble, appScope, localName = Build.MODEL),
        )
    }
}
