# Original "orbit" logo sting: comet whoosh that follows the orbit (stereo), bright arrival chime, warm major bloom.
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, sosfilt_zi, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.0; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(11)
def lp(x,f,o=4): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def hp(x,f,o=2): return sosfilt(butter(o,f,'high',fs=SR,output='sos'),x)
def tv_bp(x,fc,q=1.2,blk=128):
    y=np.zeros_like(x); zi=None
    for i in range(0,len(x),blk):
        f=float(np.clip(fc[i],60,15000)); lo=f/ (1+0.5/q); hi=min(f*(1+0.5/q),SR/2-100)
        sos=butter(2,[lo,hi],'band',fs=SR,output='sos')
        if zi is None or zi.shape!=sosfilt_zi(sos).shape: zi=sosfilt_zi(sos)*0
        y[i:i+blk],zi=sosfilt(sos,x[i:i+blk],zi=zi)
    return y
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
# comet path (same as animation)
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
th=np.radians(180-TOTAL*eio((t-T0)/(T1-T0)))
lx=np.cos(th); ly=0.18*np.sin(th)
sx=lx*math.cos(ang)-ly*math.sin(ang)                  # -1..1 screen x
front=np.sin(th)
speed=np.abs(np.gradient(th))*SR; speed/=speed.max()+1e-9
L=np.zeros(N); R=np.zeros(N)
def add(x,pan=0.0):
    global L,R
    if np.isscalar(pan): pan=np.full(N,pan)
    L+=x*np.sqrt((1-pan)/2); R+=x*np.sqrt((1+pan)/2)
# 1) intro: airy swell + low hum
air=lp(hp(rng.standard_normal(N),2500),9000)*np.interp(t,[0,.35,.6,1.0],[0,.05,.03,0])
add(air)
hum=np.sin(2*np.pi*65.41*t)*np.interp(t,[0,.4,.8,2.3,2.6],[0,.12,.1,.12,0]); add(hum)
# 2) comet whoosh following the orbit
active=np.clip((t-T0+0.08)/0.1,0,1)*np.clip((T1+0.05-t)/0.1,0,1)
fc=500+5500*speed*(0.65+0.35*(0.5+0.5*front))
wh=tv_bp(rng.standard_normal(N),fc,q=1.4)
wamp=active*(0.15+0.85*speed)*(0.55+0.45*(0.5+0.5*front))
add(wh*wamp*0.55, np.clip(sx*0.9,-1,1))
# tonal shimmer rising with progress
prog=np.clip((t-T0)/(T1-T0),0,1)
f=np.interp(eio(prog),[0,1],[392,1568])*(1+0.006*np.sin(2*np.pi*6*t))
ph=2*np.pi*np.cumsum(f)/SR
ton=(np.sin(ph)+0.3*np.sin(2*ph))*active*0.06*(0.4+0.6*speed)
add(ton, np.clip(sx*0.6,-1,1))
# 3) arrival at T1: sub boom + bright bell chord
tt=np.clip(t-T1,0,None); g=(t>=T1)
fb=40+70*np.exp(-tt/0.08); boom=np.sin(2*np.pi*np.cumsum(fb)/SR)*np.exp(-tt/0.9)*g
add(boom*0.85)
add(lp(rng.standard_normal(N),300)*np.exp(-tt/0.1)*g*0.5)
bell=np.zeros(N)
for fr,a in ((523.25,1),(659.25,.8),(783.99,.7),(1046.5,.55),(1567.98,.3)):
    for m,ma in ((1,1),(2.76,.35),(5.4,.12)):
        bell+=a*ma*np.sin(2*np.pi*fr*m*tt)*np.exp(-tt/(1.4/m))
add(bell*g*0.07, 0.25)
# 4) warm major bloom (Cmaj9)
pad=np.zeros(N)
for fr in (65.41,130.81,196.0,246.94,329.63,392.0,587.33):
    for det in (-.1,0,.1):
        ff=fr*2**(det/12); p=2*np.pi*ff*t+rng.uniform(0,6.28)
        pad+=sum(np.sin(h*p)/h for h in range(1,9) if h*ff<6000)/3
pad=lp(pad,2200)*np.interp(t,[0,2.2,2.9,4.0,5.0],[0,0,.75,.55,0])
add(pad*0.05,-0.15); add(lp(pad,900)*0.04,0.15)
# 5) text wipe arpeggio (2.35-3.2) and shine shimmer (3.3-4.0)
for k,(st,fr) in enumerate(zip(np.linspace(2.38,3.1,6),(523.25,659.25,783.99,987.77,1174.66,1567.98))):
    tt=np.clip(t-st,0,None); pl=np.sin(2*np.pi*fr*tt)*np.exp(-tt/0.35)*(t>=st)
    add(pl*0.05, -0.6+0.24*k)
for k,st in enumerate(np.linspace(3.3,3.95,9)):
    tt=np.clip(t-st,0,None); fr=2093*2**(k/12*2)
    add(np.sin(2*np.pi*fr*tt)*np.exp(-tt/0.25)*(t>=st)*0.025, -0.8+0.2*k)
# reverb + master
ir_n=int(2.6*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.7); ir=lp(ir,6000); ir/=np.sqrt((ir**2).sum())*6
Lw=L+fftconvolve(L,ir)[:N]*0.8; Rw=R+fftconvolve(R,np.roll(ir,523))[:N]*0.8
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.3,5.0],[1,1,0])[:,None]
st=np.tanh(st/np.abs(st).max()*1.5)/np.tanh(1.5)*0.89
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
