package com.ethk3.novaonepro.r952;
import java.util.*;
/** Deterministic formulas used by projection preparation. No DB, clock or statistical authority. */
public final class MatrixMetricsCP11 {
 private MatrixMetricsCP11(){}
 public static double wilsonLower(long hits,long n,double z){
  require(n>0&&hits>=0&&hits<=n&&Double.isFinite(z)&&z>0&&z<=10,"WILSON_SUPPORT");
  double p=(double)hits/n,a=z*z;return (p+a/(2*n)-z*Math.sqrt(p*(1-p)/n+a/(4.0*n*n)))/(1+a/n);
 }
 public static double correlation(long n,double x,double y,double xx,double yy,double xy){
  require(n>=2,"CORRELATION_SUPPORT");finite(x,y,xx,yy,xy);double vx=xx-x*(x/n),vy=yy-y*(y/n);finite(vx,vy);
  require(vx>1e-12&&vy>1e-12,"ZERO_VARIANCE");double cross=xy-x*(y/n);finite(cross);double value=cross/Math.sqrt(vx)/Math.sqrt(vy);finite(value);return Math.max(-1,Math.min(1,value));
 }
 public static double meanVariance(long n,double sum,double sumSq){require(n>0,"VARIANCE_SUPPORT");finite(sum,sumSq);require(sumSq>=0,"NEGATIVE_SECOND_MOMENT");double v=sumSq/n-(sum/n)*(sum/n);finite(v);return Math.max(0,v);}
 public static double pairedSE(long n,double sum,double sumSq){require(n>=2,"PAIRED_SUPPORT");finite(sum,sumSq);require(sumSq>=0,"NEGATIVE_SECOND_MOMENT");double v=(sumSq-sum*(sum/n))/(n-1);finite(v);return Math.sqrt(Math.max(0,v)/n);}
 public static double jaccard(int a,int b){require(Integer.bitCount(a)==3&&Integer.bitCount(b)==3&&(a&~1023)==0&&(b&~1023)==0,"FIXED_K3");return (double)Integer.bitCount(a&b)/Integer.bitCount(a|b);}
 public static double[] smoothed(double[] counts,double alpha){
  require(counts!=null&&counts.length>0&&Double.isFinite(alpha)&&alpha>=0,"HISTOGRAM_INPUT");double sum=alpha*counts.length;
  for(double c:counts){require(Double.isFinite(c)&&c>=0,"NEGATIVE_COUNT");sum+=c;}require(Double.isFinite(sum)&&sum>0,"EMPTY_OR_OVERFLOW_HISTOGRAM");
  double[] p=new double[counts.length];for(int i=0;i<p.length;i++)p[i]=(counts[i]+alpha)/sum;return p;
 }
 public static double js(double[] p,double[] q){probabilities(p);probabilities(q);require(p.length==q.length,"JS_SHAPE");double out=0;
  for(int i=0;i<p.length;i++){double hi=Math.max(p[i],q[i]);if(hi==0)continue;double logM=Math.log(hi)+Math.log1p(Math.min(p[i],q[i])/hi)-Math.log(2);if(p[i]>0)out+=p[i]*(Math.log(p[i])-logM)/2;if(q[i]>0)out+=q[i]*(Math.log(q[i])-logM)/2;}finite(out);return Math.max(0,out);
 }
 public static double psi(double[] p,double[] q){probabilities(p);probabilities(q);require(p.length==q.length,"PSI_SHAPE");double out=0;
  for(int i=0;i<p.length;i++){require(p[i]>0&&q[i]>0,"PSI_NEEDS_SMOOTHING");out+=(p[i]-q[i])*(Math.log(p[i])-Math.log(q[i]));}finite(out);return Math.max(0,out);
 }
 public static double mutualInformation(double[] joint,int rows,int columns){
  require(joint!=null&&rows>0&&columns>0&&((long)rows)*columns==joint.length,"MI_SHAPE");double[] r=new double[rows],c=new double[columns];double n=0;
  for(int i=0;i<rows;i++)for(int j=0;j<columns;j++){double x=joint[i*columns+j];require(Double.isFinite(x)&&x>=0,"MI_COUNT");r[i]+=x;c[j]+=x;n+=x;}require(Double.isFinite(n)&&n>0,"MI_SUPPORT");
  double out=0;for(int i=0;i<rows;i++)for(int j=0;j<columns;j++){double x=joint[i*columns+j];if(x>0)out+=x/n*(Math.log(x)+Math.log(n)-Math.log(r[i])-Math.log(c[j]));}finite(out);return Math.max(0,out);
 }
 public static double ece(long[] count,double[] sumP,double[] sumHit){require(count.length==10&&sumP.length==10&&sumHit.length==10,"ECE_SHAPE");long n=0;double sum=0;
  for(int i=0;i<10;i++){require(count[i]>=0&&Double.isFinite(sumP[i])&&Double.isFinite(sumHit[i])&&sumP[i]>=0&&sumP[i]<=count[i]+1e-9&&sumHit[i]>=0&&sumHit[i]<=count[i],"ECE_BIN");n+=count[i];sum+=Math.abs(sumP[i]-sumHit[i]);}require(n>0,"ECE_SUPPORT");return sum/n;
 }
 public static long nearestRank(long[] values,double quantile){require(values!=null&&values.length>0&&values.length<=300&&quantile>0&&quantile<=1,"QUANTILE_SUPPORT");long[] copy=values.clone();for(long x:copy)require(x>=0,"NEGATIVE_LATENCY");Arrays.sort(copy);return copy[(int)Math.ceil(quantile*copy.length)-1];}
 /** missing slots and ordinal gaps end a streak, and never count as a model miss. */
 public static int maxLossStreak(long[] ordinals,int[] hits){require(ordinals.length==hits.length,"STREAK_SHAPE");int run=0,max=0;
  for(int i=0;i<hits.length;i++){require(hits[i]>=-1&&hits[i]<=1&&(i==0||ordinals[i]>ordinals[i-1]),"STREAK_INPUT");if(i>0&&ordinals[i]!=ordinals[i-1]+1)run=0;run=hits[i]==0?run+1:0;max=Math.max(max,run);}return max;
 }
 /** Lag-one correlation uses edge pairs and separate endpoint sums, not all-row variance. */
 public static double residualLagCorrelation(long[] ordinal,double[] residual){require(ordinal.length==residual.length,"RESIDUAL_SHAPE");long n=0;double x=0,y=0,xx=0,yy=0,xy=0;
  for(int i=1;i<residual.length;i++){require(Double.isFinite(residual[i-1])&&Double.isFinite(residual[i])&&ordinal[i]>ordinal[i-1],"RESIDUAL_INPUT");if(ordinal[i]!=ordinal[i-1]+1)continue;double a=residual[i-1],b=residual[i];n++;x+=a;y+=b;xx+=a*a;yy+=b*b;xy+=a*b;}return correlation(n,x,y,xx,yy,xy);
 }
 private static void probabilities(double[] p){require(p!=null&&p.length>0,"PROBABILITY_SHAPE");double n=0;for(double x:p){require(Double.isFinite(x)&&x>=0&&x<=1,"PROBABILITY_VALUE");n+=x;}require(Math.abs(n-1)<1e-9,"PROBABILITY_SUM");}
 private static void finite(double... values){for(double value:values)require(Double.isFinite(value),"NON_FINITE_MATRIX_STATISTIC");}
 private static void require(boolean b,String code){if(!b)throw new IllegalArgumentException(code);}
}
