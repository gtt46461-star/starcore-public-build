package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Poll while serializing raw state, including highly compressible arrays with little gzip output. */
public final class CooperativeOutputStreamR26 extends FilterOutputStream {
    private int remaining=4096;
    public CooperativeOutputStreamR26(OutputStream out){super(out);}
    public void write(int value)throws IOException {if(remaining==0){ExecutionBudgetR26.poll();remaining=4096;}out.write(value);remaining--;}
    public void write(byte[] bytes,int offset,int length)throws IOException {
        if(offset<0||length<0||offset>bytes.length-length)throw new IndexOutOfBoundsException();
        while(length>0){if(remaining==0){ExecutionBudgetR26.poll();remaining=4096;}int n=Math.min(length,remaining);out.write(bytes,offset,n);offset+=n;length-=n;remaining-=n;}
    }
}
