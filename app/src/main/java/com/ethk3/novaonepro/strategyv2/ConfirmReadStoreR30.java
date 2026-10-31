package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Durable H_CONFIRM reservations on the existing research writer. */
public final class ConfirmReadStoreR30 {
    private ConfirmReadStoreR30() {}

    /** Reattach all committed reservations after a process restart, including pre-R30 exposures of unknown dataset. */
    public static void restore(SQLiteDatabase db, HoldoutLedgerR27 ledger) {
        try (Cursor c = db.rawQuery("SELECT policy_hash,dataset_hash,block_id,range_from,range_to,read_at,control FROM r30_confirm_read", null)) {
            while (c.moveToNext()) ledger.restoreConfirm(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3), c.getInt(4), c.getLong(5), c.getInt(6) != 0);
        }
        try (Cursor c = db.rawQuery("SELECT e.policy_hash,e.block_id FROM r27_holdout_exposure e WHERE NOT EXISTS (SELECT 1 FROM r30_confirm_read r WHERE r.policy_hash=e.policy_hash AND r.block_id=e.block_id)", null)) {
            while (c.moveToNext()) ledger.restoreLegacyExposure(c.getString(0), c.getLong(1));
        }
    }

    /** Bind the current research worker. Reservation commits before confirm() returns and therefore before replay. */
    public static void bind(ResearchPoolR27.Worker worker, HoldoutLedgerR27 ledger) {
        ledger.setConfirmPersistence(read -> {
            try { RuntimeR27.submit(worker, "CONFIRM_READ", reservation(read)); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("R30_CONFIRM_RESERVATION_INTERRUPTED", e); }
        });
    }

    static ResearchWriteQueueR27.Batch reservation(HoldoutLedgerR27.ConfirmRead read) {
        ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch();
        batch.before = db -> {
            String[] exact = {read.policyHash, read.datasetHash, Long.toString(read.block)};
            try (Cursor c = db.rawQuery("SELECT 1 FROM r30_confirm_read WHERE policy_hash=? AND dataset_hash=? AND block_id=?", exact)) {
                if (c.moveToFirst()) throw reused(read);
            }
            String[] legacy = {read.policyHash, Long.toString(read.block)};
            try (Cursor c = db.rawQuery("SELECT 1 FROM r27_holdout_exposure e WHERE e.policy_hash=? AND e.block_id=? AND NOT EXISTS (SELECT 1 FROM r30_confirm_read r WHERE r.policy_hash=e.policy_hash AND r.block_id=e.block_id)", legacy)) {
                if (c.moveToFirst()) throw reused(read);
            }
            return 0;
        };
        batch.add("INSERT INTO r30_confirm_read(policy_hash,dataset_hash,block_id,range_from,range_to,read_at,control) VALUES(?,?,?,?,?,?,?)",
                read.policyHash, read.datasetHash, read.block, read.from, read.to, read.readAt, read.control ? 1 : 0);
        return batch;
    }

    private static IllegalStateException reused(HoldoutLedgerR27.ConfirmRead read) {
        return new IllegalStateException(HoldoutLedgerR27.CONFIRM_REUSED + ":" + read.policyHash + "@" + read.block);
    }
}
