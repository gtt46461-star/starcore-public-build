package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;

/** Lossless, hash-checked transport; experiment meaning and gate thresholds are unchanged. */
public final class FactoryStateCodecR31 {
    private FactoryStateCodecR31() {}
    public static final String VERSION="R31_FACTORY_GZIP_CELL_1";
    public static final int MAX_CELL_BYTES=512*1024,MAX_RAW_BYTES=8*1024*1024;
    private static MemoryBudgetR31.Lease workspace(long bytes){
        MemoryBudgetR31.Admission a=MemoryBudgetR31.processBudget().tryAcquire("r31-factory-codec",MemoryBudgetR31.Kind.CHECKPOINT,bytes,()->Thread.currentThread().isInterrupted());
        if(!a.acquired())throw new MemoryBudgetR31.CapacityWait(a.reason);return a.lease;
    }
    private static void capacity(){throw new MemoryBudgetR31.CapacityWait(MemoryBudgetR31.WaitReason.DECLARED_CAPACITY);}
    public static String encode(String original){
        if(original==null||original.length()>MAX_RAW_BYTES)capacity();
        try(MemoryBudgetR31.Lease ignored=workspace(4L*Math.min(MAX_RAW_BYTES,3L*original.length())+6L*MAX_CELL_BYTES+65536)){
            byte[] raw=original.getBytes(StandardCharsets.UTF_8);if(raw.length>MAX_RAW_BYTES)capacity();
            ByteArrayOutputStream compressed=new ByteArrayOutputStream(){
                @Override public synchronized void write(byte[] b,int off,int len){if((long)count+len>360*1024)capacity();super.write(b,off,len);}
                @Override public synchronized void write(int b){if(count>=360*1024)capacity();super.write(b);}
            };
            try(GZIPOutputStream gzip=new GZIPOutputStream(compressed)){for(int at=0;at<raw.length;at+=8192){ExecutionBudgetR26.poll();gzip.write(raw,at,Math.min(8192,raw.length-at));}}
            Map<String,Object> record=new LinkedHashMap<>();record.put("codec",VERSION);record.put("rawBytes",raw.length);record.put("sha256",StatsR27.sha256(raw));record.put("gzipBase64",Base64.getEncoder().encodeToString(compressed.toByteArray()));
            String encoded=JsonR27.write(record);if(encoded.getBytes(StandardCharsets.UTF_8).length>MAX_CELL_BYTES)capacity();return encoded;
        }catch(IOException failure){throw new IllegalStateException("FACTORY_STATE_ENCODE",failure);}
    }
    @SuppressWarnings("unchecked") public static String decode(String encoded){
        if(encoded==null)return null;if(encoded.length()>MAX_CELL_BYTES)capacity();
        Map<String,Object> record=(Map<String,Object>)JsonR27.parse(encoded);
        // Existing small inline checkpoints stay readable; their bytes are not rewritten on read.
        if(!record.containsKey("codec"))return encoded;
        if(!VERSION.equals(record.get("codec")))throw new IllegalArgumentException("FACTORY_STATE_CODEC_UNKNOWN");
        Object count=record.get("rawBytes"),hash=record.get("sha256"),payload=record.get("gzipBase64");
        if(!(count instanceof Number)||!(hash instanceof String)||!(payload instanceof String))throw new IllegalArgumentException("FACTORY_STATE_HEADER");
        long size=((Number)count).longValue();if(size<1||size>MAX_RAW_BYTES||((Number)count).doubleValue()!=size||((String)payload).length()>MAX_CELL_BYTES)throw new IllegalArgumentException("FACTORY_STATE_BOUND");
        try(MemoryBudgetR31.Lease ignored=workspace(5*size+6L*MAX_CELL_BYTES+65536)){
            byte[] compressed=Base64.getDecoder().decode((String)payload);ByteArrayOutputStream raw=new ByteArrayOutputStream((int)size);
            try(GZIPInputStream input=new GZIPInputStream(new ByteArrayInputStream(compressed))){byte[] buffer=new byte[8192];int n;while((n=input.read(buffer))!=-1){ExecutionBudgetR26.poll();if((long)raw.size()+n>size)throw new IllegalArgumentException("FACTORY_STATE_INFLATE_BOUND");raw.write(buffer,0,n);}}
            byte[] bytes=raw.toByteArray();if(bytes.length!=size||!hash.equals(StatsR27.sha256(bytes)))throw new IllegalArgumentException("FACTORY_STATE_HASH");String text=new String(bytes,StandardCharsets.UTF_8);if(!Arrays.equals(bytes,text.getBytes(StandardCharsets.UTF_8)))throw new IllegalArgumentException("FACTORY_STATE_UTF8");return text;
        }catch(IOException failure){throw new IllegalArgumentException("FACTORY_STATE_CORRUPT",failure);}
    }
}
