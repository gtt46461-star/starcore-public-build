#!/usr/bin/env python3
"""Export the implemented RELSTATE10 graph and exact plain-SGD continuation state to Android."""
import argparse,base64,hashlib,json,struct,sys
from pathlib import Path
import numpy as np
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'reference/python'))
from relational_state_model import RelationalStateModel,AdamOptimizer

def java_hex(value):
    head,exponent=float(value).hex().split('p')
    if '.' in head:head=head.rstrip('0').rstrip('.')
    if '.' not in head:head+='.0'
    return head+'p'+str(int(exponent))
def canonical(value):return json.dumps(value,sort_keys=True,separators=(',',':'),ensure_ascii=False)
def fields(*values):
    digest=hashlib.sha256()
    for value in values:
        data=str(value).encode();digest.update(str(len(data)).encode()+b':'+data)
    return digest.hexdigest()
def model_binary(model,params=None):
    params=model.params if params is None else params
    data=struct.pack('>iii',0x53553152,1,model.width)
    for key in ('E','U','R','Wx','Wm','Wh','b','Wo','c'):
        data+=np.asarray(params[key],dtype='>f8').tobytes()
    return base64.b64encode(data).decode()
def checkpoint(model,window,steps,rate,clip,dataset,through,parent,objective=(0,0,0,0),optimizer=None):
    d=model.width;weights=model_binary(model)
    objective_hex=[java_hex(x) for x in objective]
    obj_hash=fields('SU1_JOINT_LOSS_V2|CE_DRAW_MEAN|BRIER_MULTICLASS_DRAW_MEAN|SMOOTH_LAYER_MEAN|L2_PARAM_MEAN|RESOURCE_PARAM_COUNT',*objective_hex)
    result={'format':'SU1_RELSTATE10_CHECKPOINT_V2','graph':'EMBED_ADD_POSITION>SOFTMAX_RELATION>MESSAGE_SUM>TANH_RECURRENT>LINEAR>SOFTMAX',
        'dtype':'FLOAT64','normalizer':'CATEGORICAL_ONE_HOT_NO_FIT','state':'WINDOW_RESET_ZERO','axes':'ISSUE,POSITION,NUMBER',
        'shapes':f'E:10x{d};U:10x{d};R:10x10;Wx:{d}x{d};Wm:{d}x{d};Wh:{d}x{d};b:{d};Wo:{d}x10;c:10',
        'weights':weights,'weightsHash':hashlib.sha256(weights.encode()).hexdigest(),'window':window,'steps':steps,
        'optimizer':'SGD_NO_MOMENTUM_V1','learningRateHex':java_hex(rate),'clipNormHex':java_hex(clip),'objective':objective_hex,
        'objectiveHash':obj_hash,'datasetHash':dataset,'trainedThroughIssue':str(through),'parentCheckpointHash':parent}
    if optimizer is not None:
        if optimizer.steps!=steps:raise ValueError('OPTIMIZER_STEP_BINDING')
        first=model_binary(model,optimizer.m);second=model_binary(model,optimizer.v)
        result.update(format='SU1_RELSTATE10_ADAM_CHECKPOINT_V3',optimizer='ADAM_BETA_09_0999_EPS_1E8_V1',firstMoment=first,
                      firstMomentHash=hashlib.sha256(first.encode()).hexdigest(),secondMoment=second,secondMomentHash=hashlib.sha256(second.encode()).hexdigest())
    return result
def portable(cp):
    wire=canonical(cp);cp_hash=hashlib.sha256(wire.encode()).hexdigest()
    payload={'format':'SU1_PORTABLE_FITTED_MODEL_V2','program':f'MODEL({cp_hash},{cp["window"]})','coverageHex':'0x1.0p0',
        'marginGate':False,'datasetSha256':cp['datasetHash'],'trainedThroughIssue':cp['trainedThroughIssue'],
        'numericProtocol':'SU1_ATOM_CONDITION_V2_1E12','fittedModel':wire}
    return {'payload':payload,'sha256':hashlib.sha256(canonical(payload).encode()).hexdigest()}
def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--model',required=True,type=Path);p.add_argument('--history',required=True,type=Path)
    p.add_argument('--trained-through-issue',required=True);p.add_argument('--window',type=int,required=True)
    p.add_argument('--sgd-steps',type=int,required=True);p.add_argument('--learning-rate',type=float,default=.03)
    p.add_argument('--clip',type=float,default=1);p.add_argument('--parent-checkpoint-sha',default='0'*64)
    p.add_argument('--adam-state',type=Path,help='Canonical AdamOptimizer.export_bytes() state, when continuing Adam')
    p.add_argument('--out',required=True,type=Path);a=p.parse_args()
    model=RelationalStateModel.from_bytes(a.model.read_bytes())
    if model.width>16 or not 1<=a.window<=256 or a.sgd_steps<0:p.error('mobile resource limits')
    optimizer=AdamOptimizer.from_bytes(a.adam_state.read_bytes(),model.params) if a.adam_state else None
    cp=checkpoint(model,a.window,a.sgd_steps,a.learning_rate,a.clip,hashlib.sha256(a.history.read_bytes()).hexdigest(),a.trained_through_issue,a.parent_checkpoint_sha,optimizer=optimizer)
    a.out.parent.mkdir(parents=True,exist_ok=True);a.out.write_text(canonical(portable(cp))+'\n')
    print('EXPORTED_FITTED_RESEARCH_MODEL',hashlib.sha256(canonical(cp).encode()).hexdigest())
if __name__=='__main__':main()
