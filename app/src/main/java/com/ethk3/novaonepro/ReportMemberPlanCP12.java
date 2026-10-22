package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONObject;

/** External merge sort: 32 bounded member records in RAM, two input lines during each merge. */
public final class ReportMemberPlanCP12 {
    private ReportMemberPlanCP12() {}
    public static final int LINE_LIMIT=8192,CHUNK_ROWS=32;
    public static void append(Writer out,String name,String relativePath,String sha,long bytes)throws Exception {
        if(!name.matches("[A-Za-z0-9_.-]{1,120}")||!sha.matches("[0-9a-f]{64}")||bytes<0||relativePath.length()>1024)throw new IOException("REPORT_MEMBER_RECORD");
        JSONObject o=new JSONObject();o.put("name",name);o.put("path",relativePath);o.put("sha256",sha);o.put("bytes",bytes);String line=o.toString()+"\n";
        if(line.getBytes(StandardCharsets.UTF_8).length>LINE_LIMIT)throw new IOException("REPORT_MEMBER_RECORD_BOUND");out.write(line);
    }
    public static File sort(File job,File unsorted,ReportNativeCP4.Control control)throws Exception {
        String nonce=UUID.randomUUID().toString().replace("-","");File work=new File(job,"member-sort-"+nonce);if(!work.mkdir())throw new IOException("REPORT_SORT_DIRECTORY");
        File result=new File(job,"members-"+nonce+".jsonl");
        try {
        long chunks=0;
        try(ReportArchiveVerifierCP12.LineReader in=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(unsorted),LINE_LIMIT)) {
            for(;;){ArrayList<String> records=new ArrayList<>(CHUNK_ROWS);for(int i=0;i<CHUNK_ROWS;i++){check(control);byte[] line=in.next();if(line==null)break;records.add(new String(line,StandardCharsets.UTF_8));}
                if(records.isEmpty())break;records.sort((a,b)->key(a).compareTo(key(b)));File file=run(work,0,chunks++);
                try(FileOutputStream fd=new FileOutputStream(file);Writer out=new OutputStreamWriter(fd,StandardCharsets.UTF_8)){for(String row:records)out.write(row);out.flush();fd.getFD().sync();}
            }
        }
        if(chunks==0)throw new IOException("REPORT_NO_MEMBERS");
        int pass=0;
        while(chunks>1){long next=0;for(long i=0;i<chunks;i+=2){check(control);File a=run(work,pass,i),b=run(work,pass,i+1),target=run(work,pass+1,next++);
                if(i+1==chunks){if(!a.renameTo(target))throw new IOException("REPORT_SORT_MOVE");}else{merge(a,b,target,control);if(!a.delete()||!b.delete())throw new IOException("REPORT_SORT_REMOVE");}}
            chunks=next;pass++;}
        // Deduplicate into a distinct, fsynced file. Equal logical names must have the identical immutable source proof.
        String previous=null,priorName=null;
        try(ReportArchiveVerifierCP12.LineReader in=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(run(work,pass,0)),LINE_LIMIT);
            FileOutputStream fd=new FileOutputStream(result);Writer out=new OutputStreamWriter(fd,StandardCharsets.UTF_8)) {
            byte[] line;while((line=in.next())!=null){check(control);String row=new String(line,StandardCharsets.UTF_8),name=key(row);
                if(name.equals(priorName)){JSONObject old=new JSONObject(previous),now=new JSONObject(row);if(!old.getString("sha256").equals(now.getString("sha256"))||old.getLong("bytes")!=now.getLong("bytes")||!old.getString("path").equals(now.getString("path")))throw new IOException("REPORT_MEMBER_ALIAS_CONFLICT");continue;}
                out.write(row);previous=row;priorName=name;}
            out.flush();fd.getFD().sync();
        }
        if(!run(work,pass,0).delete()||!removeEmpty(work))throw new IOException("REPORT_SORT_CLEANUP");return result;
        } catch(Exception failure) {
            // A resumed indexing attempt creates a new nonce; these partial files have no durable consumer.
            // Keep the original input/job evidence and the original failure, including its interrupt state.
            cleanFailedAttempt(work,failure);cleanFailedAttempt(result,failure);throw failure;
        }
    }
    private static void cleanFailedAttempt(File file,Exception failure){try{removeGenerated(file,0);}catch(Exception cleanup){failure.addSuppressed(cleanup);}}
    private static void removeGenerated(File file,int depth)throws IOException {
        if(depth>3)throw new IOException("REPORT_SORT_CLEANUP_DEPTH");
        if(!file.getAbsoluteFile().equals(file.getCanonicalFile()))throw new IOException("REPORT_SORT_CLEANUP_PATH");
        if(!file.exists())return;
        if(file.isDirectory()){File[] children=file.listFiles();if(children==null||children.length>1024)throw new IOException("REPORT_SORT_CLEANUP_BOUND");for(File child:children)removeGenerated(child,depth+1);}
        if(!file.delete())throw new IOException("REPORT_SORT_CLEANUP_REMOVE");
    }
    private static void merge(File a,File b,File target,ReportNativeCP4.Control control)throws Exception {
        try(ReportArchiveVerifierCP12.LineReader left=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(a),LINE_LIMIT);
            ReportArchiveVerifierCP12.LineReader right=new ReportArchiveVerifierCP12.LineReader(new FileInputStream(b),LINE_LIMIT);
            FileOutputStream out=new FileOutputStream(target)) {
            byte[] x=left.next(),y=right.next();while(x!=null||y!=null){check(control);boolean l=y==null||(x!=null&&key(new String(x,StandardCharsets.UTF_8)).compareTo(key(new String(y,StandardCharsets.UTF_8)))<=0);
                if(l){out.write(x);x=left.next();}else{out.write(y);y=right.next();}}out.flush();out.getFD().sync();}
    }
    private static String key(String line){try{String name=new JSONObject(line).getString("name");if(!name.matches("[A-Za-z0-9_.-]{1,120}"))throw new IllegalArgumentException("REPORT_SORT_NAME");return name;}catch(Exception bad){throw new IllegalArgumentException("REPORT_SORT_RECORD",bad);}}
    private static File run(File root,int pass,long ordinal)throws IOException{File dir=new File(root,"pass-"+pass+"/bucket-"+(ordinal/1024));if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("REPORT_SORT_BUCKET");return new File(dir,"run-"+ordinal+".jsonl");}
    private static boolean removeEmpty(File dir)throws IOException{File[] entries=dir.listFiles();if(entries==null||entries.length>1024)throw new IOException("REPORT_SORT_CLEANUP_BOUND");for(File entry:entries){if(!entry.isDirectory()||!removeEmpty(entry))return false;}return dir.delete();}
    private static void check(ReportNativeCP4.Control control)throws IOException{if(Thread.currentThread().isInterrupted()||!control.shouldContinue())throw new InterruptedIOException("REPORT_CANCELLED");}
}
