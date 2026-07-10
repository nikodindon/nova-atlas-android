package com.niko.novaatlas

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.android.gms.ads.MobileAds
import com.niko.novaatlas.ui.theme.NovaAtlasTheme

class MainActivity : ComponentActivity() {

    private lateinit var adManager: AdManager
    private lateinit var subscriptionManager: SubscriptionManager
    private lateinit var player: RadioPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { false }

        MobileAds.initialize(this) {}
        super.onCreate(savedInstanceState)

        // Edge-to-edge avec status bar noire opaque (scrim noir).
        // Les icônes système restent blanches mais invisibles - l'utilisateur
        // doit swipe pour les faire apparaître (BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK

        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

        adManager = AdManager(this)
        subscriptionManager = SubscriptionManager(this)
        player = RadioPlayer(this)

        subscriptionManager.refresh()
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