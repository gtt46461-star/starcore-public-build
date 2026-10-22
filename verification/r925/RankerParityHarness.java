package com.ethk3.novaonepro;
import java.io.*;
public final class RankerParityHarness {
    static double[] row(String text){String[] v=text.split("\t");double[] a=new double[v.length];for(int i=0;i<v.length;i++)a[i]=Double.parseDouble(v[i]);return a;}
    public static void main(String[] args)throws Exception{
        try(BufferedReader r=new BufferedReader(new FileReader(args[0]));PrintWriter w=args.length>1?new PrintWriter(args[1]):new PrintWriter(System.out)){
            double[] m=row(r.readLine()),s=row(r.readLine()),v=row(r.readLine());double b=Double.parseDouble(r.readLine());String line;
            while((line=r.readLine())!=null)w.println(RankerMathR925.score(row(line),m,s,v,b));
        }
    }
}
