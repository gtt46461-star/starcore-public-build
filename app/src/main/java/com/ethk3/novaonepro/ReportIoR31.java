package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Report output routing. The OS/file StorageBudget is authoritative across PIDs;
 * this scoped ThreadLocal only routes existing synchronous report callbacks to their owner. */
public final class ReportIoR31 {
    private ReportIoR31(){}
    public static final long STREAM_ADMISSION_SLAB=16L<<20,ATOMIC_HEADROOM=65536;
    private static final ThreadLocal<Scope> CURRENT=new ThreadLocal<>();
    private static final Map<String,StorageBudgetR31> HOST_FIXTURES=new HashMap<>();
    public static final class Scope implements Closeable {
        final File directory,dataRoot;final ReportNativeCP4.Control control;final Scope previous;final Thread owner=Thread.currentThread();boolean closed;
        Scope(File directory,ReportNativeCP4.Control control)throws IOException{
            this.directory=directory.getCanonicalFile();dataRoot=reportDataRoot(directory);this.control=Objects.requireNonNull(control);previous=CURRENT.get();CURRENT.set(this);
        }
        public void close()throws IOException{if(closed)return;if(owner!=Thread.currentThread()||CURRENT.get()!=this)throw new IOException("REPORT_IO_SCOPE_ORDER");closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}
    }
    public static boolean currentThreadHasScope(){return CURRENT.get()!=null;}
    public static Scope bind(File directory,ReportNativeCP4.Control control)throws IOException{return new Scope(directory,control);}
    private static boolean contains(File directory,File file)throws IOException{return file.getCanonicalPath().startsWith(directory.getCanonicalPath()+File.separator);}
    private static File reportDataRoot(File path)throws IOException{
        File directory=path.getCanonicalFile();while(directory!=null){if(directory.getName().equals("r981_reports")){File files=directory.getParentFile();return files==null?null:files.getParentFile();}directory=directory.getParentFile();}return null;
    }
    private static File dataRoot(File file)throws IOException{
        Scope scope=CURRENT.get();if(scope!=null&&scope.dataRoot!=null&&contains(scope.dataRoot,file))return scope.dataRoot;
        return reportDataRoot(file);
    }
    private static StorageBudgetR31 budget(File root)throws IOException{
        synchronized(HOST_FIXTURES){StorageBudgetR31 fixture=HOST_FIXTURES.get(root.getCanonicalPath());return fixture!=null?fixture:new StorageBudgetR31(root);}
    }
    private static String relative(File root,File file)throws IOException{
        File path=file.getAbsoluteFile();if(!path.equals(path.getCanonicalFile())||!contains(root,path))throw new IOException("REPORT_IO_PRIVATE_PATH");return path.getPath().substring(root.getCanonicalPath().length()+1);
    }
    private static ReportNativeCP4.Control control(File file,ReportNativeCP4.Control explicit)throws IOException{
        if(explicit!=null&&explicit.hasDiskAdmissionR31())return explicit;
        Scope scope=CURRENT.get();return scope!=null&&contains(scope.directory,file)&&scope.control.hasDiskAdmissionR31()?scope.control:null;
    }
    /** Actual sink wrappers close the underlying descriptor before releasing their independent grant. */
    public static OutputStream protect(File file,OutputStream raw,ReportNativeCP4.Control explicit)throws IOException{
        try{
            ReportNativeCP4.Control owner=control(file,explicit);if(owner!=null)return owner.protectOutput(file,raw);
            File root=dataRoot(file);if(root==null)return raw; // Compatibility for non-report legacy users.
            StorageBudgetR31.Reservation reservation=budget(root).reserve("report-stream:"+StorageBudgetR31.sha(file.getPath().getBytes(StandardCharsets.UTF_8)),0,StorageBudgetR31.Category.REPORT_WORKSPACE,relative(root,file),STREAM_ADMISSION_SLAB,ReportPhaseBudgetR31.MAX_WRITTEN,true);
            OutputStream admitted;try{admitted=reservation.wrap(file,raw);}catch(IOException|RuntimeException|Error error){try{reservation.close();}catch(IOException close){error.addSuppressed(close);}throw error;}
            return new FilterOutputStream(admitted){boolean closed;
                private void grow(StorageBudgetR31.Rejected rejected,int count)throws IOException{if(!"PHYSICAL_GROWTH_BOUND".equals(rejected.reason))throw rejected;reservation.ensureAdditional(Math.max((long)count,STREAM_ADMISSION_SLAB),ReportPhaseBudgetR31.MAX_WRITTEN,true);}
                public void write(int value)throws IOException{try{out.write(value);}catch(StorageBudgetR31.Rejected rejected){grow(rejected,1);out.write(value);}}
                public void write(byte[] bytes,int offset,int count)throws IOException{try{out.write(bytes,offset,count);}catch(StorageBudgetR31.Rejected rejected){grow(rejected,count);out.write(bytes,offset,count);}}
                public void close()throws IOException{if(closed)return;closed=true;try{out.close();}finally{reservation.close();}}
            };
        }catch(IOException|RuntimeException|Error error){try{raw.close();}catch(IOException close){error.addSuppressed(close);}throw error;}
    }
    /** Known-size metadata is admitted in full before AtomicFile can detach/truncate an old version. */
    public static final class AtomicPermit implements Closeable {
        private final StorageBudgetR31.Reservation reservation;private final long maximum;private long written;private boolean closed;
        AtomicPermit(StorageBudgetR31.Reservation reservation,long maximum){this.reservation=reservation;this.maximum=maximum;}
        public OutputStream stream(OutputStream raw){return new FilterOutputStream(raw){
            private void allow(int count)throws IOException{if(closed||count<0||count>maximum-written)throw new IOException("REPORT_ATOMIC_ADMISSION_SIZE");written+=count;}
            public void write(int value)throws IOException{allow(1);out.write(value);}public void write(byte[] bytes,int offset,int count)throws IOException{if(bytes==null||offset<0||count<0||offset>bytes.length-count)throw new IndexOutOfBoundsException();allow(count);out.write(bytes,offset,count);}
        };}
        public void close()throws IOException{if(closed)return;closed=true;if(reservation!=null)reservation.close();}
    }
    public static AtomicPermit beforeAtomic(File file,long bytes)throws IOException{
        if(bytes<0)throw new IllegalArgumentException("REPORT_ATOMIC_BYTES");
        for(File path:new File[]{file,new File(file.getPath()+".new"),new File(file.getPath()+".bak")})if(!path.getAbsoluteFile().equals(path.getCanonicalFile()))throw new IOException("REPORT_ATOMIC_SYMLINK");
        ReportNativeCP4.Control owner=control(file,null);if(owner!=null){owner.beforeOutputBytesR31(file,bytes);return new AtomicPermit(null,bytes);}
        File root=dataRoot(file);if(root==null)return new AtomicPermit(null,bytes);
        long maximum=Math.max(1,Math.addExact(bytes,ATOMIC_HEADROOM));StorageBudgetR31.Reservation reservation=budget(root).reserve("report-atomic:"+StorageBudgetR31.sha(file.getPath().getBytes(StandardCharsets.UTF_8)),0,StorageBudgetR31.Category.REPORT_WORKSPACE,relative(root,file),maximum,Math.multiplyExact(maximum,4),true);
        try{reservation.beforeChunk(bytes);return new AtomicPermit(reservation,bytes);}catch(IOException|RuntimeException|Error error){try{reservation.close();}catch(IOException close){error.addSuppressed(close);}throw error;}
    }
    public static long utf8Bytes(String value)throws IOException{
        long bytes=0;for(int n=0;n<value.length();n++){char c=value.charAt(n);if(c<=127)bytes++;else if(c<=2047)bytes+=2;else if(Character.isHighSurrogate(c)&&n+1<value.length()&&Character.isLowSurrogate(value.charAt(n+1))){bytes+=4;n++;}else bytes+=Character.isSurrogate(c)?1:3;if(bytes>8L*1024*1024)throw new IOException("REPORT_OBSERVATION_BYTE_BUDGET_8MIB");}return bytes;
    }
    /** An entire recovery/auxiliary artifact gets one grant, including its partial and receipt. */
    public static final class Artifact implements Closeable,ReportNativeCP4.Control {
        private final StorageBudgetR31.Reservation reservation;private final Scope scope;private final ReportPublicationCP12.Lease pin;private final java.util.function.BooleanSupplier cancelled;
        Artifact(StorageBudgetR31.Reservation reservation,File directory,java.util.function.BooleanSupplier cancelled)throws IOException{this.reservation=reservation;this.cancelled=Objects.requireNonNull(cancelled);pin=ReportPublicationCP12.shared(directory);try{scope=bind(directory,this);}catch(IOException|RuntimeException|Error failed){try{pin.close();}catch(IOException close){failed.addSuppressed(close);}throw failed;}}
        public boolean shouldContinue(){return !Thread.currentThread().isInterrupted()&&!cancelled.getAsBoolean();}public void checkpoint()throws IOException{if(!shouldContinue())throw new StorageBudgetR31.Rejected("CANCELLED").context(reservation.owner,reservation.fence,"RECOVERY_ZIP");}public boolean hasDiskAdmissionR31(){return true;}
        public void beforeOutputBytesR31(File file,long bytes)throws IOException{checkpoint();reservation.beforeChunk(bytes);}
        public OutputStream protectOutput(File file,OutputStream raw)throws IOException{OutputStream admitted=reservation.wrap(file,raw);return new FilterOutputStream(admitted){public void write(int value)throws IOException{checkpoint();out.write(value);}public void write(byte[] bytes,int offset,int count)throws IOException{checkpoint();out.write(bytes,offset,count);}};}
        public void close()throws IOException{try{scope.close();}finally{try{pin.close();}finally{reservation.close();}}}
    }
    public static Artifact reserveArtifact(File directory,long maximumGrowth)throws IOException{return reserveArtifact(directory,maximumGrowth,()->false);}
    public static Artifact reserveArtifact(File directory,long maximumGrowth,java.util.function.BooleanSupplier cancelled)throws IOException{
        File root=dataRoot(directory);if(root==null)throw new IOException("REPORT_ARTIFACT_ROOT_REQUIRED");
        StorageBudgetR31.Reservation reservation=budget(root).reserve("report-artifact:"+StorageBudgetR31.sha(directory.getPath().getBytes(StandardCharsets.UTF_8)),0,StorageBudgetR31.Category.REPORT_WORKSPACE,relative(root,directory),maximumGrowth,Math.multiplyExact(maximumGrowth,4),true);
        try{return new Artifact(reservation,directory,cancelled);}catch(IOException|RuntimeException|Error error){try{reservation.close();}catch(IOException close){error.addSuppressed(close);}throw error;}
    }
    /** Explicit host capacity fixture only. It cannot disable allocation requirements on Android. */
    public static Closeable hostFixture(File root,StorageBudgetR31 budget)throws IOException{
        String vm=System.getProperty("java.vm.name","").toLowerCase(Locale.ROOT),runtime=System.getProperty("java.runtime.name","").toLowerCase(Locale.ROOT);
        if(vm.contains("dalvik")||runtime.contains("android"))throw new IOException("HOST_FIXTURE_NOT_ANDROID");String key=root.getCanonicalPath();
        synchronized(HOST_FIXTURES){if(HOST_FIXTURES.size()>=4||HOST_FIXTURES.containsKey(key))throw new IOException("HOST_FIXTURE_BOUND");HOST_FIXTURES.put(key,budget);}
        return ()->{synchronized(HOST_FIXTURES){HOST_FIXTURES.remove(key);}};
    }
}
