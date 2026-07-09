package com.niko.novaatlas

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Client UI pour la radio Nova-Atlas.
 * - Ne contient PAS d'ExoPlayer : il delegue tout au RadioService (background).
 * - Connect au service via MediaController (pattern Google officiel).
 * - Expose isPlaying comme StateFlow, source de verite = le Player distant.
 *
 * Le service tourne meme quand l'activity est fermee : la notif media permet
 * a l'utilisateur de couper depuis la notif shade / lock screen.
 */
class RadioPlayer(private val context: Context) {

    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    /**
     * Listener qui propage l'etat reel du Player (service) vers le StateFlow
     * local. Sans ca, on a des "faux playing" quand l'UI dit play mais que
     * ExoPlayer est encore en pause (race entre l'update local et l'event
     * asynchrone du controller). C'est aussi ce qui fixait le bug
     * "play apres pause ne relance pas" : l'UI croit que ca joue mais le
     * player distant dit non.
     */
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
        }
    }

    /**
     * Connect au RadioService. Idempotent : peut etre rappele sans probleme.
     * A appeller depuis un LaunchedEffect(Unit) dans Compose.
     */
    fun connect(onReady: () -> Unit = {}) {
        if (controller != null) {
            onReady()
            return
        }
        val token = SessionToken(context, ComponentName(context, RadioService::class.java))
        val futureController = MediaController.Builder(context, token).buildAsync()
        futureController.addListener(
            {
                val c = futureController.get()
                controller = c
                c.addListener(playerListener)
                _isPlaying.value = c.isPlaying
                onReady()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun play() {
        // On ne touche plus _isPlaying ici : c'est le Player.Listener qui
        // fait foi quand l'etat reel change (sinon UI dit "playing" mais
        // le player distant est encore en pause = bug play apres pause).
        controller?.playWhenReady = true
    }

    fun pause() {
        controller?.playWhenReady = false
    }

    /**
     * Deconnect le controller. NE TUE PAS le service (la radio continue en
     * background, c'est le but). Si l'utilisateur veut couper completement,
     * il passe par la notif media.
     */
    fun release() {
        controller?.let {
            it.removeListener(playerListener)
            it.release()
        }
        controller = null
    }
}
