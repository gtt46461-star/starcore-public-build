package com.ethk3.novaonepro;
import java.io.IOException;

/** Conservative output promises. Metadata is an enforced admission envelope, never a source-size estimate. */
public final class ReportPhaseBudgetR31 {
    private ReportPhaseBudgetR31(){}
    public static final long METADATA_ENVELOPE=256L<<20,MARGIN=64L<<20,MAX_WRITTEN=16L<<30;
    private static long add(long a,long b)throws IOException{return StorageBudgetR31.add(a,b);}
    private static long multiply(long a,long b)throws IOException{try{return Math.multiplyExact(a,b);}catch(ArithmeticException overflow){throw new IOException("REPORT_PHASE_BUDGET_OVERFLOW",overflow);}}
    public static long initial(long snapshotUpper)throws IOException{if(snapshotUpper<1)throw new IOException("REPORT_SNAPSHOT_BOUND");return add(add(snapshotUpper,METADATA_ENVELOPE),MARGIN);}
    public static long attachmentFuture(long missingCopyBytes)throws IOException{if(missingCopyBytes<0)throw new IOException("REPORT_ATTACHMENT_BOUND");return add(add(missingCopyBytes,METADATA_ENVELOPE),MARGIN);}
    /** A frozen attachment is staged at its actual length. Each new content address may
     * create two directories and a final file block. Reused objects need no second copy. */
    public static long frozenCopyBytes(long missingBytes,long missingMembers)throws IOException {
        if(missingBytes<0||missingMembers<0)throw new IOException("REPORT_ATTACHMENT_BOUND");
        return add(missingBytes,add(multiply(missingMembers,12L<<10),64L<<10));
    }
    public static long zipUpper(long raw,long members)throws IOException{if(raw<0||members<1)throw new IOException("REPORT_PLAN_BOUND");return add(add(add(raw,raw/16),multiply(members,1024)),16L<<20);}
    public static long archiveFuture(long raw,long members)throws IOException{
        if(raw<0||members<1)throw new IOException("REPORT_PLAN_BOUND");
        long archive=add(add(add(raw,raw/16),multiply(members,65536)),16L<<20),zip=zipUpper(raw,members);
        return add(add(add(archive,zip),zip>ReportPublicationCP12.ZIP_LIMIT_R30?zip:0),MARGIN);
    }
}
