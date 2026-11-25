package com.ethk3.novaonepro;

/** Pure, host-testable contract for UI-exit/background recovery truth. */
public final class BackgroundRuntimeContractR922 {
    public static final String VERSION = "R922_BACKGROUND_RUNTIME_CONTRACT_V1";
    public static final String LIMITATION =
            "ANDROID_FORCE_STOP_OR_OEM_BATTERY_RESTRICTION_CANNOT_BE_BYPASSED";

    private BackgroundRuntimeContractR922() {}

    public static Evaluation evaluate(
            boolean userEnabled,
            boolean foregroundServiceDeclared,
            boolean stopWithTaskFalse,
            boolean bootReceiverDeclared,
            boolean jobServiceDeclared,
            boolean alarmRecoveryDeclared,
            boolean taskRemovedReconcile,
            boolean uiExitReconcile,
            boolean researchLoopArmed,
            boolean settlementLoopArmed) {
        if (!userEnabled)
            return new Evaluation(false, "USER_DISABLED", false, false,
                    "后台运行由用户关闭", LIMITATION);
        boolean staticReady = foregroundServiceDeclared && stopWithTaskFalse
                && bootReceiverDeclared && jobServiceDeclared && alarmRecoveryDeclared;
        boolean runtimeReady = taskRemovedReconcile && uiExitReconcile
                && researchLoopArmed && settlementLoopArmed;
        String status = staticReady && runtimeReady
                ? "ARMED_RECONCILABLE" : !staticReady ? "STATIC_CONTRACT_INCOMPLETE"
                : "RUNTIME_LOOP_INCOMPLETE";
        String detail = "foreground=" + (foregroundServiceDeclared ? 1 : 0)
                + "|stopWithTaskFalse=" + (stopWithTaskFalse ? 1 : 0)
                + "|boot=" + (bootReceiverDeclared ? 1 : 0)
                + "|job=" + (jobServiceDeclared ? 1 : 0)
                + "|alarm=" + (alarmRecoveryDeclared ? 1 : 0)
                + "|taskRemovedReconcile=" + (taskRemovedReconcile ? 1 : 0)
                + "|uiExitReconcile=" + (uiExitReconcile ? 1 : 0)
                + "|research=" + (researchLoopArmed ? 1 : 0)
                + "|settlement=" + (settlementLoopArmed ? 1 : 0);
        return new Evaluation(staticReady && runtimeReady, status, staticReady, runtimeReady,
                detail, LIMITATION);
    }

    static String observedOwnerR342(boolean enabled, String serviceState, String startScope,
            String businessScope, RuntimeObservationScopeR341 observer) {
        if (!enabled) return "USER_DISABLED";
        if (!"RUNNING".equals(serviceState)) return "OWNER_" + serviceState;
        try {
            org.json.JSONObject start = new org.json.JSONObject(startScope);
            org.json.JSONObject business = new org.json.JSONObject(businessScope);
            String process = start.getString("processEpoch");
            if (process.isEmpty() || RuntimeObservationScopeR341.UNRESOLVED.equals(process)
                    || !process.equals(business.getString("processEpoch"))
                    || !observer.releaseId.equals(start.getString("releaseId"))
                    || !observer.releaseId.equals(business.getString("releaseId"))
                    || !observer.sourceCommit.equals(start.getString("sourceCommit"))
                    || !observer.sourceCommit.equals(business.getString("sourceCommit"))
                    || !observer.bootId.equals(start.getString("bootId"))
                    || !observer.bootId.equals(business.getString("bootId"))
                    || business.getLong("sampleMonoAt") < start.getLong("sampleMonoAt")
                    || business.getLong("sampleMonoAt") > observer.sampleMonoAt) {
                return "BUSINESS_PROJECTION_SCOPE_MISMATCH";
            }
            return "CURRENT_OWNER_BUSINESS_PROJECTION_OBSERVED";
        } catch (org.json.JSONException missing) {
            return "OWNER_PROJECTION_MISSING";
        }
    }

    public static final class Evaluation {
        public final boolean passed, staticReady, runtimeReady;
        public final String status, detail, limitation;

        Evaluation(boolean passed, String status, boolean staticReady, boolean runtimeReady,
                String detail, String limitation) {
            this.passed = passed;
            this.status = status == null ? "" : status;
            this.staticReady = staticReady;
            this.runtimeReady = runtimeReady;
            this.detail = detail == null ? "" : detail;
            this.limitation = limitation == null ? "" : limitation;
        }

        public String canonical() {
            return VERSION + "|passed=" + (passed ? 1 : 0) + "|status=" + status + '|'
                    + detail + "|limitation=" + limitation;
        }
    }
}
