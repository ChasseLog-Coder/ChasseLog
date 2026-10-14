#!/usr/bin/env python3
"""Assemble data/ouv-fr.json à partir des fiches départementales tools/fr/src/XX.json.
Usage : python3 tools/fr/build.py [--saison 2026-2027]
Format d'une fiche : {"ouv":"AAAA-MM-JJ","clo":"AAAA-MM-JJ","n":"note","src":"url optionnelle",
  "sp":{"Chevreuil":{"aa":[[début,fin]],"bat":[[début,fin]],"n":"note"},"Lièvre":{"g":[[début,fin]],"n":"..."}}}
aa = approche/affût, bat = battue/chasse collective, g = petit gibier et oiseaux (tous modes)."""
import json,os,re,sys,datetime
H=os.path.dirname(os.path.abspath(__file__));ROOT=os.path.dirname(os.path.dirname(H))
saison=sys.argv[sys.argv.index("--saison")+1] if "--saison" in sys.argv else "2026-2027"
y=int(saison[:4]);W=[f"{y}-06-01",f"{y+1}-06-30"]
SP=["Cerf","Chevreuil","Daim","Mouflon","Chamois","Sanglier","Lièvre","Lapin","Faisan","Perdrix","Caille","Bécasse","Pigeon ramier","Renard"]
deps=[l.split("\t") for l in open(H+"/deps.tsv").read().strip().split("\n")]
src={}
for p in ["index.tsv","probe-index.tsv"]:
    if os.path.exists(H+"/"+p):
        for l in open(H+"/"+p):
            c=l.rstrip("\n").split("\t")
            u=[x for x in c if x.startswith("http") and x.endswith(".pdf")]
            if u and "Recettes" not in u[0]: src[c[0]]=u[0]
D=re.compile(r"^\d{4}-\d{2}-\d{2}$");err=[];out={}
def chk(c,k,rs):
    for a,b in rs:
        if not(D.match(a) and D.match(b)): err.append(f"{c} {k}: date invalide {a}..{b}");continue
        if a>b: err.append(f"{c} {k}: début après fin {a}>{b}")
        if a<W[0] or b>W[1]: err.append(f"{c} {k}: hors saison {a}..{b}")
for code,nom,_ in deps:
    p=f"{H}/src/{code}.json"
    if not os.path.exists(p): continue
    d=json.load(open(p))
    for k in ("ouv","clo"):
        if not D.match(d.get(k,"")): err.append(f"{code}: {k} invalide")
    sp={}
    for k,v in d["sp"].items():
        if k not in SP: err.append(f"{code}: espèce inconnue {k}");continue
        o={}
        for m in ("aa","bat","g"):
            if v.get(m): chk(code,k,v[m]);o[m]=v[m]
        if v.get("x"): o={"g":[]}  # espèce interdite cette saison
        if not o:  # oiseaux de passage sans dates : dates nationales
            if k=="Bécasse": o["g"]=[[d["ouv"],f"{y+1}-02-20"]]
            elif k=="Pigeon ramier": o["g"]=[[d["ouv"],f"{y+1}-02-10"]]
            else: err.append(f"{code}: {k} sans dates");continue
        if v.get("n"): o["n"]=v["n"]
        sp[k]=o
    if "Pigeon ramier" not in sp: sp["Pigeon ramier"]={"g":[[d["ouv"],f"{y+1}-02-10"]],"n":"Date de fermeture nationale (arrêté ministériel)."}
    if "Bécasse" not in sp: sp["Bécasse"]={"g":[[d["ouv"],f"{y+1}-02-20"]],"n":"Date de fermeture nationale. 30 bécasses par saison (PMA national)."}
    e={"ouv":d["ouv"],"clo":d["clo"],"src":d.get("src") or src.get(code,""),"sp":sp}
    if d.get("n"): e["n"]=d["n"]
    out[code]=e
nat=[{"l":"Canards, oies, limicoles et rallidés","r":[[f"{y}-08-21",f"{y+1}-01-31"]],"n":"Dates nationales indicatives (marais, étangs, cours d'eau). Sur le domaine public maritime, ouverture début août. Certaines espèces ouvrent plus tard : vérifie l'arrêté ministériel et celui de ton département."}]
J={"saison":saison,"maj":datetime.date.today().isoformat(),"win":W,"dep":out,"nat":nat}
os.makedirs(ROOT+"/data",exist_ok=True)
json.dump(J,open(ROOT+"/data/ouv-fr.json","w"),ensure_ascii=False,separators=(",",":"))
print(f"{len(out)}/{len(deps)} départements · {os.path.getsize(ROOT+'/data/ouv-fr.json')//1024} Ko")
missing=[c for c,_,_ in deps if c not in out]
if missing: print("Manquants :"," ".join(missing))
if err: print("ERREURS :\n "+"\n ".join(err)); sys.exit(1)
