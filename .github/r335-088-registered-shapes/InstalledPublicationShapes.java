package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Locale;
public final class InstalledPublicationShapes {
    private static String sha(String s) throws Exception {
        byte[] bytes=MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
        StringBuilder out=new StringBuilder();
        for(byte b:bytes) out.append(String.format(Locale.ROOT,"%02x",b&255));
        return out.toString();
    }
    private static void shape(SQLiteDatabase db,String sql,Object[] args) throws Exception {
        String status="OK";
        try {
            new EvolutionPublicationSqlR31.Shape(db,sql,Collections.singletonList(new ResearchWriteQueueR27.Op(sql,args)));
        } catch(IllegalStateException err) {
            String message=err.getMessage();
            if(message!=null && message.matches("R31_EVOLUTION_OWNER_PLAN_REQUIRED:[A-Z_]+:[a-zA-Z_][a-zA-Z_0-9]*"))
                status=message;
            else status=err.getClass().getSimpleName();
        }
        System.out.println("SHAPE|"+sha(sql)+"|"+status);
    }
    public static void main(String[] args) throws Exception {
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(args[0],null,SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            shape(db,"INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)",new Object[]{"x",1L,1L,"x",1L,"x",1L});
            shape(db,"INSERT OR IGNORE INTO r27_expert_proposal(generation,expert,proposal_json,policy_hash,recorded_at) VALUES(?,?,?,?,?)",new Object[]{1L,"x","x",null,1L});
            shape(db,"INSERT OR IGNORE INTO r27_expert_proposal(generation,expert,proposal_json,policy_hash,recorded_at) VALUES(?,?,?,?,?)",new Object[]{1L,"x","x","x",1L});
            shape(db,"INSERT OR IGNORE INTO r27_policy(policy_hash,spec_json,idea_family,birth_generation,birth_issue,dataset_hash,state,state_reason,created_at,updated_at) VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{"x","x","x",1L,"x","x","x","x",1L,1L});
            shape(db,"INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash,behavior_novel) VALUES(?,?,?,?,?,?,?,?)",new Object[]{"x","x","x",null,"x","x","x",1L});
            shape(db,"INSERT OR IGNORE INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash,behavior_novel) VALUES(?,?,?,?,?,?,?,?)",new Object[]{"x","x","x","x","x","x","x",1L});
            shape(db,"INSERT OR IGNORE INTO r27_lineage(child_hash,parent_hash,role,operator,generation,created_at,origin) VALUES(?,?,?,?,?,?,?)",new Object[]{"x","x","x","x",1L,1L,"x"});
            shape(db,"UPDATE r27_policy SET state=?,state_reason=?,updated_at=? WHERE policy_hash=? AND state NOT IN ('LIVE')",new Object[]{"x","x",1L,"x"});
            shape(db,"INSERT OR REPLACE INTO r27_replay_score(policy_hash,dataset_hash,split,n_sched,n_act,hits,brier,logloss,ece,worst_block_hr,max_losing_streak,paired_u_mean,paired_u_lcb,skipped_gap,computed_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{"x","x","x",1L,1L,1L,1L,1L,1L,1L,1L,1L,1L,1L,1L});
            shape(db,"UPDATE r26_evolution SET research_accepted=1 WHERE attempt_id=?",new Object[]{"x"});
            shape(db,"INSERT OR REPLACE INTO r27_generation(generation,loop,dataset_hash,births,rejected,no_op,duplicates,survivors,retired,champion_hash,champion_select_u,champion_holdout_hr,prev_champion_holdout_hr,null_champion_holdout_hr,i00_champion_holdout_hr,paired_lcb,verified,detail_json,started_at,finished_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{1L,"x","x",1L,1L,1L,1L,1L,1L,"x",1L,null,null,null,null,null,1L,"x",1L,1L});
            shape(db,"INSERT OR IGNORE INTO r30_confirm_dataset(dataset_hash,payload,payload_hash,recorded_at) VALUES(?,?,?,?)",new Object[]{"x",new byte[]{1},"x",1L});
            shape(db,"INSERT OR IGNORE INTO r30_frozen_confirm(task_key,policy_hash,dataset_hash,block_id,generation,packet_json,created_at) VALUES(?,?,?,?,?,?,?)",new Object[]{"x","x","x",1L,1L,"x",1L});
            shape(db,"UPDATE r27_generation SET champion_holdout_hr=?,prev_champion_holdout_hr=?,null_champion_holdout_hr=?,i00_champion_holdout_hr=?,paired_lcb=?,verified=?,detail_json=? WHERE loop=? AND generation=? AND dataset_hash=?",new Object[]{1L,null,1L,1L,null,1L,"x","x",1L,"x"});
        }
    }
}
