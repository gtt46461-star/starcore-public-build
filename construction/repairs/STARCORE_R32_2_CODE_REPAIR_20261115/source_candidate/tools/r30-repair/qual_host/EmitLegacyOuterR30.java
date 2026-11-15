package com.ethk3.novaonepro.strategyv2;
import java.nio.file.*;
import java.util.*;
/** Runs in a separate JVM with the reviewed b225 V1 encoder first on the classpath. */
public final class EmitLegacyOuterR30 {
 public static void main(String[] args)throws Exception{
  AtomNetSessionR30 s=new AtomNetSessionR30(HashingSU1.text("fixture"),"000000000999",999,12,80,40,20);
  s.work(i->{int p=i%10+1;return new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{i%7/7.,i%3/3.},p),i%3==0?1:0,i%10,AtomNetSessionR30.eventKey(String.format(Locale.ROOT,"%012d",100+i/10),p));});
  s.trainingMs=0;byte[] bytes=s.encode();if(java.nio.ByteBuffer.wrap(bytes).getInt()!=0x30A70001)throw new AssertionError("NOT_ORIGINAL_V1_ENCODER");Files.write(Paths.get(args[0]),bytes);
 }
}
