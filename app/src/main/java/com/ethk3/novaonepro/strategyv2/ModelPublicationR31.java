package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import com.ethk3.novaonepro.DbStore;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Owner-prepared SU2 publications. Staging is private; only the final fenced
 * transaction projects a complete, cardinality-declared manifest to old tables.
 * The existing R26 chunk format and SHA implementation remain the byte contract. */
public final class ModelPublicationR31 {
    private ModelPublicationR31() {}
    public static final int REVISION=1;
    private static final String QUOTA="r31_model_publication_quota";
    private static final String VERSION="R31_MODEL_PUBLICATION_1", PREFIX="R31MP1:";
    static final int PART_BYTES=16384, MAX_ITEMS=4096, MAX_PLAN_BYTES=32<<20;
    private static final String STAGE="r31_model_publication_item", ROOT="r31_model_publication";
    private static final String PART="r31_model_publication_part", CLOCK="r31_model_publication_clock";
    private static final String[] VALUE_COLUMNS=new String[16];
    static {for(int i=0;i<VALUE_COLUMNS.length;i++)VALUE_COLUMNS[i]="c"+i;}
    private static String valuesColumns(){return String.join(",",VALUE_COLUMNS);}
    private static String[] ddl(){
        List<String> out=new ArrayList<>();
        out.add("CREATE TABLE IF NOT EXISTS "+ROOT+"(publication_id TEXT PRIMARY KEY,manifest_hash TEXT NOT NULL,plan_reference BLOB NOT NULL,kind TEXT NOT NULL,source_hash TEXT NOT NULL,source_generation INTEGER NOT NULL,old_evolution_version INTEGER NOT NULL,expected_items INTEGER NOT NULL CHECK(expected_items BETWEEN 1 AND 4096),staged_items INTEGER NOT NULL DEFAULT 0,status TEXT NOT NULL CHECK(status IN ('PREPARING','PUBLISHED')),prepared_at INTEGER NOT NULL,published_at INTEGER NOT NULL DEFAULT 0)");
        out.add("CREATE TABLE IF NOT EXISTS "+STAGE+"(publication_id TEXT NOT NULL,ordinal INTEGER NOT NULL CHECK(ordinal>=0),kind TEXT NOT NULL,item_key TEXT NOT NULL,item_hash TEXT NOT NULL,"+String.join(",",VALUE_COLUMNS)+",PRIMARY KEY(publication_id,ordinal),UNIQUE(publication_id,kind,item_key)) WITHOUT ROWID");
        out.add("CREATE INDEX IF NOT EXISTS r31_model_publication_kind ON "+STAGE+"(publication_id,kind,ordinal)");
        out.add("CREATE TABLE IF NOT EXISTS "+PART+"(content_hash TEXT NOT NULL CHECK(length(content_hash)=64),part INTEGER NOT NULL CHECK(part>=0),part_count INTEGER NOT NULL CHECK(part_count BETWEEN 1 AND 2048),byte_count INTEGER NOT NULL CHECK(byte_count BETWEEN 1 AND 33554432),bytes BLOB NOT NULL CHECK(length(bytes) BETWEEN 1 AND 16384),PRIMARY KEY(content_hash,part)) WITHOUT ROWID");
        out.add("CREATE TABLE IF NOT EXISTS "+CLOCK+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),evolution_version INTEGER NOT NULL DEFAULT 0)");
        out.add("CREATE TABLE IF NOT EXISTS "+QUOTA+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),logical_bytes INTEGER NOT NULL CHECK(logical_bytes BETWEEN 0 AND 268435456),part_rows INTEGER NOT NULL CHECK(part_rows BETWEEN 0 AND 32768),item_rows INTEGER NOT NULL CHECK(item_rows BETWEEN 0 AND 65536),root_rows INTEGER NOT NULL CHECK(root_rows BETWEEN 0 AND 1024))");
        for(String table:new String[]{STAGE,PART}){
            String same=table.equals(PART)?"content_hash=NEW.content_hash AND part=NEW.part":"publication_id=NEW.publication_id AND ordinal=NEW.ordinal";
            String equal=table.equals(PART)?"part_count IS NEW.part_count AND byte_count IS NEW.byte_count AND bytes IS NEW.bytes":"kind IS NEW.kind AND item_key IS NEW.item_key AND item_hash IS NEW.item_hash";
            if(table.equals(STAGE))for(String column:VALUE_COLUMNS)equal+=" AND "+column+" IS NEW."+column;
            out.add("CREATE TRIGGER IF NOT EXISTS "+table+"_insert BEFORE INSERT ON "+table+" WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+same+") BEGIN SELECT CASE WHEN EXISTS(SELECT 1 FROM "+table+" WHERE "+same+" AND "+equal+") THEN RAISE(IGNORE) ELSE RAISE(ABORT,'R31_MODEL_STAGING_CONFLICT') END; END");
            for(String action:new String[]{"UPDATE","DELETE"})out.add("CREATE TRIGGER IF NOT EXISTS "+table+"_"+action.toLowerCase(Locale.ROOT)+" BEFORE "+action+" ON "+table+" BEGIN SELECT RAISE(ABORT,'R31_MODEL_STAGING_IMMUTABLE'); END");
        }
        out.add("CREATE TRIGGER IF NOT EXISTS r31_model_publication_insert BEFORE INSERT ON "+ROOT+" WHEN EXISTS(SELECT 1 FROM "+ROOT+" WHERE publication_id=NEW.publication_id) BEGIN SELECT CASE WHEN EXISTS(SELECT 1 FROM "+ROOT+" WHERE publication_id=NEW.publication_id AND manifest_hash IS NEW.manifest_hash AND plan_reference IS NEW.plan_reference AND kind IS NEW.kind AND source_hash IS NEW.source_hash AND source_generation IS NEW.source_generation AND old_evolution_version IS NEW.old_evolution_version AND expected_items IS NEW.expected_items AND prepared_at IS NEW.prepared_at) THEN RAISE(IGNORE) ELSE RAISE(ABORT,'R31_MODEL_PUBLICATION_CONFLICT') END; END");
        out.add("CREATE TRIGGER IF NOT EXISTS r31_model_publication_delete BEFORE DELETE ON "+ROOT+" BEGIN SELECT RAISE(ABORT,'R31_MODEL_PUBLICATION_IMMUTABLE'); END");
        out.add("CREATE TRIGGER IF NOT EXISTS r31_model_publication_header BEFORE UPDATE ON "+ROOT+" WHEN NEW.publication_id IS NOT OLD.publication_id OR NEW.manifest_hash IS NOT OLD.manifest_hash OR NEW.plan_reference IS NOT OLD.plan_reference OR NEW.kind IS NOT OLD.kind OR NEW.source_hash IS NOT OLD.source_hash OR NEW.source_generation IS NOT OLD.source_generation OR NEW.old_evolution_version IS NOT OLD.old_evolution_version OR NEW.expected_items IS NOT OLD.expected_items OR NEW.prepared_at IS NOT OLD.prepared_at OR NEW.staged_items<OLD.staged_items OR OLD.status='PUBLISHED' BEGIN SELECT RAISE(ABORT,'R31_MODEL_PUBLICATION_HEADER_IMMUTABLE'); END");
        for(String action:new String[]{"INSERT","UPDATE","DELETE"})out.add("CREATE TRIGGER IF NOT EXISTS r31_model_evolution_"+action.toLowerCase(Locale.ROOT)+" AFTER "+action+" ON su2_evolution_state BEGIN UPDATE "+CLOCK+" SET evolution_version=evolution_version+1 WHERE singleton=1; END");
        for(String table:new String[]{PART,STAGE,ROOT}){
            String cost=quotaCost(table,"NEW."),counter=table.equals(PART)?"part_rows":table.equals(STAGE)?"item_rows":"root_rows",limit=table.equals(PART)?"32768":table.equals(STAGE)?"65536":"1024";
            String key=table.equals(PART)?"content_hash=NEW.content_hash AND part=NEW.part":table.equals(STAGE)?"publication_id=NEW.publication_id AND ordinal=NEW.ordinal":"publication_id=NEW.publication_id";
            out.add("CREATE TRIGGER IF NOT EXISTS "+table+"_admit BEFORE INSERT ON "+table+" WHEN NOT EXISTS(SELECT 1 FROM "+table+" WHERE "+key+") BEGIN SELECT CASE WHEN NOT EXISTS(SELECT 1 FROM "+QUOTA+" WHERE singleton=1 AND logical_bytes+"+cost+"<=268435456 AND "+counter+"<"+limit+") THEN RAISE(ABORT,'R31_MODEL_PUBLICATION_PROTECTED_STORAGE_DEBT') END; END");
            out.add("CREATE TRIGGER IF NOT EXISTS "+table+"_account AFTER INSERT ON "+table+" BEGIN UPDATE "+QUOTA+" SET logical_bytes=logical_bytes+"+cost+","+counter+"="+counter+"+1 WHERE singleton=1; END");
        }
        return out.toArray(new String[0]);
    }
    private static String quotaCost(String table,String prefix){
        if(table.equals(PART))return "(192+length("+prefix+"bytes))";
        if(table.equals(ROOT))return "(1024+length(CAST("+prefix+"publication_id AS BLOB))+length(CAST("+prefix+"source_hash AS BLOB)))";
        StringBuilder cost=new StringBuilder("(1024+length(CAST(").append(prefix).append("item_key AS BLOB))");for(String column:VALUE_COLUMNS)cost.append("+coalesce(length(CAST(").append(prefix).append(column).append(" AS BLOB)),0)");return cost.append(')').toString();
    }
    public static String contractHashR31(){return CheckpointArtifactsR26.sha(String.join("\n",ddl()).getBytes(StandardCharsets.UTF_8));}
    /** Startup migration only. No business caller installs or repairs schema. */
    public static void ensureSchemaR31(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R31_MODEL_PUBLICATION_MIGRATION_OWNER_REQUIRED");
        DatabaseCoordinator.requireSchemaObjectsR31(db,VERSION,new String[][]{{"su2_evolution_state","singleton,state_blob"}},new String[0][]);
        for(String sql:ddl())db.execSQL(sql);
        db.execSQL("INSERT OR IGNORE INTO "+CLOCK+"(singleton) VALUES(1)");
        db.execSQL("INSERT OR IGNORE INTO "+QUOTA+" SELECT 1,(SELECT coalesce(sum("+quotaCost(PART,"")+"),0) FROM "+PART+")+(SELECT coalesce(sum("+quotaCost(STAGE,"")+"),0) FROM "+STAGE+")+(SELECT coalesce(sum("+quotaCost(ROOT,"")+"),0) FROM "+ROOT+"),(SELECT count(*) FROM "+PART+"),(SELECT count(*) FROM "+STAGE+"),(SELECT count(*) FROM "+ROOT+")");requireSchemaR31(db);verifyQuotaTotalsR31(db);
    }
    private static void verifyQuotaTotalsR31(SQLiteDatabase db){
        String actual="(SELECT coalesce(sum("+quotaCost(PART,"")+"),0) FROM "+PART+")+(SELECT coalesce(sum("+quotaCost(STAGE,"")+"),0) FROM "+STAGE+")+(SELECT coalesce(sum("+quotaCost(ROOT,"")+"),0) FROM "+ROOT+")";
        try(Cursor c=db.rawQuery("SELECT logical_bytes="+actual+" AND part_rows=(SELECT count(*) FROM "+PART+") AND item_rows=(SELECT count(*) FROM "+STAGE+") AND root_rows=(SELECT count(*) FROM "+ROOT+") FROM "+QUOTA+" WHERE singleton=1",null)){if(!c.moveToFirst()||c.getInt(0)!=1)throw new IllegalStateException("R31_MODEL_PUBLICATION_QUOTA_CORRUPT");}
    }
    public static String[][] schemaObjectsR31(){
        List<String[]> out=new ArrayList<>();for(String sql:ddl()){String[] s=sql.split(" ");out.add(new String[]{s[1].toLowerCase(Locale.ROOT),s[5].split("\\(",2)[0]});}return out.toArray(new String[0][]);
    }
    public static void requireSchemaR31(SQLiteDatabase db){
        DatabaseCoordinator.requireSchemaObjectsR31(db,VERSION,new String[][]{{ROOT,"publication_id,manifest_hash,plan_reference,kind,source_hash,source_generation,old_evolution_version,expected_items,staged_items,status,prepared_at,published_at"},{STAGE,"publication_id,ordinal,kind,item_key,item_hash,"+valuesColumns()},{PART,"content_hash,part,part_count,byte_count,bytes"},{CLOCK,"singleton,evolution_version"},{QUOTA,"singleton,logical_bytes,part_rows,item_rows,root_rows"}},schemaObjectsR31());
        // Object names alone cannot establish immutability. Compare the exact owner definitions.
        for(String sql:ddl()){String[] s=sql.split(" ");String name=s[5].split("\\(",2)[0];try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type=? AND name=?",new String[]{s[1].toLowerCase(Locale.ROOT),name})){if(!c.moveToFirst()||!normalize(sql).equals(normalize(c.getString(0))))throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"EXACT_OBJECT_CONTRACT:"+name);}}
    }
    private static String normalize(String sql){return sql==null?"":sql.replace("IF NOT EXISTS ","").trim();}
    public static long evolutionVersion(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT evolution_version FROM "+CLOCK+" WHERE singleton=1",null)){if(!c.moveToFirst())throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"EVOLUTION_CLOCK_MISSING");return c.getLong(0);}}

    private static Set<String> itemKindsR31(String kind){
        String allowed;switch(kind){
            case "LEGACY":allowed="GENOME IDENTITY LINEAGE EVOLUTION LEAGUE STAGE MATRIX LEGACY_META";break;
            case "FOUNDRY_BIRTH":allowed="FOUNDRY_ROOT CANDIDATE ATTEMPT IDENTITY FAILURE_CURSOR";break;
            case "FOUNDRY_FINISH":allowed="FOUNDRY_FINISH ACCEPTED GENOME IDENTITY LINEAGE";break;
            case "WARM_FORK":allowed="WARM_META WARM_CANDIDATE WARM_INITIALIZER WARM_ARTIFACT WARM_ATTEMPT WARM_PARENT WARM_PROGRESS FOUNDRY_ROOT IDENTITY";break;
            default:throw new IllegalArgumentException("R31_MODEL_PUBLICATION_KIND_UNSUPPORTED:"+kind);
        }return new HashSet<>(Arrays.asList(allowed.split(" ")));
    }
    private static void projectClosedR31(SQLiteDatabase db,Plan plan)throws Exception{
        switch(plan.kind){
            case "LEGACY":LegacyGenerationR26.publishPreparedR31(db,plan);return;
            case "FOUNDRY_BIRTH":FoundryCampaignR26.publishBirthPreparedR31(db,plan);return;
            case "FOUNDRY_FINISH":FoundryCampaignR26.publishFinishPreparedR31(db,plan);return;
            case "WARM_FORK":WarmPublicationR31.publishR31(db,plan);return;
            default:throw new IllegalArgumentException("R31_MODEL_PUBLICATION_KIND_UNSUPPORTED:"+plan.kind);
        }
    }
    static final class Item {
        final String kind,key,hash;final Object[] values;
        Item(String kind,String key,Object[] values){this.kind=kind;this.key=key;this.values=copy(values);hash=CheckpointArtifactsR26.sha(encodeValues(kind,key,this.values));}
    }
    /** Mutable only on the original owner worker, then serialized and frozen before submission. */
    public static final class Plan implements AutoCloseable {
        public final String id,kind,source;public final long sourceGeneration,oldEvolutionVersion,now;
        final LinkedHashMap<String,Item> items=new LinkedHashMap<>();final LinkedHashMap<String,byte[]> contents=new LinkedHashMap<>();
        private boolean frozen,closed;String manifestHash;long schemaVersion;private final MemoryBudgetR31.Lease memory;
        public Plan(String id,String kind,String source,long sourceGeneration,long oldEvolutionVersion,long now){if(id==null||id.length()>160||kind==null||source==null)throw new IllegalArgumentException("R31_MODEL_PLAN_IDENTITY");itemKindsR31(kind);this.id=id;this.kind=kind;this.source=source;this.sourceGeneration=sourceGeneration;this.oldEvolutionVersion=oldEvolutionVersion;this.now=now;MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("model-publication:"+id,MemoryBudgetR31.Kind.CHECKPOINT,1L<<20,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);memory=admission.lease;}
        public void put(String kind,String key,Object...values){if(frozen||values.length>16)throw new IllegalStateException("R31_MODEL_PLAN_FROZEN_OR_SHAPE");if(!itemKindsR31(this.kind).contains(kind))throw new IllegalArgumentException("R31_MODEL_ITEM_KIND_UNSUPPORTED:"+this.kind+":"+kind);reserve(512+3*bound(values));items.put(kind+"|"+key,new Item(kind,key,values));if(items.size()>MAX_ITEMS)throw new PublicationDependencyR31("PLAN_ITEM_RESOURCE_BOUND");}
        Item item(String kind,String key){return items.get(kind+"|"+key);}
        public int count(String kind){int n=0;for(Item item:items.values())if(item.kind.equals(kind))n++;return n;}
        public byte[] payload(byte[] bytes){
            if(frozen||bytes==null||bytes.length<1||bytes.length>MAX_PLAN_BYTES)throw new IllegalStateException("R31_MODEL_PAYLOAD_BOUND");
            reserve((1L<<20)+4L*bytes.length);try{byte[] manifest=ChunkedCheckpointR26.write(out->out.write(bytes),new ChunkedCheckpointR26.Store(){public String put(byte[] part){return content(part);}public InputStream open(String hash)throws IOException{throw new IOException("R31_MODEL_PREPARE_WRITE_ONLY");}});return reference(content(manifest));}
            catch(IOException invalid){throw new IllegalStateException("R31_MODEL_PREPARE_BYTES",invalid);}
        }
        /** Raw, finite snapshot assembly is needed by old canonical snapshot consumers. */
        public String rawPayload(byte[] bytes){if(frozen||bytes==null||bytes.length<1||bytes.length>524288)throw new IllegalStateException("R31_MODEL_SNAPSHOT_BOUND");reserve(3L*bytes.length);return content(bytes);}
        void reserve(long bytes){MemoryBudgetR31.Admission admission=memory.grow(Math.max(1,bytes));if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);}
        @Override public void close(){if(closed)return;closed=true;items.clear();contents.clear();memory.close();}
        private String content(byte[] bytes){String hash=CheckpointArtifactsR26.sha(bytes);contents.putIfAbsent(hash,bytes.clone());return hash;}
    }
    private static Object[] copy(Object[] values){Object[] out=values.clone();for(int i=0;i<out.length;i++){Object v=out[i];if(v instanceof byte[])out[i]=((byte[])v).clone();else if(v!=null&&!(v instanceof String||v instanceof Number))throw new IllegalArgumentException("R31_MODEL_MUTABLE_VALUE");}return out;}
    private static byte[] reference(String hash){return (PREFIX+hash).getBytes(StandardCharsets.US_ASCII);}
    public static boolean isReference(byte[] value){return value!=null&&value.length==PREFIX.length()+64&&new String(value,0,PREFIX.length(),StandardCharsets.US_ASCII).equals(PREFIX);}
    private static String hashReference(byte[] value)throws IOException{String hash=new String(value,PREFIX.length(),64,StandardCharsets.US_ASCII);if(!hash.matches("[0-9a-f]{64}"))throw new IOException("R31_MODEL_REFERENCE_HASH");return hash;}
    private static byte[] content(SQLiteDatabase db,String hash)throws IOException {
        ByteArrayOutputStream out=new ByteArrayOutputStream();int part=0,total=-1,length=-1;
        try(Cursor c=db.rawQuery("SELECT part,part_count,byte_count,bytes FROM "+PART+" WHERE content_hash=? ORDER BY part",new String[]{hash})){while(c.moveToNext()){if(c.getInt(0)!=part++||total!=-1&&total!=c.getInt(1)||length!=-1&&length!=c.getInt(2))throw new IOException("R31_MODEL_PART_SHAPE");total=c.getInt(1);length=c.getInt(2);byte[] bytes=c.getBlob(3);if(bytes.length>PART_BYTES||out.size()+bytes.length>MAX_PLAN_BYTES)throw new IOException("R31_MODEL_PART_BOUND");out.write(bytes);}}
        byte[] bytes=out.toByteArray();if(part!=total||bytes.length!=length||!CheckpointArtifactsR26.sha(bytes).equals(hash))throw new IOException("R31_MODEL_CONTENT_MISSING_OR_CORRUPT");return bytes;
    }
    /** Inline old bytes are unchanged; a new reference validates every original R26 chunk. */
    public static byte[] resolve(SQLiteDatabase db,byte[] value)throws IOException {
        if(!isReference(value))return value;byte[] manifest=content(db,hashReference(value));
        try(InputStream in=ChunkedCheckpointR26.open(manifest,new ChunkedCheckpointR26.Store(){public String put(byte[] bytes)throws IOException{throw new IOException("R31_MODEL_READ_ONLY");}public InputStream open(String hash)throws IOException{return new ByteArrayInputStream(content(db,hash));}});ByteArrayOutputStream out=new ByteArrayOutputStream()){byte[] b=new byte[16384];int n;while((n=in.read(b))!=-1){if(out.size()+n>MAX_PLAN_BYTES)throw new IOException("R31_MODEL_RESOLVE_BOUND");out.write(b,0,n);}return out.toByteArray();}
    }
    public static String resolveText(SQLiteDatabase db,String text)throws IOException{return text!=null&&text.startsWith(PREFIX)?new String(resolve(db,text.getBytes(StandardCharsets.US_ASCII)),StandardCharsets.UTF_8):text;}
    public static String rawExpression(String hash){if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R31_MODEL_RAW_HASH");StringJoiner parts=new StringJoiner("||","CAST("," AS BLOB)");for(int i=0;i<32;i++)parts.add("coalesce((SELECT bytes FROM "+PART+" WHERE content_hash='"+hash+"' AND part="+i+"),X'')");return parts.toString();}

    private static void value(DataOutputStream out,Object value)throws IOException{if(value==null)out.writeByte(0);else if(value instanceof byte[]){out.writeByte(1);byte[] bytes=(byte[])value;out.writeInt(bytes.length);out.write(bytes);}else if(value instanceof Double||value instanceof Float){out.writeByte(2);out.writeDouble(((Number)value).doubleValue());}else if(value instanceof Number){out.writeByte(3);out.writeLong(((Number)value).longValue());}else{out.writeByte(4);byte[] bytes=((String)value).getBytes(StandardCharsets.UTF_8);out.writeInt(bytes.length);out.write(bytes);}}
    private static Object readValue(DataInputStream in)throws IOException{int type=in.readUnsignedByte();if(type==0)return null;if(type==2)return in.readDouble();if(type==3)return in.readLong();if(type!=1&&type!=4)throw new IOException("R31_MODEL_VALUE_TYPE");int n=in.readInt();if(n<0||n>MAX_PLAN_BYTES)throw new IOException("R31_MODEL_VALUE_BOUND");byte[] bytes=new byte[n];in.readFully(bytes);return type==1?bytes:new String(bytes,StandardCharsets.UTF_8);}
    private static byte[] encodeValues(String kind,String key,Object[] values){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeUTF(kind);out.writeUTF(key);out.writeInt(values.length);for(Object value:values)value(out,value);}return bytes.toByteArray();}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    private static byte[] encode(Plan plan){long estimate=4096;for(Item item:plan.items.values())estimate+=1024+2*bound(item.values);plan.reserve(4*estimate);try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeUTF(VERSION);out.writeUTF(plan.id);out.writeUTF(plan.kind);out.writeUTF(plan.source);out.writeLong(plan.sourceGeneration);out.writeLong(plan.oldEvolutionVersion);out.writeLong(plan.now);out.writeInt(plan.items.size());for(Item item:plan.items.values()){byte[] row=encodeValues(item.kind,item.key,item.values);out.writeInt(row.length);out.write(row);}}if(bytes.size()>MAX_PLAN_BYTES)throw new PublicationDependencyR31("PLAN_BYTES_BOUND");return bytes.toByteArray();}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    private static Plan decode(byte[] bytes)throws IOException{try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(!in.readUTF().equals(VERSION))throw new IOException("R31_MODEL_PLAN_VERSION");Plan p=new Plan(in.readUTF(),in.readUTF(),in.readUTF(),in.readLong(),in.readLong(),in.readLong());try{p.reserve(4L*bytes.length);int n=in.readInt();if(n<1||n>MAX_ITEMS)throw new IOException("R31_MODEL_PLAN_COUNT");for(int i=0;i<n;i++){int size=in.readInt();if(size<1||size>MAX_PLAN_BYTES)throw new IOException("R31_MODEL_PLAN_ROW_BOUND");byte[] row=new byte[size];in.readFully(row);try(DataInputStream r=new DataInputStream(new ByteArrayInputStream(row))){String kind=r.readUTF(),key=r.readUTF();int count=r.readInt();if(count<0||count>16)throw new IOException("R31_MODEL_PLAN_COLUMN_BOUND");Object[] values=new Object[count];for(int j=0;j<count;j++)values[j]=readValue(r);p.put(kind,key,values);if(r.read()!=-1)throw new IOException("R31_MODEL_PLAN_ROW_TRAILING");}}if(in.read()!=-1)throw new IOException("R31_MODEL_PLAN_TRAILING");p.frozen=true;p.manifestHash=CheckpointArtifactsR26.sha(bytes);return p;}catch(IOException|RuntimeException|Error failed){p.close();throw failed;}}}
    /** A crashed owner resumes the persisted immutable plan, including its original timestamps. */
    public static Plan existing(SQLiteDatabase db,String id)throws IOException{try(Cursor c=db.rawQuery("SELECT manifest_hash,plan_reference FROM "+ROOT+" WHERE publication_id=?",new String[]{id})){if(!c.moveToFirst())return null;String hash=hashReference(c.getBlob(1));long length=scalar(db,"SELECT byte_count FROM "+PART+" WHERE content_hash=? AND part=0",hash);if(length<1||length>MAX_PLAN_BYTES)throw new IOException("R31_MODEL_PLAN_CONTENT_BOUND");try(MemoryBudgetR31.Lease workspace=workspaceR31("plan-restore",(1L<<20)+6*length)){byte[] bytes=content(db,hash);if(!CheckpointArtifactsR26.sha(bytes).equals(c.getString(0)))throw new IOException("R31_MODEL_PLAN_HASH");return decode(bytes);}}}

    static String select(Plan p,String kind,String columns){return "SELECT "+columns+" FROM "+STAGE+" WHERE publication_id='"+p.id.replace("'","''")+"' AND kind='"+kind+"' ORDER BY ordinal LIMIT "+p.count(kind);}
    private static long scalar(SQLiteDatabase db,String sql,String...args){try(Cursor c=db.rawQuery(sql,args)){if(!c.moveToFirst())throw new IllegalStateException("R31_MODEL_SCALAR_MISSING");return c.getLong(0);}}
    private static void changed(SQLiteDatabase db){if(scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_MODEL_PUBLICATION_CAS");}
    /** Source compatibility only. The callback parameter of the old overload is never executed. */
    @Deprecated public interface Finalizer {void publish(SQLiteDatabase db,Plan plan)throws Exception;}
    private static void submit(DbStore store,RuntimeCoordinatorR26.Claim claim,Plan p,String suffix,long chunk,ResearchWriteQueueR27.Batch batch){
        String operation="R31_MODEL_PUBLICATION_"+suffix;final DatabaseCoordinator.WriteCallable<Integer> prior=batch.before;batch.before=db->{RuntimeCoordinatorR26.requireFence(db,claim);if(scalar(db,"PRAGMA schema_version")!=p.schemaVersion)throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"SCHEMA_CHANGED_AFTER_PREPARATION");requireQuotaR31(db,suffix,batch);return prior==null?0:prior.call(db);};batch.guardAfter(db->{RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
        String key=HashingSU1.fields(VERSION,p.id,p.manifestHash,suffix,Long.toString(chunk));ResearchWriteQueueR27.ImmutableBatch frozen=batch.prepareR31(operation,key,claim.task,claim.owner,claim.fence,p.source,chunk,VERSION);
        if(frozen.overInitialBudget)throw new IllegalStateException("R31_MODEL_PREPARED_BATCH_OVER_BUDGET");
        store.coordinatorForResearchR952().runIdempotentAtomicWriteR31(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,operation),frozen.identityR31,db->{ResearchWriteQueueR27.execute(db,frozen,operation);return 0;},0);
    }
    private static void persistContent(DbStore store,RuntimeCoordinatorR26.Claim claim,Plan p,String hash,byte[] bytes){int count=(bytes.length+PART_BYTES-1)/PART_BYTES;for(int part=0;part<count;part++){ExecutionBudgetR26.poll();checkFreeFloorR31(store);int index=part;byte[] slice=Arrays.copyOfRange(bytes,part*PART_BYTES,Math.min(bytes.length,(part+1)*PART_BYTES));ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch().add("INSERT INTO "+PART+" VALUES(?,?,?,?,?)",hash,index,count,bytes.length,slice);submit(store,claim,p,"CONTENT_"+hash,index,b);}}
    /** Each staged item is immutable and the prefix CAS/receipt commit with it. */
    @Deprecated public static void stageAndPublish(DbStore store,RuntimeCoordinatorR26.Claim claim,Plan requested,Finalizer ignored)throws IOException{stageAndPublish(store,claim,requested);}
    public static void stageAndPublish(DbStore store,RuntimeCoordinatorR26.Claim claim,Plan requested)throws IOException {
        Plan existing=null;try{long schemaVersion=scalar(store.readable(),"PRAGMA schema_version");requireSchemaR31(store.readable());requireCanonicalR31(store.readable(),requested.kind);if(scalar(store.readable(),"PRAGMA schema_version")!=schemaVersion)throw new DatabaseCoordinator.SchemaPendingR31(VERSION,"SCHEMA_CHANGED_DURING_PREPARE");existing=existing(store.readable(),requested.id);Plan p=existing==null?requested:existing;p.schemaVersion=schemaVersion;
        if(existing==null){byte[] bytes=encode(p);p.manifestHash=CheckpointArtifactsR26.sha(bytes);p.frozen=true;for(Map.Entry<String,byte[]> content:p.contents.entrySet())persistContent(store,claim,p,content.getKey(),content.getValue());persistContent(store,claim,p,p.manifestHash,bytes);
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch().add("INSERT OR IGNORE INTO "+ROOT+"(publication_id,manifest_hash,plan_reference,kind,source_hash,source_generation,old_evolution_version,expected_items,prepared_at,status) VALUES(?,?,?,?,?,?,?,?,?,'PREPARING')",p.id,p.manifestHash,reference(p.manifestHash),p.kind,p.source,p.sourceGeneration,p.oldEvolutionVersion,p.items.size(),p.now);
            final Plan opening=p;b.guardAfter(db->{CheckpointGcStoreR31.pinPublicationWithin(db,opening);return 0;});submit(store,claim,p,"OPEN",0,b);
        }
        try(Cursor c=store.readable().rawQuery("SELECT manifest_hash,status FROM "+ROOT+" WHERE publication_id=?",new String[]{p.id})){if(!c.moveToFirst()||!p.manifestHash.equals(c.getString(0)))throw new IllegalStateException("R31_MODEL_PLAN_IDENTITY_CONFLICT");if(c.getString(1).equals("PUBLISHED"))return;}
        if(CheckpointGcStoreR31.ready(store.readable())){final Plan resuming=p;ResearchWriteQueueR27.Batch pins=new ResearchWriteQueueR27.Batch();pins.guardBefore(db->{CheckpointGcStoreR31.pinPublicationWithin(db,resuming);return 0;});submit(store,claim,p,"GC_PIN",0,pins);}
        validatePreparedContentsR31(store.readable(),p);
        int staged=(int)scalar(store.readable(),"SELECT staged_items FROM "+ROOT+" WHERE publication_id=?",p.id);List<Item> rows=new ArrayList<>(p.items.values());
        for(int offset=staged;offset<rows.size();){ExecutionBudgetR26.poll();ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();int before=offset;long bytes=0,cap=Math.min(262144L,Runtime.getRuntime().maxMemory()/512L)-8192;
            do{Item item=rows.get(offset);Object[] args=new Object[21];args[0]=p.id;args[1]=offset;args[2]=item.kind;args[3]=item.key;args[4]=item.hash;System.arraycopy(item.values,0,args,5,item.values.length);long bound=bound(args);if(offset>before&&(offset-before>=8||bytes+bound>cap))break;if(bound>cap)throw new PublicationDependencyR31("ITEM_BIND_RESOURCE_BOUND");batch.add("INSERT INTO "+STAGE+" VALUES("+String.join(",",Collections.nCopies(21,"?"))+")",args);bytes+=bound;offset++;}while(offset<rows.size());
            batch.add("UPDATE "+ROOT+" SET staged_items=? WHERE publication_id=? AND manifest_hash=? AND staged_items=? AND status='PREPARING'",offset,p.id,p.manifestHash,before);batch.guardAfter(db->{changed(db);return 0;});submit(store,claim,p,"STAGE",before,batch);
        }
        final Plan prepared=p;ResearchWriteQueueR27.Batch batch=new ResearchWriteQueueR27.Batch();batch.guardBefore(db->{
            try(Cursor c=db.rawQuery("SELECT status,staged_items,expected_items,manifest_hash FROM "+ROOT+" WHERE publication_id=?",new String[]{prepared.id})){if(!c.moveToFirst()||!c.getString(0).equals("PREPARING")||c.getInt(1)!=prepared.items.size()||c.getInt(2)!=prepared.items.size()||!prepared.manifestHash.equals(c.getString(3)))throw new IllegalStateException("R31_MODEL_INCOMPLETE_MANIFEST");}
            if(EventFrameBuilderSU1.generation(db)!=prepared.sourceGeneration)throw new PublicationDependencyR31("SOURCE_CHANGED");
            if(prepared.oldEvolutionVersion>=0&&evolutionVersion(db)!=prepared.oldEvolutionVersion)throw new PublicationDependencyR31("OLD_EVOLUTION_CHANGED");
            try(Cursor c=db.rawQuery("SELECT kind,count(*) FROM "+STAGE+" WHERE publication_id=? GROUP BY kind",new String[]{prepared.id})){int seen=0;while(c.moveToNext()){if(c.getInt(1)!=prepared.count(c.getString(0)))throw new IllegalStateException("R31_MODEL_TYPED_MANIFEST_COUNT");seen+=c.getInt(1);}if(seen!=prepared.items.size())throw new IllegalStateException("R31_MODEL_MANIFEST_COUNT");}
            projectClosedR31(db,prepared);return 0;});batch.add("UPDATE "+ROOT+" SET status='PUBLISHED',published_at=? WHERE publication_id=? AND manifest_hash=? AND staged_items=expected_items AND status='PREPARING'",p.now,p.id,p.manifestHash);batch.guardAfter(db->{changed(db);CheckpointGcStoreR31.releasePublicationWithin(db,prepared);return 0;});submit(store,claim,p,"PUBLISH",rows.size(),batch);
        }finally{if(existing!=null)existing.close();requested.close();}
    }
    private static void requireQuotaR31(SQLiteDatabase db,String suffix,ResearchWriteQueueR27.Batch batch){
        long bytes=0;int parts=0,items=0,roots=0;if(suffix.startsWith("CONTENT_")){parts=1;bytes=2048;}else if(suffix.equals("STAGE")){items=batch.ops.size()-1;bytes=2048L*items;}else if(suffix.equals("OPEN")){roots=1;bytes=2048;}
        if(bytes>0)for(ResearchWriteQueueR27.Op op:batch.ops)bytes+=bound(op.args);
        try(Cursor c=db.rawQuery("SELECT logical_bytes,part_rows,item_rows,root_rows FROM "+QUOTA+" WHERE singleton=1",null)){if(!c.moveToFirst()||c.getLong(0)>268435456L-bytes||c.getLong(1)>32768-parts||c.getLong(2)>65536-items||c.getLong(3)>1024-roots)throw new PublicationDependencyR31("PROTECTED_LOGICAL_STORAGE_DEBT");}
    }
    private static void validatePreparedContentsR31(SQLiteDatabase db,Plan p)throws IOException{
        try(MemoryBudgetR31.Lease workspace=workspaceR31("prepared-content-validation",24L<<20)){
            for(Item item:p.items.values()){for(Object value:item.values){ExecutionBudgetR26.poll();if(value instanceof byte[]&&isReference((byte[])value))resolve(db,(byte[])value);else if(value instanceof String&&((String)value).startsWith(PREFIX))resolveText(db,(String)value);}
                if(item.kind.equals("FOUNDRY_ROOT")){byte[] bytes=content(db,(String)item.values[3]);if(bytes.length!=((Number)item.values[8]).intValue())throw new IOException("R31_MODEL_SNAPSHOT_LENGTH");FoundryCampaignR26.Snapshot.decode(bytes);}}
        }
    }
    private static void requireCanonicalR31(SQLiteDatabase db,String kind){
        List<String[]> required=new ArrayList<>();Collections.addAll(required,new String[][]{{"su1_source_clock","singleton,generation"},{"r31_commit_receipt","commit_key,payload_hash"},{"r26_policy_identity","policy_hash,canonical_policy,genotype_text,numerical_abi,created_at"},{"su2_model_genome","genome_key,canonical_text,formula,tier,origin_op,parent_key,repair_note,born_campaign,born_sequence,dev_fitness,val_fitness,state,created_at"},{"su2_lineage","child_key,parent_key,op,campaign_id,parent_dev,child_dev,note"}});
        if(kind.equals("LEGACY"))Collections.addAll(required,new String[][]{{"r26_legacy_campaign","campaign_id,generation,status,snapshot_hash,numerical_abi"},{"su2_campaign","campaign_id,evaluated,effective_generations,status,finished_at"},{"r26_work_total","task_id,completed"},{"su2_evolution_state","singleton,campaign_id,state_blob,byte_count,updated_at"},{"su2_league","member_id,kind,name,genome_key,joined_sequence,state"},{"su2_stage_bundle","genome_key,bundle,byte_count,created_at"},{"su2_matrix_snapshot","genome_key,checkpoint_sequence,snapshot_json,factors_blob,reason_state,stats_json,graded_through,updated_at,input_through_ordinal,available_at,consumed_actions,last_consumed_at"}});
        else Collections.addAll(required,new String[][]{{"r26_foundry","campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first"},{"r26_candidate","campaign_id,policy_hash,canonical_text,parent,operation,status"},{"r26_evolution","attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash,research_accepted"},{"r26_failure_research_cursor","parent_policy,evidence_digest,window_end,campaign_id,proposal_json"}});
        DatabaseCoordinator.requireSchemaObjectsR31(db,VERSION,required.toArray(new String[0][]),new String[0][]);if(kind.equals("WARM_FORK"))WarmPublicationR31.requireSchemaR31(db);
    }
    private static void checkFreeFloorR31(DbStore store){String path=store.readable().getPath();if(path==null||path.isEmpty())throw new PublicationDependencyR31("FREE_SPACE_RESERVE_OR_UNKNOWN");java.io.File parent=new java.io.File(path).getAbsoluteFile().getParentFile();long free=0;if(parent!=null)try{free=new android.os.StatFs(parent.getPath()).getAvailableBytes();}catch(IllegalArgumentException|SecurityException unknown){throw new PublicationDependencyR31("FREE_SPACE_RESERVE_OR_UNKNOWN");}if(free<256L*1024*1024)throw new PublicationDependencyR31("FREE_SPACE_RESERVE_OR_UNKNOWN");}
    public static final class PublicationDependencyR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        PublicationDependencyR31(String reason){super("R31_MODEL_PUBLICATION_DEPENDENCY:"+reason);}public String failureCategory(){return "DEPENDENCY";}public String operation(){return VERSION;}public String phase(){return "RESOURCE_ADMISSION";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    static MemoryBudgetR31.Lease workspaceR31(String owner,long bytes){MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("model-publication:"+owner,MemoryBudgetR31.Kind.CHECKPOINT,bytes,()->Thread.currentThread().isInterrupted());if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);return admission.lease;}
    private static long bound(Object[] args){long bytes=0;for(Object a:args)bytes+=a instanceof byte[]?((byte[])a).length:a instanceof String?((String)a).getBytes(StandardCharsets.UTF_8).length:8;return bytes;}
}
