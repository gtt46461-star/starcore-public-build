package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import org.json.*;

/** The same receipt owner used by Service; synthetic publication and host file transport only. */
public final class ReportSafResumeIdentityR986 {
    private static int assertions;
    private interface Work { void run() throws Exception; }
    private static void reject(String code, Work work) throws Exception {
        assertions++;
        try { work.run(); throw new AssertionError("accepted " + code); }
        catch (IOException error) { if (!code.equals(error.getMessage())) throw new AssertionError(error); }
    }
    public static void main(String[] args) throws Exception {
        File job = new File(args[0]); job.mkdirs();
        File archive = new File(job, "archive-1"); archive.mkdirs();
        Files.write(new File(archive, "manifest.jsonl").toPath(), "EXPLICIT_SYNTHETIC_PUBLICATION_TRANSPORT_FIXTURE\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        Files.write(new File(archive, "P000001.bin").toPath(), new byte[]{11, 22, 33, 44});
        String id = String.format("%064x", 986), manifestSha = ReportArchiveCP4.hash(new File(archive,"manifest.jsonl"), () -> true);
        ReportJobJournalCP12.Job state = new ReportJobJournalCP12.Job(id, String.format("%064x", 987), String.format("%064x", 988), "FULL_REPORT", 1);
        state.stage="READY_TO_SHARE"; state.publishedDir="archive-1"; state.evidenceComplete=true; state.checksComplete=false; state.allChecksPassed=false; state.manifestSha=manifestSha;
        ReportJobJournalCP12.writeAtomic(new File(job,"job.json"),state.json().toString());
        ReportPublicationCP12.prepare(job,archive,id,manifestSha,true,false,false,()->true);
        JSONObject pointer=new JSONObject(); pointer.put("deliveryIndexSha256",ReportArchiveCP4.hash(new File(job,"delivery/index.json"),()->true));
        ReportJobJournalCP12.writeAtomic(new File(job,"published.json"),pointer.toString());
        JSONObject index=ReportPublicationCP12.delivery(job); JSONArray rows=index.getJSONArray("files");
        for (int i=0;i<=rows.length();i++) {
            String name,path,sha;long bytes;
            if(i==rows.length()) { name="index.json";path="delivery/index.json";File file=new File(job,path);bytes=file.length();sha=ReportArchiveCP4.hash(file,()->true); }
            else { JSONObject row=rows.getJSONObject(i);name=row.getString("name");path=row.getString("path");sha=row.getString("sha256");bytes=row.getLong("bytes"); }
            File target=new File(job,"saved-"+i+".bin");
            ReportSafCopyCP12.Destination destination=new ReportSafCopyCP12.Destination() {
                public OutputStream openOutputTruncating() throws IOException { return new FileOutputStream(target,false); }
                public InputStream openInput() throws IOException { return new FileInputStream(target); }
            };
            ReportSafCopyCP12.copy(ReportArchiveVerifierCP12.inside(job,path),sha,bytes,destination,()->true);
            JSONObject saved=new JSONObject();saved.put("name",name);saved.put("sha256",sha);saved.put("bytes",bytes);
            ReportSafCopyCP12.Receipt receipt=ReportSafCopyCP12.verifySavedReceipt(saved,name,sha,bytes,destination,()->true);
            assertions++;if(receipt.bytes!=bytes||!sha.equals(receipt.sha256))throw new AssertionError("verified current publication");
            final String expectedName=name,expectedSha=sha;final long expectedBytes=bytes;
            ReportSafCopyCP12.Destination neverOpen=new ReportSafCopyCP12.Destination() {
                public OutputStream openOutputTruncating() { throw new AssertionError("verification must be read-only"); }
                public InputStream openInput() { throw new AssertionError("mismatched receipt reached provider"); }
            };
            JSONObject wrongName=new JSONObject(saved.toString());wrongName.put("name","P999999.bin");
            reject("REPORT_SAF_RECEIPT_IDENTITY",()->ReportSafCopyCP12.verifySavedReceipt(wrongName,expectedName,expectedSha,expectedBytes,neverOpen,()->true));
            JSONObject wrongBytes=new JSONObject(saved.toString());wrongBytes.put("bytes",bytes+1);
            reject("REPORT_SAF_RECEIPT_IDENTITY",()->ReportSafCopyCP12.verifySavedReceipt(wrongBytes,expectedName,expectedSha,expectedBytes,neverOpen,()->true));
            JSONObject wrongHash=new JSONObject(saved.toString());wrongHash.put("sha256","0".repeat(64));
            reject("REPORT_SAF_RECEIPT_IDENTITY",()->ReportSafCopyCP12.verifySavedReceipt(wrongHash,expectedName,expectedSha,expectedBytes,neverOpen,()->true));
            JSONObject incomplete=new JSONObject(saved.toString());incomplete.remove("sha256");
            reject("REPORT_SAF_RECEIPT_IDENTITY",()->ReportSafCopyCP12.verifySavedReceipt(incomplete,expectedName,expectedSha,expectedBytes,neverOpen,()->true));
            Files.write(target.toPath(),new byte[]{99});
            reject("REPORT_SAF_VERIFY_FAILED",()->ReportSafCopyCP12.verifySavedReceipt(saved,expectedName,expectedSha,expectedBytes,destination,()->true));
        }
        String original=ReportJobJournalCP12.readAtomic(new File(job,"delivery/index.json"),65536);
        JSONObject altered=new JSONObject(original);altered.getJSONArray("files").getJSONObject(0).put("sha256","0".repeat(64));
        ReportJobJournalCP12.writeAtomic(new File(job,"delivery/index.json"),altered.toString());
        reject("REPORT_DELIVERY_INDEX_INTEGRITY",()->ReportPublicationCP12.delivery(job));
        ReportJobJournalCP12.writeAtomic(new File(job,"delivery/index.json"),original);
        System.out.println("PASS R986_SAF_RESUME_IDENTITY assertions="+assertions+" actualPublicationOwner=true transport=HOST_FILESYSTEM DEVICE_NOT_VERIFIED");
    }
}
