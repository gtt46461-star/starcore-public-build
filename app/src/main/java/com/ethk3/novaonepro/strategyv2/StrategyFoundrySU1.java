package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Mobile continuous creation. The desktop tool uses the same eight production roles at larger budgets. */
public final class StrategyFoundrySU1 implements AutoCloseable {
    public static final class Genome {
        public final String text,family,arm,operator,parent;
        public final double coverage,parentFitness;public final boolean margin;
        public Genome(String text,String family,String arm,String operator,String parent,double parentFitness,double coverage,boolean margin){this.text=ProgramTextSU1.canonical(text);this.family=family;this.arm=arm;this.operator=operator;this.parent=parent;this.parentFitness=parentFitness;this.coverage=coverage;this.margin=margin;}
        public String key(){return HashingSU1.fields("SU1_FOUNDRY_GENOME_V2",ProgramTextSU1.semanticCanonical(text),Double.toHexString(coverage),String.valueOf(margin));}
        void write(DataOutput out)throws IOException{out.writeUTF(text);out.writeUTF(family);out.writeUTF(arm);out.writeUTF(operator);out.writeUTF(parent);out.writeDouble(parentFitness);out.writeDouble(coverage);out.writeBoolean(margin);}
        static Genome read(DataInput in)throws IOException{return new Genome(in.readUTF(),in.readUTF(),in.readUTF(),in.readUTF(),in.readUTF(),in.readDouble(),in.readDouble(),in.readBoolean());}
    }
    public static final class Ranked {
        public final Genome genome;public final double fitness,valFitness;
        public final BehaviorSignatureSU1 behavior;
        Ranked(Genome g,double fitness,double val,BehaviorSignatureSU1 behavior){genome=g;this.fitness=fitness;valFitness=val;this.behavior=behavior;}
        void write(DataOutput out)throws IOException{genome.write(out);out.writeDouble(fitness);out.writeDouble(valFitness);behavior.write(out);}
        static Ranked read(DataInput in)throws IOException{return new Ranked(Genome.read(in),in.readDouble(),in.readDouble(),BehaviorSignatureSU1.read(in));}
    }
    public static final class Attempt {
        public final Genome genome;public final Ranked ranked;public final String result,detail;
        Attempt(Genome g,Ranked r,String result,String detail){genome=g;ranked=r;this.result=result;this.detail=detail;}
    }
    private final int[][] draws;
    public final String datasetHash;
    public final long seed;
    public final int population,generations,devStart,devEnd,valStart,valEnd;
    public final ResearchContextSU1 researchContext;
    public boolean memoryEnabled=true;
    public ResearchDirectorPolicySU1 director=new ResearchDirectorPolicySU1();
    public MacroLibrarySU1 macros=new MacroLibrarySU1();
    public int phase,index,generation,evaluated;
    private final List<Ranked> best=new ArrayList<>(),screenTop=new ArrayList<>();
    private final List<Genome> generationQueue=new ArrayList<>();
    private final Set<String> seen=new HashSet<>();
    public static final class Curve {
        public final int generation;public final double dev,val;public final String family;
        Curve(int generation,double dev,double val,String family){this.generation=generation;this.dev=dev;this.val=val;this.family=family;}
    }
    private Ranked champion;
    private final List<Curve> curves=new ArrayList<>();
    public Ranked champion(){return champion;}
    public List<Curve> curves(){return Collections.unmodifiableList(curves);}
    private void finishGeneration(){
        for(int i=0;i<Math.min(4,best.size());i++){Ranked r=best.get(i);if(champion==null||r.valFitness>champion.valFitness||r.valFitness==champion.valFitness&&r.genome.key().compareTo(champion.genome.key())<0)champion=r;}
        if(champion!=null)curves.add(new Curve(generation,best.get(0).fitness,champion.valFitness,champion.genome.family));
    }
    private Genome current;
    private ReplayCursorSU1 cursor;
    public StrategyFoundrySU1(int[][] draws,String datasetHash,long seed,int population,int generations){
        if(draws.length<3400||draws.length>20000||population<8||population>32||generations<1||generations>12)throw new IllegalArgumentException("SU1_FOUNDRY_INPUT");
        this.draws=draws;this.datasetHash=HashingSU1.sha(datasetHash);this.seed=seed;this.population=population;this.generations=generations;
        devStart=Math.max(1500,draws.length-4200);devEnd=draws.length-1000;valStart=devEnd;valEnd=draws.length;
        researchContext=ResearchContextSU1.from(draws,devStart);director.context(researchContext.key);
        for(int[] row:draws)RelationalStateInference.validateDraw(row);
    }
    public void restoreSeen(Collection<String> keys){seen.addAll(keys);}
    public List<Ranked> ranked(){return Collections.unmodifiableList(best);}
    public boolean done(){return phase>=4;}
    private List<String> conditions(){List<String> out=new ArrayList<>();for(int i=0;i<Math.min(8,screenTop.size());i++)out.addAll(AtomWorldScreenerSU1.conditionals(screenTop.get(i).genome.text,seed+i*1009L));return out;}
    private void remember(Ranked r,List<Ranked> target,int cap){target.add(r);target.sort(Comparator.comparingDouble((Ranked x)->x.fitness).reversed().thenComparing(x->x.genome.key()));while(target.size()>cap)target.remove(target.size()-1);}
    private Genome proposal(){
        if(phase==0){List<String> singles=AtomWorldScreenerSU1.singles();if(index<singles.size())return new Genome(singles.get(index),"GRAMMAR","GRAMMAR","screen:atom","",-1,.25,false);phase=1;index=0;}
        if(phase==1){List<String> conds=conditions();if(index<conds.size())return new Genome(conds.get(index),"GRAMMAR","GRAMMAR","screen:condition","",-1,.25,false);phase=2;index=0;}
        if(phase==2){if(index<ResearchDirectorPolicySU1.FAMILIES.length*2){Random r=new Random(seed+index);String family=ResearchDirectorPolicySU1.FAMILIES[index%8];return new Genome(GrammarFamilySU1.seed(family,r),family,family,"seed","",-1,1,false);}phase=3;index=0;}
        if(phase==3){
            if(generation>=generations){phase=4;return null;}
            if(generationQueue.isEmpty())newGeneration();
            if(index>=generationQueue.size()){finishGeneration();generation++;index=0;generationQueue.clear();if(memoryEnabled){macros.learn(best);for(MacroLibrarySU1.Macro m:macros.all())if(m.support>=3)director.add("EMERGENT:"+m.id);}return proposal();}
            return generationQueue.get(index);
        }
        return null;
    }
    private void newGeneration(){
        Random rng=new Random(seed+1000003L*(generation+1));Map<String,Integer> counts=new HashMap<>();
        List<Ranked> parents=new ArrayList<>(best.subList(0,Math.min(12,best.size())));
        for(int i=0;i<population;i++){
            String arm=director.pick(evaluated,counts,population);counts.put(arm,counts.getOrDefault(arm,0)+1);String macro=memoryEnabled?macros.textForArm(arm):null;
            Ranked parent=parents.isEmpty()?null:parents.get(rng.nextInt(parents.size()));String text=macro!=null?macro:parent==null?GrammarFamilySU1.seed(arm,rng):parent.genome.text;
            String op=director.operator(rng),other=parents.isEmpty()?GrammarFamilySU1.atom(rng):parents.get(rng.nextInt(parents.size())).genome.text;
            if(op.equals("fusion")&&parent!=null){
                Ranked complement=null;double bestComplement=Double.NEGATIVE_INFINITY;
                for(Ranked candidate:parents){double score=parent.behavior.complementarity(candidate.behavior);if(score>bestComplement){bestComplement=score;complement=candidate;}}
                if(complement==null){op="fusion_rejected_similarity";}else other=complement.genome.text;
            }
            String child;
            switch(op){case "crossover":child=GrammarFamilySU1.crossover(text,other,rng);break;case "fusion":child=GrammarFamilySU1.fuse(text,other,rng);break;case "structural":child=GrammarFamilySU1.random(rng,3);break;default:child=GrammarFamilySU1.mutate(text,rng);}
            generationQueue.add(new Genome(child,"GRAMMAR",arm,op,parent==null?"":parent.genome.key(),parent==null?-1:parent.fitness,new double[]{.10,.15,.25,1}[rng.nextInt(4)],rng.nextBoolean()));
        }
    }
    /** Returns one durable attempt; a yield preserves the exact current replay cursor. */
    public Attempt next(BudgetTokenSU1 budget){
        while(!done()){
            budget.checkpointOrYield();
            if(current==null){current=proposal();if(current==null)return null;if(seen.contains(current.key())){Genome duplicate=current;current=null;index++;return new Attempt(duplicate,null,"REJECTED","SEMANTIC_DUPLICATE");}
                cursor=new ReplayCursorSU1(draws,current.text,current.coverage,current.margin,devStart,valEnd,devEnd);}
            if(cursor.advance(budget)){
                ProgramContractSU1 compiled=ProgramContractSU1.compile(ProgramTextSU1.parse(current.text));double penalty=Math.min(.1,compiled.nodes*.00005+compiled.operations*1e-9);
                Ranked r=new Ranked(current,FoldStabilityFitnessSU1.folds(cursor.replay,devStart,devEnd)-penalty,FoldStabilityFitnessSU1.fitness(cursor.replay,valStart,valEnd)-penalty,BehaviorSignatureSU1.from(cursor.replay,devStart,devEnd,datasetHash));
                if(!Double.isFinite(r.fitness)||!Double.isFinite(r.valFitness))throw new IllegalArgumentException("SU1_NONFINITE_FITNESS");
                remember(r,best,32);if(phase==0)remember(r,screenTop,8);seen.add(current.key());evaluated++;
                director.record(current.arm,current.operator,current.parentFitness,r.fitness,compiled.operationUnits,false);Attempt result=new Attempt(current,r,"EVALUATED","CONTEXT="+researchContext.key+"|COST_UNITS="+compiled.operationUnits+"|DEV_PROBE="+r.behavior.distributionHash+"|ACTION_PROBE="+r.behavior.actionHash+"|COMPLEXITY_COST="+Double.toHexString(penalty));current=null;cursor.close();cursor=null;index++;return result;
            }
        }
        return null;
    }
    public String checkpoint(){
        try{ByteArrayOutputStream raw=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(raw))){
            out.writeInt(0x53553146);out.writeInt(4);out.writeUTF(datasetHash);out.writeLong(seed);out.writeInt(population);out.writeInt(generations);out.writeInt(phase);out.writeInt(index);out.writeInt(generation);out.writeInt(evaluated);out.writeBoolean(memoryEnabled);
            director.write(out);macros.write(out);out.writeInt(best.size());for(Ranked r:best)r.write(out);out.writeInt(screenTop.size());for(Ranked r:screenTop)r.write(out);out.writeInt(generationQueue.size());for(Genome g:generationQueue)g.write(out);
            out.writeBoolean(champion!=null);if(champion!=null)champion.write(out);out.writeInt(curves.size());for(Curve c:curves){out.writeInt(c.generation);out.writeDouble(c.dev);out.writeDouble(c.val);out.writeUTF(c.family);}
            out.writeBoolean(current!=null);if(current!=null){current.write(out);cursor.write(out);}
        }String wire=Base64.getEncoder().encodeToString(raw.toByteArray());if(wire.length()>60000)throw new IllegalStateException("SU1_CHECKPOINT_QUOTA");return wire;
        }catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public String memory(){
        try{ByteArrayOutputStream raw=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(raw))){out.writeInt(0x5355314d);director.write(out);macros.write(out);}String wire=Base64.getEncoder().encodeToString(raw.toByteArray());if(wire.length()>60000)throw new IllegalStateException("SU1_MEMORY_QUOTA");return wire;}
        catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public void inheritMemory(String wire){
        if(wire==null||wire.length()>60000)throw new IllegalArgumentException("SU1_MEMORY_SIZE");
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(wire))))){if(in.readInt()!=0x5355314d)throw new IOException("SU1_MEMORY_PROTOCOL");director=ResearchDirectorPolicySU1.read(in);director.context(researchContext.key);macros=MacroLibrarySU1.read(in);if(in.read()!=-1)throw new IOException("SU1_MEMORY_TAIL");}
        catch(IOException invalid){throw new IllegalArgumentException("SU1_MEMORY_CORRUPT",invalid);}
    }
    public static StrategyFoundrySU1 restore(int[][] draws,String expectedDataset,String wire){
        if(wire==null||wire.length()>60000)throw new IllegalArgumentException("SU1_CHECKPOINT_SIZE");
        try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(wire))))){
            if(in.readInt()!=0x53553146||in.readInt()!=4||!expectedDataset.equals(in.readUTF()))throw new IOException("SU1_CHECKPOINT_IDENTITY");
            StrategyFoundrySU1 f=new StrategyFoundrySU1(draws,expectedDataset,in.readLong(),in.readInt(),in.readInt());f.phase=in.readInt();f.index=in.readInt();f.generation=in.readInt();f.evaluated=in.readInt();
            f.memoryEnabled=in.readBoolean();
            if(f.phase<0||f.phase>4||f.index<0||f.index>4000||f.generation<0||f.generation>f.generations||f.evaluated<0)throw new IOException("SU1_CHECKPOINT_CURSOR");
            f.director=ResearchDirectorPolicySU1.read(in);f.macros=MacroLibrarySU1.read(in);int n=bounded(in,32);for(int i=0;i<n;i++)f.best.add(Ranked.read(in));n=bounded(in,8);for(int i=0;i<n;i++)f.screenTop.add(Ranked.read(in));n=bounded(in,32);for(int i=0;i<n;i++)f.generationQueue.add(Genome.read(in));
            if(in.readBoolean())f.champion=Ranked.read(in);n=bounded(in,12);double prior=-Double.MAX_VALUE;for(int i=0;i<n;i++){Curve c=new Curve(in.readInt(),in.readDouble(),in.readDouble(),in.readUTF());if(!Double.isFinite(c.dev)||!Double.isFinite(c.val)||c.val<prior)throw new IOException("SU1_CHAMPION_CURVE");f.curves.add(c);prior=c.val;}
            if(in.readBoolean()){f.current=Genome.read(in);f.cursor=ReplayCursorSU1.read(in,draws);}if(in.read()!=-1)throw new IOException("SU1_CHECKPOINT_TAIL");return f;
        }catch(IOException|IllegalArgumentException invalid){throw new IllegalArgumentException("SU1_CHECKPOINT_CORRUPT",invalid);}
    }
    private static int bounded(DataInput in,int max)throws IOException{int n=in.readInt();if(n<0||n>max)throw new IOException("SU1_CHECKPOINT_COUNT");return n;}
    @Override public void close(){if(cursor!=null)cursor.close();}
    public String knowledge(Attempt attempt){
        Map<String,Object> m=new TreeMap<>();ProgramContractSU1 compiled=ProgramContractSU1.compile(ProgramTextSU1.parse(attempt.genome.text));
        m.put("protocol","SU1_RESEARCH_EXPERIENCE_V1");m.put("context",researchContext.key);m.put("contextSource",researchContext.sourceHash);m.put("dataset",datasetHash);
        m.put("hypothesis",attempt.genome.operator+":"+attempt.genome.arm);m.put("program",attempt.genome.text);m.put("parent",attempt.genome.parent);m.put("costUnits",compiled.operationUnits);
        m.put("result",attempt.result);m.put("detail",attempt.detail);m.put("fitnessHex",attempt.ranked==null?"UNKNOWN":Double.toHexString(attempt.ranked.fitness));
        m.put("evidenceRole","DEVELOPMENT_ONLY");m.put("falsification","NO_MATCHED_VALIDATION_GAIN_OR_NEW_FUTURE_PAIRED_FAILURE");
        return com.ethk3.novaonepro.r952.StrictCanonicalJson.writeContext(m);
    }
}
