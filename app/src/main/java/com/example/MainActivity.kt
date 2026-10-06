package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.CarStreamDatabase
import com.example.data.CarStreamRepository
import com.example.ui.components.AspectRatioCalibratorSheet
import com.example.ui.components.AudioSyncCalibratorSheet
import com.example.ui.components.CarRotaryKeyboardSheet
import com.example.ui.screens.BookmarksAndHistoryScreen
import com.example.ui.screens.CarStreamFixesScreen
import com.example.ui.screens.StreamDeckScreen
import com.example.ui.theme.CarbonSurface
import com.example.ui.theme.CockpitCard
import com.example.ui.theme.CrimsonStream
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.EmeraldTelemetry
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ObsidianBlack
import com.example.viewmodel.AppDestination
import com.example.viewmodel.CarStreamViewModel

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: CarStreamViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = CarStreamDatabase.getDatabase(applicationContext)
        val repository = CarStreamRepository(db.dao())
        viewModel = ViewModelProvider(this, CarStreamViewModel.Factory(repository))[CarStreamViewModel::class.java]

        handleIncomingShareIntent(intent)

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            val view = LocalView.current

            // Toggle system bars (Status Bar & Navigation Bar) when entering/exiting Immersive Fullscreen
            LaunchedEffect(uiState.isImmersiveFullscreen) {
                val window = this@MainActivity.window ?: return@LaunchedEffect
                val insetsController = WindowCompat.getInsetsController(window, view)
                if (uiState.isImmersiveFullscreen) {
                    insetsController.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            MyApplicationTheme {
                CarStreamAppRoot(viewModel = viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIncomingShareIntent(intent)
    }

    private fun handleIncomingShareIntent(intent: Intent?) {
        if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!sharedText.isNullOrBlank()) {
                val urlMatch = Regex("(https?://\\S+)").find(sharedText)?.value ?: sharedText
                viewModel.loadWebStream(
                    url = CarStreamRepository.normalizeUrlOrSearch(urlMatch),
                    title = "ลิงก์ที่แชร์เข้ามา"
                )
            }
        }
    }
}

@Composable
fun CarStreamAppRoot(
    viewModel: CarStreamViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    val history by viewModel.history.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // Zero-permission Android Photo Picker for Local Videos
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            viewModel.loadLocalVideo(uri = uri, displayName = "Local Video (${uri.lastPathSegment ?: "MP4"})")
        }
    }

    // BackHandler exits Immersive Fullscreen first, or returns to Stream Deck from secondary tabs
    BackHandler(
        enabled = uiState.isImmersiveFullscreen || uiState.currentDestination != AppDestination.STREAM_DECK
    ) {
        if (uiState.isImmersiveFullscreen) {
            viewModel.setImmersiveFullscreen(false)
        } else {
            viewModel.selectDestination(AppDestination.STREAM_DECK)
        }
    }

    // Removed 2nd item ("จอในรถ (AA)") as requested; now 3 clean navigation items
    val navItems = listOf(
        Triple(AppDestination.STREAM_DECK, "เครื่องเล่น", Icons.Default.PlayCircleFilled),
        Triple(AppDestination.BOOKMARKS_HISTORY, "บุ๊กมาร์ก", Icons.Default.Bookmarks),
        Triple(AppDestination.ENGINE_FIXES, "แก้ข้อจำกัด", Icons.Default.Tune)
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        val isExpandedOrLandscape = maxWidth >= 640.dp

        Scaffold(
            modifier = if (uiState.isImmersiveFullscreen) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.safeDrawing)
            },
            containerColor = ObsidianBlack,
            topBar = {
                if (!uiState.isImmersiveFullscreen &&
                    (uiState.currentDestination == AppDestination.BOOKMARKS_HISTORY ||
                        uiState.currentDestination == AppDestination.ENGINE_FIXES)
                ) {
                    CarStreamTopCockpitBar(
                        currentDestination = uiState.currentDestination,
                        statusBanner = uiState.statusBannerMessage,
                        onDismissBanner = viewModel::dismissBanner,
                        onOpenRotaryKeyboard = { viewModel.toggleRotaryKeyboard(true) },
                        onPickLocalVideo = {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                    )
                }
            },
            bottomBar = {
                if (!isExpandedOrLandscape && !uiState.isImmersiveFullscreen) {
                    NavigationBar(
                        containerColor = CarbonSurface,
                        modifier = Modifier.testTag("bottom_nav_bar")
                    ) {
                        navItems.forEach { (dest, label, icon) ->
                            val selected = uiState.currentDestination == dest
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(dest) },
                                icon = {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = label
                                    )
                                },
                                label = {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                modifier = Modifier.testTag("nav_tab_${dest.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (isExpandedOrLandscape && !uiState.isImmersiveFullscreen) {
                    NavigationRail(
                        containerColor = CarbonSurface,
                        modifier = Modifier
                            .fillMaxHeight()
                            .testTag("side_nav_rail")
                    ) {
                        navItems.forEach { (dest, label, icon) ->
                            val selected = uiState.currentDestination == dest
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectDestination(dest) },
                                icon = { Icon(icon, contentDescription = label) },
                                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = ObsidianBlack,
                                    selectedTextColor = ElectricCyan,
                                    indicatorColor = ElectricCyan
                                ),
                                modifier = Modifier.testTag("rail_tab_${dest.name.lowercase()}")
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    when (uiState.currentDestination) {
                        AppDestination.STREAM_DECK -> {
                            StreamDeckScreen(
                                uiState = uiState,
                                settings = settings,
                                bookmarks = bookmarks,
                                onSelectBookmark = { bm ->
                                    viewModel.loadWebStream(bm.url, bm.title)
                                },
                                onPageStarted = viewModel::onWebPageStarted,
                                onPageFinished = viewModel::onWebPageFinished,
                                onProgressChanged = viewModel::onWebProgressChanged,
                                onTogglePlayPause = viewModel::togglePlayPause,
                                onSeekDelta = viewModel::seekBySeconds,
                                onToggleFullscreen = viewModel::toggleImmersiveFullscreen,
                                onOpenAspectSheet = { viewModel.toggleAspectSheet(true) },
                                onOpenAudioSyncSheet = { viewModel.toggleAudioSyncSheet(true) },
                                onOpenRotaryKeyboard = { viewModel.toggleRotaryKeyboard(true) },
                                onBookmarkCurrent = viewModel::bookmarkCurrentStream,
                                onPickLocalVideo = {
                                    videoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                                    )
                                }
                            )
                        }

                        AppDestination.BOOKMARKS_HISTORY -> {
                            BookmarksAndHistoryScreen(
                                bookmarks = bookmarks,
                                history = history,
                                showAddDialog = uiState.showAddBookmarkDialog,
                                onOpenAddDialog = viewModel::toggleAddBookmarkDialog,
                                onAddBookmark = { title, url, cat ->
                                    viewModel.addBookmark(title, url, cat)
                                },
                                onDeleteBookmark = viewModel::deleteBookmark,
                                onSelectBookmark = { bm ->
                                    viewModel.loadWebStream(bm.url, bm.title)
                                    viewModel.selectDestination(AppDestination.STREAM_DECK)
                                },
                                onSelectHistory = { hist ->
                                    viewModel.loadWebStream(hist.url, hist.title)
                                    viewModel.selectDestination(AppDestination.STREAM_DECK)
                                },
                                onClearHistory = viewModel::clearHistory
                            )
                        }

                        AppDestination.ENGINE_FIXES -> {
                            CarStreamFixesScreen(
                                settings = settings,
                                onUpdateUserAgent = viewModel::updateUserAgentMode,
                                onToggleAdShield = viewModel::toggleAdShield,
                                onToggleSponsorSkip = viewModel::toggleSponsorSkipHint,
                                onToggleBackgroundKeepAlive = viewModel::toggleBackgroundKeepAlive,
                                onToggleRotaryHighlight = viewModel::toggleRotaryDpadHighlight,
                                onOpenAspectCalibrator = { viewModel.toggleAspectSheet(true) },
                                onOpenAudioSyncCalibrator = { viewModel.toggleAudioSyncSheet(true) },
                                onOpenRotaryKeyboard = { viewModel.toggleRotaryKeyboard(true) }
                            )
                        }
                    }
                }
            }
        }
    }

    // Bottom Sheets for CarStream Upgrades
    if (uiState.showRotaryKeyboard) {
        CarRotaryKeyboardSheet(
            currentInput = uiState.searchQueryInput,
            onAppendChar = viewModel::appendCharFromRotaryKeyboard,
            onBackspace = viewModel::backspaceFromRotaryKeyboard,
            onClear = viewModel::clearRotaryKeyboardInput,
            onSubmitUrlOrSearch = { viewModel.submitSearchOrUrl() },
            onInjectIntoWebField = { viewModel.injectRotaryInputToFocusedWebField(uiState.searchQueryInput) },
            onDismiss = { viewModel.toggleRotaryKeyboard(false) }
        )
    }

    if (uiState.showAspectSheet) {
        AspectRatioCalibratorSheet(
            settings = settings,
            onSelectAspect = viewModel::updateAspectRatio,
            onDismiss = { viewModel.toggleAspectSheet(false) }
        )
    }

    if (uiState.showAudioSyncSheet) {
        AudioSyncCalibratorSheet(
            settings = settings,
            onUpdateOffset = viewModel::updateAudioOffsetMs,
            onDismiss = { viewModel.toggleAudioSyncSheet(false) }
        )
    }
}

@Composable
private fun CarStreamTopCockpitBar(
    currentDestination: AppDestination,
    statusBanner: String?,
    onDismissBanner: () -> Unit,
    onOpenRotaryKeyboard: () -> Unit,
    onPickLocalVideo: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CarbonSurface)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(CrimsonStream)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "CarStream Auto",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White
                    )
                    Text(
                        text = when (currentDestination) {
                            AppDestination.STREAM_DECK -> "YouTube • Plex • Local Video Cockpit"
                            AppDestination.BOOKMARKS_HISTORY -> "Pinned Channels & Playback History"
                            AppDestination.ENGINE_FIXES -> "Limitation Fixes & Engine Calibration"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = ElectricCyan
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    onClick = onOpenRotaryKeyboard,
                    shape = RoundedCornerShape(12.dp),
                    color = CockpitCard,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_btn_rotary_keyboard")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Keyboard,
                            contentDescription = "แป้นพิมพ์ในรถ",
                            tint = Color(0xFFFBBF24)
                        )
                    }
                }
                Surface(
                    onClick = onPickLocalVideo,
                    shape = RoundedCornerShape(12.dp),
                    color = CockpitCard,
                    modifier = Modifier
                        .size(44.dp)
                        .testTag("top_btn_local_video")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.FolderOpen,
                            contentDescription = "เปิดวิดีโอในเครื่อง",
                            tint = EmeraldTelemetry
                        )
                    }
                }
            }
        }

        AnimatedVisibility(visible = !statusBanner.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ElectricCyan.copy(alpha = 0.14f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
                    .border(1.dp, ElectricCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = statusBanner.orEmpty(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = ElectricCyan,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismissBanner,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "ปิดแจ้งเตือน",
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
