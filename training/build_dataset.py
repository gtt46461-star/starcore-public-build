#!/usr/bin/env python3
"""V49 strict point-in-time dataset, formula-parity with Android serving.

Every target t is built only from draws [0,t).  The array shape is
[target, position, candidate_number, 24].  It also stores the target number for
all ten positions so probability, Top3 action and position-selection metrics can
be evaluated without treating 100 candidate rows as independent periods.
"""
from __future__ import annotations
import argparse,csv,hashlib,json
from pathlib import Path
import numpy as np

WINDOWS=(6,12,24,48,96,180,360,720,1440)
FEATURE_NAMES=("position","number","freq_w6","freq_w12","freq_w24","freq_w48","freq_w96","freq_w180","freq_w360","freq_w720","freq_w1440","gap_norm","gap_percentile","trend_slope","trend_accel","volatility","position_entropy","top3_concentration","last_distance","neighbor_last","same_parity_last","same_big_last","switch_risk","family_strength")

def load(path:Path):
    periods=[];rows=[];times=[]
    with path.open(encoding='utf-8-sig',newline='') as f:
        for r in csv.DictReader(f):
            if str(r.get('valid','1'))!='1':continue
            nums=[int(x) for x in str(r['open_number']).split(',')]
            if len(nums)==10 and sorted(nums)==list(range(1,11)):
                periods.append(str(r['period']));rows.append(nums);times.append(int(r.get('open_time_utc_ms') or 0))
    order=np.argsort(np.asarray(periods))
    return np.asarray(periods)[order],np.asarray(rows,dtype=np.int16)[order],np.asarray(times,dtype=np.int64)[order]

def smooth(count,observed):return (count+.5)/(observed+5.0)
def entropy10(p):
    q=np.maximum(p,1e-12);return float(np.clip(-(q*np.log(q)).sum()/np.log(10.0),0,1))
def dist(a,b):return min(1.0,float(np.abs(a-b).sum()*.5))
def short_bias(h,p,start,end):
    x=h[max(0,start):min(len(h),end),p]
    return 0.0 if len(x)==0 else float((x>=6).mean()-.5)
def hazard(h,p):
    n=len(h)
    if n<12:return .5
    prev=short_bias(h,p,n-12,n-6);last=short_bias(h,p,n-6,n);changes=int(np.sign(prev)!=np.sign(last))
    earlier=short_bias(h,p,max(0,n-24),n-12);changes+=int(np.sign(earlier)!=np.sign(prev))
    magnitude=min(1.0,abs(last-prev)*2.0)
    return float(np.clip(.18+changes*.22+magnitude*.40,0,1))
def state_run(h,p):
    if len(h)<8:return 1
    n=len(h);cur=short_bias(h,p,n-6,n);sign=1 if cur>.08 else (-1 if cur<-.08 else 0);run=1
    for back in range(1,9):
        end=n-back;start=end-6
        if start<0:break
        b=short_bias(h,p,start,end);s=1 if b>.08 else (-1 if b<-.08 else 0)
        if s!=sign:break
        run+=1
    return run
def parity_rotation(h,p):
    if len(h)<6:return False
    x=h[-6:,p]&1;return int(np.sum(x[1:]!=x[:-1]))>=4
def big_rotation(h,p):
    if len(h)<6:return False
    x=(h[-6:,p]>=6);return int(np.sum(x[1:]!=x[:-1]))>=4

def classify(h,freq,gap_pct,slope,accel,vol,p):
    max_n=int(np.argmax(slope[p]));cold_n=int(np.argmax(gap_pct[p]));max_s=float(slope[p,max_n]);min_s=float(slope[p].min());max_gap=float(gap_pct[p,cold_n]);max_acc=float(accel[p].max())
    ent=entropy10(freq[2,p]);conc=float(np.sort(freq[2,p])[-3:].sum());recent=dist(freq[0,p],freq[3,p]);medium=dist(freq[2,p],freq[4,p]);cus=min(1.0,recent*2.4+medium*1.4);hz=hazard(h,p);switch=float(np.clip(.52*cus+.48*hz,0,1))
    if switch>.78:family='UNSTABLE_NOISE'
    elif switch>.62:family='REGIME_TRANSITION'
    elif ent<.72 and recent>.18:family='COMPRESSION_BREAKOUT'
    elif ent<.72:family='COMPRESSION_LOW_ENTROPY'
    elif ent>.96 and conc<.34:family='EXPANSION_HIGH_ENTROPY'
    elif max_s>.085 and max_acc>.025:family='HOT_ACCELERATION'
    elif max_s>.060 and freq[4,p,max_n]>.145 and accel[p,max_n]<-.015:family='HOT_EXHAUSTION'
    elif max_s>.050 and conc>.38:family='HOT_CONTINUATION_DENSE'
    elif max_s>.035:family='HOT_CONTINUATION_LIGHT'
    elif max_gap>.92 and freq[0,p,cold_n]>freq[2,p,cold_n]:family='COLD_RELEASE_CONFIRMED'
    elif max_gap>.86 and accel[p,cold_n]>0:family='COLD_RELEASE_EARLY'
    elif max_gap>.84:family='COLD_ACCUMULATION'
    elif min_s<-.055 and recent>.12:family='REVERSAL_EARLY'
    else:
        last=int(h[-1,p]);v=0.0
        if last-2>=0:v+=freq[0,p,last-2]-freq[3,p,last-2]
        if last<10:v+=freq[0,p,last]-freq[3,p,last]
        pulse=bool(np.any((freq[0,p]>.21)&(freq[2,p]<.13)&(vol[p]>.12)))
        if v>.055:family='NEIGHBOR_PROPAGATION'
        elif parity_rotation(h,p):family='PARITY_ROTATION'
        elif big_rotation(h,p):family='BIG_SMALL_ROTATION'
        elif pulse:family='PULSE_BURST'
        elif max_gap>.70 and ent>.88:family='INTERMITTENT_GAP'
        elif conc>.40:family='OSCILLATION_BIASED'
        else:family='OSCILLATION_BALANCED'
    if family.startswith('HOT'):proto=np.clip(.45+max_s*4+conc*.35,0,1)
    elif family.startswith('COLD'):proto=np.clip(.35+max_gap*.45+recent*.35,0,1)
    elif family.startswith('COMPRESSION'):proto=np.clip(.78-ent*.50+recent*.55,0,1)
    elif family.startswith('REVERSAL'):proto=np.clip(.40+abs(max_s)*3+recent*.45,0,1)
    elif family in ('REGIME_TRANSITION','UNSTABLE_NOISE'):proto=.25
    else:proto=np.clip(.42+(1-abs(ent-.90))*.25+conc*.20,0,1)
    return switch,float(np.clip(proto*(1-.55*switch),0,1)),family,state_run(h,p)

def _roll_count(cum,t,w):
    a=max(0,t-w);return cum[t]-cum[a],t-a

def build_dataset(arr,start):
    import bisect
    T,P=arr.shape;N=10
    one=np.zeros((T,P,N),np.int16)
    one[np.arange(T)[:,None],np.arange(P)[None,:],arr-1]=1
    cum=np.concatenate([np.zeros((1,P,N),np.int32),np.cumsum(one,axis=0,dtype=np.int32)],axis=0)
    out=np.empty((T-start,P,N,24),np.float32)
    last=np.full((P,N),-1,np.int32);gap_sorted=[[[] for _ in range(N)] for _ in range(P)]
    # Advance state so target=start sees exactly arr[:start].
    for i in range(start):
        for p in range(P):
            n=int(arr[i,p]-1)
            if last[p,n]>=0:bisect.insort(gap_sorted[p][n],i-int(last[p,n])-1)
            last[p,n]=i
    for oi,t in enumerate(range(start,T)):
        h=arr[:t]
        freq=np.empty((9,P,N),np.float64)
        for wi,w in enumerate(WINDOWS):
            count,observed=_roll_count(cum,t,w);freq[wi]=(count+.5)/(observed+5.0);freq[wi]/=freq[wi].sum(1,keepdims=True)
        gap=np.minimum(60,np.where(last<0,t,t-1-last))/60.0
        gap_pct=np.empty((P,N),np.float64)
        for p in range(P):
            for n in range(N):
                g=int(t if last[p,n]<0 else t-1-last[p,n]);gs=gap_sorted[p][n]
                gap_pct[p,n]=min(1.0,g/20.0) if len(gs)<3 else bisect.bisect_right(gs,g)/len(gs)
        slope=freq[1]-freq[3];accel=(freq[0]-freq[1])-(freq[2]-freq[3]);vol=np.zeros((P,N),np.float64)
        observed=min(24,t);hs=t-observed
        for p in range(P):
            for n in range(N):
                rates=[]
                for a in range(hs,t,6):
                    b=min(t,a+6);hit=int(cum[b,p,n]-cum[a,p,n]);rates.append(smooth(hit,b-a))
                vol[p,n]=min(1.0,float(np.std(rates))*4.0) if rates else 0.0
        for p in range(P):
            ent=entropy10(freq[2,p]);conc=float(np.sort(freq[2,p])[-3:].sum());sw,strength,_,_=classify(h,freq,gap_pct,slope,accel,vol,p);ln=int(arr[t-1,p])
            for n in range(N):
                out[oi,p,n]=[p/9.0,n/9.0,*freq[:,p,n].tolist(),gap[p,n],gap_pct[p,n],slope[p,n],accel[p,n],vol[p,n],ent,conc,abs((n+1)-ln)/9.0,float(abs((n+1)-ln)==1),float(((n+1)&1)==(ln&1)),float(((n+1)>=6)==(ln>=6)),sw,strength]
        # Update completed history for next target using actual draw at t.
        for p in range(P):
            n=int(arr[t,p]-1)
            if last[p,n]>=0:bisect.insort(gap_sorted[p][n],t-int(last[p,n])-1)
            last[p,n]=t
    return out

def main():
    ap=argparse.ArgumentParser();ap.add_argument('csv');ap.add_argument('--output',default='dist/v49_dataset.npz');ap.add_argument('--min-prefix',type=int,default=96);args=ap.parse_args()
    path=Path(args.csv);periods,arr,times=load(path);start=max(12,args.min_prefix)
    X=build_dataset(arr,start);y=arr[start:]-1;target=np.arange(start,len(arr),dtype=np.int32)
    sha=hashlib.sha256(path.read_bytes()).hexdigest();contract=hashlib.sha256(('V49|'+','.join(FEATURE_NAMES)).encode()).hexdigest()
    out=Path(args.output);out.parent.mkdir(parents=True,exist_ok=True);np.savez_compressed(out,X=X,y=y,target=target,period=periods,draws=arr,times=times,feature_names=np.asarray(FEATURE_NAMES),data_sha256=sha,feature_contract=contract)
    print(json.dumps({'draws':len(arr),'targets':len(X),'groups':len(X)*10,'features':24,'shape':list(X.shape),'data_sha256':sha,'feature_contract':contract,'output':str(out)},ensure_ascii=False))
if __name__=='__main__':main()

