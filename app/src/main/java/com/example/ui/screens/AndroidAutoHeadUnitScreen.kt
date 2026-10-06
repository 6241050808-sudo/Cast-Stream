package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BookmarkEntity
import com.example.data.CarStreamSettingsEntity
import com.example.ui.components.CarStreamMediaViewport
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CockpitElevated
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.PlayerUiState
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen Android Auto Head-Unit Display Window.
 * Defaults to 100% Full-Screen YouTube in the car head unit, with an optional
 * "Split HUD" button to show the side channel dock when desired.
 */
@Composable
fun AndroidAutoHeadUnitScreen(
    uiState: PlayerUiState,
    settings: CarStreamSettingsEntity,
    bookmarks: List<BookmarkEntity>,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?, Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekDelta: (Int) -> Unit,
    onOpenAspectSheet: () -> Unit,
    onOpenAudioSyncSheet: () -> Unit,
    onOpenRotaryKeyboard: () -> Unit,
    onBookmarkCurrent: () -> Unit,
    onPickLocalVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Default to full-screen YouTube in Android Auto as requested
    var isSplitCoolwalk by remember { mutableStateOf(false) }
    var rotaryFocusIndex by remember { mutableIntStateOf(0) }
    var currentTimeText by remember { mutableStateOf("10:35") }

    LaunchedEffect(Unit) {
        val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
        while (true) {
            currentTimeText = formatter.format(Date())
            delay(15_000L)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .testTag("android_auto_hud_screen")
    ) {
        // Compact Android Auto Status Rail at Top
        Surface(
            color = CarbonSurface,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "ANDROID AUTO • $currentTimeText",
                        style = MaterialTheme.typography.labelLarge,
                        color = ElectricCyan
                    )
                }

                // Rotary quick controls + Split-screen toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (settings.rotaryDpadHighlightEnabled && bookmarks.isNotEmpty()) {
                        Surface(
                            onClick = {
                                rotaryFocusIndex = (rotaryFocusIndex - 1 + bookmarks.size) % bookmarks.size
                                bookmarks.getOrNull(rotaryFocusIndex)?.let { onSelectBookmark(it) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = CockpitElevated,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_rotary_prev")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "ช่องก่อนหน้า")
                            }
                        }
                        Surface(
                            onClick = {
                                rotaryFocusIndex = (rotaryFocusIndex + 1) % bookmarks.size
                                bookmarks.getOrNull(rotaryFocusIndex)?.let { onSelectBookmark(it) }
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = CockpitElevated,
                            modifier = Modifier
                                .size(34.dp)
                                .testTag("btn_rotary_next")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "ช่องถัดไป")
                            }
                        }
                    }

                    Surface(
                        onClick = { isSplitCoolwalk = !isSplitCoolwalk },
                        shape = RoundedCornerShape(10.dp),
                        color = CockpitElevated,
                        modifier = Modifier.testTag("btn_toggle_coolwalk_split")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isSplitCoolwalk) Icons.Default.Fullscreen else Icons.Default.FullscreenExit,
                                contentDescription = null,
                                tint = ElectricCyan,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSplitCoolwalk) "เต็มจอ" else "แสดงแถบข้าง",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Main Head-Unit Canvas (Full-screen YouTube by default, or Split Coolwalk when toggled)
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (isSplitCoolwalk) {
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(
                        modifier = Modifier
                            .weight(0.72f)
                            .fillMaxHeight()
                    ) {
                        CarStreamMediaViewport(
                            uiState = uiState,
                            settings = settings,
                            bookmarks = bookmarks,
                            onSelectBookmark = onSelectBookmark,
                            onPageStarted = onPageStarted,
                            onPageFinished = onPageFinished,
                            onProgressChanged = onProgressChanged,
                            onTogglePlayPause = onTogglePlayPause,
                            onSeekDelta = onSeekDelta,
                            onOpenAspectSheet = onOpenAspectSheet,
                            onOpenAudioSyncSheet = onOpenAudioSyncSheet,
                            onOpenRotaryKeyboard = onOpenRotaryKeyboard,
                            onBookmarkCurrent = onBookmarkCurrent,
                            onPickLocalVideo = onPickLocalVideo,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    CoolwalkSideCompanionCard(
                        bookmarks = bookmarks,
                        rotaryFocusIndex = rotaryFocusIndex,
                        settings = settings,
                        onSelectBookmark = { idx, item ->
                            rotaryFocusIndex = idx
                            onSelectBookmark(item)
                        },
                        onOpenAspectSheet = onOpenAspectSheet,
                        onOpenAudioSyncSheet = onOpenAudioSyncSheet,
                        modifier = Modifier
                            .weight(0.28f)
                            .fillMaxHeight()
                    )
                }
            } else {
                CarStreamMediaViewport(
                    uiState = uiState,
                    settings = settings,
                    bookmarks = bookmarks,
                    onSelectBookmark = onSelectBookmark,
                    onPageStarted = onPageStarted,
                    onPageFinished = onPageFinished,
                    onProgressChanged = onProgressChanged,
                    onTogglePlayPause = onTogglePlayPause,
                    onSeekDelta = onSeekDelta,
                    onOpenAspectSheet = onOpenAspectSheet,
                    onOpenAudioSyncSheet = onOpenAudioSyncSheet,
                    onOpenRotaryKeyboard = onOpenRotaryKeyboard,
                    onBookmarkCurrent = onBookmarkCurrent,
                    onPickLocalVideo = onPickLocalVideo,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CoolwalkSideCompanionCard(
    bookmarks: List<BookmarkEntity>,
    rotaryFocusIndex: Int,
    settings: CarStreamSettingsEntity,
    onSelectBookmark: (Int, BookmarkEntity) -> Unit,
    onOpenAspectSheet: () -> Unit,
    onOpenAudioSyncSheet: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(CarbonSurface)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "ช่องสตรีมด่วน",
            style = MaterialTheme.typography.labelLarge,
            color = ElectricCyan
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            itemsIndexed(bookmarks, key = { _, b -> b.id }) { idx, item ->
                val isRotaryFocused = idx == rotaryFocusIndex
                Surface(
                    onClick = { onSelectBookmark(idx, item) },
                    shape = RoundedCornerShape(12.dp),
                    color = if (isRotaryFocused) ElectricCyan.copy(alpha = 0.2f) else CockpitCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isRotaryFocused) 1.5.dp else 1.dp,
                            color = if (isRotaryFocused) ElectricCyan else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricCyan
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = if (isRotaryFocused) ElectricCyan else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
