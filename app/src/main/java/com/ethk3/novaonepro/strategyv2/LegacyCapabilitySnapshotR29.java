package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/** A capability export references the complete original immutable state, not a guessed R27 approximation.
 * The realtime owner pins the CAS root before research decodes it. The shared artifact domain retains every child
 * (weights, optimizer, VM memory, calibrators and stage definitions) through the existing reachability GC.
 * Each source numerical ABI gets one immutable export; new training does not create an unbounded archive per step. */
public final class LegacyCapabilitySnapshotR29 implements ResearchPoolR27.Task,AutoCloseable {
    public static final String PIN="CREATE TABLE IF NOT EXISTS r29_legacy_checkpoint_pin(numerical_abi TEXT PRIMARY KEY,checkpoint_hash TEXT NOT NULL,state_hash TEXT NOT NULL,state_ref BLOB NOT NULL,through_ordinal INTEGER NOT NULL,genesis_ordinal INTEGER NOT NULL,source_cursor INTEGER NOT NULL,roster_hash TEXT NOT NULL,origin_json TEXT NOT NULL,pinned_at INTEGER NOT NULL)";
    public static final String SNAPSHOT="CREATE TABLE IF NOT EXISTS r29_legacy_capability_snapshot(numerical_abi TEXT PRIMARY KEY,checkpoint_hash TEXT NOT NULL,state_hash TEXT NOT NULL,state_ref BLOB NOT NULL,through_ordinal INTEGER NOT NULL,descriptor_json TEXT NOT NULL,verified_at INTEGER NOT NULL)";
    public static final String[] GUARDS={
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_pin_no_update BEFORE UPDATE ON r29_legacy_checkpoint_pin BEGIN SELECT RAISE(ABORT,'R29_LEGACY_PIN_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_pin_no_delete BEFORE DELETE ON r29_legacy_checkpoint_pin BEGIN SELECT RAISE(ABORT,'R29_LEGACY_PIN_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_snapshot_no_update BEFORE UPDATE ON r29_legacy_capability_snapshot BEGIN SELECT RAISE(ABORT,'R29_LEGACY_EXPORT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r29_legacy_snapshot_no_delete BEFORE DELETE ON r29_legacy_capability_snapshot BEGIN SELECT RAISE(ABORT,'R29_LEGACY_EXPORT_IMMUTABLE'); END"
    };
    static final class Pinned{
        String abi,checkpoint,hash,roster,origin;byte[] reference;long through,genesis,cursor;
    }
    private final DbStore store;private final String session;
    private CooperativeContinuationR26<Map<String,Object>> decode;
    private Pinned pending;
    LegacyCapabilitySnapshotR29(DbStore store,String session){this.store=store;this.session=session;}
    public String id(){return "r29-legacy-state-export";}
    static Pinned pinned(SQLiteDatabase db,String abi){
        try(Cursor c=db.rawQuery("SELECT numerical_abi,checkpoint_hash,state_hash,state_ref,through_ordinal,genesis_ordinal,source_cursor,roster_hash,origin_json FROM r29_legacy_checkpoint_pin WHERE numerical_abi=?",new String[]{abi})){
            if(!c.moveToFirst())return null;Pinned p=new Pinned();p.abi=c.getString(0);p.checkpoint=c.getString(1);p.hash=c.getString(2);p.reference=c.getBlob(3);p.through=c.getLong(4);p.genesis=c.getLong(5);p.cursor=c.getLong(6);p.roster=c.getString(7);p.origin=c.getString(8);return p;
        }
    }
    static void pinCurrentWithin(SQLiteDatabase realtime,String abi){
        if(!realtime.inTransaction())throw new IllegalStateException("R29_LEGACY_PIN_TRANSACTION");
        try(Cursor c=realtime.rawQuery("SELECT numerical_abi,checkpoint_hash,state_hash,state,through_ordinal,genesis_ordinal,cursor,roster_hash,snapshot_hash,parent_hash,source_through FROM r26_legacy_runtime WHERE singleton=1 AND numerical_abi=? AND length(state)>0 AND through_ordinal>=genesis_ordinal",new String[]{abi})){
            if(!c.moveToFirst())return;
            Map<String,Object> origin=new LinkedHashMap<>();origin.put("snapshotHash",c.getString(8));origin.put("parentCheckpoint",c.getString(9));origin.put("sourceThrough",c.getLong(10));String minted=c.getString(7);
            try(Cursor a=realtime.rawQuery("SELECT minted_roster FROM r27_legacy_roster_alias WHERE checkpoint_hash=?",new String[]{c.getString(1)})){if(a.moveToFirst())minted=a.getString(0);}
            origin.put("mintedRoster",minted);
            realtime.execSQL("INSERT OR IGNORE INTO r29_legacy_checkpoint_pin VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{c.getString(0),c.getString(1),c.getString(2),c.getBlob(3),c.getLong(4),c.getLong(5),c.getLong(6),c.getString(7),JsonR27.write(origin),System.currentTimeMillis()});
        }
    }
    static LegacyReplayStateR26 restore(CheckpointArtifactsR26 artifacts,Pinned p)throws IOException{
        LegacyReplayStateR26 state=artifacts.restoreLegacy(p.reference,p.hash);
        if(state.through()!=p.through||state.genesis!=p.genesis)throw new IOException("R29_LEGACY_EXPORT_CURSOR_CONFLICT");
        @SuppressWarnings("unchecked") Map<String,Object> origin=(Map<String,Object>)JsonR27.parse(p.origin);
        String source=(String)origin.get("snapshotHash"),parent=(String)origin.get("parentCheckpoint"),minted=(String)origin.get("mintedRoster");
        String expected=parent.isEmpty()?HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",minted,source,"0",p.hash):HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",minted,source,parent,Long.toString(p.through),p.hash);
        if(!expected.equals(p.checkpoint))throw new IOException("R29_LEGACY_EXPORT_CHECKPOINT_IDENTITY_CONFLICT");
        return state;
    }
    static Map<String,Object> describe(LegacyReplayStateR26 state,Pinned p){
        Map<String,Object> out=new LinkedHashMap<>();out.put("schema","R29_EXECUTABLE_LEGACY_STATE_1");out.put("checkpoint",p.checkpoint);out.put("sourceNumericalAbi",p.abi);out.put("currentOperatorAbi",ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256);
        out.put("stateHash",p.hash);out.put("stateCodec","R26_LEGACY_STATE_0x264c5301_AND_CHUNK_CAS_0x26435302");out.put("throughOrdinal",state.through());out.put("genesisOrdinal",state.genesis);out.put("retainedHistoricalSamples",state.samples());out.put("sourceCursor",p.cursor);out.put("canonicalPrograms",new ArrayList<>(state.canonical));out.put("rosterHash",p.roster);
        out.put("origin",JsonR27.parse(p.origin));out.put("scope","COMPLETE_R26_REPLAY_RUNTIME_STATE");out.put("externalDependencies",Arrays.asList("original operator classes bound by currentOperatorAbi","original R908 teacher asset research/r967_history/R908_125_algorithms.tsv retained in APK"));
        out.put("executableConsumers",Arrays.asList("LegacyReplayR26","ModelLiveSU2","CommitteeLiveSU2","StageStrategySU2","FamilyRegistryR26"));out.put("preservedState",Arrays.asList("all model weights and optimizer state","global/per-position VM memory","calibration and gate contexts","stage definitions and exact program bindings","temporal cursor and gap context"));
        out.put("operatorAvailability","FULL_ORIGINAL_STATE_DECODED_BY_CURRENT_KERNELS");out.put("semanticEquivalence","SEPARATE_GOLDEN_TRAJECTORY_RECEIPT_REQUIRED");out.put("naturalEvidenceTransferred",false);out.put("newTrainingSamples",0L);return out;
    }
    public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{
        if(!ResearchRouteR29.active(worker.realtimeRo()))return ResearchPoolR27.Outcome.idle("WAITING_VERIFIED_RESEARCH_CUTOVER",0,-1);
        if(decode==null){
            String abi;try(Cursor c=worker.realtimeRo().rawQuery("SELECT numerical_abi FROM r26_legacy_runtime WHERE singleton=1 AND length(state)>0 AND through_ordinal>=genesis_ordinal",null)){if(!c.moveToFirst())return ResearchPoolR27.Outcome.idle("WAITING_RETAINED_LEARNED_LEGACY_CHECKPOINT",0,-1);abi=c.getString(0);}
            try(Cursor c=worker.ro.rawQuery("SELECT 1 FROM r29_legacy_capability_snapshot WHERE numerical_abi=?",new String[]{abi})){if(c.moveToFirst())return ResearchPoolR27.Outcome.idle("NO_NEW_LEGACY_NUMERICAL_ABI",0,0);}
            // A shared artifact lease prevents GC until the durable root pin is committed.
            try(ReportInputsLeaseCP12 lease=ReportInputsLeaseCP12.acquire(store.contextForR26().getFilesDir())){
                store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R29_PIN_EXECUTABLE_LEGACY_STATE",db->{pinCurrentWithin(db,abi);return 0;});
            }
            pending=pinned(worker.realtimeRo(),abi);if(pending==null)return ResearchPoolR27.Outcome.idle("COOPERATIVE_LEGACY_SOURCE_CHANGED_RETRY",0,-1);
            final Pinned source=pending;
            decode=new CooperativeContinuationR26<>("starcore-r29-legacy-state-export",()->describe(restore(CheckpointArtifactsR26.forStore(store),source),source),worker::cancelled,null,()->android.os.SystemClock.currentThreadTimeMillis()*1000000L,session);
        }
        CooperativeContinuationR26.Slice<Map<String,Object>> slice=decode.slice(20);
        if(!slice.done)return ResearchPoolR27.Outcome.idle("COOPERATIVE_LEGACY_STATE_DECODE_UNCOMMITTED",0,-1);
        decode.close();decode=null;Pinned source=pending;pending=null;
        if(slice.error!=null){
            String reason=ResearchPoolR27.stack(slice.error);
            ResearchRouteR29.write(store,"R29_LEGACY_MISSING_DEPENDENCY",db->{db.execSQL("INSERT OR REPLACE INTO r27_legacy_export(source_id,source_table,status,reason,reproduced,compared,exported_at) VALUES(?,'r26_legacy_runtime','RESTORE_BLOCKED_OR_MISSING_DEPENDENCY',?,0,0,?)",new Object[]{"R29_STATE:"+source.abi,reason,System.currentTimeMillis()});return 0;});
            throw new IOException("R29_LEGACY_STATE_RESTORE_BLOCKED:"+source.checkpoint,slice.error);
        }
        String descriptor=JsonR27.write(slice.result);ResearchRouteR29.write(store,"R29_EXECUTABLE_LEGACY_EXPORT",db->{
            db.execSQL("INSERT OR IGNORE INTO r29_legacy_capability_snapshot VALUES(?,?,?,?,?,?,?)",new Object[]{source.abi,source.checkpoint,source.hash,source.reference,source.through,descriptor,System.currentTimeMillis()});return 0;
        });
        return ResearchPoolR27.Outcome.progress(1,0); // one restored-state receipt, not a learning update
    }
    public void close(){if(decode!=null)decode.close();}
}
