package com.ethk3.novaonepro;

/** Reproduces byte-identity and admission-classification regressions without an Android runtime. */
public final class RuntimeR34Host {
    private static int checks;
    private static void check(boolean value, String label) {
        if (!value) throw new AssertionError(label);
        checks++;
    }
    public static void main(String[] args) {
        String padded = " \r\n{\"issue\":\"202611230288\"}\t\n";
        String oldLedger = V51ChecksumEngine.sha256(padded);
        String oldBinding = V51ChecksumEngine.sha256(padded.trim());
        check(!oldLedger.equals(oldBinding), "095 regression: body whitespace changes exact receipt identity");
        check(oldLedger.equals(RawHttpIdentityR34.payloadSha256(padded)), "R34 preserves received bytes");
        check(!RawHttpIdentityR34.payloadSha256(padded).equals(RawHttpIdentityR34.payloadSha256(padded.trim())), "No normalization accepted");
        check(RawHttpIdentityR34.payloadSha256(null).equals(V51ChecksumEngine.sha256("")), "Null transport body identity");
        RawHttpIdentityR34.requireNoOverwrite(null, oldLedger);
        RawHttpIdentityR34.requireNoOverwrite("", oldLedger);
        RawHttpIdentityR34.requireNoOverwrite(oldLedger, oldLedger);
        boolean collision = false;
        try { RawHttpIdentityR34.requireNoOverwrite(oldBinding, oldLedger); }
        catch (IllegalStateException expected) { collision = expected.getMessage().equals("R34_INTEGRITY_CONFLICT_HTTP_REQUEST_PAYLOAD"); }
        check(collision, "Existing receipt conflict cannot be overwritten");
        boolean invalid = false;
        try { RawHttpIdentityR34.requireNoOverwrite(null, "" ); }
        catch (IllegalArgumentException expected) { invalid = true; }
        check(invalid, "No empty receipt hash");
        String prefix = "DEFERRED_ACTION_TX_4:WRITE_COORDINATION_FAILED:F01_WRITE_FAILED|operation=R36_DEFERRED|category=DEPENDENCY|phase=ADMISSION|commitOutcome=ROLLED_BACK <- java.lang.IllegalStateException:";
        String backlog = prefix + DeferredWaitR34.WRITER_BACKLOG;
        check(DeferredWaitR34.isWriterBackpressure(backlog), "Known rolled-back admission is backpressure");
        check(DeferredPolicyExtract.transientResourceFailure(backlog), "Exact production policy retries backlog");
        check(!DeferredWaitR34.isWriterBackpressure(backlog.replace("ROLLED_BACK", "UNKNOWN")), "Unknown commit cannot be blindly retried");
        check(!DeferredWaitR34.isWriterBackpressure(backlog.replace("ADMISSION", "END")), "End failure is not admission wait");
        check(!DeferredWaitR34.isWriterBackpressure(DeferredWaitR34.WRITER_BACKLOG), "Untyped marker alone is insufficient");
        check(!DeferredPolicyExtract.transientResourceFailure("SNAPSHOT_HASH_MISMATCH " + backlog), "Integrity takes priority over wait marker");
        check(!DeferredPolicyExtract.transientResourceFailure("CURSOR_RANGE_-1"), "Cursor corruption remains terminal");
        check(DeferredPolicyExtract.transientResourceFailure("CP3_DB_BUSY_DEADLINE"), "Prior lock-busy contract retained");
        check(!DeferredPolicyExtract.transientResourceFailure("unknown application bug"), "No blanket exception swallowing");
        System.out.println("{\"java_checks\":" + checks + ",\"status\":\"PASS\"}");
    }
}
