package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.AtomReplayR27;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import sun.misc.Unsafe;

/** Actual atom scoring consumer, persisted paired SELECT estimates, report join, and failed commit publication. */
public final class BrierContributionR30HostTest {
    static void require(boolean yes,String why){if(!yes)throw new AssertionError(why);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(c);try(Statement s=c.createStatement()){for(String ddl:SchemaR27.DDL)if(ddl.startsWith("CREATE TABLE"))s.execute(ddl);s.execute(AtomScoreR27.CONTRIBUTION_DDL_R30);}
            ReplayDatasetR27 data=ReplayDatasetR27.synthetic(3000,601,new int[0]);SplitR27 split=SplitR27.of(data,null);
            AtomReplayR27.Frame frame=AtomReplayR27.buildLegacy(data,0,split.selectTo);
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);AtomicBoolean fail=new AtomicBoolean(false);
            rt.frameValues=Arrays.copyOf(frame.values,data.size());rt.framePresent=Arrays.copyOf(frame.present,data.size());rt.frameHit=Arrays.copyOf(frame.hit,data.size());
            for(int t=split.selectTo;t<data.size();t++){rt.frameValues[t]=new double[240];Arrays.fill(rt.frameValues[t],.5);rt.framePresent[t]=new boolean[240];Arrays.fill(rt.framePresent[t],true);rt.frameHit[t]=Integer.MIN_VALUE;}
            Map<Integer,String> memory=new HashMap<>();u.putObject(rt,u.objectFieldOffset(RuntimeR27.class.getDeclaredField("atomStates")),memory);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                try{c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,op);if(fail.getAndSet(false))throw new IllegalStateException("ATOM_CONTRIBUTION_COMMIT_FAILURE");c.commit();c.setAutoCommit(true);}
                catch(Exception e){try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
            },()->false);u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);ResearchPoolR27.Worker worker=pool.new Worker(db,"contribution-test");
            AtomWorldR27.score(rt,worker,data,split);int eligible=AtomReplayR27.eligible().length;
            require(count(db,"SELECT count(*) FROM r27_atom_score")==eligible&&count(db,"SELECT count(*) FROM r30_atom_contribution")==eligible,"REAL_ATOM_PRODUCER_NOT_PERSISTED");
            try(Cursor row=db.rawQuery("SELECT s.atom_index,c.n_periods,c.mean_gain,c.lcb95,c.from_row,c.to_row FROM r27_atom_score s JOIN r30_atom_contribution c ON c.atom_id=s.atom_id AND c.dataset_hash=s.dataset_hash WHERE c.n_periods>=2 LIMIT 1",null)){
                require(row.moveToFirst(),"NO_MEASURED_ATOM");AtomScoreR27.Score expected=AtomScoreR27.score(row.getInt(0),rt.frameValues,rt.framePresent,rt.frameHit,split.selectFrom,split.selectTo);
                require(row.getLong(1)==expected.nLateSelect&&row.getDouble(2)==expected.contribution().mean()&&row.getDouble(3)==expected.contribution().lower95(),"SQL_NOT_ACTUAL_PAIRED_BRIER");
                require(row.getLong(4)==split.selectFrom&&row.getLong(5)==split.selectTo&&expected.nHoldout==0,"CONFIRM_CONTAMINATED_CONTRIBUTION");
            }
            Map<String,Object> report=ReportR29.atomContributionsR30(db);require(((List<?>)report.get("rows")).size()==eligible&&((Number)report.get("measuredAtoms")).longValue()>0,"REPORT_NOT_FROM_COMMITTED_ATOMS");
            try(Cursor source=db.rawQuery(ReportDataR27.query("ATOMS"),null)){require(source.getCount()==eligible,"EXISTING_ATOMS_CHAPTER_LOST_ROWS");}
            // A failed update must not publish new generator weights before the old/new tables are durable.
            memory.put(9999,"COMMITTED_MEMORY_SENTINEL");Map<Integer,String> prior=new HashMap<>(memory);fail.set(true);
            try{AtomWorldR27.score(rt,worker,data,split);throw new AssertionError("FAILURE_NOT_INJECTED");}catch(IllegalStateException expected){}
            require(prior.equals(memory),"ATOM_WEIGHTS_PUBLISHED_BEFORE_FAILED_COMMIT");
            require(count(db,"SELECT count(*) FROM r30_atom_contribution")==eligible,"FAILED_WRITE_CHANGED_PAIRED_TABLE");
            AtomWorldR27.score(rt,worker,data,split);require(!memory.containsKey(9999),"SUCCESSFUL_COMMIT_DID_NOT_PUBLISH_WEIGHTS");
            rt.derived=new ArrayList<>();EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;
            rt.evo=new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new StoreR27.EvolutionSink("HOST","fixture"),()->0L);rt.evo.stepDataset();
            AtomWorldR27.moleculeGeneration=-1;fail.set(true);
            try{AtomWorldR27.molecules(rt,worker,rt.evo);throw new AssertionError("MOLECULE_FAILURE_NOT_INJECTED");}catch(IllegalStateException expected){}
            require(AtomWorldR27.moleculeGeneration==-1&&count(db,"SELECT count(*) FROM r27_molecule")==0,"MOLECULE_CURSOR_ADVANCED_BEFORE_FAILED_COMMIT");
            AtomWorldR27.molecules(rt,worker,rt.evo);require(AtomWorldR27.moleculeGeneration==rt.evo.generation&&count(db,"SELECT count(*) FROM r27_molecule")>0,"MOLECULE_RETRY_LOST");
            require(count(db,"SELECT count(*) FROM r30_atom_contribution WHERE atom_id LIKE 'MOLECULE:%'")>0,"MOLECULE_PAIRED_BRIER_NOT_STORED");
            System.out.println("R30_BRIER_SQLITE|ACTUAL_ATOMS="+eligible+"|PAIRED_MEAN_AND_ONE_SIDED_LCB_PERSISTED|H_CONFIRM_LABELS_POISONED|REPORT_AND_EXISTING_ATOMS_CHAPTER_MATCH|FAILED_COMMIT_KEEPS_GENERATOR_WEIGHTS|RETRY_PUBLISHES|MOLECULE_CURSOR_ROLLBACK_AND_BRIER_REPORT|PASS");
        }
    }
}
