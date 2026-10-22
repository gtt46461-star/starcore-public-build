#!/usr/bin/env python3
"""Package a fitted RELGRAPH18 Adam state for the phone. No evidence or authority fields are accepted."""
import argparse,base64,hashlib,struct
from pathlib import Path
from export_fitted_model import canonical,java_hex

PROTOCOL='SU1_RELGRAPH10_V1|FLOAT64|WINDOW_RESET|ISSUE_MEAN_CE|IMMUTABLE_FACTS|CANONICAL_EDGES'
FEATURES=hashlib.sha256(b'SU1_RELGRAPH_FEATURES_V1|POSITION_NUMBER_EVENTS|FREQ24:PROBABILITY_ESTIMATE|XT3_1000:PROBABILITY_ESTIMATE|MASK_SEPARATE|XT3_XT1_CONTEXT_HYPEREDGES_LOG_LIFT|2_ROLES_2_TYPES|SUPPORT_REAL_ISSUES|FIXED_SCALE_NO_FIT').hexdigest()
def inspect_state(wire):
    data=base64.b64decode(wire,validate=True);at=0
    def read(fmt):
        nonlocal at
        value=struct.unpack_from(fmt,data,at);at+=struct.calcsize(fmt);return value
    def utf():
        nonlocal at
        n,=read('>H');value=data[at:at+n].decode('ascii');at+=n;return value
    if read('>ii')!=(0x53553147,1) or utf()!=PROTOCOL or utf()!=FEATURES:raise ValueError('RELGRAPH_PROTOCOL_OR_FEATURE_SCHEMA')
    d,c,roles,types,steps=read('>iiiiq')
    if not 2<=d<=16 or (c,roles,types)!=(2,2,2) or not 0<=steps<100000000:raise ValueError('RELGRAPH_SHAPES')
    names=('E','U','R','Wx','Wm','Wh','b','Wo','c','Wf','vo','Wrole','wv','btype','utype','vtype','Wmsg','vout')
    sizes=(10*d,10*d,100,d*d,d*d,d*d,d,d*10,10,2*c*d,d,roles*d*d,types*d,types*d,types*d,types*d,d*d,d)
    import math
    for group in range(3):
        for name,n in zip(names,sizes):
            if utf()!=name or read('>i')!=(n,):raise ValueError('RELGRAPH_TENSOR_MANIFEST')
            values=read('>'+str(n)+'d')
            if any(not math.isfinite(v) or group==2 and v<0 for v in values):raise ValueError('RELGRAPH_NONFINITE_OR_VARIANCE')
    if at!=len(data):raise ValueError('RELGRAPH_STATE_TAIL')
    return d,steps
def checkpoint(state,window,dataset,through,parent='0'*64,rate=.001,clip=1):
    d,steps=inspect_state(state)
    if not 1<=window<=256:raise ValueError('RELGRAPH_WINDOW')
    return {'format':'SU1_RELGRAPH10_CHECKPOINT_V1','kernel':'RELGRAPH10_SU1','protocol':PROTOCOL,'features':FEATURES,'dtype':'FLOAT64','stateMode':'WINDOW_RESET',
        'normalizer':'FIXED_PROBABILITY_AND_LOG_LIFT_WITH_SEPARATE_MASKS','operators':'EVENT_FEATURE>ROLE_HYPEREDGE_TANH>INCOMING_SOFTMAX>MESSAGE_SUM>RECURRENT_TANH>EVENT_AND_STATE_HEAD>SOFTMAX',
        'width':d,'channels':2,'roles':2,'types':2,'window':window,'optimizer':'ADAM_BETA_09_0999_EPS_1E8_V1','steps':steps,'stateWire':state,'stateHash':hashlib.sha256(state.encode()).hexdigest(),
        'dataset':dataset,'trainedThroughIssue':through,'parent':parent,'learningRateHex':java_hex(rate),'clipHex':java_hex(clip)}
def portable(cp):
    wire=canonical(cp);key=hashlib.sha256(wire.encode()).hexdigest()
    payload={'format':'SU1_PORTABLE_FITTED_RELGRAPH_V3','program':f'GMODEL({key},{cp["window"]})','coverageHex':'0x1.0p0','marginGate':False,
        'datasetSha256':cp['dataset'],'trainedThroughIssue':cp['trainedThroughIssue'],'numericProtocol':'SU1_ATOM_CONDITION_V2_1E12','graphModel':wire}
    return {'payload':payload,'sha256':hashlib.sha256(canonical(payload).encode()).hexdigest()}
def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--state',required=True,type=Path);p.add_argument('--history',required=True,type=Path);p.add_argument('--window',required=True,type=int);p.add_argument('--trained-through-issue',required=True);p.add_argument('--parent-checkpoint-sha',default='0'*64);p.add_argument('--out',required=True,type=Path);a=p.parse_args()
    cp=checkpoint(a.state.read_text().strip(),a.window,hashlib.sha256(a.history.read_bytes()).hexdigest(),a.trained_through_issue,a.parent_checkpoint_sha);wire=canonical(portable(cp))+'\n'
    if len(wire.encode())>262144:p.error('phone artifact exceeds 256 KiB')
    a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(wire);print('EXPORTED_RELGRAPH_RESEARCH_ONLY',hashlib.sha256(canonical(cp).encode()).hexdigest())
if __name__=='__main__':main()
