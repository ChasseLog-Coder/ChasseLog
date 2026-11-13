const A=["Jean Dupont","Luc Lambert","Marc Henrard","Pierre Collard","Olivier Gilson","Benoît Masson","Thierry Leroy","Michel Renard","Philippe Hubert","Denis Simon","Éric Laurent","Paul Martin"];
const I=["Antoine Delvaux","Sébastien Noël","Julien Fontaine","Nicolas Wauters","Damien Lejeune","Christophe Remy","Arnaud Pirard","François Lemaire","Xavier Mathieu","Laurent Bodart","Patrick Close","Hugues Jacquet","Yves Dethier","Bruno Gérard","Cédric Piette","Alain Dumont","Fabrice Lecomte","Guy Wilmet"];
const act=A.map((n,i)=>({id:"a"+(i+1),n}));const num={};act.forEach((a,i)=>num[a.id]=String(i+1));I.forEach((n,i)=>num["i:"+n]=String(i+13));
const po=n=>Array.from({length:n},(_,i)=>({n:i+1}));
const fin={d:{b1:{"1":{b:2,s:{Sanglier:3},p:{Sanglier:1}},"2":{b:0,s:{Chevrette:1},p:{}},"3":{b:1,s:{Sanglier:2},p:{Sanglier:1}},"5":{b:3,s:{Sanglier:4},p:{Sanglier:2}},"7":{b:0,s:{},p:{}},"8":{b:1,s:{Brocard:1},p:{Brocard:1}},"12":{b:2,s:{Sanglier:2},p:{}},"14":{b:1,s:{Sanglier:1},p:{Sanglier:1}},"17":{b:0,s:{Biche:2},p:{}},"21":{b:2,s:{Sanglier:3},p:{Sanglier:1}}}}};
const S={ev:{},reg:"be",onb:1,tourAsked:1,tourDone:1,setup:1,regAsked:1,fbAsked:1,nlAsked:1,upNote:"x",newsDocs:true,
 gch:{cur:"c1",L:{c1:{id:"c1",n:"Chasse des Hêtres",act,bat:{b1:{id:"b1",n:"Les Hêtres",po:po(30)},b2:{id:"b2",n:"Fond des Vaulx",po:po(30)}},
 jr:{j1:{id:"j1",date:"2026-11-14",title:"Battue de la Saint-Hubert",mode:"Battue",time:"08:30",pl:"Cabanon des Hêtres",bat:["b1","b2"],inv:I,num,fin}}}}}};
module.exports=S;
