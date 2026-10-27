"""Fail-closed machine-readable capability acceptance. Registry counts are not proof."""
from __future__ import annotations
from dataclasses import dataclass,asdict

@dataclass(frozen=True)
class CapabilityEvidence:
    capability:str
    implementation:str=''
    input_contract:str=''
    train_receipt:str=''
    weights_changed:bool=False
    checkpoint_restore_test:str=''
    predraw_receipt:str=''
    settlement_receipt:str=''
    feedback_receipt:str=''
    device_parity_receipt:str=''
    device_longrun_receipt:str=''
    quality_protocol:str=''
    quality_passed:bool=False
    def audit(self):
        gaps=[]
        for key in ('implementation','input_contract','train_receipt','checkpoint_restore_test','predraw_receipt',
                    'settlement_receipt','feedback_receipt','device_parity_receipt','device_longrun_receipt','quality_protocol'):
            if not getattr(self,key): gaps.append(key)
        if not self.weights_changed: gaps.append('weights_changed')
        if not self.quality_passed: gaps.append('quality_passed')
        return {'capability':self.capability,'complete':not gaps,'missing':gaps,
                'runtime_complete':not any(k in gaps for k in ('device_parity_receipt','device_longrun_receipt',
                    'predraw_receipt','settlement_receipt','feedback_receipt','checkpoint_restore_test','train_receipt','weights_changed','implementation','input_contract'))}

class LearningCursor:
    """Monotonic CAS reference: preparing weights cannot advance published state."""
    def __init__(self,through:int=0,artifact:str='seed'):
        self.through=through;self.artifact=artifact;self._pending={}
    def prepare(self,expected:int,next_through:int,artifact:str):
        if expected!=self.through or next_through<=expected or not artifact: raise ValueError('stale or non-monotonic')
        self._pending[artifact]=(expected,next_through)
    def commit(self,artifact:str,durable:bool):
        expected,nxt=self._pending.pop(artifact)
        if not durable: return False
        if self.through!=expected: return False
        self.through=nxt;self.artifact=artifact;return True
    def restore_available(self,first_retained:int,target:int):
        return first_retained-1<=self.through<target
