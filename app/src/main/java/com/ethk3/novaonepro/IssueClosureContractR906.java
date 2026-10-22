package com.ethk3.novaonepro;

/** Exact state/receipt contract; implementation remains under SettlementEngine sole ownership. */
public final class IssueClosureContractR906 {
    public enum Eligibility {
        ELIGIBLE_PREDRAW,
        EXCLUDED_NO_PREDRAW,
        BLOCKED_DRAW_CONFLICT,
        BLOCKED_DATA_ERROR
    }

    public enum CoreStatus {
        DRAW_OBSERVED,
        READY_CORE,
        RUNNING_CORE,
        RETRY_WAIT,
        COMPLETED,
        EXCLUDED,
        BLOCKED
    }

    public enum FailureClass {
        NONE(false),
        SQLITE_BUSY(true),
        SQLITE_LOCKED(true),
        TRANSIENT_TIMEOUT(true),
        PROCESS_ORPHAN(true),
        OOM_RECOVERABLE(true),
        DRAW_MISSING(true),
        DRAW_CONFLICT(false),
        HASH_CONFLICT(false),
        PREDRAW_CONTRACT_INVALID(false),
        CODE_INVARIANT(false);

        public final boolean retryable;
        FailureClass(boolean retryable) { this.retryable = retryable; }
    }

    public static final class Claim {
        public final String targetIssue, drawHash, owner, processBootId;
        public final long leaseGeneration, leaseUntil, startedAt;

        public Claim(String issue, String drawHash, String owner, String bootId,
                     long generation, long until, long startedAt) {
            if (!TargetIssueResolver.isValid(issue))
                throw new IllegalArgumentException("R906_CLAIM_TARGET");
            if (safe(drawHash).length() != 64 || safe(owner).length() == 0
                    || safe(bootId).length() == 0 || generation <= 0L)
                throw new IllegalArgumentException("R906_CLAIM_IDENTITY");
            this.targetIssue = issue;
            this.drawHash = drawHash;
            this.owner = owner;
            this.processBootId = bootId;
            this.leaseGeneration = generation;
            this.leaseUntil = until;
            this.startedAt = startedAt;
        }
    }

    public static final class Receipt {
        public final String targetIssue, drawHash, predrawHash, oosHash, settlementHash, recordHash;
        public final Eligibility eligibility;
        public final CoreStatus status;
        public final int actualNumber, hitLabel;
        public final long completedAt;

        public Receipt(String issue, String drawHash, String predrawHash, String oosHash,
                       String settlementHash, Eligibility eligibility, CoreStatus status,
                       int actual, int hit, long completedAt) {
            if (!TargetIssueResolver.isValid(issue) || eligibility == null || status == null)
                throw new IllegalArgumentException("R906_RECEIPT_INPUT");
            if (eligibility == Eligibility.ELIGIBLE_PREDRAW
                    && (status != CoreStatus.COMPLETED || (hit != 0 && hit != 1)))
                throw new IllegalArgumentException("R906_RECEIPT_ELIGIBLE_NOT_COMPLETED");
            if (eligibility == Eligibility.EXCLUDED_NO_PREDRAW && status != CoreStatus.EXCLUDED)
                throw new IllegalArgumentException("R906_RECEIPT_EXCLUSION_STATUS");
            this.targetIssue = issue;
            this.drawHash = safe(drawHash);
            this.predrawHash = safe(predrawHash);
            this.oosHash = safe(oosHash);
            this.settlementHash = safe(settlementHash);
            this.eligibility = eligibility;
            this.status = status;
            this.actualNumber = actual;
            this.hitLabel = hit;
            this.completedAt = completedAt;
            this.recordHash = V51ChecksumEngine.sha256(canonical());
        }

        public boolean countsAsSettledPrediction() {
            return eligibility == Eligibility.ELIGIBLE_PREDRAW && status == CoreStatus.COMPLETED;
        }

        public String canonical() {
            return "R906_CLOSURE|" + targetIssue + '|' + drawHash + '|' + predrawHash + '|'
                    + oosHash + '|' + settlementHash + '|' + eligibility.name() + '|'
                    + status.name() + '|' + actualNumber + '|' + hitLabel + '|' + completedAt;
        }
    }

    private static String safe(String value) { return value == null ? "" : value.trim(); }
    private IssueClosureContractR906() {}
}

