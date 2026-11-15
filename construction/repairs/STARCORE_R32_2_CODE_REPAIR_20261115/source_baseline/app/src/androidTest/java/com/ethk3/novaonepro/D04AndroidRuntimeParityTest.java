package com.ethk3.novaonepro;

import android.content.Context;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * D04 Android-runtime engineering parity. The workflow packages the exact host-trained upstream
 * artifacts and raw predraw captures into androidTest assets, then compares this output byte-for-byte
 * numerically with host Java. Emulator evidence is not a physical-device or Future-effect claim.
 */
@RunWith(AndroidJUnit4.class)
public final class D04AndroidRuntimeParityTest {
    private static String read(InputStream in) throws Exception {
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[16384];int n;
        while((n=in.read(b))>=0)out.write(b,0,n);in.close();return new String(out.toByteArray(),StandardCharsets.UTF_8);
    }
    private static double[] x(int i) {
        double phase=(i%17)/16.0d,raw=.10d+.80d*phase;
        return new double[]{raw,Math.sin(i*.17d)*1.8d,.02d+(i%5)*.025d,.12d+(i%7)*.10d,
                .25d+(i%6)*.11d,.30d+(i%4)*.15d,.18d+(i%9)*.075d};
    }
    private static int y(int i) {
        double[] v=x(i);return v[0]+.10d*v[1]+.12d*v[5]>.58d?1:0;
    }
    private static String issue(int i){return String.format(Locale.US,"20260911%04d",i+1);}
    private static AdaptiveShadowLearnerEngine trainFtrl(int stop) {
        AdaptiveShadowLearnerEngine learner=new AdaptiveShadowLearnerEngine();
        for(int i=0;i<stop;i++){
            long frozen=1_000_000L+i*10L+1L;
            AdaptiveShadowLearnerEngine.Update u=learner.updateAfterSettlement(
                    new AdaptiveShadowLearnerEngine.OosRow(issue(i),frozen,frozen+5L,true,y(i),x(i)));
            assertTrue("FTRL_TRAIN_"+i+":"+u.status,u.updated);
        }
        ResearchArtifactCodecR811.Artifact a=learner.snapshotResearchArtifact();byte[] bytes=ResearchArtifactCodecR811.encode(a);
        assertTrue(ResearchArtifactCodecR811.verify(bytes,ResearchArtifactCodecR811.hash(a)));
        return AdaptiveShadowLearnerEngine.restore(ResearchArtifactCodecR811.decode(bytes));
    }
    private static ResearchTinyDeepLearnerR909.Network trainTiny(int stop) {
        ResearchTinyDeepLearnerR909.Network n=ResearchTinyDeepLearnerR909.Network.initial();
        for(int i=0;i<stop;i++)n.update(x(i),y(i));
        ResearchArtifactCodecR811.Artifact a=n.toArtifact(issue(stop-1));byte[] bytes=ResearchArtifactCodecR811.encode(a);
        assertTrue(ResearchArtifactCodecR811.verify(bytes,ResearchArtifactCodecR811.hash(a)));
        return ResearchTinyDeepLearnerR909.Network.from(ResearchArtifactCodecR811.decode(bytes));
    }
    private static double p95(List<Long> nanos) {
        Collections.sort(nanos);if(nanos.isEmpty())return 0;int q=Math.max(0,(int)Math.ceil(.95*nanos.size())-1);
        return nanos.get(q)/1e6;
    }

    @Test public void d04HostAndroidParityVectorAndBudget() throws Exception {
        Context test=InstrumentationRegistry.getInstrumentation().getContext();
        Context target=InstrumentationRegistry.getInstrumentation().getTargetContext();
        JSONObject root=new JSONObject(read(test.getAssets().open("d04_upstream_models.json")));
        UpstreamModelMathR932.Number[] models=new UpstreamModelMathR932.Number[4];
        for(int i=0;i<4;i++)models[i]=new UpstreamModelMathR932.Number(root.getJSONArray("numbers").getJSONObject(i));
        UpstreamModelMathR932.Gate gate=new UpstreamModelMathR932.Gate(root.getJSONObject("gate"));
        double[] slow=new double[UpstreamModelMathR932.NUMBER_INPUT];ArrayList<Long> upstreamTimes=new ArrayList<Long>();
        BufferedReader reader=new BufferedReader(new InputStreamReader(test.getAssets().open("d04_raw_upstream.jsonl"),StandardCharsets.UTF_8));
        DataOutputStream out=new DataOutputStream(new BufferedOutputStream(target.openFileOutput("d04_upstream_android.bin",Context.MODE_PRIVATE)));
        int issues=0;String line;
        try {
            while((line=reader.readLine())!=null&&issues<100){
                JSONObject row=new JSONObject(line);JSONArray proofs=row.getJSONArray("proofs");String proof=proofs.getString(0);
                assertTrue(proof.startsWith(RawTrainingInputR932.PREFIX));JSONObject raw=new JSONObject(proof.substring(RawTrainingInputR932.PREFIX.length()));
                double[][] state=TrainedModelMathR932.matrix(raw.getJSONArray("states"),10,4);double[][][] experts=new double[9][][];
                for(int e=0;e<9;e++)experts[e]=TrainedModelMathR932.matrix(raw.getJSONArray("expert_logits").getJSONArray(e),10,10);
                long begin=System.nanoTime();double[] features=UpstreamModelMathR932.features(state,experts);
                double[][][] projected=new double[4][][];
                for(int m=0;m<4;m++){
                    double[][] logits=models[m].logits(features,slow,experts);
                    V51SinkhornEngine.ProjectionResult pr=new V51SinkhornEngine().projectDetailed(logits,1,240,1e-10);
                    assertTrue("D04_ANDROID_SINKHORN_"+issues+"_"+m,pr.productionValid());projected[m]=pr.matrix.probability;
                }
                double[] weights=gate.weights(state,experts);upstreamTimes.add(System.nanoTime()-begin);
                for(double v:features)out.writeDouble(v);
                for(int m=0;m<4;m++)for(double[] position:projected[m])for(double v:position)out.writeDouble(v);
                for(double v:weights)out.writeDouble(v);
                for(int i=0;i<slow.length;i++)slow[i]=.95d*slow[i]+.05d*features[i];issues++;
            }
        } finally {reader.close();out.close();}
        assertEquals(100,issues);double upstreamP95=p95(upstreamTimes);assertTrue("UPSTREAM_P95="+upstreamP95,upstreamP95<5000d);

        assertFalse(ResearchTinyDeepLearnerR909.MAY_AUTHORIZE_REAL);
        AdaptiveShadowLearnerEngine ftrl=trainFtrl(30);ResearchTinyDeepLearnerR909.Network tiny=trainTiny(30);
        ArrayList<Long> onlineTimes=new ArrayList<Long>();
        DataOutputStream online=new DataOutputStream(new BufferedOutputStream(target.openFileOutput("d04_online_android.bin",Context.MODE_PRIVATE)));
        try {
            for(int i=30;i<60;i++){long t=System.nanoTime();double v=ftrl.predictResearch(x(i));onlineTimes.add(System.nanoTime()-t);online.writeDouble(v);}
            for(int i=30;i<60;i++){long t=System.nanoTime();double v=tiny.predict(x(i));onlineTimes.add(System.nanoTime()-t);online.writeDouble(v);}
        } finally {online.close();}
        double onlineP95=p95(onlineTimes);assertTrue("ONLINE_P95="+onlineP95,onlineP95<5000d);

        JSONObject metrics=new JSONObject();metrics.put("status","PASS");metrics.put("scope","ANDROID_RUNTIME_EMULATOR_NOT_PHYSICAL_DEVICE_NOT_EFFECT");
        metrics.put("issues",100);metrics.put("upstream_host_device_values",145800);metrics.put("online_host_device_values",60);
        metrics.put("upstream_p95_ms",upstreamP95);metrics.put("online_p95_ms",onlineP95);metrics.put("budget_ms",5000);
        metrics.put("effect_proven",false);metrics.put("may_authorize_real",false);
        OutputStream m=target.openFileOutput("d04_android_metrics.json",Context.MODE_PRIVATE);m.write((metrics.toString(2)+"\n").getBytes(StandardCharsets.UTF_8));m.close();
        System.out.println("R932_D04_ANDROID_RUNTIME_COMPLETE|issues=100|upstream_p95_ms="+upstreamP95+"|online_p95_ms="+onlineP95);
    }
}
