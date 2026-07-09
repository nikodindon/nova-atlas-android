package com.niko.novaatlas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.google.android.gms.ads.MobileAds
import com.niko.novaatlas.ui.theme.NovaAtlasTheme

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var player: RadioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen : doit etre appele AVANT super.onCreate
        // Affiche notre logo + fond noir pendant que l'app demarre, puis
        // transition smooth vers le vrai ecran Compose.
        val splashScreen = installSplashScreen()
        // Optionnel : garder le splash visible jusqu'a ce qu'une condition soit
        // vraie (ex: donnees chargees). Ici on le kick direct, le splash dure ~200ms.
        splashScreen.setKeepOnScreenCondition { false }

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
