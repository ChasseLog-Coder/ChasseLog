#!/usr/bin/env bash
# Télécharge les arrêtés d'ouverture/clôture de chaque département (site de la FNC) et en extrait le texte.
set -u
OUT=out; mkdir -p "$OUT/pdf" "$OUT/txt"; : > "$OUT/index.tsv"
UA="Mozilla/5.0 (X11; Linux x86_64) ChasseLog-bot (contact.chasselog@gmail.com)"
while IFS=$'\t' read -r code nom slug; do
  [ -z "$code" ] && continue
  page="https://www.chasseurdefrance.com/pratiquer/dates-de-chasse/federation-departementale-$slug/"
  html=$(curl -sL -A "$UA" --max-time 40 "$page")
  ouv=$(echo "$html" | tr '\n' ' ' | grep -oiE "ouverture[^<]{0,20}</[^>]+>[^<]*<[^>]+>[^<]{0,80}" | head -1 | sed 's/<[^>]*>//g' | tr -s ' ')
  pdfs=$(echo "$html" | grep -oE "https://www\.chasseurdefrance\.com/wp-content/uploads/[0-9]{4}/[0-9]{2}/[^\"' ]+\.pdf" | sort -u)
  pdf=$(echo "$pdfs" | grep -E "/FDC-?" | sort -t/ -k6,6 -k7,7 | tail -1); [ -z "$pdf" ] && pdf=$(echo "$pdfs" | tail -1)
  if [ -z "$html" ]; then st="page-ko"; elif [ -z "$pdf" ]; then st="no-pdf"; else
    if curl -sL -A "$UA" --max-time 90 -o "$OUT/pdf/$code.pdf" "$pdf"; then
      pdftotext -layout "$OUT/pdf/$code.pdf" "$OUT/txt/$code.txt" 2>/dev/null
      n=$(wc -c < "$OUT/txt/$code.txt" 2>/dev/null || echo 0)
      if [ "${n:-0}" -lt 1500 ]; then # PDF scanné : OCR
        rm -rf /tmp/ocr && mkdir /tmp/ocr && pdftoppm -r 220 -gray -png "$OUT/pdf/$code.pdf" /tmp/ocr/p
        for f in /tmp/ocr/p*.png; do tesseract "$f" - -l fra 2>/dev/null; done > "$OUT/txt/$code.txt"; st="ocr"
      else st="ok"; fi
    else st="dl-ko"; fi
  fi
  printf "%s\t%s\t%s\t%s\t%s\t%s\n" "$code" "$nom" "$st" "$pdf" "$page" "$ouv" >> "$OUT/index.tsv"
  echo "$code $nom $st $pdf"; sleep 1
done < tools/fr/deps.tsv
rm -rf "$OUT/pdf"
