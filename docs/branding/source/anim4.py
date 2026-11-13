import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import brand as B
H=B.H; u='b'
VIO='#8B6CFF'
mark=B.mark(u,'#FFFFFF',VIO)
i=mark.index('\n'); Opart=mark[:i]; ring=mark[i+1:]
ring=ring.replace(f'<circle cx="{B.bx:.1f}" cy="{B.by:.1f}" r="{B.Ro*0.105:.1f}" fill="{VIO}"/>','')
ell=f'M{-B.rx:.1f} 0 A{B.rx:.1f} {B.ry:.1f} 0 1 0 {B.rx:.1f} 0 A{B.rx:.1f} {B.ry:.1f} 0 1 0 {-B.rx:.1f} 0'
a_,b_=B.rx,B.ry; ELL=math.pi*(3*(a_+b_)-math.sqrt((3*a_+b_)*(a_+3*b_)))
letters=''.join(f'<g fill="#FFFFFF">{"".join(e)}</g>' for n,e in enumerate(B.LETTERS) if n<5)
playL=''.join(f'<g fill="{VIO}">{"".join(e)}</g>' for n,e in enumerate(B.LETTERS) if n>=5)
_,XP=B.font_word(B.H,parts=(('rbita',B.FONT1),)); XP+=B.H*0.03
lettersMask=''.join(''.join(e) for e in B.LETTERS)
l,tp,rr,bt=B.ICON[0],B.ICON[1],B.TEXT_W,1.37*H; w=rr-l; hh=bt-tp
NT=34
tail=''.join(f'<circle class="tl" r="0" fill="{"#FFFFFF" if k<2 else "#B49CFF"}"/>' for k in range(NT))
svg=f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="{l:.1f} {tp:.1f} {w:.1f} {hh:.1f}" style="width:860px;overflow:visible">
{B.defs(u,'').replace('<path transform=', '<path id="cutP" stroke-dasharray="'+format(ELL/2+5,'.1f')+' '+format(ELL,'.1f')+'" stroke-dashoffset="'+format(ELL/2+5,'.1f')+'" transform=',1)}
<defs>
 <mask id="revR" maskUnits="userSpaceOnUse" {B.BIG}><path id="revRp" transform="{B.RT}" d="{ell}" fill="none" stroke="#fff" stroke-width="{B.Ro*0.3:.1f}" stroke-dasharray="{ELL:.1f} {ELL:.1f}" stroke-dashoffset="{ELL:.1f}"/></mask>
 <mask id="wipe" maskUnits="userSpaceOnUse" {B.BIG}><rect id="wipeR" x="{H*0.98:.0f}" y="-3000" width="0" height="7000" fill="#fff"/></mask>
 <mask id="txt" maskUnits="userSpaceOnUse" {B.BIG}><g fill="#fff">{lettersMask}</g></mask>
 <linearGradient id="shine"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".8"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
 <filter id="pglowF" x="-20%" y="-50%" width="140%" height="200%"><feGaussianBlur stdDeviation="60"/></filter>
 <radialGradient id="cg"><stop offset="0" stop-color="#fff" stop-opacity="1"/><stop offset=".25" stop-color="#C9B8FF" stop-opacity=".8"/><stop offset="1" stop-color="#8B6CFF" stop-opacity="0"/></radialGradient>
</defs>
<g id="all">
 <g id="Og">{Opart}</g>
 <g id="Rg" mask="url(#revR)">{ring}</g>
 <g id="comet">{tail}<circle id="cglow" r="{B.Ro*0.42:.0f}" fill="url(#cg)"/><circle id="chead" r="{B.Ro*0.105:.1f}" fill="#fff"/></g>
 <circle id="ripple" r="0" fill="none" stroke="#B49CFF" stroke-width="10" opacity="0"/>
 <g mask="url(#wipe)"><g id="letters">{letters}</g></g>
 <g id="playG" opacity="0"><g id="playGlow" filter="url(#pglowF)" opacity="0">{playL}</g>{playL}</g>
 <g>
 <g mask="url(#txt)"><rect id="shineR" x="0" y="{tp:.1f}" width="{H*0.9:.1f}" height="{hh:.1f}" fill="url(#shine)" transform="skewX(-20)" opacity="0"/></g></g>
</g></svg>'''
G=dict(DA=(rr-(XP-B.H*0.03))/2,H0=B.H*0.98,XP=XP,YP=B.H*0.6,cx=B.cx,cy=B.cy,ang=B.ang,off=B.off,rx=B.rx,ry=B.ry,Ro=B.Ro,ri=B.ri,gapB=B.gapB,ELL=ELL,DX=(l+rr)/2-500,W=rr,
       endTh=33.0)
import json
html=f'''<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1920px;height:1080px;overflow:hidden;background:#040306}}
#bg{{position:absolute;inset:0;background:radial-gradient(70% 80% at 50% 50%,#0E0919 0%,#06040B 60%,#020104 100%)}}
#halo{{position:absolute;left:50%;top:50%;width:900px;height:900px;margin:-450px 0 0 -450px;border-radius:50%;background:radial-gradient(circle,#7C5CFF2e,#7C5CFF00 62%)}}
#wrap{{position:absolute;inset:0;z-index:2;display:flex;align-items:center;justify-content:center}}
#stars{{position:absolute;inset:0}}
.neb{{position:absolute;border-radius:50%;filter:blur(60px)}}
#n1{{width:1100px;height:700px;left:-200px;top:-150px;background:radial-gradient(closest-side,#4B2A9A55,#4B2A9A00)}}
#n2{{width:1000px;height:800px;right:-250px;bottom:-250px;background:radial-gradient(closest-side,#1F4FA644,#1F4FA600)}}
#n3{{width:800px;height:500px;left:560px;top:290px;background:radial-gradient(closest-side,#7C5CFF33,#7C5CFF00)}}
#orbs{{position:absolute;inset:0}}
#vig{{position:absolute;inset:0;background:radial-gradient(75% 85% at 50% 50%,#0000 55%,#000000d0 100%)}}
</style></head><body><div id="bg"></div>
<div class="neb" id="n1"></div><div class="neb" id="n2"></div><div class="neb" id="n3"></div>
<canvas id="stars" width="1920" height="1080"></canvas>
<svg id="orbs" viewBox="0 0 1920 1080"><g transform="translate(960 540) rotate(-47)" fill="none" stroke="#9B84FF">
<ellipse id="o1" rx="1150" ry="250" stroke-opacity=".14" stroke-width="2"/><ellipse id="o2" rx="1500" ry="330" stroke-opacity=".10" stroke-width="2" stroke-dasharray="4 14"/>
<ellipse id="o3" rx="1900" ry="420" stroke-opacity=".05" stroke-width="2"/><circle id="sat" r="5" fill="#C9B8FF" stroke="none" opacity=".6"/></g></svg>
<div id="vig"></div><div id="halo"></div><div id="wrap">{svg}</div><div id="flash" style="position:absolute;left:50%;top:50%;width:1600px;height:1600px;margin:-800px 0 0 -800px;border-radius:50%;z-index:3;background:radial-gradient(circle,#ffffff 0%,#C9B8FF66 12%,#7C5CFF22 30%,#7C5CFF00 60%);opacity:0;pointer-events:none"></div><div id="fadeout" style="position:absolute;inset:0;z-index:5;background:#000;opacity:0"></div>
<script>
const G={json.dumps(G)};
const cl=x=>Math.max(0,Math.min(1,x)), seg=(t,a,b)=>cl((t-a)/(b-a));
const eo=x=>1-Math.pow(1-x,3), eio=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const back=x=>{{const c1=1.70158,c3=c1+1;return 1+c3*Math.pow(x-1,3)+c1*Math.pow(x-1,2);}};
const $=id=>document.getElementById(id); const TL=[...document.querySelectorAll('.tl')];
const A=G.ang*Math.PI/180, T0=1.6, T1=2.55, TOTAL=360+180-G.endTh;
function theta(t){{return 180-TOTAL*eio(seg(t,T0,T1));}}
function pos(th){{const r=th*Math.PI/180, lx=G.rx*Math.cos(r)+G.off, ly=G.ry*Math.sin(r);
  return [G.cx+lx*Math.cos(A)-ly*Math.sin(A), G.cy+lx*Math.sin(A)+ly*Math.cos(A), Math.sin(r)];}}
function vis(p){{const d=Math.hypot(p[0]-G.cx,p[1]-G.cy); if(p[2]>=0) return 1;
  if(d<G.ri-G.gapB*0.8) return 1; if(d<G.Ro+G.gapB) return 0; return 1;}}

// --- background: seeded starfield, drifting nebula, slow background orbits
let seed=1337; const rnd=()=>{{seed=(seed*16807)%2147483647; return seed/2147483647;}};
const STARS=[...Array(420)].map(()=>({{x:rnd()*1920,y:rnd()*1080,z:.25+rnd()*.75,ph:rnd()*6.28,sp:1+rnd()*3,
   c:rnd()<.15?'201,184,255':(rnd()<.2?'160,200,255':'255,255,255')}}));
const cv=document.getElementById('stars'), cx2=cv.getContext('2d');
function WARPPOS(t){{ let p=0; for(let k=0;k<60;k++){{ const u=t*k/60; p+=(0.08+2.6*Math.exp(-Math.pow((u-0.95)/0.42,2)))*t/60; }} return p; }}
function drawBg(t){{
  cx2.clearRect(0,0,1920,1080);
  const zoom=1+0.035*t;                       // slow push-in
  // warp: stars stream outward fast, then settle into a calm field
  const wv=Math.exp(-Math.pow((t-0.95)/0.42,2));            // warp intensity (peak ~0.95s)
  const settle=Math.min(1,Math.max(0,(t-1.35)/0.5));
  for(const s of STARS){{
    const dx=s.x-960, dy=s.y-540, d=Math.hypot(dx,dy)+1, ux=dx/d, uy=dy/d;
    const travel=(WARPPOS(t)*s.z*900)%1150;
    if(settle<1){{ const rr=((d+travel)%1150)+40, len=Math.min(rr*0.8,10+620*wv*s.z);
      const x1=960+ux*rr, y1=540+uy*rr, x0=960+ux*Math.max(0,rr-len), y0=540+uy*Math.max(0,rr-len);
      const g=cx2.createLinearGradient(x0,y0,x1,y1); const al=(1-settle)*Math.min(1,t/0.25)*(.25+.75*s.z)*Math.min(1,rr/350);
      g.addColorStop(0,'rgba('+s.c+',0)'); g.addColorStop(1,'rgba('+s.c+','+al.toFixed(3)+')');
      cx2.strokeStyle=g; cx2.lineWidth=.6+1.6*s.z; cx2.beginPath(); cx2.moveTo(x0,y0); cx2.lineTo(x1,y1); cx2.stroke(); }}
    if(settle<=0) continue;
    const x=960+(s.x-960)*(1+(zoom-1)*s.z*2), y=540+(s.y-540)*(1+(zoom-1)*s.z*2);
    const tw=.55+.45*Math.sin(t*s.sp+s.ph); const a=settle*s.z*tw*.9;
    const r=.5+1.3*s.z*s.z;
    cx2.fillStyle='rgba('+s.c+','+a.toFixed(3)+')'; cx2.beginPath(); cx2.arc(x,y,r,0,6.283); cx2.fill();
    if(s.z>.93){{ cx2.fillStyle='rgba('+s.c+','+(a*.15).toFixed(3)+')'; cx2.beginPath(); cx2.arc(x,y,r*4,0,6.283); cx2.fill(); }}
  }}
  const f=Math.min(1,t/1.2);
  document.getElementById('n1').style.transform='translate('+(30*t)+'px,'+(12*t)+'px)'; document.getElementById('n1').style.opacity=f;
  document.getElementById('n2').style.transform='translate('+(-25*t)+'px,'+(-10*t)+'px)'; document.getElementById('n2').style.opacity=f;
  document.getElementById('n3').style.opacity=f*(.7+.3*Math.sin(t*1.7));
  document.getElementById('orbs').style.opacity=Math.min(1,Math.max(0,(t-1.6)/1.0));
  const a=t*0.35; document.getElementById('sat').setAttribute('cx',(1150*Math.cos(a)).toFixed(1)); document.getElementById('sat').setAttribute('cy',(250*Math.sin(a)).toFixed(1));
  document.getElementById('o2').setAttribute('stroke-dashoffset',(-t*40).toFixed(1));
}}
window.thetaAt=theta; window.posAt=pos;
function render(t){{
  drawBg(t);
  const fl=seg(t,1.25,2.1); const fe=fl<.18?fl/.18:Math.pow(1-(fl-.18)/.82,2); document.getElementById('flash').style.opacity=(fl>0&&fl<1?fe:0).toFixed(3); document.getElementById('flash').style.transform='scale('+(.15+1.0*eo(fl))+')';
  $('bg').style.opacity=eo(seg(t,0,.5));
  const h=seg(t,1.3,2.2); $('halo').style.opacity=eo(h)*(.85+.15*Math.sin(t*2.4)); $('halo').style.transform='scale('+(.7+.3*eo(h))+')';
  const o=seg(t,1.4,1.85); $('Og').setAttribute('opacity',eo(o));
  $('Og').setAttribute('transform','translate(500 500) scale('+(.7+.3*back(o))+') translate(-500 -500)');
  const th=theta(t), trav=180-th;               // degrees travelled
  $('cutP').setAttribute('stroke-dashoffset',((G.ELL/2+5)*(1-cl(trav/180))).toFixed(1));
  $('revRp').setAttribute('stroke-dashoffset',(G.ELL*(1-cl(trav/360))).toFixed(1));
  const on=t>=T0-0.05;
  const p=pos(th); const settle=seg(t,T1-0.05,T1+0.35);
  $('chead').setAttribute('cx',p[0]);$('chead').setAttribute('cy',p[1]);
  $('chead').setAttribute('opacity',on?vis(p):0); $('chead').setAttribute('fill', settle>0? '#8B6CFF':'#fff');
  if(settle>0) $('chead').setAttribute('fill','rgb('+Math.round(255-(255-139)*settle)+','+Math.round(255-(255-108)*settle)+',255)');
  $('cglow').setAttribute('cx',p[0]);$('cglow').setAttribute('cy',p[1]);
  const flash=Math.sin(Math.PI*seg(t,T1-0.05,T1+0.6));
  $('cglow').setAttribute('opacity',on?vis(p)*(t<T1?0.85:0.85*(1-settle)+flash*0.9):0);
  $('cglow').setAttribute('r',G.Ro*(0.42+0.5*flash));
  const speed=Math.abs(theta(t+0.02)-th);
  TL.forEach((c,k)=>{{const q=pos(th+(k+1)*Math.min(speed*0.28,3.2)); const f=1-k/TL.length;
     c.setAttribute('cx',q[0]);c.setAttribute('cy',q[1]);
     c.setAttribute('r',(G.Ro*0.09*f).toFixed(1)); c.setAttribute('opacity',(on&&t<T1+0.1?vis(q)*f*0.9*cl(speed/1.5):0).toFixed(2));}});
  const rp=seg(t,T1,T1+0.7); $('ripple').setAttribute('cx',p[0]);$('ripple').setAttribute('cy',p[1]);
  $('ripple').setAttribute('r',G.Ro*(0.12+0.9*eo(rp))); $('ripple').setAttribute('opacity',rp>0&&rp<1?(1-rp)*0.9:0);
  // O centred -> 'orbita' centred -> full logo centred (synced with 'play')
  const s1=eio(seg(t,2.45,2.95)), s2=eio(seg(t,3.1,3.75));
  const tx=G.DX+(G.DA-G.DX)*s1+(0-G.DA)*s2;
  const zEnd=eio(seg(t,4.75,5.5)); const zs=1+0.05*zEnd;
  $('all').setAttribute('transform','translate(500 500) scale('+zs.toFixed(4)+') translate(-500 -500) translate('+tx.toFixed(1)+' 0)');
  document.getElementById('fadeout').style.opacity=zEnd.toFixed(3);
  $('wipeR').setAttribute('width',((G.XP-G.H0)*1.1).toFixed(0));
  [...$('letters').children].forEach((g,i)=>{{const k=eo(seg(t,2.45+i*.08,2.9+i*.08)); g.setAttribute('opacity',k.toFixed(3)); g.setAttribute('transform','translate(0 '+(120*(1-k)).toFixed(1)+')');}});
  const pp=eo(seg(t,3.1,3.7)), sc=0.94+0.06*pp;
  $('playG').setAttribute('opacity',eo(seg(t,3.1,3.45)));
  $('playG').setAttribute('transform','translate('+(140*(1-pp)).toFixed(1)+' 0) translate('+G.XP+' '+G.YP+') scale('+sc.toFixed(4)+') translate('+(-G.XP)+' '+(-G.YP)+')');
  $('playGlow').setAttribute('opacity',(Math.sin(Math.PI*seg(t,3.12,4.2))*0.9).toFixed(3));
  
  const s=seg(t,3.7,4.4); $('shineR').setAttribute('x',(-900+(G.W+1600)*eio(s)).toFixed(1)); $('shineR').setAttribute('opacity',s>0&&s<1?1:0);
}}
window.render=render; render(0);
</script></body></html>'''
open(sys.argv[1],'w').write(html)
