package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.AtomReplayR27;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class AtomNetTrainingEvidenceR34Test {
    @Test public void actualGradientAndParameterChangeSurviveDeterministicReplay() {
        AtomNetR27 continuous=new AtomNetR27(3,27);
        StatsR27.SplitMix rng=new StatsR27.SplitMix(77);
        double[][] x={{1,.2,.3},{.1,.4,.8}};
        int[] hits={1,0},numbers={2,4};
        continuous.trainBatch(x,hits,numbers,.001,rng);
        AtomNetR27.OptimizerStep first=continuous.lastStep();
        assertEquals(1,first.adamStep);
        assertEquals(2,first.batchSize);
        assertTrue(first.gradientNorm>0);
        assertTrue(first.parameterChange>0);
        AtomNetR27 restored=AtomNetR27.decode(continuous.encode());
        assertNull(restored.lastStep());
        StatsR27.SplitMix restoredRng=new StatsR27.SplitMix(rng.state);
        continuous.trainBatch(x,hits,numbers,.001,rng);
        restored.trainBatch(x,hits,numbers,.001,restoredRng);
        assertArrayEquals(continuous.encode(),restored.encode());
        assertEquals(first.inputHash,restored.lastStep().inputHash);
        assertEquals(continuous.lastStep().parameterChange,restored.lastStep().parameterChange,0);
    }
    @Test public void rejectsSameSizeCheckpointWithInvalidAdamClockOrWeight() {
        byte[] good=new AtomNetR27(3,27).encode();
        byte[] bad=good.clone();
        ByteBuffer.wrap(bad).putLong(8,-1);
        assertThrows(IllegalStateException.class,()->AtomNetR27.decode(bad));
        byte[] invalid=good.clone();
        ByteBuffer.wrap(invalid).putDouble(16,Double.NaN);
        assertThrows(IllegalStateException.class,()->AtomNetR27.decode(invalid));
        assertThrows(IllegalStateException.class,()->AtomNetR27.decode(Arrays.copyOf(good,good.length+1)));
    }
    @Test public void aLatePreviousLabelCannotSupplyTheCurrentTargetFeatures() {
        long cutoff=2_000_000L;
        assertNull(AtomReplayR27.buildAsOf(history(cutoff,true),24,cutoff));
        AtomReplayR27.VisibleFrame visible=AtomReplayR27.buildAsOf(history(cutoff,false),24,cutoff);
        assertNotNull(visible);
        assertEquals(cutoff-10_000,visible.lastKnownAt);
        assertEquals(10,visible.frame.rows());
        assertNotNull(visible.frame.values[9]);
    }
    private static ReplayDatasetR27 history(long cutoff,boolean late) {
        List<ReplayDatasetR27.Row> rows=new ArrayList<>();
        for(int i=0;i<=24;i++){
            long open=cutoff-(24-i)*60_000L;
            long seen=i==23&&late?cutoff+1:open+50_000;
            rows.add(new ReplayDatasetR27.Row(TargetIssueResolver.fromSequenceOrdinalSU1(200+i),200+i,
                new int[]{1,2,3,4,5,6,7,8,9,10},open,seen,ReplayDatasetR27.READER_ABI_R31,"DRAW_FIRST_SEEN"));
        }
        return ReplayDatasetR27.of(rows);
    }
}
