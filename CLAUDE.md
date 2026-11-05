# CLAUDE.md – ChasseLog

Carnet de chasse gratuit (chasselog.be) : calendrier des journées, tirs (BANG), munitions, GPS, photos, trophées, documents, statistiques.
Cible : chasseurs belges (Wallonie surtout) et français. Propriétaire : Maxime (chasselog-coder). Langue de l'app, des commits et des échanges : **français**.

## Principes de travail
1. **Réfléchir avant de coder.** Reformuler la demande, signaler les ambiguïtés et les effets de bord (fiche de chasse, statistiques, image partagée, visite guidée, export) avant de toucher au code. Poser la question plutôt que deviner.
2. **Simple d'abord.** La solution la plus courte qui marche. Pas de framework, pas de dépendance, pas de build : tout reste en HTML/CSS/JS natif.
3. **Modifications chirurgicales.** Ne toucher qu'à ce qui est demandé. Ne pas reformater, renommer ni « nettoyer » le reste de `index.html`.
4. **Vérifier.** Contrôler le résultat (syntaxe JS, rendu, capture d'écran si possible) avant d'annoncer que c'est fait. Dire clairement ce qui n'a pas pu être testé.

## Architecture
- **PWA 100 % locale**, sans serveur ni compte. Tout est dans `index.html` (~2 250 lignes, CSS + JS inline, code compact et minifié à la main : garder ce style).
- Données : `localStorage` clé `carnet2` (objet global `S`, sauvé par `save()`), copie `carnet2-secours` si lecture impossible. Photos et rappels : IndexedDB `carnet-photos`.
- Toute nouvelle donnée doit passer par `S` et survivre à **Menu › Exporter / Importer** (JSON complet, photos incluses). Ne jamais casser la lecture d'anciennes données : prévoir des valeurs par défaut, garder les anciens champs affichés (ex. anciennes « photos de la journée »).
- Carte : Leaflet local (`lib/leaflet/`) + tuiles OpenStreetMap, jamais mises en cache, attribution visible.
- `sw.js` : cache hors ligne. Ajouter tout nouveau fichier statique à `FILES`.
- Région : `S.reg` (`"fr"` = France avec `S.dep`, sinon Belgique/autre). Certaines options dépendent de la région (ex. chasse à courre en France uniquement).
- Échapper tout texte utilisateur avec `esc()` avant insertion HTML.

## Application Android (Capacitor)
- Même `index.html` ; `IS_APP` détecte l'app installée. Les comportements propres à l'APK passent par des branches `if(IS_APP)` et ne doivent rien changer à la version web.
- Builds : branche `apk-builds` (`info.txt`, .apk de test, .aab à signer). Play Store en test fermé.

## Statistiques anonymes (PostHog, UE)
- Uniquement des valeurs simples et anonymes, avec consentement. Jamais de position GPS, de nom de lieu, de texte libre, de photo ni de contenu de document.
- La province/le département est calculé sur le téléphone (`data/prov.json`). Ne pas confondre installations PostHog et testeurs Play Console.

## Versions et déploiement
- `main` = production (GitHub Pages → chasselog.be). **Ne jamais pousser directement sur `main` sans accord explicite.**
- Nouvelle idée : branche `prep-<sujet>`, commits « Proposition : … » ou « Maquette : … », puis validation par Maxime.
- Mise en production : commit « Version AAAA-MM-JJx : résumé » qui incrémente **ensemble** `APP_V` dans `index.html` et `V = "carnet-vNN"` dans `sw.js` (sinon les téléphones ne voient pas la mise à jour).
- Si un écran change, vérifier si la visite guidée (`tour/*.webp`, 10 étapes) et les visuels `promo/` doivent être refaits.

## Produit et ton
- Sobre et indépendant : pas de charte ni de lien d'un club ou partenaire imposé dans l'app.
- Couleurs (`:root`) : fond `#f1efe6`, vert `#3f5a2b`, accent brique `#a4452c`. Vert forêt `#1f3326` pour les Reels.
- Textes courts, tutoiement, vocabulaire de chasse exact (brocard, chevrillard, cors, poids vidé, tableau, poste, battue, approche/affût).
- **Keep it simple** : pas de texte explicatif sous chaque bouton ou réglage. Un réglage = un interrupteur ou un champ, directement là où on l'attend, sans page supplémentaire si ce n'est pas indispensable.
- Confidentialité des documents : n'afficher que ce que l'utilisateur a ajouté (un contrôle ne doit pas révéler de documents manquants).
- Données personnelles (ex. `mon-calendrier-chasse.json`) : jamais dans le dépôt public.

## Dates d'ouverture France
Procédure annuelle complète dans `tools/fr/README.md` (robots GitHub sur la branche `arretes-fr`, fiches `tools/fr/src/XX.json`, `python3 tools/fr/build.py --saison AAAA-AAAA`, branche `maj-dates-fr-AAAA`). Ne pas inventer de date : en cas de doute, laisser une note et demander.
