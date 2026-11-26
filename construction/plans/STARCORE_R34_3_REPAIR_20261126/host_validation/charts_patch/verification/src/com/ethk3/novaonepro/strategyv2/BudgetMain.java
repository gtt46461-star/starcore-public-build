package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class BudgetMain {
 public static void main(String[] args){
  for(long seed=0;seed<128;seed++){
   ReportInlineBudgetR343 b=new ReportInlineBudgetR343();
   List<String> keys=new ArrayList<>(Arrays.asList("chartObjectIndex","repairFrameIndex","researchCurrentHeadsR342","researchCurrentActionsR342"));
   for(int i=0;i<96;i++)keys.add("bulk:"+i);
   Collections.shuffle(keys,new Random(seed));int sum=0;Set<String> reserved=new HashSet<>();
   for(String key:keys){int n=b.take(key,128*1024);sum+=n;
    if(!key.startsWith("bulk:")){if(n!=(key.equals("chartObjectIndex")?16384:32768))throw new AssertionError(key+":"+n);reserved.add(key);}
    if(sum>ReportInlineBudgetR343.TOTAL_BYTES)throw new AssertionError("unbounded");
   }
   if(sum!=ReportInlineBudgetR343.TOTAL_BYTES||reserved.size()!=4||b.take("tail",8192)!=0)throw new AssertionError("lost budget");
  }
  ReportInlineBudgetR343 b=new ReportInlineBudgetR343();try{b.take("x",-1);throw new AssertionError("negative admitted");}catch(IllegalArgumentException expected){}
  System.out.println("PASS actual ReportInlineBudgetR343: 128 interleavings, full total <= 768 KiB, four metadata reservations preserved, negative rejected");
 }
}
