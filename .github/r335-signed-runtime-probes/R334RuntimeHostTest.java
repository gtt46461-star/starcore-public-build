import com.ethk3.novaonepro.FaultControlLoopR334;
import com.ethk3.novaonepro.DurableReturnTimingR334;
import java.util.ArrayDeque;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class R334RuntimeHostTest {
    private static int checks;
    private static void check(boolean ok,String reason) {
        checks++;
        if(!ok)throw new AssertionError(reason);
    }
    private static final class Virtual implements FaultControlLoopR334.Clock,FaultControlLoopR334.Scheduler {
        long now,delay;
        boolean accept=true;
        ArrayDeque<Runnable> work=new ArrayDeque<>();
        public long elapsedMs(){return now;}
        public boolean postDelayed(Runnable task,long wait){if(!accept)return false;delay=wait;work.add(task);return true;}
        public void remove(Runnable task){work.remove(task);}
        void run(){work.remove().run();}
    }
    public static void main(String[] args)throws Exception {
        Virtual v=new Virtual();
        FaultControlLoopR334 loop=new FaultControlLoopR334(1000,v,v,()->v.now=2500);
        loop.start();loop.start();
        check(v.work.size()==1,"duplicate start enqueued two owners");
        v.run();
        check(v.delay==500&&loop.snapshot()[3]==2,"late tick must skip, not flood or drift");
        loop.stop();check(v.work.isEmpty(),"stopped tick retained");
        v.accept=false;loop.start();
        check(loop.snapshot()[0]==0&&loop.snapshot()[4]==1,"rejected scheduler post hidden");

        Virtual restart=new Virtual();
        FaultControlLoopR334[] holder=new FaultControlLoopR334[1];
        holder[0]=new FaultControlLoopR334(1000,restart,restart,()->{holder[0].stop();holder[0].start();});
        holder[0].start();restart.run();
        check(restart.work.size()==1,"old in-flight generation rescheduled after restart");
        holder[0].stop();

        CountDownLatch researchEntered=new CountDownLatch(1),researchRelease=new CountDownLatch(1),faultRan=new CountDownLatch(1);
        ScheduledExecutorService research=Executors.newSingleThreadScheduledExecutor();
        ScheduledExecutorService dedicated=Executors.newSingleThreadScheduledExecutor();
        try {
            research.execute(()->{researchEntered.countDown();try{researchRelease.await();}catch(InterruptedException e){Thread.currentThread().interrupt();}});
            check(researchEntered.await(2,TimeUnit.SECONDS),"research block not installed");
            FaultControlLoopR334 separate=new FaultControlLoopR334(1000,()->System.nanoTime()/1000000L,
                new FaultControlLoopR334.Scheduler(){
                    public boolean postDelayed(Runnable task,long delay){dedicated.schedule(task,delay,TimeUnit.MILLISECONDS);return true;}
                    public void remove(Runnable task){}
                },()->faultRan.countDown());
            separate.start();
            check(faultRan.await(2,TimeUnit.SECONDS),"blocked research prevented fault scheduling");
            check(researchRelease.getCount()==1,"fault only ran after research was released");
            separate.stop();
        }finally{researchRelease.countDown();research.shutdownNow();dedicated.shutdownNow();}

        DurableReturnTimingR334 timing=new DurableReturnTimingR334(100000L,100L,90000L,250L);
        check(timing.elapsedMs==150L,"wall rollback corrupted duration");
        check(timing.returnedWallMs==90000L,"captured return identity changed");
        check(new DurableReturnTimingR334(1L,50L,3L,40L).elapsedMs==-1L,"invalid clock became 0ms success");
        System.out.println("R334_RUNTIME_HOST_PASS checks="+checks+"; Android lifecycle and device SLO NOT_RUN");
    }
}
