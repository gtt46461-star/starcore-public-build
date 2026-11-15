package com.ethk3.novaonepro;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.ReportR27;
import java.io.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;
/** Current formatter byte compatibility and bounded writer calls; never Android/device evidence. */
public final class R31ReportJsonStreamHostTest {
    static int checks;
    static void require(boolean value,String code){checks++;if(!value)throw new AssertionError(code);}
    static final class Bounded extends FilterWriter {
        int largest;
        Bounded(Writer out){super(out);}
        @Override public void write(String s,int start,int count)throws IOException{largest=Math.max(largest,count);require(count<=4096,"UNBOUNDED_STRING_COPY");out.write(s,start,count);}
        @Override public void write(char[] b,int start,int count)throws IOException{largest=Math.max(largest,count);require(count<=4096,"UNBOUNDED_CHARACTER_COPY");out.write(b,start,count);}
        @Override public void write(int c)throws IOException{out.write(c);}
    }
    static String stream(Object value)throws IOException{StringWriter body=new StringWriter();Bounded writer=new Bounded(body);ReportJsonWriterR31.write(value,writer);return body.toString();}
    public static void main(String[] args)throws Exception{
        Map<String,Object> value=new LinkedHashMap<>();value.put("z",null);value.put("a",Arrays.asList(true,false,0,Long.MIN_VALUE,1.0,-0.0,1e25,Double.MIN_VALUE));
        value.put("arrays",Arrays.asList(new int[]{1,-2,3},new double[]{1.25,2.0}));value.put("escape","\"\\\n\r\t\b\f\u0000中文🌟");
        StringBuilder longText=new StringBuilder();for(int i=0;i<65536;i++)longText.append("x🌟\n");value.put("large",longText.toString());
        require(JsonR27.write(value).equals(stream(value)),"CANONICAL_TYPED_UNICODE_BYTES_DIFFER");
        for(Object bad:new Object[]{Double.NaN,Double.POSITIVE_INFINITY,new Object()}){
            try{stream(bad);throw new AssertionError("BAD_VALUE_ACCEPTED");}catch(IllegalArgumentException expected){checks++;}
        }
        try{ReportJsonWriterR31.write(value,new Writer(){public void write(char[] b,int o,int n)throws IOException{throw new IOException("EXPECTED_SINK_FAILURE");}public void flush(){}public void close(){}});throw new AssertionError("SINK_FAILURE_SWALLOWED");}
        catch(IOException expected){require("EXPECTED_SINK_FAILURE".equals(expected.getMessage()),"SINK_FAILURE_CHANGED");}
        long historicalBytes=0;
        if(args.length>0){
            Path path=Paths.get(args[0]);byte[] original=Files.readAllBytes(path);historicalBytes=original.length;
            @SuppressWarnings("unchecked") Map<String,Object> report=(Map<String,Object>)JsonR27.parse(new String(original,StandardCharsets.UTF_8));
            require(JsonR27.write(report).equals(stream(report)),"REAL_REPORT_CANONICAL_BYTES_DIFFER");
            @SuppressWarnings("unchecked") List<Map<String,Object>> chapters=(List<Map<String,Object>>)report.get("chapters");
            StringWriter markdown=new StringWriter();ReportJsonWriterR31.markdownR30(chapters,new Bounded(markdown));
            require(ReportR27.markdownR30(chapters).equals(markdown.toString()),"REAL_REPORT_MARKDOWN_BYTES_DIFFER");
        }
        System.out.println("R31_REPORT_STREAM_HOST_PASS assertions="+checks+" historicalBytes="+historicalBytes+" CANONICAL_UNICODE_NUMERIC_ARRAYS_BOUNDED_4096_IO_FAILURE_PRESERVED HOST_ONLY");
    }
}
