package com.ethk3.novaonepro.r952;
import java.util.*;
/** Independent old per-row reconstruction versus the existing incremental window kernel.
 * Synthetic deterministic digits exercise memory, missing values and ring boundaries only. */
public final class AtomWindowParityR79 {
 static int assertions,targets;static long oldAppends,newAppends;
 static void check(boolean ok,String why){assertions++;if(!ok)throw new AssertionError(why);}
 static List<AtomEvaluatorCP4.Spec> specs(){
  List<AtomEvaluatorCP4.Spec> out=new ArrayList<>();
  String[] fs={"LAG_DIGIT_EQ","DELTA_NUMERIC","DIRECTION","FREQUENCY","ROLLING_MEAN_INDICATOR","ROLLING_VARIANCE_INDICATOR","FREQUENCY_SLOPE","OMISSION","OMISSION_RATE","GAP_LAST_GAP","GAP_MEAN_GAP","GAP_VAR_GAP","GAP_GAP_Z","TRANSITION_P1","MARKOV_SCORE","PERIODICITY","ENTROPY","CONDITIONAL_ENTROPY","MUTUAL_INFORMATION","ROLLING_MEAN_DIGIT","ROLLING_VARIANCE_DIGIT","HOT_COLD_Z","DRIFT_JS","DRIFT_PSI","REGIME_SCORE"};
  for(int w:new int[]{20,120,500})for(String f:fs)out.add(new AtomEvaluatorCP4.Spec(f,w,1,3,2,4,20,w==20?40:w,7,.5,"DRIFTING"));
  out.add(new AtomEvaluatorCP4.Spec("MARKOV_SCORE",120,1,3,2,4,20,120,1,.5,"",2,8));return out;
 }
 public static void main(String[] args){
  List<AtomEvaluatorCP4.Spec> specs=specs();long start=System.nanoTime();
  for(int pattern=0;pattern<3;pattern++){
   int[] digits=new int[6100];Random random=new Random(7931+pattern);
   for(int i=0;i<digits.length;i++)digits[i]=pattern==0?random.nextInt(10):pattern==1?i%10:(i<80?i%10:4);
   int[][] pages={{1,64},{20,83},{490,553},{1000,1063},{4930,4993},{4980,5043},{6000,6063}};
   for(int[] page:pages){MemoryBudgetCP4 cachedBudget=new MemoryBudgetCP4(8L<<20),scalarBudget=new MemoryBudgetCP4(8L<<20);
    try(AtomWindowCP4 cached=new AtomWindowCP4(cachedBudget)){
     int next=Math.max(0,page[0]-5000);
     for(int target=page[0];target<=page[1];target++){
      // Include the held-out embargo row in history before the later validation target.
      if(target==page[0]+31)continue;
      while(next<target){cached.append(next,digits[next]);next++;newAppends++;}
      try(AtomWindowCP4 scalar=new AtomWindowCP4(scalarBudget)){
       for(int i=Math.max(0,target-5000);i<target;i++){scalar.append(i,digits[i]);oldAppends++;}
       for(AtomEvaluatorCP4.Spec s:specs){
        AtomEvaluatorCP4.Result a=AtomEvaluatorCP4.evaluate(s,scalar,null,target),b=AtomEvaluatorCP4.evaluate(s,cached,null,target);
        check(a.present==b.present&&a.reason.equals(b.reason)&&Double.doubleToRawLongBits(a.value)==Double.doubleToRawLongBits(b.value),"parity pattern="+pattern+" target="+target+" family="+s.family+" w="+s.w+" old="+a.value+" new="+b.value+" oldReason="+a.reason+" newReason="+b.reason);
       }targets++;
      }
      check(scalarBudget.used()==0,"scalar reservation leak");
     }
    }
    check(cachedBudget.used()==0,"cached reservation leak");
   }
  }
  System.out.println("{\"status\":\"PASS\",\"assertions\":"+assertions+",\"targets\":"+targets+",\"specs\":"+specs.size()+",\"old_appends\":"+oldAppends+",\"page_appends\":"+newAppends+",\"elapsed_ms\":"+(System.nanoTime()-start)/1000000+",\"scope\":\"PURE_PRODUCT_KERNEL_BIT_PARITY_NOT_DEVICE_TIMING_OR_NATURAL_FUTURE\"}");
 }
}
