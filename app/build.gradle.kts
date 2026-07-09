plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.niko.novaatlas"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.niko.novaatlas"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AdMob App ID (compte niko - nikodindon@gmail.com).
        // Reconnu auto en mode debug, sert pour la prod une fois l'app signee release.
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-2776142788958553~6881853090"
    }

    // Keystore : gitignoré. Pour creer ta propre keystore, voir README.md.
    // La signature release est conditionnelle : si la keystore n'existe pas,
    // on fallback sur debug (utile pour tester un release local avant publish).
    signingConfigs {
        create("release") {
            val keystorePath = rootProject.file("keystore/nova-atlas-release.jks")
            if (keystorePath.exists()) {
                storeFile = keystorePath
                storePassword = (project.findProperty("RELEASE_STORE_PASSWORD") as String?)
                    ?: System.getenv("RELEASE_STORE_PASSWORD") ?: ""
                keyAlias = (project.findProperty("RELEASE_KEY_ALIAS") as String?)
                    ?: System.getenv("RELEASE_KEY_ALIAS") ?: "nova-atlas"
                keyPassword = (project.findProperty("RELEASE_KEY_PASSWORD") as String?)
                    ?: System.getenv("RELEASE_KEY_PASSWORD") ?: ""
            }
            // Sinon : on laisse la config "release" vide, on tombera sur debugSigning
        }
    }

    // URLs differentes selon le build type :
    // - debug : LAN HTTP (192.168.1.22, ton serveur a la maison)
    // - release : HTTPS prod via Cloudflare Tunnel (sous-domaine public)
    // C'est 1 seul BuildConfig.SERVER_HOST que ApiClient et RadioPlayer lisent.
    buildTypes {
        debug {
            // LAN : ton serveur a la maison (rapide quand t'es chez toi)
            buildConfigField("String", "LAN_HOST", "\"192.168.1.22\"")
            buildConfigField("String", "LAN_PORT_FLASK", "\"5055\"")
            buildConfigField("String", "LAN_RADIO_URL", "\"http://192.168.1.22:8000/nova-android\"")
            // Public : Cloudflare Tunnel (marche en 4G / depuis n'importe ou)
            buildConfigField("String", "PUBLIC_HOST", "\"nova-atlas.nikodindon.dpdns.org\"")
            buildConfigField("String", "PUBLIC_PORT_FLASK", "\"443\"")
            buildConfigField("String", "PUBLIC_RADIO_URL", "\"https://nova-atlas-radio.nikodindon.dpdns.org/nova-android\"")
        }
        release {
            // Meme config en release (le NetworkDiscovery choisit dynamiquement)
            buildConfigField("String", "LAN_HOST", "\"192.168.1.22\"")
            buildConfigField("String", "LAN_PORT_FLASK", "\"5055\"")
            buildConfigField("String", "LAN_RADIO_URL", "\"http://192.168.1.22:8000/nova-android\"")
            buildConfigField("String", "PUBLIC_HOST", "\"nova-atlas.nikodindon.dpdns.org\"")
            buildConfigField("String", "PUBLIC_PORT_FLASK", "\"443\"")
            buildConfigField("String", "PUBLIC_RADIO_URL", "\"https://nova-atlas-radio.nikodindon.dpdns.org/nova-android\"")

            // Si une vraie keystore est dispo, on l'utilise. Sinon fallback debug
            // (pratique pour tester assembleRelease sans creds, mais INTERDIT pour
            // publier sur le Play Store - il refusera un APK signe debug).
            signingConfig = if (signingConfigs.findByName("release")?.storeFile != null
                && signingConfigs.findByName("release")?.storeFile?.exists() == true
            ) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }

            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    // Icones Material etendues (Pause, Radio, etc. - le core n'a que ~5 icones)
    implementation("androidx.compose.material:material-icons-extended")
    // Splash screen API (Android 12+) - evite l'ecran blanc au demarrage
    implementation("androidx.core:core-splashscreen:1.0.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    // Nova-Atlas - réseau (Retrofit + OkHttp + Kotlinx Serialization)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // Nova-Atlas - player radio (Media3 / ExoPlayer)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Nova-Atlas - images
    implementation(libs.coil.compose)

    // Nova-Atlas - monétisation (AdMob)
    implementation(libs.admob)
}
