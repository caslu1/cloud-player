# Minimal, soft orbit sting: a few gentle notes that follow the comet (stereo) and one warm chord on arrival.
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.0; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(5)
def lp(x,f,o=2): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
L=np.zeros(N); R=np.zeros(N)
def place(sig,start,pan,gain):
    i=int(start*SR); n=min(len(sig),N-i)
    L[i:i+n]+=sig[:n]*gain*math.sqrt((1-pan)/2); R[i:i+n]+=sig[:n]*gain*math.sqrt((1+pan)/2)
def note(f,dur,dec,att=0.03):
    n=int(dur*SR); tt=np.arange(n)/SR
    return (np.sin(2*np.pi*f*tt)+0.1*np.sin(4*np.pi*f*tt))*np.minimum(tt/att,1)*np.exp(-tt/dec)
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
def sx(tt):
    th=math.radians(180-TOTAL*float(eio((tt-T0)/(T1-T0))))
    return math.cos(th)*math.cos(ang)-0.18*math.sin(th)*math.sin(ang)
# 6 soft notes along the orbit (rising), panned with the comet
for k,(tt,f) in enumerate(zip(np.linspace(0.6,2.05,6),(392.0,440.0,523.25,587.33,659.25,783.99))):
    place(lp(note(f,1.2,0.45),2500),tt,max(-.8,min(.8,sx(tt)*.8)),0.08)
# one warm chord on arrival (C major add9), slow attack, long fade
for f,pn in ((130.81,0),(196.0,-.25),(261.63,.25),(329.63,-.1),(392.0,.15),(587.33,0)):
    place(lp(note(f,2.75,1.3,att=0.12),1800),T1,pn,0.06)
ir_n=int(2.0*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.5); ir=lp(ir,4000,4); ir/=np.sqrt((ir**2).sum())*8
Lw=L+fftconvolve(L,ir)[:N]*0.6; Rw=R+fftconvolve(R,np.roll(ir,401))[:N]*0.6
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.2,5.0],[1,1,0])[:,None]
st=st/np.abs(st).max()*0.6
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
