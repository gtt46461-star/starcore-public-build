package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Exact Adam state for every relation, role, feature, recurrent and output parameter group. */
public final class RelGraphOptimizerSU1 {
    public final RelGraphModelSU1.Weights weights,first,second;
    public final long steps;
    public RelGraphOptimizerSU1(RelGraphModelSU1.Weights weights,RelGraphModelSU1.Weights first,RelGraphModelSU1.Weights second,long steps){
        if(weights==null||first==null||second==null||steps<0||steps>=100000000||weights.d!=first.d||weights.d!=second.d||weights.channels!=first.channels||weights.channels!=second.channels||weights.roleCount!=first.roleCount||weights.roleCount!=second.roleCount||weights.typeCount!=first.typeCount||weights.typeCount!=second.typeCount||!weights.featureSchemaHash.equals(first.featureSchemaHash)||!weights.featureSchemaHash.equals(second.featureSchemaHash))throw new IllegalArgumentException("SU1_RELGRAPH_OPTIMIZER_SHAPES");
        for(double[] tensor:second.tensors.values())for(double v:tensor)if(v<0)throw new IllegalArgumentException("SU1_RELGRAPH_ADAM_VARIANCE");this.weights=weights;this.first=first;this.second=second;this.steps=steps;
    }
    public static RelGraphOptimizerSU1 initial(RelGraphModelSU1.Weights weights){Map<String,double[]> zero=new LinkedHashMap<>();for(String key:RelGraphModelSU1.GROUPS)zero.put(key,new double[weights.get(key).length]);RelGraphModelSU1.Weights moment=new RelGraphModelSU1.Weights(weights.d,weights.channels,weights.roleCount,weights.typeCount,weights.featureSchemaHash,zero);return new RelGraphOptimizerSU1(weights,moment,moment,0);}
    public RelGraphOptimizerSU1 apply(RelGraphModelSU1.Gradient gradient,double rate,double clip){
        if(!Double.isFinite(rate)||rate<=0||rate>1||!Double.isFinite(clip)||clip<=0||clip>100||steps>=99999999)throw new IllegalArgumentException("SU1_RELGRAPH_OPTIMIZER");double norm=0;for(double[] values:gradient.tensors.values())for(double v:values)norm+=v*v;double scale=Math.min(1,clip/Math.max(1e-300,Math.sqrt(norm))),beta1=1-Math.pow(.9,steps+1),beta2=1-Math.pow(.999,steps+1);
        Map<String,double[]> value=new LinkedHashMap<>(),mean=new LinkedHashMap<>(),variance=new LinkedHashMap<>();
        for(String key:RelGraphModelSU1.GROUPS){double[] old=weights.get(key),m=first.get(key),v=second.get(key),g=gradient.tensors.get(key);if(g==null||g.length!=old.length)throw new IllegalArgumentException("SU1_RELGRAPH_GRADIENT_SHAPE");double[] nv=new double[old.length],nm=new double[old.length],ns=new double[old.length];
            for(int i=0;i<old.length;i++){double grad=g[i]*scale;nm[i]=.9*m[i]+.1*grad;ns[i]=.999*v[i]+.001*grad*grad;nv[i]=old[i]-rate*(nm[i]/beta1)/(Math.sqrt(ns[i]/beta2)+1e-8);}value.put(key,nv);mean.put(key,nm);variance.put(key,ns);
        }
        return new RelGraphOptimizerSU1(make(value),make(mean),make(variance),steps+1);
    }
    private RelGraphModelSU1.Weights make(Map<String,double[]> tensors){return new RelGraphModelSU1.Weights(weights.d,weights.channels,weights.roleCount,weights.typeCount,weights.featureSchemaHash,tensors);}
}
