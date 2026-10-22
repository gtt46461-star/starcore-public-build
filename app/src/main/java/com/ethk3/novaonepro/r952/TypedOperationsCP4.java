package com.ethk3.novaonepro.r952;
import java.util.*;
/** AST3 operation primitives only. TypedAtomMoleculeGraphR930 remains the sole DAG traversal/evaluator. */
public final class TypedOperationsCP4 {
 private TypedOperationsCP4(){}
 public static final class Value {
  public final double[] x;public final boolean[] present;
  public Value(double[]x,boolean[]present){if(x.length!=present.length)throw new IllegalArgumentException("SHAPE");this.x=x.clone();this.present=present.clone();for(int i=0;i<x.length;i++)if(present[i]&&!Double.isFinite(x[i]))throw new IllegalArgumentException("NONFINITE");}
 }
 public static void validate(String op,String type,int width,String[] types,int[] widths,double[] parameters){
  int n=types.length;if(n!=widths.length||width<1||width>32)throw new IllegalArgumentException("AST3_SHAPE");
  for(double p:parameters)if(!Double.isFinite(p))throw new IllegalArgumentException("AST3_PARAMETER");
  if(op.equals("SCALAR_MULTIPLY_VECTOR10")){require(n==2&&width==10&&type.equals("SCORE_VECTOR10")&&types[0].equals("DOUBLE")&&widths[0]==1&&types[1].equals("SCORE_VECTOR10")&&widths[1]==10&&parameters.length==0,"EXPLICIT_SCALAR_VECTOR");return;}
  if(op.equals("NUMERIC_TO_DOUBLE")){require(n==1&&width==1&&type.equals("DOUBLE")&&widths[0]==1&&parameters.length==0&&(scalar(types[0])||types[0].equals("BOOLEAN")),"NUMERIC_CAST");return;}
  if(op.equals("CONSTANT")){require(n==0&&parameters.length==width,"CONSTANT");boolean[] present=new boolean[width];Arrays.fill(present,true);validateValue(type,new Value(parameters,present));return;}
  if(op.equals("PACK10")){require(parameters.length==0,"PARAMETER_ARITY");require(n==10&&type.equals("SCORE_VECTOR10")&&width==10,"PACK10");for(int i=0;i<n;i++)require(widths[i]==1&&scalar(types[i]),"PACK10_SCALAR");return;}
  if(op.startsWith("THRESHOLD")){require(parameters.length==0,"PARAMETER_ARITY");require(n==2&&width==1&&type.equals("BOOLEAN")&&widths[0]==1&&widths[1]==1&&scalar(types[0])&&scalar(types[1]),"THRESHOLD");return;}
  if(op.equals("CONDITIONAL")){require(parameters.length==0,"PARAMETER_ARITY");require(n==3&&types[0].equals("BOOLEAN")&&widths[0]==1&&types[1].equals(types[2])&&types[1].equals(type)&&widths[1]==width&&widths[2]==width,"CONDITIONAL");return;}
  if(op.equals("REGIME_GATE")){require(n==2&&types[0].equals("REGIME")&&widths[0]==1&&type.equals(types[1])&&width==widths[1]&&parameters.length==1&&parameters[0]>=0&&parameters[0]<5&&parameters[0]==Math.rint(parameters[0]),"REGIME_GATE");return;}
  if(op.equals("TEMPORAL_PATTERN")){require(n>=1&&n<=32&&width==1&&type.equals("BOOLEAN")&&parameters.length==1&&parameters[0]>=0&&parameters[0]<=4294967295.0&&parameters[0]==Math.rint(parameters[0]),"TEMPORAL_PATTERN");for(int i=0;i<n;i++)require(types[i].equals("BOOLEAN")&&widths[i]==1,"TEMPORAL_BOOL");return;}
  if(op.equals("RANK")||op.equals("TOP_K3")||op.equals("NORMALIZE_POSITIVE")){
   require(parameters.length==0,"PARAMETER_ARITY");require(n==1&&widths[0]==10&&types[0].equals("SCORE_VECTOR10"),"VECTOR10_INPUT");
   require(op.equals("RANK")?width==10&&type.equals("RANK_VECTOR10"):op.equals("TOP_K3")?width==3&&type.equals("K3_SET"):width==10&&type.equals("SCORE_VECTOR10"),"VECTOR_OUTPUT");return;
  }
  require(scalar(type)||type.equals("SCORE_VECTOR10"),"NUMERIC_OUTPUT");for(int i=0;i<n;i++)require(types[i].equals(type)&&widths[i]==width,"NO_IMPLICIT_BROADCAST");
  if(op.equals("IDENTITY")||op.equals("SIGMOID")||op.equals("LOG1P_ABS")||op.equals("CLIP"))require(n==1,"ARITY1");
  else if(op.equals("ADD")||op.equals("SUBTRACT")||op.equals("MULTIPLY")||op.equals("SAFE_DIVIDE")||op.equals("MINIMUM")||op.equals("MAXIMUM")||op.equals("RESIDUAL")||op.equals("ABS_DIFFERENCE")||op.equals("SIGNED_PRODUCT")||op.equals("ORTHOGONAL_RESIDUAL"))require(n==2,"ARITY2");
  else if(op.equals("INTERACTION3"))require(n==3,"ARITY3");
  else if(op.equals("SUM")||op.equals("MEAN")||op.equals("WEIGHTED_SUM")||op.equals("WEIGHTED_MEAN"))require(n>=2&&n<=64,"ARITY_MANY");else throw new IllegalArgumentException("AST3_OPERATOR:"+op);
  int expected=op.equals("CLIP")?2:op.startsWith("WEIGHTED")?n:op.equals("ORTHOGONAL_RESIDUAL")?1:0;require(parameters.length==expected,"PARAMETER_ARITY");if(op.equals("CLIP"))require(parameters[0]<=parameters[1],"CLIP_ORDER");
 }
 public static void validateValue(String type,Value value){int seen=0;for(int i=0;i<value.x.length;i++){if(!value.present[i])continue;double x=value.x[i];
  if(type.equals("INTEGER"))require(x==Math.rint(x),"INTEGER_VALUE");
  else if(type.equals("PROBABILITY"))require(x>=0&&x<=1,"PROBABILITY_RANGE");
  else if(type.equals("BOOLEAN"))require(x==0||x==1,"BOOL_VALUE");
  else if(type.equals("REGIME"))require(x>=0&&x<5&&x==Math.rint(x),"REGIME_VALUE");
  else if(type.equals("K3_SET")||type.equals("RANK_VECTOR10")){require(x>=0&&x<10&&x==Math.rint(x),"DIGIT_BIN");int bit=1<<(int)x;require((seen&bit)==0,"DUPLICATE_DIGIT");seen|=bit;}
 }}
 private static boolean scalar(String t){return t.equals("DOUBLE")||t.equals("INTEGER")||t.equals("PROBABILITY");}
 private static void require(boolean ok,String code){if(!ok)throw new IllegalArgumentException("AST3_"+code);}
 public static Value apply(String op,int width,Value[]p,double[]a){double[]out=new double[width];boolean[]mask=new boolean[width];
  if(op.equals("SCALAR_MULTIPLY_VECTOR10")){require(width==10&&p.length==2&&p[0].x.length==1&&p[1].x.length==10&&a.length==0,"EXPLICIT_SCALAR_VECTOR");for(int i=0;i<10;i++){mask[i]=p[0].present[0]&&p[1].present[i];if(mask[i])out[i]=p[0].x[0]*p[1].x[i];}return new Value(out,mask);}
  if(op.equals("NUMERIC_TO_DOUBLE")){require(width==1&&p.length==1&&p[0].x.length==1&&a.length==0,"NUMERIC_CAST");return new Value(p[0].x,p[0].present);}
  if(op.equals("CONSTANT")){System.arraycopy(a,0,out,0,width);Arrays.fill(mask,true);return new Value(out,mask);}
  if(op.equals("PACK10")){for(int i=0;i<10;i++){mask[i]=p[i].present[0];if(mask[i])out[i]=p[i].x[0];}return new Value(out,mask);}
  if(op.equals("CONDITIONAL")){if(!p[0].present[0])return new Value(out,mask);require(p[0].x[0]==0||p[0].x[0]==1,"BOOL_VALUE");Value selected=p[p[0].x[0]==1?1:2];return new Value(selected.x,selected.present);}
  if(op.equals("REGIME_GATE")){if(!p[0].present[0]||p[0].x[0]!=a[0])return new Value(out,mask);return new Value(p[1].x,p[1].present);}
  if(op.equals("TEMPORAL_PATTERN")){long bits=0;for(int i=0;i<p.length;i++){if(!p[i].present[0])return new Value(out,mask);require(p[i].x[0]==0||p[i].x[0]==1,"BOOL_VALUE");bits=(bits<<1)|(long)p[i].x[0];}mask[0]=true;out[0]=bits==(long)a[0]?1:0;return new Value(out,mask);}
  if(op.equals("RANK")||op.equals("TOP_K3")||op.equals("NORMALIZE_POSITIVE")){
   for(boolean present:p[0].present)if(!present)return new Value(out,mask);double sum=0;for(double v:p[0].x){require(v>=0,"NEGATIVE_SCORE");sum+=v;}
   if(op.equals("NORMALIZE_POSITIVE")){for(int i=0;i<10;i++){out[i]=(p[0].x[i]+1e-9)/(sum+1e-8);mask[i]=true;}}
   else{if(op.equals("TOP_K3"))require(Math.abs(sum-1)<=1e-9,"PROBABILITY_SUM");Integer[]idx={0,1,2,3,4,5,6,7,8,9};Arrays.sort(idx,(i,j)->{int c=Double.compare(p[0].x[j],p[0].x[i]);return c!=0?c:Integer.compare(i,j);});for(int i=0;i<width;i++){out[i]=idx[i];mask[i]=true;}}
   return new Value(out,mask);
  }
  for(int i=0;i<width;i++){boolean present=true;for(Value v:p)present&=v.present[i];if(!present)continue;double v=0;
   switch(op){case "THRESHOLD_GE":v=p[0].x[i]>=p[1].x[i]?1:0;break;case "THRESHOLD_GT":v=p[0].x[i]>p[1].x[i]?1:0;break;case "THRESHOLD_LE":v=p[0].x[i]<=p[1].x[i]?1:0;break;
    case "IDENTITY":v=p[0].x[i];break;case "ADD":v=p[0].x[i]+p[1].x[i];break;case "SUBTRACT":case "RESIDUAL":v=p[0].x[i]-p[1].x[i];break;case "MULTIPLY":v=p[0].x[i]*p[1].x[i];break;
    case "SAFE_DIVIDE":if(Math.abs(p[1].x[i])<1e-9)continue;v=p[0].x[i]/p[1].x[i];break;
    case "MINIMUM":v=Math.min(p[0].x[i],p[1].x[i]);break;case "MAXIMUM":v=Math.max(p[0].x[i],p[1].x[i]);break;case "ABS_DIFFERENCE":v=Math.abs(p[0].x[i]-p[1].x[i]);break;case "SIGNED_PRODUCT":v=Math.copySign(Math.abs(p[0].x[i]*p[1].x[i]),p[0].x[i]);break;
    case "ORTHOGONAL_RESIDUAL":v=p[0].x[i]-a[0]*p[1].x[i];break;case "INTERACTION3":v=p[0].x[i]*p[1].x[i]*p[2].x[i];break;
    case "CLIP":v=Math.max(a[0],Math.min(a[1],p[0].x[i]));break;case "LOG1P_ABS":v=Math.log1p(Math.abs(p[0].x[i]));break;case "SIGMOID":v=1/(1+Math.exp(-Math.max(-700,Math.min(700,p[0].x[i]))));break;
    case "SUM":case "MEAN":for(Value item:p)v+=item.x[i];if(op.equals("MEAN"))v/=p.length;break;
    case "WEIGHTED_SUM":case "WEIGHTED_MEAN":double denom=0;for(int j=0;j<p.length;j++){v+=p[j].x[i]*a[j];denom+=Math.abs(a[j]);}if(op.equals("WEIGHTED_MEAN")){if(denom<1e-12)continue;v/=denom;}break;
    default:throw new IllegalArgumentException("AST3_OPERATOR:"+op);
   }
   if(Double.isFinite(v)){mask[i]=true;out[i]=v;}
  }
  return new Value(out,mask);
 }
}
