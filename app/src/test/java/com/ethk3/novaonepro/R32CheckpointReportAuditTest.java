package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.JSONObject;
import org.junit.Test;
import static org.junit.Assert.*;

public class R32CheckpointReportAuditTest {
    static String hash(byte[] body)throws Exception{StringBuilder s=new StringBuilder();for(byte b:MessageDigest.getInstance("SHA-256").digest(body))s.append(String.format("%02x",b&255));return s.toString();}
    static String id(int n){return String.format(Locale.ROOT,"%064x",n);}
    @Test public void missingSourceIsObservedWithoutCreatingAnEmptyCheckpointDirectory()throws Exception{
        File parent=Files.createTempDirectory("r32-missing-cas-").toFile(),source=new File(parent,"r26-checkpoints");StringWriter rows=new StringWriter();
        JSONObject result=CheckpointReportAuditR32.observe(source,Collections.singleton(id(32)),rows,()->true);
        assertFalse(source.exists());assertEquals(1,result.getLong("missingObjects"));assertFalse(result.getBoolean("sourceClosureVerified"));assertFalse(result.getBoolean("gcOrModelUseAuthorized"));assertTrue(rows.toString().contains(id(32)));assertTrue(rows.toString().contains("MISSING_OBJECT"));
    }
    @Test public void aMissingReferenceDoesNotAbortOtherReferenceObservations()throws Exception{
        File root=Files.createTempDirectory("r32-cas-gaps-").toFile();byte[] body="REAL_CHECKPOINT_FIXTURE".getBytes(StandardCharsets.UTF_8);String good=hash(body),absent=id(33),bad=id(34);
        Files.write(new File(root,good+".bin").toPath(),body);Files.write(new File(root,bad+".bin").toPath(),body);StringWriter rows=new StringWriter();
        JSONObject result=CheckpointReportAuditR32.observe(root,new TreeSet<>(Arrays.asList(good,absent,bad)),rows,()->true);
        assertEquals(3,result.getLong("observedObjects"));assertEquals(1,result.getLong("verifiedObjects"));assertEquals(1,result.getLong("missingObjects"));assertEquals(1,result.getLong("corruptObjects"));assertFalse(result.getBoolean("sourceClosureVerified"));
        assertTrue(rows.toString().contains("CORRUPT_HASH_MISMATCH"));assertArrayEquals(body,Files.readAllBytes(new File(root,good+".bin").toPath()));assertFalse(new File(root,absent+".bin").exists());
    }
    @Test public void aVerifiedManifestReportsItsMissingChildWithoutInventingFurtherEdges()throws Exception{
        File root=Files.createTempDirectory("r32-manifest-gap-").toFile();String missing=id(35);ByteArrayOutputStream bytes=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(0x26435302);out.writeLong(7);out.write(new byte[32]);out.writeInt(1);out.writeUTF(missing);out.writeInt(7);}
        String manifest=hash(bytes.toByteArray());Files.write(new File(root,manifest+".bin").toPath(),bytes.toByteArray());StringWriter rows=new StringWriter();
        JSONObject result=CheckpointReportAuditR32.observe(root,Collections.singleton(manifest),rows,()->true);
        assertEquals(2,result.getLong("observedObjects"));assertEquals(1,result.getLong("verifiedObjects"));assertEquals(1,result.getLong("missingObjects"));assertEquals(1,result.getLong("unknownChildSets"));assertFalse(result.getBoolean("sourceClosureVerified"));assertTrue(rows.toString().contains(missing));
    }
    @Test public void cancellationAndUnsafeRootAreNotConvertedToHealthyEvidence()throws Exception{
        File root=Files.createTempDirectory("r32-audit-cancel-").toFile();try{CheckpointReportAuditR32.observe(root,Collections.singleton(id(36)),new StringWriter(),()->false);fail();}catch(InterruptedIOException expected){assertTrue(expected.getMessage().contains("CANCELLED"));}
        File alias=new File(root,"alias");Files.createSymbolicLink(alias.toPath(),root.toPath());try{CheckpointReportAuditR32.observe(alias,Collections.singleton(id(36)),new StringWriter(),()->true);fail();}catch(IOException expected){assertTrue(expected.getMessage().contains("SYMLINK"));}
    }
}
