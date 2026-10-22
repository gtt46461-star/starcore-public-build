package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.util.*;import java.nio.file.*;
/** Explicit synthetic model and positive contrast fixture; never production OOS or certificate evidence. */
public final class FrozenAtomFixtureR982 {
 public final CandidateAstR952 ast;public final Model model;public final String freeze,key;
 public FrozenAtomFixtureR982()throws Exception {
  ast=CandidateAstR952.parse(Files.readString(Path.of(System.getProperty("starcore.test.ast"))));
  Modern shape=new Modern(ModelKind.FIXED_GRAPH,ast.family,10,0,0,300,42,"202609221440",ast.graphSpec.manifest.hash,Modern.normalizerHash(Collections.emptyList(),new double[0],new double[0]),ast.graphSpec.manifest.graphHash,V51ChecksumEngine.sha256("EXPLICIT_TEST_ONLY_RECEIPT"),new double[0],new double[0],new double[0],new int[0],new double[0],new int[0],new int[0],new double[0],Collections.emptyList());
  model=Model.fromModern(shape);freeze=FrozenCandidateAdapterR958.structure(ast,model);key="algorithm:"+FrozenCandidateAdapterR958.PREFIX+freeze.substring(0,24);
 }
 public void install(SQLiteDatabase db){
  db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,state,created_issue,may_create_real,created_at) VALUES('TEST_ONLY_PARENT','TEST_ONLY_PARENT',?,?,'VALIDATED','202609221440',0,1)",new Object[]{ast.canonical,ast.structuralHash});
  db.execSQL("INSERT INTO r958_frozen_candidate VALUES(?,?,?,?,?,?,?,?,?,1)",new Object[]{key.substring(10),"TEST_ONLY_PARENT",ast.canonical,model.payload(),freeze,model.hash(),"202609221440","202609230001",V51ChecksumEngine.sha256("EXPLICIT_TEST_ONLY_ARTIFACT")});
 }
 public void admit(SQLiteDatabase db){
  db.beginTransaction();try {
   MatrixSourceRouterCP12.registerFrozenAtomStudiesCP12(db,freeze,ast,model);
   for(TypedAtomMoleculeGraphR930.NodeSpec node:MatrixSourceRouterCP12.consumedAtomsCP12(ast)){
    AtomSpecCP4 atom;try(Cursor c=db.rawQuery("SELECT spec_hash,params_json,output_type,min_history,missing_policy,state FROM research_atom_spec_v2 WHERE atom_id=?",new String[]{node.source})){if(!c.moveToFirst())throw new AssertionError("TEST_ATOM_CATALOG_MISSING");atom=AtomSpecCP4.fromRegistry(node.source,c.getString(0),c.getString(1),c.getString(2),c.getInt(3),c.getString(4),c.getString(5));}
    String binding=MatrixSourceRouterCP12.atomBindingIdentityCP12(db,ast,ast.graphSpec.manifest,node,atom);MatrixProjectionCP12.Spec spec=MatrixSourceRouterCP12.atomStudySpecCP12(freeze,model,ast.graphSpec.manifest,atom,binding,node);MatrixProjectionCP12.State state=MatrixProjectionCP12.empty(spec);
    for(int n=61;n<=360;n++){long ord=MatrixSourceRouterCP12.issueOrdinal(String.format("20260923%04d",n));MatrixProjectionCP12.apply(state,ord,new MatrixProjectionCP12.Sample(ord,MatrixProjectionCP12.PAIRED|MatrixProjectionCP12.OBSERVED,0,0,0,7,7,.4,.3,.1),null);}
    MatrixSnapshotBusR952.CheckpointCP4 checkpoint=new MatrixSnapshotBusR952.CheckpointCP4(spec.type.name(),spec.partition,300,spec.manifest,MatrixProjectionCP12.SCHEMA_HASH,0,"202609230360",V51ChecksumEngine.sha256("EXPLICIT_SYNTHETIC_CONTRAST:"+node.id),300,MatrixProjectionCP12.encode(state));
    AtomAdmissionCP12.Assessment assessment=AtomAdmissionCP12.prepareProjectionCP12(spec,state.samplesCopy(),checkpoint);if(!assessment.validated())throw new AssertionError("TEST_CONTRAST_NOT_VALIDATED");
    if(!MatrixSnapshotBusR952.commitCP4(db,null,checkpoint,1))throw new AssertionError("TEST_CHECKPOINT_NOT_COMMITTED");AtomAdmissionCP12.commitProjectionCP12(db,assessment,checkpoint,1);
   }
   db.setTransactionSuccessful();
  }finally{db.endTransaction();}
 }
}
