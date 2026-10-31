package com.kukosin.app.playback

import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@UnstableApi
class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var settingsJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
        AudioSettingsManager.init(this)
        recreatePlayer()

        settingsJob = AudioSettingsManager.configState
            .onEach {
                recreatePlayer()
            }
            .launchIn(serviceScope)
    }

    private fun recreatePlayer() {
        val currentItems = player?.let { p ->
            val count = p.mediaItemCount
            if (count == 0) return@let null
            val list = List(count) { i -> p.getMediaItemAt(i) }
            val index = p.currentMediaItemIndex
            val pos = p.currentPosition
            val isPlaying = p.isPlaying
            Triple(list, index, pos to isPlaying)
        }

        player?.release()
        mediaSession?.release()

        val newPlayer = ExoPlayer.Builder(this)
            .setRenderersFactory(HiFiRenderersFactory(this))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .setSkipSilenceEnabled(false)
            .build()
        newPlayer.volume = 1f

        AudioRoute.attach(this, newPlayer)
        mediaSession = MediaSession.Builder(this, newPlayer).build()
        player = newPlayer

        if (currentItems != null && currentItems.first.isNotEmpty()) {
            val (items, index, posPlaying) = currentItems
            newPlayer.setMediaItems(items, index, posPlaying.first)
            newPlayer.prepare()
            if (posPlaying.second) {
                newPlayer.play()
            }
        }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        settingsJob?.cancel()
        mediaSession?.run {
            release()
        }
        player?.release()
        mediaSession = null
        player = null
        super.onDestroy()
    }
}
