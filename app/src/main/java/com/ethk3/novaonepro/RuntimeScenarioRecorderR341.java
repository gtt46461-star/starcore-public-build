package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Read-only engineering export of the request funnel and its complete, declared target range. */
public final class RuntimeScenarioRecorderR341 {
    private static final int MAX_TARGETS = 120;
    private static final String SCOPE_JOIN = " FROM " + RuntimeEventLedgerR926.TABLE_EVENT
            + " e JOIN " + RuntimeObservationScopeR341.EVENT_SCOPE + " s ON s.event_id=e.event_id "
            + " WHERE s.release_id=? AND s.source_commit=? AND s.database_identity=? AND s.sample_wall_at<=?"
            + " AND length(e.target_issue)=12 AND e.target_issue NOT GLOB '*[^0-9]*'";
    private RuntimeScenarioRecorderR341() {}

    static Map<String, Object> captureInputEnvelope(AnalysisSingleFlightR36.Request request) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("baseIssue", request.baseIssue);
        value.put("targetIssue", request.targetIssue);
        value.put("requestHash", request.requestHash);
        value.put("generation", request.generation);
        value.put("requestAt", request.requestAt);
        value.put("deadlineAt", request.deadlineAt);
        return value;
    }

    /** Only preserve a registered uppercase classification and numerical target, never exception text. */
    static String redact(String reason) {
        if (reason == null) return "UNSPECIFIED";
        String token = reason.split("[|: ]", 2)[0];
        return token.matches("[A-Z0-9_]{1,96}") ? token : "UNCLASSIFIED_DETAIL_RETAINED_IN_ORIGINAL";
    }

    static Map<String, Object> exportFixture(SQLiteDatabase snapshot, RuntimeObservationScopeR341 scope) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("schema", "R341_RUNTIME_OPPORTUNITY_COHORT_1");
        value.put("scope", scope.exportEnvelope());
        value.put("datasetRole", "DIAGNOSTIC_ONLY");
        if (!snapshot.isReadOnly()) {
            value.put("state", "IMMUTABLE_READ_ONLY_SNAPSHOT_REQUIRED");
            return value;
        }
        if (scope.scopeKind != RuntimeObservationScopeR341.Kind.SQL_SNAPSHOT
                || RuntimeObservationScopeR341.UNRESOLVED.equals(scope.databaseIdentity)
                || RuntimeObservationScopeR341.UNRESOLVED.equals(scope.sourceCommit)) {
            value.put("state", "SNAPSHOT_IDENTITY_UNAVAILABLE");
            return value;
        }
        try {
            if (!scope.databaseIdentity.equals(RuntimeObservationScopeR341.databaseIdentity(snapshot))) {
                value.put("state", "DATABASE_CHANGED");
                return value;
            }
            collect(snapshot, scope, value);
        } catch (SQLiteException unavailable) {
            value.put("state", "RESOURCE_WAIT_OR_SCHEMA_UNAVAILABLE");
            value.put("errorType", unavailable.getClass().getSimpleName());
        }
        return value;
    }

    private static void collect(SQLiteDatabase db, RuntimeObservationScopeR341 scope, Map<String, Object> out) {
        String[] scopeArgs = {scope.releaseId, scope.sourceCommit, scope.databaseIdentity,
                Long.toString(scope.sampleWallAt)};
        String first;
        String last;
        try (Cursor bounds = db.rawQuery("SELECT min(e.target_issue),max(e.target_issue)" + SCOPE_JOIN, scopeArgs)) {
            bounds.moveToFirst();
            first = bounds.getString(0);
            last = bounds.getString(1);
        }

        if (!TargetIssueResolver.isValid(first) || !TargetIssueResolver.isValid(last)) {
            out.put("state", "NO_SCOPED_EVENTS");
            return;
        }

        Map<String, Period> periods = new LinkedHashMap<>();
        String target = last;
        for (int count = 0; count < MAX_TARGETS && target != null && target.compareTo(first) >= 0; count++) {
            periods.put(target, new Period(target));
            String previous = TargetIssueResolver.previous(target);
            if (previous == null || previous.compareTo(target) >= 0) break;
            target = previous;
        }
        String earliest = null;
        for (String issue : periods.keySet()) earliest = issue;
        String[] range = {earliest, last};
        try (Cursor draws = db.rawQuery("SELECT issue,open_time FROM " + DbHelper.TABLE_DRAWS
                + " WHERE issue>=? AND issue<=? AND dirty=0 AND open_time>0", range)) {
            while (draws.moveToNext()) {
                Period period = periods.get(draws.getString(0));
                if (period != null) period.drawAt = draws.getLong(1);
            }
        }
        String[] requestedRange = {scopeArgs[0], scopeArgs[1], scopeArgs[2], scopeArgs[3], earliest, last};
        try (Cursor events = db.rawQuery("SELECT e.target_issue,e.event_id,e.event_type,e.stage,"
                        + "s.sample_wall_at,s.sample_mono_at,s.process_epoch,s.boot_id,e.epoch_id" + SCOPE_JOIN
                        + " AND e.target_issue>=? AND e.target_issue<=? AND e.event_type IN "
                        + "('REQUESTED','HISTORY_BOUND','REJECTED','CANONICAL','STARTED','EXPIRED','SETTLED','LEARNED','RECOVERY') ORDER BY e.seq",
                requestedRange)) {
            while (events.moveToNext()) {
                Period period = periods.get(events.getString(0));
                if (period != null) period.add(events);
            }
        }

        int requested = 0, drawnEligible = 0, future = 0, canonical = 0, drawnWithCanonical = 0;
        List<Object> rows = new ArrayList<>();
        for (Period period : periods.values()) {
            if (period.requested) requested++;
            if (period.earliestCanonical > 0) canonical++;
            if (period.drawAt > 0) {
                drawnEligible++;
                if (period.earliestCanonical > 0 && period.earliestCanonical < period.drawAt) drawnWithCanonical++;
            } else if (period.target.equals(last)) {
                future++;
            }
            rows.add(period.export(last));
        }
        out.put("state", "OBSERVED_DECLARED_BOUNDED_RANGE");
        out.put("bounds", "FIRST_AND_LAST_SCOPED_EVENT; MISSING_TARGETS_INSIDE_INCLUDED");
        out.put("firstRetainedTarget", earliest);
        out.put("lastRetainedTarget", last);
        out.put("retainedHistoryScope", "SEPARATE_FROM_CURRENT_RELEASE");
        out.put("unknownOrigin", RuntimeObservationScopeR341.LEGACY_UNSCOPED);
        out.put("truncatedOlderTargets", !first.equals(earliest));
        out.put("requestedTargets", requested);
        out.put("drawnEligibleTargets", drawnEligible);
        out.put("futureOrUnobservedLastTarget", future);
        out.put("canonicalSuccesses", canonical);
        out.put("drawnWithPredrawCanonical", drawnWithCanonical);
        out.put("rows", rows);
        appendProgress(db, scope, scopeArgs, out);
    }

    private static void appendProgress(SQLiteDatabase db, RuntimeObservationScopeR341 scope,
            String[] scopeArgs, Map<String, Object> out) {
        Map<String, Long> releaseCounts = new LinkedHashMap<>();
        Map<String, Long> epochCounts = new LinkedHashMap<>();
        long lastCanonicalSequence = 0L;
        try (Cursor counts = db.rawQuery("SELECT e.event_type,e.stage,count(*),"
                + "sum(CASE WHEN s.process_epoch=? AND s.boot_id=? THEN 1 ELSE 0 END),max(e.seq)" + SCOPE_JOIN
                + " AND e.event_type IN ('CANONICAL','SETTLED','LEARNED','RECOVERY') GROUP BY e.event_type,e.stage",
                new String[]{scope.processEpoch, scope.bootId, scopeArgs[0], scopeArgs[1], scopeArgs[2], scopeArgs[3]})) {
            while (counts.moveToNext()) {
                String type = counts.getString(0), stage = counts.getString(1);
                String key;
                switch (type) {
                    case "CANONICAL":
                        key = "canonicalCommits";
                        lastCanonicalSequence = Math.max(lastCanonicalSequence, counts.getLong(4));
                        break;
                    case "SETTLED": key = "settlementEvents"; break;
                    case "LEARNED":
                        if (!"LEARNED_EXACTLY_ONCE".equals(stage)) continue;
                        key = "confirmedLearningEvents"; break;
                    case "RECOVERY": key = "recoveryObservations"; break;
                    default: continue;
                }
                releaseCounts.merge(key, counts.getLong(2), Long::sum);
                epochCounts.merge(key, counts.getLong(3), Long::sum);
            }
        }
        out.put("releaseProgress", releaseCounts);
        out.put("snapshotObserverEpochProgress", epochCounts);
        out.put("lastCanonicalLedgerSequence", lastCanonicalSequence);
        out.put("settlementTimingOrigin", "NOT_MEASURABLE_FROM_EVENT_LEDGER_WITHOUT_AUTHORITATIVE_SOURCE_AND_RETURN");
    }

    private static final class Period {
        final String target;
        long drawAt;
        long earliestCanonical;
        boolean requested;
        final List<Map<String, Object>> effects = new ArrayList<>();
        int omitted;

        Period(String target) { this.target = target; }

        void add(Cursor row) {
            String type = row.getString(2);
            if ("REQUESTED".equals(type)) requested = true;
            long at = row.getLong(4);
            if ("CANONICAL".equals(type) && (earliestCanonical == 0 || earliestCanonical > at)) {
                earliestCanonical = at;
            }
            if (effects.size() == 24) {
                omitted++;
                return;
            }
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("eventId", row.getString(1));
            event.put("event", type);
            event.put("stage", redact(row.getString(3)));
            event.put("wallAt", at);
            event.put("monoAt", row.getLong(5));
            event.put("processEpoch", row.getString(6));
            event.put("bootId", row.getString(7));
            event.put("requestGeneration", row.getString(8));
            effects.add(event);
        }

        Map<String, Object> export(String head) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("target", target);
            item.put("drawAt", drawAt > 0 ? drawAt : null);
            item.put("request", requested ? "OBSERVED" : "NO_REQUEST");
            item.put("canonicalAt", earliestCanonical > 0 ? earliestCanonical : null);
            item.put("drawState", drawAt > 0 ? "OBSERVED"
                    : target.equals(head) ? "HEAD_DRAW_NOT_OBSERVED" : "MISSING_DRAW");
            item.put("effects", effects);
            item.put("omittedEvents", omitted);
            return item;
        }
    }
}
