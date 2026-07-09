package com.niko.novaatlas

import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.net.HttpURLConnection
import java.net.URL

/**
 * Decouverte reseau : detecte si le LAN repond au demarrage de l'app.
 *
 * Pourquoi : sur le LAN, le serveur repond en 1-2ms (192.168.1.22:5055).
 * Sur la 4G, il faut passer par Cloudflare Tunnel (200-500ms).
 * On teste 1 fois au mount, on garde le resultat, et toutes les requetes
 * suivantes utilisent le bon chemin. Pas de double latence, pas de
 * timeout a chaque requete.
 *
 * Logique :
 * 1. Test une requete HEAD rapide (800ms timeout) sur le LAN Flask
 * 2. Si reponse 2xx/3xx/4xx (mauvais chemin mais serveur OK) -> useLocal = true
 *    (en LAN, 4xx veut dire "route inconnue" mais Flask repond, c'est OK)
 * 3. Si timeout/erreur reseau -> useLocal = false (fallback public)
 *
 * Le resultat est expose via un StateFlow pour que l'UI puisse reactualiser
 * ses appels REST (par ex. un refresh manuel apres switch de reseau).
 */
object NetworkDiscovery {
    private const val TAG = "NetworkDiscovery"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _useLocal = MutableStateFlow(false)
    val useLocal: StateFlow<Boolean> = _useLocal.asStateFlow()

    /**
     * Teste le LAN en arriere-plan. Ne bloque pas.
     * Met a jour _useLocal selon le resultat.
     */
    fun probe() {
        scope.launch {
            val lanUrl = "http://${BuildConfig.LAN_HOST}:${BuildConfig.LAN_PORT_FLASK}/"
            val reachable = withTimeoutOrNull(800) {
                try {
                    val conn = (URL(lanUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 600
                        readTimeout = 600
                        requestMethod = "GET"
                        // On suit pas les redirections, on veut juste la reponse brute
                        instanceFollowRedirects = false
                    }
                    val code = conn.responseCode
                    conn.disconnect()
                    // 2xx, 3xx, 4xx = le serveur repond (meme si route inconnue, c'est OK
                    // pour le test : on sait que Flask est la)
                    // 5xx = serveur en erreur, on considere comme "pas joignable"
                    code in 200..499
                } catch (e: Exception) {
                    Log.d(TAG, "LAN probe failed: ${e.javaClass.simpleName}")
                    false
                }
            } ?: run {
                Log.d(TAG, "LAN probe timeout (800ms)")
                false
            }
            Log.d(TAG, "useLocal = $reachable (LAN $lanUrl)")
            _useLocal.value = reachable
        }
    }
}
