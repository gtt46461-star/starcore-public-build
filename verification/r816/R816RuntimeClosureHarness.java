package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** Host regression for the exact failures observed on the R815 physical-device report. */
public final class R816RuntimeClosureHarness {
    private static int assertions;

    public static void main(String[] args) {
        providerTimeContract();
        contiguousSuffixContract();
        boundedCatchUpContract();
        System.out.println(
                "R816_RUNTIME_CLOSURE=PASS|ASSERTIONS="
                        + assertions
                        + "|UTC_DEVICE_INVARIANT,EXPLICIT_OFFSET,EPOCH_PARITY,STALE_GATE_RECOVERY,"
                        + "METADATA_SELF_HEAL,RECENT_SUFFIX_FREEZE,SHORT_SUFFIX_FAIL_FAST,"
                        + "DEFERRED_10_CHUNKS,SHADOW_LOCK");
    }

    private static void providerTimeContract() {
        TimeZone original = TimeZone.getDefault();
        long shanghai;
        long tokyo;
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
            shanghai = ProviderTimeCodecR816.parseText("2026-08-24 12:34:56");
            TimeZone.setDefault(TimeZone.getTimeZone("Asia/Tokyo"));
            tokyo = ProviderTimeCodecR816.parseText("2026-08-24 12:34:56");
        } finally {
            TimeZone.setDefault(original);
        }
        long utc = ProviderTimeCodecR816.parseText("2026-08-24T12:34:56Z");
        long offset = ProviderTimeCodecR816.parseText("2026-08-24T20:34:56+08:00");
        require(utc > 0L && shanghai == utc && tokyo == utc, "UTC_DEVICE_INVARIANT");
        require(offset == utc, "EXPLICIT_OFFSET_EQUIVALENT");
        require(
                ProviderTimeCodecR816.parse(Long.valueOf(utc / 1000L)) == utc
                        && ProviderTimeCodecR816.parse(String.valueOf(utc)) == utc,
                "EPOCH_SECONDS_MILLIS_PARITY");

        Models.Draw current = draw("202608240012", utc);
        ProviderFreshnessGateR3611.Decision fresh =
                ProviderFreshnessGateR3611.evaluate(current, "202608240013", utc + 60000L);
        Models.Draw shifted = draw("202608240012", utc - 8L * 60L * 60L * 1000L);
        ProviderFreshnessGateR3611.Decision stale =
                ProviderFreshnessGateR3611.evaluate(shifted, "202608240013", utc + 60000L);
        require(fresh.accepted && "FRESH".equals(fresh.code), "FRESH_AFTER_UTC_PARSE");
        require(!stale.accepted && "OPEN_TIME_STALE".equals(stale.code), "OLD_R815_SHIFT_REJECTED");
        require(
                ProviderTimeCodecR816.preferCorrectedOpenTime(
                                shifted.openTime, current.openTime, utc + 30000L)
                        == current.openTime,
                "INSTALLED_DB_METADATA_SELF_HEAL");
        require(
                ProviderTimeCodecR816.preferCorrectedOpenTime(
                                current.openTime, shifted.openTime, utc + 30000L)
                        == current.openTime,
                "GOOD_METADATA_NOT_REGRESSED");
    }

    private static void contiguousSuffixContract() {
        ArrayList<Models.Draw> gapped = new ArrayList<Models.Draw>();
        for (int sequence = 1; sequence <= 3; sequence++) {
            gapped.add(draw(issue(sequence), 1700000000000L + sequence * 60000L));
        }
        for (int sequence = 20; sequence <= 31; sequence++) {
            gapped.add(draw(issue(sequence), 1700000000000L + sequence * 60000L));
        }
        HistoryIntegrityVerifierR815.Result full =
                HistoryIntegrityVerifierR815.verifyAscending(gapped);
        require(full.gapCount == 1 && full.contiguousSuffix == 12, "OLD_PREFIX_GAP_OBSERVED");
        require(full.minimalReady(), "RECENT_SUFFIX_IS_USABLE");
        List<Models.Draw> suffix = full.contiguousSuffixAscending();
        require(
                suffix.size() == 12
                        && issue(20).equals(suffix.get(0).issue)
                        && issue(31).equals(suffix.get(11).issue),
                "EXACT_SUFFIX_BOUNDARY");
        AnalysisSingleFlightR36.Request request = request(issue(31), issue(32), 816L);
        V51RuntimeInputR36 frozen = V51RuntimeInputR36.freeze(gapped, request);
        HistoryIntegrityVerifierR815.Result frozenAudit =
                HistoryIntegrityVerifierR815.verifyAscending(frozen.history);
        require(
                frozen.history.size() == 12
                        && frozenAudit.gapCount == 0
                        && frozenAudit.contiguousSuffix == 12,
                "HEAVY_RUNTIME_RECEIVES_ONLY_CONTIGUOUS_SUFFIX");

        ArrayList<Models.Draw> tooShort = new ArrayList<Models.Draw>();
        for (int sequence = 1; sequence <= 12; sequence++) {
            tooShort.add(draw(issue(sequence), 1700000000000L + sequence * 60000L));
        }
        for (int sequence = 20; sequence <= 25; sequence++) {
            tooShort.add(draw(issue(sequence), 1700000000000L + sequence * 60000L));
        }
        boolean failedBeforeCompute = false;
        try {
            V51RuntimeInputR36.freeze(tooShort, request(issue(25), issue(26), 817L));
        } catch (IllegalStateException expected) {
            failedBeforeCompute =
                    expected.getMessage() != null
                            && expected.getMessage().startsWith("R817_HISTORY_SUFFIX_LT_12");
        }
        require(failedBeforeCompute, "SHORT_SUFFIX_FAILS_BEFORE_MODEL_ALLOCATION");
    }

    private static void boundedCatchUpContract() {
        require(
                AppConfig.R36_DEFERRED_ACTION_CHUNK == 120
                        && 1200 % AppConfig.R36_DEFERRED_ACTION_CHUNK == 0
                        && 1200 / AppConfig.R36_DEFERRED_ACTION_CHUNK == 10,
                "DEFERRED_EXACTLY_TEN_ACTION_CHUNKS");
        require(
                AppConfig.R815_SHADOW_ONLY
                        && !AppConfig.R815_MODEL_PROVEN
                        && AppConfig.R815_REAL_BET_HARD_DISABLED
                        && !AppConfig.R36_REAL_BET_UNLOCK,
                "REAL_REMAINS_HARD_LOCKED");
    }

    private static AnalysisSingleFlightR36.Request request(
            String base, String target, long generation) {
        long now = 1700000000000L + generation;
        return new AnalysisSingleFlightR36.Request(
                base,
                target,
                generation,
                now,
                now + 60000L,
                "R816_HOST",
                V51ChecksumEngine.sha256("R816_HOST|" + target + '|' + generation),
                "");
    }

    private static Models.Draw draw(String issue, long openTime) {
        Models.Draw draw = new Models.Draw();
        draw.issue = issue;
        draw.openTime = openTime;
        draw.fetchTime = openTime + 30000L;
        int shift = Integer.parseInt(issue.substring(issue.length() - 4)) % 10;
        for (int position = 0; position < 10; position++) {
            draw.numbers[position] = ((position + shift) % 10) + 1;
        }
        if (!draw.isValid()) throw new AssertionError("R816_FIXTURE_INVALID:" + issue);
        return draw;
    }

    private static String issue(int sequence) {
        return "20260824" + four(sequence);
    }

    private static String four(int value) {
        return value < 10
                ? "000" + value
                : value < 100 ? "00" + value : value < 1000 ? "0" + value : String.valueOf(value);
    }

    private static void require(boolean condition, String code) {
        assertions++;
        if (!condition) throw new AssertionError("R816_ASSERTION_FAILED:" + code);
    }
}
