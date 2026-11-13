/* ChasseLog – relais du QR code du cabanon (Cloudflare Workers + D1, offre gratuite).
   Une société = une clé publique k (dans le QR code) + un secret gestionnaire a (jamais imprimé, seul son hash est stocké).
   Une seule journée ouverte à la fois par société ; envois acceptés jusqu'à minuit (heure belge) ; tout est effacé à la fermeture par le gestionnaire, ou 7 jours après minuit. */
const ORIG=[/^https:\/\/chasselog\.be$/,/^https:\/\/chasselog-coder\.github\.io$/,/^https:\/\/raw\.githack\.com$/,/^https?:\/\/localhost(:\d+)?$/,/^capacitor:\/\/localhost$/];
const GARDE=7*864e5,MAXF=8000,MAXJ=60000,MAXN=200,KEY=/^[A-Za-z0-9_-]{16,64}$/;
const now=()=>Date.now();
const hash=async s=>[...new Uint8Array(await crypto.subtle.digest("SHA-256",new TextEncoder().encode(s)))].map(b=>b.toString(16).padStart(2,"0")).join("");
/* minuit suivant la date de la journée, à l'heure de Bruxelles */
function minuit(date){const d=new Date(date+"T12:00:00Z");d.setUTCDate(d.getUTCDate()+1);const t=Date.UTC(d.getUTCFullYear(),d.getUTCMonth(),d.getUTCDate());
 const off=new Date(t).toLocaleString("en-US",{timeZone:"Europe/Brussels",hour:"2-digit",hour12:false});return t-((+off%24)||0)*3600e3}
function cors(req){const o=req.headers.get("Origin")||"";return ORIG.some(r=>r.test(o))?{"Access-Control-Allow-Origin":o,"Access-Control-Allow-Methods":"GET,POST,OPTIONS","Access-Control-Allow-Headers":"Content-Type","Access-Control-Max-Age":"86400","Vary":"Origin"}:{}}
const out=(req,o,st=200)=>new Response(JSON.stringify(o),{status:st,headers:{"Content-Type":"application/json; charset=utf-8","Cache-Control":"no-store",...cors(req)}});
async function body(req,max){const t=await req.text();if(t.length>max)throw new Error("trop gros");return JSON.parse(t||"{}")}
async function soc(env,k){return k&&KEY.test(k)?await env.DB.prepare("SELECT * FROM soc WHERE k=?").bind(k).first():null}
async function admin(env,b){const s=await soc(env,b.k);if(!s||!b.a||s.ah!==await hash(b.a))return null;return s}
const ouvert=s=>s&&s.open&&s.until>now();
async function purge(env,k){await env.DB.batch([env.DB.prepare("DELETE FROM fic WHERE k=?").bind(k),env.DB.prepare("UPDATE soc SET open=0,j=NULL,until=0 WHERE k=?").bind(k)])}

export default{
 async fetch(req,env){const u=new URL(req.url),p=u.pathname;
  if(req.method=="OPTIONS")return new Response(null,{status:204,headers:cors(req)});
  try{
   if(p=="/"||p=="/ping")return out(req,{ok:true,relais:"chasselog",t:now()});
   /* participant : lire la journée ouverte (QR code) */
   if(p=="/j"&&req.method=="GET"){const s=await soc(env,u.searchParams.get("k"));if(!s)return out(req,{ok:false,err:"inconnu"},404);
    if(!ouvert(s))return out(req,{ok:true,open:false});const j=JSON.parse(s.j||"{}");return out(req,{ok:true,open:true,j})}
   /* participant : envoyer / corriger sa fiche */
   if(p=="/f"&&req.method=="POST"){const b=await body(req,MAXF),s=await soc(env,b.k);if(!s)return out(req,{ok:false,err:"inconnu"},404);
    if(!ouvert(s))return out(req,{ok:false,err:"ferme"},409);const j=JSON.parse(s.j||"{}");
    if(!b.p||typeof b.p!="string"||b.p.length>80||b.jid!==j.id)return out(req,{ok:false,err:"fiche"},400);
    const n=await env.DB.prepare("SELECT COUNT(*) n FROM fic WHERE k=?").bind(s.k).first("n");
    const ex=await env.DB.prepare("SELECT 1 FROM fic WHERE k=? AND p=?").bind(s.k,b.p).first();if(!ex&&n>=MAXN)return out(req,{ok:false,err:"plein"},429);
    await env.DB.prepare("INSERT INTO fic(k,p,d,ts) VALUES(?,?,?,?) ON CONFLICT(k,p) DO UPDATE SET d=excluded.d,ts=excluded.ts").bind(s.k,b.p,JSON.stringify(b.d||{}),now()).run();
    return out(req,{ok:true})}
   /* gestionnaire : créer la société (premier usage) ou ouvrir une journée */
   if(p=="/open"&&req.method=="POST"){const b=await body(req,MAXJ);if(!KEY.test(b.k||"")||!b.a||!b.j||!b.j.id||!/^\d{4}-\d{2}-\d{2}$/.test(b.j.date||""))return out(req,{ok:false,err:"donnees"},400);
    const until=minuit(b.j.date);if(until<=now())return out(req,{ok:false,err:"passee"},409);
    let s=await soc(env,b.k);const ah=await hash(b.a);
    if(s&&s.ah!==ah)return out(req,{ok:false,err:"refuse"},403);
    if(!s)await env.DB.prepare("INSERT INTO soc(k,ah,ts) VALUES(?,?,?)").bind(b.k,ah,now()).run();
    const prev=s&&s.j?JSON.parse(s.j).id:null;if(prev&&prev!==b.j.id)await env.DB.prepare("DELETE FROM fic WHERE k=?").bind(b.k).run();
    await env.DB.prepare("UPDATE soc SET j=?,open=1,until=?,ts=? WHERE k=?").bind(JSON.stringify(b.j),until,now(),b.k).run();
    return out(req,{ok:true,until})}
   /* gestionnaire : lire les fiches reçues (depuis since) */
   if(p=="/fiches"&&req.method=="POST"){const b=await body(req,2000),s=await admin(env,b);if(!s)return out(req,{ok:false,err:"refuse"},403);
    const r=await env.DB.prepare("SELECT p,d,ts FROM fic WHERE k=? AND ts>? ORDER BY ts").bind(s.k,+b.since||0).all();
    return out(req,{ok:true,open:ouvert(s),until:s.until,f:r.results.map(x=>({p:x.p,d:JSON.parse(x.d),ts:x.ts}))})}
   /* gestionnaire : fermer (efface tout) */
   if(p=="/close"&&req.method=="POST"){const b=await body(req,2000),s=await admin(env,b);if(!s)return out(req,{ok:false,err:"refuse"},403);await purge(env,s.k);return out(req,{ok:true})}
   /* gestionnaire : remplacer le QR code (l'ancien cesse de fonctionner) */
   if(p=="/rotate"&&req.method=="POST"){const b=await body(req,2000),s=await admin(env,b);if(!s||!KEY.test(b.nk||""))return out(req,{ok:false,err:"refuse"},403);
    if(await soc(env,b.nk))return out(req,{ok:false,err:"existe"},409);
    await env.DB.batch([env.DB.prepare("DELETE FROM fic WHERE k=?").bind(s.k),env.DB.prepare("UPDATE soc SET k=?,open=0,j=NULL,until=0,ts=? WHERE k=?").bind(b.nk,now(),s.k)]);return out(req,{ok:true})}
   return out(req,{ok:false,err:"route"},404)
  }catch(e){return out(req,{ok:false,err:"erreur",m:String(e.message||e).slice(0,120)},400)}},
 /* chaque heure : efface les journées 7 jours après minuit si le gestionnaire ne l'a pas fait (les envois s'arrêtent à minuit) */
 async scheduled(ev,env){const r=await env.DB.prepare("SELECT k FROM soc WHERE until>0 AND until<=?").bind(now()-GARDE).all();for(const x of r.results)await purge(env,x.k)}
};
