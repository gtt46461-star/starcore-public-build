package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;
import java.util.*;

/** Synthetic persisted freezes, real VM/full1200 audit/coordinator/settlement owners.
 * Never a source of natural Future, qualification or user runtime evidence. */
public final class HistoricalOutcomeR79 {
    static int checks;
    static void ok(boolean b,String why){if(!b)throw new AssertionError(why);checks++;}
    static String h(String s){return V51ChecksumEngine.sha256(s);}
    static long n(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getLong(0);}}
    static String s(SQLiteDatabase d,String q){try(Cursor c=d.rawQuery(q,null)){ok(c.moveToFirst(),q);return c.getString(0);}}
    static String target(int i){return "20261015"+String.format(Locale.ROOT,"%04d",100+i);}
    static String genome(SQLiteDatabase d){return s(d,"SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" LIMIT 1");}
    @SuppressWarnings("unchecked") static List<String> pending(DbStore store)throws Exception{
        Method m=ResearchPredrawSettlementPumpR908.class.getDeclaredMethod("pendingIssues",DbStore.class,int.class);m.setAccessible(true);
        return (List<String>)m.invoke(null,store,32);
    }
    static void seed(SQLiteDatabase db)throws Exception {
        AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(202610150100L,1,"SYNTHETIC_R79_HISTORICAL_OUTCOME");
        AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(g);
        db.beginTransaction();try{
            AlgorithmProofRecoveryR948.schema(db);
            for(int j=0;j<100;j++){
                String prior=j==0?"202610141440":"20261015"+String.format(Locale.ROOT,"%04d",j);
                db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty) VALUES(?,?,?,1,2,3,4,5,6,7,8,9,10,0)",
                        new Object[]{prior,System.currentTimeMillis()-6000000L+j*60000,System.currentTimeMillis()-6000000L+j*60000+1});
            }
            AlgorithmInventionLabR908.registerDirected(db,g,"202610150099","202610150100","TEST","ISOLATED_HOST_FIXTURE");
            db.execSQL("INSERT INTO "+AlgorithmProofRecoveryR948.HEAD+" VALUES(?,'RESET_CLEAN','',8,1,?,?,?,'SYNTHETIC_RESET_EPOCH',?)",
                    new Object[]{g.genomeId,h(state.encode()),h("audit"),"202610150200",System.currentTimeMillis()});
            for(int k=0;k<8;k++){
                String issue=target(k),base="20261015"+String.format(Locale.ROOT,"%04d",99+k);
                CandidateExecutionAuditR932.Builder audit=new CandidateExecutionAuditR932.Builder();
                AlgorithmGenomeVmR908.Scorer scorer=new AlgorithmGenomeVmR908.Scorer(g,state);
                AlgorithmGenomeVmR908.State replay=AlgorithmGenomeVmR908.State.decode(state.encode());
                int best=-1,position=0,n1=0,n2=0,n3=0;double bestScore=-1;
                for(int p=1;p<=10;p++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
                    int ord=K3ActionSpaceEngine.globalOrdinal(p,a,b,c);double[] x=new double[AlgorithmGenomeR908.INPUTS];
                    for(int z=0;z<9;z++)x[z]=(ord+z)%997/997d;
                    double value=scorer.score(p,x),again=AlgorithmGenomeVmR908.score(g,replay,p,x);
                    audit.add(ord,value,again,ord);if(best<0||value>bestScore){best=ord;bestScore=value;position=p;n1=a;n2=b;n3=c;}
                }
                CandidateExecutionAuditR932.Proof proof=audit.finish(best,g.canonical(),g.canonical(),state.encode(),state.encode(),replay.encode());
                String features="R908F1";for(int z=0;z<AlgorithmGenomeR908.INPUTS;z++)features+="|0";
                String feature=h("R908_ALG_FEATURE|"+features),record=h("SYNTHETIC_PREDRAW|"+issue);long frozen=System.currentTimeMillis();
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                        +"(genome_id,target_issue,position,n1,n2,n3,selected_ordinal,score,feature_blob,feature_hash,frozen_at,record_hash,hit_label) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,-1)",
                        new Object[]{g.genomeId,issue,position,n1,n2,n3,best,bestScore,features,feature,frozen,record});
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R817_RESEARCH_PREDRAW
                        +"(candidate_id,target_issue,phase,position,n1,n2,n3,selected_ordinal,score,probability,formula_hash,input_hash,frozen_at,record_hash,hit_label)"
                        +" VALUES(?,?,'R908_INVENTED_ALGORITHM_FUTURE_OOS',?,?,?,?,?,?,-1,?,?,?,?,-1)",
                        new Object[]{g.genomeId,issue,position,n1,n2,n3,best,bestScore,g.structuralHash,feature,frozen,record});
                proof.freeze(db,"algorithm:"+g.genomeId,g.structuralHash,h("input"+issue),base,issue,record);
                ResearchCaptureLedgerR932.record(db,issue,"algorithm:"+g.genomeId,"FROZEN","SYNTHETIC_HOST",record,1,true);
                long opened=System.currentTimeMillis()+1;while(System.currentTimeMillis()<=opened)Thread.yield();
                Models.Draw draw=new Models.Draw();draw.issue=issue;draw.openTime=opened;draw.fetchTime=opened+1;
                draw.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};draw.sourceLine="SYNTHETIC_HOST";draw.rawJson="ISOLATED_NOT_PROVIDER_DATA";
                db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,source_line,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,first_seen_at,dirty,draw_hash_version,draw_hash_v2) VALUES(?,?,?, ?,1,2,3,4,5,6,7,8,9,10,?,0,?,?)",
                        new Object[]{draw.issue,draw.openTime,draw.fetchTime,draw.sourceLine,draw.fetchTime,DrawHashR36.CURRENT_VERSION,DrawHashR36.sha256V2(draw)});
                ok(DrawCanonicalizerR811.observeWithinWrite(db,draw).confirmed,"canonical exact draw accepted");
            }
            // A previously misclassified pre-epoch row retains its old quarantine receipt.
            AlgorithmProofRecoveryR948.quarantine(db,g.genomeId,target(1),"SETTLEMENT_REJECTED:VALID");
            // Four distinct negative cases: late clock, bad payload, actual quarantine, missing proof.
            db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" SET frozen_at=? WHERE target_issue=?",new Object[]{n(db,"SELECT open_time FROM draws WHERE issue='"+target(4)+"'"),target(4)});
            db.execSQL("UPDATE "+CandidateExecutionAuditR932.TABLE+" SET payload='corrupt' WHERE target_issue=?",new Object[]{target(5)});
            AlgorithmProofRecoveryR948.quarantine(db,g.genomeId,target(6),"UNVERIFIABLE_HISTORICAL_CAPTURE:BAD_ORIGINAL_INPUT");
            db.execSQL("DELETE FROM "+CandidateExecutionAuditR932.TABLE+" WHERE target_issue=?",new Object[]{target(7)});
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
        ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label=-1")==8,"original predictions retained");
    }
    public static void main(String[] args)throws Exception {
        try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
            DbStore store=HostProductR974.store(db);RealtimePressureGateR811.resetForTests();
            if("seed".equals(args[2])){seed(db);System.out.println("SYNTHETIC_FIXTURE_READY checks="+checks);return;}
            String id=genome(db);List<String> before=pending(store);
            if("baseline".equals(args[2])){ok(before.isEmpty(),"R78 excludes valid pre-epoch pending predictions");System.out.println("R78_BUG_REPRODUCED checks="+checks);return;}
            String state=s(db,"SELECT state_blob FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME),head=s(db,"SELECT evidence_hash FROM "+AlgorithmProofRecoveryR948.HEAD);
            ok(before.contains(target(0))&&before.contains(target(1))&&before.contains(target(2))&&before.contains(target(3)),"new queue admits original proof-bearing historical outcomes");
            ok(!before.contains(target(6))&&!before.contains(target(7)),"invalid quarantine and absent proof remain excluded");
            int rows=0;for(int pulse=0;pulse<10;pulse++){ResearchPredrawSettlementPumpR908.Result r=ResearchPredrawSettlementPumpR908.drainBatch(store,32,1600);rows+=r.rows;if(pending(store).isEmpty())break;}
            ok(rows==8,"four original and four mirrored valid labels settle through public pump: "+rows);
            ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label IN(0,1)")==4,"four authenticated historical outcomes");
            ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE hit_label IN(0,1)")==4,"mirror outcome closure");
            ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE hit_label=-1")==4,"all four invalid originals remain pending, none deleted or mislabeled");
            ok(n(db,"SELECT future_n FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"new Future count unchanged");
            ok(n(db,"SELECT state_updates FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==0,"new head not trained on old labels");
            ok(state.equals(s(db,"SELECT state_blob FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)),"new state bytes unchanged");
            ok(head.equals(s(db,"SELECT evidence_hash FROM "+AlgorithmProofRecoveryR948.HEAD)),"head integrity evidence unchanged");
            ok(n(db,"SELECT COUNT(*) FROM v57_research_candidate_outcome_r811")==0,"historical mirror never enters current training stream");
            ok(n(db,"SELECT COUNT(*) FROM research_certificate_r929")==0&&n(db,"SELECT COUNT(*) FROM trades")==0,"no qualification or REAL minted");
            ok(n(db,"SELECT COUNT(*) FROM "+AlgorithmProofRecoveryR948.QUARANTINE+" WHERE reason='SETTLEMENT_REJECTED:VALID'")==1,"old rejection retained as audit evidence");
            ok(ResearchPredrawSettlementPumpR908.drainBatch(store,32,1600).rows==0,"public replay is idempotent");
            ok(pending(store).isEmpty(),"remaining inputs are explicitly proof-blocked, not retried forever");
            System.out.println("R79_HISTORICAL_OUTCOME_PASS checks="+checks+"|scope=SYNTHETIC_ACTUAL_PRODUCT_HOST|valid=4|rejected=4|future_added=0|real=0");
        }
    }
}
