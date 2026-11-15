package com.ethk3.novaonepro;
import android.os.CancellationSignal;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Real cross-process OS file leases and the actual owner cancellation code.
 * Android CancellationSignal is the explicitly adapted transport boundary. */
public final class SourceReaderOwnerR31Host {
 static int checks;static void ok(boolean v,String name){checks++;if(!v)throw new AssertionError(name);}
 public static void main(String[] args)throws Exception{
  Path root=Path.of(args[1]);Path database=root.resolve(ReleaseManifestR926.APPLICATION_ID).resolve("databases").resolve(AppConfig.DB_NAME);Files.createDirectories(database.getParent());if(!Files.exists(database))Files.write(database,new byte[]{1});File file=database.toFile();
  if(args[0].equals("child")){try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(file)){Files.writeString(root.resolve("reader-ready"),"CONTROLLED_READER");while(!Files.exists(root.resolve("reader-release")))Thread.sleep(5);}return;}
  try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(file,()->true,1000)){
   long start=System.nanoTime();ok(SourceReadLeaseR82.tryExclusiveCheckpoint(file,()->true)==null,"ACTIVE_READER_EXCLUSIVE_TRY_REFUSED");ok(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)<50,"NONBLOCKING_LOCAL_LEASE");Thread.sleep(4);ok(!SourceReadLeaseR82.requestStaleReaders(file,1),"CANCELLATION_REQUEST_IS_NOT_OWNER_CLOSED");ok(!lease.shouldContinue(),"ACTUAL_BOOLEAN_OWNER_OBSERVES_CANCELLATION");
  }
  ok(SourceReadLeaseR82.requestStaleReaders(file,1),"OWNER_ACTUALLY_CLOSED");
  try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(file,()->true,1000)){Thread.sleep(4);lease.progress(1);ok(!SourceReadLeaseR82.requestStaleReaders(file,60000),"ACTIVE_PROGRESS_STILL_PINNED");ok(lease.shouldContinue(),"ACTUAL_PROGRESS_DOES_NOT_EXPIRE");}
  CancellationSignal signal=new CancellationSignal();AtomicReference<Throwable> error=new AtomicReference<>();CountDownLatch ready=new CountDownLatch(1),closed=new CountDownLatch(1);
  Thread owner=new Thread(()->{try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(file,signal)){ready.countDown();while(!signal.isCanceled())Thread.sleep(1);}catch(Throwable failure){error.set(failure);}finally{closed.countDown();}});owner.start();ok(ready.await(2,TimeUnit.SECONDS),"SIGNAL_OWNER_STARTED");Thread.sleep(4);ok(!SourceReadLeaseR82.requestStaleReaders(file,1),"SIGNAL_CANCEL_REQUEST_NOT_FORCED_CLOSE");ok(closed.await(2,TimeUnit.SECONDS)&&error.get()==null,"OWNER_SIGNAL_CLOSES_ON_ITS_OWN_THREAD");
  Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),SourceReaderOwnerR31Host.class.getName(),"child",root.toString()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);while(!Files.exists(root.resolve("reader-ready"))&&System.nanoTime()<until)Thread.sleep(5);ok(Files.exists(root.resolve("reader-ready")),"REAL_CROSS_PROCESS_SHARED_READER");
  long before=System.nanoTime();ok(SourceReadLeaseR82.tryExclusiveCheckpoint(file,()->true)==null,"UNKNOWN_OTHER_PROCESS_READERS_NOT_CALLED_DRAINED");ok(TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-before)<100,"OS_LEASE_TRY_DOES_NOT_WAIT_UNDER_WRITER");Files.writeString(root.resolve("reader-release"),"EXPLICIT_OWNER_RELEASE");ok(child.waitFor()==0,"REMOTE_READER_OWNER_CLOSED");try(SourceReadLeaseR82 lease=SourceReadLeaseR82.tryExclusiveCheckpoint(file,()->true)){ok(lease!=null,"ACTUAL_DRAINED_OS_LEASE_REACHABLE");}
  System.out.println("SOURCE_READER_OWNER_NATIVE_PASS assertions="+checks+" realCrossProcessFileLocks=true noForeignConnectionClose=true AndroidSignalTransport=HOST_ADAPTED");
 }
}
