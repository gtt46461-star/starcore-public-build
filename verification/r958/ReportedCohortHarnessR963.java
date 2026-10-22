package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** User report supplies real executable definitions, only synthetic draws/labels are generated. */
public final class ReportedCohortHarnessR963 {
    static int checks;
    static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static int column(JSONObject table,String name)throws Exception{JSONArray cols=table.getJSONArray("columns");for(int i=0;i<cols.length();i++)if(cols.getString(i).equals(name))return i;throw new IllegalArgumentException(name);}
    static String value(JSONArray row,int column)throws Exception{return row.getString(column).replace("\\t","\t").replace("\\n","\n").replace("\\r","\r").replace("\\\\","\\");}
    public static void main(String[] args)throws Exception{
        JSONObject report=new JSONObject(new String(Files.readAllBytes(Paths.get("verification/r958/reported_r963_world.json")),"UTF-8"));
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=DevelopmentTrainingHarnessR959.init(db);Random random=new Random(963);String base="202609190001";
            db.beginTransaction();try{for(int i=0;i<320;i++){DevelopmentTrainingHarnessR959.draw(db,base,random);if(i<319)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            String target=TargetIssueResolver.next(base).targetIssue;
            JSONObject algorithms=report.getJSONObject(DbHelper.TABLE_R908_ALGORITHM_GENOME);JSONArray rows=algorithms.getJSONArray("rows");
            ArrayList<AlgorithmGenomeR908> genomes=new ArrayList<AlgorithmGenomeR908>();
            db.beginTransaction();try{
                for(int i=0;i<rows.length();i++){
                    AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(value(rows.getJSONArray(i),column(algorithms,"genome_spec")));genomes.add(g);
                    AlgorithmInventionLabR908.registerDirected(db,g,base,target,"REPORT_EXECUTABLE_TEST","SYNTHETIC_LABELS_ONLY");
                }
                JSONObject assets=report.getJSONObject(AssetGenesisEngineR915.TABLE_ASSET_GENOME);JSONArray ar=assets.getJSONArray("rows");
                for(int i=0;i<ar.length();i++)check(AssetGenesisEngineR915.registerDirected(db,GenesisAssetGenomeR915.parse(value(ar.getJSONArray(i),column(assets,"genome_spec"))),base,target),"REPORT_ASSET_REGISTER:"+i);
                JSONObject formulas=report.getJSONObject(DbHelper.TABLE_R906_RESEARCH_ASSET);JSONArray fr=formulas.getJSONArray("rows");
                for(int i=0;i<fr.length();i++){
                    ResearchMethodPlanR906 plan=ResearchMethodPlanR906.parseCanonical(value(fr.getJSONArray(i),column(formulas,"canonical_spec")));
                    ResearchAssetRegistryR906.Asset a=ResearchAssetRegistryR906.register(db,plan,null);
                    db.execSQL("UPDATE "+DbHelper.TABLE_R906_RESEARCH_ASSET+" SET trained_until_issue=?,earliest_use_issue=? WHERE asset_id=?",new Object[]{base,target,a.assetId});
                }
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            check(genomes.size()==29&&n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME)==18&&n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET)==70,"EXACT_REPORT_29_ALGORITHMS_18_ASSETS_70_FORMULAS");
            long oldNanos=0,newNanos=0;double sink=0;
            for(AlgorithmGenomeR908 g:genomes){
                AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.initialState(g);
                for(int p=0;p<10;p++)for(int k=0;k<state.position[p].length;k++)state.position[p][k]=random.nextDouble()-.5;
                for(int k=0;k<state.global.length;k++)state.global[k]=random.nextDouble()-.5;
                String initial=state.encode();AlgorithmGenomeVmR908.Scorer scorer=new AlgorithmGenomeVmR908.Scorer(g,state);
                double[][] input=new double[1200][AlgorithmGenomeR908.INPUTS];double[] expected=new double[1200];
                for(double[] x:input)for(int k=0;k<x.length;k++)x[k]=random.nextDouble()*2-1;
                long start=System.nanoTime();for(int a=0;a<1200;a++)expected[a]=AlgorithmGenomeVmR908.score(g,state,a/120+1,input[a]);oldNanos+=System.nanoTime()-start;
                start=System.nanoTime();for(int a=0;a<1200;a++){double actual=scorer.score(a/120+1,input[a]);if(Double.doubleToLongBits(actual)!=Double.doubleToLongBits(expected[a]))throw new AssertionError("VM_BIT_PARITY:"+g.genomeId+":"+a);sink+=actual;}newNanos+=System.nanoTime()-start;
                check(initial.equals(state.encode()),"SCORER_BIT_PARITY_1200_READ_ONLY:"+g.genomeId);
                AlgorithmGenomeVmR908.update(g,state,1,input[0]);check(Double.doubleToLongBits(scorer.score(1,input[0]))==Double.doubleToLongBits(expected[0]),"SCORER_OWNS_IMMUTABLE_SNAPSHOT:"+g.genomeId);
            }
            System.out.println("VM_MEASURED_HOST_NS|reference="+oldNanos+"|batch="+newNanos+"|sink="+sink+"|NOT_PHONE_TIMING");
            // At 112 observing candidates, 24 slots imposed a <=21.4% average selection ceiling.
            List<GlobalResearchScheduleR932.Candidate> crowded=new ArrayList<GlobalResearchScheduleR932.Candidate>();
            for(int i=0;i<160;i++)crowded.add(new GlobalResearchScheduleR932.Candidate("formula:cap-"+i,"family-"+(i%12),V51ChecksumEngine.sha256("cap-"+i),"B",""));
            check(GlobalResearchScheduleR932.choose(target,crowded).size()==128,"EVALUATION_COHORT_REMAINS_BOUNDED_AT_128");
            long start=System.nanoTime();int callbacks=0;
            for(int period=0;period<10;period++){
                target=TargetIssueResolver.next(base).targetIssue;
                DeferredEvidenceSnapshotR36.Snapshot surface=ReportedCaptureHarnessR945.snapshot(base,target);byte[] blob=DeferredEvidenceSnapshotR36.encode(surface);
                db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue,job_type,status,created_at,updated_at,record_hash,snapshot_blob,snapshot_hash) VALUES(?,?,?,0,0,?,?,?)",new Object[]{target,"R963_REPORT_COHORT_SYNTHETIC","PENDING",surface.recordHash,blob,DeferredEvidenceSnapshotR36.sha256(blob)});
                for(int retry=0;retry<90;retry++){
                    callbacks++;try{ResearchPredrawCaptureR923.captureTarget(store,target,1200);}catch(ResearchWorkBudgetR927.Yield y){}
                    if(ResearchCaptureQueueR946.complete(store,target))break;
                }
                check(ResearchCaptureQueueR946.complete(store,target),"REPORT_COHORT_ALL_ADMITTED_FINISH:"+period);
                // A final successful retry releases the 1200 immutable contexts before the next issue.
                ResearchPredrawCaptureR923.captureTarget(store,target,1200);
                check(!GlobalResearchScheduleR932.retainedForTestsR964(store,target),"COMPLETED_CAPTURE_RELEASES_1200_CONTEXTS:"+period);
                if(period>=8)check(n(db,"SELECT COUNT(*) FROM "+GlobalResearchScheduleR932.TASK+" WHERE target_issue='"+target+"' AND candidate_key LIKE 'algorithm:%' AND selected=1")==29,"EVERY_REPORT_ALGORITHM_ADMITTED_TO_FULL_EVALUATION:"+period);
                DevelopmentTrainingHarnessR959.draw(db,target,random);
                for(int retry=0;retry<24;retry++){ResearchPredrawSettlementPumpR908.drainBatch(store,32,1600);}
                for(int retry=0;retry<16&&ResearchLifecycleR932.hasPendingPeriodReview(store);retry++)ResearchLifecycleR932.maintain(store);
                base=target;
            }
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE state_updates>=2 AND future_n>=2")==29,"ALL_29_REPORT_ALGORITHMS_ACTUALLY_TRAIN_AND_GAIN_FUTURE");
            check(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE state_updates<>future_n")==0,"ALL_29_EXACTLY_ONCE_LEARNING");
            try(Cursor c=db.rawQuery("SELECT asset_id,status,state_updates,future_n FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME,null)){while(c.moveToNext())System.out.println("REPORT_ASSET_RESULT|"+c.getString(0)+"|"+c.getString(1)+"|updates="+c.getLong(2)+"|future="+c.getLong(3));}
            check(n(db,"SELECT COUNT(*) FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE state_updates>=2 AND future_n>=2")==18,"ALL_18_REPORT_ASSETS_ACTUALLY_LEARN_AND_GAIN_FUTURE");
            check(n(db,"SELECT COUNT(DISTINCT candidate_id) FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id LIKE 'R906-%' AND hit_label IN (0,1)")==70,"ALL_70_REPORT_FORMULAS_HAVE_SETTLED_FROZEN_EVALUATION");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_CERTIFICATE_FABRICATED_FROM_SHORT_RUN");
            String formula=DirectedResearchCreationR932.maintain(store,"formula");check(formula.startsWith("WAIT_EXISTING_COHORT_EVIDENCE"),"FULL_FORMULA_POOL_TRAINS_EXISTING_BEFORE_OPEN_CREATION");
            // Ingest is independently durable; a failed creation keeps the original SQL cause.
            db.execSQL("INSERT INTO "+ModuleAblationR932.TABLE+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{base,"BASELINE",V51ChecksumEngine.sha256("ignored-source"),"old-code","BLOCKED_INPUT_OR_PARITY","{}",V51ChecksumEngine.sha256("{}"),1});
            db.execSQL("CREATE TRIGGER fail_r963_creation BEFORE INSERT ON "+DirectedResearchCreationR932.ATTEMPT+" BEGIN SELECT RAISE(ABORT,'R963_INJECTED_CREATION_CAUSE'); END");
            boolean cause=false;try{DirectedResearchCreationR932.maintain(store,"algorithm");}catch(IllegalStateException expected){cause=expected.getMessage().contains("R963_INJECTED_CREATION_CAUSE")&&expected.getCause()!=null;}
            check(cause,"CREATION_PRESERVES_ORIGINAL_CAUSE_IN_REPORT");
            check(n(db,"SELECT last_row FROM "+DirectedResearchCreationR932.CURSOR+" WHERE owner='MODULE'")>0,"CREATION_FAILURE_CANNOT_ROLL_BACK_INGEST_PROGRESS");
            db.execSQL("DROP TRIGGER fail_r963_creation");
            check(DirectedResearchCreationR932.maintain(store,"algorithm").startsWith("CREATED_N0"),"CREATION_RECOVERS_AFTER_FAILED_COMMIT");
            System.out.println("R963_REPORTED_COHORT_PASS|checks="+checks+"|callbacks="+callbacks+"|elapsedMs="+(System.nanoTime()-start)/1000000+"|REAL_REPORT_EXECUTABLES_SYNTHETIC_DRAWS_NOT_PHONE");
        }finally{db.shutdown();}
    }
}
