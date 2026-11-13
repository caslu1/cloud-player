# Original "orbit" sting v3: twinkling particle trail that follows the comet around the orbit (stereo),
# soft gliding synth, warm sub "whomp" + electric-piano chord on arrival, marimba run on text reveal, glass chime on shine.
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.0; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(21)
def lp(x,f,o=2): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
L=np.zeros(N); R=np.zeros(N)
def place(sig,start,pan,gain):
    i=int(start*SR); n=min(len(sig),N-i)
    if n<=0: return
    gl=math.sqrt((1-pan)/2); gr=math.sqrt((1+pan)/2)
    L[i:i+n]+=sig[:n]*gain*gl; R[i:i+n]+=sig[:n]*gain*gr
def tone(f,dur,dec,harm=((1,1),),att=0.004):
    n=int(dur*SR); tt=np.arange(n)/SR
    e=np.minimum(tt/att,1)*np.exp(-tt/dec)
    return sum(a*np.sin(2*np.pi*f*m*tt) for m,a in harm)*e
# comet path (same as animation)
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
def path(tt):
    th=math.radians(180-TOTAL*float(eio((tt-T0)/(T1-T0))))
    lx=math.cos(th); ly=0.18*math.sin(th)
    return lx*math.cos(ang)-ly*math.sin(ang), math.sin(th)
# 1) intro: soft rising pad tone (no noise)
pad_in=sum(np.sin(2*np.pi*f*t)*a for f,a in ((174.61,1),(261.63,.6),(349.23,.4)))
place(lp(pad_in,1200)*np.interp(t,[0,.5,1.2,2.3,2.6],[0,.25,.18,.2,0]),0,0,0.10)
# 2) particle trail following the comet: plucks on a rising pentatonic scale
scale=[349.23,392.0,440.0,523.25,587.33,698.46,783.99,880.0,1046.5,1174.66,1396.91,1567.98,1760.0]
tt=T0; k=0
while tt<T1:
    prog=(tt-T0)/(T1-T0); sx,front=path(tt)
    idx=min(int(prog*len(scale)+rng.integers(0,2)),len(scale)-1); f=scale[idx]
    s=lp(tone(f,0.7,0.22+0.1*rng.random(),((1,1),(2,.12)),att=0.02),3500)
    g=(0.5+0.5*(0.5+0.5*front))*(0.6+0.4*math.sin(math.pi*prog))
    place(s,tt,max(-1,min(1,sx*0.8)),0.07*g)
    # density follows speed: faster in the middle of the orbit
    sp=math.sin(math.pi*prog); tt+=0.11-0.07*sp; k+=1
# soft gliding synth following the orbit (pitch rises, pans with comet)
prog=np.clip((t-T0)/(T1-T0),0,1); act=np.clip((t-T0+.1)/.2,0,1)*np.clip((T1+.1-t)/.15,0,1)
fg=np.interp(eio(prog),[0,1],[349.23,1046.5]); ph=2*np.pi*np.cumsum(fg)/SR
glide=(np.sin(ph)+0.15*np.sin(3*ph))*act*(0.35+0.65*np.sin(np.pi*prog))
sxv=np.array([path(x)[0] for x in t[::480]]); sxv=np.interp(np.arange(N),np.arange(0,N,480),sxv)
gl=np.sqrt((1-np.clip(sxv*.7,-1,1))/2); gr=np.sqrt((1+np.clip(sxv*.7,-1,1))/2)
L+=glide*0.025*gl; R+=glide*0.025*gr
# 3) arrival: warm sub "whomp" (no noise) + electric piano Fmaj9
ta=np.clip(t-T1,0,None); g=(t>=T1)
fw=43.65+30*np.exp(-ta/0.12); whomp=np.sin(2*np.pi*np.cumsum(fw)/SR)*np.minimum(ta/0.02,1)*np.exp(-ta/0.8)*g
L+=whomp*0.3; R+=whomp*0.3
for f,pn in ((174.61,-.3),(220.0,.2),(261.63,-.1),(329.63,.3),(392.0,-.2),(523.25,.1)):
    ep=tone(f,2.8,1.1,((1,1),(2,.15)),att=0.04)*(1+0.15*np.sin(2*np.pi*4.5*np.arange(int(2.8*SR))/SR))
    place(ep,T1,pn,0.055)
# 4) marimba run on text reveal
for i,(st,f) in enumerate(zip(np.linspace(2.38,3.1,7),(698.46,880.0,1046.5,1174.66,1396.91,1760.0,2093.0))):
    place(lp(tone(f*0.5,0.6,0.15,((1,1),(3.9,.1)),att=0.01),3000),st,-0.6+0.2*i,0.04)
# 5) glass chime on the shine
for i,st in enumerate((3.35,3.5,3.65)):
    place(tone((2637.0,3135.96,3951.07)[i],1.5,0.6,((1,1),),att=0.03),st,0.4-0.3*i,0.012)
# warm tail pad
tail=sum(np.sin(2*np.pi*f*t+rng.uniform(0,6))*a for f,a in ((87.31,.8),(174.61,1),(261.63,.6),(329.63,.4),(392.0,.3)))
tail=lp(tail,1500)*np.interp(t,[0,2.3,3.0,4.2,5.0],[0,0,.3,.22,0]); L+=tail*0.08; R+=tail*0.08
# gentle reverb + master
ir_n=int(2.4*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.6); ir=lp(ir,5000,4); ir/=np.sqrt((ir**2).sum())*7
Lw=L+fftconvolve(L,ir)[:N]*0.7; Rw=R+fftconvolve(R,np.roll(ir,457))[:N]*0.7
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.3,5.0],[1,1,0])[:,None]
st=np.stack([lp(st[:,0],6000),lp(st[:,1],6000)],1); st=st/np.abs(st).max()*0.7
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
