# FR-B04-02 单批64样本预取与输入完整性

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-07/FIX-09。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java`

动作：**MODIFY**。施工范围：samples。

问题：每样本一个Cursor，64梯度批可能产生64次查询。

修改方式：一个参数化IN查询预取当前≤64索引，检查形状/finite/缺失；保留函数式Samples兼容。

原方法锚点：AtomNetStoreR30.samples(SQLiteDatabase ro,AtomNetSessionR30 session) L42–50

基线SHA256：`4b85f94c340b6db861e3a9698444ffb21b82edac34ad11bafb0639152b13b61d`。共享合并顺序：FR-B04-02。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`

动作：**MODIFY**。施工范围：work。

问题：训练/评估/FILL依赖逐个样本查询。

修改方式：每个真实计算批前调用prefetch，计算全部在writer外；不无界缓存全部历史特征。

原方法锚点：AtomNetSessionR30.work(Samples samples) L40–70

基线SHA256：`bea4b2c349b9f2342dbfdcbf3890c25896204c663378536b22efa32d67c5f35a`。共享合并顺序：FR-B04-01 → FR-B04-02 → FR-B04-03。

## 实施步骤

1. 应用B04 Store与Session联合补丁，必须一起编译。
2. 只读同一个不可变dataset_hash；缺样本在更新前拒绝；预取顺序不改变训练shuffle/RNG。

## 测试与反证

- 同样本逐个读取与批量读取更新字节一致；64索引只开一Cursor；缺一行/错blob/NaN拒绝。

## 验收

吞吐提升可实测，数值/更新顺序不变、内存有界。

## 恢复与回滚

恢复Samples默认prefetch空实现与旧loader，不清样本。

## 关键代码

顺序补丁：`patches/B04_neural_prefetch_and_truth.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetSessionR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/AtomNetStoreR30.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.util.*;
import java.util.zip.*;

/** Additive, source-keyed input cache and versioned learner state on the existing database/writer. */
public final class AtomNetStoreR30 {
    private AtomNetStoreR30(){}
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r30_atomnet_forecast(target_issue TEXT NOT NULL,position INTEGER NOT NULL,model_hash TEXT NOT NULL,trained_through_ordinal INTEGER NOT NULL,p_class_json TEXT NOT NULL,p_hit REAL NOT NULL,frozen_at INTEGER NOT NULL,deadline_at INTEGER NOT NULL,PRIMARY KEY(target_issue,position))",
        "CREATE TABLE IF NOT EXISTS r30_atomnet_dataset(dataset_hash TEXT PRIMARY KEY,payload BLOB NOT NULL,payload_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r30_atomnet_sample(dataset_hash TEXT NOT NULL,sample_index INTEGER NOT NULL,issue TEXT NOT NULL,position INTEGER NOT NULL,features BLOB NOT NULL,hit INTEGER NOT NULL,number_class INTEGER NOT NULL,PRIMARY KEY(dataset_hash,sample_index))"
    };
    public static void dataset(ResearchWriteQueueR27.Batch b,ReplayDatasetR27 data,long now){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();
            try(DataOutputStream o=new DataOutputStream(new GZIPOutputStream(bytes))){o.writeInt(data.size());for(int i=0;i<data.size();i++){o.writeUTF(data.issues[i]);o.writeLong(data.ordinals[i]);for(int n:data.draws[i])o.writeByte(n);}}
            byte[] blob=bytes.toByteArray();b.add("INSERT OR IGNORE INTO r30_atomnet_dataset VALUES(?,?,?,?)",data.datasetHash,blob,StatsR27.sha256(blob),now);
        }catch(IOException e){throw new IllegalStateException("R30_TRAIN_DATASET_ENCODE",e);}
    }
    public static ReplayDatasetR27 dataset(SQLiteDatabase ro,String hash){
        try(Cursor c=ro.rawQuery("SELECT payload,payload_hash FROM r30_atomnet_dataset WHERE dataset_hash=?",new String[]{hash})){
            if(!c.moveToFirst())throw new IllegalStateException("R30_TRAIN_DATASET_MISSING");byte[] bytes=c.getBlob(0);
            if(!StatsR27.sha256(bytes).equals(c.getString(1)))throw new IllegalStateException("R30_TRAIN_DATASET_CHECKSUM");
            try(DataInputStream d=new DataInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes)))){
                int n=d.readInt();if(n<1||n>100_000)throw new IOException("rows");List<ReplayDatasetR27.Row> rows=new ArrayList<>();
                for(int i=0;i<n;i++){String issue=d.readUTF();long ordinal=d.readLong();int[] draw=new int[10];for(int p=0;p<10;p++)draw[p]=d.readUnsignedByte();rows.add(new ReplayDatasetR27.Row(issue,ordinal,draw));}
                ReplayDatasetR27 result=ReplayDatasetR27.of(rows);if(d.read()!=-1||!result.datasetHash.equals(hash))throw new IOException("content hash");return result;
            }catch(IOException e){throw new IllegalStateException("R30_TRAIN_DATASET_DECODE",e);}
        }
    }
    static byte[] features(double[] x){
        try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);for(double v:x)o.writeDouble(v);o.close();return bytes.toByteArray();}
        catch(IOException e){throw new IllegalStateException(e);}
    }
    public static void sample(ResearchWriteQueueR27.Batch b,String dataset,int index,String issue,int position,double[] x,int y,int cls){
        b.add("INSERT OR IGNORE INTO r30_atomnet_sample VALUES(?,?,?,?,?,?,?)",dataset,index,issue,position,features(x),y,cls);
    }
    public static AtomNetSessionR30.Samples samples(SQLiteDatabase ro,AtomNetSessionR30 session){
        return new AtomNetSessionR30.Samples(){
            final Map<Integer,AtomNetTrainerR27.Sample> prepared=new HashMap<>();
            public void prefetch(int[] indices){
                if(indices.length>64)throw new IllegalArgumentException("R30_SAMPLE_PREFETCH_BOUND");
                prepared.clear();if(indices.length==0)return;
                LinkedHashSet<Integer> unique=new LinkedHashSet<>();for(int i:indices){if(i<0)throw new IllegalArgumentException("R30_SAMPLE_INDEX");unique.add(i);}
                StringBuilder sql=new StringBuilder("SELECT sample_index,issue,position,features,hit,number_class FROM r30_atomnet_sample WHERE dataset_hash=? AND sample_index IN (");
                String[] args=new String[unique.size()+1];args[0]=session.datasetHash;int at=1;
                for(int i:unique){if(at>1)sql.append(',');sql.append('?');args[at++]=Integer.toString(i);}sql.append(')');
                try(Cursor c=ro.rawQuery(sql.toString(),args)){while(c.moveToNext())prepared.put(c.getInt(0),decode(c));}
                if(prepared.size()!=unique.size())throw new IllegalStateException("R30_TRAIN_SAMPLE_MISSING_IN_PREFETCH");
            }
            AtomNetTrainerR27.Sample decode(Cursor c){
                byte[] blob=c.getBlob(3);if(blob.length!=session.input*8)throw new IllegalStateException("R30_TRAIN_SAMPLE_SHAPE");
                double[] x=new double[session.input];try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(blob))){for(int i=0;i<x.length;i++){x[i]=in.readDouble();if(!Double.isFinite(x[i]))throw new IllegalStateException("R30_TRAIN_SAMPLE_NONFINITE");}}
                catch(IOException e){throw new IllegalStateException(e);}
                return new AtomNetTrainerR27.Sample(x,c.getInt(4),c.getInt(5),AtomNetSessionR30.eventKey(c.getString(1),c.getInt(2)));
            }
            public AtomNetTrainerR27.Sample get(int index){
                AtomNetTrainerR27.Sample s=prepared.get(index);if(s==null){prefetch(new int[]{index});s=prepared.get(index);}return s;
            }
        };
    }

    public static AtomNetSessionR30 restore(SQLiteDatabase ro){
        try(Cursor c=ro.rawQuery("SELECT feature_abi,state_hash,artifact,dataset_hash,trained_through FROM r29_model_checkpoint WHERE owner=?",new String[]{AtomNetSessionR30.OWNER})){
            if(!c.moveToFirst())return null;if(!AtomNetSessionR30.ABI.equals(c.getString(0)))throw new IllegalStateException("R30_TRAIN_FEATURE_ABI");
            AtomNetSessionR30 state=AtomNetSessionR30.decode(c.getBlob(2),c.getString(1));
            if(!state.datasetHash.equals(c.getString(3))||!(state.completeIssue.isEmpty()?state.observedThrough:state.trainedThrough()).equals(c.getString(4)))throw new IllegalStateException("R30_TRAIN_CHECKPOINT_CURSOR");return state;
        }
    }
    public static void checkpoint(ResearchWriteQueueR27.Batch b,AtomNetSessionR30 state,long now){
        byte[] bytes=state.encode();
        b.add("INSERT OR REPLACE INTO r29_model_checkpoint(owner,feature_abi,dataset_hash,trained_through,state_hash,artifact,updated_at) VALUES(?,?,?,?,?,?,?)",AtomNetSessionR30.OWNER,AtomNetSessionR30.ABI,state.datasetHash,state.completeIssue.isEmpty()?state.observedThrough:state.trainedThrough(),StatsR27.sha256(bytes),bytes,now);
        b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('atomnet','ten-position-r30',?,?)",JsonR27.write(state.report()),now);
        for(AtomNetTrainerR27.Epoch e:state.receipts)b.add("INSERT OR IGNORE INTO r27_atomnet_epoch VALUES(?,?,?,?,?,?)","R30_"+state.datasetHash.substring(0,16),e.epoch,e.split,Double.isFinite(e.logloss)?e.logloss:null,Double.isFinite(e.brier)?e.brier:null,now);
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
