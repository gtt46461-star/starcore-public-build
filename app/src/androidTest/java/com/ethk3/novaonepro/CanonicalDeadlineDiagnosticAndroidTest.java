package com.ethk3.novaonepro;

import android.content.Context;
import android.content.Intent;
import android.os.Debug;
import android.os.SystemClock;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/** First Shadow prediction only. Stack sampling adds overhead; this is not acceptance/performance proof. */
@RunWith(AndroidJUnit4.class)
public final class CanonicalDeadlineDiagnosticAndroidTest {
    private static void mark(String stage, AnalysisSingleFlightR36.Request request) {
        long wall=System.currentTimeMillis(),mono=TimeSourceR811.monoNowMs();
        System.out.println("CANONICAL_DIAGNOSTIC_STAGE|stage="+stage+"|wallMs="+wall
                +"|monoMs="+mono+"|elapsedMs="+SystemClock.elapsedRealtime()
                +"|threadCpuNs="+Debug.threadCpuTimeNanos()
                +"|remainingWallMs="+(request==null?"NA":Long.toString(request.deadlineAt-wall))
                +"|remainingMonoMs="+(request==null?"NA":Long.toString(request.deadlineMonoAt-mono)));
    }
    private static final class Sample {
        final long startNs,endNs; final String chain;
        Sample(long start,long end,String chain){startNs=start;endNs=end;this.chain=chain;}
    }
    private static final class Sampler implements Runnable {
        static final int MAX_SAMPLES=4096;
        final Thread target; final Sample[] samples=new Sample[MAX_SAMPLES];
        volatile boolean stop; volatile int count; volatile boolean clipped;
        volatile String error="";
        Sampler(Thread target){this.target=target;}
        @Override public void run() {
            try {
                while(!stop&&count<MAX_SAMPLES) {
                    long start=System.nanoTime();StackTraceElement[] trace=target.getStackTrace();
                    long end=System.nanoTime();StringBuilder chain=new StringBuilder();int frames=0;
                    for(StackTraceElement frame:trace) {
                        if(!frame.getClassName().startsWith("com.ethk3.novaonepro."))continue;
                        if(frames++==16)break;
                        if(chain.length()>0)chain.append('>');
                        chain.append(frame.getClassName()).append('.').append(frame.getMethodName())
                                .append(':').append(frame.getLineNumber());
                    }
                    samples[count++]=new Sample(start,end,chain.length()==0?"NO_APPLICATION_FRAME":chain.toString());
                    Thread.sleep(20L);
                }
                clipped=!stop&&count==MAX_SAMPLES;
            } catch(InterruptedException interrupted) {
                if(!stop)error="UNEXPECTED_INTERRUPTION";
                Thread.currentThread().interrupt();
            } catch(RuntimeException failure) {error=failure.getClass().getName();}
        }
        void report(Context context,boolean joined) {
            long total=0,max=0;Map<String,long[]> groups=new LinkedHashMap<>();
            for(int i=0;i<count;i++) {
                Sample s=samples[i];if(s==null)continue;
                total+=s.endNs-s.startNs;max=Math.max(max,s.endNs-s.startNs);
                long[] g=groups.get(s.chain);
                if(g==null){g=new long[]{0,s.startNs,s.endNs};groups.put(s.chain,g);}
                g[0]++;g[2]=s.endNs;
            }
            String summary="CANONICAL_DIAGNOSTIC_SAMPLER|samples="+count+"|maxSamples="+MAX_SAMPLES
                    +"|intervalMs=20|captureTotalNs="+total+"|captureMaxNs="+max
                    +"|clipped="+clipped+"|joined="+joined+"|error="+error
                    +"|scope=SAMPLING_OVERHEAD_PRESENT_NOT_ACCEPTANCE";
            System.out.println(summary);
            StringBuilder durable=new StringBuilder(summary).append('\n');
            for(Map.Entry<String,long[]> e:groups.entrySet()) {
                long[] g=e.getValue();durable.append("CANONICAL_DIAGNOSTIC_STACK|count="+g[0]
                        +"|firstCaptureNs="+g[1]+"|lastCaptureNs="+g[2]+"|chain="+e.getKey()).append('\n');
            }
            // After run and bounded join only: no file writes, locals or DB reads in sampled scope.
            File file=new File(context.getFilesDir(),"canonical_stack_diagnostic.txt");
            try(FileOutputStream output=new FileOutputStream(file)) {
                byte[] bytes=durable.toString().getBytes(StandardCharsets.UTF_8);
                output.write(bytes);output.getFD().sync();
                System.out.println("CANONICAL_DIAGNOSTIC_DURABLE|written=true|bytes="+bytes.length
                        +"|groups="+groups.size()+"|sampleBound="+MAX_SAMPLES+"|frameBound=16");
            } catch(IOException failure) {
                // Telemetry failure must not replace an original production exception.
                System.out.println("CANONICAL_DIAGNOSTIC_DURABLE|written=false|error="+failure.getClass().getName());
            }
        }
    }
    @Test public void observeFirstShadowPredictionWithBoundedStackSampling() throws Exception {
        Context context=ApplicationProvider.getApplicationContext();
        // Same existing user-stop boundary as ShadowCycleAndroidTest; never steal a lease.
        BootReceiver.persistServiceUserEnabled(context,false);
        BootReceiver.cancelAllRecovery(context,"SYNTHETIC_OWNED_CHAIN_FIXTURE");
        context.stopService(new Intent(context,AutoFetchService.class));
        long stoppedBy=SystemClock.elapsedRealtime()+10000L;
        while(AnalysisSingleFlightR36.shared().currentLease()!=null
                &&SystemClock.elapsedRealtime()<stoppedBy)SystemClock.sleep(20L);
        assertNull("watchdog owner must release normally",AnalysisSingleFlightR36.shared().currentLease());
        DbStore store=new DbStore(context);
        try {
            long schemaBy=SystemClock.elapsedRealtime()+60000L;
            while(!store.ensureResearchSidecarR811()) {
                assertTrue(ResearchSchemaCoordinatorR926.summary(),SystemClock.elapsedRealtime()<schemaBy);
                SystemClock.sleep(20L);
            }
            ArrayList<Models.Draw> history=new ArrayList<>();long latest=System.currentTimeMillis();
            mark("DRAW_STAMP",null);
            for(int row=1;row<=240;row++) {
                Models.Draw draw=new Models.Draw();draw.issue=String.format(Locale.US,"20260910%04d",row);
                draw.openTime=latest-(240-row)*60000L;
                for(int position=0;position<10;position++)draw.numbers[position]=((position+row-1)%10)+1;
                assertTrue(draw.isValid());history.add(draw);
            }
            mark("SAVE_BEGIN",null);assertEquals(240,store.saveDraws(history));mark("SAVE_END",null);
            AnalysisRequestStoreR36 requests=new AnalysisRequestStoreR36(store);
            mark("ENSURE_BEGIN",null);
            AnalysisSingleFlightR36.Request request=requests.ensurePending("202609100240","202609100241",
                    TargetIssueResolver.predrawDeadlineWallR20261008(store.getLatestDraw()),
                    "SYNTHETIC_SHADOW_CYCLE_NOT_NATURAL");
            mark("ENSURE_END",request);
            AnalysisSingleFlightR36.SubmitResult submitted=AnalysisSingleFlightR36.shared().submit(request);
            assertTrue("isolated owner required",submitted.startNow);assertNotNull(submitted.lease);
            AnalysisSingleFlightR36.Lease lease=submitted.lease;lease.bindCurrentThread();
            try {
                mark("CLAIM_BEGIN",request);request=requests.claim(request,lease.ownerId,System.currentTimeMillis());
                assertNotNull(request);mark("CLAIM_END",request);
                mark("FREEZE_BEGIN",request);V51RuntimeInputR36 input=V51RuntimeInputR36.freeze(history,request);
                mark("FREEZE_END",request);mark("BIND_BEGIN",request);
                request=requests.bindHistoryHash(request,input.historyHash,lease.ownerId);mark("BIND_END",request);
                Sampler sampler=new Sampler(Thread.currentThread());Thread thread=new Thread(sampler,"canonical-diagnostic-sampler");
                thread.setDaemon(true);mark("RUN_BEGIN",request);thread.start();
                V51RuntimeOrchestrator.OutcomeR36 outcome;
                try {outcome=V51AutoFetchBridge.runR36(store,input,request,lease);}
                finally {
                    mark("RUN_END_OR_THROW",request);sampler.stop=true;thread.interrupt();
                    thread.join(1000L);sampler.report(context,!thread.isAlive());
                }
                assertNotNull(outcome);assertTrue(outcome.runtime.success);assertTrue(outcome.commit.success);
                assertEquals(1200,outcome.runtime.actions.length);
                System.out.println("CANONICAL_DIAGNOSTIC_RETURNED_SUCCESS|scope=FIRST_PREDICTION_OBSERVATION_ONLY");
            } finally {AnalysisSingleFlightR36.shared().releaseAndTakeNext(lease);}
        } finally {store.close();}
    }
}
