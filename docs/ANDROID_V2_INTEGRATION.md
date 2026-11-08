# Android Companion V2 — Sprint 14

Statut : candidat de revue, pas de publication Google Play. La RC serveur reste
**NO-GO exploitation publique qualifiée**. Aucun compte publicitaire, paiement,
backend réel, modèle ou radio Internet utilisé dans les tests.

## Références et baseline

Serveur `0e8752622bbbe16cf9b3b187bb7d69a9b1e4533b` (dev après fusion autorisée
PR21, dix contrôles réussis avant fusion). Android de départ
`d6fd28d2b3a3b787f13d6bc042d259d66ced2740` ; branche
`feature/v2-android-companion`. Le SHA livré est celui de la PR Android ; aucun
changement serveur requis. Plan serveur non suivi conservé inchangé.

Versions conservées : Gradle 8.9 (checksum officiel ajouté), AGP 8.7.3, Kotlin 2.0.0,
Compose BOM 2024.04.01, Retrofit 2.11.0, OkHttp 4.12.0, sérialisation 1.6.3,
Media3 1.4.1, Coil 2.6.0, Ads 23.5.0. Coroutines 1.8.1/test et MockWebServer 4.12.0
explicités ; Media3 datasource OkHttp ajouté. Material3 1.2.1 explicité car le code
historique utilise HorizontalDivider. ApplicationId et version 1/1.0.0 inchangés.

Baseline locale : tentative `testDebugUnitTest assembleDebug --offline` impossible :
Java 8 seul, SDK/adb absents, distribution Gradle non mise en cache. Aucun résultat
JVM ou APK initial réussi revendiqué. Deux reproductions hors ligne ont confirmé
le code de polling et le fallback de signature ; modèle temporel : quatre admissions
en sept secondes pour une requête lente. `tools/baseline_reproduction.py` conserve
cette reproduction historique (à exécuter sur la révision de départ, pas la branche corrigée).

## Contrats croisés

`app/src/test/resources/contracts/` contient les JSON réellement produits par le
client Flask de test : legacy articles, public site/articles/events/event/archives,
subscription, approved-articles. Données synthétiques, Unicode/HTML hostile inerte,
miniature géométrique locale approuvée ; aucune image de presse téléchargée.
`provenance.json` et `sha256.json` fixent origine et empreintes. Outils de régénération
pytest dans `tools/export_server*.py`, exécutés avec PYTHONPATH et l'environnement du
checkout serveur exact, chemins temporaires des fixtures serveur exclusivement.

| Contrat | Consommateur / comportement |
|---|---|
| `/api/articles` | Fallback uniquement si public/articles répond 404 ou 405 ; contrat inchangé |
| `/api/subscription/status` | Modèle conservé/testé ; appels désactivés par défaut |
| `/api/public/site` | Catégories (id/name/icon/order) et chemins streams connus |
| `/api/public/articles` | Première page de 50, projection textuelle et image approuvée |
| `/api/public/events` et `/<id>` | Liste bornée 20, détail, articles, domaines, dates ; index absent explicite |
| `/api/public/archives` | Fixture conservée ; écran archives Android reporté |
| `/media/images/...` | Même origine, attribution, pas de téléchargement candidat privé |
| `/stream/...` | Chemins publics sûrs ; configuration radio explicite historique conservée |

Le nombre de domaines n'est pas une confirmation indépendante. Aucun regroupement
ou reconstruction côté Android. Les hashes et articles restent inchangés.

## Environnements et transport

Build par défaut : `https://example.invalid/` et radio sur ce domaine réservé.
Aucune connexion implicite à l'installation historique. Compiler explicitement :

```sh
./gradlew assembleDebug -PATLAS_ENV=isolated \
  -PATLAS_ORIGIN=https://atlas-test.example.invalid/ \
  -PATLAS_RADIO=https://atlas-test.example.invalid/stream/nova-android
```

`ATLAS_ENV` : isolated, preproduction ou release ; valeur et endpoints validés à la
compilation. HTTPS obligatoire dans tous les APK, y compris debug ; aucun certificat
utilisateur/trust-all ni exception LAN historique. Pour le LAN, utiliser un HTTPS
avec certificat système valide (reverse proxy/Tailscale correctement configuré).
Les tests JVM seuls utilisent le HTTP loopback explicite de MockWebServer.
Aucun sélecteur silencieux d'environnement, port secret ou credential Icecast.

Les chemins radio de `/api/public/site` sont modélisés/testés et filtrables par
SafeLinks.stream ; le service utilise pour cette tranche `ATLAS_RADIO` explicite.
L'application ne change pas automatiquement de flux après une réponse site. Une
origine radio historique distincte HTTPS reste possible par configuration explicite.
Pas de redirection HTTP automatique API/images/radio : un serveur qui redirige doit
être configuré avec son URL HTTPS finale, jamais suivi vers une origine arbitraire.

## Réseau et état Compose

Un seul client Retrofit/OkHttp : connexion 10 s, lecture 15 s, appel 20 s, réponses
2 MiB maximum (succès et erreurs), profondeur JSON maximale 64 avant décodage, pas de logs corps/URL, cookies persistants,
credentials, retries transparents ou redirections. Sérialisation stricte avec
champs inconnus acceptés ; manque de champs requis = erreur format, pas liste vide.
Aucun ETag ou 304 inventé : 304 inattendu est une erreur, pas du contenu inchangé.

`NewsRepository` expose StateFlow et un mutex tryLock : un refresh concurrent est
abandonné, pas mis en attente. L'écran collecte dans repeatOnLifecycle STARTED,
annule la boucle et les appels au passage en arrière-plan ou à la sortie de l'onglet.
Refresh manuel et automatique 60 s, intervalle injectable 30–300 s ; debounce manuel
5 s après succès. Backoff 60–900 s sur erreurs ; Retry-After 429 numérique/date est
respecté même au-delà du plafond normal (pas raccourci). Aucune boucle rapide
401/403, aucun fallback legacy sur auth/429/5xx/JSON invalide.

Données précédentes et heure de dernière lecture conservées sur erreur, indication
« données anciennes », chargement, vide et erreurs visibles. Pas de collecte
arrière-plan. État conservé pendant le montage ; filtres/onglet saveable. Pas de
cache disque actualités ni garantie de conservation des données après mort du
processus. Repository simple plutôt qu'un deuxième système de jobs ; évolution
ViewModel possible pour conserver davantage d'état entre recréations.

## Catégories, dates, images et événements

Catégories serveur ordonnées, personnalisées et Unicode ; si API site absente,
catégories observées dans les articles. Aucune recatégorisation client. Palette
historique avec fallback neutre. Les dates aware sont interprétées par java.time,
les dates historiques naïves ne prennent jamais le fuseau du téléphone par défaut.
Les champs bruts sont conservés ; publication prioritaire sur timestamp de collecte.
Les textes Compose restent du texte, aucun WebView/HTML actif. Liens HTTP(S) sans
credentials uniquement, erreurs navigateur génériques sans URL en log.

Images : projection approuvée uniquement, chemin relatif `/media/`, attribution
requise, pas de SVG distant, pas de hotlink. Affichage légende, auteur, crédit,
licence, modifications et liens source/licence filtrés. Coil sans cache mémoire ni
disque pour ces médias révocables, client borné sans redirection. Erreur/404 ou
fallback serveur => fallback local textuel ; image déjà visible peut rester jusqu'au
prochain refresh (60 s au premier plan), ce n'est pas une révocation instantanée hors
ligne. Aucune autorisation déduite du titre, de l'URL ou d'un simple og:image.

## Radio Media3

Un ExoPlayer dans MediaSessionService, un MediaController par activité ; future
partagée/gardée et libérée si connexion annulée. Pas de téléchargement avant Play.
Audio focus Media3 conservé ; service en arrière-plan et notification conservés.
Pause/stop annule le chargement et les reprises, Play prépare explicitement.

Politique de chargement unique (pas de second scheduler de reconnexion) : quatre
reprises maximum, 3/6/12/24 s, 401/402/403/404/410 et TLS non retentés ; 429 respecte
Retry-After jusqu'à 60 s, au-delà arrêt explicite plutôt que raccourcir l'embargo.
Une fin de flux propre (EOF) a une reprise distincte 3/6/12/24 s, au maximum quatre prepares par action Play ; Pause détruit les callbacks. Elle ne réessaie pas une erreur HTTP terminale. Après épuisement : état erreur/fin et nouvelle action Play nécessaire après embargo ; le délai 429 est aussi contrôlé avant une relance manuelle tant que le service vit (non persistant au redémarrage du processus). Aucune reprise
qui force playWhenReady=true après Pause. Pas de garantie d'auditeur, de réception
continue, ni de test réel Wi-Fi/mobile/Bluetooth à partir de ces doubles.
Le bouton affiche les états buffering/ready/error observés ; isPlaying décrit le
lecteur local seulement. Notification, veille et pertes réseau réelles restent à
qualifier sur appareils.

## Confidentialité, Premium, signature

Publicité et Premium désactivés à la compilation pour ce candidat ; provider auto-init Ads et permissions publicitaires retirés du manifeste fusionné, aucune init Ads
ni preload ou ANDROID_ID lu sans activation. Identifiants Ads versionnés remplacés
par IDs de test. Réactivation commerciale hors périmètre : consentement UMP,
déclaration de données, politique de confidentialité, audience et fournisseur à
valider avant distribution. Pas de nouvel achat ni accès compte AdMob.

ANDROID_ID n'est pas une preuve de paiement, n'est pas garanti stable après reset,
changement de signature ou contexte d'utilisateur. Endpoint Premium historique
conservé mais non assimilé à une validation Play Billing. Logs statut génériques,
scope Premium annulé à la destruction. Sauvegarde Android désactivée, aucun jeton
persistant introduit. Permissions réseau/media existantes conservées ; pas de
permission localisation, micro ou stockage ajoutée. POST_NOTIFICATIONS et contrôle
sur API33+ restent à vérifier sur appareil.

Release : jamais de fallback signature debug ; tâches Release refusées sans
RELEASE_KEYSTORE_PATH, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS et
RELEASE_KEY_PASSWORD, ATLAS_ENV=release et origine explicite. Aucun mot de passe
imprimé, aucune keystore créée/lue par l'agent, aucune nouvelle clé, version ou
publication. Debug est l'unique artefact de compilation CI obligatoire.

## Validation et matrice appareils

CI Android : JDK17, wrapper validé + distribution SHA256, SDK34, tests JVM,
assembleDebug, lintDebug, rapport dépendances et scan motifs secrets versionnés.
MockWebServer HTTP local et horloge virtuelle pour lenteur/concurrence/annulation,
erreurs, limites et retries. Aucun serveur utilisateur, pub ou flux Internet.
Contrats serveur : deux générations pytest réussies sur fixtures temporaires.
Avis OSV : `docs/measurements/osv-direct.json` — six versions directes interrogées,
pas d'avis retourné ; **ne couvre pas toutes les transitives ni tous les avis**.
Les dépendances anciennes ne sont pas déclarées « sans vulnérabilité » pour autant.

Tests Compose instrumentés isolent composants sans MainActivity/service/API.
Workflow `Optional emulator validation` à la PR ou manuel ; aucune validation physique déduite.

| Environnement | État |
|---|---|
| JVM/Gradle SDK34 runner GitHub | Résultat final de la PR et rapports artifacts font foi |
| Émulateur API34 | Workflow préparé, exécution distincte à consigner |
| Android8 API26 physique | Non effectué |
| Android14 API34 physique (target) | Non effectué |
| Android récent physique | Non effectué |
| Faible RAM / smartphone moderne / trois appareils | Non effectué |
| Batterie, mémoire app, casque, Bluetooth, veille, Wi-Fi/mobile | Non mesuré/non effectué |

Checklist opérateur : installation et permissions, lancement sans réseau, chargement
fixtures, filtres Unicode, événements, attributions, TalkBack/clavier/RTL, rotation,
verrouillage/veille, notification, audio focus/appel/casque/Bluetooth, 404/503,
redémarrage Icecast de test, Pause pendant reprise, retour réseau, fermeture et
recréation. Consigner appareil/API/build, durée, résultat, mémoire/batterie réellement
mesurées. Aucune estimation matérielle ou performance vocale revendiquée.

## Limites et rollback

Pas de pagination infinie, archives natives, mode offline durable, migration achats,
validation multi-smartphones ni qualification radio prolongée dans cette tranche.
Aucun backend reconstruit ou déployé. Rester en essai privé tant que les portes RC
serveur et consentement/droits/distribution ne sont pas levées.

Rollback application : arrêter la lecture, conserver config d'environnement et
artefact antérieur vérifié. Réinstaller un APK compatible avec la même signature,
applicationId et versionCode ; une signature différente impose désinstallation et
perte des seules préférences locales. Aucun impact serveur ni migration de données.
Ne jamais distribuer comme release un APK debug. Ne pas changer/remplacer la clé
existante. Tous les endpoints historiques restent disponibles côté serveur.

Sources primaires : [AGP8.7/Gradle/JDK](https://developer.android.com/build/releases/agp-8-7-0-release-notes),
[lifecycle](https://developer.android.com/topic/libraries/architecture/views/coroutines-views),
[Media3 LoadErrorHandlingPolicy](https://developer.android.com/reference/androidx/media3/exoplayer/upstream/LoadErrorHandlingPolicy),
[OSV API](https://google.github.io/osv.dev/api/).
