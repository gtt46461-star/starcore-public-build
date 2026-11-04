package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

/** Actual separate JVMs, OS locks and forced process death. No Android/process-duration claim. */
public final class R31StorageProtocolHostTest {
    private static void need(boolean value,String why){if(!value)throw new AssertionError(why);}
    private static StorageBudgetR31 budget(File root)throws Exception{return new StorageBudgetR31(root,new StorageBudgetR31.Config(8L<<20,8L<<20,1024,65536,"HOST_MULTIPROCESS_CAPACITY_FIXTURE"),f->100L<<20);}
    private static Process child(String mode,File root,File output)throws Exception{return new ProcessBuilder(new File(System.getProperty("java.home"),"bin/java").getPath(),"-Xmx64m","-cp",System.getProperty("java.class.path"),R31StorageProtocolHostTest.class.getName(),mode,root.getPath(),output.getPath()).redirectError(ProcessBuilder.Redirect.INHERIT).start();}
    private static void worker(String mode,File root,File output)throws Exception{
        if(mode.equals("hold")){try(StorageBudgetR31.Reservation r=budget(root).reserve("child-report",11,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",2L<<20,true)){
            output.getParentFile().mkdirs();try(OutputStream out=r.wrap(output,new FileOutputStream(output))){out.write(new byte[131072]);}System.out.println("ACQUIRED");System.out.flush();System.in.read();
        }}else if(mode.equals("share")){try(ReportPublicationCP12.Lease lease=ReportPublicationCP12.shared(output)){System.out.println("SHARED");System.out.flush();System.in.read();}}
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&(args[0].equals("hold")||args[0].equals("share"))){worker(args[0],new File(args[1]),new File(args[2]));return;}
        File root=new File(args[0]);root.mkdirs();File payload=new File(root,"files/job/actual-output.bin");Process worker=child("hold",root,payload);
        try{need("ACQUIRED".equals(new BufferedReader(new InputStreamReader(worker.getInputStream())).readLine()),"CHILD_NOT_HOLDING");StorageBudgetR31 budget=budget(root);
            need(budget.recoverReservations()==0,"LIVE_OS_OWNER_WAS_RECOVERED");need(((List<?>)budget.snapshot().get("owners")).size()==1,"ACTIVE_CROSS_PID_OWNER_NOT_VISIBLE");
            try{budget.reserve("rival",12,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true);throw new AssertionError("OVERLAP_ACCEPTED");}catch(StorageBudgetR31.Rejected reject){need(reject.reason.equals("SCOPE_ALREADY_OWNED"),reject.toString());}
            try{budget.reserve("rival",12,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/other",7L<<20,true);throw new AssertionError("CROSS_PID_COMBINED_CAP_ACCEPTED");}catch(StorageBudgetR31.Rejected reject){need(reject.reason.equals("BLOCKED_PROTECTED_DEBT"),reject.toString());}
            worker.destroyForcibly();need(worker.waitFor(5,TimeUnit.SECONDS),"CHILD_DID_NOT_TERMINATE");need(budget.recoverReservations()==1,"DEAD_OS_OWNER_NOT_RECOVERED");need(payload.length()==131072,"PROCESS_RECOVERY_DELETED_PHYSICAL_OUTPUT");
            try(StorageBudgetR31.Reservation replacement=budget.reserve("replacement",13,StorageBudgetR31.Category.REPORT_WORKSPACE,"files/job",4096,true)){replacement.beforeChunk(1);}
            File recovery=new File(root,"files/recovery");recovery.mkdirs();Files.write(new File(recovery,"original.zip").toPath(),new byte[12345]);Process reader=child("share",root,recovery);
            try{need("SHARED".equals(new BufferedReader(new InputStreamReader(reader.getInputStream())).readLine()),"SHARE_CHILD_NOT_HOLDING");need(!ReportPublicationCP12.tryDeleteRecoveryR31(recovery),"ACTUAL_CROSS_PID_READER_NOT_PROTECTED");reader.destroyForcibly();need(reader.waitFor(5,TimeUnit.SECONDS),"READER_DID_NOT_TERMINATE");need(ReportPublicationCP12.tryDeleteRecoveryR31(recovery),"ACTUALLY_CLOSED_SHARE_NOT_RELEASED");}finally{reader.destroyForcibly();}
            System.out.println("R31_STORAGE_MULTIPROCESS_HOST|LIVE_OS_OWNER_RETAINED|COMBINED_CAP_AND_OVERLAP_DENIED|KILLED_OWNER_RECOVERED_WITH_FILES_PRESERVED|RECOVERY_SHARE_CROSS_PID_PROTECTED_UNTIL_PROCESS_EXIT|HOST_NOT_ANDROID|PASS");
        }finally{worker.destroyForcibly();}
    }
}
