package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.sql.*;
import org.sqlite.ProgressHandler;

/** Real producer VM steps. Direct JDBC seeding is fixture setup, never production work. */
public final class WarmIndexVmHostR31 {
    static final class Steps extends ProgressHandler {long count;protected int progress(){count++;return 0;}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");boolean old=args[0].equals("old");Path file=Paths.get(args[1]);Files.deleteIfExists(file);
        try(WarmAbiHostR31.World w=new WarmAbiHostR31.World(file,"retention")){
            SQLiteDatabase.scenario="CONTROLLED_VM_SETUP";if(old)w.db.execSQL("DROP INDEX r31_warm_candidate_state_owner");
            w.db.beginTransaction();try{
                try(PreparedStatement p=w.db.jdbc.prepareStatement("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,operation) VALUES('VM_SOURCE',?,'CONTROLLED_SQL_ONLY_NOT_MODEL_TRAINING','VM_SCAN_FIXTURE')")){
                    for(int i=0;i<100000;i++){p.setString(1,String.format("%064x",i+1));p.addBatch();if(i%1000==999)p.executeBatch();}
                }
                try(PreparedStatement p=w.db.jdbc.prepareStatement("INSERT INTO r26_warm_initialization VALUES(?,?,?,?,0,0,?,X'01',?,?,?,1)")){
                    for(int i=0;i<8;i++){String key=StatsR27.sha256("VM_ORPHAN_"+i);p.setString(1,key);p.setString(2,StatsR27.sha256("PARENT"));p.setString(3,StatsR27.sha256("CHECKPOINT"));p.setString(4,StatsR27.sha256("STATE"));p.setString(5,StatsR27.sha256("INITIALIZER"));p.setString(6,"VM_SOURCE");p.setString(7,StatsR27.sha256("SEED"));p.setString(8,"CONTROLLED_VM_ABI");p.addBatch();}p.executeBatch();
                }w.db.setTransactionSuccessful();
            }finally{w.db.endTransaction();}
            WarmAbiHostR31.need(WarmAbiHostR31.scalar(w.ro,"SELECT count(*) FROM r26_candidate")==100000,"ACTUAL_100000_ROW_NEGATIVE_DATABASE");Steps vm=new Steps();SQLiteDatabase.scenario=old?"BASELINE_ACTUAL_INITIALIZER_VM":"NEW_ACTUAL_INITIALIZER_VM";
            if(old){ProgressHandler.setHandler(w.db.jdbc,1,vm);try{w.coordinator.runAtomicWriteOrThrowF01("R31_VM_OLD_PRUNE",db->{RuntimeCoordinatorR26.requireFence(db,w.claim);WarmCandidateR26.pruneInitializers(db);RuntimeCoordinatorR26.requireFence(db,w.claim);return 0;});}finally{ProgressHandler.clearHandler(w.db.jdbc);}}
            else try(WarmCandidateR26.PreparedInitializerPruneR31 prepared=WarmCandidateR26.prepareInitializerPruneR31(w.ro)){WarmAbiHostR31.need(prepared.rows()==8,"EXACT_EIGHT_PREPARED_KEYS");ProgressHandler.setHandler(w.db.jdbc,1,vm);try{w.coordinator.runAtomicWriteOrThrowF01("R31_VM_NEW_PRUNE",db->{RuntimeCoordinatorR26.requireFence(db,w.claim);WarmCandidateR26.applyPreparedInitializerPruneR31(db,prepared);RuntimeCoordinatorR26.requireFence(db,w.claim);return 0;});}finally{ProgressHandler.clearHandler(w.db.jdbc);}}
            WarmAbiHostR31.need(WarmAbiHostR31.scalar(w.ro,"SELECT count(*) FROM r26_warm_initialization")==0,"EXACT_ORPHANS_PRUNED");
            System.out.println("RESULT {\"scope\":\"ACTUAL_WARM_PRUNE_NATIVE_VM_NOT_TIMING_SLO\",\"old\":"+old+",\"candidateRows\":100000,\"orphans\":8,\"vmSteps\":"+vm.count+"}");
            if(old){WarmAbiHostR31.need(vm.count>1000000,"OLD_NATIVE_SCAN_ACTUALLY_REPRODUCED");throw new AssertionError("BASELINE_INITIALIZER_WRITER_SCAN_R31:"+vm.count);}WarmAbiHostR31.need(vm.count<10000,"BOUNDED_INDEX_POINT_LOOKUPS");System.out.println("PASS "+WarmAbiHostR31.assertions+" REAL_NATIVE_INDEX_VM_BOUND");
        }
    }
}
