package com.ethk3.novaonepro.strategyv2;
public final class RuntimeCoordinatorR26 {
    private static final ThreadLocal<Claim> current = new ThreadLocal<>();
    public static final class Claim {
        public final String task, owner; public final long fence, cursor = 0;
        public String reason;
        private long chunk;
        Claim(String task, String owner, long fence) { this.task=task; this.owner=owner; this.fence=fence; }
        public void reason(String reason) { this.reason=reason; }
        public void addWorkerCpuMillis(long millis) { if(millis<0)throw new AssertionError("negative CPU"); }
        public void inheritCommitChunksR31(Claim previous) { chunk=Math.max(chunk,previous.chunk); }
    }
    public static boolean hasCriticalPressure() { return false; }
    public static void bindBackgroundClaim(Claim claim) { current.set(claim); }
}
