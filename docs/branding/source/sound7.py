# Original, low-volume sting: soft air swoosh following the comet around the O,
# then a streaming-style double hit ("tum-TUM") when "play" appears, opening into a warm bloom.
import numpy as np, sys, math
from scipy.signal import butter, sosfilt, sosfilt_zi, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=5.5; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(13)
def lp(x,f,o=2): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def eio(x): x=np.clip(x,0,1); return np.where(x<.5,4*x**3,1-(-2*x+2)**3/2)
def tvlp(x,cut,blk=256):
    y=np.zeros_like(x); zi=None
    for i in range(0,len(x),blk):
        sos=butter(2,float(np.clip(cut[i],80,16000)),'low',fs=SR,output='sos')
        if zi is None: zi=sosfilt_zi(sos)*0
        y[i:i+blk],zi=sosfilt(sos,x[i:i+blk],zi=zi)
    return y
# 1) orbit swoosh (follows the comet)
T0,T1=0.45,2.25; ang=math.radians(-47); TOTAL=360+180-33
th=np.radians(180-TOTAL*eio((t-T0)/(T1-T0)))
sx=np.cos(th)*math.cos(ang)-0.18*np.sin(th)*math.sin(ang); fr=0.5+0.5*np.sin(th)
speed=np.abs(np.gradient(th)); speed/=speed.max()+1e-12
act=np.clip((t-T0+0.1)/0.25,0,1)*np.clip((T1+0.2-t)/0.3,0,1)
sw=tvlp(lp(rng.standard_normal(N),4000,4),400+2600*speed*(0.5+0.5*fr))*act*(0.1+0.9*speed**1.5)*(0.5+0.5*fr)
pan=np.clip(sx*0.85,-1,1)
L=sw*np.sqrt((1-pan)/2)*0.12; R=sw*np.sqrt((1+pan)/2)*0.12
# 2) double hit: soft "tum" at 2.95, deep "TUM" at 3.12 (when 'play' appears)
def hit(st,f0,f1,dec,amp):
    tt=np.clip(t-st,0,None); g=(t>=st)
    f=f1+(f0-f1)*np.exp(-tt/0.07); ph=2*np.pi*np.cumsum(f*g)/SR
    body=(np.sin(ph)+0.35*np.sin(2*ph))*np.minimum(tt/0.008,1)*np.exp(-tt/dec)*g
    thump=lp(rng.standard_normal(N),140,4)*np.minimum(tt/0.004,1)*np.exp(-tt/0.09)*g*0.6
    return lp(body+thump,900)*amp
h=hit(2.95,110,52,0.35,0.55)+hit(3.12,95,41,1.2,1.0)
L+=h; R+=h
# 3) warm bloom after the hit (F major add9), slow swell
bl=np.zeros(N)
for f in (87.31,130.81,174.61,220.0,261.63,392.0):
    for det in (-.08,0,.08):
        ff=f*2**(det/12); p=2*np.pi*ff*t+rng.uniform(0,6.28)
        bl+=sum(np.sin(h*p)/h for h in range(1,7))/3
bl=lp(bl,1400)*np.interp(t,[0,3.15,3.9,4.8,5.5],[0,0,.55,.4,0])
L+=bl*0.05; R+=bl*0.05
# gentle reverb + master
ir_n=int(2.2*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.55); ir=lp(ir,4000,4); ir/=np.sqrt((ir**2).sum())*8
Lw=L+fftconvolve(L,ir)[:N]*0.55; Rw=R+fftconvolve(R,np.roll(ir,431))[:N]*0.55
st=np.stack([Lw,Rw],1)*np.interp(t,[0,4.9,5.5],[1,1,0])[:,None]
st=st/np.abs(st).max()*0.6
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
