"""Chronological Number/Gate -> actual META96 dependencies -> Ranker input.

Unchanged META slots remain the original predraw observations. No Number input is
reconstructed from META: all Number/Gate fits consume directly captured tensors.
"""
from pathlib import Path
import numpy as np


def rebuild(template, probability, gate_weights, actions):
    if template.shape != (len(probability),1200,96) or probability.shape[1:] != (10,10) or gate_weights.shape != (len(probability),9):
        raise ValueError('STACKED_META_INPUT_SHAPE')
    out=np.array(template,copy=True); positions=actions[:,0]-1; triples=actions[:,1:4]-1
    for t,q in enumerate(probability):
        if not np.isfinite(q).all() or (q<0).any() or max(np.max(abs(q.sum(0)-1)),np.max(abs(q.sum(1)-1)))>1e-6:
            raise ValueError('STACKED_META_INVALID_NUMBER_Q')
        selected=q[positions[:,None],triples]
        raw=selected[:,0]+selected[:,1]+selected[:,2]
        candidates=q[positions].copy()
        candidates[np.arange(1200)[:,None],triples]=-np.inf
        fourth=candidates.argmax(1);boundary=selected.min(1)-q[positions,fourth]
        entropy=np.clip(-np.sum(np.maximum(q,1e-15)*np.log(np.maximum(q,1e-15)),axis=1)/np.log(10),0,1)
        stability=np.clip(.45*out[t,:,14]+.35*out[t,:,20]+.20*np.clip(.5+boundary*12,0,1),0,1)
        out[t,:,0]=raw;out[t,:,1]=boundary;out[t,:,2]=entropy[positions];out[t,:,3]=stability
        mean=float(np.sum(raw)/1200);sd=float(np.sqrt(np.sum((raw-mean)**2)/1200+1e-12))
        out[t,:,5]=raw-mean;out[t,:,6]=(raw-mean)/sd;out[t,:,10]=(fourth+1)/10
        for expert in range(9):out[t,:,24+5*expert]=gate_weights[t,expert]
        # Installed V51 contract: 66 upstream slots + eight directly frozen global slots.
        for k in range(74,96):
            source=(k*17+13)%k
            out[t,:,k]=np.clip(out[t,:,source]*((raw-.30)*(.5+stability)),-3,3)
    if not np.isfinite(out).all():raise ValueError('STACKED_META_NONFINITE')
    return out


def create(directory:Path):
    from training.r929 import pipeline as p,deep_candidates as d
    x,y,issues,manifest=p.load_dataset(directory);state,experts,labels=p.load_raw_dataset(directory,manifest)
    comparison=p.read_json(directory/'number_comparison.json');expected=comparison.pop('result_sha256')
    if p.digest(p.canonical(comparison))!=expected:raise ValueError('STACK_NUMBER_COMPARISON_HASH')
    continuity=''
    try:d.require_contiguous_issues(issues)
    except ValueError as error:continuity=str(error)
    kind=comparison['tune_champion'];gate=p.read_artifact(directory/'gate_stage.json')
    number=p.read_artifact(directory/('number_'+kind+'.json')) if kind in ('D0','D1','D2','D3') else None
    inputs=d.feature_vectors(state,experts);slow=d.slow_memory(inputs);teacher=d.base_probability(experts)
    end=manifest['splits']['train'][1];warmup=max(18,end//3)
    if end-warmup<35:raise ValueError('STACKED_OOF_NEEDS_35_ROWS_AFTER_WARMUP')
    output=np.lib.format.open_memmap(directory/'stacked_meta.npy',mode='w+',dtype=np.float64,shape=x.shape);output[:]=np.nan
    folds=[];edges=list(map(int,np.linspace(warmup,end,6)))
    for start,stop in zip(edges[:-1],edges[1:]):
        train_end=start-1 # one whole issue embargo
        cut=max(8,int(train_end*.70));validation_start=cut+1
        if train_end-validation_start<3:raise ValueError('STACK_INNER_SUPPORT')
        candidates=[]
        # Hyperparameter/family selection for this OOF chunk uses only the earlier prefix.
        candidates.append(('BASE_NUMBER_Q_9_EXPERT_MEAN',d.metrics(teacher[validation_start:train_end],labels[validation_start:train_end])['cross_entropy']))
        failures=[]
        for family in ('D0','D1','D2','D3'):
            try:
                if family=='D2' and continuity:raise ValueError(continuity)
                inner=d.train(family,inputs[:cut],slow[:cut],experts[:cut],labels[:cut],teacher[:cut],epochs=2)
                score=d.metrics(d.predict(inner,inputs[validation_start:train_end],slow[validation_start:train_end],experts[validation_start:train_end]),labels[validation_start:train_end])
                candidates.append((family,score['cross_entropy']))
            except (ValueError,FloatingPointError) as error:failures.append({'family':family,'reason':str(error)})
        inner_gate=d.train_gate9(state[:cut],experts[:cut],labels[:cut],epochs=12)
        candidates.append(('GATE9',d.metrics(d.predict_gate9(inner_gate,state[validation_start:train_end],experts[validation_start:train_end])[0],labels[validation_start:train_end])['cross_entropy']))
        selected=min(candidates,key=lambda row:(row[1],row[0]))[0]
        gm=d.train_gate9(state[:train_end],experts[:train_end],labels[:train_end]);gq,gw=d.predict_gate9(gm,state[start:stop],experts[start:stop]);nm=None
        if selected=='BASE_NUMBER_Q_9_EXPERT_MEAN':q=teacher[start:stop]
        elif selected=='GATE9':q=gq
        else:
            nm=d.train(selected,inputs[:train_end],slow[:train_end],experts[:train_end],labels[:train_end],teacher[:train_end])
            q=d.predict(nm,inputs[start:stop],slow[start:stop],experts[start:stop])
        output[start:stop]=rebuild(x[start:stop],q,gw,p.ACTIONS)
        folds.append({'train':[0,train_end],'embargo':[train_end,start],'test':[start,stop],
                      'trained_until_issue':issues[train_end-1],'first_test_issue':issues[start],
                      'inner_validation':[validation_start,train_end],'selected_number_family':selected,
                      'number_hash':p.digest(p.canonical(d.serial(nm))) if nm is not None else p.digest(selected.encode()),
                      'gate_hash':p.digest(p.canonical(d.serial(gm))),'failed_trials':failures})
    # Later splits use the final frozen Number/Gate. No CAL/POLICY/SEALED labels enter fits.
    gm={key:np.asarray(value) for key,value in gate['model'].items()};gq,gw=d.predict_gate9(gm,state[end:],experts[end:])
    if kind=='BASE_NUMBER_Q_9_EXPERT_MEAN':q=teacher[end:]
    elif kind=='GATE9':q=gq
    else:
        nm=dict(number['model']);nm['mean']=np.asarray(nm['mean']);nm['scale']=np.asarray(nm['scale'])
        for key in ('weights','biases'):nm[key]=[np.asarray(value) for value in nm[key]]
        q=d.predict(nm,inputs[end:],slow[end:],experts[end:])
    for start in range(end,len(x),32):
        stop=min(len(x),start+32);output[start:stop]=rebuild(x[start:stop],q[start-end:stop-end],gw[start-end:stop-end],p.ACTIONS)
    output.flush()
    result={'revision':'R932_CHRONOLOGICAL_UPSTREAM_STACK_V1','dataset_contract_sha256':manifest['dataset_contract_sha256'],
            'source_meta_sha256':manifest['features_sha256'],'stacked_meta_sha256':p.file_digest(directory/'stacked_meta.npy'),
            'ranker_train_start':warmup,'ranker_train_end':end,'number_comparison_sha256':expected,
            'number_sha256':number['artifact_sha256'] if number else kind,'gate_sha256':gate['artifact_sha256'],
            'folds':folds,'sealed_labels_used':False,'changed_slots':[0,1,2,3,5,6,10]+list(range(24,65,5))+list(range(74,96)),
            'unavailable_warmup_is_nan':True,'production_authority':False}
    result['artifact_sha256']=p.digest(p.canonical(result));p.write_json(directory/'stacked_meta_manifest.json',result);return result


def load(directory:Path,manifest,ranker=None):
    from training.r929 import pipeline as p
    if not (directory/'stacked_meta_manifest.json').exists():create(directory)
    contract=p.read_artifact(directory/'stacked_meta_manifest.json')
    gate=p.read_artifact(directory/'gate_stage.json')
    if contract['dataset_contract_sha256']!=manifest['dataset_contract_sha256'] or contract['gate_sha256']!=gate['artifact_sha256'] or p.file_digest(directory/'stacked_meta.npy')!=contract['stacked_meta_sha256']:
        raise ValueError('STACKED_META_DEPENDENCY_DRIFT')
    comparison=p.read_json(directory/'number_comparison.json')
    expected=comparison.pop('result_sha256')
    if p.digest(p.canonical(comparison))!=expected or expected!=contract['number_comparison_sha256']:raise ValueError('STACKED_NUMBER_PARENT_CHANGED')
    kind=comparison['tune_champion']
    number_hash=p.read_artifact(directory/('number_'+kind+'.json'))['artifact_sha256'] if kind in ('D0','D1','D2','D3') else kind
    if number_hash!=contract['number_sha256']:raise ValueError('STACKED_NUMBER_ARTIFACT_CHANGED')
    if ranker is not None and ranker.get('upstream_stack_sha256')!=contract['artifact_sha256']:raise ValueError('RANKER_UPSTREAM_STACK_CHANGED')
    return np.load(directory/'stacked_meta.npy',mmap_mode='r'),contract

