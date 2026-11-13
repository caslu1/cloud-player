# Original cinematic "logo sting": two deep hits followed by a swelling synth bloom.
import numpy as np, sys
from scipy.signal import butter, sosfilt, fftconvolve
from scipy.io import wavfile
SR=48000; DUR=4.5; N=int(SR*DUR); t=np.arange(N)/SR
rng=np.random.default_rng(7)
def env(a,d,start,peak=1.0):
    e=np.zeros(N); i0=int(start*SR); n=N-i0; tt=np.arange(n)/SR
    e[i0:]=peak*np.minimum(tt/a,1.0)*np.exp(-np.maximum(tt-a,0)/d); return e
def lp(x,f,o=4): return sosfilt(butter(o,f,'low',fs=SR,output='sos'),x)
def hp(x,f,o=2): return sosfilt(butter(o,f,'high',fs=SR,output='sos'),x)
def hit(start,f0,f1,dec,amp):
    i0=int(start*SR); tt=np.clip(t-start,0,None)
    f=f1+(f0-f1)*np.exp(-tt/0.06)                    # pitch drop
    ph=2*np.pi*np.cumsum(f)/SR
    body=np.sin(ph)*env(0.003,dec,start)
    body+=0.35*np.sin(2*ph+0.3)*env(0.003,dec*0.45,start)
    click=lp(hp(rng.standard_normal(N),400),3000)*env(0.001,0.025,start)*0.6
    thud=lp(rng.standard_normal(N),180)*env(0.002,0.12,start)*1.2
    x=(body+click+thud); x[:i0]=0; return amp*x
mono=np.zeros(N)
mono+=hit(0.20,140,48,0.45,0.75)     # "ta"
mono+=hit(0.52,120,38,1.10,1.00)     # "dum"
# bloom chord (D minor add9) with slow attack and opening filter
notes=[36.71,55.0,73.42,87.31,110.0,146.83,164.81,220.0]
pad=np.zeros(N)
for k,f in enumerate(notes):
    for det in (-0.12,0.0,0.13):
        ff=f*(2**(det/12)); ph=2*np.pi*ff*t+rng.uniform(0,6.28)
        saw=sum(np.sin(h*ph)/h for h in range(1,14) if h*ff<9000)
        pad+=saw*(0.9 if f<60 else 0.55)/3
cut=np.interp(t,[0,0.55,1.6,3.0,4.5],[120,180,1400,3200,900])
# time-varying low-pass via blockwise filtering
out=np.zeros(N); blk=1024; zi=None
for i in range(0,N,blk):
    sos=butter(2,cut[i],'low',fs=SR,output='sos')
    if zi is None: from scipy.signal import sosfilt_zi; zi=sosfilt_zi(sos)*0
    seg,zi=sosfilt(sos,pad[i:i+blk],zi=zi); out[i:i+blk]=seg
padenv=np.interp(t,[0,0.5,1.0,2.2,3.4,4.5],[0,0,0.55,0.85,0.6,0.0])
mono+=out*padenv*0.22
sub=np.sin(2*np.pi*36.71*t)*np.interp(t,[0,0.52,0.7,2.5,4.5],[0,0,0.5,0.4,0])
mono+=sub*0.35
# riser noise into the bloom
mono+=lp(hp(rng.standard_normal(N),1500),7000)*np.interp(t,[0,0.6,1.5,1.7,2.2],[0,0,0.05,0.07,0])
# sparkle on planet pop (1.6s) and shine (2.5s)
for st,base in ((1.62,1318.5),(2.55,1760.0),(2.68,2637.0)):
    tt=np.clip(t-st,0,None); g=(t>=st)
    mono+=g*0.09*np.exp(-tt/0.45)*(np.sin(2*np.pi*base*tt)+0.4*np.sin(2*np.pi*base*2.01*tt))
# simple reverb
ir_n=int(2.2*SR); ir=rng.standard_normal(ir_n)*np.exp(-np.arange(ir_n)/SR/0.55); ir=lp(ir,5000); ir/=np.abs(ir).sum()**0.5*30
L=mono+fftconvolve(mono,ir)[:N]*0.9
R=mono+fftconvolve(mono,np.roll(ir,331))[:N]*0.9
st=np.stack([L,R],1)
fade=np.interp(t,[0,4.0,4.5],[1,1,0]); st*=fade[:,None]
st=np.tanh(st/np.abs(st).max()*1.6)/np.tanh(1.6)*0.89
wavfile.write(sys.argv[1],SR,(st*32767).astype(np.int16))
