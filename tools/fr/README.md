# Dates d'ouverture France : mise à jour annuelle

Les dates d'ouverture françaises affichées dans l'app viennent de `data/ouv-fr.json`. Ce fichier est construit à partir des fiches départementales `tools/fr/src/XX.json`.

## Robots GitHub (branche `arretes-fr`)
- `arretes-fr.yml` télécharge l'arrêté de chaque département depuis la page « dates de chasse » de la FNC (chasseurdefrance.com) et en extrait le texte dans `txt/XX.txt` avec `index.tsv`. Il tourne chaque lundi de juin à septembre et à chaque modification de `tools/fr/fetch.sh` ou `deps.tsv`.
- `ocr-fr.yml` fait un OCR à 300 dpi, page par page, des départements listés dans `tools/fr/ocr300.txt`. Le résultat va dans `txt300/XX.txt`. C'est le meilleur texte disponible.
- `probe-fr.yml` cherche les arrêtés que la page FNC ne lie pas (noms `FDC-XX*.pdf`), pour les départements listés dans `tools/fr/probe.txt`. Le résultat va dans `probe/txt/XX.txt` et `probe/index.tsv`.

## Procédure annuelle (en été, quand les nouveaux arrêtés sont publiés)
1. Vérifier que `index.tsv` sur la branche `arretes-fr` date de la nouvelle saison. Les URL doivent être en `/AAAA/0[5-9]/` de l'année courante. Sinon, pousser une petite modification de `tools/fr/ocr300.txt` (ou de `probe.txt`) pour relancer les robots.
2. Pour chaque département, lire le meilleur texte disponible (`probe/txt` > `txt300` > `txt`), puis réécrire `tools/fr/src/XX.json` (format décrit en tête de `build.py`) :
   - `aa` = approche/affût, `bat` = battue ou chasse collective, `g` = petit gibier et oiseaux (tous modes) ;
   - dates complètes AAAA-MM-JJ, dans la fenêtre du 1er juin au 30 juin de l'année suivante ;
   - `n` = note courte (autorisation, jours, PMA…) ;
   - une espèce interdite est omise.
3. Lancer `python3 tools/fr/build.py --saison AAAA-AAAA` : aucune erreur ne doit rester.
4. Vérifier visuellement quelques départements dans l'app (☰ › Périodes d'ouverture).
5. Pousser sur une branche `maj-dates-fr-AAAA` et prévenir le propriétaire pour validation. Ne pas pousser directement sur `main`.
- `"x": true` sur une espèce = tir interdit cette saison (affiché « Interdit »).
