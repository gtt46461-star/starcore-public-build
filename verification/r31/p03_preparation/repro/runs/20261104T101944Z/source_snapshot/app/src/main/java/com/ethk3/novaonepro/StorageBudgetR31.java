package com.ethk3.novaonepro;

import java.io.*;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Cross-process disk admission. No business SQLite connection or deletion authority.
 * Reservations are promises of additional physical bytes, not heap measurements.
 * Only writers actually using this contract are admitted; snapshot() states that boundary.
 */
public final class StorageBudgetR31 {
    public static final long MiB=1024L*1024L;
    public static final String VERSION="R31_STORAGE_ADMISSION_V1";
    public static final int MAX_ACTIVE=64,MAX_RECEIPTS=64,MAX_LEDGER_BYTES=262144;
    private static final int MAGIC=0x52333142;
    private static final Object JVM_LOCK=new Object();
    private static final Set<String> OPEN=new HashSet<>();
    public enum Category { DATABASE,WAL,INPUT_RESEARCH,MODEL,REPORT_WORKSPACE,REPORT_PUBLISHED,CACHE,DIAGNOSTICS,BACKUP }
    public interface Space { long available(File root)throws IOException; }
    public static final class Config {
        public final long normalHard,transientHard,freeFloor,metadataHeadroom;public final String version;
        public Config(long normal,long transientBytes,long floor,long metadata,String version){
            if(normal<1||transientBytes<normal||floor<0||metadata<0||version==null||version.length()>128)throw new IllegalArgumentException("STORAGE_CONFIG");
            normalHard=normal;transientHard=transientBytes;freeFloor=floor;metadataHeadroom=metadata;this.version=version;
        }
        public static Config production(){return new Config(2048*MiB,4096*MiB,256*MiB,2*MiB,VERSION);}
        String hash(){return sha((version+"|"+normalHard+"|"+transientHard+"|"+freeFloor+"|"+metadataHeadroom).getBytes(StandardCharsets.UTF_8));}
    }
    public static final class Rejected extends IOException {
        public final String reason;
        Rejected(String reason){super("STORAGE_BUDGET_"+reason);this.reason=reason;}
    }
    private static final class Row {
        String id,owner,category,scope,boot;long fence,generation,bound,writeBound,charged,baseline,observed,observedCharged,created,pid;
        void write(DataOutputStream out)throws IOException{out.writeUTF(id);out.writeUTF(owner);out.writeUTF(category);out.writeUTF(scope);out.writeUTF(boot);for(long x:new long[]{fence,generation,bound,writeBound,charged,baseline,observed,observedCharged,created,pid})out.writeLong(x);}
        static Row read(DataInputStream in)throws IOException{Row r=new Row();r.id=in.readUTF();r.owner=in.readUTF();r.category=in.readUTF();r.scope=in.readUTF();r.boot=in.readUTF();r.fence=in.readLong();r.generation=in.readLong();r.bound=in.readLong();r.writeBound=in.readLong();r.charged=in.readLong();r.baseline=in.readLong();r.observed=in.readLong();r.observedCharged=in.readLong();r.created=in.readLong();r.pid=in.readLong();
            if(!r.id.matches("[a-f0-9]{64}")||r.owner.length()>160||r.scope.length()>512||r.fence<0||r.bound<1||r.writeBound<r.bound||r.charged<0||r.charged>r.writeBound||r.baseline<0||r.observed<0||r.observedCharged<0||r.observedCharged>r.charged)throw new IOException("STORAGE_LEDGER_ROW");Category.valueOf(r.category);return r;}
    }
    private static final class State {
        long generation;String configHash="";final LinkedHashMap<String,Row> active=new LinkedHashMap<>();final ArrayDeque<String> receipts=new ArrayDeque<>();
    }
    private final File root,directory;private final Config config;private final Space space;
    public StorageBudgetR31(File dataRoot)throws IOException{this(dataRoot,Config.production(),File::getUsableSpace);}
    /** Explicit small-capacity fixtures may use this constructor; production uses production(). */
    public StorageBudgetR31(File dataRoot,Config config,Space space)throws IOException{
        if(dataRoot==null||config==null||space==null)throw new IllegalArgumentException("STORAGE_INPUT");root=dataRoot.getCanonicalFile();if(!root.isDirectory())throw new IOException("STORAGE_ROOT_MISSING");this.config=config;this.space=space;
        directory=inside(root,"files/r31-storage");if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("STORAGE_LEDGER_DIRECTORY");
    }
    public static StorageBudgetR31 forFiles(File files)throws IOException{return new StorageBudgetR31(files.getCanonicalFile().getParentFile());}
    private interface Work<T>{T run(State state)throws IOException;}
    private <T>T locked(Work<T> work)throws IOException{
        synchronized(JVM_LOCK){File lockFile=inside(directory,"admission.lock");try(RandomAccessFile io=new RandomAccessFile(lockFile,"rw")){
            FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){throw new Rejected("COORDINATOR_BUSY");}if(lock==null)throw new Rejected("COORDINATOR_BUSY");
            try{State state=readState();if(!state.active.isEmpty()&&!state.configHash.equals(config.hash()))throw new Rejected("CONFIG_CHANGED_WITH_ACTIVE_OWNER");state.configHash=config.hash();return work.run(state);}finally{lock.release();}
        }}
    }
    public Reservation reserve(String owner,long fence,Category category,String relativeScope,long maximumGrowth,boolean transientWork)throws IOException{
        return reserve(owner,fence,category,relativeScope,maximumGrowth,maximumGrowth,transientWork);
    }
    public Reservation reserve(String owner,long fence,Category category,String relativeScope,long maximumGrowth,long maximumWritten,boolean transientWork)throws IOException{
        if(owner==null||owner.isEmpty()||owner.length()>160||fence<0||category==null||maximumGrowth<1||maximumWritten<maximumGrowth)throw new IllegalArgumentException("STORAGE_RESERVATION");
        final File scope=inside(root,relativeScope);final String relative=scope.getPath().substring(root.getPath().length()+1);
        return locked(state->{
            recoverWithin(state);
            for(Row row:state.active.values())if(overlap(row.scope,relative))throw new Rejected("SCOPE_ALREADY_OWNED");
            if(state.active.size()>=MAX_ACTIVE)throw new Rejected("RESERVATION_CAPACITY");
            List<String> scopes=new ArrayList<>();for(Row row:state.active.values())scopes.add(row.scope);scopes.add(relative);
            StorageCensusSU2.Census census=StorageCensusSU2.measure(root,scopes);
            if(census.truncated)throw new Rejected("CENSUS_INCOMPLETE");
            long outstanding=0;for(Row row:state.active.values()){
                long measured=census.scopeBytes.get(row.scope);long growth=Math.max(0,measured-row.baseline);
                if(growth>row.bound)throw new Rejected("EXISTING_OWNER_EXCEEDED_BOUND");
                outstanding=add(outstanding,row.bound-growth);
            }
            long limit=transientWork?config.transientHard:config.normalHard;
            if(add(add(add(census.total,outstanding),maximumGrowth),config.metadataHeadroom)>limit)throw new Rejected("BLOCKED_PROTECTED_DEBT");
            long available=available();if(add(add(outstanding,maximumGrowth),config.freeFloor)>available)throw new Rejected("FREE_SPACE_RESERVE");
            String id=sha((owner+"|"+fence+"|"+relative+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));File leaseFile=inside(directory,id+".lease");
            RandomAccessFile io=new RandomAccessFile(leaseFile,"rw");FileLock lifetime=null;
            try{
                lifetime=io.getChannel().tryLock();if(lifetime==null)throw new Rejected("OWNER_LEASE_BUSY");
                Row row=new Row();row.id=id;row.owner=owner;row.fence=fence;row.category=category.name();row.scope=relative;row.boot=boot();row.pid=pid();row.created=System.currentTimeMillis();row.bound=maximumGrowth;row.writeBound=maximumWritten;row.baseline=census.scopeBytes.get(relative);row.observed=row.baseline;row.generation=++state.generation;
                state.active.put(id,row);writeState(state);String key=leaseFile.getCanonicalPath();OPEN.add(key);return new Reservation(row,io,lifetime,key);
            }catch(IOException|RuntimeException failure){if(lifetime!=null)try{lifetime.release();}catch(IOException close){failure.addSuppressed(close);}try{io.close();}catch(IOException close){failure.addSuppressed(close);}throw failure;}
        });
    }
    public final class Reservation implements Closeable {
        public final String id,owner,scope;public final long fence,generation;
        private final RandomAccessFile io;private final FileLock lifetime;private final String key;private boolean closed;
        private Reservation(Row row,RandomAccessFile io,FileLock lifetime,String key){id=row.id;owner=row.owner;scope=row.scope;fence=row.fence;generation=row.generation;this.io=io;this.lifetime=lifetime;this.key=key;}
        /** Durable admission before the chunk is written. A failed write stays charged until measured reconciliation. */
        public synchronized void beforeChunk(long bytes)throws IOException{
            if(bytes<0)throw new IllegalArgumentException("STORAGE_CHUNK");if(closed||!lifetime.isValid())throw new Rejected("OWNER_CLOSED");if(bytes==0)return;
            locked(state->{Row row=require(state);if(bytes>row.writeBound-row.charged)throw new Rejected("WRITE_BOUND_EXCEEDED");
                long upper=add(Math.max(0,row.observed-row.baseline),row.charged-row.observedCharged);
                if(add(upper,bytes)>row.bound){row.observed=scopeBytes(inside(root,row.scope));row.observedCharged=row.charged;upper=Math.max(0,row.observed-row.baseline);}
                if(add(upper,bytes)>row.bound)throw new Rejected("PHYSICAL_GROWTH_BOUND");
                if(add(bytes,config.freeFloor)>available())throw new Rejected("FREE_SPACE_RESERVE");
                row.charged=add(row.charged,bytes);writeState(state);return null;
            });
        }
        public synchronized void reconcile()throws IOException{
            if(closed)throw new Rejected("OWNER_CLOSED");locked(state->{Row row=require(state);row.observed=scopeBytes(inside(root,row.scope));row.observedCharged=row.charged;if(Math.max(0,row.observed-row.baseline)>row.bound)throw new Rejected("PHYSICAL_GROWTH_BOUND");writeState(state);return null;});
        }
        /** Native backup reserves a worst-case page chunk first; only a measured unwritten remainder is refunded. */
        public synchronized void refundUnwritten(long admitted,long actualGrowth)throws IOException{
            if(closed||admitted<0||actualGrowth<0||actualGrowth>admitted)throw new IllegalArgumentException("STORAGE_CHUNK_RECONCILIATION");
            locked(state->{Row row=require(state);long refund=admitted-actualGrowth;if(refund>row.charged-row.observedCharged)throw new Rejected("CHUNK_RECONCILIATION_ORDER");row.charged-=refund;writeState(state);return null;});
        }
        public OutputStream wrap(File path,OutputStream target)throws IOException{
            File verified=path.getCanonicalFile(),allowed=inside(root,scope);if(!path.getAbsoluteFile().equals(verified)||(!verified.equals(allowed)&&!verified.getPath().startsWith(allowed.getPath()+File.separator)))throw new Rejected("OUTPUT_OUTSIDE_SCOPE");
            return new FilterOutputStream(target){public void write(int value)throws IOException{beforeChunk(1);out.write(value);}public void write(byte[] bytes,int offset,int count)throws IOException{if(bytes==null||offset<0||count<0||offset>bytes.length-count)throw new IndexOutOfBoundsException();beforeChunk(count);out.write(bytes,offset,count);}};
        }
        private Row require(State state)throws IOException{Row row=state.active.get(id);if(row==null||row.fence!=fence||row.generation!=generation||!row.owner.equals(owner))throw new Rejected("STALE_RESERVATION");return row;}
        /** Caller invokes only after its actual streams/native work have terminated, never from Future.cancel(). */
        public synchronized void close()throws IOException{
            if(closed)return;IOException failure=null;
            try{locked(state->{Row row=require(state);long actual=scopeBytes(inside(root,row.scope));state.active.remove(id);receipt(state,row,"FINISHED",actual);writeState(state);return null;});}
            catch(IOException error){failure=error;}
            finally{closed=true;synchronized(JVM_LOCK){OPEN.remove(key);}try{lifetime.release();}catch(IOException error){if(failure==null)failure=error;else failure.addSuppressed(error);}try{io.close();}catch(IOException error){if(failure==null)failure=error;else failure.addSuppressed(error);}File leaseFile=new File(key);if(leaseFile.exists()&&!leaseFile.delete()){IOException error=new IOException("STORAGE_OWNER_METADATA_CLEANUP_PENDING");if(failure==null)failure=error;else failure.addSuppressed(error);}}
            if(failure!=null)throw failure;
        }
    }
    /** OS lifetime lock, not wall-clock expiry, proves that an old writer is gone. No artifact is removed. */
    public int recoverReservations()throws IOException{return locked(state->{int recovered=recoverWithin(state);if(recovered>0)writeState(state);return recovered;});}
    private int recoverWithin(State state)throws IOException{
        int count=0;for(Row row:new ArrayList<>(state.active.values())){
            File file=inside(directory,row.id+".lease");if(OPEN.contains(file.getCanonicalPath()))continue;
            boolean recovered=false;try(RandomAccessFile io=new RandomAccessFile(file,"rw")){
                FileLock lock;try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){continue;}if(lock==null)continue;
                try{long physical=scopeBytes(inside(root,row.scope));state.active.remove(row.id);receipt(state,row,"RECOVERED_OWNER_TERMINATED_FILES_PRESERVED",physical);count++;recovered=true;}finally{lock.release();}
            }if(recovered&&file.exists()&&!file.delete())throw new IOException("STORAGE_OWNER_METADATA_CLEANUP_PENDING");
        }return count;
    }
    public Map<String,Object> snapshot()throws IOException{return locked(state->{List<String> scopes=new ArrayList<>();for(Row row:state.active.values())scopes.add(row.scope);StorageCensusSU2.Census c=StorageCensusSU2.measure(root,scopes);long pending=0;List<Map<String,Object>> owners=new ArrayList<>();
        for(Row row:state.active.values()){long actual=c.scopeBytes.get(row.scope),remaining=Math.max(0,row.bound-Math.max(0,actual-row.baseline));pending=add(pending,remaining);Map<String,Object> x=new LinkedHashMap<>();x.put("id",row.id);x.put("owner",row.owner);x.put("fence",row.fence);x.put("category",row.category);x.put("scope",row.scope);x.put("maximumGrowthBytes",row.bound);x.put("chargedWriteBytes",row.charged);x.put("scopePhysicalBytes",actual);x.put("unallocatedReservationBytes",remaining);x.put("boot",row.boot);x.put("pid",row.pid);owners.add(x);}
        Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("configurationHash",config.hash());out.put("measuredPhysicalFileBytes",c.total);out.put("complete",!c.truncated);out.put("incompleteReasons",new ArrayList<>(c.incompleteReasons));out.put("unallocatedReservationBytes",pending);out.put("physicalPlusPendingBytes",add(c.total,pending));out.put("normalAdmissionBytes",config.normalHard);out.put("transientAdmissionBytes",config.transientHard);out.put("freeSpaceBytes",available());out.put("owners",owners);out.put("receipts",new ArrayList<>(state.receipts));out.put("state",c.truncated?"UNKNOWN_INCOMPLETE_CENSUS":c.total>config.normalHard?"BLOCKED_PROTECTED_DEBT":"MEASURED");out.put("coverage","REGISTERED_WRITERS_ONLY; other growth sources must be explicitly integrated");out.put("memoryMetric",false);return out;});}
    private long available()throws IOException{long bytes=space.available(root);if(bytes<0)throw new Rejected("SPACE_MEASUREMENT_UNKNOWN");return bytes;}
    private static boolean overlap(String a,String b){return a.equals(b)||a.startsWith(b+"/")||b.startsWith(a+"/");}
    private static void receipt(State state,Row row,String reason,long actual){state.receipts.addLast(row.id+"|"+row.owner+"|"+row.fence+"|"+reason+"|physical="+actual+"|wall="+System.currentTimeMillis());while(state.receipts.size()>MAX_RECEIPTS)state.receipts.removeFirst();state.generation++;}
    public static long scopeBytes(File file)throws IOException{
        if(!file.exists())return 0;if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new Rejected("SCOPE_SYMLINK");if(file.isFile())return file.length();
        final class Frame {final DirectoryCursorR31 cursor;final int depth;Frame(File directory,int depth)throws IOException{cursor=DirectoryCursorR31.open(directory);this.depth=depth;}}
        long bytes=0;int entries=0;Deque<Frame> stack=new ArrayDeque<>();
        try{stack.push(new Frame(file,0));while(!stack.isEmpty()){
            if(Thread.currentThread().isInterrupted())throw new InterruptedIOException("STORAGE_SCOPE_CANCELLED");Frame frame=stack.peek();File child=frame.cursor.next();if(child==null){frame.cursor.close();stack.pop();continue;}
            if(++entries>200000)throw new Rejected("SCOPE_ENTRY_BUDGET");if(!child.getAbsoluteFile().equals(child.getCanonicalFile()))throw new Rejected("SCOPE_SYMLINK");
            if(child.isDirectory()){if(frame.depth>=16)throw new Rejected("SCOPE_DEPTH_BUDGET");stack.push(new Frame(child,frame.depth+1));}else if(child.isFile())bytes=add(bytes,child.length());else throw new Rejected("SCOPE_UNKNOWN_TYPE");
        }}finally{while(!stack.isEmpty())stack.pop().cursor.close();}return bytes;
    }
    private State readState()throws IOException{
        File file=inside(directory,"ledger.bin"),backup=inside(directory,"ledger.bin.bak");if(!file.exists()&&!backup.exists())return new State();
        try{return decode(readBounded(file,MAX_LEDGER_BYTES));}catch(IOException bad){if(!backup.exists())throw new IOException("STORAGE_LEDGER_CORRUPT_PRESERVED",bad);try{return decode(readBounded(backup,MAX_LEDGER_BYTES));}catch(IOException both){both.addSuppressed(bad);throw new IOException("STORAGE_LEDGER_CORRUPT_PRESERVED",both);}}
    }
    private static State decode(byte[] bytes)throws IOException{
        if(bytes.length<40)throw new IOException("STORAGE_LEDGER_SHORT");int n=bytes.length-32;byte[] payload=Arrays.copyOf(bytes,n);if(!Arrays.equals(Arrays.copyOfRange(bytes,n,bytes.length),digest(payload)))throw new IOException("STORAGE_LEDGER_HASH");
        State state=new State();try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload))){if(in.readInt()!=MAGIC)throw new IOException("STORAGE_LEDGER_VERSION");state.generation=in.readLong();state.configHash=in.readUTF();int active=in.readInt();if(active<0||active>MAX_ACTIVE)throw new IOException("STORAGE_LEDGER_BOUND");for(int i=0;i<active;i++){Row row=Row.read(in);if(state.active.put(row.id,row)!=null)throw new IOException("STORAGE_LEDGER_DUPLICATE");}int receipts=in.readInt();if(receipts<0||receipts>MAX_RECEIPTS)throw new IOException("STORAGE_RECEIPT_BOUND");for(int i=0;i<receipts;i++)state.receipts.add(in.readUTF());if(in.read()!=-1)throw new IOException("STORAGE_LEDGER_TRAILING");}catch(IllegalArgumentException malformed){throw new IOException("STORAGE_LEDGER_ENUM",malformed);}return state;
    }
    private void writeState(State state)throws IOException{
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(MAGIC);out.writeLong(state.generation);out.writeUTF(state.configHash);out.writeInt(state.active.size());for(Row row:state.active.values())row.write(out);out.writeInt(state.receipts.size());for(String row:state.receipts)out.writeUTF(row);}
        byte[] payload=bytes.toByteArray();bytes.write(digest(payload));if(bytes.size()>MAX_LEDGER_BYTES)throw new Rejected("LEDGER_CAPACITY");atomicWrite(inside(directory,"ledger.bin"),bytes.toByteArray());
    }
    static byte[] readBounded(File file,int maximum)throws IOException{if(!file.isFile()||file.length()>maximum)throw new IOException("STORAGE_METADATA_BOUND");try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(n>maximum-out.size())throw new IOException("STORAGE_METADATA_BOUND");out.write(buffer,0,n);}return out.toByteArray();}}
    static void atomicWrite(File file,byte[] bytes)throws IOException{
        if(bytes.length>MAX_LEDGER_BYTES||!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("STORAGE_METADATA_BOUND_OR_PATH");File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("STORAGE_METADATA_DIRECTORY");
        File backup=new File(file.getPath()+".bak"),partial=new File(file.getPath()+".new");
        // The new bytes are durable before any old version is detached. If a
        // valid backup already exists, retain it while atomically replacing base.
        try{try(FileOutputStream out=new FileOutputStream(partial)){out.write(bytes);out.flush();out.getFD().sync();}if(file.exists()&&!backup.exists()&&!file.renameTo(backup))throw new IOException("STORAGE_METADATA_BACKUP");if(!partial.renameTo(file))throw new IOException("STORAGE_METADATA_PUBLISH");if(backup.exists()&&!backup.delete())throw new IOException("STORAGE_METADATA_BACKUP_CLEANUP");}
        catch(IOException error){if(!file.exists()&&backup.exists()&&!backup.renameTo(file))error.addSuppressed(new IOException("STORAGE_METADATA_RESTORE_PENDING"));throw error;}
    }
    static File inside(File root,String name)throws IOException{if(name==null||name.isEmpty()||name.indexOf('\\')>=0||new File(name).isAbsolute())throw new IOException("STORAGE_PRIVATE_PATH");File file=new File(root,name).getAbsoluteFile();if(!file.equals(file.getCanonicalFile())||!file.getPath().startsWith(root.getCanonicalPath()+File.separator))throw new IOException("STORAGE_PRIVATE_PATH");return file;}
    static long add(long a,long b)throws IOException{try{return Math.addExact(a,b);}catch(ArithmeticException overflow){throw new IOException("STORAGE_BYTES_OVERFLOW",overflow);}}
    static byte[] digest(byte[] bytes){try{return MessageDigest.getInstance("SHA-256").digest(bytes);}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    static String sha(byte[] bytes){StringBuilder out=new StringBuilder();for(byte b:digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
    static String boot(){try{return new String(readBounded(new File("/proc/sys/kernel/random/boot_id"),128),StandardCharsets.US_ASCII).trim();}catch(IOException unavailable){return "UNKNOWN";}}
    static long pid(){try{return Long.parseLong(new File("/proc/self").getCanonicalFile().getName());}catch(IOException|RuntimeException unavailable){return -1;}}
}
