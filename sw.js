// Service worker : met l'application en cache pour qu'elle fonctionne sans connexion.
// Les fonds de carte (OpenStreetMap) ne sont volontairement PAS mis en cache.
const V = "carnet-v2";
const FILES = [
  "./", "index.html", "manifest.webmanifest",
  "icons/icon-192.png", "icons/icon-512.png", "icons/maskable-512.png", "icons/apple-touch-icon.png",
  "lib/leaflet/leaflet.js", "lib/leaflet/leaflet.css",
  "lib/leaflet/images/marker-icon.png", "lib/leaflet/images/marker-icon-2x.png",
  "lib/leaflet/images/marker-shadow.png", "lib/leaflet/images/layers.png", "lib/leaflet/images/layers-2x.png"
];
self.addEventListener("install", e => {
  e.waitUntil(caches.open(V).then(c => c.addAll(FILES)).then(() => self.skipWaiting()));
});
self.addEventListener("activate", e => {
  e.waitUntil(caches.keys().then(k => Promise.all(k.filter(x => x !== V).map(x => caches.delete(x)))).then(() => self.clients.claim()));
});
self.addEventListener("fetch", e => {
  const r = e.request, u = new URL(r.url);
  if (r.method !== "GET" || u.origin !== location.origin) return;
  if (r.mode === "navigate") {
    // En ligne : version la plus récente. Hors ligne : version en cache.
    e.respondWith(fetch(r).then(x => { const y = x.clone(); caches.open(V).then(c => c.put("index.html", y)); return x; })
      .catch(() => caches.match("index.html")));
    return;
  }
  e.respondWith(caches.match(r).then(m => m || fetch(r).then(x => { const y = x.clone(); caches.open(V).then(c => c.put(r, y)); return x; })));
});
