package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Durable original-state pins, reachability GC roots and actual learned-state restoration; synthetic only. */
public final class LegacySnapshotR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_LEGACY_STATE|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    public static void main(String[]args)throws Exception{
        ModelGenomeSU2 genome=new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member("MSDIR",ModelEvolutionSU2.centralParams("MSDIR"),1)),"STATIC",false,"MASS",.3,.2,true);
        LegacyReplayStateR26 learned=new LegacyReplayStateR26(Collections.singletonList(genome.text()),1000);
        for(int t=0;t<40;t++){int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=(p+t)%10+1;learned.advance(draw);}
        // The original state digest hashes base64 text; the CAS filename hashes raw bytes. Preserve both ABIs.
        byte[] full=learned.encode();String fileHash=StatsR27.sha256(full),hash=LiveStateR26.hash(full),abi=StatsR27.sha256("HOST_ABI"),checkpoint=HashingSU1.fields("R26_LEGACY_CHECKPOINT_V1","roster","source","parent",Long.toString(learned.through()),hash);
        byte[] reference=("R26CAS1:"+fileHash).getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        try(SQLiteDatabase rt=EventBridgeR29HostTest.open(true)){
            rt.execSQL("CREATE TABLE r26_legacy_runtime(singleton INTEGER PRIMARY KEY,numerical_abi TEXT,checkpoint_hash TEXT,state_hash TEXT,state BLOB,through_ordinal INTEGER,genesis_ordinal INTEGER,cursor INTEGER,roster_hash TEXT,snapshot_hash TEXT,parent_hash TEXT,source_through INTEGER)");
            rt.execSQL("INSERT INTO r26_legacy_runtime VALUES(1,?,?,?,?,?,?,40,'roster','source','parent',2000)",new Object[]{abi,checkpoint,hash,reference,learned.through(),learned.genesis});
            try{EventBridgeR29HostTest.tx(rt,()->LegacyCapabilitySnapshotR29.pinCurrentWithin(rt,abi),true);}catch(IllegalStateException expected){}
            check("L1_KILLED_PIN_CANNOT_PUBLISH_PARTIAL_EXPORT",n(rt,"SELECT count(*) FROM r29_legacy_checkpoint_pin")==0);
            EventBridgeR29HostTest.tx(rt,()->LegacyCapabilitySnapshotR29.pinCurrentWithin(rt,abi),false);
            LegacyCapabilitySnapshotR29.Pinned pin=LegacyCapabilitySnapshotR29.pinned(rt,abi);
            check("L2_COMPLETE_ORIGINAL_STATE_REFERENCE_IS_PINNED",pin!=null&&Arrays.equals(pin.reference,reference)&&pin.through==learned.through()&&pin.cursor==40);
            rt.execSQL("UPDATE r26_legacy_runtime SET state=x'01',state_hash='new',checkpoint_hash='new',through_ordinal=9999");
            EventBridgeR29HostTest.tx(rt,()->LegacyCapabilitySnapshotR29.pinCurrentWithin(rt,abi),false);
            check("L3_LATER_TRAINING_CANNOT_OVERWRITE_BASELINE_PIN",hash.equals(LegacyCapabilitySnapshotR29.pinned(rt,abi).hash));
            check("L4_GC_REACHABILITY_RETAINS_EXPORTED_ORIGINAL_STATE",CheckpointReferencesR26.hashes(rt).contains(fileHash));
            boolean rejected=false;try{rt.execSQL("DELETE FROM r29_legacy_checkpoint_pin");}catch(RuntimeException expected){rejected=true;}
            check("L5_ORIGINAL_STATE_PIN_CANNOT_BE_DELETED",rejected);
            Path directory=Files.createTempDirectory("r29-legacy-state-host"),file=directory.resolve(fileHash+".bin");
            try{
                Files.write(file,full);CheckpointArtifactsR26 artifacts=new CheckpointArtifactsR26(directory.toFile());
                LegacyReplayStateR26 restored=LegacyCapabilitySnapshotR29.restore(artifacts,pin);
                check("L6_ACTUAL_ORIGINAL_LEARNED_STATE_RESTORES",Arrays.equals(full,restored.encode()));
                int[] next={10,9,8,7,6,5,4,3,2,1};learned.advance(next);restored.advance(next);
                check("L7_NEXT_LEARNED_UPDATE_REMAINS_BITWISE_IDENTICAL",Arrays.equals(learned.encode(),restored.encode()));
                Map<String,Object> descriptor=LegacyCapabilitySnapshotR29.describe(restored,pin);
                check("L8_EXPORT_CANNOT_TRANSFER_OR_INVENT_QUALITY",Boolean.FALSE.equals(descriptor.get("naturalEvidenceTransferred"))&&((Number)descriptor.get("newTrainingSamples")).longValue()==0);
                pin.through++;rejected=false;try{LegacyCapabilitySnapshotR29.restore(artifacts,pin);}catch(IOException expected){rejected=true;}pin.through--;
                check("L9_CURSOR_MISMATCH_IS_BLOCKED_WITH_STATE_RETAINED",rejected&&Files.exists(file));
                Files.write(file,new byte[]{1,2,3});rejected=false;try{LegacyCapabilitySnapshotR29.restore(artifacts,pin);}catch(IOException expected){rejected=true;}
                check("L10_CORRUPT_ARTIFACT_CANNOT_BECOME_REPRODUCED",rejected);
            }finally{Files.deleteIfExists(file);Files.deleteIfExists(directory);}
        }
        try(SQLiteDatabase research=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:"))){
            research.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT,mechanism TEXT)");
            research.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY)");
            EventBridgeR29HostTest.tx(research,()->SchemaR27.ensureResearch(research),false);
            check("L11_RESEARCH_SCHEMA_UPGRADE_HAS_NO_REALTIME_AUTHORITIES",n(research,"SELECT count(*) FROM sqlite_master WHERE name IN ('r29_event_outbox','r29_scheduler_owner','r29_legacy_checkpoint_pin')")==0&&n(research,"SELECT count(*) FROM sqlite_master WHERE name='r29_legacy_capability_snapshot'")==1);
        }
        Path roots=Files.createTempDirectory("r29-reference-two-domain-"),rootFile=roots.resolve("root.db");
        String retained=StatsR27.sha256("retained Root original model"),pinHash=StatsR27.sha256("Root immutable source pin"),current=StatsR27.sha256("current migrated RP model");
        try(SQLiteDatabase root=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite:"+rootFile));SQLiteDatabase research=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:"))){
            String model="CREATE TABLE r26_live_model(policy_hash TEXT,state BLOB,parent_hash TEXT,canonical_text TEXT)";root.execSQL(model);research.execSQL(model);
            root.execSQL(LegacyCapabilitySnapshotR29.PIN);root.execSQL("INSERT INTO r29_legacy_checkpoint_pin VALUES('abi','cp','state',?,0,0,0,'roster','{}',0)",new Object[]{("R26CAS1:"+pinHash).getBytes(java.nio.charset.StandardCharsets.US_ASCII)});
            root.execSQL("INSERT INTO r26_live_model VALUES('old',?,'','')",new Object[]{("R26CAS1:"+retained).getBytes(java.nio.charset.StandardCharsets.US_ASCII)});
            research.execSQL(ResearchRouteR29.OWNER_DDL);research.execSQL("INSERT INTO r29_research_owner VALUES(1,'READY','schema','root',1,1)");research.execSQL("INSERT INTO r26_live_model VALUES('current',?,'','')",new Object[]{("R26CAS1:"+current).getBytes(java.nio.charset.StandardCharsets.US_ASCII)});
            research.execSQL("ATTACH DATABASE ? AS realtime",new Object[]{rootFile.toString()});
            Set<String> refs=CheckpointReferencesR26.hashes(research);
            check("L13_GC_UNIONS_CURRENT_RESEARCH_RETAINED_ROOT_AND_ROOT_PIN",refs.contains(current)&&refs.contains(retained)&&refs.contains(pinHash));
            check("L14_REFERENCE_CENSUS_DOES_NOT_CHANGE_EITHER_DOMAIN",n(research,"SELECT count(*) FROM main.r26_live_model")==1&&n(root,"SELECT count(*) FROM r26_live_model")==1&&n(root,"SELECT count(*) FROM r29_legacy_checkpoint_pin")==1);
        }finally{Files.deleteIfExists(rootFile);Files.deleteIfExists(roots);}
        check("L12_UNCOMMITTED_COMPUTATION_IS_NOT_COMMITTED_PROGRESS",!WaitKindR27.PROGRESS.equals(WaitKindR27.classify(0,"COOPERATIVE_PRIVATE_STATE_ONLY",null,0,10,null,false).kind)&&WaitKindR27.PROGRESS.equals(WaitKindR27.classify(1,null,null,0,10,null,false).kind));
        System.out.println("R29_LEGACY_STATE|ALL_PASS|checks="+checks+"|ORIGINAL_STATE_ONLY_NOT_NATURAL_QUALITY");
    }
}
