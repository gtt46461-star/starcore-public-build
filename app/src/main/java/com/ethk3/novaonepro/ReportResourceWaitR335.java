package com.ethk3.novaonepro;

/** Durable resource-wait arithmetic. No I/O, admission grant, cancellation or business success. */
public final class ReportResourceWaitR335 {
    public static final long MIN_RETRY_MS=5000L,MAX_RETRY_MS=60000L;
    private ReportResourceWaitR335() {}
    public static final class State {
        public final String reason,fingerprint,boot;
        public final long sinceWallMs,nextElapsedMs;
        public final int attempt;
        public State(String reason,String fingerprint,String boot,long since,long next,int attempt) {
            if(reason==null||reason.length()>512||fingerprint==null||fingerprint.length()>1024
                    ||boot==null||boot.length()>80||since<0||next<0||attempt<0
                    ||(reason.isEmpty()&&(attempt!=0||next!=0||since!=0||!fingerprint.isEmpty()||!boot.isEmpty()))
                    ||(!reason.isEmpty()&&(!isResourceWait(reason)||attempt==0||fingerprint.isEmpty()||boot.isEmpty())))
                throw new IllegalArgumentException("REPORT_RESOURCE_WAIT_STATE");
            this.reason=reason;this.fingerprint=fingerprint;this.boot=boot;
            sinceWallMs=since;nextElapsedMs=next;this.attempt=attempt;
        }
        public static State empty(){return new State("","","",0L,0L,0);}
        public boolean active(){return !reason.isEmpty();}
    }
    public static boolean isResourceWait(String reason) {
        return reason!=null&&(reason.startsWith("REPORT_RESOURCE_WAIT|")
            ||reason.equals("REPORT_INPUT_LEASE_BUSY")||reason.equals("REPORT_ATTACHMENT_LEASE_BUSY")
            ||reason.startsWith("REPORT_MEMORY_BUDGET_WAIT")||reason.equals("REPORT_DISK_BUDGET"));
    }
    public static long retryDelay(int attempt) {
        if(attempt<1)throw new IllegalArgumentException("REPORT_WAIT_ATTEMPT");
        return Math.min(MAX_RETRY_MS,MIN_RETRY_MS<<Math.min(4,attempt-1));
    }
    public static State rejected(State prior,String reason,String fingerprint,String boot,long wall,long elapsed) {
        if(prior==null||!isResourceWait(reason)||fingerprint==null||fingerprint.isEmpty()
                ||boot==null||boot.isEmpty()||wall<0||elapsed<0)
            throw new IllegalArgumentException("REPORT_WAIT_INPUT");
        boolean same=prior.active()&&prior.reason.equals(reason)&&prior.fingerprint.equals(fingerprint);
        int attempt=same?Math.min(Integer.MAX_VALUE-1,prior.attempt)+1:1;
        long delay=retryDelay(attempt),next=elapsed>Long.MAX_VALUE-delay?Long.MAX_VALUE:elapsed+delay;
        return new State(reason,fingerprint,boot,same?prior.sinceWallMs:wall,next,attempt);
    }
    /** Unknown or changed boot must reobserve admission; never reuse a foreign elapsed clock. */
    public static long remaining(State state,String currentBoot,long nowElapsed) {
        if(state==null||!state.active()||currentBoot==null||currentBoot.equals("UNKNOWN")
                ||!currentBoot.equals(state.boot)||nowElapsed<0||state.nextElapsedMs<=nowElapsed)return 0L;
        return Math.min(MAX_RETRY_MS,state.nextElapsedMs-nowElapsed);
    }
}
