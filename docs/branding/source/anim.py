import sys, os, math
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import brand as B
H=B.H; u='a'
g=B.GRADS['o'].replace('{u}',u)+B.GRADS['ring'].replace('{u}',u)
C=2*math.pi*B.r
# ellipse path (full) in ring transform, for draw-on mask
ell=f'M{-B.rx:.1f} 0 A{B.rx:.1f} {B.ry:.1f} 0 1 0 {B.rx:.1f} 0 A{B.rx:.1f} {B.ry:.1f} 0 1 0 {-B.rx:.1f} 0'
h_=B.ry; a_=B.rx; ELL=math.pi*(3*(a_+h_)-math.sqrt((3*a_+h_)*(a_+3*h_)))
BIG=B.BIG
mark=B.mark(u,f'url(#go{u})',f'url(#gr{u})')
# split mark: O circle (first element) vs ring parts (rest); planet separate
i=mark.index('\n'); Opart=mark[:i]; ring=mark[i+1:]
ring=ring.replace(f'<circle cx="{B.bx:.1f}" cy="{B.by:.1f}" r="{B.Ro*0.105:.1f}" fill="url(#gr{u})"/>','')
planet=f'<g transform="{B.RT}"><circle id="planet" cx="{B.bx:.1f}" cy="{B.by:.1f}" r="{B.Ro*0.105:.1f}" fill="#5CE1E6"/></g>'
letters=''
for n,els in enumerate(B.LETTERS):
    c='#FFFFFF' if n<5 else '#A08BFF'
    letters+=f'<g class="L" fill="{c}" color="{c}"><g>{"".join(els)}</g></g>'
lettersMask=''.join(''.join(els) for els in B.LETTERS)
l,tp,rr,bt=B.ICON[0],B.ICON[1],B.TEXT_W,1.37*H
w=rr-l; hh=bt-tp
svg=f'''<svg id="logo" xmlns="http://www.w3.org/2000/svg" viewBox="{l:.1f} {tp:.1f} {w:.1f} {hh:.1f}" style="width:{1920*0.56:.0f}px;overflow:visible">
{B.defs(u,g)}
<defs>
 <mask id="revO" maskUnits="userSpaceOnUse" {BIG}><circle id="revOc" cx="{B.cx}" cy="{B.cy}" r="{B.r:.1f}" fill="none" stroke="#fff" stroke-width="{B.stemO*1.3:.1f}" stroke-dasharray="{C:.1f}" stroke-dashoffset="{C:.1f}" transform="rotate(-140 {B.cx} {B.cy})"/></mask>
 <mask id="revR" maskUnits="userSpaceOnUse" {BIG}><path id="revRp" transform="{B.RT}" d="{ell}" fill="none" stroke="#fff" stroke-width="{B.Ro*0.6:.1f}" stroke-dasharray="{ELL:.1f}" stroke-dashoffset="{ELL:.1f}"/></mask>
 <mask id="txt" maskUnits="userSpaceOnUse" {BIG}><g fill="#fff" color="#fff">{lettersMask}</g></mask>
 <linearGradient id="shine" x1="0" x2="1" y1="0" y2="0"><stop offset="0" stop-color="#fff" stop-opacity="0"/><stop offset=".5" stop-color="#fff" stop-opacity=".85"/><stop offset="1" stop-color="#fff" stop-opacity="0"/></linearGradient>
 <radialGradient id="pglow"><stop offset="0" stop-color="#5CE1E6" stop-opacity=".9"/><stop offset="1" stop-color="#5CE1E6" stop-opacity="0"/></radialGradient>
</defs>
<g id="all"><g id="mark">
 <g id="Og" mask="url(#revO)">{Opart}</g>
 <g id="Rg" mask="url(#revR)">{ring}</g>
 <g transform="{B.RT}"><circle id="pglowc" cx="{B.bx:.1f}" cy="{B.by:.1f}" r="{B.Ro*0.35:.1f}" fill="url(#pglow)" opacity="0"/></g>
 {planet}
</g>
<g id="letters">{letters}</g>
<g mask="url(#txt)"><rect id="shineR" x="0" y="{tp:.1f}" width="{H*0.9:.1f}" height="{hh:.1f}" fill="url(#shine)" transform="skewX(-20)" opacity="0"/></g></g>
</svg>'''
html=f'''<!doctype html><html><head><meta charset="utf-8"><style>
html,body{{margin:0;width:1920px;height:1080px;overflow:hidden;background:#07050F}}
#bg{{position:absolute;inset:0;background:radial-gradient(70% 90% at 50% 45%,#2A1766 0%,#140B36 45%,#07050F 100%)}}
#grid{{position:absolute;inset:0;background-image:linear-gradient(#ffffff0a 1px,transparent 1px),linear-gradient(90deg,#ffffff0a 1px,transparent 1px);background-size:48px 48px;-webkit-mask-image:radial-gradient(60% 70% at 50% 50%,#000,transparent)}}
#halo{{position:absolute;left:50%;top:50%;width:900px;height:900px;margin:-450px 0 0 -450px;border-radius:50%;background:radial-gradient(circle,#7C5CFF55,#7C5CFF00 65%)}}
#wrap{{position:absolute;left:0;right:0;top:0;bottom:0;display:flex;flex-direction:column;align-items:center;justify-content:center}}
#tag{{margin-top:56px;font:500 26px Inter,sans-serif;letter-spacing:10px;color:#B8B3CC}}
</style></head><body><div id="bg"></div><div id="grid"></div><div id="halo"></div>
<div id="wrap">{svg}<div id="tag">CANAIS · FILMES · SÉRIES · FUTEBOL</div></div>
<script>
const C={C:.1f}, E={ELL:.1f}, DUR=4.0, DX={(l+rr)/2-500:.1f};
const cl=(x)=>Math.max(0,Math.min(1,x));
const seg=(t,a,b)=>cl((t-a)/(b-a));
const eo=(x)=>1-Math.pow(1-x,3);              // ease-out cubic
const eio=(x)=>x<.5?4*x*x*x:1-Math.pow(-2*x+2,3)/2;
const back=(x)=>{{const c1=1.70158,c3=c1+1;return 1+c3*Math.pow(x-1,3)+c1*Math.pow(x-1,2);}};
const $=(id)=>document.getElementById(id);
const L=[...document.querySelectorAll('.L > g')];
function render(t){{
  $('bg').style.opacity=eo(seg(t,0,.6));
  $('grid').style.opacity=.9*eo(seg(t,.2,1.2));
  const hp=seg(t,.3,1.6); $('halo').style.opacity=eo(hp)*(.8+.2*Math.sin(t*2.2));
  $('halo').style.transform='scale('+(.6+.4*eo(hp))+')';
  // O draws in
  const o=eio(seg(t,.25,1.05)); $('revOc').setAttribute('stroke-dashoffset',C*(1-o));
  // mark scale pop
  const m=seg(t,.25,1.3); $('mark').setAttribute('transform','translate(500 500) scale('+(.82+.18*back(m))+') translate(-500 -500)');
  const sl=eio(seg(t,1.15,1.95)); $('all').setAttribute('transform','translate('+(DX*(1-sl)).toFixed(1)+' 0)');
  // orbit sweeps
  const r=eio(seg(t,.85,1.75)); $('revRp').setAttribute('stroke-dashoffset',E*(1-r));
  // planet pop + glow
  const p=seg(t,1.6,2.0); $('planet').setAttribute('r',({B.Ro*0.105:.1f})*back(p)); 
  $('pglowc').setAttribute('opacity',Math.sin(Math.PI*seg(t,1.6,2.6))*.9);
  // letters stagger
  L.forEach((g,i)=>{{const k=eo(seg(t,1.25+i*.07,1.85+i*.07));
    g.setAttribute('transform','translate('+(-90*(1-k))+' 0)'); g.parentNode.setAttribute('opacity',k);}});
  // shine sweep
  const s=seg(t,2.45,3.25); const W={rr:.1f};
  $('shineR').setAttribute('x',(-900+(W+1600)*eio(s)).toFixed(1)); $('shineR').setAttribute('opacity',s>0&&s<1?1:0);
  $('tag').style.opacity=eo(seg(t,2.6,3.3)); $('tag').style.transform='translateY('+(16*(1-eo(seg(t,2.6,3.3))))+'px)';
}}
window.render=render; render(0);
</script></body></html>'''
open(sys.argv[1],'w').write(html)
