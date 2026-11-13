# Three simple, low orbit-following sound options. Usage: sound6.py A|B|C out.wav
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, sosfilt_zi, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.0; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(9)
def lp(x,f,o=2): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def hp(x,f,o=2): return sosfilt(butter(o,f,'high',fs=SR,output='sos'),x)
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
def tvlp(x,cut,blk=256):
    y=np.zeros_like(x); zi=None
    for i in range(0,len(x),blk):
        sos=butter(2,float(np.clip(cut[i],80,16000)),'low',fs=SR,output='sos')
        if zi is None: zi=sosfilt_zi(sos)*0
        y[i:i+blk],zi=sosfilt(sos,x[i:i+blk],zi=zi)
    return y
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
th=np.radians(180-TOTAL*eio((t-T0)/(T1-T0)))
sx=np.cos(th)*math.cos(ang)-0.18*np.sin(th)*math.sin(ang); front=np.sin(th)
speed=np.abs(np.gradient(th)); speed/=speed.max()+1e-12
act=np.clip((t-T0+0.1)/0.25,0,1)*np.clip((T1+0.2-t)/0.3,0,1)
pan=np.clip(sx*0.85,-1,1); fr=0.5+0.5*front
ta=np.clip(t-T1,0,None); g=(t>=T1)
opt=sys.argv[1]
if opt=='A':   # soft airy swoosh
    n=lp(rng.standard_normal(N),5000,4)
    m=tvlp(n,500+3500*speed*(0.5+0.5*fr))*act*(0.15+0.85*speed**1.5)*(0.5+0.5*fr)
    y=m
    end=lp(np.sin(2*np.pi*110*ta)+0.4*np.sin(2*np.pi*165*ta),900)*np.minimum(ta/0.15,1)*np.exp(-ta/1.2)*g*0.25
elif opt=='B': # shimmering chime trail
    f=np.interp(eio((t-T0)/(T1-T0)),[0,1],[880,1320]); ph=2*np.pi*np.cumsum(f)/SR
    trem=0.6+0.4*np.sin(2*np.pi*14*t)
    y=(np.sin(ph)+0.3*np.sin(2.01*ph))*trem*act*(0.25+0.75*speed)*(0.4+0.6*fr)
    y=tvlp(y,1500+4000*fr)*0.5
    end=sum(np.sin(2*np.pi*f*ta)*a*np.exp(-ta/(1.6/k)) for k,(f,a) in enumerate(((1318.5,1),(1975.5,.5),(659.25,.6)),1))*np.minimum(ta/0.01,1)*g*0.12
else:          # deep cinematic swell
    f=np.interp(eio((t-T0)/(T1-T0)),[0,1],[55,82.4]); ph=2*np.pi*np.cumsum(f)/SR
    y=(np.sin(ph)+0.5*np.sin(2*ph)+0.25*np.sin(3*ph))*act*(0.3+0.7*speed)*(0.6+0.4*fr)
    y=tvlp(y,300+900*fr)
    fb=41.2+25*np.exp(-ta/0.2)
    end=(np.sin(2*np.pi*np.cumsum(fb)/SR)+0.3*np.sin(2*np.pi*164.8*ta))*np.minimum(ta/0.06,1)*np.exp(-ta/1.3)*g*0.6
L=y*np.sqrt((1-pan)/2)+end*0.7; R=y*np.sqrt((1+pan)/2)+end*0.7
ir_n=int(1.8*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.45); ir=lp(ir,4000,4); ir/=np.sqrt((ir**2).sum())*8
Lw=L+fftconvolve(L,ir)[:N]*0.5; Rw=R+fftconvolve(R,np.roll(ir,401))[:N]*0.5
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.0,5.0],[1,1,0])[:,None]
st=st/np.abs(st).max()*0.6
wavfile.write(sys.argv[2],SR,(st*32767).astype(np.int16))
