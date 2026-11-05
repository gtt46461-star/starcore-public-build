package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.*;
import com.ethk3.novaonepro.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import sun.misc.Unsafe;

/** Native persistence mechanics fixtures. All producers, codecs, fences,
 * coordinator transactions and receipts are actual fixed-source production. */
public final class WarmAbiBaselineHostR31 {
    static final Unsafe U;static int assertions;static final String OWNER="PUBLICATION_HOST_OWNER";
    static{try{Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);U=(Unsafe)f.get(null);}catch(Exception e){throw new ExceptionInInitializerError(e);}}
    static void need(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
    static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static String text(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){need(c.moveToFirst(),sql);return c.getString(0);}}
    static final class World implements AutoCloseable {
        final SQLiteDatabase db,ro;final DbStore store;final DatabaseCoordinator coordinator;RuntimeCoordinatorR26.Claim claim;
        World(Path file,String task)throws Exception {
            db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file));SQLiteOpenHelper.hostDatabase=db;SQLiteDatabase.scenario="BOOTSTRAP";db.execSQL("PRAGMA journal_mode=WAL");
            DbHelper helper=(DbHelper)U.allocateInstance(DbHelper.class);Method base=DbHelper.class.getDeclaredMethod("createBaseTables",SQLiteDatabase.class);base.setAccessible(true);base.invoke(helper,db);
            for(String name:new String[]{"migrateDrawColumns","migrateTradeColumns"}){Method m=DbHelper.class.getDeclaredMethod(name,SQLiteDatabase.class);m.setAccessible(true);m.invoke(helper,db);}
            for(String sql:SqlStatementsSU1.SQL)db.execSQL(sql);for(String sql:StrategySchemaSU2.DDL)db.execSQL(sql);for(String sql:RuntimeSchemaR26.DDL)db.execSQL(sql);for(String sql:SchemaR27.DDL)db.execSQL(sql);for(String sql:SchemaR27.R31_WRITE_DDL)db.execSQL(sql);for(String sql:ResearchSchemaR30R1.DDL)db.execSQL(sql);
            for(String[] column:SchemaR27.COLUMNS){boolean exists=false;try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{column[0]})){exists=c.moveToFirst();}if(!exists)continue;boolean present=false;try(Cursor c=db.rawQuery("PRAGMA table_info("+column[0]+")",null)){while(c.moveToNext())present|=column[1].equals(c.getString(1));}if(!present)db.execSQL("ALTER TABLE "+column[0]+" ADD COLUMN "+column[1]+" "+column[2]);}
            db.beginTransaction();try{RuntimeSchemaR26.ensure(db);MatrixStoreSU2.ensureMetadata(db);ModelPublicationR31.ensureSchemaR31(db);ResearchRetentionStoreR30.ensureProtected(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
            coordinator=(DatabaseCoordinator)U.allocateInstance(DatabaseCoordinator.class);Field lock=DatabaseCoordinator.class.getDeclaredField("WRITE_LOCK_CP3");lock.setAccessible(true);set(coordinator,"writeLock",(ReentrantLock)lock.get(null));set(coordinator,"helper",helper);set(coordinator,"app",new android.app.Application(file.toFile()));set(coordinator,"waitingWriters",new AtomicInteger());set(coordinator,"criticalBusinessWaitersR914",new AtomicInteger());
            store=(DbStore)U.allocateInstance(DbStore.class);set(store,"coordinator",coordinator);set(store,"helper",helper);set(store,"columnCache",new HashMap<String,HashSet<String>>());set(store,"researchAppCP12",new android.app.Application(file.toFile()));
            ro=SQLiteDatabase.openDatabase(file.toString(),null,SQLiteDatabase.OPEN_READONLY);bind(task,scalar(db,"SELECT coalesce(max(fence),0)+1 FROM r26_task"));
        }
        void bind(String task,long fence){SQLiteDatabase.scenario="OWNER_CONTROL";db.execSQL("INSERT OR REPLACE INTO r26_session(session_id,user_enabled,boot_id,state,heartbeat,actual_progress) VALUES(?,1,?,'RUNNING',0,0)",new Object[]{OWNER,IssueTimelineWriterR30.bootId()});db.execSQL("INSERT OR REPLACE INTO r26_task(task_id,lane,mechanism,cursor,fence,owner,status) VALUES(?,'R26','PERSISTENCE_FIXTURE',0,?,?,'RUNNING')",new Object[]{task,fence,OWNER});db.execSQL("INSERT OR REPLACE INTO r31_task_lease(task_id,owner_epoch,fence,boot_id,deadline_elapsed_ms,next_elapsed_ms) VALUES(?,?,?,?,?,0)",new Object[]{task,OWNER,fence,IssueTimelineWriterR30.bootId(),android.os.SystemClock.elapsedRealtime()+3600000});claim=new RuntimeCoordinatorR26.Claim(task,OWNER,fence,0);RuntimeCoordinatorR26.bindBackgroundClaim(claim);}
        void legacyFixture(){SQLiteDatabase.scenario="CONTROLLED_FIXTURE";db.execSQL("INSERT OR IGNORE INTO r26_legacy_campaign(campaign_id,snapshot_hash,snapshot,source_through,source_first,numerical_abi,updated_at) VALUES('controlled',?,X'01',3595680,3590681,?,1234567)",new Object[]{StatsR27.sha256("PUBLICATION_CONTROLLED_SOURCE"),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});db.execSQL("INSERT OR IGNORE INTO su2_campaign(campaign_id,through_sequence,dev_start,dev_end,val_start,val_end,generations,population,status,started_at) VALUES('controlled',3595680,1500,2200,2200,3400,8,24,'RUNNING',1234567)");}
        public void close(){SQLiteDatabase.afterFrame=null;SQLiteDatabase.failOnceSqlPrefix=null;RuntimeCoordinatorR26.unbindBackgroundClaimR31();ro.close();db.close();Thread.interrupted();}
    }
    static void bulk(Path file,int n)throws Exception{
        Files.deleteIfExists(file);try(World w=new World(file,"foundry-su2")){
            SQLiteDatabase.scenario="CONTROLLED_EXPIRED_ROSTER_FIXTURE";ModelGenomeSU2 template=FamilyRegistryR26.genome("F11");
            w.db.beginTransaction();try{w.db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES('expired',?,10000,X'','EXPIRED_RETENTION',1,?,1)",new Object[]{StatsR27.sha256("OLD_FROZEN_SOURCE"),ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});for(int i=0;i<n;i++){ModelGenomeSU2 g=template.bindBirth(StatsR27.sha256("CONTROLLED_EXPIRED_SOURCE_"+i),10000);w.db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,operation) VALUES('expired',?,?,?,?,?,?,'READY','PERSISTENCE_FANOUT_FIXTURE')",new Object[]{g.key(),g.text(),i%2==0?16:0,i%2==0?new byte[]{1,2,3}:null,StatsR27.sha256("OLD_STATE_"+i),StatsR27.sha256("OLD_CHECKPOINT_"+i)});}w.db.setTransactionSuccessful();}finally{w.db.endTransaction();}
            int start=SQLiteDatabase.frames.size();SQLiteDatabase.scenario="BASELINE_ACTUAL_FOUNDRY_ABI_PRESERVATION";
            need(FoundryCampaignR26.step(w.store,()->w.claim,new BudgetTokenSU1(60000,10000000,()->false))==0,"NO_HISTORY_MEANS_ONLY_REAL_ABI_CONTROL_PATH");
            need(scalar(w.ro,"SELECT count(*) FROM r26_candidate WHERE status='WAITING_WARM_FORK'")==n/2,"LEARNED_PARENT_ROWS_PRESERVED");need(scalar(w.ro,"SELECT count(*) FROM r30_keep_r26_candidate")==n/2,"ORIGINAL_REMOVALS_ARCHIVED");long maximum=0;for(int i=start;i<SQLiteDatabase.frames.size();i++)maximum=Math.max(maximum,((Number)SQLiteDatabase.frames.get(i).get("totalChangedRowsIncludingTriggersAndAttemptedRollback")).longValue());
            System.out.println("RESULT {\"scope\":\"ACTUAL_FOUNDRY_TO_WARM_PRESERVATION_SQL_NOT_TRAINING\",\"inputCandidates\":"+n+",\"changedRowsIncludingTriggers\":"+maximum+",\"learnedRetained\":"+(n/2)+",\"untrainedArchived\":"+(n/2)+"}");need(maximum>=2L*n,"SOURCE_UNBOUNDED_FANOUT_MUST_BE_ACTUALLY_OBSERVED");throw new AssertionError("BASELINE_UNBOUNDED_WARM_FANOUT_OBSERVED_R31:"+maximum);
        }
    }
    static ModelGenomeSU2 small(){return FamilyRegistryR26.genome("F11");}
    static byte[] kernel(OnlineModelsSU2.Model model)throws Exception{java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream();model.write(new java.io.DataOutputStream(out));return out.toByteArray();}
    static void warmBirth(Path file)throws Exception{
        Files.deleteIfExists(file);try(World w=new World(file,"warm-candidates")){
            long first=TargetIssueResolver.sequenceOrdinalSU1("202610220001"),exposure=first-1,through=first-17;String birth=StatsR27.sha256("CONTROLLED_OLD_WARM_SOURCE"),campaign="OLD_WARM_PARENT";LiveStateR26 parent=new LiveStateR26(small().bindBirth(birth,exposure));for(int i=0;i<16;i++)parent.learn(new int[]{1,2,3,4,5,6,7,8,9,10});byte[] state=parent.encode();String stateHash=LiveStateR26.hash(state),checkpoint=PolicyIdentityR26.sha256("STARCORE_CHECKPOINT_R26_V1|"+parent.genome.key()+"|"+campaign+"||16|"+stateHash);
            SQLiteDatabase.scenario="CONTROLLED_REAL_KERNEL_PARENT_AND_FUTURE_SOURCE";w.db.beginTransaction();try{for(int i=0;i<4096;i++)w.db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time,fetch_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,1,2)",new Object[]{TargetIssueResolver.fromSequenceOrdinalSU1(first+i)});w.db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,X'','EXPIRED_RETENTION',1,?,?)",new Object[]{campaign,birth,exposure,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,first-128});w.db.setTransactionSuccessful();}finally{w.db.endTransaction();}
            try(CheckpointArtifactsR26.Prepared fileState=CheckpointArtifactsR26.prepareLease(w.store,state)){w.db.beginTransaction();try{w.db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,status,operation) VALUES(?,?,?,16,?,?,?,'WAITING_WARM_FORK','ACTUAL_SMALL_KERNEL_FIXTURE')",new Object[]{campaign,parent.genome.key(),parent.genome.text(),fileState.reference(),stateHash,checkpoint});CheckpointManifestR26.record(w.db,checkpoint,parent.genome.key(),campaign,"",through,fileState.reference());w.db.setTransactionSuccessful();}finally{w.db.endTransaction();}}
            SQLiteDatabase.scenario="BASELINE_ACTUAL_WARM_BIRTH";int start=SQLiteDatabase.frames.size();long units=WarmCandidateR26.step(w.store,()->w.claim,new BudgetTokenSU1(60000,10000000,()->false));need(units==1,"ACTUAL_AUTHENTICATED_WARM_BIRTH");need(scalar(w.ro,"SELECT count(*) FROM r26_warm_initialization")==1,"WARM_INITIALIZER_PERSISTED");need(scalar(w.ro,"SELECT count(*) FROM r26_cohort")==0,"NO_QUALITY_EXPOSURE_GRANTED");need(scalar(w.ro,"SELECT count(*) FROM r26_candidate WHERE campaign_id='OLD_WARM_PARENT'")==0,"ORIGINAL_TRANSFER_REMOVAL");
            try(Cursor c=w.ro.rawQuery("SELECT canonical_text,state,state_hash,cursor FROM r26_candidate",null)){need(c.moveToFirst()&&c.getInt(3)==0,"NEW_PRIVATE_SAMPLE_CURSOR");LiveStateR26 child=LiveStateR26.decode(c.getString(0),CheckpointArtifactsR26.resolve(w.store,c.getBlob(1)),c.getString(2));LiveStateR26 expected=LiveStateR26.decode(parent.genome.text(),state,stateHash);expected.onGap();need(Arrays.equals(kernel(expected.hub.models.values().iterator().next()),kernel(child.hub.models.values().iterator().next())),"EXACT_ORIGINAL_KERNEL_WEIGHTS_AND_OPTIMIZER");}
            long maxBinds=0;for(int i=start;i<SQLiteDatabase.frames.size();i++)maxBinds=Math.max(maxBinds,((Number)SQLiteDatabase.frames.get(i).get("bindBytes")).longValue());long cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);System.out.println("RESULT {\"scope\":\"ACTUAL_WARM_PREPARE_AND_BIRTH_NOT_QUALITY\",\"maxBindBytes\":"+maxBinds+",\"runtimeBindCap\":"+cap+",\"heap\":"+Runtime.getRuntime().maxMemory()+"}");if(maxBinds>cap)throw new AssertionError("BASELINE_WARM_BIRTH_BIND_OVER_CAP_R31:"+maxBinds+">"+cap);System.out.println("PASS "+assertions+" BASELINE_WARM_BIRTH_WITHIN_THIS_HEAP_CAP_NOT_ALL_RUNTIME_ACCEPTANCE");
        }
    }
    public static void main(String[] args)throws Exception{Class.forName("org.sqlite.JDBC");Path file=Paths.get(args[1]);Files.createDirectories(file.getParent());if(args[0].startsWith("bulk")){bulk(file,Integer.parseInt(args[0].substring(4)));return;}warmBirth(file);}
}
