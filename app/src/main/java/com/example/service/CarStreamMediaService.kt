package com.example.service

import android.media.MediaMetadata
import android.media.browse.MediaBrowser
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Bundle
import android.service.media.MediaBrowserService

/**
 * Android Auto MediaBrowserService implementation inspired by `thekirankumar/carstream-android-auto`
 * (`MyMediaBrowserService`), upgraded to prevent the common "No new messages during drive" /
 * "Media unavailable" error on Android 13, 14, and 15+ head units.
 */
class CarStreamMediaService : MediaBrowserService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val session = MediaSession(this, "CarStreamAutoMediaSession").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() {
                    CarStreamPlaybackBridge.onTransportCommand?.invoke("PLAY")
                    updatePlaybackState(PlaybackState.STATE_PLAYING)
                }

                override fun onPause() {
                    CarStreamPlaybackBridge.onTransportCommand?.invoke("PAUSE")
                    updatePlaybackState(PlaybackState.STATE_PAUSED)
                }

                override fun onSkipToNext() {
                    CarStreamPlaybackBridge.onTransportCommand?.invoke("NEXT")
                }

                override fun onSkipToPrevious() {
                    CarStreamPlaybackBridge.onTransportCommand?.invoke("PREV")
                }

                override fun onSeekTo(pos: Long) {
                    CarStreamPlaybackBridge.onTransportCommand?.invoke("SEEK:$pos")
                }
            })
            isActive = true
        }
        mediaSession = session
        sessionToken = session.sessionToken
        updatePlaybackState(PlaybackState.STATE_PAUSED)
        updateMetadata("CarStream Auto Ready", "YouTube • Plex • Local Video")
    }

    private fun updatePlaybackState(state: Int) {
        val playbackState = PlaybackState.Builder()
            .setActions(
                PlaybackState.ACTION_PLAY or
                    PlaybackState.ACTION_PAUSE or
                    PlaybackState.ACTION_PLAY_PAUSE or
                    PlaybackState.ACTION_SKIP_TO_NEXT or
                    PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackState.ACTION_SEEK_TO
            )
            .setState(state, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1.0f)
            .build()
        mediaSession?.setPlaybackState(playbackState)
    }

    private fun updateMetadata(title: String, subtitle: String) {
        val metadata = MediaMetadata.Builder()
            .putString(MediaMetadata.METADATA_KEY_TITLE, title)
            .putString(MediaMetadata.METADATA_KEY_ARTIST, subtitle)
            .putString(MediaMetadata.METADATA_KEY_ALBUM, "CarStream Auto Projection")
            .build()
        mediaSession?.setMetadata(metadata)
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot {
        return BrowserRoot(ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowser.MediaItem>>
    ) {
        val items = mutableListOf<MediaBrowser.MediaItem>()
        if (parentId == ROOT_ID) {
            items.add(
                createBrowsableItem(
                    "yt_home",
                    "YouTube CarStream",
                    "Stream YouTube with 21:9 aspect ratio & Ad-Shield"
                )
            )
            items.add(
                createBrowsableItem(
                    "plex_tv",
                    "Plex Media Server",
                    "Direct stream movies & personal media in car"
                )
            )
            items.add(
                createBrowsableItem(
                    "local_video",
                    "Local Video Player",
                    "Zero-permission Android Photo Picker MP4/MKV"
                )
            )
        }
        result.sendResult(items)
    }

    private fun createBrowsableItem(id: String, title: String, subtitle: String): MediaBrowser.MediaItem {
        val desc = android.media.MediaDescription.Builder()
            .setMediaId(id)
            .setTitle(title)
            .setSubtitle(subtitle)
            .build()
        return MediaBrowser.MediaItem(desc, MediaBrowser.MediaItem.FLAG_PLAYABLE)
    }

    override fun onDestroy() {
        mediaSession?.release()
        super.onDestroy()
    }

    companion object {
        private const val ROOT_ID = "carstream_root"
    }
}

object CarStreamPlaybackBridge {
    var onTransportCommand: ((String) -> Unit)? = null
}
