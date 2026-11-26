class ResumePressurePreimage {
    static final class SystemClock { static long elapsedRealtime() {return now;} }
    static class Job { String mode="FULL_REPORT"; }
    final Job job = new Job();
    final Thread executingOwnerR341 = Thread.currentThread();
    static long now;
    long realtimeQuietAfterR341, stageParkedMsR341;
    static void advance(long nanos) { now += java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(nanos); }
    boolean probeControlR341() {
        if(now>=1800000L)return false;
        realtimeQuietAfterR341=now+2000L;
        return true;
    }
    public boolean shouldContinue(){
            if(!probeControlR341())return false;
            if(!"FULL_REPORT".equals(job.mode)||Thread.currentThread()!=executingOwnerR341)return true;
            long started=SystemClock.elapsedRealtime(), boundedUntil=Math.min(realtimeQuietAfterR341,started+2000L);
            if(boundedUntil<=started)return true;
            while(SystemClock.elapsedRealtime()<boundedUntil){
                if(!probeControlR341())return false;
                long left=Math.min(boundedUntil,realtimeQuietAfterR341)-SystemClock.elapsedRealtime();
                if(left<=0)break;
                advance(java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(Math.min(50L,left)));
            }
            stageParkedMsR341+=SystemClock.elapsedRealtime()-started;
            return probeControlR341();
        }

    public static void main(String[] args) {
        ResumePressurePreimage owner=new ResumePressurePreimage();
        long bytes=0L, input=48L*1024L*1024L;
        while (bytes<input && owner.shouldContinue()) {
            bytes+=32768;
            now+=1L;
        }
        if(bytes>=input || now<1800000L)throw new AssertionError("baseline requires a real timeout");
        System.out.println("{\"scope\":\"HOST_MONOTONIC_REPLAY_OF_ORIGINAL_OWNER\",\"inputBytes\":"
                +input+",\"readBytes\":"+bytes+",\"actualHoldMs\":"+owner.stageParkedMsR341
                +",\"elapsedMs\":"+now+",\"reason\":\"REPORT_STAGE_TIMEOUT\"}");
    }
}
