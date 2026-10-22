package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.StringWriter;

/** Real 125-row archive, actual product VM/creator/SQL; not real future-win evidence. */
public final class HistoricalMigrationHarnessR967 {
    static int checks;
    static final String BOUNDARY="202609210120";
    static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
    interface Work{void run()throws Exception;}
    static void rejects(Work w,String label)throws Exception{boolean rejected=false;try{w.run();}catch(Exception expected){rejected=true;}check(rejected,label);}
    static void rejectsWith(Work w,String expectedMarker,String label)throws Exception {
        boolean matched=false;
        try{w.run();}catch(Exception error){for(Throwable t=error;t!=null;t=t.getCause())if(String.valueOf(t.getMessage()).contains(expectedMarker))matched=true;if(!matched)throw error;}
        check(matched,label);
    }
    static long number(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static String modified(String raw,int column,String value){String[] f=raw.split("\t",-1);f[column]=value;return String.join("\t",f);}
    static DbStore schema(SQLiteDatabase d){
        DbStore s=new DbStore(d);d.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0,open_time INTEGER,fetch_time INTEGER)");
        DbHelper.ensureR35Schema(d);DbHelper.ensureR36Schema(d);RuntimeEventLedgerR926.ensureSchema(d);DbHelper.ensureR906CriticalSchema(d);V51Repository.ensure(d);RealtimePressureGateR811.resetForTests();
        check(ResearchSchemaCoordinatorR926.ensureToReady(s.coordinatorR36(),8),"FULL_PRODUCT_SCHEMA_READY:"+ResearchSchemaCoordinatorR926.summary());
        d.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_MISS_REASON_SAMPLE+"(target_issue TEXT,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL,created_at INTEGER)");
        for(int i=1;i<=120;i++)d.execSQL("INSERT INTO draws VALUES(?,1,2,3,4,5,6,7,8,9,10,0,1,2)",new Object[]{String.format(Locale.US,"20260921%04d",i)});
        return s;
    }
    static int batch(SQLiteDatabase d,HistoricalGenomeImportR967.Archive a,int limit){d.beginTransaction();try{int n=HistoricalGenomeImportR967.importBatch(d,a,BOUNDARY,limit);d.setTransactionSuccessful();return n;}finally{d.endTransaction();}}
    static String create(SQLiteDatabase d){d.beginTransaction();try{String r=DirectedResearchCreationR932.create(d,"algorithm");d.setTransactionSuccessful();return r;}finally{d.endTransaction();}}
    static HistoricalGenomeImportR967.Archive archive(String header,List<String> rows){byte[] b=(header+"\n"+String.join("\n",rows)+"\n").getBytes(StandardCharsets.UTF_8);return HistoricalGenomeImportR967.decode(b,HistoricalGenomeImportR967.sha(b),rows.size());}
    public static void main(String[] args)throws Exception{
        final byte[] raw=Files.readAllBytes(Paths.get(args[1]));
        final HistoricalGenomeImportR967.Archive a=HistoricalGenomeImportR967.decode(raw,HistoricalGenomeImportR967.SOURCE_SHA,125);
        check(a.rows.size()==125,"EXACT_125_ORIGINAL_ROWS_AND_PINNED_SHA");
        byte[] changed=raw.clone();changed[changed.length-4]^=1;final byte[] corrupt=changed;
        rejects(()->HistoricalGenomeImportR967.decode(corrupt,HistoricalGenomeImportR967.SOURCE_SHA,125),"CORRUPT_SOURCE_REJECTED_BEFORE_IMPORT");
        rejects(()->HistoricalGenomeImportR967.decode(raw,HistoricalGenomeImportR967.SOURCE_SHA,124),"WRONG_EXPECTED_INVENTORY_REJECTED");
        final String sample=a.rows.get(0);String[] original=sample.split("\t",-1);
        final String state=original[17];
        for(final String bad:Arrays.asList("",state.replace("R914STATE2","UNKNOWN"),state.substring(0,state.lastIndexOf('|')),state+"|0",state.replaceFirst("\\|0\\|","|-1|"),state.replaceFirst("\\|0\\|","|9999999999999999999|")))
            rejects(()->AlgorithmGenomeVmR908.State.decodeStrict(bad),"STRICT_BAD_STATE_REJECT:"+bad.substring(0,Math.min(35,bad.length())));
        for(final String token:Arrays.asList("7ff8000000000000","7ff0000000000000","4022000000000000","z","00")){
            final String bad=state.substring(0,state.lastIndexOf('|')+1)+token;
            rejects(()->AlgorithmGenomeVmR908.State.decodeStrict(bad),"STRICT_BAD_NUMERIC_STATE_REJECT:"+token);
        }
        String[] parts=state.split("\\|");StringBuilder old=new StringBuilder("R908STATE1|").append(parts[1]);for(int i=10;i<parts.length;i++)old.append('|').append(parts[i]);
        check(AlgorithmGenomeVmR908.State.decodeStrict(old.toString()).encode().equals(state),"KNOWN_V1_LOCAL_STATE_EXPLICIT_CONVERSION");
        check(AlgorithmGenomeVmR908.State.decode("bad").updates==0,"LEGACY_DECODE_BEHAVIOR_NOT_SILENTLY_CHANGED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,17,"UNKNOWN|0"),BOUNDARY).accepted(),"UNKNOWN_RECORD_STATE_NOT_ACCEPTED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,18,"999"),BOUNDARY).accepted(),"MISMATCHED_UPDATE_COUNT_REJECTED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,19,"QUARANTINED_INTEGRITY"),BOUNDARY).accepted(),"ORIGINAL_QUARANTINE_PRESERVED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,19,"UNRECOGNIZED"),BOUNDARY).accepted(),"UNKNOWN_STATUS_REJECTED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,20,"1"),BOUNDARY).accepted(),"OLD_REAL_AUTHORITY_NOT_IMPORTED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,7,"202609220001"),BOUNDARY).accepted(),"TRAINING_AFTER_IMPORT_BOUNDARY_REJECTED");
        check(!HistoricalGenomeImportR967.validate(modified(sample,0,"R908-ALG-COLLISION"),BOUNDARY).accepted(),"PROGRAM_ID_CANNOT_ALIAS_DIFFERENT_PROGRAM");
        int accepted=0,retired=0;Map<String,Integer> reasons=new TreeMap<String,Integer>();
        for(int k=0;k<a.rows.size();k++){
            String row=a.rows.get(k);String[] f=row.split("\t",-1);HistoricalGenomeImportR967.Verdict v=HistoricalGenomeImportR967.validate(row,BOUNDARY);
            reasons.put(v.reason,reasons.containsKey(v.reason)?reasons.get(v.reason)+1:1);
            if(v.accepted()){
                accepted++;AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(f[2]);AlgorithmGenomeVmR908.State prior=AlgorithmGenomeVmR908.State.decodeStrict(f[17]);AlgorithmGenomeVmR908.State seed=AlgorithmGenomeVmR908.initialState(v.seed);
                long oldUpdates=prior.updates;prior.updates=0;
                check(prior.encode().equals(seed.encode())&&seed.updates==0,"PARAMETERS_PRESERVED_COUNTER_NEW_ZERO:row="+(k+1));
                check(v.seed.generation==g.generation+1&&v.seed.parentIds.equals(g.genomeId)&&!v.seed.genomeId.equals(g.genomeId),"NEW_SEED_ID_AND_FULL_LINEAGE:row="+(k+1));
                boolean parity=true;for(int pos=1;pos<=10;pos++){
                    double[] x=new double[AlgorithmGenomeR908.INPUTS];for(int j=0;j<x.length;j++)x[j]=(j+pos)%11/11d;x[13]=0;
                    String before=seed.encode();double p=AlgorithmGenomeVmR908.score(g,prior,pos,x),q=AlgorithmGenomeVmR908.score(v.seed,seed,pos,x);
                    parity &= Double.doubleToLongBits(p)==Double.doubleToLongBits(q)&&seed.encode().equals(before);
                    x[13]=pos%2;AlgorithmGenomeVmR908.update(g,prior,pos,x);AlgorithmGenomeVmR908.update(v.seed,seed,pos,x);parity&=prior.encode().equals(seed.encode());
                }
                check(parity,"CURRENT_VM_PROGRAM_STATE_REPLAY_AND_REAL_UPDATES:row="+(k+1)+":old_updates="+oldUpdates);
            }else if(v.reason.equals("ORIGINAL_RETIREMENT_PRESERVED"))retired++;
        }
        System.out.println("ARCHIVE_VERDICTS="+reasons);
        check(accepted==63&&retired==62,"125_INVENTORY_63_NEW_RESEARCH_ELIGIBLE_62_ORIGINAL_RETIRED");
        final SQLiteDatabase d=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,TargetIssueResolver.next(BOUNDARY).targetIssue,System.currentTimeMillis())){
            DbStore store=schema(d);
            rejects(()->HistoricalGenomeImportR967.importBatch(d,a,BOUNDARY,4),"MIGRATION_REQUIRES_OWNER_TRANSACTION");
            int sum=0,slices=0;while(sum<125){int n=batch(d,a,1000);check(n>0&&n<=4,"BOUNDED_DURABLE_IMPORT_SLICE:"+(++slices));sum+=n;}
            check(batch(d,a,4)==0,"DUPLICATE_IMPORT_ZERO_NEW_RECORDS");
            check(number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.ARCHIVE)==125,"EXACT_ARCHIVE_INVENTORY_CONSERVATION");
            check(number(d,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"ARCHIVING_DOES_NOT_CREATE_LIVE_MODELS");
            check(number(d,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"ARCHIVING_DOES_NOT_CREATE_CERTIFICATES");
            try(Cursor c=d.rawQuery("SELECT row_number,raw_tsv,original_status,original_reason,row_hash FROM "+HistoricalGenomeImportR967.ARCHIVE+" ORDER BY row_number",null)){
                int n=0;while(c.moveToNext()){String row=a.rows.get(c.getInt(0)-1);String[] f=row.split("\t",-1);check(row.equals(c.getString(1))&&f[19].equals(c.getString(2))&&f[38].equals(c.getString(3))&&HistoricalGenomeImportR967.hash(row).equals(c.getString(4)),"RAW_STATUS_REASON_HASH_PRESERVED:"+(++n));}
                check(n==125,"EVERY_ORIGINAL_ROW_ACCOUNTED_FOR");
            }
            final HistoricalGenomeImportR967.Archive crash=archive(a.header,a.rows.subList(0,4));
            d.execSQL("CREATE TRIGGER r970_test_crash BEFORE INSERT ON "+HistoricalGenomeImportR967.ARCHIVE+" WHEN NEW.row_number=3 AND NEW.source_hash='"+crash.hash+"' BEGIN SELECT RAISE(ABORT,'TEST_CRASH'); END");
            rejectsWith(()->batch(d,crash,4),"TEST_CRASH","MID_BATCH_FAILURE_ROLLS_BACK");
            check(number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.SOURCE+" WHERE source_hash='"+crash.hash+"'")==0&&number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.ARCHIVE)==125,"SOURCE_AND_ROWS_ROLL_BACK_TOGETHER_NO_FALSE_ACK");
            d.execSQL("DROP TRIGGER r970_test_crash");
            d.execSQL("CREATE TRIGGER r970_test_publish_fail BEFORE INSERT ON "+HistoricalGenomeImportR967.USE+" BEGIN SELECT RAISE(ABORT,'TEST_RECEIPT_FAIL'); END");
            rejectsWith(()->create(d),"TEST_RECEIPT_FAIL","CREATOR_RECEIPT_FAULT_ROLLS_BACK");
            check(number(d,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0&&number(d,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT)==0,"SEED_ATTEMPT_LIFECYCLE_AND_RECEIPT_ATOMIC");
            d.execSQL("DROP TRIGGER r970_test_publish_fail");
            String created=create(d);check(created.startsWith("CREATED_N0|"),"REAL_DIRECTED_CREATOR_PUBLISHES_HISTORICAL_SEED:"+created);
            check(number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.USE+" WHERE state='CREATED_N0'")==1,"CREATION_RECEIPT_COMMITTED_ONCE");
            check(number(d,"SELECT SUM(future_n+future_hit+state_updates+dev_n+may_authorize_real) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"NEW_SEED_NO_OLD_SAMPLES_UPDATES_OR_REAL_AUTHORITY");
            check(number(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==0&&number(d,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_FORMAL_ALPHA_SPENDING_OR_OLD_CERTIFICATE_IMPORT");
            check(create(d).startsWith("WAIT_NEXT_DRAW")&&number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.USE)==1,"RETRY_SAME_DRAW_CANNOT_RECREATE_SEED");
            d.beginTransaction();try{check(HistoricalGenomeImportR967.choose(d,BOUNDARY)==null,"HISTORICAL_SEEDS_NOT_MORE_THAN_ONE_IN_FOUR_ATTEMPTS");d.setTransactionSuccessful();}finally{d.endTransaction();}
            check(number(d,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g JOIN "+DirectedResearchCreationR932.ATTEMPT+" a ON a.child_key='algorithm:'||g.genome_id AND a.state='CREATED' WHERE g.dev_n=0")==1,"NEW_SEED_REACHES_ACTUAL_DEV_TRAINING_SELECTOR");
            StringWriter report=new StringWriter();HistoricalGenomeImportR967.writeReport(store,report);check(report.toString().contains("ORIGINAL_RETIREMENT_PRESERVED")&&report.toString().contains("CREATED_N0")&&!report.toString().contains("STATUS=READ_ERROR"),"READ_ONLY_REPORT_HAS_REJECTION_AND_CONSUMER_TRACE");
            for(final String table:Arrays.asList(HistoricalGenomeImportR967.SOURCE,HistoricalGenomeImportR967.ARCHIVE,HistoricalGenomeImportR967.USE)){
                final String key=table.equals(HistoricalGenomeImportR967.SOURCE)?"asset_name":table.equals(HistoricalGenomeImportR967.ARCHIVE)?"reason":"reason";
                rejects(()->d.execSQL("UPDATE "+table+" SET "+key+"="+key),"IMMUTABLE_UPDATE:"+table);
                rejects(()->d.execSQL("DELETE FROM "+table),"IMMUTABLE_DELETE:"+table);
                rejects(()->d.execSQL("INSERT OR REPLACE INTO "+table+" SELECT * FROM "+table+" LIMIT 1"),"IMMUTABLE_REPLACE_WITH_RECURSIVE_TRIGGERS_OFF:"+table);
            }
            long trained=0;String trainingStatus="";
            for(int step=0;step<24&&trained==0;step++) {
                ResearchDynamicBootstrapR922.Result r=ResearchDynamicBootstrapR922.pulse(store,1000);trainingStatus=r.status;
                if(trainingStatus.startsWith("ERROR:"))throw new AssertionError("ACTUAL_DEV_TRAIN_OWNER_ERROR:"+trainingStatus);
                try(Cursor c=d.rawQuery("SELECT state_value FROM app_state WHERE state_key LIKE 'r959_development_job_%'",null)){
                    while(c.moveToNext())try{trained+=new org.json.JSONObject(c.getString(0)).optLong("n",0);}catch(org.json.JSONException bad){throw new AssertionError(bad);}
                }
            }
            check(trained>0,"HISTORICAL_SEED_ACTUAL_DEV_OWNER_NUMERIC_TRAINING:updates="+trained+":"+trainingStatus);
            // ID collision is isolated in a separate source; the current program is not overwritten.
            String current=text(d,"SELECT genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" LIMIT 1");
            String id=text(d,"SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" LIMIT 1");
            d.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET genome_id=?",new Object[]{original[0]});
            final HistoricalGenomeImportR967.Archive one=archive(a.header,Collections.singletonList(sample));
            check(batch(d,one,4)==1,"COLLIDING_OLD_RECORD_ARCHIVED_FOR_AUDIT");
            check(text(d,"SELECT reason FROM "+HistoricalGenomeImportR967.ARCHIVE+" WHERE source_hash='"+one.hash+"'").equals("CURRENT_ID_PROGRAM_COLLISION"),"SAME_ID_DIFFERENT_PROGRAM_REJECTED");
            check(current.equals(text(d,"SELECT genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" LIMIT 1")),"COLLISION_NEVER_OVERWRITES_LIVE_MODEL");
            d.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET genome_id=?",new Object[]{id});
            List<String> duplicate=Arrays.asList(sample,modified(a.rows.get(1),0,original[0]));HistoricalGenomeImportR967.Archive clash=archive(a.header,duplicate);
            check(batch(d,clash,4)==2&&number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.ARCHIVE+" WHERE source_hash='"+clash.hash+"' AND reason='SOURCE_ID_PROGRAM_COLLISION'")==2,"INTRA_SOURCE_ID_COLLISION_BOTH_RECORDS_ISOLATED");
            d.execSQL("DROP TRIGGER "+HistoricalGenomeImportR967.ARCHIVE+"_no_replace");
            check(!SchemaDomainCoordinatorR926.matches(d,SchemaDomainCoordinatorR926.researchContract()),"SAME_REVISION_MISSING_HISTORY_GUARD_NOT_READY");
            check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"SAME_REVISION_IMMUTABILITY_GUARD_REPAIRED");
            check(number(d,"SELECT COUNT(*) FROM "+HistoricalGenomeImportR967.ARCHIVE)==128,"SCHEMA_REPAIR_PRESERVES_ALL_HISTORICAL_ROWS");
            ResearchFunnelR967.Snapshot snap=ResearchFunnelR967.capture(store);check(snap.error.isEmpty()&&snap.values.get("historical_seed_created_n0")==1L,"FULL_FUNNEL_RECONCILES_REAL_HISTORY_USE");
            System.out.println("R970_HISTORICAL_MIGRATION_PASS checks="+checks+" source_rows=125 accepted=63 retired=62; actual_product_SQL_VM_creator=YES; OLD_VM_PROVEN=NO; FUTURE_WIN_RATE=NOT_PROVEN; ANDROID_DEVICE=NOT_RUN");
        }finally{d.shutdown();}
    }
}
