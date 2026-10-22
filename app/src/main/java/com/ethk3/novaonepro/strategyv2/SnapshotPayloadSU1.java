package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import com.ethk3.novaonepro.r952.StrictCanonicalJson;

/** Content-addressed 64-period chunks avoid storing the same 5,000 draws every minute. */
public final class SnapshotPayloadSU1 {
    public final String manifest;
    public final Map<String,String> chunks;
    private SnapshotPayloadSU1(String manifest,Map<String,String> chunks){this.manifest=manifest;this.chunks=Collections.unmodifiableMap(chunks);}
    private static String compress(String text){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(GZIPOutputStream gzip=new GZIPOutputStream(bytes)){gzip.write(text.getBytes(StandardCharsets.UTF_8));}return Base64.getEncoder().encodeToString(bytes.toByteArray());}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    public static SnapshotPayloadSU1 prepare(EventFrameSU1 frame){
        Map<String,String> chunks=new LinkedHashMap<>();List<String> order=new ArrayList<>();StringBuilder current=new StringBuilder();long block=-1;
        for(String line:frame.payload().split("\n")){
            long next=Long.parseLong(line.substring(0,line.indexOf('@')))/64;
            if(block!=-1&&block!=next){String payload=compress(current.toString()),hash=HashingSU1.text(payload);chunks.put(hash,payload);order.add(hash);current.setLength(0);}block=next;current.append(line).append('\n');
        }
        if(current.length()>0){String payload=compress(current.toString()),hash=HashingSU1.text(payload);chunks.put(hash,payload);order.add(hash);}
        Map<String,Object> manifest=new TreeMap<>();manifest.put("format","SU1_PIT_CHUNKS64_V1");manifest.put("chunks",order);manifest.put("rows",frame.size());manifest.put("source",frame.sourceVersionHash);manifest.put("frame",frame.contentHash);
        return new SnapshotPayloadSU1(StrictCanonicalJson.writeContext(manifest),chunks);
    }
}
