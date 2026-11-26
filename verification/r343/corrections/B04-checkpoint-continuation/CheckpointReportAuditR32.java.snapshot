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
    private static final int MAX_NODES=CheckpointAuditContinuationR343.MAX_NODES,MAX_OBJECT=32<<20,MAX_MANIFEST=4096*80+64;
    private static final long BYTE_BUDGET=8L<<20;
    private static void check(BooleanSupplier continuing)throws IOException{if(Thread.currentThread().isInterrupted()||!continuing.getAsBoolean())throw new InterruptedIOException("REPORT_CHECKPOINT_AUDIT_CANCELLED");}
    private static String hex(byte[] bytes){StringBuilder out=new StringBuilder(bytes.length*2);for(byte b:bytes)out.append(Character.forDigit((b>>>4)&15,16)).append(Character.forDigit(b&15,16));return out.toString();}
    private static void line(Writer out,JSONObject value)throws Exception{out.write(value.toString());out.write('\n');}
    /** Collect only a bounded prefix of unique direct roots. The source cursor
     * is streamed; omitted roots are explicit incompleteness, never a full mark. */
    public static JSONObject observe(File directory,com.ethk3.novaonepro.strategyv2.CheckpointClosureR324.RootSource source,Writer out,BooleanSupplier continuing)throws Exception{
        return observePage(directory,source,out,continuing,null,null,null,null);
    }
    static JSONObject observePage(File directory,com.ethk3.novaonepro.strategyv2.CheckpointClosureR324.RootSource source,
            Writer out,BooleanSupplier continuing,File progress,String snapshot,String database,String sourceCommit)throws Exception{
        Set<String> roots=new TreeSet<>();boolean[] truncated={false};
        try{source.visit(hash->{try{check(continuing);}catch(IOException failure){throw new UncheckedIOException(failure);}if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalStateException("REPORT_CHECKPOINT_REFERENCE_HASH");if(roots.contains(hash))return;if(roots.size()<MAX_NODES)roots.add(hash);else truncated[0]=true;});}
        catch(UncheckedIOException cancelled){throw cancelled.getCause();}
        CheckpointAuditContinuationR343 page=progress==null?null:CheckpointAuditContinuationR343.open(
                progress,snapshot,database,sourceCommit,roots,truncated[0]);
        return observe(directory,roots,out,continuing,truncated[0],page);
    }
    public static JSONObject observe(File directory,Set<String> roots,Writer out,BooleanSupplier continuing)throws Exception{return observe(directory,roots,out,continuing,false,null);}
    private static JSONObject observe(File directory,Set<String> roots,Writer out,BooleanSupplier continuing,
            boolean omittedRoots,CheckpointAuditContinuationR343 page)throws Exception{
        Objects.requireNonNull(roots);Objects.requireNonNull(out);Objects.requireNonNull(continuing);
        File parent=directory.getAbsoluteFile().getParentFile();if(parent==null)throw new IOException("REPORT_CHECKPOINT_AUDIT_ROOT");
        File root=new File(parent.getCanonicalFile(),directory.getName());
        if(!root.equals(root.getCanonicalFile()))throw new IOException("REPORT_CHECKPOINT_SOURCE_SYMLINK");
        Deque<String> work=new ArrayDeque<>();Set<String> queued=new HashSet<>();boolean truncated=omittedRoots;
        if(page!=null)for(String prior:page.deferredPrevious()){
            check(continuing);queued.add(prior);work.addLast(prior);
        }
        for(String hash:new TreeSet<>(roots)){
            if(hash==null||!hash.matches("[a-f0-9]{64}"))throw new IOException("REPORT_CHECKPOINT_REFERENCE_HASH");
            if(queued.contains(hash))continue;
            if(queued.size()>=MAX_NODES){truncated=true;break;}queued.add(hash);work.add(hash);
        }
        if(page!=null){
            truncated=truncated||page.wasTruncated();
            for(String prior:page.previousHashes()){
                check(continuing);
                if(queued.contains(prior))continue;
                if(queued.size()>=MAX_NODES){truncated=true;break;}
                queued.add(prior);work.addLast(prior);
            }
        }
        Set<String> verifiedThisPage=new HashSet<>(),deferred=new HashSet<>();
        long remaining=BYTE_BUDGET,objects=0,bytes=0,verified=0,missing=0,corrupt=0,unreadable=0,notRun=0,unknownEdges=0;
        long oversized=0,historical=0;
        String firstUnverifiedHash=null;
        while(!work.isEmpty()){
            check(continuing);String hash=work.removeFirst();File file=new File(root,hash+".bin");
            String priorVerifiedSnapshot=page==null?null:page.verifiedIn(hash);
            String status;String observedHash="",tier="UNKNOWN_OR_MISSING";long size=0;Set<String> children=null;
            try{tier=com.ethk3.novaonepro.strategyv2.ColdCheckpointR324.isCold(root,hash)?"COLD_LOSSLESS":"HOT_OR_MISSING";
            if(!file.getAbsoluteFile().equals(file.getCanonicalFile())){status="SYMLINK_REFUSED";unreadable++;}
            else if(!com.ethk3.novaonepro.strategyv2.ColdCheckpointR324.exists(root,hash)){status="MISSING_OBJECT";missing++;}
            else if((size=com.ethk3.novaonepro.strategyv2.ColdCheckpointR324.rawSize(root,hash))<1||size>MAX_OBJECT){status="INVALID_SIZE";corrupt++;}
            else if(priorVerifiedSnapshot!=null){
                status="NOT_RUN_ALREADY_VERIFIED_PREVIOUS_PAGE";notRun++;historical++;
            }
            else if(size>remaining){
                status="NOT_RUN_COMPACT_BYTE_BUDGET";notRun++;
                if(size>BYTE_BUDGET)oversized++;else if(page!=null)deferred.add(hash);
            }
            else{
                MessageDigest digest=MessageDigest.getInstance("SHA-256");
                try{
                    byte[] buffer=new byte[32768],header=new byte[4];int head=0;long count=0;
                    ByteArrayOutputStream manifest=size<=MAX_MANIFEST?new ByteArrayOutputStream((int)size):null;
                    try(InputStream in=com.ethk3.novaonepro.strategyv2.ColdCheckpointR324.open(root,hash)){
                        int n;while((n=in.read(buffer,0,(int)Math.min(buffer.length,remaining+1)))!=-1){
                            check(continuing);if(n>remaining)throw new IOException("REPORT_CHECKPOINT_AUDIT_GREW_BEYOND_BUDGET");remaining-=n;count+=n;digest.update(buffer,0,n);
                            if(head<4){int take=Math.min(n,4-head);System.arraycopy(buffer,0,header,head,take);head+=take;}
                            if(manifest!=null){if(count>MAX_MANIFEST)manifest=null;else manifest.write(buffer,0,n);}
                        }
                    }
                    observedHash=hex(digest.digest());
                    if(count!=size||com.ethk3.novaonepro.strategyv2.ColdCheckpointR324.rawSize(root,hash)!=size){status="CHANGED_DURING_AUDIT";unreadable++;}
                    else if(!hash.equals(observedHash)){status="CORRUPT_HASH_MISMATCH";corrupt++;}
                    else if(ChunkedCheckpointR26.isManifest(header)){
                        if(manifest==null)throw new IOException("REPORT_CHECKPOINT_MANIFEST_BOUND");
                        children=ChunkedCheckpointR26.children(manifest.toByteArray());status="VERIFIED_MANIFEST";verified++;
                    }else if(head>=3&&header[0]==0x26&&header[1]==0x43&&header[2]==0x53){status="UNKNOWN_MANIFEST_VERSION";corrupt++;}
                    else{children=Collections.emptySet();status="VERIFIED_BYTES";verified++;}
                }catch(InterruptedIOException cancelled){throw cancelled;}
                catch(FileNotFoundException absent){status="MISSING_DURING_READ";missing++;}
                catch(IOException failed){
                    if("R324_COLD_OR_HOT_HASH_MISMATCH".equals(failed.getMessage())){
                        observedHash=hex(digest.digest());status="CORRUPT_HASH_MISMATCH";corrupt++;
                    }else{status="UNREADABLE_OR_INVALID_MANIFEST";unreadable++;}
                }
            }
            }catch(InterruptedIOException cancelled){throw cancelled;}
            catch(IOException metadata){status="UNREADABLE_COLD_OR_HOT_METADATA";unreadable++;}
            objects++;bytes=Math.addExact(bytes,size);
            if(children==null){
                unknownEdges++;
                if(firstUnverifiedHash==null)firstUnverifiedHash=hash;
            }
            JSONObject row=new JSONObject().put("kind","checkpoint_object").put("expectedSha256",hash).put("relativePath","r26-checkpoints/"+hash+".bin")
                    .put("observedBytes",size).put("physicalTier",tier).put("availability",status).put("bodyIncluded",false).put("childrenKnown",children!=null).put("directRoot",roots.contains(hash));
            if(!observedHash.isEmpty())row.put("observedSha256",observedHash);
            if(priorVerifiedSnapshot!=null)row.put("historicalVerificationSnapshotSha256",priorVerifiedSnapshot)
                    .put("historicalVerifiedSha256",hash);
            row.put("verifiedChildHashes",children==null?JSONObject.NULL:new org.json.JSONArray(new TreeSet<>(children)));
            line(out,row);
            if(children!=null){
                if(page!=null)verifiedThisPage.add(hash);
                for(String child:children){
                    if(queued.contains(child))continue;if(queued.size()>=MAX_NODES){truncated=true;continue;}queued.add(child);work.addLast(child);
                }
            }
        }
        boolean closure=!truncated&&unknownEdges==0;
        JSONObject summary=new JSONObject().put("format","R32_CHECKPOINT_REFERENCE_AUDIT_V1").put("snapshotBound",true).put("readOnly",true)
                .put("state",missing+corrupt+unreadable>0?"CHECKPOINT_EVIDENCE_GAPS":!closure?"CHECKPOINT_VERIFICATION_INCOMPLETE":roots.isEmpty()?"NO_REFERENCED_CHECKPOINT_OBJECTS":"VERIFIED_REFERENCED_CHECKPOINT_CLOSURE")
                .put("referenceRoots",omittedRoots?JSONObject.NULL:roots.size()).put("retainedRootKeys",roots.size()).put("observedObjects",objects).put("observedBytes",bytes).put("verifiedObjects",verified).put("missingObjects",missing)
                .put("corruptObjects",corrupt).put("unreadableObjects",unreadable).put("byteVerificationNotRun",notRun).put("unknownChildSets",unknownEdges).put("truncated",truncated)
                .put("queuedObjectsUnvisited",work.size()).put("byteVerificationDenominator",objects)
                .put("firstUnverifiedSha256",firstUnverifiedHash==null?JSONObject.NULL:firstUnverifiedHash)
                .put("byteCoverageComplete",!truncated&&notRun==0&&missing==0&&corrupt==0&&unreadable==0)
                .put("sourceClosureVerified",closure).put("sourceRootsComplete",!truncated).put("compactByteBudget",BYTE_BUDGET).put("bytesRead",BYTE_BUDGET-remaining)
                .put("metadataMember",MEMBER).put("rawBodiesIncluded",false).put("gcOrModelUseAuthorized",false)
                .put("boundary","Diagnostic inventory only; missing objects and unknown children are not healthy, restored, exported, or a usable GC mark.");
        if(page!=null)summary.put("continuationR343",
                page.finish(queued,verifiedThisPage,deferred,oversized,historical,truncated,continuing));
        line(out,new JSONObject().put("kind","checkpoint_audit_summary").put("summary",summary));return summary;
    }
}
