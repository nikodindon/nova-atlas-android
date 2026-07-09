# Nova-Atlas - regles R8 / ProGuard
# Doc: https://developer.android.com/build/shrink-code

# ── Conserver les attributs utiles au debug ───────────────────────────────────
# Sans ca, les stack traces des crashs reports disent "a.b.c.method()" sans
# ligne. Avec, on a le mapping pour reconstruire.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── Kotlinx Serialization (data classes @Serializable) ──────────────────────
# Necessaire : Serialization genere des$KSerializer au runtime via reflection.
# Sans ces regles, R8 vire les serializers et on a des crashs genre
# "Serializer for class X is not found".
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault
-keep,includedescriptorclasses class **$$serializer { *; }
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Retrofit ─────────────────────────────────────────────────────────────────
# Retrofit genere des proxys dynamiques pour les interfaces API. Sans ca, R8
# vire les methodes de l'interface NovaAtlasApi et on a des 404 sur les endpoints.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking interface kotlin.coroutines.Continuation
-keep class com.niko.novaatlas.NovaAtlasApi { *; }
-keep class com.niko.novaatlas.Article { *; }
-keep class com.niko.novaatlas.ArticlesResponse { *; }
-keep class com.niko.novaatlas.SubscriptionStatusResponse { *; }

# ── AdMob / Google Mobile Ads ────────────────────────────────────────────────
# Google fournit des regles officielles, voir :
# https://developers.google.com/admob/android/guides/proguard
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.android.gms.** { *; }

# API 31+ (Android 12+), notre minSdk est 26 : AdMob reference ces classes
# via reflection mais elles sont juste du "bonus loudness" (normalize audio
# des pubs). On dit a R8 de pas gueuler si elles sont absentes.
-dontwarn android.media.LoudnessCodecController
-dontwarn android.media.LoudnessCodecController$OnLoudnessCodecUpdateListener

# ── Media3 / ExoPlayer ──────────────────────────────────────────────────────
# Media3 utilise des listeners et des annotations qui peuvent etre virees.
-keep class androidx.media3.** { *; }
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.session.** { *; }

# ── Compose ──────────────────────────────────────────────────────────────────
# Compose genere beaucoup de code dynamique (lambdas, slot tables). Sans ces
# regles, R8 casse l'UI de maniere cryptique.
-keep class androidx.compose.runtime.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# ── Kotlinx Coroutines ──────────────────────────────────────────────────────
# Le Dispatchers.IO et autres internals sont sensibles a R8.
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Notre code specifique ───────────────────────────────────────────────────
# RadioService est referencee dans le Manifest et instanciee par le systeme.
# Sans -keep, R8 peut renommer la classe et le systeme la trouve pas.
-keep class com.niko.novaatlas.RadioService { *; }
-keep class com.niko.novaatlas.MainActivity { *; }
