package com.example.ui.components

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.data.BookmarkEntity
import com.example.data.CarStreamSettingsEntity
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CrimsonStream
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.ActiveSourceType
import com.example.viewmodel.PlayerUiState

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun CarStreamMediaViewport(
    uiState: PlayerUiState,
    settings: CarStreamSettingsEntity,
    bookmarks: List<BookmarkEntity> = emptyList(),
    onSelectBookmark: (BookmarkEntity) -> Unit = {},
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
    // Proportional scaling that never clips the WebView container bounds
    val baseScale = (settings.customZoomPercent.coerceIn(85, 135)) / 100f
    val videoScaleX = when (settings.aspectRatioMode) {
        "ULTRAWIDE_21_9" -> 1.12f
        "STRETCH_FULL" -> 1.06f
        "ZOOM_115" -> 1.15f
        "ZOOM_130" -> 1.25f
        else -> baseScale
    }
    val videoScaleY = when (settings.aspectRatioMode) {
        "ULTRAWIDE_21_9" -> 1.02f
        "STRETCH_FULL" -> 1.06f
        "ZOOM_115" -> 1.15f
        "ZOOM_130" -> 1.25f
        else -> baseScale
    }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var customFullscreenView by remember { mutableStateOf<View?>(null) }
    var customViewCallback by remember { mutableStateOf<WebChromeClient.CustomViewCallback?>(null) }
    var lastLoadedUrl by remember { mutableStateOf("") }
    var webViewReloadInstanceKey by remember { mutableIntStateOf(0) }
    var showBottomDock by remember { mutableStateOf(true) }
    var showQuickChannelsStrip by remember { mutableStateOf(false) }

    // Auto-collapse dock when entering immersive fullscreen so YouTube fills 100% of phone & car screen
    LaunchedEffect(uiState.isImmersiveFullscreen) {
        showBottomDock = !uiState.isImmersiveFullscreen
    }

    // Execute queued JavaScript commands when commandNonce updates
    LaunchedEffect(uiState.commandNonce) {
        if (uiState.lastJsCommand.isNotBlank()) {
            runCatching {
                webViewRef?.evaluateJavascript(
                    uiState.lastJsCommand.removePrefix("javascript:"),
                    null
                )
            }
        }
    }

    // Apply User-Agent changes dynamically
    LaunchedEffect(settings.userAgentMode) {
        webViewRef?.let { wv ->
            val ua = resolveUserAgent(settings.userAgentMode)
            if (wv.settings.userAgentString != ua) {
                wv.settings.userAgentString = ua
                if (uiState.currentUrl.isNotBlank()) {
                    wv.loadUrl(uiState.currentUrl)
                    lastLoadedUrl = uiState.currentUrl
                }
            }
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val isWideCarScreen = maxWidth >= 600.dp

        // 1. PROPORTIONAL YOUTUBE / VIDEO SURFACE (Fits both Mobile Portrait/Landscape & Car Widescreen)
        if (uiState.activeSourceType == ActiveSourceType.LOCAL_VIDEO && uiState.localVideoUri != null) {
            LocalVideoSurface(
                uri = uiState.localVideoUri,
                isPlaying = uiState.isPlaying,
                scaleX = videoScaleX,
                scaleY = videoScaleY
            )
        } else {
            key(webViewReloadInstanceKey) {
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("carstream_webview"),
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = FrameLayout.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            // Prevent Chromium GPU process crash (code -1) inside Compose container
                            setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                            runCatching {
                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                            }

                            this.settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                databaseEnabled = true
                                mediaPlaybackRequiresUserGesture = false
                                mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                                loadWithOverviewMode = true
                                useWideViewPort = true
                                builtInZoomControls = false
                                displayZoomControls = false
                                textZoom = 100
                                cacheMode = WebSettings.LOAD_DEFAULT
                                userAgentString = resolveUserAgent(settings.userAgentMode)
                            }

                            webChromeClient = object : WebChromeClient() {
                                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                    onProgressChanged(newProgress)
                                }

                                // Support native HTML5 YouTube fullscreen button on both phone and car displays
                                override fun onShowCustomView(view: View?, callback: CustomViewCallback?) {
                                    customFullscreenView = view
                                    customViewCallback = callback
                                    if (!uiState.isImmersiveFullscreen) {
                                        onToggleFullscreen()
                                    }
                                }

                                override fun onHideCustomView() {
                                    customFullscreenView = null
                                    customViewCallback?.onCustomViewHidden()
                                    customViewCallback = null
                                    if (uiState.isImmersiveFullscreen) {
                                        onToggleFullscreen()
                                    }
                                }
                            }

                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    if (url != null) {
                                        onPageStarted(url)
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    if (url != null) {
                                        onPageFinished(url, view?.title, view?.canGoBack() == true)
                                    }
                                    if (settings.adShieldEnabled) {
                                        view?.evaluateJavascript(AD_SHIELD_AND_ENHANCE_SCRIPT, null)
                                    }
                                    // Ensure video fits container proportionally
                                    view?.evaluateJavascript(
                                        buildVideoFitScript(settings.aspectRatioMode, settings.customZoomPercent),
                                        null
                                    )
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val reqUrl = request?.url?.toString() ?: return false
                                    return !(reqUrl.startsWith("http://") || reqUrl.startsWith("https://"))
                                }

                                // Prevent app termination if Android WebView renderer restarts
                                override fun onRenderProcessGone(
                                    view: WebView?,
                                    detail: RenderProcessGoneDetail?
                                ): Boolean {
                                    runCatching {
                                        (view?.parent as? ViewGroup)?.removeView(view)
                                        view?.destroy()
                                    }
                                    webViewRef = null
                                    webViewReloadInstanceKey++
                                    return true
                                }
                            }

                            loadUrl(uiState.currentUrl)
                            lastLoadedUrl = uiState.currentUrl
                            webViewRef = this
                        }
                    },
                    update = { webView ->
                        webViewRef = webView
                        if (lastLoadedUrl != uiState.currentUrl && uiState.currentUrl.isNotBlank()) {
                            lastLoadedUrl = uiState.currentUrl
                            webView.loadUrl(uiState.currentUrl)
                        }
                    }
                )
            }

            // If HTML5 video triggered native custom fullscreen view, render it on top
            customFullscreenView?.let { fsView ->
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { fsView }
                )
            }
        }

        // 2. Thin Loading Indicator at very top edge
        AnimatedVisibility(
            visible = uiState.isLoadingWeb && uiState.activeSourceType == ActiveSourceType.WEB_STREAM,
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            LinearProgressIndicator(
                progress = { (uiState.webProgress / 100f).coerceIn(0.05f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = CrimsonStream,
                trackColor = Color.Transparent
            )
        }

        // 3. Dedicated Top-Right Quick Fullscreen Button (Always accessible on both Mobile & Car screen)
        Surface(
            onClick = {
                if (customFullscreenView != null) {
                    customFullscreenView = null
                    customViewCallback?.onCustomViewHidden()
                    customViewCallback = null
                }
                onToggleFullscreen()
            },
            shape = RoundedCornerShape(12.dp),
            color = ObsidianBlack.copy(alpha = 0.78f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 10.dp)
                .border(
                    width = 1.dp,
                    color = if (uiState.isImmersiveFullscreen) CrimsonStream else ElectricCyan.copy(alpha = 0.65f),
                    shape = RoundedCornerShape(12.dp)
                )
                .testTag("btn_toggle_fullscreen_top")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = if (uiState.isImmersiveFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                    contentDescription = if (uiState.isImmersiveFullscreen) "ออกจากโหมดเต็มจอ" else "เข้าสู่โหมดเต็มจอ",
                    tint = if (uiState.isImmersiveFullscreen) CrimsonStream else ElectricCyan,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (uiState.isImmersiveFullscreen) {
                        "ออกเต็มจอ"
                    } else if (isWideCarScreen) {
                        "เต็มจอรถ"
                    } else {
                        "เต็มจอ"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 4. Collapsible Floating CarStream Control Dock at Bottom
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.End
        ) {
            // Quick Channel Switcher Strip (Optional toggle so it never blocks YouTube unless opened)
            AnimatedVisibility(
                visible = showBottomDock && showQuickChannelsStrip && bookmarks.isNotEmpty(),
                enter = fadeIn() + slideInVertically { it / 2 },
                exit = fadeOut() + slideOutVertically { it / 2 }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ObsidianBlack.copy(alpha = 0.92f))
                        .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    bookmarks.forEach { bm ->
                        val active = uiState.currentUrl == bm.url
                        Surface(
                            onClick = {
                                onSelectBookmark(bm)
                                showQuickChannelsStrip = false
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) CrimsonStream else CockpitCard
                        ) {
                            Text(
                                text = bm.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Main Floating Control Bar (Proportional & Scrollable for both Mobile and Car displays)
            AnimatedVisibility(
                visible = showBottomDock,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it }
            ) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = ObsidianBlack.copy(alpha = 0.90f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f), RoundedCornerShape(18.dp))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Browser & YouTube Navigation Controls
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DockIconButton(
                                icon = Icons.Default.Home,
                                contentDesc = "หน้าแรก YouTube",
                                testTag = "btn_youtube_home",
                                tint = CrimsonStream,
                                onClick = {
                                    lastLoadedUrl = "https://m.youtube.com"
                                    webViewRef?.loadUrl("https://m.youtube.com")
                                }
                            )
                            DockIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDesc = "ย้อนกลับ",
                                testTag = "btn_web_back",
                                onClick = {
                                    if (webViewRef?.canGoBack() == true) {
                                        webViewRef?.goBack()
                                    }
                                }
                            )
                            DockIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDesc = "ไปข้างหน้า",
                                testTag = "btn_web_forward",
                                onClick = {
                                    if (webViewRef?.canGoForward() == true) {
                                        webViewRef?.goForward()
                                    }
                                }
                            )
                            DockIconButton(
                                icon = Icons.Default.Refresh,
                                contentDesc = "รีเฟรชหน้าเว็บ",
                                testTag = "btn_web_reload",
                                onClick = { webViewRef?.reload() }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Video Transport Controls (-10s, Play/Pause, +10s)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DockIconButton(
                                icon = Icons.Default.Replay10,
                                contentDesc = "ย้อนกลับ 10 วินาที",
                                testTag = "btn_rewind_10",
                                onClick = { onSeekDelta(-10) }
                            )
                            Surface(
                                onClick = onTogglePlayPause,
                                shape = CircleShape,
                                color = CrimsonStream,
                                modifier = Modifier
                                    .size(44.dp)
                                    .testTag("btn_play_pause")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (uiState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (uiState.isPlaying) "หยุดชั่วคราว" else "เล่นต่อ",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            DockIconButton(
                                icon = Icons.Default.Forward10,
                                contentDesc = "ข้ามไปข้างหน้า 10 วินาที",
                                testTag = "btn_forward_10",
                                onClick = { onSeekDelta(10) }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // CarStream Enhancement Tools (Fullscreen, Keyboard, Aspect Ratio, Audio Sync, Local Video, Channels, Hide Dock)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DockIconButton(
                                icon = if (uiState.isImmersiveFullscreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                contentDesc = if (uiState.isImmersiveFullscreen) "ออกจากโหมดเต็มจอ" else "เข้าสู่โหมดเต็มจอ",
                                testTag = "btn_toggle_fullscreen_dock",
                                tint = ElectricCyan,
                                onClick = onToggleFullscreen
                            )
                            DockIconButton(
                                icon = Icons.Default.Keyboard,
                                contentDesc = "ค้นหา YouTube / แป้นพิมพ์ในรถ",
                                testTag = "btn_rotary_keyboard",
                                tint = Color(0xFFFBBF24),
                                onClick = onOpenRotaryKeyboard
                            )
                            DockIconButton(
                                icon = Icons.Default.AspectRatio,
                                contentDesc = "ปรับสัดส่วนภาพและซูม",
                                testTag = "btn_aspect_ratio",
                                tint = ElectricCyan,
                                onClick = onOpenAspectSheet
                            )
                            DockIconButton(
                                icon = Icons.Default.GraphicEq,
                                contentDesc = "ปรับซิงค์เสียง",
                                testTag = "btn_audio_sync",
                                tint = EmeraldTelemetry,
                                onClick = onOpenAudioSyncSheet
                            )
                            DockIconButton(
                                icon = Icons.Default.FolderOpen,
                                contentDesc = "เปิดไฟล์วิดีโอในเครื่อง",
                                testTag = "btn_local_video",
                                onClick = onPickLocalVideo
                            )
                            DockIconButton(
                                icon = Icons.Default.BookmarkAdd,
                                contentDesc = "สลับช่องด่วน",
                                testTag = "btn_bookmark_current",
                                tint = if (showQuickChannelsStrip) ElectricCyan else Color.White,
                                onClick = { showQuickChannelsStrip = !showQuickChannelsStrip }
                            )
                            DockIconButton(
                                icon = Icons.Default.ExpandMore,
                                contentDesc = "ซ่อนแถบควบคุม",
                                testTag = "btn_hide_dock",
                                onClick = { showBottomDock = false }
                            )
                        }
                    }
                }
            }

            // Mini Floating Pill to restore Dock when hidden
            AnimatedVisibility(visible = !showBottomDock) {
                Surface(
                    onClick = { showBottomDock = true },
                    shape = CircleShape,
                    color = ObsidianBlack.copy(alpha = 0.82f),
                    modifier = Modifier
                        .border(1.dp, ElectricCyan.copy(alpha = 0.6f), CircleShape)
                        .testTag("btn_show_dock")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExpandLess,
                            contentDescription = "แสดงแถบควบคุม",
                            tint = ElectricCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "เมนูควบคุม",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            runCatching {
                webViewRef?.stopLoading()
            }
        }
    }
}

@Composable
private fun LocalVideoSurface(
    uri: Uri,
    isPlaying: Boolean,
    scaleX: Float,
    scaleY: Float
) {
    AndroidView(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(scaleX = scaleX, scaleY = scaleY)
            .testTag("local_video_view"),
        factory = { context ->
            VideoView(context).apply {
                val controller = MediaController(context)
                controller.setAnchorView(this)
                setMediaController(controller)
                setVideoURI(uri)
                setOnPreparedListener { mp ->
                    mp.isLooping = true
                    if (isPlaying) start()
                }
            }
        },
        update = { videoView ->
            if (isPlaying && !videoView.isPlaying) {
                videoView.start()
            } else if (!isPlaying && videoView.isPlaying) {
                videoView.pause()
            }
        }
    )
}

@Composable
private fun DockIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDesc: String,
    testTag: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1F293D),
        modifier = Modifier
            .size(44.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDesc,
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun resolveUserAgent(mode: String): String {
    return when (mode) {
        "CHROME_DESKTOP" -> "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
        "SMART_TV" -> "Mozilla/5.0 (SMART-TV; Linux; Tizen 6.5) AppleWebKit/537.36 (KHTML, like Gecko) Version/6.5 TV Safari/537.36"
        else -> "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Mobile Safari/537.36"
    }
}

private fun buildVideoFitScript(mode: String, customZoom: Int): String {
    val cssRule = when (mode) {
        "ULTRAWIDE_21_9" -> "object-fit: cover !important; transform: scale(1.14, 1.02) !important;"
        "STRETCH_FULL" -> "object-fit: fill !important; transform: scale(1.0) !important;"
        "ZOOM_115" -> "object-fit: cover !important; transform: scale(1.15) !important;"
        "ZOOM_130" -> "object-fit: cover !important; transform: scale(1.30) !important;"
        else -> {
            val scale = (customZoom.coerceIn(85, 145)) / 100.0
            "object-fit: contain !important; transform: scale($scale) !important;"
        }
    }
    return """
        (function() {
            try {
                var s = document.getElementById('carstream-fit-style');
                if (!s) {
                    s = document.createElement('style');
                    s.id = 'carstream-fit-style';
                    document.head.appendChild(s);
                }
                s.innerHTML = 'video { width: 100% !important; height: 100% !important; max-width: 100vw !important; max-height: 100vh !important; $cssRule }';
            } catch(e) {}
        })();
    """.trimIndent()
}

private val AD_SHIELD_AND_ENHANCE_SCRIPT = """
    (function() {
        try {
            var style = document.getElementById('carstream-auto-style');
            if (!style) {
                style = document.createElement('style');
                style.id = 'carstream-auto-style';
                style.innerHTML = `
                    ytm-promoted-sparkles-web-renderer,
                    ytm-companion-ad-renderer,
                    .ad-showing .video-ads,
                    .ytp-ad-overlay-container,
                    ytm-mealbar-promo-renderer,
                    .mealbar-promo-renderer,
                    ytm-statement-banner-renderer {
                        display: none !important;
                    }
                    body {
                        overflow-x: hidden !important;
                    }
                    *:focus {
                        outline: 3px solid #00E5FF !important;
                        outline-offset: 2px !important;
                    }
                `;
                document.head.appendChild(style);
            }
            var skipBtn = document.querySelector('.ytp-ad-skip-button, .ytp-ad-skip-button-modern, .ytp-skip-ad-button');
            if (skipBtn) { skipBtn.click(); }
        } catch(e) {}
    })();
""".trimIndent()
