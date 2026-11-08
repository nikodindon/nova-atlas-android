# Nova Atlas Android Companion V2

Candidat de revue privé pour Nova Atlas V2 : actualités, catégories dynamiques,
événements multi-sources, illustrations approuvées et radio Media3 en arrière-plan.
Serveur toujours NO-GO exploitation publique qualifiée. Aucune publication Play.

JDK17, Gradle8.9/AGP8.7.3, SDK34, minSdk26. ApplicationId et version inchangés.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Par défaut, les endpoints sont `https://example.invalid/` : aucune installation
réelle contactée. Configurer explicitement un serveur HTTPS isolé :

```sh
./gradlew assembleDebug -PATLAS_ENV=isolated \
  -PATLAS_ORIGIN=https://atlas-test.example.invalid/ \
  -PATLAS_RADIO=https://atlas-test.example.invalid/stream/nova-android
```

Actualisation au premier plan toutes les 60 secondes, manuelle, sans requêtes
simultanées. API V2 avec fallback legacy contrôlé. Publicité et Premium désactivés
pour les essais ; aucun identifiant de paiement probant déduit d'ANDROID_ID.
Release refusée sans configuration explicite et signature privée ; aucun fallback
debug ni upload automatique.

Voir [le dossier d'intégration](docs/ANDROID_V2_INTEGRATION.md) pour les commits de
référence, contrats réels, sécurité, limites radio, résultats CI, matrice appareils,
configuration de signature, confidentialité et rollback. Les tests JVM ne constituent
pas une validation smartphone. Tests composants instrumentés :
`./gradlew connectedDebugAndroidTest` sur émulateur/appareil de test explicite.
