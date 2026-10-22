package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

/** Product SQL and calibration owner tests. All draws below are synthetic test-only data. */
public final class PlanContractHarnessR968 {
    static int checks;
    interface Work{void run();}
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static void reject(String token,Work work){try{work.run();throw new AssertionError("Expected "+token);}catch(RuntimeException error){check(String.valueOf(error.getMessage()).contains(token),"REJECT_"+token+":"+error.getMessage());}}
    static void tx(SQLiteDatabase db,Work work){db.beginTransaction();try{work.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static String h(String text){return V51ChecksumEngine.sha256(text);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,dirty INTEGER DEFAULT 0)");
            tx(db,()->FixedResearchWindowR932.schema(db));
            check(n(db,"SELECT COUNT(*) FROM "+ResearchGatePlanR967.PLAN)==0,"SCHEMA_DOES_NOT_FABRICATE_PLANS");
            String key="genesis:plan-test",structure=h("S"),bundle=h("B"),selector=h("SELECTOR"),start="202609210001";
            tx(db,()->ResearchMultiplicityR932.enrollResearchIdentity(db,key,"TEST",structure,start));
            reject("EARLIER_CALIBRATION_REQUIRED",()->tx(db,()->FixedResearchWindowR932.register(db,key,structure,bundle,selector,start,120,1)));
            check(n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW)==0&&n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==0,"UNCALIBRATED_WINDOW_AND_ALPHA_ROLL_BACK");
            String issue=start;
            for(int i=0;i<241;i++){
                final String target=issue;final boolean hit=(i%2)==0;final double score=hit?.9:.1;
                Models.Draw draw=new Models.Draw();draw.issue=target;int[] numbers=hit?new int[]{1,2,3,4,5,6,7,8,9,10}:new int[]{4,1,2,3,5,6,7,8,9,10};System.arraycopy(numbers,0,draw.numbers,0,10);
                tx(db,()->{
                    check(CandidateCalibrationR932.captureOrPredict(db,key,structure,bundle,selector,target,h("P|"+target),score,1,1,2,3)==null,"CALIBRATION_NOT_FORMAL:"+target);
                    db.execSQL("INSERT INTO draws VALUES(?,0)",new Object[]{target});CandidateCalibrationR932.settle(db,draw);
                });
                issue=TargetIssueResolver.next(issue).targetIssue;
            }
            tx(db,()->CandidateCalibrationR932.fitOne(db));
            check(n(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1,"ACTUAL_120_1_120_CALIBRATION_MODEL");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==0,"CALIBRATION_DOES_NOT_PRESPEND_ALPHA");
            final String first=issue;final String[] window={""};
            tx(db,()->window[0]=FixedResearchWindowR932.register(db,key,structure,bundle,selector,first,120,1));
            ResearchGatePlanR967.Plan plan=ResearchGatePlanR967.forWindow(db,window[0],true);String original=plan.hash;
            check(original.matches("[0-9a-f]{64}"),"TYPED_PLAN_HASH_STORED");
            check(plan.policy.hash.equals(ResearchSemanticsGeneratedR967.POLICY_HASH),"INSTALLED_POLICY_WAS_CAPTURED");
            check(plan.calibrationHash.equals(text(db,"SELECT model_hash FROM "+CandidateCalibrationR932.MODEL)),"EXACT_PRIOR_CALIBRATION_BOUND");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchGatePlanR967.PLAN)==1&&n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"ONE_NEW_FORMAL_LOOK_ONE_PLAN");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==1,"FORMAL_HYPOTHESIS_SPENT_ONCE");
            check(Math.abs(Double.parseDouble(text(db,"SELECT alpha FROM "+ResearchMultiplicityR932.LOOK))-.0025)<1e-15,"LEGACY_ALPHA_0025_UNCHANGED");
            tx(db,()->check(window[0].equals(FixedResearchWindowR932.register(db,key,structure,bundle,selector,first,120,1)),"REGISTRATION_REPLAY_SAME_WINDOW"));
            check(original.equals(ResearchGatePlanR967.forWindow(db,window[0],true).hash),"REGISTRATION_REPLAY_PRESERVES_PLAN_HASH");
            check(n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"REPLAY_DOES_NOT_SPEND_ALPHA_TWICE");
            reject("IMMUTABLE_EXPERIMENT_PLAN",()->db.execSQL("UPDATE "+ResearchGatePlanR967.PLAN+" SET plan_json='{}'"));
            reject("IMMUTABLE_EXPERIMENT_PLAN",()->db.execSQL("DELETE FROM "+ResearchGatePlanR967.POLICY));
            reject("IMMUTABLE_EXPERIMENT_PLAN",()->db.execSQL("DELETE FROM "+ResearchGatePlanR967.REQUIRED));
            reject("PLANNED_WINDOW_IDENTITY_IMMUTABLE",()->db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET planned_n=1"));
            reject("PLANNED_WINDOW_PERMANENT",()->db.execSQL("DELETE FROM "+FixedResearchWindowR932.WINDOW));
            for(String table:new String[]{ResearchGatePlanR967.POLICY,ResearchGatePlanR967.PLAN,ResearchGatePlanR967.REQUIRED})
                reject("IMMUTABLE_EXPERIMENT_PLAN",()->db.execSQL("INSERT OR REPLACE INTO "+table+" SELECT * FROM "+table));
            reject("PLANNED_WINDOW_PERMANENT",()->db.execSQL("INSERT OR REPLACE INTO "+FixedResearchWindowR932.WINDOW+" SELECT * FROM "+FixedResearchWindowR932.WINDOW));
            reject("PLANNED_WINDOW_IDENTITY_IMMUTABLE",()->db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET formal_plan_revision='LEGACY_R932'"));
            long before=n(db,"SELECT total_changes()");ResearchGatePlanR967.forWindow(db,window[0],true);
            check(n(db,"SELECT total_changes()")==before,"PLAN_READ_IS_READ_ONLY");
            JSONObject alternative=new JSONObject(plan.policy.json);alternative.put("revision","R967_STATIC_FIXED_POLICY_V2");alternative.put("min_hit_rate",.35);
            ResearchGatePlanR967.Policy nextPolicy=ResearchGatePlanR967.policy(alternative.toString(),h(alternative.toString()));
            check(!nextPolicy.hash.equals(plan.policy.hash),"THRESHOLD_CHANGE_NEW_POLICY_HASH");
            check(ResearchGatePlanR967.forWindow(db,window[0],true).policy.minRate==.34,"EXISTING_PLAN_KEEPS_ORIGINAL_THRESHOLD");
            check(ResearchGatePlanR967.forWindow(db,window[0],true).hash.equals(original),"NEW_POLICY_DOES_NOT_REWRITE_EXISTING_PLAN");
            reject("POLICY_HASH_MISMATCH",()->ResearchGatePlanR967.policy(nextPolicy.json,h("WRONG")));
            final String badMode=nextPolicy.json.replace("STATIC_FROZEN","ADAPTIVE_UNKNOWN");
            reject("INTERPRETER_UNSUPPORTED",()->ResearchGatePlanR967.policy(badMode,h(badMode)));
            final String unknown=nextPolicy.json.substring(0,nextPolicy.json.length()-1)+",\"ignored\":true}";
            reject("POLICY_FIELDS",()->ResearchGatePlanR967.policy(unknown,h(unknown)));
            JSONObject fractional=new JSONObject(nextPolicy.json);fractional.put("window_sizes",new org.json.JSONArray("[120.5,240,240,240,360]"));final String fractionalText=fractional.toString();
            reject("POLICY_INTEGER",()->ResearchGatePlanR967.policy(fractionalText,h(fractionalText)));
            JSONObject coerced=new JSONObject(nextPolicy.json);coerced.put("min_hit_rate","0.35");final String coercedText=coerced.toString();
            reject("POLICY_TYPE",()->ResearchGatePlanR967.policy(coercedText,h(coercedText)));
            reject("ENDPOINT_NOT_REGISTERED_OR_EARLY",()->FixedWindowStatisticsR932.evaluate(db,window[0],120,first));
            String last=text(db,"SELECT last_issue FROM "+FixedResearchWindowR932.WINDOW);
            reject("EVALUATION_DENOMINATOR_MISMATCH",()->FixedWindowStatisticsR932.evaluate(db,window[0],119,last));
            FixedWindowStatisticsR932.Result missing=FixedWindowStatisticsR932.evaluate(db,window[0],120,last);
            check("PAUSED_CAPTURE".equals(missing.state),"MISSING_CAPTURES_PAUSE_NOT_IMPUTE");
            JSONObject metrics=new JSONObject(missing.json);
            check(metrics.getInt("planned_n")==120&&metrics.getInt("paired_n")==0,"MISSING_WINDOW_DENOMINATOR_KEPT");
            check(metrics.getString("plan_hash").equals(original)&&metrics.getString("policy_hash").equals(plan.policy.hash),"TERMINAL_METRICS_BIND_PLAN_AND_POLICY");
            // Supply a champion so this cannot pass merely because the unrelated champion gate returned early.
            tx(db,()->db.execSQL("INSERT INTO "+FixedResearchWindowR932.CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{first,bundle,h("champion-input"),selector,1,1,2,3,h("champion-prediction"),System.currentTimeMillis()}));
            tx(db,()->FixedResearchWindowR932.freezePair(db,key,structure,first,h("UNKNOWN_SCORE"),1,1,2,3,Double.NaN));
            tx(db,()->FixedResearchWindowR932.freezePair(db,key,structure,first,h("INFINITE_SCORE"),1,1,2,3,Double.POSITIVE_INFINITY));
            check(n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR)==0,"UNKNOWN_SCORE_CANNOT_SKIP_CALIBRATION");
            final String next=TargetIssueResolver.next(last).targetIssue;
            final ObservationGenerationR932.Chain chain=new ObservationGenerationR932.Chain(h("test-chain"),key,1,2);
            db.execSQL("CREATE TRIGGER inject_plan_failure BEFORE INSERT ON "+ResearchGatePlanR967.REQUIRED+" BEGIN SELECT RAISE(ABORT,'INJECT_PLAN_COMMIT'); END");
            reject("INJECT_PLAN_COMMIT",()->tx(db,()->FixedResearchWindowR932.persistWindow(db,key,structure,bundle,selector,next,240,2,chain)));
            check(n(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW)==1&&n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1&&n(db,"SELECT COUNT(*) FROM "+ResearchGatePlanR967.PLAN)==1,"PLAN_FAILURE_ROLLS_BACK_WINDOW_ALPHA_AND_METADATA");
            db.execSQL("DROP TRIGGER inject_plan_failure");
            String known=TargetIssueResolver.next(next).targetIssue;db.execSQL("INSERT INTO draws VALUES(?,0)",new Object[]{known});
            reject("WINDOW_REGISTRATION_POSTDRAW",()->tx(db,()->FixedResearchWindowR932.persistWindow(db,key,structure,bundle,selector,next,240,2,chain)));
            check(n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"RETROACTIVE_GAP_NOT_ALLOWED_TO_SPEND_ALPHA");
            // Emulate out-of-band corruption only after normal immutability guards were tested.
            db.execSQL("DROP TRIGGER "+ResearchGatePlanR967.PLAN+"_immutable_update");
            JSONObject fractionalPlan=new JSONObject(plan.json);fractionalPlan.put("planned_n",120.5);
            db.execSQL("UPDATE "+ResearchGatePlanR967.PLAN+" SET plan_json=?",new Object[]{fractionalPlan.toString()});
            reject("PLAN_INTEGER",()->ResearchGatePlanR967.forWindow(db,window[0],true));
            JSONObject extraPlan=new JSONObject(plan.json);extraPlan.put("unchecked_input",true);
            db.execSQL("UPDATE "+ResearchGatePlanR967.PLAN+" SET plan_json=?",new Object[]{extraPlan.toString()});
            reject("PLAN_FIELDS",()->ResearchGatePlanR967.forWindow(db,window[0],true));
            db.execSQL("UPDATE "+ResearchGatePlanR967.PLAN+" SET plan_json=?",new Object[]{plan.json});
            db.execSQL("UPDATE "+ResearchGatePlanR967.PLAN+" SET plan_hash=?",new Object[]{h("CORRUPT")});
            reject("PLAN_HASH_MISMATCH",()->ResearchGatePlanR967.forWindow(db,window[0],true));
            // Losing every side-table row must not downgrade a new-format window into legacy evidence.
            db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_CAPTURE',reason='ordinary terminal detail'");
            db.execSQL("DROP TABLE "+ResearchGatePlanR967.PLAN);db.execSQL("DROP TABLE "+ResearchGatePlanR967.REQUIRED);
            tx(db,()->ResearchGatePlanR967.schema(db));
            check(n(db,"SELECT COUNT(*) FROM "+ResearchGatePlanR967.PLAN)==0,"RECOVERY_NEVER_FABRICATES_LOST_PLANS");
            reject("FORMAL_PLAN_MISSING",()->ResearchGatePlanR967.beforeEvaluation(db,window[0]));
            check(n(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"SIDECAR_LOSS_DOES_NOT_REFUND_SPENT_ALPHA");
            db.execSQL("INSERT INTO "+FixedResearchWindowR932.WINDOW+"(window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,state,reason) VALUES('legacy','legacy','S','B','P','202001010001','202001010120',120,1,1,'LEGACY_REVIEW','old')");
            check(ResearchGatePlanR967.beforeEvaluation(db,"legacy")==null,"LEGACY_HISTORY_RETAINED_NOT_BACKFILLED_WITH_NEW_PLAN");
            System.out.println("R968_PLAN_CONTRACT_PASS checks="+checks+"; synthetic_draws=241; actual_product_SQL_and_calibration=YES; live_or_device=NOT_RUN");
        }finally{db.shutdown();}
    }
}
