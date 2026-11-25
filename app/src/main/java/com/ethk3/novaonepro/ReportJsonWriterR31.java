package com.ethk3.novaonepro;

import com.ethk3.novaonepro.strategyv2.JsonR27;
import android.util.AtomicFile;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Same canonical bytes as JsonR27 without materializing
 * the complete document String, its builder and Writer's complete char[] copy.
 * Direct writers own their publication lifecycle. */
public final class ReportJsonWriterR31 {
    private ReportJsonWriterR31() {}
    private static final int TEXT_CHUNK=4096;

    /** Bulk report JSON is admitted by its encoded size, using the same atomic
     * storage and path checks as report metadata without its journal size bound. */
    public static void writeReportArtifactAtomic(File file,Object value)throws IOException {
        class Size extends OutputStream {
            long bytes;
            @Override public void write(int value){bytes=Math.addExact(bytes,1L);}
            @Override public void write(byte[] values,int offset,int length){bytes=Math.addExact(bytes,(long)length);}
        }
        Size size=new Size();
        try(Writer counter=new BufferedWriter(new OutputStreamWriter(size,StandardCharsets.UTF_8))){
            write(value,counter);
        }
        File parent=file.getParentFile();
        if(parent==null||!parent.isDirectory()&&!parent.mkdirs())throw new IOException("REPORT_ARTIFACT_PARENT");
        try(ReportIoR31.AtomicPermit permit=ReportIoR31.beforeAtomic(file,size.bytes)){
            AtomicFile atomic=new AtomicFile(file);
            FileOutputStream out=atomic.startWrite();
            boolean published=false;
            try{
                Writer sink=new BufferedWriter(new OutputStreamWriter(permit.stream(out),StandardCharsets.UTF_8));
                write(value,sink);
                sink.flush();
                out.getFD().sync();
                atomic.finishWrite(out);
                published=true;
            }finally{
                if(!published)atomic.failWrite(out);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void write(Object value,Writer out)throws IOException {
        if(value==null){out.write("null");return;}
        if(value instanceof String){string((String)value,out);return;}
        if(value instanceof Boolean||value instanceof Number){out.write(JsonR27.write(value));return;}
        if(value instanceof Map){
            out.write('{');boolean first=true;
            for(Map.Entry<String,Object> entry:new TreeMap<>((Map<String,Object>)value).entrySet()){
                if(!first)out.write(',');first=false;string(entry.getKey(),out);out.write(':');write(entry.getValue(),out);
            }
            out.write('}');return;
        }
        if(value instanceof List){
            out.write('[');boolean first=true;
            for(Object item:(List<?>)value){if(!first)out.write(',');first=false;write(item,out);}
            out.write(']');return;
        }
        if(value instanceof double[]){
            out.write('[');double[] items=(double[])value;
            for(int i=0;i<items.length;i++){if(i>0)out.write(',');write(items[i],out);}
            out.write(']');return;
        }
        if(value instanceof int[]){
            out.write('[');int[] items=(int[])value;
            for(int i=0;i<items.length;i++){if(i>0)out.write(',');write(items[i],out);}
            out.write(']');return;
        }
        throw new IllegalArgumentException("R27_JSON_TYPE:"+value.getClass());
    }

    private static void string(String value,Writer out)throws IOException {
        out.write('"');int start=0;
        for(int i=0;i<value.length();i++){
            char c=value.charAt(i);String escape=null;
            switch(c){
                case '"':escape="\\\"";break;
                case '\\':escape="\\\\";break;
                case '\n':escape="\\n";break;
                case '\r':escape="\\r";break;
                case '\t':escape="\\t";break;
                default:if(c<0x20)escape=String.format(java.util.Locale.ROOT,"\\u%04x",(int)c);
            }
            if(escape!=null){text(value,start,i-start,out);out.write(escape);start=i+1;}
        }
        text(value,start,value.length()-start,out);out.write('"');
    }

    private static void text(String value,int offset,int count,Writer out)throws IOException {
        for(int remaining=count;remaining>0;){int chunk=Math.min(TEXT_CHUNK,remaining);out.write(value,offset,chunk);offset+=chunk;remaining-=chunk;}
    }

    /** Readable Markdown from the identical chapter list, with every data member retained. */
    public static void markdownR30(List<Map<String,Object>> chapters,Writer out)throws IOException {
        out.write("# STARCORE R30 检测报告（同一只读快照）\n\n12章来自同一只读事实快照。缺来源保持 UNKNOWN，零样本保持 INSUFFICIENT；已记录不能代替自然质量和真机验收。\n");
        for(Map<String,Object> chapter:chapters){
            out.write("\n## ");out.write(String.valueOf(chapter.get("sectionId")));out.write(' ');out.write(String.valueOf(chapter.get("title")));
            out.write("\n\n状态：");out.write(String.valueOf(chapter.get("status")));out.write("（");out.write(String.valueOf(chapter.get("reason")));out.write("）\n\n");
            if(chapter.containsKey("conclusion")){out.write("**结论（数据自动选择）**：");out.write(String.valueOf(chapter.get("conclusion")));out.write("\n\n");}
            Map<String,Object> rest=new LinkedHashMap<>(chapter);
            for(String key:new String[]{"sectionId","title","status","reason","conclusion","schemaVersion","evidenceLevel"})rest.remove(key);
            out.write("```json\n");write(rest,out);out.write("\n```\n");
        }
    }
}
