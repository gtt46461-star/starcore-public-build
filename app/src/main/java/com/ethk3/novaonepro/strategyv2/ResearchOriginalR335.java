package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.util.JsonReader;
import android.util.JsonToken;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Verifies the complete stored research summary without putting it or its frame in a CursorWindow. */
public final class ResearchOriginalR335 {
    public final Map<String,Object> fields = new HashMap<>();
    public String originalHash, frameHash;
    private static final Set<String> PROJECTIONS = new HashSet<>(Arrays.asList("sourceContract","viewId",
            "inputHash","computationNs","mean","lower95","upper95","componentPath","counterfactualHash",
            "status","evaluatedIssues"));
    private ResearchOriginalR335() {}

    private static MessageDigest digest() {
        try { return MessageDigest.getInstance("SHA-256"); }
        catch (NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String hex(byte[] data) {
        StringBuilder s=new StringBuilder(2*data.length);
        for(byte b:data) s.append(Character.forDigit((b&255) >> 4,16)).append(Character.forDigit(b&15,16));
        return s.toString();
    }
    private static void canonical(JsonReader in, Writer out, CancellationSignal cancel) throws IOException {
        if (cancel!=null) cancel.throwIfCanceled();
        switch (in.peek()) {
            case BEGIN_OBJECT:
                in.beginObject(); out.write('{'); String previous=null;
                while(in.hasNext()) {
                    String key=in.nextName();
                    if(previous!=null) {
                        if(previous.compareTo(key)>=0) throw new IOException("UNORDERED_OR_DUPLICATE_FRAME_KEY");
                        out.write(',');
                    }
                    out.write(JsonR27.write(key)); out.write(':'); canonical(in,out,cancel); previous=key;
                }
                in.endObject(); out.write('}'); return;
            case BEGIN_ARRAY:
                in.beginArray(); out.write('['); boolean first=true;
                while(in.hasNext()) { if(!first) out.write(','); canonical(in,out,cancel); first=false; }
                in.endArray(); out.write(']'); return;
            case STRING: out.write(JsonR27.write(in.nextString())); return;
            case NUMBER: out.write(in.nextString()); return;
            case BOOLEAN: out.write(Boolean.toString(in.nextBoolean())); return;
            case NULL: in.nextNull(); out.write("null"); return;
            default: throw new IOException("INVALID_ORIGINAL_TOKEN");
        }
    }
    public static ResearchOriginalR335 read(SQLiteDatabase db,String run,CancellationSignal cancel) {
        return read(db,run,com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26,cancel,true);
    }
    /** Archived frames may be displayed after their original source and digest are verified. */
    public static ResearchOriginalR335 readForDisplay(SQLiteDatabase db,String run,String sourceCommit,CancellationSignal cancel){
        return read(db,run,sourceCommit,cancel,com.ethk3.novaonepro.BuildConfig.SOURCE_COMMIT_R26.equals(sourceCommit));
    }
    private static ResearchOriginalR335 read(SQLiteDatabase db,String run,String commit,CancellationSignal cancel,boolean current) {
        if(commit==null||!commit.matches("[0-9a-f]{40}"))throw new IllegalStateException("RESEARCH_ORIGINAL_COMMIT");
        if(cancel!=null) cancel.throwIfCanceled();
        String sql="SELECT CAST(summary_json AS BLOB) FROM r30r1_analysis_run WHERE run_id=? AND source_commit=? AND status='COMMITTED'";
        try(SQLiteStatement statement=db.compileStatement(sql)) {
            statement.bindString(1,run);statement.bindString(2,commit);
            ParcelFileDescriptor descriptor=statement.simpleQueryForBlobFileDescriptor();
            if(descriptor==null) throw new IllegalStateException("MISSING_ORIGINAL_RESEARCH_SUMMARY");
            MessageDigest full=digest();
            try(JsonReader reader=new JsonReader(new InputStreamReader(new DigestInputStream(
                    new ParcelFileDescriptor.AutoCloseInputStream(descriptor),full),StandardCharsets.UTF_8))) {
                ResearchOriginalR335 result=new ResearchOriginalR335();
                reader.beginObject();
                while(reader.hasNext()) {
                    String key=reader.nextName();
                    if("frame".equals(key)) {
                        if(reader.peek()!=JsonToken.BEGIN_OBJECT || result.frameHash!=null) throw new IOException("INVALID_MACHINE_FRAME");
                        MessageDigest sha=digest();
                        OutputStream sink=new OutputStream() {
                            public void write(int b){sha.update((byte)b);}
                            public void write(byte[] bytes,int start,int length){sha.update(bytes,start,length);}
                        };
                        Writer writer=new BufferedWriter(new OutputStreamWriter(sink,StandardCharsets.UTF_8));
                        canonical(reader,writer,cancel); writer.flush(); result.frameHash=hex(sha.digest());
                    } else if(PROJECTIONS.contains(key)) {
                        if(result.fields.containsKey(key)) throw new IOException("DUPLICATE_ORIGINAL_FIELD");
                        StringWriter value=new StringWriter(); canonical(reader,value,cancel);
                        result.fields.put(key,JsonR27.parse(value.toString()));
                    } else reader.skipValue();
                }
                reader.endObject();
                if(reader.peek()!=JsonToken.END_DOCUMENT) throw new IOException("ORIGINAL_TRAILING_DATA");
                result.originalHash=hex(full.digest());
                Object raw=result.fields.get("sourceContract");
                if(!(raw instanceof Map)||!commit.equals(((Map<?,?>)raw).get("source")))
                    throw new IllegalStateException("ORIGINAL_RESEARCH_SOURCE_MISMATCH");
                if(current&&!ResearchStoreR30R1.currentSource(commit,JsonR27.write(Collections.singletonMap("sourceContract",raw))))
                    throw new IllegalStateException("CURRENT_ORIGINAL_RESEARCH_SOURCE_REQUIRED");
                return result;
            }
        } catch(IOException failure) { throw new IllegalStateException("RESEARCH_ORIGINAL_READ_FAILED",failure); }
    }
}
