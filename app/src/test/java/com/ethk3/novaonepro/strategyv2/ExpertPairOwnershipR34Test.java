package com.ethk3.novaonepro.strategyv2;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

public final class ExpertPairOwnershipR34Test {
    private static final String SNAPSHOT = "08".repeat(32);

    private static final class OwnedModel implements OnlineModelsSU2.Model, AutoCloseable {
        int closes;
        public int stateWidth(){return 0;}
        public void predict(float[][] p,float[] st){}
        public void update(int[] draw){}
        public void write(DataOutput out){}
        public void read(DataInput in){}
        public void close(){closes++;}
    }

    @Test public void rightCapacityFailureReleasesTheAllocatedLeftSide() {
        ExpertExperimentR26.Spec spec=ExpertExperimentR26.spec("E05");
        LiveStateR26 left=new LiveStateR26(ExpertExperimentR26.genome(spec.left).bindBirth(SNAPSHOT,100));
        OwnedModel model=new OwnedModel();
        left.hub.models.put("owned-test-model",model);
        MemoryBudgetR31.CapacityWait original=new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);
        assertSame(original,assertThrows(MemoryBudgetR31.CapacityWait.class,()->
            new ExpertExperimentR26.Pair(spec,()->left,()->{throw original;})));
        assertEquals(1,model.closes);
        assertTrue(left.hub.models.isEmpty());
        left.close();
        assertEquals(1,model.closes);
    }

    @Test public void persistedPairStillRestoresBothLearners() throws IOException {
        ExpertExperimentR26.Spec spec=ExpertExperimentR26.spec("E05");
        try(ExpertExperimentR26.Pair initial=new ExpertExperimentR26.Pair(spec,SNAPSHOT,100)){
            initial.advance(new int[]{1,2,3,4,5,6,7,8,9,10},false);
            byte[] checkpoint=initial.encode();
            try(ExpertExperimentR26.Pair restored=ExpertExperimentR26.Pair.decode(checkpoint)){
                assertEquals(1,restored.cursor);
                assertEquals(1,restored.left.samples());
                assertEquals(1,restored.right.samples());
                assertArrayEquals(checkpoint,restored.encode());
            }
        }
    }
}
