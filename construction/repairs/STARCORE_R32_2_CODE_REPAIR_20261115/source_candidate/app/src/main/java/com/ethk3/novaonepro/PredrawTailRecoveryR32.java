package com.ethk3.novaonepro;

import java.util.List;

/** Reconstructs a level-triggered recovery intent from the committed draw head.
 * No callback, process-local target or telemetry row is the source of truth.
 * Restart and a newer draw naturally replace the desired target. This class
 * requests history only; normal PIT/deadline and settlement owners remain final. */
public final class PredrawTailRecoveryR32 {
    private PredrawTailRecoveryR32() {}

    public static final class Intent {
        public final boolean requestHistory;
        public final String baseIssue, targetIssue, reason;
        public final int contiguousRows;
        private Intent(boolean requestHistory,String baseIssue,String targetIssue,
                       int contiguousRows,String reason) {
            this.requestHistory=requestHistory;this.baseIssue=baseIssue;
            this.targetIssue=targetIssue;this.contiguousRows=contiguousRows;this.reason=reason;
        }
    }

    public static Intent reconstruct(Models.Draw committedHead,List<Models.Draw> recent) {
        if(committedHead==null||!committedHead.isValid()
                ||!TargetIssueResolver.isValid(committedHead.issue))
            return new Intent(true,"","",0,"R32_TAIL_RECOVERY_NO_DURABLE_HEAD");
        TargetIssueResolver.Resolution next=TargetIssueResolver.next(committedHead.issue);
        if(!next.valid)return new Intent(true,committedHead.issue,"",0,
                "R32_TAIL_RECOVERY_INVALID_TARGET");
        int count=R36AnalysisHistoryPolicyR364.contiguousSuffixCount(recent,committedHead.issue);
        boolean needed=count<R36AnalysisHistoryPolicyR364.MIN_LIVE_HISTORY;
        return new Intent(needed,committedHead.issue,next.targetIssue,count,
                needed?"R32_TAIL_RECOVERY_FOR:"+next.targetIssue+"|suffix="+count
                        :"R32_TAIL_READY:"+next.targetIssue);
    }
}
