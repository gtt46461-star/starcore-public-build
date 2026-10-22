package com.ethk3.novaonepro;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.zip.*;
/** Current production KEEP contract. Full report Service remains a separate owner. */
public final class DiagnosticFileStoreContractR998 {
    private static int checks;
    private static void require(boolean value,String reason){checks++;if(!value)throw new AssertionError(reason);}
    private static void rejected(File dir,String path)throws Exception{
        try{DiagnosticFileStoreR941.resolve(dir,path);throw new AssertionError("invalid path accepted");}
        catch(IOException expected){checks++;}
    }
    public static void main(String[] args)throws Exception{
        File dir=new File(args[0]);require(dir.mkdirs()||dir.isDirectory(),"test directory");
        String body="中".repeat(8191)+"🚲"+"完整原文".repeat(5000);
        File saved=DiagnosticFileStoreR941.write(dir,body);
        require(Files.readString(saved.toPath(),StandardCharsets.UTF_8).equals(body),"exact UTF8 including encoder chunk boundary");
        String hex=java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body.getBytes(StandardCharsets.UTF_8)));
        require(saved.getName().equals("R923_"+hex+".txt"),"exact body hash filename");
        require(DiagnosticFileStoreR941.write(dir,body).equals(saved),"idempotent same body identity");
        require(DiagnosticFileStoreR941.resolve(dir,saved.getName()).equals(saved.getCanonicalFile()),"canonical bounded reader");
        String preview=DiagnosticFileStoreR941.preview(saved);
        require(preview.contains("全文已保存")&&preview.length()<DiagnosticFileStoreR941.PREVIEW_CHARS+160,"bounded preview without truncating actual file");
        rejected(dir,"../"+saved.getName());rejected(dir,"R923_"+"0".repeat(64)+".txt");rejected(dir,"other.txt");
        File zip=new File(dir,"R923_"+"a".repeat(64)+".zip");
        try(ZipOutputStream out=new ZipOutputStream(new FileOutputStream(zip))){out.putNextEntry(new ZipEntry("report.txt"));out.write("中文小详情".getBytes(StandardCharsets.UTF_8));out.closeEntry();}
        require(DiagnosticFileStoreR941.preview(zip).equals("中文小详情"),"lossless ZIP small detail preview");
        File partial=new File(dir,"detail_test.partial"),unrelated=new File(dir,"user-data.bin");
        Files.writeString(partial.toPath(),"old partial");partial.setLastModified(System.currentTimeMillis()-7200000);Files.writeString(unrelated.toPath(),"preserve unrelated");
        for(int i=0;i<6;i++)DiagnosticFileStoreR941.write(dir,"bounded small detail "+i);
        // Make saved the current write/share target after earlier cache rotations.
        saved=DiagnosticFileStoreR941.write(dir,body);
        DiagnosticFileStoreR941.prune(dir,saved);
        require(saved.isFile(),"currently shared detail retained");require(unrelated.isFile(),"unrelated file retained");require(!partial.exists(),"expired partial cleaned");
        int count=0;for(File file:dir.listFiles())if(file.getName().matches("R923_[0-9a-f]{64}\\.(txt|zip|csv|jsonl)"))count++;
        require(count<=3,"bounded small-detail retention");
        System.out.println("PASS|checks="+checks+"|scope=current production UTF8/detail/preview/retention;FULL Service routing separately scanned;not SAF/device");
    }
}
