package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Poll on inflated input bytes so restoring a checkpoint can span service quanta. */
public final class CooperativeInputStreamR26 extends FilterInputStream {
    private int remaining=4096;
    public CooperativeInputStreamR26(InputStream in){super(in);}
    public int read()throws IOException {if(remaining==0){ExecutionBudgetR26.poll();remaining=4096;}int n=in.read();if(n>=0)remaining--;return n;}
    public int read(byte[] bytes,int offset,int length)throws IOException {if(length==0)return 0;if(remaining==0){ExecutionBudgetR26.poll();remaining=4096;}int n=in.read(bytes,offset,Math.min(length,remaining));if(n>0)remaining-=n;return n;}
}
