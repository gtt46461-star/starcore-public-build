package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

/** Real retained historical input and actual owner transactions for the cold Creator replay.
 * The NO_BET observation is an explicit replay fixture. It grants no model qualification,
 * natural Future evidence or REAL authority. No settlement/candidate row is inserted by SQL.
 */
public final class HistoricalCreatorInputSetup {
    private static long count(SQLiteDatabase db, String table) {
        try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + table, null)) {
            c.moveToFirst(); return c.getLong(0);
        }
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db = new SQLiteDatabase(args[0], args[1]);
        try {
            DbStore store = HostProductR974.store(db);
            Method parse = HistoricalDrawImportEngine.class.getDeclaredMethod("parse", String.class);
            parse.setAccessible(true); // Reuse only the exact product CSV parser, no owner token.
            ArrayList<Models.Draw> original = new ArrayList<>();
            int parsed = 0;
            try (BufferedReader in = new BufferedReader(new InputStreamReader(
                    new GZIPInputStream(Files.newInputStream(Path.of(args[2]))), StandardCharsets.UTF_8))) {
                in.readLine();
                for (String line; (line = in.readLine()) != null;) {
                    Models.Draw draw = (Models.Draw) parse.invoke(null, line);
                    if (draw == null) throw new AssertionError("INVALID_RETAINED_HISTORY_ROW");
                    original.add(draw); parsed++;
                }
            }
            if (parsed != 10000 || original.size() != 10000)
                throw new AssertionError("ORIGINAL_HISTORY_SCALE");
            int begin=5000,end=10000;
            if (args.length>3 && args[3].equals("CONTIGUOUS_REAL_INPUT")) {
                int start=0,bestStart=0,bestEnd=0;
                for(int i=1;i<=original.size();i++) {
                    if(i==original.size() || !TargetIssueResolver.exact(original.get(i-1).issue,original.get(i).issue).valid) {
                        if(i-start>bestEnd-bestStart){bestStart=start;bestEnd=i;}
                        start=i;
                    }
                }
                begin=bestStart;end=bestEnd;
                if(end-begin<500+1200+121)throw new AssertionError("UNCHANGED_FULL_TRAIN_SCALE_UNAVAILABLE");
                System.out.println("REAL_CONTIGUOUS_SOURCE_SELECTION|begin="+begin+"|end="+end
                        +"|rows="+(end-begin)+"|train_budget=1200|validation=120|embargo=1|source_rows=10000");
            }
            int expected=end-begin,completed = 0;
            int originalGaps = 0;
            for (int ordinal=begin; ordinal<end; ordinal++) {
            Models.Draw last = original.get(ordinal);
            String base = original.get(ordinal-1).issue;
            if (!TargetIssueResolver.exact(base, last.issue).valid) {
                // A HOLD may be frozen at a calendar boundary with missing source periods.
                // Preserve the original gap: this is an issue identity, never a fabricated Draw.
                TargetIssueResolver.ParsedIssue target = TargetIssueResolver.parse(last.issue);
                java.time.LocalDate day = java.time.LocalDate.parse(last.issue.substring(0,8),
                        java.time.format.DateTimeFormatter.BASIC_ISO_DATE);
                base = target.sequence>1 ? last.issue.substring(0,8)+String.format(java.util.Locale.ROOT,"%04d",target.sequence-1)
                        : day.minusDays(1).format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE)+"1440";
                if (!TargetIssueResolver.exact(base,last.issue).valid) throw new AssertionError("CALENDAR_BOUNDARY");
                originalGaps++;
                System.out.println("ORIGINAL_SOURCE_GAP_PRESERVED|source_max="+original.get(ordinal-1).issue
                        +"|calendar_base="+base+"|target="+last.issue+"|no_draw_filled=true|fixture=NO_BET_HOLD");
            }
            // Replay the actual source's preceding 5000 rows through the product's
            // explicit frozen-history API. The target is excluded. Import only the
            // final 5000 targets, preserving the production live-history cap.
            ArrayList<Models.Draw> past = new ArrayList<>(original.subList(Math.max(0,ordinal-5000),ordinal));
            StringBuilder identity = new StringBuilder("R958_PIT_V2|").append(last.issue);
            for (int k=past.size()-1;k>=0;k--)
                identity.append('|').append(past.get(k).issue).append(':').append(past.get(k).numbersText());
            com.ethk3.novaonepro.r952.PITSnapshotContractR952 frozen =
                    com.ethk3.novaonepro.r952.PITSnapshotContractR952.fromFrozenHistory(
                    last.issue,past,V51ChecksumEngine.sha256(identity.toString()));
            String h = frozen.snapshotHash;
            // Simulated observation time precedes the original historical first-seen time.
            // Current wall time would correctly fail the strict historical replay PIT fence.
            long at = last.openTime - 1;
            CanonicalObservationWriterR36.CanonicalRecord observation =
                    new CanonicalObservationWriterR36.CanonicalRecord(base, last.issue,
                    1, 1, 2, 3, .3, 0, "SHADOW", "HOLD", "DOMAIN_NOT_PROVEN",
                    h, h, "", V51ChecksumEngine.sha256(h + "|record"), "B01_HISTORICAL_REPLAY",
                    h, h, h, at, at, at, at+55000);
            DatabaseCoordinator.TransactionResult<CanonicalObservationWriterR36.WriteResult> written =
                    store.coordinatorR36().runCanonicalTransactionR36(last.issue,
                    observation.attemptId, (txDb, tx) ->
                    CanonicalObservationWriterR36.insertWithinTransaction(txDb, tx, observation), null);
            if (written.value == null || !written.value.success)
                throw new AssertionError("ACTUAL_CANONICAL_OWNER_WRITE:" + written.code);
            if (store.saveDraw(last) < 0) throw new AssertionError("ACTUAL_DRAW_OWNER_SAVE");
            SettlementEngine.SettlementReport result = SettlementCoordinatorR815.settleNow(
                    store, last, SettlementCoordinatorR815.Reason.LEGACY_API);
            System.out.println("ACTUAL_SETTLEMENT_REPORT=" + result.detail);
            if (result.ok != 1 || result.shadowSettledCount != 1 || result.settledCount != 0)
                throw new AssertionError("ACTUAL_SETTLEMENT_OWNER_PREREQUISITE:" + result.detail);
            SettlementEngine.SettlementReport replay = SettlementCoordinatorR815.settleNow(
                    store, last, SettlementCoordinatorR815.Reason.LEGACY_API);
            if (replay.ok != 1 || replay.shadowSettledCount != 0 || replay.settledCount != 0)
                throw new AssertionError("SETTLEMENT_REPLAY_UNIQUE");
            completed++;
            if (completed % 100 == 0) System.out.println("ACTUAL_HISTORICAL_OWNER_PROGRESS="+completed+"/"+expected);
            }
            if (count(db, "draws") != expected || count(db, "r952_candidate") != 0
                    || count(db, DbHelper.TABLE_TRADES) != 0)
                throw new AssertionError("HISTORY_OR_AUTHORITY_BOUNDARY");
            try (Cursor c = db.rawQuery("SELECT COUNT(*) FROM draws d WHERE d.dirty=0 AND NOT EXISTS(SELECT 1 FROM "
                    + DbHelper.TABLE_R36_SETTLEMENT_TASK + " t WHERE t.target_issue=d.issue AND t.status='COMPLETED')",null)) {
                if (!c.moveToFirst() || c.getLong(0)!=0 || completed!=expected)
                    throw new AssertionError("FULL_SELECTED_HISTORY_AUTHORITATIVE_SETTLEMENT");
            }
            try (Cursor c = db.rawQuery("PRAGMA foreign_key_check", null)) {
                if (c.moveToFirst()) throw new AssertionError("HISTORY_FOREIGN_KEYS");
            }
            System.out.println("PASS|COLD_CREATOR_HISTORICAL_PREREQUISITES|original_rows=10000"
                    + "|retained_rows="+expected+"|settlement_owner=SettlementEngine|completed_targets="+completed
                    + "|candidate_rows=0|real_bets=0|source=original_history_seed.csv.gz"
                    + "|original_source_gaps_preserved="+originalGaps
                    + "|scope=HISTORICAL_HOST_REPLAY_NOT_NATURAL_FUTURE");
        } finally { db.shutdown(); }
    }
}
