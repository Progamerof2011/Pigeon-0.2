package fr.pigeon.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import fr.pigeon.app.core.AppServices
import fr.pigeon.app.ui.PigeonApp
import fr.pigeon.app.ui.theme.PigeonTheme

class MainActivity : ComponentActivity() {

    private lateinit var services: AppServices

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        services = (application as PigeonApplication).services

        setContent {
            PigeonTheme {
                // Demande les autorisations, puis démarre le Bluetooth une fois accordées.
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestMultiplePermissions()
                ) { results ->
                    if (results.values.all { it }) services.ble.start()
                }
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(requiredPermissions())
                }
                PigeonApp(services)
            }
        }
    }

    override fun onDestroy() {
        if (isFinishing) services.ble.stop()
        super.onDestroy()
    }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
}
