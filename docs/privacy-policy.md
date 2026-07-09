# Nova-Atlas Android — Privacy Policy

**Last updated** : 2026-07-09
**Effective date** : 2026-07-09
**App** : Nova-Atlas (com.niko.novaatlas)

## Résumé en français

Nova-Atlas est une application Android compagnon du serveur Nova-Atlas (moteur de news IA + webradio 24/7). Cette politique de confidentialité explique quelles données l'app collecte et pourquoi.

## Données collectées

### 1. Identifiant d'appareil (ANDROID_ID)

- **Quoi** : `Settings.Secure.ANDROID_ID`, identifiant unique généré par Android pour notre app sur ton device
- **Pourquoi** : pour vérifier ton statut d'abonnement premium (le serveur a besoin de te reconnaître entre les sessions)
- **Partagé** : non. Envoyé uniquement à notre serveur (`192.168.1.22:5055` ou domaine public ultérieur)
- **Stockage** : pas stocké sur le serveur au-delà de `data/subscriptions.json` (gitignoré, accès restreint au serveur)
- **Désactivation** : tu peux utiliser l'app sans abonnement premium, dans ce cas l'identifiant n'est pas pertinent

### 2. Contenu de l'app

L'app affiche des articles de news agrégés depuis des flux RSS publics et des bulletins radio générés par notre serveur. Aucun de ces contenus n'est collecté par nous — il est généré par notre pipeline et servi à l'app.

### 3. Publicités (AdMob)

- **Fournisseur** : Google AdMob
- **Politique de Google** : https://policies.google.com/privacy
- **Données collectées par AdMob** : identifiants publicitaires, IP approximative, type d'appareil, données d'usage (clics sur les pubs, etc.)
- **Contrôle** : tu peux réinitialiser ton identifiant publicitaire ou opt-out dans les paramètres Android : `Paramètres > Google > Publicités`

## Permissions de l'app

L'app demande les permissions suivantes :

| Permission | Pourquoi |
|---|---|
| `INTERNET` | Télécharger les articles, streamer la radio, charger les pubs |
| `ACCESS_NETWORK_STATE` | Détecter si tu es online avant de fetch |
| `WAKE_LOCK` | Empêcher le téléphone de s'endormir pendant la radio |
| `FOREGROUND_SERVICE` | Faire tourner la radio en background (notif media) |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Idem, spécifique Android 14+ |
| `POST_NOTIFICATIONS` | Afficher la notif media (Android 13+) |

**On ne demande PAS** : localisation, contacts, fichiers, caméra, micro, Bluetooth, etc.

## Réseau

L'app communique uniquement avec :
- Notre serveur : `http://192.168.1.22:5055` (Flask, votre LAN) — endpoints `/api/articles`, `/api/subscription/status`
- Notre radio : `http://192.168.1.22:8000/nova-android` (Icecast, votre LAN, mount bulletins-only sans musique libre de droits)
- Google AdMob (pour les pubs)

En HTTPS pour la production publique (Sprint E3, à venir).

## Stockage local

- **Cache des articles** : non persisté pour l'instant (Sprint H à venir avec Room)
- **Préférences UI** : pas de préférences pour l'instant (Sprint F à venir pour le premium UI)
- **Token premium** : pas de token (le check est stateless, le serveur garde le mapping device_id → premium)

## Tes droits (RGPD / CCPA)

- **Accès** : tu peux me demander ce que j'ai sur toi (le `device_id` associé à un premium)
- **Suppression** : tu peux me demander de supprimer l'entrée `data/subscriptions.json` correspondant à ton device
- **Portabilité** : pas applicable (pas de données personnelles au-delà de l'identifiant)
- **Contact** : nikodindon@gmail.com

## Modifications de cette politique

Toute modification sera annoncée dans un commit du repo (https://github.com/nikodindon/Nova-Atlas-Android) avec un changelog. La date "Last updated" en haut sera mise à jour.

## Hébergement de cette politique

Pour la publication sur Google Play Store, cette politique doit être accessible via une **URL publique** (pas un fichier local). Options :

- **GitHub Pages** (gratuit) : créer un repo `nikodindon/nova-atlas-privacy`, y mettre ce fichier, activer Pages dans Settings
- **Site Nova-Atlas** (quand HTTPS sera en place) : `https://nova-atlas.example.com/privacy`
- **Notion / Google Sites / etc.** : héberger le contenu ailleurs

L'URL sera renseignée dans la fiche du Play Store lors de l'upload de l'AAB.

---

**Version simplifiée (à mettre sur le Play Store, en anglais)** :

> Nova-Atlas displays news articles and a radio stream from our server. We collect:
> - A device identifier (ANDROID_ID) to manage your premium subscription
> - Google AdMob may collect advertising identifiers per their privacy policy
> We do not collect location, contacts, files, or any other personal data.
> Full policy: <URL_GITHUB_PAGES>
