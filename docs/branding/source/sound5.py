# Simple, low "orbit" sound: one continuous soft tone that travels with the comet around the O
# (stereo pan follows it, brighter/louder in front, muffled behind), then one soft note on arrival.
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, sosfilt_zi, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.0; N=int(SR*DUR); t=np.arange(N)/SR
def lp(x,f,o=2): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
th=np.radians(180-TOTAL*eio((t-T0)/(T1-T0)))
sx=np.cos(th)*math.cos(ang)-0.18*np.sin(th)*math.sin(ang)       # screen x (-1..1)
front=np.sin(th)                                                # +1 front of the O, -1 behind
speed=np.abs(np.gradient(th)); speed/=speed.max()+1e-12
act=np.clip((t-T0+0.15)/0.3,0,1)*np.clip((T1+0.25-t)/0.35,0,1)
# continuous tone: pitch glides up gently with the orbit, small doppler with speed
f=np.interp(eio((t-T0)/(T1-T0)),[0,1],[220.0,330.0])*(1+0.03*speed*np.sign(np.gradient(sx)))
ph=2*np.pi*np.cumsum(f)/SR
tone=np.sin(ph)+0.25*np.sin(2*ph)+0.08*np.sin(3*ph)
amp=act*(0.35+0.65*speed)*(0.55+0.45*(0.5+0.5*front))
# muffle when behind the O: time-varying low-pass
cut=600+2400*(0.5+0.5*front)
y=np.zeros(N); zi=None
for i in range(0,N,256):
    sos=butter(2,float(cut[i]),'low',fs=SR,output='sos')
    if zi is None: zi=sosfilt_zi(sos)*0
    y[i:i+256],zi=sosfilt(sos,(tone*amp)[i:i+256],zi=zi)
pan=np.clip(sx*0.85,-1,1)
L=y*np.sqrt((1-pan)/2); R=y*np.sqrt((1+pan)/2)
# one soft note on arrival (fifth above), centred
tt=np.clip(t-T1,0,None); g=(t>=T1)
n=(np.sin(2*np.pi*330*tt)+0.2*np.sin(2*np.pi*660*tt)+0.5*np.sin(2*np.pi*165*tt))*np.minimum(tt/0.08,1)*np.exp(-tt/1.1)*g
L+=lp(n,2000)*0.35; R+=lp(n,2000)*0.35
rng=np.random.default_rng(3); ir_n=int(1.6*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.4); ir=lp(ir,3500,4); ir/=np.sqrt((ir**2).sum())*9
Lw=L+fftconvolve(L,ir)[:N]*0.5; Rw=R+fftconvolve(R,np.roll(ir,389))[:N]*0.5
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.0,5.0],[1,1,0])[:,None]
st=st/np.abs(st).max()*0.6
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
