package com.ethk3.novaonepro.strategyv2;
import com.ethk3.novaonepro.AtomReplayR27;
import java.util.*;
/** Actual current producers and actual features; engineering history, no quality inference. */
public final class BaselineAtomMeasureHost {
 public static void main(String[] args)throws Exception{
  ReplayDatasetR27 data=R27TestSupport.csv(160);int input=7+AtomReplayR27.eligible().length+10;
  AtomNetSessionR30 state=new AtomNetSessionR30(data.datasetHash,data.issues[159],data.ordinals[159],input,800,400,160);
  ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();int samples=0;long featureBytes=0;
  for(int t=AtomReplayR27.MIN_ROWS;t<AtomReplayR27.MIN_ROWS+12;t++){
   AtomReplayR27.Frame frame=AtomReplayR27.build(data,t,t+1);
   for(int p=0;p<10;p++){if(frame.values[p]==null)throw new AssertionError("actual input missing");double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(frame.values[p],frame.present[p],true),p+1);AtomNetStoreR30.sample(b,data.datasetHash,(t-AtomReplayR27.MIN_ROWS)*10+p,data.issues[t],p+1,x,frame.hit[p],frame.actual[p]-1);samples++;featureBytes+=AtomNetStoreR30.features(x).length;}
   state.cachePeriod++;
  }
  state.materializedSamples=samples;ResearchWriteQueueR27.ImmutableBatch sample=b.prepareR31("ATOMNET_R30_SLICE","measure-samples","r27-atomnet","HOST_OLD",1,data.datasetHash,1,"014_CURRENT");
  byte[] outer=state.encode();String stateHash=AtomNetStoreR30.checkpointWithIdentity(b,state,System.currentTimeMillis());ResearchWriteQueueR27.ImmutableBatch combined=b.prepareR31("ATOMNET_R30_SLICE","measure-full","r27-atomnet","HOST_OLD",1,data.datasetHash,1,"014_CURRENT");
  Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL_CURRENT_PRODUCER_NEGATIVE_MAX12_PERIODS_TEN_POSITION_FEATURES_WITHOUT_TIME_SLICE_TRUNCATION");result.put("positions",10);result.put("periods",12);result.put("samples",samples);result.put("input",input);result.put("rawFeatureBytes",featureBytes);result.put("sampleOps",sample.opCount);result.put("sampleBindBytes",sample.bindBytes);result.put("sampleOverInitialBudget",sample.overInitialBudget);result.put("outerBytes",outer.length);result.put("outerHash",stateHash);result.put("combinedOps",combined.opCount);result.put("combinedBindBytes",combined.bindBytes);result.put("combinedOverInitialBudget",combined.overInitialBudget);result.put("containsDdl",combined.containsDdl);result.put("oldNumbersReused",false);result.put("engineeringFixture",true);result.put("physicalDevice","NOT_RUN");result.put("naturalQuality","NOT_EVALUATED");if(samples!=120||!combined.overInitialBudget||!sample.overInitialBudget)throw new AssertionError("negative not reproduced");System.out.println(JsonR27.write(result));
 }
}
