package com.ethk3.novaonepro;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.security.*;
import java.util.*;

/** Lossless SQLite cell wire format. Names, types, lengths and original bytes are all hashed. */
public final class FaultOriginalCodecR335 {
    private FaultOriginalCodecR335() {}
    public static final String CODEC="R33_FAULT_ROW_V1";
    public static final int MAX_BYTES=65536;
    public static final String COLUMNS="event_id,task_id,owner_epoch,fence,operation,phase,category,root_fingerprint,minute_bucket,cause_chain_json,stack,stack_hash,last_cause_chain_json,last_stack,last_stack_hash,commit_outcome,boot_id,first_wall_ms,last_wall_ms,first_elapsed_ms,last_elapsed_ms,occurrences,byte_count";
    public static final String[] NAMES=COLUMNS.split(",");
    private static final byte[] MAGIC=(CODEC+"\0").getBytes(StandardCharsets.US_ASCII);

    public static final class Cell {
        public final char type;
        private final byte[] original;
        public Cell(char type,byte[] original)throws IOException {
            if(original==null||original.length>MAX_BYTES||"NIFTB".indexOf(type)<0
                    ||type=='N'&&original.length!=0||(type=='I'||type=='F')&&original.length!=8)
                throw new IOException("FAULT_ORIGINAL_CELL");
            this.type=type;this.original=original.clone();
        }
        public byte[] bytes(){return original.clone();}
        public Object value()throws IOException {
            if(type=='N')return null;
            if(type=='I')return ByteBuffer.wrap(original).getLong();
            if(type=='F')return Double.longBitsToDouble(ByteBuffer.wrap(original).getLong());
            if(type=='B')return bytes();
            try{return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(original)).toString();}
            catch(CharacterCodingException invalid){throw new IOException("FAULT_ORIGINAL_TEXT_ENCODING",invalid);}
        }
    }
    public static byte[] encode(Cell[] cells)throws IOException {
        if(cells==null||cells.length!=NAMES.length)throw new IOException("FAULT_ORIGINAL_COLUMNS");
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        try(DataOutputStream out=new DataOutputStream(buffer)){
            out.write(MAGIC);
            for(int i=0;i<cells.length;i++){
                Cell cell=cells[i];if(cell==null)throw new IOException("FAULT_ORIGINAL_NULL_CELL");
                byte[] name=NAMES[i].getBytes(StandardCharsets.US_ASCII);
                if((long)buffer.size()+2+name.length+1+8+cell.original.length>MAX_BYTES)
                    throw new IOException("FAULT_ORIGINAL_BYTE_BOUND");
                out.writeShort(name.length);out.write(name);out.writeByte(cell.type);
                out.writeLong(cell.original.length);out.write(cell.original);
            }
        }
        return buffer.toByteArray();
    }
    public static Cell[] decode(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length>MAX_BYTES)throw new IOException("FAULT_ORIGINAL_BYTE_BOUND");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){
            byte[] magic=new byte[MAGIC.length];in.readFully(magic);if(!Arrays.equals(magic,MAGIC))throw new IOException("FAULT_ORIGINAL_CODEC");
            Cell[] out=new Cell[NAMES.length];
            for(int i=0;i<out.length;i++){
                int n=in.readUnsignedShort();byte[] expected=NAMES[i].getBytes(StandardCharsets.US_ASCII);
                if(n!=expected.length)throw new IOException("FAULT_ORIGINAL_COLUMN_NAME");
                byte[] name=new byte[n];in.readFully(name);if(!Arrays.equals(name,expected))throw new IOException("FAULT_ORIGINAL_COLUMN_NAME");
                char type=(char)in.readUnsignedByte();long length=in.readLong();
                if(length<0||length>MAX_BYTES||length>in.available())throw new IOException("FAULT_ORIGINAL_CELL_BOUND");
                byte[] value=new byte[(int)length];in.readFully(value);out[i]=new Cell(type,value);
            }
            if(in.read()!=-1)throw new IOException("FAULT_ORIGINAL_TRAILING_BYTES");
            return out;
        }
    }
    public static String sha256(byte[] bytes){
        try {StringBuilder out=new StringBuilder(64);for(byte b:MessageDigest.getInstance("SHA-256").digest(bytes))out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();}
        catch(NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
    public static Map<String,Object> values(byte[] bytes)throws IOException {
        Cell[] cells=decode(bytes);Map<String,Object> out=new LinkedHashMap<>();for(int i=0;i<cells.length;i++)out.put(NAMES[i],cells[i].value());return out;
    }
}
