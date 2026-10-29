package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Research-owned compact summary. No label, model, lifecycle, certificate or REAL writer. */
public final class LongScaleSummaryR80 {
    public static final String EPOCH="research_long_epoch_r80", HEAD="research_long_head_r80";
    public static final String SNAPSHOT="research_long_snapshot_r80", JOURNAL="research_long_journal_r80";
    public static final String PUBLICATION="research_long_publication_r80";
    public static final String TOKEN="research_long_source_token_r80";
    public static final String PROTOCOL=V51ChecksumEngine.sha256("SC_LONG_COUNTS_R80_1|BINANCE1_PK10|POSITIONS10|FREQ_ENTROPY|1440,10080,43200|STRICT_CONTIGUOUS|ORIGINAL_AVAILABLE_AT");
    public static final int PAGE_ROWS=64;
    private LongScaleSummaryR80(){}

    private static Map<String,String> buildDdl(){
        Map<String,String> out=new LinkedHashMap<>();
        ddl(out,"CREATE TABLE IF NOT EXISTS "+EPOCH+"(epoch_hash TEXT PRIMARY KEY CHECK(length(epoch_hash)=64),protocol_hash TEXT NOT NULL CHECK(length(protocol_hash)=64),first_issue TEXT NOT NULL,created_at INTEGER NOT NULL CHECK(created_at>0),reason TEXT NOT NULL)");
        ddl(out,"CREATE TABLE IF NOT EXISTS "+HEAD+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),epoch_hash TEXT NOT NULL REFERENCES "+EPOCH+"(epoch_hash),through_issue TEXT NOT NULL,generation INTEGER NOT NULL CHECK(generation>=0),state_hash TEXT NOT NULL CHECK(length(state_hash)=64),source_chain TEXT NOT NULL CHECK(length(source_chain)=64),availability_max INTEGER NOT NULL CHECK(availability_max>=0),state_blob BLOB NOT NULL CHECK(length(state_blob)="+LongScaleCountsR80.CHECKPOINT_BYTES+"),status TEXT NOT NULL CHECK(status IN('ACTIVE','INVALID_SOURCE')),last_error TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        ddl(out,"CREATE TABLE IF NOT EXISTS "+SNAPSHOT+"(snapshot_hash TEXT PRIMARY KEY CHECK(length(snapshot_hash)=64),epoch_hash TEXT NOT NULL REFERENCES "+EPOCH+"(epoch_hash),through_issue TEXT NOT NULL,generation INTEGER NOT NULL,source_chain TEXT NOT NULL CHECK(length(source_chain)=64),state_hash TEXT NOT NULL CHECK(length(state_hash)=64),payload_json TEXT NOT NULL CHECK(length(CAST(payload_json AS BLOB)) BETWEEN 1 AND 16384),prepared_at INTEGER NOT NULL CHECK(prepared_at>0),UNIQUE(epoch_hash,through_issue))");
        ddl(out,"CREATE INDEX IF NOT EXISTS idx_long_snapshot_issue_r80 ON "+SNAPSHOT+"(through_issue,epoch_hash)");
        ddl(out,"CREATE TABLE IF NOT EXISTS "+PUBLICATION+"(snapshot_hash TEXT PRIMARY KEY REFERENCES "+SNAPSHOT+"(snapshot_hash),through_issue TEXT NOT NULL,available_at INTEGER NOT NULL CHECK(available_at>0),mode TEXT NOT NULL CHECK(mode IN('ORIGINAL_RETURN','RECOVERED_AVAILABLE_NOW')),clock_session TEXT NOT NULL,receipt_hash TEXT NOT NULL UNIQUE CHECK(length(receipt_hash)=64))");
        ddl(out,"CREATE TABLE IF NOT EXISTS "+JOURNAL+"(event_hash TEXT PRIMARY KEY CHECK(length(event_hash)=64),epoch_hash TEXT NOT NULL,through_issue TEXT NOT NULL,reason TEXT NOT NULL,recorded_at INTEGER NOT NULL CHECK(recorded_at>0))");
        ddl(out,"CREATE TABLE IF NOT EXISTS "+TOKEN+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),generation INTEGER NOT NULL CHECK(generation>=0))");
        for(String table:new String[]{EPOCH,SNAPSHOT,JOURNAL,PUBLICATION}){
            ddl(out,"CREATE TRIGGER IF NOT EXISTS "+table+"_no_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'LONG_IMMUTABLE'); END");
            String primary=table.equals(EPOCH)?"epoch_hash":table.equals(JOURNAL)?"event_hash":"snapshot_hash";
            ddl(out,"CREATE TRIGGER IF NOT EXISTS "+table+"_no_replace BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+primary+"=NEW."+primary+") BEGIN SELECT RAISE(ABORT,'LONG_REPLACE_FORBIDDEN'); END");
        }
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_snapshot_key_r80 BEFORE INSERT ON "+SNAPSHOT+" WHEN EXISTS(SELECT 1 FROM "+SNAPSHOT+" WHERE epoch_hash=NEW.epoch_hash AND through_issue=NEW.through_issue) BEGIN SELECT RAISE(ABORT,'LONG_SOURCE_SNAPSHOT_EXISTS'); END");
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_snapshot_capacity_r80 BEFORE INSERT ON "+SNAPSHOT+" WHEN (SELECT COUNT(*) FROM "+SNAPSHOT+")>=5000 BEGIN SELECT RAISE(ABORT,'LONG_SNAPSHOT_CAPACITY'); END");
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_publication_time_r80 BEFORE INSERT ON "+PUBLICATION+" WHEN NOT EXISTS(SELECT 1 FROM "+SNAPSHOT+" s WHERE s.snapshot_hash=NEW.snapshot_hash AND s.prepared_at<=NEW.available_at) BEGIN SELECT RAISE(ABORT,'LONG_PUBLICATION_TIME'); END");
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_publication_receipt_r80 BEFORE INSERT ON "+PUBLICATION+" WHEN EXISTS(SELECT 1 FROM "+PUBLICATION+" WHERE receipt_hash=NEW.receipt_hash) BEGIN SELECT RAISE(ABORT,'LONG_PUBLICATION_RECEIPT_EXISTS'); END");
        String canonical=DbHelper.TABLE_R811_DRAW_CANONICAL;
        for(String operation:new String[]{"INSERT","DELETE"})ddl(out,"CREATE TRIGGER IF NOT EXISTS long_source_token_"+operation.toLowerCase(java.util.Locale.ROOT)+"_r80 AFTER "+operation+" ON "+canonical+" BEGIN UPDATE "+TOKEN+" SET generation=generation+1 WHERE singleton=1; END");
        String changed="NEW.status IS NOT OLD.status OR NEW.canonical_draw_hash IS NOT OLD.canonical_draw_hash";
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_source_token_update_r80 AFTER UPDATE ON "+canonical+" WHEN "+changed+" BEGIN UPDATE "+TOKEN+" SET generation=generation+1 WHERE singleton=1; END");
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_canonical_changed_r80 AFTER UPDATE ON "+canonical+" WHEN ("+changed+") AND EXISTS(SELECT 1 FROM "+HEAD+" h JOIN "+EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND OLD.target_issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE "+HEAD+" SET status='INVALID_SOURCE',last_error='CANONICAL_CHANGED:'||OLD.target_issue,generation=generation+1 WHERE singleton=1; END");
        StringBuilder content=new StringBuilder("NEW.issue IS NOT OLD.issue OR NEW.open_time IS NOT OLD.open_time OR NEW.dirty IS NOT OLD.dirty");
        for(int p=1;p<=10;p++)content.append(" OR NEW.n").append(p).append(" IS NOT OLD.n").append(p);
        ddl(out,"CREATE TRIGGER IF NOT EXISTS long_draw_changed_r80 AFTER UPDATE ON draws WHEN ("+content+") AND EXISTS(SELECT 1 FROM "+HEAD+" h JOIN "+EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND OLD.issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE "+HEAD+" SET status='INVALID_SOURCE',last_error='DRAW_CHANGED:'||OLD.issue,generation=generation+1 WHERE singleton=1; END");

        for(String table:new String[]{SNAPSHOT,PUBLICATION,JOURNAL,EPOCH})installArchiveDdl(out,table);
        // A late insertion inside the accepted prefix is a source correction.
        // Ordinary retirement DELETEs do not invalidate the compact head.
        ddl(out,"CREATE TRIGGER long_canonical_insert_r81 AFTER INSERT ON "+canonical+" WHEN EXISTS(SELECT 1 FROM "+HEAD+" h JOIN "+EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND NEW.target_issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE "+HEAD+" SET status='INVALID_SOURCE',last_error='CANONICAL_INSERT:'||NEW.target_issue,generation=generation+1 WHERE singleton=1; END");
        ddl(out,"CREATE TRIGGER long_draw_insert_r81 AFTER INSERT ON draws WHEN EXISTS(SELECT 1 FROM "+HEAD+" h JOIN "+EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE h.status='ACTIVE' AND NEW.issue BETWEEN e.first_issue AND h.through_issue) BEGIN UPDATE "+HEAD+" SET status='INVALID_SOURCE',last_error='DRAW_INSERT:'||NEW.issue,generation=generation+1 WHERE singleton=1; END");
        return Collections.unmodifiableMap(out);
    }
    private static void ddl(Map<String,String> out,String sql){
        String exact=sql.replace(" IF NOT EXISTS "," ");
        java.util.regex.Matcher name=java.util.regex.Pattern.compile("^CREATE (?:TABLE|INDEX|TRIGGER) ([A-Za-z_][A-Za-z0-9_]*)[ (]").matcher(exact);
        need(name.find(),"LONG_DDL_KIND");need(out.put(name.group(1),exact)==null,"LONG_DDL_DUPLICATE");
    }
    private static final Map<String,String> DDL=buildDdl();
    public static final String SCHEMA_STEP="R81_LONG_SUMMARY_CAS_ARCHIVE_V1";
    public static Set<String> requiredObjects(){return DDL.keySet();}
    /** Exact DDL identity, not a table-presence proxy. */
    public static boolean ready(SQLiteDatabase db){
        if(db==null)return false;
        if(TransactionSchemaCacheR941.readyCommitted(db,SCHEMA_STEP))return true;
        if(!com.ethk3.novaonepro.SchemaObjectSnapshotR26.read(db,DDL.keySet()).matches(DDL))return false;
        TransactionSchemaCacheR941.markCommitted(db,SCHEMA_STEP);return true;
    }
    /** Called only by the existing additive research migration owner. */
    public static void schema(SQLiteDatabase db){
        need(db!=null&&db.inTransaction(),"LONG_SCHEMA_WRITER");
        com.ethk3.novaonepro.SchemaObjectSnapshotR26 present=com.ethk3.novaonepro.SchemaObjectSnapshotR26.read(db,DDL.keySet());
        for(Map.Entry<String,String> e:DDL.entrySet()){
            if(present.contains(e.getKey())){need(e.getValue().equals(present.firstSql(e.getKey())),"LONG_SCHEMA_DRIFT:"+e.getKey());continue;}
            db.execSQL(e.getValue());
        }
        db.execSQL("INSERT OR IGNORE INTO "+TOKEN+" VALUES(1,0)");
        need(ready(db),"LONG_SCHEMA_NOT_READY");
    }
    private static void installArchiveDdl(Map<String,String> out,String table){
        String proof="SELECT 1 FROM research_retention_permit_v2 p JOIN research_evidence_archive_v2 a ON a.archive_id=p.archive_id JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='"+table+"' AND p.source_rowid=OLD.rowid AND p.source_generation=COALESCE((SELECT r.generation FROM research_row_revision_v2 r WHERE r.source_table='"+table+"' AND r.source_rowid=OLD.rowid),0) AND a.state IN('VERIFIED','REFERENCED','PRUNED')";
        String eligible=archiveEligible(table,"OLD","(SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1 OFFSET 4999)");
        ddl(out,"CREATE TRIGGER "+table+"_archive_guard BEFORE DELETE ON "+table+" WHEN NOT COALESCE(("+eligible+"),0) OR NOT EXISTS("+proof+") BEGIN SELECT RAISE(ABORT,'LONG_DELETE_REQUIRES_VERIFIED_ARCHIVE'); END");
        ddl(out,"CREATE TRIGGER "+table+"_archive_locator BEFORE DELETE ON "+table+" BEGIN INSERT OR IGNORE INTO research_evidence_locator_v2(source_table,source_key_hash,archive_id,row_hash,key_canonical,created_at) SELECT m.source_table,m.source_key_hash,m.archive_id,m.row_hash,m.key_canonical,p.created_at FROM research_retention_permit_v2 p JOIN research_evidence_archive_member_v2 m ON m.archive_id=p.archive_id AND m.source_table=p.source_table AND m.source_rowid=p.source_rowid AND m.row_hash=p.row_hash AND m.source_generation=p.source_generation WHERE p.source_table='"+table+"' AND p.source_rowid=OLD.rowid; END");
        ddl(out,"CREATE TRIGGER "+table+"_revision_insert AFTER INSERT ON "+table+" BEGIN INSERT OR REPLACE INTO research_row_revision_v2(source_table,source_rowid) VALUES('"+table+"',NEW.rowid); END");
        ddl(out,"CREATE TRIGGER "+table+"_revision_delete AFTER DELETE ON "+table+" BEGIN DELETE FROM research_row_revision_v2 WHERE source_table='"+table+"' AND source_rowid=OLD.rowid; END");
    }
    public static String archiveIssueColumn(String table){
        need(table.equals(SNAPSHOT)||table.equals(PUBLICATION)||table.equals(JOURNAL)||table.equals(EPOCH),"LONG_ARCHIVE_TABLE");return table.equals(EPOCH)?"first_issue":"through_issue";
    }
    /** The source payloads in existing SCBIND1/context CAS remain immutable.
     * Hot source rows expire only after the original 5000-draw raw tail. The
     * singleton compact head is not archived or reset by tail reclamation. */
    public static String archiveEligible(String table,String alias,String cutoff){
        String column=archiveIssueColumn(table);need(alias.matches("[A-Za-z_][A-Za-z0-9_]*"),"LONG_ARCHIVE_ALIAS");
        String sql="length("+alias+"."+column+")=12 AND "+alias+"."+column+"<"+cutoff;
        if(table.equals(SNAPSHOT))sql+=" AND NOT EXISTS(SELECT 1 FROM "+PUBLICATION+" p WHERE p.snapshot_hash="+alias+".snapshot_hash)";
        if(table.equals(EPOCH))sql+=" AND NOT EXISTS(SELECT 1 FROM "+HEAD+" h WHERE h.epoch_hash="+alias+".epoch_hash) AND NOT EXISTS(SELECT 1 FROM "+SNAPSHOT+" s WHERE s.epoch_hash="+alias+".epoch_hash) AND NOT EXISTS(SELECT 1 FROM "+JOURNAL+" j WHERE j.epoch_hash="+alias+".epoch_hash)";
        sql+=" AND NOT EXISTS(SELECT 1 FROM v57_research_input_pin_r967 p JOIN v57_research_task_r967 t ON t.task_id=p.task_id WHERE p.source_table='"+table+"' AND p.source_issue="+alias+"."+column+" AND t.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))";
        return sql;
    }

    private static final class Head{
        final String epoch,first,through,hash,chain,status,error;final long generation,available;final byte[] state;
        Head(Cursor c){epoch=c.getString(0);first=c.getString(1);through=c.getString(2);generation=c.getLong(3);hash=c.getString(4);chain=c.getString(5);available=c.getLong(6);state=c.getBlob(7);status=c.getString(8);error=c.getString(9);}
        String fence(){return epoch+'|'+through+'|'+generation+'|'+hash+'|'+chain+'|'+available+'|'+status+'|'+error;}
    }
    private static Head head(SQLiteDatabase db,boolean withState){try(Cursor c=db.rawQuery("SELECT h.epoch_hash,e.first_issue,h.through_issue,h.generation,h.state_hash,h.source_chain,h.availability_max,"+(withState?"h.state_blob":"NULL")+",h.status,h.last_error FROM "+HEAD+" h JOIN "+EPOCH+" e ON e.epoch_hash=h.epoch_hash WHERE singleton=1",null)){return c.moveToFirst()?new Head(c):null;}}
    private static long token(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT generation FROM "+TOKEN+" WHERE singleton=1",null)){need(c.moveToFirst(),"LONG_SOURCE_TOKEN_MISSING");return c.getLong(0);}}
    public static final class Prepared{
        private final String before,epoch,first,through,hash,chain,snapshot,error,reason;
        private final long historyGeneration,canonicalGeneration,generation,available,at;
        private final byte[] checkpoint;
        private Prepared(Head old,String epoch,String first,String through,long generation,String chain,long available,long at,LongScaleCountsR80 state,long historyGeneration,long canonicalGeneration,String error,String reason){
            before=old==null?"":old.fence();this.epoch=epoch;this.first=first;this.through=through;this.generation=generation;this.chain=chain;this.available=available;this.at=at;this.historyGeneration=historyGeneration;this.canonicalGeneration=canonicalGeneration;this.error=error;this.reason=reason;
            checkpoint=state.checkpoint();hash=LongScaleCountsR80.checkpointHash(checkpoint);snapshot=error.isEmpty()?snapshotJson(epoch,first,through,generation,chain,hash,available,at,state):"";
        }
    }
    /** Reads and numeric/codec work are outside the sole writer. */
    public static Prepared prepare(SQLiteDatabase db,long deadlineNs){
        need(db!=null&&!db.inTransaction(),"LONG_PREPARE_OUTSIDE_WRITER");
        need("Binance1-PK10".equals(ReleaseManifestR926.GAME_CODE),"LONG_GAME_CONTRACT");
        try(Cursor capacity=db.rawQuery("SELECT COUNT(*) FROM "+SNAPSHOT,null)){need(capacity.moveToFirst(),"LONG_SNAPSHOT_COUNT");if(capacity.getLong(0)>=5000)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_SNAPSHOT_ARCHIVE");}
        long canonicalGeneration=token(db),historyGeneration=DevelopmentProductionCP12.historyGeneration(db),now=System.currentTimeMillis();Head old=head(db,true);
        boolean fresh=old==null||!"ACTIVE".equals(old.status);LongScaleCountsR80 state=fresh?new LongScaleCountsR80():LongScaleCountsR80.restore(old.state,old.hash);
        List<Models.Draw> rows=draws(db,fresh?"":old.through,fresh?5000:PAGE_ROWS);
        if(rows.isEmpty())throw new DevelopmentProductionCP12.Wait("WAIT_LONG_SOURCE");
        if(fresh){
            // Keep only the latest contiguous authenticated suffix. Earlier gaps
            // are retained in the journal of the replaced epoch, never filled.
            int start=rows.size()-1;while(start>0&&TargetIssueResolver.exact(rows.get(start-1).issue,rows.get(start).issue).valid)start--;
            rows=new ArrayList<>(rows.subList(start,Math.min(rows.size(),start+PAGE_ROWS)));
        }
        String first=fresh?rows.get(0).issue:old.first;
        String epoch=fresh?V51ChecksumEngine.sha256("LONG_EPOCH_R80|"+PROTOCOL+'|'+first+'|'+now+'|'+UUID.randomUUID()):old.epoch;
        String chain=fresh?V51ChecksumEngine.sha256("LONG_SOURCE_CHAIN_R80|"+epoch):old.chain;
        String through=fresh?"":old.through;long available=fresh?0:old.available,generation=old==null?1:old.generation+1;int advanced=0;
        for(Models.Draw row:rows){
            if(System.nanoTime()+50_000_000L>=deadlineNs)break;
            ResearchWorkBudgetR927.checkpoint();
            if(!through.isEmpty()&&!TargetIssueResolver.exact(through,row.issue).valid)return new Prepared(old,old.epoch,old.first,old.through,generation,old.chain,old.available,now,LongScaleCountsR80.restore(old.state,old.hash),historyGeneration,canonicalGeneration,"SOURCE_GAP:"+through+":"+row.issue,"SOURCE_GAP");
            if(row.openTime<=0||row.fetchTime<row.openTime||row.fetchTime>now)throw new DevelopmentProductionCP12.Wait("WAIT_LONG_ORIGINAL_SOURCE_CLOCK");
            int[] digits=new int[10];for(int p=0;p<10;p++)digits[p]=row.numbers[p]-1;
            state.append(MatrixSourceRouterCP12.issueOrdinal(row.issue),digits);through=row.issue;available=Math.max(available,row.fetchTime);
            chain=V51ChecksumEngine.sha256(chain+'|'+row.issue+'|'+DrawHashR36.sha256V2(row)+'|'+row.fetchTime);advanced++;
        }
        if(advanced==0)throw new DevelopmentProductionCP12.Wait("TIME_BUDGET_EXCEEDED");
        need(canonicalGeneration==token(db)&&historyGeneration==DevelopmentProductionCP12.historyGeneration(db),"LONG_SOURCE_CHANGED_DURING_READ");
        return new Prepared(old,epoch,first,through,generation,chain,available,now,state,historyGeneration,canonicalGeneration,"",fresh?(old==null?"INITIAL_SOURCE":"NEW_EPOCH_AFTER:"+old.error):"ADVANCE");
    }
    private static List<Models.Draw> draws(SQLiteDatabase db,String after,int limit){
        String columns="d.issue,d.open_time,d.fetch_time,d.n1,d.n2,d.n3,d.n4,d.n5,d.n6,d.n7,d.n8,d.n9,d.n10,d.draw_hash_v2";
        String join=" FROM "+(after.isEmpty()?"draws":DrawDependencyInputCP12.SOURCE)+" d JOIN "+DbHelper.TABLE_R811_DRAW_CANONICAL+" c ON c.target_issue=d.issue AND c.status='CONFIRMED_CANONICAL_DRAW' AND c.canonical_draw_hash=d.draw_hash_v2 WHERE d.dirty=0";
        String sql=after.isEmpty()?"SELECT * FROM (SELECT "+columns+join+" ORDER BY d.issue DESC LIMIT "+limit+") ORDER BY issue":"SELECT "+columns+join+" AND d.issue>? ORDER BY d.issue LIMIT "+limit;
        List<Models.Draw> result=new ArrayList<>();try(Cursor c=db.rawQuery(sql,after.isEmpty()?null:new String[]{after})){while(c.moveToNext()){
            Models.Draw d=new Models.Draw();d.issue=c.getString(0);d.openTime=c.getLong(1);d.fetchTime=c.getLong(2);for(int p=0;p<10;p++)d.numbers[p]=c.getInt(3+p);
            need(d.isValid()&&TargetIssueResolver.isValid(d.issue)&&DrawHashR36.sha256V2(d).equals(c.getString(13)),"LONG_CANONICAL_SOURCE_INVALID");result.add(d);
        }}return result;
    }
    /** One exact state/source fence and one bounded checkpoint publication. */
    public static void commit(SQLiteDatabase db,Prepared p){
        need(db!=null&&db.inTransaction()&&p!=null,"LONG_COMMIT_WRITER");Head current=head(db,false);
        need((current==null?"":current.fence()).equals(p.before)&&token(db)==p.canonicalGeneration&&DevelopmentProductionCP12.historyGeneration(db)==p.historyGeneration,"LONG_PREDECESSOR_CAS");
        long now=System.currentTimeMillis();need(now>=p.at&&p.available<=p.at,"LONG_PUBLICATION_CLOCK");
        if(current==null||!current.epoch.equals(p.epoch)){
            db.execSQL("INSERT INTO "+EPOCH+" VALUES(?,?,?,?,?)",new Object[]{p.epoch,PROTOCOL,p.first,p.at,p.reason});
            journal(db,current==null?p.epoch:current.epoch,current==null?p.first:current.through,p.reason,now);
        }
        if(current==null)db.execSQL("INSERT INTO "+HEAD+" VALUES(1,?,?,?,?,?,?,?,?,?,?)",new Object[]{p.epoch,p.through,p.generation,p.hash,p.chain,p.available,p.checkpoint,p.error.isEmpty()?"ACTIVE":"INVALID_SOURCE",p.error,now});
        else{
            db.execSQL("UPDATE "+HEAD+" SET epoch_hash=?,through_issue=?,generation=?,state_hash=?,source_chain=?,availability_max=?,state_blob=?,status=?,last_error=?,updated_at=? WHERE singleton=1 AND generation=? AND epoch_hash=? AND state_hash=?",new Object[]{p.epoch,p.through,p.generation,p.hash,p.chain,p.available,p.checkpoint,p.error.isEmpty()?"ACTIVE":"INVALID_SOURCE",p.error,now,current.generation,current.epoch,current.hash});
            need(changes(db)==1,"LONG_HEAD_CAS");
        }
        if(!p.error.isEmpty()){journal(db,p.epoch,p.through,p.error,now);return;}
        db.execSQL("INSERT INTO "+SNAPSHOT+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{V51ChecksumEngine.sha256(p.snapshot),p.epoch,p.through,p.generation,p.chain,p.hash,p.snapshot,p.at});
    }
    private static void journal(SQLiteDatabase db,String epoch,String through,String reason,long at){String hash=V51ChecksumEngine.sha256("LONG_JOURNAL_R80|"+epoch+'|'+through+'|'+reason+'|'+at);try(Cursor c=db.rawQuery("SELECT 1 FROM "+JOURNAL+" WHERE event_hash=?",new String[]{hash})){if(c.moveToFirst())return;}db.execSQL("INSERT INTO "+JOURNAL+" VALUES(?,?,?,?,?)",new Object[]{hash,epoch,through,reason,at});}
    public static boolean drainOne(DbStore store,long deadlineNs){
        if(store==null||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive()||System.nanoTime()+75_000_000L>=deadlineNs)return false;
        try(MemoryBudgetCP4.Lease memory=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().acquire(4L<<20)){
            String pending="";try(Cursor c=store.readable().rawQuery("SELECT s.snapshot_hash FROM "+SNAPSHOT+" s WHERE NOT EXISTS(SELECT 1 FROM "+PUBLICATION+" p WHERE p.snapshot_hash=s.snapshot_hash) ORDER BY s.through_issue LIMIT 1",null)){if(c.moveToFirst())pending=c.getString(0);}
            if(!pending.isEmpty()){publishAvailability(store,pending,"RECOVERED_AVAILABLE_NOW");return true;}
            Prepared prepared=prepare(store.readable(),deadlineNs);
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("LONG_SUMMARY_R80",db->{ResearchWorkBudgetR927.checkpoint();need(!RealtimePressureGateR811.hasActiveCriticalWork(),"LONG_YIELD_P0");commit(db,prepared);return Boolean.TRUE;});
            if(!prepared.snapshot.isEmpty())publishAvailability(store,V51ChecksumEngine.sha256(prepared.snapshot),"ORIGINAL_RETURN");return true;
        }catch(DevelopmentProductionCP12.Wait wait){if(!wait.reason.equals(store.getState("long_summary_status_r80","")))store.setState("long_summary_status_r80",wait.reason);return false;}
    }
    /** The summary is already durable when this timestamp is sampled. Recovery
     * supplies current availability, never an invented original return time. */
    private static void publishAvailability(DbStore store,String snapshot,String mode){
        final long available=System.currentTimeMillis();final String session=TimeSourceR811.clockSessionCP12();
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("LONG_SUMMARY_AVAILABLE_R80",db->{sealAvailability(db,snapshot,available,mode,session);return Boolean.TRUE;});
    }
    public static void sealAvailability(SQLiteDatabase db,String snapshot,long available,String mode,String session){
        need(db!=null&&db.inTransaction()&&snapshot!=null&&snapshot.matches("[0-9a-f]{64}")&&available>0&&("ORIGINAL_RETURN".equals(mode)||"RECOVERED_AVAILABLE_NOW".equals(mode))&&session!=null&&!session.isEmpty(),"LONG_AVAILABILITY_INPUT");
        String receipt=V51ChecksumEngine.sha256("LONG_AVAILABILITY_R80|"+snapshot+'|'+available+'|'+mode+'|'+session);
        try(Cursor existing=db.rawQuery("SELECT 1 FROM "+PUBLICATION+" WHERE snapshot_hash=?",new String[]{snapshot})){if(existing.moveToFirst())return;}
        String through;try(Cursor source=db.rawQuery("SELECT through_issue FROM "+SNAPSHOT+" WHERE snapshot_hash=?",new String[]{snapshot})){need(source.moveToFirst(),"LONG_PUBLICATION_SOURCE_MISSING");through=source.getString(0);}
        db.execSQL("INSERT INTO "+PUBLICATION+" VALUES(?,?,?,?,?,?)",new Object[]{snapshot,through,available,mode,session,receipt});
    }
    public static long sourceGenerationR81(SQLiteDatabase db){return token(db);}
    public static boolean activeEpoch(SQLiteDatabase db,String epoch){try(Cursor c=db.rawQuery("SELECT 1 FROM "+HEAD+" WHERE singleton=1 AND epoch_hash=? AND status='ACTIVE'",new String[]{epoch})){return c.moveToFirst();}}
    static String snapshotJson(String epoch,String first,String through,long generation,String chain,String state,long available,long captured,LongScaleCountsR80 counts){
        Map<String,Object> root=new TreeMap<>();root.put("revision","SC_LONG_SNAPSHOT_R80_1");root.put("game",ReleaseManifestR926.GAME_CODE);root.put("protocolHash",PROTOCOL);root.put("epochHash",epoch);root.put("firstIssue",first);root.put("throughIssue",through);root.put("generation",generation);root.put("sourceChain",chain);root.put("stateHash",state);root.put("availableAt",available);root.put("preparedAt",captured);
        List<Object> scales=new ArrayList<>();for(int w:LongScaleCountsR80.windows()){Map<String,Object> scale=new TreeMap<>();scale.put("window",w);scale.put("support",counts.support(w));List<Object> positions=new ArrayList<>();for(int p=1;p<=10;p++){List<Object> bins=new ArrayList<>();for(long n:counts.counts(p,w))bins.add(n);positions.add(bins);}scale.put("counts",positions);scales.add(scale);}root.put("scales",scales);return StrictCanonicalJson.writeContext(root);
    }
    private static int changes(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){need(c.moveToFirst(),"LONG_CHANGES_READ");return c.getInt(0);}}
    private static void need(boolean valid,String reason){if(!valid)throw new IllegalStateException(reason);}
}
