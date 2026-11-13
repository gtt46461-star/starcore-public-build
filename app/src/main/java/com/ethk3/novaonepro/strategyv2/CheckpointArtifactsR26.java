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
        try{CheckpointArtifactsR26 files=forDatabaseR31(db);long rootLength=files.path(referenceHash(value)).length();try(MemoryBudgetR31.Lease rootRead=readReservationR31("root-read",2*rootLength+65536)){byte[] root=files.resolve(value);if(!ChunkedCheckpointR26.isManifest(root))return root;
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
        if(bytes==null||bytes.length==0||bytes.length>MAX_BYTES)throw new IOException("R26_ARTIFACT_SIZE");String hash=sha(bytes);Prepared lease=new Prepared(hash);
        synchronized(FILES_LOCK){
            // Never wait on a file mutex from a DB writer: a concurrent GC slice
            // has explicit dependency backpressure rather than a lock inversion.
            if(GC_ACTIVE_R31.contains(root.getPath()))throw new IOException("DEPENDENCY:R31_CAS_GC_SLICE_ACTIVE");
            String key=root.getPath()+"/"+hash;IN_FLIGHT.put(key,IN_FLIGHT.getOrDefault(key,0)+1);
        }
        try{
            // The in-flight pin above prevents a new claim while this read runs;
            // no DB connection is acquired while holding FILES_LOCK.
            if(metadataReadR31!=null&&CheckpointGcStoreR31.ready(metadataReadR31)&&CheckpointGcStoreR31.storedState(metadataReadR31,hash)==com.ethk3.novaonepro.StorageGcR31.State.DELETING)
                throw new IOException("DEPENDENCY:R31_CAS_ARTIFACT_DELETING");
            prepare(bytes);Set<String> children=ChunkedCheckpointR26.isManifest(bytes)?ChunkedCheckpointR26.children(bytes):Collections.emptySet();
            PREPARED_PROOFS_R31.put(root.getPath()+"/"+hash,new VerifiedFileR31(hash,bytes.length,path(hash).lastModified(),children));
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
        if(target.exists()){if(!Arrays.equals(readHash(hash),bytes))throw new IOException("R26_ARTIFACT_EXISTING_CONFLICT");syncDirectory(root);return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
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
    public byte[] readHash(String hash)throws IOException {byte[] bytes=readBounded(path(hash));if(!sha(bytes).equals(hash))throw new IOException("R26_ARTIFACT_CORRUPT");return bytes;}
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
            public long bytes(String hash){try{return path(hash).length();}catch(IOException invalid){throw new UncheckedIOException(invalid);}}
            public Set<String> edgesFrom(String hash){
                if(edges.containsKey(hash))return edges.get(hash);
                try{byte[] header=new byte[4];int n;try(InputStream in=new FileInputStream(path(hash))){n=in.read(header);}Set<String> children=Collections.emptySet();
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
    public interface GcMutationR31 {long run(SQLiteDatabase db);}
    public interface GcWriterR31 {long write(String operation,GcMutationR31 mutation);}
    private VerifiedFileR31 verifyFileR31(String hash)throws IOException {
        File file=path(hash);if(!file.getAbsoluteFile().equals(file.getCanonicalFile())||!file.isFile())throw new IOException("R31_GC_FILE_IDENTITY");
        long size=file.length(),modified=file.lastModified();if(size<1||size>MAX_BYTES)throw new IOException("R31_GC_FILE_SIZE");
        MessageDigest digest;try{digest=MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
        byte[] header=new byte[4],buffer=new byte[32768];int head=0;long read=0;
        try(InputStream input=new FileInputStream(file)){int n;while((n=input.read(buffer))!=-1){ExecutionBudgetR26.poll();if(head<4){int count=Math.min(4-head,n);System.arraycopy(buffer,0,header,head,count);head+=count;}read+=n;if(read>size)throw new IOException("R31_GC_FILE_CHANGED");digest.update(buffer,0,n);}}
        if(read!=size||file.length()!=size||file.lastModified()!=modified||!hash.equals(ReportHexR31.hex(digest.digest())))throw new IOException("R31_GC_FILE_CHECKSUM");
        Set<String> children=Collections.emptySet();if(head==4&&ChunkedCheckpointR26.isManifest(header))children=ChunkedCheckpointR26.children(readHash(hash));
        else if(head>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53)throw new IOException("R31_UNKNOWN_CHECKPOINT_MANIFEST_VERSION_RETENTION_BLOCKED");
        return new VerifiedFileR31(hash,size,modified,children);
    }
    /** Avoid a second 32MiB allocation just to format the streaming SHA digest. */
    private static final class ReportHexR31 {static String hex(byte[] digest){char[] out=new char[digest.length*2],alphabet="0123456789abcdef".toCharArray();for(int i=0;i<digest.length;i++){out[2*i]=alphabet[(digest[i]>>>4)&15];out[2*i+1]=alphabet[digest[i]&15];}return new String(out);}}
    private static Map<String,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind> rootsR31(SQLiteDatabase db){
        CheckpointGcStoreR31.requirePublicationCoverage(db);
        Map<String,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind> roots=new TreeMap<>();
        for(String hash:CheckpointReferencesR26.hashes(db)){roots.put(hash,com.ethk3.novaonepro.EvidenceReachabilityR31.RootKind.PERSISTED_REFERENCE);if(roots.size()>CheckpointGcStoreR31.MAX_NODES)throw new IllegalStateException("R31_GC_ROOT_BOUND");}return roots;
    }
    private long finishDeletionR31(SQLiteDatabase read,GcWriterR31 writer,String hash)throws IOException {
        CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(read,Collections.emptyMap());if(metadata.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.DELETING)return 0;
        final long version=metadata.version(hash),expectedBytes=metadata.bytes(hash);File file=path(hash);boolean removed=false;
        // The writer's reference guards forbid resurrecting a claimed identity.
        // Recheck the actual roots anyway so an unknown legacy writer preserves it.
        if(reachableHashes(CheckpointReferencesR26.hashes(read)).contains(hash))throw new IOException("R31_GC_CLAIM_HAS_NEW_ROOT_PRESERVED");
        if(file.exists()){
            VerifiedFileR31 actual=verifyFileR31(hash);if(actual.bytes!=expectedBytes)throw new IOException("R31_GC_CLAIMED_FILE_IDENTITY_CHANGED");
            removed=file.delete();
            if(removed){synchronized(FILES_LOCK){Usage u=usage();u.bytes=Math.max(0,u.bytes-expectedBytes);u.files=Math.max(0,u.files-1);}syncDirectory(root);}
        }
        final boolean gone=!file.exists();
        return writer.write(gone?"R31_GC_UNLINK_COMMIT":"R31_GC_TOMBSTONE_RETRY",db->{
            CheckpointGcStoreR31 current=new CheckpointGcStoreR31(db,Collections.emptyMap());
            if(current.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.DELETING||current.version(hash)!=version)throw new IllegalStateException("R31_GC_UNLINK_SOURCE_CHANGED");
            if(!gone){current.receipt(hash,"TOMBSTONE_RETRY");return 0;}
            return com.ethk3.novaonepro.StorageGcR31.completeUnlink(current,hash,version);
        });
    }
    /** Production protocol: one short claim transaction per candidate, real IO
     * outside it, then one atomic state/release-receipt commit. The root-level
     * admission flag is nonblocking for publishers; no file monitor spans a writer wait.
     * The caller owns the existing report-input exclusive lease and P05 proof. */
    public long pruneDurableR31(SQLiteDatabase read,GcWriterR31 writer,long now,long graceMillis,int limit)throws IOException {
        if(read.inTransaction())throw new IllegalStateException("R31_GC_IO_INSIDE_TRANSACTION");
        if(!CheckpointGcStoreR31.ready(read))throw new IOException("DEPENDENCY:R31_GC_SCHEMA_PENDING");
        synchronized(FILES_LOCK){if(GC_ACTIVE_R31.contains(root.getPath())||IN_FLIGHT.keySet().stream().anyMatch(k->k.startsWith(root.getPath()+"/")))return 0;GC_ACTIVE_R31.add(root.getPath());}
        long released=0;Map<String,Object> observation=new LinkedHashMap<>();observation.put("state","RUNNING_NOT_PASS");
        try{
            CheckpointScratchGcR31.Result scratch=CheckpointScratchGcR31.step(read,root,writer,now,graceMillis,Math.min(limit,CheckpointGcStoreR31.PAGE));
            released+=scratch.releasedBytes;observation.put("scratch",scratch.json());
            // Reconcile actual regular-file usage even when a previous process
            // died after unlink but before its metadata commit. No scan is in a writer.
            synchronized(FILES_LOCK){Usage u=usage();u.bytes=0;u.files=0;for(File file:com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2))if(file.isFile()){u.bytes+=file.length();u.files++;}}
            CheckpointGcStoreR31.requirePublicationCoverage(read);
            // Recovery precedes new claims, including death after physical unlink.
            for(String hash:CheckpointGcStoreR31.deleting(read))released+=finishDeletionR31(read,writer,hash);
            Map<String,long[]> known=CheckpointGcStoreR31.inventory(read);List<VerifiedFileR31> verified=new ArrayList<>();long scannedBytes=0;
            File[] files=com.ethk3.novaonepro.DirectoryCursorR31.children(root,FILE_LIMIT*2);Arrays.sort(files,Comparator.comparing(File::getName));
            for(File file:files){String name=file.getName();if(!name.matches("[0-9a-f]{64}\\.bin"))continue;String hash=name.substring(0,64);long[] prior=known.get(hash);
                if(prior!=null&&prior[0]==file.length()&&prior[1]==file.lastModified())continue;
                if(verified.size()>=CheckpointGcStoreR31.PAGE||(scannedBytes>0&&scannedBytes+file.length()>MAX_BYTES))break;
                VerifiedFileR31 actual=verifyFileR31(hash);verified.add(actual);scannedBytes+=actual.bytes;
            }
            if(!verified.isEmpty())writer.write("R31_GC_VERIFIED_CENSUS",db->{CheckpointGcStoreR31 metadata=new CheckpointGcStoreR31(db,Collections.emptyMap());for(VerifiedFileR31 file:verified)metadata.registerVerified(file.hash,file.bytes,file.modified,file.children,now);return verified.size();});
            observation.put("verifiedFilesThisSlice",verified.size());observation.put("hashVerifiedBytesThisSlice",scannedBytes);
            // Preserve the existing actual-file closure check as well as the
            // durable graph: missing files/unknown manifests authorize no claim.
            Set<String> actualReachable=reachableHashes(CheckpointReferencesR26.hashes(read));
            CheckpointGcStoreR31 initialGraph=new CheckpointGcStoreR31(read,rootsR31(read));Set<String> initiallyMarked=com.ethk3.novaonepro.EvidenceReachabilityR31.mark(initialGraph,CheckpointGcStoreR31.MAX_NODES);initiallyMarked.addAll(actualReachable);
            List<String> candidates=new ArrayList<>();for(File file:files){String name=file.getName();if(name.matches("[0-9a-f]{64}\\.bin")&&now-file.lastModified()>=graceMillis&&!initiallyMarked.contains(name.substring(0,64)))candidates.add(name.substring(0,64));}
            candidates.sort(Comparator.naturalOrder());int claimed=0,attempted=0;
            for(String hash:candidates){ExecutionBudgetR26.poll();if(attempted>=Math.min(limit,CheckpointGcStoreR31.PAGE))break;attempted++;
                CheckpointGcStoreR31 before=new CheckpointGcStoreR31(read,Collections.emptyMap());if(before.state(hash)!=com.ethk3.novaonepro.StorageGcR31.State.LIVE)continue;final long version=before.version(hash);
                long didClaim=writer.write("R31_GC_CLAIM",db->{CheckpointGcStoreR31 current=new CheckpointGcStoreR31(db,rootsR31(db));return com.ethk3.novaonepro.StorageGcR31.claim(current,hash,version,CheckpointGcStoreR31.MAX_NODES)?1:0;});
                if(didClaim!=0){claimed++;released+=finishDeletionR31(read,writer,hash);}
            }
            CheckpointGcStoreR31 graph=new CheckpointGcStoreR31(read,rootsR31(read));Set<String> marked=com.ethk3.novaonepro.EvidenceReachabilityR31.mark(graph,CheckpointGcStoreR31.MAX_NODES);
            Map<String,Object> budget=com.ethk3.novaonepro.EvidenceReachabilityR31.protectedBudget(graph,marked,ROOT_LIMIT);
            Object conditions=budget.get("releaseConditions");if(conditions instanceof List&&((List<?>)conditions).size()>64){budget.put("releaseConditionCount",((List<?>)conditions).size());budget.put("releaseConditions",new ArrayList<>(((List<?>)conditions).subList(0,64)));budget.put("releaseConditionsTruncated",true);}
            observation.put("protectedBudget",budget);observation.put("claimedThisSlice",claimed);observation.put("actualReleasedBytesThisSlice",released);observation.put("state","COMPLETED_BOUNDED_SLICE_NOT_QUALITY");
            return released;
        }catch(IOException|RuntimeException failure){observation.put("state","BLOCKED_OR_INCOMPLETE_ORIGINALS_PRESERVED");observation.put("failureClass",failure.getClass().getName());observation.put("actualReleasedBytesThisSlice",released);throw failure;}
        finally{
            observation.put("observedAt",System.currentTimeMillis());String json=JsonR27.write(observation);
            try{writer.write("R31_GC_STATUS",db->{db.execSQL("INSERT OR REPLACE INTO r31_gc_status VALUES(1,?,?)",new Object[]{json,System.currentTimeMillis()});return 0;});}
            finally{synchronized(FILES_LOCK){GC_ACTIVE_R31.remove(root.getPath());}}
        }
    }
    public long bytes(){synchronized(FILES_LOCK){Usage u=usage();return u.bytes+u.reserved;}}
    public static String referenceHash(byte[] value){return reference(value)?new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII):"";}
}
