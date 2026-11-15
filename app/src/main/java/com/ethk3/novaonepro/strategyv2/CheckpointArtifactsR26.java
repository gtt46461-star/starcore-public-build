package com.ethk3.novaonepro.strategyv2;

import android.content.Context;
import android.system.ErrnoException;
import android.system.Os;
import android.system.OsConstants;
import com.ethk3.novaonepro.DbStore;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Immutable private checkpoint files. prepare -> fsync -> rename -> directory fsync -> owner DB CAS. */
public final class CheckpointArtifactsR26 {
    public static final int MAX_BYTES=32<<20;
    private static final String PREFIX="R26CAS1:";
    private static final Object FILES_LOCK=new Object();
    private static final Map<String,Integer> IN_FLIGHT=new HashMap<>();
    private static final Set<String> GC_ACTIVE_R31=new HashSet<>();
    private static final java.util.concurrent.ConcurrentMap<String,VerifiedFileR31> PREPARED_PROOFS_R31=new java.util.concurrent.ConcurrentHashMap<>();
    private static final class Usage {long bytes,reserved;int files,reservedFiles;}
    private static final Map<String,Usage> USAGE=new HashMap<>();
    private static final class DatabaseRootR31{final File root;int readers;boolean persistent;DatabaseRootR31(File root){this.root=root;}}
    private static final Map<String,DatabaseRootR31> DATABASE_ROOTS_R31=new LinkedHashMap<>();
    private static final long ROOT_LIMIT=256L<<20;
    private static final int FILE_LIMIT=8192;
    private final File root;
    private SQLiteDatabase metadataReadR31;
    private GcWriterR31 metadataWriterR31;
    static final class VerifiedFileR31 {
        final String hash;final long bytes,modified;final Set<String> children;
        VerifiedFileR31(String hash,long bytes,long modified,Set<String> children){this.hash=hash;this.bytes=bytes;this.modified=modified;this.children=Collections.unmodifiableSet(new TreeSet<>(children));}
    }
    private Usage usage(){return USAGE.get(root.getPath());}
    public final class Prepared implements AutoCloseable {
        private final String hash;private boolean closed;private List<Prepared> children=Collections.emptyList();
        private Prepared(String hash){this.hash=hash;}
        public byte[] reference(){return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
        public String hash(){return hash;}
        @Override public void close(){synchronized(FILES_LOCK){if(closed)return;closed=true;String key=root.getPath()+"/"+hash;int count=IN_FLIGHT.getOrDefault(key,1)-1;if(count==0){IN_FLIGHT.remove(key);PREPARED_PROOFS_R31.remove(key);}else IN_FLIGHT.put(key,count);for(Prepared child:children)child.close();}}
    }
    public CheckpointArtifactsR26(File directory)throws IOException {
        root=directory.getCanonicalFile();
        if(!root.isDirectory()){
            if(!root.mkdirs()&&!root.isDirectory())throw new IOException("R26_ARTIFACT_DIRECTORY");
            syncDirectory(root.getParentFile());
        }
        synchronized(FILES_LOCK){if(!USAGE.containsKey(root.getPath())){Usage u=new Usage();File[] all=com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2);for(File file:all)if(file.isFile()){u.bytes+=file.length();u.files++;}USAGE.put(root.getPath(),u);}}
    }
    public static CheckpointArtifactsR26 forStore(DbStore store)throws IOException {
        CheckpointArtifactsR26 files=new CheckpointArtifactsR26(new File(store.contextForR26().getFilesDir(),"r26-checkpoints"));
        files.metadataReadR31=store.readable();
        files.metadataWriterR31=(operation,mutation)->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(
                com.ethk3.novaonepro.DatabaseCoordinator.WriteContext.of(com.ethk3.novaonepro.DatabaseCoordinator.WriteClass.RESEARCH_DATA,operation),mutation::run);
        registerDatabaseRootR31(store.readable(),files.root);return files;
    }
    /** Snapshot/report owners explicitly register their own matching, verified checkpoint directory. */
    public static void registerDatabaseRootR31(SQLiteDatabase db,File directory)throws IOException {
        File root=directory.getCanonicalFile();if(!root.isDirectory())throw new IOException("R31_ARTIFACT_SOURCE_DIRECTORY_MISSING");String path=new File(db.getPath()).getCanonicalPath();
        synchronized(DATABASE_ROOTS_R31){DatabaseRootR31 entry=databaseRootR31(path,root);entry.persistent=true;}
    }
    private static DatabaseRootR31 databaseRootR31(String path,File root)throws IOException {DatabaseRootR31 old=DATABASE_ROOTS_R31.get(path);if(old!=null&&!old.root.equals(root))throw new IOException("R31_ARTIFACT_DATABASE_ROOT_CONFLICT");if(old==null){if(DATABASE_ROOTS_R31.size()>=32)throw new IOException("R31_ARTIFACT_DATABASE_ROOT_CAPACITY");old=new DatabaseRootR31(root);DATABASE_ROOTS_R31.put(path,old);}return old;}
    public static final class DatabaseRootLeaseR31 implements AutoCloseable{private final String path;private boolean closed;private DatabaseRootLeaseR31(String path){this.path=path;}@Override public void close(){synchronized(DATABASE_ROOTS_R31){if(closed)return;closed=true;DatabaseRootR31 entry=DATABASE_ROOTS_R31.get(path);if(entry==null||entry.readers<=0)throw new IllegalStateException("R31_ARTIFACT_DATABASE_ROOT_LEASE");if(--entry.readers==0&&!entry.persistent)DATABASE_ROOTS_R31.remove(path);}}}
    /** One scope per live snapshot reader. Contains only a canonical key, never the DB/Context or a file handle. */
    public static DatabaseRootLeaseR31 openDatabaseRootR31(SQLiteDatabase db,File directory)throws IOException {File root=directory.getCanonicalFile();if(!root.isDirectory())throw new IOException("R31_ARTIFACT_SOURCE_DIRECTORY_MISSING");String path=new File(db.getPath()).getCanonicalPath();synchronized(DATABASE_ROOTS_R31){databaseRootR31(path,root).readers++;return new DatabaseRootLeaseR31(path);}}
    public static void unregisterDatabaseRootR31(SQLiteDatabase db)throws IOException {String path=new File(db.getPath()).getCanonicalPath();synchronized(DATABASE_ROOTS_R31){DatabaseRootR31 entry=DATABASE_ROOTS_R31.get(path);if(entry!=null){entry.persistent=false;if(entry.readers==0)DATABASE_ROOTS_R31.remove(path);}}}
    private static CheckpointArtifactsR26 forDatabaseR31(SQLiteDatabase db)throws IOException {
        File database=new File(db.getPath()).getCanonicalFile(),directory;synchronized(DATABASE_ROOTS_R31){DatabaseRootR31 entry=DATABASE_ROOTS_R31.get(database.getPath());directory=entry==null?null:entry.root;}
        if(directory==null){File parent=database.getParentFile();if(parent==null||!"databases".equals(parent.getName())||parent.getParentFile()==null)throw new IOException("R31_ARTIFACT_SOURCE_NOT_REGISTERED");directory=new File(parent.getParentFile(),"files/r26-checkpoints");}
        if(!directory.isDirectory())throw new IOException("R31_ARTIFACT_SOURCE_DIRECTORY_MISSING");return new CheckpointArtifactsR26(directory);
    }
    public static final class ArtifactReadR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        private final String category;ArtifactReadR31(String category,IOException cause){super("R31_ARTIFACT_"+category+":"+cause.getMessage(),cause);this.category=category;}
        public String failureCategory(){return category;}public String operation(){return "ATOMNET_ARTIFACT_READ";}public String phase(){return "ARTIFACT_RESOLVE";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    static MemoryBudgetR31.Lease readReservationR31(String phase,long bytes){if(bytes<1||bytes>512L<<20)throw new ArtifactReadR31("DEPENDENCY",new IOException("R31_ARTIFACT_READ_WORKSPACE_BOUND"));MemoryBudgetR31.Admission a=MemoryBudgetR31.processBudget().tryAcquire("atomnet-artifact-"+phase,MemoryBudgetR31.Kind.CHECKPOINT,bytes,()->Thread.currentThread().isInterrupted());if(!a.acquired())throw new MemoryBudgetR31.CapacityWait(a.reason);return a.lease;}
    /** Versioned references resolve at the actual consumer. Legacy inline codec bytes remain unchanged.
     * Never infer a snapshot's checkpoint directory or create an absent directory while reading. */
    public static byte[] resolveSerializedR31(SQLiteDatabase db,byte[] value){
        if(!reference(value))return value;
        try{CheckpointArtifactsR26 files=forDatabaseR31(db);long rootLength=ColdCheckpointR324.rawSize(files.root,referenceHash(value));try(MemoryBudgetR31.Lease rootRead=readReservationR31("root-read",2*rootLength+65536)){byte[] root=files.resolve(value);if(!ChunkedCheckpointR26.isManifest(root))return root;
            long raw;try(DataInputStream header=new DataInputStream(new ByteArrayInputStream(root))){header.readInt();raw=header.readLong();}if(raw<1||raw>64L<<20)throw new IOException("R31_ARTIFACT_SERIALIZED_BOUND");
            try(MemoryBudgetR31.Lease read=readReservationR31("serialized-read",3*raw+3L*262144+65536);InputStream input=ChunkedCheckpointR26.open(root,files.readStore());ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)raw)){byte[] buffer=new byte[32768];int n;while((n=input.read(buffer))!=-1){if((long)bytes.size()+n>raw)throw new IOException("R31_ARTIFACT_SERIALIZED_BOUND");bytes.write(buffer,0,n);}return bytes.toByteArray();}}
        }catch(IOException failure){String m=String.valueOf(failure.getMessage());throw new ArtifactReadR31(failure instanceof FileNotFoundException||m.contains("MISSING")||m.contains("NOT_REGISTERED")?"DEPENDENCY":"INTEGRITY",failure);}
    }
    static void syncDirectory(File directory)throws IOException {
        java.io.FileDescriptor fd=null;
        try{fd=Os.open(directory.getPath(),OsConstants.O_RDONLY,0);Os.fsync(fd);}
        catch(ErrnoException failed){throw new IOException("R26_ARTIFACT_DIRECTORY_SYNC",failed);}
        finally{if(fd!=null)try{Os.close(fd);}catch(ErrnoException ignored){}}
    }
    public static String sha(byte[] bytes){
        try{byte[] digest=MessageDigest.getInstance("SHA-256").digest(bytes);char[] hex="0123456789abcdef".toCharArray(),out=new char[64];for(int i=0;i<digest.length;i++){out[2*i]=hex[(digest[i]>>>4)&15];out[2*i+1]=hex[digest[i]&15];}return new String(out);}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static boolean reference(byte[] value){return value!=null&&value.length==PREFIX.length()+64&&new String(value,0,PREFIX.length(),StandardCharsets.US_ASCII).equals(PREFIX);}
    private File path(String hash)throws IOException {if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IOException("R26_ARTIFACT_HASH");return new File(root,hash+".bin");}
    public Prepared prepareLease(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");
        spillColdR324(bytes.length);
        String hash=sha(bytes);Prepared lease=new Prepared(hash);
        synchronized(FILES_LOCK){
            // Never wait on a file mutex from a DB writer: a concurrent GC slice
            // has explicit dependency backpressure rather than a lock inversion.
            if(GC_ACTIVE_R31.contains(root.getPath()))throw new IOException("DEPENDENCY:R31_CAS_GC_SLICE_ACTIVE");
            String key=root.getPath()+"/"+hash;IN_FLIGHT.put(key,IN_FLIGHT.getOrDefault(key,0)+1);
        }
        try{
            // The in-flight pin above prevents a new claim while this read runs;
            // no DB connection is acquired while holding FILES_LOCK.
            if(metadataReadR31!=null&&CheckpointGcStoreR31.ready(metadataReadR31)&&(CheckpointGcStoreR31.storedState(metadataReadR31,hash)==com.ethk3.novaonepro.StorageGcR31.State.DELETING||CheckpointGcReachabilityR324.componentBusy(metadataReadR31,hash)))
                throw new IOException("DEPENDENCY:R31_CAS_ARTIFACT_DELETING");
            prepare(bytes);Set<String> children=ChunkedCheckpointR26.isManifest(bytes)?ChunkedCheckpointR26.children(bytes):Collections.emptySet();
            PREPARED_PROOFS_R31.put(root.getPath()+"/"+hash,new VerifiedFileR31(hash,bytes.length,ColdCheckpointR324.rawModified(root,hash),children));
            if(metadataWriterR31!=null&&CheckpointGcStoreR31.ready(metadataReadR31))metadataWriterR31.write("R31_GC_REGISTER_PREPARED",db->{registerPreparedWithinR31(db,root.getPath(),hash,new HashSet<>());return 0;});
            return lease;
        }catch(IOException|RuntimeException failed){lease.close();throw failed;}
    }
    private static void registerPreparedWithinR31(SQLiteDatabase db,String directory,String hash,Set<String> visited){
        if(!visited.add(hash))return;if(visited.size()>CheckpointGcStoreR31.MAX_NODES)throw new IllegalStateException("R31_GC_PREPARED_GRAPH_BOUND");
        VerifiedFileR31 file=PREPARED_PROOFS_R31.get(directory+"/"+hash);CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(db,Collections.emptyMap());
        if(file==null){if(metadata.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.LIVE)throw new IllegalStateException("R31_GC_PREPARED_PROOF_REQUIRED");return;}
        for(String child:file.children)registerPreparedWithinR31(db,directory,child,visited);
        metadata.registerVerified(hash,file.bytes,file.modified,file.children,System.currentTimeMillis());
    }
    /** Standalone preparation is used only by file-recovery tests; production keeps a lease through DB commit. */
    public byte[] prepare(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");
        String hash=sha(bytes);File target=path(hash);
        if(ColdCheckpointR324.exists(root,hash)){if(!Arrays.equals(readHash(hash),bytes))throw new IOException("R26_ARTIFACT_EXISTING_CONFLICT");syncDirectory(root);return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
        synchronized(FILES_LOCK){Usage u=usage();if(u.bytes+u.reserved+bytes.length>ROOT_LIMIT||u.files+u.reservedFiles>=FILE_LIMIT)throw new IOException("R26_ARTIFACT_CAPACITY_WAIT_FOR_RETENTION");u.reserved+=bytes.length;u.reservedFiles++;}
        boolean reserved=true;File temporary=new File(root,".tmp-"+UUID.randomUUID());
        try{
            try(FileOutputStream out=new FileOutputStream(temporary)){out.write(bytes);out.flush();out.getFD().sync();}
            if(!sha(readBounded(temporary)).equals(hash))throw new IOException("R26_ARTIFACT_PREPARE_HASH");
            synchronized(FILES_LOCK){Usage u=usage();if(!target.exists()){if(!temporary.renameTo(target))throw new IOException("R26_ARTIFACT_ATOMIC_RENAME");u.bytes+=bytes.length;u.files++;}u.reserved-=bytes.length;u.reservedFiles--;reserved=false;}
            if(!sha(readBounded(target)).equals(hash))throw new IOException("R26_ARTIFACT_FINAL_HASH");syncDirectory(root);
            return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);
        }finally{if(temporary.exists())temporary.delete();if(reserved)synchronized(FILES_LOCK){Usage u=usage();u.reserved-=bytes.length;u.reservedFiles--;}}
    }
    private static byte[] readBounded(File file)throws IOException {
        long length=file.length();if(length<1||length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream((int)length);
        try(InputStream in=new FileInputStream(file)){byte[] buffer=new byte[32768];int n;while((n=in.read(buffer))!=-1){if(bytes.size()+n>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");ExecutionBudgetR26.poll();bytes.write(buffer,0,n);}}
        if(bytes.size()!=length)throw new IOException("R26_ARTIFACT_CHANGED_DURING_READ");return bytes.toByteArray();
    }
    public byte[] readHash(String hash)throws IOException {return ColdCheckpointR324.read(root,hash);}
    /** Old inline SQLite checkpoints remain readable during additive migration. */
    public byte[] resolve(byte[] value)throws IOException {if(!reference(value))return value;return readHash(new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII));}
    public static byte[] resolve(DbStore store,byte[] value)throws IOException {return reference(value)?forStore(store).resolve(value):value;}
    public static Prepared prepareLease(DbStore store,byte[] value)throws IOException {return forStore(store).prepareLease(value);}
    private ChunkedCheckpointR26.Store readStore(){return new ChunkedCheckpointR26.Store(){public String put(byte[] bytes)throws IOException{throw new IOException("R26_READ_ONLY_CHUNK_STORE");}public InputStream open(String hash)throws IOException{return new ByteArrayInputStream(readHash(hash));}};}
    public Prepared prepareLegacy(LegacyReplayStateR26 state)throws IOException {
        return prepareState(state::writeInflated);
    }
    public Prepared prepareState(ChunkedCheckpointR26.StateWriter state)throws IOException {
        List<Prepared> parts=new ArrayList<>();boolean complete=false;
        try{byte[] manifest=ChunkedCheckpointR26.write(state,new ChunkedCheckpointR26.Store(){
            public String put(byte[] bytes)throws IOException{Prepared part=prepareLease(bytes);parts.add(part);return part.hash();}
            public InputStream open(String hash)throws IOException{return new ByteArrayInputStream(readHash(hash));}
        });Prepared result=prepareLease(manifest);result.children=new ArrayList<>(parts);complete=true;return result;
        }finally{if(!complete)for(Prepared part:parts)part.close();}
    }
    public LegacyReplayStateR26 restoreLegacy(byte[] reference,String expectedStateHash)throws IOException {
        byte[] root=resolve(reference);if(!LiveStateR26.hash(root).equals(expectedStateHash))throw new IOException("R26_LEGACY_CHECKPOINT_HASH");
        return ChunkedCheckpointR26.isManifest(root)?LegacyReplayStateR26.readInflated(ChunkedCheckpointR26.open(root,readStore())):LegacyReplayStateR26.decode(root);
    }
    public InputStream openState(byte[] reference)throws IOException{return ChunkedCheckpointR26.open(resolve(reference),readStore());}
    public Set<String> reachableHashes(Set<String> roots)throws IOException {
        final Set<String> initial=new TreeSet<>(roots);final Map<String,Set<String>> edges=new HashMap<>();
        com.ethk3.novaonepro.EvidenceReachabilityR31.Graph graph=new com.ethk3.novaonepro.EvidenceReachabilityR31.Graph(){
            public Map<String,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind> roots(){Map<String,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind> m=new LinkedHashMap<>();for(String hash:initial)m.put(hash,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind.PERSISTED_REFERENCE);return m;}
            public Set<String> artifacts(){Set<String> all=new TreeSet<>(initial);for(Set<String> children:edges.values())all.addAll(children);return all;}
            public long bytes(String hash){try{return ColdCheckpointR324.rawSize(root,hash);}catch(IOException invalid){throw new UncheckedIOException(invalid);}}
            public Set<String> edgesFrom(String hash){
                if(edges.containsKey(hash))return edges.get(hash);
                try{byte[] header=new byte[4];int n;try(InputStream in=ColdCheckpointR324.open(root,hash)){n=in.read(header);}Set<String> children=Collections.emptySet();
                    if(n==4&&ChunkedCheckpointR26.isManifest(header))children=ChunkedCheckpointR26.children(readHash(hash));
                    else if(n>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53)throw new IOException("R31_UNKNOWN_CHECKPOINT_MANIFEST_VERSION_RETENTION_BLOCKED");
                    edges.put(hash,children);return children;
                }catch(IOException unavailable){throw new UncheckedIOException(unavailable);}
            }
        };
        try{return new TreeSet<>(com.ethk3.novaonepro.EvidenceReachabilityR31.mark(graph,FILE_LIMIT*2));}
        catch(UncheckedIOException unavailable){throw unavailable.getCause();}
        catch(com.ethk3.novaonepro.EvidenceReachabilityR31.MarkIncomplete incomplete){throw new IOException("R31_CAS_REFERENCE_MARK_INCOMPLETE_PRESERVED",incomplete);}
    }
    /** Called with DB references frozen by the P3 owner. New, not-yet-committed files have an age grace. */
    public long prune(Set<String> references,long now,long graceMillis,int limit)throws IOException {return prune(()->references,now,graceMillis,limit);}
    public long prune(java.util.function.Supplier<Set<String>> referenceSource,long now,long graceMillis,int limit)throws IOException {
        synchronized(FILES_LOCK){Set<String> references=reachableHashes(referenceSource.get());File[] files=com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2);Arrays.sort(files,Comparator.comparingLong(File::lastModified));long removed=0;int count=0;
        for(File file:files){if(count>=limit)break;String name=file.getName();if(now-file.lastModified()<graceMillis)continue;
            boolean temporary=name.startsWith(".tmp-");if(temporary&&IN_FLIGHT.keySet().stream().anyMatch(k->k.startsWith(root.getPath()+"/")))continue;String hash=name.endsWith(".bin")?name.substring(0,name.length()-4):"";
            if(!temporary&&(references.contains(hash)||IN_FLIGHT.containsKey(root.getPath()+"/"+hash)||!hash.matches("[0-9a-f]{64}")))continue;
            long size=file.length();if(file.delete()){removed+=size;count++;Usage u=usage();u.bytes=Math.max(0,u.bytes-size);u.files=Math.max(0,u.files-1);}}
        if(count>0)syncDirectory(root);return removed;}
    }
    /** Move old immutable hot duplicates to a verified cold tier before hot quota
     * admission. No database mutation or wait is performed while FILES_LOCK is held.
     * The report input lease excludes backup enumeration/copy; the maintenance
     * gate excludes GC claims and new publishers. Existing readers can fall back
     * to the already fsynced cold copy. One slice processes at most four files. */
    public long spillColdR324(long incomingBytes)throws IOException {
        synchronized(FILES_LOCK){Usage u=usage();if(u.bytes+u.reserved+incomingBytes<=ROOT_LIMIT*3/4&&u.files+u.reservedFiles+1<=FILE_LIMIT*3/4)return 0;}
        if(metadataReadR31==null||metadataWriterR31==null||!CheckpointGcStoreR31.ready(metadataReadR31))return 0; // Snapshot/read-only instances never move originals.
        if(metadataReadR31.inTransaction())throw new IllegalStateException("R324_COLD_PREPARE_OUTSIDE_WRITER_REQUIRED");
        try(CheckpointPruneGuardR26 report=CheckpointPruneGuardR26.tryAcquire(root.getParentFile())){
            if(report==null)return 0;
            synchronized(FILES_LOCK){if(GC_ACTIVE_R31.contains(root.getPath()))return 0;GC_ACTIVE_R31.add(root.getPath());}
            long released=0;
            try{
                File[] files=com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2);
                Arrays.sort(files,Comparator.comparingLong(File::lastModified).thenComparing(File::getName));int moved=0;
                for(File file:files){String name=file.getName();if(!name.matches("[0-9a-f]{64}\\.bin"))continue;
                    String identity=name.substring(0,64);synchronized(FILES_LOCK){if(IN_FLIGHT.containsKey(root.getPath()+"/"+identity))continue;}
                    // Do not archive a file already claimed by the old or new durable GC.
                    if(CheckpointGcStoreR31.storedState(metadataReadR31,identity)==com.ethk3.novaonepro.StorageGcR31.State.DELETING)continue;
                    // Old hot files can predate durable GC metadata. Publish the verified
                    // content/child graph before removing their only enumerated hot copy.
                    // A not-yet-registered child remains explicit graph debt; GC must not
                    // claim anything until its complete census/closure check succeeds.
                    final VerifiedFileR31 verified=verifyFileR31(identity);
                    metadataWriterR31.write("R31_GC_COLD_REGISTER",db->{if(!CheckpointGcStoreR31.ready(db))throw new IllegalStateException("R324_COLD_GC_SCHEMA_PENDING");new CheckpointGcStoreR31(db,Collections.emptyMap()).registerVerified(verified.hash,verified.bytes,verified.modified,verified.children,System.currentTimeMillis());return 0;});
                    File cold=ColdCheckpointR324.coldFile(root,identity);
                    try{
                        ColdCheckpointR324.Archived result=ColdCheckpointR324.archive(root,identity,CheckpointArtifactsR26::syncDirectory);
                        if(result.hotBytesRemoved>0){released=Math.addExact(released,result.hotBytesRemoved);moved++;}
                    }finally{
                        // Directory fsync can fail after the already-verified cold
                        // publication and hot unlink. Reflect the observed hot quota
                        // even on that failed call; no physical-release success is
                        // fabricated, and the original exception still propagates.
                        if(!file.exists()&&cold.isFile())synchronized(FILES_LOCK){Usage u=usage();u.bytes=Math.max(0,u.bytes-verified.bytes);u.files=Math.max(0,u.files-1);}
                    }
                    synchronized(FILES_LOCK){Usage u=usage();if(moved>=4||u.bytes+u.reserved+incomingBytes<=ROOT_LIMIT*3/4&&u.files+u.reservedFiles+1<=FILE_LIMIT*3/4)break;}
                }
                return released;
            }finally{synchronized(FILES_LOCK){GC_ACTIVE_R31.remove(root.getPath());}}
        }
    }
    public File rootDirectoryR324(){return root;}
    public interface GcMutationR31 {long run(SQLiteDatabase db);}
    public interface GcWriterR31 {long write(String operation,GcMutationR31 mutation);}
    private VerifiedFileR31 verifyFileR31(String hash)throws IOException {
        File file=path(hash);if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!ColdCheckpointR324.exists(root,hash))throw new IOException("R31_GC_FILE_IDENTITY");
        long size=ColdCheckpointR324.rawSize(root,hash),modified=ColdCheckpointR324.rawModified(root,hash);if(size<1||size>MAX_BYTES)throw new IOException("R31_GC_FILE_SIZE");
        MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        byte[] header=new byte[4],buffer=new byte[32768];int head=0;long read=0;
        try(InputStream input=ColdCheckpointR324.open(root,hash)){int n;while((n=input.read(buffer))!=-1){ExecutionBudgetR26.poll();if(head<4){int count=Math.min(4-head,n);System.arraycopy(buffer,0,header,head,count);head+=count;}read+=n;if(read>size)throw new IOException("R31_GC_FILE_CHANGED");digest.update(buffer,0,n);}}
        if(read!=size||ColdCheckpointR324.rawSize(root,hash)!=size||ColdCheckpointR324.rawModified(root,hash)!=modified||!hash.equals(ReportHexR31.hex(digest.digest())))throw new IOException("R31_GC_FILE_CHECKSUM");
        Set<String> children=Collections.emptySet();if(head==4&&ChunkedCheckpointR26.isManifest(header)){try{children=ChunkedCheckpointR26.children(readHash(hash));}
            catch(InterruptedIOException cancelled){throw cancelled;}
            catch(IOException invalidCodec){throw new IOException("R324_GC_MANIFEST_CODEC_INVALID",invalidCodec);}}
        else if(head>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53)throw new IOException("R31_UNKNOWN_CHECKPOINT_MANIFEST_VERSION_RETENTION_BLOCKED");
        return new VerifiedFileR31(hash,size,modified,children);
    }
    /** Avoid a second 32MiB allocation just to format the streaming SHA digest. */
    private static final class ReportHexR31 {static String hex(byte[] digest){char[] out=new char[digest.length*2],alphabet="0123456789abcdef".toCharArray();for(int i=0;i<digest.length;i++){out[2*i]=alphabet[(digest[i]>>>4)&15];out[2*i+1]=alphabet[digest[i]&15];}return new String(out);}}
    private long finishDeletionR31(SQLiteDatabase read,GcWriterR31 writer,String hash)throws IOException {
        CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(read,Collections.emptyMap());
        if(metadata.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.DELETING)return 0;
        final long version=metadata.version(hash),expectedBytes=metadata.bytes(hash);
        final long physicalClaim=CheckpointGcReachabilityR324.physicalClaim(read,hash,version,expectedBytes);
        if(CheckpointGcReachabilityR324.protectedForUnlink(read,hash))throw new IOException("R324_GC_CLAIM_HAS_REFERENCE_PRESERVED");
        long hotBytes=path(hash).isFile()?path(hash).length():0;
        if(ColdCheckpointR324.exists(root,hash)){
            VerifiedFileR31 actual=verifyFileR31(hash);
            if(actual.bytes!=expectedBytes)throw new IOException("R31_GC_CLAIMED_FILE_IDENTITY_CHANGED");
            ColdCheckpointR324.deleteCopiesAfterGcClaim(root,hash,CheckpointArtifactsR26::syncDirectory);
        }
        final boolean gone=!ColdCheckpointR324.exists(root,hash);
        if(hotBytes>0&&!path(hash).exists())synchronized(FILES_LOCK){Usage u=usage();u.bytes=Math.max(0,u.bytes-hotBytes);u.files=Math.max(0,u.files-1);}
        return writer.write(gone?"R31_GC_UNLINK_COMMIT":"R31_GC_TOMBSTONE_RETRY",db->{
            CheckpointGcStoreR31 current=new CheckpointGcStoreR31(db,Collections.emptyMap());
            if(current.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.DELETING||current.version(hash)!=version)throw new IllegalStateException("R31_GC_UNLINK_SOURCE_CHANGED");
            if(!gone){current.receipt(hash,"TOMBSTONE_RETRY");return 0;}
            com.ethk3.novaonepro.StorageGcR31.completeUnlink(current,hash,version);CheckpointGcReachabilityR324.completeComponentMember(db,hash);return physicalClaim;
        });
    }
    /** Production protocol: one short claim transaction per candidate, real IO
     * outside it, then one atomic state/release-receipt commit. The root-level
     * admission flag is nonblocking for publishers; no file monitor spans a writer wait.
     * The caller owns the existing report-input exclusive lease and P05 proof. */
    public long pruneDurableR31(SQLiteDatabase read,GcWriterR31 writer,long now,long graceMillis,int limit)throws IOException {
        if(read.inTransaction())throw new IllegalStateException("R31_GC_IO_INSIDE_TRANSACTION");
        if(limit<1||graceMillis<0)throw new IllegalArgumentException("R324_GC_SLICE_ARGUMENT");
        if(!CheckpointGcStoreR31.ready(read))throw new IOException("DEPENDENCY:R31_GC_SCHEMA_PENDING");
        synchronized(FILES_LOCK){if(GC_ACTIVE_R31.contains(root.getPath())||IN_FLIGHT.keySet().stream().anyMatch(k->k.startsWith(root.getPath()+"/")))return 0;GC_ACTIVE_R31.add(root.getPath());}
        long released=0;Map<String,Object> observation=new LinkedHashMap<>();observation.put("state","RUNNING_NOT_PASS");
        String candidateCursor="",deletingCursor="";
        List<Map<String,Object>> blocked=new ArrayList<>();int blockedCount=0;Throwable firstFailure=null;
        try{
            candidateCursor=CheckpointGcReachabilityR324.cursor(read);deletingCursor=CheckpointGcReachabilityR324.deletingCursor(read);
            ColdCheckpointR324.Cleanup coldScratch=ColdCheckpointR324.cleanupPreparations(root,now,60000,8,CheckpointArtifactsR26::syncDirectory);
            released+=coldScratch.releasedBytes;observation.put("coldPreparationFilesRemoved",coldScratch.removedFiles);observation.put("coldPreparationBytesReleased",coldScratch.releasedBytes);
            CheckpointScratchGcR31.Result scratch=CheckpointScratchGcR31.step(read,root,writer,now,graceMillis,Math.min(limit,CheckpointGcStoreR31.PAGE));
            released+=scratch.releasedBytes;observation.put("scratch",scratch.json());
            // The hot working set is bounded. Cold history is never collected into
            // this array or a global Java graph; its registered candidates are paged.
            File[] files=com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2);
            Arrays.sort(files,Comparator.comparing(File::getName));
            synchronized(FILES_LOCK){Usage u=usage();u.bytes=0;u.files=0;for(File file:files)if(file.isFile()){u.bytes+=file.length();u.files++;}}
            CheckpointGcStoreR31.requirePublicationCoverage(read);
            List<VerifiedFileR31> verified=new ArrayList<>();long scannedBytes=0;boolean censusPending=false;
            for(File file:files){String name=file.getName();if(!name.matches("[0-9a-f]{64}\\.bin"))continue;String hash=name.substring(0,64);
                if(CheckpointGcStoreR31.storedState(read,hash)==com.ethk3.novaonepro.StorageGcR31.State.DELETING||CheckpointGcReachabilityR324.componentBusy(read,hash))continue;
                long[] prior=CheckpointGcStoreR31.inventoryRowR324(read,hash);
                if(prior!=null&&prior[0]==file.length()&&prior[1]==file.lastModified())continue;
                if(verified.size()>=CheckpointGcStoreR31.PAGE||(scannedBytes>0&&scannedBytes+file.length()>MAX_BYTES)){censusPending=true;break;}
                VerifiedFileR31 actual=verifyFileR31(hash);verified.add(actual);scannedBytes+=actual.bytes;
            }
            if(!verified.isEmpty())writer.write("R31_GC_VERIFIED_CENSUS",db->{CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(db,Collections.emptyMap());for(VerifiedFileR31 file:verified)metadata.registerVerified(file.hash,file.bytes,file.modified,file.children,now);return verified.size();});
            observation.put("verifiedFilesThisSlice",verified.size());observation.put("hashVerifiedBytesThisSlice",scannedBytes);
            // Before the first complete census, an unregistered later parent may
            // still own an earlier hash. Register bounded slices without claiming
            // children until every hot canonical object has known verified edges.
            if(censusPending){observation.put("state","MORE_BOUNDED_METADATA_CENSUS_NO_NEW_CLAIMS");observation.put("actualReleasedBytesThisSlice",released);return released;}
            CheckpointGcReachabilityR324.requireClosedMetadata(read);
            List<String> pendingDeletes=CheckpointGcReachabilityR324.deletingPage(read,deletingCursor,limit);
            if(pendingDeletes.isEmpty())pendingDeletes=CheckpointGcReachabilityR324.deletingPage(read,"",limit);
            observation.put("deletingInspectedThisSlice",pendingDeletes.size());
            for(String hash:pendingDeletes){
                ExecutionBudgetR26.poll();deletingCursor=hash;
                try{released+=finishDeletionR31(read,writer,hash);}
                catch(InterruptedIOException cancelled){throw cancelled;}
                catch(IOException localFailure){recordLocalGcFailureR324(read,writer,hash,localFailure,blocked);blockedCount++;}
            }

            List<String> candidates=CheckpointGcReachabilityR324.candidatePage(read,candidateCursor,now-graceMillis,limit);
            if(candidates.isEmpty())candidates=CheckpointGcReachabilityR324.candidatePage(read,"",now-graceMillis,limit);
            int claimed=0,protectedCount=0,closedGroups=0;Map<String,Integer> componentDispositions=new LinkedHashMap<>();
            for(String hash:candidates){
                if(claimed>=Math.min(limit,CheckpointGcStoreR31.PAGE))break;
                ExecutionBudgetR26.poll();candidateCursor=hash;
                if(CheckpointGcReachabilityR324.protectedNow(read,hash)){
                    protectedCount++;
                    CheckpointGcReachabilityR324.ClosedComponent group=CheckpointGcReachabilityR324.closedComponent(read,hash,Math.min(limit,CheckpointGcStoreR31.PAGE)-claimed);
                    if(!group.claimable()){componentDispositions.put(group.disposition,componentDispositions.getOrDefault(group.disposition,0)+1);continue;}
                    String verifying=hash;
                    try{
                        Map<String,Long> physical=new LinkedHashMap<>();long rawTotal=0;
                        for(String member:group.versions.keySet()){
                            ExecutionBudgetR26.poll();verifying=member;CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(read,Collections.emptyMap());
                            rawTotal+=metadata.bytes(member);if(rawTotal>MAX_BYTES)break;
                            VerifiedFileR31 actual=verifyFileR31(member);if(actual.bytes!=metadata.bytes(member))throw new IOException("R324_GC_COMPONENT_RAW_SIZE_CHANGED");
                            physical.put(member,ColdCheckpointR324.physicalBytes(root,member));
                        }
                        if(rawTotal>MAX_BYTES){String why="COMPONENT_RAW_BYTES_EXCEED_SLICE_PRESERVED";componentDispositions.put(why,componentDispositions.getOrDefault(why,0)+1);continue;}
                        if(writer.write("R324_GC_CLOSED_COMPONENT_CLAIM",db->CheckpointGcReachabilityR324.claimClosedComponent(db,group,physical)?1:0)!=0){
                            claimed+=group.versions.size();closedGroups++;
                            for(String member:group.versions.keySet()){
                                try{released+=finishDeletionR31(read,writer,member);}
                                catch(InterruptedIOException cancelled){throw cancelled;}
                                catch(IOException localFailure){recordLocalGcFailureR324(read,writer,member,localFailure,blocked);blockedCount++;}
                            }
                        }
                    }catch(InterruptedIOException cancelled){throw cancelled;}
                    catch(IOException localFailure){recordLocalGcFailureR324(read,writer,verifying,localFailure,blocked);blockedCount++;}
                    continue;
                }
                CheckpointGcStoreR31 before=new CheckpointGcStoreR31(read,Collections.emptyMap());
                if(before.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.LIVE)continue;
                final long version=before.version(hash);
                try{
                    VerifiedFileR31 actual=verifyFileR31(hash);
                    if(actual.bytes!=before.bytes(hash))throw new IOException("R324_GC_CANDIDATE_RAW_SIZE_CHANGED");
                    final long physicalBytes=ColdCheckpointR324.physicalBytes(root,hash);
                    long didClaim=writer.write("R324_GC_CLAIM",db->CheckpointGcReachabilityR324.claim(db,hash,version,physicalBytes)?1:0);
                    if(didClaim!=0){claimed++;released+=finishDeletionR31(read,writer,hash);}
                }catch(InterruptedIOException cancelled){throw cancelled;}
                catch(IOException localFailure){recordLocalGcFailureR324(read,writer,hash,localFailure,blocked);blockedCount++;}
            }
            observation.put("candidateCountThisSlice",candidates.size());observation.put("protectedCandidatesThisSlice",protectedCount);
            observation.put("closedComponentsClaimedThisSlice",closedGroups);observation.put("closedComponentDispositions",componentDispositions);observation.put("claimedThisSlice",claimed);observation.put("actualReleasedBytesThisSlice",released);
            observation.put("retentionProof","CURRENT_DIRECT_ROOTS_AND_EXTERNAL_NONDELETED_PARENTS_RECHECKED_IN_WRITER;ACYCLIC_PARENT_FIRST;CYCLIC_CLOSED_GROUP_ATOMIC;UNKNOWN_GRAPH_PRESERVED");
            observation.put("cyclePolicy","ROOTLESS_REVERSE_CLOSED_CYCLIC_GROUP_ATOMIC_CLAIM_MAX_64_MEMBERS_32MIB_RAW;LARGER_COMPONENTS_PRESERVED_AND_REPORTED;NO_PARTIAL_MARK_DELETION");
            observation.put("globalHistoryNodeCap",false);observation.put("hotWorkingBytes",bytes());
            observation.put("state",blockedCount==0?"COMPLETED_BOUNDED_SLICE_NOT_QUALITY":"COMPLETED_WITH_LOCAL_ORIGINALS_PRESERVED");return released;
        }catch(IOException|RuntimeException failure){firstFailure=failure;observation.put("state","BLOCKED_OR_INCOMPLETE_ORIGINALS_PRESERVED");observation.put("failureClass",failure.getClass().getName());observation.put("actualReleasedBytesThisSlice",released);throw failure;}
        finally{
            observation.put("candidateCursorR324",candidateCursor);observation.put("deletingCursorR324",deletingCursor);
            observation.put("localFileFailuresThisSlice",blockedCount);observation.put("localFileFailureExamples",blocked);
            observation.put("observedAt",System.currentTimeMillis());String json=JsonR27.write(observation);
            try{writer.write("R31_GC_STATUS",db->{db.execSQL("INSERT OR REPLACE INTO r31_gc_status VALUES(1,?,?)",new Object[]{json,System.currentTimeMillis()});return 0;});}
            catch(RuntimeException statusFailure){if(firstFailure!=null)firstFailure.addSuppressed(statusFailure);else throw statusFailure;}
            finally{synchronized(FILES_LOCK){GC_ACTIVE_R31.remove(root.getPath());}}
        }
    }
    /** A damaged/unlink-failed individual object keeps its immutable edges and
     * tombstone, but cannot starve unrelated safe work. Unknown graph codecs and
     * cancellation remain global failures. Retry state uses the existing writer. */
    private static void recordLocalGcFailureR324(SQLiteDatabase read,GcWriterR31 writer,String hash,IOException failure,List<Map<String,Object>> examples)throws IOException {
        String failureCode=String.valueOf(failure.getMessage());
        if(failure instanceof InterruptedIOException||failureCode.contains("UNKNOWN_CHECKPOINT_MANIFEST_VERSION")||failureCode.contains("R324_GC_MANIFEST_CODEC_INVALID")||failureCode.contains("R324_COLD_VERSION"))throw failure;
        try{writer.write("R324_GC_LOCAL_FILE_RETRY",db->{CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());
            if(store.state(hash)==com.ethk3.novaonepro.StorageGcR31.State.DELETING)store.receipt(hash,"TOMBSTONE_RETRY");return 0;});}
        catch(RuntimeException metadataFailure){failure.addSuppressed(metadataFailure);throw failure;}
        if(examples.size()<8){Map<String,Object> row=new LinkedHashMap<>();row.put("artifactHash",hash);row.put("failureClass",failure.getClass().getName());
            String reason=String.valueOf(failure.getMessage());row.put("reason",reason.length()>240?reason.substring(0,240):reason);row.put("disposition","ORIGINAL_AND_EDGES_PRESERVED_RETRY_LATER");examples.add(row);}
    }
    public long bytes(){synchronized(FILES_LOCK){Usage u=usage();return u.bytes+u.reserved;}}
    public static String referenceHash(byte[] value){return reference(value)?new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII):"";}
}
