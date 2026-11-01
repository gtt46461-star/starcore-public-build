package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Unit/owner registry for the original 57 entrances. Registration is not evidence of execution. */
public final class ResearchTaskRegistryR30R1 {
    private ResearchTaskRegistryR30R1(){}
    public static final String VERSION="R30_REPAIR_TYPED_PROGRESS_1";
    public static final class Entry {
        public final String id,owner,input; public final TaskProgressR30R1.Unit unit; public final boolean training;
        Entry(String id,String owner,TaskProgressR30R1.Unit unit,boolean training,String input){this.id=id;this.owner=owner;this.unit=unit;this.training=training;this.input=input;}
    }
    public static final Map<String,Entry> ENTRIES;
    static {
        Map<String,Entry> all=new LinkedHashMap<>();
        add(all,"schema schema-base",TaskProgressR30R1.Unit.PHASE,false,"journaled schema units");
        add(all,"kernel shadow-recover evidence-recover evidence-quality sealed-planner calibration su1-certificate su2-mainline su2-certificate qualification aggregate-evidence feedback seed-incumbent incumbent-forecast archive diagnosis daily-close",TaskProgressR30R1.Unit.ISSUE,false,"canonical calendar inputs; per-owner artifact required");
        add(all,"legacy-generation foundry-su2 foundry-su1 ablation campaign-audit",TaskProgressR30R1.Unit.PHASE,true,"campaign phase; calendar source recorded separately");
        add(all,"legacy-replay live-model-training",TaskProgressR30R1.Unit.ISSUE,true,"committed canonical revealed training inputs");
        add(all,"artifact-retention warm-candidates publish-models live-forecast-preparation",TaskProgressR30R1.Unit.POLICY,false,"artifact/policy queue");
        add(all,"retention",TaskProgressR30R1.Unit.PHASE,false,"durable 27-phase maintenance checkpoint");
        for(int i=1;i<=16;i++)add(all,String.format(Locale.ROOT,"expert-E%02d",i),TaskProgressR30R1.Unit.POSITION_SAMPLE,true,"expert checkpoint samples; source calendar is independent");
        add(all,"r27-replay-dataset",TaskProgressR30R1.Unit.ISSUE,false,"immutable dataset refresh threshold; not a learner");
        add(all,"r27-evolution",TaskProgressR30R1.Unit.PHASE,true,"generation*10+phase; never a calendar ordinal");
        add(all,"r27-live",TaskProgressR30R1.Unit.ISSUE,true,"revealed live labels and frozen predictions");
        add(all,"r27-atoms r27-atomnet",TaskProgressR30R1.Unit.POSITION_SAMPLE,true,"materialized/gradient/published samples separated by learner owner");
        add(all,"r27-abi r27-calibration r27-legacy-export",TaskProgressR30R1.Unit.POLICY,false,"policy validation queue");
        add(all,"r27-chart-cache r27-health",TaskProgressR30R1.Unit.TIME_TICK,false,"committed cache tick; never training freshness");
        if(all.size()!=57)throw new ExceptionInInitializerError("R30_ORIGINAL_TASK_REGISTRY:"+all.size());
        ENTRIES=Collections.unmodifiableMap(all);
    }
    private static void add(Map<String,Entry> all,String ids,TaskProgressR30R1.Unit unit,boolean training,String input){for(String id:ids.split(" ")){if(all.put(id,new Entry(id,id.startsWith("r27-")?"RuntimeR27":"RuntimeCoordinatorR26",unit,training,input))!=null)throw new IllegalStateException(id);}}
    public static Entry require(String id){Entry entry=ENTRIES.get(id);if(entry==null)throw new IllegalArgumentException("UNREGISTERED_TYPED_TASK:"+id);return entry;}
}
