# Confidentialité — candidat Android V2, Sprint 14

Mis à jour le 8 novembre 2026. Ce document décrit la branche candidate, pas une
publication Play ni une certification juridique. Il remplace les hypothèses LAN,
publicité et Premium du prototype ; une ancienne application installée peut avoir
un comportement différent.

Le candidat contacte uniquement l'origine HTTPS et le flux HTTPS explicitement
configurés à la compilation. Par défaut : example.invalid, aucun serveur réel.
Les requêtes donnent au serveur configuré les métadonnées réseau normales (IP,
horaires, chemin demandé). Sa conservation des logs relève de cet opérateur ;
aucune promesse d'absence de journaux serveur n'est faite.

Publicité, preload, initialisation Ads et vérification Premium sont désactivés.
Le provider d'initialisation automatique et les permissions publicitaires sont
retirés du manifeste fusionné. Aucun ANDROID_ID n'est lu par le parcours désactivé.
Les bibliothèques historiques restent empaquetées ; leur retrait ou réactivation
nécessite une revue séparée. Aucun achat ni preuve de paiement côté client.

Actualités en mémoire seulement ; sauvegarde Android désactivée. Miniatures
publiques autorisées sans cache disque/mémoire Coil. Les textes RSS ne sont pas du
code exécutable. Les clics explicites sur les sources ouvrent le navigateur externe,
dont les politiques et le réseau sont distincts. Aucun diagnostic d'identifiant ou
d'URL privée n'est introduit dans les logs applicatifs.

Permissions conservées : Internet, état réseau, wake lock, service premier plan
mediaPlayback et notifications. Pas de microphone, contacts, localisation ou
nouvel accès aux fichiers. La radio poursuit sa lecture demandée en arrière-plan
via Media3 ; les actualités ne sont pas collectées en arrière-plan.

Avant toute distribution : qualification sur appareils, analyse du manifeste et des
SDK effectivement livrés, consentement si monétisation, déclaration Play Data Safety,
droits des médias/voix et politique complète validée par l'opérateur. ANDROID_ID
n'est jamais une preuve d'abonnement. Les demandes concernant d'anciennes données
serveur doivent être adressées à l'opérateur de l'installation concernée.

Voir [le dossier d'intégration](ANDROID_V2_INTEGRATION.md) pour les limites et les
résultats réels. Aucun déploiement de cette politique ou upload Play n'est effectué
par ce sprint.
