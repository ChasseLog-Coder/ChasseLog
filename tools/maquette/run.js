/* Maquette « Plan / Photo » : capture de la page Sortis à droite avec fond OSM, orthophoto SPW (Wallonie) et IGN (France). Lancé par le robot GitHub (accès aux tuiles). */
const {chromium,devices}=require('playwright');const S=require('./demo.js');const OUT=process.argv[2]||'out';require('fs').mkdirSync(OUT,{recursive:true});
const mk=(c0)=>{const poly=[[0.0050,-0.0040],[0.0090,0.0080],[0.0060,0.0220],[-0.0010,0.0240],[-0.0050,0.0130],[-0.0030,0.0000]].map(([a,b])=>[c0[0]+a,c0[1]+b]);
 const per=poly.map((a,i)=>[a,poly[(i+1)%poly.length]]),pos=n=>{const t=((n-1)/30)*per.length,i=Math.floor(t),f=t-i,[a,b]=per[i%per.length];return[a[0]+(b[0]-a[0])*f,a[1]+(b[1]-a[1])*f]};
 const s=JSON.parse(JSON.stringify(S));Object.values(s.gch.L.c1.bat).forEach(b=>{b.po=b.po.map(p=>{const[la,ln]=pos(p.n);return{n:p.n,lat:la,lng:ln}});b.zn=[{on:1,g:[[poly]]}]});
 const j=s.gch.L.c1.jr.j1;j.date="2026-11-12";j.fin.d.b2={"2":{b:1,s:{Chevrette:2,Brocard:1},p:{Brocard:1}},"6":{b:0,s:{Chevrillard:1},p:{}},"13":{b:2,s:{Sanglier:3},p:{Sanglier:1}},"18":{b:0,s:{Renard:1},p:{}}};return s};
const LAY={osm:null,spw:"spw",ign:"ign"};
const ICO='<svg viewBox="0 0 24 24" width="24" height="24" fill="none" stroke="#3f5a2b" stroke-width="1.9" stroke-linejoin="round"><path d="M12 3 3 8l9 5 9-5-9-5Z" fill="#3f5a2b22"/><path d="m3 12 9 5 9-5"/><path d="m3 16 9 5 9-5"/></svg>';
const URL_SPW="https://geoservices.wallonie.be/arcgis/services/IMAGERIE/ORTHO_LAST/MapServer/WMSServer",URL_IGN="https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&STYLE=normal&TILEMATRIXSET=PM&FORMAT=image/jpeg&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}",URL_OTM="https://{s}.tile.opentopomap.org/{z}/{x}/{y}.png";
(async()=>{const b=await chromium.launch();
 for(const [nom,c0,fond,menu] of [["1-plan",[50.167,4.244],"osm",0],["2-menu",[50.167,4.244],"osm",1],["3-photo",[50.167,4.244],"photo",0],["4-relief",[50.167,4.244],"relief",0],["5-france-photo",[49.80,4.70],"photo",0]]){
  const c=await b.newContext({...devices['Pixel 7'],deviceScaleFactor:2});await c.addInitScript(s=>{localStorage.setItem('carnet2',JSON.stringify(s))},mk(c0));
  const p=await c.newPage();p.on('pageerror',e=>console.log('ERR',e.message));
  await p.goto('http://localhost:8000/index.html');await p.waitForTimeout(2000);
  await p.evaluate(()=>{document.querySelectorAll('#onb,#tour,#setup,#welc,#inst').forEach(e=>e.remove());go("gm");gjId="j1";gdOpen("s");gdO={"b1|Sanglier":1};render()});await p.waitForTimeout(500);
  await p.evaluate(([fond,menu,ICO,SPW,IGN,OTM,c0])=>{const W=window.L;document.head.insertAdjacentHTML('beforeend',`<style>.gdmap{position:relative}.gdly{position:absolute;z-index:500;top:10px;right:10px;width:42px;height:42px;border-radius:50%;border:0;background:#fff;box-shadow:0 1px 5px #0005;display:flex;align-items:center;justify-content:center}
   .gdl3{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin-top:6px}.gdl3 button{border:0;background:none;display:flex;flex-direction:column;align-items:center;gap:6px;font:600 .85rem system-ui;color:var(--ink)}.gdl3 img{width:84px;height:84px;border-radius:16px;object-fit:cover;border:3px solid transparent}.gdl3 .on img{border-color:#3f5a2b}.gdl3 .on{color:#3f5a2b}</style>`);
   const wal=c0[0]>49.45&&c0[0]<50.85&&c0[1]>2.8&&c0[1]<6.45&&c0[0]<50.8;
   const mkL=f=>f=="photo"?(wal?W.tileLayer.wms(SPW,{layers:"0",format:"image/jpeg",version:"1.3.0",maxZoom:19,attribution:"Orthophoto © SPW"}):W.tileLayer(IGN,{maxZoom:19,attribution:"Orthophoto © IGN"})):f=="relief"?W.tileLayer(OTM,{maxZoom:17,attribution:"© OpenStreetMap · relief © OpenTopoMap (CC-BY-SA)"}):null;
   gdM.forEach(m=>{const el=m.getContainer();el.insertAdjacentHTML('beforeend',`<button class="gdly" aria-label="Type de carte">${ICO}</button>`);const L2=mkL(fond);if(L2){m.eachLayer(l=>{if(l instanceof W.TileLayer)m.removeLayer(l)});L2.addTo(m);L2.bringToBack()}});
   if(menu){const z=14,n=2**z,x=Math.floor((c0[1]+180)/360*n),la=c0[0]*Math.PI/180,y=Math.floor((1-Math.log(Math.tan(la)+1/Math.cos(la))/Math.PI)/2*n),R=6378137*Math.PI,t=2*R/n,bb=[-R+x*t,R-(y+1)*t,-R+(x+1)*t,R-y*t].join(",");
    const th={osm:`https://tile.openstreetmap.org/${z}/${x}/${y}.png`,photo:`${SPW}?SERVICE=WMS&REQUEST=GetMap&VERSION=1.3.0&LAYERS=0&STYLES=&FORMAT=image/jpeg&CRS=EPSG:3857&WIDTH=256&HEIGHT=256&BBOX=${bb}`,relief:`https://a.tile.opentopomap.org/${z}/${x}/${y}.png`};
    modal(`<h2 style="margin-top:0">Type de carte</h2><div class="gdl3">${[["osm","Plan"],["photo","Photo"],["relief","Relief"]].map(([k,l])=>`<button class="${k==fond?"on":""}"><img src="${th[k]}" alt="">${l}</button>`).join("")}</div>`)}},[fond,menu,ICO,URL_SPW,URL_IGN,URL_OTM,c0]);
  await p.waitForTimeout(7000);await p.screenshot({path:`${OUT}/${nom}.png`});
  const box=await p.$('.gdmap');if(box&&!menu)await box.screenshot({path:`${OUT}/${nom}-carte.png`});await c.close()}
 await b.close()})();
