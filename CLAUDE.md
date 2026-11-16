# CLAUDE.md – ChasseLog

Carnet de chasse gratuit (chasselog.be) : calendrier des journées, tirs (BANG), munitions, GPS, photos, trophées, documents, statistiques.
Cible : chasseurs belges (Wallonie surtout) et français. Propriétaire : Maxime (chasselog-coder). Langue de l'app, des commits et des échanges : **français**.

## Principes de travail
Ces règles privilégient la prudence à la vitesse. Pour une retouche évidente, faire preuve de bon sens.

### 1. Réfléchir avant de coder
- Reformuler la demande et dire ses hypothèses. En cas de doute, poser la question plutôt que deviner.
- S'il y a plusieurs interprétations, les présenter : ne pas en choisir une en silence.
- S'il existe une solution plus simple, le dire, même si elle contredit la demande.
- Signaler les effets de bord (fiche de chasse, statistiques, image partagée, visite guidée, export) avant de toucher au code.
- Générer des visuels et les présenter avant de coder.

### 2. Simple d'abord
- Le minimum de code qui répond à la demande. Rien de spéculatif : pas de fonction, d'option ni de réglage non demandés, pas d'abstraction pour un seul usage, pas de gestion d'erreur pour un cas impossible.
- Pas de framework, pas de dépendance, pas de build : tout reste en HTML/CSS/JS natif.
- Si 200 lignes peuvent en faire 50, réécrire.

### 3. Modifications chirurgicales
- Chaque ligne modifiée doit découler directement de la demande.
- Ne pas reformater, renommer ni « améliorer » le code voisin. Garder le style existant de `index.html`, même si on ferait autrement.
- Retirer ce que sa propre modification rend inutile (variables, fonctions, styles). Le code mort déjà présent : le signaler, ne pas le supprimer.

### 4. Un objectif vérifiable
- Avant de coder, dire à quoi on verra que c'est réussi. Pour un bug : le reproduire d'abord, puis vérifier qu'il a disparu.
- Pour une tâche en plusieurs étapes, annoncer un plan court : étape → vérification.
- Contrôler avant d'annoncer que c'est fait : syntaxe JS, rendu en capture d'écran, anciennes données toujours lues, pas de nom de fonction ou de style déjà utilisé ailleurs.
- Dire clairement ce qui n'a pas pu être testé.

## Architecture
- **PWA 100 % locale**, sans serveur ni compte (de base, un serveur sera ajouté par après). Tout est dans `index.html` (~2 400 lignes, CSS + JS inline, code compact et minifié à la main : garder ce style).
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
- Si un écran change, vérifier si la visite guidée (`tour/*.webp`, 12 étapes) et les visuels `promo/` doivent être refaits.

## Produit et ton
- Indépendant : pas de charte ni de lien d'un club ou partenaire imposé dans l'app.
- Couleurs (`:root`) : fond `#f1efe6`, vert `#3f5a2b`, accent brique `#a4452c`. Vert forêt `#1f3326` pour les Reels.
- Textes courts, tutoiement, vocabulaire de chasse exact (brocard, chevrillard, cors, poids vidé, tableau, poste, battue, approche/affût).

## Interface sobre (UNSLOP)
L'app doit être sobre, claire et intuitive : un écran se comprend sans mode d'emploi.
- Pas d'emoji ni d'icône décorative : un libellé court suffit. Une icône seulement si elle est universelle (retour, fermer, calques) et remplace vraiment du texte.
- Pas de texte explicatif sous un bouton ou un réglage. Un réglage = un interrupteur ou un champ, là où on l'attend, sans page supplémentaire si ce n'est pas indispensable.
- Ne pas multiplier les boutons ni les options : une décision se prend à un seul endroit.
- Pas de décor : un seul vert, boutons en contour, pas de dégradé, d'ombre ni de badge sans utilité.
- S'applique à tout nouvel écran et à tout écran modifié. Ne pas refondre le reste sans demande.

## Confidentialité
- Confidentialité des documents : n'afficher que ce que l'utilisateur a ajouté (un contrôle ne doit pas révéler de documents manquants).
- Données personnelles (ex. `mon-calendrier-chasse.json`) : jamais dans le dépôt public.

## Dates d'ouverture France
Procédure annuelle complète dans `tools/fr/README.md` (robots GitHub sur la branche `arretes-fr`, fiches `tools/fr/src/XX.json`, `python3 tools/fr/build.py --saison AAAA-AAAA`, branche `maj-dates-fr-AAAA`). Ne pas inventer de date : en cas de doute, laisser une note et demander.
