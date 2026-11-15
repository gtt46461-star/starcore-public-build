package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Real default RuntimeProbe/512m JVM. OS-low-memory is an explicit injected observation, never device evidence. */
public final class FactoryPressureR31Host {
    private static void require(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    private static final class Recorder extends R27BirthGateTest.Recorder {
        final List<Object> scoreRows=new ArrayList<>();
        @Override public void score(String hash,String dataset,ReplayScoreR27 score){super.score(hash,dataset,score);scoreRows.add(Arrays.asList(hash,dataset,score.json()));}
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        boolean inject="resume".equals(args[0]);MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();memory.sample();Map<String,Object> initial=memory.snapshot();
        require(((Number)initial.get("maxHeapBytes")).longValue()==512L*1024*1024,"must use original default test maximum 512m");
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(5000,3185,new int[]{700,1900});FeatureCacheR27 cache=new FeatureCacheR27(data);Recorder sink=new Recorder();EvolutionLoopR27.Config config=new EvolutionLoopR27.Config();config.births=128;config.seed=3185;config.nullControl=false;
        EvolutionLoopR27 loop=new EvolutionLoopR27(config,cache,sink,()->0L);loop.step();loop.step();String saved=loop.snapshot();Map<String,Object> frozen=(Map<String,Object>)JsonR27.parse(saved);require(((List<?>)frozen.get("pending")).size()==128,"full 128-candidate supply");
        Map<String,Object> pressure=null,recovered=null;boolean terminated=false,waitPreserved=false;String waitCategory="NOT_INJECTED";
        if(inject){
            memory.observeSystemLowMemory(true);memory.sample();pressure=memory.snapshot();final EvolutionLoopR27 active=loop;
            CooperativeContinuationR26<Void> work=new CooperativeContinuationR26<>("r31-real-factory-pressure",()->{active.step();return null;},()->false,null,System::nanoTime,"r31-real-factory-pressure");
            try{CooperativeContinuationR26.Slice<Void> result=work.slice(200);require(result.done,"pressure refusal must finish bounded worker");require(result.error instanceof MemoryBudgetR31.CapacityWait,"actual memory gate must refuse fresh optional allocation");FailureDispositionR31.Decision decision=FailureDispositionR31.classify(result.error);require(decision.disposition==FailureDispositionR31.Disposition.DEPENDENCY&&!decision.consumesFaultBudget(),"resource wait must not become fault/quarantine");waitCategory=decision.category;require(work.awaitTermination(2000),"real worker must exit before replacement");terminated=work.isTerminated();require(active.step==3,"uncommitted GATE cursor must not advance");waitPreserved=saved.equals(active.snapshot());require(waitPreserved,"resource wait must not permanently reject a candidate or change durable RNG/input");}
            finally{work.close();require(work.awaitTermination(2000),"cleanup body exit");memory.observeSystemLowMemory(false);}
            memory.sample();recovered=memory.snapshot();cache.close();cache=new FeatureCacheR27(data);loop=EvolutionLoopR27.restore(saved,config,cache,sink,()->0L);
        }
        long peak=((Number)memory.sample().get("heapUsedBytes")).longValue();while(loop.generation==0){loop.step();peak=Math.max(peak,((Number)memory.sample().get("heapUsedBytes")).longValue());}
        require(loop.generation==1&&loop.step==1,"all original nine steps must complete");require(loop.counter("attempts")==128,"no candidate count reduction");
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope","HOST_REAL_DEFAULT_PROCESS_BUDGET_FULL_FACTORY_NOT_DEVICE_OR_QUALITY");out.put("mode",args[0]);out.put("inputRows",data.size());out.put("candidateCount",128);out.put("completedGeneration",loop.generation);out.put("nextStep",loop.step);out.put("runtimeHeapMaximum",Runtime.getRuntime().maxMemory());out.put("observedStepBoundaryPeakHeapBytes",peak);out.put("initialMemory",initial);out.put("injectedOsLowMemory",inject);out.put("pressureMemory",pressure);out.put("recoveredMemory",recovered);out.put("oldWorkerActuallyTerminated",terminated);out.put("cursorAndCandidatesPreserved",waitPreserved);out.put("waitCategory",waitCategory);out.put("events",sink.events);out.put("scoreRows",sink.scoreRows);out.put("champion",loop.champion==null?null:loop.champion.fullJson());out.put("rngState",loop.rng.state);out.put("scratch",cache.scratchMemorySnapshotR31());out.put("forcedGc",false);out.put("naturalQuality","NOT_EVALUATED_SYNTHETIC_FAULT_INJECTION");
        loop.close();require(loop.resourcesClosedR31(),"real loop-owned context must close after all bodies exit");cache.close();memory.sample();out.put("finalMemory",memory.snapshot());System.out.println(JsonR27.write(out));
    }
}
