package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Binds selection to the executable policy/readout and, when required, the
 * already committed learner/model publication. ABI is recorded separately. */
public final class MainlineModelBindingR31 {
    private MainlineModelBindingR31() {}
    public static Map<String,Object> capture(SQLiteDatabase db,String policy,PolicySpecR27 spec,double temperature,
            String semanticAbi,ModelSurfaceProviderR30.FrozenModelRef model) {
        Map<String,Object> binding=new LinkedHashMap<>();binding.put("format","R31_MAINLINE_MODEL_BINDING_V1");binding.put("policyHash",policy);
        binding.put("policyText",spec.canonical());binding.put("temperature",temperature);binding.put("semanticAbi",semanticAbi);
        binding.put("scope","COMMITTED_EXECUTABLE_STATE_IDENTITY_NOT_QUALITY_EVIDENCE");
        Map<String,Object> identity=executableIdentityR31(db,policy,spec);
        if(identity==null||!Double.isFinite(temperature)||temperature<=0){binding.put("state","WAIT_EXECUTABLE_IDENTITY");return Collections.unmodifiableMap(binding);}
        binding.putAll(identity);
        try(Cursor c=db.rawQuery("SELECT spec_json,temperature,bias,through_ordinal FROM r27_live_shadow WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",new String[]{policy})){
            if(!c.moveToFirst()||!spec.policyHash().equals(PolicySpecR27.parse(c.getString(0)).policyHash())||Double.compare(temperature,c.getDouble(1))!=0)throw new IllegalStateException("R31_MAINLINE_SHADOW_STATE_CHANGED");
            byte[] bias=c.getBlob(2);if(bias==null||bias.length>8192)throw new IllegalStateException("R31_MAINLINE_SHADOW_STATE_BOUND");
            binding.put("shadowBiasSha256",StatsR27.sha256(bias));binding.put("shadowThroughOrdinal",c.getLong(3));
        }
        if(spec.ast.modelPositions==0)binding.put("modelKind","POLICY_EXECUTABLE_AND_SHADOW_STATE");
        else {
            binding.put("modelKind","ATOMNET_PUBLISHED_LEARNER_AND_WEIGHTS");
            if(model==null){binding.put("state","WAIT_MODEL_PUBLICATION");return Collections.unmodifiableMap(binding);}
            try(Cursor c=db.rawQuery("SELECT publication_id,state_hash,weights_hash,through_ordinal,model_available_at,published_at FROM r31_atomnet_publication WHERE model_hash=? AND weights_hash=? AND status='PUBLISHED' ORDER BY published_at DESC,publication_id LIMIT 1",new String[]{model.modelHash,model.weightsHash})) {
                if(!c.moveToFirst()||c.isNull(5)||c.getLong(3)!=model.trainedThroughOrdinal||c.getLong(4)!=model.availableAt||!c.getString(1).matches("[0-9a-f]{64}")){
                    binding.put("state","WAIT_MODEL_PUBLICATION");return Collections.unmodifiableMap(binding);
                }
                binding.put("publicationId",c.getString(0));binding.put("learnerStateHash",c.getString(1));binding.put("modelHash",model.modelHash);binding.put("weightsHash",c.getString(2));binding.put("trainedThroughOrdinal",c.getLong(3));binding.put("modelAvailableAt",c.getLong(4));binding.put("publishedAt",c.getLong(5));
            }
        }
        binding.put("state","BOUND");binding.put("bindingSha256",StatsR27.sha256(JsonR27.write(binding)));return Collections.unmodifiableMap(binding);
    }
    /** Small writer-side recheck; no model decoding or file traversal. */
    public static boolean stillCommitted(SQLiteDatabase db,Map<String,Object> binding) {
        if(!"BOUND".equals(binding.get("state")))return false;
        Map<String,Object> original=new LinkedHashMap<>(binding);Object expected=original.remove("bindingSha256");
        if(!Objects.equals(expected,StatsR27.sha256(JsonR27.write(original))))return false;
        PolicySpecR27 spec;try(Cursor c=db.rawQuery("SELECT spec_json FROM r27_live_shadow WHERE policy_hash=?",new String[]{String.valueOf(binding.get("policyHash"))})){if(!c.moveToFirst())return false;spec=PolicySpecR27.parse(c.getString(0));}
        Map<String,Object> identity=executableIdentityR31(db,String.valueOf(binding.get("policyHash")),spec);if(identity==null)return false;
        for(Map.Entry<String,Object> entry:identity.entrySet())if(!Objects.equals(entry.getValue(),binding.get(entry.getKey())))return false;
        try(Cursor c=db.rawQuery("SELECT temperature,bias,through_ordinal FROM r27_live_shadow WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",new String[]{String.valueOf(binding.get("policyHash"))})){
            if(!c.moveToFirst()||Double.compare(c.getDouble(0),((Number)binding.get("temperature")).doubleValue())!=0||!Objects.equals(binding.get("shadowBiasSha256"),StatsR27.sha256(c.getBlob(1)))||c.getLong(2)!=((Number)binding.get("shadowThroughOrdinal")).longValue())return false;
        }
        if(!binding.containsKey("publicationId"))return true;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_atomnet_publication WHERE publication_id=? AND state_hash=? AND model_hash=? AND weights_hash=? AND status='PUBLISHED'",new String[]{String.valueOf(binding.get("publicationId")),String.valueOf(binding.get("learnerStateHash")),String.valueOf(binding.get("modelHash")),String.valueOf(binding.get("weightsHash"))})){return c.moveToFirst();}
    }
    /** A semantic rebirth has a new experiment ID and permanent alpha index, but
     * retains the executable spec. Only the original ABI_REBIRTH writer's exact
     * lineage+CHANGED receipt can establish that exception. */
    static Map<String,Object> executableIdentityR31(SQLiteDatabase db,String experiment,PolicySpecR27 spec){
        String base,reason;long index,promoted;double temperature;try(Cursor c=db.rawQuery("SELECT base_policy,global_index,reason,promoted_at,temperature FROM r27_live_shadow WHERE policy_hash=?",new String[]{experiment})){if(!c.moveToFirst())return null;base=c.getString(0);index=c.getLong(1);reason=c.getString(2);promoted=c.getLong(3);temperature=c.getDouble(4);}
        Map<String,Object> identity=new LinkedHashMap<>();identity.put("executablePolicyHash",spec.policyHash());
        if(experiment.equals(spec.policyHash())&&(base==null||base.isEmpty()||base.equals(spec.policyHash()))){identity.put("experimentIdentity","ORIGINAL_POLICY");return identity;}
        if(!spec.policyHash().equals(base))return null;
        Map<String,Object> registered=registeredFutureSegmentR31(db,experiment,spec,reason,index,promoted,temperature);
        if(registered!=null){identity.putAll(registered);return identity;}
        try(Cursor c=db.rawQuery("SELECT l.parent_hash,a.new_build,p.spec_json,p.global_index,p.temperature FROM r27_lineage l JOIN r27_abi_continuity a ON a.policy_hash=l.parent_hash AND a.result='CHANGED' JOIN r27_live_shadow p ON p.policy_hash=l.parent_hash WHERE l.child_hash=? AND l.role='ABI_REBIRTH' AND l.operator='ABI_REBIRTH' AND l.origin='R27_LIVE' LIMIT 17",new String[]{experiment})){
            int rows=0;while(c.moveToNext()){if(++rows>16)return null;String parent=c.getString(0),build=c.getString(1);if(experiment.equals(StatsR27.sha256(parent+"|ABI_REBIRTH|"+build))&&spec.fullJson().equals(PolicySpecR27.parse(c.getString(2)).fullJson())&&index>c.getLong(3)&&Double.compare(temperature,c.getDouble(4))==0){identity.put("experimentIdentity","VERIFIED_ABI_REBIRTH");identity.put("rebirthParent",parent);identity.put("rebirthBuild",build);return identity;}}
        }return null;
    }
    /** The original independent-confirmation owner may start a new experiment
     * after sealing a missing-opportunity segment. Its packet and registration
     * are authenticated through primary-key reads, never by an ID-prefix grant. */
    private static Map<String,Object> registeredFutureSegmentR31(SQLiteDatabase db,String experiment,PolicySpecR27 spec,String reason,long index,long promoted,double temperature){
        String prefix="INDEPENDENT_CONFIRMATION:";if(reason==null||!reason.startsWith(prefix))return null;String taskKey=reason.substring(prefix.length());
        if(!taskKey.matches("[0-9a-f]{64}")||!experiment.equals(StatsR27.sha256("R30_REPAIR_NEW_FUTURE_SEGMENT|"+spec.policyHash()+"|"+taskKey)))return null;
        String packet,result,dataset;long block,registered;
        try(Cursor c=db.rawQuery("SELECT packet_json,result_json,dataset_hash,block_id,registered_at,policy_hash,state,verified,registration_state,registration_experiment_id FROM r30_frozen_confirm WHERE task_key=? AND length(CAST(packet_json AS BLOB))<=262144 AND length(CAST(result_json AS BLOB))<=262144",new String[]{taskKey})){
            if(!c.moveToFirst()||!spec.policyHash().equals(c.getString(5))||!"DONE".equals(c.getString(6))||c.getInt(7)!=1||!"REGISTERED".equals(c.getString(8))||!experiment.equals(c.getString(9)))return null;
            packet=c.getString(0);result=c.getString(1);dataset=c.getString(2);block=c.getLong(3);registered=c.getLong(4);
        }
        FrozenConfirmationR30.Task task;Map<?,?> verdict;
        try{task=FrozenConfirmationR30.Task.parse(packet);Object parsed=JsonR27.parse(result);if(!(parsed instanceof Map))return null;verdict=(Map<?,?>)parsed;}
        catch(RuntimeException malformedPacket){return null;}
        if(!taskKey.equals(task.key)||!spec.fullJson().equals(task.champion.spec().fullJson())||Double.compare(temperature,task.champion.temperature)!=0||!spec.policyHash().equals(task.champion.policyHash)||!dataset.equals(task.datasetHash)||block!=task.block||registered<=0||registered<promoted||registered<task.frozenAt||!Boolean.TRUE.equals(verdict.get("verified"))||!"COMPLETED".equals(verdict.get("status"))||!taskKey.equals(verdict.get("taskKey"))||!dataset.equals(verdict.get("datasetHash"))||!(verdict.get("holdoutBlock") instanceof Number)||((Number)verdict.get("holdoutBlock")).longValue()!=block)return null;
        long parentIndex,sealedAt;
        try(Cursor c=db.rawQuery("SELECT status,updated_at,global_index,spec_json FROM r27_live_shadow WHERE policy_hash=?",new String[]{spec.policyHash()})){
            if(!c.moveToFirst()||!QualificationGapRepairR30R1.SEALED.equals(c.getString(0))||!spec.canonical().equals(PolicySpecR27.parse(c.getString(3)).canonical()))return null;sealedAt=c.getLong(1);parentIndex=c.getLong(2);
        }
        if(task.frozenAt<=sealedAt||promoted<=sealedAt||index<=parentIndex)return null;
        Map<String,Object> identity=new LinkedHashMap<>();identity.put("experimentIdentity","VERIFIED_NEW_FUTURE_SEGMENT");identity.put("confirmationTask",taskKey);identity.put("confirmationPacketHash",StatsR27.sha256(packet));identity.put("confirmationResultHash",StatsR27.sha256(result));identity.put("registeredAt",registered);identity.put("sealedParent",spec.policyHash());identity.put("sealedAt",sealedAt);return identity;
    }
}
