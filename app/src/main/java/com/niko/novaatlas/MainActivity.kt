package com.niko.novaatlas

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.SystemBarStyle
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
        // enableEdgeToEdge : l'app prend tout l'ecran, status/nav bar sont
        // gerees par le contenu Compose (couleur de fond).
        // On force le scrim a transparent (sinon Android 13+ met un gris
        // derriere la status bar qui apparait au-dessus de notre fond noir).
        // SystemBarStyle.dark(Color.TRANSPARENT) = pas de scrim, juste les icones.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )

        // Force la status bar a etre completement transparente avec icones
        // sombres, et SANS le label d'app Android en blanc (sur Android 12+,
        // le systeme affiche le android:label dans la status bar par dessus
        // notre app). On le desactive en mettant la status bar en "low profile"
        // (sans texte, juste les icones systeme).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Icons systeme en blanc (parce que notre fond est sombre),
        // mais on force le mode "transparent" pour que rien d'autre n'apparaisse
        insetsController.isAppearanceLightStatusBars = false
        insetsController.isAppearanceLightNavigationBars = false

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
