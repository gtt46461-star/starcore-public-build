package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;
/** Bounded reachability GC. Reference capture and prepared-file leases share the CAS file lock. */
public final class ArtifactMaintenanceR26 {
    private ArtifactMaintenanceR26() {}
    public static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws IOException {
        budget.checkpointOrYield();CheckpointArtifactsR26 artifacts=CheckpointArtifactsR26.forStore(store);
        long removed,bytes;try(CheckpointPruneGuardR26 pin=CheckpointPruneGuardR26.tryAcquire(store.contextForR26().getFilesDir())){
            if(pin==null){claim.reason("REPORT_SNAPSHOT_INPUT_PIN");return 0;}
            removed=artifacts.prune(()->CheckpointReferencesR26.hashes(store.readable()),System.currentTimeMillis(),60000,64);bytes=artifacts.bytes();
        }
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_ARTIFACT_RETENTION",db->{RuntimeCoordinatorR26.requireFence(db,claim);db.execSQL("INSERT OR REPLACE INTO r26_storage_measurement VALUES('CHECKPOINT_FILES',?,268435456,?,?)",new Object[]{bytes,removed,System.currentTimeMillis()});
            db.execSQL("DELETE FROM r26_checkpoint_artifact WHERE checkpoint_hash IN (SELECT checkpoint_hash FROM r26_checkpoint_artifact WHERE checkpoint_hash NOT IN (SELECT checkpoint_hash FROM r26_live_model UNION SELECT parent_hash FROM r26_live_model UNION SELECT checkpoint_hash FROM r26_candidate UNION SELECT checkpoint_hash FROM r26_expert_experiment UNION SELECT parent_hash FROM r26_expert_experiment UNION SELECT checkpoint_hash FROM r26_legacy_runtime UNION SELECT parent_hash FROM r26_legacy_runtime UNION SELECT checkpoint_hash FROM r26_member_replay UNION SELECT checkpoint_hash FROM r26_prepared_policy) ORDER BY created_at DESC LIMIT 256 OFFSET 4096)");return 0;});return removed>0?1:0;
    }
}
