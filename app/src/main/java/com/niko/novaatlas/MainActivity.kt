package com.niko.novaatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niko.novaatlas.ui.theme.NovaAtlasTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaAtlasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RadioScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RadioScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    // remember {} : le player survit aux recompositions mais est recréé si on
    // change d'activity (ex: rotation écran). Pour un player complet, on
    // externalisera ça dans un ViewModel au Sprint B3.
    val player = remember { RadioPlayer(context) }
    var isPlaying by remember { mutableStateOf(false) }

    // Libère ExoPlayer quand le composable quitte l'écran (évite les fuites)
    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Nova-Atlas",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Web Radio",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 32.dp)
        )
        Button(
            onClick = {
                if (isPlaying) {
                    player.pause()
                } else {
                    player.play()
                }
                isPlaying = !isPlaying
            }
        ) {
            Text(text = if (isPlaying) "Pause Radio" else "Play Radio")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RadioScreenPreview() {
    NovaAtlasTheme {
        RadioScreen()
    }
}
