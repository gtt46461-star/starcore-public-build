package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** R01/R02: preregistered coarse binary targets and all four windows, never retrospective best-window selection. */
public final class StructureStudyR30R1 {
 private StructureStudyR30R1(){}public static final int[] WINDOWS={8,32,128,512};
 public static final String VERSION="R30_REPAIR_STRUCTURE_STUDY1";
 public static final String[] TARGETS={"ODD_BINARY","BIG_BINARY","SAME_POSITION_REPEAT_BINARY"};
 static double baseline(int endpoint){return endpoint==2?.1:.5;}
 /** Values at row use only earlier draws. Positions are not counted as independent calendar observations. */
 public static double[][] probabilities(ReplayDatasetR27 data,int row,int window){
  if(row<0||row>data.size()||Arrays.binarySearch(WINDOWS,window)<0)throw new IllegalArgumentException("STRUCTURE_SOURCE_WINDOW");
  if(row<window+1||row<data.size()&&!data.consecutive(row,window+1))return null;
  // A forecast after the snapshot must also have a contiguous retained prefix.
  if(row==data.size()&&data.ordinals[row-1]-data.ordinals[row-window-1]!=window)return null;
  double[][] p=new double[10][3];for(int pos=0;pos<10;pos++){
   int odd=0,big=0,repeats=0;for(int t=row-window;t<row;t++){int value=data.draws[t][pos];if((value&1)==1)odd++;if(value>5)big++;if(value==data.draws[t-1][pos])repeats++;}
   p[pos][0]=(odd+1.0)/(window+2);p[pos][1]=(big+1.0)/(window+2);p[pos][2]=(repeats+1.0)/(window+10);
  }return p;
 }
 static int outcome(ReplayDatasetR27 data,int row,int position,int endpoint){int v=data.draws[row][position];return endpoint==0?v%2:endpoint==1?v>5?1:0:row>0&&v==data.draws[row-1][position]?1:0;}
 public static final class Product {
  public final String statePayload,errorPayload,stateHash,errorHash;public final int distinctCalendarIssues;
  Product(Map<String,Object> state,Map<String,Object> errors,int n){statePayload=JsonR27.write(state);errorPayload=JsonR27.write(errors);stateHash=StatsR27.sha256(statePayload);errorHash=StatsR27.sha256(errorPayload);distinctCalendarIssues=n;}
 }
 static Map<String,Object> header(EvalContextR27 c,String view){Map<String,Object> m=new LinkedHashMap<>();m.put("schema",VERSION);m.put("viewId",view);m.put("datasetHash",c.data.datasetHash);m.put("role","SELECT");m.put("scope","CAUSAL_REPLAY_RESEARCH_NOT_SAVED_PHYSICAL_PREDRAW_OR_NATURAL_CERTIFICATE");m.put("fromOrdinal",c.data.ordinals[c.split.selectFrom]);m.put("throughOrdinal",c.data.ordinals[c.split.selectTo-1]);m.put("futureHypothesisTargetOrdinal",c.data.ordinals[c.data.size()-1]+1);m.put("formalRowsUsed",0);m.put("positionsPerCalendarIssue",10);m.put("windowSelection","ALL_PREDECLARED_WINDOWS_RETAINED");m.put("qualityEffective",null);m.put("action","GENERATE_NEW_RESEARCH_HYPOTHESIS_ONLY_NORMAL_BIRTH_AND_INDEPENDENT_CONFIRMATION_REQUIRED");return m;}
 public static Product compute(EvalContextR27 c,ResearchContractR30R1.Role role){
  if(role!=ResearchContractR30R1.Role.SELECT||c.labels!=c.data.draws||c.split.selectFrom>=c.split.selectTo)throw new IllegalArgumentException("STRUCTURE_STUDY_REQUIRES_ORIGINAL_SELECT_ROLE");
  Map<String,Object> state=header(c,"R01"),errors=header(c,"R02");List<Object> stateCells=new ArrayList<>(),errorCells=new ArrayList<>();int from=c.split.selectFrom,to=c.split.selectTo;long start=c.data.ordinals[from],end=c.data.ordinals[to-1];
  for(int window:WINDOWS){ResearchGainR30R1[] gains=new ResearchGainR30R1[3];double[] modelLoss=new double[3],baseLoss=new double[3],logLoss=new double[3];int[] n=new int[3];
   for(int k=0;k<3;k++)gains[k]=new ResearchGainR30R1(start,end,64,4,200,ResearchGainR30R1.Loss.BINARY_BRIER);
   for(int row=from;row<to;row++){if((row&15)==0)CooperativeContinuationR26.poll();double[][] p=probabilities(c.data,row,window);if(p==null)continue;for(int k=0;k<3;k++){double a=0,b=0,ll=0;for(int pos=0;pos<10;pos++){int y=outcome(c.data,row,pos,k);double probability=p[pos][k],e=probability-y,be=baseline(k)-y;a+=e*e;b+=be*be;ll-=y*Math.log(probability)+(1-y)*Math.log(1-probability);}a/=10;b/=10;ll/=10;modelLoss[k]+=a;baseLoss[k]+=b;logLoss[k]+=ll;n[k]++;gains[k].add(c.data.ordinals[row],b,a);}}
   // Predict from the legal research prefix; this is a new future hypothesis, not a reconstructed old freeze.
   double[][] next=probabilities(c.data,to,window);
   for(int pos=0;pos<10;pos++)for(int k=0;k<3;k++){Map<String,Object> cell=new LinkedHashMap<>();cell.put("position",pos+1);cell.put("window",window);cell.put("target",TARGETS[k]);cell.put("value",next==null?null:next[pos][k]);cell.put("supportCalendarIssues",next==null?0:window);cell.put("status",next==null?"BLOCKED_SOURCE":"RESEARCH_HYPOTHESIS");stateCells.add(cell);}
   for(int k=0;k<3;k++){ResearchGainR30R1.Estimate estimate=gains[k].estimate();Map<String,Object> cell=new LinkedHashMap<>();cell.put("window",window);cell.put("target",TARGETS[k]);cell.put("baselineProbability",baseline(k));cell.put("calendarIssues",n[k]);cell.put("positionSamples",n[k]*10L);cell.put("modelBrier",n[k]==0?null:modelLoss[k]/n[k]);cell.put("baselineBrier",n[k]==0?null:baseLoss[k]/n[k]);cell.put("logLoss",n[k]==0?null:logLoss[k]/n[k]);cell.put("meanPairedGain",estimate.meanBlockGain);cell.put("lower95",estimate.lower95);cell.put("upper95",estimate.upper95);cell.put("blocks",gains[k].blocks());cell.put("completeBlocks",estimate.completeBlocks);cell.put("missingIssues",estimate.missingIssues);cell.put("excludedTailIssues",estimate.excludedTailIssues);cell.put("analysisContractHash",estimate.analysisContractHash);cell.put("lossTarget",estimate.loss.name());cell.put("status",estimate.supported()?"DESCRIPTIVE_RESEARCH_ONLY":"INSUFFICIENT");errorCells.add(cell);}
  }
  state.put("cells",stateCells);state.put("cellCount",stateCells.size());state.put("smoothing","ODD/BIG Beta(1,1); REPEAT Beta(1,9), fixed before labels");errors.put("cells",errorCells);errors.put("cellCount",errorCells.size());errors.put("distinctCalendarIssues",to-from);errors.put("intervalScope","Calendar-clustered complete fixed64 blocks, descriptive t95, not a certificate");return new Product(state,errors,to-from);
 }
}
