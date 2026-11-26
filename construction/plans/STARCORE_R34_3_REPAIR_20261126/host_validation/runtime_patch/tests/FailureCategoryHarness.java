import com.ethk3.novaonepro.strategyv2.FailureDispositionR31;

/** Exercises the actual platform-independent classifier from the installed source. */
public final class FailureCategoryHarness {
    static final class Typed extends RuntimeException implements FailureDispositionR31.ClassifiedFailure {
        final String category, outcome;
        Typed(String category, String outcome) { super("SLICE_TOO_LARGE"); this.category=category; this.outcome=outcome; }
        public String failureCategory() { return category; }
        public String operation() { return "FIXTURE"; }
        public String phase() { return "ADMISSION"; }
        public String commitOutcomeName() { return outcome; }
    }
    public static void main(String[] args) {
        if (!FailureDispositionR31.classify(new Typed("RESOURCE_WAIT","ROLLED_BACK")).consumesFaultBudget())
            throw new AssertionError("baseline unsupported category regression missing");
        FailureDispositionR31.Decision queue = FailureDispositionR31.classify(new Typed("RESOURCE_QUEUE","ROLLED_BACK"));
        if (!"RESOURCE_QUEUE".equals(queue.category) || queue.consumesFaultBudget())
            throw new AssertionError("queue resource rejection must not consume bug fault budget");
        FailureDispositionR31.Decision unknown = FailureDispositionR31.classify(new Typed("RESOURCE_QUEUE","UNKNOWN"));
        if (!unknown.requiresCommitResolution || unknown.alreadyCommitted)
            throw new AssertionError("unknown commit must retain receipt-resolution requirement");
        FailureDispositionR31.Decision committed = FailureDispositionR31.classify(new Typed("RESOURCE_QUEUE","COMMITTED"));
        if (!committed.alreadyCommitted || committed.requiresCommitResolution)
            throw new AssertionError("already-committed body must not be replayed");
        System.out.println("PASS: actual FailureDispositionR31 category and commit-outcome contracts");
    }
}
