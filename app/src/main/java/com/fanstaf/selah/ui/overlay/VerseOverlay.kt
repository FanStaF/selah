package com.fanstaf.selah.ui.overlay

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fanstaf.selah.data.DisplayMode
import com.fanstaf.selah.data.DisplayStyle
import com.fanstaf.selah.ui.theme.verseTextStyle
import kotlinx.coroutines.delay

/**
 * The verse shown after unlock. Two styles:
 *  - CARD: a small floating card; the window is non-blocking (taps outside fall through).
 *  - FULLSCREEN: an opaque panel over the whole app area (tap anywhere to reveal/dismiss).
 *
 * A quiet "Selah" button holds the moment open — cancelling the auto-dismiss timer so the verse
 * stays until the user dismisses it, for those who want to pause and reflect longer.
 */
@Composable
fun VerseOverlay(
    reference: String,
    text: String,
    translation: String,
    mode: DisplayMode,
    style: DisplayStyle,
    fontScale: Float,
    revealDelayMs: Long,
    onRevealed: () -> Unit,
    onHold: () -> Unit,
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

    fun reveal() {
        revealed = true
        onRevealed()
    }

    when (style) {
        DisplayStyle.FULLSCREEN -> FullScreenOverlay(
            reference = reference,
            text = text,
            translation = translation,
            revealed = revealed,
            fontScale = fontScale,
            onTap = { if (revealed) onClose() else reveal() },
            onHold = onHold,
        )
        DisplayStyle.CARD -> CardOverlay(
            reference = reference,
            text = text,
            translation = translation,
            revealed = revealed,
            fontScale = fontScale,
            onReveal = { reveal() },
            onHold = onHold,
            onClose = onClose,
        )
    }
}

/** Quiet, low-emphasis control to hold the verse open past the timer. */
@Composable
private fun SelahHold(onHold: () -> Unit) {
    var held by remember { mutableStateOf(false) }
    if (held) {
        Text(
            "Reflecting — tap to close",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    } else {
        TextButton(onClick = { held = true; onHold() }) {
            Icon(
                Icons.Filled.Pause,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
            )
            Text(
                "  Selah",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
private fun FullScreenOverlay(
    reference: String,
    text: String,
    translation: String,
    revealed: Boolean,
    fontScale: Float,
    onTap: () -> Unit,
    onHold: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(scheme.background, scheme.surface, scheme.background),
                ),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onTap,
            )
            .padding(horizontal = 32.dp, vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Text(
                text = reference.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.secondary,
                textAlign = TextAlign.Center,
            )
            if (revealed) {
                Text(
                    text = text,
                    style = verseTextStyle(MaterialTheme.typography.headlineMedium, fontScale),
                    color = scheme.onBackground,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = translation,
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Can you say it?",
                    style = verseTextStyle(MaterialTheme.typography.headlineMedium, fontScale),
                    color = scheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "tap to reveal",
                    style = MaterialTheme.typography.labelMedium,
                    color = scheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            SelahHold(onHold = onHold)
        }
    }
}

@Composable
private fun CardOverlay(
    reference: String,
    text: String,
    translation: String,
    revealed: Boolean,
    fontScale: Float,
    onReveal: () -> Unit,
    onHold: () -> Unit,
    onClose: () -> Unit,
) {
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
                    .padding(start = 24.dp, end = 24.dp, top = 22.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
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
                    TextButton(onClick = onReveal) { Text("Reveal") }
                }
                SelahHold(onHold = onHold)
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
