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
        IdentityHashMap<Throwable, Boolean> seen = new IdentityHashMap<>();
        Decision result = null; Throwable current = failure;
        for (int i = 0; current != null && i < 16 && !seen.containsKey(current); i++) {
            seen.put(current, Boolean.TRUE);
            if (current instanceof VirtualMachineError || current instanceof ThreadDeath || current instanceof LinkageError)
                return new Decision(Disposition.FATAL, "PROCESS_FATAL", "UNKNOWN", false, false);
            if (current instanceof ClassifiedFailure) {
                ClassifiedFailure typed = (ClassifiedFailure) current;
                String outcome = typed.commitOutcomeName();
                if ("COMMITTED".equals(outcome) || "COMMITTED_DIAGNOSTIC_FAILED".equals(outcome))
                    return new Decision(Disposition.DEPENDENCY, "COMMITTED_ACK_RECOVERY", outcome, false, true);
                if ("UNKNOWN".equals(outcome))
                    return new Decision(Disposition.DEPENDENCY, "COMMIT_RESOLUTION_REQUIRED", outcome, true, false);
                String physical = physicalCauseCategoryR31(failure);
                result = category(physical == null ? typed.failureCategory() : physical, outcome);
                // Physical storage failures refine the category; the typed owner still owns the outcome.
                return result;
            }
            String name = current.getClass().getName(), message = String.valueOf(current.getMessage());
            if (current instanceof InterruptedException || current instanceof java.util.concurrent.CancellationException
                    || message.equals("R27_CANCELLED") || message.equals("R26_CONTINUATION_CANCELLED"))
                result = category("CANCELLED", "NOT_SUBMITTED");
            else if (current instanceof java.util.concurrent.TimeoutException) result = category("TEMP_TIMEOUT", "NOT_SUBMITTED");
            else if (name.endsWith("SQLiteFullException")) result = category("DISK_FULL", "ROLLED_BACK");
            else if (name.endsWith("SQLiteDatabaseCorruptException")) result = category("DB_CORRUPT", "UNKNOWN");
            else if (name.endsWith("SQLiteConstraintException")) result = category("INTEGRITY", "ROLLED_BACK");
            else if (name.endsWith("SQLiteDatabaseLockedException") || name.endsWith("SQLiteTableLockedException")
                    || name.endsWith("SQLiteBusyException") || message.startsWith("CP3_DB_BUSY_")
                    || message.startsWith("R909_BACKGROUND_WRITER_YIELD")) result = category("LOCK_BUSY", "NOT_SUBMITTED");
            else if (message.startsWith("R26_STALE_FENCE") || message.startsWith("R31_STALE_")
                    || message.startsWith("R30_PREPARED_OWNER_CHANGED")) result = category("STALE_OWNER", "NOT_SUBMITTED");
            else if(message.startsWith("RESOURCE_WAIT:MEMORY_CANCELLED")||message.startsWith("R31_MEMORY_CAPACITY_WAIT:CANCELLED")||message.startsWith("QUEUE_CAPACITY_WAIT:MEMORY_CANCELLED"))result=category("CANCELLED","NOT_SUBMITTED");
            else if(message.startsWith("R31_MEMORY_CAPACITY_WAIT")||message.startsWith("RESOURCE_WAIT:MEMORY_"))result=new Decision(Disposition.DEPENDENCY,"RESOURCE_MEMORY","NOT_SUBMITTED",false,false);
            else if(message.startsWith("QUEUE_CAPACITY_WAIT")||name.endsWith("QueueCapacityWait"))result=new Decision(Disposition.DEPENDENCY,"RESOURCE_QUEUE","NOT_SUBMITTED",false,false);
            else if (message.startsWith("MATRIX_ORIGINAL_STUDENT_UNAVAILABLE") || message.startsWith("MISSING_TEACHER")
                    || message.contains("SCHEMA_PENDING") || message.startsWith("R31_COMPUTE_CAPACITY")
                    || message.startsWith("R31_CONTINUATION_CANCELLING") || message.startsWith("R31_CONTINUATION_SLICE_IN_FLIGHT")
                    || message.startsWith("R26_UNSLICED_COMPUTATION_CAPACITY") || message.startsWith("R31_PREVIOUS_OWNER_DRAINING")
                    || name.endsWith("CapacityUnavailable")) result = category("DEPENDENCY", "NOT_SUBMITTED");
            current = current.getCause();
        }
        String physical = physicalCauseCategoryR31(failure);
        if (physical != null) return category(physical, "DB_CORRUPT".equals(physical) ? "UNKNOWN" : "ROLLED_BACK");
        return result == null ? category("BUG", "UNOBSERVED") : result;
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
            case "DEPENDENCY": disposition = Disposition.DEPENDENCY; break;
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
