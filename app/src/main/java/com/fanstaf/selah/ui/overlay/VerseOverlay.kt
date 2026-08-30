package com.fanstaf.selah.ui.overlay

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.ui.theme.verseTextStyle
import kotlinx.coroutines.delay

/**
 * The card shown over whatever is on screen after unlock. Deliberately small (wrap-content window),
 * so touches outside it fall through to the phone — it never blocks access.
 */
@Composable
fun VerseOverlay(
    reference: String,
    text: String,
    translation: String,
    mode: DisplayMode,
    fontScale: Float,
    revealDelayMs: Long,
    onRevealed: () -> Unit,
    onClose: () -> Unit,
) {
    var revealed by remember { mutableStateOf(mode == DisplayMode.READ) }

    if (mode == DisplayMode.RECALL) {
        LaunchedEffect(Unit) {
            delay(revealDelayMs)
            if (!revealed) {
                revealed = true
                onRevealed()
            }
        }
    }

    Card(
        modifier = Modifier
            .widthIn(max = 380.dp)
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
    ) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = reference.uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                )

                if (revealed) {
                    Text(
                        text = text,
                        style = verseTextStyle(MaterialTheme.typography.headlineMedium, fontScale),
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        text = translation,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Can you say it?",
                        style = verseTextStyle(MaterialTheme.typography.headlineMedium, fontScale),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    TextButton(onClick = { revealed = true; onRevealed() }) {
                        Text("Reveal")
                    }
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = "Dismiss",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
