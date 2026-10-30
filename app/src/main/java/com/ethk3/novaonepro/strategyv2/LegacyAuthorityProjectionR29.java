package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;
import java.io.*;

/** Current legacy authority is verified in its research owner, then published as one complete versioned frame.
 * Root consumers read only this frame. Historical Root copies, stale frames and incomplete commits grant nothing.
 * Source certificates, evidence, models, budgets and prediction records remain authoritative in RP. */
public final class LegacyAuthorityProjectionR29 {
    private LegacyAuthorityProjectionR29(){}
    public static final long FRESH_MS=3000;
    public static final String SOURCE="CREATE TABLE IF NOT EXISTS r29_legacy_authority_source(singleton INTEGER PRIMARY KEY CHECK(singleton=1),revision INTEGER NOT NULL,published_revision INTEGER NOT NULL,last_hash TEXT NOT NULL)";
    public static final String FRAME="CREATE TABLE IF NOT EXISTS r29_legacy_authority_frame(singleton INTEGER PRIMARY KEY CHECK(singleton=1),route_epoch INTEGER NOT NULL,revision INTEGER NOT NULL,frame_hash TEXT NOT NULL,phase TEXT NOT NULL,first_committed_wall INTEGER NOT NULL DEFAULT 0,first_committed_mono INTEGER NOT NULL DEFAULT 0,heartbeat_at INTEGER NOT NULL)";
    public static final String ROWS="CREATE TABLE IF NOT EXISTS r29_legacy_authority_projection(kind TEXT NOT NULL,identity TEXT NOT NULL,target TEXT NOT NULL,payload_json TEXT NOT NULL,payload_hash TEXT NOT NULL,route_epoch INTEGER NOT NULL,revision INTEGER NOT NULL,PRIMARY KEY(kind,identity,target))";
    private static final String[] SOURCES={"su1_certificate","su1_certificate_runtime","su1_certificate_revocation","su1_strategy_version","su1_model_artifact","su1_blob","su1_runtime_program","su1_runtime_control","su1_mainline_head_event","su1_evidence_look","su1_eval_cohort","su1_prediction","su1_snapshot","r29_su1_sealed_binding","su2_certificate","su2_model_genome","r26_certificate_protocol","r26_certificate_revocation","r26_cohort","r26_qualification_result"};
    static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    public static void ensureRealtime(SQLiteDatabase db){db.execSQL(FRAME);db.execSQL(ROWS);}
    public static void installResearch(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS r29_su1_sealed_binding(prediction_id TEXT NOT NULL,certificate_id TEXT NOT NULL,source_generation INTEGER NOT NULL,source_hash TEXT NOT NULL,target TEXT NOT NULL,sealed_at INTEGER NOT NULL,PRIMARY KEY(prediction_id,certificate_id))");
        db.execSQL(SOURCE);db.execSQL("INSERT INTO r29_legacy_authority_source SELECT 1,1,0,'' WHERE NOT EXISTS(SELECT 1 FROM r29_legacy_authority_source WHERE singleton=1)");
        for(String t:SOURCES)if(table(db,t))for(String event:new String[]{"INSERT","UPDATE","DELETE"})db.execSQL("CREATE TRIGGER IF NOT EXISTS r29_authority_"+t+"_"+event.toLowerCase(java.util.Locale.ROOT)+" AFTER "+event+" ON "+t+" BEGIN UPDATE r29_legacy_authority_source SET revision=revision+1 WHERE singleton=1; END");
    }
    static final class Entry{
        final String kind,id,target,payload,hash;
        Entry(String kind,String id,String target,Map<String,Object> value){this.kind=kind;this.id=id;this.target=target;payload=JsonR27.write(new TreeMap<>(value));hash=StatsR27.sha256(payload);}
        String key(){return kind+"|"+id+"|"+target;}
    }
    static final class Frame{
        long epoch,sourceRevision,revision;final List<Entry> rows=new ArrayList<>();
        String hash(){List<String> values=new ArrayList<>();for(Entry e:rows)values.add(e.key()+"|"+e.hash);Collections.sort(values);StringBuilder b=new StringBuilder("R29_LEGACY_AUTHORITY_1|");for(String v:values)b.append(v).append(';');return StatsR27.sha256(b.toString());}
    }
    static Map<String,Object> su1Map(CertificateSU1.Authority a){Map<String,Object> m=new TreeMap<>();m.put("strategy",a.strategy);m.put("program",a.program);m.put("artifact",a.artifact);m.put("selector",a.selector);m.put("calibration",a.calibration);m.put("certificate",a.certificate);m.put("evidence",a.evidence);m.put("source",a.source);m.put("family",a.family);m.put("quality",a.quality);m.put("issued",a.issued);m.put("expires",a.expires);m.put("verifiedBy","CertificateSU1.read(requireMainline=true)");return m;}
    static Frame capture(SQLiteDatabase research,long now){
        Frame f=new Frame();f.epoch=MigrationCopyR29.scalar(research,"SELECT epoch FROM main.r29_research_owner WHERE singleton=1 AND phase='READY'",null);f.sourceRevision=MigrationCopyR29.scalar(research,"SELECT revision FROM r29_legacy_authority_source WHERE singleton=1",null);
        if(table(research,"su2_certificate")){
            List<String> genomes=new ArrayList<>();try(Cursor c=research.rawQuery("SELECT DISTINCT genome_key FROM su2_certificate WHERE issued_at<=? AND expires_at>? ORDER BY genome_key",new String[]{Long.toString(now),Long.toString(now)})){while(c.moveToNext())genomes.add(c.getString(0));}
            for(String g:genomes){CertificateSU2.Authority a=CertificateSU2.read(research,g,now);if(a==null)continue;Map<String,Object> m=new TreeMap<>();m.put("certificate",a.certificate);m.put("genome",a.genome);m.put("program",a.program);m.put("a",a.a);m.put("b",a.b);m.put("issued",a.issued);m.put("expires",a.expires);m.put("verifiedBy","CertificateSU2.read/immutable protocol and original issuance gates");f.rows.add(new Entry("SU2_CERT",g,"",m));}
        }
        if(table(research,"su1_certificate")&&table(research,"su1_mainline_head_event")){
            List<String> certificates=new ArrayList<>();try(Cursor c=research.rawQuery("SELECT certificate_id FROM su1_certificate WHERE issued_at<=? AND expires_at>? ORDER BY certificate_id",new String[]{Long.toString(now),Long.toString(now)})){while(c.moveToNext())certificates.add(c.getString(0));}
            for(String id:certificates){
                CertificateSU1.Authority a=CertificateSU1.read(research,id,now,true);if(a==null)continue;Map<String,Object> authority=su1Map(a);f.rows.add(new Entry("SU1_CERT",id,"",authority));
                try(Cursor c=research.rawQuery("SELECT p.prediction_id,p.target_issue,p.record_hash,p.position,p.n1,p.n2,p.n3,p.selected_hit_p,p.calibration_hash,s.source_version_hash,b.source_generation,o.deadline_at,p.captured_at FROM r29_su1_sealed_binding b JOIN su1_prediction p ON p.prediction_id=b.prediction_id JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash JOIN su1_opportunity o ON o.opportunity_id=p.opportunity_id JOIN su1_eval_cohort q ON q.cohort_id=o.cohort_id WHERE p.strategy_id=? AND b.certificate_id=? AND b.source_hash=s.source_version_hash AND q.evidence_role='RESEARCH_FUTURE' AND o.deadline_at>? AND p.captured_at<=o.deadline_at-5000 ORDER BY p.target_issue,p.prediction_id",new String[]{a.strategy,a.certificate,Long.toString(now+5000)})){
                    while(c.moveToNext()){if(c.isNull(7)||!a.calibration.equals(c.getString(8)))continue;Map<String,Object> p=new TreeMap<>(authority);p.put("prediction",c.getString(0));p.put("target",Long.toString(c.getLong(1)));p.put("record",c.getString(2));p.put("position",c.getInt(3));p.put("n1",c.getInt(4));p.put("n2",c.getInt(5));p.put("n3",c.getInt(6));p.put("selectedHitP",c.getDouble(7));p.put("freezeSource",c.getString(9));p.put("generation",c.getLong(10));p.put("deadline",c.getLong(11));p.put("capturedAt",c.getLong(12));f.rows.add(new Entry("SU1_PROOF",c.getString(0),Long.toString(c.getLong(1)),p));}
                }
            }
        }
        if(f.sourceRevision!=MigrationCopyR29.scalar(research,"SELECT revision FROM r29_legacy_authority_source WHERE singleton=1",null))throw new IllegalStateException("CP3_DB_BUSY_LEGACY_AUTHORITY_CHANGED_DURING_CAPTURE");return f;
    }
    static void sealSu1(DbStore store,CertificateSU1.Authority a,EventFrameSU1 frame,String prediction,String record){
        if(!ResearchRouteR29.isResearchDatabase(store.readable()))return;
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_SU1_SEALED_PROOF_BINDING",db->{
            if(CertificateSU1.read(db,a.certificate,System.currentTimeMillis(),true)==null||EventFrameBuilderSU1.generation(LegacyResearchScopeR29.realtime(db))!=frame.sourceGeneration||System.currentTimeMillis()>=frame.targetOpenTime-5000)throw new IllegalStateException("R29_SU1_SEALED_PROOF_SOURCE_OR_AUTHORITY_CHANGED");
            try(Cursor c=db.rawQuery("SELECT p.record_hash,s.source_version_hash FROM su1_prediction p JOIN su1_snapshot s ON s.snapshot_hash=p.snapshot_hash WHERE p.prediction_id=? AND p.strategy_id=? AND CAST(p.target_issue AS TEXT)=?",new String[]{prediction,a.strategy,Long.toString(frame.targetIssue)})){if(!c.moveToFirst()||!record.equals(c.getString(0))||!frame.sourceVersionHash.equals(c.getString(1)))throw new IllegalStateException("R29_SU1_SEALED_PROOF_RECORD_CHANGED");}
            db.execSQL("INSERT OR IGNORE INTO r29_su1_sealed_binding VALUES(?,?,?,?,?,?)",new Object[]{prediction,a.certificate,frame.sourceGeneration,frame.sourceVersionHash,Long.toString(frame.targetIssue),System.currentTimeMillis()});
            try(Cursor c=db.rawQuery("SELECT source_generation,source_hash,target FROM r29_su1_sealed_binding WHERE prediction_id=? AND certificate_id=?",new String[]{prediction,a.certificate})){if(!c.moveToFirst()||c.getLong(0)!=frame.sourceGeneration||!frame.sourceVersionHash.equals(c.getString(1))||!Long.toString(frame.targetIssue).equals(c.getString(2)))throw new IllegalStateException("R29_SU1_SEALED_PROOF_BINDING_CONFLICT");}return 0;
        });
    }
    static boolean apply(SQLiteDatabase realtime,Frame f,long now){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_AUTHORITY_FRAME_TRANSACTION");
        if(MigrationCopyR29.scalar(realtime,"SELECT epoch FROM r29_database_route WHERE singleton=1 AND phase='ACTIVE'",null)!=f.epoch)throw new IllegalStateException("R29_AUTHORITY_ROUTE_EPOCH_CHANGED");
        String hash=f.hash();
        try(Cursor c=realtime.rawQuery("SELECT route_epoch,revision,frame_hash FROM r29_legacy_authority_frame WHERE singleton=1",null)){
            if(c.moveToFirst()){
                if(c.getLong(0)>f.epoch||c.getLong(0)==f.epoch&&c.getLong(1)>f.revision)return false;
                if(c.getLong(0)==f.epoch&&c.getLong(1)==f.revision){if(!hash.equals(c.getString(2)))throw new IllegalStateException("R29_AUTHORITY_REVISION_PAYLOAD_CONFLICT");realtime.execSQL("UPDATE r29_legacy_authority_frame SET heartbeat_at=? WHERE singleton=1",new Object[]{now});return false;}
            }
        }
        realtime.execSQL("DELETE FROM r29_legacy_authority_projection");
        for(Entry e:f.rows)realtime.execSQL("INSERT INTO r29_legacy_authority_projection VALUES(?,?,?,?,?,?,?)",new Object[]{e.kind,e.id,e.target,e.payload,e.hash,f.epoch,f.revision});
        realtime.execSQL("INSERT OR REPLACE INTO r29_legacy_authority_frame VALUES(1,?,?,?,'PENDING_COMMIT_RECEIPT',0,0,?)",new Object[]{f.epoch,f.revision,hash,now});return true;
    }
    static void receipt(SQLiteDatabase realtime,Frame f,long wall,long mono){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_AUTHORITY_RECEIPT_TRANSACTION");
        realtime.execSQL("UPDATE r29_legacy_authority_frame SET phase='USABLE',first_committed_wall=?,first_committed_mono=?,heartbeat_at=? WHERE singleton=1 AND route_epoch=? AND revision=? AND frame_hash=? AND phase='PENDING_COMMIT_RECEIPT' AND EXISTS(SELECT 1 FROM r29_database_route WHERE singleton=1 AND phase='ACTIVE' AND epoch=?)",new Object[]{wall,mono,System.currentTimeMillis(),f.epoch,f.revision,f.hash(),f.epoch});
    }
    public static long drain(DbStore store){
        SQLiteDatabase realtime=LegacyResearchScopeR29.realtime(store.readable());if(!ResearchRouteR29.active(realtime))return 0;if(realtime.inTransaction())throw new IllegalStateException("R29_AUTHORITY_PUBLICATION_MUST_NOT_HOLD_REALTIME_TRANSACTION");
        ResearchRouteR29.write(store,"R29_LEGACY_AUTHORITY_SETUP",db->{installResearch(db);return 0;});
        Frame f=capture(ResearchRouteR29.reader(realtime),System.currentTimeMillis());final String hash=f.hash();
        f.revision=ResearchRouteR29.write(store,"R29_LEGACY_AUTHORITY_RESERVE",db->{
            try(Cursor c=db.rawQuery("SELECT revision,published_revision,last_hash FROM r29_legacy_authority_source WHERE singleton=1",null)){
                if(!c.moveToFirst()||c.getLong(0)!=f.sourceRevision)throw new IllegalStateException("CP3_DB_BUSY_LEGACY_AUTHORITY_CHANGED_BEFORE_PUBLICATION");long revision=c.getLong(1);if(hash.equals(c.getString(2))&&revision>0)return revision;
                db.execSQL("UPDATE r29_legacy_authority_source SET published_revision=published_revision+1,last_hash=? WHERE singleton=1",new Object[]{hash});return revision+1;
            }
        });
        boolean changed=LegacyResearchScopeR29.realtimeWrite(store,"R29_LEGACY_AUTHORITY_FRAME",db->{ensureRealtime(db);return apply(db,f,System.currentTimeMillis());});
        long committedWall=System.currentTimeMillis(),committedMono=TimeSourceR811.monoNowMs();
        LegacyResearchScopeR29.realtimeWrite(store,"R29_LEGACY_AUTHORITY_COMMIT_RECEIPT",db->{receipt(db,f,committedWall,committedMono);return 0;});return changed?f.rows.size()+1:0;
    }
    @SuppressWarnings("unchecked") static Map<String,Object> read(SQLiteDatabase db,String kind,String id,String target,long now){
        if(!table(db,"r29_legacy_authority_frame"))return null;
        try(Cursor c=db.rawQuery("SELECT p.payload_json,p.payload_hash,f.first_committed_wall FROM r29_legacy_authority_projection p JOIN r29_legacy_authority_frame f ON f.singleton=1 AND f.route_epoch=p.route_epoch AND f.revision=p.revision JOIN r29_database_route r ON r.singleton=1 AND r.phase='ACTIVE' AND r.epoch=f.route_epoch WHERE p.kind=? AND p.identity=? AND p.target=? AND f.phase='USABLE' AND f.first_committed_wall>0 AND f.first_committed_mono>0 AND f.heartbeat_at<=? AND f.heartbeat_at>=?",new String[]{kind,id,target,Long.toString(now),Long.toString(now-FRESH_MS)})){
            if(!c.moveToFirst())return null;String payload=c.getString(0);if(!StatsR27.sha256(payload).equals(c.getString(1)))throw new IllegalStateException("R29_AUTHORITY_PAYLOAD_CORRUPT");Map<String,Object> m=(Map<String,Object>)JsonR27.parse(payload);m.put("projectionCommittedAt",c.getLong(2));if(!(m.get("issued") instanceof Number)||!(m.get("expires") instanceof Number)||now<((Number)m.get("issued")).longValue()||now>=((Number)m.get("expires")).longValue())return null;return m;
        }
    }
    static CertificateSU2.Authority su2(SQLiteDatabase db,String genome,long now){Map<String,Object> m=read(db,"SU2_CERT",genome,"",now);if(m==null)return null;return new CertificateSU2.Authority((String)m.get("certificate"),genome,(String)m.get("program"),((Number)m.get("a")).doubleValue(),((Number)m.get("b")).doubleValue(),((Number)m.get("issued")).longValue(),((Number)m.get("expires")).longValue());}
    static CertificateSU1.Authority su1(SQLiteDatabase db,String certificate,long now){Map<String,Object> m=read(db,"SU1_CERT",certificate,"",now);return m==null?null:new CertificateSU1.Authority(m);}
    static CertificateSU1.Authority su1ForStrategy(SQLiteDatabase db,String strategy,String certificate,long now){
        if(!table(db,"r29_legacy_authority_projection"))return null;List<String> ids=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT identity FROM r29_legacy_authority_projection WHERE kind='SU1_CERT' AND (?='' OR identity=?) ORDER BY identity",new String[]{certificate,certificate})){while(c.moveToNext())ids.add(c.getString(0));}for(String id:ids){CertificateSU1.Authority a=su1(db,id,now);if(a!=null&&strategy.equals(a.strategy))return a;}return null;
    }
    static boolean verifySu1(SQLiteDatabase db,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        if(proof==null)return false;
        return verifySu1Values(db,proof.candidate,proof.certificate,proof.target,proof.modelHash,proof.calibrationHash,proof.selectionHash,proof.selectedHitP,StrictCanonicalJson.readObject(proof.recipe),action);
    }
    static boolean verifySu1Values(SQLiteDatabase db,String candidate,String certificate,String target,String modelHash,String calibrationHash,String selectionHash,double selectedHitP,Map<String,Object> r,V51Models.PositionAction action){
        long now=System.currentTimeMillis();
        Map<String,Object> p=read(db,"SU1_PROOF",String.valueOf(r.get("prediction")),target,now);CertificateSU1.Authority a=su1(db,certificate,now);if(p==null||a==null)return false;
        if(!MainlineAdapterSU1.CONTRACT.equals(r.get("contract"))||!candidate.equals(a.candidate())||!certificate.equals(p.get("certificate"))||!modelHash.equals(a.artifact)||!calibrationHash.equals(a.calibration)||!selectionHash.equals(a.selector)||!target.equals(r.get("target"))||!a.program.equals(r.get("program"))||!a.strategy.equals(r.get("strategy"))||!a.certificate.equals(r.get("certificate"))||!a.artifact.equals(r.get("artifact"))||!a.calibration.equals(r.get("calibration"))||!a.selector.equals(r.get("selector"))||!p.get("record").equals(r.get("record"))||!p.get("freezeSource").equals(r.get("source"))||!(r.get("generation") instanceof Number)||((Number)p.get("generation")).longValue()!=((Number)r.get("generation")).longValue()||Double.compare(selectedHitP,((Number)p.get("selectedHitP")).doubleValue())!=0)return false;
        long deadline=((Number)p.get("deadline")).longValue();if(now>=deadline-5000||((Number)p.get("projectionCommittedAt")).longValue()>=deadline-5000||EventFrameBuilderSU1.generation(db)!=((Number)p.get("generation")).longValue())return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM draws WHERE issue=? AND dirty=0",new String[]{target})){if(c.moveToFirst())return false;}
        return action==null||action.position==((Number)p.get("position")).intValue()&&action.n1==((Number)p.get("n1")).intValue()&&action.n2==((Number)p.get("n2")).intValue()&&action.n3==((Number)p.get("n3")).intValue();
    }
    /** Separate legacy sealed-proof namespace; generic F05 frozen-candidate scope remains unchanged. */
    public static String exactScope(String candidate,String structure,String model,String selector){
        if(candidate==null||!(candidate.startsWith(CertificateSU1.PREFIX)||candidate.startsWith(CertificateSU2.PREFIX)))throw new IllegalArgumentException("R29_LEGACY_SCOPE_CANDIDATE");
        for(String hash:new String[]{structure,model,selector})if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R29_LEGACY_SCOPE_IDENTITY");
        return V51ChecksumEngine.sha256("R29_LEGACY_SEALED_SCOPE_1|"+candidate+'|'+structure+'|'+model+'|'+selector);
    }
    /** Called by the original canonical owner only after the Proof hash/PIT/action envelope checks succeeded. */
    public static boolean verifySu2(SQLiteDatabase db,CertifiedPredictionR974.Proof proof,V51Models.PositionAction action){
        if(proof==null||action==null||proof.expired())return false;
        return verifySu2Values(db,proof.candidate,proof.certificate,proof.target,proof.structure,proof.modelHash,proof.calibrationHash,proof.selectionHash,proof.scopePolicyHash,proof.selectedHitP,proof.capturedAt,proof.deadlineAt,StrictCanonicalJson.readObject(proof.recipe),action);
    }
    static boolean verifySu2Values(SQLiteDatabase db,String candidate,String certificate,String target,String structure,String modelHash,String calibrationHash,String selectionHash,String scope,double selectedHitP,long capturedAt,long deadlineAt,Map<String,Object> recipe,V51Models.PositionAction action){
        if(action==null||candidate==null||!candidate.startsWith(CertificateSU2.PREFIX)||!TargetIssueResolver.isValid(target))return false;
        long now=System.currentTimeMillis(),seq=TargetIssueResolver.sequenceOrdinalSU1(target);String genome=candidate.substring(CertificateSU2.PREFIX.length());CertificateSU2.Authority a=CertificateSU2.read(db,genome,now);
        if(a==null||!a.certificate.equals(certificate)||!a.program.equals(structure)||!a.program.equals(modelHash)||a.issued>capturedAt||!MainlineAdapterSU2.CONTRACT.equals(selectionHash)||!exactScope(candidate,structure,modelHash,selectionHash).equals(scope))return false;
        if(!V51ChecksumEngine.sha256("SU2_PLATT|"+Double.toHexString(a.a)+"|"+Double.toHexString(a.b)).equals(calibrationHash)||!MainlineAdapterSU2.CONTRACT.equals(recipe.get("contract"))||!certificate.equals(recipe.get("certificate"))||!genome.equals(recipe.get("genome"))||!a.program.equals(recipe.get("program"))||!target.equals(recipe.get("target")))return false;
        String member=ModelStoreSU2.currentChoice(db,seq);boolean incumbent=member.equals("XI_INCUMBENT:"+genome);if(!incumbent&&!member.equals("SU2:"+genome))return false;
        if(action.position<1||action.position>10||action.n1<1||action.n1>10||action.n2<1||action.n2>10||action.n3<1||action.n3>10)return false;
        int mask=(1<<(action.n1-1))|(1<<(action.n2-1))|(1<<(action.n3-1));if(Integer.bitCount(mask)!=3)return false;
        long frozenAt,open;double phat;
        try(Cursor c=db.rawQuery("SELECT position,number_mask,phat,frozen_at,target_open_time FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal=? AND act=1 AND hit IS NULL",new String[]{genome,Long.toString(seq)})){
            if(!c.moveToFirst()||c.isNull(2)||c.getInt(0)!=action.position||c.getInt(1)!=mask)return false;phat=c.getDouble(2);frozenAt=c.getLong(3);open=c.getLong(4);
        }
        if(!Double.isFinite(phat)||phat<=0||phat>=1||frozenAt<=0||frozenAt>capturedAt||now>=Math.min(open,deadlineAt)-5000||!(recipe.get("position") instanceof Number)||!(recipe.get("mask") instanceof Number)||((Number)recipe.get("position")).intValue()!=action.position||((Number)recipe.get("mask")).intValue()!=mask)return false;
        double expected=new SelectiveCalibrationSU2.Platt(a.a,a.b).apply(EvidenceBlockR26.quantized(phat));if(!Double.isFinite(selectedHitP)||Double.compare(selectedHitP,expected)!=0)return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM draws WHERE issue=? AND dirty=0",new String[]{target})){if(c.moveToFirst())return false;}
        float[][] probabilities;
        if(incumbent){
            try(Cursor c=db.rawQuery("SELECT frozen_at,deadline_at,source_generation FROM r26_incumbent_frozen WHERE target_ordinal=? AND input_through=?",new String[]{Long.toString(seq),Long.toString(seq-1)})){if(!c.moveToFirst()||c.getLong(0)!=frozenAt||c.getLong(1)!=open||c.getLong(2)!=EventFrameBuilderSU1.generation(db))return false;}
            probabilities=IncumbentLoopSU2.pendingTable(db,seq);if(probabilities==null)return false;
        }else{
            try(Cursor c=db.rawQuery("SELECT frozen_at,deadline_at,source_generation,probability_table FROM r26_frozen WHERE policy_hash=? AND target_ordinal=? AND input_through=?",new String[]{genome,Long.toString(seq),Long.toString(seq-1)})){
                if(!c.moveToFirst()||c.getLong(0)!=frozenAt||c.getLong(1)!=open||c.getLong(2)!=EventFrameBuilderSU1.generation(db))return false;
                byte[] bytes=c.getBlob(3);if(bytes==null||bytes.length!=400)return false;probabilities=new float[10][10];try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){for(int p=0;p<10;p++){double sum=0;for(int k=0;k<10;k++){float v=in.readFloat();if(!Float.isFinite(v)||v<0||v>1)return false;probabilities[p][k]=v;sum+=v;}if(Math.abs(sum-1)>1e-3)return false;}}catch(IOException invalid){return false;}
            }
        }
        float[] p=probabilities[action.position-1];double mass=Math.max(0,Math.min(1,Math.rint(((double)p[action.n1-1]+p[action.n2-1]+p[action.n3-1])*1e9)/1e9));return Double.compare(action.rawActionP,mass)==0;
    }
}
