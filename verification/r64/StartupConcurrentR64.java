package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;import java.io.*;import java.nio.file.*;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
public final class StartupConcurrentR64 {
 public static void main(String[]a)throws Exception{
  File f=new File(a[2]);Files.copy(Path.of(a[1]),f.toPath());SQLiteDatabase db=new SQLiteDatabase(a[0],f.getPath());
  DatabaseCoordinator owner=HostProductR974.store(db).coordinatorR36();
  HostProductR974.set(owner,"criticalSchemaReady",new AtomicBoolean(false));HostProductR974.set(owner,"criticalInitMonitorR64",new Object());
  SchemaDomainCoordinatorR926.verifyAndStamp(db,SchemaDomainCoordinatorR926.criticalContract());
  CountDownLatch entered=new CountDownLatch(1);AtomicInteger initializers=new AtomicInteger();AtomicReference<Throwable> failure=new AtomicReference<>();
  Thread one=new Thread(()->{try{owner.ensureCriticalSchemaOrThrow(x->{initializers.incrementAndGet();entered.countDown();Thread.sleep(16500);});}catch(Throwable t){failure.set(t);entered.countDown();}},"service-startup");
  Thread two=new Thread(()->{try{owner.ensureCriticalSchemaOrThrow(x->{initializers.incrementAndGet();});}catch(Throwable t){failure.set(t);}},"ui-data-startup");
  one.start();if(!entered.await(10,TimeUnit.SECONDS))throw new AssertionError("FIRST_START_FAILED");two.start();one.join(25000);two.join(10000);
  if(failure.get()!=null)throw new AssertionError("STARTUP_FAILURE",failure.get());
  if(one.isAlive()||two.isAlive()||initializers.get()!=1)throw new AssertionError("SINGLE_FLIGHT_FAILED");db.close();
  System.out.println("R64_STARTUP_CONCURRENT_PASS|firstInitMs=16500|concurrentConstructors=2|initializerCalls=1|no15000msLockTimeout=true|scope=PRODUCT_COORDINATOR_HOST_SQLITE_NOT_ANDROID_INSTALL");
 }
}
