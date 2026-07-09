package com.niko.novaatlas

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radio
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.niko.novaatlas.ui.theme.NovaAccentGreen
import com.niko.novaatlas.ui.theme.NovaAccentRed
import com.niko.novaatlas.ui.theme.NovaAccentYellow
import com.niko.novaatlas.ui.theme.NovaBg0
import com.niko.novaatlas.ui.theme.NovaBg2
import com.niko.novaatlas.ui.theme.NovaBg3
import com.niko.novaatlas.ui.theme.NovaBg4
import com.niko.novaatlas.ui.theme.NovaTextDim
import com.niko.novaatlas.ui.theme.NovaTextMuted
import com.niko.novaatlas.ui.theme.NovaTextPrimary
import com.niko.novaatlas.ui.theme.NovaTextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Categories Nova-Atlas (alignees sur CATEGORY_LABELS / CATEGORY_ICONS du serveur).
 * Source: modules/web/atlas_web.py lignes 28-60 (2026-07-09).
 */
private data class Category(val key: String, val label: String, val icon: String)

private val CATEGORIES = listOf(
    Category("geopolitique",       "Geopolitique",      "🌍"),
    Category("economie",           "Economie",          "📈"),
    Category("crypto",             "Crypto",            "₿"),
    Category("tech",               "Tech & IA",         "⚡"),
    Category("france",             "France",            "🗼"),
    Category("monde",              "Monde",             "🌐"),
    Category("science",            "Science & Sante",   "🔬"),
    Category("environnement",      "Environnement",     "🌿"),
    Category("societe",            "Societe",           "✊"),
    Category("culture",            "Culture",           "🎭"),
    Category("sport",              "Sport",             "⚽"),
    Category("sante",              "Sante",             "🏥"),
    Category("gaming",             "Gaming",            "🎮"),
    Category("sciences_humaines",  "Sciences Humaines", "🧠"),
    Category("auto",               "Auto",              "🚗"),
    Category("regions",            "Regions",           "🗺️"),
)

enum class Tab(val label: String) {
    Feed("Fil d'actu"),
    Radio("Radio"),
}

/**
 * Ecran racine : 2 onglets (Fil d'actu / Radio) avec bottom nav, theme Nova-Atlas.
 */
@Composable
fun AppScreen(
    player: RadioPlayer,
    adManager: AdManager,
    subscriptionManager: SubscriptionManager,
) {
    var currentTab by remember { mutableStateOf(Tab.Feed) }
    val isPlaying by player.isPlaying.collectAsState()
    val isPremium by subscriptionManager.isPremium.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NovaBg0)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (currentTab) {
                Tab.Feed -> {
                    Box(modifier = Modifier.weight(1f)) {
                        NewsFeedScreen()
                    }
                }
                Tab.Radio -> {
                    Box(modifier = Modifier.weight(1f)) {
                        RadioScreen(player = player, adManager = adManager, isPremium = isPremium)
                    }
                }
            }

            // BottomNav en bas, en dehors de la zone de contenu
            BottomNav(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                isPlaying = isPlaying,
                isPremium = isPremium,
            )
        }
    }
}

@Composable
private fun BottomNav(
    currentTab: Tab,
    onTabSelected: (Tab) -> Unit,
    isPlaying: Boolean,
    isPremium: Boolean,
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        NavigationBarItem(
            selected = currentTab == Tab.Feed,
            onClick = { onTabSelected(Tab.Feed) },
            icon = {
                Box(contentAlignment = Alignment.Center) {
                    Text("📰", style = MaterialTheme.typography.titleLarge)
                    if (isPlaying) {
                        // Petit indicateur qu'on est en train de streamer
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(NovaAccentGreen)
                        )
                    }
                }
            },
            label = { Text(Tab.Feed.label) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                unselectedIconColor = NovaTextMuted,
                unselectedTextColor = NovaTextMuted,
            )
        )
        NavigationBarItem(
            selected = currentTab == Tab.Radio,
            onClick = { onTabSelected(Tab.Radio) },
            icon = {
                Icon(
                    Icons.Filled.Radio,
                    contentDescription = "Radio",
                    tint = if (currentTab == Tab.Radio) MaterialTheme.colorScheme.primary else NovaTextMuted
                )
            },
            label = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(Tab.Radio.label)
                    if (isPremium) {
                        Spacer(Modifier.width(4.dp))
                        Text("✨", style = MaterialTheme.typography.labelSmall)
                    }
                }
            },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary,
                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                unselectedIconColor = NovaTextMuted,
                unselectedTextColor = NovaTextMuted,
            )
        )
    }
}

// ─── Onglet Fil d'actu ───────────────────────────────────────────────────────

@Composable
private fun NewsFeedScreen() {
    val scope = rememberCoroutineScope()
    var articles by remember { mutableStateOf<List<Article>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var lastRefresh by remember { mutableStateOf<Long>(0L) }
    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }

    // Filtre derive : si selectedCategories est vide, on garde tout ; sinon on filtre
    val filteredArticles by remember {
        derivedStateOf {
            if (selectedCategories.isEmpty()) articles
            else articles.filter { it.category in selectedCategories }
        }
    }

    // Refresh toutes les 2s, comme le site Flask
    LaunchedEffect(Unit) {
        while (true) {
            scope.launch {
                try {
                    val response = ApiClient.api.getArticles(limit = 50)
                    articles = response.articles
                    errorMsg = null
                    lastRefresh = System.currentTimeMillis()
                } catch (e: Exception) {
                    errorMsg = "Erreur chargement: ${e.message}"
                } finally {
                    isLoading = false
                }
            }
            delay(2000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header compact (style site)
        FeedHeader(
            isLoading = isLoading,
            breakingArticles = articles.take(8),  // Top 8 pour le ticker
        )

        // Filtres categories (chips multi-select cumulatifs)
        CategoryFilterBar(
            selected = selectedCategories,
            onToggle = { cat ->
                selectedCategories = if (cat in selectedCategories) {
                    selectedCategories - cat
                } else {
                    selectedCategories + cat
                }
            },
            onClear = { selectedCategories = emptySet() },
        )

        HorizontalDivider(color = NovaBg4)

        // Liste filtree
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                errorMsg != null && articles.isEmpty() -> {
                    Text(
                        text = errorMsg ?: "",
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                isLoading && articles.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                filteredArticles.isEmpty() -> {
                    Text(
                        text = if (selectedCategories.isEmpty()) "Aucun article" else "Aucun article dans ces categories",
                        modifier = Modifier.align(Alignment.Center),
                        color = NovaTextMuted,
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(filteredArticles) { article ->
                            ArticleCard(article)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    isLoading: Boolean,
    breakingArticles: List<Article>,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        // Rangée 1 : indicateur LIVE a gauche + logo N a droite du LIVE, le tout centre
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Gauche : reserve equilibre (meme largeur que la droite)
            Spacer(Modifier.weight(1f))

            // Centre : (LIVE) (logo N) cote a cote
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp,
                        color = NovaTextMuted,
                    )
                } else {
                    val infinite = rememberInfiniteTransition(label = "live-pulse")
                    val alpha by infinite.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1200, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse,
                        ),
                        label = "live-pulse-alpha",
                    )
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(NovaAccentGreen.copy(alpha = alpha))
                    )
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = "LIVE",
                        color = NovaAccentGreen,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                    )
                }
                Spacer(Modifier.width(10.dp))
                Image(
                    painter = painterResource(R.drawable.ic_header_logo),
                    contentDescription = "Nova-Atlas",
                    modifier = Modifier.size(32.dp),
                )
            }

            // Droite : reserve equilibre
            Spacer(Modifier.weight(1f))
        }

        // Rangée 2 : ticker breaking (defile horizontalement, style site)
        if (breakingArticles.isNotEmpty()) {
            BreakingTicker(articles = breakingArticles)
        }
    }
}

/**
 * Ticker "BREAKING" : un seul titre a la fois, defile horizontalement.
 * Tap sur le ticker = ouvre l'article dans le navigateur.
 * Format : [BREAKING] | 🌍  France - Maroc en direct : suivez le premier...
 *
 * - basicMarquee() = composant natif Android pour le texte defilant (smooth,
 *   s'arrete quand on quitte l'app, etc.)
 * - La rotation entre les news est geree par un LaunchedEffect qui change
 *   l'index toutes les 8s (le temps que le titre ait fini de defiler)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BreakingTicker(articles: List<Article>) {
    val context = LocalContext.current
    var currentIndex by remember { mutableStateOf(0) }

    // Rotation auto : change de news toutes les 12s (laisse le temps de lire
    // 1-2 passages du titre complet, meme pour les titres longs)
    LaunchedEffect(articles.size) {
        if (articles.size > 1) {
            while (true) {
                delay(12_000)
                currentIndex = (currentIndex + 1) % articles.size
            }
        }
    }

    // L'article courant (protege si la liste change entre 2 frames)
    val current = articles.getOrNull(currentIndex) ?: articles.firstOrNull()
    if (current == null) return

    val cat = CATEGORIES.firstOrNull { it.key == current.category }
    val catIcon = cat?.icon ?: "📰"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .background(NovaBg2)
            // Tap = ouvre l'article dans le navigateur
            .clickable {
                val link = current.link
                if (!link.isNullOrEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
                    runCatching { context.startActivity(intent) }
                        .onFailure { Log.w("BreakingTicker", "Cannot open link: $link") }
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Label "BREAKING" fixe a gauche
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .background(NovaAccentRed)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = "BREAKING",
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
            )
        }

        // Titre de l'article courant, defile avec basicMarquee
        // (le composant natif Compose pour le texte qui defile a la demande)
        Text(
            text = "$catIcon  ${current.title}",
            color = NovaTextPrimary,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 12.dp)
                .basicMarquee(
                    iterations = Int.MAX_VALUE,  // Defile en boucle indefiniment
                    delayMillis = 800,           // Pause 0.8s entre 2 passages du meme titre
                    velocity = 50.dp,            // Vitesse : 50dp par seconde (plus rapide)
                ),
        )
    }
}

@Composable
private fun CategoryFilterBar(
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item {
            FilterChip(
                selected = selected.isEmpty(),
                onClick = onClear,
                label = { Text("Tout", style = MaterialTheme.typography.labelMedium) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary,
                ),
            )
        }
        items(CATEGORIES) { cat ->
            val isSelected = cat.key in selected
            FilterChip(
                selected = isSelected,
                onClick = { onToggle(cat.key) },
                label = {
                    Text(
                        "${cat.icon} ${cat.label}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = NovaAccentYellow.copy(alpha = 0.2f),
                    selectedLabelColor = NovaAccentYellow,
                ),
            )
        }
    }
}

@Composable
fun ArticleCard(article: Article) {
    val context = LocalContext.current
    // Tap sur le titre = ouvre l'article dans le navigateur
    val openArticle = {
        val link = article.link
        if (!link.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(link))
            runCatching { context.startActivity(intent) }
                .onFailure { Log.w("ArticleCard", "Cannot open link: $link") }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = NovaBg3),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Pastille categorie coloree
                Text(
                    text = "${CATEGORIES.firstOrNull { it.key == article.category }?.icon ?: "🌐"}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = (CATEGORIES.firstOrNull { it.key == article.category }?.label
                        ?: article.category).uppercase(),
                    color = NovaAccentYellow,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.weight(1f))
                if (!article.source.isNullOrBlank()) {
                    Text(
                        text = article.source,
                        color = NovaTextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                // Heure relative (il y a 2h, hier, 3j, etc.)
                val relativeTime = remember(article.timestamp, article.pub_date) {
                    formatRelativeTime(article.timestamp, article.pub_date)
                }
                if (relativeTime.isNotEmpty()) {
                    Text(
                        text = "· $relativeTime",
                        color = NovaTextDim,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            // Titre : couleur plus claire (NovaTextPrimary, blanc), font semi-bold
            // pour ressortir par rapport au resume (qui sera NovaTextDim, gris doux)
            // Tap = ouvre l'article dans le navigateur
            Text(
                text = article.title,
                color = NovaTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable(onClick = openArticle),
            )
            if (!article.summary.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                var expanded by remember(article.hash) { mutableStateOf(false) }
                // Resume : couleur plus douce (NovaTextDim, gris moyen)
                // Tap = expand/collapse le resume
                Text(
                    text = article.summary,
                    color = NovaTextDim,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = if (expanded) Int.MAX_VALUE else 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded },
                )
            }
        }
    }
}

/**
 * Formate un timestamp Nova-Atlas en francais relatif.
 * Priorite : timestamp ISO (interne) > pub_date RFC2822 (RSS) > vide.
 * Retourne : "il y a 5min", "il y a 2h", "hier", "3j", ou date absolue "12 juil."
 */
private fun formatRelativeTime(timestamp: String?, pubDate: String?): String {
    // 1) Essayer le timestamp ISO interne (le plus fiable)
    val date = parseNovaDate(timestamp) ?: parseRfc2822(pubDate) ?: return ""

    val now = System.currentTimeMillis()
    val diff = now - date.time
    if (diff < 0) return ""  // futur (clock skew), on affiche rien

    val min = diff / 60_000
    val hr = diff / 3_600_000
    val day = diff / 86_400_000

    return when {
        min < 1 -> "a l'instant"
        min < 60 -> "il y a ${min}min"
        hr < 24 -> "il y a ${hr}h"
        day < 2 -> "hier"
        day < 7 -> "il y a ${day}j"
        else -> {
            // Date absolue courte : "12 juil."
            val cal = java.util.Calendar.getInstance().apply { time = date }
            val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
            val month = arrayOf(
                "janv.", "fevr.", "mars", "avr.", "mai", "juin",
                "juil.", "aout", "sept.", "oct.", "nov.", "dec."
            )[cal.get(java.util.Calendar.MONTH)]
            "$day $month"
        }
    }
}

private fun parseNovaDate(iso: String?): java.util.Date? {
    if (iso.isNullOrBlank()) return null
    return try {
        // Format: "2026-07-08T08:00:33.152009" (le LLM le fait deja dans l'app)
        val cleaned = iso.replace("Z", "+00:00")
        java.time.OffsetDateTime.parse(cleaned).toInstant().let { java.util.Date.from(it) }
    } catch (e: Exception) {
        try {
            java.time.LocalDateTime.parse(iso).atZone(java.time.ZoneId.systemDefault()).toInstant()
                .let { java.util.Date.from(it) }
        } catch (e2: Exception) {
            null
        }
    }
}

private fun parseRfc2822(rfc: String?): java.util.Date? {
    if (rfc.isNullOrBlank()) return null
    // Format RSS typique: "Wed, 08 Jul 2026 05:52:29 GMT"
    val months = arrayOf(
        "Jan", "Feb", "Mar", "Apr", "May", "Jun",
        "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
    )
    try {
        // SimpleDateFormat thread-safe via synchronized
        val sdf = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
        return sdf.parse(rfc)
    } catch (e: Exception) {
        // Try without leading day name
        return try {
            val parts = rfc.split(",", limit = 2)
            val tail = if (parts.size == 2) parts[1].trim() else rfc
            val sdf = java.text.SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", java.util.Locale.ENGLISH)
            sdf.parse(tail)
        } catch (e2: Exception) {
            null
        }
    }
}

// ─── Onglet Radio ────────────────────────────────────────────────────────────

@Composable
private fun RadioScreen(player: RadioPlayer, adManager: AdManager, isPremium: Boolean) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val isPlaying by player.isPlaying.collectAsState()

    // PAS de DisposableEffect ici : le player est partage avec le RadioService
    // qui tourne en background. Si on release ici, on detruit le MediaController
    // et le play() redevient un no-op quand l'user revient sur l'onglet.
    // Le release se fait dans MainActivity.onDestroy (vraie fin de l'app).

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nova-Atlas",
            color = NovaTextPrimary,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = if (isPremium) "Web Radio IA · Premium ✨" else "Web Radio IA",
            color = NovaTextMuted,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(48.dp))

        // Gros bouton Play/Pause rond
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    if (isPlaying) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else NovaAccentYellow.copy(alpha = 0.2f)
                ),
            contentAlignment = Alignment.Center,
        ) {
            FilledIconButton(
                onClick = {
                    if (isPlaying) {
                        player.pause()
                    } else if (isPremium) {
                        player.play()
                    } else if (activity != null) {
                        adManager.showIfReady(activity) { player.play() }
                    } else {
                        player.play()
                    }
                },
                modifier = Modifier.size(80.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = if (isPlaying) MaterialTheme.colorScheme.primary
                                     else NovaAccentYellow,
                    contentColor = MaterialTheme.colorScheme.background,
                )
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = if (isPlaying) "En cours de lecture" else "Tapez pour lire",
            color = if (isPlaying) NovaAccentGreen else NovaTextMuted,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
