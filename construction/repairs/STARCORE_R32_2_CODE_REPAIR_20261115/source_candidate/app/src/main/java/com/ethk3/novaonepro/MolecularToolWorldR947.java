package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import com.ethk3.novaonepro.r952.AtomDiscoveryEngineR952;
import com.ethk3.novaonepro.r952.MoleculeCompilerR952;
import com.ethk3.novaonepro.r952.ResearchIntentLedgerR952;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;
import com.ethk3.novaonepro.r952.WorldAtomR952;
import com.ethk3.novaonepro.r952.WorldModelRepositoryR952;
import com.ethk3.novaonepro.r952.WorldMoleculeR952;

/**
 * R947 research-only bridge: mature atomic tools -> typed admissions -> molecule grammar search
 * -> Genesis creation material. It never creates a REAL action or production certificate.
 */
public final class MolecularToolWorldR947 {
    public static final String VERSION = "R947_MOLECULAR_TOOL_WORLD_V1";
    public static final String TABLE = "molecular_tool_proposal_r947";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private static final int MAX_PROPOSALS_PER_PULSE = 8;
    private MolecularToolWorldR947() {}

    /** Canonical Director IMPORT slot delegates here; no independent scheduler. */
    public static String importSeedBatchCP4(DbStore store,long deadlineNs) {
        return com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.maintain(store,deadlineNs);
    }
    /** Publishes at most 25 already compiled rows and their durable cursor in one transaction. */
    public static long importSeedBatchCP4(SQLiteDatabase db,
            List<com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.SeedRow> rows,long cursor) {
        return com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.importPrepared(db,rows,cursor);
    }

    static void schema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "(proposal_hash TEXT PRIMARY KEY,novelty_key TEXT NOT NULL,operator TEXT NOT NULL,parent_indices TEXT NOT NULL,parent_hashes TEXT NOT NULL,state TEXT NOT NULL,created_issue TEXT NOT NULL,created_at INTEGER NOT NULL,selected_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r947_molecule_state ON " + TABLE + "(state,created_issue,created_at)");
    }

    /** Low-priority proposal refresh. Quality evidence alone never authorizes production use. */
    public static String maintain(final DbStore store) {
        if (store == null) return "STORE_NULL";
        if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
            return "YIELD_P0_PRESSURE";
        String result = store.coordinatorR36().runAtomicWrite("R947_MOLECULAR_TOOL_REFRESH",
                new DatabaseCoordinator.WriteCallable<String>() {
                    @Override public String call(SQLiteDatabase db) {
                        String issue = latestIssue(db);
                        if (issue.length() == 0) return "WAIT_HISTORY";
                        int created = propose(db, issue, seed(issue), MAX_PROPOSALS_PER_PULSE);
                        int ready = scalar(db, "SELECT COUNT(1) FROM " + TABLE + " WHERE state='READY'");
                        int activeAtoms = activeAdmissions(db).size();
                        return "READY|activeTypedAtoms=" + activeAtoms + "|newMolecules=" + created
                                + "|readyMolecules=" + ready + "|productionAuthority=0";
                    }
                }, "MOLECULAR_REFRESH_COMMIT_FAILED");
        store.setState("r947_molecular_world_status", result);
        store.setState("r947_molecular_world_at", String.valueOf(System.currentTimeMillis()));
        return result;
    }

    /** Creation-time handoff. A molecule only changes a new child's research genome, never a gate. */
    static Plan creationPlan(SQLiteDatabase db, int[] fallback, long seed) {
        if (db == null || fallback == null || fallback.length < GenesisAssetGenomeR915.MIN_ATOMS)
            throw new IllegalArgumentException("R947_MOLECULE_PLAN_INPUT");
        schema(db);
        String issue = latestIssue(db);
        if (issue.length() > 0) propose(db, issue, seed, MAX_PROPOSALS_PER_PULSE);
        List<TypedAtomMoleculeGraphR930.Admission> active=activeAdmissions(db);
        Set<Integer> activeIndices=new HashSet<Integer>();for(TypedAtomMoleculeGraphR930.Admission a:active)activeIndices.add(parseToolIndex(a.node.id));
        Cursor c = null;
        try {
            c = db.rawQuery("SELECT proposal_hash,operator,parent_indices FROM " + TABLE
                    + " WHERE state='READY' ORDER BY created_issue,created_at,proposal_hash LIMIT 8", null);
            while(c.moveToNext()){
            String proposal = c.getString(0), operator = c.getString(1), parentCsv = c.getString(2);
            boolean admitted=true;for(int parent:parse(parentCsv))admitted&=activeIndices.contains(parent);
            if(!admitted){db.execSQL("UPDATE "+TABLE+" SET state='REJECTED_PARENT_ADMISSION' WHERE proposal_hash=? AND state='READY'",new Object[]{proposal});continue;}
            int[] ranked = AtomicToolWorldR943.creationAtoms(db, fallback);
            ArrayList<Integer> selected = new ArrayList<Integer>();
            for (int index : parse(parentCsv)) addUniqueProduction(selected, index);
            for (int index : ranked) { if (selected.size() >= 12) break; addUniqueProduction(selected, index); }
            for (int index : fallback) { if (selected.size() >= 12) break; addUniqueProduction(selected, index); }
            if (selected.size() < GenesisAssetGenomeR915.MIN_ATOMS)
                return Plan.fallback(fallback, "MOLECULE_PARENT_SET_TOO_SMALL");
            int[] out = new int[Math.min(12, selected.size())];
            for (int i = 0; i < out.length; i++) out[i] = selected.get(i).intValue();
            Arrays.sort(out);
            Executable executable=new Executable(operator,parse(parentCsv));
            // Old READY proposals acquire no authority from a name or hash: compile/register the
            // actual admitted parents before consuming the executable in a new genome.
            registerWorld(db,issue,executable,active);
            WorldModelRepositoryR952.require(db,"WORLD-"+executable.hash);
            db.execSQL("UPDATE " + TABLE + " SET state='SELECTED_FOR_GENESIS',selected_at=? WHERE proposal_hash=? AND state='READY'",
                    new Object[]{System.currentTimeMillis(), proposal});
            return new Plan(out, proposal, operator, "MOLECULE_EXECUTABLE_TYPED_INPUT",
                    executable);
            }
            return Plan.fallback(fallback,"ATOMIC_ONLY_NO_MATURE_MOLECULE");
        } finally { if (c != null) c.close(); }
    }

    private static int propose(SQLiteDatabase db, String issue, long seed, int maximum) {
        schema(db);
        ResearchMigrationRegistryR952.ensure(db);
        ResearchMultiplicityR932.schema(db);
        List<TypedAtomMoleculeGraphR930.Admission> admissions = activeAdmissions(db);
        // Atom registration is independently useful. Binary operators need two parents;
        // requiring three here prevented the report's two mature atoms from entering the world.
        if(!admissions.isEmpty()){
            String intent=ResearchIntentLedgerR952.put(db,"OBSERVED_ATOM_ADMISSION","",
                    "MATURE_FUTURE_UTILITY","INDEPENDENT_MOLECULE_EVALUATION","NORMALIZED_ATOMS",1,issue);
            for(TypedAtomMoleculeGraphR930.Admission admission:admissions){
                ResearchWorkBudgetR927.checkpoint();
                WorldAtomR952 atom=AtomDiscoveryEngineR952.propose("NORMALIZED_ATOM_"+parseToolIndex(admission.node.id),admission);
                WorldModelRepositoryR952.putAtom(db,atom);
                WorldModelRepositoryR952.recordAdmission(db,atom,intent,admission,issue);
            }
        }
        if (admissions.size() < 2) return 0;
        final Map<String,Integer> parentIndex = new HashMap<String,Integer>();
        for (TypedAtomMoleculeGraphR930.Admission a : admissions)
            parentIndex.put(a.node.structuralHash, Integer.valueOf(parseToolIndex(a.node.id)));
        Set<String> structural = new HashSet<String>(), novelty = new HashSet<String>();
        try (Cursor c = db.rawQuery("SELECT proposal_hash,novelty_key FROM " + TABLE, null)) {
            while (c.moveToNext()) { structural.add(c.getString(0)); novelty.add(c.getString(1)); }
        }
        List<AblationRepairCreationR930.SearchProposal> proposals =
                AblationRepairCreationR930.createNovelStructures("GENESIS_ATOMIC_TOOL", admissions,
                        structural, novelty, Math.max(1, Math.min(64, maximum)), seed);
        int inserted = 0;
        for (AblationRepairCreationR930.SearchProposal p : proposals) {
            ArrayList<Integer> indices = new ArrayList<Integer>();
            boolean valid = true;
            for (String parent : p.parentNodeHashes) {
                Integer index = parentIndex.get(parent);
                if (index == null) { valid = false; break; }
                indices.add(index);
            }
            if (!valid || indices.size() < 2) continue;
            String csv = csv(indices);
            String parents = join(p.parentNodeHashes);
            registerWorld(db,issue,new Executable(p.operator.name(),parse(csv)),admissions);
            db.execSQL("INSERT OR IGNORE INTO " + TABLE + " VALUES(?,?,?,?,?,'READY',?,?,0)",
                    new Object[]{p.structuralHash,p.noveltyKey,p.operator.name(),csv,parents,issue,System.currentTimeMillis()});
            if (changed(db)) inserted++;
        }
        return inserted;
    }

    private static void registerWorld(SQLiteDatabase db,String issue,Executable executable,
            List<TypedAtomMoleculeGraphR930.Admission> admissions){
        ResearchMigrationRegistryR952.ensure(db);ResearchMultiplicityR932.schema(db);
        String id="WORLD-"+executable.hash;
        String intent=ResearchIntentLedgerR952.put(db,"MOLECULAR_RESIDUAL_SEARCH",executable.canonical,
                "UNTESTED_NEW_COMPOSITION","INDEPENDENT_CAL_AND_FUTURE_BRIER",executable.output.operator.name(),MAX_PROPOSALS_PER_PULSE,issue);
        List<String> parents=new ArrayList<String>();
        for(int index:executable.parents()){
            TypedAtomMoleculeGraphR930.Admission observed=null;
            for(TypedAtomMoleculeGraphR930.Admission a:admissions)if(parseToolIndex(a.node.id)==index){observed=a;break;}
            if(observed==null)throw new IllegalStateException("R958_WORLD_PARENT_ADMISSION_NO_LONGER_ACTIVE");
            WorldAtomR952 atom=AtomDiscoveryEngineR952.propose("NORMALIZED_ATOM_"+index,observed);
            WorldModelRepositoryR952.putAtom(db,atom);
            WorldModelRepositoryR952.recordAdmission(db,atom,intent,observed,issue);
            parents.add(atom.atomId);
        }
        WorldMoleculeR952 molecule=new WorldMoleculeR952(id,executable.output.operator.name(),parents,1,parents.size()+1,
                executable.hash,executable.output.parametersCopy());
        MoleculeCompilerR952.Compiled compiled=WorldModelRepositoryR952.putMolecule(db,molecule);
        ResearchMultiplicityR932.registerCandidate(db,id,"GENESIS_ATOMIC_TOOL",compiled.semanticHash,issue);
    }

    /** Convert AtomicToolWorld's mature Future-only utility evidence into new typed tool admissions. */
    private static List<TypedAtomMoleculeGraphR930.Admission> activeAdmissions(SQLiteDatabase db) {
        AtomicToolWorldR943.schema(db);
        Map<Integer,AtomicToolWorldR943.Quality> quality = AtomicToolWorldR943.qualities(db);
        AtomicToolWorldR943.NetworkState network = AtomicToolWorldR943.network(db);
        Set<Integer> quarantined = AtomCaptureR932.quarantined(db);
        ArrayList<TypedAtomMoleculeGraphR930.Admission> out = new ArrayList<TypedAtomMoleculeGraphR930.Admission>();
        for (AtomicToolWorldR943.Quality q : quality.values()) {
            if (!"RESEARCH_PARENT".equals(q.state) || quarantined.contains(Integer.valueOf(q.index))) continue;
            GenesisSchemaRegistryR915.AtomSpec atom = GenesisSchemaRegistryR915.atoms().get(q.index);
            if (!atom.productionScoreEligible()) continue;
            TypedAtomMoleculeGraphR930.ValueType type = valueType(atom.valueType);
            TypedAtomMoleculeGraphR930.NodeSpec node = new TypedAtomMoleculeGraphR930.NodeSpec(
                    TypedAtomMoleculeGraphR930.NEW_NAMESPACE,
                    String.format(java.util.Locale.US,"ATOM_TOOL_%03d",q.index),
                    "ATOMIC_TOOL:" + atom.id + ':' + atom.name,
                    TypedAtomMoleculeGraphR930.NodeKind.ATOM,
                    type, new TypedAtomMoleculeGraphR930.Shape(1,1),
                    TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW,
                    atom.valueRange, Math.max(0,atom.maxLag), 0,
                    TypedAtomMoleculeGraphR930.MissingPolicy.MASK,
                    "AtomicToolWorldR943", "PREDRAW_FROZEN|" + atom.leakagePolicy,
                    0L, Collections.<String>emptyList(), null, new double[0], false, VERSION);
            double[] mv = moments(q.counts);
            double lower = q.lower();
            double neural = Math.max(0.0d, network.ablation[Math.max(0, Math.min(6, q.index / 20 >= 7 ? 6 : q.index / 20))])
                    / Math.max(1, network.network.updates);
            double meanQuality = clamp(0.50d + Math.max(0.0d, lower) + Math.min(0.20d, neural), 0.50d, 1.0d);
            double missing = q.missing / (double)Math.max(1, q.n + q.missing);
            String sourceEvidence = qualitySource(q,network);
            String evidence = V51ChecksumEngine.sha256(sourceEvidence);
            TypedAtomMoleculeGraphR930.AtomQuality aq = new TypedAtomMoleculeGraphR930.AtomQuality(
                    node.qualifiedId(), q.n + q.missing, q.n, 0, missing, mv[0], mv[1],
                    meanQuality, 0L, 0.0d, evidence,sourceEvidence);
            TypedAtomMoleculeGraphR930.Admission admission = TypedAtomMoleculeGraphR930.admit(node, aq);
            if (admission.productionEligible && admission.state == TypedAtomMoleculeGraphR930.AdmissionState.ACTIVE)
                out.add(admission);
        }
        return out;
    }

    private static String qualitySource(AtomicToolWorldR943.Quality q,AtomicToolWorldR943.NetworkState n){
        try {
            JSONArray counts=new JSONArray(),ablation=new JSONArray();
            for(double x:q.counts)counts.put(x);for(double x:n.ablation)ablation.put(x);
            return new JSONObject().put("version","R958_ATOMIC_QUALITY_SOURCE_V1")
                .put("owner",AtomicToolWorldR943.VERSION).put("atom_index",q.index).put("n",q.n).put("missing",q.missing)
                .put("counts",counts).put("gain",q.gain).put("gain_sq",q.gainSq).put("block_n",q.blockN)
                .put("block_gain",q.blockGain).put("blocks",q.blocks).put("positive_blocks",q.positiveBlocks)
                .put("state",q.state).put("last_issue",q.last).put("network_hash",n.hash)
                .put("network_until",n.until).put("network_updates",n.network.updates).put("network_ablation",ablation).toString();
        }catch(JSONException e){throw new IllegalArgumentException("R958_ATOMIC_QUALITY_SOURCE",e);}
    }

    private static double[] moments(double[] counts) {
        double n=0,mean=0;for(int i=0;i<5;i++){double c=counts[i],x=(i+.5d)/5d;n+=c;mean+=c*x;}
        if(n<=0)return new double[]{0.5d,0.0d};mean/=n;double v=0;for(int i=0;i<5;i++){double x=(i+.5d)/5d;v+=counts[i]*(x-mean)*(x-mean);}v/=n;return new double[]{mean,v};
    }
    private static TypedAtomMoleculeGraphR930.ValueType valueType(String type){
        if("INTEGER".equals(type))return TypedAtomMoleculeGraphR930.ValueType.INTEGER;
        if("BOOLEAN".equals(type))return TypedAtomMoleculeGraphR930.ValueType.BOOLEAN;
        if("STRING".equals(type))return TypedAtomMoleculeGraphR930.ValueType.CATEGORY;
        return TypedAtomMoleculeGraphR930.ValueType.DOUBLE;
    }
    private static void addUniqueProduction(List<Integer> out,int index){
        if(index<0||index>=GenesisSchemaRegistryR915.atomCount())return;
        if(!GenesisSchemaRegistryR915.atoms().get(index).productionScoreEligible())return;
        Integer value=Integer.valueOf(index);if(!out.contains(value))out.add(value);
    }
    private static int parseToolIndex(String id){try{return Integer.parseInt(id.substring(id.lastIndexOf('_')+1));}catch(NumberFormatException e){return -1;}}
    private static int[] parse(String csv){if(csv==null||csv.length()==0)return new int[0];String[] p=csv.split(",");int[] out=new int[p.length];for(int i=0;i<p.length;i++)try{out[i]=Integer.parseInt(p[i]);}catch(NumberFormatException e){out[i]=-1;}return out;}
    private static String csv(List<Integer> values){StringBuilder b=new StringBuilder();for(Integer i:values){if(b.length()>0)b.append(',');b.append(i.intValue());}return b.toString();}
    private static String join(List<String> values){StringBuilder b=new StringBuilder();for(String v:values){if(b.length()>0)b.append(',');b.append(v);}return b.toString();}
    private static int scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static boolean changed(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){return c.moveToFirst()&&c.getInt(0)>0;}}
    private static String latestIssue(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){return c.moveToFirst()?c.getString(0):"";}}
    private static long seed(String value){String h=V51ChecksumEngine.sha256(VERSION+'|'+value);return Long.parseUnsignedLong(h.substring(0,16),16);}
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,Double.isFinite(x)?x:lo));}

    static final class Plan {
        final int[] atomIndices; final String proposalHash,operator,reason;
        final Executable executable;
        Plan(int[] atoms,String proposal,String operator,String reason,Executable executable){atomIndices=atoms;proposalHash=proposal;this.operator=operator;this.reason=reason;this.executable=executable;}
        static Plan fallback(int[] atoms,String reason){return new Plan(Arrays.copyOf(atoms,atoms.length),"","",reason,null);}
        String trace(){return proposalHash.length()==0?reason:"MOLECULE="+proposalHash+"|OP="+operator+"|"+reason;}
    }

    /** Frozen scalar grammar over the existing normalized, masked Genesis atoms. */
    public static final class Executable {
        public static final String REVISION="R958_MOL1";
        public final String canonical, hash;
        private final int[] parents;
        private final TypedAtomMoleculeGraphR930.NodeSpec output;
        private final MoleculeCompilerR952.Compiled compiled;
        public Executable(String operator,int[] indices){
            TypedAtomMoleculeGraphR930.Operator op=TypedAtomMoleculeGraphR930.Operator.valueOf(operator);
            if(!(op==TypedAtomMoleculeGraphR930.Operator.RESIDUAL
                    ||op==TypedAtomMoleculeGraphR930.Operator.ABS_DIFFERENCE
                    ||op==TypedAtomMoleculeGraphR930.Operator.SAFE_DIVIDE
                    ||op==TypedAtomMoleculeGraphR930.Operator.SIGNED_PRODUCT
                    ||op==TypedAtomMoleculeGraphR930.Operator.ORTHOGONAL_RESIDUAL
                    ||op==TypedAtomMoleculeGraphR930.Operator.INTERACTION3))
                throw new IllegalArgumentException("R958_MOLECULE_OPERATOR");
            int arity=op==TypedAtomMoleculeGraphR930.Operator.INTERACTION3?3:2;
            if(indices==null||indices.length!=arity)throw new IllegalArgumentException("R958_MOLECULE_ARITY");
            parents=indices.clone();Set<Integer> seen=new HashSet<Integer>();
            List<TypedAtomMoleculeGraphR930.NodeSpec> nodes=new ArrayList<TypedAtomMoleculeGraphR930.NodeSpec>();
            List<String> parentIds=new ArrayList<String>();StringBuilder encoded=new StringBuilder(REVISION).append(':').append(op.name()).append(':');
            for(int i=0;i<parents.length;i++){
                int index=parents[i];
                if(index<0||index>=GenesisSchemaRegistryR915.atomCount()||!seen.add(index)
                        ||!GenesisSchemaRegistryR915.atoms().get(index).productionScoreEligible())
                    throw new IllegalArgumentException("R958_MOLECULE_PARENT_NOT_PREDRAW_NUMERIC");
                String id="ATOM_"+index;
                TypedAtomMoleculeGraphR930.NodeSpec node=node(id,"NORMALIZED_"+GenesisSchemaRegistryR915.atoms().get(index).id,
                        TypedAtomMoleculeGraphR930.NodeKind.ATOM,Collections.<String>emptyList(),null,new double[0]);
                nodes.add(node);parentIds.add(node.qualifiedId());if(i>0)encoded.append(',');encoded.append(index);
            }
            output=node("OUTPUT","RESEARCH_MOLECULE_INPUT",TypedAtomMoleculeGraphR930.NodeKind.MOLECULE,parentIds,op,
                    op==TypedAtomMoleculeGraphR930.Operator.ORTHOGONAL_RESIDUAL?new double[]{.5d}:new double[0]);
            nodes.add(output);
            TypedAtomMoleculeGraphR930.Graph graph=new TypedAtomMoleculeGraphR930.Graph("BINANCE1_PK10","FROZEN_PREDRAW",REVISION,nodes);
            compiled=MoleculeCompilerR952.compile(graph,output.qualifiedId());
            canonical=encoded.toString();hash=V51ChecksumEngine.sha256(canonical+'|'+graph.graphHash);
        }
        private static TypedAtomMoleculeGraphR930.NodeSpec node(String id,String semantic,
                TypedAtomMoleculeGraphR930.NodeKind kind,List<String> parents,TypedAtomMoleculeGraphR930.Operator op,double[] parameters){
            return new TypedAtomMoleculeGraphR930.NodeSpec("R958_MOLECULE",id,semantic,kind,
                    TypedAtomMoleculeGraphR930.ValueType.DOUBLE,new TypedAtomMoleculeGraphR930.Shape(1,1),
                    TypedAtomMoleculeGraphR930.Stage.FROZEN_PREDRAW,"1",0,0,
                    TypedAtomMoleculeGraphR930.MissingPolicy.REJECT,"GenesisAtomicWorldR920",
                    "NORMALIZED_MASKED|source_issue<target_issue",0L,parents,op,parameters,false,REVISION);
        }
        public int[] parents(){return parents.clone();}
        public static Executable parse(String value){
            String[] parts=value==null?new String[0]:value.split(":",-1);
            if(parts.length!=3||!REVISION.equals(parts[0]))throw new IllegalArgumentException("R958_MOLECULE_ENCODING");
            Executable e=new Executable(parts[1],MolecularToolWorldR947.parse(parts[2]));
            if(!value.equals(e.canonical))throw new IllegalArgumentException("R958_MOLECULE_NONCANONICAL");return e;
        }
        public double evaluate(GenesisAssetVmR915.PredrawContext context){
            if(context==null||!TargetIssueResolver.isValid(context.baseIssue)
                    ||TargetIssueResolver.compare(context.baseIssue,context.targetIssue)>=0)
                throw new IllegalArgumentException("R958_MOLECULE_NOT_PIT");
            double[] values=new double[parents.length];
            for(int i=0;i<parents.length;i++){
                int index=parents[i];values[i]=context.atoms[index];
                if(context.observation!=null&&(!context.observation.present[index]
                        ||!Double.isFinite(context.observation.values[index])
                        ||TargetIssueResolver.compare(context.observation.maxSourceIssue,context.targetIssue)>=0))
                    throw new IllegalArgumentException("R958_MOLECULE_MASK_OR_SOURCE");
            }
            return compiled.scalar(values);
        }
    }
}
