package com.niko.novaatlas

import android.content.Intent
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
class RadioService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        val player = ExoPlayer.Builder(this)
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
        player.prepare()

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Si l'utilisateur swipe l'app depuis le recent apps, on coupe le player
        // (UX "fermer l'app = arreter la radio", comme la plupart des radios)
        val session = mediaSession ?: return
        if (!session.player.playWhenReady || session.player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }
}
