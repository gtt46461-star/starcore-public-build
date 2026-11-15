package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Rate limit, failover, circuit breaker, EWMA telemetry, provider time and redacted archive owner.
 */
public final class ApiTransportGovernanceEngine {
    public static final String MATERIALIZATION_MARKER =
            "STARCORE_TIANSHU_B01_MATERIALIZED_20260805";

    private static final java.util.Random RETRY_JITTER = new java.util.Random();
    private static long monoMs(){return System.nanoTime()/1000000L;}

    public enum CircuitState {
        CLOSED,
        OPEN,
        HALF_OPEN
    }

    public enum Outcome {
        SUCCESS,
        BUDGET_DENIED,
        TRANSPORT_ERROR,
        HTTP_ERROR,
        EMPTY_BODY,
        NON_JSON,
        BUSINESS_FAILURE,
        SCHEMA_FAILURE
    }

    public interface TransportAdapter {
        HttpResponse execute(String url);
    }

    /**
     * Optional business-payload gate executed after transport/HTTP/JSON validation but before an
     * endpoint is accepted as successful. A rejected primary payload must fall through to backup.
     * This gate is data-only and owns no prediction, settlement or BET authority.
     */
    public interface PayloadGate {
        PayloadDecision evaluate(String sourceLine, String body, long nowMs);
    }

    public static final class PayloadDecision {
        public final boolean accepted;
        public final String code;
        public final String detail;

        private PayloadDecision(boolean accepted, String code, String detail) {
            this.accepted = accepted;
            this.code = code == null ? "" : code;
            this.detail = SafeLog.sanitize(detail);
        }

        public static PayloadDecision accept() {
            return new PayloadDecision(true, "OK", "");
        }

        public static PayloadDecision reject(String code, String detail) {
            return new PayloadDecision(false, code, detail);
        }
    }

    public static final class HttpResponse {
        public final boolean transportOk;
        public final int httpCode;
        public final String body;
        public final String error;
        public final long latencyMs;

        public HttpResponse(
                boolean transportOk, int httpCode, String body, String error, long latencyMs) {
            this.transportOk = transportOk;
            this.httpCode = httpCode;
            this.body = body == null ? "" : body;
            this.error = SafeLog.sanitize(error);
            this.latencyMs = Math.max(0L, latencyMs);
        }

        public boolean httpOk() {
            return transportOk && httpCode >= 200 && httpCode < 300;
        }
    }

    public static final class TransportResult {
        public final boolean success;
        public final String body;
        public final String sourceLine;
        public final boolean fromBackup;
        public final Outcome outcome;
        public final String publicError;
        public final int attempts;
        public final long elapsedMs;

        TransportResult(
                boolean success,
                String body,
                String sourceLine,
                boolean fromBackup,
                Outcome outcome,
                String publicError,
                int attempts,
                long elapsedMs) {
            this.success = success;
            this.body = body == null ? "" : body;
            this.sourceLine = sourceLine == null ? "" : sourceLine;
            this.fromBackup = fromBackup;
            this.outcome = outcome;
            this.publicError = SafeLog.sanitize(publicError);
            this.attempts = attempts;
            this.elapsedMs = elapsedMs;
        }
    }

    public static final class TokenBucket {
        private final double capacity;
        private final double refillPerMs;
        private double tokens;
        private long lastRefillMs;

        TokenBucket(int perMinute) {
            this.capacity = Math.max(1, perMinute);
            this.refillPerMs = capacity / 60000.0d;
            this.tokens = capacity;
            this.lastRefillMs = monoMs();
        }

        synchronized boolean tryAcquire(long now) {
            refill(now);
            if (tokens < 1.0d) return false;
            tokens -= 1.0d;
            return true;
        }

        synchronized double available(long now) {
            refill(now);
            return tokens;
        }

        private void refill(long now) {
            if (now <= lastRefillMs) return;
            tokens = Math.min(capacity, tokens + (now - lastRefillMs) * refillPerMs);
            lastRefillMs = now;
        }
    }

    public static final class Ewma {
        private final double alpha;
        private double value;
        private boolean initialized;

        Ewma(double alpha) {
            this.alpha = alpha <= 0.0d || alpha > 1.0d ? 0.25d : alpha;
        }

        synchronized void add(double sample) {
            value = initialized ? alpha * sample + (1.0d - alpha) * value : sample;
            initialized = true;
        }

        synchronized double value() {
            return initialized ? value : 0.0d;
        }
    }

    public static final class EndpointState {
        public final BinanceGameContract.EndpointContract endpoint;
        private CircuitState state = CircuitState.CLOSED;
        private int failureStreak;
        private int successStreak;
        private long openUntilMs;
        private boolean probeInFlight;
        private final Ewma latency = new Ewma(0.25d);

        EndpointState(BinanceGameContract.EndpointContract endpoint) {
            this.endpoint = endpoint;
        }

        synchronized boolean ready(long now) { return state==CircuitState.CLOSED || (state==CircuitState.OPEN && now>=openUntilMs); }

        synchronized boolean allow(long now) {
            if (state == CircuitState.CLOSED) return true;
            if (state == CircuitState.OPEN && now >= openUntilMs) {
                state = CircuitState.HALF_OPEN;
                probeInFlight = true;
                return true;
            }
            if (state==CircuitState.HALF_OPEN && !probeInFlight) {probeInFlight=true;return true;}
            return false;
        }

        synchronized void success(long latencyMs) {
            latency.add(latencyMs);
            probeInFlight = false;
            failureStreak = 0;
            successStreak++;
            state = CircuitState.CLOSED;
            openUntilMs = 0L;
        }

        synchronized void responsiveButNotCurrentR71(long latencyMs) {
            // HTTP worked; a delayed/old draw is not a broken network circuit.
            latency.add(latencyMs);probeInFlight=false;failureStreak=0;successStreak=0;
            state=CircuitState.CLOSED;openUntilMs=0L;
        }

        synchronized void failure(long now) {
            probeInFlight = false;
            successStreak = 0;
            failureStreak++;
            if (failureStreak >= 2 || state == CircuitState.HALF_OPEN) {
                state = CircuitState.OPEN;
                long backoff = Math.min(120000L, 5000L * (1L << Math.min(5, failureStreak - 2)));
                openUntilMs = now + Math.min(120000L, backoff + RETRY_JITTER.nextInt((int)Math.max(1L,backoff/4L)));
            }
        }

        synchronized EndpointSnapshot snapshot() {
            return new EndpointSnapshot(
                    endpoint.id, state, failureStreak, successStreak, openUntilMs, latency.value());
        }
    }

    public static final class EndpointSnapshot {
        public final String id;
        public final CircuitState state;
        public final int failures;
        public final int successes;
        public final long openUntilMs;
        public final double ewmaLatencyMs;

        EndpointSnapshot(
                String id,
                CircuitState state,
                int failures,
                int successes,
                long openUntilMs,
                double ewmaLatencyMs) {
            this.id = id;
            this.state = state;
            this.failures = failures;
            this.successes = successes;
            this.openUntilMs = openUntilMs;
            this.ewmaLatencyMs = ewmaLatencyMs;
        }
    }

    public static final class TelemetryEvent {
        public final long timeMs;
        public final String requestId;
        public final String endpoint;
        public final Outcome outcome;
        public final int httpCode;
        public final long latencyMs;
        public final String detail;

        TelemetryEvent(
                long timeMs,
                String requestId,
                String endpoint,
                Outcome outcome,
                int httpCode,
                long latencyMs,
                String detail) {
            this.timeMs = timeMs;
            this.requestId = requestId;
            this.endpoint = endpoint;
            this.outcome = outcome;
            this.httpCode = httpCode;
            this.latencyMs = latencyMs;
            this.detail = SafeLog.sanitize(detail);
        }
    }

    public static final class ArchiveEntry {
        public final long timeMs;
        public final String requestId;
        public final String endpoint;
        public final String bodyFingerprint;
        public final int bodyLength;
        public final Outcome outcome;
        public final String preview;

        ArchiveEntry(long timeMs, String requestId, String endpoint, String body, Outcome outcome) {
            this.timeMs = timeMs;
            this.requestId = requestId;
            this.endpoint = endpoint;
            this.bodyLength = body == null ? 0 : body.length();
            this.bodyFingerprint = V51ChecksumEngine.sha256(body == null ? "" : body);
            this.outcome = outcome;
            String clean = SafeLog.sanitize(body);
            this.preview = clean.length() <= 160 ? clean : clean.substring(0, 160);
        }
    }

    public static final class TelemetrySnapshot {
        public final EndpointSnapshot primary;
        public final EndpointSnapshot backup;
        public final double latestTokens;
        public final double dateTokens;
        public final int eventCount;
        public final int archiveCount;
        /** Last issue whose payload passed both transport and domain parsing. */
        public final String lastSuccessfulIssue;
        public final long lastSuccessAt;
        public final long staleAgeMs;
        public final int primaryBackupSwitchCount;
        public final String activeSourceLine;

        TelemetrySnapshot(
                EndpointSnapshot primary,
                EndpointSnapshot backup,
                double latestTokens,
                double dateTokens,
                int eventCount,
                int archiveCount,
                String lastSuccessfulIssue,
                long lastSuccessAt,
                long staleAgeMs,
                int primaryBackupSwitchCount,
                String activeSourceLine) {
            this.primary = primary;
            this.backup = backup;
            this.latestTokens = latestTokens;
            this.dateTokens = dateTokens;
            this.eventCount = eventCount;
            this.archiveCount = archiveCount;
            this.lastSuccessfulIssue = safe(lastSuccessfulIssue);
            this.lastSuccessAt = Math.max(0L, lastSuccessAt);
            this.staleAgeMs = staleAgeMs;
            this.primaryBackupSwitchCount = Math.max(0, primaryBackupSwitchCount);
            this.activeSourceLine = safe(activeSourceLine);
        }
    }

    private final TokenBucket latestBucket;
    private final TokenBucket dateBucket;
    private final EndpointState primaryState;
    private final EndpointState backupState;
    private final ArrayList<TelemetryEvent> events = new ArrayList<TelemetryEvent>();
    private final ArrayList<ArchiveEntry> archive = new ArrayList<ArchiveEntry>();
    private final Map<String, Object> singleFlight = new HashMap<String, Object>();
    private final int maxEvents;
    private final int maxArchive;
    private String lastSuccessfulIssue = "";
    private long lastSuccessAt;
    private int primaryBackupSwitchCount;
    private String activeSourceLine = "";

    public ApiTransportGovernanceEngine() {
        this(
                AppConfig.TianshuB01.LATEST_BUDGET_PER_MINUTE,
                AppConfig.TianshuB01.DATE_BUDGET_PER_MINUTE,
                256,
                64);
    }

    public ApiTransportGovernanceEngine(
            int latestPerMinute, int datePerMinute, int maxEvents, int maxArchive) {
        this.latestBucket = new TokenBucket(latestPerMinute);
        this.dateBucket = new TokenBucket(datePerMinute);
        this.primaryState = new EndpointState(BinanceGameContract.primary());
        this.backupState = new EndpointState(BinanceGameContract.backup());
        this.maxEvents = Math.max(32, maxEvents);
        this.maxArchive = Math.max(16, maxArchive);
    }

    public TransportResult execute(
            BinanceGameContract.RequestSpec request, TransportAdapter adapter) {
        return execute(request, adapter, null);
    }

    public TransportResult execute(
            BinanceGameContract.RequestSpec request,
            TransportAdapter adapter,
            PayloadGate gate) {
        long start = monoMs();
        if (request == null || !request.valid())
            return fail(Outcome.SCHEMA_FAILURE, "REQUEST_INVALID", 0, start);
        if (adapter == null) return fail(Outcome.TRANSPORT_ERROR, "ADAPTER_NULL", 0, start);
        TokenBucket bucket =
                request.kind == BinanceGameContract.RequestKind.LATEST ? latestBucket : dateBucket;
        if (!bucket.tryAcquire(start)) {
            record(request, "NONE", Outcome.BUDGET_DENIED, 0, 0L, "rate budget", "");
            return fail(Outcome.BUDGET_DENIED, "RATE_BUDGET_DENIED", 0, start);
        }
        Object flight = acquireFlight(request.canonical());
        synchronized (flight) {
            try {
                EndpointState[] endpoints = orderEndpoints(start);
                int attempts = 0;
                Outcome lastOutcome = Outcome.TRANSPORT_ERROR;
                String lastError = "NO_ENDPOINT";
                for (int i = 0; i < endpoints.length; i++) {
                    EndpointState state = endpoints[i];
                    long now = monoMs();
                    if (!state.allow(now)) continue;
                    attempts++;
                    String key = KeyProvider.getApiKey();
                    String url = BinanceGameContract.buildUrl(state.endpoint, request, key);
                    key = "";
                    long callStart = monoMs();
                    HttpResponse response;
                    try { response = adapter.execute(url); }
                    catch (RuntimeException transportFailure) { response = new HttpResponse(false, 0, "", transportFailure.getClass().getSimpleName(), monoMs()-callStart); }
                    long latency =
                            response == null
                                    ? monoMs() - callStart
                                    : response.latencyMs;
                    Evaluation evaluation = evaluate(response);
                    if (evaluation.success) {
                        String sourceLine =
                                state.endpoint.primary
                                        ? AppConfig.API_LINE_PRIMARY
                                        : AppConfig.API_LINE_BACKUP;
                        PayloadDecision payload;
                        try {
                            payload =
                                    gate == null
                                            ? PayloadDecision.accept()
                                            : gate.evaluate(
                                                    sourceLine,
                                                    response.body,
                                                    System.currentTimeMillis());
                        } catch (Throwable gateError) {
                            payload =
                                    PayloadDecision.reject(
                                            "PAYLOAD_GATE_EXCEPTION",
                                            gateError.getClass().getSimpleName());
                        }
                        if (payload != null && payload.accepted) {
                            state.success(latency);
                            record(
                                    request,
                                    state.endpoint.id,
                                    Outcome.SUCCESS,
                                    response.httpCode,
                                    latency,
                                    "ok",
                                    response.body);
                            return new TransportResult(
                                    true,
                                    response.body,
                                    sourceLine,
                                    !state.endpoint.primary,
                                    Outcome.SUCCESS,
                                    "",
                                    attempts,
                                    monoMs() - start);
                        }

                        if (payload != null && ("OPEN_TIME_STALE".equals(payload.code)
                                ||"AWAITING_NEXT_ISSUE".equals(payload.code)
                                ||payload.code.startsWith("ISSUE_BEHIND_")
                                ||"CLOCK_SKEW_QUARANTINED".equals(payload.code)))
                            state.responsiveButNotCurrentR71(latency);
                        else state.failure(monoMs());
                        lastOutcome = Outcome.BUSINESS_FAILURE;
                        lastError =
                                payload == null
                                        ? "PAYLOAD_GATE_NULL"
                                        : payload.code + ":" + payload.detail;
                        record(
                                request,
                                state.endpoint.id,
                                Outcome.BUSINESS_FAILURE,
                                response.httpCode,
                                latency,
                                lastError,
                                response.body);
                        continue;
                    }
                    state.failure(monoMs());
                    lastOutcome = evaluation.outcome;
                    lastError = evaluation.error;
                    record(
                            request,
                            state.endpoint.id,
                            evaluation.outcome,
                            response == null ? 0 : response.httpCode,
                            latency,
                            evaluation.error,
                            response == null ? "" : response.body);
                }
                return new TransportResult(
                        false,
                        "",
                        "",
                        false,
                        lastOutcome,
                        lastError,
                        attempts,
                        monoMs() - start);
            } finally {
                releaseFlight(request.canonical(), flight);
            }
        }
    }

    private EndpointState[] orderEndpoints(long now) {
        boolean primaryAllowed = primaryState.ready(now);
        boolean backupAllowed = backupState.ready(now);
        if (primaryAllowed) return new EndpointState[] {primaryState, backupState};
        if (backupAllowed) return new EndpointState[] {backupState, primaryState};
        return new EndpointState[] {primaryState, backupState};
    }

    private static final class Evaluation {
        final boolean success;
        final Outcome outcome;
        final String error;

        Evaluation(boolean success, Outcome outcome, String error) {
            this.success = success;
            this.outcome = outcome;
            this.error = error;
        }
    }

    private Evaluation evaluate(HttpResponse response) {
        if (response == null)
            return new Evaluation(false, Outcome.TRANSPORT_ERROR, "RESPONSE_NULL");
        if (!response.transportOk)
            return new Evaluation(false, Outcome.TRANSPORT_ERROR, response.error);
        if (response.httpCode < 200 || response.httpCode >= 300)
            return new Evaluation(false, Outcome.HTTP_ERROR, "HTTP_" + response.httpCode);
        String body = response.body == null ? "" : response.body.trim();
        if (body.length() == 0) return new Evaluation(false, Outcome.EMPTY_BODY, "EMPTY_BODY");
        if (!(body.startsWith("{") || body.startsWith("[")))
            return new Evaluation(false, Outcome.NON_JSON, "NON_JSON");
        String lower = body.toLowerCase(java.util.Locale.ROOT);
        if (lower.indexOf("\"info\"") >= 0 && lower.indexOf("success") < 0)
            return new Evaluation(false, Outcome.BUSINESS_FAILURE, "INFO_NOT_SUCCESS");
        return new Evaluation(true, Outcome.SUCCESS, "");
    }

    private TransportResult fail(Outcome outcome, String error, int attempts, long start) {
        return new TransportResult(
                false, "", "", false, outcome, error, attempts, monoMs() - start);
    }

    private synchronized Object acquireFlight(String key) {
        Object flight = singleFlight.get(key);
        if (flight == null) {
            flight = new Object();
            singleFlight.put(key, flight);
        }
        return flight;
    }

    private synchronized void releaseFlight(String key, Object flight) {
        if (singleFlight.get(key) == flight) singleFlight.remove(key);
    }

    private synchronized void record(
            BinanceGameContract.RequestSpec request,
            String endpoint,
            Outcome outcome,
            int httpCode,
            long latency,
            String detail,
            String body) {
        events.add(
                new TelemetryEvent(
                        System.currentTimeMillis(),
                        request.requestId,
                        endpoint,
                        outcome,
                        httpCode,
                        latency,
                        detail));
        while (events.size() > maxEvents) events.remove(0);
        archive.add(
                new ArchiveEntry(
                        System.currentTimeMillis(), request.requestId, endpoint, body, outcome));
        while (archive.size() > maxArchive) archive.remove(0);
    }

    public synchronized List<TelemetryEvent> events() {
        return Collections.unmodifiableList(new ArrayList<TelemetryEvent>(events));
    }

    public synchronized List<ArchiveEntry> archive() {
        return Collections.unmodifiableList(new ArrayList<ArchiveEntry>(archive));
    }

    /**
     * Records domain-level success only after ApiParser accepted a real latest draw. Transport
     * success alone is deliberately insufficient: a syntactically valid payload may still be
     * stale or unusable.
     */
    public synchronized void recordSuccessfulIssue(String issue, String sourceLine) {
        String cleanIssue = safe(issue);
        String cleanSource = safe(sourceLine);
        if (cleanIssue.length() == 0) return;
        if (activeSourceLine.length() > 0
                && cleanSource.length() > 0
                && !activeSourceLine.equals(cleanSource)) {
            primaryBackupSwitchCount++;
        }
        lastSuccessfulIssue = cleanIssue;
        lastSuccessAt = System.currentTimeMillis();
        if (cleanSource.length() > 0) activeSourceLine = cleanSource;
    }

    public TelemetrySnapshot snapshot() {
        long now = System.currentTimeMillis();
        synchronized (this) {
            long staleAge =
                    lastSuccessAt <= 0L ? Long.MAX_VALUE : Math.max(0L, now - lastSuccessAt);
            return new TelemetrySnapshot(
                    primaryState.snapshot(),
                    backupState.snapshot(),
                    latestBucket.available(monoMs()),
                    dateBucket.available(monoMs()),
                    events.size(),
                    archive.size(),
                    lastSuccessfulIssue,
                    lastSuccessAt,
                    staleAge,
                    primaryBackupSwitchCount,
                    activeSourceLine);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class MetricSpec {
        public final String id;
        public final String field;
        public final String group;
        public final boolean mandatory;

        MetricSpec(String id, String field, String group, boolean mandatory) {
            this.id = id;
            this.field = field;
            this.group = group;
            this.mandatory = mandatory;
        }
    }

    public static List<MetricSpec> metrics() {
        ArrayList<MetricSpec> rules = new ArrayList<MetricSpec>();
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-001", "LATEST_TOKEN", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-002", "DATE_TOKEN", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-003", "PRIMARY_STATE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-004", "BACKUP_STATE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-005", "EWMA_LATENCY", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-006", "HTTP_STATUS", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-007", "EMPTY_BODY", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-008", "NON_JSON", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-009", "BUSINESS_FAILURE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-010", "SCHEMA_FAILURE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-011", "TIMEOUT", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-012", "IO_ERROR", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-013", "FAILOVER", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-014", "HALF_OPEN", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-015", "OPEN_UNTIL", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-016", "SUCCESS_STREAK", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-017", "FAILURE_STREAK", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-018", "SINGLE_FLIGHT", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-019", "QUEUE_DELAY", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-020", "REQUEST_DURATION", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-021", "ARCHIVE_SIZE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-022", "REDACTION", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-023", "PROVIDER_TIME", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-024", "REPLAY", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-025", "FAULT_INJECTION", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-026", "BUDGET_DENIAL", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-027", "BACKOFF", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-028", "ATTEMPT", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-029", "SOURCE_LINE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-030", "REQUEST_ID", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-031", "GAME_CODE", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-032", "DATE_PRIORITY", "GROUP_01", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-033", "LATEST_TOKEN", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-034", "DATE_TOKEN", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-035", "PRIMARY_STATE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-036", "BACKUP_STATE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-037", "EWMA_LATENCY", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-038", "HTTP_STATUS", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-039", "EMPTY_BODY", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-040", "NON_JSON", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-041", "BUSINESS_FAILURE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-042", "SCHEMA_FAILURE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-043", "TIMEOUT", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-044", "IO_ERROR", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-045", "FAILOVER", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-046", "HALF_OPEN", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-047", "OPEN_UNTIL", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-048", "SUCCESS_STREAK", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-049", "FAILURE_STREAK", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-050", "SINGLE_FLIGHT", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-051", "QUEUE_DELAY", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-052", "REQUEST_DURATION", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-053", "ARCHIVE_SIZE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-054", "REDACTION", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-055", "PROVIDER_TIME", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-056", "REPLAY", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-057", "FAULT_INJECTION", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-058", "BUDGET_DENIAL", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-059", "BACKOFF", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-060", "ATTEMPT", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-061", "SOURCE_LINE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-062", "REQUEST_ID", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-063", "GAME_CODE", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-064", "DATE_PRIORITY", "GROUP_02", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-065", "LATEST_TOKEN", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-066", "DATE_TOKEN", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-067", "PRIMARY_STATE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-068", "BACKUP_STATE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-069", "EWMA_LATENCY", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-070", "HTTP_STATUS", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-071", "EMPTY_BODY", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-072", "NON_JSON", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-073", "BUSINESS_FAILURE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-074", "SCHEMA_FAILURE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-075", "TIMEOUT", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-076", "IO_ERROR", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-077", "FAILOVER", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-078", "HALF_OPEN", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-079", "OPEN_UNTIL", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-080", "SUCCESS_STREAK", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-081", "FAILURE_STREAK", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-082", "SINGLE_FLIGHT", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-083", "QUEUE_DELAY", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-084", "REQUEST_DURATION", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-085", "ARCHIVE_SIZE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-086", "REDACTION", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-087", "PROVIDER_TIME", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-088", "REPLAY", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-089", "FAULT_INJECTION", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-090", "BUDGET_DENIAL", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-091", "BACKOFF", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-092", "ATTEMPT", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-093", "SOURCE_LINE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-094", "REQUEST_ID", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-095", "GAME_CODE", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-096", "DATE_PRIORITY", "GROUP_03", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-097", "LATEST_TOKEN", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-098", "DATE_TOKEN", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-099", "PRIMARY_STATE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-100", "BACKUP_STATE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-101", "EWMA_LATENCY", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-102", "HTTP_STATUS", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-103", "EMPTY_BODY", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-104", "NON_JSON", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-105", "BUSINESS_FAILURE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-106", "SCHEMA_FAILURE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-107", "TIMEOUT", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-108", "IO_ERROR", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-109", "FAILOVER", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-110", "HALF_OPEN", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-111", "OPEN_UNTIL", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-112", "SUCCESS_STREAK", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-113", "FAILURE_STREAK", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-114", "SINGLE_FLIGHT", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-115", "QUEUE_DELAY", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-116", "REQUEST_DURATION", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-117", "ARCHIVE_SIZE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-118", "REDACTION", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-119", "PROVIDER_TIME", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-120", "REPLAY", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-121", "FAULT_INJECTION", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-122", "BUDGET_DENIAL", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-123", "BACKOFF", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-124", "ATTEMPT", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-125", "SOURCE_LINE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-126", "REQUEST_ID", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-127", "GAME_CODE", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-128", "DATE_PRIORITY", "GROUP_04", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-129", "LATEST_TOKEN", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-130", "DATE_TOKEN", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-131", "PRIMARY_STATE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-132", "BACKUP_STATE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-133", "EWMA_LATENCY", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-134", "HTTP_STATUS", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-135", "EMPTY_BODY", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-136", "NON_JSON", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-137", "BUSINESS_FAILURE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-138", "SCHEMA_FAILURE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-139", "TIMEOUT", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-140", "IO_ERROR", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-141", "FAILOVER", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-142", "HALF_OPEN", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-143", "OPEN_UNTIL", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-144", "SUCCESS_STREAK", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-145", "FAILURE_STREAK", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-146", "SINGLE_FLIGHT", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-147", "QUEUE_DELAY", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-148", "REQUEST_DURATION", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-149", "ARCHIVE_SIZE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-150", "REDACTION", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-151", "PROVIDER_TIME", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-152", "REPLAY", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-153", "FAULT_INJECTION", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-154", "BUDGET_DENIAL", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-155", "BACKOFF", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-156", "ATTEMPT", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-157", "SOURCE_LINE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-158", "REQUEST_ID", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-159", "GAME_CODE", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-160", "DATE_PRIORITY", "GROUP_05", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-161", "LATEST_TOKEN", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-162", "DATE_TOKEN", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-163", "PRIMARY_STATE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-164", "BACKUP_STATE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-165", "EWMA_LATENCY", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-166", "HTTP_STATUS", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-167", "EMPTY_BODY", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-168", "NON_JSON", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-169", "BUSINESS_FAILURE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-170", "SCHEMA_FAILURE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-171", "TIMEOUT", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-172", "IO_ERROR", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-173", "FAILOVER", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-174", "HALF_OPEN", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-175", "OPEN_UNTIL", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-176", "SUCCESS_STREAK", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-177", "FAILURE_STREAK", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-178", "SINGLE_FLIGHT", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-179", "QUEUE_DELAY", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-180", "REQUEST_DURATION", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-181", "ARCHIVE_SIZE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-182", "REDACTION", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-183", "PROVIDER_TIME", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-184", "REPLAY", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-185", "FAULT_INJECTION", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-186", "BUDGET_DENIAL", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-187", "BACKOFF", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-188", "ATTEMPT", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-189", "SOURCE_LINE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-190", "REQUEST_ID", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-191", "GAME_CODE", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-192", "DATE_PRIORITY", "GROUP_06", true));
        rules.add(new MetricSpec("TIANSHU-TRANSPORT-193", "LATEST_TOKEN", "GROUP_07", true));
        return Collections.unmodifiableList(rules);
    }

    public static boolean selfTest() {
        ApiTransportGovernanceEngine engine = new ApiTransportGovernanceEngine(45, 4, 64, 32);
        BinanceGameContract.RequestSpec request = BinanceGameContract.RequestSpec.latest(1);
        TransportResult result =
                engine.execute(
                        request,
                        new TransportAdapter() {
                            public HttpResponse execute(String url) {
                                return new HttpResponse(
                                        true,
                                        200,
                                        "{\"info\":\"success\",\"lotteryCode\":\"Binance1-PK10\",\"data\":[{}]}",
                                        "",
                                        1L);
                            }
                        });
        return result.success && metrics().size() >= 150 && engine.snapshot().eventCount == 1;
    }
}
