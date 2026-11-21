package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.nio.charset.StandardCharsets;

/** Read-only original-source projections for UPGRADE_DIAGNOSIS. Runs on the same
 * frozen report database, never in the diagnosis writer and never creates samples.
 * A projection has its own observed bounds; old snapshot aggregates are retained. */
public final class DiagnosisEvidenceR335 {
    private DiagnosisEvidenceR335() {}
    interface Read { Map<String,Object> read() throws Exception; }
    static Map<String,Object> checked(String source,Read read){try{Map<String,Object> out=read.read();out.put("source",source);out.put("qualityEffective",null);return out;}catch(Exception bad){Map<String,Object> out=new LinkedHashMap<>();out.put("source",source);out.put("status","UNKNOWN");out.put("records",null);out.put("errorType",bad.getClass().getName());out.put("error",String.valueOf(bad.getMessage()));out.put("qualityEffective",null);return out;}}
    static Map<String,Object> rows(List<?> rows,long total){Map<String,Object> r=new LinkedHashMap<>();r.put("status",total==0?"INSUFFICIENT":rows.size()<total?"PARTIAL":"RECORDED_UNVALIDATED");r.put("records",total);r.put("rows",rows);r.put("displayedRows",rows.size());r.put("omittedRows",Math.max(0,total-rows.size()));return r;}
    static Map<String,Object> map(Object value){if(!(value instanceof Map))throw new IllegalArgumentException("R335_SOURCE_OBJECT_REQUIRED");@SuppressWarnings("unchecked")Map<String,Object> out=(Map<String,Object>)value;return out;}
    static List<?> list(Object value){if(!(value instanceof List))throw new IllegalArgumentException("R335_SOURCE_LIST_REQUIRED");return (List<?>)value;}
    public static Map<String,Object> capture(SQLiteDatabase db,long diagnosticSequence,UpgradeDiagnosisSU2.Ledger ledger){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema","R335_SU2_ORIGINAL_SOURCE_EVIDENCE_V1");out.put("diagnosticSequence",diagnosticSequence);
        out.put("scope","SAME_FROZEN_REPORT_DATABASE; EACH_SOURCE_RETAINS_ITS_OWN_CHECKPOINT_AND_OBSERVED_BOUNDS; NOT_NATURAL_QUALIFICATION");
        out.put("GENERATION",checked("su2_lineage + su2_model_genome + su2_live_decision",()->paired(db,diagnosticSequence)));
        out.put("OPERATORS",checked("su2_evolution_state.state_blob / ModelEvolutionSU2.State.opStats",()->operators(db)));
        out.put("REPAIR",checked("su2_evolution_state.state_blob / repairNotes; su2_lineage",()->repairs(db)));
        Map<String,Object> matrices=checked("su2_matrix_snapshot.snapshot_json + stats_json",()->matrices(db));out.put("MATRICES",matrices);
        out.put("ATOMS",matrixProjection(matrices,"reasonGrades","原SU2前推原因分级；不同于R27 atom_score命名空间"));
        out.put("TEACHERS",matrixProjection(matrices,"teachers","原检查点教师状态关联；非同机会因果消融或自然资格"));
        out.put("STORAGE",checked("su2_storage_budget",()->storage(db)));
        out.put("REFIT",checked("su2_diag_state.P5 + su2_live_state.PROBE",()->probe(db,ledger)));
        out.put("LEDGER",checked("su2_diag_state.ledger_text + su2_lineage receipt + actual child/league/shadow",()->{
            Map<String,Object> r=DiagnosisShadowReceiptR335.readbackLedger(db,ledger.text());r.put("records",ledger.entries.size());r.put("status",Boolean.TRUE.equals(r.get("ack"))?"RECEIPT_VERIFIED_NOT_QUALITY":"RECORDED_OR_PENDING");return r;}));
        out.put("missingFactsRemainUnknown",true);return out;
    }
    static Map<String,Object> probe(SQLiteDatabase db,UpgradeDiagnosisSU2.Ledger ledger){
        if(ledger.probeSourceR335==null||ledger.probeSourceR335.isEmpty()){Map<String,Object> r=new LinkedHashMap<>();r.put("status","UNKNOWN_LEGACY_CHECKPOINT_NO_MEASURED_REFIT_ORIGINAL");r.put("knownLifetimeRefits",null);return r;}
        Map<String,Object> r=new LinkedHashMap<>(map(JsonR27.parse(ledger.probeSourceR335)));DiagnosisStoreSU2.ProbeCheckpointR335 checkpoint=DiagnosisStoreSU2.readProbeR335(db);byte[] blob=checkpoint.bytes;
        boolean exact=blob!=null&&HashingSU1.bytes(blob).equals(r.get("checkpointHash"))&&checkpoint.through==DiagnosisShadowReceiptR335.number(r.get("throughSequence"),-1);
        r.put("status",exact?"PERSISTED_CHECKPOINT_SHA_VERIFIED":"UNKNOWN_CHECKPOINT_REFERENCE_CHANGED");r.put("countsUsable",exact);r.put("completeRefitOriginal","Decode InformationProbeSU2 from this exact protected checkpoint; ContMatSU2.refitOriginalR335 contains every pattern/scope n1,k1,n2,k2,z1,z2 and verdict");return r;
    }
    static Map<String,Object> operators(SQLiteDatabase db)throws Exception{
        ModelEvolutionSU2.State s=ModelStoreSU2.loadEvolution(db);List<Object> data=new ArrayList<>();
        for(Map.Entry<String,double[]> entry:s.opStats.entrySet()){double[] x=entry.getValue();if(x==null||x.length<2||!finite(x[0])||!finite(x[1]))throw new IllegalArgumentException("R335_OPERATOR_STATE_INVALID");
            Map<String,Object> r=new LinkedHashMap<>();r.put("operator",entry.getKey());r.put("rawDenominator",x[0]);r.put("rawNumerator",x[1]);r.put("fixedPriorAttempts",1);r.put("fixedPriorWins",1);
            boolean valid=x[0]>=1&&x[1]>=1&&x[1]<=x[0]&&x[0]==Math.rint(x[0])&&x[1]==Math.rint(x[1]);r.put("actualAttempts",valid?(long)x[0]-1:null);r.put("actualDevWins",valid?(long)x[1]-1:null);r.put("status",valid?"RECORDED_DEV_ONLY":"UNKNOWN_INVALID_COUNTS");data.add(r);}
        Map<String,Object> out=rows(data,data.size());out.put("checkpoint",evolutionCheckpoint(db));out.put("definition","Actual ModelEvolutionSU2 producer initializes {1,1}; observations subtract only that fixed prior. DEV wins are not independent future gains.");return out;
    }
    static Map<String,Object> repairs(SQLiteDatabase db)throws Exception{
        ModelEvolutionSU2.State s=ModelStoreSU2.loadEvolution(db);List<Object> data=new ArrayList<>();
        for(int i=0;i<Math.min(40,s.repairNotes.size());i++){String note=s.repairNotes.get(i);Map<String,Object> r=new LinkedHashMap<>();r.put("originalIndex",i);r.put("originalNote",note);r.put("sha256",HashingSU1.text(note));data.add(r);}
        Map<String,Object> out=rows(data,s.repairNotes.size());out.put("checkpoint",evolutionCheckpoint(db));out.put("producerNoteCapacity",200);out.put("allAttemptCount",null);out.put("unrecordedRejections",null);out.put("rejectionReasonsStatus","UNKNOWN_WHERE_ORIGINAL_OWNER_DID_NOT_RECORD; notes are bounded outcomes, not the complete attempt denominator");return out;
    }
    static Map<String,Object> evolutionCheckpoint(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT campaign_id,state_blob,updated_at FROM su2_evolution_state WHERE singleton=1",null)){if(!c.moveToFirst())throw new IllegalStateException("R335_EVOLUTION_CHECKPOINT_MISSING");Map<String,Object> r=new LinkedHashMap<>();r.put("campaignId",c.getString(0));r.put("storedReferenceHash",HashingSU1.bytes(c.getBlob(1)));r.put("updatedAt",c.getLong(2));return r;}}
    static Map<String,Object> storage(SQLiteDatabase db){List<Object> data=new ArrayList<>();long total;try(Cursor c=db.rawQuery("SELECT count(*) FROM su2_storage_budget",null)){if(!c.moveToFirst())throw new IllegalStateException("R335_STORAGE_COUNT_UNKNOWN");total=c.getLong(0);}
        try(Cursor c=db.rawQuery("SELECT category,measured_bytes,cap_bytes,measured_at,action FROM su2_storage_budget ORDER BY category LIMIT 32",null)){while(c.moveToNext()){Map<String,Object> r=new LinkedHashMap<>();r.put("category",c.getString(0));boolean measured=!c.isNull(3)&&c.getLong(3)>0;r.put("measuredBytes",measured&&!c.isNull(1)?c.getLong(1):null);r.put("capBytes",c.isNull(2)?null:c.getLong(2));r.put("measuredAt",measured?c.getLong(3):null);r.put("action",c.getString(4));r.put("status",measured?"MEASURED_AT_RECORDED_TIME":"UNKNOWN_NOT_MEASURED");data.add(r);}}
        Map<String,Object> out=rows(data,total);out.put("measurementFreshnessNotInferred",true);return out;
    }
    static Map<String,Object> matrices(SQLiteDatabase db)throws Exception{List<Object> data=new ArrayList<>();long total;try(Cursor c=db.rawQuery("SELECT count(*) FROM su2_matrix_snapshot",null)){if(!c.moveToFirst())throw new IllegalStateException("R335_MATRIX_COUNT_UNKNOWN");total=c.getLong(0);}
        try(Cursor c=db.rawQuery("SELECT genome_key,checkpoint_sequence,snapshot_json,stats_json,graded_through,updated_at FROM su2_matrix_snapshot ORDER BY checkpoint_sequence DESC,genome_key LIMIT 8",null)){while(c.moveToNext()){
            Map<String,Object> r=new LinkedHashMap<>();r.put("genomeKey",c.getString(0));r.put("checkpointSequence",c.getLong(1));r.put("gradedThrough",c.getLong(4));r.put("updatedAt",c.getLong(5));
            try{String body=ModelPublicationR31.resolveText(db,c.getString(2));Map<String,Object> doc=map(JsonR27.parse(body)),stats=map(JsonR27.parse(c.getString(3)));
                if(!c.getString(0).equals(doc.get("genome_key"))||DiagnosisShadowReceiptR335.number(doc.get("checkpoint_sequence"),-1)!=c.getLong(1))throw new IllegalArgumentException("R335_MATRIX_IDENTITY_MISMATCH");
                r.put("sourceBodyHash",HashingSU1.text(body));r.put("statsHash",HashingSU1.text(c.getString(3)));List<Object> grades=new ArrayList<>();boolean missing=stats.containsKey("__MISSING_SOURCE");long gradeTotal=0;
                for(String key:new TreeSet<>(stats.keySet())){if(key.startsWith("__"))continue;gradeTotal++;if(grades.size()==32)continue;List<?> a=list(stats.get(key));if(a.size()!=4)throw new IllegalArgumentException("R335_REASON_COUNTS");long[] n=new long[4];for(int i=0;i<4;i++){n[i]=DiagnosisShadowReceiptR335.number(a.get(i),-1);if(n[i]<0)throw new IllegalArgumentException("R335_REASON_COUNTS");}if(n[1]>n[0]||n[3]>n[2])throw new IllegalArgumentException("R335_REASON_HITS");
                    Map<String,Object> g=new LinkedHashMap<>();g.put("family",key);g.put("supported",n[0]);g.put("supportedHits",n[1]);g.put("other",n[2]);g.put("otherHits",n[3]);g.put("grade",missing?"C_EXPLAIN_ONLY":MatrixSnapshotSU2.grade(n[0],n[1],n[2],n[3]));g.put("missingSource",missing);grades.add(g);}
                r.put("reasonGrades",rows(grades,gradeTotal));r.put("missingSourceOriginal",stats.get("__MISSING_SOURCE"));
                List<?> teachers=list(doc.get("teacher_matrix"));List<Object> bounded=new ArrayList<>();for(int i=0;i<Math.min(8,teachers.size());i++)bounded.add(teachers.get(i));r.put("teachers",rows(bounded,teachers.size()));
                r.put("atomicExplanation",doc.get("atomic_explanation"));r.put("status","RECORDED_ASSOCIATION_NOT_QUALIFICATION");
            }catch(Exception bad){r.put("status","UNKNOWN");r.put("errorType",bad.getClass().getName());r.put("error",String.valueOf(bad.getMessage()));}
            data.add(r);
        }}return rows(data,total);
    }
    static Map<String,Object> matrixProjection(Map<String,Object> source,String key,String scope){List<Object> values=new ArrayList<>();long total=0;boolean unknown=false;Object raw=source.get("rows");
        if(raw instanceof List)for(Object item:(List<?>)raw){Map<String,Object> r=map(item);Object child=r.get(key);if(!(child instanceof Map)){unknown=true;continue;}Map<String,Object> m=map(child);total+=DiagnosisShadowReceiptR335.number(m.get("records"),0);Map<String,Object> ref=new LinkedHashMap<>();ref.put("genomeKey",r.get("genomeKey"));ref.put("checkpointSequence",r.get("checkpointSequence"));ref.put("gradedThrough",r.get("gradedThrough"));ref.put("updatedAt",r.get("updatedAt"));ref.put("sourceBodyHash",r.get("sourceBodyHash"));ref.put("evidence",m);values.add(ref);}else unknown=true;
        Map<String,Object> out=new LinkedHashMap<>();out.put("status",unknown?"PARTIAL_UNKNOWN_SOURCE":total==0?"INSUFFICIENT":"RECORDED_UNVALIDATED");out.put("records",unknown?null:total);out.put("sourceFrames",values);out.put("sourceFramesOmitted",source.get("omittedRows"));out.put("scope",scope);out.put("qualityEffective",null);return out;
    }
    static Map<String,Object> paired(SQLiteDatabase db,long sequence){List<Object> out=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT l.child_key,l.parent_key,l.campaign_id,g.born_sequence FROM su2_lineage l JOIN su2_model_genome g ON g.genome_key=l.child_key WHERE g.born_sequence<? ORDER BY g.born_sequence DESC,l.child_key,l.parent_key LIMIT 6",new String[]{String.valueOf(sequence)})){
            while(c.moveToNext()){String child=c.getString(0),parent=c.getString(1);long start=Math.max(c.getLong(3),sequence-5000);Map<String,Object> r=new LinkedHashMap<>();r.put("childKey",child);r.put("parentKey",parent);r.put("campaignId",c.getString(2));r.put("fromOrdinal",start);r.put("throughExclusive",sequence);r.put("windowRule","FIXED_LATEST_5000_MAX_AND_NOT_BEFORE_CHILD_BIRTH; DESCRIPTIVE_NOT_PREREGISTERED_QUALIFICATION");
                long[] window;
                try {
                    window = pairWindow(db, child, parent, start, sequence);
                } catch (IllegalArgumentException invalid) {
                    r.put("status", "UNKNOWN_INVALID_PAIR_ORIGINAL");
                    r.put("reason", invalid.getMessage());
                    r.put("pairedOpportunities", null);
                    r.put("pairedMeanOpportunityHitDifference", null);
                    r.put("qualityEffective", null);
                    out.add(r);
                    continue;
                }
                r.put("lineageWindowOrdinals", sequence - start);
                r.put("ordinalsMissingOneOrBothFreezes", window[2]);
                r.put("ordinalsPendingSettlement", window[1]);
                long n=0,childActs=0,parentActs=0;double sum=0,squares=0;StringBuilder original=new StringBuilder();
                try(Cursor p=db.rawQuery("SELECT a.sequence_ordinal,a.act,a.hit,b.act,b.hit,a.position,a.number_mask,b.position,b.number_mask,a.frozen_at,b.frozen_at,a.target_open_time,a.settled_at,b.settled_at FROM su2_live_decision a JOIN su2_live_decision b ON b.sequence_ordinal=a.sequence_ordinal WHERE a.genome_key=? AND b.genome_key=? AND a.sequence_ordinal>=? AND a.sequence_ordinal<? AND a.hit IS NOT NULL AND b.hit IS NOT NULL AND a.settled_at>0 AND b.settled_at>0 AND a.frozen_at<a.target_open_time AND b.frozen_at<b.target_open_time AND a.target_open_time=b.target_open_time ORDER BY a.sequence_ordinal LIMIT 5001",new String[]{child,parent,String.valueOf(start),String.valueOf(sequence)})){
                    while(p.moveToNext()){if(++n>5000)throw new IllegalStateException("R335_PAIRED_WINDOW_BOUND");int ca=p.getInt(1),ch=p.getInt(2),pa=p.getInt(3),ph=p.getInt(4);if((ca|pa|ch|ph)<0||ca>1||pa>1||ch>1||ph>1)throw new IllegalArgumentException("R335_PAIRED_BINARY");double d=ca*ch-pa*ph;sum+=d;squares+=d*d;childActs+=ca;parentActs+=pa;for(int k=0;k<14;k++)original.append(p.getString(k)).append(k==13?'\n':'\t');}
                }
                if (n != window[0]) throw new IllegalStateException("R335_PAIRS_SNAPSHOT_COUNT_MISMATCH");
                r.put("pairedOpportunities",n);r.put("childActs",childActs);r.put("parentActs",parentActs);r.put("pairedOriginalSha256",HashingSU1.text(original.toString()));r.put("differenceDefinition","child(act*hit)-parent(act*hit), both on the identical settled sequence; abstention contributes zero. Not monetary return.");
                Double mean=n>0?sum/n:null,lower=n>1?mean-1.6448536269514722*Math.sqrt(Math.max(0,(squares-sum*sum/n)/(n-1))/n):null;
                r.put("pairedMeanOpportunityHitDifference",mean);r.put("descriptiveOneSided95NormalLower",lower);r.put("intervalAssumptions","IID_NORMAL_APPROXIMATION_ONLY; serial dependence, selection, missing opportunities and sequential eligibility are not resolved by this interval");r.put("qualityEffective",null);r.put("status",n==0?"INSUFFICIENT":n==1?"INSUFFICIENT_INTERVAL":"RECORDED_PAIRED_DESCRIPTIVE");r.put("independentEnabledDenominator",null);out.add(r);
            }
        }
        Map<String,Object> result=rows(out,out.size());result.put("candidateSelection","LATEST_SIX_ACTUAL_LINEAGE_PAIRS; OMITTED_OLDER_PAIRS_NOT_DECLARED_ABSENT");result.put("legacySeparateRateNotUsedAsPairedGain",true);return result;
    }

    /** Read from the same frozen snapshot as the joined numerical summary. */
    private static long[] pairWindow(SQLiteDatabase db, String child, String parent, long start, long end) {
        if (child.equals(parent) || start < 0 || end <= start || end - start > 5000) {
            throw new IllegalArgumentException("R335_INVALID_LINEAGE_WINDOW");
        }
        String validRow = "typeof(sequence_ordinal)='integer' AND typeof(frozen_at)='integer' "
                + "AND typeof(target_open_time)='integer' AND frozen_at>0 AND frozen_at<target_open_time "
                + "AND typeof(act)='integer' AND act IN (0,1) AND ((hit IS NULL "
                + "AND (settled_at IS NULL OR (typeof(settled_at)='integer' AND settled_at=0))) "
                + "OR (typeof(hit)='integer' AND hit IN (0,1) AND typeof(settled_at)='integer' "
                + "AND settled_at>=target_open_time))";
        String sql = "SELECT sequence_ordinal,count(*),count(DISTINCT genome_key),"
                + "min(target_open_time),max(target_open_time),count(hit),"
                + "sum(CASE WHEN " + validRow + " THEN 0 ELSE 1 END) "
                + "FROM su2_live_decision WHERE genome_key IN (?,?) AND sequence_ordinal>=? "
                + "AND sequence_ordinal<? GROUP BY sequence_ordinal ORDER BY sequence_ordinal LIMIT 5001";
        long settledPairs = 0, pendingPairs = 0;
        String[] args = {child, parent, String.valueOf(start), String.valueOf(end)};
        try (Cursor cursor = db.rawQuery(sql, args)) {
            while (cursor.moveToNext()) {
                long arms = cursor.getLong(1);
                if (cursor.getLong(6) != 0 || arms != cursor.getLong(2) || arms > 2
                        || cursor.getLong(3) != cursor.getLong(4)) {
                    throw new IllegalArgumentException("R335_INVALID_PAIR_AT:" + cursor.getLong(0));
                }
                if (arms == 2) {
                    if (cursor.getLong(5) == 2) settledPairs++;
                    else pendingPairs++;
                }
            }
        }
        return new long[]{settledPairs, pendingPairs, end - start - settledPairs - pendingPairs};
    }
    static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
}
