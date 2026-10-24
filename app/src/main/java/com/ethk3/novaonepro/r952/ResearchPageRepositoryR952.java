package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import com.ethk3.novaonepro.*;
import java.io.File;
import java.io.IOException;
import java.util.*;

/** Four read-only pages share one bounded immutable local statement projection. */
public final class ResearchPageRepositoryR952 {
    public interface ReadPort { List<Row> read(CancellationSignal cancel); }
    public static final class Row {
        public final String page,key,title,detail;
        public Row(String page,String key,String title,String detail){this.page=safe(page);this.key=safe(key);this.title=safe(title);this.detail=safe(detail);}
    }
    private static final class FileRows extends AbstractList<Row> {
        final ResearchSnapshotFileR82.Handle handle;final String asOfIssue;boolean claimed;
        FileRows(ResearchSnapshotFileR82.Handle h,String issue){handle=h;asOfIssue=issue;}
        @Override public int size(){return handle.offsets.length;}
        @Override public Row get(int index){try(ResearchSnapshotFileR82.Reader reader=handle.open()){return row(reader.get(index));}catch(IOException e){throw new IllegalStateException("SNAPSHOT_READ_FAILED",e);}}
    }
    public static final class Snapshot {
        public final String id,asOfIssue;public final long capturedAt;public final List<Row> rows;
        final ResearchSnapshotFileR82.Handle handle;
        Snapshot(List<Row> source){this(source,new CancellationSignal());}
        Snapshot(List<Row> source,CancellationSignal cancel){
            FileRows files;
            if(source instanceof FileRows){files=(FileRows)source;files.claimed=true;}
            else files=writeRows(source,cancel,defaultCacheDir());
            handle=files.handle;rows=Collections.unmodifiableList(files);capturedAt=System.currentTimeMillis();
            asOfIssue=files.asOfIssue;id=V51ChecksumEngine.sha256("R82_PAGE_SNAPSHOT|"+handle.hash+'|'+capturedAt+'|'+UUID.randomUUID());
        }
        void retire(){handle.retire();}
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
    private static final int MAX_CACHE=4;
    private static final Map<String,Snapshot> CACHE=new LinkedHashMap<String,Snapshot>(8,0.75f,true);
    private final ReadPort port;
    public ResearchPageRepositoryR952(ReadPort port){if(port==null)throw new IllegalArgumentException("READ_PORT_REQUIRED");this.port=port;}
    public PageResult load(String page,String snapshotId,String after,String filter,int limit,CancellationSignal cancel){
        if(!validPage(page)||limit<1||limit>40||after==null||filter==null||cancel==null)return failure("INVALID_PAGE_QUERY",null);
        Snapshot fresh=null;
        try{
            check(cancel);Snapshot snapshot;
            if(snapshotId==null||snapshotId.isEmpty()){
                trimForRead();List<Row> read=null;
                try{read=port.read(cancel);check(cancel);fresh=new Snapshot(read,cancel);read=null;}
                finally{discardRead(read);}
                check(cancel);snapshot=fresh;
                synchronized(CACHE){CACHE.put(snapshot.id,snapshot);while(CACHE.size()>MAX_CACHE)removeOldest();}
                fresh=null;
            }else{
                synchronized(CACHE){snapshot=CACHE.get(snapshotId);}
                if(snapshot==null)return failure("SNAPSHOT_EXPIRED",null);
            }
            List<Row> found=new ArrayList<Row>();int total=0;boolean more=false;
            try(ResearchSnapshotFileR82.Reader reader=snapshot.handle.open()){
                for(int i=0;i<snapshot.handle.offsets.length;i++){
                    check(cancel);ResearchSnapshotFileR82.Record value=reader.get(i);
                    if(!value.page.equals(page)||(!filter.isEmpty()&&!value.key.contains(filter)&&!value.title.contains(filter)))continue;
                    total++;if(!after.isEmpty()&&compareBinary(value.key,after)<=0)continue;
                    if(found.size()<limit)found.add(row(value));else more=true;
                }
            }
            return new PageResult(found.isEmpty()?"EMPTY":"READY","",snapshot,found,total,more?found.get(found.size()-1).key:"",null);
        }catch(OperationCanceledException e){return failure("QUERY_CANCELLED",e);}
        catch(android.database.sqlite.SQLiteException e){return failure("DB_NOT_READY_OR_SCHEMA_DRIFT",e);}
        catch(IOException e){return failure(e.getMessage(),e);}
        catch(IllegalStateException e){return failure(e.getMessage(),e);}
        catch(RuntimeException e){return failure("PAGE_READ_ERROR:"+e.getClass().getSimpleName(),e);}
        finally{if(fresh!=null)fresh.retire();}
    }
    /** Detail reads the exact selected immutable snapshot, without a second database query. */
    public PageResult detail(String snapshotId,String page,String key,CancellationSignal cancel){
        if(snapshotId==null||snapshotId.isEmpty()||!validPage(page)||key==null||cancel==null)return failure("INVALID_DETAIL_QUERY",null);
        Snapshot snapshot;synchronized(CACHE){snapshot=CACHE.get(snapshotId);}
        if(snapshot==null)return failure("SNAPSHOT_EXPIRED",null);
        try(ResearchSnapshotFileR82.Reader reader=snapshot.handle.open()){
            for(int i=0;i<snapshot.handle.offsets.length;i++){
                check(cancel);ResearchSnapshotFileR82.Record value=reader.get(i);
                if(value.page.equals(page)&&value.key.equals(key))return new PageResult("READY","",snapshot,Collections.singletonList(row(value)),1,"",null);
            }
            return new PageResult("EMPTY","DETAIL_NOT_IN_SNAPSHOT",snapshot,Collections.<Row>emptyList(),0,"",null);
        }catch(OperationCanceledException e){return failure("QUERY_CANCELLED",e);}
        catch(IOException e){return failure(e.getMessage(),e);}
    }
    /** Releases a database read that cancellation prevented from becoming a cached snapshot. */
    public static void discardRead(List<Row> rows){if(rows instanceof FileRows){FileRows files=(FileRows)rows;if(!files.claimed)files.handle.retire();}}
    private static void trimForRead(){synchronized(CACHE){
        long bytes=0;for(Snapshot snapshot:CACHE.values())bytes+=snapshot.handle.bytes;
        while(!CACHE.isEmpty()&&(CACHE.size()>=MAX_CACHE||bytes>ResearchSnapshotFileR82.MAX_TOTAL_BYTES-ResearchSnapshotFileR82.MAX_FILE_BYTES)){
            Snapshot first=CACHE.values().iterator().next();bytes-=first.handle.bytes;removeOldest();
        }
    }}
    private static void removeOldest(){String first=CACHE.keySet().iterator().next();Snapshot removed=CACHE.remove(first);removed.retire();}
    private static boolean validPage(String page){return Arrays.asList("deep","evolution","creation","atoms").contains(page);}
    private static void check(CancellationSignal cancel){if(cancel.isCanceled()||Thread.currentThread().isInterrupted())throw new OperationCanceledException();}
    private static int compareBinary(String left,String right){
        try{byte[] a=left.getBytes("UTF-8"),b=right.getBytes("UTF-8");int n=Math.min(a.length,b.length);
            for(int i=0;i<n;i++){int difference=(a[i]&255)-(b[i]&255);if(difference!=0)return difference;}
            return a.length-b.length;
        }catch(java.io.UnsupportedEncodingException impossible){throw new AssertionError(impossible);}
    }
    private static String safe(String value){return value==null?"":value;}
    private static Row row(ResearchSnapshotFileR82.Record value){return new Row(value.page,value.key,value.title,value.detail);}
    private static File defaultCacheDir(){return new File(System.getProperty("java.io.tmpdir","."),"r952-research-snapshots");}
    private static PageResult failure(String reason,Throwable e){return new PageResult("ERROR",reason,null,Collections.<Row>emptyList(),0,"",e);}
    private static FileRows writeRows(Iterable<Row> source,CancellationSignal cancel,File directory){
        try(ResearchSnapshotFileR82.Writer writer=new ResearchSnapshotFileR82.Writer(directory,new ResearchSnapshotFileR82.Cancellation(){public void check(){ResearchPageRepositoryR952.check(cancel);}})){
            String issue="";
            List<Row> sorted=new ArrayList<Row>();
            for(Row value:source){check(cancel);if(sorted.size()>=ResearchSnapshotFileR82.MAX_ROWS)throw new IllegalStateException("SNAPSHOT_ROW_LIMIT_EXCEEDED");sorted.add(value);}
            Collections.sort(sorted,new Comparator<Row>(){public int compare(Row a,Row b){int p=compareBinary(a.page,b.page);return p==0?compareBinary(a.key,b.key):p;}});
            String previousPage="",previousKey="";
            for(Row value:sorted){check(cancel);
                if(value.page.equals(previousPage)&&compareBinary(value.key,previousKey)<=0)throw new IllegalStateException("SNAPSHOT_NON_UNIQUE_OR_UNSORTED_KEY");
                writer.append(new ResearchSnapshotFileR82.Record(value.page,value.key,value.title,value.detail));if(value.page.equals("meta"))issue=value.key;
                previousPage=value.page;previousKey=value.key;
            }
            check(cancel);return new FileRows(writer.finish(),issue);
        }catch(OperationCanceledException e){throw e;}
        catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}
    }
    /** Compatibility overload. Production supplies Context.getCacheDir() explicitly. */
    public static List<Row> readDatabase(SQLiteDatabase db,CancellationSignal cancel){return readDatabase(db,cancel,defaultCacheDir());}
    public static List<Row> readDatabase(SQLiteDatabase db,CancellationSignal cancel,File cacheDir){
        if(db==null||cancel==null||cacheDir==null)throw new IllegalArgumentException("SNAPSHOT_READ_ARGUMENTS");
        // The Presenter opens a dedicated OPEN_READONLY database without ENABLE_WRITE_AHEAD_LOGGING.
        // Android's read-only non-WAL pool has one connection; both version probes use that same connection.
        if(!db.isReadOnly()||db.isWriteAheadLoggingEnabled())throw new IllegalStateException("SNAPSHOT_SINGLE_READONLY_CONNECTION_REQUIRED");
        check(cancel);long before=dataVersion(db,cancel);ResearchSnapshotFileR82.Handle finished=null;
        try(ResearchSnapshotFileR82.Writer writer=new ResearchSnapshotFileR82.Writer(cacheDir,new ResearchSnapshotFileR82.Cancellation(){public void check(){ResearchPageRepositoryR952.check(cancel);}})){
            String issue="",previousPage="",previousKey="";
            try(Cursor c=db.rawQuery(sql(),null,cancel)){
                while(c.moveToNext()){
                    check(cancel);String page=safe(c.getString(0)),key=safe(c.getString(1));
                    if(page.equals(previousPage)&&compareBinary(key,previousKey)<=0)throw new IllegalStateException("SNAPSHOT_NON_UNIQUE_OR_UNSORTED_KEY");
                    writer.append(new ResearchSnapshotFileR82.Record(page,key,c.getString(2),c.isNull(3)?"未记录":c.getString(3)));
                    if(page.equals("meta"))issue=key;previousPage=page;previousKey=key;
                }
            }
            check(cancel);if(before!=dataVersion(db,cancel))throw new IllegalStateException("SNAPSHOT_CHANGED_DURING_READ");
            finished=writer.finish();check(cancel);FileRows rows=new FileRows(finished,issue);finished=null;return rows;
        }catch(OperationCanceledException e){throw e;}
        catch(RuntimeException e){throw e;}
        catch(Exception e){throw new IllegalStateException(e.getMessage(),e);}
        finally{if(finished!=null)finished.retire();}
    }
    private static long dataVersion(SQLiteDatabase db,CancellationSignal cancel){
        try(Cursor c=db.rawQuery("PRAGMA data_version",null,cancel)){
            if(!c.moveToFirst())throw new IllegalStateException("SNAPSHOT_VERSION_UNAVAILABLE");return c.getLong(0);
        }
    }
    /** One SELECT fixes all four pages at the same SQLite read snapshot; no schema or counter writes. */
    public static String sql(){return
        "SELECT * FROM (SELECT 'meta' page,COALESCE(MAX(issue),'') item_key,'快照期号' title,'' detail FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0"
        +" UNION ALL SELECT 'deep','algorithm:'||genome_id,display_name_zh,'状态：'||status||char(10)||'历史训练样本：'||dev_n||'；在线更新：'||state_updates||'；Future：'||future_n||char(10)||'训练截至：'||trained_until_issue||'；Future截至：'||future_last_issue||char(10)||'HR：'||CASE WHEN future_n>0 THEN CAST(future_hit_rate AS TEXT) ELSE '未建立' END||'；Brier：'||CASE WHEN future_n>0 AND future_brier>=0 AND future_brier<1 THEN CAST(future_brier AS TEXT) ELSE '未建立/未通过' END||'；ECE：'||CASE WHEN future_n>0 AND future_ece>=0 AND future_ece<1 THEN CAST(future_ece AS TEXT) ELSE '未建立/未通过' END||char(10)||'配对样本：'||paired_mainline_n||'；阻断原因：'||lifecycle_reason||char(10)||'机制：'||mechanism_spec_zh||char(10)||'程序：'||genome_spec FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
        +" UNION ALL SELECT 'deep','numeric:'||c.candidate_id,'数值学习器 '||c.candidate_id,'状态：'||c.state||'；Future：'||c.future_n||'；参数更新：'||COALESCE(a.numeric_updates,0)||char(10)||'训练截至：'||COALESCE(l.trained_until_issue,'未训练')||char(10)||'模型哈希：'||COALESCE(a.payload_hash,'未建立')||char(10)||'程序：'||c.ast||char(10)||'在线训练与封存评价分账；训练次数不等于证书。' FROM r952_candidate c LEFT JOIN r952_learning_cursor l ON l.consumer_id='R952:'||c.candidate_id LEFT JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash"
        +" UNION ALL SELECT 'evolution','state:'||candidate_key,candidate_key,'状态：'||state||'；Future/配对：'||future_n||'/'||paired_n||char(10)||'最近期号：'||last_issue||char(10)||'阻断/迁移原因：'||reason||char(10)||'证据：'||evidence_hash FROM "+ResearchLifecycleR932.TABLE
        +" UNION ALL SELECT 'evolution','certificate:'||candidate_key||':'||certificate_hash,'证书 '||candidate_id,'层级：'||level||'；状态：'||state||'；权重上限：'||maximum_weight||char(10)||'有效期：'||valid_from_issue||'—'||valid_until_issue||char(10)||'原因：'||reason||char(10)||'证书：'||certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE
        +" UNION ALL SELECT 'evolution','applied:'||target_issue||':'||record_hash,'主链应用 '||target_issue,'算法：'||candidate_name||'；采用：'||applied||'；权重：'||influence_weight||char(10)||'状态：'||status||char(10)||'证据：'||evidence_hash FROM (SELECT * FROM "+ResearchMainlineContributionAuditR922.TABLE+" ORDER BY target_issue DESC,record_hash DESC LIMIT 30)"
        +" UNION ALL SELECT 'creation','attempt:'||attempt_id,'创造 '||lane||' / '||kind,'状态：'||state||'；创建期号：'||created_issue||char(10)||'父项：'||parent_key||char(10)||'子项：'||child_key||char(10)||'原因：'||reason||char(10)||'具体计划：'||plan_json FROM (SELECT * FROM research_creation_attempt_r932 ORDER BY created_at DESC,attempt_id DESC LIMIT 200)"
        +" UNION ALL SELECT 'atoms','atom:'||atom_id,atom_id,'状态：'||state||'；样本：'||n||'；缺失：'||missing||char(10)||'平均Brier增益：'||CASE WHEN n>0 THEN CAST(gain_sum/n AS TEXT) ELSE '未建立' END||'；正增益块：'||positive_blocks||'/'||blocks||char(10)||'最近更新：'||last_issue FROM "+AtomicToolWorldR943.QUALITY
        +" UNION ALL SELECT 'atoms','network:'||owner,'原子神经网络 '||owner,'更新：'||updates||'；loss：'||loss||char(10)||'训练截至：'||trained_until||char(10)||'模型哈希：'||artifact_hash FROM "+AtomicToolWorldR943.MODEL
        +" UNION ALL SELECT 'atoms','world:'||atom_id,'世界原子 '||atom_id,'类型：'||type||'；域：'||domain||'；生产者：'||producer||char(10)||'版本：'||revision||char(10)||'PIT合同：'||pit_contract||char(10)||'哈希：'||atom_hash FROM r952_world_atom"
        +" UNION ALL SELECT 'atoms','molecule:'||molecule_id,'分子 '||molecule_id,'算子：'||operator||'；深度：'||depth||'；成本：'||cost||char(10)||'输入：'||input_ids||char(10)||'编译哈希：'||compiled_hash FROM r952_world_molecule) ORDER BY page,item_key COLLATE BINARY";}
}
