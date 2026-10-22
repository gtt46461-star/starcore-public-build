package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.EnumSet;

/** Persistent asset identity/tier/evidence registry. */
public final class ResearchAssetRegistryR906 {
    private ResearchAssetRegistryR906(){}
    public static Asset register(SQLiteDatabase db,ResearchMethodPlanR906 plan,ResearchMethodPlanR906 parent){
        if(db==null||plan==null)throw new IllegalArgumentException("R906_ASSET_REGISTER_INPUT");plan.validateNoLeakage();String sig=ResearchMethodSignatureR906.of(plan);String parentSig=parent==null?"":ResearchMethodSignatureR906.of(parent);
        EnumSet<ResearchAssetContractR906.Dimension> changed=plan.changedFrom(parent);ResearchCompiledGraphRuntimeR906.Compiled compiled=ResearchCompiledGraphRuntimeR906.compile(plan);
        ResearchAssetContractR906.NoveltyClass novelty=ResearchAssetContractR906.classify(parentSig,sig,changed,compiled.executable);
        ResearchAssetContractR906.ArtifactStatus artifact=compiled.executable?ResearchAssetContractR906.ArtifactStatus.PARITY_PASS:ResearchAssetContractR906.ArtifactStatus.WAITING_OFFLINE_TRAINER;
        String name=ResearchChineseNamingR906.name(plan),id="R906-"+sig.substring(0,24);long now=System.currentTimeMillis();
        Cursor c=null;try{c=db.rawQuery("SELECT asset_id,display_name_zh,tier,artifact_status,artifact_hash,registry_hash FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE method_signature=? LIMIT 1",new String[]{sig});if(c.moveToFirst())return new Asset(c.getString(0),c.getString(1),sig,ResearchAssetContractR906.Tier.valueOf(c.getString(2)),ResearchAssetContractR906.ArtifactStatus.valueOf(c.getString(3)),c.getString(4),c.getString(5));}finally{if(c!=null)c.close();}
        name=availableName(db,name,sig);
        String norm=ResearchChineseNamingR906.normalized(name);
        String registryHash=V51ChecksumEngine.sha256(id+'|'+name+'|'+sig+'|'+plan.canonical()+'|'+artifact);
        String trainedUntil=latestStrictOos(db);String earliestUse=nextIssue(trainedUntil);
        ContentValues v=new ContentValues();v.put("asset_id",id);v.put("display_name_zh",name);v.put("normalized_name_zh",norm);v.put("candidate_type",compiled.executable?"LIGHT_RUNTIME":"MODEL");v.put("novelty_class",novelty.name());v.put("tier",ResearchAssetContractR906.Tier.IDEA.name());v.put("method_signature",sig);v.put("canonical_spec",plan.canonical());v.put("representation_family",plan.representation);v.put("learner_family",plan.learner);v.put("update_rule",plan.updateRule);v.put("objective_family",plan.objective);v.put("calibration_family",plan.calibration);v.put("risk_family",plan.risk);v.put("falsifier_family",plan.falsifier);v.put("parent_asset_ids",plan.parentAssetIds);v.put("generation",plan.generation);v.put("genetic_operator",plan.geneticOperator);v.put("feature_schema_hash",ResearchFeatureGrammarR906.schemaHash());v.put("artifact_status",artifact.name());v.put("artifact_hash",compiled.artifactHash);v.put("trained_until_issue",trainedUntil);v.put("earliest_use_issue",earliestUse);v.put("runtime_executable",compiled.executable?1:0);v.put("may_authorize_real",0);v.put("last_transition_code",compiled.executable?"R906_LIGHT_COMPILED_PARITY_PASS":"WAITING_OFFLINE_TRAINER");v.put("created_at",now);v.put("updated_at",now);v.put("registry_hash",registryHash);db.insertOrThrow(DbHelper.TABLE_R906_RESEARCH_ASSET,null,v);
        return new Asset(id,name,sig,ResearchAssetContractR906.Tier.IDEA,artifact,compiled.artifactHash,registryHash);
    }
    private static String availableName(SQLiteDatabase db,String base,String signature){
        String proposed=base;
        // Naming is presentation only. Keep the method signature, novelty classification and
        // duplicate-method identity intact while resolving a different method's name collision.
        for(int ordinal=0;ordinal<Integer.MAX_VALUE;ordinal++){
            try(Cursor c=db.rawQuery("SELECT method_signature FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET
                    +" WHERE normalized_name_zh=?",new String[]{ResearchChineseNamingR906.normalized(proposed)})){
                if(!c.moveToFirst()||signature.equals(c.getString(0)))return proposed;
            }
            String suffix="·"+signature.substring(0,12)+(ordinal==0?"":"·"+ordinal);
            proposed=base.substring(0,Math.min(base.length(),40-suffix.length()))+suffix;
            ResearchAssetContractR906.requireChineseName(proposed);
        }
        throw new IllegalStateException("R906_ASSET_NAME_SPACE_EXHAUSTED");
    }
    public static void transition(SQLiteDatabase db,String assetId,ResearchAssetContractR906.Tier tier,String code){if(ResearchLifecycleR932.managed(db,"formula:"+assetId))return;ContentValues v=new ContentValues();v.put("tier",tier.name());v.put("last_transition_code",safe(code));v.put("updated_at",System.currentTimeMillis());db.update(DbHelper.TABLE_R906_RESEARCH_ASSET,v,"asset_id=?",new String[]{safe(assetId)});}
    private static String latestStrictOos(SQLiteDatabase db){Cursor c=null;try{c=db.rawQuery("SELECT MAX(target_issue) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null);return c.moveToFirst()?safe(c.getString(0)):"";}finally{if(c!=null)c.close();}}
    private static String nextIssue(String issue){if(!TargetIssueResolver.isValid(issue))return "";TargetIssueResolver.Resolution r=TargetIssueResolver.next(issue);return r.valid?r.targetIssue:"";}
    public static final class Asset{public final String assetId,displayNameZh,methodSignature,artifactHash,registryHash;public final ResearchAssetContractR906.Tier tier;public final ResearchAssetContractR906.ArtifactStatus artifactStatus;Asset(String id,String n,String s,ResearchAssetContractR906.Tier t,ResearchAssetContractR906.ArtifactStatus a,String ah,String rh){assetId=safe(id);displayNameZh=safe(n);methodSignature=safe(s);tier=t;artifactStatus=a;artifactHash=safe(ah);registryHash=safe(rh);}}
    private static String safe(String x){return x==null?"":x.trim();}
}
