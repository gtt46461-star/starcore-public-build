package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.AppConfig;

/** Transactional receipts of computations actually consumed by a numeric prediction.
 * A registered atom, an empty row or an inherited score is never a computation receipt.
 * Detailed traces retain at most 5000 issues. The original predraw and lineage remain independent.
 */
public final class ComputationReceiptR974 {
    public static final String TABLE="r974_computation_receipt",LINEAGE="r974_model_lineage";
    private ComputationReceiptR974(){}
    static void schema(SQLiteDatabase db){
        db.execSQL("CREATE TABLE IF NOT EXISTS r974_prediction_rejection(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,certificate_hash TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_id,certificate_hash))");

        db.execSQL("CREATE TABLE IF NOT EXISTS r974_exact_real_receipt(target_issue TEXT PRIMARY KEY,trade_id INTEGER NOT NULL,candidate_id TEXT NOT NULL,model_hash TEXT NOT NULL,certificate_hash TEXT NOT NULL,proof_hash TEXT NOT NULL,recipe_hash TEXT NOT NULL,action_hash TEXT NOT NULL,authorization_hash TEXT NOT NULL,committed_at INTEGER NOT NULL)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r974_real_no_update BEFORE UPDATE ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'R974_REAL_RECEIPT_IMMUTABLE'); END");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(target_issue TEXT NOT NULL,candidate_id TEXT NOT NULL,ast_hash TEXT NOT NULL,model_hash TEXT NOT NULL,predraw_input_hash TEXT NOT NULL,world_input_hash TEXT NOT NULL,world_output_hash TEXT NOT NULL,features TEXT NOT NULL,prediction_mask INTEGER NOT NULL,trace TEXT NOT NULL,receipt_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_id))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r974_compute_issue ON "+TABLE+"(target_issue)");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r974_compute_no_update BEFORE UPDATE ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R974_COMPUTATION_IMMUTABLE'); END");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+LINEAGE+"(child_id TEXT NOT NULL,parent_id TEXT NOT NULL,parent_role TEXT NOT NULL,parent_ast TEXT NOT NULL,evidence_hash TEXT NOT NULL,created_issue TEXT NOT NULL,PRIMARY KEY(child_id,parent_id))");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r974_lineage_no_update BEFORE UPDATE ON "+LINEAGE+" BEGIN SELECT RAISE(ABORT,'R974_LINEAGE_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r974_lineage_no_delete BEFORE DELETE ON "+LINEAGE+" BEGIN SELECT RAISE(ABORT,'R974_LINEAGE_PERMANENT'); END");
    }
    static void record(SQLiteDatabase db,String target,String id,String ast,String model,String input,CandidateSandboxR952.Prediction p){
        if(db==null||!db.inTransaction()||p==null||p.computation==null)throw new IllegalStateException("R974_COMPUTATION_TRANSACTION");
        if(!TargetIssueResolver.isValid(target)||id==null||id.isEmpty()||!sha(ast)||!sha(model)||!sha(input)||AppConfig.countK3MaskNumbers(p.mask)!=3)
            throw new IllegalArgumentException("R974_COMPUTATION_IDENTITY_K3");
        WorldFeatureComputationR974.Result world=p.computation;
        if(!sha(world.inputHash)||!sha(world.outputHash))throw new IllegalArgumentException("R974_WORLD_HASH");
        for(double value:world.values())if(!Double.isFinite(value))throw new IllegalArgumentException("R974_FEATURE_NONFINITE");
        if(!p.features.equals(CandidateCompilerR952.encode(world.values())))throw new IllegalStateException("R974_COMPUTATION_NOT_CONSUMED");
        String hash=V51ChecksumEngine.sha256("R974_COMPUTATION|"+target+'|'+id+'|'+ast+'|'+model+'|'+input+'|'+world.outputHash+'|'+p.mask);
        try(Cursor c=db.rawQuery("SELECT receipt_hash FROM "+TABLE+" WHERE target_issue=? AND candidate_id=?",new String[]{target,id})){
            if(c.moveToFirst()){if(!hash.equals(c.getString(0)))throw new IllegalStateException("R974_COMPUTATION_REPLAY_DRIFT");return;}
        }
        db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{target,id,ast,model,input,world.inputHash,world.outputHash,p.features,p.mask,world.trace,hash,System.currentTimeMillis()});
        // Issue-based rolling window, not a counter reset and not a deletion of parent formulas.
        db.execSQL("DELETE FROM "+TABLE+" WHERE target_issue<(SELECT target_issue FROM "+TABLE+" GROUP BY target_issue ORDER BY target_issue DESC LIMIT 1 OFFSET 4999)");
    }
    static void lineage(SQLiteDatabase db,String child,String parent,String role,String formula,String evidence,String issue){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R974_LINEAGE_TRANSACTION");
        if(child==null||child.isEmpty()||parent==null||parent.isEmpty()||child.equals(parent)
                ||(!"PRIMARY".equals(role)&&!"FUSION_SECONDARY".equals(role))||!sha(evidence)||!TargetIssueResolver.isValid(issue))
            throw new IllegalArgumentException("R974_LINEAGE_IDENTITY");
        CandidateAstR952 parentAst=CandidateAstR952.parse(formula);
        try(Cursor c=db.rawQuery("SELECT parent_role,parent_ast,evidence_hash,created_issue FROM "+LINEAGE+" WHERE child_id=? AND parent_id=?",new String[]{child,parent})){
            if(c.moveToFirst()){
                if(!role.equals(c.getString(0))||!formula.equals(c.getString(1))||!evidence.equals(c.getString(2))||!issue.equals(c.getString(3)))throw new IllegalStateException("R974_LINEAGE_DRIFT");
                return; // Exact original receipt replay cannot mutate an already evaluated child.
            }
        }
        try(Cursor c=db.rawQuery("SELECT structural_hash,ast FROM r952_candidate WHERE candidate_id=?",new String[]{parent})){
            if(!c.moveToFirst()||!parentAst.structuralHash.equals(c.getString(0))||!formula.equals(c.getString(1)))throw new IllegalStateException("R974_LINEAGE_PARENT_CAS");
        }
        try(Cursor c=db.rawQuery("SELECT structural_hash,future_n,may_create_real,created_issue,state FROM r952_candidate WHERE candidate_id=?",new String[]{child})){
            if(!c.moveToFirst()||parentAst.structuralHash.equals(c.getString(0))||c.getLong(1)!=0||c.getInt(2)!=0||!issue.equals(c.getString(3))||!"NURSERY".equals(c.getString(4)))
                throw new IllegalStateException("R974_LINEAGE_CHILD_INDEPENDENCE");
        }
        db.execSQL("INSERT INTO "+LINEAGE+" VALUES(?,?,?,?,?,?)",new Object[]{child,parent,role,formula,evidence,issue});
        try(Cursor c=db.rawQuery("SELECT parent_role,parent_ast,evidence_hash,created_issue FROM "+LINEAGE+" WHERE child_id=? AND parent_id=?",new String[]{child,parent})){
            if(!c.moveToFirst()||!role.equals(c.getString(0))||!formula.equals(c.getString(1))||!evidence.equals(c.getString(2))||!issue.equals(c.getString(3)))throw new IllegalStateException("R974_LINEAGE_DRIFT");
        }
    }
    private static boolean sha(String value){return value!=null&&value.matches("[0-9a-f]{64}");}

public static void teacherLineageCP12(SQLiteDatabase db,String child,String action,EvolutionTeachersCP12.Binding binding,CandidateAstR952 ast){
        if(db==null||!db.inTransaction()||binding==null||!action.equals(binding.action)||!ast.structuralHash.matches("[0-9a-f]{64}"))throw new IllegalStateException("TEACHER_LINEAGE_TX");
        EvolutionTeachersCP12.Binding durable=EvolutionTeachersCP12.readCP12(db,action,binding.evidence);if(durable==null||!durable.proof.equals(binding.proof))throw new IllegalStateException("TEACHER_LINEAGE_BINDING_CAS");
        try(Cursor c=db.rawQuery("SELECT a.state,a.child_id,c.structural_hash FROM research_action_v2 a JOIN r952_candidate c ON c.candidate_id=a.child_id WHERE a.action_id=?",new String[]{action})){if(!c.moveToFirst()||!c.getString(0).equals("WAIT_FUTURE")||!child.equals(c.getString(1))||!ast.structuralHash.equals(c.getString(2)))throw new IllegalStateException("TEACHER_LINEAGE_CHILD_CAS");}
        if(ast.graphSpec.experts.size()!=binding.refs.size())throw new IllegalStateException("TEACHER_LINEAGE_GRAPH");
        for(int i=0;i<binding.refs.size();i++){CandidateCompilerR952.ExpertRef ref=binding.refs.get(i),actual=ast.graphSpec.experts.get(i);if(!ref.freezeHash.equals(actual.freezeHash)||!ref.modelHash.equals(actual.modelHash)||!ref.featureManifestHash.equals(actual.featureManifestHash)||ref.position!=actual.position)throw new IllegalStateException("TEACHER_LINEAGE_GRAPH");
            java.util.Map<String,Object> witness=binding.witnesses.get(i);String witnessProof=V51ChecksumEngine.sha256(StrictCanonicalJson.writeContext(witness));
            String receipt=V51ChecksumEngine.sha256("CP12_CHILD_TEACHER|"+child+'|'+action+'|'+ref.freezeHash+'|'+ref.modelHash+'|'+ref.featureManifestHash+'|'+binding.proof+'|'+witnessProof);
            try(Cursor c=db.rawQuery("SELECT receipt_hash FROM research_child_teacher_lineage_cp12 WHERE child_id=? AND teacher_freeze_hash=?",new String[]{child,ref.freezeHash})){if(c.moveToFirst()){if(!receipt.equals(c.getString(0)))throw new IllegalStateException("TEACHER_LINEAGE_REPLAY_DRIFT");continue;}}
            db.execSQL("INSERT INTO research_child_teacher_lineage_cp12(child_id,teacher_freeze_hash,teacher_model_hash,teacher_manifest_hash,teacher_candidate_id,witness_issue,action_id,binding_proof_hash,witness_proof_hash,receipt_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{child,ref.freezeHash,ref.modelHash,ref.featureManifestHash,EvolutionTeachersCP12.str(witness,"candidate_id"),EvolutionTeachersCP12.str(witness,"target"),action,binding.proof,witnessProof,receipt,System.currentTimeMillis()});
        }
    }
}
