package fr.pigeon.app.features

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import fr.pigeon.app.core.AppServices

/**
 * Une fonction de l'app, affichée comme un onglet de la barre du bas.
 *
 * Pour ajouter une fonction : créer son écran, puis l'ajouter dans [FeatureRegistry].
 * Mettre [enabled] à false pour la masquer sans la supprimer.
 */
data class Feature(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val content: @Composable (AppServices) -> Unit,
)
