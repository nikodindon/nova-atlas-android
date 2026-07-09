package com.niko.novaatlas

import android.content.ComponentName
import android.content.Context
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
 * - Expose isPlaying comme StateFlow pour que Compose reactive proprement.
 *
 * Le service tourne meme quand l'activity est fermee : la notif media permet
 * a l'utilisateur de couper depuis la notif shade / lock screen.
 */
class RadioPlayer(private val context: Context) {

    private var controller: MediaController? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

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
                controller = futureController.get()
                _isPlaying.value = controller?.isPlaying == true
                onReady()
            },
            MoreExecutors.directExecutor()
        )
    }

    fun play() {
        controller?.playWhenReady = true
        _isPlaying.value = true
    }

    fun pause() {
        controller?.playWhenReady = false
        _isPlaying.value = false
    }

    /**
     * Deconnect le controller. NE TUE PAS le service (la radio continue en
     * background, c'est le but). Si l'utilisateur veut couper completement,
     * il passe par la notif media.
     */
    fun release() {
        controller?.release()
        controller = null
    }
}
