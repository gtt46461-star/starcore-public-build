package com.ethk3.novaonepro;
import java.io.*;
/** Explicit capacity fixture for the unchanged R30 driver; no Android admission bypass exists. */
public final class R31LegacyReportHostEntry {
    public static void main(String[] args)throws Exception{
        File files=new File(args[0]),root=files.getCanonicalFile().getParentFile();files.mkdirs();
        StorageBudgetR31 budget=new StorageBudgetR31(root,new StorageBudgetR31.Config(1024L<<20,1024L<<20,1024,65536,"HOST_LEGACY_REPORT_METADATA_FIXTURE"),f->8L<<30);
        try(Closeable fixture=ReportIoR31.hostFixture(root,budget)){ReportDeliveryR30HostTest.main(args);}
    }
}
