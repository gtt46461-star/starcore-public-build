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
        if(!policy.equals(spec.policyHash())||!Double.isFinite(temperature)||temperature<=0)throw new IllegalStateException("R31_MAINLINE_EXECUTABLE_IDENTITY");
        try(Cursor c=db.rawQuery("SELECT spec_json,temperature,bias,through_ordinal FROM r27_live_shadow WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",new String[]{policy})){
            if(!c.moveToFirst()||!policy.equals(PolicySpecR27.parse(c.getString(0)).policyHash())||Double.compare(temperature,c.getDouble(1))!=0)throw new IllegalStateException("R31_MAINLINE_SHADOW_STATE_CHANGED");
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
        try(Cursor c=db.rawQuery("SELECT temperature,bias,through_ordinal FROM r27_live_shadow WHERE policy_hash=? AND status IN ('ACTIVE','ACTIVE_REPAIR1')",new String[]{String.valueOf(binding.get("policyHash"))})){
            if(!c.moveToFirst()||Double.compare(c.getDouble(0),((Number)binding.get("temperature")).doubleValue())!=0||!Objects.equals(binding.get("shadowBiasSha256"),StatsR27.sha256(c.getBlob(1)))||c.getLong(2)!=((Number)binding.get("shadowThroughOrdinal")).longValue())return false;
        }
        if(!binding.containsKey("publicationId"))return true;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_atomnet_publication WHERE publication_id=? AND state_hash=? AND model_hash=? AND weights_hash=? AND status='PUBLISHED'",new String[]{String.valueOf(binding.get("publicationId")),String.valueOf(binding.get("learnerStateHash")),String.valueOf(binding.get("modelHash")),String.valueOf(binding.get("weightsHash"))})){return c.moveToFirst();}
    }
}
