package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.EvidenceReachabilityR31;
import com.ethk3.novaonepro.StorageGcR31;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual production GC/roots/CAS with a native SQLite host transport.
 * Metadata fixtures describe prior committed roots; no Android/device timing or
 * natural quality is claimed by this host contract. */
public final class GcClosureR324Host {
    static int checks;static final List<String> scenarios=new ArrayList<>();
    static void ok(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static void scenario(String id){scenarios.add(id);System.out.println("SCENARIO="+id);}
    interface Checked {void run()throws Exception;}
    static void refused(Checked op,String token)throws Exception{try{op.run();throw new AssertionError("expected "+token);}catch(AssertionError e){throw e;}catch(Exception e){ok(String.valueOf(e.getMessage()).contains(token),"wrong rejection: "+e);}}
    static String synthetic(int n){return String.format(Locale.ROOT,"%064x",n);}
    static byte[] bytes(String value){return value.getBytes(StandardCharsets.UTF_8);}
    static final class Env implements AutoCloseable {
        final Path temp;final File root;final SQLiteDatabase db;final CheckpointArtifactsR26 files;
        String failOperation;boolean failOnce;long maxMutationMillis;
        Env()throws Exception{
            temp=Files.createTempDirectory("r324-gc-host-");root=temp.resolve("files/r26-checkpoints").toFile();root.mkdirs();
            db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+temp.resolve("native.db")));
            for(String sql:CheckpointGcStoreR31.DDL)db.execSQL(sql);
            files=new CheckpointArtifactsR26(root);ok(CheckpointGcStoreR31.ready(db),"production migration DDL ready");
        }
        long write(String operation,CheckpointArtifactsR26.GcMutationR31 mutation){
            if(operation.equals(failOperation)){if(failOnce)failOperation=null;throw new IllegalStateException("INJECTED_"+operation);}
            db.beginTransaction();long start=System.nanoTime();
            try{long n=mutation.run(db);db.setTransactionSuccessful();return n;}
            finally{db.endTransaction();maxMutationMillis=Math.max(maxMutationMillis,(System.nanoTime()-start)/1000000);}
        }
        CheckpointGcStoreR31 store(){return new CheckpointGcStoreR31(db,Collections.emptyMap());}
        String add(byte[] payload)throws Exception{String hash=CheckpointArtifactsR26.referenceHash(files.prepare(payload));register(hash);return hash;}
        String addUnregistered(byte[] payload)throws Exception{return CheckpointArtifactsR26.referenceHash(files.prepare(payload));}
        void register(String hash)throws Exception{
            byte[] raw=files.readHash(hash);Set<String> children=ChunkedCheckpointR26.isManifest(raw)?ChunkedCheckpointR26.children(raw):Collections.emptySet();
            long modified=ColdCheckpointR324.rawModified(root,hash);
            write("FIXTURE_VERIFIED_METADATA",d->{store().registerVerified(hash,raw.length,modified,children,System.currentTimeMillis());return 1;});
        }
        long value(String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new AssertionError("missing scalar");return c.getLong(0);}}
        long gc(int page)throws Exception{try(CheckpointPruneGuardR26 lease=CheckpointPruneGuardR26.tryAcquire(root.getParentFile())){ok(lease!=null,"exclusive report input lease");return files.pruneDurableR31(db,this::write,System.currentTimeMillis()+2000,0,page);}}
        void claim(String hash)throws Exception{long version=store().version(hash),physical=ColdCheckpointR324.physicalBytes(root,hash);ok(write("FIXTURE_REAL_CLAIM",d->CheckpointGcReachabilityR324.claim(d,hash,version,physical)?1:0)==1,"actual GC claim");}
        public void close()throws Exception{db.close();try(java.util.stream.Stream<Path> all=Files.walk(temp)){for(Path p:(Iterable<Path>)all.sorted(Comparator.reverseOrder())::iterator)Files.deleteIfExists(p);}}
    }

    static void damagedTombstoneDoesNotStarve()throws Exception{
        scenario("LOCAL_BAD_TOMBSTONE_PRESERVED_OTHER_ARTIFACT_COLLECTED");
        try(Env e=new Env()){
            String bad=e.add(bytes("claimed bad original")),good=e.add(bytes("unrelated valid original"));e.claim(bad);
            Files.write(ColdCheckpointR324.hotFile(e.root,bad).toPath(),bytes("CORRUPT"));
            long released=e.gc(64);
            ok(released>0,"independent garbage reclaimed despite damaged tombstone");
            ok(e.store().state(bad)==StorageGcR31.State.DELETING,"damaged claim retained");
            ok(ColdCheckpointR324.exists(e.root,bad),"damaged original not deleted");
            ok(e.store().state(good)==StorageGcR31.State.DELETED,"other artifact committed deleted");
            ok(e.value("SELECT retry_count FROM r31_gc_receipt WHERE artifact_hash=?",bad)>0,"failed unlink retry recorded");
        }
    }

    static void parentFirstAndProtection()throws Exception{
        scenario("PARENT_FIRST_EDGE_RETAINED_UNTIL_PHYSICAL_UNLINK");
        try(Env e=new Env()){
            byte[] raw=new byte[40000];Arrays.fill(raw,(byte)11);byte[] reference;
            try(CheckpointArtifactsR26.Prepared p=e.files.prepareState(out->out.write(raw))){reference=p.reference();}
            String parent=CheckpointArtifactsR26.referenceHash(reference);Set<String> children=ChunkedCheckpointR26.children(e.files.readHash(parent));
            for(String child:children)e.register(child);e.register(parent);
            String child=children.iterator().next();ok(CheckpointGcReachabilityR324.protectedNow(e.db,child),"unrooted live parent still protects child");
            long cv=e.store().version(child);ok(e.write("CHILD_CLAIM",d->CheckpointGcReachabilityR324.claim(d,child,cv,1)?1:0)==0,"child claim refused before parent");
            e.claim(parent);ok(CheckpointGcReachabilityR324.protectedNow(e.db,child),"DELETING parent retains edge protection");
            ok(e.value("SELECT count(*) FROM r31_gc_edge WHERE from_hash=?",parent)==children.size(),"claim does not detach immutable edges");
            e.gc(64);e.gc(64);
            ok(!ColdCheckpointR324.exists(e.root,parent),"parent physically gone");
            for(String h:children)ok(!ColdCheckpointR324.exists(e.root,h),"child collected after parent gone");
        }
    }

    static void pinClaimOrdersAndReferenceTrigger()throws Exception{
        scenario("PIN_CLAIM_VERSION_AND_NEW_REFERENCE_SERIALIZATION");
        try(Env e=new Env()){
            String h=e.add(bytes("pin order artifact"));long before=e.store().version(h);
            e.write("PIN",d->{ok(e.store().pin("pin",h,EvidenceReachabilityR31.RootKind.OPEN_TASK),"pin LIVE succeeds");return 0;});
            ok(e.write("STALE_CLAIM",d->CheckpointGcReachabilityR324.claim(d,h,before,10)?1:0)==0,"pinned claim refused");
            e.write("UNPIN",d->{e.store().releasePin("pin",h);return 0;});
            ok(e.write("STALE_CLAIM_AFTER_RELEASE",d->CheckpointGcReachabilityR324.claim(d,h,before,10)?1:0)==0,"stale version still refused after unpin");
            e.claim(h);
            e.write("PIN_AFTER_CLAIM",d->{ok(!e.store().pin("later",h,EvidenceReachabilityR31.RootKind.OPEN_TASK),"pin after claim refused");return 0;});
            e.db.execSQL("CREATE TABLE r29_model_checkpoint(artifact BLOB)");for(String sql:CheckpointGcStoreR31.referenceGuards(e.db))e.db.execSQL(sql);
            refused(()->e.db.execSQL("INSERT INTO r29_model_checkpoint VALUES(?)",new Object[]{bytes("R26CAS1:"+h)}),"REFERENCE_NOT_LIVE");
        }
    }

    static void coldPhysicalReceiptsAndCrash()throws Exception{
        scenario("COLD_RAW_HASH_PRESERVED_COMPRESSED_PHYSICAL_BYTES_EXACTLY_ONCE");
        try(Env e=new Env()){
            byte[] raw=new byte[1<<20];Arrays.fill(raw,(byte)7);String h=e.add(raw);
            ColdCheckpointR324.archive(e.root,h,CheckpointArtifactsR26::syncDirectory);long physical=ColdCheckpointR324.physicalBytes(e.root,h);
            ok(physical<raw.length,"cold fixture really compresses");ok(Arrays.equals(e.files.readHash(h),raw),"production reader resolves cold exact raw bytes");
            long released=e.gc(64);ok(released==physical,"GC returns actual compressed bytes");
            ok(e.value("SELECT released_bytes FROM r31_gc_receipt WHERE artifact_hash=?",h)==physical,"durable receipt compressed bytes");
            ok(e.gc(64)==0,"repeated GC does not double count");
        }
        scenario("UNLINK_BEFORE_COMMIT_RETRY_COUNTS_ONCE");
        try(Env e=new Env()){
            String h=e.add(bytes("ack crash original"));long physical=ColdCheckpointR324.physicalBytes(e.root,h);
            e.failOperation="R31_GC_UNLINK_COMMIT";e.failOnce=true;
            refused(()->e.gc(64),"INJECTED_R31_GC_UNLINK_COMMIT");
            ok(!ColdCheckpointR324.exists(e.root,h),"physical unlink occurred before commit failure");
            ok(e.store().state(h)==StorageGcR31.State.DELETING,"failed ack leaves durable DELETING");
            ok(e.gc(64)==physical,"retry completes historical physical release once");
            ok(e.value("SELECT released_bytes FROM r31_gc_receipt WHERE artifact_hash=?",h)==physical,"exactly one durable release");
        }
    }

    static void deletingCursorFairness()throws Exception{
        scenario("MORE_THAN_64_BAD_TOMBSTONES_DO_NOT_STARVE_LATER_VALID_TOMBSTONE");
        try(Env e=new Env()){
            List<String> hashes=new ArrayList<>();for(int i=0;i<66;i++)hashes.add(e.add(bytes("tombstone "+i)));
            Collections.sort(hashes);for(String h:hashes)e.claim(h);String good=hashes.get(65);
            for(String h:hashes.subList(0,65))Files.write(ColdCheckpointR324.hotFile(e.root,h).toPath(),bytes("bad"));
            e.gc(64);ok(e.store().state(good)==StorageGcR31.State.DELETING,"first page leaves later claim");
            String cursor=CheckpointGcReachabilityR324.deletingCursor(e.db);ok(cursor.equals(hashes.get(63)),"deleting cursor persisted at 64");
            e.gc(64);ok(e.store().state(good)==StorageGcR31.State.DELETED,"second page reaches valid claim");
            ok(e.value("SELECT count(*) FROM r31_gc_artifact WHERE state='DELETING'")==65,"corrupt claims remain preserved");
        }
    }

    static void largeRegisteredMetadata()throws Exception{
        scenario("17000_COMMITTED_ROOT_METADATA_ROWS_WITH_BOUNDED_LIVE_CANDIDATE_PAGE");
        try(Env e=new Env()){
            // Historical metadata is a fixture representing prior verified commits.
            // This scenario tests SQL/metadata scalability, not restoration of 17000 files.
            e.write("FIXTURE_HISTORICAL_ROOTS",d->{for(int i=1;i<=17000;i++){
                String h=synthetic(i);d.execSQL("INSERT INTO r31_gc_artifact VALUES(?,1,1,'LIVE',1,1)",new Object[]{h});
                d.execSQL("INSERT INTO r31_gc_pin VALUES(?,?,'OPEN_TASK')",new Object[]{"historical:"+i,h});
            }return 0;});
            CheckpointGcReachabilityR324.requireClosedMetadata(e.db);
            ok(CheckpointGcReachabilityR324.candidatePage(e.db,"",Long.MAX_VALUE,9999).size()==64,"candidate page clamps to 64");
            ok(CheckpointGcReachabilityR324.candidatePage(e.db,synthetic(16980),Long.MAX_VALUE,64).size()==20,"keyset reaches beyond 16384");
            String real=e.add(bytes("actual unrooted after 17000 prior registered roots"));
            e.db.execSQL("INSERT OR REPLACE INTO r31_gc_status VALUES(1,?,1)",new Object[]{JsonR27.write(Collections.singletonMap("candidateCursorR324",synthetic(17000)))});
            ok(e.gc(64)>0,"unrelated actual file collected despite root count >16384");
            ok(e.store().state(real)==StorageGcR31.State.DELETED,"actual GC entry commits deletion");
            ok(e.value("SELECT count(*) FROM r31_gc_pin")==17000,"all prior pins preserved");
        }
    }

    static void incompleteCensusKeepsLateParent()throws Exception{
        scenario("UNREGISTERED_PARENT_BEYOND_FIRST_64_PREVENTS_EARLY_CHILD_COLLECTION");
        try(Env e=new Env()){
            Map<String,byte[]> wanted=null;String parent=null,child=null;
            for(int attempt=0;attempt<20000;attempt++){
                Map<String,byte[]> chunks=new LinkedHashMap<>();final int nonce=attempt;
                byte[] manifest=ChunkedCheckpointR26.write(out->{out.writeInt(nonce);out.write(new byte[1024]);},new ChunkedCheckpointR26.Store(){
                    public String put(byte[] value){String h=CheckpointArtifactsR26.sha(value);chunks.put(h,value);return h;}
                    public InputStream open(String h){return new ByteArrayInputStream(chunks.get(h));}
                });
                String ph=CheckpointArtifactsR26.sha(manifest),ch=chunks.keySet().iterator().next();
                if(ph.startsWith("f")&&ch.startsWith("0")){chunks.put(ph,manifest);wanted=chunks;parent=ph;child=ch;break;}
            }
            ok(wanted!=null,"deterministic late parent fixture found");
            for(byte[] data:wanted.values())e.addUnregistered(data);
            int created=0;for(int i=0;created<70;i++){
                byte[] data=bytes("earlier standalone census original "+i);String h=CheckpointArtifactsR26.sha(data);
                if(h.compareTo(child)>0&&h.compareTo(parent)<0){e.addUnregistered(data);created++;}
            }
            ok(e.value("SELECT count(*) FROM r31_gc_artifact")==0,"legacy directory begins unregistered");
            long released=e.gc(64);ok(released==0,"first incomplete census makes no new claim");
            ok(ColdCheckpointR324.exists(e.root,child),"child survives while parent unregistered");
            ok(e.store().state(parent)==null,"late parent really outside first metadata page");
            for(int i=0;i<6;i++)e.gc(64);
            ok(!ColdCheckpointR324.exists(e.root,parent)&&!ColdCheckpointR324.exists(e.root,child),"later completed census collects parent then child without deadlock");
        }
    }

    static void unknownCodecAndFirstError()throws Exception{
        scenario("UNKNOWN_ROOT_CODEC_BLOCKS_ALL_AND_FIRST_FAILURE_SURVIVES_STATUS_FAILURE");
        try(Env e=new Env()){
            String h=e.add(bytes("retain for unknown root codec"));e.db.execSQL("CREATE TABLE r29_model_checkpoint(artifact BLOB)");
            e.db.execSQL("INSERT INTO r29_model_checkpoint VALUES(?)",new Object[]{bytes("R99CAS:unknown")});
            e.failOperation="R31_GC_STATUS";e.failOnce=true;
            try{e.gc(64);throw new AssertionError("unknown roots must block");}catch(IllegalStateException expected){
                ok(String.valueOf(expected.getMessage()).contains("ROOT_VERSION_UNKNOWN"),"root codec remains first cause");
                ok(expected.getSuppressed().length==1,"secondary status failure attached");
            }
            ok(ColdCheckpointR324.exists(e.root,h)&&e.store().state(h)==StorageGcR31.State.LIVE,"unknown global root preserves all originals");
        }
        scenario("CANCELLATION_NOT_CLASSIFIED_AS_LOCAL_FILE_RETRY");
        try(Env e=new Env()){
            String h=e.add(bytes("cancelled original"));Thread.currentThread().interrupt();
            try{refused(()->e.gc(64),"INTERRUPTED");}finally{Thread.interrupted();}
            ok(e.store().state(h)==StorageGcR31.State.LIVE,"cancelled work has no claim");
        }
    }

    static void statusFailureReleasesGateAndColdCodecBlocks()throws Exception{
        scenario("MALFORMED_STATUS_CANNOT_LEAK_MAINTENANCE_ADMISSION_GATE");
        try(Env e=new Env()){
            String h=e.add(bytes("status parsing failure original"));
            e.db.execSQL("INSERT OR REPLACE INTO r31_gc_status VALUES(1,?,?)",new Object[]{"{broken",System.currentTimeMillis()});
            try{e.gc(64);throw new AssertionError("malformed status must surface");}catch(IllegalArgumentException expected){ok(true,"parse error surfaced");}
            ok(e.store().state(h)==StorageGcR31.State.LIVE,"malformed status cannot authorize collection");
            ok(e.gc(64)>0&&e.store().state(h)==StorageGcR31.State.DELETED,"next repaired status slice reacquires gate");
        }
        scenario("UNKNOWN_COLD_CODEC_REMAINS_GLOBAL_PRESERVE_FAILURE");
        try(Env e=new Env()){
            byte[] raw=new byte[10000];Arrays.fill(raw,(byte)37);String h=e.add(raw);ColdCheckpointR324.archive(e.root,h,CheckpointArtifactsR26::syncDirectory);e.claim(h);
            try(RandomAccessFile file=new RandomAccessFile(ColdCheckpointR324.coldFile(e.root,h),"rw")){file.seek(4);file.writeInt(99);}
            String good=e.add(bytes("good unrelated object with unknown cold codec"));
            refused(()->e.gc(64),"R324_COLD_VERSION");
            ok(e.store().state(h)==StorageGcR31.State.DELETING&&ColdCheckpointR324.exists(e.root,h),"unknown cold original preserved");
            ok(e.store().state(good)==StorageGcR31.State.LIVE&&ColdCheckpointR324.exists(e.root,good),"unknown codec stops new claims");
        }
    }

    static void cycleGroups()throws Exception{
        scenario("ROOTLESS_METADATA_CYCLE_ATOMIC_RETIREMENT_REAL_FILES_AND_NO_EDGE_DETACH");
        try(Env e=new Env()){
            String a=e.add(bytes("cycle a")),b=e.add(bytes("cycle b"));
            e.write("GRAPH_ADMISSION",d->{ok(e.store().addEdgeIfLive(a,b)&&e.store().addEdgeIfLive(b,a),"actual admitted retention edges create a metadata cycle");return 0;});
            long expected=ColdCheckpointR324.physicalBytes(e.root,a)+ColdCheckpointR324.physicalBytes(e.root,b);
            ok(e.gc(64)==expected,"actual production GC retires rootless metadata cycle and unlinks real files");
            ok(e.store().state(a)==StorageGcR31.State.DELETED&&e.store().state(b)==StorageGcR31.State.DELETED,"whole group completed");
            ok(e.value("SELECT count(*) FROM r31_gc_edge")==2,"immutable edges retained after whole group retirement");
            ok(e.value("SELECT count(*) FROM r31_gc_closed_component_r324")==0,"completed group permission released");
            try(Cursor c=e.db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1",null)){ok(c.moveToFirst()&&c.getString(0).contains("\"closedComponentsClaimedThisSlice\":1"),"actual group claim reported");}
        }
        scenario("ROOTED_CYCLE_PRESERVED_THEN_COLLECTED_AFTER_EXPLICIT_UNPIN");
        try(Env e=new Env()){
            String a=e.add(bytes("root cycle a")),b=e.add(bytes("root cycle b"));
            e.write("GRAPH_AND_ROOT",d->{e.store().addEdgeIfLive(a,b);e.store().addEdgeIfLive(b,a);ok(e.store().pin("cycle-root",b,EvidenceReachabilityR31.RootKind.OPEN_TASK),"root pin admitted");return 0;});
            ok(e.gc(64)==0&&ColdCheckpointR324.exists(e.root,a)&&ColdCheckpointR324.exists(e.root,b),"rooted cycle preserved");
            e.write("RELEASE_ROOT",d->{e.store().releasePin("cycle-root",b);return 0;});ok(e.gc(64)>0,"rootless cycle collected after unpin");
        }
        scenario("CYCLE_PIN_CLAIM_RACE_HAS_ONLY_ONE_WINNER_NO_PARTIAL_GROUP");
        try(Env e=new Env()){
            String a=e.add(bytes("race cycle a")),b=e.add(bytes("race cycle b"));
            e.write("GRAPH",d->{e.store().addEdgeIfLive(a,b);e.store().addEdgeIfLive(b,a);return 0;});
            CheckpointGcReachabilityR324.ClosedComponent old=CheckpointGcReachabilityR324.closedComponent(e.db,a,64);Map<String,Long> physical=new LinkedHashMap<>();for(String member:old.versions.keySet())physical.put(member,ColdCheckpointR324.physicalBytes(e.root,member));
            e.write("RACING_PIN",d->{ok(e.store().pin("race",b,EvidenceReachabilityR31.RootKind.OPEN_TASK),"pin wins first");return 0;});
            ok(e.write("STALE_GROUP_CLAIM",d->CheckpointGcReachabilityR324.claimClosedComponent(d,old,physical)?1:0)==0,"stale closure cannot claim any member");
            ok(e.store().state(a)==StorageGcR31.State.LIVE&&e.store().state(b)==StorageGcR31.State.LIVE,"no partial claim on stale second member");
            e.write("UNPIN",d->{e.store().releasePin("race",b);return 0;});CheckpointGcReachabilityR324.ClosedComponent fresh=CheckpointGcReachabilityR324.closedComponent(e.db,a,64);
            ok(e.write("WHOLE_GROUP_CLAIM",d->CheckpointGcReachabilityR324.claimClosedComponent(d,fresh,physical)?1:0)==1,"group claim wins atomically");
            e.write("LATE_PIN",d->{ok(!e.store().pin("late-cycle",b,EvidenceReachabilityR31.RootKind.OPEN_TASK),"later pin refused for all members");return 0;});
            e.gc(64);ok(!ColdCheckpointR324.exists(e.root,a)&&!ColdCheckpointR324.exists(e.root,b),"claimed group resumes through normal DELETING cursor");
        }
        scenario("SECOND_MEMBER_SQL_FAILURE_ROLLS_BACK_ENTIRE_COMPONENT_CLAIM");
        try(Env e=new Env()){
            String a=e.add(bytes("atomic failure a")),b=e.add(bytes("atomic failure b"));
            e.write("GRAPH",d->{e.store().addEdgeIfLive(a,b);e.store().addEdgeIfLive(b,a);return 0;});
            CheckpointGcReachabilityR324.ClosedComponent plan=CheckpointGcReachabilityR324.closedComponent(e.db,a,64);Map<String,Long> physical=new LinkedHashMap<>();for(String member:plan.versions.keySet())physical.put(member,ColdCheckpointR324.physicalBytes(e.root,member));
            String second=new ArrayList<>(plan.versions.keySet()).get(1);
            e.db.execSQL("CREATE TRIGGER host_fail_second BEFORE UPDATE OF state ON r31_gc_artifact WHEN OLD.artifact_hash='"+second+"' AND NEW.state='DELETING' BEGIN SELECT RAISE(ABORT,'HOST_SECOND_MEMBER_SQL_FAILURE');END");
            refused(()->e.write("GROUP_TRANSACTION",d->CheckpointGcReachabilityR324.claimClosedComponent(d,plan,physical)?1:0),"HOST_SECOND_MEMBER_SQL_FAILURE");
            ok(e.store().state(a)==StorageGcR31.State.LIVE&&e.store().state(b)==StorageGcR31.State.LIVE,"first state change rolled back with second member failure");
            ok(e.value("SELECT count(*) FROM r31_gc_closed_component_r324")==0&&e.value("SELECT count(*) FROM r31_gc_physical_claim_r324")==0,"all component permissions and byte claims rolled back");
            e.db.execSQL("DROP TRIGGER host_fail_second");ok(e.gc(64)>0,"clean retry can atomically collect the complete group");
        }
        scenario("PARTIAL_CYCLE_UNLINK_RETRY_RETAINS_PERMISSION_AND_PREVENTS_REVIVAL");
        try(Env e=new Env()){
            byte[] aRaw=bytes("retry cycle a"),bRaw=bytes("retry cycle b");String a=e.add(aRaw),b=e.add(bRaw);
            e.write("GRAPH",d->{e.store().addEdgeIfLive(a,b);e.store().addEdgeIfLive(b,a);return 0;});
            CheckpointGcReachabilityR324.ClosedComponent plan=CheckpointGcReachabilityR324.closedComponent(e.db,a,64);Map<String,Long> physical=new LinkedHashMap<>();for(String member:plan.versions.keySet())physical.put(member,ColdCheckpointR324.physicalBytes(e.root,member));
            e.write("GROUP_CLAIM",d->CheckpointGcReachabilityR324.claimClosedComponent(d,plan,physical)?1:0);
            Files.write(ColdCheckpointR324.hotFile(e.root,b).toPath(),bytes("broken"));e.gc(64);
            ok(e.store().state(a)==StorageGcR31.State.DELETED&&e.store().state(b)==StorageGcR31.State.DELETING,"partial physical progress preserves damaged member");
            ok(CheckpointGcReachabilityR324.componentBusy(e.db,a),"completed identity still fenced by unfinished group");
            refused(()->e.write("ILLEGAL_REVIVE",d->{e.store().registerVerified(a,aRaw.length,0,Collections.singleton(b),System.currentTimeMillis());return 0;}),"COMPONENT_RETIREMENT_PENDING");
            Files.write(ColdCheckpointR324.hotFile(e.root,b).toPath(),bRaw);ok(e.gc(64)==bRaw.length,"fixed retained original resumes exact remaining bytes");
            ok(!CheckpointGcReachabilityR324.componentBusy(e.db,a)&&e.value("SELECT sum(released_bytes) FROM r31_gc_receipt")==aRaw.length+bRaw.length,"permission ends and bytes count once");
        }
        scenario("CYCLIC_COMPONENT_RAW_IO_BUDGET_PRESERVES_ALL_MEMBERS");
        try(Env e=new Env()){
            byte[] aRaw=new byte[17<<20],bRaw=new byte[17<<20];Arrays.fill(aRaw,(byte)41);Arrays.fill(bRaw,(byte)42);
            String a=e.add(aRaw),b=e.add(bRaw);e.write("GRAPH",d->{e.store().addEdgeIfLive(a,b);e.store().addEdgeIfLive(b,a);return 0;});
            ok(e.gc(64)==0,"cycle beyond 32MiB raw budget makes no partial claim");
            ok(e.store().state(a)==StorageGcR31.State.LIVE&&e.store().state(b)==StorageGcR31.State.LIVE,"large original members remain LIVE");
            try(Cursor c=e.db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1",null)){ok(c.moveToFirst()&&c.getString(0).contains("COMPONENT_RAW_BYTES_EXCEED_SLICE_PRESERVED"),"byte-budget limitation explicit in real status");}
        }
        scenario("65_MEMBER_CYCLE_PRESERVED_WITHOUT_BLOCKING_UNRELATED_GARBAGE");
        try(Env e=new Env()){
            List<String> members=new ArrayList<>();for(int i=0;i<65;i++)members.add(e.add(bytes("oversized metadata cycle "+i)));
            e.write("GRAPH",d->{for(int i=0;i<members.size();i++)ok(e.store().addEdgeIfLive(members.get(i),members.get((i+1)%members.size())),"admitted edge");return 0;});
            String other=e.add(bytes("unrelated garbage outside large ring"));e.gc(64);e.gc(64);
            ok(e.store().state(other)==StorageGcR31.State.DELETED,"large cycle does not starve independent candidate");
            for(String member:members)ok(e.store().state(member)==StorageGcR31.State.LIVE&&ColdCheckpointR324.exists(e.root,member),"oversized cycle intact");
            try(Cursor c=e.db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1",null)){ok(c.moveToFirst()&&c.getString(0).contains("COMPONENT_EXCEEDS_SLICE_PRESERVED"),"oversized component explicitly reported, never called collected");}
        }
    }

    static void preparationCleanup()throws Exception{
        scenario("COLD_ABANDONED_PREPARATION_CLEANUP_SEPARATE_FROM_ORIGINALS");
        try(Env e=new Env()){
            File dir=new File(ColdCheckpointR324.directory(e.root),"prepare");dir.mkdirs();
            File old=new File(dir,".prepare-"+synthetic(4)+"-11111111-1111-1111-1111-111111111111");Files.write(old.toPath(),bytes("dead prepare"));old.setLastModified(System.currentTimeMillis()-120000);
            File recent=new File(dir,".prepare-"+synthetic(5)+"-22222222-2222-2222-2222-222222222222");Files.write(recent.toPath(),bytes("recent prepare"));
            long expected=old.length();ok(e.gc(64)==expected,"old scratch actual bytes released");
            ok(!old.exists()&&recent.exists(),"old removed recent preserved");
            try(Cursor c=e.db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1",null)){ok(c.moveToFirst(),"status present");Map<?,?> status=(Map<?,?>)JsonR27.parse(c.getString(0));ok(((Number)status.get("coldPreparationFilesRemoved")).intValue()==1,"cold scratch reported separately");}
        }
    }

    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        if(args.length==1){
            if(args[0].equals("bad_tombstone"))damagedTombstoneDoesNotStarve();
            else if(args[0].equals("incomplete_census"))incompleteCensusKeepsLateParent();
            else if(args[0].equals("status_gate"))statusFailureReleasesGateAndColdCodecBlocks();
            else if(args[0].equals("cycles"))cycleGroups();
            else throw new IllegalArgumentException("unknown scenario");
        }else{
            damagedTombstoneDoesNotStarve();parentFirstAndProtection();pinClaimOrdersAndReferenceTrigger();coldPhysicalReceiptsAndCrash();
            deletingCursorFairness();largeRegisteredMetadata();incompleteCensusKeepsLateParent();unknownCodecAndFirstError();statusFailureReleasesGateAndColdCodecBlocks();cycleGroups();preparationCleanup();
        }
        Map<String,Object> out=new LinkedHashMap<>();out.put("status","PASS");out.put("assertions",checks);out.put("scenarios",scenarios);out.put("scope","PRODUCTION_JAVA_GC_CAS_ROOTS_NATIVE_SQLITE_HOST;NOT_ANDROID_OR_NATURAL_QUALITY");
        System.out.println(JsonR27.write(out));
    }
}
