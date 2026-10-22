package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Bounded research generation from frozen module interventions, with paid, reproducible attempts. */
public final class DirectedResearchCreationR932 {
    static final String GROUP="research_repair_group_r932",ATTEMPT="research_creation_attempt_r932",CURSOR="research_creation_cursor_r932";
    static final int FAILURES=30,OPEN_INTERVAL=1,PAGE=16;
    private DirectedResearchCreationR932(){}
    static void schema(SQLiteDatabase db){
        ModuleAblationR932.schema(db);ResearchLifecycleR932.schema(db);ResearchMultiplicityR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CURSOR+"(owner TEXT PRIMARY KEY,last_row INTEGER NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO "+CURSOR+" VALUES('MODULE',0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+GROUP+"(group_hash TEXT PRIMARY KEY,lane TEXT NOT NULL,parent_key TEXT NOT NULL,module TEXT NOT NULL,domain TEXT NOT NULL,boundary TEXT NOT NULL,bundle_hash TEXT NOT NULL,source_hash TEXT NOT NULL,failures INTEGER NOT NULL,gain INTEGER NOT NULL,consumed INTEGER NOT NULL,last_issue TEXT NOT NULL,evidence_hash TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_creation_groups ON "+GROUP+"(lane,bundle_hash,source_hash,last_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ATTEMPT+"(attempt_id TEXT PRIMARY KEY,lane TEXT NOT NULL,kind TEXT NOT NULL,parent_key TEXT NOT NULL,group_hash TEXT NOT NULL,seed TEXT NOT NULL,created_issue TEXT NOT NULL,state TEXT NOT NULL,child_key TEXT NOT NULL,child_structure TEXT NOT NULL,reason TEXT NOT NULL,plan_json TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r966_creation_parent ON "+ATTEMPT+"(parent_key,kind,state,created_issue)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_creation_quota ON "+ATTEMPT+"(lane,kind,created_issue)");
    }
    public static String maintain(final DbStore store,final String lane){
        if(store==null)return "STORE_NULL";
        if(!Arrays.asList("algorithm","genesis","formula").contains(lane))throw new IllegalArgumentException("R932_CREATION_LANE");
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
        if(!store.coordinatorR36().ensureR811ResearchSchemaOnce())return "WAIT_RESEARCH_SCHEMA";
        String latest="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(latest.isEmpty())return "WAIT_HISTORY";
        if(lane.equals("algorithm"))HistoricalGenomeImportR967.maintain(store);
        // Dedicated continuity lanes maintain causal and molecular evidence. Creation consumes
        // their committed results; repeating those workers here could exhaust every creation slice.
        String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");if(active.isEmpty())active=ModelBundleRuntimeR932.current().hash;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,TargetIssueResolver.next(latest).targetIssue,System.currentTimeMillis())){
            final RuntimeException[] failure=new RuntimeException[1];
            boolean ingested=store.coordinatorR36().runAtomicWrite("R963_CREATION_INGEST",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                try{ingest(db);}catch(RuntimeException e){failure[0]=e;throw e;}
            }});
            if(!ingested)throw new IllegalStateException("R963_CREATION_INGEST_COMMIT_FAILED|"+
                    (failure[0]==null?"WRITER_INTERRUPTED":DatabaseCoordinator.describeCauseChain(failure[0],1000)),failure[0]);
            ResearchWorkBudgetR927.checkpoint();
            String result=store.coordinatorR36().runAtomicWrite("R932_DIRECTED_CREATION_"+lane,new DatabaseCoordinator.WriteCallable<String>(){public String call(SQLiteDatabase db){
                try{return create(db,lane);}catch(RuntimeException e){failure[0]=e;throw e;}
            }},"COMMIT_FAILED");
            if(result==null||result.equals("COMMIT_FAILED"))throw new IllegalStateException("R932_CREATION_COMMIT_FAILED|"+
                    (failure[0]==null?"WRITER_INTERRUPTED":DatabaseCoordinator.describeCauseChain(failure[0],1000)),failure[0]);
            return result;
        }
    }
    /** Append-only row cursor and aggregates commit together; only actual active-module replays count. */
    static void ingest(SQLiteDatabase db){
        long cursor=0;try(Cursor c=db.rawQuery("SELECT last_row FROM "+CURSOR+" WHERE owner='MODULE'",null)){if(c.moveToFirst())cursor=c.getLong(0);}
        try(Cursor c=db.rawQuery("SELECT rowid,payload,payload_hash,code_hash,state FROM "+ModuleAblationR932.TABLE+" WHERE rowid>? ORDER BY rowid LIMIT 16",new String[]{String.valueOf(cursor)})){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();cursor=c.getLong(0);
                if(!c.getString(4).equals("REEXECUTED")||!c.getString(3).equals(SourceManifestR815.SOURCE_TREE_SHA256))continue;
                if(!V51ChecksumEngine.sha256(c.getString(1)).equals(c.getString(2)))throw new IllegalStateException("R932_CREATION_ABLATION_HASH");
                try{
                    JSONObject p=new JSONObject(c.getString(1));if(!p.getBoolean("original_failure")||p.getInt("formal_action_count")!=1200)continue;
                    String consumer=p.getString("consumer_lane"),lane=consumer.equals("INVENTED_ALGORITHM_CERTIFIED")?"algorithm":consumer.equals("GENESIS_NEW_CERTIFIED")?"genesis":consumer.equals("FORMULA_DSL_ACTIVE_SHADOW")?"formula":"";
                    if(lane.isEmpty()||p.getString("consumer_id").isEmpty())continue;
                    String module=p.getString("module");ModuleReplayR932.Module.valueOf(module);if(module.equals("BASELINE"))continue;
                    double margin=p.getDouble("original_third_margin");if(!Double.isFinite(margin))throw new IllegalStateException("R932_CREATION_BOUNDARY_NONFINITE");
                    String boundary=margin<.02?"LT_0.02":margin<.10?"0.02_TO_0.10":"GE_0.10",domain=p.getString("predraw_domain")+":"+p.getString("predraw_lifecycle"),parent=lane+":"+p.getString("consumer_id"),bundle=p.getString("model_bundle_hash");
                    String group=hash(lane+'|'+parent+'|'+module+'|'+domain+'|'+boundary+'|'+bundle+'|'+SourceManifestR815.SOURCE_TREE_SHA256);
                    db.execSQL("INSERT OR IGNORE INTO "+GROUP+" VALUES(?,?,?,?,?,?,?,?,0,0,0,'','')",new Object[]{group,lane,parent,module,domain,boundary,bundle,SourceManifestR815.SOURCE_TREE_SHA256});
                    String evidence="";try(Cursor old=db.rawQuery("SELECT evidence_hash FROM "+GROUP+" WHERE group_hash=?",new String[]{group})){if(old.moveToFirst())evidence=old.getString(0);}
                    db.execSQL("UPDATE "+GROUP+" SET failures=failures+1,gain=gain+?,last_issue=?,evidence_hash=? WHERE group_hash=?",new Object[]{p.getInt("delta_hit"),p.getString("target_issue"),hash(evidence+'|'+c.getString(2)),group});
                }catch(JSONException error){throw new IllegalStateException("R932_CREATION_ABLATION_JSON",error);}
            }
        }
        db.execSQL("UPDATE "+CURSOR+" SET last_row=? WHERE owner='MODULE'",new Object[]{cursor});
    }
    static String create(SQLiteDatabase db,String lane){
        if(!db.inTransaction())throw new IllegalStateException("R932_CREATION_TRANSACTION_REQUIRED");
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}if(latest.isEmpty())return "WAIT_HISTORY";
        String populationVictim="";
        if(lane.equals("algorithm")){
            if(!ResearchPopulationR966.normalize(db,latest))return "WAIT_LEGACY_OVERCAPACITY_MATURITY_OR_NEXT_SLICE";
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+ATTEMPT+" WHERE lane='algorithm' AND created_issue=? LIMIT 1",new String[]{latest})){if(c.moveToFirst())return "WAIT_NEXT_DRAW_ALGORITHM_ATTEMPT_QUOTA";}
            populationVictim=ResearchPopulationR966.replacement(db);
            if(populationVictim==null)return "WAIT_POPULATION_MATURITY_OR_PROTECTED_MEMBERS|capacity=64";
        }
        String group="",parent="",module="OPEN",evidence="",domain="GLOBAL_ALL_POSITIONS";int support=0;
        try(Cursor c=db.rawQuery("SELECT group_hash,parent_key,module,evidence_hash,domain,failures FROM "+GROUP+" WHERE lane=? AND bundle_hash=? AND source_hash=? AND failures-consumed>=30 AND gain>0 ORDER BY last_issue,group_hash LIMIT 1",new String[]{lane,ModelBundleRuntimeR932.current().hash,SourceManifestR815.SOURCE_TREE_SHA256})){
            if(c.moveToFirst()){group=c.getString(0);parent=c.getString(1);module=c.getString(2);evidence=c.getString(3);domain=c.getString(4);support=c.getInt(5);}
        }
        CausalResearchFeedbackR947.Plan causal = CausalResearchFeedbackR947.choose(db,lane,latest);
        if(group.isEmpty() && causal.ready()){
            module=causal.module; evidence=causal.evidenceHash; domain="SETTLED_CAUSAL_DIAGNOSTIC:"+causal.module;
        }
        HistoricalGenomeImportR967.Seed historical=group.isEmpty()&&lane.equals("algorithm")
                ?HistoricalGenomeImportR967.choose(db,latest):null;
        if(historical!=null){parent="historical:"+historical.recordId;module="HISTORICAL_PROGRAM_AND_INITIAL_STATE";
            domain="NEW_RESEARCH_CURRENT_VM_NOT_OLD_EVIDENCE";evidence=historical.rowHash;}
        ResearchFusionR964.Plan priorityElite=null;
        if(historical==null&&group.isEmpty()&&lane.equals("algorithm")){
            try(Cursor c=db.rawQuery("SELECT kind FROM "+ATTEMPT+" WHERE lane='algorithm' AND state='CREATED' ORDER BY created_issue DESC,attempt_id LIMIT 1",null)){
                if(c.moveToFirst()&&ResearchFailureEvolutionR965.KIND.equals(c.getString(0)))priorityElite=ResearchFusionR964.choose(db,latest);
            }
        }
        ResearchFailureEvolutionR965.Plan repair=historical==null&&priorityElite==null&&group.isEmpty()&&lane.equals("algorithm")?ResearchFailureEvolutionR965.choose(db,latest):null;
        if(repair!=null){parent="algorithm:"+repair.parent.genomeId;module=repair.intent.name();domain="SETTLED_PREDRAW_RESEARCH_FAILURE";evidence=repair.evidence;support=repair.labels;}
        ResearchFusionR964.Plan fusion=priorityElite!=null?priorityElite:historical==null&&repair==null&&group.isEmpty()&&lane.equals("algorithm")?ResearchFusionR964.choose(db,latest):null;
        if(fusion!=null){parent="algorithm:"+fusion.left.genome.genomeId;module=fusion.module();domain=fusion.right==null?"MEASURED_ELITE_PARENT":"TWO_MEASURED_RESEARCH_PARENTS";evidence=fusion.evidence();}
        String kind=historical!=null?HistoricalGenomeImportR967.KIND:repair!=null?ResearchFailureEvolutionR965.KIND:fusion!=null?fusion.kind():group.isEmpty()?"OPEN_EXPLORATION":"DIRECTED_REPAIR";
        if(group.isEmpty()&&repair==null&&fusion==null&&historical==null){
            String table=lane.equals("algorithm")?DbHelper.TABLE_R908_ALGORITHM_GENOME:
                    lane.equals("genesis")?AssetGenesisEngineR915.TABLE_ASSET_GENOME:DbHelper.TABLE_R906_RESEARCH_ASSET;
            int capacity=lane.equals("algorithm")?ResearchPopulationR966.CAPACITY:lane.equals("genesis")?24:64;
            String activePredicate=lane.equals("formula")?"runtime_executable=1 AND artifact_status NOT IN ('PRUNED','QUARANTINED','RETIRED','ARCHIVED')":
                    "status NOT LIKE 'PRUNED%' AND status NOT LIKE 'QUARANTINED%' AND status NOT IN ('RETIRED','ARCHIVED')";
            try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table+" WHERE "+activePredicate,null)){
                if(c.moveToFirst()&&c.getInt(0)>=capacity&&!lane.equals("algorithm"))return "WAIT_EXISTING_COHORT_EVIDENCE|lane="+lane+"|active="+c.getInt(0)+"|capacity="+capacity;
            }
            String previous="";try(Cursor c=db.rawQuery("SELECT created_issue FROM "+ATTEMPT+" WHERE lane=? AND kind='OPEN_EXPLORATION' ORDER BY created_issue DESC LIMIT 1",new String[]{lane})){if(c.moveToFirst())previous=c.getString(0);}
            int n=0;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM (SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 AND issue>? LIMIT 120)",new String[]{previous})){if(c.moveToFirst())n=c.getInt(0);}
            if(n<(previous.isEmpty()?96:OPEN_INTERVAL))return "WAIT_PREREGISTERED_OPEN_QUOTA_OR_30_NEW_FAILURES";
        }
        String attempt=hash("R959_PER_DRAW_CREATION_V2|"+lane+'|'+kind+'|'+(repair!=null?parent+":"+evidence:group.isEmpty()?latest:group+":"+support)+'|'+ModelBundleRuntimeR932.current().hash+'|'+SourceManifestR815.SOURCE_TREE_SHA256);
        try(Cursor c=db.rawQuery("SELECT state FROM "+ATTEMPT+" WHERE attempt_id=?",new String[]{attempt})){if(c.moveToFirst())return "ALREADY_ATTEMPTED_"+c.getString(0);}
        long seed=Long.parseUnsignedLong(attempt.substring(0,16),16);
        String next=TargetIssueResolver.next(latest).targetIssue;
        // Failed compilation, semantic duplicates and rejected children also remain paid attempts.
        ResearchMultiplicityR932.registerCandidate(db,"creation_attempt:"+attempt,"creation_attempt:"+lane,hash("ATTEMPT|"+attempt),next);
        String plan;try{JSONObject p=new JSONObject();p.put("module",module);p.put("domain",domain);p.put("failure_support",support);p.put("evidence_hash",evidence);p.put("seed",Long.toUnsignedString(seed));p.put("open_interval",OPEN_INTERVAL);p.put("minimum_new_failures",FAILURES);p.put("attempt_budget",1);p.put("planned_new_future",120);p.put("scope","POSTMORTEM_DIAGNOSTIC_NOT_CAUSAL_OR_FUTURE_PROOF");p.put("causal_feedback_module",causal.module);p.put("causal_feedback_support",causal.support);p.put("causal_feedback_score",causal.score);p.put("causal_feedback_evidence",causal.evidenceHash);p.put("causal_feedback_reason",causal.reason);p.put("may_authorize_real",false);if(fusion!=null)fusion.describe(p);if(repair!=null)repair.describe(p);if(historical!=null)historical.describe(p);plan=p.toString();}catch(JSONException e){throw new IllegalStateException(e);}
        db.execSQL("INSERT INTO "+ATTEMPT+" VALUES(?,?,?,?,?,?,?,'REGISTERED','','','',?,?)",new Object[]{attempt,lane,kind,parent,group,Long.toUnsignedString(seed),latest,plan,System.currentTimeMillis()});
        if(!group.isEmpty())db.execSQL("UPDATE "+GROUP+" SET consumed=? WHERE group_hash=?",new Object[]{support,group});
        String child="",structure="",family="",ps="",classification="NEW_FAMILY";String atoms="UNCHANGED_REGISTERED_INPUTS";
        try{
            if(!parent.isEmpty())try(Cursor c=db.rawQuery("SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",new String[]{parent})){if(c.moveToFirst()&&c.getString(0).equals("QUARANTINED"))throw new IllegalArgumentException("QUARANTINED_PARENT");}
            if(lane.equals("algorithm")){
                AlgorithmGenomeR908 g;
                if(historical!=null){g=historical.genome;ps=historical.originalProgramHash;classification="HISTORICAL_INITIALIZED_SEED";atoms="ORIGINAL_PROGRAM_CURRENT_VM_NEW_EVIDENCE";}
                else if(repair!=null){g=repair.create(seed);ps=repair.parent.structuralHash;classification="STRUCTURAL_CHANGE";atoms="PARENT_VM_INPUTS_WITH_CHANGED_MEMORY_ROUTING_AND_UPDATE_PROGRAM";}
                else if(fusion!=null){g=fusion.create(seed);ps=fusion.left.genome.structuralHash;classification="STRUCTURAL_CHANGE";atoms=fusion.right==null?"ELITE_PARENT_STABILITY_AND_UPDATE_PROGRAM":"TWO_PARENT_INFERENCE_AND_UPDATE_PROGRAMS";}
                else if(parent.isEmpty())g=directedSeed(seed,kind,module);
                else {AlgorithmGenomeR908 old;try(Cursor c=db.rawQuery("SELECT genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=?",new String[]{suffix(parent)})){if(!c.moveToFirst())throw new IllegalArgumentException("PARENT_MISSING");old=AlgorithmGenomeR908.parse(c.getString(0));}ps=old.structuralHash;g=minimal(old,module,seed);classification="STRUCTURAL_CHANGE";}
                if(!g.algorithmGrade())throw new IllegalArgumentException("ALGORITHM_GRADE");
                child="algorithm:"+g.genomeId;structure=g.structuralHash;family=algorithmFamily(g);
                if(exists(db,DbHelper.TABLE_R908_ALGORITHM_GENOME,"genome_id",g.genomeId))throw new IllegalArgumentException("DUPLICATE_STRUCTURE");
                AlgorithmInventionLabR908.registerDirected(db,g,latest,next,kind,plan);
            }else if(lane.equals("genesis")){
                GenesisAssetGenomeR915 g;
                if(parent.isEmpty()){
                    ArrayList<Integer> allowed=new ArrayList<Integer>();for(int i=0;i<GenesisSchemaRegistryR915.atomCount();i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible())allowed.add(i);
                    int[] indices=new int[12];for(int i=0;i<indices.length;i++)indices[i]=allowed.get(i*allowed.size()/indices.length);
                    indices=AtomicToolWorldR943.creationAtoms(db,indices);
                    MolecularToolWorldR947.Plan molecule=MolecularToolWorldR947.creationPlan(db,indices,seed);
                    indices=molecule.atomIndices;
                    g=new GenesisAssetGenomeR915("PRIMITIVE_SEARCH",1,"",kind+"|ATOM_SELECTION="+java.util.Arrays.toString(indices)+"|"+molecule.trace()+"|CAUSAL_FEEDBACK="+causal.module+":"+causal.evidenceHash,latest,GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,indices,directedSeed(seed,kind,module),molecule.executable);
                }else{GenesisAssetGenomeR915 old;try(Cursor c=db.rawQuery("SELECT genome_spec FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id=?",new String[]{suffix(parent)})){if(!c.moveToFirst())throw new IllegalArgumentException("PARENT_MISSING");old=GenesisAssetGenomeR915.parse(c.getString(0));}ps=old.structuralHash;g=new GenesisAssetGenomeR915(old.familyId,old.generation+1,old.assetId,kind,latest,old.outputType,old.atomIndices,minimal(old.kernel,module,seed),old.molecule);classification="STRUCTURAL_CHANGE";}
                if(!g.assetGrade()||!g.usesOnlyProductionScoreAtoms())throw new IllegalArgumentException("GENESIS_GRADE");child="genesis:"+g.assetId;structure=g.structuralHash;family="genesis:"+g.familyId;
                if(exists(db,AssetGenesisEngineR915.TABLE_ASSET_GENOME,"asset_id",g.assetId))throw new IllegalArgumentException("DUPLICATE_STRUCTURE");
                if(!AssetGenesisEngineR915.registerDirected(db,g,latest,next))throw new IllegalStateException("GENESIS_INSERT");
            }else{
                ResearchMethodPlanR906 old=null;
                if(!parent.isEmpty())try(Cursor c=db.rawQuery("SELECT canonical_spec,artifact_hash FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE asset_id=?",new String[]{suffix(parent)})){if(!c.moveToFirst())throw new IllegalArgumentException("PARENT_MISSING");old=ResearchMethodPlanR906.parseCanonical(c.getString(0));ps=c.getString(1);}
                ResearchMethodPlanR906 p=formula(old,module,seed,parent);ResearchCompiledGraphRuntimeR906.Compiled compiled=ResearchCompiledGraphRuntimeR906.compile(p);
                String id="R906-"+ResearchMethodSignatureR906.of(p).substring(0,24);if(exists(db,DbHelper.TABLE_R906_RESEARCH_ASSET,"asset_id",id))throw new IllegalArgumentException("DUPLICATE_STRUCTURE");
                ResearchAssetRegistryR906.Asset a=ResearchAssetRegistryR906.register(db,p,old);child="formula:"+a.assetId;structure=compiled.artifactHash;family="formula:"+p.representation+":"+p.learner;
                if(old!=null&&old.representation.equals(p.representation))classification="STRUCTURAL_CHANGE";
                atoms=compiled.expression.dependencies.toString();
                db.execSQL("UPDATE "+DbHelper.TABLE_R906_RESEARCH_ASSET+" SET trained_until_issue=?,earliest_use_issue=? WHERE asset_id=?",new Object[]{latest,next,a.assetId});
            }
        }catch(IllegalArgumentException rejected){
            if(historical!=null)HistoricalGenomeImportR967.rejected(db,historical,attempt,latest,rejected.getMessage());
            db.execSQL("UPDATE "+ATTEMPT+" SET state='REJECTED',reason=? WHERE attempt_id=?",new Object[]{rejected.getMessage(),attempt});return "REJECTED_PAID_ATTEMPT|"+rejected.getMessage();
        }
        // A structural repair never resets multiplicity by acquiring a differently named family.
        if(!parent.isEmpty())try(Cursor c=db.rawQuery("SELECT h.family FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{parent})){if(c.moveToFirst())family=c.getString(0);}
        ResearchMultiplicityR932.registerCandidate(db,child,family,structure,next);
        ResearchLifecycleR932.enroll(db,child,structure,next,"C");
        if(!parent.isEmpty()&&historical==null)ResearchLifecycleR932.freshChild(db,child,parent,ps,structure,"MODULE="+module+";DOMAIN="+domain,module,atoms,"POSITIVE_INDEPENDENT_PAIRED_GAIN_WITHOUT_BRIER_OR_COST_REGRESSION","FAIL_FIXED_120_IF_NO_GAIN;MISSING_CAPTURE_PAUSES;NO_REAL_AUTHORITY",classification,next);
        db.execSQL("UPDATE "+ATTEMPT+" SET state='CREATED',child_key=?,child_structure=?,reason=? WHERE attempt_id=?",new Object[]{child,structure,classification,attempt});
        if(historical!=null)HistoricalGenomeImportR967.committed(db,historical,attempt,child,latest);
        if(fusion!=null)fusion.committed(db,child);
        if(lane.equals("algorithm"))ResearchPopulationR966.committed(db,populationVictim,child,latest);
        return "CREATED_N0|"+child+"|"+classification+(fusion==null?"":"|"+fusion.module());
    }
    static AlgorithmGenomeR908 directedSeed(long seed,String kind,String module){
        AlgorithmGenomeR908 base=AlgorithmGenomeR908.seed(seed,1,"PAID_"+kind+"|CAUSAL_DIAGNOSTIC="+module);
        if(!module.equals("TREND")&&!module.equals("SHAPE")&&!module.equals("RELATION"))return base;
        int feature=module.equals("TREND")?2:module.equals("SHAPE")?3:1;
        int temporary=(base.scoreOutputReg+1)%AlgorithmGenomeR908.REGS;
        ArrayList<AlgorithmGenomeR908.Insn> score=new ArrayList<AlgorithmGenomeR908.Insn>(base.scoreProgram);
        if(score.size()+3>AlgorithmGenomeR908.MAX_PROGRAM)return base;
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.LOAD_X,temporary,feature,0,0));
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.TANH,temporary,temporary,0,0));
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.MIX,base.scoreOutputReg,base.scoreOutputReg,temporary,.05+.05*Math.floorMod(seed,4)));
        return new AlgorithmGenomeR908(1,base.parentIds,score,base.updateProgram,base.scoreOutputReg);
    }
    /** Append one bounded feature mixture; preserve every parent instruction and its update law. */
    static AlgorithmGenomeR908 minimal(AlgorithmGenomeR908 parent,String module,long seed){
        if(parent.scoreProgram.size()+3>AlgorithmGenomeR908.MAX_PROGRAM)throw new IllegalArgumentException("MINIMAL_REPAIR_PROGRAM_LIMIT");
        int feature=module.equals("TREND")?2:module.equals("SHAPE")?3:module.equals("RELATION")?1:6;
        int temporary=(parent.scoreOutputReg+1)%AlgorithmGenomeR908.REGS;
        ArrayList<AlgorithmGenomeR908.Insn> score=new ArrayList<AlgorithmGenomeR908.Insn>(parent.scoreProgram);
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.LOAD_X,temporary,feature,0,0));
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.TANH,temporary,temporary,0,0));
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.MIX,parent.scoreOutputReg,parent.scoreOutputReg,temporary,.05+.05*Math.floorMod(seed,4)));
        return new AlgorithmGenomeR908(parent.generation+1,parent.genomeId,score,parent.updateProgram,parent.scoreOutputReg);
    }
    static ResearchMethodPlanR906 formula(ResearchMethodPlanR906 old,String module,long seed,String parent){
        String base=old!=null&&old.representation.equals("TYPED_EXPRESSION_R932")?old.feature:"FEATURE(raw_action_p)";
        String feature=module.equals("TREND")?"window_consensus":module.equals("RELATION")?"assignment_support":module.equals("SHAPE")?"stability":TypedExpressionR932.INPUTS[4+Math.floorMod(seed,3)];
        double weight=.05+.05*Math.floorMod(seed,4);
        String expression="CLIP(ADD(MUL(CONST("+(1-weight)+"),"+base+"),MUL(CONST("+weight+"),FEATURE("+feature+"))),CONST(0),CONST(1))";
        return new ResearchMethodPlanR906("TYPED_EXPRESSION_R932",expression,"BOUNDED_SYMBOLIC_BYTECODE","FROZEN_NO_ONLINE_PARAMETER_UPDATE","INDEPENDENT_PAIRED_HIT_AND_BRIER","EARLIER_120_EMBARGO_1_VALIDATE_120","FIXED_COST_250MS","FIXED_PAIRED_120_NEW_ALPHA",parent.isEmpty()?"":suffix(parent),old==null?1:old.generation+1,"PAID_TYPED_EXPRESSION_MUTATION",true);
    }
    static String algorithmFamily(AlgorithmGenomeR908 g){TreeSet<String> ops=new TreeSet<String>();for(AlgorithmGenomeR908.Insn i:g.scoreProgram)ops.add(i.op.name());for(AlgorithmGenomeR908.Insn i:g.updateProgram)ops.add("update:"+i.op.name());return "algorithm:"+hash(ops.toString());}
    private static boolean exists(SQLiteDatabase db,String table,String column,String id){try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" WHERE "+column+"=?",new String[]{id})){return c.moveToFirst();}}
    private static String suffix(String key){return key.substring(key.indexOf(':')+1);}
    private static String hash(String value){return V51ChecksumEngine.sha256(value);}
}
