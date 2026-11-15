package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * B10 formal action-space owner.
 *
 * <p>The business universe is immutable: ten positions, and exactly C(10,3)=120 unordered
 * three-number actions for each position. This class validates the complete 1200-action surface,
 * provides deterministic identity/index functions, and freezes the unique Rank-1 champion. It never
 * prunes actions and has no BET or settlement authority.
 */
public final class K3ActionSpaceEngine {
    public static final int POSITIONS = 10;
    public static final int NUMBERS = 10;
    public static final int K = 3;
    public static final int COMBINATIONS_PER_POSITION = 120;
    public static final int TOTAL_ACTIONS = 1200;
    public static final String V57_B10_OWNER = "FORMAL_ACTION_SPACE_OWNER";
    public static final String DELETION_POLICY = "FORBIDDEN";

    /** Immutable coordinate in the formal universe; it contains no model score. */
    public static final class ActionIdentity {
        public final int position;
        public final int n1;
        public final int n2;
        public final int n3;
        public final int positionOrdinal;
        public final int globalOrdinal;
        public final String canonical;
        public final String checksum;

        ActionIdentity(int position, int n1, int n2, int n3) {
            this.position = position;
            this.n1 = n1;
            this.n2 = n2;
            this.n3 = n3;
            this.positionOrdinal = combinationOrdinal(n1, n2, n3);
            this.globalOrdinal = (position - 1) * COMBINATIONS_PER_POSITION + positionOrdinal;
            this.canonical = position + "|" + n1 + "|" + n2 + "|" + n3;
            this.checksum = V51ChecksumEngine.sha256("B10_ACTION_IDENTITY|" + canonical);
        }

        public int[] numbers() {
            return new int[] {n1, n2, n3};
        }

        public boolean contains(int number) {
            return number == n1 || number == n2 || number == n3;
        }
    }

    /** One explicit validation fault; no row-count fallback is rendered as PASS. */
    public static final class ValidationFinding {
        public final int arrayIndex;
        public final String code;
        public final String detail;
        public final String checksum;

        ValidationFinding(int arrayIndex, String code, String detail) {
            this.arrayIndex = arrayIndex;
            this.code = safe(code);
            this.detail = safe(detail);
            this.checksum =
                    V51ChecksumEngine.sha256(
                            "B10_FINDING|" + arrayIndex + '|' + this.code + '|' + this.detail);
        }
    }

    /** Full validation result used by build tests, runtime checks and diagnostics. */
    public static final class ValidationReport {
        public final boolean valid;
        public final int inputRows;
        public final int uniqueRows;
        public final int[] rowsPerPosition;
        public final ValidationFinding[] findings;
        public final String identityUniverseHash;
        public final String payloadHash;
        public final String checksum;

        ValidationReport(
                int inputRows,
                int uniqueRows,
                int[] rowsPerPosition,
                ArrayList<ValidationFinding> findings,
                String identityUniverseHash,
                String payloadHash) {
            this.inputRows = inputRows;
            this.uniqueRows = uniqueRows;
            this.rowsPerPosition = copy(rowsPerPosition);
            this.findings = findings.toArray(new ValidationFinding[findings.size()]);
            this.identityUniverseHash = safe(identityUniverseHash);
            this.payloadHash = safe(payloadHash);
            this.valid =
                    inputRows == TOTAL_ACTIONS
                            && uniqueRows == TOTAL_ACTIONS
                            && this.findings.length == 0
                            && allPositionsComplete(this.rowsPerPosition);
            StringBuilder body = new StringBuilder(4096);
            body.append(valid)
                    .append('|')
                    .append(inputRows)
                    .append('|')
                    .append(uniqueRows)
                    .append('|')
                    .append(this.identityUniverseHash)
                    .append('|')
                    .append(this.payloadHash);
            for (int p = 1; p <= POSITIONS; p++) body.append('|').append(rowsPerPosition[p]);
            for (int i = 0; i < this.findings.length; i++)
                body.append('|').append(this.findings[i].checksum);
            this.checksum = V51ChecksumEngine.sha256("B10_VALIDATION|" + body.toString());
        }

        public String summary() {
            return "valid="
                    + valid
                    + "|rows="
                    + inputRows
                    + "|unique="
                    + uniqueRows
                    + "|findings="
                    + findings.length
                    + "|identityHash="
                    + identityUniverseHash
                    + "|payloadHash="
                    + payloadHash
                    + "|auditHash="
                    + checksum;
        }
    }

    /** Rank-1 action plus the exact complete surface on which it won. */
    public static final class FrozenChampion {
        public final V51Models.PositionAction action;
        public final String championHash;
        public final String actionSpaceHash;
        public final String identityUniverseHash;
        public final long frozenAt;

        FrozenChampion(V51Models.PositionAction action, String actionSpaceHash, long frozenAt) {
            this.action = action;
            this.actionSpaceHash = safe(actionSpaceHash);
            this.identityUniverseHash = expectedIdentityUniverseHash();
            this.frozenAt = Math.max(0L, frozenAt);
            this.championHash =
                    V51ChecksumEngine.sha256(
                            "B10_CHAMPION|"
                                    + action.checksum
                                    + '|'
                                    + action.fingerprint
                                    + '|'
                                    + this.actionSpaceHash
                                    + '|'
                                    + this.identityUniverseHash
                                    + '|'
                                    + this.frozenAt);
        }

        public boolean unchanged(V51Models.PositionAction now) {
            return now != null
                    && action.position == now.position
                    && action.n1 == now.n1
                    && action.n2 == now.n2
                    && action.n3 == now.n3
                    && action.rank == now.rank
                    && action.fingerprint.equals(now.fingerprint)
                    && action.checksum.equals(now.checksum);
        }

        public String canonical() {
            return identity(action).canonical
                    + "|rank="
                    + action.rank
                    + "|fingerprint="
                    + action.fingerprint
                    + "|payload="
                    + action.checksum
                    + "|surface="
                    + actionSpaceHash
                    + "|universe="
                    + identityUniverseHash
                    + "|frozenAt="
                    + frozenAt
                    + "|championHash="
                    + championHash;
        }
    }

    /** Generate all formal identities in position/lexicographic order. */
    public static ActionIdentity[] enumerateIdentityUniverse() {
        ActionIdentity[] out = new ActionIdentity[TOTAL_ACTIONS];
        int index = 0;
        for (int position = 1; position <= POSITIONS; position++) {
            for (int n1 = 1; n1 <= NUMBERS - 2; n1++) {
                for (int n2 = n1 + 1; n2 <= NUMBERS - 1; n2++) {
                    for (int n3 = n2 + 1; n3 <= NUMBERS; n3++) {
                        ActionIdentity id = new ActionIdentity(position, n1, n2, n3);
                        if (id.globalOrdinal != index) {
                            throw new IllegalStateException(
                                    "B10_IDENTITY_ORDINAL_MISMATCH_"
                                            + index
                                            + '_'
                                            + id.globalOrdinal);
                        }
                        out[index++] = id;
                    }
                }
            }
        }
        if (index != TOTAL_ACTIONS)
            throw new IllegalStateException("B10_IDENTITY_ENUMERATION_" + index);
        return out;
    }

    /**
     * Lexicographic ordinal within one position. Returns -1 for an invalid or non-canonical triple.
     */
    public static int combinationOrdinal(int n1, int n2, int n3) {
        if (!validTriple(n1, n2, n3)) return -1;
        int ordinal = 0;
        for (int a = 1; a <= NUMBERS - 2; a++) {
            for (int b = a + 1; b <= NUMBERS - 1; b++) {
                for (int c = b + 1; c <= NUMBERS; c++) {
                    if (a == n1 && b == n2 && c == n3) return ordinal;
                    ordinal++;
                }
            }
        }
        return -1;
    }

    public static int globalOrdinal(int position, int n1, int n2, int n3) {
        int local = combinationOrdinal(n1, n2, n3);
        if (position < 1 || position > POSITIONS || local < 0) return -1;
        return (position - 1) * COMBINATIONS_PER_POSITION + local;
    }

    public static ActionIdentity identity(V51Models.PositionAction action) {
        if (action == null) throw new IllegalArgumentException("B10_ACTION_IDENTITY_NULL");
        int ordinal = globalOrdinal(action.position, action.n1, action.n2, action.n3);
        if (ordinal < 0) throw new IllegalArgumentException("B10_ACTION_IDENTITY_INVALID");
        return new ActionIdentity(action.position, action.n1, action.n2, action.n3);
    }

    public static ValidationReport validateDetailed(V51Models.PositionAction[] actions) {
        ArrayList<ValidationFinding> findings = new ArrayList<ValidationFinding>();
        int rows = actions == null ? 0 : actions.length;
        int[] perPosition = new int[POSITIONS + 1];
        HashSet<String> identities = new HashSet<String>();
        StringBuilder payload = new StringBuilder(Math.max(4096, rows * 72));

        if (actions == null) {
            findings.add(new ValidationFinding(-1, "ACTION_ARRAY_NULL", "Expected 1200 rows"));
        } else if (actions.length != TOTAL_ACTIONS) {
            findings.add(
                    new ValidationFinding(
                            -1,
                            "ACTION_COUNT",
                            "expected=" + TOTAL_ACTIONS + "|actual=" + actions.length));
        }

        if (actions != null) {
            for (int i = 0; i < actions.length; i++) {
                V51Models.PositionAction action = actions[i];
                if (action == null) {
                    findings.add(new ValidationFinding(i, "ACTION_NULL", "row is null"));
                    continue;
                }
                if (action.position < 1 || action.position > POSITIONS) {
                    findings.add(
                            new ValidationFinding(
                                    i, "POSITION_RANGE", String.valueOf(action.position)));
                    continue;
                }
                perPosition[action.position]++;
                if (!validTriple(action.n1, action.n2, action.n3)) {
                    findings.add(
                            new ValidationFinding(
                                    i,
                                    "NUMBER_TRIPLE",
                                    action.n1 + "," + action.n2 + "," + action.n3));
                    continue;
                }
                ActionIdentity id =
                        new ActionIdentity(action.position, action.n1, action.n2, action.n3);
                if (!identities.add(id.canonical)) {
                    findings.add(new ValidationFinding(i, "DUPLICATE", id.canonical));
                }
                if (!validFourthBoundary(action.n1, action.n2, action.n3, action.n4)) {
                    findings.add(
                            new ValidationFinding(i, "FOURTH_BOUNDARY_NUMBER", "n4=" + action.n4));
                }
                if (Double.isNaN(action.rawActionP)
                        || Double.isInfinite(action.rawActionP)
                        || action.rawActionP < 0.0d
                        || action.rawActionP > 1.0d) {
                    findings.add(
                            new ValidationFinding(
                                    i, "RAW_ACTION_P", String.valueOf(action.rawActionP)));
                }
                payload.append(
                        canonicalPayloadRow(
                                action.position,
                                action.n1,
                                action.n2,
                                action.n3,
                                action.n4,
                                action.rawActionP,
                                action.rankScore,
                                action.rank,
                                action.fingerprint,
                                action.checksum));
            }
        }

        for (int position = 1; position <= POSITIONS; position++) {
            if (perPosition[position] != COMBINATIONS_PER_POSITION) {
                findings.add(
                        new ValidationFinding(
                                -1,
                                "POSITION_CARDINALITY",
                                "position="
                                        + position
                                        + "|expected="
                                        + COMBINATIONS_PER_POSITION
                                        + "|actual="
                                        + perPosition[position]));
            }
        }

        if (identities.size() == TOTAL_ACTIONS) {
            ActionIdentity[] expected = enumerateIdentityUniverse();
            for (int i = 0; i < expected.length; i++) {
                if (!identities.contains(expected[i].canonical)) {
                    findings.add(
                            new ValidationFinding(-1, "MISSING_IDENTITY", expected[i].canonical));
                }
            }
        }

        return new ValidationReport(
                rows,
                identities.size(),
                perPosition,
                findings,
                expectedIdentityUniverseHash(),
                V51ChecksumEngine.sha256("B10_ACTION_PAYLOAD|" + payload.toString()));
    }

    public static void validateOrThrow(V51Models.PositionAction[] actions) {
        ValidationReport report = validateDetailed(actions);
        if (!report.valid) {
            String detail =
                    report.findings.length == 0
                            ? report.summary()
                            : report.findings[0].code + '|' + report.findings[0].detail;
            throw new IllegalStateException("B10_ACTION_SPACE_INVALID|" + detail);
        }
    }

    /** Hashes the complete model payload; deletion or mutation changes the hash. */
    public static String actionSpaceHash(V51Models.PositionAction[] actions) {
        ValidationReport report = validateDetailed(actions);
        if (!report.valid)
            throw new IllegalStateException("B10_ACTION_SPACE_HASH_INVALID|" + report.summary());
        return actionSpaceHash(report);
    }

    private static String actionSpaceHash(ValidationReport report) {
        return V51ChecksumEngine.sha256(
                "B10_ACTION_SPACE|"
                        + report.identityUniverseHash
                        + '|'
                        + report.payloadHash
                        + '|'
                        + report.inputRows);
    }

    /**
     * One canonical row shared by the live 1200-action audit and the durable snapshot verifier.
     * Keeping this serializer here prevents the two sides of the atomic-commit boundary from
     * silently inventing different payload hashes.
     */
    public static String canonicalPayloadRow(
            int position,
            int n1,
            int n2,
            int n3,
            int n4,
            double rawActionP,
            double rankScore,
            int rank,
            String fingerprint,
            String checksum) {
        int ordinal = globalOrdinal(position, n1, n2, n3);
        if (ordinal < 0)
            throw new IllegalArgumentException(
                    "B10_CANONICAL_PAYLOAD_IDENTITY_INVALID|position="
                            + position
                            + "|k3="
                            + n1
                            + ','
                            + n2
                            + ','
                            + n3);
        return ordinal
                + ":"
                + position
                + '|'
                + n1
                + '|'
                + n2
                + '|'
                + n3
                + '|'
                + n4
                + '|'
                + rawActionP
                + '|'
                + rankScore
                + '|'
                + rank
                + '|'
                + safe(fingerprint)
                + '|'
                + safe(checksum)
                + ';';
    }

    /** Recomputes the producer hash from the canonical rows stored in a deferred snapshot. */
    public static String actionSpaceHashFromPayloadBody(String payloadBody, int inputRows) {
        String payloadHash =
                V51ChecksumEngine.sha256("B10_ACTION_PAYLOAD|" + safe(payloadBody));
        return V51ChecksumEngine.sha256(
                "B10_ACTION_SPACE|"
                        + expectedIdentityUniverseHash()
                        + '|'
                        + payloadHash
                        + '|'
                        + inputRows);
    }

    /** Hash of identities only; it is stable across model scores and rankings. */
    public static String expectedIdentityUniverseHash() {
        ActionIdentity[] identities = enumerateIdentityUniverse();
        StringBuilder body = new StringBuilder(TOTAL_ACTIONS * 16);
        for (int i = 0; i < identities.length; i++)
            body.append(identities[i].globalOrdinal)
                    .append(':')
                    .append(identities[i].canonical)
                    .append(';');
        return V51ChecksumEngine.sha256("B10_IDENTITY_UNIVERSE|" + body.toString());
    }

    public FrozenChampion freeze(
            V51Models.PositionAction rankOne, V51Models.PositionAction[] all, long now) {
        if (rankOne == null) throw new IllegalArgumentException("B10_CHAMPION_NULL");
        ValidationReport validation = validateDetailed(all);
        if (!validation.valid)
            throw new IllegalStateException(
                    "B10_ACTION_SPACE_INVALID|" + validation.summary());
        if (rankOne.rank != 1)
            throw new IllegalArgumentException("B10_CHAMPION_RANK_NOT_ONE_" + rankOne.rank);
        int matches = 0;
        for (int i = 0; i < all.length; i++) {
            V51Models.PositionAction action = all[i];
            if (action != null && action.rank == 1) matches++;
        }
        if (matches != 1) throw new IllegalStateException("B10_CHAMPION_CARDINALITY_" + matches);
        return new FrozenChampion(rankOne, actionSpaceHash(validation), now);
    }

    /** Exact settlement predicate for one position; no cross-position or chase semantics. */
    public static boolean hit(V51Models.PositionAction action, int[] actualPermutation) {
        if (action == null || actualPermutation == null || actualPermutation.length != NUMBERS)
            return false;
        if (action.position < 1 || action.position > POSITIONS) return false;
        int actual = actualPermutation[action.position - 1];
        return actual == action.n1 || actual == action.n2 || actual == action.n3;
    }

    public static int choose(int n, int k) {
        if (n < 0 || k < 0 || k > n) return 0;
        if (k > n - k) k = n - k;
        long value = 1L;
        for (int i = 1; i <= k; i++) value = value * (n - k + i) / i;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    public static boolean hasDecisionAuthority() {
        return false;
    }

    public static boolean canDeleteOrPruneFormalActions() {
        return false;
    }

    public static boolean validTriple(int n1, int n2, int n3) {
        return n1 >= 1 && n1 < n2 && n2 < n3 && n3 <= NUMBERS;
    }

    /** n4 is the best excluded boundary number; it is evidence, not a fourth betting number. */
    public static boolean validFourthBoundary(int n1, int n2, int n3, int n4) {
        return validTriple(n1, n2, n3)
                && n4 >= 1
                && n4 <= NUMBERS
                && n4 != n1
                && n4 != n2
                && n4 != n3;
    }

    private static boolean allPositionsComplete(int[] rowsPerPosition) {
        if (rowsPerPosition == null || rowsPerPosition.length <= POSITIONS) return false;
        for (int position = 1; position <= POSITIONS; position++)
            if (rowsPerPosition[position] != COMBINATIONS_PER_POSITION) return false;
        return true;
    }

    private static int[] copy(int[] source) {
        if (source == null) return new int[0];
        int[] out = new int[source.length];
        System.arraycopy(source, 0, out, 0, source.length);
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
