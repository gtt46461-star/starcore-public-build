package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.util.*;
/** Complete registered production budget; synthetic mechanism evidence only. */
public final class FullNullR30Host {
 public static void main(String[] args)throws Exception{
  Path out=Paths.get(args[1]);Files.createDirectories(out);Path state=out.resolve("checkpoint.json");NullPipelineSimulationR30 s=Files.isRegularFile(state)?NullPipelineSimulationR30.restore(Files.readString(state)):new NullPipelineSimulationR30(args[0]);if(!s.sourceCommit().equals(args[0]))throw new AssertionError("SOURCE_MISMATCH");
  int work=0,previous=-1;while(!s.complete()){if(!s.step())throw new AssertionError("NO_WORK");String snapshot=s.snapshot();Path tmp=out.resolve("checkpoint.tmp");Files.writeString(tmp,snapshot);Files.move(tmp,state,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);s=NullPipelineSimulationR30.restore(snapshot);int n=((Number)s.summary().get("completedNullTrials")).intValue();if(n!=previous){System.out.println("Q08_PROGRESS completed="+n+"/"+NullPipelineSimulationR30.NULL_TRIALS);previous=n;}if(++work>1000)throw new AssertionError("REGISTERED_BUDGET_EXCEEDED");}
  Map<String,Object> summary=s.summary();if(((Number)summary.get("completedNullTrials")).intValue()!=20||Boolean.TRUE.equals(summary.get("naturalQualityEligible")))throw new AssertionError("INVALID_SCOPE");Files.writeString(out.resolve("result.json"),JsonR27.write(summary));System.out.println("R30_REPAIR_FULL_Q08|20_INDEPENDENT_FULL_PIPELINES|CHECKPOINT_RESTORED_EVERY_UNIT|FALSE_QUALIFICATIONS="+summary.get("falseSequentialQualifications")+"|ELIGIBLE="+summary.get("eligibleNullCandidates")+"|SYNTHETIC_NOT_NATURAL|PASS");
 }
}
