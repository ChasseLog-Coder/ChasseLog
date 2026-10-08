// Service worker : met l'application en cache pour qu'elle fonctionne sans connexion.
// Les fonds de carte (OpenStreetMap) ne sont volontairement PAS mis en cache.
const V = "carnet-v6";
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

// ---- Rappels : lecture de la liste enregistrée par l'application (IndexedDB) ----
function idb() { return new Promise((res, rej) => { const r = indexedDB.open("carnet-photos", 1); r.onupgradeneeded = () => r.result.createObjectStore("p"); r.onsuccess = () => res(r.result); r.onerror = () => rej(r.error); }); }
async function remGet() { const d = await idb(); return new Promise(res => { const q = d.transaction("p").objectStore("p").get("_rem"); q.onsuccess = () => res(q.result ? JSON.parse(q.result) : []); q.onerror = () => res([]); }); }
async function remPut(l) { const d = await idb(); return new Promise(res => { const t = d.transaction("p", "readwrite"); t.objectStore("p").put(JSON.stringify(l), "_rem"); t.oncomplete = () => res(); t.onerror = () => res(); }); }
self.addEventListener("periodicsync", e => {
  if (e.tag !== "rappels") return;
  e.waitUntil((async () => {
    const l = await remGet(), now = Date.now();
    for (const x of l) {
      if (x.fired || x.due > now || now - x.due > 36 * 36e5) continue;
      x.fired = true;
      await self.registration.showNotification("ChasseLog", { body: "Pense à encoder ta fiche : " + x.title + " (" + x.date.split("-").reverse().join("/") + ")", tag: "fiche-" + x.id, vibrate: [250, 120, 250], data: { id: x.id }, icon: "icons/icon-192.png", badge: "icons/icon-192.png" });
    }
    await remPut(l);
  })());
});
self.addEventListener("notificationclick", e => {
  e.notification.close();
  const id = e.notification.data && e.notification.data.id;
  e.waitUntil((async () => {
    const cs = await clients.matchAll({ type: "window", includeUncontrolled: true });
    for (const c of cs) { if ("focus" in c) { await c.focus(); c.postMessage({ fiche: id }); return; } }
    await clients.openWindow("./?fiche=" + encodeURIComponent(id));
  })());
});
