"""R23 executable HOST research mechanisms. Not an Android production implementation.
Input is exclusively the history before the target: [batch,time,10], digits 0..9.
All families return ten categorical logits; K3 probability is a marginal sum.
"""
from __future__ import annotations
import copy, hashlib, io, math
from dataclasses import dataclass
import numpy as np
import torch
from torch import nn
from torch.nn import functional as F

FAMILIES = ('causal_tcn','lstm','temporal_attention','relation_graph')

class CausalBlock(nn.Module):
    def __init__(self, width:int, dilation:int):
        super().__init__(); self.pad=2*dilation
        self.a=nn.Conv1d(width,width,3,dilation=dilation)
        self.b=nn.Conv1d(width,width,1)
    def forward(self,x):
        return x+self.b(F.gelu(self.a(F.pad(x,(self.pad,0)))))

class TemporalNet(nn.Module):
    def __init__(self,kind:str,width:int=32,depth:int=3):
        super().__init__()
        if kind not in FAMILIES: raise ValueError('unknown computational family')
        self.kind=kind;self.width=width;self.depth=depth
        if kind=='relation_graph':
            self.embed=nn.Embedding(10,width)
            self.pos=nn.Parameter(torch.zeros(1,10,width))
            self.rnn=nn.GRU(width,width,batch_first=True)
            self.q=nn.Linear(width,width,bias=False);self.k=nn.Linear(width,width,bias=False)
            self.v=nn.Linear(width,width,bias=False);self.out=nn.Linear(width*2,10)
        else:
            self.proj=nn.Linear(100,width)
            if kind=='causal_tcn': self.blocks=nn.ModuleList([CausalBlock(width,2**i) for i in range(depth)])
            elif kind=='lstm': self.rnn=nn.LSTM(width,width,batch_first=True)
            else:
                layer=nn.TransformerEncoderLayer(width,4,width*2,dropout=0.0,batch_first=True,norm_first=True)
                self.attn=nn.TransformerEncoder(layer,num_layers=2,enable_nested_tensor=False)
            self.out=nn.Linear(width,100)
    def forward(self,x:torch.Tensor)->torch.Tensor:
        if x.ndim!=3 or x.shape[-1]!=10: raise ValueError('expected B,T,10')
        if self.kind=='relation_graph':
            b,t,p=x.shape; h=self.embed(x).permute(0,2,1,3).reshape(b*p,t,self.width)
            _,h=self.rnn(h);h=h[-1].reshape(b,p,self.width)+self.pos
            w=torch.softmax(self.q(h)@self.k(h).transpose(-1,-2)/math.sqrt(self.width),-1)
            return self.out(torch.cat((h,w@self.v(h)),dim=-1))
        h=self.proj(F.one_hot(x,num_classes=10).float().flatten(2))
        if self.kind=='causal_tcn':
            h=h.transpose(1,2)
            for block in self.blocks: h=block(h)
            h=h[:,:,-1]
        elif self.kind=='lstm': h=self.rnn(h)[0][:,-1]
        else:
            t=h.shape[1]; pos=torch.arange(t,device=h.device).float()[:,None]
            freqs=torch.exp(torch.arange(0,self.width,2,device=h.device).float()*(-math.log(10000)/self.width))
            pe=torch.zeros(t,self.width,device=h.device);pe[:,0::2]=torch.sin(pos*freqs);pe[:,1::2]=torch.cos(pos*freqs)
            mask=torch.triu(torch.ones(t,t,device=h.device,dtype=torch.bool),diagonal=1)
            h=self.attn(h+pe,mask=mask)[:,-1]
        return self.out(h).reshape(-1,10,10)
    def config(self): return {'kind':self.kind,'width':self.width,'depth':self.depth}

def parameter_hash(model:nn.Module)->str:
    h=hashlib.sha256()
    for name,v in sorted(model.state_dict().items()):
        h.update(name.encode());h.update(str(tuple(v.shape)).encode());h.update(v.detach().cpu().numpy().tobytes())
    return h.hexdigest()

def coherent_k3(prob:np.ndarray):
    p=np.asarray(prob,dtype=np.float64)
    if p.shape!=(10,10) or not np.isfinite(p).all() or (p<0).any() or not np.allclose(p.sum(1),1,atol=1e-6):
        raise ValueError('invalid marginal table')
    # Stable tie break: smaller position and number.
    top=np.argsort(-p,axis=1,kind='stable')[:,:3]
    masses=np.take_along_axis(p,top,axis=1).sum(1)
    pos=int(np.argmax(masses)); return pos,tuple(sorted(int(x) for x in top[pos])),float(masses[pos])

def grow_tcn(parent:TemporalNet):
    """Function-preserving topology mutation; no parent parameters are modified.
    Optimizer transplantation is intentionally a separate validated operation.
    """
    if parent.kind!='causal_tcn': raise ValueError('TCN growth only')
    child=TemporalNet('causal_tcn',parent.width,parent.depth+1)
    child.load_state_dict(parent.state_dict(),strict=False)
    with torch.no_grad(): child.blocks[-1].b.weight.zero_();child.blocks[-1].b.bias.zero_()
    return child

def mutate_weights(parent:TemporalNet,seed:int,scale:float=0.005):
    if not (0<scale<=0.05): raise ValueError('bounded mutation required')
    child=copy.deepcopy(parent); g=torch.Generator().manual_seed(seed)
    with torch.no_grad():
        for p in child.parameters(): p.add_(torch.randn(p.shape,generator=g)*scale)
    return child

def distill_step(student:TemporalNet,teacher:TemporalNet,optimizer,x,y,temperature:float=2.0):
    """Call only with DEV histories and teachers trained no later than the run cutoff."""
    if temperature<=0: raise ValueError('temperature')
    teacher.eval();student.train()
    with torch.no_grad(): target=F.softmax(teacher(x)/temperature,dim=-1)
    s=student(x)
    kl=F.kl_div(F.log_softmax(s/temperature,dim=-1),target,reduction='sum')/(x.shape[0]*10)*temperature**2
    loss=.5*F.cross_entropy(s.reshape(-1,10),y.reshape(-1))+.5*kl
    optimizer.zero_grad();loss.backward();torch.nn.utils.clip_grad_norm_(student.parameters(),1.0);optimizer.step()
    return float(loss.detach())

def save_checkpoint(path,model,optimizer,trained_through:int,rule_hash:str):
    if trained_through<0 or not rule_hash: raise ValueError('checkpoint provenance required')
    torch.save({'config':model.config(),'weights':model.state_dict(),'optimizer':optimizer.state_dict(),
                'trained_through':trained_through,'rule_hash':rule_hash,'torch_rng':torch.get_rng_state()},path)

def load_checkpoint(path,expected_rule:str,target:int):
    # Only accept trusted, locally generated tensor checkpoint files.
    x=torch.load(path,map_location='cpu',weights_only=True)
    if x['rule_hash']!=expected_rule or x['trained_through']>=target: raise ValueError('incompatible/future checkpoint')
    m=TemporalNet(**x['config']);m.load_state_dict(x['weights'])
    opt=torch.optim.AdamW(m.parameters(),lr=0.001);opt.load_state_dict(x['optimizer'])
    torch.set_rng_state(x['torch_rng']);return m,opt,x['trained_through']
