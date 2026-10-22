package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import org.json.JSONArray;
import org.json.JSONObject;

/** Actual JNI snapshot -> SCROW -> immutable reply -> production prune on synthetic host SQLite. */
public final class DiagnosisNativeArchiveCurrent {
    private static final long BUDGET=4L<<20;
    private static final ReportNativeCP4.Control YES=()->true;
    private static final JSONArray assertions=new JSONArray();
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);assertions.put(text);}
    private interface Work{void run()throws Exception;}
    private static void rejected(Work work,String text)throws Exception{
        try{work.run();}catch(IOException|IllegalArgumentException|IllegalStateException expected){check(true,text+":"+expected.getMessage());return;}
        throw new AssertionError("accepted "+text);
    }
    private static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){check(c.moveToFirst(),"scalar row available");return c.getLong(0);}}
    private static String hash(File file)throws Exception{
        MessageDigest digest=MessageDigest.getInstance("SHA-256");byte[] buffer=new byte[8192];
        try(InputStream in=new FileInputStream(file)){int n;while((n=in.read(buffer))!=-1)digest.update(buffer,0,n);}
        return RetentionArchiveProtocolCP12.hex(digest.digest());
    }
    private static int publish(SQLiteDatabase db,RetentionArchiveProtocolCP12.Receipt proof,boolean success){
        db.beginTransaction();try{int result=DataRetentionGovernorR900.publishAndPruneCP12(db,proof,"current-native-diagnosis",Long.MAX_VALUE);if(success)db.setTransactionSuccessful();return result;}finally{db.endTransaction();}
    }
    public static void main(String[] args)throws Exception{
        File files=new File(args[2]);check(files.mkdir(),"fresh synthetic files directory");
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try{
            db.execSQL("PRAGMA foreign_keys=ON");
            String issue="202609230001";db.beginTransaction();
            try{StringBuilder values=new StringBuilder();for(int i=0;i<5000;i++){
                if(values.length()>0)values.append(',');values.append("('").append(issue).append("',").append(10000+i).append(",0,1,2,3,4,5,6,7,8,9,10)");
                issue=TargetIssueResolver.next(issue).targetIssue;
                if(i%100==99){db.execSQL("INSERT INTO draws(issue,open_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES "+values);values.setLength(0);}
            }db.setTransactionSuccessful();}finally{db.endTransaction();}
            String payload="诊断\u0000é🙂".repeat(65537);byte[] expected=payload.getBytes(StandardCharsets.UTF_8);
            String expectedHash=RetentionArchiveProtocolCP12.sha(expected);
            String[] tables={"adaptive_judge_log","diagnosis_case_log"},keys={"judge_key","case_key"};
            ReportNativeCP4.loadForDiagnosticProcess("host:diagnostics","host");
            JSONArray receipts=new JSONArray();
            for(int t=0;t<tables.length;t++){
                String table=tables[t];db.execSQL("INSERT INTO "+table+"("+keys[t]+",target_issue,detail_text,created_at,updated_at) VALUES('native-row','202601010001',?,1,1)",new Object[]{payload});
                RetentionArchiveProtocolCP12.Page page=DataRetentionGovernorR900.prepareArchivePageCP12(db,table);
                check(page!=null&&page.rows.size()==1,"production Governor selects "+table);
                check(page.registryHash.equals(RetentionTableRegistryCP12.INSTALLED_HASH),"extension registry identity "+table);
                File job=new File(files,"job-"+t);check(job.mkdir(),"fresh native snapshot job");File snapshot=new File(job,"snapshot.db");
                ReportNativeCP4.backup(new File(args[1]),snapshot,BUDGET,YES);String snapshotHash=hash(snapshot);
                rejected(()->RetentionArchiveIoCP12.run(files,page,job,snapshot,"0".repeat(64),YES),"wrong snapshot hash");
                rejected(()->RetentionArchiveIoCP12.run(files,page,job,snapshot,snapshotHash,()->false),"cancelled native archive");
                check(scalar(db,"SELECT COUNT(*) FROM "+table)==1,"negative IO retains source row");
                RetentionArchiveProtocolCP12.Receipt proof=RetentionArchiveIoCP12.run(files,page,job,snapshot,snapshotHash,YES);
                File cas=RetentionArchiveProtocolCP12.casFile(files,proof.archiveHash);
                check(hash(cas).equals(proof.archiveHash)&&cas.length()==proof.byteCount,"actual SCROW CAS hash and byte length");
                final int[] detailColumn={-1};final MessageDigest detailHash=MessageDigest.getInstance("SHA-256");final long[] seen={0};
                try(InputStream in=new FileInputStream(cas)){
                    RetentionRowCodecCP5.ArchiveDigest digest=RetentionRowCodecCP5.readVerify(in,new RetentionRowCodecCP5.RowConsumer(){
                        public void beginTable(RetentionRowCodecCP5.TableShape shape){detailColumn[0]=shape.orderedColumns.indexOf("detail_text");check(detailColumn[0]>=0,"SCROW exact installed column identity");}
                        public void beginRow(long index,byte[] key){check(index==0,"one actual selected SCROW row");}
                        public void cellHeader(int column,int type,long length){if(column==detailColumn[0])check(length==expected.length,"exact Unicode/NUL UTF8 cell length");}
                        public void cellBytes(int column,byte[] buffer,int offset,int count){if(column==detailColumn[0]){detailHash.update(buffer,offset,count);seen[0]+=count;}}
                        public void endRow(String rowHash){check(rowHash.equals(proof.members.get(0).rowHash),"native row digest consumed in receipt");}
                        public void endTable(RetentionRowCodecCP5.ArchiveDigest digest){check(digest.rowCount==1,"exact archive row denominator");}
                    },()->{});
                    check(digest.rawSha256.equals(proof.archiveHash),"independent SCROW verifier digest");
                }
                check(seen[0]==expected.length&&RetentionArchiveProtocolCP12.hex(detailHash.digest()).equals(expectedHash),"native stream preserves every Unicode/NUL payload byte");
                check(RetentionArchiveIoCP12.run(files,page,job,snapshot,snapshotHash,YES).manifestHash.equals(proof.manifestHash),"identical native archive retry is idempotent");
                File requestRoot=new File(files,"retention-requests-cp12"),replyRoot=new File(files,"retention-replies-cp12");requestRoot.mkdirs();replyRoot.mkdirs();
                File request=new File(requestRoot,page.requestId+".json"),reply=new File(replyRoot,page.requestId+".receipt");Files.writeString(request.toPath(),page.requestJson);
                RetentionArchiveIoCP12.writeReceipt(reply,proof);
                RetentionArchiveProtocolCP12.Receipt consumed=RetentionArchiveIoCP12.validatePublishedProof(files,reply,proof);
                check(consumed.manifestHash.equals(proof.manifestHash),"production consumer reads durable exact proof");
                Files.writeString(request.toPath(),page.requestJson+" ");rejected(()->RetentionArchiveIoCP12.validatePublishedProof(files,reply,proof),"request changed blocks proof");Files.writeString(request.toPath(),page.requestJson);
                check(publish(db,consumed,false)==1,"actual native receipt reaches unique prune writer");
                check(scalar(db,"SELECT COUNT(*) FROM "+table)==1,"writer rollback preserves source after real archive");
                check(publish(db,consumed,true)==1,"same current generation consumed and pruned");
                check(scalar(db,"SELECT COUNT(*) FROM "+table)==0,"actual raw source row removed only after proof");
                check(publish(db,consumed,true)==0,"duplicate consumed proof does not delete twice");
                check(scalar(db,"SELECT COUNT(*) FROM research_retention_permit_v2")==0,"no orphan delete permit");
                receipts.put(new JSONObject().put("table",table).put("snapshotHash",snapshotHash).put("archiveHash",proof.archiveHash).put("manifestHash",proof.manifestHash).put("rawBytes",proof.byteCount).put("payloadBytes",expected.length));
            }
            for(String table:new String[]{"diagnosis_fix_plan","dynamic_freeze_matrix","module_effect_cache","shadow_promotion_cache"})check(RetentionTableRegistryCP12.require(table).policy.equals("PERMANENT_BOUNDED_AGGREGATE"),"bounded aggregate remains explicit "+table);
            try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){check(c.moveToFirst()&&"ok".equals(c.getString(0)),"final SQLite integrity");}
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){check(!c.moveToFirst(),"final SQLite FK integrity");}
            Files.writeString(Path.of(args[3]),new JSONObject().put("status","PASS").put("assertion_count",assertions.length()).put("assertions",assertions).put("receipts",receipts).put("scope","Actual current Android product classes plus host framework adapters and fresh production JNI; synthetic411-table data. Direct production diagnostics IO/prune methods executed; Android Service lifecycle, SAF, devices, customer release and natural future remain separate.").toString(2)+'\n');
            System.out.println("PASS|DiagnosisNativeArchiveCurrent|assertions="+assertions.length());
        }finally{db.shutdown();}
    }
}
