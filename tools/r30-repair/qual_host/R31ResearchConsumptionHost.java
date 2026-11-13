package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;

/** The actual source producer, actual family adapter, original birth gate and
 * original same-transaction consumer on synthetic native SQLite. */
public final class R31ResearchConsumptionHost {
    static int assertions;
    static void check(boolean condition,String code){assertions++;if(!condition)throw new AssertionError(code);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("COUNT_MISSING");return c.getLong(0);}}
    @SuppressWarnings("unchecked") static void rejectedBirth(Connection connection,SQLiteDatabase db,ResearchWriteQueueR27.Batch batch,String child,String field,Object wrong)throws Exception{
        String id,json;try(Cursor c=db.rawQuery("SELECT proposal_id,payload_json FROM r30r1_proposal WHERE state='READY' ORDER BY created_at LIMIT 1",null)){check(c.moveToFirst(),"READY_PROPOSAL_FOR_NEGATIVE");id=c.getString(0);json=c.getString(1);}
        Map<String,Object> payload=(Map<String,Object>)JsonR27.parse(json),plan=(Map<String,Object>)payload.get("actionContractR31");plan.put(field,wrong);
        String invalid=JsonR27.write(payload);db.execSQL("UPDATE r30r1_proposal SET payload_json=?,payload_hash=? WHERE proposal_id=?",new Object[]{invalid,StatsR27.sha256(invalid),id});
        Map<String,String> reasons=new HashMap<>();reasons.put("target","R31_RESULT_TARGET");reasons.put("sourceRevision","R31_RESULT_SOURCE_REVISION");reasons.put("budget","R31_RESULT_BUDGET");reasons.put("decisionOrdinal","R31_RESULT_LATE_LABEL");reasons.put("beforeHash","R31_ACTION_ABSENCE_IDENTITY");reasons.put("preparedAt","R31_ACTION_SOURCE_AVAILABLE_AFTER_PREPARATION");reasons.put("sourceHashes","R31_ACTION_PRODUCT_CHANGED");reasons.put("sourceViews",wrong instanceof Map&&((Map<?,?>)wrong).containsValue("R10")?"R31_ACTION_ORIGINAL_VIEW_MISMATCH":"R31_RESULT_NOT_APPLICABLE_OPTIMIZATION");reasons.put("sourceContracts","R31_ACTION_SOURCE_CONTRACT_CHANGED");
        boolean refused=false;try{R31ResearchContractHost.tx(connection,db,batch);}catch(RuntimeException expected){StringBuilder chain=new StringBuilder();for(Throwable cause=expected;cause!=null;cause=cause.getCause())chain.append(cause.getMessage()).append('|');check(chain.toString().contains(reasons.get(field)),"EXACT_PRODUCTION_REJECTION:"+field);refused=true;}
        check(refused,"ACTUAL_BORN_TRANSACTION_REFUSED:"+field);
        check(count(db,"SELECT count(*) FROM r27_policy WHERE policy_hash='"+child+"'")==0,"NO_HALF_CHILD_AFTER_ACTION_REJECTION:"+field);
        check(count(db,"SELECT count(*) FROM r30r1_consume")==0,"NO_HALF_CONSUME_AFTER_ACTION_REJECTION:"+field);
        db.execSQL("UPDATE r30r1_proposal SET payload_json=?,payload_hash=? WHERE proposal_id=?",new Object[]{json,StatsR27.sha256(json),id});
    }
    @SuppressWarnings("unchecked") static Map<String,Object> receipt(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT payload_json FROM r30r1_research_measure WHERE kind='RESEARCH_SOURCE_CONSUMPTION' ORDER BY measure_key LIMIT 1",null)){check(c.moveToFirst(),"ACTUAL_CONSUMPTION_RECEIPT_PRESENT");return (Map<String,Object>)JsonR27.parse(c.getString(0));}}
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");for(boolean matrix:new boolean[]{false,true})try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            com.ethk3.novaonepro.ResearchEvolutionStoreR930.createSchema(db);
            for(String[] ddl:new String[][]{SchemaR27.DDL,ResearchSchemaR30R1.DDL,RuntimeSchemaR26.DDL})R31ResearchContractHost.schema(db,ddl,false);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,open_time INTEGER,dirty INTEGER,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT)");
            connection.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);ResearchInputPinR30R1.ensure(db);connection.commit();connection.setAutoCommit(true);
            ReplayDatasetR27 random=ReplayDatasetR27.synthetic(5000,901,new int[0]).planted(.8,902);List<ReplayDatasetR27.Row> rows=new ArrayList<>();long start=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202610240001");
            for(int i=0;i<random.size();i++)rows.add(new ReplayDatasetR27.Row(com.ethk3.novaonepro.TargetIssueResolver.fromSequenceOrdinalSU1(start+i),start+i,random.draws[i]));
            ReplayDatasetR27 data=ReplayDatasetR27.of(rows);
            try(FeatureCacheR27 cache=new FeatureCacheR27(data)){
                EvalContextR27 ctx=new EvalContextR27(cache,SplitR27.of(data,null),null);
                ResearchWriteQueueR27.Batch pin=new ResearchWriteQueueR27.Batch();ResearchInputPinR30R1.append(pin,data,0,1);R31ResearchContractHost.tx(connection,db,pin);
                if(matrix){
                    MatrixTensorR30R1.Tensor tensor=MatrixResearchR30R1.build("CROSS_POSITION_FLOW",ctx,null,0);ResearchWriteQueueR27.Batch produced=new ResearchWriteQueueR27.Batch();MatrixTensorR30R1.append(produced,tensor,2);R31ResearchContractHost.tx(connection,db,produced);
                }else{
                    Map<String,Object> frame=ResearchObservatoryR30R1.compute("R03",ctx,Collections.emptyList(),0);R31ResearchContractHost.tx(connection,db,ResearchObservatoryR30R1.product("R03",ResearchMeasureKindR31.POSITION_COMPATIBILITY.name(),ctx,0,frame,1,2));
                }
                ResearchContextR30R1 research=ResearchContextR30R1.read(db,ctx);check(!research.sourceRunsR31().isEmpty(),"REAL_PRODUCER_READ_INTO_RESEARCH_CONTEXT");
                IdeaFamilyR27.Context idea=new IdeaFamilyR27.Context();idea.generation=0;idea.maxDepth=BirthGateR27.MAX_DEPTH;idea.evaluation=ctx;idea.research=research;
                PolicySpecR27 selected=null;
                for(long seed=1;seed<=100&&selected==null;seed++){
                    StatsR27.SplitMix actual=new StatsR27.SplitMix(seed),ordinary=new StatsR27.SplitMix(seed);
                    ResearchContextR30R1 ordinaryContext=ResearchContextR30R1.read(db,ctx);IdeaFamilyR27.Context ordinaryIdea=new IdeaFamilyR27.Context();ordinaryIdea.generation=0;ordinaryIdea.maxDepth=idea.maxDepth;ordinaryIdea.evaluation=ctx;ordinaryIdea.research=ordinaryContext;
                    PolicySpecR27 unchangedStream=new IdeaI04CrossPositionR27().propose(ordinary,ordinaryIdea);
                    PolicySpecR27 candidate=research.proposeFamilyR31(new IdeaI04CrossPositionR27(),actual,idea);
                    check(actual.state==ordinary.state&&candidate.policyHash().equals(unchangedStream.policyHash()),"COUNTERFACTUAL_CANNOT_CHANGE_ACTUAL_BIRTH_RNG_OR_POLICY");
                    if(!ResearchContextR30R1.references(candidate).isEmpty())selected=candidate;
                }
                check(selected!=null,"ORIGINAL_SIGNAL_ACTUALLY_REACHES_FAMILY_PROPOSAL");
                EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;StoreR27.EvolutionSink stage=new StoreR27.EvolutionSink("HOST_R31_ACTION",data.issues[data.size()-1]);
                try(EvolutionLoopR27 loop=new EvolutionLoopR27(cfg,cache,stage,()->1)){
                    loop.addPendingForTest(selected);loop.researchContext=research;loop.stageResearchHypotheses(stage.batch,3);R31ResearchContractHost.tx(connection,db,stage.batch);
                    StoreR27.EvolutionSink born=new StoreR27.EvolutionSink("HOST_R31_ACTION",data.issues[data.size()-1]);loop.setSink(born);loop.step=3;loop.step();
                    check("BORN".equals(loop.pendingState(selected.policyHash())),"ORIGINAL_BIRTH_GATE_STILL_REQUIRED");loop.commitResearchAdmissions(born.batch,4);
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"target","POSITION:99");
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"sourceRevision",7L);
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"budget",0L);
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"decisionOrdinal",data.ordinals[ctx.split.selectTo-1]);
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"beforeHash",StatsR27.sha256("FOREIGN_PARENT"));
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"preparedAt",0L);
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"sourceHashes",Collections.singletonMap(ResearchContextR30R1.references(selected).get(0),StatsR27.sha256("FOREIGN_ORIGINAL")));
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"sourceViews",Collections.singletonMap(ResearchContextR30R1.references(selected).get(0),"R11"));
                    rejectedBirth(connection,db,born.batch,selected.policyHash(),"sourceViews",Collections.singletonMap(ResearchContextR30R1.references(selected).get(0),"R10"));
                    try(Cursor source=db.rawQuery("SELECT summary_json FROM r30r1_analysis_run WHERE run_id=?",new String[]{ResearchContextR30R1.references(selected).get(0)})){check(source.moveToFirst(),"ORIGINAL_CONTRACT_FOR_TAMPER_CONTROL");Map<String,Object> original=(Map<String,Object>)JsonR27.parse(source.getString(0)),wrong=new LinkedHashMap<>((Map<String,Object>)original.get("sourceContract"));wrong.put("abi","FOREIGN_ABI");rejectedBirth(connection,db,born.batch,selected.policyHash(),"sourceContracts",Collections.singletonMap(ResearchContextR30R1.references(selected).get(0),wrong));}
                    R31ResearchContractHost.tx(connection,db,born.batch);
                    Map<String,Object> link=receipt(db),typed=(Map<String,Object>)link.get("resultContractR31");check(typed!=null&&Boolean.TRUE.equals(typed.get("validated")),"PRODUCTION_TYPED_VALIDATOR_CALLED_FROM_ORIGINAL_BORN_TRANSACTION");
                    Map<String,Object> fields=(Map<String,Object>)typed.get("receipts");for(String field:ResearchResultContractR31.REQUIRED_ACTION_RECEIPTS)check(fields.containsKey(field),"ACTUAL_REQUIRED_FIELD:"+field);
                    check(selected.policyHash().equals(fields.get("after_hash")),"AFTER_HASH_IS_ACTUAL_BORN_CHILD");check(((Number)fields.get("budget")).longValue()==cfg.creationSliceUnits,"BUDGET_IS_ACTUAL_ORIGINAL_LOOP_CEILING");
                    check(((List<?>)typed.get("usages")).size()>0,"ACTUAL_USAGES_PERSISTED_WITH_CHILD");
                    check(count(db,"SELECT count(*) FROM r30r1_research_measure WHERE kind='RESEARCH_SOURCE_ABLATION_R31'")==1,"ACTUAL_WITHOUT_SOURCE_REPLAY_RECEIPT_PERSISTED");
                    try(Cursor c=db.rawQuery("SELECT payload_json FROM r30r1_research_measure WHERE kind='RESEARCH_SOURCE_ABLATION_R31'",null)){
                        check(c.moveToFirst(),"ABLATION_ORIGINAL_PRESENT");Map<String,Object> value=(Map<String,Object>)JsonR27.parse(c.getString(0));
                        check("MEASURED_SELECT_ONLY".equals(value.get("state")),"REAL_WITH_AND_WITHOUT_POLICIES_REPLAYED");
                        check(value.get("budgetWith").equals(value.get("budgetWithout")),"SAME_DECLARED_BUDGET");check("DESCRIPTIVE_RESEARCH_ONLY".equals(value.get("intervalScope")),"SELECT_INTERVAL_NEVER_CERTIFIED");
                        check(selected.policyHash().equals(value.get("withHash"))&&Boolean.TRUE.equals(value.get("sourceRemovedFromCounterfactual")),"EXACT_SOURCE_REMOVAL_AND_CHILD_BINDING");
                    }
                    long receipts=count(db,"SELECT count(*) FROM r30r1_consume");
                    try{R31ResearchContractHost.tx(connection,db,born.batch);throw new AssertionError("DUPLICATE_CONSUME_ACCEPTED");}catch(IllegalStateException expected){check(count(db,"SELECT count(*) FROM r30r1_consume")==receipts,"DUPLICATE_DELIVERY_HAS_NO_SECOND_CONSUMPTION");}
                    Map<String,Object> report=ResearchConsumptionStoreR31.report(db);check(((Number)report.get("validatedActionReceipts")).longValue()==1,"ACTUAL_REPORT_READS_VALIDATED_CONSUMPTION");check(((List<?>)report.get("lastAblations")).size()==1,"ACTUAL_REPORT_READS_SAME_BUDGET_ABLATION");
                }
            }
        }
        System.out.println("R31_RESEARCH_CONSUMPTION_NATIVE_PASS assertions="+assertions+" source=ACTUAL_R03_AND_TYPED_MATRIX_PRODUCERS consumer=ACTUAL_FAMILY_AND_BORN_TRANSACTION ablation=ACTUAL_SAME_SEED_SELECT_REPLAY evidence=DESCRIPTIVE_ONLY scope=HOST_SYNTHETIC_NOT_ANDROID_NOT_NATURAL");
    }
}
