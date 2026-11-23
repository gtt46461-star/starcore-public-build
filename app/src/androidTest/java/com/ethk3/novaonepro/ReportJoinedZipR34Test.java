package com.ethk3.novaonepro;

import java.io.*;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class ReportJoinedZipR34Test {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();

    @Test public void partsSaveAsOneReopenableZipAndRejectDamagedSourcesAndDestinations() throws Exception {
        File job=temporary.newFolder("job"), delivery=new File(job,"delivery");
        assertTrue(delivery.mkdir());
        File original=temporary.newFile("original.zip"), saved=temporary.newFile("saved.zip");
        byte[] buffer=new byte[32768];
        long limit=ReportPublicationCP12.ZIP_LIMIT_R30;
        try(ZipOutputStream zip=new ZipOutputStream(new FileOutputStream(original))){
            zip.setLevel(0);
            zip.putNextEntry(new ZipEntry("evidence.bin"));
            for(long n=0;n<=limit;n+=buffer.length)zip.write(buffer);
            zip.closeEntry();
        }
        ReportNativeCP4.Control active=()->true;
        String wholeSha=ReportArchiveCP4.hash(original,active);
        JSONArray parts=new JSONArray();File last=null;
        try(InputStream input=new FileInputStream(original)){
            for(long offset=0;offset<original.length();offset+=limit){
                int number=parts.length()+1;
                String name=String.format(Locale.ROOT,"STARCORE_FULL_REPORT.zip.part%02d",number);
                last=new File(delivery,name);
                long size=Math.min(limit,original.length()-offset);
                try(OutputStream output=new FileOutputStream(last)){
                    for(long left=size;left>0;){
                        int read=input.read(buffer,0,(int)Math.min(left,buffer.length));
                        assertTrue(read>0);output.write(buffer,0,read);left-=read;
                    }
                }
                parts.put(new JSONObject().put("kind","ZIP_PART").put("name",name)
                    .put("path","delivery/"+name).put("partNumber",number).put("offset",offset)
                    .put("bytes",size).put("sha256",ReportArchiveCP4.hash(last,active)));
            }
        }
        JSONObject index=new JSONObject().put("format",ReportPublicationCP12.ZIP_PART_FORMAT_R30)
            .put("files",parts).put("zipBytes",original.length()).put("totalVolumeBytes",original.length())
            .put("zipSha256",wholeSha).put("expectedVolumes",2).put("exportedVolumes",2).put("omittedVolumes",0);
        boolean[] corruptRead={false};
        ReportSafCopyCP12.Destination destination=new ReportSafCopyCP12.Destination(){
            public OutputStream openOutputTruncating()throws IOException{return new FileOutputStream(saved);}
            public InputStream openInput()throws IOException{return corruptRead[0]?new ByteArrayInputStream(new byte[]{1}):new FileInputStream(saved);}
        };
        assertEquals(wholeSha,ReportPublicationCP12.saveJoinedZipR34(job,index,destination,active).sha256);
        try(ZipFile zip=new ZipFile(saved)){assertEquals(limit+buffer.length,zip.getEntry("evidence.bin").getSize());}
        corruptRead[0]=true;
        try{
            ReportPublicationCP12.saveJoinedZipR34(job,index,destination,active);
            fail("A changed provider readback cannot receive a receipt");
        }catch(IOException expected){assertEquals("REPORT_SAF_VERIFY_FAILED",expected.getMessage());}
        corruptRead[0]=false;
        try(RandomAccessFile damaged=new RandomAccessFile(last,"rw")){int first=damaged.read();damaged.seek(0);damaged.write(first^1);}
        try{
            ReportPublicationCP12.saveJoinedZipR34(job,index,destination,active);
            fail("A changed part cannot replace the saved ZIP");
        }catch(IOException expected){assertEquals("R30_ZIP_PART_HASH",expected.getMessage());}
        assertEquals(wholeSha,ReportArchiveCP4.hash(saved,active));
    }
}
