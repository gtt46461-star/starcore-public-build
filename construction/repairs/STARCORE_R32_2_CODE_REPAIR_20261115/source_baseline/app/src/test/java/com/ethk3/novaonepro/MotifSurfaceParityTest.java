package com.ethk3.novaonepro;

import java.util.Random;
import org.junit.Test;
import static org.junit.Assert.*;

/** Independent oracle retained from the shipped scalar estimator. */
public final class MotifSurfaceParityTest {
    @Test public void allCellsMatchShippedEstimatorExactly() {
        for (int size : new int[]{0,15,16,25,26,49,50,97,98,720,5000}) {
            V51Models.DrawFrame[] h=history(size,741L+size,false);
            double[][] actual=new V51MotifMatrixProfileEngine().scoreSurface(h);
            for(int p=0;p<10;p++) for(int n=1;n<=10;n++)
                assertEquals("size="+size+" position="+p+" number="+n,
                    Double.doubleToLongBits(original(h,p,n)),Double.doubleToLongBits(actual[p][n-1]));
        }
    }
    @Test public void equalDistancesRetainFirstSuccessorAndCallsAreIsolated() {
        V51Models.DrawFrame[] repeated=history(200,9,true),other=history(200,19,false);
        V51MotifMatrixProfileEngine engine=new V51MotifMatrixProfileEngine();
        double[][] first=engine.scoreSurface(repeated);engine.scoreSurface(other);
        double[][] next=engine.scoreSurface(repeated);
        for(int p=0;p<10;p++)for(int n=1;n<=10;n++) {
            assertEquals(Double.doubleToLongBits(original(repeated,p,n)),Double.doubleToLongBits(first[p][n-1]));
            assertEquals(Double.doubleToLongBits(first[p][n-1]),Double.doubleToLongBits(next[p][n-1]));
        }
        first[0][0]=999;assertNotEquals(999,engine.scoreSurface(repeated)[0][0],0);
        assertEquals(0,engine.scoreSurface(null)[0][0],0);
    }
    static V51Models.DrawFrame[] history(int size,long seed,boolean repeat) {
        Random random=new Random(seed);V51Models.DrawFrame[] out=new V51Models.DrawFrame[size];
        for(int i=0;i<size;i++) {
            int[] p={1,2,3,4,5,6,7,8,9,10};
            if(!repeat)for(int j=9;j>0;j--){int k=random.nextInt(j+1),v=p[j];p[j]=p[k];p[k]=v;}
            out[i]=new V51Models.DrawFrame("fixture"+i,p,i+1);
        }return out;
    }
    static double original(V51Models.DrawFrame[] h,int position,int number) {
        if(h==null||h.length<16)return 0;
        double total=0,weight=0;
        for(int length:new int[]{6,12,24,48}) {
            if(h.length<length*2+2)continue;
            double best=Double.POSITIVE_INFINITY;int end=-1,query=h.length-length;
            for(int start=Math.max(0,h.length-720);start+length<h.length-Math.max(2,length/2);start++) {
                double sum=0;int count=0;
                for(int t=0;t<length;t++) {
                    int[] a=inverse(h[start+t].permutation),b=inverse(h[query+t].permutation);
                    for(int n=1;n<=10;n++){double d=(a[n]-b[n])/9.0d;sum+=d*d;count++;}
                }
                double d=Math.sqrt(sum/Math.max(1,count));
                if(d<best){best=d;end=start+length;}
            }
            if(end>=0&&end<h.length){double w=Math.exp(-best*2)*Math.sqrt(length);
                total+=w*((h[end].permutation[position]==number?1.0d:0.0d)-0.1d);weight+=w;}
        }return weight==0?0:total/weight;
    }
    private static int[] inverse(int[] p){int[] a=new int[11];for(int i=0;i<10;i++)a[p[i]]=i;return a;}
}
