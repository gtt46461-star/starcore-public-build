#!/usr/bin/env python3
"""R27 基线与零假设对照（可复现）。
用法: python3 -I r27_null_baseline.py <draws.csv> <out.json>
CSV列: period,open_time_utc_ms,open_time_local,open_number,block,hash,valid,fetch_time
内容:
  A. 13个简单策略在第1位置固定K3的逐期前推命中率（只用t-1及以前数据）
  B. 进化搜索(64个体x30代, 交叉+变异)在前半段训练、后半段留出未来上的命中率
  C. 同一进化流程在“打乱标签”上运行的对照（证明训练集上的代际上升可由过拟合产生）
  D. 43个零优势算法各跑20/32期时“最好那个”的命中率分布
  E. 88个已结算样本、滚动20期Brier在纯噪声下的波动区间
  F. 植入弱信号（第1位以概率q改为上一期第2位的号码，保持排列合法）后，同一进化流程能否在留出未来上找到它
不依赖任何App代码；结果只说明这批历史数据上的统计事实，不构成对未来的保证。
"""
import csv, json, sys
import numpy as np

def load(path):
    rows=[r for r in csv.DictReader(open(path)) if r['valid']=='1']
    rows.sort(key=lambda r:r['period'])
    return np.array([[int(x) for x in r['open_number'].split(',')] for r in rows])-1

def topk(s,k,rng):
    s=s+1e-9*rng.random(s.shape)
    return np.argsort(-s,axis=-1)[...,:k]

def part_a(D,rng,start=500):
    y=D[:,0];T=len(D);out={}
    def run(name,score):
        h=0
        for t in range(start,T):
            h+=int(y[t] in topk(score(t),3,rng))
        out[name]={'n':T-start,'hits':h,'rate':h/(T-start)}
    run('random',lambda t:rng.random(10))
    for W in (20,60,240,1000):
        run(f'hot_W{W}',lambda t,W=W:np.bincount(y[max(0,t-W):t],minlength=10).astype(float))
        run(f'cold_W{W}',lambda t,W=W:-np.bincount(y[max(0,t-W):t],minlength=10).astype(float))
    last=np.full(10,-1);gaps=np.zeros((T,10))
    for t in range(T):gaps[t]=t-last;last[y[t]]=t
    run('longest_gap',lambda t:gaps[t]);run('shortest_gap',lambda t:-gaps[t])
    tr=np.ones((10,10));M=np.zeros((T,10))
    for t in range(1,T):M[t]=tr[y[t-1]];tr[y[t-1],y[t]]+=1
    run('markov1',lambda t:M[t])
    cr=np.ones((10,10));C=np.zeros((T,10))
    for t in range(1,T):C[t]=cr[D[t-1,1]];cr[D[t-1,1],y[t]]+=1
    run('cross_pos2_lag1',lambda t:C[t])
    out['_standard_error']=(0.3*0.7/(T-start))**0.5
    return out

def features(D):
    y=D[:,0];T=len(D);Ws=[10,20,60,240];K=len(Ws)*2+3
    F=np.zeros((T,10,K));last=np.full(10,-1);tr=np.ones((10,10));cr=np.ones((10,10))
    for t in range(1,T):
        j=0
        for W in Ws:
            F[t,:,j]=np.bincount(y[max(0,t-W):t],minlength=10)/min(t,W);j+=1
            F[t,:,j]=np.bincount(D[max(0,t-W):t,1],minlength=10)/min(t,W);j+=1
        F[t,:,j]=np.minimum(t-1-last,100)/100;j+=1
        F[t,:,j]=tr[y[t-1]]/tr[y[t-1]].sum();j+=1
        F[t,:,j]=cr[D[t-1,1]]/cr[D[t-1,1]].sum();j+=1
        tr[y[t-1],y[t]]+=1;cr[D[t-1,1],y[t]]+=1;last[y[t-1]]=t-1
    return F,K

def evolve(F,Y,K,rng,gens=30,pop_n=64):
    N=len(Y);half=N//2;tr=slice(0,half);te=slice(half,N)
    def hit(w,sl):return (topk(F[sl]@w,3,rng)==Y[sl][:,None]).any(1).mean()
    pop=rng.normal(size=(pop_n,K));log=[]
    for g in range(gens):
        fit=np.array([hit(w,tr) for w in pop]);o=np.argsort(-fit)
        log.append({'generation':g,'best_train':float(fit[o[0]]),'mean_train':float(fit.mean())})
        elite=pop[o[:16]];kids=[]
        while len(kids)<pop_n-16:
            a,b=elite[rng.integers(16)],elite[rng.integers(16)]
            kids.append(np.where(rng.random(K)<.5,a,b)+rng.normal(scale=.3,size=K))
        pop=np.vstack([elite,kids])
    fit=np.array([hit(w,tr) for w in pop]);best=pop[int(np.argmax(fit))]
    return {'train_n':half,'test_n':N-half,'generations':log,'best_train':float(fit.max()),'best_heldout_future':float(hit(best,te))}

def plant(D,q,rng):
    D=D.copy()
    for t in range(1,len(D)):
        if rng.random()<q:
            v=D[t-1,1];j=int(np.where(D[t]==v)[0][0]);D[t,[0,j]]=D[t,[j,0]]
    return D

def main():
    D=load(sys.argv[1]);rng=np.random.default_rng(20261028);res={'draws':int(len(D))}
    res['A_simple_strategies_walk_forward']=part_a(D,rng)
    F,K=features(D);s=300;F=F[s:];Y=D[s:,0]
    res['B_evolution_real_labels']=evolve(F,Y,K,rng)
    res['C_evolution_shuffled_labels']=evolve(F,rng.permutation(Y),K,rng)
    for n in (20,32,120):
        m=rng.binomial(n,.3,size=(100000,43)).max(1)/n
        res[f'D_best_of_43_null_N{n}']={'median':float(np.median(m)),'p95':float(np.percentile(m,95)),'P_ge_0.50':float((m>=.5).mean())}
    band=[]
    for _ in range(4000):
        h=rng.random(88)<.3;p=np.clip(.3+rng.normal(scale=.08,size=88),.01,.99)
        r=[np.mean((p[i-20:i]-h[i-20:i])**2) for i in range(20,89)];band.append((min(r),max(r)))
    band=np.array(band)
    res['E_rolling20_brier_pure_noise_88']={'median_min':float(np.median(band[:,0])),'median_max':float(np.median(band[:,1])),'p5_min':float(np.percentile(band[:,0],5)),'p95_max':float(np.percentile(band[:,1],95)),'constant_0.3_expected':0.21}
    for q in (0.05,0.10):
        Dp=plant(D,q,rng);Fp,Kp=features(Dp)
        res[f'F_planted_signal_q{q}']=evolve(Fp[s:],Dp[s:,0],Kp,rng)
    json.dump(res,open(sys.argv[2],'w'),ensure_ascii=False,indent=1)
    print(json.dumps({k:(v if not isinstance(v,dict) or 'generations' not in v else {kk:vv for kk,vv in v.items() if kk!='generations'}) for k,v in res.items()},ensure_ascii=False,indent=1))
if __name__=='__main__':main()
