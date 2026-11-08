package com.niko.novaatlas

import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.CancellationException
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.CachePolicy
import android.content.Intent
import android.net.Uri

@Composable fun ApprovedImage(image: PublicImage?) {
    val path = SafeLinks.image(image)
    var failed by remember(path) { mutableStateOf(false) }
    if (path == null || failed) { Text("◈ Illustration indisponible"); return }
    val context = LocalContext.current
    // Revocable media: no persistent image cache, no cross-origin redirect, bounded bytes.
    val loader = remember { coil.ImageLoader.Builder(context).okHttpClient(ApiClient.client()).build() }
    DisposableEffect(loader) { onDispose { loader.shutdown() } }
    AsyncImage(model = ImageRequest.Builder(context).data(BuildConfig.API_ORIGIN.trimEnd('/')+path)
        .diskCachePolicy(CachePolicy.DISABLED).memoryCachePolicy(CachePolicy.DISABLED).size(768,432).build(),
        imageLoader=loader, contentDescription=image?.caption, onError={ failed=true }, modifier=Modifier.fillMaxWidth().height(160.dp))
    Text(image?.caption ?: "")
    image?.attribution?.let { a ->
        Text("${a.author} · ${a.credit} · ${a.license}")
        Text(a.changes)
        listOf("Source" to a.source,"Licence" to a.license_url).forEach { (label,url) ->
            SafeLinks.external(url)?.let { safe -> TextButton(onClick={ runCatching { context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse(safe))) } }) { Text(label) } }
        }
    }
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun EventsScreen() {
    var result by remember { mutableStateOf<EventsResponse?>(null) }
    var selected by remember { mutableStateOf<EditorialEvent?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(selected?.event_id) {
        val id = selected?.event_id ?: return@LaunchedEffect
        try { selected=ApiClient.api.event(id).also { require(it.status == "ok" && it.event.event_id == id) }.event } catch (e: CancellationException) { throw e } catch (_: Exception) { error="Détail indisponible — dernière observation affichée" }
    }
    val owner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    LaunchedEffect(owner) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            try { result=ApiClient.api.events().also { require(it.status == "ok" && it.events.size <= 20) }; error=null }
            catch (e: CancellationException) { throw e }
            catch (_: Exception) { error="Événements indisponibles" }
        }
    }
    LazyColumn(contentPadding=PaddingValues(16.dp)) {
        item { Text("Événements",style=MaterialTheme.typography.headlineMedium)
            Text("Plusieurs couvertures ne constituent pas une confirmation indépendante.")
            error?.let { Text(it) }
            if (result?.index_available == false) Text("Index événementiel indisponible")
            if (result?.events?.isEmpty() == true) Text("Aucun événement")
        }
        selected?.let { event ->
            item { TextButton(onClick={selected=null}) { Text("Retour") }; Text(event.title); ApprovedImage(event.image) }
            items(event.articles) { article -> ArticleCard(Article(article.article_hash,"",article.category,article.title,link=article.link,source=article.source,pub_date=article.published_raw)) }
        } ?: items(result?.events ?: emptyList()) { event ->
            Card(onClick={selected=event},modifier=Modifier.fillMaxWidth().padding(vertical=6.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(event.title); Text("${event.article_count} articles · ${event.domain_count} domaines")
                    Text(event.last_updated_at ?: "Date inconnue")
                }
            }
        }
    }
}
