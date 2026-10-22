package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.LinkedHashMap;

/**
 * Per-draw, low-priority dynamic-evolution heartbeat.
 *
 * <p>R947.1 closes two scheduler defects that made evolution look idle under one-minute draws:
 * (1) a pulse used to advance only one of four stages, so the review cursor could fall behind;
 * (2) after finishing an older issue the scheduler jumped directly to the newest clean draw and
 * could skip intermediate issues. The new cursor anchors at the current latest issue on first
 * install, then advances strictly through the next persisted clean issue. Within one bounded pulse
 * it may advance multiple cheap stages, stopping immediately on P0 pressure, budget yield or a
 * non-terminal error. Creation quotas and Future evidence remain authoritative: a review does not
 * promise a child, and any child still starts from N=0 Future-OOS.</p>
 *
 * <p>This class owns no Production decision and cannot authorize REAL.</p>
 */
public final class ResearchEvolutionHeartbeatR947 {
    public static final String VERSION = "R947_PER_DRAW_EVOLUTION_HEARTBEAT_V2";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private static final int STAGE_LIFECYCLE = 0;
    private static final int STAGE_FORMULA = 1;
    private static final int STAGE_ALGORITHM = 2;
    private static final int STAGE_GENESIS = 3;
    private static final int STAGE_DONE = 4;
    private static final int MAX_STAGES_PER_PULSE = 4;

    private ResearchEvolutionHeartbeatR947() {}

    /** Current draw owns live creation. Old recovery cursors cannot mark today's lane complete.
     * Historical review debt remains recorded in the original cursor; it is never relabeled. */
    public static Result pulseCurrent(DbStore store,long budgetMs) {
        if(store==null)return Result.fail("STORE_NULL");
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return Result.fail("YIELD_P0_PRESSURE");
        String issue=latestIssue(store);if(issue.isEmpty())return Result.fail("WAIT_HISTORY");
        String key="r960_evolution_stage_"+issue;
        int oldStage=parse(store.getState(key,"0"),-1);
        if(oldStage<0||oldStage>STAGE_DONE)return Result.fail("ERROR_INVALID_CURRENT_EVOLUTION_CURSOR");
        String maskKey="r965_evolution_mask_"+issue;
        int mask=parse(store.getState(maskKey,String.valueOf((1<<oldStage)-1)),-1);
        int first=parse(store.getState("r965_evolution_next_stage","0"),-1);
        if(mask<0||mask>15||first<0||first>=4)return Result.fail("ERROR_INVALID_EVOLUTION_MASK");
        long deadline=TimeSourceR811.monoNowMs()+Math.max(1L,budgetMs);
        String detail="ALREADY_REVIEWED";
        for(int turn=0;turn<4&&TimeSourceR811.monoNowMs()<deadline-30;turn++) {
            int stage=(first+turn)%4;
            if((mask&(1<<stage))!=0)continue;
            if(!issue.equals(latestIssue(store)))return new Result(false,issue,Integer.bitCount(mask),"SUPERSEDED_CURRENT_DRAW");
            LinkedHashMap<String,String> claim=new LinkedHashMap<String,String>();
            claim.put("r965_evolution_next_stage",String.valueOf((stage+1)%4));
            ResearchLearningContinuityR912.commitProgress(store,claim);
            detail=runStage(store,stage,Math.min(300L,Math.max(1L,deadline-TimeSourceR811.monoNowMs()-25)));
            if(terminalReview(detail))mask|=1<<stage;
            LinkedHashMap<String,String> update=new LinkedHashMap<String,String>();
            update.put("r960_evolution_detail_"+issue+"_"+stage,detail);
            update.put(maskKey,String.valueOf(mask));
            // The old cursor means a completed prefix. Never turn a later success into stage-zero success.
            int prefix=0;while(prefix<4&&(mask&(1<<prefix))!=0)prefix++;
            update.put(key,String.valueOf(prefix));
            update.put("r960_evolution_current_issue",issue);
            update.put("r947_evolution_status",(mask==15?"DONE":"CURRENT_PENDING")+"|issue="+issue+"|completedMask="+mask+"|lastStage="+stage+"|"+detail);
            update.put("r947_evolution_at",String.valueOf(System.currentTimeMillis()));
            ResearchLearningContinuityR912.commitProgress(store,update);
            // An incomplete lifecycle review is independent of the creation lanes.
            if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())break;
        }
        return new Result(mask==15,issue,Integer.bitCount(mask),(mask==15?"DONE":"CURRENT_PENDING")+"|issue="+issue+"|completedMask="+mask+"|"+detail);
    }

    public static Result pulse(DbStore store, long budgetMs) {
        if (store == null) return Result.fail("STORE_NULL");
        if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
            return Result.fail("YIELD_P0_PRESSURE");
        if (MAY_AUTHORIZE_REAL) throw new IllegalStateException("R947_EVOLUTION_REAL_BOUNDARY");
        String latest = latestIssue(store);
        if (latest.length() == 0) return Result.fail("WAIT_HISTORY");

        String completed = safe(store.getState("r947_evolution_completed_through", ""));
        String issue = safe(store.getState("r947_evolution_issue", ""));
        int stage = parse(store.getState("r947_evolution_stage", "0"), 0);

        // First install deliberately anchors at the current clean draw. Historical development data
        // cannot be retroactively called Future evolution. From that anchor onward every clean draw
        // is reviewed in order and no intermediate issue may be skipped.
        if (issue.length() == 0 || stage >= STAGE_DONE) {
            issue = completed.length() == 0 ? latest : nextCleanIssue(store, completed);
            if (issue.length() == 0) {
                publishBacklog(store, completed);
                return new Result(true, completed, STAGE_DONE, "IDLE_LATEST_ALREADY_REVIEWED");
            }
            stage = STAGE_LIFECYCLE;
            publishCursor(store, issue, stage, completed.length() == 0
                    ? "ANCHOR_CURRENT_LATEST_NO_RETROACTIVE_EVOLUTION"
                    : "START_NEXT_CHRONOLOGICAL_EVOLUTION");
        }

        final String workingIssue = issue;
        final long started = TimeSourceR811.monoNowMs();
        final long deadline = started + Math.max(180L, Math.min(1600L, budgetMs));
        int advanced = 0;
        String lastDetail = "NO_STAGE_RUN";
        boolean created = false;

        while (stage < STAGE_DONE && advanced < MAX_STAGES_PER_PULSE
                && TimeSourceR811.monoNowMs() < deadline) {
            if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive()) {
                lastDetail = "YIELD_P0_PRESSURE";
                break;
            }
            long remaining = Math.max(120L, deadline - TimeSourceR811.monoNowMs());
            long childBudget = Math.max(120L, Math.min(450L, remaining));
            String detail = runStage(store, stage, childBudget);
            increment(store, "r947_evolution_stage_attempts_total", 1L);
            store.setState("r947_evolution_stage_" + stage + "_last", detail);
            store.setState("r947_evolution_stage_" + stage + "_last_issue", workingIssue);
            lastDetail = detail;
            if (isCreated(detail)) {
                created = true;
                increment(store, "r947_evolution_children_created_total", 1L);
                store.setState("r947_evolution_last_created", detail);
                store.setState("r947_evolution_last_created_issue", workingIssue);
                store.setState("r947_evolution_last_created_at", String.valueOf(System.currentTimeMillis()));
            }
            if (!terminalReview(detail)) {
                publishCursor(store, workingIssue, stage, detail);
                publishBacklog(store, completed);
                return new Result(false, workingIssue, stage,
                        "PAUSED|advanced=" + advanced + "|" + detail);
            }
            stage++;
            advanced++;
            if (stage < STAGE_DONE) publishCursor(store, workingIssue, stage,
                    "STAGE_" + (stage - 1) + "_DONE|" + detail);
        }

        if (stage < STAGE_DONE) {
            publishCursor(store, workingIssue, stage,
                    "BUDGET_SLICE_END|advanced=" + advanced + "|last=" + lastDetail);
            publishBacklog(store, completed);
            return new Result(true, workingIssue, stage,
                    "PARTIAL_BUDGETED_REVIEW|advanced=" + advanced + "|last=" + lastDetail);
        }

        increment(store, "r947_evolution_reviews_total", 1L);
        LinkedHashMap<String,String> state = new LinkedHashMap<String,String>();
        state.put("r947_evolution_completed_through", workingIssue);
        state.put("r947_evolution_issue", workingIssue);
        state.put("r947_evolution_stage", String.valueOf(STAGE_DONE));
        state.put("r947_evolution_status", "DONE|issue=" + workingIssue + "|stages=" + advanced
                + "|created=" + (created ? 1 : 0) + "|last=" + lastDetail
                + "|newChildrenStillRequireFutureN0=1|productionAuthority=0");
        state.put("r947_evolution_at", String.valueOf(System.currentTimeMillis()));
        store.setStates(state);
        publishBacklog(store, workingIssue);
        return new Result(true, workingIssue, STAGE_DONE, state.get("r947_evolution_status"));
    }

    private static String runStage(DbStore store, int stage, long budgetMs) {
        ResearchWorkBudgetR927.Scope scope = ResearchWorkBudgetR927.enter(budgetMs);
        try {
            if (stage == STAGE_LIFECYCLE) {
                return AlgorithmEvolutionLifecycleR912.maintain(store).detail;
            } else if (stage == STAGE_FORMULA) {
                return DirectedResearchCreationR932.maintain(store, "formula");
            } else if (stage == STAGE_ALGORITHM) {
                return AlgorithmInventionLabR908.maintain(store);
            } else if (stage == STAGE_GENESIS) {
                return AssetGenesisEngineR915.maintain(store, budgetMs);
            }
            return "STAGE_ALREADY_DONE";
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            return "YIELD:" + safe(yielded.getMessage());
        } catch (Throwable error) {
            return "ERROR:" + error.getClass().getSimpleName() + ':' + safe(error.getMessage());
        } finally {
            ResearchWorkBudgetR927.leave(scope);
        }
    }

    /** Completion belongs to every nested stage, not merely to the outer label.
     * Composite lane results use | and key=value wrappers. A pending lifecycle
     * result must never advance the durable served watermark when it is wrapped.
     */
    static boolean terminalReview(String value) {
        String x = (value == null ? "" : value.trim()).toUpperCase(java.util.Locale.US);
        if (x.length() == 0 || x.indexOf("YIELD") >= 0 || x.indexOf("ERROR") >= 0
                || x.indexOf("FAILED") >= 0 || x.indexOf("COMMIT_FAILED") >= 0) return false;
        for (String field : x.split("[|=:]")) {
            String token = field.trim();
            for (String pending : new String[] {"LIFECYCLE_PENDING", "DEV_REGISTERED",
                    "DEV_RUNNING", "EVOLUTION_PENDING", "ATOMIC_PENDING",
                    "DEV_ROUND_PENDING", "R952_PENDING", "CURRENT_PENDING",
                    "FIXED_WINDOW_PENDING", "CERTIFICATE_PENDING", "MODULE_PENDING", "SLOW_STATE_PENDING"}) {
                if (token.startsWith(pending)) return false;
            }
        }
        return true;
    }

    static boolean isCreated(String value) {
        String x = (value==null?"":value.trim()).toUpperCase(java.util.Locale.US);
        if(x.startsWith("CREATED_N0|")||x.startsWith("CREATED_N0_"))return true;
        for(String field:x.split("\\|")) {
            if(field.startsWith("GENERATED=")||field.startsWith("CREATED=")) {
                try{if(Integer.parseInt(field.substring(field.indexOf('=')+1).trim())>0)return true;}
                catch(NumberFormatException invalid){ /* A non-numeric status is not a created child. */ }
            }
        }
        return false;
    }

    /** First clean issue strictly after the completed cursor. */
    static String nextCleanIssue(DbStore store, String completed) {
        if (store == null || !TargetIssueResolver.isValid(completed)) return "";
        try (Cursor c = store.readable().rawQuery("SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 AND issue>? ORDER BY issue ASC LIMIT 1", new String[] {completed})) {
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        }
    }

    private static void publishBacklog(DbStore store, String completed) {
        if (store == null) return;
        int backlog = 0;
        try (Cursor c = store.readable().rawQuery("SELECT COUNT(1) FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 AND issue>?", new String[] {safe(completed)})) {
            if (c.moveToFirst()) backlog = Math.max(0, c.getInt(0));
        } catch (Throwable ignored) {}
        store.setState("r947_evolution_backlog", String.valueOf(backlog));
        store.setState("r947_evolution_backlog_at", String.valueOf(System.currentTimeMillis()));
    }

    private static void publishCursor(DbStore store, String issue, int stage, String status) {
        LinkedHashMap<String,String> state = new LinkedHashMap<String,String>();
        state.put("r947_evolution_issue", safe(issue));
        state.put("r947_evolution_stage", String.valueOf(Math.max(0, stage)));
        state.put("r947_evolution_status", safe(status));
        state.put("r947_evolution_at", String.valueOf(System.currentTimeMillis()));
        store.setStates(state);
    }

    private static void increment(DbStore store, String key, long delta) {
        if (store == null || delta == 0L) return;
        long old = 0L;
        try { old = Long.parseLong(safe(store.getState(key, "0"))); } catch (Throwable ignored) {}
        long next = Math.max(0L, old + delta);
        store.setState(key, String.valueOf(next));
    }

    private static String latestIssue(DbStore store) {
        try (Cursor c = store.readable().rawQuery("SELECT issue FROM " + DbHelper.TABLE_DRAWS
                + " WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) {
            return c.moveToFirst() ? safe(c.getString(0)) : "";
        }
    }
    private static int parse(String value, int fallback) {
        try { return Integer.parseInt(safe(value)); } catch (Throwable ignored) { return fallback; }
    }
    private static String safe(String value) { return value == null ? "" : value.replace('|','_').trim(); }

    public static final class Result {
        public final boolean success; public final String issue,status; public final int stage;
        Result(boolean success,String issue,int stage,String status){this.success=success;this.issue=safe(issue);this.stage=stage;this.status=safe(status);}
        static Result fail(String status){return new Result(false,"",-1,status);}
        public String canonical(){return VERSION+"|success="+(success?1:0)+"|issue="+issue+"|stage="+stage+"|status="+status;}
    }
}
