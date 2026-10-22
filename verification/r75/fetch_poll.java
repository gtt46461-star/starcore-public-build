package com.ethk3.novaonepro;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/** Synthetic provider responses against the real poll gate/transport. No live-source timing claim. */
public final class fetch_poll {
    private static int checks;
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
        checks++;
        System.out.println("PASS|" + name);
    }
    private static Models.Draw draw(String issue, long open) {
        Models.Draw d = new Models.Draw();
        d.issue = issue; d.openTime = open;
        d.numbers = new int[] {1,2,3,4,5,6,7,8,9,10};
        d.sourceLine = AppConfig.API_LINE_PRIMARY;
        return d;
    }
    private static ProviderFreshnessGateR3611.Decision poll(Models.Draw draw, String target, long now) {
        // The same executable fixture demonstrates the old product's missing failover.
        try {
            Method m = ProviderFreshnessGateR3611.class.getMethod(
                    "evaluateLatestPollR75", Models.Draw.class, String.class, long.class);
            return (ProviderFreshnessGateR3611.Decision)m.invoke(null, draw, target, now);
        } catch (NoSuchMethodException baseline) {
            return ProviderFreshnessGateR3611.evaluate(draw, target, now);
        } catch (Exception failure) { throw new RuntimeException(failure); }
    }
    private static String body(Models.Draw d) {
        return "{\"issue\":\""+d.issue+"\",\"openTime\":"+d.openTime+
                ",\"numbers\":[1,2,3,4,5,6,7,8,9,10]}";
    }
    public static void main(String[] args) throws Exception {
        long now = System.currentTimeMillis();
        final String next = "202610130323";
        Models.Draw previous = draw("202610130322", now - 65000L);
        check(ProviderFreshnessGateR3611.evaluate(previous,next,now).accepted,
                "OLD_DRAW_REMAINS_VALID_FOR_DURABLE_SETTLEMENT");
        check(!poll(previous,next,now).accepted,
                "DUE_PREVIOUS_DRAW_CANNOT_SUPPRESS_BACKUP_FOR_125_SECONDS");
        check("AWAITING_NEXT_ISSUE".equals(poll(previous,next,now).code),
                "PUBLICATION_WAIT_IS_EXPLICIT");
        check(poll(draw(previous.issue,now-59000L),next,now).accepted,
                "PRE_DEADLINE_OLD_DRAW_ACCEPTED");
        check(!poll(draw(previous.issue,now-60000L),next,now).accepted,
                "DEADLINE_BOUNDARY_TRIES_BACKUP");
        check(poll(draw(next,now-5000L),next,now).accepted,"CURRENT_DRAW_ACCEPTED");
        check(!poll(previous,"",now).accepted,"STARTUP_OLD_SOURCE_ALSO_TRIES_BACKUP");
        check(poll(currentDraw(next,now),"",now).accepted,"STARTUP_CURRENT_SOURCE_REMAINS_VALID");
        Models.Draw seconds=draw(previous.issue,(now-59000L)/1000L);
        check(poll(seconds,next,now).accepted,"SECONDS_TIMESTAMP_BEFORE_DEADLINE_ACCEPTED");
        check(seconds.openTime==(now-59000L)/1000L,"TIMESTAMP_NORMALIZATION_NEVER_MUTATES_SOURCE");
        check(!poll(draw(previous.issue,(now-65000L)/1000L),next,now).accepted,
                "SECONDS_TIMESTAMP_AFTER_DEADLINE_TRIES_BACKUP");
        check(!poll(draw("202610130321",now-65000L),next,now).accepted,"ISSUE_GAP_STILL_REJECTED");
        check(!poll(draw(previous.issue,now-126000L),next,now).accepted,"HISTORICAL_RESPONSE_STILL_REJECTED");
        final Models.Draw old = previous, current = draw(next,now-5000L);
        ApiTransportGovernanceEngine engine = new ApiTransportGovernanceEngine(1000,100,32,16);
        ApiTransportGovernanceEngine.TransportAdapter adapter = url ->
                new ApiTransportGovernanceEngine.HttpResponse(true,200,"{\"info\":\"success\"}","",500L);
        ApiTransportGovernanceEngine.TransportResult result = engine.execute(
                BinanceGameContract.RequestSpec.latest(5),adapter,(line,body,at) -> {
                    ProviderFreshnessGateR3611.Decision d=poll(
                            AppConfig.API_LINE_PRIMARY.equals(line)?old:current,next,now);
                    return d.accepted ? ApiTransportGovernanceEngine.PayloadDecision.accept()
                            : ApiTransportGovernanceEngine.PayloadDecision.reject(d.code,d.compact());
                });
        check(result.success && result.fromBackup && result.attempts==2,
                "PRIMARY_OLD_BACKUP_CURRENT_RETURNS_BACKUP_SAME_POLL");
        for(int i=0;i<10;i++) {
            result=engine.execute(BinanceGameContract.RequestSpec.latest(5),adapter,(line,body,at) -> {
                ProviderFreshnessGateR3611.Decision d=poll(old,next,now);
                return ApiTransportGovernanceEngine.PayloadDecision.reject(d.code,d.compact());
            });
            check(!result.success && result.attempts==2,"BOTH_OLD_RETRY_WITHOUT_BACKOFF_"+i);
        }
        check(engine.snapshot().primary.openUntilMs==0 && engine.snapshot().backup.openUntilMs==0,
                "DELAYED_PUBLICATION_NEVER_OPENS_NETWORK_CIRCUIT");
        result=engine.execute(BinanceGameContract.RequestSpec.latest(5),adapter,
                (line,body,at)->ApiTransportGovernanceEngine.PayloadDecision.accept());
        check(result.success,"FIRST_NEW_PUBLICATION_ACCEPTED_AFTER_STALE_STREAK");
        final int[] calls={0};
        final String primaryHost=new URL(BinanceGameContract.primary().endpoint()).getHost();
        URL.setURLStreamHandlerFactory(protocol -> "https".equals(protocol) ? new URLStreamHandler() {
            @Override protected URLConnection openConnection(URL url) {
                calls[0]++;
                final byte[] bytes=body(primaryHost.equals(url.getHost())?old:current).getBytes(StandardCharsets.UTF_8);
                return new HttpURLConnection(url) {
                    public void connect() {} public void disconnect() {}
                    public boolean usingProxy() { return false; }
                    public int getResponseCode() { return 200; }
                    public InputStream getInputStream() { return new ByteArrayInputStream(bytes); }
                };
            }
        } : null);
        Models.ApiResult live = new ApiClient().fetchLatest(next);
        check(live.ok && live.fromBackup && next.equals(live.latestDraw.issue) && calls[0]==2,
                "ACTUAL_API_CLIENT_LATEST_PARSES_AND_SELECTS_BACKUP");
        calls[0]=0;
        Models.ApiResult startup = new ApiClient().fetchLatest("");
        check(startup.ok && startup.fromBackup && next.equals(startup.latestDraw.issue) && calls[0]==2,
                "ACTUAL_API_CLIENT_COLD_START_ALSO_SELECTS_BACKUP");
        calls[0]=0;
        Models.ApiResult recovery = new ApiClient().fetchHistoryFresh(100,next);
        check(recovery.ok && recovery.fromBackup && next.equals(recovery.latestDraw.issue) && calls[0]==2,
                "ACTUAL_API_CLIENT_HISTORY_RECOVERY_ALSO_TRIES_BACKUP");
        String json="{\"status\":\"PASS\",\"checks\":"+checks+
                ",\"scope\":\"ACTUAL_POLL_GATE_TRANSPORT_SYNTHETIC_RESPONSES_NOT_DEVICE\"}\n";
        if(args.length>0)Files.writeString(Path.of(args[0]),json);
        System.out.println(json);
    }
    private static Models.Draw currentDraw(String issue,long now) { return draw(issue,now-5000L); }
}
