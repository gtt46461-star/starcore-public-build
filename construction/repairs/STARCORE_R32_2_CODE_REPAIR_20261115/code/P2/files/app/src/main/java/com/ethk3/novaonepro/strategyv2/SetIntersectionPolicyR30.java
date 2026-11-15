package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Actual intersection of two independently sourced past K3 sets. Old mix-based I14 policies keep their original ABI. */
public final class SetIntersectionPolicyR30 implements IdeaFamilyR27 {
 static final int[] W={8,32,128,512};
 public String id(){return "I14";}public String nameZh(){return "集合交叉族";}public String coreIdea(){return "事前本位短窗K3与两邻位长窗K3精确相交；不足3弃权";}
 static int gen(IdeaFamilyR27.Context c){return c==null?0:c.generation;}
 public PolicySpecR27 generate(StatsR27.SplitMix r,IdeaFamilyR27.Context c){int tp=IdeaFamilyR27.Grow.pos(r);if(c!=null&&c.research!=null)tp=c.research.position(r,tp);int i=r.nextInt(W.length-1),j=i+1+r.nextInt(W.length-i-1);return spec(tp,W[i],W[j],IdeaFamilyR27.meta(id(),"GENERATE",gen(c),r.state,"事前集合：本位近"+W[i]+"期 × 两邻位近"+W[j]+"期；只接受精确K3交集；不足3明确弃权"));}
 static PolicySpecR27 spec(int tp,int shortWindow,int longWindow,PolicySpecR27.Meta meta){return new PolicySpecR27(PolicyAstR27.leaf("set_intersection",shortWindow,longWindow),tp,PolicySpecR27.Rule.ALWAYS,0,1,1,false,0,0,meta);}
 public PolicySpecR27 mutate(PolicySpecR27 p,StatsR27.SplitMix r,IdeaFamilyR27.Context c){PolicySpecR27 fresh=generate(r,c);return fresh.withMeta(new PolicySpecR27.Meta(id(),"SET_SOURCE_WINDOW",Collections.singletonList(p.policyHash()),gen(c),r.state,"改变事前集合源窗口的新身份；原集合/旧策略保留；新资格重新确认"));}
 public static boolean exact(PolicySpecR27 p){return p!=null&&p.ast!=null&&(p.ast.op.equals("set_intersection")||p.ast.op.equals("set_intersection_when_repeat"));}
 static PolicySpecR27 conditional(int tp,int shortWindow,int longWindow,PolicySpecR27.Meta meta){return new PolicySpecR27(PolicyAstR27.leaf("set_intersection_when_repeat",shortWindow,longWindow),tp,PolicySpecR27.Rule.ALWAYS,0,1,1,false,0,0,meta);}
 public static Map<String,Object> support(PolicySpecR27 p,FeatureCacheR27 cache,int row){Map<String,Object> out=new LinkedHashMap<>(support(cache,row,p.position,p.ast.ints[0],p.ast.ints[1]));if(p.ast.op.equals("set_intersection_when_repeat")){double condition=leafValueR31(cache,"regime",p.position,0,p.ast.ints[1],row*10);out.put("predrawRepeatCondition",Double.isFinite(condition)?condition:null);out.put("conditionWindow",p.ast.ints[1]);out.put("conditionThreshold",.1);if(row<p.ast.ints[1]||!cache.data.consecutive(row,p.ast.ints[1]))out.put("status","BLOCKED_HISTORY_OR_GAP");else if(Double.isFinite(condition)&&condition<=.1)out.put("status","ABSTAIN_CONDITION");}return out;}
 // Support consumes one row. Keep the immutable reader scoped instead of
 // creating a second public T*10 array alongside the AST's private inputs.
 private static double leafValueR31(FeatureCacheR27 cache,String kind,int tp,int a,int b,int offset){
  try(SharedFeatureStoreR31.Reader reader=cache.acquireLeafR31(kind,tp,a,b)){
   return reader==null?cache.leaf(kind,tp,a,b)[offset]:reader.doubleAt(offset);
  }
 }
 private static int leafMaskR31(FeatureCacheR27 cache,String kind,int tp,int a,int b,int row){
  try(SharedFeatureStoreR31.Reader reader=cache.acquireLeafR31(kind,tp,a,b)){
   if(reader==null)return mask(cache.leaf(kind,tp,a,b),row*10);
   double[] values=new double[10];reader.copyRangeTo(row*10,values,0,10);return mask(values,0);
  }
 }
 static int mask(double[] values,int offset){int[] order=new int[10];double[] p=new double[10];for(int k=0;k<10;k++){p[k]=values[offset+k];if(!Double.isFinite(p[k]))return 0;}ReplayEvaluatorR27.top3(p,order);return (1<<order[0])|(1<<order[1])|(1<<order[2]);}
 public static Map<String,Object> support(FeatureCacheR27 cache,int row,int position,int shortWindow,int longWindow){
  if(row<0||row>=cache.data.size()||position<1||position>10||!PolicyAstR27.window(shortWindow)||!PolicyAstR27.window(longWindow)||shortWindow>=longWindow)throw new IllegalArgumentException("SET_SOURCE_CONTRACT");
  Map<String,Object> out=new LinkedHashMap<>();out.put("targetOrdinal",cache.data.ordinals[row]);out.put("position",position);out.put("sourcePositions",Arrays.asList(position,position==1?10:position-1,position==10?1:position+1));out.put("shortWindow",shortWindow);out.put("longWindow",longWindow);out.put("inputThroughOrdinal",row==0?null:cache.data.ordinals[row-1]);out.put("availabilityContract","STRICTLY_EARLIER_CANONICAL_ROWS");out.put("availableAt",null);
  int a=0,b=0;if(row>=longWindow&&cache.data.consecutive(row,longWindow)){a=leafMaskR31(cache,"freq",position,position,shortWindow,row);b=leafMaskR31(cache,"freqnb",position,position,longWindow,row);}
  int intersection=a&b;out.put("sourceAMask",a);out.put("sourceBMask",b);out.put("intersectionMask",intersection);out.put("sourceCandidates",Integer.bitCount(a|b));out.put("intersectionCandidates",Integer.bitCount(intersection));out.put("reduction",Integer.bitCount(a|b)-Integer.bitCount(intersection));out.put("status",a==0||b==0?"BLOCKED_HISTORY_OR_GAP":Integer.bitCount(intersection)==3?"SUPPORTED_K3":"ABSTAIN_INTERSECTION_LT3");out.put("filledNumbers",0);out.put("coverageLoss",null);out.put("coverageLossReason","REQUIRES_SAME_TARGET_RESEARCH_PAIRING_NOT_ASSUMED");return Collections.unmodifiableMap(out);
 }
}
