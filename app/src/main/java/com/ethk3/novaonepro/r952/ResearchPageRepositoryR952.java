package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Four read-only pages share one materialized SQLite statement snapshot, including pagination. */
public final class ResearchPageRepositoryR952 {
    public interface ReadPort { List<Row> read(CancellationSignal cancel); }
    public static final class Row {
        public final String page,key,title,detail;
        public Row(String page,String key,String title,String detail){this.page=page;this.key=key;this.title=title;this.detail=detail;}
    }
    public static final class Snapshot {
        public final String id,asOfIssue;public final long capturedAt;public final List<Row> rows;
        Snapshot(List<Row> source){
            rows=Collections.unmodifiableList(new ArrayList<Row>(source));capturedAt=System.currentTimeMillis();
            String issue="",hash="R962_PAGE_SNAPSHOT";
            for(Row r:rows){if(r.page.equals("meta"))issue=r.key;hash=V51ChecksumEngine.sha256(hash+'|'+r.page+'|'+r.key+'|'+r.title+'|'+r.detail);}
            asOfIssue=issue;id=V51ChecksumEngine.sha256(hash+'|'+capturedAt);
        }
    }
    public static final class PageResult {
        public final String status,error,snapshotId,asOfIssue,nextKey;public final long capturedAt;
        public final List<Row> rows;public final int total;public final Throwable cause;
        PageResult(String status,String error,Snapshot snapshot,List<Row> rows,int total,String next,Throwable cause){
            this.status=status;this.error=error==null?"UNKNOWN_READ_FAILURE":error;this.snapshotId=snapshot==null?"":snapshot.id;
            asOfIssue=snapshot==null?"":snapshot.asOfIssue;capturedAt=snapshot==null?0:snapshot.capturedAt;
            this.rows=Collections.unmodifiableList(new ArrayList<Row>(rows));this.total=total;nextKey=next;this.cause=cause;
        }
        public String text(){
            StringBuilder b=new StringBuilder("状态：").append(status).append("\n快照期号：").append(asOfIssue)
                .append("\n读取时间：").append(capturedAt==0?"未读取":new java.util.Date(capturedAt).toString())
                .append("\n快照：").append(snapshotId).append("\n");
            if(!error.isEmpty())b.append("读取原因：").append(error).append("\n");
            b.append("本页 ").append(rows.size()).append(" 项；匹配 ").append(total).append(" 项\n");
            if(status.equals("EMPTY"))b.append("当前快照未找到记录；不代表通过验收。\n");
            for(Row r:rows)b.append("\n").append(r.title).append("\n标识：").append(r.key).append('\n').append(r.detail).append('\n');
            return b.toString();
        }
    }
    private static final Map<String,Snapshot> CACHE=new LinkedHashMap<String,Snapshot>();
    private final ReadPort port;
    public ResearchPageRepositoryR952(ReadPort port){if(port==null)throw new IllegalArgumentException("READ_PORT_REQUIRED");this.port=port;}
    public PageResult load(String page,String snapshotId,String after,String filter,int limit,CancellationSignal cancel){
        if(!Arrays.asList("deep","evolution","creation","atoms").contains(page)||limit<1||limit>40||after==null||filter==null)
            return failure("INVALID_PAGE_QUERY",null);
        try{
            if(cancel.isCanceled())throw new OperationCanceledException();
            Snapshot snapshot;
            if(snapshotId==null||snapshotId.isEmpty()){
                snapshot=new Snapshot(port.read(cancel));
                if(cancel.isCanceled())throw new OperationCanceledException();
                synchronized(CACHE){CACHE.put(snapshot.id,snapshot);while(CACHE.size()>4)CACHE.remove(CACHE.keySet().iterator().next());}
            }else{
                synchronized(CACHE){snapshot=CACHE.get(snapshotId);}
                if(snapshot==null)return failure("SNAPSHOT_EXPIRED",null);
            }
            List<Row> found=new ArrayList<Row>();int total=0;boolean more=false;
            for(Row r:snapshot.rows){
                if(cancel.isCanceled())throw new OperationCanceledException();
                if(!r.page.equals(page)||(!filter.isEmpty()&&!r.key.contains(filter)&&!r.title.contains(filter)))continue;
                total++;if(!after.isEmpty()&&r.key.compareTo(after)<=0)continue;
                if(found.size()<limit)found.add(r);else more=true;
            }
            return new PageResult(found.isEmpty()?"EMPTY":"READY","",snapshot,found,total,
                    more?found.get(found.size()-1).key:"",null);
        }catch(OperationCanceledException e){return failure("QUERY_CANCELLED",e);}
        catch(android.database.sqlite.SQLiteException e){return failure("DB_NOT_READY_OR_SCHEMA_DRIFT",e);}
        catch(IllegalStateException e){return failure(e.getMessage(),e);}
        catch(RuntimeException e){return failure("PAGE_READ_ERROR:"+e.getClass().getSimpleName(),e);}
    }
    private static PageResult failure(String reason,Throwable e){return new PageResult("ERROR",reason,null,Collections.<Row>emptyList(),0,"",e);}
    public static List<Row> readDatabase(SQLiteDatabase db,CancellationSignal cancel){
        long before=dataVersion(db,cancel);
        List<Row> rows=new ArrayList<Row>();
        try(Cursor c=db.rawQuery(sql(),null,cancel)){
            while(c.moveToNext()){
                if(cancel.isCanceled())throw new OperationCanceledException();
                if(rows.size()>=4000)throw new IllegalStateException("SNAPSHOT_CAPACITY_EXCEEDED");
                rows.add(new Row(c.getString(0),c.getString(1),c.getString(2),c.isNull(3)?"未记录":c.getString(3)));
            }
        }
        // Android may refill a CursorWindow with a fresh statement. Reject a concurrent
        // external commit rather than silently combining different database revisions.
        if(before!=dataVersion(db,cancel))throw new IllegalStateException("SNAPSHOT_CHANGED_DURING_READ");
        return rows;
    }
    private static long dataVersion(SQLiteDatabase db,CancellationSignal cancel){
        try(Cursor c=db.rawQuery("PRAGMA data_version",null,cancel)){
            if(!c.moveToFirst())throw new IllegalStateException("SNAPSHOT_VERSION_UNAVAILABLE");
            return c.getLong(0);
        }
    }
    /** One SELECT fixes all four pages at the same SQLite read snapshot; no schema or counter writes. */
    public static String sql(){return
        "SELECT * FROM (SELECT 'meta' page,COALESCE(MAX(issue),'') item_key,'快照期号' title,'' detail FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0"
        +" UNION ALL SELECT 'deep','algorithm:'||genome_id,display_name_zh,'状态：'||status||char(10)||'历史训练样本：'||dev_n||'；在线更新：'||state_updates||'；Future：'||future_n||char(10)||'训练截至：'||trained_until_issue||'；Future截至：'||future_last_issue||char(10)||'HR：'||CASE WHEN future_n>0 THEN CAST(future_hit_rate AS TEXT) ELSE '未建立' END||'；Brier：'||CASE WHEN future_n>0 AND future_brier>=0 AND future_brier<1 THEN CAST(future_brier AS TEXT) ELSE '未建立/未通过' END||'；ECE：'||CASE WHEN future_n>0 AND future_ece>=0 AND future_ece<1 THEN CAST(future_ece AS TEXT) ELSE '未建立/未通过' END||char(10)||'配对样本：'||paired_mainline_n||'；阻断原因：'||lifecycle_reason||char(10)||'机制：'||mechanism_spec_zh||char(10)||'程序：'||genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
        +" UNION ALL SELECT 'deep','numeric:'||c.candidate_id,'数值学习器 '||c.candidate_id,'状态：'||c.state||'；Future：'||c.future_n||'；参数更新：'||COALESCE(a.numeric_updates,0)||char(10)||'训练截至：'||COALESCE(l.trained_until_issue,'未训练')||char(10)||'模型哈希：'||COALESCE(a.payload_hash,'未建立')||char(10)||'程序：'||c.ast||char(10)||'在线训练与封存评价分账；训练次数不等于证书。' FROM r952_candidate c LEFT JOIN r952_learning_cursor l ON l.consumer_id='R952:'||c.candidate_id LEFT JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash"
        +" UNION ALL SELECT 'evolution','state:'||candidate_key,candidate_key,'状态：'||state||'；Future/配对：'||future_n||'/'||paired_n||char(10)||'最近期号：'||last_issue||char(10)||'阻断/迁移原因：'||reason||char(10)||'证据：'||evidence_hash FROM "+ResearchLifecycleR932.TABLE
        +" UNION ALL SELECT 'evolution','certificate:'||candidate_key,'证书 '||candidate_id,'层级：'||level||'；状态：'||state||'；权重上限：'||maximum_weight||char(10)||'有效期：'||valid_from_issue||'—'||valid_until_issue||char(10)||'原因：'||reason||char(10)||'证书：'||certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE
        +" UNION ALL SELECT 'evolution','applied:'||target_issue,'主链应用 '||target_issue,'算法：'||candidate_name||'；采用：'||applied||'；权重：'||influence_weight||char(10)||'状态：'||status||char(10)||'证据：'||evidence_hash FROM (SELECT * FROM "+ResearchMainlineContributionAuditR922.TABLE+" ORDER BY target_issue DESC LIMIT 30)"
        +" UNION ALL SELECT 'creation','attempt:'||attempt_id,'创造 '||lane||' / '||kind,'状态：'||state||'；创建期号：'||created_issue||char(10)||'父项：'||parent_key||char(10)||'子项：'||child_key||char(10)||'原因：'||reason||char(10)||'具体计划：'||plan_json FROM (SELECT * FROM research_creation_attempt_r932 ORDER BY created_at DESC LIMIT 200)"
        +" UNION ALL SELECT 'atoms','atom:'||atom_id,atom_id,'状态：'||state||'；样本：'||n||'；缺失：'||missing||char(10)||'平均Brier增益：'||CASE WHEN n>0 THEN CAST(gain_sum/n AS TEXT) ELSE '未建立' END||'；正增益块：'||positive_blocks||'/'||blocks||char(10)||'最近更新：'||last_issue FROM "+AtomicToolWorldR943.QUALITY
        +" UNION ALL SELECT 'atoms','network:'||owner,'原子神经网络 '||owner,'更新：'||updates||'；loss：'||loss||char(10)||'训练截至：'||trained_until||char(10)||'模型哈希：'||artifact_hash FROM "+AtomicToolWorldR943.MODEL
        +" UNION ALL SELECT 'atoms','world:'||atom_id,'世界原子 '||atom_id,'类型：'||type||'；域：'||domain||'；生产者：'||producer||char(10)||'版本：'||revision||char(10)||'PIT合同：'||pit_contract||char(10)||'哈希：'||atom_hash FROM r952_world_atom"
        +" UNION ALL SELECT 'atoms','molecule:'||molecule_id,'分子 '||molecule_id,'算子：'||operator||'；深度：'||depth||'；成本：'||cost||char(10)||'输入：'||input_ids||char(10)||'编译哈希：'||compiled_hash FROM r952_world_molecule) ORDER BY page,item_key LIMIT 4001";}
}
