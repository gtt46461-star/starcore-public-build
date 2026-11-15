package com.ethk3.novaonepro.r952;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Deterministic bounded asset pages. Director IMPORT is the only dispatcher; no model or REAL authority. */
public final class MoleculeSeedPageCP12 {
    public static final int ROWS=4200, BYTES=3265020, PAGE_ROWS=25, PAGE_BYTES=65536, LINE_BYTES=8192;
    public static final String SHA="b4ab9a61e2a36a4a426d0588ffb52350a25c09165273f802b9aa4c63e3c726ad";
    public interface NanoClock { long now(); }
    public static final class Cursor {
        public final int row; public final long offset,generation; public final String prefix;
        private final byte[] state;
        public Cursor(int row,long offset,long generation,String prefix,byte[] state) {
            need(row>=0&&row<=ROWS&&offset>=0&&offset<=BYTES&&generation>=0,"SEED_CURSOR_RANGE");
            ResumableSha256CP12 sha=ResumableSha256CP12.restore(state,offset);
            need(sha.digestHex().equals(prefix),"SEED_CURSOR_SHA");
            this.row=row;this.offset=offset;this.generation=generation;this.prefix=prefix;this.state=state.clone();
        }
        public static Cursor zero(){ResumableSha256CP12 s=new ResumableSha256CP12();return new Cursor(0,0,0,s.digestHex(),s.checkpoint());}
        public byte[] shaState(){return state.clone();}
        public boolean fullyVerified(){return row==ROWS&&offset==BYTES&&prefix.equals(SHA);}
    }
    public static final class Page {
        public final Cursor before,after;public final List<String> rawRows;public final int bytesRead;
        private Page(Cursor before,Cursor after,List<String> rows,int bytesRead){this.before=before;this.after=after;this.rawRows=Collections.unmodifiableList(new ArrayList<>(rows));this.bytesRead=bytesRead;}
    }
    /** Caller must open precisely at before.offset. At most 64KiB is read; resume never skips prefix.
     * An unread or uncommitted tail is read again from after.offset, never hashed twice.
     * Time budget checked per bounded line, so completed earlier lines can commit on yield. */
    public static Page prepare(InputStream input,Cursor before,int stopRow,long deadlineNanos,NanoClock clock)throws IOException {
        need(input!=null&&before!=null&&clock!=null&&stopRow>=before.row&&stopRow<=ROWS,"SEED_PAGE_INPUT");
        if(before.row==stopRow||clock.now()>=deadlineNanos)return null;
        final int[] readCount={0};
        InputStream limited=new FilterInputStream(input){
            public int read()throws IOException{if(readCount[0]>=PAGE_BYTES)return -1;int value=in.read();if(value>=0)readCount[0]++;return value;}
            public int read(byte[] b,int off,int len)throws IOException{if(len==0)return 0;if(readCount[0]>=PAGE_BYTES)return -1;int n=in.read(b,off,Math.min(len,PAGE_BYTES-readCount[0]));if(n>0)readCount[0]+=n;return n;}
        };
        BufferedInputStream buffered=new BufferedInputStream(limited,8192);
        ResumableSha256CP12 sha=ResumableSha256CP12.restore(before.state,before.offset);
        List<String> rows=new ArrayList<>();int consumed=0;
        while(rows.size()<PAGE_ROWS&&before.row+rows.size()<stopRow) {
            // Reserve enough room for one maximum-sized line; completed lines are preserved.
            if(clock.now()>=deadlineNanos||PAGE_BYTES-consumed<LINE_BYTES+1)break;
            ByteArrayOutputStream raw=new ByteArrayOutputStream(1024);int value;
            while((value=buffered.read())!=-1){need(value!='\r',"SEED_NONCANONICAL_NEWLINE");raw.write(value);need(raw.size()<=LINE_BYTES+1,"SEED_LINE_BOUND");if(value=='\n')break;}
            if(value<0)throw new EOFException("SEED_ASSET_TRUNCATED");
            byte[] bytes=raw.toByteArray();need(bytes.length>1,"SEED_EMPTY_LINE");
            String line=new String(bytes,0,bytes.length-1,StandardCharsets.UTF_8);
            need(Arrays.equals(Arrays.copyOf(bytes,bytes.length-1),line.getBytes(StandardCharsets.UTF_8)),"SEED_UTF8");
            rows.add(line);sha.update(bytes,0,bytes.length);consumed+=bytes.length;
        }
        if(rows.isEmpty())return null;
        Cursor after=new Cursor(before.row+rows.size(),before.offset+consumed,before.generation+1,sha.digestHex(),sha.checkpoint());
        if(after.row==ROWS)need(after.fullyVerified(),"SEED_FULL_ASSET_IDENTITY");
        else need(after.offset<BYTES,"SEED_PREMATURE_EOF");
        return new Page(before,after,rows,readCount[0]);
    }
    /** Only completed, strictly compiled rows may advance the materialization cursor. */
    public static Page completedPrefix(Page raw,int count) {
        need(raw!=null&&count>0&&count<=raw.rawRows.size(),"SEED_COMPLETED_PREFIX");
        ResumableSha256CP12 digest=ResumableSha256CP12.restore(raw.before.state,raw.before.offset);
        long offset=raw.before.offset;List<String> rows=new ArrayList<>();
        for(int i=0;i<count;i++){String line=raw.rawRows.get(i);byte[] bytes=(line+"\n").getBytes(StandardCharsets.UTF_8);digest.update(bytes,0,bytes.length);offset+=bytes.length;rows.add(line);}
        Cursor after=new Cursor(raw.before.row+count,offset,raw.before.generation+1,digest.digestHex(),digest.checkpoint());
        if(after.row==ROWS)need(after.fullyVerified(),"SEED_FULL_ASSET_IDENTITY");
        return new Page(raw.before,after,rows,raw.bytesRead);
    }
    private static void need(boolean ok,String code){if(!ok)throw new IllegalStateException(code);}
}
