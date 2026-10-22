package com.ethk3.novaonepro;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Append-compatible rendering with a 16Ki-character resident buffer. Large reports spill, never disappear. */
public final class ReportTextCP12 implements Appendable,CharSequence {
    public static final String REF_BEGIN="\u001eSTARCORE_REPORT_REF_CP12|",REF_END="\u001e";
    private static final int CHARS=16384;
    public interface Context {void checkpoint();File jobDirectory();void trackRenderer(ReportTextCP12 text);void releaseRenderer(ReportTextCP12 text);void recordTextArtifact(File file,String sha,long bytes)throws IOException;}
    private final Context context;private final StringBuilder buffer=new StringBuilder(CHARS);
    private Writer writer;private FileOutputStream fd;private File spool;private boolean finished;private String token;private long chars;
    public ReportTextCP12(Context context){this.context=Objects.requireNonNull(context);}
    public ReportTextCP12(Context context,int capacity){this(context);if(capacity<0)throw new NegativeArraySizeException();}
    public ReportTextCP12(Context context,String prefix){this(context);append(prefix);}
    public ReportTextCP12 append(CharSequence text){if(text==null)text="null";return append(text,0,text.length());}
    public ReportTextCP12 append(CharSequence text,int start,int end){if(text==null)text="null";if(start<0||end<start||end>text.length())throw new IndexOutOfBoundsException();
        try {check();String value=text.subSequence(start,end).toString();int at=0;
            while(at<value.length()){int ref=value.indexOf(REF_BEGIN,at);if(ref<0){literal(value,at,value.length());break;}literal(value,at,ref);int finish=value.indexOf(REF_END,ref+REF_BEGIN.length());if(finish<0)throw new IOException("REPORT_RENDER_REFERENCE_UNTERMINATED");
                String[] fields=value.substring(ref+REF_BEGIN.length(),finish).split("\\|",-1);if(fields.length!=3||!fields[0].matches("report-text-[0-9a-f]{64}\\.txt")||!fields[1].matches("[0-9a-f]{64}"))throw new IOException("REPORT_RENDER_REFERENCE");
                File file=ReportArchiveVerifierCP12.inside(context.jobDirectory(),fields[0]);long bytes=Long.parseLong(fields[2]);if(bytes<0||file.length()!=bytes||!fields[1].equals(ReportArchiveCP4.hash(file,()->{context.checkpoint();return true;})))throw new IOException("REPORT_RENDER_REFERENCE_HASH");
                try(Reader in=new InputStreamReader(new FileInputStream(file),StandardCharsets.UTF_8)){char[] block=new char[8192];int n;while((n=in.read(block))!=-1){context.checkpoint();literal(new String(block,0,n),0,n);}}
                at=finish+REF_END.length();
            }return this;
        }catch(Exception failure){throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_FAILED",failure);}
    }
    public ReportTextCP12 append(char value){try{check();if(buffer.length()==CHARS)flush();buffer.append(value);chars++;return this;}catch(IOException failure){throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_IO",failure);}}
    public ReportTextCP12 append(Object value){return append(String.valueOf(value));}public ReportTextCP12 append(int value){return append(Integer.toString(value));}public ReportTextCP12 append(long value){return append(Long.toString(value));}
    public ReportTextCP12 append(double value){return append(Double.toString(value));}public ReportTextCP12 append(float value){return append(Float.toString(value));}public ReportTextCP12 append(boolean value){return append(Boolean.toString(value));}
    private void check()throws IOException{context.checkpoint();if(finished)throw new IOException("REPORT_RENDER_ALREADY_FINALIZED");}
    private void literal(String value,int start,int end)throws IOException {for(int at=start;at<end;){context.checkpoint();if(buffer.length()==CHARS)flush();int n=Math.min(end-at,CHARS-buffer.length());buffer.append(value,at,at+n);chars+=n;at+=n;}}
    private void flush()throws IOException {if(writer==null){context.trackRenderer(this);spool=new File(context.jobDirectory(),"report-text-"+UUID.randomUUID().toString().replace("-","")+".partial");if(!spool.createNewFile())throw new IOException("REPORT_RENDER_EXCLUSIVE_FILE");fd=new FileOutputStream(spool);writer=new BufferedWriter(new OutputStreamWriter(fd,StandardCharsets.UTF_8),8192);}writer.append(buffer);buffer.setLength(0);}
    public void setLength(int length){if(writer!=null||finished||length<0||length>CHARS)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_RANDOM_ACCESS");buffer.setLength(length);chars=length;}
    public int length(){if(chars>Integer.MAX_VALUE)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_LENGTH");return (int)chars;}
    public char charAt(int at){if(writer!=null)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_RANDOM_ACCESS");return buffer.charAt(at);}
    public CharSequence subSequence(int start,int end){if(writer!=null)throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_RANDOM_ACCESS");return buffer.subSequence(start,end);}
    @Override public String toString(){if(token!=null)return token;if(writer==null)return buffer.toString();
        try{flush();writer.flush();fd.getFD().sync();writer.close();context.releaseRenderer(this);finished=true;String hash=ReportArchiveCP4.hash(spool,()->{context.checkpoint();return true;});File target=new File(context.jobDirectory(),"report-text-"+hash+".txt");if(target.exists()){if(target.length()!=spool.length()||!hash.equals(ReportArchiveCP4.hash(target,()->{context.checkpoint();return true;})))throw new IOException("REPORT_RENDER_CAS_CONFLICT");if(!spool.delete())throw new IOException("REPORT_RENDER_PARTIAL_REMOVE");}else if(!spool.renameTo(target))throw new IOException("REPORT_RENDER_PUBLISH");spool=target;context.recordTextArtifact(spool,hash,spool.length());token=REF_BEGIN+spool.getName()+"|"+hash+"|"+spool.length()+REF_END;return token;
        }catch(Exception failure){try{if(writer!=null)writer.close();}catch(IOException ignored){}context.releaseRenderer(this);finished=true;throw new ReportReadContextCP4.ReportReadFailureCP4("REPORT_RENDER_FINISH",failure);}
    }

    public Writer writer(){return new Writer(){public void write(char[] b,int off,int len)throws IOException{ReportTextCP12.this.append(new String(b,off,len));}public void flush(){}public void close(){ReportTextCP12.this.toString();}};}
    public void abort(){try{if(writer!=null)writer.close();}catch(IOException ignored){}if(spool!=null&&spool.getName().endsWith(".partial"))spool.delete();finished=true;context.releaseRenderer(this);}
    public static String sha256(Context context,String value){ReportTextCP12 full=new ReportTextCP12(context);full.append(value);String result=full.toString();if(result.startsWith(REF_BEGIN)&&result.endsWith(REF_END)){String[] fields=result.substring(REF_BEGIN.length(),result.length()-REF_END.length()).split("\\|",-1);return fields[1];}return V51ChecksumEngine.sha256(result);}
}
