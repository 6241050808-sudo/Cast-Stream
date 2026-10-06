package com.example.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.BookmarkEntity
import com.example.data.CarStreamRepository
import com.example.data.CarStreamSettingsEntity
import com.example.data.HistoryEntity
import com.example.service.CarStreamPlaybackBridge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AppDestination {
    STREAM_DECK,
    BOOKMARKS_HISTORY,
    ENGINE_FIXES
}

enum class ActiveSourceType {
    WEB_STREAM,
    LOCAL_VIDEO
}

data class PlayerUiState(
    val currentDestination: AppDestination = AppDestination.STREAM_DECK,
    val activeSourceType: ActiveSourceType = ActiveSourceType.WEB_STREAM,
    val currentUrl: String = "https://m.youtube.com",
    val currentPageTitle: String = "YouTube CarStream Ready",
    val localVideoUri: Uri? = null,
    val localVideoName: String = "",
    val isPlaying: Boolean = true,
    val isImmersiveFullscreen: Boolean = false,
    val isLoadingWeb: Boolean = false,
    val webProgress: Int = 0,
    val showRotaryKeyboard: Boolean = false,
    val showAddBookmarkDialog: Boolean = false,
    val showAspectSheet: Boolean = false,
    val showAudioSyncSheet: Boolean = false,
    val searchQueryInput: String = "",
    val canGoBackInWeb: Boolean = false,
    val lastJsCommand: String = "",
    val commandNonce: Long = 0L,
    val statusBannerMessage: String? = null
)

class CarStreamViewModel(
    private val repository: CarStreamRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    val bookmarks: StateFlow<List<BookmarkEntity>> = repository.bookmarks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val history: StateFlow<List<HistoryEntity>> = repository.history
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<CarStreamSettingsEntity> = repository.settings
        .map { it ?: CarStreamSettingsEntity() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CarStreamSettingsEntity())

    init {
        viewModelScope.launch {
            repository.ensureDefaultData()
        }
        CarStreamPlaybackBridge.onTransportCommand = { cmd ->
            when {
                cmd == "PLAY" -> setPlaying(true)
                cmd == "PAUSE" -> setPlaying(false)
                cmd == "NEXT" -> seekBySeconds(15)
                cmd == "PREV" -> seekBySeconds(-15)
            }
        }
    }

    fun selectDestination(destination: AppDestination) {
        _uiState.update {
            it.copy(
                currentDestination = destination,
                isImmersiveFullscreen = if (destination != AppDestination.STREAM_DECK) false else it.isImmersiveFullscreen
            )
        }
    }

    fun setImmersiveFullscreen(fullscreen: Boolean) {
        _uiState.update { it.copy(isImmersiveFullscreen = fullscreen) }
        if (fullscreen) {
            // Also expand HTML5 video inside YouTube/WebView to fill the viewport cleanly
            val js = """
                javascript:(function(){
                    var v = document.querySelector('video');
                    if (v) {
                        v.style.objectFit = 'contain';
                        v.style.width = '100%';
                        v.style.height = '100%';
                    }
                })();
            """.trimIndent()
            dispatchJsCommand(js)
        }
    }

    fun toggleImmersiveFullscreen() {
        setImmersiveFullscreen(!_uiState.value.isImmersiveFullscreen)
    }

    fun updateSearchInput(text: String) {
        _uiState.update { it.copy(searchQueryInput = text) }
    }

    fun appendCharFromRotaryKeyboard(char: String) {
        _uiState.update { state ->
            state.copy(searchQueryInput = state.searchQueryInput + char)
        }
    }

    fun backspaceFromRotaryKeyboard() {
        _uiState.update { state ->
            if (state.searchQueryInput.isNotEmpty()) {
                state.copy(searchQueryInput = state.searchQueryInput.dropLast(1))
            } else {
                state
            }
        }
    }

    fun clearRotaryKeyboardInput() {
        _uiState.update { it.copy(searchQueryInput = "") }
    }

    fun toggleRotaryKeyboard(show: Boolean) {
        _uiState.update { it.copy(showRotaryKeyboard = show) }
    }

    fun submitSearchOrUrl(rawInput: String = _uiState.value.searchQueryInput) {
        val targetUrl = CarStreamRepository.normalizeUrlOrSearch(rawInput)
        loadWebStream(
            url = targetUrl,
            title = if (rawInput.isBlank()) "YouTube Home" else rawInput.trim()
        )
        _uiState.update { it.copy(showRotaryKeyboard = false) }
    }

    fun loadWebStream(url: String, title: String = url) {
        _uiState.update {
            it.copy(
                activeSourceType = ActiveSourceType.WEB_STREAM,
                currentUrl = url,
                currentPageTitle = title,
                searchQueryInput = url,
                isPlaying = true,
                statusBannerMessage = "กำลังเชื่อมต่อสตรีม: $title"
            )
        }
        viewModelScope.launch {
            repository.recordHistory(title = title, url = url, sourceType = "WEB_STREAM")
        }
    }

    fun loadLocalVideo(uri: Uri, displayName: String = "ไฟล์วิดีโอในเครื่อง (MP4/MKV)") {
        _uiState.update {
            it.copy(
                activeSourceType = ActiveSourceType.LOCAL_VIDEO,
                localVideoUri = uri,
                localVideoName = displayName,
                currentPageTitle = displayName,
                isPlaying = true,
                statusBannerMessage = "กำลังเล่นวิดีโอจากเครื่อง: $displayName"
            )
        }
        viewModelScope.launch {
            repository.recordHistory(
                title = displayName,
                url = uri.toString(),
                sourceType = "LOCAL_VIDEO"
            )
        }
    }

    fun onWebPageStarted(url: String) {
        _uiState.update {
            it.copy(
                isLoadingWeb = true,
                currentUrl = url,
                searchQueryInput = url
            )
        }
    }

    fun onWebPageFinished(url: String, title: String?, canGoBack: Boolean) {
        val cleanTitle = title?.takeIf { it.isNotBlank() } ?: url
        _uiState.update {
            it.copy(
                isLoadingWeb = false,
                currentUrl = url,
                currentPageTitle = cleanTitle,
                canGoBackInWeb = canGoBack
            )
        }
    }

    fun onWebProgressChanged(progress: Int) {
        _uiState.update { it.copy(webProgress = progress) }
    }

    fun setPlaying(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
        val js = if (playing) {
            "javascript:(function(){var v=document.querySelector('video');if(v){v.play();}})();"
        } else {
            "javascript:(function(){var v=document.querySelector('video');if(v){v.pause();}})();"
        }
        dispatchJsCommand(js)
    }

    fun togglePlayPause() {
        setPlaying(!_uiState.value.isPlaying)
    }

    fun seekBySeconds(deltaSeconds: Int) {
        val js = "javascript:(function(){var v=document.querySelector('video');if(v){v.currentTime=Math.max(0, v.currentTime + ($deltaSeconds));}})();"
        dispatchJsCommand(js)
        _uiState.update {
            it.copy(
                statusBannerMessage = if (deltaSeconds > 0) "ข้ามไปข้างหน้า +${deltaSeconds}s" else "ย้อนกลับ ${deltaSeconds}s"
            )
        }
    }

    fun injectRotaryInputToFocusedWebField(textToInsert: String) {
        val escaped = textToInsert.replace("\\", "\\\\").replace("'", "\\'")
        val js = """
            javascript:(function(){
                var el = document.activeElement;
                if (el && (el.tagName === 'INPUT' || el.tagName === 'TEXTAREA')) {
                    var start = el.selectionStart || el.value.length;
                    var end = el.selectionEnd || el.value.length;
                    el.value = el.value.substring(0, start) + '$escaped' + el.value.substring(end);
                    el.dispatchEvent(new Event('input', { bubbles: true }));
                    el.dispatchEvent(new Event('change', { bubbles: true }));
                }
            })();
        """.trimIndent()
        dispatchJsCommand(js)
        _uiState.update {
            it.copy(
                showRotaryKeyboard = false,
                statusBannerMessage = "ส่งข้อความ '$textToInsert' เข้าช่องกรอกในเว็บโดยไม่ล้างค่าเดิมแล้ว"
            )
        }
    }

    private fun dispatchJsCommand(js: String) {
        _uiState.update {
            it.copy(
                lastJsCommand = js,
                commandNonce = System.currentTimeMillis()
            )
        }
    }

    fun updateAspectRatio(mode: String, customZoom: Int = settings.value.customZoomPercent) {
        viewModelScope.launch {
            val updated = settings.value.copy(
                aspectRatioMode = mode,
                customZoomPercent = customZoom
            )
            repository.updateSettings(updated)
            applyAspectRatioCssToWeb(mode, customZoom)
        }
    }

    fun applyAspectRatioCssToWeb(mode: String, customZoom: Int) {
        val objectFitAndTransform = when (mode) {
            "ULTRAWIDE_21_9" -> "v.style.objectFit='cover';v.style.transform='scale(1.14, 1.02)';"
            "STRETCH_FULL" -> "v.style.objectFit='fill';v.style.transform='scale(1.0)';"
            "ZOOM_115" -> "v.style.objectFit='cover';v.style.transform='scale(1.15)';"
            "ZOOM_130" -> "v.style.objectFit='cover';v.style.transform='scale(1.30)';"
            else -> {
                val scale = (customZoom.coerceIn(85, 145)) / 100.0
                "v.style.objectFit='contain';v.style.transform='scale($scale)';"
            }
        }
        val js = "javascript:(function(){var v=document.querySelector('video');if(v){$objectFitAndTransform}})();"
        dispatchJsCommand(js)
    }

    fun updateAudioOffsetMs(offsetMs: Int) {
        viewModelScope.launch {
            val clamped = offsetMs.coerceIn(-500, 500)
            repository.updateSettings(settings.value.copy(audioOffsetMs = clamped))
            _uiState.update {
                it.copy(statusBannerMessage = "ตั้งค่าชดเชย Audio/Video Sync: ${if (clamped >= 0) "+$clamped" else "$clamped"} ms")
            }
        }
    }

    fun updateUserAgentMode(mode: String) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(userAgentMode = mode))
            _uiState.update {
                it.copy(statusBannerMessage = "สลับโหมด User-Agent เป็น $mode เรียบร้อยแล้ว")
            }
        }
    }

    fun toggleAdShield(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(adShieldEnabled = enabled))
        }
    }

    fun toggleSponsorSkipHint(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(sponsorSkipHintEnabled = enabled))
        }
    }

    fun toggleBackgroundKeepAlive(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(backgroundAudioKeepAlive = enabled))
        }
    }

    fun toggleRotaryDpadHighlight(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSettings(settings.value.copy(rotaryDpadHighlightEnabled = enabled))
        }
    }

    fun addBookmark(title: String, url: String, category: String, accentHex: String = "#00E5FF") {
        viewModelScope.launch {
            repository.addBookmark(title, url, category, accentHex)
            _uiState.update {
                it.copy(
                    showAddBookmarkDialog = false,
                    statusBannerMessage = "บันทึกบุ๊กมาร์ก '$title' ลงคลังแล้ว"
                )
            }
        }
    }

    fun bookmarkCurrentStream() {
        val state = _uiState.value
        addBookmark(
            title = state.currentPageTitle.take(40),
            url = state.currentUrl,
            category = if (state.currentUrl.contains("youtube")) "YOUTUBE" else "STREAM",
            accentHex = "#FF2A54"
        )
    }

    fun deleteBookmark(id: Int) {
        viewModelScope.launch {
            repository.deleteBookmark(id)
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _uiState.update { it.copy(statusBannerMessage = "ล้างประวัติการรับชมเรียบร้อยแล้ว") }
        }
    }

    fun toggleAddBookmarkDialog(show: Boolean) {
        _uiState.update { it.copy(showAddBookmarkDialog = show) }
    }

    fun toggleAspectSheet(show: Boolean) {
        _uiState.update { it.copy(showAspectSheet = show) }
    }

    fun toggleAudioSyncSheet(show: Boolean) {
        _uiState.update { it.copy(showAudioSyncSheet = show) }
    }

    fun dismissBanner() {
        _uiState.update { it.copy(statusBannerMessage = null) }
    }

    class Factory(private val repository: CarStreamRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CarStreamViewModel(repository) as T
        }
    }
}
