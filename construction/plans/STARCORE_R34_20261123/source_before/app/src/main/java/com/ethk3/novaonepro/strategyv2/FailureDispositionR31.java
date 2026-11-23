package com.ethk3.novaonepro.strategyv2;

import java.util.IdentityHashMap;

/** One bounded, platform-independent scheduling decision for every runtime/write entry point. */
public final class FailureDispositionR31 {
    private FailureDispositionR31() {}
    public enum Disposition { RETRY_LATER, CANCELLED, DEPENDENCY, STALE_OWNER, FAULT, FATAL }
    /** Implemented by the coordinator adapter without exposing Android SQLite types to host contracts. */
    public interface ClassifiedFailure {
        String failureCategory();
        String operation();
        String phase();
        String commitOutcomeName();
    }
    public static final class Decision {
        public final Disposition disposition;
        public final String category, commitOutcome;
        public final boolean requiresCommitResolution, alreadyCommitted;
        Decision(Disposition disposition, String category, String outcome, boolean resolve, boolean committed) {
            this.disposition = disposition; this.category = category; this.commitOutcome = outcome;
            this.requiresCommitResolution = resolve; this.alreadyCommitted = committed;
        }
        public boolean consumesFaultBudget() { return disposition == Disposition.FAULT; }
    }
    public static Decision classify(Throwable failure) {
        if (failure == null) return new Decision(Disposition.RETRY_LATER, "NO_FAILURE", "NOT_APPLICABLE", false, false);
        if(processFatalCause(failure)!=null)return new Decision(Disposition.FATAL,"PROCESS_FATAL","UNKNOWN",false,false);
        ClassifiedFailure typed=firstTypedR31(failure);
        if (typed != null) {
            String outcome = typed.commitOutcomeName();
            if ("COMMITTED".equals(outcome) || "COMMITTED_DIAGNOSTIC_FAILED".equals(outcome))
                return new Decision(Disposition.DEPENDENCY, "COMMITTED_ACK_RECOVERY", outcome, false, true);
            if ("UNKNOWN".equals(outcome))
                return new Decision(Disposition.DEPENDENCY, "COMMIT_RESOLUTION_REQUIRED", outcome, true, false);
            String physical = physicalCauseCategoryR31(failure);
            // Physical storage failures refine the category; the typed owner still owns the outcome.
            return category(physical == null ? typed.failureCategory() : physical, outcome);
        }
        String root=rootCategoryR31(failure);
        String outcome;
        switch(root){
            case "DISK_FULL":case "INTEGRITY":outcome="ROLLED_BACK";break;
            case "DB_CORRUPT":outcome="UNKNOWN";break;
            case "BUG":outcome="UNOBSERVED";break;
            default:outcome="NOT_SUBMITTED";
        }
        return category(root,outcome);
    }
    /** The one bounded cause-chain category shared by the R26 entry, the R27 pool, direct research writes,
     * task claims and the DatabaseCoordinator adapter. Precedence: physical storage failure, then the first
     * typed producer category, then cancellation, then stale owner, then the outermost transient/dependency
     * cause. A cause chain without an explicit category is a BUG; no blanket IllegalStateException retry. */
    public static String rootCategoryR31(Throwable failure){
        if(failure==null)return "BUG";
        String physical=physicalCauseCategoryR31(failure);if(physical!=null)return physical;
        String declared=null,cancelled=null,stale=null,other=null;
        IdentityHashMap<Throwable,Boolean> seen=new IdentityHashMap<>();
        for(Throwable cause=failure;cause!=null&&seen.size()<16&&seen.put(cause,Boolean.TRUE)==null;cause=cause.getCause()){
            if(declared==null&&cause instanceof ClassifiedFailure)declared=declaredCategoryR31(((ClassifiedFailure)cause).failureCategory());
            String own=ownCategoryR31(cause);
            if(own==null)continue;
            if(own.equals("CANCELLED"))cancelled=own;else if(own.equals("STALE_OWNER"))stale=own;else if(other==null)other=own;
        }
        if(declared!=null)return declared;
        if(cancelled!=null)return cancelled;
        if(stale!=null)return stale;
        return other==null?"BUG":other;
    }
    private static ClassifiedFailure firstTypedR31(Throwable failure){
        IdentityHashMap<Throwable,Boolean> seen=new IdentityHashMap<>();
        for(Throwable cause=failure;cause!=null&&seen.size()<16&&seen.put(cause,Boolean.TRUE)==null;cause=cause.getCause())
            if(cause instanceof ClassifiedFailure)return (ClassifiedFailure)cause;
        return null;
    }
    private static String declaredCategoryR31(String category){
        switch(category==null?"":category){
            case "LOCK_BUSY":case "TEMP_TIMEOUT":case "CANCELLED":case "STALE_OWNER":case "DEPENDENCY":case "RESOURCE_MEMORY":case "RESOURCE_QUEUE":
            case "DISK_FULL":case "DB_CORRUPT":case "INTEGRITY":case "BUG":return category;
            default:return "BUG";
        }
    }
    /** Category of one throwable on its own (no cause walk). Explicit names and messages only. */
    private static String ownCategoryR31(Throwable cause){
        String name=cause.getClass().getName(),simple=cause.getClass().getSimpleName(),message=String.valueOf(cause.getMessage()).toUpperCase(java.util.Locale.ROOT);
        if(cause instanceof InterruptedException||cause instanceof java.util.concurrent.CancellationException||message.contains("CANCELLED")||message.contains("WAIT_INTERRUPTED"))return "CANCELLED";
        if(message.contains("STALE_OWNER")||message.contains("STALE_FENCE")||message.contains("FENCE_LOST")||message.contains("OWNER_MISMATCH")||message.startsWith("R31_STALE_")||message.startsWith("R30_PREPARED_OWNER_CHANGED"))return "STALE_OWNER";
        if(simple.contains("SQLiteDatabaseLocked")||simple.contains("SQLiteTableLocked")||name.endsWith("SQLiteBusyException")||message.contains("DATABASE IS LOCKED")
                ||message.contains("DATABASE TABLE IS LOCKED")||message.contains("CP3_DB_BUSY_")||message.startsWith("R909_BACKGROUND_WRITER_YIELD"))return "LOCK_BUSY";
        if(message.startsWith("R31_MEMORY_CAPACITY_WAIT")||message.startsWith("RESOURCE_WAIT:MEMORY_"))return "RESOURCE_MEMORY";
        if(message.startsWith("QUEUE_CAPACITY_WAIT")||name.endsWith("QueueCapacityWait"))return "RESOURCE_QUEUE";
        if(message.contains("DEPENDENCY")||message.contains("SCHEMA_PENDING")||message.contains("NO SUCH TABLE")||message.contains("NO SUCH COLUMN")
                ||message.startsWith("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE")||message.startsWith("MISSING_TEACHER")||message.startsWith("R31_COMPUTE_CAPACITY")
                ||message.startsWith("R31_CONTINUATION_CANCELLING")||message.startsWith("R31_CONTINUATION_SLICE_IN_FLIGHT")||message.startsWith("R26_UNSLICED_COMPUTATION_CAPACITY")
                ||message.startsWith("R31_PREVIOUS_OWNER_DRAINING")||name.endsWith("CapacityUnavailable"))return "DEPENDENCY";
        if(cause instanceof java.util.concurrent.TimeoutException)return "TEMP_TIMEOUT";
        return null;
    }
    private static String physicalCauseCategoryR31(Throwable failure) {
        IdentityHashMap<Throwable,Boolean> seen = new IdentityHashMap<>();
        for (Throwable cause=failure; cause!=null && seen.size()<16 && seen.put(cause,Boolean.TRUE)==null; cause=cause.getCause()) {
            String name=cause.getClass().getSimpleName(), message=String.valueOf(cause.getMessage()).toUpperCase(java.util.Locale.ROOT);
            if (name.contains("SQLiteFull") || message.contains("SQLITE_FULL") || message.contains("DATABASE OR DISK IS FULL")) return "DISK_FULL";
            if (name.contains("Corrupt") || message.contains("SQLITE_CORRUPT") || message.contains("DATABASE DISK IMAGE IS MALFORMED")) return "DB_CORRUPT";
            if (name.contains("Constraint") || message.contains("CONSTRAINT FAILED") || message.contains("INTEGRITY_CONFLICT")) return "INTEGRITY";
        }
        return null;
    }
    public static Error processFatalCause(Throwable failure){
        IdentityHashMap<Throwable,Boolean> seen=new IdentityHashMap<>();
        for(Throwable current=failure;current!=null&&seen.size()<16&&seen.put(current,Boolean.TRUE)==null;current=current.getCause())
            if(current instanceof VirtualMachineError||current instanceof ThreadDeath||current instanceof LinkageError)return(Error)current;
        return null;
    }
    private static Decision category(String category, String outcome) {
        Disposition disposition;
        switch (category == null ? "" : category) {
            case "LOCK_BUSY": case "TEMP_TIMEOUT": disposition = Disposition.RETRY_LATER; break;
            case "CANCELLED": disposition = Disposition.CANCELLED; break;
            case "DEPENDENCY": case "RESOURCE_MEMORY": case "RESOURCE_QUEUE": disposition = Disposition.DEPENDENCY; break;
            case "STALE_OWNER": disposition = Disposition.STALE_OWNER; break;
            case "DISK_FULL": case "DB_CORRUPT": disposition = Disposition.FATAL; break;
            default: disposition = Disposition.FAULT;
        }
        return new Decision(disposition, category, outcome, false, false);
    }
    public static long backoffMillis(int streak, String task) {
        int exponent = Math.min(6, Math.max(0, streak - 1));
        return Math.min(1000, 25L << exponent) + Math.floorMod(task == null ? 0 : task.hashCode(), 17);
    }
    public static long quarantineMillis(int level) {
        long[] waits = {30_000, 120_000, 600_000, 1_800_000};
        return waits[Math.min(waits.length - 1, Math.max(0, level - 1))];
    }
}
