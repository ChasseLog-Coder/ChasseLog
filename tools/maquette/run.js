/* Maquette « Plan / Photo » : capture de la page Sortis à droite avec fond OSM, orthophoto SPW (Wallonie) et IGN (France). Lancé par le robot GitHub (accès aux tuiles). */
const {chromium,devices}=require('playwright');const S=require('./demo.js');const OUT=process.argv[2]||'out';require('fs').mkdirSync(OUT,{recursive:true});
const mk=(c0)=>{const poly=[[0.0050,-0.0040],[0.0090,0.0080],[0.0060,0.0220],[-0.0010,0.0240],[-0.0050,0.0130],[-0.0030,0.0000]].map(([a,b])=>[c0[0]+a,c0[1]+b]);
 const per=poly.map((a,i)=>[a,poly[(i+1)%poly.length]]),pos=n=>{const t=((n-1)/30)*per.length,i=Math.floor(t),f=t-i,[a,b]=per[i%per.length];return[a[0]+(b[0]-a[0])*f,a[1]+(b[1]-a[1])*f]};
 const s=JSON.parse(JSON.stringify(S));Object.values(s.gch.L.c1.bat).forEach(b=>{b.po=b.po.map(p=>{const[la,ln]=pos(p.n);return{n:p.n,lat:la,lng:ln}});b.zn=[{on:1,g:[[poly]]}]});
 const j=s.gch.L.c1.jr.j1;j.date="2026-11-12";j.fin.d.b2={"2":{b:1,s:{Chevrette:2,Brocard:1},p:{Brocard:1}},"6":{b:0,s:{Chevrillard:1},p:{}},"13":{b:2,s:{Sanglier:3},p:{Sanglier:1}},"18":{b:0,s:{Renard:1},p:{}}};return s};
const LAY={osm:null,spw:"spw",ign:"ign"};
(async()=>{const b=await chromium.launch();
 for(const [nom,c0,fond] of [["wallonie-plan",[50.167,4.244],"osm"],["wallonie-photo",[50.167,4.244],"spw"],["france-photo",[49.80,4.70],"ign"]]){
  const c=await b.newContext({...devices['Pixel 7'],deviceScaleFactor:2});await c.addInitScript(s=>{localStorage.setItem('carnet2',JSON.stringify(s))},mk(c0));
  const p=await c.newPage();p.on('pageerror',e=>console.log('ERR',e.message));p.on('requestfailed',r=>{if(!/posthog/.test(r.url()))console.log('ECHEC',r.url().slice(0,140))});
  await p.goto('http://localhost:8000/index.html');await p.waitForTimeout(2000);
  await p.evaluate(()=>{document.querySelectorAll('#onb,#tour,#setup,#welc,#inst').forEach(e=>e.remove());go("gm");gjId="j1";gdOpen("s");gdO={"b1|Sanglier":1};render()});await p.waitForTimeout(500);
  await p.evaluate(fond=>{const W=window.L;document.head.insertAdjacentHTML('beforeend','<style>.gdmap{position:relative}.gdsw{position:absolute;z-index:500;top:8px;right:8px;display:flex;background:#fff;border-radius:999px;box-shadow:0 1px 4px #0004;overflow:hidden}.gdsw button{border:0;background:none;padding:5px 11px;font:600 .78rem system-ui;color:#3f5a2b}.gdsw button.on{background:#3f5a2b;color:#fff}</style>');
   gdM.forEach(m=>{const el=m.getContainer();el.insertAdjacentHTML('beforeend',`<div class="gdsw"><button class="${fond=="osm"?"on":""}">Plan</button><button class="${fond!="osm"?"on":""}">Photo</button></div>`);
    if(fond=="osm")return;m.eachLayer(l=>{if(l instanceof W.TileLayer)m.removeLayer(l)});
    const t=fond=="spw"?W.tileLayer.wms("https://geoservices.wallonie.be/arcgis/services/IMAGERIE/ORTHO_LAST/MapServer/WMSServer",{layers:"0",format:"image/jpeg",version:"1.3.0",maxZoom:19,attribution:"Orthophoto © SPW"}):W.tileLayer("https://data.geopf.fr/wmts?SERVICE=WMTS&REQUEST=GetTile&VERSION=1.0.0&LAYER=ORTHOIMAGERY.ORTHOPHOTOS&STYLE=normal&TILEMATRIXSET=PM&FORMAT=image/jpeg&TILEMATRIX={z}&TILEROW={y}&TILECOL={x}",{maxZoom:19,attribution:"Orthophoto © IGN"});
    t.addTo(m);t.bringToBack()})},fond);
  await p.waitForTimeout(6000);await p.screenshot({path:`${OUT}/${nom}.png`,fullPage:true});
  const box=await p.$('.gdmap');if(box)await box.screenshot({path:`${OUT}/${nom}-carte.png`});await c.close()}
 await b.close()})();
