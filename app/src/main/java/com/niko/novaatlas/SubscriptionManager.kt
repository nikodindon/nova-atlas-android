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
import kotlinx.coroutines.cancel
import kotlinx.coroutines.CancellationException

/** Legacy status adapter, disabled by default. ANDROID_ID is not purchase proof.
 * Any future billing requires verified server-side entitlements and consent review.
 */
class SubscriptionManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val deviceId: String by lazy {
        // Scoped installation signal only; reset/signature/user changes can change it.
        @Suppress("HardwareIds")
        Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown-device"
    }

    fun close() { scope.cancel() }

    private companion object {
        const val TAG = "SubscriptionManager"
    }

    /**
     * Fetch le statut premium depuis le serveur. Appelle ca au mount de l'UI
     * + apres chaque achat reussi.
     */
    fun refresh() {
        if (!BuildConfig.PREMIUM_ENABLED) return
        scope.launch {
            try {
                val response = ApiClient.api.getSubscriptionStatus(deviceId)
                _isPremium.value = response.is_premium
                Log.d(TAG, "Premium observation updated or unavailable")
            } catch (e: CancellationException) { throw e
            } catch (e: Exception) {
                // En cas d'erreur reseau, on reste sur la valeur precedente
                // (defaut: false). Pas grave, on retentera au prochain refresh.
                Log.d(TAG, "Premium observation updated or unavailable")
            }
        }
    }
}
