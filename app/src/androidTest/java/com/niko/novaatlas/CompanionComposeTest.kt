package com.niko.novaatlas

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.material3.MaterialTheme
import org.junit.Rule
import org.junit.Test

/** Small isolated components: no MainActivity, ads, service, API or radio connection. */
class CompanionComposeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun unicodeAndInertHostileTitle() {
        compose.setContent { MaterialTheme { ArticleCard(Article("h","","日本語","<script>alert(1)</script> العربية 🎙️",link="javascript:alert(1)",summary="Résumé")) } }
        compose.onNodeWithText("<script>alert(1)</script> العربية 🎙️").assertIsDisplayed()
    }
    @Test fun unknownRightsRemainFallback() {
        compose.setContent { MaterialTheme { ApprovedImage(PublicImage("https://example.invalid/private.jpg",false)) } }
        compose.onNodeWithText("◈ Illustration indisponible").assertIsDisplayed()
    }
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    @Test fun media3DecodesLocalSyntheticAudioAndPauses() {
        val context=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val file=java.io.File.createTempFile("companion-test-", ".wav", context.cacheDir)
        val size=32000
        val bytes=java.nio.ByteBuffer.allocate(44+size).order(java.nio.ByteOrder.LITTLE_ENDIAN)
        bytes.put("RIFF".toByteArray()).putInt(36+size).put("WAVEfmt ".toByteArray()).putInt(16)
            .putShort(1.toShort()).putShort(1.toShort()).putInt(8000).putInt(16000)
            .putShort(2.toShort()).putShort(16.toShort()).put("data".toByteArray()).putInt(size)
        file.writeBytes(bytes.array()) // two seconds of generated PCM silence, never published
        val played=java.util.concurrent.atomic.AtomicBoolean(false)
        val failure=java.util.concurrent.atomic.AtomicReference<androidx.media3.common.PlaybackException?>(null)
        var player: androidx.media3.exoplayer.ExoPlayer?=null
        compose.setContent { MaterialTheme { androidx.compose.material3.Text("Essai audio local") } }
        try {
            compose.runOnIdle {
                player=androidx.media3.exoplayer.ExoPlayer.Builder(context)
                    .setMediaSourceFactory(androidx.media3.exoplayer.source.DefaultMediaSourceFactory(context)
                        .setLoadErrorHandlingPolicy(RadioRetryPolicy())).build()
                player!!.addListener(object: androidx.media3.common.Player.Listener {
                    override fun onIsPlayingChanged(value: Boolean) { if(value) played.set(true) }
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) { failure.set(error) }
                })
                player!!.setMediaItem(androidx.media3.common.MediaItem.fromUri(android.net.Uri.fromFile(file)))
                player!!.prepare();player!!.play()
            }
            compose.waitUntil(15000) { played.get() || failure.get()!=null }
            org.junit.Assert.assertNull(failure.get())
            org.junit.Assert.assertTrue(played.get())
            compose.runOnIdle { player!!.pause();org.junit.Assert.assertFalse(player!!.playWhenReady) }
        } finally {
            compose.runOnIdle { player?.release() }
            file.delete()
        }
    }
}
