import com.ethk3.novaonepro.RealtimeContractR33;
import com.ethk3.novaonepro.PostCanonicalObservationR33;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
public final class RealtimeContractR33HostTest {
 static int n;
 static void yes(boolean v){n++;if(!v)throw new AssertionError("case="+n);}
 static void fails(Runnable r){n++;try{r.run();throw new AssertionError("case="+n+" expected rejection");}catch(IllegalArgumentException|IllegalStateException expected){}}
 public static void main(String[] args)throws Exception {
  yes(RealtimeContractR33.coverage(2,1,100,99,true,101,true)==RealtimeContractR33.Coverage.FROZEN_ON_TIME);
  yes(RealtimeContractR33.coverage(2,1,100,100,true,101,true)==RealtimeContractR33.Coverage.MISSED_PREDRAW);
  yes(RealtimeContractR33.coverage(2,1,100,0,false,101,true)==RealtimeContractR33.Coverage.MISSED_PREDRAW);
  yes(RealtimeContractR33.coverage(2,1,100,0,false,99,true)==RealtimeContractR33.Coverage.NOT_DUE);
  yes(RealtimeContractR33.coverage(2,2,100,90,true,101,true)==RealtimeContractR33.Coverage.INVALID_PIT);
  yes(RealtimeContractR33.coverage(2,1,100,90,true,101,false)==RealtimeContractR33.Coverage.UNKNOWN_CLOCK);
  yes(RealtimeContractR33.monotonicSpan("a","b",10,20)==-1);
  yes(RealtimeContractR33.monotonicSpan("a","a",20,10)==-1);
  yes(RealtimeContractR33.monotonicSpan("a","a",10,20)==10);
  yes("UNKNOWN".equals(RealtimeContractR33.performance(-1,0,5000)));
  yes("FAIL".equals(RealtimeContractR33.performance(36417,134,5000)));
  yes(new RealtimeContractR33.WindowCounts(100,100,80,20,0,0).onTimeRate()==0.8);
  fails(()->new RealtimeContractR33.WindowCounts(80,100,80,20,0,0));
  String h=RealtimeContractR33.digest("x"),source="bb7b49bbcc9266bad548ec37e71403256fc06a4d";
  RealtimeContractR33.FeedbackEnvelope e=new RealtimeContractR33.FeedbackEnvelope("202611160729",h,h,h,source,"SHADOW");
  RealtimeContractR33.FeedbackEnvelope same=new RealtimeContractR33.FeedbackEnvelope("202611160729",h,h,h,source,"SHADOW");
  yes(e.eventId.equals(same.eventId));e.requireSame(same);
  RealtimeContractR33.requireInboxAck(e,e.eventId,RealtimeContractR33.inboxPayload(e),true);yes(true);
  fails(()->RealtimeContractR33.requireInboxAck(e,e.eventId,RealtimeContractR33.inboxPayload(e),false));
  fails(()->e.requireSame(new RealtimeContractR33.FeedbackEnvelope("202611160729",h,h,h,source,"REAL")));
  yes(!RealtimeContractR33.fields("a","bc").equals(RealtimeContractR33.fields("ab","c")));
  CountDownLatch blocked=new CountDownLatch(1),started=new CountDownLatch(1);
  yes(PostCanonicalObservationR33.offer(()->{started.countDown();try{blocked.await(3,TimeUnit.SECONDS);}catch(InterruptedException x){Thread.currentThread().interrupt();}}));
  yes(started.await(1,TimeUnit.SECONDS));
  for(int i=0;i<8;i++)yes(PostCanonicalObservationR33.offer(()->{}));
  long t=System.nanoTime();yes(!PostCanonicalObservationR33.offer(()->{}));yes((System.nanoTime()-t)/1000000<100);
  yes(((Number)PostCanonicalObservationR33.snapshot().get("rejected")).longValue()==1);
  blocked.countDown();
  System.out.println("PASS meaningful contract cases="+n+"; Android integration remains NOT_RUN");
 }
}
