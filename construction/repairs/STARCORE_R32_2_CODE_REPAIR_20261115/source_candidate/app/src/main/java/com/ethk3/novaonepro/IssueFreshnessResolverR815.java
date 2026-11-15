package com.ethk3.novaonepro;

/**
 * Chooses the most recent valid provider payload by business issue, never by transport success or
 * server open-time.  The result is deliberately read-only: persistence and settlement stay with
 * their respective owners.
 */
public final class IssueFreshnessResolverR815 {
    public enum Status {
        ADVANCED,
        NO_NEW_ISSUE,
        PROVIDER_REGRESSION,
        INVALID_PAYLOAD
    }

    public static final class Result {
        public final Status status;
        public final Models.Draw selected;
        public final String source;
        public final String localIssue;
        public final String primaryIssue;
        public final String backupIssue;

        Result(
                Status status,
                Models.Draw selected,
                String source,
                String localIssue,
                String primaryIssue,
                String backupIssue) {
            this.status = status == null ? Status.INVALID_PAYLOAD : status;
            this.selected = selected == null ? null : selected.copy();
            this.source = safe(source);
            this.localIssue = safe(localIssue);
            this.primaryIssue = safe(primaryIssue);
            this.backupIssue = safe(backupIssue);
        }

        public boolean advanced() {
            return status == Status.ADVANCED && selected != null;
        }

        public String summary() {
            return status.name()
                    + "|selected="
                    + (selected == null ? "" : selected.issue)
                    + "|source="
                    + source
                    + "|local="
                    + localIssue
                    + "|primary="
                    + primaryIssue
                    + "|backup="
                    + backupIssue;
        }
    }

    private IssueFreshnessResolverR815() {}

    public static Result select(String localIssue, Models.Draw primary, Models.Draw backup) {
        Models.Draw primaryValid = valid(primary) ? primary.copy() : null;
        Models.Draw backupValid = valid(backup) ? backup.copy() : null;
        String primaryIssue = primary == null ? "" : safe(primary.issue);
        String backupIssue = backup == null ? "" : safe(backup.issue);
        String local = safe(localIssue);
        if (primaryValid == null && backupValid == null) {
            return new Result(
                    Status.INVALID_PAYLOAD, null, "NONE", local, primaryIssue, backupIssue);
        }
        Models.Draw selected = newer(primaryValid, backupValid);
        String source = selected == primaryValid ? "PRIMARY" : "BACKUP";
        if (!TargetIssueResolver.isValid(local)) {
            return new Result(Status.ADVANCED, selected, source, local, primaryIssue, backupIssue);
        }
        int distance = TargetIssueResolver.distance(local, selected.issue);
        if (distance > 0) {
            return new Result(Status.ADVANCED, selected, source, local, primaryIssue, backupIssue);
        }
        if (distance == 0) {
            return new Result(Status.NO_NEW_ISSUE, selected, source, local, primaryIssue, backupIssue);
        }
        return new Result(
                Status.PROVIDER_REGRESSION, selected, source, local, primaryIssue, backupIssue);
    }

    private static Models.Draw newer(Models.Draw left, Models.Draw right) {
        if (left == null) return right;
        if (right == null) return left;
        int distance = TargetIssueResolver.distance(left.issue, right.issue);
        if (distance > 0) return right;
        if (distance < 0) return left;
        // Same business issue is not "advanced".  Prefer primary deterministically and never let
        // an open-time difference turn a duplicated response into a new draw.
        return left;
    }

    private static boolean valid(Models.Draw draw) {
        if (draw == null || !draw.isValid() || !TargetIssueResolver.isValid(draw.issue)) return false;
        if (draw.numbers == null || draw.numbers.length != AppConfig.NUMBER_COUNT) return false;
        boolean[] seen = new boolean[AppConfig.MAX_NUMBER + 1];
        for (int i = 0; i < draw.numbers.length; i++) {
            int number = draw.numbers[i];
            if (!AppConfig.isValidNumber(number) || seen[number]) return false;
            seen[number] = true;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
