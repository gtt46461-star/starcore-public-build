package com.ethk3.novaonepro;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Single owner for target issue advancement and cross-day rollover. */
public final class TargetIssueResolver {
    public static final int ISSUES_PER_DAY = 1440;

    public static final class Resolution {
        public final boolean valid;
        public final String baseIssue;
        public final String targetIssue;
        public final V50Enums.FailureCode failureCode;
        public final String detail;

        Resolution(
                boolean valid,
                String baseIssue,
                String targetIssue,
                V50Enums.FailureCode failureCode,
                String detail) {
            this.valid = valid;
            this.baseIssue = baseIssue == null ? "" : baseIssue;
            this.targetIssue = targetIssue == null ? "" : targetIssue;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = detail == null ? "" : detail;
        }
    }

    private TargetIssueResolver() {}

    // Period validation is hot in every history/number/action consumer. Parse each UTC
    // date once, with the original strict Gregorian formatter; keep memory bounded.
    private static final java.util.LinkedHashMap<String,Long> UTC_DAYS_CP12 =
            new java.util.LinkedHashMap<String,Long>(33,.75f,true);
    private static long cachedUtcDayCP12(String value) {
        synchronized(UTC_DAYS_CP12) {
            Long known=UTC_DAYS_CP12.get(value);
            if(known!=null)return known.longValue();
        }
        SimpleDateFormat format=new SimpleDateFormat("yyyyMMdd",Locale.US);
        format.setLenient(false);format.setTimeZone(TimeZone.getTimeZone("UTC"));
        long day;
        try { day=format.parse(value).getTime()/86400000L; }
        catch(ParseException invalid) { day=Long.MIN_VALUE; }
        synchronized(UTC_DAYS_CP12) {
            UTC_DAYS_CP12.put(value,day);
            while(UTC_DAYS_CP12.size()>32)UTC_DAYS_CP12.remove(UTC_DAYS_CP12.keySet().iterator().next());
        }
        return day;
    }
    private static String sequenceTextCP12(int sequence) {
        return new String(new char[]{(char)('0'+sequence/1000),(char)('0'+sequence/100%10),
                (char)('0'+sequence/10%10),(char)('0'+sequence%10)});
    }

    public static Resolution next(String baseIssue) {
        ParsedIssue parsed = parse(baseIssue);
        if (parsed == null) {
            return new Resolution(
                    false,
                    baseIssue,
                    "",
                    V50Enums.FailureCode.TARGET_ISSUE_MISMATCH,
                    "V50_BASE_ISSUE_INVALID");
        }
        int nextSequence = parsed.sequence + 1;
        String nextDate = parsed.date;
        if (nextSequence > ISSUES_PER_DAY) {
            nextSequence = 1;
            nextDate = addDays(parsed.date, 1);
            if (nextDate.length() != 8 || !validDate(nextDate)) {
                return new Resolution(
                        false,
                        baseIssue,
                        "",
                        V50Enums.FailureCode.TARGET_ISSUE_MISMATCH,
                        "V50_DATE_ROLLOVER_FAILED");
            }
        }
        String target = nextDate + sequenceTextCP12(nextSequence);
        return new Resolution(
                true, baseIssue, target, V50Enums.FailureCode.NONE, "V50_TARGET_ISSUE_OK");
    }

    /** One-minute current-game deadline from the durable source draw, never now+55s.
     * Different game identities require their own verified period contract. */
    public static long predrawDeadlineWallR20261008(Models.Draw draw) {
        if(!"Binance1-PK10".equals(AppConfig.LOTTERY_CODE)
                || !"Binance1-PK10".equals(DatabaseCoordinator.GAME_CODE))
            throw new IllegalStateException("F01_UNKNOWN_GAME_PERIOD_CONTRACT");
        if(draw==null||!draw.isValid()||!isValid(draw.issue)||draw.openTime<=0L)
            throw new IllegalArgumentException("F01_TARGET_CLOCK_INVALID");
        try { return Math.addExact(draw.openTime,60000L); }
        catch(ArithmeticException invalid){throw new IllegalArgumentException("F01_TARGET_CLOCK_OVERFLOW",invalid);}
    }

    public static Resolution exact(String baseIssue, String requestedTarget) {
        Resolution canonical = next(baseIssue);
        if (!canonical.valid) return canonical;
        if (!canonical.targetIssue.equals(requestedTarget == null ? "" : requestedTarget)) {
            return new Resolution(
                    false,
                    baseIssue,
                    requestedTarget,
                    V50Enums.FailureCode.TARGET_ISSUE_MISMATCH,
                    "V50_TARGET_EXPECTED=" + canonical.targetIssue);
        }
        return canonical;
    }

    public static boolean isValid(String issue) {
        return parse(issue) != null;
    }

    public static int compare(String left, String right) {
        ParsedIssue a = parse(left);
        ParsedIssue b = parse(right);
        if (a == null || b == null) throw new IllegalArgumentException("V50_ISSUE_COMPARE_INVALID");
        int d = distance(left, right);
        return d < 0 ? 1 : (d > 0 ? -1 : 0);
    }

    /** Signed issue distance, including the 1440-to-0001 UTC day rollover. */
    public static int distance(String earlier, String later) {
        ParsedIssue a = parse(earlier);
        ParsedIssue b = parse(later);
        if (a == null || b == null) return Integer.MIN_VALUE;
        long dayDelta = utcDayIndex(b.date) - utcDayIndex(a.date);
        long value = dayDelta * ISSUES_PER_DAY + (b.sequence - a.sequence);
        if (value > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        if (value < Integer.MIN_VALUE) return Integer.MIN_VALUE;
        return (int) value;
    }

    public static boolean isAfter(String candidate, String reference) {
        int d = distance(reference, candidate);
        return d != Integer.MIN_VALUE && d > 0;
    }

    public static ParsedIssue parse(String issue) {
        if (issue == null || issue.length() != 12) return null;
        for (int i = 0; i < issue.length(); i++) {
            char ch = issue.charAt(i);
            if (ch < '0' || ch > '9') return null;
        }
        String date = issue.substring(0, 8);
        int sequence;
        try {
            sequence = Integer.parseInt(issue.substring(8));
        } catch (Throwable e) {
            return null;
        }
        if (sequence < 1 || sequence > ISSUES_PER_DAY || !validDate(date)) return null;
        return new ParsedIssue(date, sequence);
    }

    private static long utcDayIndex(String value) {
        long day=cachedUtcDayCP12(value);
        return day==Long.MIN_VALUE?Long.MIN_VALUE/ISSUES_PER_DAY:day;
    }

    private static boolean validDate(String value) {
        return cachedUtcDayCP12(value)!=Long.MIN_VALUE;
    }

    private static String addDays(String value, int days) {
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd", Locale.US);
        format.setLenient(false);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        try {
            Date date = format.parse(value);
            Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"), Locale.US);
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, days);
            return format.format(calendar.getTime());
        } catch (Throwable e) {
            return "";
        }
    }

    public static final class ParsedIssue {
        public final String date;
        public final int sequence;

        ParsedIssue(String date, int sequence) {
            this.date = date;
            this.sequence = sequence;
        }

        public String canonical() {
            return date + sequenceTextCP12(sequence);
        }
    }
}
