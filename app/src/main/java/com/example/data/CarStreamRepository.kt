package com.example.data

import kotlinx.coroutines.flow.Flow

class CarStreamRepository(private val dao: CarStreamDao) {
    val bookmarks: Flow<List<BookmarkEntity>> = dao.getAllBookmarks()
    val history: Flow<List<HistoryEntity>> = dao.getRecentHistory()
    val settings: Flow<CarStreamSettingsEntity?> = dao.getSettings()

    suspend fun ensureDefaultData() {
        if (dao.getBookmarkCount() == 0) {
            val defaults = listOf(
                BookmarkEntity(
                    title = "YouTube Mobile",
                    url = "https://m.youtube.com",
                    category = "YOUTUBE",
                    accentHex = "#FF2A54",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "YouTube Music",
                    url = "https://music.youtube.com",
                    category = "YOUTUBE",
                    accentHex = "#FF0055",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "Plex Web TV",
                    url = "https://app.plex.tv/desktop",
                    category = "PLEX",
                    accentHex = "#E5A00D",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "NASA Live Stream (HLS)",
                    url = "https://www.youtube.com/embed/21X5lGlDOfg?autoplay=1",
                    category = "STREAM",
                    accentHex = "#00E5FF",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "Lofi Girl — Driving Beats",
                    url = "https://www.youtube.com/embed/jfKfPfyJRdk?autoplay=1",
                    category = "YOUTUBE",
                    accentHex = "#10B981",
                    isPinned = true
                ),
                BookmarkEntity(
                    title = "Twitch Mobile",
                    url = "https://m.twitch.tv",
                    category = "STREAM",
                    accentHex = "#9146FF",
                    isPinned = false
                ),
                BookmarkEntity(
                    title = "Vimeo Watch",
                    url = "https://vimeo.com/watch",
                    category = "STREAM",
                    accentHex = "#00ADEF",
                    isPinned = false
                ),
                BookmarkEntity(
                    title = "Google Search",
                    url = "https://www.google.com",
                    category = "WEB",
                    accentHex = "#3B82F6",
                    isPinned = false
                )
            )
            defaults.forEach { dao.insertBookmark(it) }
            // Default to MOBILE_ANDROID so WebView renderer stays lightweight and avoids Desktop SPA OOM
            dao.saveSettings(CarStreamSettingsEntity(userAgentMode = "MOBILE_ANDROID"))
        }
    }

    suspend fun addBookmark(title: String, url: String, category: String = "WEB", accentHex: String = "#00E5FF") {
        dao.insertBookmark(
            BookmarkEntity(
                title = title.ifBlank { url },
                url = normalizeUrlOrSearch(url),
                category = category,
                accentHex = accentHex,
                isPinned = true
            )
        )
    }

    suspend fun deleteBookmark(id: Int) {
        dao.deleteBookmarkById(id)
    }

    suspend fun recordHistory(title: String, url: String, sourceType: String, positionMs: Long = 0L) {
        dao.insertHistory(
            HistoryEntity(
                title = title.ifBlank { url },
                url = url,
                sourceType = sourceType,
                lastPositionMs = positionMs
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearHistory()
    }

    suspend fun updateSettings(settings: CarStreamSettingsEntity) {
        dao.saveSettings(settings)
    }

    companion object {
        fun normalizeUrlOrSearch(input: String): String {
            val trimmed = input.trim()
            if (trimmed.isEmpty()) return "https://m.youtube.com"
            if (trimmed.startsWith("http://") || trimmed.startsWith("https://") || trimmed.startsWith("content://") || trimmed.startsWith("file://")) {
                return trimmed
            }
            if (trimmed.contains(".") && !trimmed.contains(" ")) {
                return "https://$trimmed"
            }
            val encoded = java.net.URLEncoder.encode(trimmed, "UTF-8")
            return "https://m.youtube.com/results?search_query=$encoded"
        }
    }
}
