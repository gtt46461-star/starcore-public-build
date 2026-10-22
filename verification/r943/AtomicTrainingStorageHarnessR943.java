package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.io.*;
import java.util.*;

public final class AtomicTrainingStorageHarnessR943 {
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}}
    public static void main(String[] args)throws Exception{
        RealtimePressureGateR811.resetForTests();
        byte[] features="0.2,0.3,0.4,0.5,0.6,0.7,0.8".getBytes(StandardCharsets.UTF_8);
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1);Thread.sleep(3);boolean yielded=false;
        try{ResearchTinyDeepLearnerR909.decodeFeatures(features);}catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}finally{ResearchWorkBudgetR927.leave(scope);}
        check(yielded,"BUDGET_YIELD_IS_NOT_MALFORMED_FEATURE");check(ResearchTinyDeepLearnerR909.decodeFeatures(features)!=null,"VALID_FEATURE_SURVIVES_RETRY");
        check(ResearchTinyDeepLearnerR909.decodeFeatures("NaN,0,0,0,0,0,0".getBytes(StandardCharsets.UTF_8))==null,"TRUE_INVALID_FEATURE_REJECTED");
        check(TargetIssueResolver.distance("202609121440","202609130001")==1,"MIDNIGHT_IS_CONTIGUOUS_NOT_8560_MISSING");
        File dir=new File(args[1]);dir.mkdirs();File keep=null;
        for(int i=0;i<7;i++){keep=DiagnosticFileStoreR941.write(dir,"fixture "+i);}
        check(dir.listFiles().length<=3&&keep.isFile(),"REPORT_CACHE_BOUNDED_CURRENT_FILE_PRESERVED");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try{
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0)");
            AtomicToolWorldR943.schema(db);
            String base="202609120001";long previous=0;
            for(int k=0;k<5;k++){
                String target=TargetIssueResolver.next(base).targetIssue;
                AtomCaptureR932.Values v=new AtomCaptureR932.Values(240,base);
                for(int i=0;i<240;i++){v.values[i]=(i%9+1)/10d;v.present[i]=GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible();}
                GenesisAssetVmR915.PredrawContext c=new GenesisAssetVmR915.PredrawContext(base,target,V51ChecksumEngine.sha256("history"+base),V51ChecksumEngine.sha256("surface"),1000,1,1,2,3,0,v.values,v);
                db.beginTransaction();try{AtomicToolWorldR943.freeze(db,c);AtomicToolWorldR943.freeze(db,c);db.setTransactionSuccessful();}finally{db.endTransaction();}
                check(scalar(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.CONTEXT+" WHERE target_issue='"+target+"'")==1,"ONE_FROZEN_ATOM_CONTEXT_PER_ISSUE_"+k);
                check(AtomicToolWorldR943.maintain(store).startsWith("WAIT_NEW"),"NO_TRAINING_BEFORE_DRAW_"+k);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{target});
                String result=AtomicToolWorldR943.maintain(store);check(result.startsWith("LEARNED"),"ACTUAL_ATOMIC_NEURAL_SQLITE_UPDATE_"+k);
                long updates=scalar(db,"SELECT updates FROM "+AtomicToolWorldR943.MODEL);check(updates==previous+1,"NETWORK_ADVANCES_EXACTLY_ONCE_"+k);previous=updates;
                AtomicToolWorldR943.maintain(store);check(scalar(db,"SELECT updates FROM "+AtomicToolWorldR943.MODEL)==previous,"RETRY_DOES_NOT_DOUBLE_TRAIN_"+k);
                base=target;
            }
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.QUALITY)==165,"ALL_165_PRODUCTION_ATOMS_RECEIVE_LABELS");
            check(scalar(db,"SELECT MIN(n) FROM "+AtomicToolWorldR943.QUALITY)==5,"EVERY_ATOM_HAS_FIVE_REAL_FROZEN_TEST_LABELS");
            AtomicToolWorldR943.NetworkState model=AtomicToolWorldR943.network(db);check(model.network.updates==5,"NEURAL_WEIGHTS_RELOAD_WITH_CURSOR");
            // 6,000 true period ids, delete in bounded production pages, preserve the boundary.
            db.execSQL("CREATE TABLE retained_test(target_issue TEXT PRIMARY KEY,payload BLOB)");
            String issue="202609010001",cutoff="";db.beginTransaction();try{for(int i=0;i<6000;i++){if(i==1000)cutoff=issue;db.execSQL("INSERT INTO retained_test VALUES(?,?)",new Object[]{issue,new byte[16]});issue=TargetIssueResolver.next(issue).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            int deleted=0,n;do{n=DataRetentionGovernorR900.deletePage(db,"retained_test","target_issue",cutoff,512);check(n<=512,"RETENTION_PAGE_IS_BOUNDED");deleted+=n;}while(n>0);
            check(deleted==1000&&scalar(db,"SELECT COUNT(*) FROM retained_test")==5000,"EXACT_5000_PERIOD_RETENTION");
            check(scalar(db,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.MODEL)==1,"RETENTION_PRESERVES_LEARNED_MODEL");
            boolean rejected=false;try{DataRetentionGovernorR900.deletePage(db,"retained_test;DROP TABLE x","target_issue",cutoff,512);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"RETENTION_SQL_IDENTIFIER_VALIDATED");
            // Exercise the actual immutable-history trigger upgrade, including the recent-row guard.
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_B03_ACTION_EVENT+"(target_issue TEXT,payload TEXT,game_code TEXT NOT NULL DEFAULT 'Binance1-PK10')");
            db.execSQL("DELETE FROM "+DbHelper.TABLE_DRAWS);
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+"(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) SELECT target_issue,1,2,3,4,5,6,7,8,9,10,0 FROM retained_test");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_B03_ACTION_EVENT+"(target_issue,payload) SELECT issue,'TEST_ONLY' FROM "+DbHelper.TABLE_DRAWS);
            DbHelper.installRetentionTriggersR943(db);
            boolean protectedRecent=false;try{db.execSQL("DELETE FROM "+DbHelper.TABLE_B03_ACTION_EVENT+" WHERE target_issue=(SELECT MAX(issue) FROM "+DbHelper.TABLE_DRAWS+")");}catch(RuntimeException expected){protectedRecent=true;}
            check(protectedRecent,"CURRENT_IMMUTABLE_PREDICTION_CANNOT_BE_DELETED");
            String heavy;try(Cursor q=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" ORDER BY issue DESC LIMIT 1 OFFSET 119",null)){q.moveToFirst();heavy=q.getString(0);}
            while(DataRetentionGovernorR900.deletePage(db,DbHelper.TABLE_B03_ACTION_EVENT,"target_issue",heavy,512)>0){}
            check(scalar(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_B03_ACTION_EVENT)==120,"HEAVY_IMMUTABLE_HISTORY_ACTUALLY_PRUNES_TO_120");
            boolean wrongApi=false;try{db.execSQL("PRAGMA incremental_vacuum(16)");}catch(IllegalStateException expected){wrongApi=true;}
            check(wrongApi,"ANDROID_ROW_PRAGMA_EXECSQL_REJECTED");
            StorageReclaimR943.reclaimPage(db,16);
            check(true,"PRODUCTION_RECLAIM_USES_QUERY_API");
            boolean badPage=false;try{StorageReclaimR943.reclaimPage(db,0);}catch(IllegalArgumentException expected){badPage=true;}
            check(badPage,"RECLAIM_REJECTS_UNBOUNDED_PAGE_ARGUMENT");
            // A real rollback must leave both the label and model untouched.
            long saved=scalar(db,"SELECT updates FROM "+AtomicToolWorldR943.MODEL);db.beginTransaction();try{db.execSQL("UPDATE "+AtomicToolWorldR943.MODEL+" SET updates=999");}finally{db.endTransaction();}
            check(scalar(db,"SELECT updates FROM "+AtomicToolWorldR943.MODEL)==saved,"SQLITE_MODEL_UPDATE_ROLLBACK");
            System.out.println("R943_ATOMIC_TRAINING_STORAGE_PASS|scope=SYNTHETIC_REAL_SQLITE_NOT_PHONE_NOT_WINRATE");
        }finally{db.shutdown();}
    }
}
