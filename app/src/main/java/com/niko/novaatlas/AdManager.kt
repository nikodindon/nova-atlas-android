package com.niko.novaatlas

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.atomic.AtomicLong

/**
 * Gestionnaire de pub interstitial AdMob.
 *
 - Une seule pub a la fois en memoire (la "preloaded ad")
 - On en charge une nouvelle des qu'une est affichee/fermee
 - Rate limit : 1 pub MAX toutes les [minIntervalMs] ms (defaut 5 min)
 - Si pas de pub prete OU rate limit pas respecte, on appelle onComplete
   direct (la radio demarre sans pub, on n'a pas coupe l'utilisateur)
 */
class AdManager(private val context: Context) {

    private val adUnitId: String = AD_UNIT_ID
    private val minIntervalMs: Long = 5 * 60 * 1000L  // 5 min

    private var preloadedAd: InterstitialAd? = null
    private val lastShownMs = AtomicLong(0L)

    init {
        loadAd()
    }

    private fun loadAd() {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    preloadedAd = ad
                    Log.d(TAG, "Pub interstitial prete")
                }

                override fun onAdFailedToLoad(adError: LoadAdError) {
                    preloadedAd = null
                    // Pas grave, on retentira a la prochaine demande
                    Log.d(TAG, "Echec chargement pub: ${adError.message}")
                }
            }
        )
    }

    /**
     * Affiche la pub si prete + rate limit respecte. Appelle onComplete()
     * systematiquement, que la pub soit affichee, fermee, ou skippee.
     */
    fun showIfReady(activity: Activity, onComplete: () -> Unit) {
        val ad = preloadedAd
        val now = System.currentTimeMillis()
        val elapsed = now - lastShownMs.get()

        val rateOk = elapsed >= minIntervalMs
        if (ad == null || !rateOk) {
            // Pas de pub dispo OU trop tot depuis la derniere : on stream direct
            onComplete()
            return
        }

        ad.fullScreenContentCallback = object : com.google.android.gms.ads.FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                // Pub fermee par l'utilisateur : on stream la radio
                preloadedAd = null
                lastShownMs.set(System.currentTimeMillis())
                loadAd()  // precharger la suivante
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                // Echec affichage (rare) : on stream quand meme
                preloadedAd = null
                onComplete()
            }
        }

        ad.show(activity)
    }

    companion object {
        private const val TAG = "AdManager"
        // Ad Unit ID du compte niko (nikodindon@gmail.com), bloc radio_play_interstitial
        // Cree le 2026-07-09 dans l'app Nova-Atlas.
        private const val AD_UNIT_ID = "ca-app-pub-2776142788958553/2751036399"
    }
}
