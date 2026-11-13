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
    /** A competing process normally holds admission for milliseconds, not a failed export.
     * Use one monotonic deadline for both locks; never enter work without the OS lease. */
    public static final long COORDINATOR_WAIT_MS=5000L;
    private static final java.util.concurrent.locks.ReentrantLock JVM_LOCK=new java.util.concurrent.locks.ReentrantLock(true);
    private static final Set<String> OPEN=Collections.newSetFromMap(new java.util.concurrent.ConcurrentHashMap<String,Boolean>());
    public enum Category { DATABASE,WAL,INPUT_RESEARCH,MODEL,REPORT_WORKSPACE,REPORT_PUBLISHED,CACHE,DIAGNOSTICS,BACKUP }
    public interface Space { long available(File root)throws IOException; }
    public static final class Config {
        public final long normalHard,transientHard,freeFloor,metadataHeadroom;public final String version;public final boolean requireAllocated;
        public Config(long normal,long transientBytes,long floor,long metadata,String version){
            if(normal<1||transientBytes<normal||floor<0||metadata<0||version==null||version.length()>128)throw new IllegalArgumentException("STORAGE_CONFIG");
            normalHard=normal;transientHard=transientBytes;freeFloor=floor;metadataHeadroom=metadata;this.version=version;requireAllocated=VERSION.equals(version);
        }
        public static Config production(){return new Config(2048*MiB,4096*MiB,256*MiB,2*MiB,VERSION);}
        String hash(){return sha((version+"|"+normalHard+"|"+transientHard+"|"+freeFloor+"|"+metadataHeadroom).getBytes(StandardCharsets.UTF_8));}
    }
    public static final class Rejected extends IOException implements com.ethk3.novaonepro.strategyv2.FailureDispositionR31.ClassifiedFailure {
        public final String reason,owner,admissionOperation;public final long fence;
        Rejected(String reason){this(reason,"COMMON_BUDGET",-1,"ADMISSION");}
        private Rejected(String reason,String owner,long fence,String operation){super("STORAGE_BUDGET_"+reason+"|owner="+owner+"|fence="+fence);this.reason=reason;this.owner=owner;this.fence=fence;admissionOperation=operation;}
        Rejected context(String owner,long fence,String operation){Rejected result=new Rejected(reason,owner,fence,operation);result.initCause(this);return result;}
        public String failureCategory(){switch(reason){
            case "STALE_RESERVATION":case "OWNER_CLOSED":return "STALE_OWNER";
            case "CANCELLED":return "CANCELLED";
            case "CENSUS_STALE":case "COORDINATOR_BUSY":case "CONFIG_CHANGED_WITH_ACTIVE_OWNER":case "SCOPE_ALREADY_OWNED":case "RESERVATION_CAPACITY":case "CENSUS_INCOMPLETE":case "ALLOCATED_BYTES_UNKNOWN":case "BLOCKED_PROTECTED_DEBT":case "FREE_SPACE_RESERVE":case "OWNER_LEASE_BUSY":case "SPACE_MEASUREMENT_UNKNOWN":case "WRITE_BOUND_EXCEEDED":case "PHYSICAL_GROWTH_BOUND":case "SCOPE_ENTRY_BUDGET":case "SCOPE_DEPTH_BUDGET":return "DEPENDENCY";
            default:return "BUG";
        }}
        public String operation(){return admissionOperation;}public String phase(){return "STORAGE_ADMISSION";}
        public String commitOutcomeName(){return "NOT_SUBMITTED";} // Current admission, never an assertion about a prior file/SQL commit.
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
        final long deadline=System.nanoTime()+java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(COORDINATOR_WAIT_MS);
        boolean entered=false;
        try {
            try { entered=JVM_LOCK.tryLock(Math.max(0L,deadline-System.nanoTime()),java.util.concurrent.TimeUnit.NANOSECONDS); }
            catch(InterruptedException cancelled){Thread.currentThread().interrupt();throw new Rejected("CANCELLED");}
            if(!entered)throw new Rejected("COORDINATOR_BUSY");
            File lockFile=inside(directory,"admission.lock");try(RandomAccessFile io=new RandomAccessFile(lockFile,"rw")){
            FileLock lock;
            for(;;){
                if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED");
                try{lock=io.getChannel().tryLock();}catch(OverlappingFileLockException busy){lock=null;}
                if(lock!=null)break;
                long left=deadline-System.nanoTime();if(left<=0L)throw new Rejected("COORDINATOR_BUSY");
                java.util.concurrent.locks.LockSupport.parkNanos(Math.min(left,java.util.concurrent.TimeUnit.MILLISECONDS.toNanos(10L)));
            }
            try{State state=readState();if(!state.active.isEmpty()&&!state.configHash.equals(config.hash()))throw new Rejected("CONFIG_CHANGED_WITH_ACTIVE_OWNER");state.configHash=config.hash();return work.run(state);}finally{lock.release();}
            }
        } finally {if(entered)JVM_LOCK.unlock();}
    }
    public Reservation reserve(String owner,long fence,Category category,String relativeScope,long maximumGrowth,boolean transientWork)throws IOException{
        return reserve(owner,fence,category,relativeScope,maximumGrowth,maximumGrowth,transientWork);
    }
    private <T>T lockedContext(String owner,long fence,String operation,Work<T> work)throws IOException{try{return locked(work);}catch(Rejected rejected){throw rejected.context(owner,fence,operation);}}
    /** Census never runs under admission.lock. The commit checks the exact ledger
     * generation and scope set sampled before traversal; stale measurements fail closed. */
    private final class CensusTicket {
        long generation,started=System.nanoTime();List<String> scopes;StorageCensusSU2.Census census;
        void validate(State state)throws IOException {
            if(state.generation!=generation || System.nanoTime()-started>java.util.concurrent.TimeUnit.SECONDS.toNanos(30))
                throw new Rejected("CENSUS_STALE");
        }
    }
    private CensusTicket measureOutside(String extra)throws IOException {
        CensusTicket ticket=locked(state->{CensusTicket t=new CensusTicket();t.generation=state.generation;t.scopes=new ArrayList<>();
            for(Row row:state.active.values())t.scopes.add(row.scope);if(extra!=null&&!t.scopes.contains(extra))t.scopes.add(extra);return t;});
        ticket.census=StorageCensusSU2.measure(root,ticket.scopes);
        if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED");
        // A diagnostic snapshot must still expose incomplete/unknown allocation.
        // reserve/ensureAdditional check completeness under their commit lease
        // and continue to refuse every write admission from that measurement.
        return ticket;
    }
    private CensusTicket measureOutsideContextR31(String owner,long fence,String operation,String scope)throws IOException {
        try{return measureOutside(scope);}catch(Rejected rejected){throw rejected.context(owner,fence,operation);}
    }
    public Reservation reserve(String owner,long fence,Category category,String relativeScope,long maximumGrowth,long maximumWritten,boolean transientWork)throws IOException{
        long retryDeadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        for(int attempt=0;;attempt++)try{return reserveOnceR31(owner,fence,category,relativeScope,maximumGrowth,maximumWritten,transientWork);}
        catch(Rejected stale){
            // A generation race is detected before any grant is committed.
            // Remeasure outside the OS lease; never reuse or accept that ticket.
            if(!"CENSUS_STALE".equals(stale.reason)||attempt>=2||System.nanoTime()>=retryDeadline||Thread.currentThread().isInterrupted())throw stale;
        }
    }
    private Reservation reserveOnceR31(String owner,long fence,Category category,String relativeScope,long maximumGrowth,long maximumWritten,boolean transientWork)throws IOException{
        if(owner==null||owner.isEmpty()||owner.length()>160||fence<0||category==null||maximumGrowth<1||maximumWritten<maximumGrowth)throw new IllegalArgumentException("STORAGE_RESERVATION");
        final File scope=inside(root,relativeScope);final String relative=scope.getPath().substring(root.getPath().length()+1);
        if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED").context(owner,fence,"RESERVE");
        try{recoverReservations();}catch(Rejected rejected){throw rejected.context(owner,fence,"RESERVE");}
        final CensusTicket ticket=measureOutsideContextR31(owner,fence,"RESERVE",relative);
        return lockedContext(owner,fence,"RESERVE",state->{
            ticket.validate(state);
            for(Row row:state.active.values())if(overlap(row.scope,relative))throw new Rejected("SCOPE_ALREADY_OWNED");
            if(state.active.size()>=MAX_ACTIVE)throw new Rejected("RESERVATION_CAPACITY");
            StorageCensusSU2.Census census=ticket.census;
            if(census.truncated)throw new Rejected("CENSUS_INCOMPLETE");if(config.requireAllocated&&!census.allocatedBytesKnown)throw new Rejected("ALLOCATED_BYTES_UNKNOWN");
            long outstanding=0;for(Row row:state.active.values()){
                long measured=censusAccounted(census,row.scope);long growth=Math.max(0,measured-row.baseline);
                if(growth>row.bound)throw new Rejected("EXISTING_OWNER_EXCEEDED_BOUND");
                outstanding=add(outstanding,row.bound-growth);
            }
            long limit=transientWork?config.transientHard:config.normalHard;
            if(add(add(add(censusAccounted(census,null),outstanding),maximumGrowth),config.metadataHeadroom)>limit)throw new Rejected("BLOCKED_PROTECTED_DEBT");
            long available=available();if(add(add(add(outstanding,maximumGrowth),config.freeFloor),config.metadataHeadroom)>available)throw new Rejected("FREE_SPACE_RESERVE");
            String id=sha((owner+"|"+fence+"|"+relative+"|"+UUID.randomUUID()).getBytes(StandardCharsets.UTF_8));File leaseFile=inside(directory,id+".lease");
            RandomAccessFile io=new RandomAccessFile(leaseFile,"rw");FileLock lifetime=null;
            try{
                lifetime=io.getChannel().tryLock();if(lifetime==null)throw new Rejected("OWNER_LEASE_BUSY");
                Row row=new Row();row.id=id;row.owner=owner;row.fence=fence;row.category=category.name();row.scope=relative;row.boot=boot();row.pid=pid();row.created=System.currentTimeMillis();row.bound=maximumGrowth;row.writeBound=maximumWritten;row.baseline=censusAccounted(census,relative);row.observed=row.baseline;row.generation=++state.generation;
                state.active.put(id,row);writeState(state);String key=leaseFile.getCanonicalPath();OPEN.add(key);return new Reservation(row,io,lifetime,key);
            }catch(IOException|RuntimeException failure){if(lifetime!=null)try{lifetime.release();}catch(IOException close){failure.addSuppressed(close);}try{io.close();}catch(IOException close){failure.addSuppressed(close);}throw failure;}
        });
    }
    public final class Reservation implements Closeable {
        public final String id,owner,scope;public final long fence,generation;
        private final RandomAccessFile io;private final FileLock lifetime;private final String key;private boolean closed;
        private Reservation(Row row,RandomAccessFile io,FileLock lifetime,String key){id=row.id;owner=row.owner;scope=row.scope;fence=row.fence;generation=row.generation;this.io=io;this.lifetime=lifetime;this.key=key;}
        private <T>T owned(String operation,Work<T> work)throws IOException{return lockedContext(owner,fence,operation,work);}
        /** Durable admission before the chunk is written. A failed write stays charged until measured reconciliation. */
        public synchronized void beforeChunk(long bytes)throws IOException{
            if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED").context(owner,fence,"BEFORE_CHUNK");if(bytes<0)throw new IllegalArgumentException("STORAGE_CHUNK");if(closed||!lifetime.isValid())throw new Rejected("OWNER_CLOSED").context(owner,fence,"BEFORE_CHUNK");if(bytes==0)return;
            boolean needsMeasurement=owned("CHUNK_PREFLIGHT",state->{Row row=require(state);return add(add(Math.max(0,row.observed-row.baseline),row.charged-row.observedCharged),bytes)>row.bound;});
            if(needsMeasurement)reconcile();
            owned("BEFORE_CHUNK",state->{Row row=require(state);if(bytes>row.writeBound-row.charged)throw new Rejected("WRITE_BOUND_EXCEEDED");
                long upper=add(Math.max(0,row.observed-row.baseline),row.charged-row.observedCharged);
                if(add(upper,bytes)>row.bound)throw new Rejected("PHYSICAL_GROWTH_BOUND");
                if(add(add(bytes,config.freeFloor),config.metadataHeadroom)>available())throw new Rejected("FREE_SPACE_RESERVE");
                row.charged=add(row.charged,bytes);writeState(state);return null;
            });
        }
        /** Reserve a measured next phase before materialization; no physical bytes are reclaimed here. */
        public synchronized void ensureAdditional(long futureBytes,long maximumWritten,boolean transientWork)throws IOException{
            long retryDeadline=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
            for(int attempt=0;;attempt++)try{ensureAdditionalOnceR31(futureBytes,maximumWritten,transientWork);return;}
            catch(Rejected stale){if(!"CENSUS_STALE".equals(stale.reason)||attempt>=2||System.nanoTime()>=retryDeadline||Thread.currentThread().isInterrupted())throw stale;}
        }
        private void ensureAdditionalOnceR31(long futureBytes,long maximumWritten,boolean transientWork)throws IOException{
            if(closed)throw new Rejected("OWNER_CLOSED").context(owner,fence,"NEXT_PHASE");if(futureBytes<0||maximumWritten<0)throw new IllegalArgumentException("STORAGE_PHASE_BYTES");if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED").context(owner,fence,"NEXT_PHASE");
            final CensusTicket ticket=measureOutsideContextR31(owner,fence,"NEXT_PHASE",null);
            owned("NEXT_PHASE",state->{ticket.validate(state);Row own=require(state);
                StorageCensusSU2.Census census=ticket.census;if(census.truncated)throw new Rejected("CENSUS_INCOMPLETE");if(config.requireAllocated&&!census.allocatedBytesKnown)throw new Rejected("ALLOCATED_BYTES_UNKNOWN");
                long pending=0,ownActual=censusAccounted(census,own.scope),growth=Math.max(0,ownActual-own.baseline),target=Math.max(own.bound,add(growth,futureBytes));
                for(Row row:state.active.values()){long actual=censusAccounted(census,row.scope),used=Math.max(0,actual-row.baseline);if(used>row.bound)throw new Rejected("EXISTING_OWNER_EXCEEDED_BOUND");pending=add(pending,(row==own?target:row.bound)-used);}
                if(add(add(censusAccounted(census,null),pending),config.metadataHeadroom)>(transientWork?config.transientHard:config.normalHard))throw new Rejected("BLOCKED_PROTECTED_DEBT");
                if(add(add(pending,config.freeFloor),config.metadataHeadroom)>available())throw new Rejected("FREE_SPACE_RESERVE");own.bound=target;own.writeBound=Math.max(own.writeBound,Math.max(target,maximumWritten));own.observed=ownActual;own.observedCharged=own.charged;writeState(state);return null;
            });
        }
        public synchronized long maximumGrowthBytes()throws IOException{if(closed)throw new Rejected("OWNER_CLOSED").context(owner,fence,"OBSERVE_RESERVATION");return owned("OBSERVE_RESERVATION",state->require(state).bound);}
        public synchronized void reconcile()throws IOException{
            if(closed)throw new Rejected("OWNER_CLOSED").context(owner,fence,"RECONCILE");final long measured=accountedScopeBytes(inside(root,scope));owned("RECONCILE",state->{Row row=require(state);row.observed=measured;row.observedCharged=row.charged;if(Math.max(0,row.observed-row.baseline)>row.bound)throw new Rejected("PHYSICAL_GROWTH_BOUND");writeState(state);return null;});
        }
        /** Native backup reserves a worst-case page chunk first; only a measured unwritten remainder is refunded. */
        public synchronized void refundUnwritten(long admitted,long actualGrowth)throws IOException{
            if(closed)throw new Rejected("OWNER_CLOSED").context(owner,fence,"REFUND_UNWRITTEN");if(admitted<0||actualGrowth<0||actualGrowth>admitted)throw new IllegalArgumentException("STORAGE_CHUNK_RECONCILIATION");
            owned("REFUND_UNWRITTEN",state->{Row row=require(state);long refund=admitted-actualGrowth;if(refund>row.charged-row.observedCharged)throw new Rejected("CHUNK_RECONCILIATION_ORDER");row.charged-=refund;writeState(state);return null;});
        }
        public OutputStream wrap(File path,OutputStream target)throws IOException{
            File verified=path.getCanonicalFile(),allowed=inside(root,scope);if(!path.getAbsoluteFile().equals(verified)||(!verified.equals(allowed)&&!verified.getPath().startsWith(allowed.getPath()+File.separator)))throw new Rejected("OUTPUT_OUTSIDE_SCOPE").context(owner,fence,"OPEN_OUTPUT");
            return new FilterOutputStream(target){public void write(int value)throws IOException{beforeChunk(1);out.write(value);}public void write(byte[] bytes,int offset,int count)throws IOException{if(bytes==null||offset<0||count<0||offset>bytes.length-count)throw new IndexOutOfBoundsException();beforeChunk(count);out.write(bytes,offset,count);}};
        }
        private Row require(State state)throws IOException{Row row=state.active.get(id);if(row==null||row.fence!=fence||row.generation!=generation||!row.owner.equals(owner))throw new Rejected("STALE_RESERVATION");return row;}
        /** Caller invokes only after its actual streams/native work have terminated, never from Future.cancel(). */
        public synchronized void close()throws IOException{
            if(closed)return;IOException failure=null;
            try{final long measured=accountedScopeBytes(inside(root,scope));owned("FINISH",state->{Row row=require(state);long actual=measured;boolean exceeded=Math.max(0,actual-row.baseline)>row.bound;state.active.remove(id);receipt(state,row,exceeded?"FINISHED_BOUND_EXCEEDED_FILES_PRESERVED":"FINISHED",actual);writeState(state);if(exceeded)throw new Rejected("FINISHED_PHYSICAL_GROWTH_BOUND_EXCEEDED");return null;});}
            catch(IOException error){failure=error;}
            finally{closed=true;OPEN.remove(key);try{lifetime.release();}catch(IOException error){if(failure==null)failure=error;else failure.addSuppressed(error);}try{io.close();}catch(IOException error){if(failure==null)failure=error;else failure.addSuppressed(error);}File leaseFile=new File(key);if(leaseFile.exists()&&!leaseFile.delete()){IOException error=new IOException("STORAGE_OWNER_METADATA_CLEANUP_PENDING");if(failure==null)failure=error;else failure.addSuppressed(error);}}
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
                try{state.active.remove(row.id);receipt(state,row,"RECOVERED_OWNER_TERMINATED_FILES_PRESERVED_MEASUREMENT_PENDING",-1L);count++;recovered=true;}finally{lock.release();}
            }if(recovered&&file.exists()&&!file.delete())throw new IOException("STORAGE_OWNER_METADATA_CLEANUP_PENDING");
        }return count;
    }
    public Map<String,Object> snapshot()throws IOException{final CensusTicket ticket=measureOutside(null);return locked(state->{ticket.validate(state);StorageCensusSU2.Census c=ticket.census;long pending=0;List<Map<String,Object>> owners=new ArrayList<>();
        for(Row row:state.active.values()){long actual=censusAccounted(c,row.scope),remaining=Math.max(0,row.bound-Math.max(0,actual-row.baseline));pending=add(pending,remaining);Map<String,Object> x=new LinkedHashMap<>();x.put("id",row.id);x.put("owner",row.owner);x.put("fence",row.fence);x.put("category",row.category);x.put("scope",row.scope);x.put("maximumGrowthBytes",row.bound);x.put("chargedWriteBytes",row.charged);x.put("scopeAccountedBytes",actual);x.put("scopeRegularFileBytes",c.scopeBytes.get(row.scope));x.put("scopeAllocatedBytes",c.allocatedBytesKnown&&!c.truncated?c.allocatedScopeBytes.get(row.scope):null);x.put("unallocatedReservationBytes",remaining);x.put("boot",row.boot);x.put("pid",row.pid);owners.add(x);}
        Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("configurationHash",config.hash());out.put("measuredRegularFileBytes",c.total);out.put("measuredAllocatedBytes",c.allocatedBytesKnown&&!c.truncated?c.allocatedFileAndDirectoryBytes:null);out.put("accountingBasis",config.requireAllocated?"MAX_REGULAR_AND_ALLOCATED; UNKNOWN_BLOCKS_DENY_ADMISSION":"EXPLICIT_FIXTURE_FILE_LENGTH_WITH_ALLOCATED_IF_AVAILABLE");out.put("complete",!c.truncated&&(!config.requireAllocated||c.allocatedBytesKnown));out.put("incompleteReasons",new ArrayList<>(c.incompleteReasons));out.put("unallocatedReservationBytes",pending);out.put("accountedPlusPendingBytes",add(censusAccounted(c,null),pending));out.put("normalAdmissionBytes",config.normalHard);out.put("transientAdmissionBytes",config.transientHard);out.put("freeSpaceBytes",available());out.put("owners",owners);out.put("receipts",new ArrayList<>(state.receipts));out.put("state",c.truncated?"UNKNOWN_INCOMPLETE_CENSUS":config.requireAllocated&&!c.allocatedBytesKnown?"UNKNOWN_ALLOCATION_DENY_ADMISSION":censusAccounted(c,null)>config.normalHard?"BLOCKED_PROTECTED_DEBT":"MEASURED");out.put("coverage","REGISTERED_WRITERS_ONLY; other growth sources must be explicitly integrated");out.put("memoryMetric",false);return out;});}
    long controlHeadroomR31(){return config.metadataHeadroom;}
    long controlFloorR31(){return config.freeFloor;}
    long controlAvailableR31()throws IOException{return available();}
    private long available()throws IOException{long bytes=space.available(root);if(bytes<0)throw new Rejected("SPACE_MEASUREMENT_UNKNOWN");return bytes;}
    private static boolean overlap(String a,String b){return a.equals(b)||a.startsWith(b+"/")||b.startsWith(a+"/");}
    private static void receipt(State state,Row row,String reason,long actual){state.receipts.addLast(row.id+"|"+row.owner+"|"+row.fence+"|"+reason+"|physical="+actual+"|wall="+System.currentTimeMillis());while(state.receipts.size()>MAX_RECEIPTS)state.receipts.removeFirst();state.generation++;}
    private static long censusAccounted(StorageCensusSU2.Census census,String scope){
        long logical=scope==null?census.total:census.scopeBytes.get(scope);
        long allocated=scope==null?census.allocatedFileAndDirectoryBytes:census.allocatedScopeBytes.get(scope);
        return census.allocatedBytesKnown?Math.max(logical,allocated):logical;
    }
    public static long scopeBytes(File file)throws IOException{return scopeUsage(file)[0];}
    private long accountedScopeBytes(File file)throws IOException{
        long[] usage=scopeUsage(file);if(config.requireAllocated&&usage[1]<0)throw new Rejected("ALLOCATED_BYTES_UNKNOWN");return Math.max(usage[0],Math.max(0,usage[1]));
    }
    /** Bounded streaming traversal: regular lengths and kernel blocks stay independent. */
    private static long[] scopeUsage(File file)throws IOException{
        if(!file.exists())return new long[]{0,0};if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new Rejected("SCOPE_SYMLINK");
        long allocated=StorageCensusSU2.allocatedBytes(file);boolean known=allocated>=0;
        if(file.isFile())return new long[]{file.length(),allocated};
        final class Frame {final DirectoryCursorR31 cursor;final int depth;Frame(File directory,int depth)throws IOException{cursor=DirectoryCursorR31.open(directory);this.depth=depth;}}
        long bytes=0;int entries=0;Deque<Frame> stack=new ArrayDeque<>();
        try{stack.push(new Frame(file,0));while(!stack.isEmpty()){
            if(Thread.currentThread().isInterrupted())throw new Rejected("CANCELLED");Frame frame=stack.peek();File child=frame.cursor.next();if(child==null){frame.cursor.close();stack.pop();continue;}
            if(++entries>200000)throw new Rejected("SCOPE_ENTRY_BUDGET");if(!child.getAbsoluteFile().equals(child.getCanonicalFile()))throw new Rejected("SCOPE_SYMLINK");
            long blocks=StorageCensusSU2.allocatedBytes(child);if(blocks<0)known=false;else if(known)allocated=add(allocated,blocks);
            if(child.isDirectory()){if(frame.depth>=16)throw new Rejected("SCOPE_DEPTH_BUDGET");stack.push(new Frame(child,frame.depth+1));}else if(child.isFile())bytes=add(bytes,child.length());else throw new Rejected("SCOPE_UNKNOWN_TYPE");
        }}finally{IOException closing=null;while(!stack.isEmpty())try{stack.pop().cursor.close();}catch(IOException failed){if(closing==null)closing=failed;else closing.addSuppressed(failed);}if(closing!=null)throw closing;}return new long[]{bytes,known?allocated:-1};
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
        state.generation=Math.addExact(state.generation,1L);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(MAGIC);out.writeLong(state.generation);out.writeUTF(state.configHash);out.writeInt(state.active.size());for(Row row:state.active.values())row.write(out);out.writeInt(state.receipts.size());for(String row:state.receipts)out.writeUTF(row);}
        byte[] payload=bytes.toByteArray();bytes.write(digest(payload));if(bytes.size()>MAX_LEDGER_BYTES)throw new Rejected("LEDGER_CAPACITY");atomicWrite(inside(directory,"ledger.bin"),bytes.toByteArray());
    }
    static byte[] readBounded(File file,int maximum)throws IOException{if(!file.isFile()||file.length()>maximum)throw new IOException("STORAGE_METADATA_BOUND");try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] buffer=new byte[4096];int n;while((n=in.read(buffer))!=-1){if(n>maximum-out.size())throw new IOException("STORAGE_METADATA_BOUND");out.write(buffer,0,n);}return out.toByteArray();}}
    static void atomicWrite(File file,byte[] bytes)throws IOException{
        if(bytes.length>MAX_LEDGER_BYTES||!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("STORAGE_METADATA_BOUND_OR_PATH");File parent=file.getParentFile();if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("STORAGE_METADATA_DIRECTORY");
        File backup=new File(file.getPath()+".bak"),partial=new File(file.getPath()+".new");
        if(!backup.getAbsoluteFile().equals(backup.getCanonicalFile())||!partial.getAbsoluteFile().equals(partial.getCanonicalFile()))throw new IOException("STORAGE_METADATA_SYMLINK");
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
