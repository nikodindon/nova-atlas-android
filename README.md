# Nova-Atlas Android

Application Android compagnon du projet [Nova-Atlas](../nova-atlas) (moteur de news IA + webradio 24/7).

**Fonctionnalités** :
- 📰 **Fil d'actu** : 50 dernières news, refresh auto toutes les 2s, filtres catégories multi-select, heure relative ("il y a 2h"), résumé expand/collapse au tap
- 📻 **Radio IA** : stream Icecast du serveur, notif media lock screen, contrôle depuis la notif, fond sonore entre les bulletins
- ✨ **Premium** : skip les pubs, badge "Premium ✨" (check serveur anti-triche)
- 💰 **Monétisation** : pub AdMob interstitial au tap Play (compte `nikodindon@gmail.com`)

**Stack technique** :
- Kotlin + Jetpack Compose + Material 3
- minSdk 26 (Android 8.0+) / targetSdk 34 / compileSdk 34
- Retrofit + OkHttp + Kotlinx Serialization (API JSON)
- Media3 / ExoPlayer (radio stream + MediaSessionService pour notif)
- Google Mobile Ads SDK (AdMob)
- Coil (images, prêt pour plus tard)

## Prérequis

- Android Studio (Ladybug | 2024.2.1+)
- JDK 17
- Android SDK 34 (platform-tools, build-tools, platform 34)
- KVM activé (émulateur rapide) ou téléphone Android en USB

## Premier lancement

```bash
git clone <ce-repo>
cd nova-atlas-android
# Ouvrir dans Android Studio
# Run > Run 'app' (sélectionner un device)
```

L'app pointe par défaut vers `http://192.168.1.22:5055/` (Flask) et `http://192.168.1.22:8000/nova` (Icecast). Pour changer : éditer les `buildConfigField` dans `app/build.gradle.kts` (lignes 22-26) puis rebuild.

## Architecture

```
app/src/main/java/com/niko/novaatlas/
├── MainActivity.kt          # Bootstrap (init AdMob, Connect player, setContent)
├── AppScreen.kt             # UI principale (2 onglets : Feed + Radio)
├── RadioPlayer.kt           # Client MediaController (proxy du RadioService)
├── RadioService.kt          # MediaSessionService (ExoPlayer en background)
├── AdManager.kt             # Interstitial AdMob + rate limit 5min
├── SubscriptionManager.kt   # Check premium serveur (ANDROID_ID, StateFlow)
├── ApiClient.kt             # Singleton Retrofit + data classes JSON
├── Article.kt               # Data class Article
└── ui/theme/
    ├── Color.kt             # Palette Nova-Atlas (alignée sur site Flask)
    └── Theme.kt             # darkColorScheme custom, dynamicColor désactivé
```

**Flow radio** : `MainActivity` crée un `RadioPlayer` (client) qui se connecte au `RadioService` (déclaré dans le manifest) via `SessionToken` + `MediaController`. Le service tient l'`ExoPlayer` en vie même quand l'activity est fermée. La notif media est auto-générée par `MediaSession`.

**Flow premium** : `SubscriptionManager.refresh()` hit `GET /api/subscription/status?device_id=<ANDROID_ID>`. Le serveur lit `data/subscriptions.json` (gitignoré) et renvoie `is_premium`. L'UI observe le `StateFlow` et skip la pub si true.

## Build release

### 1. Créer la keystore (une seule fois)

```bash
cd ~/AndroidStudioProjects/NovaAtlas
mkdir -p keystore
keytool -genkey -v -keystore keystore/nova-atlas-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nova-atlas
# Te demandera : mot de passe keystore + ton nom/orga/ville
# => 2 mots de passe à noter (storePassword + keyPassword)
```

**⚠️ IMPORTANT** :
- Le fichier `keystore/nova-atlas-release.jks` est **gitignoré** (`.gitignore` ligne "keystore/")
- **Ne JAMAIS commit** la keystore (si tu la perds, tu peux plus updater l'app sur le Play Store — il faut créer un nouveau package)
- **Note les 2 mots de passe** dans un password manager

### 2. Set les variables d'environnement

```bash
# Ajouter dans ~/.bashrc ou ~/.zshrc :
export RELEASE_STORE_PASSWORD="ton-store-password"
export RELEASE_KEY_PASSWORD="ton-key-password"
export RELEASE_KEY_ALIAS="nova-atlas"
```

### 3. Build l'AAB (pour le Play Store)

```bash
./gradlew bundleRelease
# Sortie : app/build/outputs/bundle/release/app-release.aab
```

C'est ce `.aab` que tu uploades sur la Google Play Console.

### 4. Build l'APK (pour tester en local ou sideload)

```bash
./gradlew assembleRelease
# Sortie : app/build/outputs/apk/release/app-release.apk
```

### Vérifier que R8 a bien shrink

L'APK release doit faire **~6 Mo** (vs 23 Mo en debug = -74% via R8). Si c'est plus gros, R8 a pas viré assez de code mort, vérifier les `keep` rules dans `proguard-rules.pro`.

## Monétisation (AdMob)

- **Compte** : `nikodindon@gmail.com` (AdMob)
- **App ID** : `ca-app-pub-2776142788958553~6881853090`
- **Ad Unit ID** : `ca-app-pub-2776142788958553/2751036399` (radio_play_interstitial)
- **Compte Google Play Console** (à créer, 25$ one-time) : `play.google.com/console`

⚠️ Pour publier, faut ajouter une **privacy policy** (URL publique obligatoire). Voir `docs/privacy-policy.md` (à venir).

## Tests

```bash
./gradlew test              # Unit tests JVM
./gradlew connectedAndroidTest  # Tests instrumentés (besoin d'un device/emu)
```

## Dépendances principales (versions dans `gradle/libs.versions.toml`)

| Lib | Version | Usage |
|---|---|---|
| Compose BOM | 2024.04.01 | UI |
| Material 3 | (via BOM) | Composants UI |
| Material icons extended | (via BOM) | Icônes (Pause, Radio, etc.) |
| Retrofit | 2.11.0 | HTTP client |
| OkHttp | 4.12.0 | HTTP bas niveau |
| Kotlinx Serialization | 1.6.3 | JSON |
| Media3 / ExoPlayer | 1.4.1 | Radio stream |
| Coil | 2.6.0 | Images (futur) |
| Google Mobile Ads | 23.5.0 | AdMob |
| AGP | 8.7.3 | Android Gradle Plugin |
| Kotlin | 2.0.0 | Langage |

## Crédits

- Le code suit les conventions du projet `nova-atlas` (commits atomiques, 1 sprint = 1 commit, code review avant push)
- Le thème est aligné sur le site Flask du projet principal (palette extraite de `site/static/css/`)
- L'icône de l'app est le logo Material par défaut (à custom Sprint E2 futur)

## Roadmap

- [ ] Sprint E3 (publish) : Play Console, keystore, privacy policy, upload AAB
- [ ] Sprint F (premium UI) : écran "Passer Premium" + Google Play Billing
- [ ] Sprint G (notifications) : "Breaking news" notif pour les cat importantes
- [ ] Sprint H (offline) : cache local des news (Room), écoute radio offline
- [ ] Sprint I (i18n) : EN/FR switchable
