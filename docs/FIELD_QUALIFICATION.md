# Android — Qualification terrain Sprint 15

Référence dev après fusion de PR1 : `0bf4ba0dcbedf62521ae6daebaca9b703e7c1354`.
Serveur de référence `0e8752622bbbe16cf9b3b187bb7d69a9b1e4533b`, PR21 fusionnée.
PR Android [#2](https://github.com/nikodindon/nova-atlas-android/pull/2), serveur
[#22](https://github.com/nikodindon/Nova-Atlas/pull/22). Aucun téléphone physique,
endurance, batterie, Bluetooth ou réception Icecast sur appareil qualifié ici.
Le dossier serveur [FIELD_QUALIFICATION.md](https://github.com/nikodindon/Nova-Atlas/blob/feature/v2-field-qualification/docs/FIELD_QUALIFICATION.md)
fait autorité sur les portes A/B, les autorisations et la campagne 24 h.

## Défaut confirmé et correction

La configuration Release antérieure vérifiait seulement origin.contains(example.invalid).
ATLAS_RADIO pouvait garder sa valeur fictive alors qu'une origine API réelle était
fournie et que les credentials de signature étaient présents. Preuve statique
versionnée, sans lire de keystore. Le script `gradle/release-endpoints.gradle.kts`
contrôle désormais **les deux endpoints**, dès la configuration et indépendamment
de la signature. HTTPS, absence de credentials/query/fragment, refus des domaines
réservés et variantes de casse/point terminal. Il ne prouve pas l'autorisation ou
la disponibilité du domaine choisi. Pas de changement de cible SDK, signature,
applicationId ou version sans autre reproduction. Ads/Premium restent désactivés.

Tests CI : `help -PATLAS_ENV=release -PATLAS_ORIGIN=https://atlas.operator.net/`
doit échouer tant que radio est fictive ; sa variante EXAMPLE.INVALID. aussi.
Le domaine de cette commande est une chaîne de configuration seulement : aucune
requête réseau n'est effectuée. Ne jamais utiliser cette commande comme autorisation
de contacter un domaine réel. Les refus de signature sans secrets et HTTP restent
vérifiés séparément. Aucun APK release, keystore opérateur ou upload Play.

## Observations radio privées

En debug, le bouton « Exporter les observations radio » ouvre le sélecteur de
document Android. Export seulement sur geste explicite, pas de télémétrie envoyée.
Version1, horloge monotone, anneau de 128 événements et compteurs bornés en mémoire.
La disparition du processus perd ces données ; conserver un export avant incident
quand possible. Aucun URL, identifiant Android, numéro de série, SSID, adresse,
credential ou message d'exception libre. Les données exportées restent privées.

| Observation | Ce qui est réellement observé |
|---|---|
| PLAY_REQUESTED / PAUSE_REQUESTED | Intention du bouton applicatif, pas preuve de lecture |
| SESSION_PLAY_INTENT / SESSION_PAUSE_INTENT | playWhenReady du service, inclut contrôles système/focus |
| BUFFERING | État ExoPlayer ; pas garantie d'une connexion |
| CONNECTION_OPEN | TransferListener : source réseau ouverte ; pas validation audio |
| FIRST_NETWORK_BYTES / network_bytes | Octets transférés ; pas décodage/auditeur |
| LOCAL_PLAYBACK_STARTED / STOPPED | isPlaying local ; pas réception côté auditeur distant |
| RECONNECT_SCHEDULED | Callback EOF planifié, délai numérique |
| TERMINAL_ERROR | Code Media3 numérique sans message/URL |
| CLEAN_END | Fin de flux, distincte d'une exception HTTP |
| PLAYER_CREATED / RELEASED | Événements lifecycle enregistrés ; un crash peut manquer RELEASED |

Les reprises internes de chargement Media3 ne produisent pas toutes un événement
RECONNECT_SCHEDULED : comparer ouvertures/transferts/erreurs et les délais. Ne pas
inventer un nombre de reconnexions simplement à partir du compteur buffering.
L'instrumentation n'ajoute aucun lecteur, retry, connexion ou scheduler. Les tests
JVM valident l'anneau, les compteurs, premières données par ouverture et refus de
valeurs invalides ; ils ne simulent pas la réception d'un bulletin sur un téléphone.

## SBOM et avis réel

`./gradlew companionSbom` produit `app/build/reports/sbom/` : graphe Maven résolu de
debugRuntimeClasspath (JAR/AAR avec SHA256), CycloneDX1.6, hash APK et inventaire .so
effectivement présents. Les SDK désactivés sont inclus car ils sont empaquetés.
Ce graphe ne prouve ni leur atteignabilité ni leurs licences. Le premier inventaire
comptait 126 composants, aucun .so dans cet APK debug. Une autre variante doit être
inventoriée séparément ; aucune absence native universelle n'est déduite.

Syft1.55.0/Grype0.121.2 sont téléchargés uniquement en CI, assets SHA256 officiels
vérifiés. La version/schéma/date de base et les JSON complets sont artifacts. Ce
n'est plus un scan de six dépendances directes. Les limites d'identification
native, de base CVE et de droits restent explicites.

Le scan a trouvé Guava33.0.0-android affecté par GHSA-xxph-c9ww-hj94/CVE-2026-102554.
La version affectée est confirmée ; la désérialisation native de données hostiles
n'est pas démontrée dans cette app (API Kotlinx JSON). Pas de faux positif ni
inatteignabilité totale déclarés sans preuve. Fix **ciblé33.7.2-android**, publication
Android du fournisseur vérifiée. Le rescan du graphe et tous les tests ont réussi
avec zéro correspondance dans cette base, sans suppression d'avis. Cela ne constitue
pas une attestation « aucune vulnérabilité ». Sévérité High du scanner et Moderate
du fournisseur conservées dans `docs/measurements/guava-triage.json`.

## Campagne opérateur, trois rapports séparés

Pour chaque appareil physique : alias/modèle/API, commit et SHA256 APK, signature,
version, réseau de test, date aware, protocole, PASS/FAIL/WARNING/NOT_TESTED/BLOCKED,
limites et référence privée du rapport. Ne pas confondre API34 émulateur avec
Android14 physique ; A/B Linux sont aussi des environnements distincts.

1. Installer un debug configuré pour HTTPS de test autorisé. Pas de certificat
   permissif, pas de domaine de production, aucune publicité/achat réel.
2. Tester lancement/permissions/articles/Unicode/RTL/filtres/catégories personnalisées,
   événements et provenance, images/attribution/révocation/fallback, mode sombre et
   accessibilité. API invalide, 429, 500, timeout et lenteur seulement sur fixtures.
3. Mesurer rotation/récréation, arrière-plan/premier plan, annulation et consommation
   réelle de réseau sur une fenêtre annoncée ; pas d'estimation batterie à partir JVM.
4. Radio sur vrai mount isolé : Play/Pause, buffering, notification, écran verrouillé,
   veille, arrêt système, casque/Bluetooth/focus/appel, Wi-Fi/mobile, perte/retour,
   404/503, redémarrage Icecast, EOF, 429 et TLS invalide refusé. Exporter les
   observations avant/après ; Pause ne doit jamais être suivie d'une reprise non demandée.
5. Noter écoute humaine, durée, omissions/silences, mémoire/batterie uniquement si
   réellement observés. isPlaying ou premiers octets ne suffisent pas.

État initial : les trois téléphones, Android8 physique et version récente physique,
Bluetooth/casque/veille/batterie et vrais chemins HTTPS+Icecast sont NOT_TESTED.
CI JVM et émulateur restent dans leur propre périmètre. RC-1 non qualifiée ; NO-GO
public maintenu même si toutes les CI sont vertes. APK antérieur compatible seulement
avec signature/version compatibles ; pas de remplacement de keystore ni de bascule
backend automatique. Aucune donnée serveur modifiée.
