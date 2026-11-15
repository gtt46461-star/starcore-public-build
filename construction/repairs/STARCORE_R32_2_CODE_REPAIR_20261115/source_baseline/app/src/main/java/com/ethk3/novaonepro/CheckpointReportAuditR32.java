package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.ChunkedCheckpointR26;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.function.BooleanSupplier;
import org.json.JSONObject;

/** Read-only diagnostic observation, never a GC mark or a model repair.
 * A missing/corrupt referenced object is report data. Unknown children remain
 * unknown; strict raw backups and the original fail-closed GC are unchanged. */
public final class CheckpointReportAuditR32 {
    private CheckpointReportAuditR32(){}
    public static final String MEMBER="r32-checkpoint-observations.jsonl";
    private static final int MAX_NODES=16384,MAX_OBJECT=32<<20,MAX_MANIFEST=4096*80+64;
    private static final long BYTE_BUDGET=8L<<20;
    private static void check(BooleanSupplier continuing)throws IOException{if(Thread.currentThread().isInterrupted()||!continuing.getAsBoolean())throw new InterruptedIOException("REPORT_CHECKPOINT_AUDIT_CANCELLED");}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder(bytes.length*2);for(byte b:bytes)out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return out.toString();}
    private static void line(Writer out,JSONObject value)throws Exception{out.write(value.toString());out.write('\n');}
    public static JSONObject observe(File directory,Set<String> roots,Writer out,BooleanSupplier continuing)throws Exception{
        Objects.requireNonNull(roots);Objects.requireNonNull(out);Objects.requireNonNull(continuing);
        File parent=directory.getAbsoluteFile().getParentFile();if(parent==null)throw new IOException("REPORT_CHECKPOINT_AUDIT_ROOT");
        File root=new File(parent.getCanonicalFile(),directory.getName());
        if(!root.equals(root.getCanonicalFile()))throw new IOException("REPORT_CHECKPOINT_SOURCE_SYMLINK");
        Deque<String> work=new ArrayDeque<>();Set<String> queued=new HashSet<>();boolean truncated=false;
        for(String hash:new TreeSet<>(roots)){
            if(hash==null||!hash.matches("[a-f0-9]{64}"))throw new IOException("REPORT_CHECKPOINT_REFERENCE_HASH");
            if(queued.size()>=MAX_NODES){truncated=true;break;}queued.add(hash);work.add(hash);
        }
        long remaining=BYTE_BUDGET,objects=0,bytes=0,verified=0,missing=0,corrupt=0,unreadable=0,notRun=0,unknownEdges=0;
        while(!work.isEmpty()){
            check(continuing);String hash=work.removeFirst();File file=new File(root,hash+".bin");
            String status;String observedHash="";long size=0;Set<String> children=null;
            if(!file.getAbsoluteFile().equals(file.getCanonicalFile())){status="SYMLINK_REFUSED";unreadable++;}
            else if(!file.isFile()){status="MISSING_OBJECT";missing++;}
            else if((size=file.length())<1||size>MAX_OBJECT){status="INVALID_SIZE";corrupt++;}
            else if(size>remaining){status="NOT_RUN_COMPACT_BYTE_BUDGET";notRun++;}
            else{
                try{
                    MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[32768],header=new byte[4];int head=0;long count=0;
                    ByteArrayOutputStream manifest=size<=MAX_MANIFEST?new ByteArrayOutputStream((int)size):null;
                    try(InputStream in=new FileInputStream(file)){
                        int n;while((n=in.read(buffer,0,(int)Math.min(buffer.length,remaining+1)))!=-1){
                            check(continuing);if(n>remaining)throw new IOException("REPORT_CHECKPOINT_AUDIT_GREW_BEYOND_BUDGET");remaining-=n;count+=n;digest.update(buffer,0,n);
                            if(head<4){int take=Math.min(n,4-head);System.arraycopy(buffer,0,header,head,take);head+=take;}
                            if(manifest!=null){if(count>MAX_MANIFEST)manifest=null;else manifest.write(buffer,0,n);}
                        }
                    }
                    observedHash=hex(digest.digest());
                    if(count!=size||file.length()!=size){status="CHANGED_DURING_AUDIT";unreadable++;}
                    else if(!hash.equals(observedHash)){status="CORRUPT_HASH_MISMATCH";corrupt++;}
                    else if(ChunkedCheckpointR26.isManifest(header)){
                        if(manifest==null)throw new IOException("REPORT_CHECKPOINT_MANIFEST_BOUND");
                        children=ChunkedCheckpointR26.children(manifest.toByteArray());status="VERIFIED_MANIFEST";verified++;
                    }else if(head>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53){status="UNKNOWN_MANIFEST_VERSION";corrupt++;}
                    else{children=Collections.emptySet();status="VERIFIED_BYTES";verified++;}
                }catch(InterruptedIOException cancelled){throw cancelled;}
                catch(FileNotFoundException absent){status="MISSING_DURING_READ";missing++;}
                catch(IOException failed){status="UNREADABLE_OR_INVALID_MANIFEST";unreadable++;}
            }
            objects++;bytes=Math.addExact(bytes,size);if(children==null)unknownEdges++;
            JSONObject row=new JSONObject().put("kind","checkpoint_object").put("expectedSha256",hash).put("relativePath","r26-checkpoints/"+hash+".bin")
                    .put("observedBytes",size).put("availability",status).put("bodyIncluded",false).put("childrenKnown",children!=null).put("directRoot",roots.contains(hash));
            if(!observedHash.isEmpty())row.put("observedSha256",observedHash);line(out,row);
            if(children!=null)for(String child:children){
                if(queued.contains(child))continue;if(queued.size()>=MAX_NODES){truncated=true;continue;}queued.add(child);work.addLast(child);
            }
        }
        boolean closure=!truncated&&unknownEdges==0;
        JSONObject summary=new JSONObject().put("format","R32_CHECKPOINT_REFERENCE_AUDIT_V1").put("snapshotBound",true).put("readOnly",true)
                .put("state",missing+corrupt+unreadable>0?"CHECKPOINT_EVIDENCE_GAPS":!closure?"CHECKPOINT_VERIFICATION_INCOMPLETE":roots.isEmpty()?"NO_REFERENCED_CHECKPOINT_OBJECTS":"VERIFIED_REFERENCED_CHECKPOINT_CLOSURE")
                .put("referenceRoots",roots.size()).put("observedObjects",objects).put("observedBytes",bytes).put("verifiedObjects",verified).put("missingObjects",missing)
                .put("corruptObjects",corrupt).put("unreadableObjects",unreadable).put("byteVerificationNotRun",notRun).put("unknownChildSets",unknownEdges).put("truncated",truncated)
                .put("sourceClosureVerified",closure).put("sourceRootsComplete",!truncated).put("compactByteBudget",BYTE_BUDGET).put("bytesRead",BYTE_BUDGET-remaining)
                .put("metadataMember",MEMBER).put("rawBodiesIncluded",false).put("gcOrModelUseAuthorized",false)
                .put("boundary","Diagnostic inventory only; missing objects and unknown children are not healthy, restored, exported, or a usable GC mark.");
        line(out,new JSONObject().put("kind","checkpoint_audit_summary").put("summary",summary));return summary;
    }
}
