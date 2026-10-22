package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;
import com.ethk3.novaonepro.r952.CandidateCompilerR952.*;
import java.nio.file.*;import java.util.*;
/** New test-only fixture replacing an unavailable original helper. Never production training evidence. */
public final class TrainingFixtureCP11 {
 public final CandidateAstR952 ast;
 public final DevelopmentTrainerCP9.Dataset dataset;
 public TrainingFixtureCP11()throws Exception {
  ast=CandidateAstR952.parse(Files.readString(Path.of(System.getProperty("starcore.test.ast"))));
  if(ast.graphSpec.modelKind!=ModelKind.FIXED_GRAPH)throw new IllegalArgumentException("TEST_FIXED_GRAPH_REQUIRED");
  List<DevelopmentTrainerCP9.Row> rows=new ArrayList<>();String issue="202609230001";
  for(int n=0;n<420;n++){
   if(n==300)issue=TargetIssueResolver.next(issue).targetIssue;
   double[] values=new double[10];Arrays.fill(values,1);values[n%10]=2;boolean[] present=new boolean[10];Arrays.fill(present,true);
   rows.add(new DevelopmentTrainerCP9.Row(issue,V51ChecksumEngine.sha256("EXPLICIT_SYNTHETIC_TRAINING_ROW:"+n),values,present,n%10+1));issue=TargetIssueResolver.next(issue).targetIssue;
  }
  String h=V51ChecksumEngine.sha256("EXPLICIT_SYNTHETIC_TRAINING_FIXTURE");
  Modern shape=new Modern(ModelKind.FIXED_GRAPH,ast.family,10,0,0,300,42,rows.get(419).issue,ast.graphSpec.manifest.hash,Modern.normalizerHash(Collections.emptyList(),new double[0],new double[0]),ast.graphSpec.manifest.graphHash,h,new double[0],new double[0],new double[0],new int[0],new double[0],new int[0],new int[0],new double[0],Collections.emptyList());
  dataset=new DevelopmentTrainerCP9.Dataset(shape,h,h,Collections.emptyList(),Collections.emptyList(),rows,300);
 }
}
