package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.example.data.BookmarkEntity
import com.example.data.CarStreamSettingsEntity
import com.example.ui.components.CarStreamMediaViewport
import com.example.viewmodel.PlayerUiState

@Composable
fun StreamDeckScreen(
    uiState: PlayerUiState,
    settings: CarStreamSettingsEntity,
    bookmarks: List<BookmarkEntity>,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String, String?, Boolean) -> Unit,
    onProgressChanged: (Int) -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekDelta: (Int) -> Unit,
    onToggleFullscreen: () -> Unit,
    onOpenAspectSheet: () -> Unit,
    onOpenAudioSyncSheet: () -> Unit,
    onOpenRotaryKeyboard: () -> Unit,
    onBookmarkCurrent: () -> Unit,
    onPickLocalVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("stream_deck_screen")
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
            onToggleFullscreen = onToggleFullscreen,
            onOpenAspectSheet = onOpenAspectSheet,
            onOpenAudioSyncSheet = onOpenAudioSyncSheet,
            onOpenRotaryKeyboard = onOpenRotaryKeyboard,
            onBookmarkCurrent = onBookmarkCurrent,
            onPickLocalVideo = onPickLocalVideo,
            modifier = Modifier.fillMaxSize()
        )
    }
}
