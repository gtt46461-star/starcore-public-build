# FR-B06-01 矩阵静态接线与运行效果分列

优先级：P1。状态：设计交付，尚未Android/设备验收。原需求：FIX-12。

前驱：B03。同批之前卡涉及本卡相同文件时必须先集成；顺序见FILE_OWNERS。

## 逐文件施工

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java`

动作：**MODIFY**。施工范围：report。

问题：20方法反射成功即TRACEABLE，而19来源不可读。

修改方式：应用补丁分staticBindingStatus/runtimeSnapshotStatus/qualityStatus，不把静态1当有效。

原方法锚点：MatrixRegistryStoreR30.report(SQLiteDatabase db) L10–33

基线SHA256：`6c7f105be669f7af85c71a1196ee75a7ae777a4b894cd7d4676f38caebf44cfb`。共享合并顺序：FR-B06-01。

### `app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryR30.java`

动作：**MODIFY**。施工范围：Definition/all。

问题：多数algorithmConsumer只是PERSISTENCE_AND_DIAGNOSTICS。

修改方式：为每kind登记真实张量合同、生产事件、算法消费者，旧反射信息单独保留。

原方法锚点：MatrixRegistryR30$Definition.<init>(String id,String ns,String kind,String producer,String method,String consumer,List<String> methods,String table,String scope,String algorithm) L15–17；MatrixRegistryR30$Definition.json() L18–18；MatrixRegistryR30.all() L27–27

基线SHA256：`cc6a058d8b560dc4dc3229cfcc4c9d53af947274814091d0f6eba3952d5428ea`。共享合并顺序：FR-B06-01。

## 实施步骤

1. 19个R930对象与1个SU2对象保持原id；不能用r952的237行冒充20矩阵成功。
2. 所有缺源/缺产出/缺消费/未证有效的原因分别显示。

## 测试与反证

- 源表缺失、空表、有产出无消费、有消费无增益、静态方法缺失。

## 验收

矩阵实际状态不会因方法存在被标成全部通过。

## 恢复与回滚

回退投影版本，保留原矩阵与注册原件。

## 关键代码

顺序补丁：`patches/B06_matrix_static_runtime_separation.patch`。必须按SERIES顺序应用，一份跨文件补丁只应用一次。

已预写目标文件：`code/app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java`。它是本包关键补丁叠加后的组合目标，不是允许覆盖其他卡修改的单独发布文件。

### 可直接审阅的关键代码

`app/src/main/java/com/ethk3/novaonepro/strategyv2/MatrixRegistryStoreR30.java`（关键组合目标；集成限制见上）：

```java
package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Read-only inventory over existing tables. Stored observations are not statistical validation. */
public final class MatrixRegistryStoreR30 {
    private MatrixRegistryStoreR30(){}
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Map<String,Object>> nativeRows=new HashMap<>();String nativeUnavailable=null,su2Unavailable=null;Map<String,Object> su2=null;
        try(Cursor c=db.rawQuery("SELECT kind,count(*),max(target_issue),max(frozen_at) FROM research_matrix_snapshot_r930 GROUP BY kind",null)){
            while(c.moveToNext()){Map<String,Object> m=new LinkedHashMap<>();m.put("retainedSnapshotCount",c.getLong(1));m.put("lastProducedIssue",c.isNull(2)?null:c.getString(2));m.put("lastProducedAt",c.isNull(3)?null:c.getLong(3));nativeRows.put(c.getString(0),m);}
        }catch(RuntimeException missing){nativeUnavailable="R930_SNAPSHOT_SOURCE_UNAVAILABLE:"+missing.getClass().getSimpleName();}
        try(Cursor c=db.rawQuery("SELECT count(*),max(checkpoint_sequence),max(updated_at) FROM su2_matrix_snapshot",null)){
            if(c.moveToFirst()){su2=new LinkedHashMap<>();su2.put("retainedSnapshotCount",c.getLong(0));su2.put("lastProducedCheckpoint",c.isNull(1)?null:c.getLong(1));su2.put("lastProducedAt",c.isNull(2)?null:c.getLong(2));su2.put("periodKind","SU2_CHECKPOINT_SEQUENCE");}
        }catch(RuntimeException missing){su2Unavailable="SU2_SNAPSHOT_SOURCE_UNAVAILABLE:"+missing.getClass().getSimpleName();}
        List<Object> rows=new ArrayList<>();long linked=0,recorded=0;
        for(MatrixRegistryR30.Definition d:MatrixRegistryR30.all()){
            Map<String,Object> r=d.json();boolean links=MatrixRegistryR30.declared(d.producerClass,d.producerMethod);for(String m:d.consumerMethods)links&=MatrixRegistryR30.declared(d.consumerClass,m);
            if(links)linked++;r.put("bindingsVerified",links);r.put("callableImplementation",links);
            Map<String,Object> observation=d.namespace.equals("SU2")?su2:nativeRows.get(d.kind);String unavailable=d.namespace.equals("SU2")?su2Unavailable:nativeUnavailable;
            if(observation!=null)r.putAll(observation);
            Long n;if(observation!=null)n=((Number)observation.get("retainedSnapshotCount")).longValue();else if(unavailable==null)n=0L;else n=null;
            r.put("retainedSnapshotCount",n);r.putIfAbsent("lastProducedIssue",null);r.putIfAbsent("lastProducedAt",null);
            r.put("evidenceState",n!=null&&n>0?"RECORDED_UNVALIDATED":unavailable!=null?"BLOCKED_SOURCE":"NOT_RUN");
            r.put("notProducedReason",unavailable!=null?unavailable:n!=null&&n>0?null:"NO_RETAINED_PRODUCTION_SNAPSHOT");
            r.put("bindingReason",links?null:"DECLARED_PRODUCER_OR_CONSUMER_METHOD_UNAVAILABLE");r.put("qualityEffective",null);
            if(n!=null&&n>0)recorded++;rows.add(r);
        }
        Map<String,Object> result=new LinkedHashMap<>();result.put("rows",rows);result.put("registeredObjects",(long)rows.size());result.put("r930NativeKinds",19L);result.put("su2FactorTeacherBundles",1L);result.put("verifiedBindings",linked);result.put("traceableRatio",linked/(double)rows.size());result.put("objectsWithRetainedSnapshots",recorded);
        result.put("staticBindingStatus",linked==rows.size()?"TRACEABLE":"UNVERIFIED_BINDINGS");
        result.put("runtimeSnapshotStatus",nativeUnavailable!=null||su2Unavailable!=null?"BLOCKED_SOURCE":recorded==0?"NO_RETAINED_PRODUCTION":"RECORDED_UNVALIDATED");
        result.put("qualityStatus","UNKNOWN_NOT_VALIDATED");
        result.put("status",nativeUnavailable!=null||su2Unavailable!=null?"BLOCKED_SOURCE":recorded==rows.size()?"RECORDED_UNVALIDATED":"INCOMPLETE_PRODUCTION");
        result.put("scope","EXISTING_MATRIX_OBJECTS_AND_RETAINED_WATERMARKS");return result;
    }
}

```

本卡剩余工作：所有不在上述顺序补丁中的步骤与调用方接入，以及Android类型检查、数据库迁移/故障注入、设备运行和效果验收。

## 施工回执待填

实现commit：NOT_IMPLEMENTED。测试回执：待本卡真实执行。Android构建：NOT_RUN。设备：NOT_RUN。自然质量：NOT_PROVEN。本轮主机已验证范围另见verification。
