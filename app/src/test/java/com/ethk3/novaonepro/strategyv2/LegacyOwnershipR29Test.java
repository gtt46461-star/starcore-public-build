package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DrawDependencyInputCP12;
import org.junit.Test;
import static org.junit.Assert.*;

/** Protects authority boundaries across the expansion from R27-only to all existing research state. */
public final class LegacyOwnershipR29Test {
    @Test public void legacyResearchStateHasOneIndependentOwner(){
        for(String table:new String[]{"r26_task","r26_session","r26_legacy_runtime","r26_live_model",
            "r26_training_step","r26_candidate","su1_model_artifact","su1_cohort_runtime",
            "su2_model_genome","su2_certificate","research_historical_genome_archive_r967","r27_legacy_roster_alias"}){
            assertTrue(table,MigrationCopyR29.ownedResearch(table));assertFalse(table,MigrationCopyR29.realtimeOnly(table));
        }
    }
    @Test public void formalDrawFreezeSettlementAndRealOwnersStayRealtime(){
        for(String table:new String[]{"draws","r26_feedback","r26_frozen","r26_incumbent_frozen",
            "su1_source_clock","su1_mainline_binding","su2_live_decision","su2_mainline_binding",
            DbHelper.TABLE_DECISIONS,DbHelper.TABLE_TRADES,DbHelper.TABLE_R35_SHADOW_OBSERVATION,
            DbHelper.TABLE_R35_OOS_LABEL,DrawDependencyInputCP12.TABLE})
            assertFalse(table,MigrationCopyR29.ownedResearch(table));
    }
    @Test public void independentOwnerCannotTakeOverTransportOrPublicationAuthority(){
        for(String table:new String[]{"r29_event_outbox","r29_registration_authority","r29_database_route",
            "r29_scheduler_owner","r29_legacy_checkpoint_pin","r29_projection_applied","r29_legacy_live_projection"}){
            assertTrue(table,MigrationCopyR29.realtimeOnly(table));assertFalse(table,MigrationCopyR29.ownedResearch(table));
        }
    }
}
