#!/usr/bin/env bash
# OCR page par page (seulement les pages scannées) des départements listés dans tools/fr/ocr300.txt.
# Reprend là où il s'est arrêté (saute ceux déjà présents dans txt300/ sur la branche) et publie par lots.
set -u
ocrpage(){ # $1 = image ; psm 1 (rotation auto) puis psm 3, garde le texte le plus « français »
  sc(){ echo "$1" | grep -oiE "(^| )(de|la|le|les|du|des|et|chasse|au|en|ouverture|clôture)( |$)" | wc -l; }
  t=$(timeout 240 tesseract "$1" - -l fra --psm 1 2>/dev/null); n=$(sc "$t")
  u=$(timeout 240 tesseract "$1" - -l fra --psm 3 2>/dev/null); m=$(sc "$u"); [ "$m" -gt "$n" ] && { t=$u; n=$m; }
  if [ "$n" -lt 15 ]; then for a in 90 270 180; do convert "$1" -rotate $a "$1.r.png" 2>/dev/null || continue
    u=$(timeout 240 tesseract "$1.r.png" - -l fra --psm 3 2>/dev/null); m=$(sc "$u"); [ "$m" -gt "$n" ] && { t=$u; n=$m; }; done; fi
  echo "$t"; }
export -f ocrpage
git fetch -q origin +refs/heads/arretes-fr:refs/remotes/origin/arretes-fr
git show origin/arretes-fr:index.tsv > /tmp/index.tsv
done_list=$(for f in $(git ls-tree --name-only origin/arretes-fr:txt300 2>/dev/null); do git show origin/arretes-fr:txt300/$f | head -1 | grep -q '^#v3' && echo ${f%.txt}; done)
mkdir -p /tmp/o300
ocr1(){ code=$1; url=$(awk -F'\t' -v c="$code" '$1==c{print $4}' /tmp/index.tsv); [ -z "$url" ] && return
  d=$(mktemp -d); curl -sL --max-time 120 -o "$d/a.pdf" "$url"
  np=$(pdfinfo "$d/a.pdf" 2>/dev/null | awk '/^Pages:/{print $2}'); [ -z "$np" ] && np=0; [ "$np" -gt 14 ] && np=14
  echo "#v3"; for p in $(seq 1 $np); do echo "=== page $p"; t=$(pdftotext -layout -f $p -l $p "$d/a.pdf" - 2>/dev/null)
    if [ $(echo "$t" | grep -oiE "(^| )(de|la|le|les|du|des|et|chasse|au|en)( |$)" | wc -l) -ge 15 ]; then echo "$t"; else pdftoppm -r 300 -gray -png -f $p -l $p "$d/a.pdf" "$d/p$p"; for im in "$d"/p$p*.png; do ocrpage "$im"; done; fi
  done > /tmp/o300/$code.txt; rm -rf "$d"; echo "$code ok $np"; }
export -f ocr1
publish(){ git fetch -q origin +refs/heads/arretes-fr:refs/remotes/origin/arretes-fr; git checkout -q -B arretes-fr origin/arretes-fr; mkdir -p txt300; cp /tmp/o300/*.txt txt300/ 2>/dev/null
  git add txt300; git commit -qm "OCR 300 dpi (lot)" || return 0
  for i in 1 2 3; do git push -q origin arretes-fr && break; git fetch -q origin arretes-fr; git rebase -q origin/arretes-fr; done; git checkout -q -; }
todo=$(grep -oE "^[0-9AB]{2}" tools/fr/ocr300.txt | grep -vxF -f <(echo "$done_list") )
set -- $todo
while [ $# -gt 0 ]; do lot="${@:1:8}"; shift $(( $# < 8 ? $# : 8 ))
  echo $lot | tr ' ' '\n' | xargs -n1 -P8 bash -c 'ocr1 "$0"'
  publish
done
