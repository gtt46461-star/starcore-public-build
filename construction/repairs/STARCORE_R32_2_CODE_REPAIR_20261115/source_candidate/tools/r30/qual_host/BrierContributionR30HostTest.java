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
    static void require(boolean yes,String why){R31AtomPublicationHost.need(yes,why);}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    public static void main(String[] args)throws Exception{
        java.io.File output=new java.io.File(args[1]);
        try(R31AtomPublicationHost h=new R31AtomPublicationHost("brier-original",output,null,true)){
            try{
            SQLiteDatabase db=h.db;h.scientificFrame();ReplayDatasetR27 data=h.data;SplitR27 split=h.split;RuntimeR27 rt=h.runtime;ResearchPoolR27.Worker worker=h.worker;Map<Integer,String> memory=rt.atomStates;
            AtomWorldR27.score(rt,worker,data,split);int eligible=AtomReplayR27.eligible().length;
            require(count(db,"SELECT count(*) FROM r31_atom_score_visible WHERE derived=0")==eligible&&count(db,"SELECT count(*) FROM r31_atom_contribution_visible")==eligible,"REAL_ATOM_PRODUCER_NOT_PERSISTED");
            try(Cursor row=db.rawQuery("SELECT s.atom_index,c.n_periods,c.mean_gain,c.lcb95,c.from_row,c.to_row FROM r31_atom_score_visible s JOIN r31_atom_contribution_visible c ON c.atom_id=s.atom_id AND c.dataset_hash=s.dataset_hash WHERE c.n_periods>=2 LIMIT 1",null)){
                require(row.moveToFirst(),"NO_MEASURED_ATOM");AtomScoreR27.Score expected=AtomScoreR27.score(row.getInt(0),rt.frameValues,rt.framePresent,rt.frameHit,split.selectFrom,split.selectTo);
                require(row.getLong(1)==expected.nLateSelect&&row.getDouble(2)==expected.contribution().mean()&&row.getDouble(3)==expected.contribution().lower95(),"SQL_NOT_ACTUAL_PAIRED_BRIER");
                require(row.getLong(4)==split.selectFrom&&row.getLong(5)==split.selectTo&&expected.nHoldout==0,"CONFIRM_CONTAMINATED_CONTRIBUTION");
            }
            Map<String,Object> report=ReportR29.atomContributionsR30(db);require(((List<?>)report.get("rows")).size()==eligible&&((Number)report.get("measuredAtoms")).longValue()>0,"REPORT_NOT_FROM_COMMITTED_ATOMS");
            try(Cursor source=db.rawQuery(ReportDataR27.query("ATOMS"),null)){require(source.getCount()==eligible,"EXISTING_ATOMS_CHAPTER_LOST_ROWS");}
            // A fresh guarded source revision reuses exactly the original frame, split and opportunities.
            // Replaying an already completed input must be idempotent, so it cannot serve as a failed-update fixture.
            String old=h.text("SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES'");
            memory.put(9999,"COMMITTED_MEMORY_SENTINEL");Map<Integer,String> prior=new HashMap<>(memory);h.newInputSameScientificOpportunities();h.failStage=h.stageCalls+83;
            try{AtomWorldR27.score(rt,worker,data,split);throw new AssertionError("FAILURE_NOT_INJECTED");}catch(com.ethk3.novaonepro.DatabaseCoordinator.WriteRejectedR31 expected){require(h.injected,"REAL_COORDINATOR_FAILURE_NOT_INJECTED");}
            require(prior.equals(memory),"ATOM_WEIGHTS_PUBLISHED_BEFORE_FAILED_COMMIT");
            require(count(db,"SELECT count(*) FROM r31_atom_contribution_visible")==eligible,"FAILED_WRITE_CHANGED_PAIRED_TABLE");
            require(old.equals(h.text("SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES'")),"OLD_CURRENT_LOST_AFTER_PARTIAL_FAILURE");
            require(count(db,"SELECT count(*) FROM r31_atom_score_stage WHERE run_id<>(SELECT run_id FROM r31_atom_result_active WHERE kind='SCORES')")==82,"FAILURE_DID_NOT_LEAVE_REAL_INVISIBLE_PARTIAL_STAGE");
            AtomWorldR27.score(rt,worker,data,split);require(!memory.containsKey(9999),"SUCCESSFUL_COMMIT_DID_NOT_PUBLISH_WEIGHTS");
            rt.derived=new ArrayList<>();EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.nullControl=false;
            rt.evo=new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new StoreR27.EvolutionSink("HOST","fixture"),()->0L);rt.evo.stepDataset();
            AtomWorldR27.moleculeGeneration=-1;h.injected=false;h.failStage=h.stageCalls+17;
            try{AtomWorldR27.molecules(rt,worker,rt.evo);throw new AssertionError("MOLECULE_FAILURE_NOT_INJECTED");}catch(com.ethk3.novaonepro.DatabaseCoordinator.WriteRejectedR31 expected){require(h.injected,"REAL_MOLECULE_FAILURE_NOT_INJECTED");}
            require(AtomWorldR27.publishedMoleculeGenerationR31(rt)==-1&&count(db,"SELECT count(*) FROM r31_molecule_visible")==0,"MOLECULE_CURSOR_ADVANCED_BEFORE_FAILED_COMMIT");
            require(count(db,"SELECT count(*) FROM r31_molecule_stage")==16,"REAL_INCOMPLETE_MOLECULE_STAGE_MISSING");
            AtomWorldR27.molecules(rt,worker,rt.evo);require(AtomWorldR27.publishedMoleculeGenerationR31(rt)==rt.evo.generation&&count(db,"SELECT count(*) FROM r31_molecule_visible")>0,"MOLECULE_RETRY_LOST");
            require(AtomWorldR27.moleculeGeneration==-1,"RUNTIME_CHANGED_ANOTHER_OWNER_GLOBAL_GENERATION");
            require(count(db,"SELECT count(*) FROM r31_molecule_stage WHERE run_id=(SELECT run_id FROM r31_atom_result_active WHERE kind='MOLECULES')")==32,"ORIGINAL_32_CANDIDATES_REDUCED");
            require(count(db,"SELECT count(*) FROM r31_atom_contribution_visible WHERE atom_id LIKE 'MOLECULE:%'")>0,"MOLECULE_PAIRED_BRIER_NOT_STORED");
            // A fresh runtime needs its own complete consumer state even at the same persisted generation.
            RuntimeR27 restored=h.newRuntime();restored.atomStates.putAll(AtomPublicationStoreR31.activeStates(db));restored.evo=new EvolutionLoopR27(cfg,new FeatureCacheR27(data),new StoreR27.EvolutionSink("HOST","fixture"),()->0L);restored.evo.stepDataset();
            long receipts=count(db,"SELECT count(*) FROM r31_commit_receipt");require(AtomWorldR27.molecules(restored,worker,restored.evo)==32&&AtomWorldR27.publishedMoleculeGenerationR31(restored)==restored.evo.generation,"FRESH_RUNTIME_SKIPPED_COMPLETE_MOLECULE_CONSUMER");require(count(db,"SELECT count(*) FROM r31_commit_receipt")==receipts,"RESTORED_RUNTIME_DUPLICATED_SAME_INPUT_COMMIT");require(AtomWorldR27.moleculeGeneration==-1,"FRESH_RUNTIME_CHANGED_GLOBAL_OWNER_CURSOR");
            h.compareActualBaselineScience();
            h.assertBudget(false);h.writeEvidence("ORIGINAL_BRIER_ASSERTIONS_COMPLETED_ON_PRODUCTION_COMPLETE_PUBLICATIONS");
            System.out.println("R30_BRIER_SQLITE|ACTUAL_ATOMS="+eligible+"|PAIRED_MEAN_AND_ONE_SIDED_LCB_PERSISTED|H_CONFIRM_LABELS_POISONED|REPORT_AND_EXISTING_ATOMS_CHAPTER_MATCH|FAILED_COMMIT_KEEPS_GENERATOR_WEIGHTS|RETRY_PUBLISHES|MOLECULE_CURSOR_ROLLBACK_AND_BRIER_REPORT|PASS");
            }catch(Throwable failure){h.writeEvidence("FAILED_ORIGINAL_BRIER_ASSERTION");throw failure;}
        }
    }
}
