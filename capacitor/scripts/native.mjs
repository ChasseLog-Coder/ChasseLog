// Ajustements du projet Android généré par « npx cap add android » :
// autorisations, affichage bord à bord, icônes, écran de démarrage, numéro de version.
// Lancé par le workflow GitHub après « cap add android » et « cap sync ».
import { readFileSync, writeFileSync, existsSync, cpSync, readdirSync, statSync, rmSync, mkdirSync } from "node:fs";
import { join, dirname } from "node:path";
import { fileURLToPath } from "node:url";

const here = dirname(fileURLToPath(import.meta.url)), cap = join(here, ".."), root = join(cap, "..");
const app = join(cap, "android", "app"), main = join(app, "src", "main"), res = join(main, "res");
const cfg = JSON.parse(readFileSync(join(cap, "capacitor.config.json"), "utf8"));
const must = (ok, what) => { if (!ok) throw new Error("Ajustement impossible : " + what); };
const edit = (f, fn) => { const a = readFileSync(f, "utf8"), b = fn(a); if (b !== a) writeFileSync(f, b); return b !== a; };

// 1. Autorisations (la caméra n'est volontairement pas déclarée : la photo passe par l'appareil photo du téléphone)
const manifest = join(main, "AndroidManifest.xml");
must(existsSync(manifest), "AndroidManifest.xml introuvable");
const PERMS = [
  '<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />',
  '<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />',
  '<uses-feature android:name="android.hardware.location.gps" android:required="false" />',
  '<uses-permission android:name="android.permission.VIBRATE" />',
  '<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="29" />'
];
edit(manifest, x => {
  for (const p of PERMS) { const name = p.match(/android:name="([^"]+)"/)[1]; if (!x.includes('"' + name + '"')) x = x.replace("</manifest>", "    " + p + "\n</manifest>"); }
  return x;
});

// 2. Bord à bord (Android 15+) : recommandé par Capacitor quand SystemBars gère les marges
const pkgPath = cfg.appId.split(".");
const act = join(main, "java", ...pkgPath, "MainActivity.java");
must(existsSync(act), "MainActivity.java introuvable (" + act + ")");
writeFileSync(act, `package ${cfg.appId};

import android.os.Bundle;
import androidx.activity.EdgeToEdge;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
    }
}
`);

// 3. Icônes (préparées dans capacitor/res)
cpSync(join(cap, "res"), res, { recursive: true });

// 4. Écran de démarrage : fond crème + logo (remplace les images Capacitor par défaut)
const walk = d => readdirSync(d).flatMap(n => { const p = join(d, n); return statSync(p).isDirectory() ? walk(p) : [p]; });
walk(res).filter(p => /[\\/]splash\.png$/.test(p)).forEach(p => rmSync(p));
mkdirSync(join(res, "drawable"), { recursive: true });
writeFileSync(join(res, "drawable", "splash.xml"), `<?xml version="1.0" encoding="utf-8"?>
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item android:drawable="@color/splash_bg" />
    <item android:width="140dp" android:height="140dp" android:gravity="center" android:drawable="@mipmap/ic_launcher" />
</layer-list>
`);
writeFileSync(join(res, "values", "chasselog_colors.xml"), `<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="splash_bg">#F1EFE6</color>
</resources>
`);
const styles = join(res, "values", "styles.xml");
if (existsSync(styles)) edit(styles, x => x.replace(/(<style name="AppTheme\.NoActionBarLaunch"[^>]*parent="Theme\.SplashScreen"[^>]*>)([\s\S]*?)(<\/style>)/, (m, a, body, c) => {
  if (body.includes("windowSplashScreenBackground")) return m;
  return a + body + `    <item name="windowSplashScreenBackground">@color/splash_bg</item>\n        <item name="windowSplashScreenAnimatedIcon">@mipmap/ic_launcher_foreground</item>\n        <item name="windowSplashScreenIconBackgroundColor">@color/ic_launcher_background</item>\n    ` + c;
}));

// 5. Version : versionCode = numéro de build GitHub, versionName = version de l'app web (APP_V)
const html = readFileSync(join(root, "index.html"), "utf8");
const appV = (html.match(/const APP_V="([^"]+)"/) || [])[1];
must(appV, "APP_V introuvable dans index.html");
const code = parseInt(process.env.VERSION_CODE || "1", 10);
const gradle = join(app, "build.gradle");
must(edit(gradle, x => x.replace(/versionCode(\s*=?\s*)\d+/, "versionCode$1" + code).replace(/versionName(\s*=?\s*)"[^"]*"/, `versionName$1"${appV}"`)) || readFileSync(gradle, "utf8").includes(`"${appV}"`), "version non modifiable dans build.gradle");
// 6. Signature des APK de test (clé publique de test, voir keys/LISEZMOI.txt)
edit(gradle, x => {
  if (x.includes("signingConfigs.chasselogTest")) return x;
  must(/buildTypes\s*\{/.test(x) && /release\s*\{/.test(x), "bloc buildTypes/release introuvable");
  x = x.replace(/(\n\s*)buildTypes\s*\{/, `$1signingConfigs {
        chasselogTest {
            storeFile file("../../keys/chasselog-test.jks")
            storePassword "chasselog-test"
            keyAlias "chasselog-test"
            keyPassword "chasselog-test"
        }
    }$1buildTypes {`);
  return x.replace(/(buildTypes\s*\{[\s\S]*?release\s*\{)/, `$1\n            signingConfig signingConfigs.chasselogTest`);
});
console.log("Projet Android ajusté :", cfg.appId, "version", appV, "(" + code + ")");
