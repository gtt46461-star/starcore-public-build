package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Durable per-table retention migration through the original single writer. */
public final class RetentionMigrationR34 {
    private RetentionMigrationR34() {}
    private static final String REVISION="R34_RETENTION_DEFINITIONS_1";
    private static final SchemaUnitPlanR34.Budget BUDGET=new SchemaUnitPlanR34.Budget(16,256,32,262144);
    private static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r34_schema_unit_run (\n  run_id TEXT PRIMARY KEY,\n  database_id TEXT NOT NULL,\n  migration_id TEXT NOT NULL,\n  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),\n  schema_revision INTEGER NOT NULL CHECK(schema_revision>0),\n  definition_revision TEXT NOT NULL,\n  table_count INTEGER NOT NULL CHECK(table_count>0),\n  phase TEXT NOT NULL CHECK(phase IN ('INSTALL','VERIFY','DONE')),\n  next_index INTEGER NOT NULL CHECK(next_index>=0 AND next_index<=table_count),\n  revision INTEGER NOT NULL CHECK(revision>=0),\n  owner_epoch TEXT NOT NULL,\n  owner_fence INTEGER NOT NULL CHECK(owner_fence>0),\n  audit_round INTEGER NOT NULL CHECK(audit_round>=0),\n  audit_schema_cookie INTEGER NOT NULL DEFAULT -1 CHECK(audit_schema_cookie>=-1),\n  last_commit_key TEXT NOT NULL DEFAULT '',\n  updated_at INTEGER NOT NULL,\n  CHECK((phase='DONE' AND next_index=table_count AND audit_schema_cookie>=0)\n    OR (phase<>'DONE' AND next_index<table_count))\n)",
        "CREATE TABLE IF NOT EXISTS r34_schema_unit_receipt (\n  commit_key TEXT PRIMARY KEY CHECK(length(commit_key)=64),\n  run_id TEXT NOT NULL,\n  plan_hash TEXT NOT NULL CHECK(length(plan_hash)=64),\n  phase TEXT NOT NULL CHECK(phase IN ('INSTALL','VERIFY')),\n  unit_index INTEGER NOT NULL CHECK(unit_index>=0),\n  source_table TEXT NOT NULL,\n  audit_round INTEGER NOT NULL CHECK(audit_round>=0),\n  cursor_before INTEGER NOT NULL CHECK(cursor_before>=0),\n  cursor_after INTEGER NOT NULL CHECK(cursor_after=cursor_before+1),\n  owner_epoch TEXT NOT NULL,\n  owner_fence INTEGER NOT NULL CHECK(owner_fence>0),\n  payload_hash TEXT NOT NULL CHECK(length(payload_hash)=64),\n  definition_hash TEXT NOT NULL CHECK(length(definition_hash)=64),\n  original_root TEXT NOT NULL CHECK(length(original_root)=64),\n  schema_cookie_before INTEGER NOT NULL,\n  schema_cookie_after INTEGER NOT NULL,\n  committed_at INTEGER NOT NULL,\n  UNIQUE(run_id,phase,unit_index,audit_round),\n  FOREIGN KEY(run_id) REFERENCES r34_schema_unit_run(run_id)\n)",
        "CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_update\nBEFORE UPDATE ON r34_schema_unit_receipt BEGIN\n  SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_IMMUTABLE');\nEND",
        "CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_delete\nBEFORE DELETE ON r34_schema_unit_receipt BEGIN\n  SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_RETENTION_REQUIRES_OWNER_PROTOCOL');\nEND",
        "CREATE TRIGGER IF NOT EXISTS r34_schema_unit_receipt_no_replace\nBEFORE INSERT ON r34_schema_unit_receipt\nWHEN EXISTS(SELECT 1 FROM r34_schema_unit_receipt WHERE commit_key=NEW.commit_key\n OR (run_id=NEW.run_id AND phase=NEW.phase AND unit_index=NEW.unit_index AND audit_round=NEW.audit_round))\nBEGIN SELECT RAISE(ABORT,'R34_SCHEMA_RECEIPT_DUPLICATE_REQUIRE_READBACK'); END"
    };
    private static long scalar(SQLiteDatabase db,String sql,String... args) {
        try(Cursor c=db.rawQuery(sql,args.length==0?null:args)) {
            if(!c.moveToFirst())throw new IllegalStateException("R34_MIGRATION_SCALAR");
            return c.getLong(0);
        }
    }
    private static String databaseId(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT database_id FROM r34_database_identity WHERE singleton=1",null)) {
            if(!c.moveToFirst())throw new IllegalStateException("R34_DATABASE_IDENTITY_MISSING");
            return c.getString(0);
        }
    }
    private static long cookie(SQLiteDatabase db) {return scalar(db,"PRAGMA main.schema_version");}
    private static long dataVersion(SQLiteDatabase db) {return scalar(db,"PRAGMA main.data_version");}
    private static String sourceDefinition(SQLiteDatabase db,String source) {
        try(Cursor c=db.rawQuery("SELECT sql FROM main.sqlite_master WHERE type='table' AND name=?",new String[]{source})) {
            return RetentionDefinitionsR34.approved(source,c.moveToFirst()?c.getString(0):null);
        }
    }
    // A busy telemetry or fetch writer changes main.data_version even when a
    // protected table is absent or empty. Probe one row before recomputing its
    // exact empty digest under the writer. Nonempty originals stay outside it.
    private static boolean emptySource(SQLiteDatabase db,String source) {
        if(!SchemaUnitPlanR34.PROTECTED_TABLES_095.contains(source))throw new IllegalArgumentException("R34_UNKNOWN_PROTECTED_TABLE");
        return !ResearchRetentionStoreR30.exists(db,source) || scalar(db,"SELECT NOT EXISTS(SELECT 1 FROM main."+source+" LIMIT 1)")!=0;
    }
    private static void bootstrap(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS r34_database_identity(singleton INTEGER PRIMARY KEY CHECK(singleton=1),database_id TEXT NOT NULL)");
        db.execSQL("INSERT OR IGNORE INTO r34_database_identity VALUES(1,?)",new Object[]{UUID.randomUUID().toString()});
        for(String sql:DDL)db.execSQL(sql);
        ResearchRetentionStoreR30.initializeProtectionMetadataR34(db);
    }
    static boolean runtimeBaseCurrent(SQLiteDatabase db) {
        if(!ResearchRetentionStoreR30.exists(db,"r34_runtime_startup_seal"))return false;
        try(Cursor c=db.rawQuery("SELECT schema_cookie FROM r34_runtime_startup_seal WHERE source_commit=?",
                new String[]{BuildConfig.SOURCE_COMMIT_R26})) {
            return c.moveToFirst() && c.getLong(0)==cookie(db);
        }
    }

    static void sealRuntimeBase(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R34_RUNTIME_SEAL_OWNER");
        db.execSQL("CREATE TABLE IF NOT EXISTS r34_runtime_startup_seal(source_commit TEXT PRIMARY KEY,schema_cookie INTEGER NOT NULL)");
        db.execSQL("INSERT OR REPLACE INTO r34_runtime_startup_seal VALUES(?,?)",
            new Object[]{BuildConfig.SOURCE_COMMIT_R26,cookie(db)});
    }
    private static SchemaUnitPlanR34.Plan plan(SQLiteDatabase db) {
        LinkedHashMap<String,SchemaUnitPlanR34.Definition> definitions=new LinkedHashMap<>();
        for(String source:SchemaUnitPlanR34.PROTECTED_TABLES_095) {
            String expected=sourceDefinition(db,source);
            definitions.put(source,new SchemaUnitPlanR34.Definition(expected,
                ResearchRetentionStoreR30.protectionDefinitionR34(db,source),ResearchRetentionStoreR30.exists(db,source)));
        }
        return new SchemaUnitPlanR34.Plan(databaseId(db),"R34_RETENTION",1,REVISION,definitions);
    }
    private static SchemaUnitPlanR34.Cursor cursor(SQLiteDatabase db,SchemaUnitPlanR34.Plan plan) {
        try(Cursor row=db.rawQuery("SELECT run_id,owner_epoch,phase,next_index,revision,owner_fence,audit_round,audit_schema_cookie FROM r34_schema_unit_run WHERE plan_hash=?",new String[]{plan.planHash})) {
            if(!row.moveToFirst())throw new IllegalStateException("R34_MIGRATION_RUN_MISSING");
            return new SchemaUnitPlanR34.Cursor(plan.planHash,row.getString(0),row.getString(1),
                SchemaUnitPlanR34.Phase.valueOf(row.getString(2)),row.getInt(3),row.getLong(4),row.getLong(5),row.getLong(6),row.getLong(7));
        }
    }
    private static void seed(SQLiteDatabase db,SchemaUnitPlanR34.Plan plan,String ownerEpoch) {
        db.execSQL("INSERT OR IGNORE INTO r34_schema_unit_run VALUES(?,?,?,?,?,?,?,'INSTALL',0,0,?,1,0,-1,'',?)",
            new Object[]{plan.planHash,plan.databaseId,plan.migrationId,plan.planHash,plan.schemaRevision,plan.definitionRevision,
                plan.units.size(),ownerEpoch,System.currentTimeMillis()});
        db.execSQL("UPDATE r34_schema_unit_run SET owner_epoch=?,owner_fence=owner_fence+1,revision=revision+1,updated_at=? WHERE run_id=? AND owner_epoch<>?",
            new Object[]{ownerEpoch,System.currentTimeMillis(),plan.planHash,ownerEpoch});
    }
    private static void advance(SQLiteDatabase db,SchemaUnitPlanR34.Cursor before,SchemaUnitPlanR34.Cursor after,String key) {
        db.execSQL("UPDATE r34_schema_unit_run SET phase=?,next_index=?,revision=?,audit_round=?,audit_schema_cookie=?,last_commit_key=?,updated_at=?"
            +" WHERE run_id=? AND plan_hash=? AND owner_epoch=? AND owner_fence=? AND phase=? AND next_index=? AND revision=? AND audit_round=?",
            new Object[]{after.phase.name(),after.nextIndex,after.revision,after.auditRound,after.auditSchemaCookie,key,System.currentTimeMillis(),
                before.runId,before.planHash,before.ownerEpoch,before.fence,before.phase.name(),before.nextIndex,before.revision,before.auditRound});
        if(scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R34_SCHEMA_CURSOR_CAS");
    }
    private static SchemaUnitPlanR34.View view(SQLiteDatabase db,String source,String root) {
        return new SchemaUnitPlanR34.View(databaseId(db),1,REVISION,cookie(db),sourceDefinition(db,source),root,
            ResearchRetentionStoreR30.exists(db,source));
    }
    private static SchemaUnitPlanR34.ReadBack readBack(SQLiteDatabase db,SchemaUnitPlanR34.Plan plan,String key) {
        try(Cursor c=db.rawQuery("SELECT r.payload_hash,s.run_id,s.owner_epoch,s.phase,s.next_index,s.revision,s.owner_fence,s.audit_round,s.audit_schema_cookie"
            +" FROM r34_schema_unit_receipt r JOIN r34_schema_unit_run s ON s.run_id=r.run_id WHERE r.commit_key=? AND s.plan_hash=?",
            new String[]{key,plan.planHash})) {
            if(!c.moveToFirst())return null;
            SchemaUnitPlanR34.Cursor current=new SchemaUnitPlanR34.Cursor(plan.planHash,c.getString(1),c.getString(2),
                SchemaUnitPlanR34.Phase.valueOf(c.getString(3)),c.getInt(4),c.getLong(5),c.getLong(6),c.getLong(7),c.getLong(8));
            return new SchemaUnitPlanR34.ReadBack(key,c.getString(0),current);
        }
    }
    private static void noMetadataTriggers(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT name FROM sqlite_master WHERE type='trigger' AND tbl_name IN ('r30_retention_archive_index','r34_database_identity','r34_schema_unit_run') LIMIT 1",null)) {
            if(c.moveToFirst())throw new IllegalStateException("R34_MIGRATION_UNKNOWN_METADATA_TRIGGER:"+c.getString(0));
        }
    }
    @FunctionalInterface
    interface Writer {
        void write(DatabaseCoordinator.WriteContext context, SchemaDomainCoordinatorR926.Step task);
    }
    private static DatabaseCoordinator.WriteContext migration(String operation) {
        return DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.MIGRATION,operation);
    }
    static final class Session {
        private final SQLiteDatabase read;
        private final Writer writer;
        private final String ownerEpoch;
        private SchemaUnitPlanR34.Plan plan;
        private long planCookie = -1;

        Session(SQLiteDatabase read,Writer writer,String ownerEpoch) {
            this.read=read;
            this.writer=writer;
            this.ownerEpoch=ownerEpoch;
            writer.write(migration("R34_RETENTION_BOOTSTRAP"),RetentionMigrationR34::bootstrap);
        }

        /** One call commits one protection step or observes the existing DONE certificate. */
        boolean step() {
            long schemaCookie=cookie(read);
            if(plan==null || planCookie!=schemaCookie) {
                SchemaUnitPlanR34.Plan preparedPlan=plan(read);
                if(schemaCookie!=cookie(read))return false;
                if(plan==null || !plan.planHash.equals(preparedPlan.planHash))
                    writer.write(migration("R34_RETENTION_RUN"),db->seed(db,preparedPlan,ownerEpoch));
                plan=preparedPlan;
                planCookie=schemaCookie;
            }
            SchemaUnitPlanR34.Cursor before=cursor(read,plan);
            if(!before.ownerEpoch.equals(ownerEpoch))throw new IllegalStateException("R34_STALE_MIGRATION_OWNER");
            if(before.phase==SchemaUnitPlanR34.Phase.DONE && SchemaUnitPlanR34.certificateCurrent(plan,before,plan.databaseId,1,REVISION,cookie(read)))return true;
            if(before.phase!=SchemaUnitPlanR34.Phase.INSTALL && before.auditSchemaCookie>=0 && before.auditSchemaCookie!=cookie(read)) {
                SchemaUnitPlanR34.Cursor restarted=SchemaUnitPlanR34.restartVerification(plan,before);
                writer.write(migration("R34_RETENTION_REVERIFY"),db->advance(db,before,restarted,""));
                return false;
            }
            String source=plan.units.get(before.nextIndex).source;
            long generation=dataVersion(read);
            String root=SchemaDomainCoordinatorR926.retentionOriginalRootR34(read,source);
            SchemaUnitPlanR34.View prepared=view(read,source,root);
            if(prepared.schemaCookie!=planCookie)return false;
            if(generation!=dataVersion(read)&&!emptySource(read,source))return false;
            boolean install=before.phase==SchemaUnitPlanR34.Phase.INSTALL;
            SchemaUnitPlanR34.Cost cost=ResearchRetentionStoreR30.protectionCostR34(read,source,install);
            noMetadataTriggers(read);
            if(cookie(read)!=prepared.schemaCookie)return false;
            SchemaUnitPlanR34.Ticket ticket=SchemaUnitPlanR34.prepare(plan,before,prepared,cost,BUDGET,SystemClock.elapsedRealtime()+250);
            final SchemaUnitPlanR34.ReceiptPayload[] payload={null};
            try {
                writer.write(migration("R34_RETENTION_UNIT").withDeadlineMonoMs(ticket.deadlineMonoMs),db->{
                    // Once the writer starts, other connections cannot install a trigger or
                    // replace the checked source definition without changing this schema cookie.
                    if(cookie(db)!=prepared.schemaCookie || !databaseId(db).equals(prepared.databaseId))
                        throw new DatabaseCoordinator.SchemaPendingR31("R34_RETENTION","WAIT_PREPARED_PROOF");
                    if(dataVersion(read)!=generation && !(emptySource(db,source)
                            && root.equals(SchemaDomainCoordinatorR926.retentionOriginalRootR34(db,source))))
                        throw new DatabaseCoordinator.SchemaPendingR31("R34_RETENTION","WAIT_PREPARED_PROOF");
                    SchemaUnitPlanR34.Admission admission=SchemaUnitPlanR34.beforeWrite(plan,ticket,cursor(db,plan),prepared,SystemClock.elapsedRealtime());
                    if(install) {
                        if(source.equals("r26_feedback"))SchemaDomainCoordinatorR926.migrateR31RetentionProtection(db,x->ResearchRetentionStoreR30.installProtectionUnitR34(x,source));
                        else ResearchRetentionStoreR30.installProtectionUnitR34(db,source);
                    }
                    ResearchRetentionStoreR30.verifyProtectionUnitR34(db,source);
                    // The admitted owner executes DDL and its index DML only. The index has no triggers.
                    // Source and archive row mutation is excluded by construction of this owner.
                    long nextCookie=cookie(db);
                    int schemaChanges=Math.toIntExact(nextCookie-prepared.schemaCookie);
                    SchemaUnitPlanR34.Cost applied=new SchemaUnitPlanR34.Cost(schemaChanges,cost.columns,cost.attachments,cost.definitionBytes);
                    SchemaUnitPlanR34.Effect effect=new SchemaUnitPlanR34.Effect(
                        ResearchRetentionStoreR30.protectionDefinitionR34(db,source),root,nextCookie,applied);
                    payload[0]=SchemaUnitPlanR34.receiptPayload(admission,effect);
                    SchemaUnitPlanR34.ReceiptPayload value=payload[0];
                    db.execSQL("INSERT INTO r34_schema_unit_receipt VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{value.commitKey,before.runId,plan.planHash,before.phase.name(),before.nextIndex,source,before.auditRound,
                            before.revision,value.next.revision,before.ownerEpoch,before.fence,value.payloadHash,value.installedDefinitionHash,
                            root,value.schemaCookieBefore,value.schemaCookieAfter,System.currentTimeMillis()});
                    advance(db,before,value.next,value.commitKey);
                });
            } catch(DatabaseCoordinator.WriteRejectedR31 failure) {
                // A confirmed rollback before DDL leaves the same durable unit ready.
                // Never retry a fault in its definition, owner fence, cost or receipt as a timeout.
                if (failure.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK
                        && failure.getCause() instanceof SchemaUnitPlanR34.ProtocolViolation) {
                    String code=((SchemaUnitPlanR34.ProtocolViolation)failure.getCause()).code;
                    if ("DEADLINE_BEFORE_BEGIN".equals(code) || "REPREPARE_SCHEMA_COOKIE".equals(code)
                            || "REPREPARE_ORIGINAL_INPUT".equals(code)) return false;
                }
                if(payload[0]==null || failure.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK)throw failure;
                SchemaUnitPlanR34.resolve(ticket,payload[0],SchemaUnitPlanR34.Outcome.UNKNOWN,readBack(read,plan,ticket.commitKey));
                return false;
            }
            if(payload[0]==null)throw new IllegalStateException("R34_SCHEMA_RECEIPT_NOT_PRODUCED");
            SchemaUnitPlanR34.Ack ack=SchemaUnitPlanR34.resolve(ticket,payload[0],SchemaUnitPlanR34.Outcome.COMMITTED,readBack(read,plan,ticket.commitKey));
            if(ack.resolution!=SchemaUnitPlanR34.Resolution.ACKNOWLEDGED)return false;
            // This owner only installed the derived archive, its guards and its journal.
            // External DDL still changes the cookie and forces a complete plan comparison.
            planCookie=payload[0].schemaCookieAfter;
            return ack.cursor.phase==SchemaUnitPlanR34.Phase.DONE;
        }
    }

    static void prepare(DbStore store,String ownerEpoch,java.util.function.BooleanSupplier cancelled,Runnable yield) {
        DatabaseCoordinator owner=store.coordinatorForResearchR952();
        try(SQLiteDatabase read=SQLiteDatabase.openDatabase(store.readable().getPath(),null,
                SQLiteDatabase.OPEN_READONLY|SQLiteDatabase.NO_LOCALIZED_COLLATORS)) {
            Writer writer=(context,task)->owner.runAtomicWriteOrThrowF01(context,db->{task.run(db);return 0;});
            Session session=null;
            while(!cancelled.getAsBoolean()){
                try{
                    // Bootstrap can miss the writer just like an individual unit. Keep an
                    // already completed base migration and this read connection across retries.
                    if(session==null)session=new Session(read,writer,ownerEpoch);
                    if(session.step())return;
                }catch(DatabaseCoordinator.WriteRejectedR31 failure){
                    if(!ResearchPoolR27.retryRolledBackLock(failure)
                            && !(failure.commitOutcome==DatabaseCoordinator.CommitOutcome.ROLLED_BACK
                            && failure.getCause() instanceof DatabaseCoordinator.SchemaPendingR31
                            && "SCHEMA_PENDING:R34_RETENTION:WAIT_PREPARED_PROOF".equals(failure.getCause().getMessage())))throw failure;
                }
                yield.run();
            }
        }
    }
}
