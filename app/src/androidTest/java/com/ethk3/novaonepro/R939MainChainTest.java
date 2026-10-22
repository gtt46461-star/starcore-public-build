package com.ethk3.novaonepro;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.ArrayList;
import java.util.Locale;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** Actual Android SQLite -> sole dispatcher -> canonical -> UI readback, with synthetic history. */
@RunWith(AndroidJUnit4.class)
public final class R939MainChainTest {
    @Test public void completeProductionChainPublishesCurrentIssueNumbers() throws Exception {
        boolean runtimeIdentity=false;
        for(BuildIdentity.CheckResult c:BuildIdentity.verifyReport(ApplicationProvider.getApplicationContext()).checks)
            if("R36_RUNTIME".equals(c.id))runtimeIdentity=c.passed;
        assertTrue("Runtime identity must follow the actual release manifest",runtimeIdentity);
        DbStore store = new DbStore(ApplicationProvider.getApplicationContext());
        ArrayList<Models.Draw> history = new ArrayList<>();
        long latestOpenTime = System.currentTimeMillis();
        for (int row=1; row<=240; row++) {
            Models.Draw d=new Models.Draw();
            d.issue=String.format(Locale.US,"20260910%04d",row);
            d.openTime=latestOpenTime-(240-row)*60000L;
            for(int p=0;p<10;p++)d.numbers[p]=((p+row-1)%10)+1;
            assertTrue(d.isValid()); history.add(d);
        }
        assertEquals(240,store.saveDraws(history));
        AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);
        AnalysisSingleFlightR36.Request request=requests.ensurePending("202609100240","202609100241",TargetIssueResolver.predrawDeadlineWallR20261008(store.getLatestDraw()),"R939_SYNTHETIC_MAIN_CHAIN_ACCEPTANCE");
        AnalysisSingleFlightR36.SubmitResult submitted=AnalysisSingleFlightR36.shared().submit(request);
        assertTrue(submitted.startNow); assertNotNull(submitted.lease);
        AnalysisSingleFlightR36.Lease lease=submitted.lease;
        lease.bindCurrentThread();
        try {
            request=requests.claim(request,lease.ownerId,System.currentTimeMillis());
            assertNotNull(request);
            V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(history,request);
            request=requests.bindHistoryHash(request,input.historyHash,lease.ownerId);
            V51RuntimeOrchestrator.OutcomeR36 outcome=V51AutoFetchBridge.runR36(store,input,request,lease);
            assertNotNull(outcome); assertTrue(outcome.runtime.success); assertTrue(outcome.commit.success);
            assertEquals(1200,outcome.runtime.actions.length);
            assertTrue(CanonicalObservationWriterR36.hasCanonical(store,request.targetIssue));
            Models.Decision view=CanonicalDecisionViewAdapterR36.read(store,request.targetIssue);
            assertEquals(request.targetIssue,view.targetIssue);
            assertTrue(view.position>=1&&view.position<=10);
            assertTrue(DecisionGateEngine.b12K3(view.n1,view.n2,view.n3));
            assertEquals("CANONICAL_COMMITTED",requests.status(request.targetIssue));
            assertNotNull(view.decision); assertFalse(view.decision.isEmpty());
            assertEquals(view.n1,CanonicalDecisionViewAdapterR36.read(new DbStore(ApplicationProvider.getApplicationContext()),request.targetIssue).n1);
            System.out.println("R939_ANDROID_MAIN_CHAIN_PASS|actions=1200|canonical=true|ui_numbers=true|decision="+view.decision+"|block="+view.blockCode+"|elapsed_ms="+outcome.runtime.elapsedMs+"|scope=SYNTHETIC_NOT_LIVE_PHONE");
        } finally {
            AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);
        }
    }
}
