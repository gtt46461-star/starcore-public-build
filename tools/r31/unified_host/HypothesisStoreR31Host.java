package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Native SQLite protocol + actual ModelLive SELECT trajectories. Synthetic
 * controls, never Android lifecycle, natural quality or certificate evidence. */
public final class HypothesisStoreR31Host {
    static int assertions,actualDecisions;
    static void ok(boolean value,String code){assertions++;if(!value)throw new AssertionError(code);}
    static void fails(Runnable body,String code){boolean failed=false;try{body.run();}catch(RuntimeException expected){failed=true;}ok(failed,code);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("NO_SCALAR");return c.getLong(0);}}
    static void tx(SQLiteDatabase db,Runnable body)throws Exception{db.jdbc.setAutoCommit(false);try{body.run();db.jdbc.commit();}catch(Throwable e){db.jdbc.rollback();throw e;}finally{db.jdbc.setAutoCommit(true);}}
    static SQLiteDatabase open(Path path)throws Exception{Connection c=DriverManager.getConnection("jdbc:sqlite:"+path);try(Statement statement=c.createStatement()){statement.execute("PRAGMA journal_mode=WAL");}return new SQLiteDatabase(c);}
    static void schema(SQLiteDatabase db)throws Exception{
        tx(db,()->{
            db.execSQL("CREATE TABLE r26_foundry(campaign_id TEXT PRIMARY KEY,snapshot_hash TEXT,status TEXT,source_through INTEGER,snapshot BLOB,created_at INTEGER,numerical_abi TEXT,source_first INTEGER)");
            db.execSQL("CREATE TABLE r26_candidate(campaign_id TEXT,policy_hash TEXT,parent TEXT,operation TEXT,status TEXT DEFAULT 'READY',cursor INTEGER DEFAULT 0,state_hash TEXT DEFAULT '',checkpoint_hash TEXT DEFAULT '',service_cpu_ms INTEGER DEFAULT 0,canonical_text TEXT,PRIMARY KEY(campaign_id,policy_hash))");
            db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT,campaign_id TEXT,policy_hash TEXT,parent TEXT,operation TEXT,category TEXT,structure_hash TEXT)");
            db.execSQL("CREATE TABLE r26_failure_research_cursor(parent_policy TEXT PRIMARY KEY,evidence_digest TEXT,window_end INTEGER,campaign_id TEXT,proposal_json TEXT)");
            db.execSQL("CREATE TABLE r26_qualification_result(policy_hash TEXT,evidence_digest TEXT,reason TEXT,window_end INTEGER)");
            db.execSQL("CREATE TABLE r26_live_model(policy_hash TEXT,canonical_text TEXT,status TEXT)");
            db.execSQL("CREATE TABLE r31_model_publication_part(content_hash TEXT,part INTEGER,bytes BLOB)");
            db.execSQL("CREATE TABLE r26_policy_identity(policy_hash TEXT,canonical_policy TEXT,genotype_text TEXT,numerical_abi TEXT)");
            StringJoiner columns=new StringJoiner(",");for(int i=0;i<16;i++)columns.add("c"+i);
            db.execSQL("CREATE TABLE r31_model_publication_item(publication_id TEXT,ordinal INTEGER,kind TEXT,"+columns+")");
            HypothesisStoreR31.installWithinMigrationR31(db);
        });HypothesisStoreR31.requireSchemaR31(db);
    }
    static final class Fixture {
        final String campaign,child,control,source,parent;final long through;final int samples;final List<Long> ordinals=new ArrayList<>();final String metadata,digest,controlText,childText,parentText;
        Fixture(String campaign,int samples){
            this.campaign=campaign;this.samples=samples;source=StatsR27.sha256(campaign+"snapshot");
            ModelGenomeSU2 raw=FamilyRegistryR26.genome("F01");ModelGenomeSU2 base=new ModelGenomeSU2(raw.members,raw.pool,raw.sink,raw.gate,raw.coverage,raw.metaC,false);parent=base.key();parentText=base.text();
            for(int i=samples*3/5;i<samples;i++)ordinals.add(100000L+i+(i>samples*4/5?1:0));through=ordinals.get(ordinals.size()-1);digest=StatsR27.sha256(JsonR27.write(ordinals));
            ModelGenomeSU2 controlGenome=base.bindBirth(source,through),childGenome=new ModelGenomeSU2(base.members,base.pool,base.sink,base.gate,base.coverage,base.metaC,true).bindBirth(source,through);
            control=controlGenome.key();child=childGenome.key();controlText=controlGenome.text();childText=childGenome.text();
            ResearchHypothesisR31.Hypothesis h=ResearchHypothesisR31.fromFailure(new ResearchHypothesisR31.Failure(parent,StatsR27.sha256(campaign+"failed-source"),ResearchHypothesisR31.Mechanism.CALIBRATION,"HELD_LATER_ECE_GT_004","","REVEALED_FAILED_LOOK",99999,99998,500,.2));
            ResearchHypothesisR31.Registry reg=new ResearchHypothesisR31.Registry();reg.register(h);
            Map<String,Object> m=new LinkedHashMap<>();m.put("parentPolicy",parent);m.put("failureEvidenceDigest",h.sourceRun);m.put("failureReason","HELD_LATER_ECE_GT_004");m.put("failureWindowToExclusive",99999L);m.put("failureActs",500L);m.put("failureRate",.2);m.put("researchSnapshot",source);m.put("sourceThrough",through);m.put("hypothesisR31",h.json());m.put("dispatchR31",reg.dispatch(h));
            List<Object> children=new ArrayList<>();for(String policy:new String[]{control,child}){Map<String,Object> row=new LinkedHashMap<>();row.put("policy",policy);row.put("operator",policy.equals(control)?"PRESERVE_PARENT_STRUCTURE":h.action);children.add(row);}m.put("children",children);metadata=JsonR27.write(m);
        }
        void birth(SQLiteDatabase db)throws Exception{tx(db,()->{
            db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,status) VALUES(?,?,'RUNNING')",new Object[]{campaign,source});
            for(String policy:new String[]{child,control}){
                Map<String,Object> event=new TreeMap<>((Map<String,Object>)JsonR27.parse(metadata));event.put("childPolicy",policy);event.put("operator",policy.equals(control)?"PRESERVE_PARENT_STRUCTURE":"READOUT_TEMPERATURE_REFIT_ON_TRAIN");
                String operation="FAILURE_GUIDED_V1:"+JsonR27.write(event);
                db.execSQL("INSERT INTO r26_candidate VALUES(?,?,?,?,'DONE',?,?,?,120,?)",new Object[]{campaign,policy,policy.equals(control)?"":parent,policy.equals(control)?"REGISTERED_SEED":operation,samples,StatsR27.sha256(policy+"state"),StatsR27.sha256(policy+"checkpoint"),policy.equals(control)?controlText:childText});
                db.execSQL("INSERT INTO r26_evolution(campaign_id,policy_hash,parent,operation) VALUES(?,?,?,?)",new Object[]{campaign,policy,parent,operation});
            }
            HypothesisStoreR31.registerWithinR31(db,campaign,source,samples,digest,through,metadata,1);
        });}
        @SuppressWarnings("unchecked") void birthViaProduction(SQLiteDatabase db)throws Exception{
            Map<String,Object> m=(Map<String,Object>)JsonR27.parse(metadata);
            try(ModelPublicationR31.Plan plan=new ModelPublicationR31.Plan("BIRTH:"+campaign,"FOUNDRY_BIRTH",source,0,-1,1)){
                plan.put("FOUNDRY_ROOT",campaign,campaign,source,through,StatsR27.sha256("controlled-old-raw"),"RUNNING",1L,"controlled-abi",100000L,1,samples,digest);
                plan.put("FAILURE_CURSOR",parent,parent,"",-1L,m.get("failureEvidenceDigest"),99999L,metadata,"HELD_LATER_ECE_GT_004",parentText);
                for(String policy:new String[]{control,child}){
                    Map<String,Object> event=new TreeMap<>(m);event.put("childPolicy",policy);event.put("operator",policy.equals(control)?"PRESERVE_PARENT_STRUCTURE":"READOUT_TEMPERATURE_REFIT_ON_TRAIN");String operation="FAILURE_GUIDED_V1:"+JsonR27.write(event);
                    plan.put("CANDIDATE",policy,campaign,policy,policy.equals(control)?controlText:childText,policy.equals(control)?"":parent,policy.equals(control)?"REGISTERED_SEED":operation);
                    plan.put("ATTEMPT",policy,"attempt:"+policy,campaign,policy,parent,operation,"STRUCTURAL_CANDIDATE",StatsR27.sha256(policy));
                }
                tx(db,()->{
                    db.execSQL("DELETE FROM r26_failure_research_cursor WHERE parent_policy=?",new Object[]{parent});
                    db.execSQL("INSERT INTO r26_qualification_result VALUES(?,?,'HELD_LATER_ECE_GT_004',99999)",new Object[]{parent,m.get("failureEvidenceDigest")});
                    db.execSQL("INSERT INTO r26_live_model VALUES(?,?,'ACTIVE')",new Object[]{parent,parentText});
                    stage(db,plan);FoundryCampaignR26.publishBirthPreparedR31(db,plan);
                });
            }
        }
        List<HypothesisStoreR31.Sample> trajectory(boolean changed){
            ModelGenomeSU2 original=FamilyRegistryR26.genome("F01");
            ModelGenomeSU2 model=changed?new ModelGenomeSU2(original.members,original.pool,original.sink,original.gate,original.coverage,original.metaC,true):original;
            ModelLiveSU2.Hub hub=new ModelLiveSU2.Hub();ModelLiveSU2 live=new ModelLiveSU2(model,hub,ModelLiveLoopSU2.START_META,ModelLiveLoopSU2.WINDOW,ModelLiveLoopSU2.REFIT);
            List<HypothesisStoreR31.Sample> trace=new ArrayList<>();
            for(int t=0;t<samples;t++){
                int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=(p+t)%10+1;
                ModelLiveSU2.Decision d=live.decide();boolean hit=live.settle(draw);hub.update(draw);actualDecisions++;
                if(t>=samples*3/5)trace.add(new HypothesisStoreR31.Sample(t,ordinals.get(t-samples*3/5),d,hit));
            }return trace;
        }
        void trace(SQLiteDatabase db,String policy,List<HypothesisStoreR31.Sample> trace)throws Exception{for(int i=0;i<trace.size();i+=16){List<HypothesisStoreR31.Sample> part=trace.subList(i,Math.min(i+16,trace.size()));tx(db,()->HypothesisStoreR31.recordSelectWithinR31(db,campaign,policy,source,part));}}
        void finishViaProduction(SQLiteDatabase db,String prepared)throws Exception{
            try(ModelPublicationR31.Plan p=new ModelPublicationR31.Plan("FINISH:"+campaign,"FOUNDRY_FINISH",source,0,-1,10)){
                p.put("FOUNDRY_FINISH",campaign,campaign,prepared);tx(db,()->FoundryCampaignR26.publishFinishPreparedR31(db,p));
            }
        }
    }
    static void stage(SQLiteDatabase db,ModelPublicationR31.Plan plan){int index=0;for(ModelPublicationR31.Item item:plan.items.values()){
        Object[] values=new Object[19];values[0]=plan.id;values[1]=index++;values[2]=item.kind;System.arraycopy(item.values,0,values,3,item.values.length);
        db.execSQL("INSERT INTO r31_model_publication_item VALUES("+String.join(",",Collections.nCopies(19,"?"))+")",values);
    }}
    @SuppressWarnings("unchecked") static Map<String,Object> outcome(String text){return (Map<String,Object>)((List<?>)JsonR27.parse(text)).get(0);}
    static String changed(String prepared,String key,Object value){Map<String,Object> row=outcome(prepared);row.put(key,value);return JsonR27.write(Collections.singletonList(row));}
    static void child(String[] args)throws Exception{
        Path path=Paths.get(args[1]);SQLiteDatabase db=open(path);Fixture f=new Fixture("kill",1000);
        db.jdbc.setAutoCommit(false);
        HypothesisStoreR31.recordSelectWithinR31(db,f.campaign,f.child,f.source,Collections.singletonList(new HypothesisStoreR31.Sample(600,f.ordinals.get(0),new ModelLiveSU2.Decision(600,1,7,true,.5,.5),true)));
        if(args[0].equals("--commit-kill")){db.jdbc.commit();Runtime.getRuntime().halt(78);}Runtime.getRuntime().halt(77);
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].startsWith("--")){child(args);return;}
        ModelGenomeSU2 base=FamilyRegistryR26.genome("F01"),oldA=base.bindBirth(StatsR27.sha256("old-A"),100),oldB=base.bindBirth(StatsR27.sha256("old-B"),200);
        ok(!oldA.key().equals(oldB.key()),"HISTORICAL_PARENT_IDENTITIES_DISTINCT");
        String nextSnapshot=StatsR27.sha256("one-new-snapshot");
        ModelGenomeSU2 childA=new ModelGenomeSU2(oldA.members,oldA.pool,oldA.sink,oldA.gate,oldA.coverage,oldA.metaC,!oldA.calibrated).bindBirth(nextSnapshot,300);
        ModelGenomeSU2 childB=new ModelGenomeSU2(oldB.members,oldB.pool,oldB.sink,oldB.gate,oldB.coverage,oldB.metaC,!oldB.calibrated).bindBirth(nextSnapshot,300);
        ok(childA.key().equals(childB.key()),"REAL_BIRTH_REBINDING_COLLAPSES_HISTORICAL_ALIASES");
        List<FailureDirectedResearchR26.Batch> aliases=new ArrayList<>();
        for(ModelGenomeSU2 parent:new ModelGenomeSU2[]{oldA,oldB}){
            List<StructuralProposalR26.Proposal> proposals=Arrays.asList(new StructuralProposalR26.Proposal(parent.bindBirth(nextSnapshot,300),parent.key(),"PRESERVE_PARENT_STRUCTURE","NO_OP"),new StructuralProposalR26.Proposal(childA,parent.key(),"READOUT_TEMPERATURE_REFIT_ON_TRAIN","STRUCTURAL_CANDIDATE"));
            aliases.add(new FailureDirectedResearchR26.Batch(parent.key(),parent.text(),StatsR27.sha256(parent.key()),"HELD_LATER_ECE_GT_004","",300,-1,"{}",proposals));
        }
        List<FailureDirectedResearchR26.Batch> distinct=FailureDirectedResearchR26.distinctBatchesR31(aliases);
        ok(distinct.size()==1&&distinct.get(0)==aliases.get(0),"SAME_TRAINING_NOT_DOUBLE_REGISTERED_OR_CREDITED");
        ok(aliases.size()==2&&distinct.stream().noneMatch(b->b==aliases.get(1)),"DEFERRED_ALIAS_CURSOR_NOT_SELECTED_FOR_PUBLICATION");
        Path root=Paths.get(args[0]);Files.createDirectories(root);Path file=root.resolve("hypothesis.db");SQLiteDatabase db=open(file);schema(db);
        ok("OBSERVED_DURABLE_DEVELOPMENT_REGISTRY_NOT_QUALITY_PROOF".equals(HypothesisStoreR31.report(db).get("status")),"REAL_REPORT_PROJECTION");
        Fixture killed=new Fixture("kill",1000);killed.birth(db);db.jdbc.close();
        String java=Paths.get(System.getProperty("java.home"),"bin","java").toString(),cp=System.getProperty("java.class.path");
        Process pre=new ProcessBuilder(java,"-cp",cp,HypothesisStoreR31Host.class.getName(),"--rollback-kill",file.toString()).inheritIO().start();ok(pre.waitFor()==77,"ACTUAL_PROCESS_KILL_BEFORE_COMMIT");
        db=open(file);ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_select")==0,"KILL_ROLLS_BACK_TRACE_AND_QUOTA");long before=scalar(db,"SELECT logical_bytes FROM r31_hypothesis_quota");db.jdbc.close();
        Process post=new ProcessBuilder(java,"-cp",cp,HypothesisStoreR31Host.class.getName(),"--commit-kill",file.toString()).inheritIO().start();ok(post.waitFor()==78,"ACTUAL_PROCESS_KILL_AFTER_COMMIT");
        db=open(file);ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_select")==1,"COMMITTED_SAMPLE_SURVIVES_REOPEN");ok(scalar(db,"SELECT logical_bytes FROM r31_hypothesis_quota")==before+1024,"QUOTA_COMMITTED_WITH_SAMPLE");
        final SQLiteDatabase active=db;long current=scalar(db,"SELECT logical_bytes FROM r31_hypothesis_quota");
        tx(db,()->HypothesisStoreR31.recordSelectWithinR31(active,killed.campaign,killed.child,killed.source,Collections.singletonList(new HypothesisStoreR31.Sample(600,killed.ordinals.get(0),new ModelLiveSU2.Decision(600,1,7,true,.5,.5),true))));
        ok(scalar(db,"SELECT logical_bytes FROM r31_hypothesis_quota")==current,"RETRY_DOES_NOT_DUPLICATE_SAMPLE_OR_COST");
        fails(()->HypothesisStoreR31.recordSelectWithinR31(active,killed.campaign,killed.child,killed.source,Collections.singletonList(new HypothesisStoreR31.Sample(0,100000,new ModelLiveSU2.Decision(0,1,7,true,.5,.5),true))),"NO_BUSINESS_DDL_OR_UNFENCED_TRACE");
        tx(db,()->fails(()->HypothesisStoreR31.recordSelectWithinR31(active,killed.campaign,killed.child,killed.source,Collections.singletonList(new HypothesisStoreR31.Sample(599,100599,new ModelLiveSU2.Decision(599,1,7,true,.5,.5),true))),"TRAIN_ROLE_CANNOT_ENTER_SELECT"));
        tx(db,()->fails(()->HypothesisStoreR31.recordSelectWithinR31(active,killed.campaign,killed.child,killed.source,Collections.singletonList(new HypothesisStoreR31.Sample(600,killed.ordinals.get(0),new ModelLiveSU2.Decision(600,1,7,true,.6,.6),false))),"RETRY_WITH_CHANGED_LABEL_REJECTED"));
        String partial=HypothesisStoreR31.prepareOutcomesR31(db,killed.campaign);ok("MISSING_ORIGINAL_SELECT_TRACE".equals(outcome(partial).get("state")),"PARTIAL_IS_UNKNOWN_NOT_CREDIT");ok(outcome(partial).get("netCredit")==null,"MISSING_TRACE_NEVER_ZERO_PASS");
        killed.finishViaProduction(db,partial);ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_outcome WHERE state='MISSING_ORIGINAL_SELECT_TRACE' AND net_credit IS NULL")==1,"ACTUAL_FOUNDRY_FINISH_PERSISTS_UNKNOWN");
        Fixture first=new Fixture("paired0",1000);List<HypothesisStoreR31.Sample> control=first.trajectory(false),child=first.trajectory(true);
        for(int repetition=0;repetition<3;repetition++){
            Fixture f=new Fixture("paired"+repetition,1000);f.birth(db);f.trace(db,f.control,control);f.trace(db,f.child,child);String prepared=HypothesisStoreR31.prepareOutcomesR31(db,f.campaign);
            Map<String,Object> row=outcome(prepared);ok("PAIRED_SELECT_COMPLETE".equals(row.get("state")),"ACTUAL_MODEL_TRAJECTORIES_PAIRED_BY_ORDINAL_AND_SOURCE");ok(((Number)row.get("pairedRows")).longValue()==400,"ALL_OPPORTUNITIES_INCLUDE_ABSTAIN");
            fails(()->{try{f.finishViaProduction(active,changed(prepared,"role","FUTURE"));}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}},"FUTURE_CANNOT_UPDATE_SELECT_CREDIT");
            fails(()->{try{f.finishViaProduction(active,changed(prepared,"family","WRONG"));}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}},"FOREIGN_HYPOTHESIS_FAMILY_REJECTED");
            fails(()->{try{f.finishViaProduction(active,changed(prepared,"netCredit",999.0));}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}},"TAMPERED_NET_CREDIT_REJECTED");
            f.finishViaProduction(db,prepared);ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_outcome WHERE state='PAIRED_SELECT_COMPLETE'")==repetition+1,"CREDIT_COMMITS_WITH_CAMPAIGN_FINISH");
            final String repeat=prepared;tx(db,()->HypothesisStoreR31.publishOutcomesWithinR31(active,f.campaign,repeat,11));
            ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_outcome WHERE state='PAIRED_SELECT_COMPLETE'")==repetition+1,"RETRIED_OUTCOME_IDEMPOTENT");
            if(repetition<2)ok(HypothesisStoreR31.creditR31(db,"I10")==0,"SPARSE_CREDIT_RETURNS_EXPLORATION_PRIOR");
        }
        Fixture blocked=new Fixture("blocked",1000);blocked.birth(db);tx(db,()->active.execSQL("UPDATE r26_candidate SET status='BLOCKED_CHECKPOINT_OR_PROGRAM' WHERE campaign_id='blocked'"));
        String quarantined=HypothesisStoreR31.prepareOutcomesR31(db,blocked.campaign);ok("CANDIDATE_NOT_SCORED".equals(outcome(quarantined).get("state")),"QUARANTINED_CANDIDATE_NOT_FAKE_SCORED");blocked.finishViaProduction(db,quarantined);
        ok(scalar(db,"SELECT count(*) FROM r26_foundry WHERE campaign_id='blocked' AND status='DONE'")==1,"FAILED_CHILD_DOES_NOT_DEADLOCK_CAMPAIGN_FINISH");
        Fixture shortCase=new Fixture("short",64);shortCase.birth(db);shortCase.trace(db,shortCase.control,shortCase.trajectory(false));shortCase.trace(db,shortCase.child,shortCase.trajectory(true));String insufficient=HypothesisStoreR31.prepareOutcomesR31(db,shortCase.campaign);
        ok("INSUFFICIENT_SELECT_SUPPORT".equals(outcome(insufficient).get("state"))&&outcome(insufficient).get("netCredit")==null,"SHORT_COMPLETE_TRACE_DOES_NOT_LOWER_SUPPORT_THRESHOLD");shortCase.finishViaProduction(db,insufficient);
        db.execSQL("INSERT INTO r26_foundry(campaign_id,snapshot_hash,status) VALUES('legacy-finish',?,'RUNNING')",new Object[]{first.source});
        try(ModelPublicationR31.Plan legacy=new ModelPublicationR31.Plan("LEGACY_FINISH","FOUNDRY_FINISH",first.source,0,-1,20)){
            legacy.put("FOUNDRY_FINISH","legacy-finish","legacy-finish");tx(db,()->FoundryCampaignR26.publishFinishPreparedR31(active,legacy));
        }
        ok(scalar(db,"SELECT count(*) FROM r26_foundry WHERE campaign_id='legacy-finish' AND status='DONE'")==1,"OLD_PERSISTED_SINGLE_VALUE_FINISH_RECOVERS_WITHOUT_FAKE_TRACE");
        long registryBefore=scalar(db,"SELECT count(*) FROM r31_hypothesis_registry");
        try(ModelPublicationR31.Plan legacy=new ModelPublicationR31.Plan("LEGACY_BIRTH","FOUNDRY_BIRTH",first.source,0,-1,21)){
            legacy.put("FOUNDRY_ROOT","legacy-birth","legacy-birth",first.source,first.through,StatsR27.sha256("old-raw"),"RUNNING",21L,"old-abi",100000L,1);
            legacy.put("FAILURE_CURSOR",first.parent,first.parent,"",-1L,"old-evidence",99999L,"{\"legacy\":\"untraced\"}","HELD_LATER_ECE_GT_004",first.controlText);
            tx(db,()->{
                active.execSQL("INSERT INTO r26_qualification_result VALUES(?,'old-evidence','HELD_LATER_ECE_GT_004',99999)",new Object[]{first.parent});
                active.execSQL("INSERT INTO r26_live_model VALUES(?,?,'ACTIVE')",new Object[]{first.parent,first.controlText});
                int index=0;for(ModelPublicationR31.Item item:legacy.items.values()){
                    Object[] values=new Object[19];values[0]=legacy.id;values[1]=index++;values[2]=item.kind;System.arraycopy(item.values,0,values,3,item.values.length);
                    active.execSQL("INSERT INTO r31_model_publication_item VALUES("+String.join(",",Collections.nCopies(19,"?"))+")",values);
                }
                FoundryCampaignR26.publishBirthPreparedR31(active,legacy);
            });
        }
        ok(scalar(db,"SELECT count(*) FROM r26_foundry WHERE campaign_id='legacy-birth'")==1,"OLD_NINE_VALUE_BIRTH_AND_FAILURE_CURSOR_RECOVER");
        ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_registry")==registryBefore,"OLD_BIRTH_DOES_NOT_SYNTHESIZE_PAST_HYPOTHESIS_OR_SELECT");
        Fixture realBirth=new Fixture("actual-birth",1000);realBirth.birthViaProduction(db);
        ok(scalar(db,"SELECT count(*) FROM r31_hypothesis_binding WHERE campaign_id='actual-birth'")==1,"ACTUAL_FOUNDRY_BIRTH_REGISTERS_TYPED_BINDING");
        ok(scalar(db,"SELECT count(*) FROM r26_candidate WHERE campaign_id='actual-birth' AND parent='' AND operation='REGISTERED_SEED'")==1,"SAME_ID_SEED_CONTROL_FIRST_ORIGIN_PRESERVED");
        double measured=HypothesisStoreR31.creditR31(db,"I10");ok(Double.isFinite(measured),"NEXT_DISPATCH_READS_DURABLE_DEVELOPMENT_CREDIT");
        tx(db,()->fails(()->active.execSQL("INSERT OR REPLACE INTO r31_hypothesis_select SELECT campaign_id,policy_hash,sample_index,ordinal,snapshot_hash,position,mask,act,1-hit,phat,trace_hash FROM r31_hypothesis_select LIMIT 1"),"INSERT_REPLACE_CANNOT_REWRITE_ORIGINAL"));
        tx(db,()->fails(()->active.execSQL("UPDATE r31_hypothesis_outcome SET net_credit=1"),"OUTCOMES_IMMUTABLE"));
        tx(db,()->active.execSQL("UPDATE r31_hypothesis_quota SET logical_bytes=268435456"));
        ok(!HypothesisStoreR31.canAdmitResearchR31(db,1000),"FULL_BRANCH_DEBT_ADMISSION_EXPLICIT_WAIT");
        ok(FailureDirectedResearchR26.select(db,first.source,first.through,1000).isEmpty(),"REAL_SELECTOR_OMITS_ONLY_NEW_FAILURE_BRANCH_AT_QUOTA");
        Fixture over=new Fixture("quota",1000);fails(()->{try{over.birth(active);}catch(RuntimeException e){throw e;}catch(Exception e){throw new RuntimeException(e);}},"PROTECTED_BUDGET_STOPS_RESEARCH_WITHOUT_DELETING_ORIGINALS");
        ok(scalar(db,"SELECT count(*) FROM r26_foundry WHERE campaign_id='quota'")==0,"ADMISSION_FAILURE_ROLLS_BACK_BIRTH");
        String original=null;for(String sql:HypothesisStoreR31.ddlR31())if(sql.startsWith("CREATE TRIGGER IF NOT EXISTS r31_hypothesis_registry_update "))original=sql;
        ok(original!=null,"OWNER_TRIGGER_SOURCE_FOUND");
        for(String tamper:new String[]{"R31_HYPOTHESIS_ORIGINAL_IF NOT EXISTS IMMUTABLE","r31_hypothesis_original_immutable","R31_HYPOTHESIS_ORIGINAL_ IMMUTABLE","R31_HYPOTHESIS_ORIGINAL_;IMMUTABLE"}){
            final String changedSql=original.replace("R31_HYPOTHESIS_ORIGINAL_IMMUTABLE",tamper);
            tx(db,()->{active.execSQL("DROP TRIGGER r31_hypothesis_registry_update");active.execSQL(changedSql);});
            fails(()->HypothesisStoreR31.requireSchemaR31(active),"QUOTED_LITERAL_BYTES_NOT_NORMALIZED_AWAY");
        }
        final String ownerSql=original;tx(db,()->{active.execSQL("DROP TRIGGER r31_hypothesis_registry_update");active.execSQL(ownerSql);});
        HypothesisStoreR31.requireSchemaR31(db);ok("OBSERVED_DURABLE_DEVELOPMENT_REGISTRY_NOT_QUALITY_PROOF".equals(HypothesisStoreR31.report(db).get("status")),"REPORT_CONSUMES_PERSISTED_OUTCOMES");
        try(Connection empty=DriverManager.getConnection("jdbc:sqlite::memory:")){ok("UNAVAILABLE_REGISTRY_OR_SOURCE".equals(HypothesisStoreR31.report(new SQLiteDatabase(empty)).get("status")),"MISSING_SOURCE_IS_UNKNOWN");}
        db.jdbc.close();System.out.println("HYPOTHESIS_STORE_NATIVE_PASS assertions="+assertions+" actualModelDecisions="+actualDecisions+" actualKillRestart=true syntheticOnly=true");
    }
}
