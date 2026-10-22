#!/usr/bin/env python3
"""Independent NumPy RELGRAPH forward and central-difference gradients for every parameter."""
from pathlib import Path
import base64,hashlib,json,struct
import numpy as np
from export_fitted_model import canonical,java_hex
import export_relgraph_artifact

ROOT=Path(__file__).resolve().parents[1]
PROTOCOL='SU1_RELGRAPH10_V1|FLOAT64|WINDOW_RESET|ISSUE_MEAN_CE|IMMUTABLE_FACTS|CANONICAL_EDGES'
SCHEMA=hashlib.sha256(b'SYNTHETIC_RELGRAPH_CONTRACT_C2_R2_T2').hexdigest()
GROUPS=('E','U','R','Wx','Wm','Wh','b','Wo','c','Wf','vo','Wrole','wv','btype','utype','vtype','Wmsg','vout')
D,C,ROLES,TYPES=2,2,2,2
SHAPES=((10,D),(10,D),(10,10),(D,D),(D,D),(D,D),(D,),(D,10),(10,),(2*C,D),(D,),(ROLES,D,D),(TYPES,D),(TYPES,D),(TYPES,D),(TYPES,D),(D,D),(D,))
def utf(value):
    wire=value.encode('ascii');return struct.pack('>H',len(wire))+wire
def state_wire(parameters,schema=SCHEMA):
    wire=struct.pack('>ii',0x53553147,1)+utf(PROTOCOL)+utf(schema)+struct.pack('>iiiiq',D,C,ROLES,TYPES,0)
    for values in (parameters,{k:np.zeros_like(v) for k,v in parameters.items()},{k:np.zeros_like(v) for k,v in parameters.items()}):
        for key in GROUPS:
            vector=np.asarray(values[key],dtype='>f8').ravel();wire+=utf(key)+struct.pack('>i',len(vector))+vector.tobytes()
    return base64.b64encode(wire).decode()
def inputs():
    result=[]
    for t in range(3):
        root=hashlib.sha256(('synthetic source '+str(t)).encode()).hexdigest();features=np.empty((100,2));edges=[]
        for node in range(100):
            p,n=divmod(node,10);features[node]=(1 if n==(p+3)%10 else -1,np.sin(node*.31+t*.47))
            for kind in range(2):
                q=(p+3+kind)%10;r=(q+2)%10
                sources=[q*10+q,r*10+r];roles=[0,1]
                key=f'{p}:{n}|{kind}|[{", ".join(sorted(f"{role}:{src//10}:{src%10}" for role,src in zip(roles,sources)))}]|{t+1}|{root}'
                edges.append((key,node,kind,sources,roles,float(np.sin(node*.13+kind)),10+node%7))
        edges.sort(key=lambda e:e[0]);result.append({'issue':t+1,'root':root,'features':features,'mask':np.ones((100,2),dtype=bool),'draw':np.arange(1,11),'edges':edges})
    return result
def input_wire(steps):
    wire=struct.pack('>ii',0x53553149,1)+utf(SCHEMA)+struct.pack('>qq',4,3)+utf('HISTORICAL_RECONSTRUCTED')+struct.pack('>i',len(steps))
    for s in steps:
        wire+=struct.pack('>qq',s['issue'],s['issue'])+utf(s['root'])+struct.pack('>i',C)
        wire+=bytes(s['draw'].tolist())+bytes(np.repeat(np.arange(10),10).tolist())+bytes(np.tile(np.arange(10),10).tolist())
        wire+=np.asarray(s['features'],dtype='>f8').tobytes()+s['mask'].astype(np.uint8).tobytes()+struct.pack('>i',len(s['edges']))
        for _,receiver,kind,sources,roles,value,support in s['edges']:
            wire+=struct.pack('>BBidqB',receiver,kind,support,value,s['issue'],len(sources))
            for source,role in zip(sources,roles):wire+=struct.pack('>BB',source,role)
    return base64.b64encode(wire).decode()
def softmax(x):
    z=np.exp(x-np.max(x,axis=-1,keepdims=True));return z/z.sum(axis=-1,keepdims=True)
def forward(w,steps,trace=False):
    state=np.zeros((10,D));relation=softmax(w['R']);layers={};pidx=np.repeat(np.arange(10),10);nidx=np.tile(np.arange(10),10)
    for t,s in enumerate(steps):
        actual=np.arange(10)*10+s['draw']-1;occurrence=np.zeros(100);occurrence[actual]=1
        x=w['E'][nidx]+w['U'][pidx]+np.concatenate((s['features']*s['mask'],s['mask'].astype(float)),axis=1)@w['Wf']+occurrence[:,None]*w['vo']
        receiver=np.array([e[1] for e in s['edges']]);kind=np.array([e[2] for e in s['edges']]);sources=np.array([e[3] for e in s['edges']]);roles=np.array([e[4] for e in s['edges']]);value=np.array([e[5] for e in s['edges']])
        q=np.tanh(np.einsum('esk,eskj->esj',x[sources],w['Wrole'][roles]).mean(axis=1)+value[:,None]*w['wv'][kind]+w['btype'][kind])
        scores=np.sum(x[receiver]*w['utype'][kind],axis=1)+np.sum(q*w['vtype'][kind],axis=1)
        maximum=np.full(100,-np.inf);np.maximum.at(maximum,receiver,scores);attention=np.exp(scores-maximum[receiver]);denominator=np.zeros(100);np.add.at(denominator,receiver,attention);attention/=denominator[receiver]
        aggregate=np.zeros((100,D));np.add.at(aggregate,receiver,attention[:,None]*q);v=x+aggregate@w['Wmsg'];z=v[actual];message=relation@z
        state=np.tanh(z@w['Wx']+message@w['Wm']+state@w['Wh']+w['b'])
        if trace:
            for name,array in (('event',x),('edge',q),('attention',attention),('value',v),('state',state)):layers[f'{name}:{t}']=[java_hex(a) for a in array.ravel()]
    probabilities=softmax(state@w['Wo']+(v@w['vout']).reshape(10,10)+w['c'])
    return probabilities,layers
def build():
    rng=np.random.default_rng(197);weights={k:rng.normal(0,.15,size=shape) for k,shape in zip(GROUPS,SHAPES)};steps=inputs();actual=(np.arange(10)+3)%10
    probability,layers=forward(weights,steps,True)
    def loss():return float(-np.log(forward(weights,steps)[0][np.arange(10),actual]).mean())
    before=loss();gradients={}
    for key in GROUPS:
        tensor=weights[key];grad=np.empty_like(tensor)
        for index in np.ndindex(tensor.shape):
            old=float(tensor[index]);epsilon=1e-4;tensor[index]=old+epsilon;plus=loss();tensor[index]=old-epsilon;minus=loss();tensor[index]=old;grad[index]=(plus-minus)/(2*epsilon)
        gradients[key]=grad
    norm=np.sqrt(sum(np.sum(v*v) for v in gradients.values()));scale=.03*min(1,1/max(norm,1e-300));updated={k:w-scale*gradients[k] for k,w in weights.items()}
    state=state_wire(weights);source=input_wire(steps)
    export_state=state_wire(weights,export_relgraph_artifact.FEATURES)
    export=export_relgraph_artifact.portable(export_relgraph_artifact.checkpoint(export_state,4,hashlib.sha256(b'graph transport fixture').hexdigest(),'202610210001'))
    result={'protocol':'SU1_RELGRAPH_PARITY_V1','scope':'SYNTHETIC_NUMERICS_NO_NATURAL_AUTHORITY','input':source,'inputHash':hashlib.sha256(source.encode()).hexdigest(),'state':state,'stateHash':hashlib.sha256(state.encode()).hexdigest(),
        'label':(actual+1).tolist(),'layers':layers,'probability':[java_hex(v) for v in probability.ravel()],'loss':java_hex(before),
        'gradientMethod':'INDEPENDENT_NUMPY_FORWARD_CENTRAL_DIFFERENCE_EPS_1E4','gradients':{k:[java_hex(v) for v in g.ravel()] for k,g in gradients.items()},'updated':{k:[java_hex(v) for v in g.ravel()] for k,g in updated.items()},'portableGraph':canonical(export)}
    wire=canonical(result)+'\n';(ROOT/'reference/fixtures/relgraph_parity.json').write_text(wire);(ROOT.parents[1]/'app/src/androidTest/assets/su1_relgraph_parity.json').write_text(wire)
    print('RELGRAPH_GOLDEN bytes',len(wire),'values',sum(len(v) for v in layers.values()),'groups',len(gradients),'sha256',hashlib.sha256(wire.encode()).hexdigest())
if __name__=='__main__':build()
