package com.ethk3.novaonepro;

import java.util.LinkedHashMap;

/** Process-local fallback for the UI that submitted the request. It is never a
 * durable acknowledgement, a successful export, or evidence that a job ran. */
public final class ReportRequestStatusR31 {
    private ReportRequestStatusR31() {}
    private static final int MAX_FAILURES=16,MAX_REASON=4096;
    private static final LinkedHashMap<String,String> FAILURES=new LinkedHashMap<>();
    public static synchronized void failed(String requestId,String reason) {
        if(requestId==null||!requestId.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("REPORT_REQUEST_ID");
        String text=reason==null?"REPORT_REQUEST_FAILED":reason;
        FAILURES.remove(requestId);FAILURES.put(requestId,text.substring(0,Math.min(MAX_REASON,text.length())));
        while(FAILURES.size()>MAX_FAILURES)FAILURES.remove(FAILURES.keySet().iterator().next());
    }
    public static synchronized String failure(String requestId) {return FAILURES.get(requestId);}
}
