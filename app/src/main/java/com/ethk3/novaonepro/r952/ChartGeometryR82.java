package com.ethk3.novaonepro.r952;
import java.util.*;
/** Pure bounded coordinates; absent observations remain gaps. */
public final class ChartGeometryR82 {
 public static final int MAX_POINTS=120,MAX_NODES=64;private ChartGeometryR82(){}
 public static boolean finite(double v){return !Double.isNaN(v)&&!Double.isInfinite(v);}
 public static double[] range(double[] values){double lo=Double.POSITIVE_INFINITY,hi=Double.NEGATIVE_INFINITY;for(double v:values)if(finite(v)){lo=Math.min(lo,v);hi=Math.max(hi,v);}if(!finite(lo))return new double[]{0,1};if(lo==hi){double pad=Math.max(.05,Math.abs(lo)*.05);lo-=pad;hi+=pad;}return new double[]{lo,hi};}
 public static float x(int i,int n,float left,float right){return n<=1?(left+right)/2:left+(right-left)*i/(n-1f);}
 public static float y(double v,double lo,double hi,float top,float bottom){if(!finite(v)||!finite(lo)||!finite(hi)||hi<=lo)return Float.NaN;return (float)(bottom-(bottom-top)*(v-lo)/(hi-lo));}
 public static int nearest(float x,int n,float left,float right){if(n<=0)return -1;if(n==1||right<=left)return 0;return Math.max(0,Math.min(n-1,Math.round((x-left)*(n-1)/(right-left))));}
 public static Map<String,float[]> graph(List<String> names,float width,float height){if(names.size()>MAX_NODES)throw new IllegalArgumentException("GRAPH_NODE_BUDGET");Map<String,float[]> out=new LinkedHashMap<>();for(int i=0;i<names.size();i++){double a=2*Math.PI*i/Math.max(1,names.size())-Math.PI/2;out.put(names.get(i),new float[]{width*.5f+(float)Math.cos(a)*width*.36f,height*.5f+(float)Math.sin(a)*height*.33f});}return Collections.unmodifiableMap(out);}
 public static double[] wilson(long hits,long n){if(n<=0||hits<0||hits>n)return new double[]{Double.NaN,Double.NaN};double z=1.959963984540054,p=hits/(double)n,d=1+z*z/n,c=(p+z*z/(2*n))/d,h=z*Math.sqrt(p*(1-p)/n+z*z/(4d*n*n))/d;return new double[]{Math.max(0,c-h),Math.min(1,c+h)};}
}
