package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Models;

/** Post-COMPLETED sidecar only; it never invokes SettlementEngine. */
public final class ResearchSettlementHookRegistryR952 {
    private ResearchSettlementHookRegistryR952() {}
    public static int dispatchCompleted(DbStore store, Models.Draw draw, String settlementHash, long completedAt) {
        if (store == null || draw == null || !draw.isValid()) return 0;
        try {
            return ResearchDirectorR952.onSettlementCompleted(store, draw, settlementHash, completedAt);
        } catch (RuntimeException failure) {
            // The production terminal already committed. Continuity replays this exact terminal.
            store.setState("r958_settlement_hook_error",draw.issue+"|"+failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage()));
            return 0;
        }
    }
}
