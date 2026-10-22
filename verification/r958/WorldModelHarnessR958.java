package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import com.ethk3.novaonepro.TypedAtomMoleculeGraphR930.*;
import java.util.*;
import org.json.JSONObject;

/** Unmodified production VM and SQL on synthetic fixtures, never live/Future evidence. */
public final class WorldModelHarnessR958 {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    private static void rejects(Runnable work,String name){boolean rejected=false;try{work.run();}catch(IllegalArgumentException|IllegalStateException expected){rejected=true;}check(rejected,name);}
    private static String h(String text){return V51ChecksumEngine.sha256(text);}
    private static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    private static WorldAtomR952 atom(String id){return new WorldAtomR952(id,"RATE","BINANCE1_PK10","TEST_NORMALIZED","FLOAT64","source_issue<target_issue",h(id),"LOW","1");}
    private static WorldMoleculeR952 molecule(String id,String op,int depth,int cost,String... parents){return new WorldMoleculeR952(id,op,Arrays.asList(parents),depth,cost,h("semantics"));}
    private static FrozenInputs inputs(String source,long available){
        Map<String,Value> values=new HashMap<String,Value>();values.put("R958_WORLD:a",Value.present(new double[]{.8},source,available,0,h("a")));
        values.put("R958_WORLD:b",Value.present(new double[]{.2},source,available,0,h("b")));
        return new FrozenInputs("202609190101",1000,h("snapshot"),values);
    }
    public static void main(String[] args)throws Exception{
        Map<String,WorldAtomR952> atoms=new HashMap<String,WorldAtomR952>();atoms.put("a",atom("a"));atoms.put("b",atom("b"));
        WorldMoleculeR952 sum=molecule("sum","SUM",1,3,"a","b");
        MoleculeCompilerR952.Compiled simple=MoleculeCompilerR952.compile(sum,atoms,Collections.<String,WorldMoleculeR952>emptyMap());
        check(simple.evaluate(inputs("202609190100",900)).require(simple.outputId).get(0)==1d,"RESOLVED_WORLD_ACTUALLY_EXECUTES_SUM");
        rejects(()->MoleculeCompilerR952.compile(sum),"HASH_ONLY_DESCRIPTOR_CANNOT_COMPILE");
        Map<String,WorldMoleculeR952> nodes=new HashMap<String,WorldMoleculeR952>();nodes.put("sum",sum);
        WorldMoleculeR952 clip=new WorldMoleculeR952("clip","CLAMP",Arrays.asList("sum"),2,4,h("clip"),new double[]{0,.7});
        MoleculeCompilerR952.Compiled nested=MoleculeCompilerR952.compile(clip,atoms,nodes);
        check(nested.depth==2&&nested.cost==4&&nested.evaluate(inputs("202609190100",900)).require(nested.outputId).get(0)==.7,"NESTED_DAG_DEPTH_COST_AND_EXECUTION");
        WorldMoleculeR952 weighted=new WorldMoleculeR952("weighted","WEIGHTED_SUM",Arrays.asList("a","b"),1,3,h("weighted"),new double[]{.25,.75});
        MoleculeCompilerR952.Compiled weights=MoleculeCompilerR952.compile(weighted,atoms,nodes);
        check(Math.abs(weights.scalar(new double[]{.8,.2})-.35)<1e-12,"WEIGHTED_SUM_IS_NOT_WEIGHTED_MEAN");
        WorldMoleculeR952 scaled=new WorldMoleculeR952("weighted2","WEIGHTED_SUM",Arrays.asList("a","b"),1,3,h("weighted"),new double[]{.5,1.5});
        check(Math.abs(MoleculeCompilerR952.compile(scaled,atoms,nodes).scalar(new double[]{.8,.2})-.7)<1e-12,"WEIGHT_MAGNITUDE_AFFECTS_EXECUTION");
        WorldMoleculeR952 renamed=molecule("renamed","SUM",1,3,"a","b");
        check(sum.structuralHash.equals(renamed.structuralHash)&&simple.semanticHash.equals(MoleculeCompilerR952.compile(renamed,atoms,nodes).semanticHash),"RENAMING_DOES_NOT_CREATE_NEW_STRUCTURE");
        rejects(()->simple.evaluate(inputs("202609190101",900)),"CURRENT_ISSUE_ATOM_REJECT");
        rejects(()->simple.evaluate(inputs("202609190100",1001)),"POSTFREEZE_AVAILABLE_ATOM_REJECT");
        rejects(()->simple.evaluate(new FrozenInputs("202609190101",1000,h("missing"),Collections.<String,Value>emptyMap())),"MISSING_PARENT_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","SUM",1,2,"a","b"),atoms,nodes),"UNDERDECLARED_COST_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","SUM",2,3,"a","b"),atoms,nodes),"DECLARED_DEPTH_MISMATCH_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","SUM",1,101,"a","b"),atoms,nodes),"UNBOUNDED_COST_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","EXEC_SHELL",1,3,"a","b"),atoms,nodes),"ILLEGAL_OPERATOR_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","WEIGHTED_SUM",1,3,"a","b"),atoms,nodes),"MISSING_OPERATOR_PARAMETERS_REJECT");
        rejects(()->MoleculeCompilerR952.compile(molecule("bad","SUM",1,3,"a","unknown"),atoms,nodes),"UNRESOLVED_PARENT_REJECT");
        rejects(()->new WorldMoleculeR952("bad","CLAMP",Arrays.asList("a"),1,2,h("bad"),new double[]{0,Double.NaN}),"NAN_PARAMETER_REJECT");
        Map<String,WorldMoleculeR952> cycle=new HashMap<String,WorldMoleculeR952>();cycle.put("x",molecule("x","SUM",1,3,"y","a"));cycle.put("y",molecule("y","SUM",1,3,"x","b"));
        rejects(()->MoleculeCompilerR952.compile(cycle.get("x"),atoms,cycle),"INDIRECT_CYCLE_REJECT");
        Map<String,WorldAtomR952> badSchema=new HashMap<String,WorldAtomR952>(atoms);
        badSchema.put("a",new WorldAtomR952("a","RATE","BINANCE1_PK10","TEST","STRING","source_issue<target_issue",h("a"),"LOW","1"));
        rejects(()->MoleculeCompilerR952.compile(sum,badSchema,nodes),"WRONG_VALUE_SCHEMA_REJECT");
        rejects(()->new WorldAtomR952("a|b","RATE","BINANCE1_PK10","TEST","FLOAT64","source_issue<target_issue",h("a"),"LOW","1"),"AMBIGUOUS_CANONICAL_DELIMITER_REJECT");
        List<AtomObservation> repeated=new ArrayList<AtomObservation>();AtomObservation observation=new AtomObservation("a","202609190100",true,true,10,.4,.7,h("obs"));repeated.add(observation);repeated.add(observation);
        rejects(()->TypedAtomMoleculeGraphR930.summarize("a",repeated),"REPEATED_ISSUE_CANNOT_INFLATE_ATOM_SUPPORT");
        rejects(()->AtomDiscoveryEngineR952.propose("a",(Admission)null),"DISCOVERY_REQUIRES_OBSERVED_ADMISSION");
        NodeSpec inputNode=simple.graph.require("R958_WORLD:a");
        AtomQuality nonfinite=new AtomQuality(inputNode.qualifiedId(),48,48,0,0,Double.NaN,.1,.8,0,0,h("bad_quality"));
        check(!TypedAtomMoleculeGraphR930.admit(inputNode,nonfinite).productionEligible,"NAN_QUALITY_CANNOT_BYPASS_ADMISSION_COMPARISONS");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try{
            DbStore store=new DbStore(db);ResearchMigrationRegistryR952.ensure(db);
            WorldModelRepositoryR952.putAtom(store,atoms.get("a"));WorldModelRepositoryR952.putAtom(store,atoms.get("b"));
            WorldModelRepositoryR952.putMolecule(store,sum);WorldModelRepositoryR952.putMolecule(store,clip);
            check(WorldModelRepositoryR952.require(db,"clip").structuralHash.equals(nested.structuralHash),"PERSISTED_GRAPH_RECOMPILES_IDENTICALLY");
            check(WorldModelRepositoryR952.require(db,"clip").evaluate(inputs("202609190100",900)).require(nested.outputId).get(0)==.7,"RELOADED_WORLD_EXECUTION_IDENTICAL");
            WorldModelRepositoryR952.putMolecule(store,clip);check(count(db,"SELECT COUNT(*) FROM r952_world_molecule")==2,"RETRY_PERSISTS_EXACTLY_ONCE");
            rejects(()->WorldModelRepositoryR952.putMolecule(store,renamed),"RENAMED_DUPLICATE_CANNOT_REGISTER_TWO_ACTIVE_STRUCTURES");
            rejects(()->WorldModelRepositoryR952.putAtom(store,new WorldAtomR952("a","RATE","BINANCE1_PK10","DIFFERENT","FLOAT64","source_issue<target_issue",h("a"),"LOW","1")),"IMMUTABLE_ATOM_ID_REUSE_REJECT");
            rejects(()->db.execSQL("UPDATE r952_world_atom SET producer='TAMPER' WHERE atom_id='a'"),"SQL_TAMPERING_ATOM_REJECT");
            rejects(()->db.execSQL("DELETE FROM r952_world_molecule WHERE molecule_id='sum'"),"SQL_DELETION_CANNOT_RESET_LINEAGE");
            rejects(()->WorldModelRepositoryR952.putAtom(db,atom("outside")),"SECOND_WRITER_OUTSIDE_TRANSACTION_REJECT");
            db.execSQL("CREATE TRIGGER injected_world_atom BEFORE INSERT ON r952_world_atom BEGIN SELECT RAISE(ABORT,'INJECTED_DISK_FAILURE'); END");
            rejects(()->WorldModelRepositoryR952.putAtom(store,atom("failure")),"WRITE_FAILURE_PROPAGATES_NO_FALSE_SUCCESS");
            check(count(db,"SELECT COUNT(*) FROM r952_world_atom WHERE atom_id='failure'")==0,"FAILED_ATOM_WRITE_ROLLED_BACK");db.execSQL("DROP TRIGGER injected_world_atom");
            boolean committed=store.coordinatorR36().runAtomicWrite("WORLD_ROLLBACK",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase d){
                WorldModelRepositoryR952.putAtom(d,atom("rollback"));WorldModelRepositoryR952.putMolecule(d,molecule("invalid","SUM",1,3,"rollback","missing"));
            }});
            check(!committed&&count(db,"SELECT COUNT(*) FROM r952_world_atom WHERE atom_id='rollback'")==0,"PARENT_AND_PROGRAM_ATOMIC_ROLLBACK");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609190100',0)");
            AtomicToolWorldR943.schema(db);int[] fallback={20,40,60,80,100,120,140,160,180,21,41,61};
            byte[] counts=AtomicToolWorldR943.pack(new double[]{48,48,48,48,48,18,20,22,24,26});
            for(int index:fallback)if(GenesisSchemaRegistryR915.atoms().get(index).productionScoreEligible())db.execSQL("INSERT INTO "+AtomicToolWorldR943.QUALITY+" VALUES(?,?,240,0,?,72.0,26.0,0,0,2,2,'RESEARCH_PARENT','202609190099')",new Object[]{index,GenesisSchemaRegistryR915.atoms().get(index).id,counts});
            final MolecularToolWorldR947.Plan[] plan={null};
            boolean created=store.coordinatorR36().runAtomicWrite("ACTUAL_MOLECULAR_CREATION",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase d){plan[0]=MolecularToolWorldR947.creationPlan(d,fallback,123456789L);}});
            check(created&&plan[0]!=null&&plan[0].executable!=null,"REAL_CREATION_ENTRY_COMPILES_EXECUTABLE");
            String id="WORLD-"+plan[0].executable.hash;MoleculeCompilerR952.Compiled real=WorldModelRepositoryR952.require(db,id);
            check(real.depth==1&&real.cost>=3,"REAL_PROPOSAL_PARENT_DAG_RELOAD");
            check(count(db,"SELECT COUNT(*) FROM r952_knowledge WHERE kind='ATOM_ADMISSION'")>0,"OBSERVED_ADMISSION_EVIDENCE_LINKED");
            String knowledge;
            try(Cursor c=db.rawQuery("SELECT knowledge_hash FROM r952_knowledge WHERE kind='ATOM_ADMISSION' LIMIT 1",null)){c.moveToFirst();knowledge=c.getString(0);}
            final String frozenKnowledge=knowledge;
            String frozenBody=KnowledgeLedgerR952.require(db,knowledge);
            JSONObject admissionBody=new JSONObject(frozenBody);
            Admission restored=TypedAtomMoleculeGraphR930.decodeAdmission(admissionBody.getString("admission"));
            check(restored.productionEligible&&restored.quality.support==240&&restored.quality.variance>0,"STORED_ADMISSION_RECONSTRUCTS_NUMERIC_DECISION");
            JSONObject rawQuality=new JSONObject(restored.quality.sourceEvidence);
            check(rawQuality.getJSONArray("counts").length()==10&&rawQuality.getInt("n")==240
                &&h(restored.quality.sourceEvidence).equals(restored.quality.evidenceHash),"ORIGINAL_QUALITY_BINS_AND_NETWORK_REFERENCE_PRESERVED");
            JSONObject altered=new JSONObject(restored.encode());altered.put("mean",restored.quality.mean+.01);
            final String changedMean=altered.toString();
            rejects(()->TypedAtomMoleculeGraphR930.decodeAdmission(changedMean),"NUMERIC_ADMISSION_DRIFT_REJECTED");
            altered=new JSONObject(restored.encode());altered.put("quality_source",restored.quality.sourceEvidence+" ");
            final String changedSource=altered.toString();
            rejects(()->TypedAtomMoleculeGraphR930.decodeAdmission(changedSource),"ADMISSION_SOURCE_PREIMAGE_DRIFT_REJECTED");
            rejects(()->db.execSQL("UPDATE r952_knowledge SET payload='altered' WHERE knowledge_hash=?",new Object[]{frozenKnowledge}),"IMMUTABLE_KNOWLEDGE_BODY_SQL_GUARD");
            rejects(()->db.execSQL("DELETE FROM r952_knowledge WHERE knowledge_hash=?",new Object[]{frozenKnowledge}),"ADMISSION_EVIDENCE_CANNOT_BE_DELETED");
            db.execSQL("INSERT INTO r952_knowledge(knowledge_hash,target_issue,candidate_id,kind,payload_hash,created_at) VALUES('LEGACY','202601010001','old','STRICT_FUTURE',?,1)",new Object[]{h("old")});
            rejects(()->KnowledgeLedgerR952.require(db,"LEGACY"),"LEGACY_HASH_ONLY_NEVER_REPORTED_REPLAYABLE");
            int trimmed=DataRetentionGovernorR900.deletePage(db,"r952_knowledge","target_issue","202612010001",512);
            check(trimmed==1&&frozenBody.equals(KnowledgeLedgerR952.require(db,knowledge)),"RETENTION_PRESERVES_WORLD_ADMISSION_BODY");
            check(count(db,"SELECT COUNT(*) FROM r952_intent WHERE problem_type='MOLECULAR_RESIDUAL_SEARCH'")>0,"REAL_CREATION_INTENT_REGISTERED");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.ALIAS+" WHERE candidate_key='"+id+"'")==1,"REAL_COMPILED_HYPOTHESIS_REGISTERED");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==0,"DISCOVERY_DOES_NOT_FABRICATE_FUTURE_LOOKS");
            check(!MolecularToolWorldR947.MAY_AUTHORIZE_REAL,"WORLD_EXECUTION_CANNOT_GRANT_REAL");
            long hypotheses=count(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
            byte[] constant=AtomicToolWorldR943.pack(new double[]{240,0,0,0,0,120,0,0,0,0});
            db.execSQL("UPDATE "+AtomicToolWorldR943.QUALITY+" SET counts=?",new Object[]{constant});
            check(frozenBody.equals(KnowledgeLedgerR952.require(db,knowledge)),"LATER_MUTABLE_QUALITY_CANNOT_REWRITE_ADMISSION_HISTORY");
            boolean retried=store.coordinatorR36().runAtomicWrite("CONSTANT_PARENT_CREATION",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase d){plan[0]=MolecularToolWorldR947.creationPlan(d,fallback,123456789L);}});
            check(retried&&plan[0].executable==null,"ACTUAL_CONSTANT_ATOMS_NOT_GRANTED_ARTIFICIAL_VARIANCE");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==hypotheses,"INVALIDATED_PARENT_DOES_NOT_CREATE_MORE_HYPOTHESES");
        }finally{db.shutdown();}
        System.out.println("R958_WORLD_MODEL_PASS|checks="+checks+"|scope=SYNTHETIC_PRODUCTION_METHODS_NOT_LIVE_NOT_DEVICE");
    }
}
