package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Research-only crossover of measured eligible parents; every child starts with new evidence. */
public final class ResearchFusionR964 {
    private static final String USED="r964_fusion_pair_";
    private ResearchFusionR964(){}
    static final class Parent {
        final AlgorithmGenomeR908 genome;final String evidence;final int labels;
        Parent(String program,String evidence,int labels){genome=AlgorithmGenomeR908.parse(program);this.evidence=evidence;this.labels=labels;}
    }
    static final class Plan {
        final Parent left,right;final String pair;
        Plan(Parent a,Parent b){left=a;right=b;pair=V51ChecksumEngine.sha256(a.genome.structuralHash+'|'+(b==null?"SOLO":b.genome.structuralHash));}
        String kind(){return right==null?"ELITE_IMPROVEMENT":"ELITE_FUSION";}
        String module(){return right==null?"ELITE_STABILITY_IMPROVEMENT":"ELITE_PROGRAM_CROSSOVER";}
        String evidence(){return V51ChecksumEngine.sha256(left.evidence+'|'+(right==null?"SOLO":right.evidence));}
        AlgorithmGenomeR908 create(long seed){
            if(right==null)return left.genome.mutateForIntent(seed,left.genome.generation+1,AlgorithmGenomeR908.MutationIntent.BLOCK_STABILITY);
            AlgorithmGenomeR908 child=left.genome.crossover(right.genome,seed,Math.max(left.genome.generation,right.genome.generation)+1);
            // Crossover's novelty fallback may use a transient child as parent. Persist actual origins.
            return new AlgorithmGenomeR908(child.generation,left.genome.genomeId+","+right.genome.genomeId,
                    child.scoreProgram,child.updateProgram,child.scoreOutputReg);
        }
        void describe(JSONObject p)throws JSONException{
            if(right==null){p.put("improvement_parent",left.genome.genomeId);p.put("improvement_parent_structure",left.genome.structuralHash);p.put("improvement_parent_evidence",left.evidence);p.put("improvement_parent_labels",left.labels);p.put("minimum_new_labels_between_improvements",24);p.put("scope","ELITE_PROGRAM_MUTATION_FUTURE_N_ZERO_PARENT_RETAINED");return;}
            p.put("fusion_pair",pair);p.put("fusion_parent_a",left.genome.genomeId);p.put("fusion_parent_b",right.genome.genomeId);
            p.put("fusion_structure_a",left.genome.structuralHash);p.put("fusion_structure_b",right.genome.structuralHash);
            p.put("fusion_evidence_a",left.evidence);p.put("fusion_evidence_b",right.evidence);
            p.put("fusion_scope","PROGRAM_CROSSOVER_NO_PARAMETER_OR_FUTURE_INHERITANCE");
        }
        void committed(SQLiteDatabase db,String child){
            if(right==null){db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{"r966_elite_improvement_n_"+left.genome.genomeId,String.valueOf(left.labels),System.currentTimeMillis()});return;}
            db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{USED+pair,child,System.currentTimeMillis()});
        }
    }
    static Plan choose(SQLiteDatabase db,String latest){
        ArrayList<Parent> parents=new ArrayList<Parent>();
        try(Cursor c=db.rawQuery("SELECT g.genome_spec,e.evidence_hash,g.future_n FROM "+ResearchLifecycleR932.ELITE+" e JOIN "+ResearchLifecycleR932.TABLE
                +" l ON l.candidate_key=e.candidate_key AND l.structural_hash=e.structural_hash AND l.evidence_hash=e.evidence_hash JOIN "
                +DbHelper.TABLE_R908_ALGORITHM_GENOME+" g ON e.candidate_key='algorithm:'||g.genome_id AND g.structural_hash=e.structural_hash"
                +" WHERE l.last_issue=? AND l.state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED','PAUSED','NURSERY','IDEA')"
                +" AND e.future_n>=30 AND e.hit_rate>=? AND e.brier<=0.24 AND e.ece<=0.08 AND e.paired_delta>0"
                +" AND g.status NOT LIKE 'PRUNED%' AND g.status NOT LIKE 'QUARANTINED%' ORDER BY e.candidate_key LIMIT 32",
                new String[]{latest,String.valueOf(CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE)})){
            while(c.moveToNext())parents.add(new Parent(c.getString(0),c.getString(1),c.getInt(2)));
        }
        if(parents.size()<2)return improvement(db,parents);
        ArrayList<Plan> pairs=new ArrayList<Plan>();ArrayList<String> keys=new ArrayList<String>();StringBuilder marks=new StringBuilder();
        for(int a=0;a<parents.size();a++)for(int b=a+1;b<parents.size();b++){
            ResearchWorkBudgetR927.checkpoint();Plan p=new Plan(parents.get(a),parents.get(b));pairs.add(p);keys.add(USED+p.pair);
            if(marks.length()>0)marks.append(',');marks.append('?');
        }
        HashSet<String> used=new HashSet<String>();
        // At most 496 current pairs, below SQLite's legacy 999 bind-parameter limit.
        try(Cursor c=db.rawQuery("SELECT state_key FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key IN ("+marks+")",keys.toArray(new String[0]))){
            while(c.moveToNext())used.add(c.getString(0));
        }
        for(Plan p:pairs)if(!used.contains(USED+p.pair))return p;
        return improvement(db,parents);
    }
    private static Plan improvement(SQLiteDatabase db,List<Parent> parents){
        for(Parent p:parents){
            int previous=-1;
            try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{"r966_elite_improvement_n_"+p.genome.genomeId})){if(c.moveToFirst())previous=Integer.parseInt(c.getString(0));}
            if(previous<0||p.labels-previous>=24)return new Plan(p,null);
        }
        return null;
    }
}
