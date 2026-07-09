package com.niko.novaatlas

import android.content.Context
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Gestion de l'abonnement premium cote app.
 *
 * - device_id : identifiant stable du tel (Settings.Secure.ANDROID_ID).
 *   Pas un truc lie au compte Google, reste apres reinstall et apres
 *   factory reset (en general). C'est le minimum pour notre cas.
 * - isPremium : StateFlow que l'UI observe pour skip les pubs.
 * - refresh() : a appeler au mount + quand l'user revient d'un achat.
 */
class SubscriptionManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val deviceId: String by lazy {
        // ANDROID_ID est unique par app+device, reset au factory reset mais pas
        // a la reinstall. Suffisant pour identifier un abonne de maniere stable.
        @Suppress("HardwareIds")
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }

    private companion object {
        const val TAG = "SubscriptionManager"
    }

    /**
     * Fetch le statut premium depuis le serveur. Appelle ca au mount de l'UI
     * + apres chaque achat reussi.
     */
    fun refresh() {
        scope.launch {
            try {
                val response = ApiClient.api.getSubscriptionStatus(deviceId)
                _isPremium.value = response.is_premium
                Log.d(TAG, "Premium status: ${response.is_premium} (expires=${response.expires_at})")
            } catch (e: Exception) {
                // En cas d'erreur reseau, on reste sur la valeur precedente
                // (defaut: false). Pas grave, on retentera au prochain refresh.
                Log.d(TAG, "Refresh premium failed: ${e.message}")
            }
        }
    }
}
