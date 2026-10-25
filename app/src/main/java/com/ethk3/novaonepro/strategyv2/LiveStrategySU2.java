package com.ethk3.novaonepro.strategyv2;

import java.io.*;

/** A strategy that runs every period in the live loop: an SU2 program (ModelLiveSU2) or an R14 committee (CommitteeLiveSU2). */
public interface LiveStrategySU2 {
    String key();
    ModelLiveSU2.Decision decide();
    boolean settle(int[] draw);
    boolean hasPending();
    /** Pooled table behind the pending decision for local period t (ranks the 1,200 K3 actions), else null. */
    float[][] pendingTable(long t);
    void write(DataOutput o) throws IOException;
    void readInto(DataInput in) throws IOException;
}
