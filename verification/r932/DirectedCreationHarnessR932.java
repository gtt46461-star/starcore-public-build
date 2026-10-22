package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import org.json.JSONObject;

/** Actual production registration/VM/SQL; synthetic attribution is never reported as future effect. */
public final class DirectedCreationHarnessR932 {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println(s+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void legacy(SQLiteDatabase db)throws Exception{
        String source=new String(Files.readAllBytes(Paths.get("app/src/main/java/com/ethk3/novaonepro/DbHelper.java")),"UTF-8");
        for(String table:new String[]{DbHelper.TABLE_R908_ALGORITHM_GENOME,DbHelper.TABLE_R906_RESEARCH_ASSET}){
            Matcher m=Pattern.compile("\"(CREATE TABLE IF NOT EXISTS "+table+" \\([^\"]+)\"").matcher(source);if(!m.find())throw new AssertionError("DDL_MISSING");db.execSQL(m.group(1));
        }
        Matcher columns=Pattern.compile("addColumnR36\\(db, TABLE_R908_ALGORITHM_GENOME, \"([^\"]+)\"\\)").matcher(source);HashSet<String> seen=new HashSet<>();while(columns.find())if(seen.add(columns.group(1).split(" ")[0]))db.execSQL("ALTER TABLE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" ADD COLUMN "+columns.group(1));
        db.execSQL("CREATE TABLE v57_hypothesis_r925(genome_id TEXT PRIMARY KEY,family TEXT,structure TEXT,origin TEXT,created_at INTEGER)");
        db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_OOS_LABEL+"(target_issue TEXT,strict_oos INTEGER)");AssetGenesisEngineR915.ensureSchema(db);
    }
    static void ingest(SQLiteDatabase db){db.beginTransaction();try{DirectedResearchCreationR932.ingest(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static String create(SQLiteDatabase db,String lane){db.beginTransaction();try{String r=DirectedResearchCreationR932.create(db,lane);db.setTransactionSuccessful();return r;}finally{db.endTransaction();}}
    static void failure(SQLiteDatabase db,String issue,String id)throws Exception{
        JSONObject p=new JSONObject();p.put("original_failure",true);p.put("formal_action_count",1200);p.put("consumer_lane","INVENTED_ALGORITHM_CERTIFIED");p.put("consumer_id",id);p.put("module","TREND");p.put("original_third_margin",.015);p.put("predraw_domain","FIXTURE_DOMAIN");p.put("predraw_lifecycle","STABLE");p.put("model_bundle_hash",ModelBundleRuntimeR932.current().hash);p.put("delta_hit",1);p.put("target_issue",issue);
        String json=p.toString();db.execSQL("INSERT INTO "+ModuleAblationR932.TABLE+" VALUES(?,'TREND',?,?,'REEXECUTED',?,?,1)",new Object[]{issue,V51ChecksumEngine.sha256(issue),SourceManifestR815.SOURCE_TREE_SHA256,json,V51ChecksumEngine.sha256(json)});
    }
    public static void main(String[] args)throws Exception{
        String a="CLIP(ADD(FEATURE(raw_action_p),FEATURE(stability)),CONST(0),CONST(1))",b="CLIP(ADD(FEATURE(stability),FEATURE(raw_action_p)),CONST(0),CONST(1))";
        check(TypedExpressionR932.compile(a).hash.equals(TypedExpressionR932.compile(b).hash),"TYPED_COMMUTATIVE_SEMANTIC_IDENTITY");
        String repeated="CLIP(ADD(MUL(FEATURE(raw_action_p),FEATURE(stability)),MUL(FEATURE(raw_action_p),FEATURE(stability))),CONST(0),CONST(1))";
        check(TypedExpressionR932.compile(repeated).instructions.size()==7,"TYPED_COMMON_SUBEXPRESSION_EXECUTED_ONCE");
        boolean rejected=false;try{TypedExpressionR932.compile("FEATURE(hit_label)");}catch(Exception e){rejected=true;}check(rejected,"TYPED_POSTDRAW_FEATURE_REJECTED");
        rejected=false;try{TypedExpressionR932.compile("SAFE_DIV(FEATURE(raw_action_p),CONST(0),CONST(0))");}catch(Exception e){rejected=true;}check(rejected,"TYPED_UNBOUNDED_DIVISION_REJECTED");
        rejected=false;try{TypedExpressionR932.compile(a).evaluate(new double[]{.5,0,0,Double.NaN,.5,.5,.5});}catch(Exception e){rejected=true;}check(rejected,"TYPED_MISSING_INPUT_NOT_COERCED_TO_ZERO");
        ResearchMethodPlanR906 plan=DirectedResearchCreationR932.formula(null,"TREND",17,""),decoded=ResearchMethodPlanR906.parseCanonical(plan.canonical());
        ResearchCompiledGraphRuntimeR906.Compiled compiled=ResearchCompiledGraphRuntimeR906.compile(plan),replay=ResearchCompiledGraphRuntimeR906.compile(decoded);boolean parity=true,sensitive=false;
        for(int i=0;i<100;i++){double[] x={.3,0,0,.5,.5,i/100d,.5};double score=ResearchCompiledGraphRuntimeR906.evaluate(compiled,x),second=ResearchCompiledGraphRuntimeR906.evaluate(replay,x);parity&=Double.doubleToLongBits(score)==Double.doubleToLongBits(second);sensitive|=Math.abs(score-.3)>.001;}
        check(parity&&sensitive,"ACTUAL_TYPED_FORMULA_100_SERIALIZED_PARITY_AND_FEATURE_SENSITIVITY");
        AlgorithmGenomeR908 parent=AlgorithmGenomeR908.seed(17,1,"FIXTURE"),child=DirectedResearchCreationR932.minimal(parent,"TREND",17);
        check(child.scoreProgram.size()==parent.scoreProgram.size()+3&&child.updateProgram.toString().equals(parent.updateProgram.toString()),"MINIMAL_ALGORITHM_REPAIR_PRESERVES_PARENT_BODY_AND_UPDATE");
        check(child.structuralHash.equals(DirectedResearchCreationR932.minimal(parent,"TREND",17).structuralHash),"GENERATION_SEED_REPRODUCES_EXECUTABLE_CHILD");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,"202609100121",System.currentTimeMillis())){
            new DbStore(db);legacy(db);DirectedResearchCreationR932.schema(db);
            // R953 verification fixture follows the production post-draw causal feedback schema.
            // The directed creator now consults CausalResearchFeedbackR947 even when no miss rows
            // exist, so the synthetic DB must expose the same draw columns and empty miss ledger.
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_MISS_REASON_SAMPLE+"(target_issue TEXT,position INTEGER,actual_number INTEGER,window_wrong_score REAL,position_wrong_score REAL,main_number_wrong_score REAL,third_drag_score REAL,counter_ignored_score REAL,created_at INTEGER)");
            for(int i=1;i<=120;i++)db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0)",new Object[]{String.format(Locale.US,"20260910%04d",i)});
            for(String lane:new String[]{"algorithm","genesis","formula"})check(create(db,lane).startsWith("CREATED_N0|"),"ACTUAL_"+lane.toUpperCase(Locale.US)+"_PRODUCTION_REGISTRATION");
            check(scalar(db,"SELECT SUM(future_n) FROM "+ResearchLifecycleR932.TABLE).equals("0"),"ALL_THREE_GENERATED_CONSUMERS_START_N0");
            check(create(db,"algorithm").startsWith("WAIT_")&&scalar(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT).equals("3"),"OPEN_QUOTA_CANNOT_REPEAT_WITHOUT_120_NEW_ISSUES");
            String id=scalar(db,"SELECT genome_id FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" LIMIT 1");
            for(int i=1;i<=29;i++)failure(db,String.format(Locale.US,"20260910%04d",i),id);ingest(db);ingest(db);
            check(create(db,"algorithm").startsWith("WAIT_"),"TWENTYNINE_FAILURES_CANNOT_TRIGGER_DIRECTED_REPAIR");
            failure(db,"202609100030",id);ingest(db);ingest(db);
            check(scalar(db,"SELECT failures FROM "+DirectedResearchCreationR932.GROUP).equals("30"),"CURSOR_RETRY_NEVER_DOUBLES_FAILURE_SUPPORT");
            String result=create(db,"algorithm");check(result.startsWith("CREATED_N0|"),"THIRTY_ACTUAL_INTERVENTION_ROWS_CREATE_MINIMAL_REPAIR");
            check(scalar(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.CHILD+" WHERE outcome='PENDING' AND challenged_id=''").equals("1"),"CREATION_ENROLLS_NEW_FUTURE_CHALLENGE_WITHOUT_SUCCESS");
            check(create(db,"algorithm").startsWith("WAIT_"),"SAME_FAILURE_GROUP_CANNOT_PAY_FOR_UNLIMITED_CHILDREN");
            String prior=scalar(db,"SELECT last_row FROM "+DirectedResearchCreationR932.CURSOR);failure(db,"202609100031",id);
            db.beginTransaction();try{DirectedResearchCreationR932.ingest(db);}finally{db.endTransaction();}
            check(prior.equals(scalar(db,"SELECT last_row FROM "+DirectedResearchCreationR932.CURSOR))&&scalar(db,"SELECT failures FROM "+DirectedResearchCreationR932.GROUP).equals("30"),"INTERRUPTION_ROLLS_BACK_ATTRIBUTION_AND_CURSOR_TOGETHER");
            db.execSQL("UPDATE "+DirectedResearchCreationR932.GROUP+" SET failures=60,parent_key='algorithm:missing'");String before=scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
            check(create(db,"algorithm").startsWith("REJECTED_PAID_ATTEMPT")&&Integer.parseInt(scalar(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS))==Integer.parseInt(before)+1,"REJECTED_GENERATION_RETAINS_HYPOTHESIS_BUDGET");
            check(scalar(db,"SELECT COUNT(*) FROM "+DirectedResearchCreationR932.ATTEMPT+" WHERE state='REJECTED'").equals("1"),"REJECTED_ATTEMPT_AND_REASON_PERSIST");
            System.out.println("R932_DIRECTED_CREATION_PASS|scope=ACTUAL_VM_AND_REGISTRATION_SYNTHETIC_ATTRIBUTION_NO_FUTURE_EFFECT_CLAIM");
        }finally{db.shutdown();}
    }
}
