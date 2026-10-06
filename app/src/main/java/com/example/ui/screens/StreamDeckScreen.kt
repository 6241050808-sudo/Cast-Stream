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

/**
 * Full-Screen YouTube & Video Player screen without any top Omnibox card blocking the view.
 * Gives 100% of the screen area directly to YouTube / video playback.
 */
@Composable
fun StreamDeckScreen(
    uiState: PlayerUiState,
    settings: CarStreamSettingsEntity,
    bookmarks: List<BookmarkEntity>,
    onSearchQueryChange: (String) -> Unit,
    onSubmitSearchOrUrl: (String) -> Unit,
    onSelectBookmark: (BookmarkEntity) -> Unit,
    onOpenAddBookmark: () -> Unit,
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
    onOpenCarHudScreen: () -> Unit,
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
            onOpenAspectSheet = onOpenAspectSheet,
            onOpenAudioSyncSheet = onOpenAudioSyncSheet,
            onOpenRotaryKeyboard = onOpenRotaryKeyboard,
            onBookmarkCurrent = onBookmarkCurrent,
            onPickLocalVideo = onPickLocalVideo,
            modifier = Modifier.fillMaxSize()
        )
    }
}
