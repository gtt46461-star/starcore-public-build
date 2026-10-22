package com.ethk3.novaonepro;

/** Durable next-issue request first; exact settlement and historical catch-up retain their existing owners. */
public final class BusinessReconcilerR36 {
    public interface AnalysisRequester { void request(AnalysisSingleFlightR36.Request request); }

    public ReconcileResult reconcile(DbStore store, Models.Draw latest, String reason, AnalysisRequester requester) {
        if (store == null || latest == null || !latest.isValid())
            return new ReconcileResult(false,false,false,0,"INVALID_INPUT","","");
        TargetIssueResolver.Resolution next=TargetIssueResolver.next(latest.issue);
        if (!next.valid) return new ReconcileResult(false,false,false,0,"TARGET_INVALID",latest.issue,"");
        // Persist and submit the next legal request before any prior-issue settlement/telemetry I/O.
        // A locked historical row must not consume this issue's remaining pre-draw window.
        final boolean canonicalAlreadyReady = CanonicalObservationWriterR36.hasCanonical(store,next.targetIssue);
        final long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(latest);
        boolean requestInTime=deadline>System.currentTimeMillis();
        boolean requestDeferred=false;
        if (!canonicalAlreadyReady && requestInTime) {
            RealtimePressureGateR811.pulseLiveRequest();
            AnalysisSingleFlightR36.Request pending;
            try {
                pending = new AnalysisRequestStoreR36(store)
                        .ensurePending(latest.issue,next.targetIssue,deadline,reason);
            } catch (IllegalStateException expired) {
                // The fixed deadline can pass after the check above. Do not renew it or
                // recompute after draw; still enqueue settlement for the observed draw.
                pending=null;
                if (retryableRequestContention(expired)) {
                    // The existing watchdog retries the SAME target and immutable deadline.
                    // A failed write is neither a durable request nor a fatal service failure.
                    requestDeferred=true;
                } else if ("F01_TARGET_DEADLINE_EXPIRED".equals(expired.getMessage())
                        && deadline<=System.currentTimeMillis()) requestInTime=false;
                else throw expired;
            }
            if (pending != null && requester != null) requester.request(pending);
        }

        boolean previousBusinessClosed=SettlementCoreTruthR909.businessClosed(store,latest.issue);
        if(!previousBusinessClosed) RealtimePressureGateR811.pulseLiveRequest();

        // R915 separates the durable settlement lane from the next pre-draw analysis lane in every
        // release mode.  Synchronously spinning on a locked/late settlement used to consume the
        // only analysis window and caused both missing settlements and missing next predictions.
        boolean exactQueued=SettlementCoordinatorR815.queueCurrentExactR914(
                store,latest,SettlementCoordinatorR815.Reason.RECONCILER);
        SettlementTaskStoreR36.Terminal terminal=SettlementTaskStoreR36.terminal(store,latest.issue);
        boolean businessClosed=SettlementCoreTruthR909.businessClosed(store,latest.issue);
        boolean settledPrediction=SettlementCoreTruthR909.settledPrediction(store,latest.issue);
        int settled=settledPrediction?1:0;
        String closureState=SettlementCoreTruthR909.state(store,latest.issue);
        final boolean priorSettlementBlocked=!canonicalAlreadyReady&&terminal!=null&&(SettlementTaskStoreR36.FAILED_TERMINAL.equals(terminal.status)||SettlementTaskStoreR36.INVALID_DRAW_TERMINAL.equals(terminal.status));
        String blockedCode="";
        if(priorSettlementBlocked)
            blockedCode=terminal==null?"UNKNOWN":safe(terminal.terminalCode.length()>0?terminal.terminalCode:terminal.error);
        try{
            java.util.LinkedHashMap<String,String> projection=new java.util.LinkedHashMap<>();
            projection.put("r906_business_current_issue",latest.issue);
            projection.put("r906_business_current_closure",closureState);
            projection.put("r906_business_current_closed",businessClosed?"1":"0");
            projection.put("r906_business_current_settled_prediction",settledPrediction?"1":"0");
            projection.put("r906_supervisor_pulse_requested","1");
            if(priorSettlementBlocked){
                projection.put("r915_analysis_after_blocked_settlement_issue",latest.issue);
                projection.put("r915_analysis_after_blocked_settlement_code",blockedCode);
                projection.put("r915_analysis_after_blocked_settlement_policy","SEPARATE_LANES_CONTINUE_NEXT_PREDRAW_NO_SETTLEMENT_REWRITE");
            }
            // All optional observations of this invocation share one atomic
            // projection. The blocked-settlement branch must not add three
            // further writer waits or publish only half of its display state.
            store.trySetTelemetryStatesR77(projection);
        }catch(Throwable ignore){}

        if(!next.valid)
            return new ReconcileResult(false,businessClosed,businessClosed,settled,
                    "TARGET_INVALID|closure="+closureState,latest.issue,"");

        if(canonicalAlreadyReady) {
            return new ReconcileResult(true,businessClosed,businessClosed,settled,
                    businessClosed?"BUSINESS_CLOSED":"CANONICAL_READY|closure="+closureState,
                    latest.issue,next.targetIssue);
        }

        // A blocked historical row stays immutable and visible, but it must not erase the next
        // issue's pre-draw observation.  REAL authorization is independently fail-closed by the
        // canonical decision gate and therefore does not require coupling these two schedulers.

        String exactDetail=exactQueued
                ?"R915_DURABLE_QUEUE_SETTLEMENT_LANE"
                :"R915_QUEUE_REJECTED";
        return new ReconcileResult(false,businessClosed,businessClosed,settled,
                (requestDeferred?"ANALYSIS_RETRY_DB_BUSY":requestInTime?"ANALYSIS_REQUESTED":"TARGET_EXPIRED_WAIT_LATEST")+"|closure="+closureState+"|exact="+exactDetail
                        +(blockedCode.length()>0?"|priorSettlementBlocked="+blockedCode:""),
                latest.issue,next.targetIssue);
    }

    static boolean retryableRequestContention(IllegalStateException error) {
        String message=safe(error.getMessage());
        if(!message.startsWith("R36_REQUEST_PERSIST_FAILED:"))return false;
        return message.contains("CP3_DB_BUSY_DEADLINE")
                ||message.contains("CP3_DB_BUSY_LOCK_TIMEOUT");
    }

    public static final class ReconcileResult {
        public final boolean canonicalReady, settlementTerminal, businessClosed;
        public final int settlementTerminalizedCount;
        public final String status, baseIssue, targetIssue;
        ReconcileResult(boolean ready,boolean terminal,boolean closed,int terminalizedCount,String status,String base,String target){
            canonicalReady=ready; settlementTerminal=terminal; businessClosed=closed;
            settlementTerminalizedCount=Math.max(0,terminalizedCount);this.status=safe(status);baseIssue=safe(base);targetIssue=safe(target);
        }
    }
    private static String safe(String x){return x==null?"":x.trim();}
}
