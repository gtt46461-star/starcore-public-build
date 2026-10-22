package com.ethk3.novaonepro.r952;
import java.util.Arrays;
import java.io.*;
import java.security.MessageDigest;
/** Compact exact rolling counts used by the research-owned long-scale summary. */
public final class LongScaleCountsR80 {
 private static final int[] WINDOWS={1440,10080,43200};
 public static int[] windows(){return WINDOWS.clone();}
 private static final int MAX=43200,POSITIONS=10;
 private final byte[] ring=new byte[MAX*POSITIONS];
 private final long[][][] counts=new long[3][POSITIONS][10];
 private int next,size;private long ordinal=-1;
 public void append(long index,int[] digits){
  if(index<0||index==Long.MAX_VALUE||ordinal>=0&&index!=ordinal+1||digits==null||digits.length!=10)throw new IllegalArgumentException("LONG_SCALE_SEQUENCE");
  int mask=0;for(int digit:digits){if(digit<0||digit>9||(mask&(1<<digit))!=0)throw new IllegalArgumentException("LONG_SCALE_PERMUTATION");mask|=1<<digit;}
  // All validation before mutation, and subtract every outgoing item before overwriting the ring cell.
  for(int s=0;s<3;s++)for(int p=0;p<10;p++){
   if(size>=WINDOWS[s]){int old=Math.floorMod(next-WINDOWS[s],MAX);counts[s][p][ring[old*10+p]]--;}
   counts[s][p][digits[p]]++;
  }
  for(int p=0;p<10;p++)ring[next*10+p]=(byte)digits[p];next=(next+1)%MAX;size=Math.min(MAX,size+1);ordinal=index;
 }
 private static int scale(int window){for(int i=0;i<WINDOWS.length;i++)if(WINDOWS[i]==window)return i;throw new IllegalArgumentException("LONG_SCALE_WINDOW");}
 public int support(int window){scale(window);return Math.min(size,window);}
 public boolean ready(int window){return support(window)==window;}
 public long[] counts(int position,int window){if(position<1||position>10)throw new IllegalArgumentException("LONG_SCALE_POSITION");return counts[scale(window)][position-1].clone();}
 public double frequency(int position,int window,int digit){if(digit<0||digit>9)throw new IllegalArgumentException("LONG_SCALE_DIGIT");if(!ready(window))throw new IllegalStateException("WAIT_LONG_SCALE_HISTORY");return counts(position,window)[digit]/(double)window;}
 public long lastOrdinal(){return ordinal;}
 public long primitivePayloadBytes(){return ring.length+3L*10L*10L*8L;}
 /** Detached checkpoint bytes. The durable owner must bind this exact digest to
  * its epoch/generation/source receipt in the same transaction as publication. */
 public byte[] checkpoint(){
  try{ByteArrayOutputStream bytes=new ByteArrayOutputStream(CHECKPOINT_BYTES);DataOutputStream out=new DataOutputStream(bytes);
   out.writeLong(MAGIC);out.writeInt(1);out.writeInt(next);out.writeInt(size);out.writeLong(ordinal);out.write(ring);
   for(int s=0;s<3;s++)for(int p=0;p<10;p++)for(int d=0;d<10;d++)out.writeLong(counts[s][p][d]);
   out.flush();byte[] result=bytes.toByteArray();if(result.length!=CHECKPOINT_BYTES)throw new AssertionError("LONG_SCALE_CODEC_LENGTH");return result;
  }catch(IOException impossible){throw new AssertionError(impossible);}
 }
 private static final long MAGIC=0x53434C4F4E473830L;
 public static final int CHECKPOINT_BYTES=28+432000+2400;
 public static String checkpointHash(byte[] payload){
  if(payload==null||payload.length!=CHECKPOINT_BYTES)throw new IllegalArgumentException("LONG_SCALE_CODEC_LENGTH");
  try{byte[] hash=MessageDigest.getInstance("SHA-256").digest(payload);StringBuilder out=new StringBuilder(64);for(byte b:hash){out.append("0123456789abcdef".charAt((b&255)>>>4));out.append("0123456789abcdef".charAt(b&15));}return out.toString();}
  catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
 }
 /** Restart validates the complete compact ring and independently rebuilds all
  * window counters. A corrupt checkpoint never becomes a partially warm state. */
 public static LongScaleCountsR80 restore(byte[] payload,String expectedHash){
  if(expectedHash==null||!expectedHash.matches("[0-9a-f]{64}")||!checkpointHash(payload).equals(expectedHash))throw new IllegalArgumentException("LONG_SCALE_CODEC_HASH");
  LongScaleCountsR80 restored=new LongScaleCountsR80();
  try{DataInputStream in=new DataInputStream(new ByteArrayInputStream(payload));
   if(in.readLong()!=MAGIC||in.readInt()!=1)throw new IllegalArgumentException("LONG_SCALE_CODEC_REVISION");
   restored.next=in.readInt();restored.size=in.readInt();restored.ordinal=in.readLong();
   if(restored.next<0||restored.next>=MAX||restored.size<0||restored.size>MAX
      ||restored.size<MAX&&restored.next!=restored.size
      ||restored.size==0&&(restored.ordinal!=-1||restored.next!=0)
      ||restored.size>0&&(restored.ordinal<restored.size-1L||restored.ordinal==Long.MAX_VALUE))throw new IllegalArgumentException("LONG_SCALE_CODEC_CURSOR");
   in.readFully(restored.ring);
   for(int slot=0;slot<MAX;slot++){
    if(restored.size<MAX&&slot>=restored.size){for(int p=0;p<10;p++)if(restored.ring[slot*10+p]!=0)throw new IllegalArgumentException("LONG_SCALE_CODEC_UNUSED");continue;}
    int mask=0;for(int p=0;p<10;p++){int digit=restored.ring[slot*10+p]&255;if(digit>9||(mask&(1<<digit))!=0)throw new IllegalArgumentException("LONG_SCALE_CODEC_PERMUTATION");mask|=1<<digit;}
   }
   for(int s=0;s<3;s++){
    int support=Math.min(restored.size,WINDOWS[s]),first=Math.floorMod(restored.next-support,MAX);
    for(int i=0;i<support;i++){int slot=(first+i)%MAX;for(int p=0;p<10;p++)restored.counts[s][p][restored.ring[slot*10+p]]++;}
    for(int p=0;p<10;p++)for(int d=0;d<10;d++)if(in.readLong()!=restored.counts[s][p][d])throw new IllegalArgumentException("LONG_SCALE_CODEC_COUNTER");
   }
   if(in.read()!=-1||!Arrays.equals(restored.checkpoint(),payload))throw new IllegalArgumentException("LONG_SCALE_CODEC_NONCANONICAL");return restored;
  }catch(IOException malformed){throw new IllegalArgumentException("LONG_SCALE_CODEC_TRUNCATED",malformed);}
 }
 /** Owner-side preparation copy; no mutable ring/counter reference escapes. */
 public LongScaleCountsR80 copy(){LongScaleCountsR80 c=new LongScaleCountsR80();System.arraycopy(ring,0,c.ring,0,ring.length);for(int s=0;s<3;s++)for(int p=0;p<10;p++)System.arraycopy(counts[s][p],0,c.counts[s][p],0,10);c.next=next;c.size=size;c.ordinal=ordinal;return c;}
}
