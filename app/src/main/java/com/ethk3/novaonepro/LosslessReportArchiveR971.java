package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.DigestOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.util.zip.Deflater;

/** One pass, bounded memory, exact UTF-8 evidence. An incomplete ZIP is never published. */
public final class LosslessReportArchiveR971 {
    public interface Body { void write(Writer out) throws Exception; }
    public static final class Result {
        public final File file;
        public final long bodyBytes, reportBytes;
        public final String bodySha256, reportSha256, archiveSha256;
        Result(File f,long b,long r,String bh,String rh,String ah){file=f;bodyBytes=b;reportBytes=r;bodySha256=bh;reportSha256=rh;archiveSha256=ah;}
    }
    private LosslessReportArchiveR971() {}
    public static Result create(File directory, Body body) throws Exception {
        if(directory==null||body==null)throw new IllegalArgumentException("REPORT_ARGUMENT");
        if(!directory.isDirectory()&&!directory.mkdirs())throw new IOException("REPORT_DIRECTORY");
        File partial=File.createTempFile("R971_complete_", ".partial", directory);
        boolean published=false;
        try {
            MessageDigest archiveHash=MessageDigest.getInstance("SHA-256");
            long bodyBytes,reportBytes;String bodySha,reportSha;
            try(FileOutputStream bytes=new FileOutputStream(partial);
                DigestOutputStream archiveBytes=new DigestOutputStream(bytes,archiveHash);
                ZipOutputStream zip=new ZipOutputStream(archiveBytes)) {
                zip.setLevel(Deflater.BEST_SPEED);
                ZipEntry report=new ZipEntry("report.txt");report.setTime(0L);zip.putNextEntry(report);
                Meter sink=new Meter(zip);
                Writer writer=new BufferedWriter(new OutputStreamWriter(sink,StandardCharsets.UTF_8),32768);
                body.write(writer);writer.flush();
                bodyBytes=sink.bytes;bodySha=hex(sink.bodyHash.digest());sink.bodyEnabled=false;
                writer.write("\nFILE_BODY_BYTES="+bodyBytes+"\nFILE_BODY_SHA256="+bodySha
                        +"\nHashScope=EXACT_FIRST_FILE_BODY_BYTES\nEND_OF_COMPLETE_FILE\n");
                writer.flush();reportBytes=sink.bytes;reportSha=hex(sink.allHash.digest());
                zip.closeEntry();
                ZipEntry manifest=new ZipEntry("manifest.json");manifest.setTime(0L);zip.putNextEntry(manifest);
                String json="{\"format\":\"R971_LOSSLESS_COMPLETE_REPORT_V1\",\"entry\":\"report.txt\","
                        +"\"report_bytes\":"+reportBytes+",\"report_sha256\":\""+reportSha+"\","
                        +"\"body_bytes\":"+bodyBytes+",\"body_sha256\":\""+bodySha+"\","
                        +"\"completion\":\"ALL_REQUESTED_WRITES_COMPLETED\","
                        +"\"snapshot_scope\":\"PER_TABLE_READ_TIME_NOT_GLOBAL_ATOMIC_BACKUP\","
                        +"\"truncation\":false,\"compression\":\"LOSSLESS_DEFLATE_LEVEL_1\"}\n";
                zip.write(json.getBytes(StandardCharsets.UTF_8));zip.closeEntry();zip.finish();zip.flush();bytes.getFD().sync();
            }
            String archiveSha=hex(archiveHash.digest());
            File ready=new File(directory,"R923_"+archiveSha+".zip");
            if(ready.exists()){
                if(ready.length()!=partial.length())throw new IOException("REPORT_DIGEST_LENGTH_CONFLICT");
                if(!partial.delete())throw new IOException("REPORT_PARTIAL_DELETE");
            }else if(!partial.renameTo(ready))throw new IOException("REPORT_PUBLISH");
            published=true;return new Result(ready,bodyBytes,reportBytes,bodySha,reportSha,archiveSha);
        } finally {if(!published&&partial.exists())partial.delete();}
    }
    private static final class Meter extends OutputStream {
        final OutputStream target;
        final MessageDigest bodyHash,allHash;
        long bytes;boolean bodyEnabled=true;
        Meter(OutputStream target)throws Exception{this.target=target;bodyHash=MessageDigest.getInstance("SHA-256");allHash=MessageDigest.getInstance("SHA-256");}
        @Override public void write(int b)throws IOException{byte[] one={(byte)b};write(one,0,1);}
        @Override public void write(byte[] b,int off,int len)throws IOException{
            target.write(b,off,len);allHash.update(b,off,len);if(bodyEnabled)bodyHash.update(b,off,len);bytes+=len;
        }
        @Override public void flush()throws IOException{target.flush();}
    }
    static String hex(byte[] hash){StringBuilder s=new StringBuilder(hash.length*2);for(byte b:hash)s.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return s.toString();}
}
