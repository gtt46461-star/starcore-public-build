package com.ethk3.novaonepro;

import android.database.Cursor;

/**
 * Read-only unified-certificate stage projection for lifecycle/UI truth.
 *
 * <p>L4/L5 intentionally carry zero mainline influence and therefore must not be inferred through
 * authorizeCurrent(), which correctly rejects SHADOW_ONLY_LEVEL. This reader applies the same
 * revocation, validity, dependency and freeze-before-request checks but never grants weight and
 * cannot authorize REAL.</p>
 */
public final class CertificateStageProjectionR957 {
    public static final boolean MAY_AUTHORIZE_REAL=false;
    private CertificateStageProjectionR957(){}

    public static Stage current(DbStore store, CertificateBundleServiceR929.Consumer consumer,
            String id, String structural, String target) {
        if (MAY_AUTHORIZE_REAL) throw new IllegalStateException("R957_REAL_BOUNDARY");
        if(store==null||consumer==null||!hash(clean(structural))||clean(id).length()==0
                ||!TargetIssueResolver.isValid(clean(target))) return Stage.invalid("STAGE_INPUT_INVALID");
        Cursor c=null;
        try {
            c=store.readable().rawQuery("SELECT c.level,c.state,c.bundle_hash,c.certificate_hash,"
                    +"c.reason,c.valid_from_issue,c.valid_until_issue,c.issued_at,c.schema_hash,"
                    +"c.selection_hash,c.code_hash,c.domain_hash,c.atom_graph_hash,c.model_hash,"
                    +"c.policy_hash,c.calibration_hash,c.trial_hash,c.family_id,b.state,b.signature_verified,c.all_gates_pass "
                    +"FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c JOIN "+ResearchPlatformStoreR929.TABLE_BUNDLE
                    +" b ON b.bundle_hash=c.bundle_hash WHERE c.consumer=? AND c.candidate_id=? "
                    +"AND c.structural_hash=? AND c.contract_revision='R932_REQUIRED_GATES_V1' "
                    +"AND NOT EXISTS(SELECT 1 FROM "+ResearchPlatformStoreR929.TABLE_REVOCATION
                    +" rv WHERE rv.certificate_hash=c.certificate_hash) ORDER BY c.issued_at DESC LIMIT 2",
                    new String[]{consumer.name(),clean(id),clean(structural)});
            if(!c.moveToFirst())return Stage.invalid("NO_UNIFIED_CERTIFICATE");
            String level=clean(c.getString(0)),state=clean(c.getString(1)),bundle=clean(c.getString(2));
            String certificate=clean(c.getString(3)),reason=clean(c.getString(4));
            String validFrom=clean(c.getString(5)),validUntil=clean(c.getString(6));long issuedAt=c.getLong(7);
            String schema=clean(c.getString(8)),selection=clean(c.getString(9)),code=clean(c.getString(10)),domain=clean(c.getString(11));
            String atom=clean(c.getString(12)),model=clean(c.getString(13)),policy=clean(c.getString(14)),calibration=clean(c.getString(15)),trial=clean(c.getString(16)),family=clean(c.getString(17));
            String bundleState=clean(c.getString(18));boolean signature=c.getInt(19)==1,gates=c.getInt(20)==1;
            if(c.moveToNext())return Stage.invalid("CERTIFICATE_CARDINALITY_GT1");
            if(!"ACTIVE".equals(state)||!"ACTIVE".equals(bundleState)||!signature)return Stage.invalid("CERTIFICATE_OR_BUNDLE_NOT_ACTIVE");
            if(!gates)return Stage.invalid("CERTIFICATE_GATE_FAILED");
            if(!target.equals(ModelBundleRuntimeR932.currentTarget())||issuedAt>=ModelBundleRuntimeR932.capturedAt())return Stage.invalid("CERTIFICATE_NOT_FROZEN_BEFORE_REQUEST");
            if(PitFeatureDagR929.compareIssues(target,validFrom)<0||(validUntil.length()>0&&PitFeatureDagR929.compareIssues(target,validUntil)>0))return Stage.invalid("CERTIFICATE_OUTSIDE_ISSUE_RANGE");
            if(!ModelBundleRuntimeR932.current().hash.equals(bundle)||!V51ActionMetaFeatureEngine.featureManifestHash().equals(schema)
                    ||!CertificateDependenciesR932.selectionHash().equals(selection)||!SourceManifestR815.SOURCE_TREE_SHA256.equals(code)
                    ||!DomainRouterR929.installedSchemaHash().equals(domain))return Stage.invalid("CERTIFICATE_DEPENDENCY_DRIFT");
            String dependency=CertificateDependenciesR932.check(store,atom,model,policy,calibration,trial,structural,family,issuedAt);
            if(dependency.length()>0)return Stage.invalid(dependency);
            return new Stage(true,level,certificate,bundle,reason);
        } catch(RuntimeException failure) {
            if(failure instanceof ResearchWorkBudgetR927.Yield)throw failure;
            return Stage.invalid("CERTIFICATE_STAGE_READ_ERROR_"+failure.getClass().getSimpleName());
        } finally {if(c!=null)c.close();}
    }

    private static boolean hash(String value){return value!=null&&value.matches("[0-9a-fA-F]{64}");}
    private static String clean(String value){return value==null?"":value.trim();}
    public static final class Stage {
        public final boolean valid;public final String level,certificateHash,bundleHash,reason;
        Stage(boolean valid,String level,String certificateHash,String bundleHash,String reason){
            this.valid=valid;this.level=clean(level);this.certificateHash=clean(certificateHash);this.bundleHash=clean(bundleHash);this.reason=clean(reason);
        }
        static Stage invalid(String reason){return new Stage(false,"","","",reason);}
        public boolean is(String expected){return valid&&clean(expected).equals(level);}
    }
}

