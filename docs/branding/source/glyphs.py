# Geometric monoline glyphs: perfectly round bowls matching the O icon.
def word(H=1000.0):
    s=H*0.1675            # stroke = O stroke
    xh=H*0.35             # x-height top (y-down, baseline at H)
    Rb=(H-xh)/2           # bowl outer radius
    rm=Rb-s/2             # bowl centerline radius
    cy=H-Rb               # bowl center y
    desc=H*1.25
    G=H*0.115             # gap between letters
    els=[]; x=H+H*0.15    # start after the O icon
    def rect(x0,y0,x1,y1): els.append(f'<rect x="{x0:.1f}" y="{y0:.1f}" width="{x1-x0:.1f}" height="{y1-y0:.1f}"/>')
    def ring(cx,cy_,r): els.append(f'<circle cx="{cx:.1f}" cy="{cy_:.1f}" r="{r:.1f}" fill="none" stroke="currentColor" stroke-width="{s:.1f}"/>')
    def arc(d): els.append(f'<path d="{d}" fill="none" stroke="currentColor" stroke-width="{s:.1f}"/>')
    def dot(cx,cy_,r): els.append(f'<circle cx="{cx:.1f}" cy="{cy_:.1f}" r="{r:.1f}"/>')
    def r_(x):
        rect(x,xh,x+s,H)
        arc(f'M{x+s/2:.1f} {cy:.1f} A{rm:.1f} {rm:.1f} 0 0 1 {x+Rb:.1f} {xh+s/2:.1f}')
        rect(x+Rb-3,xh,x+Rb+s*0.12,xh+s)
        return Rb+s*0.12
    def b_(x):
        rect(x,0,x+s,H); ring(x+Rb,cy,rm); return 2*Rb
    def i_(x):
        rect(x,xh,x+s,H); dot(x+s/2,s*0.62,s*0.62); return s
    def t_(x):
        Rt=H*0.27; rt=Rt-s/2
        rect(x,H*0.13,x+s,H-Rt+3)
        arc(f'M{x+s/2:.1f} {H-Rt:.1f} A{rt:.1f} {rt:.1f} 0 0 0 {x+Rt:.1f} {H-s/2:.1f}')
        rect(x+Rt-3,H-s,x+Rt+H*0.04,H)
        rect(x-H*0.07,xh,x+s+H*0.17,xh+s)
        return s+H*0.17
    def a_(x):
        ring(x+Rb,cy,rm); rect(x+2*Rb-s,xh,x+2*Rb,H); return 2*Rb
    def p_(x):
        rect(x,xh,x+s,desc); ring(x+Rb,cy,rm); return 2*Rb
    def l_(x):
        rect(x,0,x+s,H); return s
    def y_(x):
        rect(x,xh,x+s,cy+3)
        arc(f'M{x+s/2:.1f} {cy:.1f} A{rm:.1f} {rm:.1f} 0 0 0 {x+2*Rb-s/2:.1f} {cy:.1f}')
        yc=desc-Rb; xr=x+2*Rb-s/2
        rect(x+2*Rb-s,xh,x+2*Rb,yc+3)
        arc(f'M{xr:.1f} {yc:.1f} A{rm:.1f} {rm:.1f} 0 0 1 {x+Rb:.1f} {desc-s/2:.1f}')
        rect(x+s*0.25,desc-s,x+Rb+3,desc)
        return 2*Rb
    letters=[]
    lead={'t':H*0.07}  # left overhang of t crossbar
    for ch,fn in [('r',r_),('b',b_),('i',i_),('t',t_),('a',a_),('p',p_),('l',l_),('a',a_),('y',y_)]:
        x+=lead.get(ch,0)
        n0=len(els); w=fn(x); x+=w+G
        letters.append(els[n0:])
    return letters, x-G
