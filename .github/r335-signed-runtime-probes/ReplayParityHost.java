package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class ReplayParityHost {
 public static void main(String[] args) {
  List<ReplayDatasetR27.Row> rows=new ArrayList<>();
  long base=java.time.LocalDate.of(2026,11,1).toEpochDay();
  long origin=java.time.LocalDate.of(2020,1,1).toEpochDay()*1440L;
  for(int i=0;i<5000;i++){
   int j=i+(i>=111?3:0)+(i>=2711?7:0);
   String issue=java.time.LocalDate.ofEpochDay(base+j/1440).toString().replace("-","")+String.format(java.util.Locale.ROOT,"%04d",j%1440+1);
   int[] ns={1,2,3,4,5,6,7,8,9,10};int a=i%10,b=(i+3)%10,t=ns[a];ns[a]=ns[b];ns[b]=t;
   long ord=ReplayDatasetR27.ordinalOf(issue)-origin;
   rows.add(new ReplayDatasetR27.Row(issue,ord,ns,1000+i*60000L,2000+i*60000L,ReplayDatasetR27.READER_ABI_R31,"DRAW_FIRST_SEEN"));
  }
  rows.add(rows.get(42)); // duplicate is counted, never becomes another sample
  int[] bad={1,1,3,4,5,6,7,8,9,10};rows.add(new ReplayDatasetR27.Row("202611010044",9999,bad));
  Collections.reverse(rows);
  ReplayDatasetR27 d=ReplayDatasetR27.of(rows);
  if(d.size()!=5000||d.rejectedRows!=2||d.gaps()!=2)throw new AssertionError("rows/gaps changed");
  byte[] b=d.originalBytesR31();ReplayDatasetR27 back=ReplayDatasetR27.originalBytesR31(b);
  if(!d.datasetHash.equals(back.datasetHash)||!Arrays.equals(b,back.originalBytesR31()))throw new AssertionError("original roundtrip");
  if(!d.knownBeforeR31(20,2001+20*60000L)||d.knownBeforeR31(20,2000+20*60000L))throw new AssertionError("PIT cutoff changed");
  byte[] corrupt=b.clone();corrupt[0]^=1;boolean refused=false;
  try{ReplayDatasetR27.originalBytesR31(corrupt);}catch(IllegalArgumentException expected){refused=true;}
  if(!refused)throw new AssertionError("corrupt magic accepted");
  System.out.println(d.datasetHash+"|"+d.provenanceHashR31+"|"+StatsR27.sha256(b)+"|"+d.size()+"|"+d.rejectedRows+"|"+d.gaps());
  if(args.length>0&&"patched".equals(args[0])){
   if(ExecutionBudgetR26.polls<5000/64)throw new AssertionError("row polling missing");
   Thread.currentThread().interrupt();boolean cancelled=false;
   try{ReplayDatasetR27.of(rows);}catch(java.util.concurrent.CancellationException expected){cancelled=true;}finally{Thread.interrupted();}
   if(!cancelled)throw new AssertionError("cancellation not observed");
   System.err.println("poll/cancel assertions passed: "+ExecutionBudgetR26.polls);
  }
 }
}
