package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.util.*;

/** Synthetic execution/fault fixtures, never a phone/Future/effect certificate. */
public final class MolecularExecutionHarnessR958 {
    private static int checks;
    private static void check(boolean ok,String name){if(!ok)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    private static void rejects(Runnable body,String name){boolean rejected=false;try{body.run();}catch(IllegalArgumentException|IllegalStateException expected){rejected=true;}check(rejected,name);}
    private static String h(String x){return V51ChecksumEngine.sha256(x);}
    private static GenesisAssetVmR915.PredrawContext context(double left,double right){
        double[] atoms=new double[240];Arrays.fill(atoms,.45);atoms[20]=left;atoms[40]=right;
        return new GenesisAssetVmR915.PredrawContext("202609190100","202609190101",h("history"),h("surface"),240,1,1,2,3,0,atoms);
    }
    private static AlgorithmGenomeR908 kernel(){
        List<AlgorithmGenomeR908.Insn> score=new ArrayList<AlgorithmGenomeR908.Insn>(AlgorithmGenomeR908.seed(771,1,"FIXTURE").scoreProgram);
        score.add(new AlgorithmGenomeR908.Insn(AlgorithmGenomeR908.Op.LOAD_X,15,0,0,0));
        return new AlgorithmGenomeR908(1,"",score,AlgorithmGenomeR908.seed(771,1,"FIXTURE").updateProgram,15);
    }
    public static void main(String[] args)throws Exception{
        MolecularToolWorldR947.Executable residual=new MolecularToolWorldR947.Executable("RESIDUAL",new int[]{20,40});
        MolecularToolWorldR947.Executable product=new MolecularToolWorldR947.Executable("SIGNED_PRODUCT",new int[]{20,40});
        check(Math.abs(residual.evaluate(context(.8,.2))-.6)<1e-12,"ACTUAL_PARENT_SUBTRACTION");
        check(Math.abs(product.evaluate(context(.8,.2))-.16)<1e-12,"ACTUAL_PARENT_PRODUCT");
        check(MolecularToolWorldR947.Executable.parse(residual.canonical).hash.equals(residual.hash),"PROGRAM_SERIAL_REPLAY");
        rejects(()->new MolecularToolWorldR947.Executable("CONST",new int[]{20,40}),"UNKNOWN_OPERATOR_REJECT");
        rejects(()->new MolecularToolWorldR947.Executable("RESIDUAL",new int[]{0,0}),"DUPLICATE_PARENT_REJECT");
        rejects(()->new MolecularToolWorldR947.Executable("INTERACTION3",new int[]{20,40}),"ARITY_REJECT");
        int forbidden=-1;for(int i=0;i<240;i++)if(!GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible()){forbidden=i;break;}
        final int bad=forbidden;rejects(()->new MolecularToolWorldR947.Executable("RESIDUAL",new int[]{20,bad}),"AUDIT_OR_POSTDRAW_PARENT_REJECT");
        List<Integer> eligible=new ArrayList<Integer>();for(int i=0;i<240;i++)if(GenesisSchemaRegistryR915.atoms().get(i).productionScoreEligible())eligible.add(i);
        int[] atoms=new int[12];for(int i=0;i<12;i++)atoms[i]=eligible.get(i*eligible.size()/12);
        GenesisAssetGenomeR915 legacy=new GenesisAssetGenomeR915("MOLECULE_FIXTURE",1,"","FIXTURE","202609190100",GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,kernel());
        GenesisAssetGenomeR915 g=new GenesisAssetGenomeR915("MOLECULE_FIXTURE",1,"","FIXTURE","202609190100",GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,kernel(),residual);
        GenesisAssetGenomeR915 g2=new GenesisAssetGenomeR915("MOLECULE_FIXTURE",1,"","FIXTURE","202609190100",GenesisAssetGenomeR915.OutputType.SCALAR_SCORE,atoms,kernel(),product);
        check(!legacy.canonical().contains("|molecule=")&&legacy.molecule==null,"LEGACY_FORMAT_UNCHANGED");
        System.out.println("LEGACY_ID="+legacy.structuralHash+"|BEHAVIOR="+legacy.behaviorHash);
        check(!g.structuralHash.equals(g2.structuralHash)&&!g.behaviorHash.equals(g2.behaviorHash),"OPERATOR_ALTERS_STRUCTURE_AND_EXECUTED_BEHAVIOR");
        GenesisAssetGenomeR915 replay=GenesisAssetGenomeR915.parse(g.canonical());
        check(replay.structuralHash.equals(g.structuralHash)&&replay.behaviorHash.equals(g.behaviorHash),"GENOME_RELOAD_EXACT");
        GenesisAssetVmR915.State state=new GenesisAssetVmR915.State();String before=state.hash();
        double score=GenesisAssetVmR915.evaluatePrimary(g,state,context(.8,.2));
        check(Double.doubleToLongBits(score)==Double.doubleToLongBits(GenesisAssetVmR915.evaluatePrimary(replay,state,context(.8,.2))),"RELOADED_VM_SCORE_EXACT");
        check(Math.abs(score-GenesisAssetVmR915.evaluatePrimary(g2,state,context(.8,.2)))>1e-8,"MOLECULE_REACHES_ACTUAL_ASSET_SCORE");
        check(state.hash().equals(before),"MOLECULAR_PREDRAW_STATE_READ_ONLY");
        check(GenesisAssetVmR915.settle(g,state,context(.8,.2),"202609190101",1,1,score).updated&&!state.hash().equals(before),"MOLECULAR_SETTLEMENT_LEARNS");
        GenesisAssetGenomeR915 child=g.mutate(312,2,"MECHANISM_TEST");
        check(child.molecule!=null&&child.molecule.hash.equals(g.molecule.hash),"CHILD_PRESERVES_MOLECULE_CLOSURE");
        for(int parent:residual.parents())check(Arrays.binarySearch(child.atomIndices,parent)>=0,"CHILD_PARENT_"+parent);
        AtomCaptureR932.Values mask=new AtomCaptureR932.Values(240,"202609190100");Arrays.fill(mask.present,true);Arrays.fill(mask.values,.4);mask.present[20]=false;
        GenesisAssetVmR915.PredrawContext missing=new GenesisAssetVmR915.PredrawContext("202609190100","202609190101",h("h"),h("s"),240,1,1,2,3,0,mask.values,mask);
        rejects(()->GenesisAssetVmR915.evaluatePrimary(g,new GenesisAssetVmR915.State(),missing),"MASKED_PARENT_BLOCKS_VM");
        TypedAtomMoleculeGraphR930.Value a=TypedAtomMoleculeGraphR930.Value.present(new double[]{.5},"202609190100",1000,2,h("source"));
        TypedAtomMoleculeGraphR930.Value b=TypedAtomMoleculeGraphR930.Value.present(new double[]{.5},"202609190100",1000,500,h("source"));
        check(a.valueHash.equals(b.valueHash)&&a.latencyNs!=b.latencyNs,"REPLAY_HASH_INDEPENDENT_OF_MEASURED_COST");
        check(CausalValidationEngineR952.evaluate("H",500,.4,"not measured",0).status==CausalEdgeR952.Status.CORRELATIONAL,"POSITIVE_AGGREGATES_NEVER_CAUSAL_VALIDATED");
        rejects(()->CausalValidationEngineR952.evaluate("H",5,Double.NaN,"x",0),"NAN_CAUSAL_GAIN_REJECT");
        rejects(()->new CausalEdgeR952("a","b","MECHANISM","PAST_TO_FUTURE",h("h"),h("e"),1,CausalEdgeR952.Status.VALIDATED),"HASH_ALONE_CANNOT_AUTHORIZE_CAUSAL_VALIDATION");
        List<List<AblationRepairCreationR930.CounterfactualRow>> windows=new ArrayList<List<AblationRepairCreationR930.CounterfactualRow>>();
        AblationRepairCreationR930.CandidateDefinition candidate=new AblationRepairCreationR930.CandidateDefinition("TEST","TEST",h("graph"),Collections.singletonList(h("node")),"out","FROZEN_PREDRAW",2,1000,"","SYNTHETIC_MECHANISM");
        AblationRepairCreationR930.Intervention intervention=new AblationRepairCreationR930.Intervention(AblationRepairCreationR930.InterventionKind.SET_ZERO,"NOISE","",0);
        for(int i=0;i<5;i++){
            String target="20260919010"+(i+1),base="20260919010"+i;
            AblationRepairCreationR930.IssueCase issue=new AblationRepairCreationR930.IssueCase(target,base,1000,h("input"+i),h("label"+i),true,true,"TEST");
            AblationRepairCreationR930.CounterfactualRow row=AblationRepairCreationR930.counterfactual(candidate,issue,intervention,h("confound"),(c,x,disabled,interventions)->{
                double noise=interventions.isEmpty()?-.3:0;double predicted=.8+noise;
                return new AblationRepairCreationR930.EvaluationOutput(x.targetIssue,c.structuralHash,h("fixed_action"),predicted,predicted,true,true,10,x.inputHash,h(x.inputHash+"|CALC_NOISE="+noise));
            });windows.add(Collections.singletonList(row));
        }
        check(CausalValidationEngineR952.evaluateMeasured(h("preregistered"),windows).status==CausalEdgeR952.Status.INTERVENTION_SUPPORTED,"MEASURED_REEXECUTION_SEPARATE_FROM_MECHANISM_PROOF");
        List<List<AblationRepairCreationR930.CounterfactualRow>> overlap=new ArrayList<List<AblationRepairCreationR930.CounterfactualRow>>(windows);overlap.add(windows.get(0));
        rejects(()->CausalValidationEngineR952.evaluateMeasured(h("preregistered"),overlap),"OVERLAPPED_INTERVENTION_WINDOWS_REJECT");
        MoleculeGraphR952 graph=new MoleculeGraphR952();graph.add(new WorldMoleculeR952("a","MEAN",Arrays.asList("b","raw"),1,2,h("s")));
        rejects(()->graph.add(new WorldMoleculeR952("a","MEAN",Arrays.asList("raw","raw2"),1,2,h("s"))),"GRAPH_ID_REPLACEMENT_REJECT");
        rejects(()->graph.add(new WorldMoleculeR952("b","MEAN",Arrays.asList("a","raw"),1,2,h("s"))),"REJECTED_EDIT_PRESERVES_ORIGINAL_CYCLE_DETECTION");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            DbStore store=new DbStore(db);db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_APP_STATE+"(state_key TEXT PRIMARY KEY,state_value TEXT)");
            ResearchMigrationRegistryR952.ensure(db);
            db.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY)");
            MatrixSnapshotBusR952.publish(store,"202609190101",h("feature"),h("payload"));
            rejects(()->MatrixSnapshotBusR952.publish(store,"202609190101",h("feature"),h("conflicting")),"MATRIX_DRIFT_PROPAGATES_FAILED_COMMIT");
            db.execSQL("CREATE TRIGGER fail_matrix BEFORE INSERT ON r952_matrix_snapshot BEGIN SELECT RAISE(ABORT,'INJECTED_MATRIX_FAILURE'); END");
            rejects(()->MatrixSnapshotBusR952.publish(store,"202609190102",h("feature"),h("new")),"MATRIX_SQL_FAILURE_NO_FALSE_SUCCESS");
        }finally{db.shutdown();}
        System.out.println("R958_MOLECULAR_EXECUTION_PASS|checks="+checks+"|scope=SYNTHETIC_PRODUCTION_METHODS_NOT_LIVE_NOT_DEVICE");
    }
}
