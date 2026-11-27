package com.ethk3.novaonepro;
public final class WriterAdmissionProbe {
    public static void main(String[] args) {
        String queue = "F01_WRITE_FAILED|operation=R36_DEFERRED|category=RESOURCE_QUEUE|phase=ADMISSION|commitOutcome=ROLLED_BACK <- java.lang.IllegalStateException:" + DeferredWaitR34.WRITER_BACKLOG;
        if (!DeferredWaitR34.isWriterBackpressure(queue)) throw new AssertionError("R36 admission misclassified");
        if (DeferredWaitR34.isWriterBackpressure(queue.replace("ROLLED_BACK","UNKNOWN")) || DeferredWaitR34.isWriterBackpressure(queue.replace(DeferredWaitR34.WRITER_BACKLOG,"SNAPSHOT_MISSING"))) throw new AssertionError("non-wait misclassified");
        System.out.println("QUEUE_ADMISSION_AND_NEGATIVES_PASS");
    }
}
