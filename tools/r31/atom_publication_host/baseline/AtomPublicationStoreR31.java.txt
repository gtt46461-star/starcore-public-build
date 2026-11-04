package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.BuildConfig;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.util.*;

/** Original atom/molecule computations publish immutable, complete result sets on the existing writer. */
public final class AtomPublicationStoreR31 {
    private AtomPublicationStoreR31() {}
    public static final String SCORE_VIEW="r31_atom_score_visible", CONTRIBUTION_VIEW="r31_atom_contribution_visible", MOLECULE_VIEW="r31_molecule_visible";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_atom_result_run(run_id TEXT PRIMARY KEY,kind TEXT NOT NULL CHECK(kind IN ('SCORES','MOLECULES')),dataset_hash TEXT NOT NULL,input_hash TEXT NOT NULL,operator_version TEXT NOT NULL,generation INTEGER NOT NULL,source_revision INTEGER NOT NULL,source_last_ordinal INTEGER NOT NULL,predecessor_run TEXT,expected_primary INTEGER NOT NULL,expected_contribution INTEGER NOT NULL,expected_chunks INTEGER NOT NULL CHECK(expected_chunks BETWEEN 1 AND 512),manifest_hash TEXT NOT NULL,total_bind_bytes INTEGER NOT NULL CHECK(total_bind_bytes>=0),work_cursor INTEGER NOT NULL CHECK(work_cursor>=0),computed_at INTEGER NOT NULL,owner_epoch TEXT NOT NULL,fence INTEGER NOT NULL CHECK(fence>=0),state TEXT NOT NULL CHECK(state IN ('PREPARING','PUBLISHED')),published_at INTEGER,CHECK((kind='SCORES' AND expected_primary=165 AND expected_contribution=165) OR (kind='MOLECULES' AND expected_primary=32 AND expected_contribution=32)))",
        "CREATE TABLE IF NOT EXISTS r31_atom_result_chunk(run_id TEXT NOT NULL,chunk_seq INTEGER NOT NULL CHECK(chunk_seq>=0),chunk_hash TEXT NOT NULL,primary_rows INTEGER NOT NULL CHECK(primary_rows>=0),contribution_rows INTEGER NOT NULL CHECK(contribution_rows>=0),bind_bytes INTEGER NOT NULL CHECK(bind_bytes>=0),committed_at INTEGER NOT NULL,PRIMARY KEY(run_id,chunk_seq),FOREIGN KEY(run_id) REFERENCES r31_atom_result_run(run_id))",
        "CREATE TABLE IF NOT EXISTS r31_atom_result_active(kind TEXT PRIMARY KEY CHECK(kind IN ('SCORES','MOLECULES')),run_id TEXT NOT NULL UNIQUE,activated_at INTEGER NOT NULL,FOREIGN KEY(run_id) REFERENCES r31_atom_result_run(run_id))",
        "CREATE TABLE IF NOT EXISTS r31_atom_score_stage(run_id TEXT NOT NULL,entry_index INTEGER NOT NULL CHECK(entry_index BETWEEN 0 AND 164),row_hash TEXT NOT NULL,atom_id TEXT NOT NULL,atom_index INTEGER NOT NULL,dataset_hash TEXT NOT NULL,n_select INTEGER NOT NULL,gain_select REAL,lcb_select REAL,ucb_select REAL,n_holdout INTEGER NOT NULL,gain_holdout REAL,lcb_holdout REAL,missing_rate REAL NOT NULL,state TEXT NOT NULL,derived INTEGER NOT NULL DEFAULT 0 CHECK(derived=0),definition TEXT NOT NULL DEFAULT '',computed_at INTEGER NOT NULL,PRIMARY KEY(run_id,entry_index),UNIQUE(run_id,atom_id),FOREIGN KEY(run_id) REFERENCES r31_atom_result_run(run_id))",
        "CREATE TABLE IF NOT EXISTS r31_atom_contribution_stage(run_id TEXT NOT NULL,entry_index INTEGER NOT NULL CHECK(entry_index BETWEEN 0 AND 164),row_hash TEXT NOT NULL,atom_id TEXT NOT NULL,dataset_hash TEXT NOT NULL,from_row INTEGER NOT NULL,to_row INTEGER NOT NULL,n_periods INTEGER NOT NULL,paired_comparisons INTEGER NOT NULL,mean_gain REAL,lcb95 REAL,source TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(run_id,entry_index),FOREIGN KEY(run_id) REFERENCES r31_atom_result_run(run_id))",
        "CREATE TABLE IF NOT EXISTS r31_molecule_stage(run_id TEXT NOT NULL,entry_index INTEGER NOT NULL CHECK(entry_index BETWEEN 0 AND 31),row_hash TEXT NOT NULL,molecule_hash TEXT NOT NULL,definition TEXT NOT NULL,generation INTEGER NOT NULL,state TEXT NOT NULL,select_gain REAL,reason TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(run_id,entry_index),FOREIGN KEY(run_id) REFERENCES r31_atom_result_run(run_id))",
        "CREATE INDEX IF NOT EXISTS r31_atom_run_publication ON r31_atom_result_run(kind,state,published_at,run_id)",
        "CREATE INDEX IF NOT EXISTS r31_atom_score_identity ON r31_atom_score_stage(atom_id,dataset_hash,run_id)",
        "CREATE INDEX IF NOT EXISTS r31_atom_contribution_identity ON r31_atom_contribution_stage(atom_id,dataset_hash,run_id,entry_index)",
        "CREATE INDEX IF NOT EXISTS r31_molecule_identity ON r31_molecule_stage(molecule_hash,run_id,entry_index)",
        "CREATE VIEW IF NOT EXISTS r31_atom_score_visible AS SELECT s.atom_id,s.atom_index,s.dataset_hash,s.n_select,s.gain_select,s.lcb_select,s.ucb_select,s.n_holdout,s.gain_holdout,s.lcb_holdout,s.missing_rate,s.state,s.derived,s.definition,s.computed_at FROM r27_atom_score s WHERE s.derived<>0 OR NOT EXISTS(SELECT 1 FROM r31_atom_result_active WHERE kind='SCORES') UNION ALL SELECT s.atom_id,s.atom_index,s.dataset_hash,s.n_select,s.gain_select,s.lcb_select,s.ucb_select,s.n_holdout,s.gain_holdout,s.lcb_holdout,s.missing_rate,s.state,s.derived,s.definition,s.computed_at FROM r31_atom_score_stage s JOIN r31_atom_result_active a ON a.run_id=s.run_id AND a.kind='SCORES' JOIN r31_atom_result_run r ON r.run_id=s.run_id AND r.state='PUBLISHED'",
        "CREATE VIEW IF NOT EXISTS r31_molecule_visible AS SELECT m.molecule_hash,m.definition,m.generation,m.state,m.select_gain,m.reason,m.created_at FROM r27_molecule m UNION ALL SELECT m.molecule_hash,m.definition,m.generation,m.state,m.select_gain,m.reason,m.created_at FROM r31_molecule_stage m JOIN r31_atom_result_run r ON r.run_id=m.run_id AND r.state='PUBLISHED' WHERE NOT EXISTS(SELECT 1 FROM r27_molecule old WHERE old.molecule_hash=m.molecule_hash) AND NOT EXISTS(SELECT 1 FROM r31_molecule_stage prior JOIN r31_atom_result_run pr ON pr.run_id=prior.run_id AND pr.state='PUBLISHED' WHERE prior.molecule_hash=m.molecule_hash AND (pr.published_at<r.published_at OR (pr.published_at=r.published_at AND (pr.run_id<r.run_id OR (pr.run_id=r.run_id AND prior.entry_index<m.entry_index)))))",
        "CREATE VIEW IF NOT EXISTS r31_atom_contribution_visible AS SELECT c.atom_id,c.dataset_hash,c.from_row,c.to_row,c.n_periods,c.paired_comparisons,c.mean_gain,c.lcb95,c.source,c.recorded_at FROM r30_atom_contribution c WHERE NOT EXISTS(SELECT 1 FROM r31_atom_contribution_stage n JOIN r31_atom_result_run r ON r.run_id=n.run_id AND r.state='PUBLISHED' WHERE n.atom_id=c.atom_id AND n.dataset_hash=c.dataset_hash) UNION ALL SELECT c.atom_id,c.dataset_hash,c.from_row,c.to_row,c.n_periods,c.paired_comparisons,c.mean_gain,c.lcb95,c.source,c.recorded_at FROM r31_atom_contribution_stage c JOIN r31_atom_result_run r ON r.run_id=c.run_id AND r.state='PUBLISHED' WHERE NOT EXISTS(SELECT 1 FROM r31_atom_contribution_stage n JOIN r31_atom_result_run nr ON nr.run_id=n.run_id AND nr.state='PUBLISHED' WHERE n.atom_id=c.atom_id AND n.dataset_hash=c.dataset_hash AND (nr.published_at>r.published_at OR (nr.published_at=r.published_at AND (nr.run_id>r.run_id OR (nr.run_id=r.run_id AND n.entry_index>c.entry_index)))))",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_score_stage_insert_guard BEFORE INSERT ON r31_atom_score_stage WHEN NOT EXISTS(SELECT 1 FROM r31_atom_result_run WHERE run_id=NEW.run_id AND state='PREPARING') BEGIN SELECT RAISE(ABORT,'R31_ATOM_RUN_NOT_PREPARING'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_score_stage_no_update BEFORE UPDATE ON r31_atom_score_stage BEGIN SELECT RAISE(ABORT,'R31_ATOM_STAGE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_score_stage_protect_active BEFORE DELETE ON r31_atom_score_stage WHEN EXISTS(SELECT 1 FROM r31_atom_result_active WHERE run_id=OLD.run_id) BEGIN SELECT RAISE(ABORT,'R31_ATOM_ACTIVE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_contribution_stage_insert_guard BEFORE INSERT ON r31_atom_contribution_stage WHEN NOT EXISTS(SELECT 1 FROM r31_atom_result_run WHERE run_id=NEW.run_id AND state='PREPARING') BEGIN SELECT RAISE(ABORT,'R31_ATOM_RUN_NOT_PREPARING'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_contribution_stage_no_update BEFORE UPDATE ON r31_atom_contribution_stage BEGIN SELECT RAISE(ABORT,'R31_ATOM_STAGE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_contribution_stage_protect_active BEFORE DELETE ON r31_atom_contribution_stage WHEN EXISTS(SELECT 1 FROM r31_atom_result_active WHERE run_id=OLD.run_id) BEGIN SELECT RAISE(ABORT,'R31_ATOM_ACTIVE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_molecule_stage_insert_guard BEFORE INSERT ON r31_molecule_stage WHEN NOT EXISTS(SELECT 1 FROM r31_atom_result_run WHERE run_id=NEW.run_id AND state='PREPARING') BEGIN SELECT RAISE(ABORT,'R31_ATOM_RUN_NOT_PREPARING'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_molecule_stage_no_update BEFORE UPDATE ON r31_molecule_stage BEGIN SELECT RAISE(ABORT,'R31_ATOM_STAGE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_molecule_stage_protect_active BEFORE DELETE ON r31_molecule_stage WHEN EXISTS(SELECT 1 FROM r31_atom_result_active WHERE run_id=OLD.run_id) BEGIN SELECT RAISE(ABORT,'R31_ATOM_ACTIVE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_result_chunk_insert_guard BEFORE INSERT ON r31_atom_result_chunk WHEN NOT EXISTS(SELECT 1 FROM r31_atom_result_run WHERE run_id=NEW.run_id AND state='PREPARING') BEGIN SELECT RAISE(ABORT,'R31_ATOM_RUN_NOT_PREPARING'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_result_chunk_no_update BEFORE UPDATE ON r31_atom_result_chunk BEGIN SELECT RAISE(ABORT,'R31_ATOM_STAGE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_result_chunk_protect_active BEFORE DELETE ON r31_atom_result_chunk WHEN EXISTS(SELECT 1 FROM r31_atom_result_active WHERE run_id=OLD.run_id) BEGIN SELECT RAISE(ABORT,'R31_ATOM_ACTIVE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_run_identity BEFORE UPDATE ON r31_atom_result_run WHEN NEW.run_id IS NOT OLD.run_id OR NEW.kind IS NOT OLD.kind OR NEW.dataset_hash IS NOT OLD.dataset_hash OR NEW.input_hash IS NOT OLD.input_hash OR NEW.operator_version IS NOT OLD.operator_version OR NEW.generation IS NOT OLD.generation OR NEW.source_revision IS NOT OLD.source_revision OR NEW.source_last_ordinal IS NOT OLD.source_last_ordinal OR NEW.predecessor_run IS NOT OLD.predecessor_run OR NEW.expected_primary IS NOT OLD.expected_primary OR NEW.expected_contribution IS NOT OLD.expected_contribution OR NEW.expected_chunks IS NOT OLD.expected_chunks OR NEW.manifest_hash IS NOT OLD.manifest_hash OR NEW.total_bind_bytes IS NOT OLD.total_bind_bytes OR NEW.work_cursor IS NOT OLD.work_cursor OR NEW.computed_at IS NOT OLD.computed_at OR NEW.owner_epoch IS NOT OLD.owner_epoch OR NEW.fence IS NOT OLD.fence OR NOT(OLD.state='PREPARING' AND NEW.state='PUBLISHED' AND OLD.published_at IS NULL AND NEW.published_at IS NOT NULL) BEGIN SELECT RAISE(ABORT,'R31_ATOM_RUN_IDENTITY_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_run_protect_active BEFORE DELETE ON r31_atom_result_run WHEN EXISTS(SELECT 1 FROM r31_atom_result_active WHERE run_id=OLD.run_id) BEGIN SELECT RAISE(ABORT,'R31_ATOM_ACTIVE_PROTECTED'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_active_insert_guard BEFORE INSERT ON r31_atom_result_active WHEN NOT(EXISTS(SELECT 1 FROM r31_atom_result_run r WHERE r.run_id=NEW.run_id AND r.kind=NEW.kind AND r.state='PUBLISHED' AND r.published_at IS NOT NULL AND (SELECT count(*) FROM r31_atom_result_chunk WHERE run_id=r.run_id)=r.expected_chunks AND (SELECT count(*) FROM r31_atom_contribution_stage WHERE run_id=r.run_id)=r.expected_contribution AND ((r.kind='SCORES' AND (SELECT count(*) FROM r31_atom_score_stage WHERE run_id=r.run_id)=r.expected_primary) OR (r.kind='MOLECULES' AND (SELECT count(*) FROM r31_molecule_stage WHERE run_id=r.run_id)=r.expected_primary)))) BEGIN SELECT RAISE(ABORT,'R31_ATOM_PUBLICATION_INCOMPLETE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_atom_active_update_guard BEFORE UPDATE ON r31_atom_result_active WHEN NOT(EXISTS(SELECT 1 FROM r31_atom_result_run r WHERE r.run_id=NEW.run_id AND r.kind=NEW.kind AND r.state='PUBLISHED' AND r.published_at IS NOT NULL AND (SELECT count(*) FROM r31_atom_result_chunk WHERE run_id=r.run_id)=r.expected_chunks AND (SELECT count(*) FROM r31_atom_contribution_stage WHERE run_id=r.run_id)=r.expected_contribution AND ((r.kind='SCORES' AND (SELECT count(*) FROM r31_atom_score_stage WHERE run_id=r.run_id)=r.expected_primary) OR (r.kind='MOLECULES' AND (SELECT count(*) FROM r31_molecule_stage WHERE run_id=r.run_id)=r.expected_primary)))) BEGIN SELECT RAISE(ABORT,'R31_ATOM_PUBLICATION_INCOMPLETE'); END"
    };
    private static final Map<SQLiteDatabase,Integer> READY=Collections.synchronizedMap(new WeakHashMap<>());
    static final String OPERATOR="R31_ATOM_PUBLICATION_V1|"+BuildConfig.SOURCE_WORKTREE_SHA256_R31;
    static final int PROTOCOL_OP_ALLOWANCE=32,MAX_BODY_OPS=32;

    /** Called exclusively by the existing startup schema owner, never a research task. */
    public static void ensureSchemaR31(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R31_ATOM_SCHEMA_OWNER_TRANSACTION_REQUIRED");
        for(String sql:DDL)db.execSQL(sql);
        READY.remove(db);requireReadyR31(db);
    }
    private static int schemaVersion(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())throw dependency("SCHEMA_VERSION_UNAVAILABLE");return c.getInt(0);}}
    private static String definition(String sql){return sql==null?null:sql.replace(" IF NOT EXISTS "," ").trim();}
    private static String objectName(String sql){String rest=sql.substring(sql.indexOf(" IF NOT EXISTS ")+15);int space=rest.indexOf(' '),paren=rest.indexOf('(');int end=space<0?paren:paren<0?space:Math.min(space,paren);return rest.substring(0,end);}
    /** One catalog snapshot checks definitions, including views/indexes/triggers, rather than only names. */
    public static int requireReadyR31(SQLiteDatabase db) {
        int version=schemaVersion(db);Integer cached=READY.get(db);if(cached!=null&&cached==version)return version;
        Map<String,String> actual=new HashMap<>();try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE name LIKE 'r31_atom_%' OR name LIKE 'r31_molecule_%'",null)){while(c.moveToNext())actual.put(c.getString(0),c.getString(1));}
        for(String sql:DDL){String name=objectName(sql);if(!definition(sql).equals(definition(actual.get(name))))throw dependency("SCHEMA_DEFINITION_MISMATCH:"+name);}
        if(schemaVersion(db)!=version)throw dependency("SCHEMA_CHANGED_DURING_READINESS");READY.put(db,version);return version;
    }
    static final class Dependency extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        Dependency(String detail){super("DEPENDENCY(R31_ATOM_PUBLICATION:"+detail+")");}
        public String failureCategory(){return "DEPENDENCY";}public String operation(){return "R31_ATOM_PUBLICATION";}public String phase(){return "PREPARED_RESULT_PROTOCOL";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    static Dependency dependency(String reason){return new Dependency(reason);}

    static final class Input {
        final String kind,dataset,input,run,predecessor,creator;final long revision,lastOrdinal,computedAt,fence;final int generation,schema;
        Input(String kind,String dataset,String input,String run,String predecessor,long revision,long ordinal,long at,int generation,String creator,long fence,int schema){this.kind=kind;this.dataset=dataset;this.input=input;this.run=run;this.predecessor=predecessor;this.revision=revision;lastOrdinal=ordinal;computedAt=at;this.generation=generation;this.creator=creator;this.fence=fence;this.schema=schema;}
    }
    static final class Pair {
        final int index;final Object[] primary,contribution;final String hash;
        Pair(int index,Object[] primary,Object[] contribution){this.index=index;this.primary=primary.clone();this.contribution=contribution.clone();hash=HashingSU1.fields("R31_ATOM_PAIR_V1",Integer.toString(index),JsonR27.write(Arrays.asList(Arrays.asList(this.primary),Arrays.asList(this.contribution))));}
    }
    static final class Part {
        final int sequence;final List<Pair> pairs;final String hash;final long bytes;
        Part(int seq,List<Pair> pairs,long bytes){sequence=seq;this.pairs=Collections.unmodifiableList(new ArrayList<>(pairs));this.bytes=bytes;List<Object> hashes=new ArrayList<>();for(Pair p:pairs)hashes.add(p.hash);hash=HashingSU1.fields("R31_ATOM_PART_V1",Integer.toString(seq),JsonR27.write(hashes));}
    }
    static final class Plan {
        final Input source;final List<Part> parts;final String manifest;final long bytes,workCursor;final int count;final ResearchWriteQueueR27.Op finalCache;
        Plan(Input source,List<Part> parts,long bytes,long work,int count,ResearchWriteQueueR27.Op cache){this.source=source;this.parts=Collections.unmodifiableList(parts);finalCache=cache;this.bytes=bytes+(cache==null?0:bindBytes(cache.args));workCursor=work;this.count=count;List<Object> hashes=new ArrayList<>();for(Part p:parts)hashes.add(p.hash);manifest=HashingSU1.fields("R31_ATOM_MANIFEST_V1",source.run,JsonR27.write(hashes),cache==null?"":JsonR27.write(Arrays.asList(cache.sql,Arrays.asList(cache.args))));}
    }
    static Input open(ResearchPoolR27.Worker w,String kind,String dataset,String input,int generation,long revision,long lastOrdinal){
        int schema=requireReadyR31(w.ro);RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();if(claim==null)throw dependency("TASK_CLAIM_REQUIRED");
        String run=HashingSU1.fields("R31_ATOM_RUN_V1",kind,input,OPERATOR),predecessor=null,creator=claim.owner;long at=System.currentTimeMillis(),fence=claim.fence;
        try(Cursor c=w.ro.rawQuery("SELECT run_id FROM r31_atom_result_active WHERE kind=?",new String[]{kind})){if(c.moveToFirst())predecessor=c.getString(0);}
        try(Cursor c=w.ro.rawQuery("SELECT kind,dataset_hash,input_hash,operator_version,generation,source_revision,source_last_ordinal,predecessor_run,computed_at,owner_epoch,fence FROM r31_atom_result_run WHERE run_id=?",new String[]{run})){
            if(c.moveToFirst()){
                if(!kind.equals(c.getString(0))||!dataset.equals(c.getString(1))||!input.equals(c.getString(2))||!OPERATOR.equals(c.getString(3))||generation!=c.getInt(4)||revision!=c.getLong(5)||lastOrdinal!=c.getLong(6))throw new IllegalStateException("R31_ATOM_RUN_IDENTITY_CONFLICT");
                predecessor=c.getString(7);at=c.getLong(8);creator=c.getString(9);fence=c.getLong(10);
            }
        }
        return new Input(kind,dataset,input,run,predecessor,revision,lastOrdinal,at,generation,creator,fence,schema);
    }
    static Plan prepare(Input source,List<Pair> pairs,long workCursor,ResearchWriteQueueR27.Op cache){
        int expected=source.kind.equals("SCORES")?165:32;if(pairs.size()!=expected)throw new IllegalArgumentException("R31_ATOM_COMPLETE_CANDIDATE_SET_REQUIRED");
        List<Part> parts=new ArrayList<>();List<Pair> current=new ArrayList<>();long all=0,bytes=0;
        for(int i=0;i<pairs.size();i++){
            Pair pair=pairs.get(i);if(pair.index!=i)throw new IllegalArgumentException("R31_ATOM_ENTRY_SEQUENCE");
            long size=pairBytes(source,pair);
            if(!fits(6,size))throw dependency("SINGLE_RESULT_BIND_BUDGET_DEBT");
            // Fixed one-result parts make the persisted manifest independent of a later process heap size.
            // Reopening the same input must not repartition/rename already committed scientific results.
            if(!current.isEmpty()){parts.add(new Part(parts.size(),current,bytes));current=new ArrayList<>();bytes=0;}
            current.add(pair);bytes+=size;all+=size;
        }
        if(!current.isEmpty())parts.add(new Part(parts.size(),current,bytes));
        if(cache!=null&&!fits(4,bindBytes(cache.args)))throw dependency("PUBLICATION_CACHE_BIND_BUDGET_DEBT");
        return new Plan(source,parts,all,workCursor,expected,cache);
    }
    private static long bindBytes(Object[] args){long n=0;for(Object v:args)n+=v instanceof String?((String)v).getBytes(java.nio.charset.StandardCharsets.UTF_8).length:8;return n;}
    private static long protocolBytes(){RuntimeCoordinatorR26.Claim c=RuntimeCoordinatorR26.currentClaimR30();return 16384L+16*bindBytes(new Object[]{c==null?"":c.owner,c==null?"":c.task,IssueTimelineWriterR30.bootId()});}
    private static boolean fits(int ops,long bytes){return ops+PROTOCOL_OP_ALLOWANCE<=64&&bytes+protocolBytes()<=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L);}
    private static Object[] bound(Input source,Pair pair,boolean primary){Object[] values=primary?pair.primary:pair.contribution,out=new Object[values.length+3];out[0]=source.run;out[1]=pair.index;out[2]=pair.hash;System.arraycopy(values,0,out,3,values.length);return out;}
    private static long pairBytes(Input source,Pair pair){return bindBytes(bound(source,pair,true))+bindBytes(bound(source,pair,false))+2*bindBytes(new Object[]{source.run,Integer.toString(pair.index),pair.hash});}
    private static void guard(SQLiteDatabase db,Input source){if(schemaVersion(db)!=source.schema)throw dependency("SCHEMA_CHANGED");ResearchInputPinR30R1.require(db,source.dataset,source.revision);}
    private static void verifyRow(SQLiteDatabase db,String table,String run,int index,String hash){try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" WHERE run_id=? AND entry_index=? AND row_hash=?",new String[]{run,Integer.toString(index),hash})){if(!c.moveToFirst())throw new IllegalStateException("R31_ATOM_STAGE_HASH_CONFLICT:"+table);}}
    private static void commit(ResearchPoolR27.Worker w,String operation,String key,String input,long seq,ResearchWriteQueueR27.Batch batch)throws InterruptedException {
        RuntimeCoordinatorR26.Claim c=RuntimeCoordinatorR26.currentClaimR30();if(c==null)throw dependency("TASK_CLAIM_REQUIRED");
        ResearchWriteQueueR27.ImmutableBatch frozen=batch.prepareR31(operation,HashingSU1.fields("R31_ATOM_COMMIT_V1",c.task,key),c.task,c.owner,c.fence,input,seq,OPERATOR);
        if(frozen.bindBytes+protocolBytes()>Math.min(262144L,Runtime.getRuntime().maxMemory()/512L))throw dependency("PREPARED_BIND_BUDGET_DEBT");
        while(!w.writes().submit(operation,frozen,w::cancelled)) {if(w.cancelled()||Thread.currentThread().isInterrupted())throw new InterruptedException("R31_ATOM_PUBLICATION_CANCELLED");CooperativeContinuationR26.yieldPreparedWrite();}
    }
    static void publish(ResearchPoolR27.Worker w,Plan plan)throws InterruptedException {
        Input in=plan.source;if(isActive(w.ro,in.kind,in.run))return;
        try(Cursor c=w.ro.rawQuery("SELECT state FROM r31_atom_result_run WHERE run_id=?",new String[]{in.run})){if(c.moveToFirst()&&"PUBLISHED".equals(c.getString(0)))throw dependency("COMPLETED_RESULT_SUPERSEDED");}
        ResearchWriteQueueR27.Batch header=new ResearchWriteQueueR27.Batch();
        header.guardBefore(db->{guard(db,in);return 0;});
        header.add("INSERT OR IGNORE INTO r31_atom_result_run(run_id,kind,dataset_hash,input_hash,operator_version,generation,source_revision,source_last_ordinal,predecessor_run,expected_primary,expected_contribution,expected_chunks,manifest_hash,total_bind_bytes,work_cursor,computed_at,owner_epoch,fence,state,published_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,'PREPARING',NULL)",in.run,in.kind,in.dataset,in.input,OPERATOR,in.generation,in.revision,in.lastOrdinal,in.predecessor,plan.count,plan.count,plan.parts.size(),plan.manifest,plan.bytes,plan.workCursor,in.computedAt,in.creator,in.fence);
        header.guardAfter(db->{verifyHeader(db,plan,false);return 0;});commit(w,"ATOM_RESULT_PREPARE",in.run+"|HEADER",in.input,0,header);
        for(Part part:plan.parts){
            if(w.cancelled()||Thread.currentThread().isInterrupted())throw new InterruptedException("R31_ATOM_PUBLICATION_CANCELLED");
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();b.guardBefore(db->{guard(db,in);verifyHeader(db,plan,false);return 0;});
            String primary=in.kind.equals("SCORES")?"r31_atom_score_stage":"r31_molecule_stage";
            int columns=in.kind.equals("SCORES")?18:10;
            for(Pair row:part.pairs){b.add(insertSql(primary,columns),bound(in,row,true));b.add(insertSql("r31_atom_contribution_stage",13),bound(in,row,false));}
            b.add("INSERT OR IGNORE INTO r31_atom_result_chunk(run_id,chunk_seq,chunk_hash,primary_rows,contribution_rows,bind_bytes,committed_at) VALUES(?,?,?,?,?,?,CAST(strftime('%s','now') AS INTEGER)*1000)",in.run,part.sequence,part.hash,part.pairs.size(),part.pairs.size(),part.bytes);
            b.guardAfter(db->{for(Pair row:part.pairs){verifyRow(db,primary,in.run,row.index,row.hash);verifyRow(db,"r31_atom_contribution_stage",in.run,row.index,row.hash);}try(Cursor c=db.rawQuery("SELECT 1 FROM r31_atom_result_chunk WHERE run_id=? AND chunk_seq=? AND chunk_hash=? AND primary_rows=? AND contribution_rows=? AND bind_bytes=?",new String[]{in.run,Integer.toString(part.sequence),part.hash,Integer.toString(part.pairs.size()),Integer.toString(part.pairs.size()),Long.toString(part.bytes)})){if(!c.moveToFirst())throw new IllegalStateException("R31_ATOM_CHUNK_HASH_CONFLICT");}return 0;});
            commit(w,"ATOM_RESULT_STAGE",in.run+"|CHUNK|"+part.sequence+"|"+part.hash,in.input,part.sequence+1,b);
        }
        ResearchWriteQueueR27.Batch done=new ResearchWriteQueueR27.Batch();done.guardBefore(db->{guard(db,in);verifyHeader(db,plan,false);verifyComplete(db,plan);String active=activeRun(db,in.kind);if(!Objects.equals(active,in.predecessor))throw dependency("ACTIVE_PREDECESSOR_CHANGED");return 0;});
        // Publication order is durable even for two commits in one clock tick or a wall-clock rollback.
        // computed_at remains the original computation clock; published_at is its strictly ordered commit clock.
        done.add("UPDATE r31_atom_result_run SET state='PUBLISHED',published_at=max(CAST((julianday('now')-2440587.5)*86400000 AS INTEGER),coalesce((SELECT max(published_at)+1 FROM r31_atom_result_run WHERE state='PUBLISHED'),0)) WHERE run_id=? AND state='PREPARING'",in.run);
        if(in.predecessor==null)done.add("INSERT INTO r31_atom_result_active(kind,run_id,activated_at) SELECT ?,?,CAST(strftime('%s','now') AS INTEGER)*1000 WHERE NOT EXISTS(SELECT 1 FROM r31_atom_result_active WHERE kind=?)",in.kind,in.run,in.kind);
        else done.add("UPDATE r31_atom_result_active SET run_id=?,activated_at=CAST(strftime('%s','now') AS INTEGER)*1000 WHERE kind=? AND run_id=?",in.run,in.kind,in.predecessor);
        if(plan.finalCache!=null)done.add(plan.finalCache.sql,plan.finalCache.args);
        done.guardAfter(db->{if(!isActive(db,in.kind,in.run))throw dependency("ACTIVE_PUBLICATION_CAS_FAILED");return 0;});
        commit(w,in.kind.equals("SCORES")?"ATOM_SCORES":"MOLECULES",in.run+"|PUBLISH",in.input,plan.parts.size()+1,done);
        if(!isActive(w.ro,in.kind,in.run))throw dependency("COMPLETED_RESULT_SUPERSEDED");
    }
    private static String insertSql(String table,int count){StringBuilder s=new StringBuilder("INSERT OR IGNORE INTO ").append(table).append(" VALUES(");for(int i=0;i<count;i++){if(i>0)s.append(',');s.append('?');}return s.append(')').toString();}
    static String activeRun(SQLiteDatabase db,String kind){try(Cursor c=db.rawQuery("SELECT run_id FROM r31_atom_result_active WHERE kind=?",new String[]{kind})){return c.moveToFirst()?c.getString(0):null;}}
    static boolean isActive(SQLiteDatabase db,String kind,String run){try(Cursor c=db.rawQuery("SELECT 1 FROM r31_atom_result_active a JOIN r31_atom_result_run r ON r.run_id=a.run_id WHERE a.kind=? AND a.run_id=? AND r.state='PUBLISHED'",new String[]{kind,run})){return c.moveToFirst();}}
    private static void verifyHeader(SQLiteDatabase db,Plan p,boolean published){try(Cursor c=db.rawQuery("SELECT kind,dataset_hash,input_hash,operator_version,generation,source_revision,source_last_ordinal,expected_primary,expected_contribution,expected_chunks,manifest_hash,total_bind_bytes,work_cursor,computed_at,state FROM r31_atom_result_run WHERE run_id=?",new String[]{p.source.run})){
        Input in=p.source;if(!c.moveToFirst()||!in.kind.equals(c.getString(0))||!in.dataset.equals(c.getString(1))||!in.input.equals(c.getString(2))||!OPERATOR.equals(c.getString(3))||in.generation!=c.getInt(4)||in.revision!=c.getLong(5)||in.lastOrdinal!=c.getLong(6)||p.count!=c.getInt(7)||p.count!=c.getInt(8)||p.parts.size()!=c.getInt(9)||!p.manifest.equals(c.getString(10))||p.bytes!=c.getLong(11)||p.workCursor!=c.getLong(12)||in.computedAt!=c.getLong(13)||!(published?"PUBLISHED":"PREPARING").equals(c.getString(14)))throw new IllegalStateException("R31_ATOM_MANIFEST_IDENTITY_CONFLICT");
    }}
    private static void verifyComplete(SQLiteDatabase db,Plan p){
        try(Cursor c=db.rawQuery("SELECT chunk_seq,chunk_hash,primary_rows,contribution_rows,bind_bytes FROM r31_atom_result_chunk WHERE run_id=? ORDER BY chunk_seq",new String[]{p.source.run})){
            int i=0;while(c.moveToNext()){if(i>=p.parts.size())throw new IllegalStateException("R31_ATOM_EXTRA_CHUNK");Part part=p.parts.get(i);if(c.getInt(0)!=i||!part.hash.equals(c.getString(1))||c.getInt(2)!=part.pairs.size()||c.getInt(3)!=part.pairs.size()||c.getLong(4)!=part.bytes)throw new IllegalStateException("R31_ATOM_INCOMPLETE_MANIFEST");i++;}if(i!=p.parts.size())throw new IllegalStateException("R31_ATOM_MISSING_CHUNK");
        }
        for(String table:new String[]{p.source.kind.equals("SCORES")?"r31_atom_score_stage":"r31_molecule_stage","r31_atom_contribution_stage"})try(Cursor c=db.rawQuery("SELECT count(*) FROM "+table+" WHERE run_id=?",new String[]{p.source.run})){if(!c.moveToFirst()||c.getInt(0)!=p.count)throw new IllegalStateException("R31_ATOM_INCOMPLETE_RESULT_COUNT");}
    }
    static Map<Integer,String> activeStates(SQLiteDatabase db){
        requireReadyR31(db);Map<Integer,String> states=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery("SELECT r.dataset_hash,r.source_revision,r.operator_version FROM r31_atom_result_active a JOIN r31_atom_result_run r ON r.run_id=a.run_id WHERE a.kind='SCORES' AND r.state='PUBLISHED'",null)){
            if(!c.moveToFirst())return states;if(!OPERATOR.equals(c.getString(2)))return states;
            try{ResearchInputPinR30R1.require(db,c.getString(0),c.getLong(1));}catch(ResearchStoreR30R1.StaleResearch corrected){return states;}
        }
        try(Cursor c=db.rawQuery("SELECT s.atom_index,s.state FROM r31_atom_score_stage s JOIN r31_atom_result_active a ON a.kind='SCORES' AND a.run_id=s.run_id JOIN r31_atom_result_run r ON r.run_id=s.run_id AND r.state='PUBLISHED' ORDER BY s.entry_index",null)){while(c.moveToNext())states.put(c.getInt(0),c.getString(1));}
        if(states.size()!=165)throw new IllegalStateException("R31_ATOM_PUBLISHED_SET_INCOMPLETE");return states;
    }
    static List<PolicyAstR27.Node> activeMolecules(SQLiteDatabase db) {
        requireReadyR31(db);List<PolicyAstR27.Node> nodes=new ArrayList<>();int count=0;
        try(Cursor c=db.rawQuery("SELECT r.dataset_hash,r.source_revision,r.operator_version FROM r31_atom_result_active a JOIN r31_atom_result_run r ON r.run_id=a.run_id WHERE a.kind='MOLECULES' AND r.state='PUBLISHED'",null)) {
            if(!c.moveToFirst()||!OPERATOR.equals(c.getString(2)))return nodes;
            try{ResearchInputPinR30R1.require(db,c.getString(0),c.getLong(1));}catch(ResearchStoreR30R1.StaleResearch corrected){return nodes;}
        }
        try(Cursor c=db.rawQuery("SELECT s.state,s.definition FROM r31_molecule_stage s JOIN r31_atom_result_active a ON a.kind='MOLECULES' AND a.run_id=s.run_id JOIN r31_atom_result_run r ON r.run_id=s.run_id AND r.state='PUBLISHED' ORDER BY s.entry_index",null)) {
            while(c.moveToNext()){count++;if("SURVIVED".equals(c.getString(0))){String text=c.getString(1);int at=text.lastIndexOf('@');if(at<0)throw new IllegalStateException("R31_MOLECULE_DEFINITION");nodes.add(PolicyAstR27.parse(text.substring(0,at)));}}
        }
        if(count!=32)throw new IllegalStateException("R31_MOLECULE_PUBLISHED_SET_INCOMPLETE");return nodes;
    }
}
