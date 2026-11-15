package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import com.ethk3.novaonepro.*;
/** Full-report read pins protect immutable checkpoint files until export completes. */
public final class CheckpointPruneGuardR26 implements AutoCloseable {
    private final ReportInputsLeaseCP12 lease;
    private CheckpointPruneGuardR26(ReportInputsLeaseCP12 lease){this.lease=lease;}
    public static CheckpointPruneGuardR26 tryAcquire(File filesDir)throws IOException {
        ReportInputsLeaseCP12 lease=ReportInputsLeaseCP12.tryAcquireExclusive(filesDir);
        return lease==null?null:new CheckpointPruneGuardR26(lease);
    }
    @Override public void close()throws IOException {lease.close();}
}
