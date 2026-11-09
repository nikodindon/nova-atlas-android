import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val environment = providers.gradleProperty("ATLAS_ENV").getOrElse("isolated")
require(environment in listOf("isolated", "preproduction", "release"))
val origin = providers.gradleProperty("ATLAS_ORIGIN").getOrElse("https://example.invalid/")

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

        manifestPlaceholders["ADMOB_APP_ID"] = "ca-app-pub-3940256099942544~3347511713"
    }
    val radio = providers.gradleProperty("ATLAS_RADIO").getOrElse("https://example.invalid/stream/nova-android")
    fun checked(value: String): String {
        val uri = URI(value)
        require(uri.scheme == "https" && uri.host != null && uri.rawUserInfo == null && uri.rawQuery == null && uri.rawFragment == null) { "Explicit HTTPS endpoint required" }
        require(value.none { it == '"' || it == '\\' || it.code < 32 })
        return value
    }
    checked(origin); checked(radio)
    require(URI(origin).path in listOf("", "/")) { "ATLAS_ORIGIN must be an origin" }
    signingConfigs {
        create("release") {
            val path = providers.environmentVariable("RELEASE_KEYSTORE_PATH").orNull
            if (path != null) storeFile = file(path)
            storePassword = providers.environmentVariable("RELEASE_STORE_PASSWORD").orNull
            keyAlias = providers.environmentVariable("RELEASE_KEY_ALIAS").orNull
            keyPassword = providers.environmentVariable("RELEASE_KEY_PASSWORD").orNull
        }
    }
    buildTypes {
        all {
            buildConfigField("String", "API_ORIGIN", "\"${origin.trimEnd('/')}/\"")
            buildConfigField("String", "RADIO_STREAM_URL", "\"$radio\"")
            buildConfigField("String", "ENVIRONMENT", "\"$environment\"")
            buildConfigField("boolean", "ADS_ENABLED", "false")
            buildConfigField("boolean", "PREMIUM_ENABLED", "false")
        }
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
    implementation("androidx.compose.material3:material3:1.2.1")
    // Icones Material etendues (Pause, Radio, etc. - le core n'a que ~5 icones)
    implementation("androidx.compose.material:material-icons-extended")
    // Splash screen API (Android 12+) - evite l'ecran blanc au demarrage
    implementation("androidx.core:core-splashscreen:1.0.1")
    testImplementation(libs.junit)
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
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
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")
    implementation(libs.media3.ui)
    implementation(libs.media3.session)

    // Nova-Atlas - images
    implementation(libs.coil.compose)

    // Nova-Atlas - monétisation (AdMob)
    implementation(libs.admob)
}

// No debug-signing fallback, no key generation, no upload task.
gradle.taskGraph.whenReady {
    if (allTasks.any { it.name.contains("Release", ignoreCase = true) }) {
        val required = listOf("RELEASE_KEYSTORE_PATH", "RELEASE_STORE_PASSWORD", "RELEASE_KEY_ALIAS", "RELEASE_KEY_PASSWORD")
        require(required.all { !System.getenv(it).isNullOrBlank() }) { "Release requires explicit private signing configuration" }
        require(file(System.getenv("RELEASE_KEYSTORE_PATH")).isFile) { "Release keystore missing" }
        require(environment == "release") { "Release requires ATLAS_ENV=release" }
        require(!origin.contains("example.invalid")) { "Release requires explicit server configuration" }
    }
}
