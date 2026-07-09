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
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // IP du serveur Nova-Atlas sur le LAN. Changeable ici ou via shared prefs plus tard.
        buildConfigField("String", "SERVER_HOST", "\"192.168.1.22\"")
        // Flask tourne sur 5055, Icecast sur 8000 (mount /nova)
        buildConfigField("String", "SERVER_PORT_FLASK", "\"5055\"")
        buildConfigField("String", "SERVER_PORT_ICECAST", "\"8000\"")
        buildConfigField("String", "ICECAST_MOUNT", "\"/nova\"")
        // AdMob test app ID (Google fournit des IDs de test, OBLIGATOIRE en dev).
        // On remplacera par le vrai ID quand tu auras créé le compte AdMob.
        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
