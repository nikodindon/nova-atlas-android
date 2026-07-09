package com.niko.novaatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.ads.MobileAds
import com.niko.novaatlas.ui.theme.NovaAtlasTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init AdMob SDK le plus tot possible (avant super.onCreate = best practice)
        MobileAds.initialize(this) {}
        super.onCreate(savedInstanceState)

        adManager = AdManager(this)

        enableEdgeToEdge()
        setContent {
            NovaAtlasTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RadioScreen(
                        modifier = Modifier.padding(innerPadding),
                        adManager = adManager,
                    )
                }
            }
        }
    }
}

@Composable
fun RadioScreen(modifier: Modifier = Modifier, adManager: AdManager) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val player = remember { RadioPlayer(context) }
    val isPlaying by player.isPlaying.collectAsState()

    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        player.connect()
        scope.launch {
            try {
                val response = ApiClient.api.getArticles(limit = 50)
                articles = response.articles
                errorMsg = null
            } catch (e: Exception) {
                errorMsg = "Erreur chargement articles: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = "Nova-Atlas", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Web Radio",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Button(
                onClick = {
                    // Si on coupe la radio, on ne montre PAS de pub (UX propre)
                    if (isPlaying) {
                        player.pause()
                        return@Button
                    }
                    // Sinon on montre la pub (si prete + rate limit ok) PUIS on lance la radio
                    if (activity != null) {
                        adManager.showIfReady(activity) {
                            player.play()
                        }
                    } else {
                        player.play()
                    }
                }
            ) {
                Text(text = if (isPlaying) "Pause Radio" else "Play Radio")
            }
        }

        HorizontalDivider()

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
                errorMsg != null -> Text(
                    text = errorMsg!!,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.error
                )
                articles.isEmpty() -> Text(
                    text = "Aucun article aujourd'hui",
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "Fil d'actu (${articles.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    items(articles) { article ->
                        ArticleCard(article)
                    }
                }
            }
        }
    }
}

@Composable
fun ArticleCard(article: Article) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${article.category} • ${article.source ?: ""}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium
            )
            if (!article.summary.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = article.summary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 3
                )
            }
        }
    }
}
