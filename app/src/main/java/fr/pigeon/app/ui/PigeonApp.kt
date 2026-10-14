package fr.pigeon.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import fr.pigeon.app.core.AppServices
import fr.pigeon.app.features.FeatureRegistry

/** Coquille de l'app : barre du bas construite à partir de [FeatureRegistry]. */
@Composable
fun PigeonApp(services: AppServices) {
    val features = FeatureRegistry.enabled
    var selectedId by rememberSaveable { mutableStateOf(features.first().id) }
    val current = features.firstOrNull { it.id == selectedId } ?: features.first()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                features.forEach { feature ->
                    NavigationBarItem(
                        selected = feature.id == current.id,
                        onClick = { selectedId = feature.id },
                        icon = { Icon(feature.icon, contentDescription = feature.title) },
                        label = { Text(feature.title) },
                    )
                }
            }
        },
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            current.content(services)
        }
    }
}
