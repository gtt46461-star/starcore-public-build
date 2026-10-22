package com.ethk3.novaonepro.r952;
import java.util.*;
public class MatrixProjectionCP12Test {
 static int checks;
 static void yes(boolean x){checks++;if(!x)throw new AssertionError("check "+checks);}
 static void near(double a,double b){yes(Math.abs(a-b)<1e-8*Math.max(1,Math.abs(a)));}
 static void rejects(Runnable r,String code){try{r.run();throw new AssertionError("accepted "+code);}catch(IllegalArgumentException e){yes(e.getMessage().equals(code));}}
 static MatrixProjectionCP12.Sample sample(MatrixProjectionCP12.Type t,long n){int flags=MatrixProjectionCP12.ELIGIBLE|MatrixProjectionCP12.OBSERVED|MatrixProjectionCP12.PAIRED;if(n%3==0)flags|=MatrixProjectionCP12.HIT_A;if(n%4==0)flags|=MatrixProjectionCP12.HIT_B;
  int a=(int)(n%10),b=(int)((n+1)%10),c=(int)(n%5);double x=.1+(n%8)*.1,y=.2+(n%7)*.1,z=(n%3)-1;
  if(t==MatrixProjectionCP12.Type.FailureMatrix){a=(int)(n%4);z=0;}
  if(t==MatrixProjectionCP12.Type.LatencyMatrix)x=n%2000;
  return new MatrixProjectionCP12.Sample(n,flags,a,b,c,7,56,x,y,z);
 }
 public static void main(String[] args){String h=MatrixProjectionCP12.hash("test");
  for(MatrixProjectionCP12.Type t:MatrixProjectionCP12.Type.values()){
   MatrixProjectionCP12.Spec spec=new MatrixProjectionCP12.Spec(t,t==MatrixProjectionCP12.Type.ConditionalTransitionMatrix?"test|regimePolicy:"+MatrixProjectionCP12.hash("CP12_ATOM_REGIME_ARGMAX5_V1"):"test",t==MatrixProjectionCP12.Type.FreezeMatrix?1:13,h);MatrixProjectionCP12.State state=MatrixProjectionCP12.empty(spec);
   Map<String,String> components=t==MatrixProjectionCP12.Type.FreezeMatrix?Collections.singletonMap("feature",h):null;
   for(int n=1;n<=150;n++){
    if(t==MatrixProjectionCP12.Type.FreezeMatrix&&n>1){MatrixProjectionCP12.apply(state,n,sample(t,n),components);yes(state.samples()==1);continue;}
    MatrixProjectionCP12.apply(state,n,sample(t,n),components);
    byte[] bytes=MatrixProjectionCP12.encode(state);MatrixProjectionCP12.State restored=MatrixProjectionCP12.decode(spec,bytes);yes(Arrays.equals(bytes,MatrixProjectionCP12.encode(restored)));
    MatrixProjectionCP12.State fresh=MatrixProjectionCP12.empty(spec);for(MatrixProjectionCP12.Sample row:state.samplesCopy())MatrixProjectionCP12.apply(fresh,row.ordinal,row,components);
    // Drift's final through ordinal is the same as the last stored slot.
    double[] a=state.sufficientStatistics(),b=fresh.sufficientStatistics();yes(a.length==b.length);for(int i=0;i<a.length;i++)near(a[i],b[i]);
    Map<String,double[]> actual=MatrixProjectionCP12.metrics(state),expected=MatrixProjectionCP12.metrics(fresh);yes(actual.keySet().equals(expected.keySet()));for(String key:actual.keySet()){double[] av=actual.get(key),ev=expected.get(key);yes(av.length==ev.length);for(int i=0;i<av.length;i++)near(av[i],ev[i]);}
    state=restored;
   }
  }
  MatrixProjectionCP12.Spec f=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FrequencyMatrix,"p",4,h);MatrixProjectionCP12.State fs=MatrixProjectionCP12.empty(f);for(int n=0;n<4;n++)MatrixProjectionCP12.apply(fs,n,new MatrixProjectionCP12.Sample(n,0,n%2,0,0,0,0,0,0,0),null);double[] p=MatrixProjectionCP12.metrics(fs).get("probabilities");near(p[0],.5);near(p[1],.5);
  MatrixProjectionCP12.Spec cw=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.CandidateWindowMatrix,"freeze",4,h);MatrixProjectionCP12.State cs=MatrixProjectionCP12.empty(cw);for(int n=0;n<4;n++){int fl=MatrixProjectionCP12.ELIGIBLE|MatrixProjectionCP12.OBSERVED|(n%2==0?MatrixProjectionCP12.HIT_A:0);MatrixProjectionCP12.apply(cs,n,new MatrixProjectionCP12.Sample(n,fl,0,0,0,0,0,.3,0,0),null);}near(MatrixProjectionCP12.metrics(cs).get("hitRate")[0],.5);
  MatrixProjectionCP12.Spec full=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FrequencyMatrix,"p",5000,h);MatrixProjectionCP12.State huge=MatrixProjectionCP12.empty(full);for(int n=0;n<5000;n++)MatrixProjectionCP12.apply(huge,n,sample(MatrixProjectionCP12.Type.FrequencyMatrix,n),null);byte[] encoded=MatrixProjectionCP12.encode(huge);yes(encoded.length<262144);yes(MatrixProjectionCP12.decode(full,encoded).samples()==5000);
  MatrixProjectionCP12.Spec fail=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FailureMatrix,"reason",10,h);MatrixProjectionCP12.State failure=MatrixProjectionCP12.empty(fail);MatrixProjectionCP12.apply(failure,1,new MatrixProjectionCP12.Sample(1,0,2,0,0,0,0,0,0,0),null);MatrixProjectionCP12.apply(failure,3,new MatrixProjectionCP12.Sample(1,MatrixProjectionCP12.RECOVERED,2,0,0,0,0,0,0,123),null);near(MatrixProjectionCP12.metrics(failure).get("failureCount")[0],1);near(MatrixProjectionCP12.metrics(failure).get("recoveryLagMean")[0],123);yes(Arrays.equals(MatrixProjectionCP12.encode(failure),MatrixProjectionCP12.encode(MatrixProjectionCP12.decode(fail,MatrixProjectionCP12.encode(failure)))));
  rejects(()->MatrixProjectionCP12.apply(failure,4,new MatrixProjectionCP12.Sample(1,MatrixProjectionCP12.RECOVERED,2,0,0,0,0,0,0,123),null),"UNMATCHED_OR_DUPLICATE_RECOVERY");
  MatrixProjectionCP12.Spec freeze=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FreezeMatrix,"freeze",1,h);MatrixProjectionCP12.State fr=MatrixProjectionCP12.empty(freeze);MatrixProjectionCP12.apply(fr,1,sample(MatrixProjectionCP12.Type.FreezeMatrix,1),Collections.singletonMap("feature",h));rejects(()->MatrixProjectionCP12.apply(fr,2,sample(MatrixProjectionCP12.Type.FreezeMatrix,2),Collections.singletonMap("feature",MatrixProjectionCP12.hash("changed"))),"FREEZE_IDENTITY_DRIFT");
  System.out.println("CP12_MATRIX_PROJECTION_PASS checks="+checks+" types=20 max5000Bytes="+encoded.length);
 }
}
