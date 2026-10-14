#!/usr/bin/env bash
# Pour les départements dont la page FNC ne lie pas le bon arrêté : essaie les noms FDC-XX*.pdf habituels,
# garde celui qui ressemble à l'arrêté d'ouverture/clôture, et extrait le texte (OCR des pages scannées).
set -u; OUT=${OUT:-probe}; mkdir -p "$OUT/txt"
UA="Mozilla/5.0 (X11; Linux x86_64) ChasseLog-bot (contact.chasselog@gmail.com)"
txt1(){ # texte page par page, OCR si page scannée
  np=$(pdfinfo "$1" 2>/dev/null | awk '/^Pages:/{print $2}'); [ -z "$np" ] && np=0; [ "$np" -gt 20 ] && np=20
  for p in $(seq 1 $np); do echo "=== page $p"; t=$(pdftotext -layout -f $p -l $p "$1" - 2>/dev/null)
    if [ $(echo "$t" | tr -d ' \n' | wc -c) -gt 400 ]; then echo "$t"; else d=$(mktemp -d); pdftoppm -r 300 -gray -png -f $p -l $p "$1" "$d/p"; timeout 300 tesseract "$d"/p*.png - -l fra --psm 1 2>/dev/null; rm -rf "$d"; fi; done; }
probe(){ code=$1; OUT=$2; UA="$3"; n=$code; [ "$code" = "2A" ] && n="20-A"; [ "$code" = "2B" ] && n="20-B"
  best=""; bs=-1; d=$(mktemp -d)
  for y in 2026; do for m in 04 05 06 07 08 09 10; do for s in "" -1 -2 -3 -4; do
    u="https://www.chasseurdefrance.com/wp-content/uploads/$y/$m/FDC-$n$s.pdf"
    c=$(curl -s -o "$d/c.pdf" -w "%{http_code}" -A "$UA" --max-time 60 "$u")
    [ "$c" = "200" ] || continue
    h=$(pdftotext -l 3 "$d/c.pdf" - 2>/dev/null | tr '\n' ' ')
    sc=$(echo "$h" | grep -oiE "ouverture g[ée]n[ée]rale|cl[ôo]ture|2026-2027|2026/2027" | wc -l)
    echo "$h" | grep -qiE "susceptibles? d.occasionner" && sc=$((sc-5))
    [ ${#h} -lt 300 ] && sc=$((sc+2)) # scanné : on ne peut pas juger, on garde une chance
    echo "  $code $u score=$sc" >&2
    if [ $sc -gt $bs ]; then bs=$sc; best=$u; cp "$d/c.pdf" "$d/best.pdf"; fi
  done; done; done
  if [ -n "$best" ]; then txt1 "$d/best.pdf" > "$OUT/txt/$code.txt"; st=ok; else st=none; fi
  printf "%s\t%s\t%s\n" "$code" "$st" "$best" > "$OUT/txt/$code.row"; rm -rf "$d"; echo "$code $st $best"; }
export -f probe txt1
grep -oE "^[0-9AB]{2}" tools/fr/probe.txt | xargs -n1 -P4 bash -c 'probe "$0" "'"$OUT"'" "'"$UA"'"'
cat "$OUT"/txt/*.row > "$OUT/index.tsv"; rm -f "$OUT"/txt/*.row
