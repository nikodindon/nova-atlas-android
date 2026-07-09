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

L'app pointe par défaut vers `http://192.168.1.22:5055/` (Flask) et `http://192.168.1.22:8000/nova-android` (Icecast, mount bulletins-only sans musique libre de droits pour le Play Store). Pour changer : éditer les `buildConfigField` dans `app/build.gradle.kts` (lignes 22-26) puis rebuild.

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

## Publier sur le Play Store

Une fois que t'as un compte Google Play Console (25$ one-time sur https://play.google.com/console), voici les 4 étapes pour publier :

### 1. Créer la keystore (une seule fois, JAMAIS à refaire)

```bash
cd ~/AndroidStudioProjects/NovaAtlas
mkdir -p keystore
keytool -genkey -v -keystore keystore/nova-atlas-release.jks \
  -keyalg RSA -keysize 2048 -validity 10000 \
  -alias nova-atlas
# Te demandera : mot de passe keystore + ton nom/orga/ville
# => 2 mots de passe à noter (storePassword + keyPassword)
```

⚠️ Si tu perds cette keystore, tu peux plus jamais updater l'app sur le Play Store. **Note les 2 mots de passe** dans un password manager + **backup le fichier `.jks`** sur un disque externe.

### 2. Set les variables d'environnement

Ajoute à `~/.bashrc` ou `~/.zshrc` :

```bash
export RELEASE_STORE_PASSWORD="ton-store-password"
export RELEASE_KEY_PASSWORD="ton-key-password"
export RELEASE_KEY_ALIAS="nova-atlas"
```

Puis `source ~/.bashrc` pour les activer dans le terminal courant.

### 3. Build l'AAB (Android App Bundle)

```bash
cd ~/AndroidStudioProjects/NovaAtlas
./gradlew bundleRelease
```

L'AAB est dans `app/build/outputs/bundle/release/app-release.aab` (~11 Mo avec R8 shrink). C'est ce fichier que tu uploades sur la Play Console, **pas l'APK**.

### 4. Upload sur la Play Console

1. https://play.google.com/console → ton app → **Production** (ou "Tests internes" pour un pre-publish)
2. **Create new release** → Upload l'AAB
3. Remplir la fiche :
   - **App name** : Nova-Atlas
   - **Short description** : "AI-powered news feed + 24/7 web radio"
   - **Full description** : (à toi d'écrire, ou inspire-toi du README)
   - **Screenshots** : 2-8 captures d'écran (1080x1920 minimum)
   - **App icon** : 512x512 PNG (utilise l'icône Nova-Atlas que t'as générée, redimensionnée)
   - **Feature graphic** : 1024x500 PNG (optionnel mais recommandé)
   - **Privacy policy URL** : `https://nikodindon.github.io/nova-atlas-android/`
   - **Category** : News & Magazines
   - **Content rating** : remplir le questionnaire (c'est rapide)
4. **Pricing & distribution** : gratuit, tous pays
5. **Submit for review**

Le review Google prend 1-7 jours. Si pas de souci, l'app est live sur le Play Store.

### 5. Pour updater plus tard (v1.0.1, v1.1.0, etc.)

1. Bumper `versionCode` (1 → 2) ET `versionName` (1.0.0 → 1.0.1) dans `app/build.gradle.kts`
2. `./gradlew bundleRelease`
3. Upload le nouvel AAB sur la console (même package, même keystore)
4. Review ~1 jour

⚠️ **Ne JAMAIS changer le `applicationId`** entre 2 versions (= `com.niko.novaatlas` forever), sinon Google considère que c'est une nouvelle app.
