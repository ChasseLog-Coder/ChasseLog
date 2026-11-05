# ChasseLog pour Android (Capacitor)

L'application Android emballe l'app web du dépôt (`index.html` et ses dossiers). Il n'y a qu'un seul code :
dans l'APK, `IS_APP` est vrai et l'app utilise les fonctions natives (partage, fichiers, bouton retour,
liens externes, barres du système). Sur le site, rien ne change.

- `capacitor.config.json` : identifiant `be.chasselog.app`, réglages Capacitor.
- `scripts/www.mjs` : copie l'app web dans `www/` (sans service worker ni écran d'installation).
- `scripts/native.mjs` : ajuste le projet Android généré (autorisations, bord à bord, icônes, démarrage, version).
- `res/` : icônes ; `assets/logo-1024.png` : logo source.
- Aucune clé de signature dans ce dépôt public : le fichier .aab est construit non signé, puis signé hors de GitHub avec la clé d'importation Google Play.

Le dossier `android/` n'est pas versionné : le workflow `.github/workflows/android.yml` le génère à chaque build
et dépose le fichier .aab (non signé) sur la branche `apk-builds` (avec `build.log` et `info.txt`).
