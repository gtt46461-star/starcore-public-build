package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;

/** Copies are made by the runner. All business/source rows here come from prior public owners. */
public final class matrix_registration_owner_R75 {
    static final class TimedDb extends SQLiteDatabase {
        long longestQueryMs,longestConditionalMs;int slowQueries;
        TimedDb(String bridge,String path)throws Exception{super(bridge,path);}
        @Override public synchronized Cursor rawQuery(String sql,String[] args){long begin=System.nanoTime();try{return super.rawQuery(sql,args);}finally{long ms=(System.nanoTime()-begin)/1_000_000L;longestQueryMs=Math.max(longestQueryMs,ms);if(sql.contains("FROM draws d WHERE")&&sql.contains("research_matrix_regime5_cp12"))longestConditionalMs=Math.max(longestConditionalMs,ms);if(ms>100){slowQueries++;System.out.println("SLOW_SQL ms="+ms+" sql="+sql);}}}
    }
    static int checks;
    static void ok(boolean value,String reason){if(!value)throw new AssertionError(reason);checks++;System.out.println("PASS "+reason);}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()&&!c.isNull(0)?c.getString(0):"";}}
    static String roleSnapshot(SQLiteDatabase db){StringBuilder b=new StringBuilder();for(String table:new String[]{"r952_candidate","r952_artifact","research_training_protocol_v2","research_development_receipt_v2","r967_frozen_evaluation_v2","research_formal_observation_cp12","research_fixed_pair_r932","research_cp4_frozen_lifetime","research_certificate_r929","trades",CandidateCalibrationR932.OBS,CandidateCalibrationR932.MODEL})b.append(table).append('=').append(n(db,"SELECT COUNT(*) FROM "+table)).append(';');b.append(n(db,"SELECT COALESCE(SUM(future_n+may_create_real),0) FROM r952_candidate"));return b.toString();}
    static DbStore store(SQLiteDatabase db,String[] a)throws Exception{DbStore s=HostProductR974.store(db);File root=new File(a[2]);root.mkdirs();HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;}
    static void pulse(SQLiteDatabase db,String[] a)throws Exception{ResearchWorkBudgetR927.Scope budget=ResearchWorkBudgetR927.enter(300);try{MatrixSnapshotBusR952.drainOneCP4(store(db,a),System.nanoTime()+250_000_000L);}catch(ResearchWorkBudgetR927.Yield allowed){}finally{ResearchWorkBudgetR927.leave(budget);}}
    public static void main(String[] a)throws Exception{
      try(TimedDb db=new TimedDb(a[0],a[1])){
        RealtimePressureGateR811.resetForTests();String originalRole=roleSnapshot(db);
        try(Cursor frozen=db.rawQuery("SELECT ast FROM r967_frozen_candidate_all",null)){while(frozen.moveToNext()){
          CandidateAstR952 ast=CandidateAstR952.parse(frozen.getString(0));
          ok(a[5].equals("neural")?ast.graphSpec.featureOutputIds.equals(Collections.singletonList("n2"))&&ast.graphSpec.typedGraph.require("CP4:n2").kind!=TypedAtomMoleculeGraphR930.NodeKind.ATOM:ast.graphSpec.featureOutputIds.isEmpty(),"PUBLIC_OWNER_FIXTURE_HAS_NO_DIRECT_ATOM_FEATURE");
        }}
        long discovered=n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='DISCOVERED'"),requires=n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='REQUIRES_BINDING'");
        String progress=text(db,"SELECT GROUP_CONCAT(state_key||'='||state_value,';') FROM (SELECT state_key,state_value FROM app_state WHERE state_key LIKE 'r75_matrix_registration_%' ORDER BY state_key)");
        for(int i=0;i<5;i++)ok(!MatrixSnapshotBusR952.drainOneCP4(store(db,a),System.nanoTime()+1_000_000L),"EXHAUSTED_OWNER_YIELDS_"+i);
        ok(progress.equals(text(db,"SELECT GROUP_CONCAT(state_key||'='||state_value,';') FROM (SELECT state_key,state_value FROM app_state WHERE state_key LIKE 'r75_matrix_registration_%' ORDER BY state_key)")),"EXHAUSTED_OWNER_PRESERVES_DURABLE_CURSOR");
        int limit=a[4].equals("first")?20:150,pulses=0;long started=System.nanoTime();for(;pulses<limit;pulses++){
          pulse(db,a);
          if(a[4].equals("resume")&&pulses%10==9
              &&n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type IN('FrequencyMatrix','OmissionMatrix','TransitionMatrix','DriftMatrix','ConditionalTransitionMatrix')")==90
              &&n(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2 WHERE matrix_type IN('FrequencyMatrix','OmissionMatrix','TransitionMatrix','DriftMatrix')")==40
              &&n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='RuleHealthMatrix'")==n(db,"SELECT COUNT(*) FROM r967_frozen_candidate_all")
              &&n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='AtomTargetMatrix'")==0){pulses++;break;}
        }
        ok(originalRole.equals(roleSnapshot(db)),"REGISTRATION_AND_PROJECTION_DO_NOT_CREATE_QUALITY_OR_TRAINING_EVIDENCE");
        ok(discovered==n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='DISCOVERED'")&&requires==n(db,"SELECT COUNT(*) FROM research_atom_spec_v2 WHERE state='REQUIRES_BINDING'"),"CATALOG_STATES_UNCHANGED");
        ok(n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type IN('TeacherMatrix','CoOccurrenceMatrix','CandidateSimilarityMatrix','CandidateDiversityMatrix')")==0,"NO_FAKE_TEACHER_OR_COMMON_FUTURE");
        ok(n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='RuleHealthMatrix'")>0,"REAL_FROZEN_OWNER_DISCOVERED");
        ok(n(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2")>0,"ACTUAL_MATRIX_OWNER_PROJECTED_SOURCE");
        if(a[4].equals("resume")){
          ok(n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type IN('FrequencyMatrix','OmissionMatrix','TransitionMatrix','DriftMatrix','ConditionalTransitionMatrix')")==90,"ALL_90_DEFAULTS_RESUMED_WITHOUT_DUPLICATES");
          ok(n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='RuleHealthMatrix'")==n(db,"SELECT COUNT(DISTINCT structural_hash) FROM r967_frozen_candidate_all"),"EVERY_REAL_FREEZE_DISCOVERED");
          ok(n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='ParentChildMatrix'")==(a[5].equals("repair")?1:0),"ONLY_ACTUAL_REGISTERED_LINEAGE_GETS_PARENT_CHILD_SPEC");
          long targets=n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12 WHERE matrix_type='AtomTargetMatrix'");
          ok(targets==0,"INTERMEDIATE_ATOM_CANNOT_BORROW_MODEL_FEATURE_NORMALIZER");
          ok(n(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2 WHERE matrix_type IN('AtomTargetMatrix','AtomRegimeMatrix','TeacherMatrix','ParentChildMatrix','CoOccurrenceMatrix')")==0,"MISSING_GENUINE_SOURCE_DOES_NOT_BECOME_MATRIX_SAMPLE");
          ok(n(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2 WHERE matrix_type IN('FrequencyMatrix','OmissionMatrix','TransitionMatrix','DriftMatrix')")==40,"SOURCE_CURSOR_SERVES_ALL_FORTY_HISTORY_PARTITIONS");
          ok(db.longestConditionalMs<100,"EMPTY_CONDITIONAL_SOURCE_QUERY_UNDER_100MS_HOST_FIXTURE");
          String latest=text(db,"SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1");
          try(Cursor c=db.rawQuery("SELECT candidate_id FROM r967_frozen_candidate_all",null)){while(c.moveToNext()){
            FrozenCandidateAdapterR958.Frozen frozen=FrozenCandidateAdapterR958.load(db,c.getString(0));
            try(AtomAdmissionCP12.Group admission=AtomAdmissionCP12.readForFrozenCP12(db,frozen.structure,frozen.model,frozen.ast,latest,System.nanoTime()+1_000_000_000L)){
              ok(!admission.eligible,"REAL_ATOM_ADMISSION_STILL_REJECTS_NO_OOS_"+frozen.id);
            }
          }}
        }
        Map<String,Object> out=new TreeMap<>();out.put("status","PASS");out.put("checks",checks);out.put("phase",a[4]);out.put("fixture",a[5]);out.put("pulses",pulses);out.put("elapsedMs",(System.nanoTime()-started)/1_000_000L);out.put("specs",n(db,"SELECT COUNT(*) FROM research_matrix_spec_cp12"));out.put("checkpoints",n(db,"SELECT COUNT(*) FROM research_matrix_checkpoint_v2"));out.put("inputReceipts",n(db,"SELECT COUNT(*) FROM research_matrix_input_cp12"));out.put("longestQueryMs",db.longestQueryMs);out.put("longestConditionalMs",db.longestConditionalMs);out.put("slowQueriesOver100ms",db.slowQueries);out.put("sourceScope","COPIED_PUBLIC_TRAIN_FREEZE_OWNER_FIXTURE_NO_HAND_INSERTED_MATRIX_SOURCE_NO_LIVE_DEVICE_QUALITY");out.put("businessRoleSnapshot",originalRole);
        Files.writeString(Path.of(a[6]),StrictCanonicalJson.writeContext(out));System.out.println(StrictCanonicalJson.writeContext(out));
      }
    }
}
