package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual loader, original-row codecs, pin/source guards and recovery on native
 * SQLite. Every draw here is a synthetic fixture; no natural visibility claim. */
public final class ReplayKnowledgeR31Host {
    static int checks;
    static void need(boolean yes,String code){checks++;if(!yes)throw new AssertionError(code);}
    static void reject(Runnable work,String code){boolean rejected=false;try{work.run();}catch(RuntimeException expected){rejected=true;}need(rejected,code);}
    static SQLiteDatabase open(Path file)throws Exception{return new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+file));}
    static void tx(SQLiteDatabase db,Runnable action)throws Exception{db.jdbc.setAutoCommit(false);try{action.run();db.jdbc.commit();}catch(Throwable e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static void apply(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch)throws Exception{
        db.jdbc.setAutoCommit(false);try{ResearchWriteQueueR27.execute(db,batch,"HOST_REPLAY_KNOWLEDGE");db.jdbc.commit();}catch(Throwable e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}
    }
    static ReplayDatasetR27 fixture(int n,boolean metadata){
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();long ordinal=TargetIssueResolver.sequenceOrdinalSU1("202610300001");
        for(int i=0;i<n;i++){long at=ordinal+i+(i>=1440?1:0);String issue=TargetIssueResolver.fromSequenceOrdinalSU1(at);int[] numbers=new int[10];for(int p=0;p<10;p++)numbers[p]=(p+i)%10+1;
            long opened=1793318400000L+60000L*i,known=i==5?-1:opened+1000;
            rows.add(metadata?new ReplayDatasetR27.Row(issue,at,numbers,opened,known,ReplayDatasetR27.READER_ABI_R31,"DRAW_FIRST_SEEN"):new ReplayDatasetR27.Row(issue,at,numbers));
        }return ReplayDatasetR27.of(rows);
    }
    static void schema(SQLiteDatabase db)throws Exception{
        StringJoiner columns=new StringJoiner(",");for(int n=1;n<=10;n++)columns.add("n"+n+" INTEGER");
        db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER,"+columns+")");
        db.execSQL(HistoryImportR27.DDL);for(String sql:HistoryImportR27.R31_PUBLICATION_DDL)db.execSQL(sql);for(String sql:ResearchSchemaR30R1.DDL)db.execSQL(sql);for(String sql:AtomNetStoreR30.DDL)db.execSQL(sql);for(String sql:FrozenConfirmationStoreR30.DDL)db.execSQL(sql);
        tx(db,()->{SchemaR27.ensureLiveSourceRevision(db);ResearchInputPinR30R1.ensure(db);});
    }
    static FrozenConfirmationR30.Task task(ReplayDatasetR27 data){
        PolicySpecR27 policy=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.MARGIN,0,0,1,false,0,0,IdeaFamilyR27.meta("I01","BIRTH",0,1,"SYNTHETIC_NATIVE_CODEC_ONLY"));
        Map<String,Object> champion=new LinkedHashMap<>();champion.put("spec",policy.fullJson());champion.put("tau",1.0);champion.put("kind","MAIN");champion.put("seed",0L);champion.put("evaluationDatasetHash",data.datasetHash);
        SplitR27 split=SplitR27.of(data,null);Map<String,Object> packet=new LinkedHashMap<>();packet.put("dataset",data.datasetHash);packet.put("sourceCommit","SYNTHETIC_NATIVE_CODEC_ONLY");packet.put("researchSemanticHash",FrozenConfirmationR30.currentSemantics());packet.put("generation",0L);packet.put("block",split.holdoutBlock);packet.put("from",split.holdoutFrom);packet.put("to",split.holdoutTo);packet.put("frozenAt",1L);packet.put("nullRequired",false);packet.put("champion",champion);packet.put("previous",null);packet.put("i00",null);packet.put("null",null);return FrozenConfirmationR30.Task.parse(JsonR27.write(packet));
    }
    static void saveDatasetConsumers(SQLiteDatabase db,ReplayDatasetR27 data)throws Exception{
        ResearchWriteQueueR27.Batch pin=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(pin,data,SchemaR27.liveSourceRevision(db),1);apply(db,pin);
        ResearchWriteQueueR27.Batch atom=new ResearchWriteQueueR27.Batch();AtomNetStoreR30.dataset(atom,data,1);apply(db,atom);
        ResearchWriteQueueR27.Batch frozen=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.freeze(frozen,task(data),data);apply(db,frozen);
    }
    static void restoreConsumers(SQLiteDatabase db,ReplayDatasetR27 expected){
        ResearchInputPinR30R1.Pin pin=ResearchInputPinR30R1.load(db,expected.datasetHash);need(pin!=null&&pin.data.datasetHash.equals(expected.datasetHash),"PIN_IDENTITY_RECOVERED");need(Arrays.equals(expected.knownAtR31,pin.data.knownAtR31),"PIN_KNOWN_AT_EXACT");need(Arrays.equals(expected.readerAbiR31,pin.data.readerAbiR31),"PIN_READER_ABI_EXACT");
        ReplayDatasetR27 atom=AtomNetStoreR30.dataset(db,expected.datasetHash);need(atom.datasetHash.equals(expected.datasetHash)&&Arrays.equals(atom.knownAtR31,expected.knownAtR31),"ACTUAL_ATOM_DATASET_RECOVERY_CARRIES_KNOWLEDGE");
        EvalContextR27 context=FrozenConfirmationStoreR30.context(db,task(expected));try{need(context.data.datasetHash.equals(expected.datasetHash)&&Arrays.equals(context.data.knownAtR31,expected.knownAtR31),"ACTUAL_CONFIRM_CONTEXT_RECOVERS_EXACT_KNOWLEDGE");need(context.data.provenanceHashR31.equals(expected.provenanceHashR31),"CONFIRM_PROVENANCE_HASH_EXACT");}finally{context.cache.close();}
    }
    static void publishedImport(SQLiteDatabase db,String id,String issue,long known,int sequence)throws Exception{
        db.execSQL("INSERT INTO r31_history_import_run(job_id,source_hash,parser_abi,source_path,source_bytes,data_role,status,owner_epoch,fence,owner_pid,owner_boot,lease_elapsed_ms,created_at) VALUES(?,?,'R31_HISTORY_CSV_V1','fixture.csv',1,'REPLAY_ONLY_CANONICAL_OLDER','STAGING','fixture',1,1,'fixture',0,?)",new Object[]{id,StatsR27.sha256(id),known});
        db.execSQL("INSERT INTO r31_history_import_row(job_id,issue,ordinal,numbers,status,source_line,row_hash) VALUES(?,?,?,'1,2,3,4,5,6,7,8,9,10','IMPORTED',1,?)",new Object[]{id,issue,TargetIssueResolver.sequenceOrdinalSU1(issue),StatsR27.sha256(id+issue)});
        db.execSQL("UPDATE r31_history_import_run SET status='COMPLETE',publish_seq=?,completed_at=? WHERE job_id=?",new Object[]{sequence,known,id});
    }
    static void importAndTamper(SQLiteDatabase db,ReplayDatasetR27 source)throws Exception{
        String imported=TargetIssueResolver.fromSequenceOrdinalSU1(source.ordinals[0]-2),legacy=TargetIssueResolver.fromSequenceOrdinalSU1(source.ordinals[0]-3);
        db.execSQL("INSERT INTO r27_history_import VALUES(?,?,'1,2,3,4,5,6,7,8,9,10','IMPORTED',NULL,'old fixture',999)",new Object[]{legacy,TargetIssueResolver.sequenceOrdinalSU1(legacy)});
        publishedImport(db,"known-import-1",imported,1794508800000L,1);
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();HistoryImportR27.appendOlder(db,rows,source.issues[0],10);ReplayDatasetR27 before=ReplayDatasetR27.of(rows);need(before.size()==2,"ACTUAL_IMPORT_READER_MERGES_EXISTING_PUBLICATIONS");
        for(int row=0;row<before.size();row++)if(imported.equals(before.issues[row]))need(before.knownAtR31[row]==1794508800000L&&"IMPORT_APP_PUBLISHED".equals(before.knowledgeScopeR31[row]),"IMPORT_KNOWLEDGE_IS_ACTUAL_APP_PUBLICATION_NOT_FAKE_PAST");else need(before.knownAtR31[row]==-1,"LEGACY_IMPORT_TIMESTAMP_REMAINS_UNKNOWN");
        publishedImport(db,"known-import-2",imported,1794508810000L,2);rows.clear();HistoryImportR27.appendOlder(db,rows,source.issues[0],10);need(before.datasetHash.equals(ReplayDatasetR27.of(rows).datasetHash),"IDENTICAL_REIMPORT_PRESERVES_FIRST_AUTHENTICATED_KNOWLEDGE_TIME");
        String original;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name='r31_pin_draw_known_at_update'",null)){need(c.moveToFirst(),"KNOWLEDGE_OWNER_TRIGGER_FOUND");original=c.getString(0);}
        for(String literal:new String[]{"DRAWs","draws;","draws IF NOT EXISTS altered","draws  altered"}){
            db.execSQL("DROP TRIGGER r31_pin_draw_known_at_update");db.execSQL(original.replace("'draws'","'"+literal+"'"));need("CONFLICT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"QUOTED_SOURCE_LITERAL_TAMPER_REJECTED");
            boolean rejected=false;try{tx(db,()->ResearchInputPinR30R1.ensure(db));}catch(RuntimeException expected){rejected=true;}need(rejected,"OWNER_NEVER_RECERTIFIES_UNKNOWN_TRIGGER_DEFINITION");
        }
        db.execSQL("DROP TRIGGER r31_pin_draw_known_at_update");db.execSQL(original);need(ResearchInputPinR30R1.triggersValid(db),"OWNER_DEFINITION_RESTORED_EXACTLY_FOR_FIXTURE");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase old=new SQLiteDatabase(connection);old.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10)");old.execSQL("INSERT INTO draws VALUES(?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{source.issues[0]});ReplayDatasetR27 unknown=StoreR27.loadDataset(old,1);need(unknown.knownAtR31[0]==-1&&unknown.openTimeR31[0]==-1&&!unknown.knownBeforeR31(0,Long.MAX_VALUE),"MISSING_SCHEMA_TIMESTAMPS_ARE_UNKNOWN_NOT_INVENTED");
        }
    }
    static void write(Path file)throws Exception{
        SQLiteDatabase db=open(file);schema(db);ReplayDatasetR27 source=fixture(5000,true);
        tx(db,()->{for(int i=0;i<source.size();i++){Object[] row=new Object[14];row[0]=source.issues[i];row[1]=source.openTimeR31[i];row[2]=source.knownAtR31[i]>0?source.knownAtR31[i]:0;row[3]=0;for(int p=0;p<10;p++)row[p+4]=source.draws[i][p];db.execSQL("INSERT INTO draws VALUES("+String.join(",",Collections.nCopies(14,"?"))+")",row);}});
        ReplayDatasetR27 loaded=StoreR27.loadDataset(db,5000);need(source.datasetHash.equals(loaded.datasetHash),"ACTUAL_HOT_LOADER_CARRIES_FIRST_SEEN_AND_OPEN");need(loaded.size()==5000,"ORIGINAL_5000_WINDOW_PRESERVED");
        need(!loaded.legacyIdentityR31&&((Number)loaded.provenanceR31().get("unknownKnownAtRows")).intValue()==1,"UNKNOWN_IS_EXPLICIT_NOT_FAKE_TIMESTAMP");need(!loaded.knownBeforeR31(5,Long.MAX_VALUE),"UNKNOWN_NEVER_POSITIVE_KNOWLEDGE_PROOF");need(!loaded.knownBeforeR31(0,loaded.knownAtR31[0])&&loaded.knownBeforeR31(0,loaded.knownAtR31[0]+1),"STRICT_KNOWN_AT_CUTOFF");
        ReplayDatasetR27 legacy=fixture(5000,false);need(!source.datasetHash.equals(legacy.datasetHash)&&legacy.datasetHash.equals(ReplayDatasetR27.hash(legacy.issues,legacy.draws,legacy.segment)),"ORIGINAL_LEGACY_V2_HASH_PRESERVED_AND_NEW_IDENTITY_ISOLATED");
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();for(int i=0;i<source.size();i++)rows.add(source.rowR31(i));ReplayDatasetR27.Row first=rows.get(0);rows.set(0,new ReplayDatasetR27.Row(first.issue,first.ordinal,first.numbers,first.openTime,first.knownAt+1,first.readerAbi,first.knowledgeScope));need(!ReplayDatasetR27.of(rows).datasetHash.equals(source.datasetHash),"KNOWLEDGE_REVISION_CHANGES_DATASET_HASH");
        rows.set(0,new ReplayDatasetR27.Row(first.issue,first.ordinal,first.numbers,first.openTime,first.openTime-1,first.readerAbi,first.knowledgeScope));ReplayDatasetR27 badClock=ReplayDatasetR27.of(rows);need(!badClock.knownBeforeR31(0,Long.MAX_VALUE)&&((Number)badClock.provenanceR31().get("invalidKnownAtChronologyRows")).intValue()==1,"IMPOSSIBLE_FIRST_SEEN_CLOCK_IS_NOT_POSITIVE_PIT_PROOF");
        rows.set(0,new ReplayDatasetR27.Row(first.issue,first.ordinal,first.numbers,first.openTime,first.knownAt,"OTHER_READER",first.knowledgeScope));need(!ReplayDatasetR27.of(rows).datasetHash.equals(source.datasetHash),"READER_ABI_CHANGES_IDENTITY");
        rows.clear();for(int i=4500;i<source.size();i++)rows.add(source.rowR31(i));ReplayDatasetR27 tail=ReplayDatasetR27.of(rows);need(tail.datasetHash.equals(source.tail(500).datasetHash)&&Arrays.equals(tail.knownAtR31,source.tail(500).knownAtR31),"ROLLING_TAIL_CARRIES_METADATA");
        rows.clear();for(int i=0;i<4999;i++)rows.add(source.rowR31(i));need(ReplayDatasetR27.of(rows).append(source.rowR31(4999)).datasetHash.equals(source.datasetHash),"APPEND_AND_RESTART_IDENTITY_STABLE");
        ReplayDatasetR27 tenThousand=fixture(10000,true);byte[] raw=tenThousand.originalBytesR31();need(raw.length<ReplayDatasetR27.MAX_ORIGINAL_BYTES_R31&&ReplayDatasetR27.originalBytesR31(raw).datasetHash.equals(tenThousand.datasetHash),"TEN_THOUSAND_METADATA_ROWS_WITHIN_ORIGINAL_ONE_MIB_BOUND");
        byte[] bad=raw.clone();bad[bad.length-1]^=1;reject(()->ReplayDatasetR27.originalBytesR31(bad),"CORRUPTED_PROVENANCE_REFUSED");byte[] extra=Arrays.copyOf(raw,raw.length+1);reject(()->ReplayDatasetR27.originalBytesR31(extra),"EXTRA_CODEC_BYTES_REFUSED");
        reject(()->new ReplayDatasetR27.Row(first.issue,first.ordinal+1,first.numbers,first.openTime,first.knownAt,first.readerAbi,first.knowledgeScope),"WRONG_CALENDAR_CANNOT_MASQUERADE_AS_NEW_ABI");
        saveDatasetConsumers(db,loaded);saveDatasetConsumers(db,legacy);restoreConsumers(db,loaded);restoreConsumers(db,legacy);
        long generation=SchemaR27.liveSourceRevision(db),fence=ResearchSourceJournalR31.currentFence(db);db.jdbc.setAutoCommit(false);db.execSQL("UPDATE draws SET first_seen_at=? WHERE issue=?",new Object[]{source.openTimeR31[5]+3000,source.issues[5]});need(SchemaR27.liveSourceRevision(db)>generation&&ResearchSourceJournalR31.currentFence(db)>fence,"KNOWN_AT_REPAIR_ADVANCES_SOURCE_AND_EVENT_FENCES");need(ResearchInputPinR30R1.invalid(db,source.datasetHash),"FIRST_SEEN_REPAIR_INVALIDATES_OLD_INPUT_IDENTITY");db.jdbc.rollback();db.jdbc.setAutoCommit(true);need(!ResearchInputPinR30R1.invalid(db,source.datasetHash),"METADATA_ROLLBACK_DOES_NOT_INVALIDATE_PIN");
        db.execSQL("DELETE FROM draws WHERE issue<?",new Object[]{source.issues[100]});need(!ResearchInputPinR30R1.invalid(db,source.datasetHash),"ROLLING_DELETION_KEEPS_ACTUAL_ORIGINAL_PIN");restoreConsumers(db,source);
        db.execSQL("DROP TRIGGER r31_pin_draw_known_at_update");need("R31_V1_EXACT".equals(ResearchSourceJournalR31.inspectUpgradeR31(db)),"EXACT_PREVIOUS_SIX_TRIGGER_SHAPE_RECOGNIZED");tx(db,()->ResearchInputPinR30R1.ensure(db));need(ResearchInputPinR30R1.triggersValid(db),"OWNER_REPAIRS_ONLY_DECLARED_MISSING_KNOWLEDGE_TRIGGER");
        importAndTamper(db,source);
        db.jdbc.close();System.out.println("REPLAY_KNOWLEDGE_WRITE_PASS checks="+checks+" fullSourceRows=5000 originalBound=1048576");System.out.flush();Runtime.getRuntime().halt(0);
    }
    static void read(Path file)throws Exception{SQLiteDatabase db=open(file);restoreConsumers(db,fixture(5000,true));restoreConsumers(db,fixture(5000,false));need(ResearchInputPinR30R1.triggersValid(db),"SOURCE_OWNER_EXACT_AFTER_REAL_PROCESS_REOPEN");db.jdbc.close();System.out.println("REPLAY_KNOWLEDGE_READ_PASS checks="+checks+" actualProcessReopen=true syntheticOnly=true");}
    public static void main(String[] args)throws Exception{
        if(args[0].equals("write")){write(Paths.get(args[1]));return;}if(args[0].equals("read")){read(Paths.get(args[1]));return;}
        Path root=Paths.get(args[0]);Files.createDirectories(root);Path file=root.resolve("replay.db");String java=Paths.get(System.getProperty("java.home"),"bin","java").toString(),cp=System.getProperty("java.class.path");
        Process writer=new ProcessBuilder(java,"-Xmx768m","-cp",cp,ReplayKnowledgeR31Host.class.getName(),"write",file.toString()).inheritIO().start();need(writer.waitFor()==0,"ACTUAL_WRITE_PROCESS_EXIT");Process reader=new ProcessBuilder(java,"-Xmx768m","-cp",cp,ReplayKnowledgeR31Host.class.getName(),"read",file.toString()).inheritIO().start();need(reader.waitFor()==0,"ACTUAL_FRESH_PROCESS_REOPEN");System.out.println("REPLAY_KNOWLEDGE_NATIVE_PASS sourceAdmissionAndOriginalCodecs=true actualProcessReopen=true physicalDevice=NOT_RUN naturalQuality=NOT_PROVEN");
    }
}
