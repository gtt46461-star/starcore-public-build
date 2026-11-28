import java.util.*;

/** Actual writerBusy source bodies, isolated from Android. The failure-category adapter below
 * supplies the typed category for these cases; this does NOT test production fallback classification,
 * SQLite, writer admission scheduling, prepared batch retention, APK execution or device performance. */
class WriterBusySourceContract {
    static final class DatabaseCoordinator {
        enum CommitOutcome { ROLLED_BACK, COMMITTED, COMMITTED_DIAGNOSTIC_FAILED, UNKNOWN }
        static final class WriteRejectedR31 extends IllegalStateException {
            final String category, phase;
            final CommitOutcome commitOutcome;
            WriteRejectedR31(String category,String phase,CommitOutcome outcome,Throwable cause) {
                super("fixture", cause); this.category=category; this.phase=phase; this.commitOutcome=outcome;
            }
        }
        static String failureCategoryR31(Throwable failure) {
            return failure instanceof WriteRejectedR31 ? ((WriteRejectedR31)failure).category : "BUG";
        }
    }
    static final class BaselineQueue {
    static boolean writerBusy(Throwable failure) {
        if (failure instanceof DatabaseCoordinator.WriteRejectedR31) {
            DatabaseCoordinator.WriteRejectedR31 rejected = (DatabaseCoordinator.WriteRejectedR31) failure;
            if (rejected.commitOutcome != DatabaseCoordinator.CommitOutcome.ROLLED_BACK) return false;
            if ("DEPENDENCY".equals(rejected.category)) {
                Throwable cause = rejected.getCause();
                return "ADMISSION".equals(rejected.phase) && cause != null
                        && "R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN".equals(cause.getMessage());
            }
        }
        String category = DatabaseCoordinator.failureCategoryR31(failure);
        return "LOCK_BUSY".equals(category) || "TEMP_TIMEOUT".equals(category);
    }
    }
    static final class CandidateQueue {
    static boolean writerBusy(Throwable failure) {
        if (failure instanceof DatabaseCoordinator.WriteRejectedR31) {
            DatabaseCoordinator.WriteRejectedR31 rejected = (DatabaseCoordinator.WriteRejectedR31) failure;
            if (rejected.commitOutcome != DatabaseCoordinator.CommitOutcome.ROLLED_BACK) return false;
            // Older persisted wrappers used DEPENDENCY; the current coordinator emits
            // RESOURCE_QUEUE for this exact, pre-transaction backlog admission hold.
            // Do not retry other resource failures, or an unproven commit outcome.
            if ("DEPENDENCY".equals(rejected.category) || "RESOURCE_QUEUE".equals(rejected.category)) {
                Throwable cause = rejected.getCause();
                return "ADMISSION".equals(rejected.phase) && cause != null
                        && "R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN".equals(cause.getMessage());
            }
        }
        String category = DatabaseCoordinator.failureCategoryR31(failure);
        return "LOCK_BUSY".equals(category) || "TEMP_TIMEOUT".equals(category);
    }
    }
    static final class Case {
        final String name; final Throwable failure; final boolean expected;
        Case(String name,Throwable failure,boolean expected){this.name=name;this.failure=failure;this.expected=expected;}
    }
    public static void main(String[] args) {
        String cause="R31_WRITER_HOLD_FAULT_BACKLOG_PENDING_DURABLE_DRAIN";
        List<Case> cases=new ArrayList<>();
        for(String category:new String[]{"DEPENDENCY","RESOURCE_QUEUE"}) {
            for(DatabaseCoordinator.CommitOutcome outcome:DatabaseCoordinator.CommitOutcome.values())
                cases.add(new Case(category+"/ADMISSION/"+outcome,
                        new DatabaseCoordinator.WriteRejectedR31(category,"ADMISSION",outcome,new IllegalStateException(cause)),
                        outcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK));
            for(String phase:new String[]{"BODY","END","READBACK","SCHEMA_PENDING"})
                cases.add(new Case(category+"/"+phase,new DatabaseCoordinator.WriteRejectedR31(category,phase,
                        DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException(cause)),false));
            int i=0;
            for(Throwable bad:new Throwable[]{null,new IllegalStateException("UNRELATED_QUEUE_CAPACITY"),
                    new IllegalStateException("database is locked"),new IllegalStateException(cause+":changed")})
                cases.add(new Case(category+"/bad-cause-"+(i++),new DatabaseCoordinator.WriteRejectedR31(category,
                        "ADMISSION",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,bad),false));
        }
        for(String category:new String[]{"INTEGRITY","STALE_OWNER","CANCELLED","DISK_FULL","BUG"})
            cases.add(new Case(category,new DatabaseCoordinator.WriteRejectedR31(category,"ADMISSION",
                    DatabaseCoordinator.CommitOutcome.ROLLED_BACK,new IllegalStateException(cause)),false));
        cases.add(new Case("untyped-string",new IllegalStateException(cause),false));
        int beforeFailed=0,afterFailed=0;
        List<String> failingBaseline=new ArrayList<>();
        for(Case test:cases) {
            if(BaselineQueue.writerBusy(test.failure)!=test.expected){beforeFailed++;failingBaseline.add(test.name);}
            if(CandidateQueue.writerBusy(test.failure)!=test.expected){afterFailed++;System.out.println("CANDIDATE_FAIL "+test.name);}
        }
        if(beforeFailed!=1||!failingBaseline.equals(Collections.singletonList("RESOURCE_QUEUE/ADMISSION/ROLLED_BACK")))
            throw new AssertionError("unexpected baseline failures "+failingBaseline);
        if(afterFailed!=0)throw new AssertionError("candidate failed "+afterFailed);
        System.out.println("SOURCE_EXTRACTED_CLASSIFIER cases="+cases.size()+" baselineExpectedFailures="+beforeFailed+" candidateFailures="+afterFailed);
        System.out.println("BASELINE_FAILURE "+failingBaseline.get(0));
        System.out.println("NOT_ANDROID_NOT_DATABASE_NOT_DEVICE_NO_FULL_JUNIT");
    }
}
