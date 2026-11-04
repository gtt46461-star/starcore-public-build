package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Search -> durable champion selection -> single held-out replay. Resume boundaries cover both stages. */
public final class CampaignAuditSessionSU1 implements AutoCloseable {
    public final CampaignControlsSU1.Variant variant;
    public final String dataset;
    public final long seed;
    public final int trainEnd;
    private final int[][] full;
    public final StrategyFoundrySU1 foundry;
    public String phase="SEARCH";
    private StrategyFoundrySU1.Genome selected;
    private ReplayCursorSU1 sealed;
    private String report;
    public StrategyFoundrySU1.Attempt attempt;
    public long costUnits,attempts,elapsedMicros;

    public CampaignAuditSessionSU1(int[][] raw,String source,CampaignControlsSU1.Variant variant,long seed){
        this.variant=variant;this.seed=seed;full=CampaignControlsSU1.transform(raw,variant,seed);trainEnd=full.length-1440;
        dataset=HashingSU1.fields("SU1_MATCHED_HISTORICAL_AUDIT_V1",HashingSU1.sha(source),variant.name(),String.valueOf(seed));
        foundry=new StrategyFoundrySU1(Arrays.copyOf(full,trainEnd),dataset,seed,24,3);
        foundry.startRefresh(Collections.emptyList());
        if(variant==CampaignControlsSU1.Variant.NO_DIRECTOR)foundry.director.enabled=false;
        if(variant==CampaignControlsSU1.Variant.NO_MEMORY)foundry.memoryEnabled=false;
    }
    private CampaignAuditSessionSU1(int[][] full,String dataset,CampaignControlsSU1.Variant variant,long seed,StrategyFoundrySU1 foundry){this.full=full;this.dataset=dataset;this.variant=variant;this.seed=seed;this.foundry=foundry;trainEnd=full.length-1440;}
    /** The caller must commit each returned selection state before calling advance again. */
    public void advance(BudgetTokenSU1 budget){
        long started=System.nanoTime();try{advanceInternal(budget);}finally{elapsedMicros+=Math.max(0,(System.nanoTime()-started)/1000);}
        if(phase.equals("DONE")&&selected!=null)report=buildReport();
    }
    private void advanceInternal(BudgetTokenSU1 budget){
        attempt=null;
        if(phase.equals("SEARCH")){
            attempt=foundry.next(budget);
            if(attempt!=null){attempts++;costUnits+=ProgramContractSU1.compile(ProgramTextSU1.parse(attempt.genome.text)).operationUnits;}
            if(foundry.done()){
                StrategyFoundrySU1.Ranked champion=foundry.champion();
                if(champion==null){phase="DONE";report=StrictCanonicalJson.writeContext(Collections.singletonMap("status","NO_CHAMPION"));return;}
                selected=champion.genome;sealed=new ReplayCursorSU1(full,selected.text,selected.coverage,selected.margin,trainEnd,full.length);phase="SEALED";
            }
            return;
        }
        if(phase.equals("SEALED")&&sealed.advance(budget)){phase="DONE";report=buildReport();}
    }
    public String selectedHash(){return selected==null?null:selected.key();}
    public String selectedText(){return selected==null?null:selected.text;}
    public String result(){if(!phase.equals("DONE")||report==null)throw new IllegalStateException("SU1_AUDIT_NOT_COMPLETE");return report;}
    private Map<String,Object> counts(int n,int k,int opportunities){Map<String,Object> m=new TreeMap<>();m.put("opportunities",opportunities);m.put("actions",n);m.put("hits",k);m.put("coverageHex",Double.toHexString(n/(double)Math.max(1,opportunities)));m.put("hitRateHex",n==0?"UNKNOWN":Double.toHexString(k/(double)n));return m;}
    private String buildReport(){
        StrategyFoundrySU1.Ranked champion=foundry.champion();Map<String,Object> m=new TreeMap<>();
        m.put("protocol","SU1_MATCHED_HISTORICAL_AUDIT_V1");m.put("evidenceRole","HISTORICAL_RESEARCH_ONLY_NO_AUTHORITY");m.put("variant",variant.name());m.put("dataset",dataset);m.put("seed",seed);m.put("population",24);m.put("generations",3);m.put("attempts",attempts);m.put("estimatedCostUnits",costUnits);m.put("computeElapsedMicros",elapsedMicros);
        m.put("champion",selected.text);m.put("championHash",selected.key());m.put("dev",counts(champion.devActions,champion.devHits,foundry.devEnd-foundry.devStart));m.put("val",counts(champion.valActions,champion.valHits,foundry.valEnd-foundry.valStart));
        m.put("sealed",counts(sealed.replay.actions(trainEnd,full.length),sealed.replay.hits(trainEnd,full.length),full.length-trainEnd));m.put("sealedSelectedBeforeOpen",true);m.put("memoryInherited",false);m.put("searchDataThroughExclusive",trainEnd);m.put("sealedFirst",trainEnd);m.put("sealedLastExclusive",full.length);
        m.put("sameAllocationContract","IDENTICAL_REFRESH_STAGES_POPULATION_GENERATIONS_AND_SLICE_CAP;ACTUAL_COST_REPORTED");
        m.put("memoryAblation","MACROS_AND_CROSS_GENERATION_DIRECTOR_EXPERIENCE_OFF;UCB_ALGORITHM_RETAINED");
        m.put("memoryEnabled",foundry.memoryEnabled);m.put("directorEnabled",foundry.director.enabled);
        return StrictCanonicalJson.writeContext(m);
    }
    private static String cursorWire(ReplayCursorSU1 cursor){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(new GZIPOutputStream(bytes))){cursor.write(out);}return Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException impossible){throw new IllegalStateException(impossible);}
    }
    public String checkpoint(){
        Map<String,Object> m=new TreeMap<>();m.put("protocol","SU1_AUDIT_CHECKPOINT_V1");m.put("dataset",dataset);m.put("variant",variant.name());m.put("seed",seed);m.put("phase",phase);m.put("session",foundry.checkpoint());m.put("attempts",attempts);m.put("costUnits",costUnits);m.put("elapsedMicros",elapsedMicros);
        if(selected!=null){m.put("selected",selected.text);m.put("coverage",Double.toHexString(selected.coverage));m.put("margin",selected.margin);m.put("selectedKey",selected.key());m.put("sealedCursor",cursorWire(sealed));}
        if(report!=null)m.put("report",report);return StrictCanonicalJson.writeContext(m);
    }
    public static CampaignAuditSessionSU1 restore(int[][] raw,String source,String wire){
        Map<String,Object> m=StrictCanonicalJson.readContextObject(wire);if(!"SU1_AUDIT_CHECKPOINT_V1".equals(m.get("protocol")))throw new IllegalArgumentException("SU1_AUDIT_PROTOCOL");
        CampaignControlsSU1.Variant variant=CampaignControlsSU1.Variant.valueOf((String)m.get("variant"));long seed=((Number)m.get("seed")).longValue();
        int[][] full=CampaignControlsSU1.transform(raw,variant,seed);String dataset=HashingSU1.fields("SU1_MATCHED_HISTORICAL_AUDIT_V1",HashingSU1.sha(source),variant.name(),String.valueOf(seed));if(!dataset.equals(m.get("dataset")))throw new IllegalArgumentException("SU1_AUDIT_DATASET");
        StrategyFoundrySU1 foundry=StrategyFoundrySU1.restore(Arrays.copyOf(full,full.length-1440),dataset,(String)m.get("session"));CampaignAuditSessionSU1 out=new CampaignAuditSessionSU1(full,dataset,variant,seed,foundry);out.phase=(String)m.get("phase");out.attempts=((Number)m.get("attempts")).longValue();out.costUnits=((Number)m.get("costUnits")).longValue();out.elapsedMicros=m.containsKey("elapsedMicros")?((Number)m.get("elapsedMicros")).longValue():0;
        if(!Arrays.asList("SEARCH","SEALED","DONE").contains(out.phase))throw new IllegalArgumentException("SU1_AUDIT_PHASE");
        if(m.containsKey("selected")){
            StrategyFoundrySU1.Ranked champion=foundry.champion();if(champion==null||!champion.genome.key().equals(m.get("selectedKey"))||!champion.genome.text.equals(m.get("selected"))||Double.compare(champion.genome.coverage,Double.valueOf((String)m.get("coverage")))!=0||champion.genome.margin!=(Boolean)m.get("margin"))throw new IllegalArgumentException("SU1_AUDIT_CHAMPION_CHANGED");
            out.selected=champion.genome;String cursor=(String)m.get("sealedCursor");if(cursor.length()>131072)throw new IllegalArgumentException("SU1_AUDIT_CURSOR_BYTES");
            try(DataInputStream in=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode(cursor))))){out.sealed=ReplayCursorSU1.read(in,full);if(in.read()!=-1||!out.sealed.text.equals(out.selected.text)||out.sealed.start!=out.trainEnd||out.sealed.end!=full.length)throw new IOException("SU1_AUDIT_SEALED_BINDING");}catch(IOException invalid){throw new IllegalArgumentException("SU1_AUDIT_CURSOR",invalid);}
        }
        out.report=(String)m.get("report");return out;
    }
    @Override public void close(){foundry.close();if(sealed!=null)sealed.close();}
}
