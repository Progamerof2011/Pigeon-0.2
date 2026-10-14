package fr.pigeon.app.features

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Settings
import fr.pigeon.app.features.chat.ChatScreen
import fr.pigeon.app.features.settings.SettingsScreen

/**
 * Liste de toutes les fonctions de l'app. L'ordre ici est l'ordre dans la barre du bas.
 */
object FeatureRegistry {

    val all: List<Feature> = listOf(
        Feature(id = "chat", title = "Messages", icon = Icons.Filled.Email) { services ->
            ChatScreen(services)
        },
        Feature(id = "settings", title = "Réglages", icon = Icons.Filled.Settings) { services ->
            SettingsScreen(services)
        },
        // Prochaine fonction (NFC) : créer features/nfc/NfcScreen.kt puis décommenter :
        // Feature(id = "nfc", title = "NFC", icon = Icons.Filled.Share, enabled = false) { services ->
        //     NfcScreen(services)
        // },
    )

    val enabled: List<Feature>
        get() = all.filter { it.enabled }
}
