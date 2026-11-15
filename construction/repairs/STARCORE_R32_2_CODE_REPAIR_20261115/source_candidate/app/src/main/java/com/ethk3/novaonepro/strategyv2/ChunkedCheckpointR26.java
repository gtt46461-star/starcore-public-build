package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.security.*;
import java.util.*;
import java.util.zip.*;

/** Bounded streaming states. Content-defined raw boundaries reuse unchanged shifted history. */
public final class ChunkedCheckpointR26 {
    private ChunkedCheckpointR26() {}
    private static final int MAGIC=0x26435302,MIN=16384,MAX=262144,MAX_PARTS=4096;
    private static final long RAW_LIMIT=128L<<20;
    private static final long[] GEAR=new long[256];
    static {long x=0x52626c736563686bL;for(int i=0;i<GEAR.length;i++){x^=x<<13;x^=x>>>7;x^=x<<17;GEAR[i]=x;}}
    public interface Store {String put(byte[] bytes)throws IOException;InputStream open(String hash)throws IOException;}
    public interface StateWriter {void write(DataOutput out)throws IOException;}
    private static MessageDigest digest(){try{return MessageDigest.getInstance("SHA-256");}catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}}
    private static final class Part {final String hash;final int raw;Part(String h,int n){hash=h;raw=n;}}
    private static final class Manifest {long raw;byte[] digest;final List<Part> parts=new ArrayList<>();}
    public static boolean isManifest(byte[] bytes){return bytes!=null&&bytes.length>=4&&((bytes[0]&255)<<24|(bytes[1]&255)<<16|(bytes[2]&255)<<8|bytes[3]&255)==MAGIC;}
    private static Manifest parse(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length>MAX_PARTS*80+64)throw new IOException("R26_CHUNK_MANIFEST_BOUND");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            if(in.readInt()!=MAGIC)throw new IOException("R26_CHUNK_MANIFEST_MAGIC");Manifest m=new Manifest();m.raw=in.readLong();if(m.raw<1||m.raw>RAW_LIMIT)throw new IOException("R26_CHUNK_RAW_BOUND");m.digest=new byte[32];in.readFully(m.digest);int n=in.readInt();if(n<1||n>MAX_PARTS)throw new IOException("R26_CHUNK_COUNT_BOUND");long total=0;
            for(int i=0;i<n;i++){if(in.readUnsignedShort()!=64)throw new IOException("R26_CHUNK_HASH_BOUND");byte[] ascii=new byte[64];in.readFully(ascii);String hash=new String(ascii,java.nio.charset.StandardCharsets.US_ASCII);int raw=in.readInt();if(!hash.matches("[0-9a-f]{64}")||raw<1||raw>MAX)throw new IOException("R26_CHUNK_PART_BOUND");total+=raw;m.parts.add(new Part(hash,raw));}
            if(total!=m.raw||in.read()!=-1)throw new IOException("R26_CHUNK_MANIFEST_SHAPE");return m;
        }
    }
    public static Set<String> children(byte[] bytes)throws IOException {Set<String> out=new TreeSet<>();for(Part p:parse(bytes).parts)out.add(p.hash);return Collections.unmodifiableSet(out);}
    public static byte[] write(StateWriter state,Store store)throws IOException {
        final class ChunkOutput extends OutputStream {
            final byte[] buffer=new byte[MAX];final Manifest manifest=new Manifest();final MessageDigest sha=digest();int used;long gear;
            public void write(int value)throws IOException {
                if(manifest.raw>=RAW_LIMIT)throw new IOException("R26_CHUNK_RAW_BOUND");if((used&4095)==0)ExecutionBudgetR26.poll();buffer[used++]=(byte)value;manifest.raw++;gear=(gear<<1)+GEAR[value&255];
                if(used==MAX||used>=MIN&&(gear&65535)==0)flushPart();
            }
            public void write(byte[] bytes,int offset,int length)throws IOException {if(offset<0||length<0||offset>bytes.length-length)throw new IndexOutOfBoundsException();for(int i=0;i<length;i++)write(bytes[offset+i]);}
            void flushPart()throws IOException {if(used==0)return;if(manifest.parts.size()>=MAX_PARTS)throw new IOException("R26_CHUNK_COUNT_BOUND");ExecutionBudgetR26.poll();sha.update(buffer,0,used);ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)){gzip.write(buffer,0,used);}String hash=store.put(bytes.toByteArray());if(!hash.matches("[0-9a-f]{64}"))throw new IOException("R26_CHUNK_STORE_HASH");manifest.parts.add(new Part(hash,used));used=0;gear=0;}
        }
        ChunkOutput out=new ChunkOutput();state.write(new DataOutputStream(out));out.flushPart();if(out.manifest.raw==0)throw new IOException("R26_CHUNK_EMPTY_STATE");ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream manifest=new DataOutputStream(bytes)){manifest.writeInt(MAGIC);manifest.writeLong(out.manifest.raw);manifest.write(out.sha.digest());manifest.writeInt(out.manifest.parts.size());for(Part p:out.manifest.parts){manifest.writeUTF(p.hash);manifest.writeInt(p.raw);}}return bytes.toByteArray();
    }
    public static InputStream open(byte[] bytes,Store store)throws IOException {
        Manifest manifest=parse(bytes);
        return new InputStream(){
            final MessageDigest sha=digest();int index,partRead,pollRemaining=4096;long total;InputStream part;boolean closed,verified;final byte[] one=new byte[1];
            public int read()throws IOException {int n=read(one,0,1);return n<0?-1:one[0]&255;}
            public int read(byte[] output,int offset,int length)throws IOException {
                if(offset<0||length<0||offset>output.length-length)throw new IndexOutOfBoundsException();if(closed)throw new IOException("R26_CHUNK_READER_CLOSED");if(length==0)return 0;
                for(;;){if(pollRemaining<=0){ExecutionBudgetR26.poll();pollRemaining=4096;}if(part==null){if(index==manifest.parts.size()){if(!verified){if(total!=manifest.raw||!Arrays.equals(sha.digest(),manifest.digest))throw new IOException("R26_CHUNK_STATE_DIGEST");verified=true;}return -1;}Part spec=manifest.parts.get(index);byte[] raw=new byte[spec.raw];try(DataInputStream gzip=new DataInputStream(new GZIPInputStream(store.open(spec.hash)))){gzip.readFully(raw);if(gzip.read()!=-1)throw new IOException("R26_CHUNK_INFLATE_BOUND");}sha.update(raw);part=new ByteArrayInputStream(raw);partRead=0;}
                    int n=part.read(output,offset,Math.min(length,pollRemaining));if(n<0){if(partRead!=manifest.parts.get(index).raw)throw new IOException("R26_CHUNK_PART_LENGTH");part.close();part=null;index++;continue;}
                    partRead+=n;total+=n;pollRemaining-=n;if(partRead>manifest.parts.get(index).raw||total>manifest.raw)throw new IOException("R26_CHUNK_INFLATE_BOUND");return n;
                }
            }
            public void close()throws IOException {closed=true;if(part!=null)part.close();}
        };
    }
}
