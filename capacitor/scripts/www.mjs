// Copie l'app web (racine du dépôt) dans capacitor/www, sans ce qui ne sert qu'au site :
// service worker, manifeste, écran d'installation, images promo, outils.
import { cpSync, rmSync, mkdirSync, existsSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";
const here = dirname(fileURLToPath(import.meta.url)), root = join(here, "..", ".."), www = join(here, "..", "www");
rmSync(www, { recursive: true, force: true }); mkdirSync(www, { recursive: true });
const KEEP = ["index.html", "animals", "weapons", "tour", "lib", "data", "icons"];
for (const k of KEEP) {
  const src = join(root, k);
  if (!existsSync(src)) throw new Error("Fichier manquant pour l'app : " + k);
  cpSync(src, join(www, k), { recursive: true });
}
console.log("www prêt :", KEEP.join(", "));
