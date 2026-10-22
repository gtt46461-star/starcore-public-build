package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Exercises proof-gated invented-program learning and the atomic SQLite commit boundary. */
public final class AlgorithmSettlementHarnessR943 {
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){c.moveToFirst();return c.getLong(0);}}
    static String features(){StringBuilder v=new StringBuilder("R908F1");for(int i=0;i<14;i++)v.append('|').append(Long.toHexString(Double.doubleToLongBits(.3)));return v.toString();}
    static CandidateExecutionAuditR932.Proof proof(){
        CandidateExecutionAuditR932.Builder a=new CandidateExecutionAuditR932.Builder();
        for(int i=0;i<1200;i++)a.add(i,1200-i,1200-i,i);
        return a.finish(0,"FIXTURE_PROGRAM","FIXTURE_PROGRAM","FIXTURE_STATE","FIXTURE_STATE","FIXTURE_STATE");
    }
    static void prediction(SQLiteDatabase db,AlgorithmGenomeR908 genome,String base,String target,boolean completeProof){
        String f=features(),featureHash=V51ChecksumEngine.sha256("R908_ALG_FEATURE|"+f);
        String record=V51ChecksumEngine.sha256("R943_FIXTURE|"+genome.genomeId+'|'+target+'|'+featureHash);
        db.beginTransaction();try{
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+"(genome_id,target_issue,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash,frozen_at,record_hash) VALUES(?,?,1,1,2,3,0,.3,?,?,1,?)",
                new Object[]{genome.genomeId,target,f,featureHash,record});
            if(completeProof){
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+"(candidate_id,target_issue,phase,position,n1,n2,n3,selected_ordinal,score,probability,formula_probability,adaptive_probability,adaptive_artifact_hash,adaptive_trained_until,adaptive_updates,adaptive_weight,formula_hash,input_hash,frozen_at,record_hash) VALUES(?,?,?,1,1,2,3,0,.3,-1,-1,-1,'','',0,0,?,?,1,?)",
                    new Object[]{genome.genomeId,target,"R908_INVENTED_ALGORITHM_FUTURE_OOS",genome.structuralHash,featureHash,record});
                proof().freeze(db,"algorithm:"+genome.genomeId,genome.structuralHash,featureHash,base,target,record);
                ResearchCaptureLedgerR932.record(db,target,"algorithm:"+genome.genomeId,"FROZEN","SYNTHETIC_FULL1200_FIXTURE",record,1,true);
            }
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609130103",System.currentTimeMillis())){
            DbStore store=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);
            db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER NOT NULL DEFAULT 0)");
            check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"ALGORITHM_SETTLEMENT_SCHEMA_READY");
            AlgorithmGenomeR908 genome=AlgorithmGenomeR908.seed(943L,1,"");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_GENOME+"(genome_id,display_name_zh,genome_spec,structural_hash,generation,state_blob,status,created_at,updated_at) VALUES(?,?,?,?,1,?,'IDEA_WAIT_FUTURE_OOS',1,1)",
                new Object[]{genome.genomeId,genome.chineseName(),genome.canonical(),genome.structuralHash,new AlgorithmGenomeVmR908.State().encode()});
            db.beginTransaction();try{AlgorithmProofRecoveryR948.requireCleanForFreeze(db,genome.genomeId);db.setTransactionSuccessful();}finally{db.endTransaction();}

            Models.Draw draw=new Models.Draw();for(int i=0;i<10;i++)draw.numbers[i]=i+1;
            draw.issue="202609130100";prediction(db,genome,"202609130059",draw.issue,false);
            AlgorithmInventionLabR908.settleDraw(store,draw);
            check(scalar(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"NO_PROOF_CAPTURE_CANNOT_TRAIN_CREATED_ALGORITHM");

            draw.issue="202609130101";prediction(db,genome,"202609130100",draw.issue,true);
            db.execSQL("CREATE TRIGGER fail_state BEFORE UPDATE ON "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" BEGIN SELECT RAISE(ABORT,'INJECTED_STATE_WRITE_FAILURE'); END");
            AlgorithmInventionLabR908.settleDraw(store,draw);
            check(scalar(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0&&scalar(db,"SELECT hit_label FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE target_issue='202609130101'")==-1,"STATE_WRITE_FAILURE_ROLLS_BACK_LABEL_AND_MEMORY");
            db.execSQL("DROP TRIGGER fail_state");
            AlgorithmInventionLabR908.settleDraw(store,draw);
            check(scalar(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==1&&scalar(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==1,"CREATED_ALGORITHM_ACTUALLY_LEARNS_PROOFED_FROZEN_LABEL");
            AlgorithmInventionLabR908.settleDraw(store,draw);
            check(scalar(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==1,"ALGORITHM_RETRY_NEVER_DOUBLE_UPDATES");

            draw.issue="202609130102";prediction(db,genome,"202609130101",draw.issue,true);AlgorithmInventionLabR908.settleDraw(store,draw);
            check(scalar(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==2,"NEXT_ISSUE_CONTINUES_EXISTING_PROGRAM_MEMORY");
            System.out.println("R943_ALGORITHM_SETTLEMENT_PASS|scope=REAL_PROGRAM_REAL_SQLITE_SYNTHETIC_PROOFED_FROZEN_FEATURES");
        }finally{db.shutdown();}
    }
}
