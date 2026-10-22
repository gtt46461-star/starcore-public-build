package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Independent reader/issuer. History rankings, missing probabilities and loose PASS bits grant nothing. */
public final class CertificateSU1 {
    public static final String POLICY=HashingSU1.text("SU1_CERT_V1|FIXED_PAIRED_CALENDAR|NULL_AND_PARENT|CAL_120_1_120|BRIER_ECE|4_BLOCKS|NULL_CONTROL|RESOURCE_7|EXPIRY_24H");
    public static final String PREFIX="SU1-F-";
    private CertificateSU1() {}
    public static final class Authority {
        public final String strategy,program,artifact,selector,calibration,certificate,evidence,source,family,quality;
        public final long issued,expires;
        Authority(Cursor c){strategy=c.getString(0);program=c.getString(1);artifact=c.getString(2);selector=c.getString(3);calibration=c.getString(4);certificate=c.getString(5);evidence=c.getString(6);source=c.getString(7);family=c.getString(8);quality=c.getString(9);issued=c.getLong(10);expires=c.getLong(11);}
        public String candidate(){return PREFIX+strategy;}
    }
    private static boolean metrics(String json){
        Map<String,Object> m=StrictCanonicalJson.readObject(json);Object b=m.get("brier"),e=m.get("ece"),base=m.get("baselineBrier");if(!(b instanceof String)||!(e instanceof String)||!(base instanceof String)||"UNKNOWN".equals(b)||"UNKNOWN".equals(e)||"UNKNOWN".equals(base))return false;
        double bv=Double.valueOf((String)b),ev=Double.valueOf((String)e),nv=Double.valueOf((String)base);if(!Double.isFinite(bv)||!Double.isFinite(ev)||bv>.24||bv>=nv||ev>.04||((Number)m.get("probabilityN")).intValue()<240||((Number)m.get("maxLossStreak")).intValue()>30)return false;
        List<?> blocks=(List<?>)m.get("blocks");if(blocks.size()!=4)return false;for(Object block:blocks){List<?> a=(List<?>)block;int n=((Number)a.get(0)).intValue(),k=((Number)a.get(1)).intValue();if(n<30||k/(double)n<=.30)return false;}return true;
    }
    public static Authority read(SQLiteDatabase db,String certificate,long now,boolean requireMainline){
        if(requireMainline&&!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_runtime_control WHERE singleton=1 AND mode='MAINLINE_ADAPTER' AND contract_hash=?",MainlineAdapterSU1.CONTRACT))return null;
        String baseline,kernel,descriptor,primaryMetrics;
        Authority a;
        try(Cursor c=db.rawQuery("SELECT v.strategy_id,v.program_hash,v.artifact_hash,v.selector_hash,v.calibration_hash,c.certificate_id,c.evidence_hash,c.source_chain_hash,r.family_root_hash,r.quality_hash,c.issued_at,c.expires_at,c.baseline_evidence_hash,r.kernel_hash,r.descriptor_hash,e.metrics_json FROM su1_certificate c JOIN su1_certificate_runtime r ON r.certificate_id=c.certificate_id JOIN su1_strategy_version v ON v.strategy_id=c.strategy_id JOIN su1_evidence_look e ON e.evidence_hash=c.evidence_hash WHERE c.certificate_id=? AND c.policy_hash=? AND v.strategy_role='CANDIDATE' AND v.calibration_state='FROZEN' AND NOT EXISTS(SELECT 1 FROM su1_certificate_revocation x WHERE x.certificate_id=c.certificate_id)",new String[]{certificate,POLICY})){if(!c.moveToFirst())return null;a=new Authority(c);baseline=c.getString(12);kernel=c.getString(13);descriptor=c.getString(14);primaryMetrics=c.getString(15);}
        if(now<a.issued||now>=a.expires||!TrainingCodeIdentityR964.SHA256.equals(kernel)||!descriptor(a.strategy,a.evidence,baseline,a.quality,a.family,a.issued,a.expires).equals(descriptor))return null;
        if(!EvidenceQualitySU1.verifiedPass(db,a.evidence)||!EvidenceQualitySU1.verifiedPass(db,baseline)||!metrics(primaryMetrics))return null;
        CalibrationSU1 cal=CalibrationSU1.decode(CampaignStateSU1.localBlob(db,a.calibration));if(!cal.valid())return null;
        Map<String,Object> quality=StrictCanonicalJson.readObject(CampaignStateSU1.localBlob(db,a.quality));double latency=Double.valueOf((String)quality.get("latencyP95Ms"));if(!Double.isFinite(latency)||latency>100||((Number)quality.get("runs")).intValue()!=7||!a.strategy.equals(quality.get("strategy"))||!kernel.equals(quality.get("kernel")))return null;
        if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_eval_cohort c JOIN su1_evidence_look e ON e.cohort_id=c.cohort_id WHERE c.family_root_hash=? AND c.claim_kind='NULL_CONTROL' AND e.result_state='PASS'",a.family))return null;
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_eval_cohort c JOIN su1_evidence_look e ON e.cohort_id=c.cohort_id WHERE c.family_root_hash=? AND c.claim_kind='NULL_CONTROL' AND e.result_state='FAIL' AND e.coverage>=0.95",a.family))return null;
        if(!StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_runtime_program WHERE strategy_id=? AND kernel_hash=? AND coverage>=0.999",a.strategy,kernel))return null;
        return a;
    }
    private static String descriptor(String id,String evidence,String baseline,String quality,String family,long at,long expiry){return HashingSU1.fields(POLICY,id,evidence,baseline,quality,family,TrainingCodeIdentityR964.SHA256,String.valueOf(at),String.valueOf(expiry));}
    private static String resource(DbStore store,String strategy){
        Models.Draw latest=store.getLatestDraw();if(latest==null)return null;String target=TargetIssueResolver.next(latest.issue).targetIssue;
        EventFrameSU1 frame=EventFrameBuilderSU1.capture(store,target);String text;try(Cursor c=store.readable().rawQuery("SELECT canonical_text FROM su1_runtime_program WHERE strategy_id=?",new String[]{strategy})){if(!c.moveToFirst())return null;text=c.getString(0);}
        int[][] history=frame.history();double[] samples=new double[7];long maxHeap=0;ProgramTreeSU1.Node program=ProgramTreeSU1.parse(text);
        for(int i=0;i<7;i++){BudgetTokenSU1 budget=new BudgetTokenSU1(250,3000000,SettlementExecutionGateR819::isActive);long before=android.os.SystemClock.elapsedRealtimeNanos();try(AtomLiftSU1 atoms=new AtomLiftSU1(history)){program.score(new ProgramTreeSU1.Context(history,history.length,atoms,new ScoreHistoryCacheSU1(2048),budget));}samples[i]=(android.os.SystemClock.elapsedRealtimeNanos()-before)/1000000d;Runtime r=Runtime.getRuntime();maxHeap=Math.max(maxHeap,r.totalMemory()-r.freeMemory());}
        Arrays.sort(samples);Map<String,Object> m=new TreeMap<>();m.put("protocol","SU1_DEVICE_RESOURCE_PROBE_7_V1");m.put("strategy",strategy);m.put("kernel",TrainingCodeIdentityR964.SHA256);m.put("source",frame.sourceVersionHash);m.put("latencyP95Ms",Double.toHexString(samples[6]));m.put("processHeapBytes",maxHeap);m.put("runs",7);return StrictCanonicalJson.write(m);
    }
    public static void maintain(DbStore store){
        List<String[]> candidates=new ArrayList<>();
        try(Cursor c=store.readable().rawQuery("SELECT c.strategy_id,e.evidence_hash,c.family_root_hash,e.source_chain_hash,e.metrics_json FROM su1_eval_cohort c JOIN su1_evidence_look e ON e.cohort_id=c.cohort_id JOIN su1_strategy_version v ON v.strategy_id=c.strategy_id JOIN su1_runtime_program k ON k.strategy_id=v.strategy_id WHERE c.claim_kind='VS_NULL_BASELINE' AND c.evidence_role='CONFIRM' AND e.result_state='PASS' AND v.calibration_state='FROZEN' AND k.kernel_hash=? AND NOT EXISTS(SELECT 1 FROM su1_certificate x WHERE x.baseline_evidence_hash=e.evidence_hash) ORDER BY e.recorded_at LIMIT 1",new String[]{TrainingCodeIdentityR964.SHA256})){while(c.moveToNext()){String[] row=new String[5];for(int i=0;i<5;i++)row[i]=c.getString(i);candidates.add(row);}}
        for(String[] r:candidates){
            if(!EvidenceQualitySU1.verifiedPass(store.readable(),r[1])||!metrics(r[4]))continue;
            if(!StrategyRepositorySU1.exists(store.readable(),"SELECT 1 FROM su1_eval_cohort c JOIN su1_evidence_look e ON e.cohort_id=c.cohort_id WHERE c.family_root_hash=? AND c.claim_kind='NULL_CONTROL' AND e.result_state='FAIL' AND e.coverage>=0.95",r[2]))continue;
            String primary=r[1],source=r[3],parent=null;try(Cursor c=store.readable().rawQuery("SELECT parent_id FROM su1_runtime_program WHERE strategy_id=?",new String[]{r[0]})){if(c.moveToFirst())parent=c.getString(0);}
            if(parent!=null){try(Cursor c=store.readable().rawQuery("SELECT e.evidence_hash,e.source_chain_hash,e.metrics_json FROM su1_eval_cohort c JOIN su1_evidence_look e ON e.cohort_id=c.cohort_id WHERE c.strategy_id=? AND c.parent_strategy_id=? AND c.family_root_hash=? AND c.claim_kind='PARENT_CHILD' AND e.result_state='PASS' ORDER BY e.recorded_at DESC LIMIT 1",new String[]{r[0],parent,r[2]})){if(!c.moveToFirst()||!EvidenceQualitySU1.verifiedPass(store.readable(),c.getString(0))||!metrics(c.getString(2)))continue;primary=c.getString(0);source=c.getString(1);}}
            String quality=resource(store,r[0]);if(quality==null)continue;Map<String,Object> measured=StrictCanonicalJson.readObject(quality);if(Double.valueOf((String)measured.get("latencyP95Ms"))>100)continue;
            long now=System.currentTimeMillis(),expiry=now+86400000;String qhash=HashingSU1.text(quality),desc=descriptor(r[0],primary,r[1],qhash,r[2],now,expiry),cert=HashingSU1.fields("SU1_CERTIFICATE_V1",desc);final String proof=primary,chain=source;
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("SU1_CERTIFICATE_ISSUE",db->{if(StrategyRepositorySU1.exists(db,"SELECT 1 FROM su1_certificate WHERE baseline_evidence_hash=?",r[1]))return 0;StrategyRepositorySU1.blob(db,quality,"SU1_DEVICE_RESOURCE_PROBE_V1",now);StrategyRepositorySU1.row(db,"su1_certificate","certificate_id",cert,"strategy_id",r[0],"evidence_hash",proof,"baseline_evidence_hash",r[1],"policy_hash",POLICY,"source_chain_hash",chain,"issued_at",now,"expires_at",expiry);StrategyRepositorySU1.row(db,"su1_certificate_runtime","certificate_id",cert,"kernel_hash",TrainingCodeIdentityR964.SHA256,"quality_hash",qhash,"family_root_hash",r[2],"descriptor_hash",desc);if(read(db,cert,now,false)==null)throw new IllegalStateException("SU1_CERTIFICATE_SELF_VERIFICATION");return 0;});
        }
    }
}
