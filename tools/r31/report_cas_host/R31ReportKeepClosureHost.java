package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.KeepMigrationStoreR31;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import org.json.*;

/** Real Keep row codec/CAS, persisted SQLite registry, export metadata and the
 * same production exact-byte copy function. No Android Service/SAF claim. */
public final class R31ReportKeepClosureHost {
    static int assertions;static File work,files,root;static final String ARCHIVE="r30_keep_r27_policy";
    static final ReportNativeCP4.Control RUN=()->true;
    static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    interface Action {void run()throws Exception;}
    static void fails(Action action,String...codes)throws Exception{
        try{action.run();throw new AssertionError("EXPECTED_PRODUCTION_FAILURE");}
        catch(IOException expected){for(String code:codes)if(String.valueOf(expected.getMessage()).contains(code)){assertions++;return;}throw expected;}
    }
    static SQLiteDatabase read(File db){SQLiteDatabase out=SQLiteDatabase.openDatabase(db.getPath(),null,SQLiteDatabase.OPEN_READONLY);need(out.isReadOnly(),"real native readonly connection");return out;}
    static File job(String name)throws IOException{File p=new File(work,"jobs/"+name);Files.createDirectories(p.toPath());return p;}
    static File fixture(String name,int rows,boolean large)throws Exception{
        File db=new File(work,name+".db");
        try(SQLiteDatabase write=SQLiteDatabase.openDatabase(db.getPath(),null,0)){
            for(String sql:KeepMigrationStoreR31.DDL)write.execSQL(sql);
            write.execSQL("CREATE TABLE "+ARCHIVE+"(__r30_id INTEGER PRIMARY KEY,__r30_at INTEGER,__r30_reason TEXT,policy_hash TEXT,payload BLOB,nullable TEXT,real_value REAL)");
            KeepMigrationStoreR31 store=new KeepMigrationStoreR31(write,root,(operation,body)->{throw new AssertionError("REPORT_FIXTURE_MUST_NOT_INVOKE_MIGRATION_WRITER");});
            for(int i=1;i<=rows;i++){
                byte[] payload=new byte[large&&i==rows?(8<<20)+257:193+i];for(int j=0;j<payload.length;j++)payload[j]=(byte)(i+j*31);
                write.execSQL("INSERT INTO "+ARCHIVE+" VALUES(?,?,?, ?,?,NULL,?)",new Object[]{i,1234L+i,"NATIVE_CODEC_FIXTURE","policy-"+i,payload,.125+i});
                String hash=store.rowDigest(ARCHIVE,i);File original=new File(root,hash+".row");need(original.isFile()&&original.length()>payload.length,"actual rowDigest produces complete codec original");
                write.execSQL("INSERT INTO r31_keep_original VALUES(?,?,?,?,?,?,?,?)",new Object[]{ARCHIVE,i,hash,original.length(),"[]",rows>1&&i==rows?"PRODUCER_PREPARED":"MIGRATED_ORIGINAL_RETAINED",1000,0});
            }
            KeepMigrationStoreR31.unregisterLiveRoot(write);
        }return db;
    }
    static JSONObject capture(SQLiteDatabase db,String name,boolean compact,Writer descriptors,ReportNativeCP4.Control control)throws Exception{
        return ReportAttachmentClosureCP12.captureKeepOriginalsR31(db,files,job(name),descriptors,control,compact);
    }
    static List<JSONObject> lines(String text)throws Exception{List<JSONObject> result=new ArrayList<>();for(String line:text.split("\n"))if(!line.isEmpty())result.add(new JSONObject(line));return result;}
    static void copy(File input,File output,long bytes,String hash,ReportNativeCP4.Control control)throws Exception{
        Method method=ReportAttachmentClosureCP12.class.getDeclaredMethod("copy",InputStream.class,File.class,long.class,String.class,ReportNativeCP4.Control.class);method.setAccessible(true);
        try(InputStream stream=new FileInputStream(input)){try{method.invoke(null,stream,output,bytes,hash,control);}catch(InvocationTargetException e){Throwable cause=e.getCause();if(cause instanceof Exception)throw (Exception)cause;throw (Error)cause;}}
    }
    static void rawAndPagination()throws Exception{
        File db=fixture("many",130,true);StringWriter plans=new StringWriter();
        try(SQLiteDatabase snapshot=read(db);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            String previous="";int seen=0,pages=0;long expectedBytes=0;
            for(;;){List<KeepMigrationStoreR31.ArtifactRef> page=KeepMigrationStoreR31.snapshotArtifactPage(snapshot,previous,64);if(page.isEmpty())break;pages++;need(page.size()<=64,"keyset page bound");for(KeepMigrationStoreR31.ArtifactRef ref:page){need(ref.hash.compareTo(previous)>0,"keyset strictly advances");previous=ref.hash;seen++;expectedBytes+=ref.bytes;}}
            need(seen==130&&pages==3,"all persisted states cross exact 64-object pagination");
            int readonlyBefore=SQLiteDatabase.readonlyWriteAttempts;
            JSONObject receipt=capture(snapshot,"raw",false,plans,RUN);need(receipt.getLong("objects")==130&&receipt.getLong("registeredBytes")==expectedBytes,"snapshot registry exact object/byte denominator");
            need(receipt.getBoolean("bodiesIncluded")&&receipt.getLong("verifiedBodies")==130&&receipt.getLong("unavailableBodies")==0,"raw body verification is actual");
            List<JSONObject> rows=lines(plans.toString());need(rows.size()==131,"130 exact body descriptors and one metadata descriptor");int copied=0;
            for(JSONObject row:rows)if(row.getString("sourceKind").equals("R31_KEEP_ORIGINAL")){
                File input=new File(row.getString("source")),output=new File(job("raw-readback"),row.getString("name"));long bytes=row.getLong("bytes");String hash=row.getString("sha256");
                need(input.getName().equals(hash+".row")&&row.getString("name").equals("keep-original-"+hash+".row"),"real descriptor has content-addressed identity");
                copy(input,output,bytes,hash,RUN);need(output.length()==bytes&&hash.equals(ReportArchiveCP4.hash(output,RUN)),"production generic copy/readback hashes actual complete bytes");copied++;
            }need(copied==130,"every raw descriptor really copied and read back");
            StringWriter compactPlan=new StringWriter();JSONObject compact=capture(snapshot,"compact",true,compactPlan,RUN);
            need(!compact.getBoolean("bodiesIncluded")&&compact.getLong("bodyVerificationNotRun")==1&&compact.getLong("verifiedBodies")==129,"compact large original stays NOT_RUN under the original 8MiB budget");
            need(lines(compactPlan.toString()).size()==1&&lines(compactPlan.toString()).get(0).getString("sourceKind").equals("FILE"),"compact exports metadata only and no pretend body descriptor");
            for(JSONObject row:lines(Files.readString(new File(job("compact"),"r31-keep-originals.jsonl").toPath())))need(!row.getBoolean("bodyIncluded"),"all compact rows explicitly omit body");
            need(SQLiteDatabase.readonlyWriteAttempts==readonlyBefore,"actual closure capture never attempts SQLite DDL or DML");
        }
    }
    static void missingCorruptAndSchema()throws Exception{
        File db=fixture("missing",1,false);String hash;long bytes;
        try(SQLiteDatabase snapshot=read(db)){KeepMigrationStoreR31.ArtifactRef ref=KeepMigrationStoreR31.snapshotArtifactPage(snapshot,"",1).get(0);hash=ref.hash;bytes=ref.bytes;}
        File original=new File(root,hash+".row");byte[] saved=Files.readAllBytes(original.toPath());
        try(SQLiteDatabase snapshot=read(db);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            Files.delete(original.toPath());fails(()->capture(snapshot,"raw-missing",false,new StringWriter(),RUN),"REPORT_KEEP_ORIGINAL_UNAVAILABLE");
            JSONObject missing=capture(snapshot,"compact-missing",true,new StringWriter(),RUN);need(missing.getLong("unavailableBodies")==1&&missing.getLong("verifiedBodies")==0,"missing compact body is unavailable, never healthy zero");
            Files.write(original.toPath(),saved);byte[] corrupt=saved.clone();corrupt[corrupt.length-1]^=1;Files.write(original.toPath(),corrupt);
            fails(()->capture(snapshot,"raw-corrupt",false,new StringWriter(),RUN),"REPORT_KEEP_ORIGINAL_UNAVAILABLE");
            JSONObject bad=capture(snapshot,"compact-corrupt",true,new StringWriter(),RUN);need(bad.getLong("unavailableBodies")==1&&bad.getLong("registeredBytes")==bytes,"compact checksum failure retains its actual denominator");
            need(lines(Files.readString(new File(job("compact-corrupt"),"r31-keep-originals.jsonl").toPath())).get(0).getString("availability").equals("CORRUPT_HASH_MISMATCH"),"corrupt and missing are distinguished");
            Files.write(original.toPath(),saved);
            File cancelled=new File(job("copy-cancel"),"fixed.row");
            fails(()->copy(original,cancelled,bytes,hash,()->false),"REPORT_CANCELLED");need(!cancelled.exists(),"cancelled production copy never publishes fixed bytes");
            File wrong=new File(job("copy-identity"),"fixed.row");
            fails(()->copy(original,wrong,bytes,String.format(Locale.ROOT,"%064x",99),RUN),"REPORT_ATTACHMENT_COPY_HASH");need(!wrong.exists(),"wrong-identity production copy never publishes fixed bytes");
        }
        File absent=new File(work,"absent.db");try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+absent);Statement s=c.createStatement()){s.execute("CREATE TABLE fixture(id INTEGER PRIMARY KEY)");}
        try(SQLiteDatabase snapshot=read(absent);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            JSONObject unknown=capture(snapshot,"no-schema",true,new StringWriter(),RUN);need(unknown.isNull("objects")&&unknown.isNull("registeredBytes")&&unknown.getString("state").equals("SCHEMA_NOT_PRESENT"),"missing schema reports UNKNOWN denominators");
            fails(()->capture(snapshot,"no-schema-cancel",true,new StringWriter(),()->false),"REPORT_CANCELLED");
        }
        File partial=new File(work,"partial.db");try(SQLiteDatabase write=SQLiteDatabase.openDatabase(partial.getPath(),null,0)){write.execSQL(KeepMigrationStoreR31.DDL[1]);}
        try(SQLiteDatabase snapshot=read(partial);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            JSONObject unknown=capture(snapshot,"partial-schema",true,new StringWriter(),RUN);need(unknown.isNull("objects")&&unknown.isNull("registeredBytes"),"partial registry must not masquerade as a measured empty lifecycle");
            fails(()->capture(snapshot,"partial-schema-raw",false,new StringWriter(),RUN),"REPORT_KEEP_REGISTRY_PARTIAL");
        }
    }
    static void pinsReadOnlyCancelAndSymlinks()throws Exception{
        File db=fixture("pins",1,false),other=new File(work,"other/files");Files.createDirectories(new File(other,"r31-keep").toPath());
        try(SQLiteDatabase snapshot=read(db)){
            fails(()->ReportExportServiceCP4.openKeepSourceR31(snapshot,files),"REPORT_KEEP_INPUT_PIN_REQUIRED");
            fails(()->capture(snapshot,"no-pin",true,new StringWriter(),RUN),"REPORT_KEEP_INPUT_PIN_REQUIRED");
            try(ReportInputsLeaseCP12 exclusive=ReportInputsLeaseCP12.tryAcquireExclusive(files)){need(exclusive!=null,"exclusive fixture acquired");fails(()->ReportExportServiceCP4.openKeepSourceR31(snapshot,files),"REPORT_KEEP_INPUT_PIN_REQUIRED");}
            try(ReportInputsLeaseCP12 wrong=ReportInputsLeaseCP12.acquire(other)){
                fails(()->ReportExportServiceCP4.openKeepSourceR31(snapshot,files),"REPORT_KEEP_INPUT_PIN_REQUIRED");fails(()->capture(snapshot,"wrong-pin",true,new StringWriter(),RUN),"REPORT_KEEP_INPUT_PIN_REQUIRED");
            }
            try(ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
                try(KeepMigrationStoreR31.ReaderRootLease mapping=ReportExportServiceCP4.openKeepSourceR31(snapshot,files)){
                    Object[] row=KeepMigrationStoreR31.readOriginal(snapshot,ARCHIVE,1,"payload","nullable","real_value");need(row!=null&&row[0] instanceof byte[]&&row[1]==null&&((Number)row[2]).doubleValue()==1.125,"actual complete CAS consumer decodes SQLite storage classes through scoped explicit root");
                }
                try{KeepMigrationStoreR31.readOriginal(snapshot,ARCHIVE,1,"payload");throw new AssertionError("SNAPSHOT_ROOT_GUESSED_AFTER_SCOPE_EXIT");}
                catch(IllegalStateException expected){need(expected.getMessage().equals("R31_KEEP_ORIGINAL_READER_INTEGRITY"),"snapshot reader cannot infer a keep root from its DB path after scoped lease exit");}
                snapshot.jdbc.setAutoCommit(false);
                try{fails(()->capture(snapshot,"active-read-transaction",true,new StringWriter(),RUN),"REPORT_KEEP_READONLY_SNAPSHOT_REQUIRED");}
                finally{snapshot.jdbc.rollback();snapshot.jdbc.setAutoCommit(true);}
                fails(()->capture(snapshot,"cancelled",true,new StringWriter(),()->false),"REPORT_CANCELLED");
                Thread.currentThread().interrupt();
                try{fails(()->capture(snapshot,"thread-interrupted",true,new StringWriter(),RUN),"REPORT_CANCELLED");}
                finally{Thread.interrupted();}
                try{snapshot.execSQL("DELETE FROM r31_keep_original");throw new AssertionError("ACTUAL_READONLY_NATIVE_CONNECTION_WRITABLE");}catch(android.database.sqlite.SQLiteException expected){need(expected.getMessage().contains("READONLY"),"native SQLite rejects report writes");}
                need(ReportInputsLeaseCP12.tryAcquireExclusive(files)==null,"reader scope exit does not release the actual outer file pin");
            }
        }
        need(!ReportInputsLeaseCP12.currentThreadHoldsInput(files),"all shared path references are released");try(ReportInputsLeaseCP12 released=ReportInputsLeaseCP12.tryAcquireExclusive(files)){need(released!=null,"exclusive available only after actual input scopes exit");}
        try(SQLiteDatabase write=SQLiteDatabase.openDatabase(db.getPath(),null,0);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            fails(()->capture(write,"writable",true,new StringWriter(),RUN),"REPORT_KEEP_READONLY_SNAPSHOT_REQUIRED");
        }
        String key;try(SQLiteDatabase snapshot=read(db)){key=KeepMigrationStoreR31.snapshotArtifactPage(snapshot,"",1).get(0).hash;}
        File body=new File(root,key+".row"),outside=new File(work,"outside.row");byte[] saved=Files.readAllBytes(body.toPath());Files.write(outside.toPath(),saved);Files.delete(body.toPath());Files.createSymbolicLink(body.toPath(),outside.toPath());
        try(SQLiteDatabase snapshot=read(db);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){fails(()->capture(snapshot,"file-symlink",false,new StringWriter(),RUN),"REPORT_PRIVATE_PATH");}
        finally{Files.delete(body.toPath());Files.write(body.toPath(),saved);}
        File real=new File(files,"real-keep-directory");Files.move(root.toPath(),real.toPath());Files.createSymbolicLink(root.toPath(),real.toPath());
        try(SQLiteDatabase snapshot=read(db);ReportInputsLeaseCP12 pin=ReportInputsLeaseCP12.acquire(files)){
            fails(()->capture(snapshot,"directory-symlink",true,new StringWriter(),RUN),"REPORT_KEEP_SOURCE_SYMLINK");
            fails(()->ReportExportServiceCP4.openKeepSourceR31(snapshot,files),"REPORT_KEEP_SOURCE_SYMLINK");
        }finally{Files.delete(root.toPath());Files.move(real.toPath(),root.toPath());}
    }
    public static void main(String[] args)throws Exception{
        work=new File(args[0]);Files.createDirectories(work.toPath());files=new File(work,"app/files");root=new File(files,"r31-keep");Files.createDirectories(root.toPath());Class.forName("org.sqlite.JDBC");
        rawAndPagination();missingCorruptAndSchema();pinsReadOnlyCancelAndSymlinks();
        JSONObject receipt=new JSONObject().put("status","ACTUAL_NATIVE_KEEP_CLOSURE_CASES_COMPLETED").put("assertions",assertions).put("scope","REAL_ROW_CODEC_CAS_REGISTRY_DESCRIPTOR_GENERIC_COPY_READBACK_AND_INPUT_PIN; NOT_ANDROID_SERVICE_OR_SAF").put("physicalDevice","NOT_RUN").put("naturalQuality","NOT_PROVEN");
        Files.writeString(new File(args[1],"RESULT.json").toPath(),receipt.toString()+"\n");System.out.println(receipt.toString());
    }
}
