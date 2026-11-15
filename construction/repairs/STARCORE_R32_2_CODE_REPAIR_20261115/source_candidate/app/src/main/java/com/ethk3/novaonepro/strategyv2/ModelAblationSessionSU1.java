package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Matched historical model controls. All weights freeze durably before the common held-out block opens. */
public final class ModelAblationSessionSU1 {
    public enum Variant { PARENT_UNTRAINED,SIMPLE_FREQUENCY,TRAINED,NO_RELATION,NO_RECURRENT_MEMORY,SHUFFLED_RELATION,REPLACED_RELATION,SHUFFLED_LABEL }
    public static final String PROTOCOL="SU1_MODEL_ABLATIONS_V1|WIDTH2_WINDOW4_ADAM001_CLIP1|64_PAST_UPDATES|FIXED1440_HOLDOUT|NO_AUTHORITY";
    private static final Variant[] VARIANTS=Variant.values();
    private final int[][] raw;
    private final RelationTensorAdapterSU1.Factory factory;
    public final String dataset,kernel;
    public final long seed;
    public final int trainStart,trainEnd;
    private final int[] shuffled;
    private final RelGraphOptimizerSU1[] models=new RelGraphOptimizerSU1[VARIANTS.length];
    private final long[] updates=new long[VARIANTS.length],n=new long[VARIANTS.length],hits=new long[VARIANTS.length],micros=new long[VARIANTS.length];
    private final double[] logLoss=new double[VARIANTS.length];
    private final int[] hitMasks=new int[1440];
    private int variant=2,target;
    public String phase="TRAIN",frozenHash;
    private boolean durablyFrozen;
    public ModelAblationSessionSU1(int[][] input,String source,String kernel,long seed){
        if(input==null||input.length<1600||input.length>20000||seed<0)throw new IllegalArgumentException("SU1_MODEL_AUDIT_HISTORY");
        raw=new int[input.length][];StringBuilder numbers=new StringBuilder();for(int t=0;t<input.length;t++){RelationalStateInference.validateDraw(input[t]);raw[t]=input[t].clone();for(int value:raw[t])numbers.append(value).append(',');}
        this.kernel=HashingSU1.sha(kernel);this.seed=seed;dataset=HashingSU1.fields(PROTOCOL,HashingSU1.sha(source),HashingSU1.text(numbers.toString()),this.kernel,String.valueOf(seed));
        factory=new RelationTensorAdapterSU1.Factory(raw);trainEnd=raw.length-1440;trainStart=Math.max(4,trainEnd-64);target=trainStart;
        RelGraphModelSU1.Weights initial=RelGraphModelSU1.initial(2,2,2,2,RelationTensorAdapterSU1.SCHEMA,seed);
        for(int v=0;v<models.length;v++)models[v]=constrain(RelGraphOptimizerSU1.initial(initial),VARIANTS[v]);
        shuffled=new int[trainEnd-trainStart];for(int i=0;i<shuffled.length;i++)shuffled[i]=trainStart+i;Random random=new Random(seed^0x5355314d);for(int i=shuffled.length-1;i>0;i--){int q=random.nextInt(i+1),value=shuffled[i];shuffled[i]=shuffled[q];shuffled[q]=value;}
    }
    private static RelGraphOptimizerSU1 constrain(RelGraphOptimizerSU1 state,Variant variant){
        String zero=variant==Variant.NO_RELATION?"Wm":variant==Variant.NO_RECURRENT_MEMORY?"Wh":null;if(zero==null)return state;
        RelGraphModelSU1.Weights[] source={state.weights,state.first,state.second},next=new RelGraphModelSU1.Weights[3];
        for(int i=0;i<3;i++){Map<String,double[]> parameters=source[i].parameters();Arrays.fill(parameters.get(zero),0);next[i]=new RelGraphModelSU1.Weights(source[i].d,source[i].channels,source[i].roleCount,source[i].typeCount,source[i].featureSchemaHash,parameters);}
        return new RelGraphOptimizerSU1(next[0],next[1],next[2],state.steps);
    }
    private RelGraphModelSU1.Input input(int target,Variant variant,BudgetTokenSU1 budget){
        RelGraphModelSU1.Input input=factory.input(target,4,budget);
        if(variant==Variant.NO_RELATION||variant==Variant.SHUFFLED_RELATION||variant==Variant.REPLACED_RELATION)return RelGraphModelSU1.controlInput(input,variant.name(),seed);
        return input;
    }
    /** A LOCKED checkpoint must be committed and restored before this object can see holdout labels. */
    public void advance(BudgetTokenSU1 budget){
        if(phase.equals("DONE"))return;
        while(phase.equals("TRAIN")){
            budget.checkpointOrYield();final int v=variant;long start=System.nanoTime();
            try{
                RelGraphModelSU1.Input input=input(target,VARIANTS[v],budget);int labelTarget=VARIANTS[v]==Variant.SHUFFLED_LABEL?shuffled[target-trainStart]:target;
                RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(models[v].weights,input,raw[labelTarget],budget,TrainingObjectiveSU1.research());
                RelGraphOptimizerSU1 next=constrain(models[v].apply(gradient,.001,1),VARIANTS[v]);budget.checkpointOrYield();models[v]=next;updates[v]++;target++;
            }finally{micros[v]+=(System.nanoTime()-start)/1000;}
            if(target==trainEnd){variant++;target=trainStart;if(variant==models.length){variant=0;target=trainEnd;phase="LOCKED";frozenHash=freezeHash();return;}}
        }
        if(!durablyFrozen)throw new IllegalStateException("SU1_MODEL_HOLDOUT_REQUIRES_DURABLE_FREEZE");
        while(target<raw.length){
            budget.checkpointOrYield();final int v=variant;long start=System.nanoTime();
            try{
                RelGraphModelSU1.Input input=input(target,VARIANTS[v],budget);double[][] probabilities=VARIANTS[v]==Variant.SIMPLE_FREQUENCY?RelGraphModelSU1.frequencyBaseline(input):RelGraphModelSU1.predict(models[v].weights,input,budget);
                double[][] scores=new double[10][10];for(int p=0;p<10;p++)for(int number=0;number<10;number++)scores[p][number]=ProgramTreeSU1.clean(Math.log(Math.max(1e-12,probabilities[p][number])));
                int[] action=ProgramTreeSU1.action(scores);boolean hit=raw[target][action[0]-1]==action[1]||raw[target][action[0]-1]==action[2]||raw[target][action[0]-1]==action[3];double loss=0;for(int p=0;p<10;p++)loss-=Math.log(Math.max(1e-300,probabilities[p][raw[target][p]-1]))/10;
                budget.checkpointOrYield();n[v]++;if(hit){hits[v]++;hitMasks[target-trainEnd]|=1<<v;}logLoss[v]+=loss;variant++;
            }finally{micros[v]+=(System.nanoTime()-start)/1000;}
            if(variant==models.length){variant=0;target++;}
        }
        phase="DONE";
    }
    private String freezeHash(){List<String> parts=new ArrayList<>();parts.add(PROTOCOL);parts.add(dataset);parts.add(TrainingObjectiveSU1.research().hash);for(RelGraphOptimizerSU1 model:models)parts.add(HashingSU1.text(RelGraphCodecSU1.encode(model)));return HashingSU1.text(String.join("|",parts));}
    public String checkpoint(){
        Map<String,Object> m=new TreeMap<>();m.put("protocol",PROTOCOL);m.put("objective",TrainingObjectiveSU1.research().hash);m.put("dataset",dataset);m.put("kernel",kernel);m.put("seed",seed);m.put("phase",phase);m.put("variant",variant);m.put("target",target);m.put("frozen",frozenHash);
        List<String> states=new ArrayList<>(),loss=new ArrayList<>();for(RelGraphOptimizerSU1 model:models)states.add(RelGraphCodecSU1.encode(model));for(double value:logLoss)loss.add(Double.toHexString(value));m.put("states",states);m.put("loss",loss);m.put("updates",list(updates));m.put("n",list(n));m.put("hits",list(hits));m.put("micros",list(micros));List<Integer> masks=new ArrayList<>();for(int mask:hitMasks)masks.add(mask);m.put("hitMasks",masks);return StrictCanonicalJson.writeContext(m);
    }
    private static List<Long> list(long[] values){List<Long> out=new ArrayList<>();for(long value:values)out.add(value);return out;}
    private static long count(Object value){if(!(value instanceof Number)||!value.toString().matches("[0-9]+"))throw new IllegalArgumentException("SU1_MODEL_AUDIT_COUNTER");return Long.parseLong(value.toString());}
    private static void counts(Object value,long[] into){if(!(value instanceof List)||((List<?>)value).size()!=into.length)throw new IllegalArgumentException("SU1_MODEL_AUDIT_VECTOR");for(int i=0;i<into.length;i++)into[i]=count(((List<?>)value).get(i));}
    public static ModelAblationSessionSU1 restore(int[][] raw,String source,String kernel,String wire){
        Map<String,Object> m=StrictCanonicalJson.readContextObject(wire);ModelAblationSessionSU1 out=new ModelAblationSessionSU1(raw,source,kernel,count(m.get("seed")));if(!PROTOCOL.equals(m.get("protocol"))||!out.dataset.equals(m.get("dataset"))||!out.kernel.equals(m.get("kernel")))throw new IllegalArgumentException("SU1_MODEL_AUDIT_BINDING");
        out.phase=(String)m.get("phase");if(!Arrays.asList("TRAIN","LOCKED","DONE").contains(out.phase))throw new IllegalArgumentException("SU1_MODEL_AUDIT_PHASE");out.variant=Math.toIntExact(count(m.get("variant")));out.target=Math.toIntExact(count(m.get("target")));out.frozenHash=(String)m.get("frozen");
        counts(m.get("updates"),out.updates);counts(m.get("n"),out.n);counts(m.get("hits"),out.hits);counts(m.get("micros"),out.micros);List<?> states=(List<?>)m.get("states"),loss=(List<?>)m.get("loss"),masks=(List<?>)m.get("hitMasks");if(states.size()!=out.models.length||loss.size()!=out.models.length||masks.size()!=1440)throw new IllegalArgumentException("SU1_MODEL_AUDIT_STATE");
        for(int v=0;v<out.models.length;v++){String encoded=(String)states.get(v);out.models[v]=RelGraphCodecSU1.decode(encoded,HashingSU1.text(encoded));out.logLoss[v]=Double.valueOf((String)loss.get(v));if(!Double.isFinite(out.logLoss[v])||out.logLoss[v]<0||out.hits[v]>out.n[v]||out.n[v]>1440||out.updates[v]>out.trainEnd-out.trainStart)throw new IllegalArgumentException("SU1_MODEL_AUDIT_METRICS");}
        for(int i=0;i<1440;i++){out.hitMasks[i]=Math.toIntExact(count(masks.get(i)));if(out.hitMasks[i]>255)throw new IllegalArgumentException("SU1_MODEL_AUDIT_HIT_MASK");}
        if(out.variant<0||out.variant>=out.models.length||out.phase.equals("TRAIN")&&(out.variant<2||out.target<out.trainStart||out.target>=out.trainEnd)||!out.phase.equals("TRAIN")&&(out.target<out.trainEnd||out.target>raw.length||!out.freezeHash().equals(out.frozenHash)))throw new IllegalArgumentException("SU1_MODEL_AUDIT_CURSOR_OR_FROZEN_STATE");
        int complete=out.phase.equals("TRAIN")?0:out.target-out.trainEnd;
        for(int v=0;v<out.models.length;v++){
            long expectedUpdates=v<2?0:out.phase.equals("TRAIN")?(v<out.variant?out.trainEnd-out.trainStart:v==out.variant?out.target-out.trainStart:0):out.trainEnd-out.trainStart;
            long expectedN=out.phase.equals("TRAIN")?0:complete+(v<out.variant?1:0),countedHits=0;
            for(int t=0;t<1440;t++){boolean hit=(out.hitMasks[t]&(1<<v))!=0;if(hit){if(t>=expectedN)throw new IllegalArgumentException("SU1_MODEL_AUDIT_FUTURE_HIT");countedHits++;}}
            if(out.updates[v]!=expectedUpdates||out.models[v].steps!=expectedUpdates||out.n[v]!=expectedN||out.hits[v]!=countedHits)throw new IllegalArgumentException("SU1_MODEL_AUDIT_STATE_COUNTER_BINDING");
            if(!RelGraphCodecSU1.encode(out.models[v]).equals(RelGraphCodecSU1.encode(constrain(out.models[v],VARIANTS[v]))))throw new IllegalArgumentException("SU1_MODEL_AUDIT_ABLATION_CONSTRAINT");
        }
        out.durablyFrozen=!out.phase.equals("TRAIN");if(!out.checkpoint().equals(wire))throw new IllegalArgumentException("SU1_MODEL_AUDIT_UNKNOWN_OR_CHANGED_FIELDS");return out;
    }
    public String report(){
        if(!phase.equals("DONE")||!freezeHash().equals(frozenHash))throw new IllegalStateException("SU1_MODEL_AUDIT_NOT_DONE");List<Object> rows=new ArrayList<>();
        for(int v=0;v<models.length;v++){if(n[v]!=1440)throw new IllegalStateException("SU1_MODEL_AUDIT_COMMON_DENOMINATOR");long won=0,lost=0;for(int mask:hitMasks){boolean child=(mask&(1<<2))!=0,other=(mask&(1<<v))!=0;if(child&&!other)won++;if(!child&&other)lost++;}
            Map<String,Object> row=new TreeMap<>();row.put("variant",VARIANTS[v].name());row.put("opportunities",1440);row.put("actions",n[v]);row.put("hits",hits[v]);row.put("meanLogLossHex",Double.toHexString(logLoss[v]/n[v]));row.put("trainUpdates",updates[v]);row.put("computeElapsedMicros",micros[v]);row.put("trainedWinsComparator",won);row.put("trainedLossesComparator",lost);row.put("pairedN",1440);row.put("optimizerStateHash",HashingSU1.text(RelGraphCodecSU1.encode(models[v])));rows.add(row);}
        Map<String,Object> report=new TreeMap<>();report.put("protocol",PROTOCOL);report.put("dataset",dataset);report.put("kernel",kernel);report.put("seed",seed);report.put("objective",TrainingObjectiveSU1.research().hash);report.put("trainFirst",trainStart);report.put("trainLastExclusive",trainEnd);report.put("sealedFirst",trainEnd);report.put("sealedLastExclusive",raw.length);report.put("frozenModelsHash",frozenHash);report.put("variants",rows);report.put("scope","HISTORICAL_RESEARCH_ONLY_NO_ALPHA_NO_CERTIFICATE_NO_NATURAL_AUTHORITY");report.put("budget","SAME64_UPDATES_6_TRAINED_VARIANTS_SAME_ARCHITECTURE_SAME1440_EVALUATION;PARENT_AND_FREQUENCY_NO_TRAIN;ACTUAL_COMPUTE_REPORTED");return StrictCanonicalJson.writeContext(report);
    }
}
