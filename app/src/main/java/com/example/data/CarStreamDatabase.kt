package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val category: String, // "YOUTUBE", "STREAM", "WEB", "PLEX"
    val accentHex: String,
    val isPinned: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "playback_history")
data class HistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String,
    val sourceType: String, // "WEB_STREAM", "LOCAL_VIDEO", "DIRECT_HLS"
    val lastPositionMs: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "carstream_settings")
data class CarStreamSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val aspectRatioMode: String = "FIT_16_9", // FIT_16_9, ULTRAWIDE_21_9, STRETCH_FULL, ZOOM_115, ZOOM_130
    val customZoomPercent: Int = 100,
    val audioOffsetMs: Int = 0, // -500 to +500 ms to fix CarStream A/V sync issues
    val userAgentMode: String = "MOBILE_ANDROID", // MOBILE_ANDROID, CHROME_DESKTOP, SMART_TV
    val adShieldEnabled: Boolean = true,
    val sponsorSkipHintEnabled: Boolean = true,
    val autoResumePlayback: Boolean = true,
    val backgroundAudioKeepAlive: Boolean = true,
    val coolwalkSplitRatio: Float = 0.68f, // 68% video / 32% side telemetry & queue
    val rotaryDpadHighlightEnabled: Boolean = true
)

@Dao
interface CarStreamDao {
    @Query("SELECT * FROM bookmarks ORDER BY isPinned DESC, timestamp DESC")
    fun getAllBookmarks(): Flow<List<BookmarkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBookmark(bookmark: BookmarkEntity)

    @Query("DELETE FROM bookmarks WHERE id = :id")
    suspend fun deleteBookmarkById(id: Int)

    @Query("SELECT COUNT(*) FROM bookmarks")
    suspend fun getBookmarkCount(): Int

    @Query("SELECT * FROM playback_history ORDER BY timestamp DESC LIMIT 30")
    fun getRecentHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(item: HistoryEntity)

    @Query("DELETE FROM playback_history")
    suspend fun clearHistory()

    @Query("SELECT * FROM carstream_settings WHERE id = 1")
    fun getSettings(): Flow<CarStreamSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: CarStreamSettingsEntity)
}

@Database(
    entities = [BookmarkEntity::class, HistoryEntity::class, CarStreamSettingsEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CarStreamDatabase : RoomDatabase() {
    abstract fun dao(): CarStreamDao

    companion object {
        @Volatile
        private var INSTANCE: CarStreamDatabase? = null

        fun getDatabase(context: Context): CarStreamDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    CarStreamDatabase::class.java,
                    "carstream_auto_db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
