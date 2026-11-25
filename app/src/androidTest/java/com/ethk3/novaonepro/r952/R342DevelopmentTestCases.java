package com.ethk3.novaonepro.r952;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.ResearchSchemaCoordinatorR926;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.strategyv2.R341FoundryTestCases;
import com.ethk3.novaonepro.strategyv2.ResearchInputPinR30R1;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class R342DevelopmentTestCases {
    private R342DevelopmentTestCases() {}

    private static List<Models.Draw> history(int n) {
        List<Models.Draw> rows = new ArrayList<>();
        long now = System.currentTimeMillis();
        for (int i = 1; i <= n; i++) {
            Models.Draw draw = new Models.Draw();
            draw.issue = String.format(Locale.ROOT,"20261125%04d",i);
            draw.openTime = now - 2000L - (n-i)*60_000L;
            draw.fetchTime = draw.openTime + 100L;
            for (int j = 0; j < 10; j++) draw.numbers[j] = (i+j)%10+1;
            rows.add(draw);
        }
        return rows;
    }

    public static void independentFamilyRegistration(Context app) {
        DbStore store = new DbStore(app);
        try {
            long deadline = System.nanoTime() + 150_000_000_000L;
            while (!ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorForResearchR952(),8)) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(),System.nanoTime()<deadline);
                android.os.SystemClock.sleep(15L);
            }
            while (!AtomCatalogImportCP12.completeCP12(store.readable())) {
                assertTrue("Verified atom catalog did not finish",System.nanoTime()<deadline);
                AtomCatalogImportCP12.drainOneCP12(store,deadline);
            }
            assertEquals(AtomCatalogPinnedCP12.ROWS,AtomCatalogImportCP12.readCursorCP12(store.readable()).row);
            assertEquals("SEEDS_CHECKPOINT_INITIALIZED", ModernSeedFactoryCP12.maintain(store,deadline));
            assertFalse(ModernSeedFactoryCP12.completeCP12(store.readable()));
            DevelopmentProductionCP12.Wait linear = assertThrows(DevelopmentProductionCP12.Wait.class, () ->
                ModernFamilyFactoryCP12.prepare(store.readable(),CandidateAstR952.Family.LINEAR,0));
            assertEquals("WAIT_MOLECULE_CATALOG_VERIFICATION",linear.reason);
            assertEquals(520,store.saveDraws(history(520)));
            String familyResult = ModernFamilyFactoryCP12.maintain(store,deadline);
            assertTrue(familyResult,familyResult.matches("[0-9a-f]{64}"));
            assertFalse(ModernSeedFactoryCP12.completeCP12(store.readable()));
            try (Cursor row=store.readable().rawQuery(
                    "SELECT c.parent_id,c.future_n,c.may_create_real,p.input_hash,q.status FROM r952_candidate c "
                    + "JOIN research_training_protocol_v2 p ON p.candidate_id=c.candidate_id "
                    + "JOIN v57_research_task_r967 q ON q.input_hash=p.input_hash AND q.kind='TRAIN_CANDIDATE' "
                    + "WHERE c.candidate_id LIKE 'CP12-FAMILY-%'",null)) {
                assertTrue("Factory must commit the input, protocol and real training queue",row.moveToFirst());
                assertEquals("",row.getString(0));
                assertEquals(0,row.getInt(1));
                assertEquals(0,row.getInt(2));
                assertTrue(row.getString(3).matches("[a-f0-9]{64}"));
                assertFalse(row.moveToNext());
            }
        } finally { store.close(); }
    }

    public static void exactHistoryGap(Context ignored) {
        List<Models.Draw> rows=history(450);
        Models.Draw missing=rows.remove(350);
        DevelopmentProductionCP12.Wait failure=assertThrows(DevelopmentProductionCP12.Wait.class,
                () -> DevelopmentProductionCP12.selectHistoryCP12(rows,20,false));
        assertEquals("WAIT_HISTORY",failure.reason);
        assertTrue(failure.getCause().getMessage().contains("HISTORY_REQUIRED=441|HISTORY_ACTUAL=99"));
        assertTrue(failure.getCause().getMessage().contains("BREAK=202611250350/202611250352"));
        rows.add(350,missing);
        List<Models.Draw> verified=DevelopmentProductionCP12.selectHistoryCP12(rows,20,false);
        assertEquals(450,verified.size());
        assertEquals("202611250450",verified.get(449).issue);
    }

    public static void checkpointReplay(Context ignored) {
        int n=300;
        String hash=V51ChecksumEngine.sha256("R342 development fixture");
        List<DevelopmentTrainerCP9.Row> rows=new ArrayList<>();
        String issue="202611250001";
        for(int i=0;i<n+120;i++){
            if(i==n)issue=TargetIssueResolver.next(issue).targetIssue;
            rows.add(new DevelopmentTrainerCP9.Row(issue,V51ChecksumEngine.sha256(issue),
                    new double[]{(i%20)/20.0}, new boolean[]{true},1+i%10));
            if(i+1<n+120)issue=TargetIssueResolver.next(issue).targetIssue;
        }
        CandidateCompilerR952.Modern shape=new CandidateCompilerR952.Modern(
                CandidateCompilerR952.ModelKind.SOFTMAX_LINEAR,CandidateAstR952.Family.LINEAR,
                2,1,0,n,61L,issue,hash,hash,hash,hash,
                new double[]{0},new double[]{1},new double[30],new int[0],new double[0],
                new int[0],new int[0],new double[0],Collections.emptyList());
        DevelopmentTrainerCP9.Dataset data=new DevelopmentTrainerCP9.Dataset(shape,hash,hash,
                Collections.singletonList("n0"),Collections.singletonList("MASK"),rows,n);
        DevelopmentTrainerCP9.State initial=DevelopmentTrainerCP9.start(data);
        for(int i=0;i<359;i++)DevelopmentTrainerCP9.step(data,initial);
        byte[] checkpoint=DevelopmentTrainerCP9.checkpoint(data,initial);
        DevelopmentTrainerCP9.State restored=DevelopmentTrainerCP9.restore(data,checkpoint);
        assertArrayEquals(checkpoint,DevelopmentTrainerCP9.checkpoint(data,restored));
        for(int step=0;step<30_000&&!initial.complete();step++){
            DevelopmentTrainerCP9.step(data,initial);
            DevelopmentTrainerCP9.step(data,restored);
        }
        assertTrue(initial.complete());
        assertTrue(restored.complete());
        assertTrue(restored.optimizerSteps()>0L);
        assertEquals(n,restored.normalizerCount(0));
        assertEquals(DevelopmentTrainerCP9.finish(data,initial,hash).hash(),
                     DevelopmentTrainerCP9.finish(data,restored,hash).hash());
    }

    public static void originalPinRecovery(Context app) throws Exception {
        R341FoundryTestCases.retainedTailAndProductionFoundry(app);
        try(SQLiteDatabase db=SQLiteDatabase.create(null)) {
            assertNull(ResearchInputPinR30R1.loadCurrent(db,"legacy-invalid-id"));
        }
    }
}
