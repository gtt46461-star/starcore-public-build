package com.ethk3.novaonepro;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/** B02开奖完整性、幂等去重、同期开奖冲突隔离、缺口恢复与确定性回放唯一Owner。 */
public final class DrawIntegrityRecoveryEngine {
    public enum GapKind {
        DATA_GAP,
        PREDRAW_COVERAGE_GAP
    }
    public static final String OWNER = "DrawIntegrityRecoveryEngine";
    public static final String VERSION = "STARCORE_TIANSHU_B02_DRAW_INTEGRITY_V1";
    public static final String GAME_CODE = "Binance1-PK10";
    public static final long DEFAULT_INTERVAL_MS = 60000L;
    public static final long MAX_FUTURE_SKEW_MS = 120000L;
    public static final long MAX_STALE_MS = 600000L;
    public static final int MAX_RECOVERY_ATTEMPTS = 8;
    public static final int MAX_GAPS_PER_PLAN = 256;

    public DrawIntegrityRecoveryEngine() {}

    public VerificationResult verifySingle(Models.Draw draw, String gameCode, long nowMs) {
        VerificationResult r = new VerificationResult();
        r.gameCode = safe(gameCode);
        r.checkedAt = nowMs;
        if (!GAME_CODE.equals(r.gameCode))
            return r.reject("GAME_CODE_MISMATCH", "开奖游戏不是Binance1-PK10");
        if (draw == null) return r.reject("DRAW_NULL", "开奖为空");
        r.issue = safe(draw.issue);
        r.providerIssue = r.issue;
        r.gmtIssue = r.issue;
        r.rawOpenTime = draw.openTime;
        r.providerOpenTime = draw.openTime;
        r.fetchTime = draw.fetchTime;
        r.sourceLine = normalizeSource(draw.sourceLine);
        r.numbers = copy(draw.numbers);
        if (r.issue.length() == 0) return r.reject("ISSUE_MISSING", "开奖缺少期号");
        if (!isIssueSafe(r.issue)) return r.reject("ISSUE_INVALID", "开奖期号格式非法");
        if (!isPermutation10(r.numbers))
            return r.reject("NUMBER_PERMUTATION_INVALID", "开奖号码必须是1至10完整排列");
        if (r.rawOpenTime < 0L || r.fetchTime < 0L) return r.reject("TIME_NEGATIVE", "开奖时间字段非法");
        if (nowMs > 0L && r.rawOpenTime > nowMs + MAX_FUTURE_SKEW_MS)
            return r.reject("OPEN_TIME_FUTURE", "开奖时间超前");
        if (!isAllowedSource(r.sourceLine)) return r.reject("SOURCE_LINE_INVALID", "开奖线路身份非法");
        r.estimatedIntervalMs = DEFAULT_INTERVAL_MS;
        r.targetIssue = nextIssue(r.issue);
        r.canonical =
                canonicalDraw(
                        r.gameCode, r.issue, r.rawOpenTime, r.fetchTime, r.sourceLine, r.numbers);
        r.canonicalHash = sha256(r.canonical);
        r.rawPayloadHash = sha256(safe(draw.rawJson));
        r.accepted = true;
        r.code = "DRAW_VERIFIED";
        r.publicMessage = "开奖完整性通过";
        return r;
    }

    public HistoryVerification verifyHistory(List<Models.Draw> input, String gameCode, long nowMs) {
        HistoryVerification out = new HistoryVerification();
        out.gameCode = safe(gameCode);
        out.checkedAt = nowMs;
        if (!GAME_CODE.equals(out.gameCode))
            return out.reject("GAME_CODE_MISMATCH", "历史游戏不是Binance1-PK10");
        if (input == null || input.size() == 0) return out.reject("HISTORY_EMPTY", "历史开奖为空");
        ArrayList<VerificationResult> accepted = new ArrayList<VerificationResult>();
        for (int i = 0; i < input.size(); i++) {
            VerificationResult r = verifySingle(input.get(i), gameCode, nowMs);
            out.inputCount++;
            if (!r.accepted) {
                out.invalidCount++;
                out.invalidResults.add(r);
                continue;
            }
            accepted.add(r);
        }
        Collections.sort(
                accepted,
                new Comparator<VerificationResult>() {
                    public int compare(VerificationResult a, VerificationResult b) {
                        return compareIssue(a.issue, b.issue);
                    }
                });
        HashMap<String, VerificationResult> unique = new HashMap<String, VerificationResult>();
        for (int i = 0; i < accepted.size(); i++) {
            VerificationResult current = accepted.get(i);
            String key = current.gameCode + "|" + current.issue;
            VerificationResult previous = unique.get(key);
            if (previous == null) {
                unique.put(key, current);
                continue;
            }
            if (previous.canonicalHash.equals(current.canonicalHash)) {
                out.duplicateCount++;
                out.duplicates.add(
                        new DuplicateRecord(key, current.canonicalHash, current.sourceLine));
            } else {
                out.conflictCount++;
                out.conflicts.add(
                        new ConflictRecord(
                                current.gameCode,
                                current.issue,
                                previous.canonicalHash,
                                current.canonicalHash,
                                previous.sourceLine,
                                current.sourceLine,
                                "SAME_ISSUE_DIFFERENT_CHECKSUM"));
            }
        }
        out.verified.addAll(unique.values());
        Collections.sort(
                out.verified,
                new Comparator<VerificationResult>() {
                    public int compare(VerificationResult a, VerificationResult b) {
                        return compareIssue(a.issue, b.issue);
                    }
                });
        for (int i = 1; i < out.verified.size(); i++) {
            VerificationResult previous = out.verified.get(i - 1);
            VerificationResult current = out.verified.get(i);
            Gap gap =
                    detectGap(
                            previous.issue,
                            current.issue,
                            previous.rawOpenTime,
                            current.rawOpenTime);
            if (gap != null) out.gaps.add(gap);
            if (current.rawOpenTime > 0L
                    && previous.rawOpenTime > 0L
                    && current.rawOpenTime < previous.rawOpenTime) out.timeDisorderCount++;
        }
        out.verifiedCount = out.verified.size();
        if (out.verifiedCount > 0) {
            out.firstIssue = out.verified.get(0).issue;
            out.latestIssue = out.verified.get(out.verifiedCount - 1).issue;
            out.targetIssue = nextIssue(out.latestIssue);
        }
        out.historyHash = historyHash(out.verified);
        out.accepted =
                out.verifiedCount > 0 && out.conflictCount == 0 && out.timeDisorderCount == 0;
        out.code =
                out.accepted
                        ? "HISTORY_VERIFIED"
                        : (out.conflictCount > 0 ? "HISTORY_CONFLICT" : "HISTORY_INVALID");
        out.publicMessage = out.accepted ? "历史开奖完整性通过" : "历史开奖存在硬冲突";
        return out;
    }

    public RecoveryPlan buildRecoveryPlan(HistoryVerification history, long nowMs) {
        return buildRecoveryPlan(history, null, nowMs);
    }

    /**
     * Builds data-repair requests separately from immutable predraw coverage gaps. Missing draws
     * may be fetched; a historical predraw canonical is never synthesized after its draw.
     */
    public RecoveryPlan buildRecoveryPlan(
            HistoryVerification history, java.util.Set<String> canonicalTargets, long nowMs) {
        RecoveryPlan plan = new RecoveryPlan();
        plan.createdAt = nowMs;
        plan.gameCode = history == null ? GAME_CODE : history.gameCode;
        if (history == null) return plan.block("HISTORY_NULL", "没有历史验证结果");
        if (history.conflictCount > 0) return plan.block("CONFLICT_FIRST", "同期开奖冲突必须先隔离");
        int limit = Math.min(MAX_GAPS_PER_PLAN, history.gaps.size());
        for (int i = 0; i < limit; i++) {
            Gap gap = history.gaps.get(i);
            plan.dataGaps.add(gap);
            String requestDate =
                    dateFromMillis(gap.estimatedStartTime > 0L ? gap.estimatedStartTime : nowMs);
            RecoveryRequest request =
                    new RecoveryRequest(
                            "B02-RECOVERY-" + sha256(gap.canonical()).substring(0, 16),
                            GAME_CODE,
                            gap.fromIssue,
                            gap.toIssue,
                            requestDate,
                            0,
                            nowMs,
                            "PENDING",
                            gap.gapSize,
                            gap.gapHash);
            plan.requests.add(request);
        }
        if (canonicalTargets != null) {
            HashSet<String> seen = new HashSet<String>(canonicalTargets);
            for (int i = 0; i < history.verified.size(); i++) {
                String target = history.verified.get(i).issue;
                if (!seen.contains(target)) {
                    plan.predrawCoverageGaps.add(
                            new CoverageGap(
                                    target,
                                    GapKind.PREDRAW_COVERAGE_GAP,
                                    "DRAW_EXISTS_WITHOUT_PRE_DRAW_CANONICAL"));
                }
            }
        }
        plan.state = plan.requests.size() == 0 ? "NO_GAP" : "READY";
        plan.accepted = true;
        plan.planHash = sha256(plan.canonicalWithoutHash());
        return plan;
    }

    public RecoveryResult applyRecovery(
            HistoryVerification before,
            List<Models.Draw> recovered,
            RecoveryRequest request,
            long nowMs) {
        RecoveryResult result = new RecoveryResult();
        result.request = request;
        result.startedAt = nowMs;
        if (before == null || request == null)
            return result.reject("RECOVERY_INPUT_NULL", "恢复输入为空");
        ArrayList<Models.Draw> merged = new ArrayList<Models.Draw>();
        for (int i = 0; i < before.verified.size(); i++) merged.add(toDraw(before.verified.get(i)));
        if (recovered != null) merged.addAll(recovered);
        HistoryVerification after = verifyHistory(merged, GAME_CODE, nowMs);
        result.after = after;
        result.beforeHistoryHash = before.historyHash;
        result.afterHistoryHash = after.historyHash;
        result.recoveredCount = Math.max(0, after.verifiedCount - before.verifiedCount);
        result.remainingGapCount = after.gaps.size();
        result.conflictCount = after.conflictCount;
        result.completedAt = nowMs;
        result.accepted = after.conflictCount == 0 && result.recoveredCount > 0;
        result.code =
                result.accepted
                        ? "RECOVERY_APPLIED"
                        : (after.conflictCount > 0 ? "RECOVERY_CONFLICT" : "RECOVERY_NO_PROGRESS");
        result.publicMessage = result.accepted ? "缺口恢复已应用" : "缺口恢复未形成有效进展";
        result.resultHash = sha256(result.canonicalWithoutHash());
        return result;
    }

    public TargetFreshness checkTargetFreshness(
            String currentIssue,
            String targetIssue,
            long providerOpenTime,
            long receivedAt,
            long nowMs) {
        TargetFreshness f = new TargetFreshness();
        f.currentIssue = safe(currentIssue);
        f.targetIssue = safe(targetIssue);
        f.providerOpenTime = providerOpenTime;
        f.receivedAt = receivedAt;
        f.nowMs = nowMs;
        f.ageMs = receivedAt <= 0L ? Long.MAX_VALUE : Math.max(0L, nowMs - receivedAt);
        f.untilOpenMs =
                providerOpenTime <= 0L ? 0L : providerOpenTime + DEFAULT_INTERVAL_MS - nowMs;
        if (f.currentIssue.length() == 0 || f.targetIssue.length() == 0)
            return f.reject("TARGET_ISSUE_EMPTY", "目标期缺失");
        if (!nextIssue(f.currentIssue).equals(f.targetIssue))
            return f.reject("TARGET_ISSUE_MISMATCH", "目标期不是当前期下一期");
        if (providerOpenTime > nowMs + MAX_FUTURE_SKEW_MS)
            return f.reject("PROVIDER_TIME_FUTURE", "开奖源时间超前");
        if (f.ageMs > MAX_STALE_MS) return f.reject("PROVIDER_DATA_STALE", "开奖源数据过旧");
        f.fresh = true;
        f.code = "TARGET_FRESH";
        f.publicMessage = "目标期与开奖源时钟一致";
        f.fingerprint = sha256(f.canonicalWithoutHash());
        return f;
    }

    public String replayHash(List<Models.Draw> history, long frozenNowMs) {
        HistoryVerification result = verifyHistory(history, GAME_CODE, frozenNowMs);
        return sha256(result.canonical());
    }

    public static boolean selfTest() {
        DrawIntegrityRecoveryEngine e = new DrawIntegrityRecoveryEngine();
        Models.Draw a = sample("202608050001", 1000L, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10});
        Models.Draw b = sample("202608050002", 61000L, new int[] {2, 1, 3, 4, 5, 6, 7, 8, 9, 10});
        Models.Draw c = sample("202608050004", 181000L, new int[] {3, 1, 2, 4, 5, 6, 7, 8, 9, 10});
        ArrayList<Models.Draw> list = new ArrayList<Models.Draw>();
        list.add(a);
        list.add(b);
        list.add(c);
        list.add(b.copy());
        HistoryVerification h1 = e.verifyHistory(list, GAME_CODE, 200000L);
        HistoryVerification h2 = e.verifyHistory(list, GAME_CODE, 200000L);
        if (!h1.accepted || h1.duplicateCount != 1 || h1.gaps.size() != 1) return false;
        if (!h1.historyHash.equals(h2.historyHash)) return false;
        RecoveryPlan p = e.buildRecoveryPlan(h1, 200000L);
        if (!p.accepted || p.requests.size() != 1) return false;
        TargetFreshness f =
                e.checkTargetFreshness("202608050004", "202608050005", 181000L, 190000L, 200000L);
        return f.fresh && e.ruleCatalog().size() >= 96;
    }

    private static Models.Draw sample(String issue, long openTime, int[] numbers) {
        Models.Draw d = new Models.Draw();
        d.issue = issue;
        d.openTime = openTime;
        d.fetchTime = openTime + 100L;
        d.sourceLine = "PRIMARY";
        d.numbers = numbers;
        d.rawJson = "{}";
        return d;
    }

    private static Models.Draw toDraw(VerificationResult v) {
        Models.Draw d = new Models.Draw();
        d.issue = v.issue;
        d.openTime = v.rawOpenTime;
        d.fetchTime = v.fetchTime;
        d.sourceLine = v.sourceLine;
        d.numbers = copy(v.numbers);
        d.rawJson = "";
        return d;
    }

    private static int[] copy(int[] values) {
        if (values == null) return new int[0];
        int[] out = new int[values.length];
        System.arraycopy(values, 0, out, 0, values.length);
        return out;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static String normalizeSource(String value) {
        String s = safe(value).trim().toUpperCase(Locale.US);
        if (s.indexOf("BACKUP") >= 0) return "BACKUP";
        if (s.indexOf("PRIMARY") >= 0 || s.length() == 0) return "PRIMARY";
        return s;
    }

    private static boolean isAllowedSource(String source) {
        return "PRIMARY".equals(source) || "BACKUP".equals(source);
    }

    private static boolean isIssueSafe(String issue) {
        if (issue == null || issue.length() == 0 || issue.length() > 64) return false;
        for (int i = 0; i < issue.length(); i++) {
            char c = issue.charAt(i);
            if (!Character.isDigit(c) && c != '-' && c != '_' && c != ':') return false;
        }
        return true;
    }

    private static boolean isPermutation10(int[] numbers) {
        if (numbers == null || numbers.length != 10) return false;
        boolean[] seen = new boolean[11];
        for (int i = 0; i < numbers.length; i++) {
            int n = numbers[i];
            if (n < 1 || n > 10 || seen[n]) return false;
            seen[n] = true;
        }
        return true;
    }

    private static String canonicalDraw(
            String game, String issue, long open, long fetch, String source, int[] numbers) {
        StringBuilder sb =
                new StringBuilder(game)
                        .append('|')
                        .append(issue)
                        .append('|')
                        .append(open)
                        .append('|')
                        .append(fetch)
                        .append('|')
                        .append(source);
        for (int i = 0; i < numbers.length; i++) sb.append('|').append(numbers[i]);
        return sb.toString();
    }

    private static String historyHash(List<VerificationResult> list) {
        StringBuilder sb = new StringBuilder(VERSION);
        for (int i = 0; i < list.size(); i++) sb.append('|').append(list.get(i).canonicalHash);
        return sha256(sb.toString());
    }

    private static int compareIssue(String a, String b) {
        String da = digits(a), db = digits(b);
        if (da.length() > 0 && db.length() > 0) {
            BigInteger x = new BigInteger(da), y = new BigInteger(db);
            int c = x.compareTo(y);
            if (c != 0) return c;
        }
        return safe(a).compareTo(safe(b));
    }

    private static String digits(String value) {
        StringBuilder sb = new StringBuilder();
        String s = safe(value);
        for (int i = 0; i < s.length(); i++)
            if (Character.isDigit(s.charAt(i))) sb.append(s.charAt(i));
        return sb.toString();
    }

    public static String nextIssue(String issue) {
        String s = safe(issue);
        String d = digits(s);
        if (d.length() == 0) return "";
        BigInteger next = new BigInteger(d).add(BigInteger.ONE);
        String n = next.toString();
        while (n.length() < d.length()) n = "0" + n;
        if (d.length() == s.length()) return n;
        int pos = s.lastIndexOf(d);
        return pos >= 0 ? s.substring(0, pos) + n + s.substring(pos + d.length()) : n;
    }

    private static Gap detectGap(String from, String to, long fromTime, long toTime) {
        String a = digits(from), b = digits(to);
        if (a.length() == 0 || b.length() == 0) return null;
        BigInteger x = new BigInteger(a), y = new BigInteger(b);
        BigInteger diff = y.subtract(x);
        if (diff.compareTo(BigInteger.ONE) <= 0) return null;
        BigInteger missing = diff.subtract(BigInteger.ONE);
        // diff > 1 above, so missing is positive. Avoid API31 intValueExact on minSDK21.
        int size = missing.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) > 0
                ? Integer.MAX_VALUE : missing.intValue();
        return new Gap(
                from,
                to,
                size,
                fromTime,
                toTime,
                fromTime > 0L ? fromTime + DEFAULT_INTERVAL_MS : 0L);
    }

    private static String dateFromMillis(long millis) {
        SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        return f.format(new Date(Math.max(0L, millis)));
    }

    public static String sha256(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] data = md.digest(safe(text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(data.length * 2);
            for (int i = 0; i < data.length; i++) {
                int x = data[i] & 0xff;
                if (x < 16) sb.append('0');
                sb.append(Integer.toHexString(x));
            }
            return sb.toString();
        } catch (Throwable e) {
            throw new IllegalStateException("B02_SHA256_UNAVAILABLE", e);
        }
    }

    public static final class VerificationResult {
        public boolean accepted;
        public String code,
                publicMessage,
                gameCode,
                providerIssue,
                gmtIssue,
                issue,
                targetIssue,
                sourceLine,
                canonical,
                canonicalHash,
                rawPayloadHash;
        public long rawOpenTime, providerOpenTime, fetchTime, checkedAt, estimatedIntervalMs;
        public int[] numbers;

        public VerificationResult() {
            accepted = false;
            code = "UNVERIFIED";
            publicMessage = "";
            gameCode = "";
            providerIssue = "";
            gmtIssue = "";
            issue = "";
            targetIssue = "";
            sourceLine = "";
            canonical = "";
            canonicalHash = "";
            rawPayloadHash = "";
            rawOpenTime = 0L;
            providerOpenTime = 0L;
            fetchTime = 0L;
            checkedAt = 0L;
            estimatedIntervalMs = DEFAULT_INTERVAL_MS;
            numbers = new int[0];
        }

        public VerificationResult reject(String c, String m) {
            accepted = false;
            code = safe(c);
            publicMessage = safe(m);
            canonical = canonicalDraw(gameCode, issue, rawOpenTime, fetchTime, sourceLine, numbers);
            canonicalHash = sha256(canonical + '|' + code);
            return this;
        }

        public String canonicalRecord() {
            return (accepted ? 1 : 0)
                    + "|"
                    + code
                    + "|"
                    + gameCode
                    + "|"
                    + issue
                    + "|"
                    + targetIssue
                    + "|"
                    + sourceLine
                    + "|"
                    + rawOpenTime
                    + "|"
                    + fetchTime
                    + "|"
                    + canonicalHash
                    + "|"
                    + rawPayloadHash;
        }
    }

    public static final class HistoryVerification {
        public boolean accepted;
        public String code,
                publicMessage,
                gameCode,
                firstIssue,
                latestIssue,
                targetIssue,
                historyHash;
        public long checkedAt;
        public int inputCount,
                verifiedCount,
                invalidCount,
                duplicateCount,
                conflictCount,
                timeDisorderCount;
        public final ArrayList<VerificationResult> verified = new ArrayList<VerificationResult>();
        public final ArrayList<VerificationResult> invalidResults =
                new ArrayList<VerificationResult>();
        public final ArrayList<DuplicateRecord> duplicates = new ArrayList<DuplicateRecord>();
        public final ArrayList<ConflictRecord> conflicts = new ArrayList<ConflictRecord>();
        public final ArrayList<Gap> gaps = new ArrayList<Gap>();

        public HistoryVerification() {
            accepted = false;
            code = "UNVERIFIED";
            publicMessage = "";
            gameCode = "";
            firstIssue = "";
            latestIssue = "";
            targetIssue = "";
            historyHash = "";
            checkedAt = 0L;
        }

        public HistoryVerification reject(String c, String m) {
            accepted = false;
            code = safe(c);
            publicMessage = safe(m);
            historyHash = sha256(canonical());
            return this;
        }

        public String canonical() {
            StringBuilder sb =
                    new StringBuilder(
                            (accepted ? 1 : 0)
                                    + "|"
                                    + code
                                    + "|"
                                    + gameCode
                                    + "|"
                                    + firstIssue
                                    + "|"
                                    + latestIssue
                                    + "|"
                                    + targetIssue
                                    + "|"
                                    + inputCount
                                    + "|"
                                    + verifiedCount
                                    + "|"
                                    + invalidCount
                                    + "|"
                                    + duplicateCount
                                    + "|"
                                    + conflictCount
                                    + "|"
                                    + timeDisorderCount);
            for (VerificationResult v : verified) sb.append('|').append(v.canonicalHash);
            for (Gap g : gaps) sb.append('|').append(g.gapHash);
            return sb.toString();
        }
    }

    public static final class DuplicateRecord {
        public final String key, checksum, sourceLine;

        public DuplicateRecord(String key, String checksum, String sourceLine) {
            this.key = safe(key);
            this.checksum = safe(checksum);
            this.sourceLine = safe(sourceLine);
        }

        public String canonical() {
            return key + '|' + checksum + '|' + sourceLine;
        }
    }

    public static final class ConflictRecord {
        public final String gameCode,
                issue,
                firstHash,
                secondHash,
                firstSource,
                secondSource,
                reason,
                conflictHash;

        public ConflictRecord(
                String gameCode,
                String issue,
                String firstHash,
                String secondHash,
                String firstSource,
                String secondSource,
                String reason) {
            this.gameCode = safe(gameCode);
            this.issue = safe(issue);
            this.firstHash = safe(firstHash);
            this.secondHash = safe(secondHash);
            this.firstSource = safe(firstSource);
            this.secondSource = safe(secondSource);
            this.reason = safe(reason);
            this.conflictHash = sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return gameCode
                    + '|'
                    + issue
                    + '|'
                    + firstHash
                    + '|'
                    + secondHash
                    + '|'
                    + firstSource
                    + '|'
                    + secondSource
                    + '|'
                    + reason;
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + conflictHash;
        }
    }

    public static final class Gap {
        public final GapKind kind;
        public final String fromIssue, toIssue;
        public final int gapSize;
        public final long fromOpenTime, toOpenTime, estimatedStartTime;
        public final String gapHash;

        public Gap(
                String fromIssue,
                String toIssue,
                int gapSize,
                long fromOpenTime,
                long toOpenTime,
                long estimatedStartTime) {
            this.fromIssue = safe(fromIssue);
            this.kind = GapKind.DATA_GAP;
            this.toIssue = safe(toIssue);
            this.gapSize = gapSize;
            this.fromOpenTime = fromOpenTime;
            this.toOpenTime = toOpenTime;
            this.estimatedStartTime = estimatedStartTime;
            this.gapHash = sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return fromIssue
                    + '|'
                    + kind
                    + '|'
                    + toIssue
                    + '|'
                    + gapSize
                    + '|'
                    + fromOpenTime
                    + '|'
                    + toOpenTime
                    + '|'
                    + estimatedStartTime;
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + gapHash;
        }
    }

    public static final class CoverageGap {
        public final String targetIssue;
        public final GapKind kind;
        public final String reason;
        public final String gapHash;

        CoverageGap(String targetIssue, GapKind kind, String reason) {
            this.targetIssue = safe(targetIssue);
            this.kind = kind == null ? GapKind.PREDRAW_COVERAGE_GAP : kind;
            this.reason = safe(reason);
            this.gapHash = sha256(this.targetIssue + '|' + this.kind + '|' + this.reason);
        }

        public String canonical() {
            return targetIssue + '|' + kind + '|' + reason + '|' + gapHash;
        }
    }

    public static final class RecoveryRequest {
        public final String recoveryId, gameCode, fromIssue, toIssue, requestDate, state, gapHash;
        public final int attempt, expectedCount;
        public final long nextAttemptAt;

        public RecoveryRequest(
                String recoveryId,
                String gameCode,
                String fromIssue,
                String toIssue,
                String requestDate,
                int attempt,
                long nextAttemptAt,
                String state,
                int expectedCount,
                String gapHash) {
            this.recoveryId = safe(recoveryId);
            this.gameCode = safe(gameCode);
            this.fromIssue = safe(fromIssue);
            this.toIssue = safe(toIssue);
            this.requestDate = safe(requestDate);
            this.attempt = attempt;
            this.nextAttemptAt = nextAttemptAt;
            this.state = safe(state);
            this.expectedCount = expectedCount;
            this.gapHash = safe(gapHash);
        }

        public long backoffMs() {
            long base = 15000L;
            int exp = Math.min(6, Math.max(0, attempt));
            return base * (1L << exp);
        }

        public RecoveryRequest nextAttempt(long nowMs, String nextState) {
            return new RecoveryRequest(
                    recoveryId,
                    gameCode,
                    fromIssue,
                    toIssue,
                    requestDate,
                    attempt + 1,
                    nowMs + backoffMs(),
                    nextState,
                    expectedCount,
                    gapHash);
        }

        public String canonical() {
            return recoveryId
                    + '|'
                    + gameCode
                    + '|'
                    + fromIssue
                    + '|'
                    + toIssue
                    + '|'
                    + requestDate
                    + '|'
                    + attempt
                    + '|'
                    + nextAttemptAt
                    + '|'
                    + state
                    + '|'
                    + expectedCount
                    + '|'
                    + gapHash;
        }
    }

    public static final class RecoveryPlan {
        public boolean accepted;
        public String code, publicMessage, gameCode, state, planHash;
        public long createdAt;
        public final ArrayList<RecoveryRequest> requests = new ArrayList<RecoveryRequest>();
        public final ArrayList<Gap> dataGaps = new ArrayList<Gap>();
        public final ArrayList<CoverageGap> predrawCoverageGaps =
                new ArrayList<CoverageGap>();

        public RecoveryPlan() {
            accepted = false;
            code = "UNPLANNED";
            publicMessage = "";
            gameCode = GAME_CODE;
            state = "NEW";
            planHash = "";
            createdAt = 0L;
        }

        public RecoveryPlan block(String c, String m) {
            accepted = false;
            code = safe(c);
            publicMessage = safe(m);
            state = "BLOCKED";
            planHash = sha256(canonicalWithoutHash());
            return this;
        }

        private String canonicalWithoutHash() {
            StringBuilder sb =
                    new StringBuilder(
                            (accepted ? 1 : 0)
                                    + "|"
                                    + code
                                    + "|"
                                    + gameCode
                                    + "|"
                                    + state
                                    + "|"
                                    + createdAt);
            for (RecoveryRequest r : requests) sb.append('|').append(r.canonical());
            for (CoverageGap g : predrawCoverageGaps) sb.append('|').append(g.canonical());
            return sb.toString();
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + planHash;
        }
    }

    public static final class RecoveryResult {
        public boolean accepted;
        public String code, publicMessage, beforeHistoryHash, afterHistoryHash, resultHash;
        public RecoveryRequest request;
        public HistoryVerification after;
        public int recoveredCount, remainingGapCount, conflictCount;
        public long startedAt, completedAt;

        public RecoveryResult() {
            accepted = false;
            code = "UNAPPLIED";
            publicMessage = "";
            beforeHistoryHash = "";
            afterHistoryHash = "";
            resultHash = "";
            recoveredCount = 0;
            remainingGapCount = 0;
            conflictCount = 0;
            startedAt = 0L;
            completedAt = 0L;
        }

        public RecoveryResult reject(String c, String m) {
            accepted = false;
            code = safe(c);
            publicMessage = safe(m);
            resultHash = sha256(canonicalWithoutHash());
            return this;
        }

        private String canonicalWithoutHash() {
            return (accepted ? 1 : 0)
                    + "|"
                    + code
                    + "|"
                    + beforeHistoryHash
                    + "|"
                    + afterHistoryHash
                    + "|"
                    + recoveredCount
                    + "|"
                    + remainingGapCount
                    + "|"
                    + conflictCount
                    + "|"
                    + startedAt
                    + "|"
                    + completedAt
                    + "|"
                    + (request == null ? "" : request.canonical());
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + resultHash;
        }
    }

    public static final class TargetFreshness {
        public boolean fresh;
        public String code, publicMessage, currentIssue, targetIssue, fingerprint;
        public long providerOpenTime, receivedAt, nowMs, ageMs, untilOpenMs;

        public TargetFreshness() {
            fresh = false;
            code = "UNVERIFIED";
            publicMessage = "";
            currentIssue = "";
            targetIssue = "";
            fingerprint = "";
            providerOpenTime = 0L;
            receivedAt = 0L;
            nowMs = 0L;
            ageMs = Long.MAX_VALUE;
            untilOpenMs = 0L;
        }

        public TargetFreshness reject(String c, String m) {
            fresh = false;
            code = safe(c);
            publicMessage = safe(m);
            fingerprint = sha256(canonicalWithoutHash());
            return this;
        }

        private String canonicalWithoutHash() {
            return (fresh ? 1 : 0)
                    + "|"
                    + code
                    + "|"
                    + currentIssue
                    + "|"
                    + targetIssue
                    + "|"
                    + providerOpenTime
                    + "|"
                    + receivedAt
                    + "|"
                    + nowMs
                    + "|"
                    + ageMs
                    + "|"
                    + untilOpenMs;
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + fingerprint;
        }
    }

    public static final class RuleDescriptor {
        public final String id, category, input, output, failureMode, owner;
        public final boolean hard;

        public RuleDescriptor(
                String id,
                String category,
                String input,
                String output,
                String failureMode,
                String owner,
                boolean hard) {
            this.id = safe(id);
            this.category = safe(category);
            this.input = safe(input);
            this.output = safe(output);
            this.failureMode = safe(failureMode);
            this.owner = safe(owner);
            this.hard = hard;
        }

        public String canonical() {
            return id
                    + ':'
                    + category
                    + ':'
                    + input
                    + ':'
                    + output
                    + ':'
                    + failureMode
                    + ':'
                    + owner
                    + ':'
                    + (hard ? 1 : 0);
        }
    }

    public List<RuleDescriptor> ruleCatalog() {
        ArrayList<RuleDescriptor> out = new ArrayList<RuleDescriptor>();
        out.add(
                new RuleDescriptor(
                        "B02-RULE-001",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-002",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-003",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-004",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-005",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-006",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-007",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-008",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-009",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-010",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-011",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-012",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-013",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-014",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-015",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-016", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-017",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-018",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-019",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-020",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-021",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-022",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-023",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-024",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-025",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-026",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-027",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-028",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-029",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-030",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-031",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-032",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-033",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-034",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-035",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-036",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-037",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-038",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-039",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-040",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-041",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-042",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-043",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-044",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-045",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-046",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-047",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-048",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-049",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-050",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-051",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-052",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-053",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-054",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-055",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-056",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-057",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-058",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-059",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-060",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-061",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-062",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-063",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-064",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-065",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-066",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-067",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-068",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-069",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-070",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-071",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-072",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-073",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-074",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-075",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-076", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-077",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-078",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-079",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-080",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-081",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-082",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-083",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-084",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-085",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-086",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-087",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-088",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-089",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-090",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-091",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-092",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-093",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-094",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-095",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-096",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-097",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-098",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-099",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-100",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-101",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-102",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-103",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-104",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-105",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-106",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-107",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-108",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-109",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-110",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-111",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-112",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-113",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-114",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-115",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-116",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-117",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-118",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-119",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-120",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-121",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-122",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-123",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-124",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-125",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-126",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-127",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-128",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-129",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-130",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-131",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-132",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-133",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-134",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-135",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-136", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-137",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-138",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-139",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-140",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-141",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-142",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-143",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-144",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-145",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-146",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-147",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-148",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-149",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-150",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-151",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-152",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-153",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-154",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-155",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-156",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-157",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-158",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-159",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-160",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-161",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-162",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-163",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-164",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-165",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-166",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-167",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-168",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-169",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-170",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-171",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-172",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-173",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-174",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-175",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-176",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-177",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-178",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-179",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-180",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-181",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-182",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-183",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-184",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-185",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-186",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-187",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-188",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-189",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-190",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-191",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-192",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-193",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-194",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-195",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-196", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-197",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-198",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-199",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-200",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-201",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-202",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-203",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-204",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-205",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-206",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-207",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-208",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-209",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-210",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-211",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-212",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-213",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-214",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-215",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-216",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-217",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-218",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-219",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-220",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-221",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-222",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-223",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-224",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-225",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-226",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-227",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-228",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-229",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-230",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-231",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-232",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-233",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-234",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-235",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-236",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-237",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-238",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-239",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-240",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-241",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-242",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-243",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-244",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-245",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-246",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-247",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-248",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-249",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-250",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-251",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-252",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-253",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-254",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-255",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-256", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-257",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-258",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-259",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-260",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-261",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-262",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-263",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-264",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-265",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-266",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-267",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-268",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-269",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-270",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-271",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-272",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-273",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-274",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-275",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-276",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-277",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-278",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-279",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-280",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-281",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-282",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-283",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-284",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-285",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-286",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-287",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-288",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-289",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-290",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-291",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-292",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-293",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-294",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-295",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-296",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-297",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-298",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-299",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-300",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-301",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-302",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-303",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-304",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-305",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-306",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-307",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-308",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-309",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-310",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-311",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-312",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-313",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-314",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-315",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-316", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-317",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-318",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-319",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-320",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-321",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-322",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-323",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-324",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-325",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-326",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-327",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-328",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-329",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-330",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-331",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-332",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-333",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-334",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-335",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-336",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-337",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-338",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-339",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-340",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-341",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-342",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-343",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-344",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-345",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-346",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-347",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-348",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-349",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-350",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-351",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-352",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-353",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-354",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-355",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-356",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-357",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-358",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-359",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-360",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-361",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-362",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-363",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-364",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-365",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-366",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-367",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-368",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-369",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-370",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-371",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-372",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-373",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-374",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-375",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-376", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-377",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-378",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-379",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-380",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-381",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-382",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-383",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-384",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-385",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-386",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-387",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-388",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-389",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-390",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-391",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-392",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-393",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-394",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-395",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-396",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-397",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-398",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-399",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-400",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-401",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-402",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-403",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-404",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-405",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-406",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-407",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-408",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-409",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-410",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-411",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-412",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-413",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-414",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-415",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-416",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-417",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-418",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-419",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-420",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-421",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-422",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-423",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-424",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-425",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-426",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-427",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-428",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-429",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-430",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-431",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-432",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-433",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-434",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-435",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-436", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-437",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-438",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-439",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-440",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-441",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-442",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-443",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-444",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-445",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-446",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-447",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-448",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-449",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-450",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-451",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-452",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-453",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-454",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-455",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-456",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-457",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-458",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-459",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-460",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-461",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-462",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-463",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-464",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-465",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-466",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-467",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-468",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-469",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-470",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-471",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-472",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-473",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-474",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-475",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-476",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-477",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-478",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-479",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-480",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-481",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-482",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-483",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-484",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-485",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-486",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-487",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-488",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-489",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-490",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-491",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-492",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-493",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-494",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-495",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-496", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-497",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-498",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-499",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-500",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-501",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-502",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-503",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-504",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-505",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-506",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-507",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-508",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-509",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-510",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-511",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-512",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-513",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-514",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-515",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-516",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-517",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-518",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-519",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-520",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-521",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-522",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-523",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-524",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-525",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-526",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-527",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-528",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-529",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-530",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-531",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-532",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-533",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-534",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-535",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-536",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-537",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-538",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-539",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-540",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-541",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-542",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-543",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-544",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-545",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-546",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-547",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-548",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-549",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-550",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-551",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-552",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-553",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-554",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-555",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-556", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-557",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-558",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-559",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-560",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-561",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-562",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-563",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-564",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-565",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-566",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-567",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-568",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-569",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-570",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-571",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-572",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-573",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-574",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-575",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-576",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-577",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-578",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-579",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-580",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-581",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-582",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-583",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-584",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-585",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-586",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-587",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-588",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-589",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-590",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-591",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-592",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-593",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-594",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-595",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-596",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-597",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-598",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-599",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-600",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-601",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-602",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-603",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-604",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-605",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-606",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-607",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-608",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-609",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-610",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-611",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-612",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-613",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-614",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-615",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-616", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-617",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-618",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-619",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-620",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-621",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-622",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-623",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-624",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-625",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-626",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-627",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-628",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-629",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-630",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-631",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-632",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-633",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-634",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-635",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-636",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-637",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-638",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-639",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-640",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-641",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-642",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-643",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-644",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-645",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-646",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-647",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-648",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-649",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-650",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-651",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-652",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-653",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-654",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-655",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-656",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-657",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-658",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-659",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-660",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-661",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-662",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-663",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-664",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-665",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-666",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-667",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-668",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-669",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-670",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-671",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-672",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-673",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-674",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-675",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-676", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-677",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-678",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-679",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-680",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-681",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-682",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-683",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-684",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-685",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-686",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-687",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-688",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-689",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-690",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-691",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-692",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-693",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-694",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-695",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-696",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-697",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-698",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-699",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-700",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-701",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-702",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-703",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-704",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-705",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-706",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-707",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-708",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-709",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-710",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-711",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-712",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-713",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-714",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-715",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-716",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-717",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-718",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-719",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-720",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-721",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-722",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-723",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-724",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-725",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-726",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-727",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-728",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-729",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-730",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-731",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-732",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-733",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-734",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-735",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-736", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-737",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-738",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-739",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-740",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-741",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-742",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-743",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-744",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-745",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-746",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-747",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-748",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-749",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-750",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-751",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-752",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-753",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-754",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-755",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-756",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-757",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-758",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-759",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-760",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-761",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-762",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-763",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-764",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-765",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-766",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-767",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-768",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-769",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-770",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-771",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-772",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-773",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-774",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-775",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-776",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-777",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-778",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-779",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-780",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-781",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-782",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-783",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-784",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-785",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-786",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-787",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-788",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-789",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-790",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-791",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-792",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-793",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-794",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-795",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-796", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-797",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-798",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-799",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-800",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-801",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-802",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-803",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-804",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-805",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-806",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-807",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-808",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-809",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-810",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-811",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-812",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-813",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-814",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-815",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-816",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-817",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-818",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-819",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-820",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-821",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-822",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-823",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-824",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-825",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-826",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-827",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-828",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-829",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-830",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-831",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-832",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-833",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-834",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-835",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-836",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-837",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-838",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-839",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-840",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-841",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-842",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-843",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-844",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-845",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-846",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-847",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-848",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-849",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-850",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-851",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-852",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-853",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-854",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-855",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-856", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-857",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-858",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-859",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-860",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-861",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-862",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-863",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-864",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-865",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-866",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-867",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-868",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-869",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-870",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-871",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-872",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-873",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-874",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-875",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-876",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-877",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-878",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-879",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-880",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-881",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-882",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-883",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-884",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-885",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-886",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-887",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-888",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-889",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-890",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-891",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-892",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-893",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-894",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-895",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-896",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-897",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-898",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-899",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-900",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-901",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-902",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-903",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-904",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-905",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-906",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-907",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-908",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-909",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-910",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-911",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-912",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-913",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-914",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-915",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-916", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-917",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-918",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-919",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-920",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-921",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-922",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-923",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-924",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-925",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-926",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-927",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-928",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-929",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-930",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-931",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-932",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-933",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-934",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-935",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-936",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-937",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-938",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-939",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-940",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-941",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-942",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-943",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-944",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-945",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-946",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-947",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-948",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-949",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-950",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-951",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-952",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-953",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-954",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-955",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-956",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-957",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-958",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-959",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-960",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-961",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-962",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-963",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-964",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-965",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-966",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-967",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-968",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-969",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-970",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-971",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-972",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-973",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-974",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-975",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-976", "TIME", "issue", "VERIFIED_ISSUE", "AUDIT", OWNER, false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-977",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-978",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-979",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-980",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-981",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-982",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-983",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-984",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-985",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-986",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-987",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-988",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-989",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-990",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-991",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-992",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-993",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-994",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-995",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-996",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-997",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-998",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-999",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1000",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1001",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1002",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1003",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1004",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1005",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1006",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1007",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1008",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1009",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1010",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1011",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1012",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1013",
                        "SOURCE",
                        "numbers",
                        "VERIFIED_NUMBERS",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1014",
                        "CHECKSUM",
                        "rawOpenTime",
                        "VERIFIED_RAWOPENTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1015",
                        "DEDUP",
                        "fetchTime",
                        "VERIFIED_FETCHTIME",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1016",
                        "CONFLICT",
                        "sourceLine",
                        "VERIFIED_SOURCELINE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1017",
                        "GAP",
                        "rawJson",
                        "VERIFIED_RAWJSON",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1018",
                        "RECOVERY",
                        "canonicalHash",
                        "VERIFIED_CANONICALHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1019",
                        "REPLAY",
                        "historyHash",
                        "VERIFIED_HISTORYHASH",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1020",
                        "SECURITY",
                        "targetIssue",
                        "VERIFIED_TARGETISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1021",
                        "IDENTITY",
                        "gameCode",
                        "VERIFIED_GAMECODE",
                        "AUDIT",
                        OWNER,
                        false));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1022",
                        "ISSUE",
                        "providerIssue",
                        "VERIFIED_PROVIDERISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1023",
                        "NUMBER",
                        "gmtIssue",
                        "VERIFIED_GMTISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        out.add(
                new RuleDescriptor(
                        "B02-RULE-1024",
                        "TIME",
                        "issue",
                        "VERIFIED_ISSUE",
                        "HOLD_OR_SHADOW",
                        OWNER,
                        true));
        return Collections.unmodifiableList(out);
    }

    public String ruleCatalogHash() {
        StringBuilder sb = new StringBuilder(VERSION);
        for (RuleDescriptor r : ruleCatalog()) sb.append('|').append(r.canonical());
        return sha256(sb.toString());
    }
}
