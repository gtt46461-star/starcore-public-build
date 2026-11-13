package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.File;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual DatasetTask/claim/fence/writer on native SQLite. Synthetic controlled
 * rows only; the POSIX adapter measures the actual filesystem with stock limits. */
public final class ReplayKnowledgePublicationR31Host {
    static int checks;
    static void need(boolean yes,String name){if(!yes)throw new AssertionError(name);checks++;}
    static final class ObservedDb extends SQLiteDatabase {
        int hotLoads;boolean reportOnly;
        ObservedDb(Connection db){super(db);}
        @Override public Cursor rawQuery(String sql,String[] args){
            if(sql.startsWith("SELECT issue,")&&sql.contains("FROM draws"))hotLoads++;
            return super.rawQuery(sql,args);
        }
        @Override public void execSQL(String sql,Object[] args){if(reportOnly)throw new AssertionError("READ_ONLY_REPORT_WROTE_SQL");super.execSQL(sql,args);}
    }
    static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT count(*) FROM "+table,null)){if(!c.moveToFirst())throw new AssertionError(table);return c.getLong(0);}}
    static void reset(RuntimeR27 runtime){if(runtime.evoCache!=null)runtime.evoCache.close();if(runtime.pendingCache!=null)runtime.pendingCache.close();runtime.evoData=null;runtime.evoCache=null;runtime.pendingData=null;runtime.pendingCache=null;}
    static Map<String,Object> report(ObservedDb db){int loads=db.hotLoads;db.reportOnly=true;try{Map<String,Object> value=ReplayKnowledgeStoreR31.report(db);need(db.hotLoads==loads,"REPORT_NEVER_LOADS_DRAW_DATASET");return value;}finally{db.reportOnly=false;}}
    static void cursor(ObservedDb db,String hash){db.execSQL("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,0,0,?,1,'HOST_REFERENCE_ONLY',1)",new Object[]{RuntimeR27.LOOP,hash});}
    static void verify(Map<String,Object> value,String mode,String hash){
        need(mode.equals(value.get("publicationMode")),"ACTUAL_DATASET_TASK_BRANCH_"+mode);
        need(hash.equals(value.get("datasetHash")),"ORIGINAL_PIN_DATASET_IDENTITY");
        need(Boolean.TRUE.equals(value.get("readOnly")),"VALIDATED_READ_ONLY_PROJECTION");
        need(Boolean.FALSE.equals(value.get("authorizesCertificateOrReal"))&&"NOT_PROVEN".equals(value.get("naturalQuality")),"INPUT_METADATA_NEVER_QUALITY_AUTHORITY");
        need(value.get("sourceCommit") instanceof String&&value.get("sourceWorktreeHash") instanceof String,"SOURCE_BUILD_IDENTITY_CARRIED");
        need("NOT_INFERRED_GENERATION_BOUNDARY_UNCHANGED".equals(value.get("consumedByCurrentGeneration")),"NO_INVENTED_GENERATION_CONSUMPTION");
    }
    static void write(Path file,String library)throws Exception {
        Files.createDirectories(file.getParent());HistoryPosixStatR31Host.install(library);
        ObservedDb db=new ObservedDb(DriverManager.getConnection("jdbc:sqlite:"+file));
        StringJoiner columns=new StringJoiner(",");for(int i=1;i<=10;i++)columns.add("n"+i+" INTEGER");
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,"+columns+")");
        long start=TargetIssueResolver.sequenceOrdinalSU1("202610300001");
        db.beginTransaction();try{for(int i=0;i<5000;i++){Object[] row=new Object[14];row[0]=TargetIssueResolver.fromSequenceOrdinalSU1(start+i);row[1]=1793318400000L+i*60000L;row[2]=i==5?0:1793318401000L+i*60000L;row[3]=0;for(int p=0;p<10;p++)row[p+4]=(p+i)%10+1;db.execSQL("INSERT INTO draws VALUES("+String.join(",",Collections.nCopies(14,"?"))+")",row);}db.setTransactionSuccessful();}finally{db.endTransaction();}
        try(R31OwnedRuntimeFixture fixture=new R31OwnedRuntimeFixture(db,"r27-replay-dataset")){
            fixture.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,"HOST_REPLAY_PUBLICATION_PREREQUISITES"),tx->{HistoryImportR27.ensureR31PublicationSchema(tx);ResearchInputPinR30R1.ensure(tx);return 0;});
            need("UNAVAILABLE_REPLAY_KNOWLEDGE".equals(report(db).get("status")),"MISSING_METADATA_IS_UNKNOWN");
            ResearchPoolR27.Outcome fresh=fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));
            need(fresh.units==1&&fixture.runtime.evoData.size()==5000,"ACTUAL_NEW_DATASET_TASK_PUBLISHED_ORIGINAL_5000_WINDOW");
            ReplayDatasetR27 data=fixture.runtime.evoData;Map<String,Object> projection=report(db);verify(projection,"NEW_INPUT_PINNED",data.datasetHash);
            Map<?,?> provenance=(Map<?,?>)projection.get("provenance");need(((Number)provenance.get("recordedKnownAtRows")).intValue()==4999&&((Number)provenance.get("unknownKnownAtRows")).intValue()==1,"ACTUAL_MIXED_KNOWN_AND_UNKNOWN_INPUT_REPORTED");
            need("RECORDED_INPUT_METADATA_ONLY".equals(projection.get("status")),"TIMESTAMP_RECORDING_IS_NOT_PAST_FORECAST_PROOF");
            need(ChartCacheR27.classR31(ReplayKnowledgeStoreR31.PANEL)!=ChartCacheR27.CacheClassR31.DISPLAY,"METADATA_SLOT_IS_NOT_DISPLAY_GC");
            long pins=count(db,"r30r1_analysis_run"),headers=count(db,"r27_replay_dataset"),source=SchemaR27.liveSourceRevision(db);int hot=db.hotLoads;
            reset(fixture.runtime);cursor(db,data.datasetHash);
            fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));verify(report(db),"RECOVERED_COMMITTED_PIN",data.datasetHash);
            need(db.hotLoads==hot,"CURSOR_RECOVERY_DOES_NOT_RELOAD_5000_DRAWS");
            need(count(db,"r30r1_analysis_run")==pins&&count(db,"r27_replay_dataset")==headers&&SchemaR27.liveSourceRevision(db)==source,"RECOVERY_REUSES_ORIGINAL_PIN_AND_WATERMARK");
            reset(fixture.runtime);db.execSQL("DELETE FROM r27_evolution_cursor");hot=db.hotLoads;
            fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));verify(report(db),"RESTORED_PUBLISHED_DATASET",data.datasetHash);
            need(db.hotLoads==hot+1,"EXISTING_DATASET_BRANCH_ONLY_ORIGINAL_LOAD");
            need(count(db,"r30r1_analysis_run")==pins&&count(db,"r27_replay_dataset")==headers,"PUBLISHED_DATASET_RECOVERY_DOES_NOT_RECREATE_INPUT");
            List<ReplayDatasetR27.Row> oldRows=new ArrayList<>();for(int i=0;i<data.size();i++)oldRows.add(new ReplayDatasetR27.Row(data.issues[i],data.ordinals[i],data.draws[i]));ReplayDatasetR27 legacy=ReplayDatasetR27.of(oldRows);
            fixture.owned(()->{ResearchWriteQueueR27.Batch old=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(old,legacy,source,1);RuntimeR27.submit(fixture.worker,"HOST_ORIGINAL_LEGACY_PIN",old);return 0;});
            reset(fixture.runtime);cursor(db,legacy.datasetHash);hot=db.hotLoads;
            fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));projection=report(db);verify(projection,"RECOVERED_COMMITTED_PIN",legacy.datasetHash);
            need("LEGACY_KNOWLEDGE_TIME_UNKNOWN".equals(projection.get("status")),"OLD_CODEC_RECOVERY_NEVER_INVENTS_KNOWLEDGE_TIME");
            provenance=(Map<?,?>)projection.get("provenance");need(((Number)provenance.get("recordedKnownAtRows")).intValue()==0&&((Number)provenance.get("unknownKnownAtRows")).intValue()==5000,"LEGACY_UNKNOWN_COUNTS_ARE_ORIGINAL_METADATA");need(db.hotLoads==hot,"LEGACY_RECOVERY_READS_PIN_NOT_DRAWS");
            String payload;try(Cursor c=db.rawQuery("SELECT payload_json FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{ReplayKnowledgeStoreR31.PANEL,ReplayKnowledgeStoreR31.KEY})){need(c.moveToFirst(),"ACTUAL_PRODUCER_PERSISTED_METADATA");payload=c.getString(0);}
            db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id=? AND key=?",new Object[]{payload.replace("ORIGINAL_LEGACY_PIN_TIME_UNRECORDED","TAMPER"),ReplayKnowledgeStoreR31.PANEL,ReplayKnowledgeStoreR31.KEY});need("INVALID_REPLAY_KNOWLEDGE".equals(report(db).get("status")),"TAMPERED_PROJECTION_NEVER_VALIDATED");
            db.execSQL("UPDATE r27_chart_cache SET payload_json=? WHERE panel_id=? AND key=?",new Object[]{String.join("",Collections.nCopies(65537,"x")),ReplayKnowledgeStoreR31.PANEL,ReplayKnowledgeStoreR31.KEY});need("UNAVAILABLE_OVERSIZED_REPLAY_KNOWLEDGE".equals(report(db).get("status")),"OVERSIZED_METADATA_NEVER_LOADED_FOR_PARSE");
            reset(fixture.runtime);fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));verify(report(db),"RECOVERED_COMMITTED_PIN",legacy.datasetHash);
            need(HistoryPosixStatR31Host.calls.get()>0,"REAL_POSIX_ALLOCATION_MEASUREMENT_WITH_DEFAULT_BUDGETS");
            long persistedAt=((Number)report(db).get("publishedAt")).longValue();reset(fixture.runtime);
            fixture.beforeNextOwnedWrite(()->db.execSQL("UPDATE draws SET first_seen_at=first_seen_at+1 WHERE issue=?",new Object[]{data.issues[0]}));
            boolean stale=false;try{fixture.owned(()->fixture.runtime.new DatasetTask().step(fixture.worker));}catch(RuntimeException expected){stale=true;}
            need(stale,"SOURCE_CHANGED_AFTER_RECOVERY_PREPARE_REFUSES_PUBLICATION");need(fixture.runtime.evoData==null&&fixture.runtime.evoCache==null,"STALE_RECOVERY_DOES_NOT_PUBLISH_RUNTIME_CACHE");
            need(SchemaR27.liveSourceRevision(db)>source&&ResearchInputPinR30R1.invalid(db,legacy.datasetHash),"ACTUAL_SOURCE_TRIGGER_AND_PIN_GUARD_EXECUTED");
            projection=report(db);need(((Number)projection.get("publishedAt")).longValue()==persistedAt,"STALE_RECOVERY_ROLLS_BACK_METADATA");need("NOT_RECHECKED_DERIVED_METADATA_ONLY".equals(projection.get("inputValidity")),"LAST_PUBLICATION_PROJECTION_NEVER_ASSERTS_CURRENT_INPUT_VALIDITY");
            reset(fixture.runtime);
        }
        db.jdbc.close();System.out.println("REPLAY_KNOWLEDGE_PUBLICATION_WRITE_PASS checks="+checks+" actualDatasetTask=true syntheticRows=5000");System.out.flush();Runtime.getRuntime().halt(0);
    }
    static void read(Path file)throws Exception{ObservedDb db=new ObservedDb(DriverManager.getConnection("jdbc:sqlite:"+file));Map<String,Object> value=report(db);need("LEGACY_KNOWLEDGE_TIME_UNKNOWN".equals(value.get("status")),"FRESH_PROCESS_READS_DURABLE_UNKNOWN_METADATA");need(db.hotLoads==0,"FRESH_REPORT_PROCESS_NEVER_LOADS_REPLAY_DATA");need(count(db,"r27_replay_dataset")==1&&count(db,"r30r1_analysis_run")==2,"ORIGINAL_NEW_AND_LEGACY_ASSETS_RETAINED");db.jdbc.close();System.out.println("REPLAY_KNOWLEDGE_PUBLICATION_READ_PASS checks="+checks+" actualFreshProcess=true");}
    public static void main(String[] args)throws Exception{
        if("write".equals(args[0])){write(Paths.get(args[1]),args[2]);return;}if("read".equals(args[0])){read(Paths.get(args[1]));return;}
        File root=new File(args[0]);File database=new File(root,"databases/main.db");String java=new File(System.getProperty("java.home"),"bin/java").getPath(),cp=System.getProperty("java.class.path");
        Process writer=new ProcessBuilder(java,"-Xmx768m","-cp",cp,ReplayKnowledgePublicationR31Host.class.getName(),"write",database.getPath(),args[1]).inheritIO().start();need(writer.waitFor()==0,"WRITE_PROCESS_EXIT");Process reader=new ProcessBuilder(java,"-Xmx768m","-cp",cp,ReplayKnowledgePublicationR31Host.class.getName(),"read",database.getPath()).inheritIO().start();need(reader.waitFor()==0,"FRESH_READ_PROCESS_EXIT");System.out.println("REPLAY_KNOWLEDGE_PUBLICATION_NATIVE_PASS actualRuntimeDatasetBranches=3 processReopen=true physicalDevice=NOT_RUN naturalQuality=NOT_PROVEN");
    }
}
