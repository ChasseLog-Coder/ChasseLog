#!/usr/bin/env bash
# Télécharge les arrêtés d'ouverture/clôture de chaque département (site de la FNC) et en extrait le texte.
set -u
OUT=${OUT:-out}; mkdir -p "$OUT/pdf" "$OUT/txt" "$OUT/row"
one(){ code=$1; nom=$2; slug=$3; OUT=$4
  UA="Mozilla/5.0 (X11; Linux x86_64) ChasseLog-bot (contact.chasselog@gmail.com)"
  page="https://www.chasseurdefrance.com/pratiquer/dates-de-chasse/federation-departementale-$slug/"
  html=$(curl -sL -A "$UA" --max-time 40 "$page")
  pdfs=$(echo "$html" | grep -oE "https://www\.chasseurdefrance\.com/wp-content/uploads/[0-9]{4}/[0-9]{2}/[^\"' ]+\.pdf" | sort -u)
  pdf=$(echo "$pdfs" | grep -E "/FDC-?" | sort -t/ -k6,6 -k7,7 | tail -1); [ -z "$pdf" ] && pdf=$(echo "$pdfs" | tail -1)
  if [ -z "$html" ]; then st="page-ko"; elif [ -z "$pdf" ]; then st="no-pdf"; else
    if curl -sL -A "$UA" --max-time 90 -o "$OUT/pdf/$code.pdf" "$pdf"; then
      pdftotext -layout "$OUT/pdf/$code.pdf" "$OUT/txt/$code.txt" 2>/dev/null
      n=$(wc -c < "$OUT/txt/$code.txt" 2>/dev/null || echo 0)
      if [ "${n:-0}" -lt 1500 ]; then # PDF scanné : OCR (12 premières pages)
        d=$(mktemp -d); pdftoppm -r 150 -gray -png -l 12 "$OUT/pdf/$code.pdf" "$d/p"
        for f in "$d"/p*.png; do timeout 120 tesseract "$f" - -l fra 2>/dev/null; done > "$OUT/txt/$code.txt"; rm -rf "$d"; st="ocr"
      else st="ok"; fi
    else st="dl-ko"; fi
  fi
  printf "%s\t%s\t%s\t%s\t%s\n" "$code" "$nom" "$st" "$pdf" "$page" > "$OUT/row/$code"
  echo "$code $nom $st $pdf"
}
export -f one
tr '\t' '\n' < tools/fr/deps.tsv | xargs -d '\n' -n3 -P6 bash -c 'one "$0" "$1" "$2" '"$OUT"
cat "$OUT"/row/* > "$OUT/index.tsv"; rm -rf "$OUT/pdf" "$OUT/row"
