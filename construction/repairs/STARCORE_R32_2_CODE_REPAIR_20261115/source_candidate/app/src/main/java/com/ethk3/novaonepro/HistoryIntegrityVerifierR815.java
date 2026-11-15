package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Immutable business-issue history verifier used before prediction and research. */
public final class HistoryIntegrityVerifierR815 {
    public enum Readiness {
        NONE,
        MINIMUM_12,
        TRANSITION_24,
        LONG_CONTEXT_360,
        FULL_CONTEXT_720
    }

    public static final class Result {
        public final int validRows;
        public final int gapCount;
        public final int conflictCount;
        public final int duplicateCount;
        public final int contiguousSuffix;
        public final String minIssue;
        public final String maxIssue;
        public final String historyHash;
        public final Readiness readiness;
        private final List<Models.Draw> cleanAscending;

        Result(
                int validRows,
                int gapCount,
                int conflictCount,
                int duplicateCount,
                int contiguousSuffix,
                String minIssue,
                String maxIssue,
                String historyHash,
                Readiness readiness,
                List<Models.Draw> cleanAscending) {
            this.validRows = Math.max(0, validRows);
            this.gapCount = Math.max(0, gapCount);
            this.conflictCount = Math.max(0, conflictCount);
            this.duplicateCount = Math.max(0, duplicateCount);
            this.contiguousSuffix = Math.max(0, contiguousSuffix);
            this.minIssue = safe(minIssue);
            this.maxIssue = safe(maxIssue);
            this.historyHash = safe(historyHash);
            this.readiness = readiness == null ? Readiness.NONE : readiness;
            ArrayList<Models.Draw> copy = new ArrayList<Models.Draw>();
            if (cleanAscending != null) {
                for (int i = 0; i < cleanAscending.size(); i++) copy.add(cleanAscending.get(i).copy());
            }
            this.cleanAscending = Collections.unmodifiableList(copy);
        }

        public boolean minimalReady() {
            return conflictCount == 0 && contiguousSuffix >= AppConfig.R815_MIN_CANONICAL_HISTORY;
        }

        public boolean hasHardConflict() {
            return conflictCount > 0;
        }

        public List<Models.Draw> cleanAscending() {
            ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
            for (int i = 0; i < cleanAscending.size(); i++) out.add(cleanAscending.get(i).copy());
            return out;
        }

        /**
         * Returns only the most recent exact business-issue chain. Older, observable gaps remain
         * in {@link #gapCount} for diagnostics, but cannot poison a complete recent PIT suffix.
         */
        public List<Models.Draw> contiguousSuffixAscending() {
            ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();
            int start = Math.max(0, cleanAscending.size() - contiguousSuffix);
            for (int i = start; i < cleanAscending.size(); i++) {
                out.add(cleanAscending.get(i).copy());
            }
            return out;
        }

        public String summary() {
            return "valid="
                    + validRows
                    + "|gaps="
                    + gapCount
                    + "|conflicts="
                    + conflictCount
                    + "|duplicates="
                    + duplicateCount
                    + "|suffix="
                    + contiguousSuffix
                    + "|min="
                    + minIssue
                    + "|max="
                    + maxIssue
                    + "|readiness="
                    + readiness.name()
                    + "|hash="
                    + historyHash;
        }
    }

    private HistoryIntegrityVerifierR815() {}

    /**
     * Sorts a defensive copy by the 12-digit issue, removes exact duplicate evidence, and retains
     * conflicting evidence as a typed conflict rather than silently choosing one provider.
     */
    public static Result verifyAscending(List<Models.Draw> rows) {
        ArrayList<Models.Draw> candidate = new ArrayList<Models.Draw>();
        int conflicts = 0;
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                Models.Draw draw = rows.get(i);
                if (!validDraw(draw)) {
                    conflicts++;
                } else {
                    candidate.add(draw.copy());
                }
            }
        }
        Collections.sort(
                candidate,
                new Comparator<Models.Draw>() {
                    @Override
                    public int compare(Models.Draw left, Models.Draw right) {
                        return TargetIssueResolver.compare(left.issue, right.issue);
                    }
                });
        ArrayList<Models.Draw> clean = new ArrayList<Models.Draw>();
        int duplicates = 0;
        for (int i = 0; i < candidate.size(); i++) {
            Models.Draw draw = candidate.get(i);
            if (clean.isEmpty()) {
                clean.add(draw);
                continue;
            }
            Models.Draw prior = clean.get(clean.size() - 1);
            if (!prior.issue.equals(draw.issue)) {
                clean.add(draw);
                continue;
            }
            String priorHash = DrawHashR36.sha256V2(prior);
            String drawHash = DrawHashR36.sha256V2(draw);
            if (priorHash.equals(drawHash)) {
                duplicates++;
            } else {
                conflicts++;
            }
        }
        int gaps = 0;
        int suffix = longestContiguousSuffix(clean);
        for (int i = 1; i < clean.size(); i++) {
            if (TargetIssueResolver.distance(clean.get(i - 1).issue, clean.get(i).issue) != 1) gaps++;
        }
        String min = clean.isEmpty() ? "" : clean.get(0).issue;
        String max = clean.isEmpty() ? "" : clean.get(clean.size() - 1).issue;
        StringBuilder canonical = new StringBuilder("R815_HISTORY_V1");
        for (int i = 0; i < clean.size(); i++) {
            canonical.append('|').append(DrawHashR36.sha256V2(clean.get(i)));
        }
        canonical.append("|conflicts=").append(conflicts).append("|gaps=").append(gaps);
        return new Result(
                clean.size(),
                gaps,
                conflicts,
                duplicates,
                suffix,
                min,
                max,
                V51ChecksumEngine.sha256(canonical.toString()),
                readiness(suffix, conflicts),
                clean);
    }

    public static int longestContiguousSuffix(List<Models.Draw> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        int suffix = 0;
        String next = "";
        for (int i = rows.size() - 1; i >= 0; i--) {
            Models.Draw draw = rows.get(i);
            if (!validDraw(draw)) break;
            if (next.length() > 0 && TargetIssueResolver.distance(draw.issue, next) != 1) break;
            suffix++;
            next = draw.issue;
        }
        return suffix;
    }

    private static Readiness readiness(int suffix, int conflicts) {
        if (conflicts > 0 || suffix < AppConfig.R815_MIN_CANONICAL_HISTORY) return Readiness.NONE;
        if (suffix >= AppConfig.R815_FULL_CONTEXT_HISTORY) return Readiness.FULL_CONTEXT_720;
        if (suffix >= AppConfig.R815_LONG_CONTEXT_HISTORY) return Readiness.LONG_CONTEXT_360;
        if (suffix >= AppConfig.R815_MIN_TRANSITION_HISTORY) return Readiness.TRANSITION_24;
        return Readiness.MINIMUM_12;
    }

    private static boolean validDraw(Models.Draw draw) {
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
