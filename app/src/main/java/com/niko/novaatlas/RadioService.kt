package com.niko.novaatlas

import android.content.Intent
import androidx.media3.common.Player
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

/**
 * Service Android qui tient l'ExoPlayer en vie même quand l'activity est fermée.
 * Le systeme affiche automatiquement une "Media Notification" (style Spotify)
 * avec play/pause/stop, titre du stream, et controls sur le lock screen.
 *
 * Le client (Compose UI) se connecte via un MediaController - on n'instancie
 * plus d'ExoPlayer cote UI.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class RadioService : MediaSessionService() {

    private val recoveryHandler=android.os.Handler(android.os.Looper.getMainLooper())
    private val endRecovery=RadioEndRecovery()
    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val embargo = RadioEmbargo()
        val http = OkHttpDataSource.Factory(OkHttpClient.Builder().followRedirects(false).followSslRedirects(false).retryOnConnectionFailure(false).connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).build())
        val player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(http).setLoadErrorHandlingPolicy(RadioRetryPolicy(embargo)))
            // Le stream est de la musique (classifie "media" par Android, priorite
            // audio correcte, duck sur notifs/Calls)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .build()
            .apply { playWhenReady = false }

        val streamUri = BuildConfig.RADIO_STREAM_URL

        // MediaMetadata permet au systeme d'afficher "Nova-Atlas - Web Radio"
        // dans la notif media + sur le lock screen
        val item = MediaItem.Builder()
            .setMediaId("nova-radio")
            .setUri(streamUri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Nova-Atlas")
                    .setArtist("Web Radio IA")
                    .setStation("Nova-Atlas")
                    .build()
            )
            .build()

        player.setMediaItem(item)
        // No connection before explicit Play; Stop cancels a pending load/retry.
        player.addListener(object: Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state != Player.STATE_ENDED || !player.playWhenReady) return
                val wait=endRecovery.nextDelay()
                if (wait == null || !embargo.allowed()) { player.pause(); return }
                recoveryHandler.removeCallbacksAndMessages(null)
                recoveryHandler.postDelayed({
                    if (player.playWhenReady && player.playbackState == Player.STATE_ENDED && embargo.allowed()) {
                        player.seekToDefaultPosition(); player.prepare()
                    }
                },wait)
            }
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                if (playWhenReady && !embargo.allowed()) { player.pause(); return }
                if (!playWhenReady) { recoveryHandler.removeCallbacksAndMessages(null); player.stop() }
                else if (player.playbackState == Player.STATE_IDLE) { endRecovery.reset(); player.prepare() }
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // A playing session can continue after activity dismissal; explicit Pause stops it.
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        recoveryHandler.removeCallbacksAndMessages(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
