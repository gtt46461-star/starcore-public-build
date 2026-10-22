package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;

/** Adversarial synthetic draws exercise real error collection, training, and repair lineage. */
public final class FailureRepairHarnessR958 {
    private static int checks;
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void draw(SQLiteDatabase db,String issue,int winner,long at) {
        Object[] values=new Object[14];values[0]=issue;for(int i=0;i<10;i++)values[i+1]=(winner-1+i)%10+1;values[11]=0;values[12]=at;values[13]=at+1;
        db.execSQL("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",values);
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try {
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER)");
            String base="202609190001";long clock=System.currentTimeMillis()-10000000;
            for(int i=0;i<120;i++){draw(db,base,i%10+1,clock+i*60000);if(i<119)base=TargetIssueResolver.next(base).targetIssue;}
            for(int i=0;i<120;i++) {
                String target=TargetIssueResolver.next(base).targetIssue;
                if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("FAILURE_TEST|"+target),8)!=1)throw new AssertionError("TRAIN_FREEZE_"+i);
                int mask=(int)count(db,"SELECT prediction_mask FROM r952_candidate_predraw WHERE target_issue='"+target+"'");
                int actual=1;while(AppConfig.maskContainsNumber(mask,actual))actual++;
                long at=System.currentTimeMillis()+1;draw(db,target,actual,at);String hash=V51ChecksumEngine.sha256("TERMINAL|"+target);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{target,hash,at+1});
                ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash,at+1);
                if(!ResearchOutboxR952.drainOne(store))throw new AssertionError("TRAIN_COMMIT_"+i);
                base=target;
            }
            check(count(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE hit=0 AND settled=1")==120,"ACTUAL_FROZEN_ERRORS_COLLECTED");
            check(count(db,"SELECT COUNT(*) FROM r952_candidate WHERE parent_id<>''")==1,"ACTUAL_ERROR_WINDOW_CREATES_ONE_CHILD");
            check(count(db,"SELECT future_n FROM r952_candidate WHERE parent_id<>''")==0,"REPAIR_CHILD_DOES_NOT_INHERIT_PARENT_SUPPORT");
            check(count(db,"SELECT COUNT(*) FROM r952_artifact a JOIN r952_candidate c ON a.candidate_id=c.candidate_id WHERE c.parent_id<>''")==0,"REPAIR_CHILD_DOES_NOT_INHERIT_MODEL_ARTIFACT");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.CHILD+" WHERE kind='PARAMETER_VARIANT' AND length(changed_modules)>0 AND length(changed_atoms)>0 AND length(expected_effect)>0 AND length(falsification)>0 AND repair_target LIKE '%|%' AND outcome='PENDING'")==1,"ACTUAL_REPAIR_LINEAGE_AND_FALSIFICATION_PERSISTED");
            check(EvolutionControllerR952.evolveOne(store,base).equals("WAIT_FUTURE_EVIDENCE"),"REPEATED_EVOLUTION_DOES_NOT_CREATE_DUPLICATE_CHILD");
            check(EvolutionControllerR952.evolveOne(store,TargetIssueResolver.next(base).targetIssue).equals("WAIT_AUTHORITATIVE_SETTLEMENT"),"PRE_SETTLEMENT_MUTATION_REFUSED");
            for(int generation=1;generation<=2;generation++) {
                String learner=text(db,"SELECT candidate_id FROM r952_candidate WHERE future_n=0 ORDER BY created_at DESC LIMIT 1");
                for(int i=0;i<120;i++) {
                    String target=TargetIssueResolver.next(base).targetIssue;
                    if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("NEXT_GENERATION|"+target),8)!=1)throw new AssertionError("GENERATION_CAPTURE:"+generation+":"+i);
                    int mask=(int)count(db,"SELECT prediction_mask FROM r952_candidate_predraw WHERE candidate_id='"+learner+"' AND target_issue='"+target+"'");
                    boolean hit=generation==1&&i>=30&&i<100;int actual=1;
                    while(AppConfig.maskContainsNumber(mask,actual)!=hit)actual++;
                    long at=System.currentTimeMillis()+1;draw(db,target,actual,at);String hash=V51ChecksumEngine.sha256("TERMINAL|"+target);
                    db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?)",new Object[]{target,hash,at+1});
                    ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(target),hash,at+1);
                    if(!ResearchOutboxR952.drainOne(store))throw new AssertionError("GENERATION_TRAIN:"+generation+":"+i);base=target;
                }
                check(count(db,"SELECT COUNT(*) FROM r952_candidate")==generation+2,"REVERSING_FAILURE_PATTERN_CONTINUES_GENERATION:"+generation);
                check(count(db,"SELECT COUNT(*) FROM r952_candidate WHERE parent_id='"+learner+"' AND future_n=0")==1,"NEW_GENERATION_NO_INHERITED_LABELS:"+generation);
            }
            check(count(db,"SELECT COUNT(DISTINCT structural_hash) FROM r952_candidate")==4,"THREE_REAL_TRAINED_GENERATIONS_PRODUCE_FOUR_UNIQUE_STRUCTURES");
            CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,1,120);
            boolean rejected=false;try{MutationPolicyR952.propose("parent",ast,V51ChecksumEngine.sha256("fake-counts"));}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"HASH_OF_COUNTS_IS_NOT_FAILURE_EVIDENCE");
            rejected=false;try{MutationPolicyR952.propose("parent",ast,V51ChecksumEngine.sha256("no-errors"),0,0);}catch(IllegalArgumentException expected){rejected=true;}
            check(rejected,"NO_ERROR_CANNOT_CLAIM_FAILURE_REPAIR");
            System.out.println("R958_FAILURE_REPAIR_PASS|checks="+checks+"|scope=ACTUAL_LEARNING_ERROR_LINEAGE_SYNTHETIC_NOT_EFFECT");
        }finally{db.shutdown();}
    }
}
