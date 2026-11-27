package com.ethk3.novaonepro;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
public class FieldSizeCopy {
    public static void main(String[] args) throws Exception {
        File root=new File(args[0]),files=new File(root,"files");
        files.mkdirs();
        File source=new File(root,"source/com.starcore.research.r13/databases/fixture.db");
        File official=new File(source.getParent(),AppConfig.DB_NAME);
        java.nio.file.Files.move(source.toPath(),official.toPath());
        android.app.Service.filesR31=files;
        R31PhoneReportHostTest.installHostStat();
        StorageBudgetR31 budget=new StorageBudgetR31(root,new StorageBudgetR31.Config(10L<<30,10L<<30,4096L,2L<<20,"FIELD_SIZE_HOST_FIXTURE"),File::getUsableSpace);
        ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
        try(Closeable fixture=ReportIoR31.hostFixture(root,budget);ReportJobJournalCP12 journal=ReportJobJournalCP12.tryClaim(files)){
            var job=journal.recoverOrCreateR31("1".repeat(64),"2".repeat(64),"FULL_REPORT");
            Object active=R31PhoneReportHostTest.active(job,journal.root());
            File dir=journal.directory(job),target=new File(dir,"snapshot.db");
            try(var grant=budget.reserve("report:"+job.jobId,job.revision,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/r981_reports/jobs/"+job.jobId,official.length()+(32L<<20),2*official.length(),true);
                var scope=ReportIoR31.bind(dir,(ReportNativeCP4.Control)active)){
                R31PhoneReportHostTest.set(active,"storageReservationR31",grant);
                R31PhoneReportHostTest.enter(active,"BACKUP");
                Method copy=active.getClass().getDeclaredMethod("copyPinnedSnapshot",File.class,File.class);
                copy.setAccessible(true);
                long start=System.nanoTime();
                copy.invoke(active,official,target);
                long after=System.nanoTime();
                R31PhoneReportHostTest.enter(active,"SNAPSHOT_HASH");
                String sha=ReportArchiveCP4.hash(target,(ReportNativeCP4.Control)active);
                long finished=System.nanoTime();
                if(target.length()!=official.length() || !"".equals(R31PhoneReportHostTest.get(active,"stopReason")))throw new AssertionError();
                System.out.printf("FIELD_SIZE_NATIVE_BACKUP_HASH bytes=%d backupMs=%d hashMs=%d sha=%s target=%s%n",target.length(),(after-start)/1000000,(finished-after)/1000000,sha,target);
            }
        }
    }
}
