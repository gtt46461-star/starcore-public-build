package com.ethk3.novaonepro;

import android.app.Activity;
import android.database.Cursor;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Streams exact persisted 1200x96 runtime inputs without reconstructing features after the draw.
 * R929 binds every exported row to the canonical feature DAG/manifest and keeps legacy R925 rows
 * readable only as legacy audit data; no missing upstream evidence is synthesized.
 */
public final class TrainingSnapshotExportR925 {
    private TrainingSnapshotExportR925() {}
    private static final FeatureDagContractR929.Manifest R929_MANIFEST =
            FeatureDagContractR929.actionMeta96Manifest();

    public static void start(final Activity activity,final DbStore store){
        new Thread(new Runnable(){public void run(){
            try{
                File dir=new File(activity.getCacheDir(),"r923_reports");
                if(!dir.isDirectory()&&!dir.mkdirs())throw new IOException("EXPORT_DIR");
                File temp=File.createTempFile("training-",".tmp",dir);
                int count=write(store,temp);
                if(count==0){temp.delete();throw new IOException("没有可导出的严格赛前完整快照");}
                MessageDigest md=MessageDigest.getInstance("SHA-256");
                try(InputStream in=new FileInputStream(temp)){byte[] block=new byte[32768];int n;while((n=in.read(block))!=-1)md.update(block,0,n);}
                StringBuilder hash=new StringBuilder();for(byte v:md.digest())hash.append(String.format(java.util.Locale.US,"%02x",v&255));
                final File file=new File(dir,"R929_"+hash+".jsonl");
                if(!temp.renameTo(file))throw new IOException("EXPORT_RENAME");
                writeManifestSidecar(dir);
                final int rows=count;
                activity.runOnUiThread(new Runnable(){public void run(){
                    android.widget.Toast.makeText(activity,"已导出 "+rows+" 期R929完整1200动作PIT训练输入",android.widget.Toast.LENGTH_LONG).show();
                    DiagnosticTextExportR923.sharePrepared(activity,"R929完整1200动作训练快照",file,"application/x-ndjson");
                }});
            }catch(final Exception failure){activity.runOnUiThread(new Runnable(){public void run(){
                android.widget.Toast.makeText(activity,"训练数据导出失败："+failure.getMessage(),android.widget.Toast.LENGTH_LONG).show();
            }});}
        }},"R929-Training-Export").start();
    }

    private static void writeManifestSidecar(File dir)throws IOException{
        File file=new File(dir,"R929_FEATURE_MANIFEST_"+R929_MANIFEST.hash+".txt");
        if(file.isFile())return;
        File tmp=File.createTempFile("r929-feature-manifest-",".tmp",dir);
        try(Writer w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmp),StandardCharsets.UTF_8))){
            w.write("hash="+R929_MANIFEST.hash+"\n");
            w.write(R929_MANIFEST.canonicalWithoutHash());
        }
        if(!tmp.renameTo(file)){
            if(file.isFile()){tmp.delete();return;}
            throw new IOException("R929_MANIFEST_RENAME");
        }
    }

    static int write(DbStore store,File file)throws Exception{
        if(store==null)throw new IOException("STORE_NULL");
        String highWater=store.getLatestDrawIssue(), after="";
        int count=0;
        // A keyset page owns exactly one immutable blob. Close the cursor before decoding or writing
        // so export never retains a multi-minute WAL read snapshot.
        try(Writer w=new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file),StandardCharsets.UTF_8),32768)){
            while(true){
                byte[] bytes;String hash,target;long frozen,drawAt;int[] actual=new int[10];
                Cursor c=store.readable().rawQuery(
                        "SELECT j.target_issue,j.snapshot_blob,j.snapshot_hash,o.created_at,d.open_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10 FROM "
                        +DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" j JOIN "
                        +DbHelper.TABLE_R35_SHADOW_OBSERVATION+" o ON o.target_issue=j.target_issue JOIN draws d ON d.issue=j.target_issue AND d.dirty=0"
                        +" WHERE o.pre_draw_frozen=1 AND o.created_at<d.open_time AND j.snapshot_blob IS NOT NULL"
                        +" AND j.target_issue>? AND j.target_issue<=? ORDER BY j.target_issue ASC LIMIT 1",
                        new String[]{after,highWater});
                try{
                    if(!c.moveToFirst())break;
                    target=c.getString(0);bytes=c.getBlob(1);hash=c.getString(2);
                    frozen=c.getLong(3);drawAt=c.getLong(4);
                    for(int i=0;i<10;i++)actual[i]=c.getInt(i+5);
                }finally{c.close();}
                if(!DeferredEvidenceSnapshotR36.sha256(bytes).equals(hash))throw new IOException("SNAPSHOT_HASH_MISMATCH:"+target);
                DeferredEvidenceSnapshotR36.Snapshot snapshot=DeferredEvidenceSnapshotR36.decode(bytes);
                if(!target.equals(snapshot.targetIssue)||!TargetIssueResolver.exact(snapshot.baseIssue,target).valid
                        ||target.compareTo(after)<=0||frozen<=0||frozen>=drawAt)throw new IOException("PIT_SQL_SNAPSHOT_MISMATCH:"+target);
                FeatureDagContractR929.requirePointInTime(snapshot.baseIssue,target,frozen,drawAt);
                V51Models.PositionAction[] actions=snapshot.positionActions();
                validateActions(actions,actual);
                FeatureDagContractR929.validateActionSurface(actions);
                StringWriter row=new StringWriter(1200000);
                row.write("{\"schema\":\"R929_CANONICAL_1200_META96\",\"game\":\"Binance1-PK10\",\"input_stage\":\"ACTION_META_1200\",\"feature_manifest_revision\":\""+FeatureDagContractR929.REVISION+"\",\"feature_manifest_sha256\":\""+R929_MANIFEST.hash+"\",\"parent_bundle_hash\":\""+snapshot.bundleHash+"\",\"selection_contract_hash\":\""+snapshot.contractHash+"\",\"record_hash\":\""+snapshot.recordHash+"\",\"upstream_raw_available\":"+hasR929RawProof(snapshot)+",\"legacy_missing_is_unknown\":true,\"integrity_version\":2,\"target_issue\":\""+target+"\",\"max_source_issue\":\""+snapshot.baseIssue+"\",\"snapshot_hash\":\""+hash+"\",\"frozen_at\":"+frozen+",\"draw_at\":"+drawAt+",\"actual\":[");
                for(int i=0;i<10;i++){if(i>0)row.write(',');row.write(Integer.toString(actual[i]));}
                row.write("],\"actions\":[");
                for(int i=0;i<actions.length;i++){
                    V51Models.PositionAction a=actions[i];if(i>0)row.write(',');
                    row.write("["+a.position+","+a.n1+","+a.n2+","+a.n3+",[");
                    for(int k=0;k<96;k++){if(k>0)row.write(',');row.write(Double.toString(a.metaFeatures[k]));}
                    row.write("]]");
                }
                row.write("],\"proofs\":[");
                for(int i=0;i<snapshot.proofs.size();i++){
                    if(i>0)row.write(',');writeJsonString(row,snapshot.proofs.get(i));
                }
                row.write("]}");String payload=row.toString();
                // Hash exact UTF-8 payload bytes; Python validates this before JSON interpretation.
                String payloadHash=DeferredEvidenceSnapshotR36.sha256(payload.getBytes(StandardCharsets.UTF_8));
                w.write(payload.substring(0,payload.length()-1));
                w.write(",\"payload_sha256\":\""+payloadHash+"\"}\n");
                count++;after=target;
            }
        }catch(Exception failure){
            if(file.exists()&&!file.delete())android.util.Log.e("R929_EXPORT","PARTIAL_EXPORT_DELETE_FAILED");
            throw failure;
        }
        return count;
    }

    private static boolean hasR929RawProof(DeferredEvidenceSnapshotR36.Snapshot snapshot){
        return snapshot!=null&&RawTrainingInputR932.ready(snapshot.proofs);
    }

    private static void writeJsonString(Writer w,String value)throws IOException{
        String s=value==null?"":value;
        w.write('"');
        for(int i=0;i<s.length();i++){
            char c=s.charAt(i);
            if(c=='"'||c=='\\'){w.write('\\');w.write(c);}
            else if(c=='\n')w.write("\\n");
            else if(c=='\r')w.write("\\r");
            else if(c=='\t')w.write("\\t");
            else if(c<32)w.write(String.format(java.util.Locale.US,"\\u%04x",(int)c));
            else w.write(c);
        }
        w.write('"');
    }

    static void validateActions(V51Models.PositionAction[] actions,int[] actual)throws IOException{
        if(actions==null||actions.length!=1200||actual==null||actual.length!=10)throw new IOException("GROUP_NOT_1200_OR_DRAW_NOT_10");
        int mask=0;for(int n:actual){if(n<1||n>10||(mask&(1<<n))!=0)throw new IOException("INVALID_PERMUTATION");mask|=1<<n;}
        int ordinal=0,hits=0;
        for(int pos=1;pos<=10;pos++)for(int a=1;a<=8;a++)for(int b=a+1;b<=9;b++)for(int c=b+1;c<=10;c++){
            V51Models.PositionAction action=actions[ordinal++];
            if(action==null||action.position!=pos||action.n1!=a||action.n2!=b||action.n3!=c||action.metaFeatures==null||action.metaFeatures.length!=96)throw new IOException("ACTION_ORDINAL_OR_SCHEMA");
            for(double v:action.metaFeatures)if(Double.isNaN(v)||Double.isInfinite(v))throw new IOException("NONFINITE_FEATURE");
            if(actual[pos-1]==a||actual[pos-1]==b||actual[pos-1]==c)hits++;
        }
        if(hits!=360)throw new IOException("HIT_LABEL_GROUP_NOT_360");
    }
}
