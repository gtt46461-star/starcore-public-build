package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import org.json.JSONObject;

/** Exercises the current journal parser and recovery scans with real host files.
 * No Android, published ZIP, or business-health acceptance is claimed here. */
public final class R31ReportJournalRecoveryHostTest {
    private static int assertions;
    private static void need(boolean value,String reason){assertions++;if(!value)throw new AssertionError(reason);}
    private static String id(int value){return String.format(Locale.ROOT,"%064x",value);}
    private static void write(File file,String text)throws IOException {
        Files.createDirectories(file.getParentFile().toPath());
        Files.write(file.toPath(),text.getBytes(StandardCharsets.UTF_8));
    }
    public static void main(String[] args)throws Exception {
        File files=new File(args[0],"files");Files.createDirectories(files.toPath());
        File root=ReportDirectoriesCP12.root(files);
        ReportJobJournalCP12.Job valid=new ReportJobJournalCP12.Job(id(1),id(2),id(3),"FULL_REPORT",1L);
        valid.stage="READY_TO_SHARE";
        File good=new File(root,"jobs/"+valid.jobId+"/job.json");write(good,valid.json().toString());
        File broken=new File(root,"jobs/"+id(4)+"/job.json"),payload=new File(broken.getParentFile(),"snapshot.db");
        write(payload,"OPAQUE_ORIGINAL_MUST_BE_PRESERVED");byte[] preserved=Files.readAllBytes(payload.toPath());
        for(String field:new String[]{"stage","resumeStage","jobId","requestId","releaseHash","mode"}) {
            ReportJobJournalCP12.Job unrelated=new ReportJobJournalCP12.Job(id(4),id(5),id(3),"RETENTION_PAGE",2L);
            String damaged=unrelated.json().put(field,"BROKEN").toString();write(broken,damaged);
            ReportJobJournalCP12.Job recovered=ReportJobJournalCP12.findRequestR31(root,valid.requestId);
            need(recovered!=null&&valid.jobId.equals(recovered.jobId),"UNRELATED_DAMAGED_"+field+"_HID_VALID_REPORT");
            JSONObject state=ReportJobJournalCP12.authoritativeStateR31(root,valid.requestId);
            need(state!=null&&"READY_TO_SHARE".equals(state.getString("stage")),"AUTHORITATIVE_READY_LOST_"+field);
            need(valid.jobId.equals(ReportJobJournalCP12.latestFullReport(root).jobId),"LATEST_REPORT_LOST_"+field);
            try{ReportJobJournalCP12.Job.parse(damaged);throw new AssertionError("DAMAGED_FIELD_ACCEPTED:"+field);}
            catch(IOException expected){need(expected.getMessage().equals("REPORT_JOB_FORMAT"),"UNTYPED_FORMAT_FAILURE:"+field);}
            need(damaged.equals(new String(Files.readAllBytes(broken.toPath()),StandardCharsets.UTF_8)),"DAMAGED_JOURNAL_REWRITTEN:"+field);
            need(Arrays.equals(preserved,Files.readAllBytes(payload.toPath())),"ORIGINAL_PAYLOAD_CHANGED:"+field);
        }
        ReportJobJournalCP12.Job duplicate=new ReportJobJournalCP12.Job(id(4),valid.requestId,id(3),"FULL_REPORT",3L);
        write(broken,duplicate.json().toString());
        try{ReportJobJournalCP12.findRequestR31(root,valid.requestId);throw new AssertionError("DUPLICATE_REQUEST_ACCEPTED");}
        catch(IOException expected){need("REPORT_DUPLICATE_REQUEST_JOBS".equals(expected.getMessage()),"DUPLICATE_ERROR_LOST");}
        Files.delete(broken.toPath());
        File sameId=new File(root,"jobs/"+id(6)+"/job.json");
        ReportJobJournalCP12.Job damagedTarget=new ReportJobJournalCP12.Job(id(6),id(6),id(3),"FULL_REPORT",4L);
        String original=damagedTarget.json().put("stage","BROKEN").toString();write(sameId,original);
        try(ReportJobJournalCP12 owner=ReportJobJournalCP12.tryClaim(files)) {
            need(owner!=null,"JOURNAL_OWNER_UNAVAILABLE");
            try{owner.recoverOrCreateR31(id(6),id(3),"FULL_REPORT");throw new AssertionError("DAMAGED_TARGET_OVERWRITTEN");}
            catch(IOException expected){need("REPORT_JOB_EXISTS".equals(expected.getMessage()),"DAMAGED_TARGET_NOT_PROTECTED");}
        }
        need(original.equals(new String(Files.readAllBytes(sameId.toPath()),StandardCharsets.UTF_8)),"DAMAGED_TARGET_BYTES_CHANGED");
        System.out.println("REPORT_JOURNAL_DAMAGED_ENTRY_RECOVERY_HOST_PASS assertions="+assertions+" INVALID_STAGE_RESUME_ID_MODE_ISOLATED ORIGINALS_PRESERVED DUPLICATE_REQUEST_STILL_REJECTED; HOST_ONLY_NOT_ANDROID");
    }
}
