#!/usr/bin/env python3
"""Independent NumPy layer, full BPTT-gradient and SGD-update golden for deployed Android code."""
from pathlib import Path
import hashlib,json,sys
import numpy as np
from export_fitted_model import checkpoint,canonical,portable,java_hex
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'reference/python'))
from relational_state_model import RelationalStateModel,synthetic_examples,AdamOptimizer

def flat(x):return [java_hex(v) for v in np.asarray(x).ravel()]
def build():
    model=RelationalStateModel.from_bytes((ROOT/'reference/fixtures/relstate10_model.json').read_bytes())
    history,label=synthetic_examples(1,412)[0]
    dataset=hashlib.sha256(history.tobytes()+label.tobytes()).hexdigest()
    cp=checkpoint(model,len(history),0,.03,1,dataset,'202610210003','0'*64)
    probability,logits,state,relation,tape=model._forward(history)
    layers={'relation:-1':flat(relation),'probability:'+str(len(history)):flat(probability)}
    for i,(_,z,m,previous,h) in enumerate(tape):
        layers['embedding:'+str(i)]=flat(z);layers['message:'+str(i)]=flat(m);layers['state:'+str(i)]=flat(h)
    loss,gradients=model.loss_and_grads(history,label)
    model.train_batch([(history,label)],learning_rate=.03,clip_norm=1)
    adam_model=RelationalStateModel.from_bytes((ROOT/'reference/fixtures/relstate10_model.json').read_bytes());adam=AdamOptimizer(adam_model.params)
    adam_cp=checkpoint(adam_model,len(history),0,.03,1,dataset,'202610210003','0'*64,optimizer=adam)
    adam_model.train_batch([(history,label)],learning_rate=.03,clip_norm=1,optimizer=adam)
    result={'protocol':'SU1_TRAINING_PARITY_V1','checkpoint':canonical(cp),'portable':canonical(portable(cp)),
        'history':history.tolist(),'label':label.tolist(),'layers':layers,'loss':java_hex(loss),
        'gradients':{k:flat(v) for k,v in gradients.items()},'updated':{k:flat(v) for k,v in model.params.items()},
        'adamCheckpoint':canonical(adam_cp),'adamUpdated':{k:flat(v) for k,v in adam_model.params.items()},
        'tolerance':'0x1.b7cdfd9d7bdbbp-34'}
    wire=canonical(result)+'\n'
    (ROOT/'reference/fixtures/training_parity.json').write_text(wire)
    target=ROOT.parents[1]/'app/src/androidTest/assets/su1_training_parity.json';target.write_text(wire)
    print('TRAINING_GOLDEN',hashlib.sha256(wire.encode()).hexdigest(),'bytes',len(wire))
if __name__=='__main__':build()
