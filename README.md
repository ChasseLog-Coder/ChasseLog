# Carnet de chasse – PWA (test)

Application 100 % locale : données et photos restent sur le téléphone (localStorage + IndexedDB). Aucun serveur, aucun compte.
La carte (OpenStreetMap) ne s'affiche qu'avec une connexion ; sans réseau, un message l'indique. Pas de cartes hors ligne pour l'instant.

## Héberger gratuitement (HTTPS obligatoire pour une PWA)
**GitHub Pages** : créer un dépôt public, y déposer le contenu de ce dossier (index.html à la racine), puis Settings → Pages → Branch `main` / root.
L'adresse sera `https://<pseudo>.github.io/<depot>/`. (Alternative : Cloudflare Pages / Netlify, glisser-déposer le dossier.)

## Installer
- **Android (Chrome)** : ouvrir l'adresse → menu ⋮ → « Installer l'application » / « Ajouter à l'écran d'accueil ». Autoriser la localisation quand demandé.
- **iPhone (Safari)** : Partager → « Sur l'écran d'accueil ».

Après la première ouverture, l'app fonctionne hors ligne (sauf la carte). Une mise à jour est proposée via un message quand le site change (incrémenter `carnet-v1` dans `sw.js`).

## Données
- Menu → Exporter : sauvegarde JSON complète (photos incluses). Menu → Importer pour restaurer.
- Importer `mon-calendrier-chasse.json` (fourni à part) pour récupérer le calendrier personnel. Ne pas le publier sur le site public.
- Les tuiles OpenStreetMap sont soumises à leur politique d'usage (usage léger, attribution affichée).
