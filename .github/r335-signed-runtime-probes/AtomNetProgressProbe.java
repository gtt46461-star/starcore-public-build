import com.ethk3.novaonepro.strategyv2.AtomNetProgressR33;
public final class AtomNetProgressProbe {
 static int checks;
 static AtomNetProgressR33.Frame f(long m,long t,long o,String p,int offset,int next,String partial){return new AtomNetProgressR33.Frame("dataset",p,m,t,o,0,offset,0,next,"202611160100",partial);}
 static void eq(String got,String expected){if(!got.equals(expected))throw new AssertionError(got);checks++;}
 static void reject(Runnable r){try{r.run();throw new AssertionError("accepted bad input");}catch(IllegalStateException expected){checks++;}}
 public static void main(String[] args){
  eq(AtomNetProgressR33.validate(f(3150,0,0,"TRAIN",0,1,""),f(3250,0,0,"TRAIN",0,1,""),100),"MATERIALIZE");
  eq(AtomNetProgressR33.validate(f(20560,0,0,"TRAIN",0,1,""),f(20560,64,0,"TRAIN",64,1,""),64),"PRETRAIN_GRADIENT");
  eq(AtomNetProgressR33.validate(f(20560,640,10,"ONLINE",0,1,""),f(20560,640,11,"ONLINE",0,2,"202611160101"),1),"ONLINE_GRADIENT");
  eq(AtomNetProgressR33.validate(f(20560,640,0,"EVAL_TRAIN",0,1,""),f(20560,640,0,"EVAL_TRAIN",64,1,""),64),"EVALUATION_OR_PHASE_TRANSITION");
  reject(()->AtomNetProgressR33.validate(f(3150,0,0,"TRAIN",0,1,""),f(3150,0,0,"TRAIN",0,1,""),1));
  reject(()->AtomNetProgressR33.validate(f(3150,0,0,"TRAIN",0,1,""),f(3140,0,0,"TRAIN",0,1,""),0));
  reject(()->AtomNetProgressR33.validate(f(3150,0,0,"TRAIN",0,1,""),f(3250,64,0,"TRAIN",64,1,""),64));
  reject(()->AtomNetProgressR33.validate(f(20560,0,0,"TRAIN",0,1,""),f(20560,65,0,"TRAIN",65,1,""),65));
  reject(()->AtomNetProgressR33.validate(f(3150,0,0,"TRAIN",0,1,""),f(3250,0,0,"TRAIN",0,1,""),99));
  reject(()->AtomNetProgressR33.validate(f(20560,0,0,"TRAIN",0,1,""),f(20560,64,0,"TRAIN",64,1,""),63));
  System.out.println("production progress validator checks="+checks);
 }
}
