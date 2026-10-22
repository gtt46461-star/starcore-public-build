package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Arrays;
import org.json.JSONObject;

/** Measured, predraw full-group replay proof, committed with the candidate prediction.
 * The second evaluation uses a decoded copy of the serialized program and state.
 * This proves runtime serialization parity; it is not an Android/host device benchmark.
 */
public final class CandidateExecutionAuditR932 {
    public static final String TABLE = "research_candidate_execution_r932";
    public static final String REVISION = "R932_FULL_GROUP_SERIALIZATION_REPLAY_V1";
    private CandidateExecutionAuditR932() {}

    private static long usedHeapCp4(){Runtime r=Runtime.getRuntime();return Math.max(0L,r.totalMemory()-r.freeMemory())+Math.max(0L,android.os.Debug.getNativeHeapAllocatedSize());}

    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.readyCommitted(db,"CandidateExecutionAuditR932")) return;

        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "(candidate_key TEXT NOT NULL,target_issue TEXT NOT NULL,structural_hash TEXT NOT NULL,input_hash TEXT NOT NULL,base_issue TEXT NOT NULL,prediction_hash TEXT NOT NULL,ranking_hash TEXT NOT NULL,score_hash TEXT NOT NULL,code_hash TEXT NOT NULL,schema_hash TEXT NOT NULL,atom_graph_hash TEXT NOT NULL,action_count INTEGER NOT NULL,max_error REAL NOT NULL,elapsed_ns INTEGER NOT NULL,payload TEXT NOT NULL,proof_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(candidate_key,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_execution_window ON " + TABLE + "(candidate_key,structural_hash,target_issue)");
    
        TransactionSchemaCacheR941.markCommitted(db,"CandidateExecutionAuditR932");
    }

    public static final class Builder {
        private final double[] scores = new double[1200];
        private final int[] ties = new int[1200];
        private final boolean[] seen = new boolean[1200];
        private final long started = System.nanoTime();
        private final long heapStart=usedHeapCp4();
        private long heapPeak=heapStart; private int heapSamples;
        private void sampleHeapCp4(){heapPeak=Math.max(heapPeak,usedHeapCp4());heapSamples++;}
        private int count;
        private double error;
        public void add(int ordinal, double score, double replayScore, int tieRank) {
            if (ordinal < 0 || ordinal >= 1200 || seen[ordinal])
                throw new IllegalArgumentException("R932_AUDIT_ACTION_IDENTITY");
            if (!Double.isFinite(score) || !Double.isFinite(replayScore))
                throw new IllegalArgumentException("R932_AUDIT_NONFINITE_SCORE");
            double difference = Math.abs(score - replayScore);
            if (difference > 1e-12 * Math.max(1d, Math.abs(score)))
                throw new IllegalStateException("R932_AUDIT_SERIALIZATION_PARITY");
            error = Math.max(error, difference);
            seen[ordinal] = true; scores[ordinal] = score; ties[ordinal] = tieRank; count++; if((count&31)==0)sampleHeapCp4();
        }
        public Proof finish(int selected, String programBefore, String programAfter,
                String stateBefore, String stateAfter, String replayStateAfter) {
            if (count != 1200) throw new IllegalStateException("R932_AUDIT_FULL1200_REQUIRED");
            if (!programBefore.equals(programAfter) || !stateBefore.equals(stateAfter)
                    || !stateBefore.equals(replayStateAfter))
                throw new IllegalStateException("R932_AUDIT_PREDRAW_MUTATION");
            Integer[] order = new Integer[1200];
            StringBuilder values = new StringBuilder(30000);
            for (int i = 0; i < 1200; i++) {
                order[i] = i;
                values.append(i).append(':').append(Double.toHexString(scores[i])).append(';');
            }
            Arrays.sort(order, (a, b) -> {
                int compare = scores[b]==scores[a]?0:Double.compare(scores[b], scores[a]);
                if (compare == 0) compare = Integer.compare(ties[a], ties[b]);
                return compare == 0 ? Integer.compare(a, b) : compare;
            });
            if (selected != order[0]) throw new IllegalStateException("R932_AUDIT_SELECTED_PARITY");
            StringBuilder ranks = new StringBuilder(6000);
            for (int ordinal : order) ranks.append(ordinal).append(',');
            sampleHeapCp4();
            return new Proof(selected, V51ChecksumEngine.sha256(ranks.toString()),
                    V51ChecksumEngine.sha256(values.toString()),
                    V51ChecksumEngine.sha256(programBefore), V51ChecksumEngine.sha256(stateBefore),
                    error, Math.max(1L, System.nanoTime() - started),Math.max(0L,heapPeak-heapStart),heapSamples);
        }
    }

    public static final class Proof {
        public final int selectedOrdinal;
        public final String rankingHash, scoreHash, programHash, stateHash;
        public final double maxError;
        public final long elapsedNs,heapIncrementCp4; public final int heapSamplesCp4;
        private Proof(int selected, String ranking, String score, String program,
                String state, double error, long elapsed,long heapIncrement,int heapSamples) {
            selectedOrdinal=selected; rankingHash=ranking; scoreHash=score;
            programHash=program; stateHash=state; maxError=error; elapsedNs=elapsed;heapIncrementCp4=heapIncrement;heapSamplesCp4=heapSamples;
        }
        public void freeze(SQLiteDatabase db, String key, String structure, String input,
                String base, String target, String prediction) {
            if (!db.inTransaction() || !TargetIssueResolver.exact(base,target).valid
                    || !hash(structure) || !hash(input) || !hash(prediction))
                throw new IllegalArgumentException("R932_AUDIT_PIT_OR_TRANSACTION");
            schema(db);
            try (Cursor c=db.rawQuery("SELECT 1 FROM " + DbHelper.TABLE_DRAWS
                    + " WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{target})) {
                if(c.moveToFirst())throw new IllegalStateException("R932_AUDIT_POSTDRAW_REJECTED");
            }
            String atom=AtomCaptureR932.graphHash(db), code=SourceManifestR815.SOURCE_TREE_SHA256,
                    schema=V51ActionMetaFeatureEngine.featureManifestHash();
            try {
                JSONObject value=new JSONObject(); value.put("revision",REVISION);
                value.put("candidate_key",key); value.put("structural_hash",structure);
                value.put("input_hash",input); value.put("base_issue",base); value.put("target_issue",target);
                value.put("prediction_hash",prediction); value.put("selected_ordinal",selectedOrdinal);
                value.put("ranking_hash",rankingHash); value.put("score_hash",scoreHash);
                value.put("program_hash",programHash); value.put("state_hash",stateHash);
                value.put("model_bundle_hash",ModelBundleRuntimeR932.current().hash);
                value.put("code_hash",code); value.put("schema_hash",schema); value.put("atom_graph_hash",atom);
                value.put("build_source_hash",code);
                value.put("effective_numeric_hash",CalibrationRuntimeIdentityF04.numericHash());
                value.put("execution_scope_hash",CalibrationRuntimeIdentityF04.executionScope(key,structure,ModelBundleRuntimeR932.current().hash));
                value.put("semantic_revision",CalibrationRuntimeIdentityF04.REVISION);
                if(key.startsWith("algorithm:"+FrozenCandidateAdapterR958.PREFIX))
                    value.put("exact_scope_policy_hash",CalibrationRuntimeIdentityF04.exactScope(key.substring("algorithm:".length()),structure,stateHash,SelectionEvidenceContractR929.installedPolicy().hash));
                value.put("action_count",1200); value.put("max_error",maxError);
                value.put("parity_scope","FULL1200_SERIALIZED_PROGRAM_STATE_REEXECUTION");
                value.put("may_authorize_real",false);
                String payload=value.toString(), proof=V51ChecksumEngine.sha256(payload);
                db.execSQL("INSERT OR IGNORE INTO " + TABLE + " VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{key,target,structure,input,base,prediction,rankingHash,scoreHash,
                            code,schema,atom,1200,maxError,elapsedNs,payload,proof,System.currentTimeMillis()});
                if(ResearchGatePlanR967.installed().isCp3)CandidateResourceEvidenceCP4.freeze(db,key,target,prediction,proof,selectedOrdinal,elapsedNs,heapIncrementCp4,heapSamplesCp4);
                try(Cursor c=db.rawQuery("SELECT proof_hash FROM " + TABLE
                        + " WHERE candidate_key=? AND target_issue=?",new String[]{key,target})) {
                    if(!c.moveToFirst() || !proof.equals(c.getString(0)))
                        throw new IllegalStateException("R932_AUDIT_FROZEN_CONFLICT");
                }
            } catch(org.json.JSONException e) { throw new IllegalStateException("R932_AUDIT_JSON",e); }
        }
    }
    /** Read-only qualification of a stored algorithm freeze. Row counts are not proof.
     * This deliberately validates the original payload against its own source/schema identity:
     * upgrading the APK does not rewrite historical evidence to the new source hash.
     */
    static String verifyAlgorithm(SQLiteDatabase db, String id, String target) {
        final String key = "algorithm:" + id;
        try (Cursor c = db.rawQuery("SELECT p.record_hash,p.feature_blob,p.feature_hash,p.position,p.n1,p.n2,p.n3,p.selected_ordinal,p.score,"
                + "r.record_hash,r.formula_hash,r.input_hash,r.position,r.n1,r.n2,r.n3,r.selected_ordinal,r.phase,"
                + "e.structural_hash,e.input_hash,e.base_issue,e.prediction_hash,e.ranking_hash,e.score_hash,e.code_hash,e.schema_hash,e.atom_graph_hash,e.action_count,e.max_error,e.payload,e.proof_hash,"
                + "o.frozen,o.full1200,o.record_hash,g.structural_hash "
                + "FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p "
                + "JOIN " + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g ON g.genome_id=p.genome_id "
                + "LEFT JOIN " + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " r ON r.candidate_id=p.genome_id AND r.target_issue=p.target_issue "
                + "LEFT JOIN " + TABLE + " e ON e.candidate_key=? AND e.target_issue=p.target_issue "
                + "LEFT JOIN " + ResearchCaptureLedgerR932.TABLE + " o ON o.candidate_key=? AND o.target_issue=p.target_issue "
                + "WHERE p.genome_id=? AND p.target_issue=?", new String[]{key,key,id,target})) {
            if (!c.moveToFirst()) return "PREDICTION_MISSING";
            if (c.isNull(9)) return "MIRROR_MISSING";
            if (c.isNull(18)) return "EXECUTION_PROOF_MISSING";
            if (c.isNull(31)) return "CAPTURE_TERMINAL_MISSING";
            String prediction=c.getString(0), structure=c.getString(18), payload=c.getString(29);
            if (!hash(prediction) || !prediction.equals(c.getString(9))
                    || !prediction.equals(c.getString(21)) || !prediction.equals(c.getString(33)))
                return "PREDICTION_HASH_JOIN_MISMATCH";
            if (!structure.equals(c.getString(10)) || !structure.equals(c.getString(34)))
                return "STRUCTURE_JOIN_MISMATCH";
            if (!V51ChecksumEngine.sha256("R908_ALG_FEATURE|"+c.getString(1)).equals(c.getString(2))
                    || !c.getString(2).equals(c.getString(11))) return "FROZEN_FEATURE_HASH_MISMATCH";
            for (int i=3;i<=7;i++) if(c.getInt(i)!=c.getInt(i+9)) return "MIRROR_ACTION_MISMATCH";
            if (!"R908_INVENTED_ALGORITHM_FUTURE_OOS".equals(c.getString(17))) return "MIRROR_PHASE_MISMATCH";
            if (!AppConfig.isValidPosition(c.getInt(3)) || !DecisionGateEngine.b12K3(c.getInt(4),c.getInt(5),c.getInt(6))
                    || K3ActionSpaceEngine.globalOrdinal(c.getInt(3),c.getInt(4),c.getInt(5),c.getInt(6))!=c.getInt(7)
                    || !Double.isFinite(c.getDouble(8))) return "FROZEN_ACTION_INVALID";
            if (c.getInt(27)!=1200 || !Double.isFinite(c.getDouble(28)) || c.getDouble(28)<0
                    || c.getInt(31)!=1 || c.getInt(32)!=1) return "FULL1200_TERMINAL_INVALID";
            if (payload==null || payload.length()>262144 || !V51ChecksumEngine.sha256(payload).equals(c.getString(30)))
                return "PROOF_PAYLOAD_HASH_MISMATCH";
            try {
                JSONObject proof=new JSONObject(payload);
                if (!REVISION.equals(proof.getString("revision")) || !key.equals(proof.getString("candidate_key"))
                        || !target.equals(proof.getString("target_issue")) || proof.getBoolean("may_authorize_real")
                        || proof.getInt("selected_ordinal")!=c.getInt(7) || proof.getInt("action_count")!=1200
                        || !"FULL1200_SERIALIZED_PROGRAM_STATE_REEXECUTION".equals(proof.getString("parity_scope")))
                    return "PROOF_PAYLOAD_IDENTITY_MISMATCH";
                String[] fields={"structural_hash","input_hash","base_issue","prediction_hash","ranking_hash","score_hash","code_hash","schema_hash","atom_graph_hash"};
                for(int i=0;i<fields.length;i++) if(!c.getString(18+i).equals(proof.getString(fields[i]))) return "PROOF_COLUMN_MISMATCH_"+fields[i];
                if(!TargetIssueResolver.exact(c.getString(20),target).valid
                        || !hash(proof.getString("program_hash")) || !hash(proof.getString("state_hash"))
                        || Double.compare(proof.getDouble("max_error"),c.getDouble(28))!=0)
                    return "PROOF_PIT_OR_STATE_INVALID";
            } catch (org.json.JSONException error) { return "PROOF_JSON_INVALID"; }
            try(Cursor event=db.rawQuery("SELECT 1 FROM "+ResearchCaptureLedgerR932.EVENT
                    +" WHERE candidate_key=? AND target_issue=? AND status='FROZEN' AND record_hash=? LIMIT 1",
                    new String[]{key,target,prediction})) {
                if(!event.moveToFirst())return "FROZEN_EVENT_MISSING";
            }
            return "VALID";
        }
    }

    /** Labeling an old freeze still requires its original time boundary and a
     * confirmed exact draw. No execution proof or timestamp is repaired here. */
    static String verifyAlgorithmSettlementR79(SQLiteDatabase db,String id,Models.Draw draw) {
        if(draw==null||!draw.isValid()||draw.dirty!=0||draw.openTime<=0
                ||draw.openTime>System.currentTimeMillis()
                ||!DrawCanonicalizerR811.isConfirmedWithinRead(db,draw))return "AUTHORITATIVE_DRAW_REQUIRED";
        try(Cursor d=db.rawQuery("SELECT d.open_time,d.dirty,EXISTS(SELECT 1 FROM draws hot WHERE hot.issue=d.issue)"
                +" FROM "+DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=?",new String[]{draw.issue})) {
            if(!d.moveToFirst()||d.getLong(0)!=draw.openTime||d.getInt(1)!=0)return "EXACT_DRAW_CLOCK_REQUIRED";
            if(d.getInt(2)==0&&DrawDependencyInputCP12.verifiedOpenTimeR77(db,draw.issue)!=draw.openTime)
                return "RETAINED_DRAW_PROOF_REQUIRED";
        }
        String original=verifyAlgorithm(db,id,draw.issue);if(!"VALID".equals(original))return original;
        String key="algorithm:"+id;
        try(Cursor c=db.rawQuery("SELECT p.frozen_at,r.frozen_at,e.frozen_at FROM "
                +DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" p JOIN "+DbHelper.TABLE_R817_RESEARCH_PREDRAW
                +" r ON r.candidate_id=p.genome_id AND r.target_issue=p.target_issue JOIN "+TABLE
                +" e ON e.candidate_key=? AND e.target_issue=p.target_issue WHERE p.genome_id=? AND p.target_issue=?",
                new String[]{key,id,draw.issue})) {
            if(!c.moveToFirst())return "FROZEN_CLOCK_MISSING";
            for(int i=0;i<3;i++)if(c.getLong(i)<=0||c.getLong(i)>=draw.openTime)return "STRICT_PREDRAW_CLOCK_REQUIRED";
        }
        try(Cursor c=db.rawQuery("SELECT MIN(at_ms) FROM "+ResearchCaptureLedgerR932.EVENT
                +" WHERE candidate_key=? AND target_issue=? AND status='FROZEN'"
                +" AND record_hash=(SELECT record_hash FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                +" WHERE genome_id=? AND target_issue=?)",new String[]{key,draw.issue,id,draw.issue})) {
            if(!c.moveToFirst()||c.isNull(0)||c.getLong(0)<=0||c.getLong(0)>=draw.openTime)return "STRICT_CAPTURE_EVENT_CLOCK_REQUIRED";
        }
        return "VALID";
    }

    private static boolean hash(String value) {return value!=null&&value.matches("[0-9a-f]{64}");}
}
