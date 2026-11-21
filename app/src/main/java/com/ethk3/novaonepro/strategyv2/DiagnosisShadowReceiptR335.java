package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.util.*;

/** One S5 consumer, inside the original SU2_DIAGNOSIS transaction. The original
 * ledger is the pending queue. The original lineage row and ledger carry the same
 * immutable receipt; neither a shadow row nor a proposed object is an ACK. */
public final class DiagnosisShadowReceiptR335 {
    private DiagnosisShadowReceiptR335() {}
    static final String VERSION="R335_S5_RECEIPT_V1", OP="R335_S5_RECEIPT";
    static final int MAX_RECEIPT_BYTES=16384;
    public static final class Source {
        final long generation,through,refitCursor;
        final String epoch,frameHash,checkpointHash,refitHash;
        final byte[] refitGzip; final int refitRawBytes,checkpointBytes;
        private boolean storageAdmitted;private String storageReason="NOT_ADMITTED";
        final Long lifetimeRefits;
        final long observedRefits;
        public Source(EventFrameSU1 frame,byte[] checkpoint,long through,ContMatSU2 model) {
            if(frame==null||checkpoint==null||model==null)throw new IllegalArgumentException("R335_S5_SOURCE_REQUIRED");
            generation=frame.sourceGeneration;epoch=frame.sourceEpoch;frameHash=frame.contentHash;
            checkpointHash=HashingSU1.bytes(checkpoint);checkpointBytes=checkpoint.length;this.through=through;
            refitCursor=model.lastRefitCursorR335();refitHash=model.refitOriginalSha256R335();
            lifetimeRefits=model.successfulRefitsR335();observedRefits=model.observedRefitsSinceEvidenceR335();
            byte[] raw=model.refitOriginalR335().getBytes(StandardCharsets.UTF_8);refitRawBytes=raw.length;refitGzip=raw.length==0?new byte[0]:ModelStoreSU2.gzip(raw);
            if(refitRawBytes>DiagnosisSchemaR335.MAX_RAW_BYTES||refitGzip.length>DiagnosisSchemaR335.MAX_GZIP_BYTES)throw new IllegalArgumentException("R335_REFIT_BODY_BOUND");
        }
        Map<String,Object> values(){Map<String,Object> m=new LinkedHashMap<>();m.put("epoch",epoch);m.put("sourceGeneration",generation);m.put("frameHash",frameHash);m.put("checkpointHash",checkpointHash);m.put("throughSequence",through);m.put("relativeRefitCursor",refitCursor);m.put("refitOriginalHash",refitHash);m.put("knownLifetimeRefits",lifetimeRefits);m.put("observedRefitsSinceEvidence",observedRefits);return m;}
        boolean measured(){return refitCursor>=0&&observedRefits>0&&refitGzip.length>0;}
    }
    /** A bounded common-budget lease is acquired before the SQLite writer. The
     * envelope includes PROBE, one retained original, two small receipts, page/WAL
     * expansion and a fixed margin. It does not claim other writers are registered. */
    public static final class Admission implements java.io.Closeable {
        final com.ethk3.novaonepro.StorageBudgetR31.Reservation lease;final Source source;final String reason;
        Admission(com.ethk3.novaonepro.StorageBudgetR31.Reservation lease,Source source,String reason){this.lease=lease;this.source=source;this.reason=reason;}
        public boolean admitted(){return lease!=null;}
        public void close() throws IOException {if(source!=null)source.storageAdmitted=false;if(lease!=null)lease.close();}
    }
    static long storageEnvelope(Source source){return 4L*(source.checkpointBytes+source.refitGzip.length+2L*UpgradeDiagnosisSU2.SNAP_MAX_BYTES+2L*MAX_RECEIPT_BYTES+65536L)+(2L<<20);}
    public static Admission admit(com.ethk3.novaonepro.DbStore store,Source source){
        if(source==null)return new Admission(null,null,"NO_SOURCE");
        try{java.io.File files=store.contextForR26().getFilesDir().getCanonicalFile(),root=files.getParentFile().getCanonicalFile(),database=new java.io.File(store.readable().getPath()).getCanonicalFile().getParentFile();
            String prefix=root.getPath()+java.io.File.separator;if(!database.getPath().startsWith(prefix))throw new IOException("R335_DIAG_DATABASE_SCOPE");
            return admit(com.ethk3.novaonepro.StorageBudgetR31.forFiles(files),database.getPath().substring(prefix.length()),source);
        }catch(IOException|RuntimeException denied){source.storageReason=denied.getClass().getSimpleName()+":"+denied.getMessage();return new Admission(null,source,source.storageReason);}
    }
    static Admission admit(com.ethk3.novaonepro.StorageBudgetR31 budget,String scope,Source source)throws IOException{
        long envelope=storageEnvelope(source);com.ethk3.novaonepro.StorageBudgetR31.Reservation lease=budget.reserve("SU2_DIAG_ORIGINAL:"+ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256.substring(0,16),Math.max(0,source.generation),com.ethk3.novaonepro.StorageBudgetR31.Category.DATABASE,scope,envelope,envelope,false);
        try{lease.beforeChunk(envelope);source.storageAdmitted=true;source.storageReason="COMMON_STORAGE_BUDGET_LEASE_HELD";return new Admission(lease,source,source.storageReason);}catch(IOException|RuntimeException denied){try{lease.close();}catch(IOException suppressed){denied.addSuppressed(suppressed);}throw denied;}
    }
    @SuppressWarnings("unchecked") static Map<String,Object> state(UpgradeDiagnosisSU2.Entry e){
        if(e.s5StateR335==null||e.s5StateR335.isEmpty())return new LinkedHashMap<>();
        Object x=JsonR27.parse(e.s5StateR335);if(!(x instanceof Map))throw new IllegalStateException("R335_S5_LEDGER_STATE");return new LinkedHashMap<>((Map<String,Object>)x);
    }
    static void save(UpgradeDiagnosisSU2.Entry e,Map<String,Object> state){String text=JsonR27.write(state);if(text.getBytes(StandardCharsets.UTF_8).length>MAX_RECEIPT_BYTES)throw new IllegalStateException("R335_S5_LEDGER_RECEIPT_BOUND");e.s5StateR335=text;}
    static void waitState(UpgradeDiagnosisSU2.Entry e,Map<String,Object> m,String reason,long seq,Source source){
        m.put("version",VERSION);m.put("state",reason);m.put("attemptSequence",seq);long old=number(m.get("attempts"),0);m.put("attempts",old==Long.MAX_VALUE?old:old+1);if(source!=null)m.put("lastObservedSource",source.values());save(e,m);
    }
    static boolean activeFinding(UpgradeDiagnosisSU2.Finding f,UpgradeDiagnosisSU2.Entry e,long seq){return f!=null&&e!=null&&"S5".equals(f.code)&&"".equals(f.scope)&&"ADD_MEMBER CONTMAT{}".equals(f.presc)&&f.presc.equals(e.presc)&&e.since>=0&&e.since<=seq&&!"RESOLVED".equals(e.status);}
    /** Called even inside an already diagnosed 500-period block, so parent arrival
     * and released capacity retry the same case without inventing another finding. */
    static void attempt(SQLiteDatabase db,UpgradeDiagnosisSU2.Diagnosis diagnosis,UpgradeDiagnosisSU2.Ledger ledger,long seq,Source source){
        if(!db.inTransaction())throw new IllegalStateException("R335_S5_ORIGINAL_WRITER_REQUIRED");
        UpgradeDiagnosisSU2.Entry entry=ledger.entries.get("S5|");if(entry==null)return;
        UpgradeDiagnosisSU2.Finding finding=null;for(UpgradeDiagnosisSU2.Finding f:diagnosis.findings)if("S5".equals(f.code)&&"".equals(f.scope)){finding=f;break;}
        Map<String,Object> st=state(entry);
        if(!activeFinding(finding,entry,seq)){waitState(entry,st,"PRESCRIPTION_NOT_CURRENT",seq,source);return;}
        try{DiagnosisSchemaR335.requireReady(db);com.ethk3.novaonepro.DatabaseCoordinator.requireSchemaObjectsR31(db,"R335_S5_POLICY_IDENTITY",new String[][]{{"r26_policy_identity","policy_hash,canonical_policy,genotype_text,numerical_abi"}},new String[][]{{"trigger","r26_policy_identity_immutable"}});}catch(com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31 pending){waitState(entry,st,"WAIT_ORIGINAL_SCHEMA_MIGRATION",seq,source);return;}
        String[] existing=shadow(db);
        Map<String,Object> original=receipt(st);
        if(existing!=null){
            boolean recoveredLedger=original==null;
            if(recoveredLedger)original=lineageReceipt(db,existing[0],existing[1]);
            if(original==null){waitState(entry,st,"UNKNOWN_ORIGINAL_RECEIPT_MISSING",seq,source);return;}
            if(recoveredLedger&&number(original.get("since"),-1)!=entry.since){
                UpgradeDiagnosisSU2.Entry prior=new UpgradeDiagnosisSU2.Entry();prior.code=entry.code;prior.scope=entry.scope;prior.presc=entry.presc;prior.since=number(original.get("since"),-1);
                validateReceipt(original,prior);
                if(!matches(db,original,false)){waitState(entry,st,"CONFLICT_ORIGINAL_CASE_OBJECTS",seq,source);return;}
                st.put("caseRestoredFromOriginalLineage",true);st.put("replacementLedgerFirstObservedAt",entry.first);st.put("originalSeenCountUnknownAfterLedgerLoss",true);
                entry.since=prior.since;entry.first=Math.min(entry.first,prior.since);
            }
            validateReceipt(original,entry);
            if(!hasOriginal(db,original)){
                if(source!=null&&source.measured()&&source.refitHash.equals(mapSource(original).get("refitOriginalHash"))){
                    if(!source.storageAdmitted){waitState(entry,st,"WAIT_ORIGINAL_STORAGE:"+source.storageReason,seq,source);return;}
                    storeOriginal(db,original,source);
                }
                else {waitState(entry,st,"UNKNOWN_REFIT_ORIGINAL_MISSING",seq,source);return;}
            }
            if(!matches(db,original,false)){waitState(entry,st,"CONFLICT_EXISTING_SHADOW_OR_CHILD",seq,source);return;}
            restoreReceiptCopy(db,original);
            st.put("receipt",original);st.put("receiptHash",HashingSU1.text(JsonR27.write(original)));
            waitState(entry,st,matches(db,original,true)?"RECEIPT_PRESENT_REQUIRES_INDEPENDENT_READBACK":"RECEIVED_THEN_RETIRED",seq,null);return;
        }
        if(original!=null){
            validateReceipt(original,entry);waitState(entry,st,"UNKNOWN_SHADOW_ORIGINAL_MISSING",seq,source);return;
        }
        if(source==null||!source.measured()){waitState(entry,st,"WAIT_MEASURED_REFIT_ORIGINAL",seq,source);return;}
        if(EventFrameBuilderSU1.generation(db)!=source.generation){waitState(entry,st,"WAIT_SOURCE_CHANGED",seq,source);return;}
        try(Cursor c=db.rawQuery("SELECT through_sequence,byte_count FROM su2_live_state WHERE owner=?",new String[]{DiagnosisStoreSU2.PROBE_OWNER})){
            if(!c.moveToFirst()||c.getLong(0)!=source.through||c.getLong(1)!=source.checkpointBytes)throw new IllegalStateException("R335_S5_CHECKPOINT_NOT_SAME_TRANSACTION");
        } // maintain just wrote these exact prepared bytes in this transaction; independent P5 readback checks their SHA.
        if(!source.storageAdmitted){waitState(entry,st,"WAIT_ORIGINAL_STORAGE:"+source.storageReason,seq,source);return;}
        try(Cursor c=db.rawQuery("SELECT case_id FROM r335_s5_refit_original LIMIT 2",null)){if(c.moveToFirst()){waitState(entry,st,"WAIT_PROTECTED_ORIGINAL_CAPACITY",seq,source);return;}}
        int count;try(Cursor c=db.rawQuery("SELECT count(*) FROM su2_diag_shadow WHERE state='ACTIVE'",null)){if(!c.moveToFirst())throw new IllegalStateException("R335_S5_CAPACITY_UNKNOWN");count=c.getInt(0);}
        if(count>=DiagnosisStoreSU2.MAX_SHADOWS){waitState(entry,st,"WAIT_CAPACITY",seq,source);return;}
        String parent=null,text=null;
        try(Cursor c=db.rawQuery("SELECT g.genome_key,g.canonical_text FROM su2_model_genome g JOIN su2_league l ON l.genome_key=g.genome_key WHERE g.state='LEAGUE' AND g.tier='F15' AND g.origin_op<>'STAGE' AND l.state='ACTIVE' AND l.name NOT LIKE '影子%' ORDER BY l.joined_sequence DESC,g.genome_key LIMIT 1",null)){
            if(c.moveToFirst()){parent=c.getString(0);text=c.getString(1);}
        }
        if(parent==null){waitState(entry,st,"WAIT_PARENT",seq,source);return;}
        CommitteeSU2 parentProgram;
        try{parentProgram=CommitteeSU2.parse(text);}catch(IllegalArgumentException bad){waitState(entry,st,"WAIT_LEGAL_PARENT",seq,source);return;}
        if(!parentProgram.legalReadoutR26()||!parentProgram.key().equals(parent)){waitState(entry,st,"WAIT_CURRENT_RULE_PARENT",seq,source);return;}
        String childText=UpgradeDiagnosisSU2.shadowTextR335(finding.presc,text);
        if(childText==null){waitState(entry,st,"WAIT_PARENT_ALREADY_HAS_MEMBER_OR_UNAVAILABLE_REWRITE",seq,source);return;}
        CommitteeSU2 child=CommitteeSU2.parse(childText);
        if(!child.legalReadoutR26()||child.key().equals(parent)){waitState(entry,st,"WAIT_DISTINCT_LEGAL_CHILD",seq,source);return;}
        try(Cursor c=db.rawQuery("SELECT state FROM su2_model_genome WHERE genome_key=?",new String[]{child.key()})){
            if(c.moveToFirst()){waitState(entry,st,"RETIRED".equals(c.getString(0))?"CHILD_RETIRED_NO_RESURRECTION":"CHILD_ALREADY_OWNED_NO_NEW_BIRTH",seq,source);return;}
        }
        String caseId=HashingSU1.fields(VERSION,entry.key(),String.valueOf(entry.since),entry.presc,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
        String campaign="DIAG_S5:"+caseId;
        Map<String,Object> r=new LinkedHashMap<>();r.put("version",VERSION);r.put("caseId",caseId);r.put("issueKey",entry.key());r.put("since",entry.since);r.put("prescription",entry.presc);r.put("birthSequence",seq);
        r.put("parentKey",parent);r.put("parentTextHash",HashingSU1.text(text));r.put("childKey",child.key());r.put("childTextHash",HashingSU1.text(child.text()));r.put("campaign",campaign);
        r.put("trainingRule",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);r.put("sourceCommit",BuildConfig.SOURCE_COMMIT_R26);r.put("source",source.values());r.put("refitOriginalKey",caseId);
        r.put("qualification","DEVELOPMENT_SHADOW_ONLY_NO_REAL_AUTHORITY");r.put("createdAt",System.currentTimeMillis());
        validateReceipt(r,entry);String body=JsonR27.write(r);if(body.getBytes(StandardCharsets.UTF_8).length>MAX_RECEIPT_BYTES-4096)throw new IllegalStateException("R335_S5_RECEIPT_TOO_LARGE");
        storeOriginal(db,r,source);
        ModelStoreSU2.saveCommittee(db,child,"F15",campaign,seq,-9,System.currentTimeMillis());
        db.execSQL("UPDATE su2_model_genome SET parent_key=?,repair_note=? WHERE genome_key=? AND born_campaign=? AND parent_key IS NULL",new Object[]{parent,"S5 receipt "+HashingSU1.text(body),child.key(),campaign});
        ModelStoreSU2.joinLeague(db,"SU2:"+child.key(),"SU2","影子·S5",child.key(),seq);
        db.execSQL("INSERT INTO su2_diag_shadow(issue_key,shadow_key,base_key,since_sequence,state) VALUES(?,?,?,?,'ACTIVE')",new Object[]{entry.key(),child.key(),parent,seq});
        db.execSQL("INSERT INTO su2_lineage(child_key,parent_key,op,campaign_id,note) VALUES(?,?,?,?,?)",new Object[]{child.key(),parent,OP,campaign,body});
        if(!matches(db,r,true))throw new IllegalStateException("R335_S5_IN_TRANSACTION_OBJECT_READBACK");
        st.put("receipt",r);st.put("receiptHash",HashingSU1.text(body));waitState(entry,st,"RECEIPT_PRESENT_REQUIRES_INDEPENDENT_READBACK",seq,null);
    }
    @SuppressWarnings("unchecked") static Map<String,Object> receipt(Map<String,Object> state){Object r=state.get("receipt");if(r==null)return null;if(!(r instanceof Map))throw new IllegalStateException("R335_S5_RECEIPT_TYPE");Map<String,Object> m=(Map<String,Object>)r;String actual=HashingSU1.text(JsonR27.write(m));if(state.containsKey("receiptHash")&&!actual.equals(state.get("receiptHash")))throw new IllegalStateException("R335_S5_LEDGER_HASH");return m;}
    @SuppressWarnings("unchecked") static Map<String,Object> lineageReceipt(SQLiteDatabase db,String child,String parent){
        try(Cursor c=db.rawQuery("SELECT op,note FROM su2_lineage WHERE child_key=? AND parent_key=?",new String[]{child,parent})){
            if(!c.moveToFirst())return null;if(!OP.equals(c.getString(0)))return null;Object r=JsonR27.parse(c.getString(1));if(!(r instanceof Map))throw new IllegalStateException("R335_S5_LINEAGE_RECEIPT_TYPE");return (Map<String,Object>)r;
        }
    }
    static void restoreReceiptCopy(SQLiteDatabase db,Map<String,Object> r){
        String body=JsonR27.write(r);String child=string(r,"childKey"),parent=string(r,"parentKey");
        try(Cursor c=db.rawQuery("SELECT op,campaign_id,note FROM su2_lineage WHERE child_key=? AND parent_key=?",new String[]{child,parent})){
            if(c.moveToFirst()){if(!OP.equals(c.getString(0))||!string(r,"campaign").equals(c.getString(1))||!body.equals(c.getString(2)))throw new IllegalStateException("R335_S5_LINEAGE_RECEIPT_CONFLICT");return;}
        }
        db.execSQL("INSERT INTO su2_lineage(child_key,parent_key,op,campaign_id,note) VALUES(?,?,?,?,?)",new Object[]{child,parent,OP,string(r,"campaign"),body});
    }
    static String[] shadow(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT shadow_key,base_key,state,since_sequence FROM su2_diag_shadow WHERE issue_key='S5|'",null)){return c.moveToFirst()?new String[]{c.getString(0),c.getString(1),c.getString(2),String.valueOf(c.getLong(3))}:null;}}
    static void validateReceipt(Map<String,Object> r,UpgradeDiagnosisSU2.Entry e){
        if(!VERSION.equals(r.get("version"))||!e.key().equals(r.get("issueKey"))||number(r.get("since"),-1)!=e.since||!e.presc.equals(r.get("prescription"))||number(r.get("birthSequence"),-1)<e.since)throw new IllegalStateException("R335_S5_CASE_MISMATCH");
        if(!ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256.equals(r.get("trainingRule"))||!BuildConfig.SOURCE_COMMIT_R26.equals(r.get("sourceCommit")))throw new IllegalStateException("R335_S5_RULE_OR_SOURCE_MISMATCH");
        String expected=HashingSU1.fields(VERSION,e.key(),String.valueOf(e.since),e.presc,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
        if(!expected.equals(r.get("caseId"))||!("DIAG_S5:"+expected).equals(r.get("campaign")))throw new IllegalStateException("R335_S5_CASE_HASH");
        for(String key:new String[]{"parentKey","parentTextHash","childKey","childTextHash"})HashingSU1.sha(string(r,key));
        if(r.get("parentKey").equals(r.get("childKey"))||!(r.get("source") instanceof Map))throw new IllegalStateException("R335_S5_DISTINCT_SOURCE_REQUIRED");
        Map<?,?> source=(Map<?,?>)r.get("source");
        if(number(source.get("relativeRefitCursor"),-1)<0||number(source.get("observedRefitsSinceEvidence"),0)<1)throw new IllegalStateException("R335_S5_MEASURED_REFIT_REQUIRED");
        if(!expected.equals(r.get("refitOriginalKey")))throw new IllegalStateException("R335_S5_REFIT_LOCATOR");
        HashingSU1.sha(String.valueOf(source.get("refitOriginalHash")));HashingSU1.sha(String.valueOf(source.get("checkpointHash")));HashingSU1.sha(String.valueOf(source.get("frameHash")));
        if(number(source.get("sourceGeneration"),-1)<0||number(source.get("throughSequence"),-1)<0||!(source.get("epoch") instanceof String)||((String)source.get("epoch")).isEmpty())throw new IllegalStateException("R335_S5_SOURCE_IDENTITY");
    }
    static boolean matches(SQLiteDatabase db,Map<String,Object> r,boolean active){
        String child=string(r,"childKey"),parent=string(r,"parentKey");long seq=number(r.get("birthSequence"),-1);
        try(Cursor c=db.rawQuery("SELECT canonical_text,state,born_campaign,born_sequence,parent_key FROM su2_model_genome WHERE genome_key=?",new String[]{child})){
            if(!c.moveToFirst()||!HashingSU1.text(c.getString(0)).equals(r.get("childTextHash"))||active&&!"LEAGUE".equals(c.getString(1))||!string(r,"campaign").equals(c.getString(2))||seq!=c.getLong(3)||!parent.equals(c.getString(4)))return false;
            CommitteeSU2 parsed=CommitteeSU2.parse(c.getString(0));if(!parsed.legalReadoutR26()||!child.equals(parsed.key()))return false;
        }
        try(Cursor c=db.rawQuery("SELECT canonical_policy,genotype_text,numerical_abi FROM r26_policy_identity WHERE policy_hash=?",new String[]{child})){if(!c.moveToFirst()||!child.equals(PolicyIdentityR26.sha256(c.getString(0)))||!HashingSU1.text(c.getString(1)).equals(r.get("childTextHash"))||!ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256.equals(c.getString(2)))return false;}
        try(Cursor c=db.rawQuery("SELECT canonical_text FROM su2_model_genome WHERE genome_key=?",new String[]{parent})){if(!c.moveToFirst()||!HashingSU1.text(c.getString(0)).equals(r.get("parentTextHash")))return false;}
        try(Cursor c=db.rawQuery("SELECT genome_key,joined_sequence,state,kind FROM su2_league WHERE member_id=?",new String[]{"SU2:"+child})){if(!c.moveToFirst()||!child.equals(c.getString(0))||seq!=c.getLong(1)||active&&!"ACTIVE".equals(c.getString(2))||!"SU2".equals(c.getString(3)))return false;}
        String[] s=shadow(db);return s!=null&&child.equals(s[0])&&parent.equals(s[1])&&String.valueOf(seq).equals(s[3])&&(!active||"ACTIVE".equals(s[2]));
    }
    /** Caller must use a reader after the original write has returned, never its
     * prepared Entry. Both immutable copies and all actual objects are re-read. */
    public static Map<String,Object> readback(SQLiteDatabase db){
        if(db.inTransaction())throw new IllegalStateException("R335_S5_INDEPENDENT_READER_REQUIRED");
        String text;try(Cursor c=db.rawQuery("SELECT ledger_text FROM su2_diag_state WHERE singleton=1",null)){if(!c.moveToFirst())return result("NO_PERSISTED_LEDGER",false);text=c.getString(0);}
        return readbackLedger(db,text);
    }
    /** Same immutable report snapshot may already be in a read transaction. */
    static Map<String,Object> readbackLedger(SQLiteDatabase db,String text){
        UpgradeDiagnosisSU2.Entry e=UpgradeDiagnosisSU2.Ledger.fromText(text,null).entries.get("S5|");if(e==null)return result("NO_S5_CASE",false);
        Map<String,Object> st=state(e),r=receipt(st);if(r==null){Map<String,Object> out=result(String.valueOf(st.getOrDefault("state","NO_ORIGINAL_RECEIPT")),false);out.put("pending",st);return out;}
        validateReceipt(r,e);
        if(!hasOriginal(db,r))return result("ORIGINAL_REFIT_MISSING_RESTORE_EXACT_HASH_REQUIRED",false);
        verifyOriginal(db,r);
        Map<String,Object> copy=lineageReceipt(db,string(r,"childKey"),string(r,"parentKey"));
        if(copy==null)return result("ORIGINAL_LINEAGE_RECEIPT_MISSING_RETRY_SAME_CASE",false);
        if(!JsonR27.write(copy).equals(JsonR27.write(r)))throw new IllegalStateException("R335_S5_RECEIPT_COPIES_CONFLICT");
        boolean structural=matches(db,r,false),active=structural&&matches(db,r,true);Map<String,Object> out=result(active?"DURABLE_RECEIPT_AND_ACTIVE_OBJECTS_VERIFIED":structural?"RECEIVED_THEN_RETIRED":"OBJECT_ORIGINAL_MISSING_OR_CHANGED",active);
        out.put("receiptHash",HashingSU1.text(JsonR27.write(r)));out.put("receipt",r);return out;
    }
    static Map<String,Object> result(String state,boolean ack){Map<String,Object> r=new LinkedHashMap<>();r.put("state",state);r.put("ack",ack);r.put("qualificationGranted",false);return r;}
    static long number(Object x,long missing){if(!(x instanceof Number))return missing;double v=((Number)x).doubleValue();return Double.isNaN(v)||Double.isInfinite(v)||v!=Math.rint(v)?missing:((Number)x).longValue();}
    static String string(Map<?,?> m,String key){Object x=m.get(key);if(!(x instanceof String))throw new IllegalStateException("R335_S5_STRING:"+key);return (String)x;}
    @SuppressWarnings("unchecked") static Map<String,Object> mapSource(Map<String,Object> r){return (Map<String,Object>)r.get("source");}
    static boolean hasOriginal(SQLiteDatabase db,Map<String,Object> r){
        try(Cursor c=db.rawQuery("SELECT original_hash,checkpoint_hash FROM r335_s5_refit_original WHERE case_id=?",new String[]{string(r,"refitOriginalKey")})){
            if(!c.moveToFirst())return false;
            if(!mapSource(r).get("refitOriginalHash").equals(c.getString(0))||!mapSource(r).get("checkpointHash").equals(c.getString(1)))throw new IllegalStateException("R335_S5_ORIGINAL_IDENTITY_CONFLICT");return true;
        }
    }
    static void storeOriginal(SQLiteDatabase db,Map<String,Object> r,Source source){
        if(!source.refitHash.equals(mapSource(r).get("refitOriginalHash")))throw new IllegalStateException("R335_S5_ORIGINAL_RECOVERY_HASH");
        db.execSQL("INSERT OR IGNORE INTO r335_s5_refit_original(case_id,original_hash,checkpoint_hash,raw_bytes,compressed,compressed_hash,created_at) VALUES(?,?,?,?,?,?,?)",new Object[]{string(r,"refitOriginalKey"),source.refitHash,mapSource(r).get("checkpointHash"),source.refitRawBytes,source.refitGzip,HashingSU1.bytes(source.refitGzip),number(r.get("createdAt"),0)});
        try(Cursor c=db.rawQuery("SELECT original_hash,checkpoint_hash,raw_bytes,length(compressed),compressed_hash FROM r335_s5_refit_original WHERE case_id=?",new String[]{string(r,"refitOriginalKey")})){
            if(!c.moveToFirst()||!source.refitHash.equals(c.getString(0))||!mapSource(r).get("checkpointHash").equals(c.getString(1))||source.refitRawBytes!=c.getLong(2)||source.refitGzip.length!=c.getLong(3)||!HashingSU1.bytes(source.refitGzip).equals(c.getString(4)))throw new IllegalStateException("R335_S5_ORIGINAL_WRITE_CONFLICT");
        }
    }
    static void verifyOriginal(SQLiteDatabase db,Map<String,Object> r){
        String key=string(r,"refitOriginalKey");long length,rawLength;String compressedHash;
        try(Cursor c=db.rawQuery("SELECT raw_bytes,length(compressed),compressed_hash FROM r335_s5_refit_original WHERE case_id=?",new String[]{key})){
            if(!c.moveToFirst())throw new IllegalStateException("R335_S5_ORIGINAL_MISSING");rawLength=c.getLong(0);length=c.getLong(1);compressedHash=c.getString(2);
        }
        if(length<1||length>DiagnosisSchemaR335.MAX_GZIP_BYTES||rawLength<1||rawLength>DiagnosisSchemaR335.MAX_RAW_BYTES)throw new IllegalStateException("R335_S5_ORIGINAL_BOUNDS");
        java.io.ByteArrayOutputStream chunks=new java.io.ByteArrayOutputStream((int)length);
        for(int offset=0;offset<length;offset+=65536){try(Cursor c=db.rawQuery("SELECT substr(compressed,?,?) FROM r335_s5_refit_original WHERE case_id=?",new String[]{String.valueOf(offset+1),String.valueOf(Math.min(65536,length-offset)),key})){
            if(!c.moveToFirst())throw new IllegalStateException("R335_S5_ORIGINAL_DISAPPEARED");byte[] part=c.getBlob(0);if(part==null||part.length!=Math.min(65536,length-offset))throw new IllegalStateException("R335_S5_ORIGINAL_CHUNK_MISSING");chunks.write(part,0,part.length);
        }}
        byte[] compressed=chunks.toByteArray();if(!HashingSU1.bytes(compressed).equals(compressedHash))throw new IllegalStateException("R335_S5_COMPRESSED_HASH");
        byte[] raw=inflate(compressed);if(raw.length!=rawLength||!HashingSU1.bytes(raw).equals(mapSource(r).get("refitOriginalHash")))throw new IllegalStateException("R335_S5_ORIGINAL_BODY_HASH");
    }
    static byte[] inflate(byte[] compressed){
        try(java.util.zip.GZIPInputStream in=new java.util.zip.GZIPInputStream(new java.io.ByteArrayInputStream(compressed));java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()){
            byte[] block=new byte[8192];int n;while((n=in.read(block))!=-1){if(out.size()>DiagnosisSchemaR335.MAX_RAW_BYTES-n)throw new IOException("R335_REFIT_INFLATE_BOUND");out.write(block,0,n);}return out.toByteArray();
        }catch(IOException bad){throw new IllegalStateException("R335_S5_ORIGINAL_DECODE",bad);}
    }
}
