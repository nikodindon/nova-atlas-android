package com.niko.novaatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.ads.MobileAds
import com.niko.novaatlas.ui.theme.NovaAtlasTheme

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var player: RadioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Init AdMob SDK le plus tot possible
        MobileAds.initialize(this) {}
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        adManager = AdManager(this)
        subscriptionManager = SubscriptionManager(this)
        player = RadioPlayer(this)

        // Refresh premium au mount (en // du demarrage UI)
        subscriptionManager.refresh()
        // Connect au RadioService (en //, le service demarre quand l'UI le demande)
        player.connect()

        setContent {
            NovaAtlasTheme {
                AppScreen(
                    player = player,
                    adManager = adManager,
                    subscriptionManager = subscriptionManager,
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        player.release()
    }
}
