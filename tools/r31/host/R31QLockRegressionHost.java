package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** Real OS file-lock contention, separate JVMs, cancellation and admission.
 * No Android VM, handset, or model-quality claim. */
public final class R31QLockRegressionHost {
    private static void need(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    private static StorageBudgetR31 budget(File root)throws Exception{
        return new StorageBudgetR31(root,new StorageBudgetR31.Config(8L<<20,8L<<20,1024,65536,"R31Q_LOCK_HOST_FIXTURE"),f->64L<<20);
    }
    private static String id(int i){return String.format(Locale.ROOT,"%064x",i);}
    private static Process hold(File root)throws Exception{
        Process p=new ProcessBuilder(new File(System.getProperty("java.home"),"bin/java").getPath(),"-cp",System.getProperty("java.class.path"),R31QLockRegressionHost.class.getName(),"hold",root.getPath()).redirectError(ProcessBuilder.Redirect.INHERIT).start();
        need("LOCKED".equals(new BufferedReader(new InputStreamReader(p.getInputStream())).readLine()),"LOCK_CHILD_NOT_READY");return p;
    }
    private static void release(Process p)throws Exception{p.getOutputStream().write(1);p.getOutputStream().flush();need(p.waitFor(5,TimeUnit.SECONDS)&&p.exitValue()==0,"LOCK_CHILD_EXIT");}
    public static void main(String[] args)throws Exception{
        File root=new File(args[1]);root.mkdirs();File ledger=new File(root,"files/r31-storage");ledger.mkdirs();
        if(args[0].equals("hold")){
            try(RandomAccessFile io=new RandomAccessFile(new File(ledger,"admission.lock"),"rw");FileLock lock=io.getChannel().lock()){
                System.out.println("LOCKED");System.out.flush();System.in.read();
            }return;
        }
        StorageBudgetR31 b=budget(root);Process transientOwner=hold(root);
        ScheduledExecutorService release=Executors.newSingleThreadScheduledExecutor();
        release.schedule(()->{try{release(transientOwner);}catch(Exception e){throw new RuntimeException(e);}},350,TimeUnit.MILLISECONDS);
        long started=System.nanoTime();
        try{
            try(StorageBudgetR31.Reservation r=b.reserve("interactive-report",1,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/report",8192,true)){
                need(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started)>=150,"DID_NOT_WAIT_FOR_OS_OWNER");r.beforeChunk(3);
            }
        }finally{release.shutdownNow();if(transientOwner.isAlive())transientOwner.destroyForcibly();}
        System.out.println("TRANSIENT_CROSS_PROCESS_ADMISSION=PASS");
        Process held=hold(root);byte[] before=Files.readAllBytes(new File(ledger,"ledger.bin").toPath());
        try{
            started=System.nanoTime();
            try{b.reserve("deadline-report",2,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/deadline",8192,true);throw new AssertionError("ENTERED_WITHOUT_OS_LOCK");}
            catch(StorageBudgetR31.Rejected busy){need("COORDINATOR_BUSY".equals(busy.reason)&&busy.owner.equals("deadline-report"),"WRONG_BUSY_CONTEXT");}
            long elapsed=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-started);
            need(elapsed>=4500&&elapsed<8000,"UNBOUNDED_OR_NO_WAIT:"+elapsed);
            need(Arrays.equals(before,Files.readAllBytes(new File(ledger,"ledger.bin").toPath())),"WROTE_LEDGER_WITHOUT_LOCK");
            System.out.println("PERMANENT_LOCK_DEADLINE_AND_NO_WRITE=PASS elapsedMs="+elapsed);
            AtomicReference<Throwable> result=new AtomicReference<>();AtomicBoolean interrupted=new AtomicBoolean();CountDownLatch entering=new CountDownLatch(1);
            Thread waiter=new Thread(()->{try{entering.countDown();b.reserve("cancelled-report",3,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/cancelled",8192,true);result.set(new AssertionError("CANCEL_ENTERED"));}
                catch(Throwable t){result.set(t);interrupted.set(Thread.currentThread().isInterrupted());}});
            waiter.start();entering.await();Thread.sleep(100);waiter.interrupt();waiter.join(2000);
            need(!waiter.isAlive()&&result.get() instanceof StorageBudgetR31.Rejected&&"CANCELLED".equals(((StorageBudgetR31.Rejected)result.get()).reason)&&interrupted.get(),"CANCEL_NOT_BOUNDED_OR_FLAG_LOST");
            System.out.println("LOCK_WAIT_CANCELLATION=PASS");
        }finally{release(held);}
        try{b.reserve("oversized-report",4,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/over",16L<<20,true);throw new AssertionError("CAPACITY_BYPASSED");}
        catch(StorageBudgetR31.Rejected denied){need("BLOCKED_PROTECTED_DEBT".equals(denied.reason),"WRONG_CAPACITY_REJECTION");}
        System.out.println("CAPACITY_AND_OWNER_CONTRACT_PRESERVED=PASS");
        ReportRequestStatusR31.failed(id(1),"COORDINATOR_BUSY; durable ACK unavailable");
        need(ReportRequestStatusR31.failure(id(2))==null&&ReportRequestStatusR31.failure(id(1)).contains("COORDINATOR_BUSY"),"FAILURE_ATTACHED_TO_DIFFERENT_REQUEST");
        for(int i=2;i<=17;i++)ReportRequestStatusR31.failed(id(i),"failure"+i);
        need(ReportRequestStatusR31.failure(id(1))==null&&"failure17".equals(ReportRequestStatusR31.failure(id(17))),"UNBOUNDED_FAILURE_HISTORY");
        System.out.println("SUBMITTER_FAILURE_FALLBACK_BOUNDED_AND_SCOPED=PASS");
    }
}
