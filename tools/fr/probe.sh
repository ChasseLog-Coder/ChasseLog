#!/usr/bin/env bash
# Départements dont la page FNC n'a pas été trouvée : essaie les variantes d'adresse de la page
# (de-, de-la-, du-, des-, d', de-l'), garde la page qui lie un arrêté FDC-XX*.pdf, puis extrait le texte
# (OCR à 300 dpi des pages scannées). Résultats : probe/txt/XX.txt et probe/index.tsv.
set -u; OUT=${OUT:-probe}; mkdir -p "$OUT/txt"
UA="Mozilla/5.0 (X11; Linux x86_64) ChasseLog-bot (contact.chasselog@gmail.com)"
txt1(){ np=$(pdfinfo "$1" 2>/dev/null | awk '/^Pages:/{print $2}'); [ -z "$np" ] && np=0; [ "$np" -gt 14 ] && np=14
  for p in $(seq 1 $np); do echo "=== page $p"; t=$(pdftotext -layout -f $p -l $p "$1" - 2>/dev/null)
    if [ $(echo "$t" | tr -d ' \n' | wc -c) -gt 400 ]; then echo "$t"; else d=$(mktemp -d); pdftoppm -r 300 -gray -png -f $p -l $p "$1" "$d/p"; timeout 240 tesseract "$d"/p*.png - -l fra --psm 1 2>/dev/null; rm -rf "$d"; fi; done; }
probe(){ code=$1; OUT=$2; UA="$3"
  nom=$(awk -F'\t' -v c="$code" '$1==c{print $2}' tools/fr/deps.tsv)
  b=$(echo "$nom" | iconv -f utf8 -t ascii//TRANSLIT | tr 'A-Z' 'a-z' | sed "s/[' ]/-/g; s/--*/-/g")
  b2=$(echo "$b" | tr -d '-' ); b3=$(echo "$b" | sed 's/-/ /g' | awk '{print $1}')
  n=$code; [ "$code" = "2A" ] && n="20-A"; [ "$code" = "2B" ] && n="20-B"
  pdf=""; page=""
  for s in "de-$b" "de-la-$b" "du-$b" "des-$b" "d$b" "de-l$b" "de-l-$b" "$b" "de-la-$(echo $b | sed 's/-/-/g')" ; do
    u="https://www.chasseurdefrance.com/pratiquer/dates-de-chasse/federation-departementale-$s/"
    h=$(curl -sL -A "$UA" --max-time 30 "$u")
    p=$(echo "$h" | grep -oE "https://www\.chasseurdefrance\.com/wp-content/uploads/[0-9]{4}/[0-9]{2}/FDC-?$n[^\"' ]*\.pdf" | sort -t/ -k6,6 -k7,7 | tail -1)
    if [ -n "$p" ]; then pdf=$p; page=$u; break; fi
  done
  if [ -n "$pdf" ]; then d=$(mktemp -d); curl -sL -A "$UA" --max-time 120 -o "$d/a.pdf" "$pdf"; txt1 "$d/a.pdf" > "$OUT/txt/$code.txt"; rm -rf "$d"; st=ok; else st=none; fi
  printf "%s\t%s\t%s\t%s\t%s\n" "$code" "$nom" "$st" "$pdf" "$page" > "$OUT/txt/$code.row"; echo "$code $st $pdf"; }
export -f probe txt1
grep -oE "^[0-9AB]{2}" tools/fr/probe.txt | xargs -n1 -P6 bash -c 'probe "$0" "'"$OUT"'" "'"$UA"'"'
cat "$OUT"/txt/*.row > "$OUT/index.tsv"; rm -f "$OUT"/txt/*.row
