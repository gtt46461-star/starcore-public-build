package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Controlled preverified confirmation is setup input, not a confirmation
 * evaluation or natural result. Only the ORIGINAL freeze/complete/register
 * producers and the experiment/model identity readers are exercised here. */
public final class TypedRegisteredSegmentR31Host {
    private static int assertions;
    private static void need(boolean yes,String why){assertions++;if(!yes)throw new AssertionError(why);}
    private static long n(TypedFreezeRuntimeR31Fixture f,String sql,String... args){try(Cursor c=f.db.rawQuery(sql,args)){if(!c.moveToFirst())throw new AssertionError("SEGMENT_SCALAR_UNKNOWN");return c.getLong(0);}}
    private static String parentState(TypedFreezeRuntimeR31Fixture f){
        List<Object> fields=new ArrayList<>();try(Cursor c=f.db.rawQuery("SELECT global_index,alpha,n,acts,hits,log_e_up,log_e_down,sum_u,sum_u2,last_ordinal,state FROM r27_eprocess WHERE policy_hash=?",new String[]{f.policy})){need(c.moveToFirst(),"PARENT_ORIGINAL_STATE_EXISTS");for(int i=0;i<c.getColumnCount();i++)fields.add(c.getString(i));}return StatsR27.sha256(JsonR27.write(fields));
    }
    private static void invalid(TypedFreezeRuntimeR31Fixture f,String child,PolicySpecR27 spec,Map<String,Object> bound,String label,Runnable mutate){
        f.transaction(()->{
            f.db.execSQL("SAVEPOINT registered_segment_negative");
            try{
                mutate.run();
                Map<String,Object> changed=MainlineModelBindingR31.capture(f.db,child,spec,1,f.runtime.semanticAbi(spec),null);
                need(!"BOUND".equals(changed.get("state")),"REGISTRATION_TAMPER_RETURNS_WAIT:"+label);
                need(!MainlineModelBindingR31.stillCommitted(f.db,bound),"IMMUTABLE_BINDING_READER_REFUSES:"+label);
            }finally{f.db.execSQL("ROLLBACK TO registered_segment_negative");f.db.execSQL("RELEASE registered_segment_negative");}
        });
        need(MainlineModelBindingR31.stillCommitted(f.db,bound),"ROLLBACK_RESTORES_REAL_REGISTRATION_BINDING:"+label);
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        try(TypedFreezeRuntimeR31Fixture f=new TypedFreezeRuntimeR31Fixture(Path.of(args[0]))){
            Field workerField=TypedFreezeRuntimeR31Fixture.class.getDeclaredField("worker");workerField.setAccessible(true);ResearchPoolR27.Worker worker=(ResearchPoolR27.Worker)workerField.get(f);
            PolicySpecR27 spec;try(Cursor c=f.db.rawQuery("SELECT spec_json FROM r27_live_shadow WHERE policy_hash=?",new String[]{f.policy})){need(c.moveToFirst(),"ACTUAL_REGISTERED_PARENT_SPEC");spec=PolicySpecR27.parse(c.getString(0));}
            String original=parentState(f);long parentIndex=n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",f.policy);
            long now=System.currentTimeMillis();
            f.transaction(()->f.db.execSQL("UPDATE r27_live_shadow SET status=?,updated_at=? WHERE policy_hash=?",new Object[]{QualificationGapRepairR30R1.SEALED,now-10,f.policy}));
            ReplayDatasetR27 data=ReplayDatasetR27.synthetic(5000,931,new int[0]);
            Map<String,Object> policy=new LinkedHashMap<>();policy.put("spec",spec.fullJson());policy.put("tau",1d);policy.put("kind","MAIN");policy.put("seed",0L);policy.put("evaluationDatasetHash",data.datasetHash);
            Map<String,Object> packet=new LinkedHashMap<>();packet.put("dataset",data.datasetHash);packet.put("sourceCommit",com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26);packet.put("researchSemanticHash",FrozenConfirmationR30.currentSemantics());packet.put("generation",0L);packet.put("block",31L);packet.put("from",4000L);packet.put("to",4500L);packet.put("frozenAt",now);packet.put("nullRequired",false);packet.put("champion",policy);packet.put("previous",null);packet.put("i00",null);packet.put("null",null);
            FrozenConfirmationR30.Task task=FrozenConfirmationR30.Task.parse(JsonR27.write(packet));
            ResearchWriteQueueR27.Batch freeze=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.freeze(freeze,task,data);f.owned(()->{RuntimeR27.submit(worker,"HOST_CONTROLLED_CONFIRM_PACKET",freeze);return null;});
            Map<String,Object> controlledResult=new LinkedHashMap<>();controlledResult.put("taskKey",task.key);controlledResult.put("datasetHash",task.datasetHash);controlledResult.put("holdoutBlock",task.block);controlledResult.put("status","COMPLETED");controlledResult.put("verified",true);controlledResult.put("inputScope","CONTROLLED_PREVERIFIED_CONFIRMATION_INPUT_NOT_EVALUATION_NOT_NATURAL");
            FrozenConfirmationR30.Result result=new FrozenConfirmationR30.Result(controlledResult);ResearchWriteQueueR27.Batch complete=new ResearchWriteQueueR27.Batch();FrozenConfirmationStoreR30.complete(complete,task,result);f.owned(()->{RuntimeR27.submit(worker,"HOST_CONTROLLED_PREVERIFIED_CONFIRM_COMPLETION",complete);return null;});
            f.owned(()->{RuntimeR27.submit(worker,"HOST_ORIGINAL_REGISTER_NEW_FUTURE_SEGMENT",FrozenConfirmationStoreR30.register(task,f.runtime.semanticAbi(spec),32));return null;});
            String child=StatsR27.sha256("R30_REPAIR_NEW_FUTURE_SEGMENT|"+spec.policyHash()+"|"+task.key);
            need(!child.equals(spec.policyHash()),"REGISTERED_SEGMENT_HAS_INDEPENDENT_EXPERIMENT_ID");
            try(Cursor c=f.db.rawQuery("SELECT registration_experiment_id,registration_state,registered_at FROM r30_frozen_confirm WHERE task_key=?",new String[]{task.key})){need(c.moveToFirst()&&child.equals(c.getString(0))&&"REGISTERED".equals(c.getString(1))&&c.getLong(2)>0,"ORIGINAL_REGISTRATION_DURABLE_RECEIPT");}
            Map<String,Object> bound=MainlineModelBindingR31.capture(f.db,child,spec,1,f.runtime.semanticAbi(spec),null);
            need("BOUND".equals(bound.get("state")),"ORIGINAL_CONFIRMATION_REGISTERED_SEGMENT_BINDABLE");
            need(spec.policyHash().equals(bound.get("executablePolicyHash"))&&child.equals(bound.get("policyHash")),"EXECUTABLE_AND_SEGMENT_IDENTITY_KEPT_SEPARATE");
            need(MainlineModelBindingR31.stillCommitted(f.db,bound),"REAL_REGISTERED_SEGMENT_READER_ACCEPTS");
            need(n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",child)>parentIndex&&n(f,"SELECT n FROM r27_eprocess WHERE policy_hash=?",child)==0&&n(f,"SELECT acts FROM r27_eprocess WHERE policy_hash=?",child)==0&&n(f,"SELECT last_ordinal FROM r27_eprocess WHERE policy_hash=?",child)==-1,"ORIGINAL_NEW_ALPHA_ALLOCATION_WITH_ZERO_FUTURE_EXPOSURE");
            need(original.equals(parentState(f)),"SEALED_PARENT_FULL_ORIGINAL_EXPOSURE_UNCHANGED");
            long childIndex=n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",child);
            f.owned(()->{RuntimeR27.submit(worker,"HOST_REGISTER_DUPLICATE_REDELIVERY",FrozenConfirmationStoreR30.register(task,f.runtime.semanticAbi(spec),32));return null;});
            need(n(f,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",child)==childIndex&&n(f,"SELECT count(*) FROM r27_eprocess WHERE policy_hash=?",child)==1,"ORIGINAL_REGISTRATION_RETRY_DOES_NOT_SPEND_ANOTHER_ALPHA_INDEX");

            invalid(f,child,spec,bound,"FOREIGN_REGISTRATION_EXPERIMENT",()->f.db.execSQL("UPDATE r30_frozen_confirm SET registration_experiment_id=? WHERE task_key=?",new Object[]{StatsR27.sha256("HOST_FOREIGN_SEGMENT"),task.key}));
            Map<String,Object> changedPacket=new LinkedHashMap<>(packet),changedPolicy=new LinkedHashMap<>(policy);changedPolicy.put("tau",2d);changedPacket.put("champion",changedPolicy);
            invalid(f,child,spec,bound,"PACKET_READOUT_CHANGED",()->f.db.execSQL("UPDATE r30_frozen_confirm SET packet_json=? WHERE task_key=?",new Object[]{JsonR27.write(changedPacket),task.key}));
            Map<String,Object> falseResult=new LinkedHashMap<>(controlledResult);falseResult.put("verified",false);
            invalid(f,child,spec,bound,"RESULT_NOT_VERIFIED",()->f.db.execSQL("UPDATE r30_frozen_confirm SET result_json=? WHERE task_key=?",new Object[]{JsonR27.write(falseResult),task.key}));
            Map<String,Object> foreignResult=new LinkedHashMap<>(controlledResult);foreignResult.put("taskKey",StatsR27.sha256("HOST_FOREIGN_PACKET"));
            invalid(f,child,spec,bound,"RESULT_FOREIGN_TASK",()->f.db.execSQL("UPDATE r30_frozen_confirm SET result_json=? WHERE task_key=?",new Object[]{JsonR27.write(foreignResult),task.key}));
            invalid(f,child,spec,bound,"PARENT_NOT_SEALED",()->f.db.execSQL("UPDATE r27_live_shadow SET status='ACTIVE' WHERE policy_hash=?",new Object[]{f.policy}));
            invalid(f,child,spec,bound,"PACKET_NOT_AFTER_SEALED_PARENT",()->f.db.execSQL("UPDATE r27_live_shadow SET updated_at=? WHERE policy_hash=?",new Object[]{task.frozenAt+1,f.policy}));
            need(original.equals(parentState(f)),"ALL_NEGATIVE_CONTROLS_PRESERVE_PARENT_EXPOSURE");
            need(n(f,"SELECT count(*) FROM r27_certificate WHERE policy_hash=? AND status='ACTIVE'",child)==0,"REGISTERED_SEGMENT_WITHOUT_NEW_FUTURE_NEVER_GETS_CERTIFICATE");
        }
        System.out.println("TYPED_REGISTERED_SEGMENT_R31_HOST_PASS assertions="+assertions+" actualOriginalFreezeCompleteRegister=YES actualIndependentIndexAndZeroExposure=YES inputScope=CONTROLLED_PREVERIFIED_CONFIRMATION_INPUT_NOT_EVALUATION naturalQuality=NOT_PROVEN Android=NOT_RUN");
    }
}
