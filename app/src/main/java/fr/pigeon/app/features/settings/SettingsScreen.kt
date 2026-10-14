package fr.pigeon.app.features.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.pigeon.app.core.AppServices
import fr.pigeon.app.ui.components.InfoCard

/** Réglages : état du Bluetooth, appareils à portée, redémarrage de la connexion. */
@Composable
fun SettingsScreen(services: AppServices) {
    val peers by services.ble.peers.collectAsState()
    val status by services.ble.status.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Réglages", style = MaterialTheme.typography.headlineMedium)

        InfoCard(title = "Pseudo", value = services.repository.localName)
        InfoCard(title = "Bluetooth", value = status)
        InfoCard(title = "Appareils à portée", value = peers.size.toString())

        Button(
            onClick = {
                services.ble.stop()
                services.ble.start()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Redémarrer la connexion")
        }

        Text(
            "Pigeon 0.1 · bêta",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
