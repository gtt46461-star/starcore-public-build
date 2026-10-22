package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;

/** E05 host acceptance: four cards stay separate and missing future evidence is never shown as 0%. */
public final class Batch34DiagnosticCardsHarnessR930 {
    private static void check(boolean value,String name){
        if(!value)throw new AssertionError("E05|"+name);
        System.out.println("ASSERT|E05|PASS|"+name);
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=1)throw new IllegalArgumentException("R930_E05_SQLITE_PATH_REQUIRED");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        DbStore store=new DbStore(db);
        try{
            Batch34SchemaR930.ensure(db);
            RuntimeResourceAuditR927.ensure(db);
            Batch34DiagnosticCardsR930.Cards cards=Batch34DiagnosticCardsR930.capture(store);
            String text=cards.render();
            check(text.contains("【E05-A.模型准确率卡】")
                    &&text.contains("【E05-B.策略效果卡】")
                    &&text.contains("【E05-C.期级链路卡】")
                    &&text.contains("【E05-D.资源效率卡】"),"FOUR_INDEPENDENT_CARDS_VISIBLE");
            check(cards.modelAccuracy.contains("RawRankingScoreIsProbability=false")
                    &&cards.modelAccuracy.contains("SelectedHitRate=N/A")
                    &&cards.modelAccuracy.contains("Brier=N/A")
                    &&cards.modelAccuracy.contains("ECE=N/A"),"NO_FUTURE_EVIDENCE_RENDERED_NA_NOT_ZERO");
            check(cards.strategyEffect.contains("TargetSelectedCoverage=15.00%")
                    &&cards.strategyEffect.contains("TargetSelectedHitRate=40.00%")
                    &&cards.strategyEffect.contains("QuotaEnforcementAllowed=false")
                    &&cards.strategyEffect.contains("FrequencyDebtAllowed=false")
                    &&cards.strategyEffect.contains("StrategyEffectStatus=UNPROVEN"),"EFFECT_TARGETS_SEPARATE_AND_NOT_FORCED");
            check(cards.issueChain.contains("DecisionOwner=DecisionGateEngine_ONLY")
                    &&cards.issueChain.contains("SettlementOwner=SettlementEngine_ONLY")
                    &&cards.issueChain.contains("ShadowRequiredWhenNotReal=true")
                    &&cards.issueChain.contains("SHADOW_REQUIRED"),"ISSUE_CHAIN_PRESERVES_UNIQUE_OWNERS_AND_SHADOW");
            check(cards.resourceEfficiency.contains("MetricScope=COMPUTE_MEMORY_DB_AND_RESEARCH_BUDGET_ONLY")
                    &&cards.resourceEfficiency.contains("ResourceMetricsDoNotProveAccuracy=true")
                    &&cards.resourceEfficiency.contains("FrequencyDebtAllowed=false"),"RESOURCE_CARD_CANNOT_IMPLY_ACCURACY");
            check(Batch34EvidenceContractR929.isSha256(cards.canonicalSha256)
                    &&text.contains("E05NoMetricConflation=true")
                    &&text.contains("E05MissingEvidenceRenderedAs=N/A_OR_UNPROVEN"),"CARD_SET_HASHED_AND_TRUTH_BOUND");
            System.out.println("ENHANCEMENT|E05|PASS");
            System.out.println("R930_E05_DIAGNOSTIC_PASS");
        }finally{db.shutdown();}
    }
}
