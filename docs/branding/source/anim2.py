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
letters=''.join(f'<g fill="{"#FFFFFF" if n<5 else VIO}" color="{"#FFFFFF" if n<5 else VIO}">{"".join(e)}</g>' for n,e in enumerate(B.LETTERS))
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
 <radialGradient id="cg"><stop offset="0" stop-color="#fff" stop-opacity="1"/><stop offset=".25" stop-color="#C9B8FF" stop-opacity=".8"/><stop offset="1" stop-color="#8B6CFF" stop-opacity="0"/></radialGradient>
</defs>
<g id="all">
 <g id="Og">{Opart}</g>
 <g id="Rg" mask="url(#revR)">{ring}</g>
 <g id="comet">{tail}<circle id="cglow" r="{B.Ro*0.42:.0f}" fill="url(#cg)"/><circle id="chead" r="{B.Ro*0.105:.1f}" fill="#fff"/></g>
 <circle id="ripple" r="0" fill="none" stroke="#B49CFF" stroke-width="10" opacity="0"/>
 <g mask="url(#wipe)"><g id="letters">{letters}</g>
 <g mask="url(#txt)"><rect id="shineR" x="0" y="{tp:.1f}" width="{H*0.9:.1f}" height="{hh:.1f}" fill="url(#shine)" transform="skewX(-20)" opacity="0"/></g></g>
</g></svg>'''
G=dict(cx=B.cx,cy=B.cy,ang=B.ang,off=B.off,rx=B.rx,ry=B.ry,Ro=B.Ro,ri=B.ri,gapB=B.gapB,ELL=ELL,DX=(l+rr)/2-500,W=rr,
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
<div id="vig"></div><div id="halo"></div><div id="wrap">{svg}</div>
<script>
const G={json.dumps(G)};
const cl=x=>Math.max(0,Math.min(1,x)), seg=(t,a,b)=>cl((t-a)/(b-a));
const eo=x=>1-Math.pow(1-x,3), eio=x=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const back=x=>{{const c1=1.70158,c3=c1+1;return 1+c3*Math.pow(x-1,3)+c1*Math.pow(x-1,2);}};
const $=id=>document.getElementById(id); const TL=[...document.querySelectorAll('.tl')];
const A=G.ang*Math.PI/180, T0=0.45, T1=2.25, TOTAL=360+180-G.endTh;
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
function drawBg(t){{
  cx2.clearRect(0,0,1920,1080);
  const zoom=1+0.035*t;                       // slow push-in
  for(const s of STARS){{
    const x=960+(s.x-960)*(1+(zoom-1)*s.z*2), y=540+(s.y-540)*(1+(zoom-1)*s.z*2);
    const tw=.55+.45*Math.sin(t*s.sp+s.ph); const a=Math.min(1,t/0.8)*s.z*tw*.9;
    const r=.5+1.3*s.z*s.z;
    cx2.fillStyle='rgba('+s.c+','+a.toFixed(3)+')'; cx2.beginPath(); cx2.arc(x,y,r,0,6.283); cx2.fill();
    if(s.z>.93){{ cx2.fillStyle='rgba('+s.c+','+(a*.15).toFixed(3)+')'; cx2.beginPath(); cx2.arc(x,y,r*4,0,6.283); cx2.fill(); }}
  }}
  const f=Math.min(1,t/1.2);
  document.getElementById('n1').style.transform='translate('+(30*t)+'px,'+(12*t)+'px)'; document.getElementById('n1').style.opacity=f;
  document.getElementById('n2').style.transform='translate('+(-25*t)+'px,'+(-10*t)+'px)'; document.getElementById('n2').style.opacity=f;
  document.getElementById('n3').style.opacity=f*(.7+.3*Math.sin(t*1.7));
  document.getElementById('orbs').style.opacity=Math.min(1,Math.max(0,(t-.3)/1.2));
  const a=t*0.35; document.getElementById('sat').setAttribute('cx',(1150*Math.cos(a)).toFixed(1)); document.getElementById('sat').setAttribute('cy',(250*Math.sin(a)).toFixed(1));
  document.getElementById('o2').setAttribute('stroke-dashoffset',(-t*40).toFixed(1));
}}
window.thetaAt=theta; window.posAt=pos;
function render(t){{
  drawBg(t);
  $('bg').style.opacity=eo(seg(t,0,.5));
  const h=seg(t,.2,1.4); $('halo').style.opacity=eo(h)*(.85+.15*Math.sin(t*2.4)); $('halo').style.transform='scale('+(.7+.3*eo(h))+')';
  const o=seg(t,.1,.7); $('Og').setAttribute('opacity',eo(o));
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
  const sl=eio(seg(t,2.25,3.05)); $('all').setAttribute('transform','translate('+(G.DX*(1-sl)).toFixed(1)+' 0)');
  $('wipeR').setAttribute('width',(G.W*1.05*eo(seg(t,2.35,3.2))).toFixed(0));
  const L=$('letters'); L.setAttribute('transform','translate('+(-60*(1-eo(seg(t,2.35,3.2))))+' 0)');
  const s=seg(t,3.3,4.0); $('shineR').setAttribute('x',(-900+(G.W+1600)*eio(s)).toFixed(1)); $('shineR').setAttribute('opacity',s>0&&s<1?1:0);
}}
window.render=render; render(0);
</script></body></html>'''
open(sys.argv[1],'w').write(html)
