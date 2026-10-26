package com.ethk3.novaonepro.strategyv2;

import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;

/** Executable numerical reference, not an Android integration or a claimed live strategy.
 * SU1-RELSTATE10-REFERENCE-V1, float64, WINDOW_RESET.
 * Production must load authenticated weights and PIT history through the SU1 adapters.
 */
public final class RelationalStateInference {
    private RelationalStateInference() {}
    public static final class Weights {
        final int d;
        final double[][] e, u, r, wx, wm, wh, wo;
        final double[] b, c;
        public Weights(double[][] e,double[][] u,double[][] r,double[][] wx,
                double[][] wm,double[][] wh,double[] b,double[][] wo,double[] c) {
            if(b==null||b.length<2||b.length>64)throw new IllegalArgumentException("WIDTH");
            this.d=b.length;
            this.e=matrix(e,10,d);this.u=matrix(u,10,d);this.r=matrix(r,10,10);
            this.wx=matrix(wx,d,d);this.wm=matrix(wm,d,d);this.wh=matrix(wh,d,d);
            this.b=vector(b,d);this.wo=matrix(wo,d,10);this.c=vector(c,10);
        }
    }
    public interface Trace { void layer(String name,int step,double[][] values); }
    public static double[][] predict(Weights w,int[][] history) {return predict(w,history,null);}
    public static double[][] predict(Weights w,int[][] history,Trace trace) {
        if(w==null||history==null||history.length<1||history.length>256)
            throw new IllegalArgumentException("HISTORY");
        double[][] a=new double[10][];
        for(int p=0;p<10;p++)a[p]=softmax(w.r[p]);
        if(trace!=null)trace.layer("relation",-1,a);
        double[][] h=new double[10][w.d];
        int step=0;
        for(int[] draw:history) {
            validateDraw(draw);
            double[][] z=new double[10][w.d],m=new double[10][w.d],next=new double[10][w.d];
            for(int p=0;p<10;p++)for(int j=0;j<w.d;j++)z[p][j]=w.e[draw[p]-1][j]+w.u[p][j];
            for(int p=0;p<10;p++)for(int q=0;q<10;q++)for(int j=0;j<w.d;j++)m[p][j]+=a[p][q]*z[q][j];
            for(int p=0;p<10;p++)for(int j=0;j<w.d;j++) {
                double v=w.b[j];
                for(int k=0;k<w.d;k++)v+=z[p][k]*w.wx[k][j]+m[p][k]*w.wm[k][j]+h[p][k]*w.wh[k][j];
                if(!Double.isFinite(v))throw new IllegalArgumentException("NONFINITE_STATE");
                next[p][j]=Math.tanh(v);
            }
            h=next;
            if(trace!=null){trace.layer("embedding",step,z);trace.layer("message",step,m);trace.layer("state",step,h);}step++;
        }
        double[][] probabilities=new double[10][10];
        for(int p=0;p<10;p++){
            double[] scores=w.c.clone();
            for(int n=0;n<10;n++)for(int k=0;k<w.d;k++)scores[n]+=h[p][k]*w.wo[k][n];
            probabilities[p]=softmax(scores);
        }
        if(trace!=null)trace.layer("probability",step,probabilities);
        return probabilities;
    }
    /** One-based position followed by three one-based numbers; stable exact ties. */
    public static int[] action(double[][] probabilities) {
        double[][] p=matrix(probabilities,10,10);
        int[] selected=null;double best=-1;
        for(int pos=0;pos<10;pos++){
            double total=0;for(double v:p[pos]){if(v<0||v>1)throw new IllegalArgumentException("PROB_RANGE");total+=v;}
            if(Math.abs(total-1)>1e-10)throw new IllegalArgumentException("PROB_SUM");
            final double[] row=p[pos];Integer[] order=new Integer[10];
            for(int i=0;i<10;i++)order[i]=i;
            Arrays.sort(order,(x,y)->{int q=Double.compare(row[y],row[x]);return q!=0?q:Integer.compare(x,y);});
            double mass=row[order[0]]+row[order[1]]+row[order[2]];
            if(mass>best){best=mass;selected=new int[]{pos+1,order[0]+1,order[1]+1,order[2]+1};}
        }
        return selected;
    }
    public static void validateDraw(int[] values){
        if(values==null||values.length!=10)throw new IllegalArgumentException("DRAW_SHAPE");
        int mask=0;for(int v:values){if(v<1||v>10||(mask&(1<<(v-1)))!=0)throw new IllegalArgumentException("DRAW_PERMUTATION");mask|=1<<(v-1);}
    }
    private static double[] softmax(double[] x){
        double max=-Double.MAX_VALUE;for(double v:x){if(!Double.isFinite(v))throw new IllegalArgumentException("NONFINITE");max=Math.max(max,v);}
        double[] out=new double[x.length];double sum=0;for(int i=0;i<x.length;i++){out[i]=Math.exp(x[i]-max);sum+=out[i];}
        for(int i=0;i<x.length;i++)out[i]/=sum;return out;
    }
    private static double[] vector(double[] x,int n){
        if(x==null||x.length!=n)throw new IllegalArgumentException("VECTOR_SHAPE");
        double[] copy=x.clone();for(double v:copy)if(!Double.isFinite(v))throw new IllegalArgumentException("NONFINITE");return copy;
    }
    private static double[][] matrix(double[][] x,int rows,int cols){
        if(x==null||x.length!=rows)throw new IllegalArgumentException("MATRIX_SHAPE");
        double[][] copy=new double[rows][];for(int i=0;i<rows;i++)copy[i]=vector(x[i],cols);return copy;
    }
    private static double[][] readMatrix(Scanner s,int rows,int cols){double[][] x=new double[rows][cols];for(int r=0;r<rows;r++)for(int c=0;c<cols;c++)x[r][c]=s.nextDouble();return x;}
    private static double[] readVector(Scanner s,int n){double[] x=new double[n];for(int i=0;i<n;i++)x[i]=s.nextDouble();return x;}
    /** Test fixture transport only: d,T; E,U,R,Wx,Wm,Wh,b,Wo,c; T legal draws. */
    public static void main(String[] args){
        try(Scanner s=new Scanner(System.in).useLocale(Locale.ROOT)){
            int d=s.nextInt(),t=s.nextInt();
            if(d<2||d>64||t<1||t>256)throw new IllegalArgumentException("FIXTURE_BUDGET");
            Weights w=new Weights(readMatrix(s,10,d),readMatrix(s,10,d),readMatrix(s,10,10),
                    readMatrix(s,d,d),readMatrix(s,d,d),readMatrix(s,d,d),readVector(s,d),readMatrix(s,d,10),readVector(s,10));
            int[][] history=new int[t][10];for(int i=0;i<t;i++)for(int p=0;p<10;p++)history[i][p]=s.nextInt();
            if(s.hasNext())throw new IllegalArgumentException("TRAILING_INPUT");
            double[][] out=predict(w,history);for(double[] row:out){for(double v:row)System.out.print(Double.toString(v)+" ");System.out.println();}
            int[] selected=action(out);for(int x:selected)System.out.print(x+" ");System.out.println();
        }
    }
}
