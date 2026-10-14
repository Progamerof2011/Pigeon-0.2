package fr.pigeon.app.features.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import fr.pigeon.app.core.AppServices
import fr.pigeon.app.ui.components.ChannelCard
import fr.pigeon.app.ui.components.MessageBubble
import fr.pigeon.app.ui.components.MessageInput

/** Écran du canal général : carte du canal, messages, champ de saisie. */
@Composable
fun ChatScreen(services: AppServices) {
    val repository = services.repository
    val messages by repository.messages.collectAsState()
    val peers by services.ble.peers.collectAsState()
    val status by services.ble.status.collectAsState()
    var draft by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(8.dp))
        ChannelCard(
            title = "Canal général",
            subtitle = "$status · ${peers.size} appareil(s) à portée",
        )
        Spacer(Modifier.height(12.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(message = message, mine = repository.isMine(message))
            }
        }

        MessageInput(
            value = draft,
            onValueChange = { draft = it },
            onSend = {
                repository.send(draft)
                draft = ""
            },
        )
    }
}
