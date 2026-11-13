import sys, math, os
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from glyphs import word

H=1000.0
Ro=H/2; stemO=Ro*0.335; r=Ro-stemO/2; ri=Ro-stemO; cx=Ro; cy=H/2
ang=-47; rx=Ro*1.54; ry=Ro*0.28
ringW=Ro*0.085; slashW=Ro*0.10; gapF=Ro*0.07; gapB=Ro*0.10; off=Ro*0.04
RT=f'translate({cx:.1f} {cy:.1f}) rotate({ang}) translate({off:.1f} 0)'
front=f'M{-rx:.1f} 0 A{rx:.1f} {ry:.1f} 0 0 0 {rx:.1f} 0'
back=f'M{-rx:.1f} 0 A{rx:.1f} {ry:.1f} 0 0 1 {rx:.1f} 0'
t=math.radians(33); bx,by=rx*math.cos(t),ry*math.sin(t)
neck=f'M{bx:.1f} {by:.1f} A{rx:.1f} {ry:.1f} 0 0 0 {rx:.1f} 0'
BIG='x="-3000" y="-3000" width="12000" height="7000"'
from fontTools.ttLib import TTFont
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.pens.boundsPen import BoundsPen
FD=os.path.join(os.path.dirname(os.path.abspath(__file__)),'fonts')
FONT1=os.environ.get('FONT1','Poppins-SemiBold.ttf'); FONT2=os.environ.get('FONT2','Poppins-Light.ttf')
def font_word(H, parts=(('rbita',FONT1),('play',FONT2)), track=0.015, start=1.19):
    x=H*start; letters=[]
    for pi,(txt,fn) in enumerate(parts):
        if pi: x+=H*0.03
        f=TTFont(os.path.join(FD,fn)); gs=f.getGlyphSet(); cm=f.getBestCmap(); upm=f['head'].unitsPerEm
        bp=BoundsPen(gs); gs[cm[ord('x')]].draw(bp); k=H*0.65/bp.bounds[3]
        for ch in txt:
            g=cm[ord(ch)]; sp=SVGPathPen(gs); gs[g].draw(TransformPen(sp,(k,0,0,-k,x,H)))
            letters.append([f'<path d="{sp.getCommands()}"/>']); x+=gs[g].width*k+upm*k*track
    return letters, x
LETTERS,TEXT_W=font_word(H) if os.environ.get('CUSTOM')!='1' else word(H)
# icon extents (approx, measured): left/top/right/bottom of the orbit mark
ICON=(-0.22*H, -0.13*H, 1.17*H, 1.12*H)

def defs(uid, grads):
    return f'''<defs>{grads}
  <mask id="cf{uid}" maskUnits="userSpaceOnUse" {BIG}><rect {BIG} fill="#fff"/>
    <path transform="{RT}" d="{front}" fill="none" stroke="#000" stroke-width="{ringW+2*gapF:.1f}"/></mask>
  <mask id="out{uid}" maskUnits="userSpaceOnUse" {BIG}><rect {BIG} fill="#fff"/>
    <circle cx="{cx}" cy="{cy}" r="{Ro+gapB:.1f}" fill="#000"/></mask>
  <clipPath id="hole{uid}"><circle cx="{cx}" cy="{cy}" r="{ri-gapB*0.8:.1f}"/></clipPath>
</defs>'''

def mark(uid, oC, ringC):
    return f'''<circle cx="{cx}" cy="{cy}" r="{r:.1f}" fill="none" stroke="{oC}" stroke-width="{stemO:.1f}" mask="url(#cf{uid})"/>
<g mask="url(#out{uid})"><path transform="{RT}" d="{back}" fill="none" stroke="{ringC}" stroke-width="{ringW:.1f}" stroke-linecap="round"/></g>
<g clip-path="url(#hole{uid})"><path transform="{RT}" d="{back}" fill="none" stroke="{ringC}" stroke-width="{slashW:.1f}"/></g>
<g transform="{RT}"><path d="{front}" fill="none" stroke="{ringC}" stroke-width="{ringW:.1f}" stroke-linecap="round"/>
<path d="{neck}" fill="none" stroke="{ringC}" stroke-width="{ringW*1.55:.1f}" stroke-linecap="round"/>
<circle cx="{bx:.1f}" cy="{by:.1f}" r="{Ro*0.105:.1f}" fill="{ringC}"/></g>'''

def text(c1, c2):
    out=[]
    for i,els in enumerate(LETTERS):
        c=c1 if i<5 else c2
        out.append(f'<g fill="{c}" color="{c}">'+''.join(els)+'</g>')
    return ''.join(out)

GRADS={
 'o': '<linearGradient id="go{u}" gradientUnits="userSpaceOnUse" x1="0" y1="0" x2="1000" y2="1000"><stop offset="0" stop-color="#B9A6FF"/><stop offset="1" stop-color="#7C5CFF"/></linearGradient>',
 'ring': '<linearGradient id="gr{u}" gradientUnits="userSpaceOnUse" x1="-200" y1="1100" x2="1150" y2="-100"><stop offset="0" stop-color="#7C5CFF"/><stop offset=".5" stop-color="#3FA8F0"/><stop offset="1" stop-color="#00D4C8"/></linearGradient>',
}

def svg(kind, scheme, bg=None, pad=0.08, square=False, radius=0.22):
    u=kind[0]+scheme[:3]
    g=''; oC=ringC=None
    S={'white':('#FFFFFF','#FFFFFF','#FFFFFF','#FFFFFF'),
       'dark':('#0B0716','#0B0716','#0B0716','#0B0716'),
       'violet':('#7C5CFF','#7C5CFF','#7C5CFF','#7C5CFF'),
       'color':('url(#go{u})','url(#gr{u})','#FFFFFF','#A08BFF'),
       'white-violet':('#FFFFFF','#8B6CFF','#FFFFFF','#8B6CFF'),
       'color-white':('url(#go{u})','url(#gr{u})','#FFFFFF','#FFFFFF'),
       'color-light':('url(#go{u})','url(#gr{u})','#0B0716','#7C5CFF')}[scheme]
    oC,ringC,t1,t2=[c.replace('{u}',u) for c in S]
    if scheme.startswith('color'):
        g=GRADS['o'].replace('{u}',u)+GRADS['ring'].replace('{u}',u)
        if scheme=='color-light':
            g=g.replace('#B9A6FF','#9B82FF').replace('#7C5CFF"/></linearGradient>','#5B3BE0"/></linearGradient>',1)
    body=mark(u,oC,ringC)
    if kind=='logo':
        body+=text(t1,t2)
        l,tp,rr,b=ICON[0],ICON[1],TEXT_W,1.25*H
    else:
        l,tp,rr,b=ICON
    w,h=rr-l,b-tp
    if square:
        side=max(w,h)*(1+2*pad); X=l-(side-w)/2; Y=tp-(side-h)/2; w2=h2=side
    else:
        p=pad*h; X=l-p; Y=tp-p; w2=w+2*p; h2=h+2*p
    bgel=''
    if bg:
        if bg=='brand':
            g+=f'<linearGradient id="bg{u}" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#241356"/><stop offset=".55" stop-color="#140B36"/><stop offset="1" stop-color="#0B0716"/></linearGradient>'
            fillbg=f'url(#bg{u})'
        else: fillbg=bg
        rx_=radius*w2 if square else 0
        bgel=f'<rect x="{X:.1f}" y="{Y:.1f}" width="{w2:.1f}" height="{h2:.1f}" rx="{rx_:.1f}" fill="{fillbg}"/>'
    return f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{X:.1f} {Y:.1f} {w2:.1f} {h2:.1f}">{defs(u,g)}{bgel}{body}</svg>\n', w2/h2

if __name__=='__main__':
    out=sys.argv[1]; os.makedirs(out,exist_ok=True)
    jobs={}
    for sc in ['white','dark','violet','color','color-white','white-violet','color-light']:
        jobs[f'logo/orbitaplay-logo-{sc}']=svg('logo',sc)
        jobs[f'icon/orbitaplay-icon-{sc}']=svg('icon',sc,pad=0.04)
    jobs['icon/orbitaplay-appicon']=svg('icon','color',bg='brand',square=True,pad=0.16)
    jobs['icon/orbitaplay-appicon-white']=svg('icon','white',bg='brand',square=True,pad=0.16)
    jobs['icon/orbitaplay-appicon-violet']=svg('icon','white',bg='#7C5CFF',square=True,pad=0.16)
    jobs['logo/orbitaplay-logo-color-bg']=svg('logo','color',bg='brand',pad=0.35)
    import json
    meta={}
    for k,(s,ar) in jobs.items():
        os.makedirs(os.path.dirname(os.path.join(out,k)),exist_ok=True)
        open(os.path.join(out,k+'.svg'),'w').write(s); meta[k]=ar
    json.dump(meta,open(os.path.join(out,'meta.json'),'w'),indent=1)
