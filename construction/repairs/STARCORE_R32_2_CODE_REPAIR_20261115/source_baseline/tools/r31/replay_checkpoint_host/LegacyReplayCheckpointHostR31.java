package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.*;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Synthetic, isolated SQLite/file/process controls of the actual legacy replay owner.
 * Android transports are adapted; no model, replay, checkpoint or transaction body is replaced. */
public final class LegacyReplayCheckpointHostR31 {
    static final Unsafe U; static int checks;
    static {try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void need(boolean ok,String code){checks++;if(!ok)throw new AssertionError(code);}
    static void set(Object o,String name,Object value)throws Exception{Field f=o.getClass().getDeclaredField(name);f.setAccessible(true);f.set(o,value);}
    static long number(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"SCALAR_MISSING");return c.getLong(0);}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"TEXT_MISSING");return c.getString(0);}}
    static byte[] blob(SQLiteDatabase db,String col){try(Cursor c=db.rawQuery("SELECT "+col+" FROM r26_legacy_runtime",null)){need(c.moveToFirst(),"ROW_MISSING");return c.getBlob(0);}}
    static final class World implements AutoCloseable {
        final SQLiteDatabase db;final DbStore store;final RuntimeCoordinatorR26.Claim claim;
        World(Path file)throws Exception{
            Files.createDirectories(file.getParent());db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file));SQLiteOpenHelper.hostDatabase=db;db.execSQL("PRAGMA journal_mode=WAL");
            DbHelper helper=(DbHelper)U.allocateInstance(DbHelper.class);Method base=DbHelper.class.getDeclaredMethod("createBaseTables",SQLiteDatabase.class);base.setAccessible(true);base.invoke(helper,db);
            for(String name:new String[]{"migrateDrawColumns","migrateTradeColumns"}){Method m=DbHelper.class.getDeclaredMethod(name,SQLiteDatabase.class);m.setAccessible(true);m.invoke(helper,db);}
            for(String sql:SqlStatementsSU1.SQL)db.execSQL(sql);for(String sql:StrategySchemaSU2.DDL)db.execSQL(sql);for(String sql:RuntimeSchemaR26.DDL)db.execSQL(sql);for(String sql:SchemaR27.DDL)db.execSQL(sql);for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);for(String sql:ResearchSchemaR30R1.DDL)db.execSQL(sql);
            db.beginTransaction();try{RuntimeSchemaR26.ensure(db);MatrixStoreSU2.ensureMetadata(db);ModelPublicationR31.ensureSchemaR31(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            DatabaseCoordinator coordinator=(DatabaseCoordinator)U.allocateInstance(DatabaseCoordinator.class);Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lock.get(null));set(coordinator,"helper",helper);set(coordinator,"app",new android.app.Application(file.toFile()));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=(DbStore)U.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<String,HashSet<String>>());set(store,"researchAppCP12",new android.app.Application(file.toFile()));
            long fence=number(db,"SELECT coalesce(max(fence),0)+1 FROM r26_task");String owner="R31_REPLAY_SYNTHETIC_"+ProcessHandle.current().pid();
            db.execSQL("INSERT OR REPLACE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',0,0)",new Object[]{owner,IssueTimelineWriterR30.bootId()});
            db.execSQL("INSERT OR REPLACE INTO r26_task(task_id,lane,mechanism,cursor,fence,owner,status) VALUES('legacy-replay','P2','ISOLATED_HOST_CONTROL',0,?,?,'RUNNING')",new Object[]{fence,owner});
            db.execSQL("INSERT OR REPLACE INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms,next_elapsed_ms) VALUES('legacy-replay',?,?,?,?,0)",new Object[]{owner,fence,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+3600000});
            claim=new RuntimeCoordinatorR26.Claim("legacy-replay",owner,fence,0);RuntimeCoordinatorR26.bindBackgroundClaim(claim);
        }
        void fixture()throws Exception{
            long first=TargetIssueResolver.sequenceOrdinalSU1("202611040001");
            for(int i=0;i<64;i++){Object[] values=new Object[13];values[0]=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);for(int p=0;p<10;p++)values[p+1]=1+(p+i*3)%10;values[11]=1L;values[12]=2L;db.execSQL("INSERT OR IGNORE INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,0,?,?)",values);}
            if(number(db,"SELECT count(*) FROM su2_model_genome")!=0)return;
            db.beginTransaction();try{for(String family:new String[]{"F03","F19"}){ModelGenomeSU2 g=FamilyRegistryR26.genome(family).bindBirth(StatsR27.sha256("SYNTHETIC_REPLAY_FIXTURE"),first-1);ModelStoreSU2.saveGenome(db,new ModelEvolutionSU2.Ranked(g,"CONTROLLED_EXISTING_KERNEL","","Synthetic persistence control; no quality"),"synthetic",first-1,.1,.1,"LEAGUE",1);db.execSQL("INSERT INTO su2_league(member_id,kind,name,genome_key,joined_sequence,state) VALUES(?,'SU2','SYNTHETIC',?,?,'ACTIVE')",new Object[]{"SU2:"+g.key(),g.key(),first});}db.setTransactionSuccessful();}finally{db.endTransaction();}
        }
        long step()throws Exception{return LegacyReplayR26.step(store,()->claim,new BudgetTokenSU1(60000,100000000,()->false));}
        LegacyReplayStateR26 state()throws Exception{return CheckpointArtifactsR26.forStore(store).restoreLegacy(blob(db,"state"),text(db,"SELECT state_hash FROM r26_legacy_runtime"));}
        public void close(){SQLiteDatabase.afterFrame=null;SQLiteDatabase.beforeExecR31=null;RuntimeCoordinatorR26.unbindBackgroundClaimR31();LegacyReplayR26.clearWorking();db.close();}
    }
    static String hash(byte[] bytes){return StatsR27.sha256(bytes);}
    static byte[] decisions(LegacyReplayStateR26 state)throws Exception{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream out=new DataOutputStream(bytes);
        for(Map.Entry<String,LiveStrategySU2> e:state.lives.entrySet()){
            ModelLiveSU2.Decision d=e.getValue().decide();out.writeUTF(e.getKey());
            for(Field f:ModelLiveSU2.Decision.class.getDeclaredFields()){if(Modifier.isStatic(f.getModifiers()))continue;f.setAccessible(true);Object v=f.get(d);out.writeUTF(f.getName());out.writeUTF(v instanceof int[]?Arrays.toString((int[])v):v instanceof double[]?Arrays.toString((double[])v):String.valueOf(v));}
            float[][] table=e.getValue().pendingTable(state.samples());if(table!=null)for(float[] row:table)for(float v:row)out.writeFloat(v);
        }
        out.flush();return bytes.toByteArray();
    }
    static void checkpointResult(World w,Path result)throws Exception{
        need("LIVE".equals(text(w.db,"SELECT status FROM r26_legacy_runtime")),"FINISHED_LIVE");need(number(w.db,"SELECT cursor FROM r26_legacy_runtime")==64,"CURSOR_64");LegacyReplayStateR26 state=w.state();need(state.samples()==64,"FULL_NUMERIC_LIFETIME_64");
        FoundryCampaignR26.Snapshot source=FoundryCampaignR26.capture(w.store);LegacyReplayStateR26 golden=new LegacyReplayStateR26(state.canonical,source.ordinal[0]);for(int i=0;i<64;i++)golden.advance(source.draws[i],source.ordinal[i]);need(Arrays.equals(golden.encode(),state.encode()),"FULL_LEARNER_OPTIMIZER_RNG_CALIBRATION_BYTES_EQUAL");
        String stateHash=hash(state.encode()),decisionHash=hash(decisions(state));need(Arrays.equals(decisions(golden),decisions(LegacyReplayStateR26.decode(w.state().encode()))),"ACT_K3_AND_PROBABILITY_EXACT");
        long manifests=number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact"),cursor=number(w.db,"SELECT cursor FROM r26_legacy_runtime");for(int i=0;i<3;i++)need(w.step()==0,"DUPLICATE_DELIVERY_WITHOUT_NEW_LABEL_NO_TRAINING");need(manifests==number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact")&&cursor==number(w.db,"SELECT cursor FROM r26_legacy_runtime"),"DUPLICATE_NO_NEW_PROGRESS_OR_RECEIPT");need(stateHash.equals(hash(w.state().encode())),"DUPLICATE_NO_NUMERICAL_CHANGE");
        Map<String,Object> data=new TreeMap<>();data.put("stateSha256",stateHash);data.put("decisionSha256",decisionHash);data.put("checkpointHash",text(w.db,"SELECT checkpoint_hash FROM r26_legacy_runtime"));data.put("manifests",manifests);data.put("cursor",cursor);data.put("modelSteps",w.state().samples());data.put("checks",checks);Files.writeString(result,JsonR27.write(data));
    }
    static void markerWait(Path marker)throws Exception{Files.writeString(marker,"READY");for(;;)Thread.sleep(1000);}
    static void generationRefusal(World w)throws Exception{
        w.fixture();FoundryCampaignR26.Snapshot original=FoundryCampaignR26.capture(w.store);String source=LiveStateR26.hash(original.encode());int n=original.draws[0][0];original.draws[0][0]=original.draws[0][1];original.draws[0][1]=n;byte[] damaged=original.encode();
        w.db.execSQL("INSERT INTO r26_legacy_campaign(campaign_id,snapshot_hash,snapshot,source_through,source_first,numerical_abi,updated_at) VALUES('synthetic-damaged',?,?,?,?,?,1)",new Object[]{source,damaged,original.through,original.ordinal[0],ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        need(LegacyGenerationR26.step(w.store,w.claim,new BudgetTokenSU1(60000,10000000,()->false))==0,"DAMAGED_GENERATION_SOURCE_NO_PROGRESS");need("BLOCKED_FROZEN_SOURCE_PRESERVED".equals(text(w.db,"SELECT status FROM r26_legacy_campaign WHERE campaign_id='synthetic-damaged'")),"GENERATION_RESUME_MUST_AUTHENTICATE_SOURCE_BEFORE_COMPUTE");
        try(Cursor c=w.db.rawQuery("SELECT snapshot,snapshot_hash FROM r26_legacy_campaign WHERE campaign_id='synthetic-damaged'",null)){need(c.moveToFirst()&&Arrays.equals(damaged,c.getBlob(0))&&source.equals(c.getString(1)),"GENERATION_DAMAGED_ORIGINAL_BYTES_AND_IDENTITY_PRESERVED");}
        need(number(w.db,"SELECT count(*) FROM r26_member_replay")==0&&number(w.db,"SELECT count(*) FROM r31_model_publication")==0,"GENERATION_NO_NUMERIC_WORKSPACE_OR_PUBLICATION_FOR_FALSE_SOURCE");System.out.println("PASS_GENERATION_RESUME_REFUSAL checks="+checks+" scope=ACTUAL_GENERATION_STEP_NOT_ANDROID");
    }
    static void train(World w,String mode,Path directory)throws Exception{
        w.fixture();if(number(w.db,"SELECT count(*) FROM r26_legacy_runtime")==0)need(w.step()==1,"ACTUAL_BOOTSTRAP_BIRTH");
        if(mode.equals("kill-before"))SQLiteDatabase.beforeExecR31=sql->{if(sql.startsWith("UPDATE r26_legacy_runtime SET cursor=")&&number(w.db,"SELECT cursor FROM r26_legacy_runtime")==17)try{markerWait(directory.resolve("ready"));}catch(Exception e){throw new RuntimeException(e);}};
        if(mode.equals("kill-after"))SQLiteDatabase.afterFrame=frame->{if(number(w.db,"SELECT cursor FROM r26_legacy_runtime")==18)try{markerWait(directory.resolve("ready"));}catch(Exception e){throw new RuntimeException(e);}};
        int turns=0;while(!"LIVE".equals(text(w.db,"SELECT status FROM r26_legacy_runtime"))){need(++turns<256,"BOUNDED_REPLAY_PROGRESS");w.step();}
        checkpointResult(w,directory.resolve("result.json"));
    }
    static void refusals(World w)throws Exception{
        w.fixture();w.step();for(int i=0;i<7;i++)w.step();byte[] state=blob(w.db,"state"),snapshot=blob(w.db,"snapshot");String cp=text(w.db,"SELECT checkpoint_hash FROM r26_legacy_runtime"),sh=text(w.db,"SELECT state_hash FROM r26_legacy_runtime");long cursor=number(w.db,"SELECT cursor FROM r26_legacy_runtime"),manifests=number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact");
        for(String kind:new String[]{"snapshot","cursor","source-through","manifest","checkpoint"}){
            if(kind.equals("snapshot")){FoundryCampaignR26.Snapshot changed=FoundryCampaignR26.Snapshot.decode(snapshot);int swap=changed.draws[7][0];changed.draws[7][0]=changed.draws[7][1];changed.draws[7][1]=swap;w.db.execSQL("UPDATE r26_legacy_runtime SET snapshot=?",new Object[]{changed.encode()});}
            if(kind.equals("cursor"))w.db.execSQL("UPDATE r26_legacy_runtime SET cursor=cursor+1");
            if(kind.equals("source-through"))w.db.execSQL("UPDATE r26_legacy_runtime SET source_through=source_through+1");
            if(kind.equals("manifest"))w.db.execSQL("UPDATE r26_checkpoint_artifact SET source_hash='INVALID' WHERE checkpoint_hash=?",new Object[]{cp});
            if(kind.equals("checkpoint"))w.db.execSQL("UPDATE r26_legacy_runtime SET parent_hash='INVALID'");
            need(w.step()==0,"MALFORMED_BINDING_MUST_NOT_LEARN_"+kind);need("BLOCKED_REPLAY_CHECKPOINT_PRESERVED".equals(text(w.db,"SELECT status FROM r26_legacy_runtime")),"MALFORMED_BINDING_MUST_BE_EXPLICIT_"+kind);need(Arrays.equals(state,blob(w.db,"state"))&&sh.equals(text(w.db,"SELECT state_hash FROM r26_legacy_runtime"))&&cp.equals(text(w.db,"SELECT checkpoint_hash FROM r26_legacy_runtime")),"ORIGINAL_ASSET_PRESERVED_"+kind);need(number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact")==manifests,"NO_NEW_MANIFEST_"+kind);
            w.db.execSQL("UPDATE r26_legacy_runtime SET snapshot=?,cursor=?,source_through=?,parent_hash=?,status='BOOTSTRAP'",new Object[]{snapshot,cursor,FoundryCampaignR26.Snapshot.decode(snapshot).through,text(w.db,"SELECT parent_hash FROM r26_checkpoint_artifact WHERE checkpoint_hash='"+cp+"'")});w.db.execSQL("UPDATE r26_checkpoint_artifact SET source_hash=(SELECT snapshot_hash FROM r26_legacy_runtime) WHERE checkpoint_hash=?",new Object[]{cp});
        }
        need(w.step()==1,"RESTORED_AUTHENTIC_SOURCE_RESUMES");need(w.state().samples()==cursor+1,"NEXT_SAMPLE_LEARNED_ONCE_AFTER_REFUSAL");
        long beforeCursor=number(w.db,"SELECT cursor FROM r26_legacy_runtime");byte[] beforeState=blob(w.db,"state");long beforeManifests=number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact");LegacyReplayStateR26 expected=w.state();FoundryCampaignR26.Snapshot source=FoundryCampaignR26.Snapshot.decode(blob(w.db,"snapshot"));expected.advance(source.draws[(int)beforeCursor],source.ordinal[(int)beforeCursor]);
        SQLiteDatabase.beforeExecR31=sql->{if(sql.startsWith("UPDATE r26_legacy_runtime SET cursor=")){SQLiteDatabase.beforeExecR31=null;throw new SQLiteException("CONTROLLED_REPLAY_SQLITE_ROLLBACK");}};
        boolean refused=false;try{w.step();}catch(DatabaseCoordinator.WriteRejectedR31 failure){refused=true;}finally{SQLiteDatabase.beforeExecR31=null;}
        need(refused,"ACTUAL_OWNER_ROLLBACK_EXPOSED");need(beforeCursor==number(w.db,"SELECT cursor FROM r26_legacy_runtime")&&Arrays.equals(beforeState,blob(w.db,"state"))&&beforeManifests==number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact"),"SQLITE_ROLLBACK_PROGRESS_STATE_MANIFEST_ATOMIC");need(w.step()==1&&Arrays.equals(expected.encode(),w.state().encode()),"ROLLBACK_RETRY_NEXT_UPDATE_EXACT");
        beforeCursor=number(w.db,"SELECT cursor FROM r26_legacy_runtime");beforeState=blob(w.db,"state");beforeManifests=number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact");final byte[] changed=blob(w.db,"snapshot").clone();changed[changed.length-1]^=1;
        SQLiteDatabase.beforeExecR31=sql->{if(sql.startsWith("UPDATE r26_legacy_runtime SET cursor=")){SQLiteDatabase.beforeExecR31=null;w.db.execSQL("UPDATE r26_legacy_runtime SET snapshot=?",new Object[]{changed});}};
        refused=false;try{w.step();}catch(DatabaseCoordinator.WriteRejectedR31 failure){refused=true;}finally{SQLiteDatabase.beforeExecR31=null;}
        need(refused,"SOURCE_CHANGED_AFTER_PREPARE_REFUSED_IN_ACTUAL_COMMIT");need(beforeCursor==number(w.db,"SELECT cursor FROM r26_legacy_runtime")&&Arrays.equals(beforeState,blob(w.db,"state"))&&beforeManifests==number(w.db,"SELECT count(*) FROM r26_checkpoint_artifact"),"SOURCE_CAS_ROLLS_BACK_ALL_PROGRESS_AND_RECEIPTS");need(w.step()==1,"SOURCE_CAS_RETRY_FROM_COMMITTED_STATE");
        // Older valid snapshots encoded only ordinals/draws. Authenticate those original
        // bytes as written; do not manufacture historical available/opened timestamps.
        source=FoundryCampaignR26.Snapshot.decode(blob(w.db,"snapshot"));ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream old=new DataOutputStream(bytes);old.writeInt(source.draws.length);for(int i=0;i<source.draws.length;i++){old.writeLong(source.ordinal[i]);for(int n:source.draws[i])old.writeByte(n);}old.flush();byte[] oldBytes=bytes.toByteArray();String oldSource=LiveStateR26.hash(oldBytes),oldStateHash=text(w.db,"SELECT state_hash FROM r26_legacy_runtime"),parent=text(w.db,"SELECT parent_hash FROM r26_legacy_runtime"),roster=text(w.db,"SELECT roster_hash FROM r26_legacy_runtime");long through=number(w.db,"SELECT through_ordinal FROM r26_legacy_runtime");String oldCheckpoint=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1",roster,oldSource,parent,String.valueOf(through),oldStateHash);
        w.db.beginTransaction();try{w.db.execSQL("UPDATE r26_legacy_runtime SET snapshot_hash=?,snapshot=?,checkpoint_hash=?",new Object[]{oldSource,oldBytes,oldCheckpoint});CheckpointManifestR26.record(w.db,oldCheckpoint,"LEGACY:"+roster,oldSource,parent,through,blob(w.db,"state"));w.db.setTransactionSuccessful();}finally{w.db.endTransaction();}
        LegacyReplayR26.clearWorking();expected=w.state();int next=(int)number(w.db,"SELECT cursor FROM r26_legacy_runtime");expected.advance(source.draws[next],source.ordinal[next]);need(w.step()==1&&Arrays.equals(expected.encode(),w.state().encode()),"AUTHENTIC_OLD_SNAPSHOT_CONTINUES_EXACT_WITHOUT_METADATA_REWRITE");need(Arrays.equals(oldBytes,blob(w.db,"snapshot")),"OLD_SOURCE_ORIGINAL_BYTES_PRESERVED");
        for(int window=1;window<=2;window++){
            expected=w.state();long lifetime=expected.samples();expected.onGap();long first=TargetIssueResolver.sequenceOrdinalSU1("202611040001")+window*6000;
            w.db.execSQL("UPDATE r26_legacy_runtime SET status='EXPIRED_RETENTION',snapshot=X''");LegacyReplayR26.clearWorking();
            for(int i=0;i<64;i++){Object[] values=new Object[13];values[0]=TargetIssueResolver.fromSequenceOrdinalSU1(first+i);for(int p=0;p<10;p++)values[p+1]=1+(p+i*7+window)%10;values[11]=1L;values[12]=2L;w.db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,0,?,?)",values);}
            need(w.step()==1,"ACTUAL_WARM_RESUME_STRICT_LATER_SOURCE_"+window);need(w.state().samples()==lifetime&&Arrays.equals(expected.encode(),w.state().encode()),"EXPIRED_SOURCE_PRESERVES_COMPLETE_NUMERICS_"+window);FoundryCampaignR26.Snapshot later=FoundryCampaignR26.Snapshot.decode(blob(w.db,"snapshot"));need(later.ordinal[0]==first&&later.draws.length==64,"NO_REUSED_OR_EXPOSED_OLD_LABELS_"+window);expected.advance(later.draws[0],later.ordinal[0]);need(w.step()==1&&Arrays.equals(expected.encode(),w.state().encode()),"WARM_NEXT_UPDATE_EXACT_"+window);
        }
        System.out.println("PASS_REPLAY_REFUSALS checks="+checks+" bindingDamageControls=5 rollback=1 sourceCas=1 oldFormat=1 warmWindows=2 scope=ACTUAL_REPLAY_SQLITE_FILES_NOT_ANDROID");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");String mode=args[0];Path directory=Paths.get(args[1]);Files.createDirectories(directory);try(World w=new World(directory.resolve("fixture.db"))){if(mode.equals("refusals"))refusals(w);else if(mode.equals("generation-refusal"))generationRefusal(w);else train(w,mode,directory);}System.out.println("PASS_REPLAY_PROCESS checks="+checks+" scope=ACTUAL_REPLAY_SQLITE_FILES_NOT_ANDROID");
    }
}
