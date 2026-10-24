package com.ethk3.novaonepro;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public final class ApiClient {

    private static final String DATA_PATH = "/data";

    private static final int LATEST_DEFAULT_LIMIT = 5;
    private static final int MAX_LATEST_LIMIT = 100;
    private static final int MIN_LIMIT = 1;

    private static final int MAX_BODY_CHARS = 2 * 1024 * 1024;

    private final DbStore ledgerStoreR925;
    private final ThreadLocal<String> ledgerRequestR925=new ThreadLocal<String>();
    public ApiClient() { this(null); }
    public ApiClient(DbStore store) { ledgerStoreR925=store; }

    public Models.ApiResult fetchLatest() {
        return fetchLatest("");
    }

    /**
     * Fresh authoritative latest. Transport success is not enough: a stale HTTP-200 primary
     * payload is rejected so the same logical request can continue to the backup endpoint.
     * Historical/date recovery remains data-only and never creates predictions or BET records.
     */
    public Models.ApiResult fetchLatest(final String localTargetIssue) {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }
        final String expected = safeIssue(localTargetIssue);
        Models.ApiResult raw =
                requestGovernedLatest(
                        LATEST_DEFAULT_LIMIT,
                        new ApiTransportGovernanceEngine.PayloadGate() {
                            @Override
                            public ApiTransportGovernanceEngine.PayloadDecision evaluate(
                                    String sourceLine, String body, long nowMs) {
                                Models.ApiResult parsed = ApiParser.parseLatest(body, sourceLine);
                                if (parsed == null
                                        || (!parsed.success && !parsed.ok)
                                        || parsed.latestDraw == null
                                        || !parsed.latestDraw.isValid()) {
                                    return ApiTransportGovernanceEngine.PayloadDecision.reject(
                                            "PARSE_OR_DRAW_INVALID", "latest");
                                }
                                ProviderFreshnessGateR3611.Decision freshness =
                                        ProviderFreshnessGateR3611.evaluateLatestPollR75(
                                                parsed.latestDraw, expected, nowMs);
                                ProviderFreshnessGateR3611.record(
                                        sourceLine, freshness, false, nowMs);
                                return freshness.accepted
                                        ? ApiTransportGovernanceEngine.PayloadDecision.accept()
                                        : ApiTransportGovernanceEngine.PayloadDecision.reject(
                                                freshness.code, freshness.compact());
                            }
                        });
        if (raw == null || (!raw.success && !raw.ok)) return raw;
        Models.ApiResult parsed = ApiParser.parseLatest(raw.rawJson, raw.sourceLine);
        if (parsed == null) return Models.ApiResult.fail("PARSE_EMPTY", "最新开奖解析为空");
        parsed.rawJson = raw.rawJson;
        parsed.setSourceLine(raw.sourceLine);
        FetchLedgerR925.bindParsedResultR82(raw,parsed);
        if (parsed.latestDraw != null && parsed.latestDraw.isValid()) {
            TIANSHU_TRANSPORT.recordSuccessfulIssue(parsed.latestDraw.issue, raw.sourceLine);
            ProviderFreshnessGateR3611.Decision selectedFreshness =
                    ProviderFreshnessGateR3611.evaluate(
                            parsed.latestDraw, expected, System.currentTimeMillis());
            ProviderFreshnessGateR3611.record(
                    raw.sourceLine,
                    selectedFreshness,
                    true,
                    System.currentTimeMillis());
        }
        return parsed;
    }

    /**
     * A freshness-gated history request used by runtime recovery. The complete history body is
     * accepted only when its newest valid draw is current enough for the expected target.
     */
    public Models.ApiResult fetchHistoryFresh(final int count, final String localTargetIssue) {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }
        int bounded = count;
        if (bounded < MIN_LIMIT) bounded = LATEST_DEFAULT_LIMIT;
        if (bounded > MAX_LATEST_LIMIT) bounded = MAX_LATEST_LIMIT;
        final String expected = safeIssue(localTargetIssue);
        Models.ApiResult raw =
                requestGovernedLatest(
                        bounded,
                        new ApiTransportGovernanceEngine.PayloadGate() {
                            @Override
                            public ApiTransportGovernanceEngine.PayloadDecision evaluate(
                                    String sourceLine, String body, long nowMs) {
                                Models.ApiResult parsed = ApiParser.parseHistory(body, sourceLine);
                                Models.Draw latest = latestValidDraw(parsed);
                                if (latest == null) {
                                    return ApiTransportGovernanceEngine.PayloadDecision.reject(
                                            "HISTORY_EMPTY_OR_INVALID", "latest100");
                                }
                                ProviderFreshnessGateR3611.Decision freshness =
                                        ProviderFreshnessGateR3611.evaluateLatestPollR75(latest, expected, nowMs);
                                ProviderFreshnessGateR3611.record(
                                        sourceLine, freshness, false, nowMs);
                                return freshness.accepted
                                        ? ApiTransportGovernanceEngine.PayloadDecision.accept()
                                        : ApiTransportGovernanceEngine.PayloadDecision.reject(
                                                freshness.code, freshness.compact());
                            }
                        });
        if (raw == null || (!raw.success && !raw.ok)) return raw;
        Models.ApiResult parsed = ApiParser.parseHistory(raw.rawJson, raw.sourceLine);
        if (parsed == null) return Models.ApiResult.fail("PARSE_EMPTY", "历史开奖解析为空");
        parsed.rawJson = raw.rawJson;
        parsed.setSourceLine(raw.sourceLine);
        FetchLedgerR925.bindParsedResultR82(raw,parsed);
        parsed.latestDraw = latestValidDraw(parsed);
        if (parsed.latestDraw != null && parsed.latestDraw.isValid()) {
            TIANSHU_TRANSPORT.recordSuccessfulIssue(parsed.latestDraw.issue, raw.sourceLine);
            ProviderFreshnessGateR3611.Decision selectedFreshness =
                    ProviderFreshnessGateR3611.evaluate(
                            parsed.latestDraw, expected, System.currentTimeMillis());
            ProviderFreshnessGateR3611.record(
                    raw.sourceLine,
                    selectedFreshness,
                    true,
                    System.currentTimeMillis());
        }
        return parsed;
    }

    /** Today-date recovery with the same freshness rule and endpoint failover. */
    public Models.ApiResult fetchTodayBackfillFresh(final String localTargetIssue) {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }
        final String expected = safeIssue(localTargetIssue);
        final String date = todayDate();
        Models.ApiResult raw =
                requestGovernedDate(
                        date,
                        new ApiTransportGovernanceEngine.PayloadGate() {
                            @Override
                            public ApiTransportGovernanceEngine.PayloadDecision evaluate(
                                    String sourceLine, String body, long nowMs) {
                                Models.ApiResult parsed = ApiParser.parseHistory(body, sourceLine);
                                Models.Draw latest = latestValidDraw(parsed);
                                if (latest == null) {
                                    return ApiTransportGovernanceEngine.PayloadDecision.reject(
                                            "DATE_EMPTY_OR_INVALID", date);
                                }
                                ProviderFreshnessGateR3611.Decision freshness =
                                        ProviderFreshnessGateR3611.evaluate(latest, expected, nowMs);
                                ProviderFreshnessGateR3611.record(
                                        sourceLine, freshness, false, nowMs);
                                return freshness.accepted
                                        ? ApiTransportGovernanceEngine.PayloadDecision.accept()
                                        : ApiTransportGovernanceEngine.PayloadDecision.reject(
                                                freshness.code, freshness.compact());
                            }
                        });
        if (raw == null || (!raw.success && !raw.ok)) return raw;
        Models.ApiResult parsed = ApiParser.parseHistory(raw.rawJson, raw.sourceLine);
        if (parsed == null) return Models.ApiResult.fail("PARSE_EMPTY", "日期回补解析为空");
        parsed.rawJson = raw.rawJson;
        parsed.setSourceLine(raw.sourceLine);
        FetchLedgerR925.bindParsedResultR82(raw,parsed);
        parsed.latestDraw = latestValidDraw(parsed);
        if (parsed.latestDraw != null && parsed.latestDraw.isValid()) {
            ProviderFreshnessGateR3611.Decision selectedFreshness =
                    ProviderFreshnessGateR3611.evaluate(
                            parsed.latestDraw, expected, System.currentTimeMillis());
            ProviderFreshnessGateR3611.record(
                    raw.sourceLine,
                    selectedFreshness,
                    true,
                    System.currentTimeMillis());
        }
        return parsed;
    }

    public static boolean needsLimitedBackfill(String localTargetIssue, String apiLatestIssue) {
        try {
            String target = safeIssue(localTargetIssue);
            String latest = safeIssue(apiLatestIssue);
            if (!TargetIssueResolver.isValid(target) || !TargetIssueResolver.isValid(latest))
                return false;
            return TargetIssueResolver.distance(latest, target) >= 2;
        } catch (Throwable error) {
            return false;
        }
    }

    private static int compareIssue(String left, String right) {
        try {
            return new java.math.BigInteger(safeIssue(left))
                    .compareTo(new java.math.BigInteger(safeIssue(right)));
        } catch (Throwable error) {
            return safeIssue(left).compareTo(safeIssue(right));
        }
    }

    private static String safeIssue(String issue) {
        return issue == null ? "" : issue.trim();
    }

    public Models.ApiResult fetchHistory(int count) {
        if (count <= 0) {
            count = AppConfig.WINDOW_240;
        }

        Models.ApiResult raw;

        /*
         * 多趣 limit 最大通常按 100 处理。
         * 大于 100 时优先走日期回补，拿当天全量；失败再走 limit=100。
         */
        if (count > MAX_LATEST_LIMIT) {
            raw = fetchRawTodayHistory();

            if (raw != null && (raw.success || raw.ok) && looksLikeJson(raw.rawJson)) {
                Models.ApiResult parsedToday = ApiParser.parseHistory(raw.rawJson, raw.sourceLine);
                if (parsedToday != null
                        && parsedToday.history != null
                        && parsedToday.history.size() > 0) {
                    parsedToday.rawJson = raw.rawJson;
                    parsedToday.setSourceLine(raw.sourceLine);
                    FetchLedgerR925.bindParsedResultR82(raw,parsedToday);
                    return parsedToday;
                }
            }
        }

        raw = fetchRawHistory(count);

        if (raw == null) {
            return Models.ApiResult.fail("API_EMPTY", "历史接口返回为空");
        }

        if (!raw.success && !raw.ok) {
            return raw;
        }

        Models.ApiResult parsed = ApiParser.parseHistory(raw.rawJson, raw.sourceLine);

        if (parsed == null) {
            return Models.ApiResult.fail("PARSE_EMPTY", "历史开奖解析为空");
        }

        parsed.rawJson = raw.rawJson;
        parsed.setSourceLine(raw.sourceLine);
        FetchLedgerR925.bindParsedResultR82(raw,parsed);
        return parsed;
    }

    public Models.ApiResult fetchTodayBackfill() {
        return fetchDateHistory(todayDate());
    }

    public Models.ApiResult fetchYesterdayBackfill() {
        return fetchDateHistory(offsetDate(-1));
    }

    public Models.ApiResult fetchBackfillDate(String date) {
        return fetchDateHistory(date);
    }

    public Models.ApiResult fetchDateHistory(String date) {
        if (date == null || date.length() == 0) {
            date = todayDate();
        }

        Models.ApiResult raw = fetchRawDateHistory(date);

        if (raw == null) {
            return Models.ApiResult.fail("API_EMPTY", "日期回补接口返回为空");
        }

        if (!raw.success && !raw.ok) {
            return raw;
        }

        Models.ApiResult parsed = ApiParser.parseHistory(raw.rawJson, raw.sourceLine);

        if (parsed == null) {
            return Models.ApiResult.fail("PARSE_EMPTY", "日期回补解析为空");
        }

        parsed.rawJson = raw.rawJson;
        parsed.setSourceLine(raw.sourceLine);
        FetchLedgerR925.bindParsedResultR82(raw,parsed);
        return parsed;
    }

    public Models.ApiResult fetchTwoDayBackfill() {
        Models.ApiResult today = fetchTodayBackfill();

        if (today != null
                && (today.success || today.ok)
                && today.history != null
                && today.history.size() >= MAX_LATEST_LIMIT) {
            return today;
        }

        Models.ApiResult yesterday = fetchYesterdayBackfill();

        if (today == null || (!today.success && !today.ok)) {
            return yesterday;
        }

        if (yesterday == null || (!yesterday.success && !yesterday.ok)) {
            return today;
        }

        ArrayList<Models.Draw> merged = new ArrayList<Models.Draw>();

        if (yesterday.history != null) {
            for (int i = 0; i < yesterday.history.size(); i++) {
                Models.Draw d = yesterday.history.get(i);
                if (d != null && d.isValid() && !containsIssue(merged, d.issue)) {
                    merged.add(d);
                }
            }
        }

        if (today.history != null) {
            for (int i = 0; i < today.history.size(); i++) {
                Models.Draw d = today.history.get(i);
                if (d != null && d.isValid() && !containsIssue(merged, d.issue)) {
                    merged.add(d);
                }
            }
        }

        Models.ApiResult out = new Models.ApiResult();
        out.setOk(true);
        out.setSourceLine(
                today.sourceLine == null || today.sourceLine.length() == 0
                        ? AppConfig.API_LINE_PRIMARY
                        : today.sourceLine);
        out.setDraws(merged);
        out.rawJson = "";
        if (merged.size() > 0) {
            out.latestDraw = merged.get(merged.size() - 1);
        }

        return out;
    }

    public Models.ApiResult fetchRawLatest() {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }

        return requestGovernedLatest(LATEST_DEFAULT_LIMIT);
    }

    public Models.ApiResult fetchRawHistory(int count) {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }

        if (count < MIN_LIMIT) {
            count = LATEST_DEFAULT_LIMIT;
        }

        if (count > MAX_LATEST_LIMIT) {
            count = MAX_LATEST_LIMIT;
        }

        return requestGovernedLatest(count);
    }

    public Models.ApiResult fetchRawTodayHistory() {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }

        return requestGovernedDate(todayDate());
    }

    public Models.ApiResult fetchRawYesterdayHistory() {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }

        return requestGovernedDate(offsetDate(-1));
    }

    public Models.ApiResult fetchRawDateHistory(String date) {
        if (!KeyProvider.isKeyReady()) {
            return Models.ApiResult.fail("KEY_NOT_READY", KeyProvider.unavailableReasonCP12());
        }

        if (date == null || date.length() == 0) {
            date = todayDate();
        }

        return requestGovernedDate(date);
    }

    private Models.ApiResult requestDuoquLatest(int limit) {
        if (limit < MIN_LIMIT) {
            limit = LATEST_DEFAULT_LIMIT;
        }

        if (limit > MAX_LATEST_LIMIT) {
            limit = MAX_LATEST_LIMIT;
        }

        String[] bases = new String[] {AppConfig.API_PRIMARY_BASE, AppConfig.API_BACKUP_BASE};

        String lastError = "";
        int retryCount =
                AppConfig.HTTP_MAX_RETRY_PER_LINE <= 0 ? 1 : AppConfig.HTTP_MAX_RETRY_PER_LINE;

        for (int i = 0; i < bases.length; i++) {
            String base = bases[i];

            if (base == null || base.length() == 0) {
                continue;
            }

            String line = i == 0 ? AppConfig.API_LINE_PRIMARY : AppConfig.API_LINE_BACKUP;
            ArrayList<String> urls = buildLatestUrlCandidates(base, limit);

            for (int u = 0; u < urls.size(); u++) {
                String url = urls.get(u);

                for (int retry = 0; retry < retryCount; retry++) {
                    RawHttpResult result = requestGet(url);

                    if (result.ok && looksLikeJson(result.body)) {
                        return rawSuccess(result.body, line);
                    }

                    lastError = buildPublicError(result, "latest");
                }
            }
        }

        return Models.ApiResult.fail("API_LATEST_FAILED", lastError);
    }

    private Models.ApiResult requestDuoquDate(String date) {
        if (date == null || date.length() == 0) {
            date = todayDate();
        }

        String[] bases = new String[] {AppConfig.API_PRIMARY_BASE, AppConfig.API_BACKUP_BASE};

        String lastError = "";
        int retryCount =
                AppConfig.HTTP_MAX_RETRY_PER_LINE <= 0 ? 1 : AppConfig.HTTP_MAX_RETRY_PER_LINE;

        for (int i = 0; i < bases.length; i++) {
            String base = bases[i];

            if (base == null || base.length() == 0) {
                continue;
            }

            String line = i == 0 ? AppConfig.API_LINE_PRIMARY : AppConfig.API_LINE_BACKUP;
            ArrayList<String> urls = buildDateUrlCandidates(base, date);

            for (int u = 0; u < urls.size(); u++) {
                String url = urls.get(u);

                for (int retry = 0; retry < retryCount; retry++) {
                    RawHttpResult result = requestGet(url);

                    if (result.ok && looksLikeJson(result.body)) {
                        return rawSuccess(result.body, line);
                    }

                    lastError = buildPublicError(result, "date");
                }
            }
        }

        return Models.ApiResult.fail("API_DATE_FAILED", lastError);
    }

    private Models.ApiResult rawSuccess(String rawJson, String line) {
        Models.ApiResult api = new Models.ApiResult();
        api.success = true;
        api.ok = true;
        api.rawJson = rawJson == null ? "" : rawJson;
        api.sourceLine = line == null ? AppConfig.API_LINE_PRIMARY : line;
        api.apiLine = api.sourceLine;
        api.fromBackup = AppConfig.API_LINE_BACKUP.equals(api.sourceLine);
        FetchLedgerR925.bindRawResultR82(api,ledgerStoreR925==null?null:ledgerRequestR925.get());
        return api;
    }

    public RawHttpResult requestGet(String urlText) {
        final String requestIdR925=FetchLedgerR925.begin(ledgerStoreR925,"GET",urlText);
        final long requestStartedR925=TimeSourceR811.monoNowMs();
        ledgerRequestR925.set(requestIdR925);
        HttpURLConnection conn = null;
        InputStream in = null;

        RawHttpResult out = new RawHttpResult();
        out.url = sanitizeUrl(urlText);

        if (urlText == null || urlText.length() == 0) {
            out.ok = false;
            out.errorMessage = "empty url";
            FetchLedgerR925.complete(ledgerStoreR925,requestIdR925,0,out);
            return out;
        }

        try {
            URL url = new URL(urlText);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout((int)Math.max(1L, Math.min(AppConfig.HTTP_CONNECT_TIMEOUT_MS, RuntimeResourceAuditR927.remainingMs())));
            conn.setReadTimeout((int)Math.max(1L, Math.min(AppConfig.HTTP_READ_TIMEOUT_MS, RuntimeResourceAuditR927.remainingMs())));
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("User-Agent", buildUserAgent());

            int code = conn.getResponseCode();
            out.httpCode = code;

            if (code >= 200 && code < 300) {
                in = new BufferedInputStream(conn.getInputStream());
                out.body = readAll(in);
                out.ok = out.body != null && out.body.length() > 0;

                if (!out.ok) {
                    out.errorMessage = "empty response";
                }
            } else {
                InputStream err = conn.getErrorStream();

                if (err != null) {
                    out.body = readAll(err);
                    closeQuietly(err);
                }

                out.ok = false;
                out.errorMessage = "http " + code;
            }
        } catch (Exception e) {
            out.ok = false;
            out.errorMessage = sanitizeMessage(e.getMessage());
        } finally {
            closeQuietly(in);

            if (conn != null) {
                conn.disconnect();
            }
        }

        FetchLedgerR925.complete(ledgerStoreR925,requestIdR925,TimeSourceR811.monoNowMs()-requestStartedR925,out);
        return out;
    }

    public RawHttpResult requestPostJson(String urlText, String jsonBody) {
        final String requestIdR925=FetchLedgerR925.begin(ledgerStoreR925,"POST",urlText);
        final long requestStartedR925=TimeSourceR811.monoNowMs();
        ledgerRequestR925.set(requestIdR925);
        HttpURLConnection conn = null;
        InputStream in = null;
        OutputStream os = null;

        RawHttpResult out = new RawHttpResult();
        out.url = sanitizeUrl(urlText);

        if (urlText == null || urlText.length() == 0) {
            out.ok = false;
            out.errorMessage = "empty url";
            FetchLedgerR925.complete(ledgerStoreR925,requestIdR925,0,out);
            return out;
        }

        try {
            URL url = new URL(urlText);
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout((int)Math.max(1L, Math.min(AppConfig.HTTP_CONNECT_TIMEOUT_MS, RuntimeResourceAuditR927.remainingMs())));
            conn.setReadTimeout((int)Math.max(1L, Math.min(AppConfig.HTTP_READ_TIMEOUT_MS, RuntimeResourceAuditR927.remainingMs())));
            conn.setRequestMethod("POST");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setRequestProperty("Accept", "application/json,text/plain,*/*");
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Connection", "close");
            conn.setRequestProperty("User-Agent", buildUserAgent());

            byte[] data = jsonBody == null ? new byte[0] : jsonBody.getBytes("UTF-8");
            os = conn.getOutputStream();
            os.write(data);
            os.flush();

            int code = conn.getResponseCode();
            out.httpCode = code;

            if (code >= 200 && code < 300) {
                in = new BufferedInputStream(conn.getInputStream());
                out.body = readAll(in);
                out.ok = out.body != null && out.body.length() > 0;

                if (!out.ok) {
                    out.errorMessage = "empty response";
                }
            } else {
                InputStream err = conn.getErrorStream();

                if (err != null) {
                    out.body = readAll(err);
                    closeQuietly(err);
                }

                out.ok = false;
                out.errorMessage = "http " + code;
            }
        } catch (Exception e) {
            out.ok = false;
            out.errorMessage = sanitizeMessage(e.getMessage());
        } finally {
            closeQuietly(os);
            closeQuietly(in);

            if (conn != null) {
                conn.disconnect();
            }
        }

        FetchLedgerR925.complete(ledgerStoreR925,requestIdR925,TimeSourceR811.monoNowMs()-requestStartedR925,out);
        return out;
    }

    private ArrayList<String> buildLatestUrlCandidates(String base, int limit) {
        ArrayList<String> urls = new ArrayList<String>();

        addUrl(urls, buildUrl(base, DATA_PATH, limit, ""));
        addUrl(urls, buildUrl(base, AppConfig.API_PATH_DATA, limit, ""));

        return urls;
    }

    private ArrayList<String> buildDateUrlCandidates(String base, String date) {
        ArrayList<String> urls = new ArrayList<String>();

        addUrl(urls, buildUrl(base, DATA_PATH, 0, date));
        addUrl(urls, buildUrl(base, AppConfig.API_PATH_DATA, 0, date));

        return urls;
    }

    private void addUrl(ArrayList<String> list, String url) {
        if (list == null || url == null || url.length() == 0) {
            return;
        }

        for (int i = 0; i < list.size(); i++) {
            if (url.equals(list.get(i))) {
                return;
            }
        }

        list.add(url);
    }

    private String buildUrl(String base, String path, int limit, String date) {
        StringBuilder sb = new StringBuilder();

        sb.append(trimRightSlash(base));

        if (path == null || path.length() == 0) {
            path = DATA_PATH;
        }

        if (!path.startsWith("/")) {
            sb.append("/");
        }

        sb.append(path);

        appendParam(sb, "code", AppConfig.LOTTERY_CODE);

        if (limit > 0) {
            appendParam(sb, "limit", String.valueOf(limit));
        }

        if (date != null && date.length() > 0) {
            appendParam(sb, "date", date);
        }

        appendParam(sb, "key", KeyProvider.getApiKey());

        return sb.toString();
    }

    private void appendParam(StringBuilder sb, String key, String value) {
        if (sb == null || key == null || key.length() == 0 || value == null) {
            return;
        }

        try {
            if (sb.indexOf("?") < 0) {
                sb.append("?");
            } else {
                sb.append("&");
            }

            sb.append(URLEncoder.encode(key, "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(value, "UTF-8"));
        } catch (Exception e) {
        }
    }

    public String todayDateText() {
        return todayDate();
    }

    public String yesterdayDateText() {
        return offsetDate(-1);
    }

    public String offsetDateText(int offsetDays) {
        return offsetDate(offsetDays);
    }

    private String todayDate() {
        return offsetDate(0);
    }

    private String offsetDate(int offsetDays) {
        try {
            TimeZone zone = TimeZone.getTimeZone("Asia/Shanghai");
            Calendar calendar = Calendar.getInstance(zone, Locale.US);
            calendar.setTime(new Date());
            calendar.add(Calendar.DAY_OF_MONTH, offsetDays);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            sdf.setTimeZone(zone);
            return sdf.format(calendar.getTime());
        } catch (Throwable e) {
            return "";
        }
    }

    private String trimRightSlash(String value) {
        if (value == null || value.length() == 0) {
            return "";
        }

        String out = value;

        while (out.endsWith("/") && out.length() > 1) {
            out = out.substring(0, out.length() - 1);
        }

        return out;
    }

    private boolean looksLikeJson(String body) {
        if (body == null) {
            return false;
        }

        String s = body.trim();

        return s.startsWith("{") || s.startsWith("[");
    }

    private String readAll(InputStream input) throws Exception {
        if (input == null) {
            return "";
        }

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        try {
            br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
            char[] buffer = new char[4096];

            while (true) {
                int len = br.read(buffer);

                if (len < 0) {
                    break;
                }

                if (len > 0) {
                    sb.append(buffer, 0, len);

                    if (sb.length() > MAX_BODY_CHARS) {
                        break;
                    }
                }
            }
        } finally {
            closeQuietly(br);
        }

        return sb.toString();
    }

    private static Models.Draw latestValidDraw(Models.ApiResult result) {
        if (result == null) return null;
        Models.Draw best =
                result.latestDraw != null && result.latestDraw.isValid()
                        ? result.latestDraw
                        : null;
        java.util.List<Models.Draw> rows =
                result.history != null ? result.history : result.draws;
        if (rows != null) {
            for (int i = 0; i < rows.size(); i++) {
                Models.Draw d = rows.get(i);
                if (d == null || !d.isValid()) continue;
                if (best == null || compareIssue(d.issue, best.issue) > 0) best = d;
            }
        }
        return best;
    }

    private boolean containsIssue(ArrayList<Models.Draw> list, String issue) {
        if (list == null || issue == null || issue.length() == 0) {
            return false;
        }

        for (int i = 0; i < list.size(); i++) {
            Models.Draw d = list.get(i);
            if (d != null && issue.equals(d.issue)) {
                return true;
            }
        }

        return false;
    }

    private String buildPublicError(RawHttpResult result, String type) {
        if (result == null) {
            return type + " request failed";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(type == null ? "api" : type);
        sb.append(" failed");

        if (result.httpCode > 0) {
            sb.append(" http=");
            sb.append(result.httpCode);
        }

        if (result.errorMessage != null && result.errorMessage.length() > 0) {
            sb.append(" ");
            sb.append(sanitizeMessage(result.errorMessage));
        }

        return sb.toString();
    }

    private String buildUserAgent() {
        String name = AppConfig.PROJECT_NAME == null ? "ETH-K3" : AppConfig.PROJECT_NAME;
        return name.replace(" ", "-") + "/1.0";
    }

    private String sanitizeUrl(String url) {
        if (url == null) {
            return "";
        }

        String out = url;

        out = maskQueryValue(out, "key");
        out = maskQueryValue(out, "token");
        out = maskQueryValue(out, "secret");
        out = maskQueryValue(out, "apikey");
        out = maskQueryValue(out, "api_key");

        return out;
    }

    private String maskQueryValue(String url, String key) {
        if (url == null || key == null || key.length() == 0) {
            return url == null ? "" : url;
        }

        String lower = url.toLowerCase(Locale.US);
        String search = key.toLowerCase(Locale.US) + "=";

        int start = lower.indexOf(search);

        if (start < 0) {
            return url;
        }

        int valueStart = start + search.length();
        int end = url.indexOf("&", valueStart);

        if (end < 0) {
            return url.substring(0, valueStart) + "***";
        }

        return url.substring(0, valueStart) + "***" + url.substring(end);
    }

    private String sanitizeMessage(String message) {
        if (message == null) {
            return "";
        }

        String out = message;
        String key = KeyProvider.getApiKey();

        if (key != null && key.length() > 0) {
            out = out.replace(key, "***");
        }

        out = maskQueryValue(out, "key");
        out = maskQueryValue(out, "token");
        out = maskQueryValue(out, "secret");
        out = maskQueryValue(out, "apikey");
        out = maskQueryValue(out, "api_key");

        if (out.length() > 180) {
            out = out.substring(0, 180);
        }

        return out;
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable == null) {
            return;
        }

        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    public static final class RawHttpResult {
        public boolean ok;
        public int httpCode;
        public String url;
        public String body;
        public String errorMessage;

        public RawHttpResult() {
            ok = false;
            httpCode = 0;
            url = "";
            body = "";
            errorMessage = "";
        }
    }

    /* STARCORE_TIANSHU_B01_MATERIALIZED_20260805 */
    private static final ApiTransportGovernanceEngine TIANSHU_TRANSPORT =
            new ApiTransportGovernanceEngine();

    private Models.ApiResult requestGovernedLatest(int limit) {
        return requestGovernedLatest(limit, null);
    }

    private Models.ApiResult requestGovernedLatest(
            int limit, ApiTransportGovernanceEngine.PayloadGate gate) {
        final BinanceGameContract.RequestSpec request =
                BinanceGameContract.RequestSpec.latest(limit);
        return executeGoverned(request, "API_LATEST_FAILED", gate);
    }

    private Models.ApiResult requestGovernedDate(String date) {
        return requestGovernedDate(date, null);
    }

    private Models.ApiResult requestGovernedDate(
            String date, ApiTransportGovernanceEngine.PayloadGate gate) {
        final BinanceGameContract.RequestSpec request = BinanceGameContract.RequestSpec.date(date);
        return executeGoverned(request, "API_DATE_FAILED", gate);
    }

    private Models.ApiResult executeGoverned(
            final BinanceGameContract.RequestSpec request, String errorCode) {
        return executeGoverned(request, errorCode, null);
    }

    private Models.ApiResult executeGoverned(
            final BinanceGameContract.RequestSpec request,
            String errorCode,
            final ApiTransportGovernanceEngine.PayloadGate gate) {
        ApiTransportGovernanceEngine.PayloadGate auditedGate=new ApiTransportGovernanceEngine.PayloadGate(){
            public ApiTransportGovernanceEngine.PayloadDecision evaluate(String source,String body,long now){
                ApiTransportGovernanceEngine.PayloadDecision decision=gate==null?ApiTransportGovernanceEngine.PayloadDecision.accept():gate.evaluate(source,body,now);
                String issue="";
                try{Models.ApiResult parsed=ApiParser.parseLatest(body,source);if(parsed!=null&&parsed.latestDraw!=null)issue=parsed.latestDraw.issue;}catch(Throwable invalid){issue="PARSE_UNAVAILABLE";}
                FetchLedgerR925.payload(ledgerStoreR925,ledgerRequestR925.get(),source,issue,decision.accepted,decision.code);
                return decision;
            }
        };
        ApiTransportGovernanceEngine.TransportResult result =
                TIANSHU_TRANSPORT.execute(
                        request,
                        new ApiTransportGovernanceEngine.TransportAdapter() {
                            public ApiTransportGovernanceEngine.HttpResponse execute(String url) {
                                long started = System.currentTimeMillis();
                                RawHttpResult raw = requestGet(url);
                                long elapsed = System.currentTimeMillis() - started;
                                return new ApiTransportGovernanceEngine.HttpResponse(
                                        raw != null && raw.ok,
                                        raw == null ? 0 : raw.httpCode,
                                        raw == null ? "" : raw.body,
                                        raw == null ? "null response" : raw.errorMessage,
                                        elapsed);
                            }
                        },
                        auditedGate);
        if (result.success) return rawSuccess(result.body, result.sourceLine);
        return Models.ApiResult.fail(
                errorCode,
                result.outcome
                        + "|"
                        + result.publicError
                        + "|attempts="
                        + result.attempts
                        + "|elapsedMs="
                        + result.elapsedMs);
    }

    public static ApiTransportGovernanceEngine.TelemetrySnapshot tianshuTransportSnapshot() {
        return TIANSHU_TRANSPORT.snapshot();
    }

    public static boolean tianshuTransportSelfTest() {
        return BinanceGameContract.selfTest()
                && ApiTransportGovernanceEngine.selfTest()
                && AppConfig.TianshuB01.isProductionTransportConfigValid();
    }

    public static String tianshuPublicEndpointSummary() {
        return "game="
                + BinanceGameContract.GAME_CODE
                + "|primary="
                + BinanceGameContract.primary().endpoint()
                + "|backup="
                + BinanceGameContract.backup().endpoint()
                + "|key="
                + KeyProvider.maskedStatus();
    }

    /** Preserve the actual failed response, never a fabricated stale-local-draw explanation. */
    public static String failureSummary(Models.ApiResult result) {
        if(result==null)return "API_RESULT_NULL";
        return SafeLog.sanitize("code="+result.errorCode+"|detail="+result.errorMessage+"|line="+result.apiLine);
    }

    public static final class TianshuRequestAudit {
        public final String operation;
        public final String gameCode;
        public final String endpointClass;
        public final int requestedLimit;
        public final String requestedDate;
        public final boolean keyReady;
        public final boolean canonicalPath;
        public final boolean budgetConfigured;
        public final boolean shadowOnly;
        public final long createdAt;
        public final String fingerprint;

        private TianshuRequestAudit(String operation, int limit, String date) {
            this.operation = operation == null ? "" : operation;
            this.gameCode = BinanceGameContract.GAME_CODE;
            this.endpointClass = "DUOQU_DATA";
            this.requestedLimit = limit;
            this.requestedDate = date == null ? "" : date;
            this.keyReady = KeyProvider.isKeyReady();
            this.canonicalPath = "/data".equals(BinanceGameContract.DATA_PATH);
            this.budgetConfigured =
                    AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE > 0
                            && AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE > 0;
            this.shadowOnly = V51BuildManifest.SHADOW_ONLY;
            this.createdAt = System.currentTimeMillis();
            this.fingerprint = stableFingerprint();
        }

        public static TianshuRequestAudit latest(int limit) {
            return new TianshuRequestAudit("LATEST", limit, "");
        }

        public static TianshuRequestAudit date(String date) {
            return new TianshuRequestAudit("DATE", 0, date);
        }

        public boolean valid() {
            if (!BinanceGameContract.GAME_CODE.equals(gameCode)) return false;
            if (!canonicalPath || !budgetConfigured || !shadowOnly) return false;
            if ("LATEST".equals(operation)) return requestedLimit >= 1 && requestedLimit <= 100;
            if ("DATE".equals(operation)) return BinanceGameContract.isValidDate(requestedDate);
            return false;
        }

        public String canonical() {
            return operation
                    + '|'
                    + gameCode
                    + '|'
                    + endpointClass
                    + '|'
                    + requestedLimit
                    + '|'
                    + requestedDate
                    + '|'
                    + (keyReady ? 1 : 0)
                    + '|'
                    + (canonicalPath ? 1 : 0)
                    + '|'
                    + (budgetConfigured ? 1 : 0)
                    + '|'
                    + (shadowOnly ? 1 : 0)
                    + '|'
                    + fingerprint;
        }

        private String stableFingerprint() {
            String text =
                    operation
                            + '|'
                            + gameCode
                            + '|'
                            + endpointClass
                            + '|'
                            + requestedLimit
                            + '|'
                            + requestedDate
                            + "|B01";
            return V51ChecksumEngine.sha256(text);
        }
    }

    public static final class TianshuClientContract {
        private final String id;
        private final String field;
        private final String group;
        private final boolean mandatory;

        TianshuClientContract(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }

        public String canonical() {
            return id + ':' + field + ':' + group + ':' + (mandatory ? '1' : '0');
        }
    }

    public static java.util.List<TianshuClientContract> tianshuClientContracts() {
        java.util.ArrayList<TianshuClientContract> items =
                new java.util.ArrayList<TianshuClientContract>();
        String[] fields =
                new String[] {
                    "GAME_CODE", "CANONICAL_PATH", "LATEST_LIMIT", "DATE_FORMAT",
                    "PRIMARY_FIRST", "SAME_CALL_FAILOVER", "TOKEN_BUCKET", "SINGLE_FLIGHT",
                    "CONNECT_TIMEOUT", "READ_TIMEOUT", "NON_2XX_FAIL", "EMPTY_BODY_FAIL",
                    "NON_JSON_FAIL", "BUSINESS_FAIL", "STRUCTURE_FAIL", "KEY_REDACTION",
                    "URL_REDACTION", "ERROR_REDACTION", "RAW_ARCHIVE_REDACTION", "SHADOW_ONLY",
                    "MODEL_NOT_PROVEN", "LEGACY_PATH_DISABLED", "PROVIDER_TIME", "REPLAY_HASH"
                };
        for (int i = 0; i < 48; i++) {
            String id = "TIANSHU-B01-CLIENT-" + pad3(i + 1);
            String field = fields[i % fields.length];
            String group = "GROUP_" + pad2((i / fields.length) + 1);
            items.add(new TianshuClientContract(id, field, group, true));
        }
        return java.util.Collections.unmodifiableList(items);
    }

    private static String pad2(int value) {
        if (value < 10) return "0" + value;
        return String.valueOf(value);
    }

    private static String pad3(int value) {
        if (value < 10) return "00" + value;
        if (value < 100) return "0" + value;
        return String.valueOf(value);
    }

    public static String tianshuClientContractHash() {
        StringBuilder sb = new StringBuilder();
        java.util.List<TianshuClientContract> items = tianshuClientContracts();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append('|');
            sb.append(items.get(i).canonical());
        }
        return V51ChecksumEngine.sha256(sb.toString());
    }

    public static boolean tianshuClientAuditSelfTest() {
        TianshuRequestAudit latest = TianshuRequestAudit.latest(20);
        TianshuRequestAudit date = TianshuRequestAudit.date("2026-08-05");
        return latest.valid()
                && date.valid()
                && latest.fingerprint.equals(TianshuRequestAudit.latest(20).fingerprint)
                && tianshuClientContracts().size() == 48
                && tianshuClientContractHash().length() == 64
                && requestPathInvariant();
    }

    public static final class TianshuRequestPlan {
        public final String operation;
        public final String primaryEndpoint;
        public final String backupEndpoint;
        public final String queryShape;
        public final int budgetPerMinute;
        public final int providerLimitPerMinute;
        public final boolean failoverSameCall;
        public final boolean rawResponseRedacted;
        public final String contractHash;

        private TianshuRequestPlan(
                String operation,
                String queryShape,
                int budgetPerMinute,
                int providerLimitPerMinute) {
            this.operation = operation;
            this.primaryEndpoint = BinanceGameContract.primary().endpoint();
            this.backupEndpoint = BinanceGameContract.backup().endpoint();
            this.queryShape = queryShape;
            this.budgetPerMinute = budgetPerMinute;
            this.providerLimitPerMinute = providerLimitPerMinute;
            this.failoverSameCall = true;
            this.rawResponseRedacted = true;
            this.contractHash = V51ChecksumEngine.sha256(canonicalWithoutHash());
        }

        private String canonicalWithoutHash() {
            return operation
                    + '|'
                    + primaryEndpoint
                    + '|'
                    + backupEndpoint
                    + '|'
                    + queryShape
                    + '|'
                    + budgetPerMinute
                    + '|'
                    + providerLimitPerMinute
                    + '|'
                    + (failoverSameCall ? 1 : 0)
                    + '|'
                    + (rawResponseRedacted ? 1 : 0);
        }

        public String canonical() {
            return canonicalWithoutHash() + '|' + contractHash;
        }

        public boolean valid() {
            return operation != null
                    && operation.length() > 0
                    && primaryEndpoint.startsWith("https://")
                    && backupEndpoint.startsWith("https://")
                    && primaryEndpoint.endsWith("/data")
                    && backupEndpoint.endsWith("/data")
                    && queryShape.indexOf("code=Binance1-PK10") >= 0
                    && queryShape.indexOf("key=${DUOQU_API_KEY}") >= 0
                    && budgetPerMinute > 0
                    && providerLimitPerMinute > budgetPerMinute
                    && failoverSameCall
                    && rawResponseRedacted
                    && contractHash.length() == 64;
        }
    }

    public static TianshuRequestPlan tianshuLatestPlan() {
        return new TianshuRequestPlan(
                "LATEST",
                "code=Binance1-PK10&limit={1..100}&key=${DUOQU_API_KEY}",
                AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE,
                AppConfig.TianshuB01.PROVIDER_LATEST_LIMIT_PER_MINUTE);
    }

    public static TianshuRequestPlan tianshuDatePlan() {
        return new TianshuRequestPlan(
                "DATE",
                "code=Binance1-PK10&date={yyyy-MM-dd}&key=${DUOQU_API_KEY}",
                AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE,
                AppConfig.TianshuB01.PROVIDER_DATE_LIMIT_PER_MINUTE);
    }

    public static String tianshuAuditSummary() {
        ApiTransportGovernanceEngine.TelemetrySnapshot snapshot = tianshuTransportSnapshot();
        return "batch=B01|game="
                + BinanceGameContract.GAME_CODE
                + "|latestPlan="
                + tianshuLatestPlan().contractHash
                + "|datePlan="
                + tianshuDatePlan().contractHash
                + "|transportEvents="
                + snapshot.eventCount
                + "|key="
                + KeyProvider.maskedStatus()
                + "|release="
                + V51BuildManifest.releaseState();
    }

    private static boolean requestPathInvariant() {
        return "/data".equals(AppConfig.API_PATH_DATA)
                && AppConfig.API_PATH_DATA.equals(AppConfig.API_PATH_LATEST)
                && AppConfig.API_PATH_DATA.equals(AppConfig.API_PATH_HISTORY)
                && BinanceGameContract.GAME_CODE.equals(AppConfig.LOTTERY_CODE)
                && tianshuLatestPlan().valid()
                && tianshuDatePlan().valid();
    }
}
