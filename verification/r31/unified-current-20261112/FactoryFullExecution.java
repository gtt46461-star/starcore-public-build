package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.util.*;
/** Execution harness only: all factory/selection/confirmation logic is production code. */
public final class FactoryFullExecution {
  public static void main(String[] args) throws Exception {
    Path out=Paths.get(args[0]);Files.createDirectories(out);
    final String source=args[1];long began=System.nanoTime();int steps=0,last=-1;
    try(FactoryCapabilityExperimentR31 experiment=new FactoryCapabilityExperimentR31(source)){
      while(!experiment.complete()){
        experiment.step();steps++;
        Map<String,Object> summary=experiment.summary();int n=((Number)summary.get("completedTrials")).intValue();
        if(n!=last){
          Map<String,Object> artifact=experiment.pendingArtifact();
          if(artifact!=null)Files.writeString(out.resolve(String.format("trial-%03d.json",n)),JsonR27.write(artifact));
          summary.put("scope","SYNTHETIC_HOST_FULL_PRODUCTION_FACTORY; NOT_ANDROID_SERVICE_OR_NATURAL_QUALITY");
          summary.put("elapsedMs",(System.nanoTime()-began)/1000000);summary.put("actualSteps",steps);
          Files.writeString(out.resolve("SUMMARY.json"),JsonR27.write(summary));
          System.out.println("FACTORY_FULL completed="+n+" steps="+steps+" elapsedMs="+(System.nanoTime()-began)/1000000+" failed="+summary.get("failedTrials"));System.out.flush();last=n;
        }
        if(steps%25==0)Files.writeString(out.resolve("CHECKPOINT.json"),experiment.snapshot());
      }
      Map<String,Object> summary=experiment.summary();summary.put("actualSteps",steps);summary.put("elapsedMs",(System.nanoTime()-began)/1000000);
      summary.put("scope","SYNTHETIC_HOST_FULL_PRODUCTION_FACTORY; NOT_ANDROID_SERVICE_OR_NATURAL_QUALITY");
      Files.writeString(out.resolve("SUMMARY.json"),JsonR27.write(summary));
      System.out.println("FACTORY_FULL_TERMINAL "+summary.get("status")+" registered="+summary.get("meetsRegisteredExperiment"));
      if(((Number)summary.get("failedTrials")).longValue()!=0)System.exit(1);
    }
  }
}
