package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Immutable, original-input recovery. Formal rows are retained with their original split, never optimized. */
public final class ResearchInputPinR30R1 {
    private ResearchInputPinR30R1() {}
    public static final String STATUS="INPUT_DATASET_PINNED", VERSION="R30_REPAIR_INPUT_PIN1";
    public static String id(String dataset,long revision){return StatsR27.sha256(VERSION+'|'+ResearchContractR30R1.sha(dataset)+'|'+revision);}
    public static final class Pin {public final ReplayDatasetR27 data;public final long revision;Pin(ReplayDatasetR27 d,long r){data=d;revision=r;}}
    static String ordinal(String row){String s=row+".issue";return "(CAST(julianday(substr("+s+",1,4)||'-'||substr("+s+",5,2)||'-'||substr("+s+",7,2))-julianday('2020-01-01') AS INTEGER)*1440+CAST(substr("+s+",9,4) AS INTEGER))";}
    static String[] triggerSql(){return ResearchSourceJournalR31.sourceTriggerSqlR31();}
    /** Compatibility migration entry; the existing Domain owns Critical trigger cutover/restamping. */
    public static void ensure(SQLiteDatabase db){com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31SourceJournal(db);}
    public static boolean triggersValid(SQLiteDatabase db){return ResearchSourceJournalR31.readySchemaR31(db);}
    public static void requireReady(SQLiteDatabase db){if(!triggersValid(db))throw new com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31("RESEARCH_INPUT_PIN","R31_SOURCE_JOURNAL");}
    public static String validRunSql(String alias){ResearchSourceJournalR31.alias(alias);return "NOT EXISTS(SELECT 1 FROM r30r1_research_measure invalid WHERE invalid.run_id="+alias+".run_id AND invalid.kind='INPUT_INVALIDATION') AND NOT ("+ResearchSourceJournalR31.affectedSql(alias)+")";}
    public static boolean runValid(SQLiteDatabase db,String run){requireReady(db);try(Cursor q=db.rawQuery("SELECT r.status,r.contract_hash,"+ResearchSourceJournalR31.deleteTouchesSql("r")+" FROM r30r1_analysis_run r WHERE r.run_id=? AND ("+validRunSql("r")+") LIMIT 1",new String[]{run})){if(!q.moveToFirst())return false;return !STATUS.equals(q.getString(0))||q.getInt(2)==0||originalRecoveryValid(db,run,q.getString(1));}}
    public static void requireRun(SQLiteDatabase db,String run){if(!runValid(db,run))throw new ResearchStoreR30R1.StaleResearch();}
    public static boolean invalid(SQLiteDatabase db,String hash){requireReady(db);String run=null,contract=null;boolean invalid=false,deleted=false;try(Cursor c=db.rawQuery("SELECT p.run_id,p.contract_hash,NOT ("+validRunSql("p")+"),"+ResearchSourceJournalR31.deleteTouchesSql("p")+" FROM r30r1_analysis_run p WHERE p.dataset_hash=? AND p.status='INPUT_DATASET_PINNED' ORDER BY p.source_revision DESC LIMIT 1",new String[]{hash})){if(c.moveToFirst()){run=c.getString(0);contract=c.getString(1);invalid=c.getInt(2)!=0;deleted=c.getInt(3)!=0;}}
        if(invalid||run==null||!deleted)return invalid;
        // A physical rolling-history deletion cannot destroy the complete original snapshot.
        // Verify that evidence outside a write transaction; the hot writer only checks its immutable proof.
        return !originalRecoveryValid(db,run,contract);
    }
    private static boolean originalRecoveryValid(SQLiteDatabase db,String run,String contract){if(PinSnapshotProofR31.known(db,run,contract))return true;if(db.inTransaction())throw new com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31("RESEARCH_INPUT_PIN","PIN_ORIGINAL_RECOVERY_VERIFY");try(Cursor q=db.rawQuery("SELECT dataset_hash,source_revision,summary_json,contract_hash FROM r30r1_analysis_run WHERE run_id=? AND status=?",new String[]{run,STATUS})){if(!q.moveToFirst()||!contract.equals(q.getString(3)))return false;decode(q.getString(0),q.getLong(1),q.getString(2),contract);PinSnapshotProofR31.verified(db,run,contract);return true;}catch(RuntimeException broken){Error fatal=FailureDispositionR31.processFatalCause(broken);if(fatal!=null)throw fatal;return false;}}
    public static Pin loadCurrent(SQLiteDatabase db,String hash){Pin pin=load(db,hash);if(pin==null||invalid(db,hash))return null;try{require(db,hash,pin.revision);}catch(ResearchStoreR30R1.StaleResearch changed){return null;}return pin;}
    /** Legacy evidence without a pin retains its original conservative global-revision guard. New runtime always pins. */
    public static long revision(SQLiteDatabase db,String dataset){try(Cursor c=db.rawQuery("SELECT source_revision FROM r30r1_analysis_run WHERE dataset_hash=? AND status=? ORDER BY source_revision DESC LIMIT 1",new String[]{dataset,STATUS})){if(c.moveToFirst()){if(invalid(db,dataset)||!triggersValid(db))throw new ResearchStoreR30R1.StaleResearch();return c.getLong(0);}}return SchemaR27.liveSourceRevision(db);}
    public static void require(SQLiteDatabase db,String dataset,long revision){if(!SchemaR27.liveSourceTriggersValid(db)||revision(db,dataset)!=revision)throw new ResearchStoreR30R1.StaleResearch();}
    public static void append(ResearchWriteQueueR27.Batch b,ReplayDatasetR27 d,long sourceRevision,long now){
        if(d.size()==0||d.size()>10000||sourceRevision<0)throw new IllegalArgumentException("PIN_INPUT_BOUNDARY");
        PinSnapshotProofR31.prepare();Map<String,Object> payload=new LinkedHashMap<>();StringBuilder raw=new StringBuilder();for(int i=0;i<d.size();i++){if(!ReplayDatasetR27.legal(d.draws[i])||!TargetIssueResolver.isValid(d.issues[i])||TargetIssueResolver.sequenceOrdinalSU1(d.issues[i])!=d.ordinals[i])throw new IllegalArgumentException("PIN_CANONICAL_ISSUE");raw.append(d.issues[i]).append(':');for(int n:d.draws[i])raw.append((char)('a'+n-1));raw.append('\n');}
        String encoded;try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(GZIPOutputStream zip=new GZIPOutputStream(bytes)){zip.write(raw.toString().getBytes(StandardCharsets.UTF_8));}encoded=Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException e){throw new IllegalStateException(e);}
        SplitR27 split=SplitR27.of(d,null);payload.put("schema",VERSION);payload.put("datasetHash",d.datasetHash);payload.put("rows",d.size());payload.put("compressedOriginalRows",encoded);payload.put("rawHash",StatsR27.sha256(raw.toString()));payload.put("split",split.describe());payload.put("trainTo",split.trainTo);payload.put("selectTo",split.selectTo);payload.put("holdoutTo",split.holdoutTo);payload.put("formalRowsRole","ORIGINAL_CONFIRM_AND_OPEN_RETAINED_FOR_RECOVERY_ONLY_NOT_SHARED_OPTIMIZATION");payload.put("firstIssue",d.issues[0]);payload.put("lastIssue",d.issues[d.size()-1]);
        String json=JsonR27.write(payload),hash=StatsR27.sha256(json);decode(d.datasetHash,sourceRevision,json,hash); // Validate the frozen bytes/roles outside the writer, not mutable caller arrays.
        b.guardBefore(db->{if(SchemaR27.liveSourceRevision(db)!=sourceRevision||!triggersValid(db))throw new ResearchStoreR30R1.StaleResearch();return 0;});
        b.add("INSERT OR IGNORE INTO r30r1_analysis_run SELECT ?,?,?,?,?,?,?,?,?,?,?,? WHERE NOT EXISTS(SELECT 1 FROM r30r1_analysis_run p WHERE p.dataset_hash=? AND p.status='INPUT_DATASET_PINNED' AND ("+validRunSql("p")+"))",id(d.datasetHash,sourceRevision),d.datasetHash,StatsR27.sha256("ORIGINAL_DATASET_INPUT"),"SELECT",BuildConfig.SOURCE_COMMIT_R26,sourceRevision,d.ordinals[0],d.ordinals[d.size()-1],hash,STATUS,json,now,d.datasetHash);
        b.guardAfter(db->{if(SchemaR27.liveSourceRevision(db)!=sourceRevision)throw new ResearchStoreR30R1.StaleResearch();try(Cursor c=db.rawQuery("SELECT contract_hash,summary_json,run_id FROM r30r1_analysis_run WHERE dataset_hash=? AND status='INPUT_DATASET_PINNED' ORDER BY source_revision DESC LIMIT 1",new String[]{d.datasetHash})){if(!c.moveToFirst()||!hash.equals(c.getString(0))||!json.equals(c.getString(1)))throw new IllegalStateException("PIN_IDENTITY_CONFLICT");PinSnapshotProofR31.verified(db,c.getString(2),hash);if(invalid(db,d.datasetHash))throw new ResearchStoreR30R1.StaleResearch();}return 0;});
    }
    /** Original evidence recovery, including invalidated snapshots; never itself a source-admission gate. */
    public static Pin load(SQLiteDatabase db,String hash){try(Cursor c=db.rawQuery("SELECT source_revision,summary_json,contract_hash,run_id FROM r30r1_analysis_run WHERE dataset_hash=? AND status=? ORDER BY source_revision DESC LIMIT 1",new String[]{hash,STATUS})){if(!c.moveToFirst())return null;Pin pin=decode(hash,c.getLong(0),c.getString(1),c.getString(2));PinSnapshotProofR31.verified(db,c.getString(3),c.getString(2));return pin;}}
    @SuppressWarnings("unchecked") private static Pin decode(String hash,long revision,String json,String contract){
        if(json==null||json.length()>1048576||!StatsR27.sha256(json).equals(contract))throw new IllegalStateException("PIN_PAYLOAD_HASH_OR_BOUNDS");Object parsed=JsonR27.parse(json);if(!(parsed instanceof Map))throw new IllegalStateException("PIN_PAYLOAD_TYPE");Map<String,Object> p=(Map<String,Object>)parsed;
        if(!VERSION.equals(p.get("schema"))||!hash.equals(p.get("datasetHash"))||!"ORIGINAL_CONFIRM_AND_OPEN_RETAINED_FOR_RECOVERY_ONLY_NOT_SHARED_OPTIMIZATION".equals(p.get("formalRowsRole")))throw new IllegalStateException("PIN_ORIGINAL_ROLE_OR_SCHEMA");
        Object encodedValue=p.get("compressedOriginalRows");if(!(encodedValue instanceof String)||((String)encodedValue).length()>1048576)throw new IllegalStateException("PIN_BOUNDS");ByteArrayOutputStream raw=new ByteArrayOutputStream();try(InputStream zip=new GZIPInputStream(new ByteArrayInputStream(Base64.getDecoder().decode((String)encodedValue)))){byte[] buffer=new byte[8192];int n;while((n=zip.read(buffer))!=-1){if(n>1048576-raw.size())throw new IllegalStateException("PIN_RAW_BOUNDS");raw.write(buffer,0,n);}}catch(IOException e){throw new IllegalStateException("PIN_GZIP",e);}
        String text=new String(raw.toByteArray(),StandardCharsets.UTF_8);if(!StatsR27.sha256(text).equals(p.get("rawHash")))throw new IllegalStateException("PIN_RAW_HASH");List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(String line:text.split("\n")){if(rows.size()>=10000||line.length()!=23||line.charAt(12)!=':')throw new IllegalStateException("PIN_ROW");int[] values=new int[10];for(int i=0;i<10;i++)values[i]=line.charAt(13+i)-'a'+1;String issue=line.substring(0,12);rows.add(new ReplayDatasetR27.Row(issue,TargetIssueResolver.sequenceOrdinalSU1(issue),values));}
        ReplayDatasetR27 d=ReplayDatasetR27.of(rows);SplitR27 split=SplitR27.of(d,null);if(d.size()==0||d.rejectedRows!=0||!hash.equals(d.datasetHash)||!(p.get("rows") instanceof Number)||d.size()!=((Number)p.get("rows")).intValue()||!split.describe().equals(p.get("split"))||!boundary(p,"trainTo",split.trainTo)||!boundary(p,"selectTo",split.selectTo)||!boundary(p,"holdoutTo",split.holdoutTo)||!d.issues[0].equals(p.get("firstIssue"))||!d.issues[d.size()-1].equals(p.get("lastIssue")))throw new IllegalStateException("PIN_DATASET_OR_ROLE_HASH");return new Pin(d,revision);
    }
    private static boolean boundary(Map<String,Object> p,String key,int value){return p.get(key) instanceof Number&&((Number)p.get(key)).longValue()==value;}
}
