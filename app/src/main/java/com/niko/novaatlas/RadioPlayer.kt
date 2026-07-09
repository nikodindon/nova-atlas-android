package com.niko.novaatlas

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Wrap minimal autour d'ExoPlayer pour streamer la radio Nova-Atlas.
 * Stream URL = http://192.168.1.22:8000/nova (Icecast mount /nova)
 *
 * Note pour plus tard : on rajoutera un MediaSessionService pour la notif
 * media + playback en background (Sprint B3).
 */
class RadioPlayer(private val context: Context) {

    private val player: ExoPlayer = ExoPlayer.Builder(context).build().apply {
        // Auto-play dès qu'on a un MediaItem prêt
        playWhenReady = false
    }

    private val streamUrl = "http://192.168.1.22:8000/nova"

    fun play() {
        // setMediaItem est idempotent : si on re-tap Play après une pause, on évite de
        // rebuffer le stream en re-setant le même item.
        if (player.currentMediaItem == null) {
            player.setMediaItem(MediaItem.fromUri(streamUrl))
            player.prepare()
        }
        player.play()
    }

    fun pause() {
        player.pause()
    }

    fun release() {
        player.release()
    }

    val isPlaying: Boolean
        get() = player.isPlaying
}
