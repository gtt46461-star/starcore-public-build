package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * Sole bounded production consumer for one certified GENESIS_NEW analysis asset.
 *
 * <p>The mixer evaluates only the Production top-64 for mobile cost, preserves all 1200 actions,
 * limits residual influence to eight percent, and still has no authorization or trade-writing
 * method. Certificate revocation or any runtime/identity mismatch returns the untouched mainline.</p>
 */
public final class CertifiedResearchMixerR915 {
    public static final String VERSION = "R915_CERTIFIED_ASSET_MIXER_V4_1";
    public static final String TABLE_CONTRIBUTION = "research_new_contribution_v41";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    public static final int TOP_EVALUATION = 64;
    public static final double MAX_INFLUENCE = 0.30d;

    public static final class Result {
        public final boolean applied;
        public final String frozenRecipe;
        public final V51Models.PositionAction[] actions;
        public final V51Models.PositionAction rankOne;
        public final String status, assetId, nameZh, evidenceHash, contributionHash;
        public final int futureN, pairedN;
        public final double influenceWeight, futureRate, wilson, brier, ece, coverage;
        public final double pairedDelta, fdrQ;

        Result(
                boolean applied,
                V51Models.PositionAction[] actions,
                V51Models.PositionAction rankOne,
                String status,
                Candidate candidate,
                double weight,
                String contributionHash) {
            this(applied,actions,rankOne,status,candidate,weight,contributionHash,"");
        }
        Result(boolean applied,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String status,Candidate candidate,double weight,String contributionHash,String frozenRecipe) {
            this.frozenRecipe=frozenRecipe;
            this.applied = applied;
            this.actions = actions;
            this.rankOne = rankOne;
            this.status = safe(status);
            assetId = candidate == null ? "" : candidate.assetId;
            nameZh = candidate == null ? "" : candidate.nameZh;
            evidenceHash = candidate == null ? "" : candidate.evidenceHash;
            influenceWeight = weight;
            futureN = candidate == null ? 0 : candidate.n;
            futureRate = candidate == null ? 0.0d : candidate.rate;
            wilson = candidate == null ? 0.0d : candidate.wilson;
            brier = candidate == null ? 1.0d : candidate.brier;
            ece = candidate == null ? 1.0d : candidate.ece;
            coverage = candidate == null ? 0.0d : candidate.coverage;
            pairedN = candidate == null ? 0 : candidate.paired;
            pairedDelta = candidate == null ? 0.0d : candidate.delta;
            fdrQ = candidate == null ? 1.0d : candidate.fdr;
            this.contributionHash = safe(contributionHash);
        }

        public String proof() {
            return status + "|asset=" + assetId + "|name=" + nameZh + "|weight="
                    + influenceWeight + "|evidence=" + evidenceHash + "|contribution="
                    + contributionHash + "|futureN=" + futureN + "|rate=" + futureRate
                    + "|wilson=" + wilson + "|brier=" + brier + "|ece=" + ece
                    + "|coverage=" + coverage + "|pairedN=" + pairedN
                    + "|pairedDelta=" + pairedDelta + "|FWER_adjusted_p=" + fdrQ + "|directREAL=0";
        }
    }

    private CertifiedResearchMixerR915() {}

    public static void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_CONTRIBUTION + "("
                + "target_issue TEXT NOT NULL,asset_id TEXT NOT NULL,asset_version_hash TEXT NOT NULL,"
                + "certificate_id TEXT NOT NULL,display_name_zh TEXT NOT NULL,production_rank1_hash TEXT NOT NULL,"
                + "mixed_rank1_hash TEXT NOT NULL,influence_weight REAL NOT NULL,top_evaluated INTEGER NOT NULL,"
                + "evidence_hash TEXT NOT NULL,status TEXT NOT NULL,record_hash TEXT NOT NULL UNIQUE,"
                + "created_ms INTEGER NOT NULL,PRIMARY KEY(target_issue,asset_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r915_contribution_issue ON " + TABLE_CONTRIBUTION
                + "(target_issue,status,created_ms)");
    }

    public static Result mix(
            DbStore store,
            String baseIssue,
            String targetIssue,
            V51RuntimeResult runtime,
            V51Models.PositionAction[] productionActions) {
        if (store == null || runtime == null || !runtime.success
                || !TargetIssueResolver.exact(baseIssue,targetIssue).valid
                || productionActions == null
                || productionActions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return fallback(store,productionActions,"INPUT_INVALID",null);
        if (MAY_AUTHORIZE_REAL || AssetGenesisEngineR915.MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R915_CERTIFIED_ASSET_AUTHORITY_VIOLATION");
        Candidate candidate = loadBest(store,targetIssue);
        if (candidate == null) return fallback(store,productionActions,"NO_CERTIFIED_NEW_ASSET",null);
        HistoricalStrategyInventionR908.Profile profile = HistoricalStrategyInventionR908.buildProfileAt(store,baseIssue);
        if (profile == null || !profile.ready) return fallback(store,productionActions,"PIT_PROFILE_NOT_READY",candidate);
        try {
            PromotionWindowR925.Window window=PromotionWindowR925.authorize(store,CertificateBundleServiceR929.Consumer.GENESIS_ASSET,candidate.assetId,candidate.structuralHash,targetIssue);
            if(window.weight<=0)return fallback(store,productionActions,window.summary(),candidate);
            GenesisAssetGenomeR915 identity=GenesisAssetGenomeR915.parse(candidate.genomeSpec);
            if(!identity.assetGrade()||!identity.assetId.equals(candidate.assetId)||!identity.structuralHash.equals(candidate.structuralHash)||!identity.behaviorHash.equals(candidate.behaviorHash)||!identity.mechanismNameZh.equals(candidate.nameZh))return fallback(store,productionActions,"ASSET_IDENTITY_MISMATCH",candidate);
            if(GenesisMoleculeTraceR932.blockedByQuarantine(identity,true,AtomCaptureR932.quarantined(store)))
                return fallback(store,productionActions,"ASSET_PARENT_ATOM_QUARANTINED_ZERO_CONTRIBUTION",candidate);
            double weight=window.weight;
            FrozenConsumerRecipeR932.Recipe recipe=FrozenConsumerRecipeR932.freeze("GENESIS_NEW_CERTIFIED",candidate.assetId,candidate.genomeSpec,
                    GenesisAssetVmR915.State.decode(candidate.stateBlob).encode(),candidate.structuralHash,candidate.evidenceHash,candidate.certificateId,weight,baseIssue,targetIssue,profile,runtime);
            FrozenConsumerRecipeR932.Mixed mixed=FrozenConsumerRecipeR932.execute(recipe,productionActions);
            V51Models.PositionAction[] integrated=mixed.actions;V51Models.PositionAction rankOne=mixed.rankOne,productionRankOne=null;
            for(V51Models.PositionAction action:productionActions)if(action.rank==1)productionRankOne=action;
            int evaluated=TOP_EVALUATION;
            String contribution=V51ChecksumEngine.sha256("R915_CONTRIBUTION|"+targetIssue+'|'
                    +candidate.assetId+'|'+productionRankOne.checksum+'|'+rankOne.checksum+'|'+weight+'|'+evaluated);
            Result result=new Result(true,integrated,rankOne,"CERTIFIED_NEW_ASSET_INFLUENCE",
                    candidate,weight,contribution,recipe.encoded);
            persistContribution(store,targetIssue,candidate,productionRankOne,rankOne,weight,evaluated,result);
            persistTruth(store,result);return result;
        } catch(Throwable failure) {
            return fallback(store,productionActions,"ASSET_RUNTIME_FAIL_"+failure.getClass().getSimpleName(),candidate);
        }
    }

    public static int revoke(DbStore store,String assetId,String reason) {
        if(store==null||safe(assetId).length()==0)return 0;
        final String id=safe(assetId),code=safe(reason).length()==0?"MANUAL_ROLLBACK":safe(reason);
        Integer changed=store.coordinatorR36().runWrite("R915_CERTIFIED_ASSET_REVOKE",
                new DatabaseCoordinator.WriteCallable<Integer>(){@Override public Integer call(SQLiteDatabase db){
                    ContentValues c=new ContentValues();c.put("status","REVOKED");c.put("revocation_code",code);
                    c.put("updated_ms",System.currentTimeMillis());int rows=db.update(
                            AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE,c,"asset_id=? AND status='ACTIVE'",new String[]{id});
                    ContentValues g=new ContentValues();g.put("status","REVOKED_CERTIFICATE");
                    g.put("lifecycle_reason",code);g.put("updated_ms",System.currentTimeMillis());
                    db.update(AssetGenesisEngineR915.TABLE_ASSET_GENOME,g,"asset_id=?",new String[]{id});return Integer.valueOf(rows);}},Integer.valueOf(0));
        return changed==null?0:changed.intValue();
    }

    private static Candidate loadBest(DbStore store,String target){
        UnifiedResearchChoiceR932.Evidence e=UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.GENESIS_ASSET);
        if(e==null)return null;
        try(Cursor c=store.readable().rawQuery("SELECT asset_id,display_name_zh,genome_spec,state_blob,structural_hash,behavior_hash,trained_until_issue,earliest_use_issue,future_block_min,future_regime_min,future_max_miss FROM "+AssetGenesisEngineR915.TABLE_ASSET_GENOME+" WHERE asset_id=? AND structural_hash=? AND may_authorize_real=0",new String[]{e.id,e.structure})){
            if(!c.moveToFirst())return null;
            Candidate candidate=new Candidate(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),e.evidence,c.getString(6),c.getString(7),
                e.count("FUTURE_N"),e.metric("HIT_RATE"),e.metric("WILSON_LOWER"),e.count("PAIRED_N"),e.metric("PAIRED_DELTA"),e.metric("ECE"),e.metric("BRIER"),e.metric("COVERAGE"),c.getDouble(8),c.getDouble(9),c.getInt(10),e.metric("FWER_ADJUSTED_P"),e.certificate);
            return candidate.valid(target)?candidate:null;
        }catch(RuntimeException error){if(error instanceof ResearchWorkBudgetR927.Yield)throw error;return null;}
    }

    private static void persistContribution(DbStore store,final String target,final Candidate c,
            final V51Models.PositionAction before,final V51Models.PositionAction after,final double weight,
            final int evaluated,final Result result){store.coordinatorR36().runWrite("R915_ASSET_CONTRIBUTION",
        new DatabaseCoordinator.WriteCallable<Boolean>(){@Override public Boolean call(SQLiteDatabase db){
            ContentValues v=new ContentValues();v.put("target_issue",target);v.put("asset_id",c.assetId);
            v.put("asset_version_hash",c.structuralHash);v.put("certificate_id",c.certificateId);
            v.put("display_name_zh",c.nameZh);v.put("production_rank1_hash",before.checksum);
            v.put("mixed_rank1_hash",after.checksum);v.put("influence_weight",weight);v.put("top_evaluated",evaluated);
            v.put("evidence_hash",c.evidenceHash);v.put("status",result.status);v.put("record_hash",result.contributionHash);
            v.put("created_ms",System.currentTimeMillis());return Boolean.valueOf(db.insertWithOnConflict(
                    TABLE_CONTRIBUTION,null,v,SQLiteDatabase.CONFLICT_IGNORE)>=0);}},Boolean.FALSE);}

    private static Result fallback(DbStore store,V51Models.PositionAction[] actions,String status,Candidate c){
        V51Models.PositionAction rankOne=null;if(actions!=null)for(V51Models.PositionAction a:actions)
            if(a!=null&&a.rank==1){rankOne=a;break;}Result r=new Result(false,actions,rankOne,status,c,0,"");
        if(store!=null)persistTruth(store,r);return r;}
    private static void persistTruth(DbStore store,Result r){try{LinkedHashMap<String,String>s=new LinkedHashMap<String,String>();
        s.put("r915_asset_mixer_status",r.status);s.put("r915_asset_mixer_id",r.assetId);
        s.put("r915_asset_mixer_name_zh",r.nameZh);s.put("r915_asset_mixer_proof",r.proof());
        s.put("r915_asset_mixer_at",String.valueOf(System.currentTimeMillis()));store.setStates(s);}catch(Throwable ignored){}}
    private static double clamp01(double v){return clamp(v,0,1);}private static double clamp(double v,double lo,double hi){
        if(Double.isNaN(v)||Double.isInfinite(v))v=lo;return Math.max(lo,Math.min(hi,v));}
    private static String safe(String v){return v==null?"":v.trim();}

    private static final class Candidate{
        final String assetId,nameZh,genomeSpec,stateBlob,structuralHash,behaviorHash,evidenceHash,
                trained,earliest,certificateId;final int n,paired,maxMiss;
        final double rate,wilson,delta,ece,brier,coverage,block,regime,fdr;
        Candidate(String id,String name,String spec,String state,String structural,String behavior,String evidence,
                String trained,String earliest,int n,double rate,double wilson,int paired,double delta,double ece,
                double brier,double coverage,double block,double regime,int maxMiss,double fdr,String certificate){assetId=safe(id);nameZh=safe(name);
            genomeSpec=safe(spec);stateBlob=safe(state);structuralHash=safe(structural);behaviorHash=safe(behavior);
            evidenceHash=safe(evidence);this.trained=safe(trained);this.earliest=safe(earliest);this.n=n;this.rate=rate;this.wilson=wilson;
            this.paired=paired;this.delta=delta;this.ece=ece;this.brier=brier;this.block=block;this.regime=regime;
            this.coverage=coverage;this.maxMiss=maxMiss;this.fdr=fdr;certificateId=safe(certificate);}
        boolean valid(String target){return assetId.length()>0&&nameZh.endsWith("代")&&structuralHash.length()==64
                &&behaviorHash.length()==64&&evidenceHash.length()==64&&certificateId.length()>0
                &&TargetIssueResolver.isValid(trained)&&TargetIssueResolver.compare(trained,target)<0
                &&(earliest.length()==0||(TargetIssueResolver.isValid(earliest)&&TargetIssueResolver.compare(earliest,target)<=0))
;}}
}
