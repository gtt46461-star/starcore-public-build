package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.*;
import java.util.*;

/** Actual production Store/claim/IO protocol on persistent native SQLite and real files.
 * This is HOST evidence, never an Android service or natural-quality claim. */
public final class CheckpointGcStoreR31Host {
    static int checks,transactions;
    static void ok(boolean condition,String name){checks++;if(!condition)throw new AssertionError(name);}
    static String h(String name){return CheckpointArtifactsR26.sha(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    static final class Fixture implements AutoCloseable {
        final Path directory,dbFile,files;final Connection jdbc;final SQLiteDatabase db;final CheckpointArtifactsR26 cas;
        Fixture(Path directory)throws Exception {
            this.directory=directory;Files.createDirectories(directory);dbFile=directory.resolve("gc.db");files=directory.resolve("files");Files.createDirectories(files);
            jdbc=DriverManager.getConnection("jdbc:sqlite:"+dbFile);db=new SQLiteDatabase(jdbc);db.execSQL("PRAGMA journal_mode=WAL");
            for(String ddl:CheckpointGcStoreR31.DDL)db.execSQL(ddl);
            db.execSQL("CREATE TABLE IF NOT EXISTS r26_candidate(campaign_id TEXT,policy_hash TEXT,state BLOB,canonical_text TEXT)");
            for(String ddl:CheckpointGcStoreR31.referenceGuards(db))db.execSQL(ddl);
            cas=new CheckpointArtifactsR26(files.toFile());
            java.lang.reflect.Field read=CheckpointArtifactsR26.class.getDeclaredField("metadataReadR31");read.setAccessible(true);read.set(cas,db);
            java.lang.reflect.Field writer=CheckpointArtifactsR26.class.getDeclaredField("metadataWriterR31");writer.setAccessible(true);writer.set(cas,(CheckpointArtifactsR26.GcWriterR31)this::write);
        }
        long write(String operation,CheckpointArtifactsR26.GcMutationR31 work){
            try{if(!jdbc.getAutoCommit())throw new AssertionError("nested writer: "+operation);jdbc.setAutoCommit(false);
                try{long result=work.run(db);jdbc.commit();transactions++;return result;}catch(Throwable failure){jdbc.rollback();throw failure;}finally{jdbc.setAutoCommit(true);}
            }catch(RuntimeException|Error failure){throw failure;}catch(Exception failure){throw new IllegalStateException(failure);}
        }
        CheckpointGcStoreR31 metadata(){return new CheckpointGcStoreR31(db,Collections.emptyMap());}
        void register(String hash,Set<String> children){write("test-register",db->{metadata().registerVerified(hash,20,1,children,2);return 0;});}
        long scalar(String sql)throws Exception{try(Statement s=jdbc.createStatement();ResultSet r=s.executeQuery(sql)){if(!r.next())throw new AssertionError("scalar");return r.getLong(1);}}
        Path file(String hash){return files.resolve(hash+".bin");}
        void root(String hash){db.execSQL("INSERT INTO r26_candidate VALUES('fixture',?,?, 'SU2|HOST_ROOT')",new Object[]{hash,("R26CAS1:"+hash).getBytes(java.nio.charset.StandardCharsets.US_ASCII)});}
        long prune()throws Exception{return cas.pruneDurableR31(db,this::write,System.currentTimeMillis()+120000,60000,64);}
        public void close()throws Exception{jdbc.close();}
    }
    static void requireSqlRefusal(Runnable call,String name){try{call.run();throw new AssertionError(name+" accepted");}catch(android.database.sqlite.SQLiteException expected){ok(expected.getMessage().contains("R31_GC_REFERENCE_NOT_LIVE"),name);}}
    static void metadataContracts(Path base)throws Exception {
        try(Fixture f=new Fixture(base.resolve("metadata"))){
            String root=h("root"),child=h("child"),a=h("cycleA"),b=h("cycleB"),orphan=h("orphan");
            try{f.metadata().registerVerified(root,1,1,Collections.emptySet(),1);throw new AssertionError("writer required");}catch(IllegalStateException expected){ok(expected.getMessage().equals("R31_GC_EXISTING_WRITER_REQUIRED"),"mutations require actual writer");}
            f.register(child,Collections.emptySet());f.register(root,Collections.singleton(child));f.register(a,Collections.singleton(b));f.register(b,Collections.singleton(a));f.register(orphan,Collections.emptySet());
            try(android.database.Cursor c=f.db.rawQuery("SELECT tbl_name,type,name,sql FROM sqlite_master WHERE name='r31_gc_ref_r26_candidate_state_insert'",null)){
                ok(c.moveToFirst(),"actual reference guard is installed");String table=c.getString(0),type=c.getString(1),name=c.getString(2),sql=c.getString(3);
                ok(CheckpointGcStoreR31.knownReferenceGuard(f.db,table,type,name,sql),"archive owner authenticates whole GC guard");
                ok(!CheckpointGcStoreR31.knownReferenceGuard(f.db,"another_table",type,name,sql),"guard table mismatch refused");
                ok(!CheckpointGcStoreR31.knownReferenceGuard(f.db,table,"index",name,sql),"guard object type mismatch refused");
                ok(!CheckpointGcStoreR31.knownReferenceGuard(f.db,table,type,name+"_fake",sql),"guard name-prefix lookalike refused");
                ok(!CheckpointGcStoreR31.knownReferenceGuard(f.db,table,type,name,sql.replace("'LIVE'","'live'")),"guard literal tampering refused");
            }
            Map<String,EvidenceReachabilityR31.RootKind> roots=Collections.singletonMap(root,EvidenceReachabilityR31.RootKind.CURRENT_MODEL);
            f.write("rooted-and-cycle",db->{CheckpointGcStoreR31 s=new CheckpointGcStoreR31(db,roots);
                ok(!StorageGcR31.claim(s,root,s.version(root),100),"current model cannot be claimed");ok(!StorageGcR31.claim(s,child,s.version(child),100),"reachable child cannot be claimed");
                ok(StorageGcR31.claim(s,a,s.version(a),100),"unrooted cycle can be claimed");ok(!s.addEdgeIfLive(a,child),"deleting from-node cannot add edge");ok(!s.addEdgeIfLive(root,a),"deleting to-node cannot add edge");ok(!s.pin("late-pin",a,EvidenceReachabilityR31.RootKind.SHARE),"pin loses to claim");return 0;});
            requireSqlRefusal(()->f.root(a),"insert reference to deleting refused");
            f.root(root);requireSqlRefusal(()->f.db.execSQL("UPDATE r26_candidate SET state=?",new Object[]{("R26CAS1:"+a).getBytes(java.nio.charset.StandardCharsets.US_ASCII)}),"update reference to deleting refused");
            long observed=f.metadata().version(orphan);
            f.write("pin-wins",db->{ok(f.metadata().pin("share",orphan,EvidenceReachabilityR31.RootKind.SHARE),"pin wins before claim");ok(!StorageGcR31.claim(f.metadata(),orphan,observed,100),"pin prevents stale version claim");f.metadata().releasePin("share",root);ok(!StorageGcR31.claim(f.metadata(),orphan,f.metadata().version(orphan),100),"wrong-release identity leaves pin");f.metadata().releasePin("share",orphan);ok(!StorageGcR31.claim(f.metadata(),orphan,observed,100),"released pin still invalidates old version");return 0;});
            long original=f.metadata().version(orphan);
            try{f.write("rollback-claim",db->{ok(StorageGcR31.claim(f.metadata(),orphan,original,100),"claim inside rollback");throw new IllegalStateException("ACTUAL_ROLLBACK");});throw new AssertionError();}catch(IllegalStateException expected){ok(expected.getMessage().equals("ACTUAL_ROLLBACK"),"rollback is genuine");}
            ok(f.metadata().state(orphan)==StorageGcR31.State.LIVE&&f.metadata().version(orphan)==original,"rollback preserves state/version");
            ok(f.scalar("SELECT count(*) FROM r31_gc_receipt WHERE artifact_hash='"+orphan+"'")==0,"rollback leaves no false receipt");
            f.write("delete-final",db->{ok(StorageGcR31.claim(f.metadata(),orphan,original,100),"unrooted claim after released pin");long bytes=StorageGcR31.completeUnlink(f.metadata(),orphan,f.metadata().version(orphan));ok(bytes==20,"complete unlink reports exact registered bytes");return 0;});
            requireSqlRefusal(()->f.root(orphan),"deleted raw reference cannot resurrect missing file");
            ok(f.scalar("SELECT released_bytes FROM r31_gc_receipt WHERE artifact_hash='"+orphan+"'")==20,"state and release receipt atomic");
            f.write("repeat-same-version-receipt",db->{f.metadata().receipt(orphan,"UNLINKED");return 0;});ok(f.scalar("SELECT released_bytes FROM r31_gc_receipt WHERE artifact_hash='"+orphan+"'")==20,"same-version receipt cannot report bytes twice");
            try{EvidenceReachabilityR31.mark(new CheckpointGcStoreR31(f.db,roots),1);throw new AssertionError();}catch(EvidenceReachabilityR31.MarkIncomplete expected){ok(true,"incomplete graph never authorizes deletion");}
        }
    }
    static void actualFiles(Path base)throws Exception {
        try(Fixture f=new Fixture(base.resolve("files"))){
            byte[] bytes="real retained learned checkpoint bytes".getBytes(java.nio.charset.StandardCharsets.UTF_8);String hash=h("real retained learned checkpoint bytes");
            try(CheckpointArtifactsR26.Prepared prepared=f.cas.prepareLease(bytes)){
                ok(f.metadata().state(hash)==StorageGcR31.State.LIVE,"actual verified Prepared registered before publish");ok(f.prune()==0&&Files.isRegularFile(f.file(hash)),"active Prepared lease excludes GC");f.root(hash);
            }
            long version=f.metadata().version(hash);f.db.execSQL("UPDATE r26_candidate SET state=state");ok(f.metadata().version(hash)>version,"actual reference guard bumps version");
            ok(f.prune()==0&&Files.exists(f.file(hash)),"real persisted root survives");
            f.db.execSQL("DELETE FROM r26_candidate");
            Files.setPosixFilePermissions(f.files,PosixFilePermissions.fromString("r-xr-xr-x"));
            try{ok(f.prune()==0&&Files.exists(f.file(hash)),"failed actual unlink does not report released bytes");ok(f.metadata().state(hash)==StorageGcR31.State.DELETING,"failed actual unlink leaves durable deleting");ok(f.scalar("SELECT retry_count FROM r31_gc_receipt WHERE artifact_hash='"+hash+"'")==1,"tombstone retry persists");}
            finally{Files.setPosixFilePermissions(f.files,PosixFilePermissions.fromString("rwx------"));}
            requireSqlRefusal(()->f.root(hash),"pending tombstone refuses publication");
            ok(f.prune()==bytes.length&&!Files.exists(f.file(hash)),"actual recovery unlinks and reports exact bytes");ok(f.prune()==0,"recovery releases bytes exactly once");
            requireSqlRefusal(()->f.root(hash),"completed tombstone refuses old raw reference");
            try(CheckpointArtifactsR26.Prepared recreated=f.cas.prepareLease(bytes)){ok(f.metadata().state(hash)==StorageGcR31.State.LIVE,"same hash re-created only after new verified file");f.root(hash);}
            ok(f.prune()==0&&Files.exists(f.file(hash)),"legitimate re-created rooted incarnation protected");
            f.db.execSQL("DELETE FROM r26_candidate");
            // Real manifest+chunk production codec, not a metadata-only helper.
            byte[] raw=new byte[70000];new Random(123).nextBytes(raw);byte[] manifest;
            try(CheckpointArtifactsR26.Prepared chunked=f.cas.prepareState(out->out.write(raw))){manifest=chunked.reference();f.root(CheckpointArtifactsR26.referenceHash(manifest));}
            ok(f.prune()>=bytes.length,"unrooted prior incarnation retired while manifest lives");
            ok(f.scalar("SELECT released_bytes FROM r31_gc_receipt WHERE artifact_hash='"+hash+"'")==2L*bytes.length,"new verified incarnation contributes its actual released bytes once");
            ok(Arrays.equals(raw,f.cas.openState(manifest).readAllBytes()),"rooted manifest retains real readable chunk closure");
            String missing=h("missing never verified");f.root(missing);Path prior=f.files.resolve("count");long count=Files.list(f.files).count();
            try{f.prune();throw new AssertionError("unknown root allowed deletion");}catch(IllegalStateException expected){ok(expected.getMessage().contains("ROOT_NOT_LIVE_OR_UNREGISTERED"),"unknown root preserves all artifacts");}catch(FileNotFoundException expected){ok(true,"unknown root without an actual file preserves all artifacts");}
            ok(Files.list(f.files).count()==count,"unknown root did not unlink any file");
        }
    }
    static void killChild(Path directory,String boundary)throws Exception {
        try(Fixture f=new Fixture(directory)){
            byte[] bytes=("kill-boundary:"+boundary).getBytes(java.nio.charset.StandardCharsets.UTF_8);String hash=CheckpointArtifactsR26.sha(bytes);
            try(CheckpointArtifactsR26.Prepared p=f.cas.prepareLease(bytes)){}
            f.write("actual-claim-before-death",db->{ok(StorageGcR31.claim(f.metadata(),hash,f.metadata().version(hash),100),"child claim");return 0;});
            if(boundary.equals("after-unlink"))Files.delete(f.file(hash));
            Runtime.getRuntime().halt(79);
        }
    }
    static void killRecovery(Path base,String boundary)throws Exception {
        Path directory=base.resolve(boundary);Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),CheckpointGcStoreR31Host.class.getName(),"child",directory.toString(),boundary).redirectErrorStream(true).redirectOutput(directory.getParent().resolve(boundary+".child.log").toFile()).start();
        ok(child.waitFor()==79,"real process halt at "+boundary);
        try(Fixture f=new Fixture(directory)){byte[] bytes=("kill-boundary:"+boundary).getBytes(java.nio.charset.StandardCharsets.UTF_8);String hash=CheckpointArtifactsR26.sha(bytes);
            ok(f.metadata().state(hash)==StorageGcR31.State.DELETING,"durable claim survives "+boundary);ok(f.prune()==bytes.length,"restart finalizes "+boundary);ok(f.metadata().state(hash)==StorageGcR31.State.DELETED&&!Files.exists(f.file(hash)),"restart final state "+boundary);ok(f.prune()==0,"restart does not count twice "+boundary);
        }
    }
    static ModelPublicationR31.Plan publicationPlan(Fixture f)throws Exception {
        byte[] state="pending warm state".getBytes(java.nio.charset.StandardCharsets.US_ASCII),initializer="pending warm initializer".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        byte[] stateRef,initializerRef;try(CheckpointArtifactsR26.Prepared s=f.cas.prepareLease(state);CheckpointArtifactsR26.Prepared i=f.cas.prepareLease(initializer)){stateRef=s.reference();initializerRef=i.reference();}
        ModelPublicationR31.Plan p=new ModelPublicationR31.Plan("WARM_NATIVE_PINNED","WARM_FORK",h("source"),1,-1,1);
        p.put("WARM_CANDIDATE","child","campaign","child","program",stateRef,h("state"),h("checkpoint"),h("parent"),"fork");
        p.put("WARM_INITIALIZER","child","child",h("parent"),h("parent-checkpoint"),h("parent-state"),1,"PAST",h("initializer"),initializerRef);
        java.lang.reflect.Method encode=ModelPublicationR31.class.getDeclaredMethod("encode",ModelPublicationR31.Plan.class);encode.setAccessible(true);p.manifestHash=CheckpointArtifactsR26.sha((byte[])encode.invoke(null,p));return p;
    }
    static void openPublication(Fixture f,ModelPublicationR31.Plan p){f.db.execSQL("CREATE TABLE IF NOT EXISTS r31_model_publication(publication_id TEXT PRIMARY KEY,manifest_hash TEXT NOT NULL,status TEXT NOT NULL)");f.db.execSQL("INSERT OR IGNORE INTO r31_model_publication VALUES(?,?,'PREPARING')",new Object[]{p.id,p.manifestHash});}
    static void publicationContracts(Path base)throws Exception {
        try(Fixture f=new Fixture(base.resolve("publication"));ModelPublicationR31.Plan p=publicationPlan(f)){
            openPublication(f,p);
            try{f.prune();throw new AssertionError("uncovered old PREPARING plan permitted GC");}catch(IllegalStateException expected){ok(expected.getMessage().contains("PENDING_PUBLICATION_PIN_COVERAGE"),"historical incomplete publication blocks destructive GC");}
            f.write("pin-complete-immutable-plan",db->{CheckpointGcStoreR31.pinPublicationWithin(db,p);return 0;});
            ok(f.scalar("SELECT reference_count FROM r31_gc_publication")==2&&f.scalar("SELECT count(*) FROM r31_gc_pin")==2,"complete plan covers state and initializer, not staged prefix");
            ok(f.prune()==0,"durable pending publication roots survive Prepared lease close");
            f.db.execSQL("DELETE FROM r31_gc_pin WHERE artifact_hash=?",new Object[]{h("pending warm initializer")});
            try{f.prune();throw new AssertionError("partial coverage permitted GC");}catch(IllegalStateException expected){ok(expected.getMessage().contains("PIN_COVERAGE"),"partial pin subset never means complete");}
            f.write("restore-complete-pins",db->{CheckpointGcStoreR31.pinPublicationWithin(db,p);return 0;});
        }
    }
    static void publicationChild(Path directory,String boundary)throws Exception {
        try(Fixture f=new Fixture(directory);ModelPublicationR31.Plan p=publicationPlan(f)){
            openPublication(f,p);f.write("OPEN-atomic-pin",db->{CheckpointGcStoreR31.pinPublicationWithin(db,p);return 0;});
            f.write("projection-and-pin-release",db->{f.root(h("pending warm state"));f.root(h("pending warm initializer"));db.execSQL("UPDATE r31_model_publication SET status='PUBLISHED'");CheckpointGcStoreR31.releasePublicationWithin(db,p);if(boundary.equals("before-publication-commit"))Runtime.getRuntime().halt(79);return 0;});
            Runtime.getRuntime().halt(79);
        }
    }
    static void publicationRecovery(Path base,String boundary)throws Exception {
        Path directory=base.resolve(boundary);Process child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-cp",System.getProperty("java.class.path"),CheckpointGcStoreR31Host.class.getName(),"publication-child",directory.toString(),boundary).redirectErrorStream(true).redirectOutput(directory.getParent().resolve(boundary+".child.log").toFile()).start();ok(child.waitFor()==79,"real publication process halt "+boundary);
        try(Fixture f=new Fixture(directory)){
            boolean before=boundary.equals("before-publication-commit");ok(f.scalar("SELECT count(*) FROM r31_gc_pin")== (before?2:0),"pins and publication are atomic "+boundary);ok(f.scalar("SELECT count(*) FROM r26_candidate")== (before?0:2),"projection commits with pin release "+boundary);
            ok(f.prune()==0&&Files.exists(f.file(h("pending warm state")))&&Files.exists(f.file(h("pending warm initializer"))),"reopened GC preserves both warm artifacts "+boundary);
        }
    }
    static void missingRootPreservesAll(Path base)throws Exception {
        try(Fixture f=new Fixture(base.resolve("missing-root"))){String root=h("missing original"),orphan=h("still existing orphan");
            try(CheckpointArtifactsR26.Prepared p=f.cas.prepareLease("missing original".getBytes(java.nio.charset.StandardCharsets.UTF_8));CheckpointArtifactsR26.Prepared q=f.cas.prepareLease("still existing orphan".getBytes(java.nio.charset.StandardCharsets.UTF_8))){f.root(root);}
            Files.delete(f.file(root));try{f.prune();throw new AssertionError("missing rooted file permitted GC");}catch(IOException expected){ok(true,"actual missing root blocks before any claim");}
            ok(Files.exists(f.file(orphan))&&f.metadata().state(orphan)==StorageGcR31.State.LIVE,"missing root does not claim or unlink other originals");
        }
    }
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("child")){killChild(Path.of(args[1]),args[2]);return;}
        if(args.length>0&&args[0].equals("publication-child")){publicationChild(Path.of(args[1]),args[2]);return;}
        Path base=Path.of(args[0]);Files.createDirectories(base);metadataContracts(base);actualFiles(base);killRecovery(base,"after-claim");killRecovery(base,"after-unlink");publicationContracts(base);publicationRecovery(base,"before-publication-commit");publicationRecovery(base,"after-publication-commit");missingRootPreservesAll(base);
        System.out.println("GC_NATIVE_PRODUCTION_PROTOCOL_PASS assertions="+checks+" writerTransactions="+transactions+" realProcessKillBoundaries=4 actualFilesAndNativeSqlite=true AndroidService=NOT_RUN fullModelPublicationProjection=NOT_INFERRED_FROM_PROTOCOL_FIXTURE naturalQuality=NOT_PROVEN");
    }
}
