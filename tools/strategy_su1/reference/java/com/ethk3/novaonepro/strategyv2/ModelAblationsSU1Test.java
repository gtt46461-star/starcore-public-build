package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
public final class ModelAblationsSU1Test {
    private static int checks;
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(60000,100000000,null);}
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    public static void main(String[] args){
        int[][] raw=new int[1600][10];for(int t=0;t<raw.length;t++)for(int p=0;p<10;p++)raw[t][p]=(t+p)%10+1;String source=HashingSU1.text("fixed control input"),kernel=HashingSU1.text("fixed control code");int original=Arrays.deepHashCode(raw);
        RelationTensorAdapterSU1.Factory factory=new RelationTensorAdapterSU1.Factory(raw);RelGraphModelSU1.Input x=factory.input(120,4,budget());RelGraphModelSU1.Weights weights=RelGraphModelSU1.initial(2,2,2,2,RelationTensorAdapterSU1.SCHEMA,11);
        for(String mode:new String[]{"NO_RELATION","SHUFFLED_RELATION","REPLACED_RELATION"}){
            RelGraphModelSU1.Input control=RelGraphModelSU1.controlInput(x,mode,11);check(!control.hash.equals(x.hash),"control has real source identity");check(control.provenance.equals("HISTORICAL_RECONSTRUCTED"),"control impersonates online facts");check(Arrays.deepEquals(RelGraphModelSU1.predict(weights,control,budget()),RelGraphModelSU1.predict(weights,RelGraphModelSU1.controlInput(x,mode,11),budget())),"control is not deterministic");
        }
        ModelAblationSessionSU1 session=new ModelAblationSessionSU1(raw,source,kernel,11);String before=session.checkpoint();try{session.advance(new BudgetTokenSU1(1000,1000000,()->true));throw new AssertionError("preempt ignored");}catch(BudgetTokenSU1.Yield expected){check(before.equals(session.checkpoint()),"preemption advanced model state");}
        session.advance(budget());check(session.phase.equals("LOCKED"),"models did not train");try{session.advance(budget());throw new AssertionError("holdout opened without durable model freeze");}catch(IllegalStateException expected){check(expected.getMessage().equals("SU1_MODEL_HOLDOUT_REQUIRES_DURABLE_FREEZE"),"wrong durable freeze error");}
        int[][] changed=new int[raw.length][];for(int t=0;t<raw.length;t++){changed[t]=raw[t].clone();if(t>=session.trainEnd){int value=changed[t][0];changed[t][0]=changed[t][1];changed[t][1]=value;}}
        ModelAblationSessionSU1 changedFuture=new ModelAblationSessionSU1(changed,source,kernel,11);changedFuture.advance(budget());check(StrictCanonicalJson.readContextObject(session.checkpoint()).get("states").equals(StrictCanonicalJson.readContextObject(changedFuture.checkpoint()).get("states")),"heldout labels changed pre-registered fitted models");
        String frozen=session.checkpoint();ModelAblationSessionSU1 restored=ModelAblationSessionSU1.restore(raw,source,kernel,frozen);check(frozen.equals(restored.checkpoint()),"optimizer/cursor resume changed state");
        while(!restored.phase.equals("DONE")){try{restored.advance(new BudgetTokenSU1(2000,30000000,null));}catch(BudgetTokenSU1.Yield yielded){/* actual job slice */}restored=ModelAblationSessionSU1.restore(raw,source,kernel,restored.checkpoint());}
        Map<String,Object> report=StrictCanonicalJson.readContextObject(restored.report());List<?> variants=(List<?>)report.get("variants");check(variants.size()==8,"model controls missing");
        for(int v=0;v<variants.size();v++){Map<?,?> row=(Map<?,?>)variants.get(v);check(((Number)row.get("pairedN")).intValue()==1440&&((Number)row.get("actions")).intValue()==1440,"unmatched heldout denominator");check(((Number)row.get("trainUpdates")).intValue()==(v<2?0:64),"unmatched training allocation");check(((Number)row.get("computeElapsedMicros")).longValue()>0,"actual model computation not measured");}
        check(original==Arrays.deepHashCode(raw),"model controls changed real history");check(frozen.contains(restored.frozenHash),"frozen model set changed after seeing holdout");
        System.out.println("SU1_MODEL_ABLATIONS_PASS checks="+checks+" variants=8 commonN=1440 updates=64 scope=synthetic_execution_not_natural_effectiveness");
    }
}
