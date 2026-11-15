package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.lang.reflect.Field;
import org.junit.Test;

/** Exercises real committed-publication selection while a private learner advances. No quality claims. */
public final class R32PublishedModelDuringTrainingTest {
    private static RuntimeR27 runtime() throws Exception {
        Class<?> type=Class.forName("sun.misc.Unsafe");
        Field f=type.getDeclaredField("theUnsafe");f.setAccessible(true);
        return (RuntimeR27)type.getMethod("allocateInstance",Class.class).invoke(f.get(null),RuntimeR27.class);
    }
    private static AtomNetSessionR30 ready(String dataset){
        AtomNetSessionR30 s=new AtomNetSessionR30(dataset,"000000000999",999,12,80,40,20);
        s.phase="ONLINE";s.completeIssue=s.observedThrough;s.onlineObservedOrdinal=999;return s;
    }
    private static ModelSurfaceProviderR30.FrozenModelRef frozen(AtomNetSessionR30 s){
        return new ModelSurfaceProviderR30.FrozenModelRef(s.trainer.net.encode(),s.trainedThroughOrdinal(),1700000000000L);
    }
    @Test public void partialTrainingKeepsExactPreviousPublicationAndPredictions() throws Exception {
        RuntimeR27 rt=runtime();AtomNetSessionR30 s=ready("dataset");
        ModelSurfaceProviderR30.FrozenModelRef complete=frozen(s);rt.publishAtomStateR31(s,complete);
        double[] x=AtomNetR27.positionedInput(new double[]{.2,.7},1);
        double before=AtomNetR27.decode(complete.weights()).forward(x,null).pHit;
        s.online(new AtomNetTrainerR27.Sample(x,1,0,AtomNetSessionR30.eventKey("000000001000",1)),"000000001000",1000,1);
        rt.publishAtomStateR31(s,frozen(s));
        assertFalse(s.consumableR31());assertSame(complete,AtomNetRuntimeR30.forecastModelR32(rt,s));
        assertEquals(999,AtomNetRuntimeR30.forecastModelR32(rt,s).trainedThroughOrdinal);
        assertNotEquals(complete.weightsHash,StatsR27.sha256(s.trainer.net.encode()));
        assertEquals(before,AtomNetR27.decode(AtomNetRuntimeR30.forecastModelR32(rt,s).weights()).forward(x,null).pHit,0);
    }
    @Test public void validationRetainsPreviousCompleteModelWithoutPublishingDraft() throws Exception {
        RuntimeR27 rt=runtime();AtomNetSessionR30 s=ready("dataset");
        ModelSurfaceProviderR30.FrozenModelRef complete=frozen(s);rt.publishAtomStateR31(s,complete);
        s.phase="EVAL_ONLINE_DEVELOPMENT_VALIDATION";rt.publishAtomStateR31(s,null);
        assertFalse(s.ready());assertSame(complete,AtomNetRuntimeR30.forecastModelR32(rt,s));
        assertNull("a stale state cannot bind to a current publication",AtomNetRuntimeR30.forecastModelR32(rt,ready("dataset")));
    }
    @Test public void differentDatasetNeverBorrowsPreviousInferenceModel() throws Exception {
        RuntimeR27 rt=runtime();AtomNetSessionR30 old=ready("old");rt.publishAtomStateR31(old,frozen(old));
        AtomNetSessionR30 next=ready("new");next.phase="TRAIN";rt.publishAtomStateR31(next,null);
        assertNull(AtomNetRuntimeR30.forecastModelR32(rt,next));
    }
    @Test public void coldStartInPartialOrValidationStateStillFailsClosed() throws Exception {
        RuntimeR27 rt=runtime();AtomNetSessionR30 s=ready("dataset");s.partialIssue="000000001000";s.nextPosition=2;
        rt.publishAtomStateR31(s,frozen(s));assertNull(AtomNetRuntimeR30.forecastModelR32(rt,s));
        s.phase="EVAL_ONLINE_DEVELOPMENT_VALIDATION";rt.publishAtomStateR31(s,null);assertNull(AtomNetRuntimeR30.forecastModelR32(rt,s));
    }
}
