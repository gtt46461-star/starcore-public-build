package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;

/** Actual production quota recovery and DDL on native SQLite. No store stubs. */
public final class HypothesisQuotaR32Host {
    static final String JOURNAL="r32_hypothesis_quota_recount_v1";
    static final String[] TABLES={"r31_hypothesis_registry","r31_hypothesis_binding","r31_hypothesis_select","r31_hypothesis_outcome"};
    static final String[] ORDER={"rowid","campaign_id,child_policy","campaign_id,policy_hash,sample_index","campaign_id,child_policy"};
    static int checks,slices,rollbacks,maxRows;
    static void need(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    static final class CountDb extends SQLiteDatabase {
        int quotaRows;
        CountDb(String path)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+path));}
        @Override public Cursor rawQuery(String sql,String[] args){Cursor c=super.rawQuery(sql,args);if(sql.contains(" FROM r31_hypothesis_")&&sql.endsWith(" LIMIT 64"))quotaRows+=c.getCount();return c;}
    }
    static CountDb open(String path)throws Exception{return new CountDb(path);}
    static String value(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args.length==0?null:args)){return c.moveToFirst()?c.getString(0):null;}}
    static long number(SQLiteDatabase db,String sql){return Long.parseLong(value(db,sql));}
    static String journal(SQLiteDatabase db){return value(db,"SELECT state_value FROM app_state WHERE state_key=?",JOURNAL);}
    static String originals(SQLiteDatabase db){List<Object> all=new ArrayList<>();for(int n=0;n<TABLES.length;n++){List<Object> rows=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT * FROM "+TABLES[n]+" ORDER BY "+ORDER[n],null)){while(c.moveToNext()){List<Object> cells=new ArrayList<>();for(int i=0;i<c.getColumnCount();i++)cells.add(c.getString(i));rows.add(cells);}}all.add(rows);}return StatsR27.sha256(JsonR27.write(all));}
    static void schema(CountDb db){db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");db.beginTransaction();try{HypothesisStoreR31.installWithinMigrationR31(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static String payload(String kind,int index){return kind+"-中文轮胎😀-"+index;}
    static void registry(SQLiteDatabase db,String id,String payload){db.execSQL("INSERT INTO r31_hypothesis_registry VALUES(?,?,?,?,?,?,?,?)",new Object[]{id,"parent","run","F03","REGISTERED",payload,StatsR27.sha256(payload),17});}
    static long seed(CountDb db){long expected=0;db.beginTransaction();try{
        for(int i=0;i<66;i++){String text=payload("registry",i);registry(db,String.format(Locale.ROOT,"h-%03d",i),text);expected+=1024+text.getBytes(StandardCharsets.UTF_8).length;}
        for(int i=0;i<65;i++){db.execSQL("INSERT INTO r31_hypothesis_binding VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{"c-"+(i/33),String.format(Locale.ROOT,"b-%02d",i%33),"h-000","control","snapshot",0,64,"ordinal-digest",600});expected+=1024;}
        for(int i=0;i<131;i++){db.execSQL("INSERT INTO r31_hypothesis_select VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{"c-"+(i/70),"p-"+(i%3),i%70,2000+i,"snapshot",1,7,1,i%2,0.3,"trace-"+i});expected+=1024;}
        for(int i=0;i<64;i++){String text=payload("outcome",i);db.execSQL("INSERT INTO r31_hypothesis_outcome VALUES(?,?,?,?,'SELECT',?,?,?,?,?,?,?,?)",new Object[]{"c-"+(i/31),String.format(Locale.ROOT,"o-%02d",i%31),"h-000","F03","PAIRED_SELECT_COMPLETE",64,0.0,0.0,0.0,text,StatsR27.sha256(text),99});expected+=1024+text.getBytes(StandardCharsets.UTF_8).length;}
        db.setTransactionSuccessful();
    }finally{db.endTransaction();}return expected;}
    static void expectFailure(Runnable work,String code){boolean rejected=false;try{work.run();}catch(RuntimeException failure){rejected=failure.toString().contains(code);}need(rejected,"must refuse "+code);}
    static boolean slice(CountDb db,boolean commit){db.quotaRows=0;db.beginTransaction();boolean done;try{done=HypothesisStoreR31.recoverQuotaSliceR32(db);if(commit)db.setTransactionSuccessful();}finally{db.endTransaction();}need(db.quotaRows<=64,"one recovery transaction scans at most 64 original rows");maxRows=Math.max(maxRows,db.quotaRows);if(commit)slices++;else rollbacks++;return done;}
    static void invalidState(CountDb db,String text,String code,String originalHash){String good=journal(db);db.execSQL("UPDATE app_state SET state_value=? WHERE state_key=?",new Object[]{text,JOURNAL});db.beginTransaction();try{expectFailure(()->HypothesisStoreR31.recoverQuotaSliceR32(db),code);}finally{db.endTransaction();}need(text.equals(journal(db)),"rejected journal is retained, not reset");need(originalHash.equals(originals(db)),"malformed journal never mutates any original row");need(number(db,"SELECT count(*) FROM r31_hypothesis_quota")==0,"malformed journal cannot create quota");db.execSQL("UPDATE app_state SET state_value=? WHERE state_key=?",new Object[]{good,JOURNAL});}
    static String signedState(Object stage,Object used,List<?> last){List<Object> v=new ArrayList<>(Arrays.asList(stage,used,last));v.add(StatsR27.sha256(JsonR27.write(v)));return JsonR27.write(v);}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");String file=args[1];
        if(args[0].equals("halt")){try(CountDb db=open(file)){db.beginTransaction();HypothesisStoreR31.recoverQuotaSliceR32(db);Runtime.getRuntime().halt(73);}throw new AssertionError("halt must terminate");}
        String originalHash;long expected;
        try(CountDb db=open(file)){
            schema(db);expectFailure(()->HypothesisStoreR31.recoverQuotaSliceR32(db),"EXISTING_WRITER_REQUIRED");expected=seed(db);originalHash=originals(db);
            need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota WHERE singleton=1")==expected,"original account triggers match independent UTF8 byte total");
            need(payload("registry",0).getBytes(StandardCharsets.UTF_8).length>payload("registry",0).length(),"fixture actually distinguishes UTF8 bytes from characters");
            for(int i=0;i<4;i++)need(number(db,"SELECT count(*) FROM "+TABLES[i])==new int[]{66,65,131,64}[i],"all four tables seeded around page boundaries "+i);
            db.execSQL("DELETE FROM r31_hypothesis_quota"); // fixture-only legacy missing singleton
            need(!HypothesisStoreR31.canAdmitResearchR31(db,64),"factory admission rejects missing quota");
            expectFailure(()->HypothesisStoreR31.requireSchemaR31(db),"QUOTA_MISSING");
            expectFailure(()->registry(db,"blocked-new","new bytes"),"PROTECTED_STORAGE_WAIT");
            registry(db,"h-000",payload("registry",0));need(originalHash.equals(originals(db)),"same-key identical replay remains harmless while quota missing");
            expectFailure(()->db.execSQL("UPDATE r31_hypothesis_registry SET payload='changed' WHERE hypothesis_id='h-000'"),"ORIGINAL_IMMUTABLE");
            expectFailure(()->db.execSQL("DELETE FROM r31_hypothesis_binding"),"ORIGINAL_IMMUTABLE");
        }
        Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),HypothesisQuotaR32Host.class.getName(),"halt",file).inheritIO().start();
        need(child.waitFor()==73,"real child JVM killed after recovery write before commit");
        try(CountDb db=open(file)){
            need(journal(db)==null&&number(db,"SELECT count(*) FROM r31_hypothesis_quota")==0,"uncommitted quota cursor disappears after process death");
            need(originalHash.equals(originals(db)),"process death preserves all original rows");
            need(!slice(db,true),"first 64-row page remains pending");String good=journal(db);List<?> parsed=(List<?>)JsonR27.parse(good);
            need(((Number)parsed.get(0)).intValue()==0&&((List<?>)parsed.get(2)).equals(Arrays.asList("64")),"first durable registry cursor is actual rowid 64");
            List<Object> corrupted=new ArrayList<>((List<?>)parsed);corrupted.set(1,((Number)parsed.get(1)).longValue()+1);
            invalidState(db,JsonR27.write(corrupted),"R32_QUOTA_RECOUNT_JOURNAL",originalHash);
            invalidState(db,"[]","R32_QUOTA_RECOUNT_JOURNAL",originalHash);
            invalidState(db,signedState(4,0,Collections.emptyList()),"R32_QUOTA_RECOUNT_RANGE",originalHash);
            invalidState(db,signedState(0,-1,Collections.emptyList()),"R32_QUOTA_RECOUNT_RANGE",originalHash);
            invalidState(db,signedState(0,268435457L,Collections.emptyList()),"R32_QUOTA_RECOUNT_RANGE",originalHash);
            invalidState(db,signedState(1,0,Arrays.asList("only-one-key")),"R32_QUOTA_RECOUNT_RANGE",originalHash);
        }
        boolean done=false;int iterations=0;
        while(!done&&iterations++<30){
            String before;
            try(CountDb db=open(file)){before=journal(db);slice(db,false);need(Objects.equals(before,journal(db)),"rollback does not advance cursor/sum");need(number(db,"SELECT count(*) FROM r31_hypothesis_quota")==0,"rollback never publishes quota");need(originalHash.equals(originals(db)),"rolled-back slice preserves original bytes");}
            try(CountDb db=open(file)){done=slice(db,true);need(originalHash.equals(originals(db)),"committed slice preserves original bytes");if(!done){need(journal(db)!=null,"unfinished slice has durable restart cursor");need(!HypothesisStoreR31.canAdmitResearchR31(db,64),"admission stays closed until final quota commit");}}
        }
        need(done,"all four tables finish within bounded slices");need(maxRows==64,"test actually exercised full 64-row pages");
        try(CountDb db=open(file)){
            HypothesisStoreR31.requireSchemaR31(db);need(journal(db)==null,"final quota and journal deletion committed together");
            need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota WHERE singleton=1")==expected,"cross-page recovery reproduces exact independent logical bytes");
            need(number(db,"SELECT count(*) FROM r31_hypothesis_quota")==1,"exactly one quota singleton");
            for(int i=0;i<2;i++){db.beginTransaction();try{HypothesisStoreR31.seedQuotaWithinMigrationR31(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
            need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota WHERE singleton=1")==expected,"production seed readiness is idempotent after recovery");
            need(HypothesisStoreR31.canAdmitResearchR31(db,64),"ordinary factory admission reopens after exact accounting");
            registry(db,"h-000",payload("registry",0));need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota WHERE singleton=1")==expected,"duplicate original does not charge twice");
            expectFailure(()->registry(db,"h-000","different payload"),"ORIGINAL_CONFLICT");need(originalHash.equals(originals(db)),"conflicting same key preserves every original");
            String next=payload("new-admitted",700);registry(db,"new-after-recovery",next);need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota WHERE singleton=1")==expected+1024+next.getBytes(StandardCharsets.UTF_8).length,"next actual admitted insert resumes original UTF8 accounting");
        }
        try(CountDb db=open(file+".empty")){schema(db);db.execSQL("DELETE FROM r31_hypothesis_quota");need(slice(db,true),"empty four-table store finishes one transaction");need(number(db,"SELECT logical_bytes FROM r31_hypothesis_quota")==0,"empty store starts at exact zero");HypothesisStoreR31.requireSchemaR31(db);}
        System.out.println("HYPOTHESIS_QUOTA_NATIVE_PASS assertions="+checks+" committedSlices="+slices+" rolledBackSlices="+rollbacks+" maxDataRowsPerSlice="+maxRows+" realKillRestart=true");
    }
}
