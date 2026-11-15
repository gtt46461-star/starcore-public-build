package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class R30ResearchRetentionTest {
    static File file(File root,String name,int bytes)throws Exception{File f=new File(root,name);f.getParentFile().mkdirs();Files.write(f.toPath(),new byte[bytes]);return f;}
    static ResearchRetentionR30.Audit audit(List<String> events,boolean approve){return new ResearchRetentionR30.Audit(){
        public String retiredCopyReference(ResearchRetentionR30.Candidate c){return approve?"FB_ACTUAL_REFERENCE":null;}
        public void beforeDelete(ResearchRetentionR30.Candidate c,String ref){assertTrue(c.file.isFile());events.add("BEFORE:"+c.relative);}
        public void afterDelete(ResearchRetentionR30.Candidate c,String ref,boolean deleted){assertTrue(deleted);assertFalse(c.file.exists());events.add("AFTER:"+c.relative);}
    };}
    @Test public void overBudgetDeletesOnlyRecomputableFilesAndPreservesModelAndEvidence()throws Exception{
        File root=Files.createTempDirectory("r30-retention-files-").toFile();File model=file(root,"files/model-bundles-r929/full-state",1024),frozen=file(root,"databases/source.db",1024),cache=file(root,"code_cache/compiled",4096);List<String> events=new ArrayList<>();
        ResearchRetentionR30.Result r=ResearchRetentionR30.clean(root,2048,audit(events,false));assertEquals("WITHIN_BUDGET",r.state);assertEquals(4096,r.deletedBytes);assertEquals(2048,r.after.bytes);assertFalse(cache.exists());assertTrue(model.exists());assertTrue(frozen.exists());assertEquals(Arrays.asList("BEFORE:code_cache/compiled","AFTER:code_cache/compiled"),events);
        assertEquals("BLOCKED_PROTECTED_DATA_BUDGET",ResearchRetentionR30.clean(root,1,audit(events,false)).state);assertTrue(model.exists());assertTrue(frozen.exists());
    }
    @Test public void retiredDuplicateRequiresProofAndDurableAuditBeforeUnlink()throws Exception{
        File root=Files.createTempDirectory("r30-retention-retired-").toFile();File retired=file(root,ResearchRetentionR30.RETIRED,4096);file(root,"code_cache/rebuildable",1024);List<String> events=new ArrayList<>();
        ResearchRetentionR30.Result noProof=ResearchRetentionR30.clean(root,1,audit(events,false));assertTrue(retired.exists());assertEquals(1,noProof.protectedRetired);assertEquals("BLOCKED_PROTECTED_DATA_BUDGET",noProof.state);
        try{ResearchRetentionR30.clean(root,1,new ResearchRetentionR30.Audit(){public String retiredCopyReference(ResearchRetentionR30.Candidate c){return "verified";}public void beforeDelete(ResearchRetentionR30.Candidate c,String ref)throws IOException{throw new IOException("DURABLE_REFERENCE_WRITE_FAILED");}public void afterDelete(ResearchRetentionR30.Candidate c,String ref,boolean ok){fail("unlink must not happen");}});fail("audit failure ignored");}catch(IOException expected){}
        assertTrue(retired.exists());events.clear();assertEquals("WITHIN_BUDGET",ResearchRetentionR30.clean(root,1,audit(events,true)).state);assertTrue(events.get(0).startsWith("BEFORE:databases/"));assertFalse(retired.exists());
    }
    @Test public void unresolvedLinksOrInvalidBudgetCannotBecomeMeasuredSuccessOrEvictData()throws Exception{
        File root=Files.createTempDirectory("r30-retention-unknown-").toFile();File cache=file(root,"code_cache/rebuildable",4096);List<String> events=new ArrayList<>();
        ResearchRetentionR30.Result invalid=ResearchRetentionR30.clean(root,0,audit(events,false));assertEquals("UNKNOWN_INVALID_BUDGET",invalid.state);assertNull(invalid.json().get("targetMet"));assertTrue(cache.exists());
        File outside=Files.createTempDirectory("r30-outside-").toFile();file(outside,"protected-state",1024);Files.createSymbolicLink(new File(root,"unresolved-data-link").toPath(),outside.toPath());
        ResearchRetentionR30.Result unknown=ResearchRetentionR30.clean(root,1,audit(events,true));assertEquals("UNKNOWN_INCOMPLETE_CENSUS",unknown.state);assertFalse(unknown.after.complete);assertNull(unknown.json().get("targetMet"));assertTrue(cache.exists());assertTrue(events.isEmpty());assertTrue(new File(outside,"protected-state").exists());
    }
}
