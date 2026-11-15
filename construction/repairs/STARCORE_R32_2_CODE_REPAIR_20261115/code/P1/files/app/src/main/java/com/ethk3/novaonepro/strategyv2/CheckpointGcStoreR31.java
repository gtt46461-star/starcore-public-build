package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.EvidenceReachabilityR31;
import com.ethk3.novaonepro.StorageGcR31;
import java.util.*;

/** Durable metadata for the real checkpoint CAS. Mutations belong to the existing writer;
 * file verification and unlinking never occur in this class or its transactions. */
public final class CheckpointGcStoreR31 implements StorageGcR31.Store {
    public static final int MAX_NODES=16384, PAGE=64;
    private static final String[] CORE_DDL={
        "CREATE TABLE IF NOT EXISTS r31_gc_artifact(artifact_hash TEXT PRIMARY KEY,bytes INTEGER NOT NULL CHECK(bytes>=0),modified_at INTEGER NOT NULL,state TEXT NOT NULL CHECK(state IN ('LIVE','DELETING','DELETED')),version INTEGER NOT NULL CHECK(version>=0),verified_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_gc_edge(from_hash TEXT NOT NULL,to_hash TEXT NOT NULL,PRIMARY KEY(from_hash,to_hash))",
        "CREATE TABLE IF NOT EXISTS r31_gc_pin(pin_id TEXT PRIMARY KEY,artifact_hash TEXT NOT NULL,root_kind TEXT NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_gc_receipt(artifact_hash TEXT PRIMARY KEY,phase TEXT NOT NULL,version INTEGER NOT NULL,bytes INTEGER NOT NULL,retry_count INTEGER NOT NULL DEFAULT 0,released_bytes INTEGER NOT NULL DEFAULT 0,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_gc_status(singleton INTEGER PRIMARY KEY CHECK(singleton=1),report_json TEXT NOT NULL,updated_at INTEGER NOT NULL)",
        "CREATE TABLE IF NOT EXISTS r31_gc_publication(publication_id TEXT PRIMARY KEY,manifest_hash TEXT NOT NULL,reference_count INTEGER NOT NULL CHECK(reference_count BETWEEN 0 AND 64),status TEXT NOT NULL CHECK(status IN ('PINNED','PUBLISHED')),registered_at INTEGER NOT NULL)"
    };
    public static final String[] DDL;
    static {List<String> all=new ArrayList<>(Arrays.asList(CORE_DDL));Collections.addAll(all,CheckpointScratchGcR31.ddlR31());DDL=all.toArray(new String[0]);}
    private final SQLiteDatabase db;
    private final Map<String,EvidenceReachabilityR31.RootKind> currentRoots;
    public CheckpointGcStoreR31(SQLiteDatabase db,Map<String,EvidenceReachabilityR31.RootKind> roots){
        this.db=db;currentRoots=new TreeMap<>(roots);if(currentRoots.size()>MAX_NODES)throw new IllegalStateException("R31_GC_ROOT_BOUND");
    }
    private void writer(){if(!db.inTransaction())throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");}
    private static void hash(String hash){if(hash==null||!hash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R31_GC_ARTIFACT_HASH");}
    private int changes(){try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst())throw new IllegalStateException("R31_GC_CHANGES_UNKNOWN");return c.getInt(0);}}
    public static boolean ready(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r31_gc_artifact','r31_gc_edge','r31_gc_pin','r31_gc_receipt','r31_gc_status','r31_gc_publication')",null)){return c.moveToFirst()&&c.getInt(0)==6&&CheckpointScratchGcR31.ready(db);}
    }
    /** Each statement is installed separately by the migration owner, never by a business caller.
     * The same writer serializes a new reference and a GC claim even for legacy publishers. */
    public static String[] referenceGuards(SQLiteDatabase db){
        List<String> guards=new ArrayList<>();
        Map<String,String[]> roots=new LinkedHashMap<>();
        for(String table:new String[]{"r26_live_model","r26_candidate","r26_warm_initialization","r26_expert_experiment","r26_legacy_runtime","r26_member_replay","r26_prepared_policy"}){roots.put(table,new String[]{"state"});roots.put("r30_keep_"+table,new String[]{"state"});}
        roots.put("r26_replay_output_chunk",new String[]{"artifact_ref"});
        roots.put("r29_model_checkpoint",new String[]{"artifact"});roots.put("r29_legacy_model_checkpoint",new String[]{"artifact"});roots.put("r30_model_artifact",new String[]{"weights"});roots.put("r30_atomnet_dataset",new String[]{"payload"});
        roots.put("r31_atomnet_publication",new String[]{"state_ref","weights_ref","dataset_ref"});
        roots.put("r26_checkpoint_artifact",new String[]{"artifact_hash"});
        String[] stagedColumns=new String[16];for(int i=0;i<16;i++)stagedColumns[i]="c"+i;roots.put("r31_model_publication_item",stagedColumns);
        for(String table:new String[]{"r29_model_checkpoint","r29_legacy_model_checkpoint","r30_model_artifact","r30_atomnet_dataset"})roots.put("r30_keep_"+table,roots.get(table));
        for(Map.Entry<String,String[]> root:roots.entrySet()){
            Set<String> present=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info(\""+root.getKey()+"\")",null)){while(c.moveToNext())present.add(c.getString(1));}
            for(String column:root.getValue())if(present.contains(column))for(String operation:new String[]{"INSERT","UPDATE OF \""+column+"\""}){
                String event=operation.startsWith("UPDATE")?"update":"insert";
                String value="CAST(NEW.\""+column+"\" AS TEXT)",artifact=column.equals("artifact_hash")?value:"substr("+value+",9)";
                String isReference=column.equals("artifact_hash")?"length("+value+")=64":"substr("+value+",1,8)='R26CAS1:'";
                guards.add("CREATE TRIGGER IF NOT EXISTS \"r31_gc_ref_"+root.getKey()+"_"+column+"_"+event+"\" BEFORE "+operation+" ON \""+root.getKey()+"\" WHEN "+isReference+" BEGIN SELECT CASE WHEN EXISTS(SELECT 1 FROM r31_gc_artifact WHERE artifact_hash="+artifact+" AND state<>'LIVE') THEN RAISE(ABORT,'R31_GC_REFERENCE_NOT_LIVE') END; UPDATE r31_gc_artifact SET version=version+1 WHERE artifact_hash="+artifact+" AND state='LIVE'; END");
            }
        }
        return guards.toArray(new String[0]);
    }
    /** Archive owners admit this additional attachment only by its entire
     * declaration; names/prefixes alone never authenticate a protection trigger. */
    public static boolean knownReferenceGuard(SQLiteDatabase db,String table,String type,String name,String sql){
        if(!"trigger".equals(type)||table==null||name==null||sql==null)return false;
        java.util.regex.Pattern header=java.util.regex.Pattern.compile("^CREATE TRIGGER IF NOT EXISTS \"([^\"]+)\" BEFORE (?:INSERT|UPDATE OF \"[^\"]+\") ON \"([^\"]+)\" WHEN ");
        String actual=sql.trim();if(actual.startsWith("CREATE TRIGGER IF NOT EXISTS "))actual="CREATE TRIGGER "+actual.substring("CREATE TRIGGER IF NOT EXISTS ".length());
        for(String expected:referenceGuards(db)){java.util.regex.Matcher match=header.matcher(expected);if(match.find()&&name.equals(match.group(1))&&table.equals(match.group(2))){String normalized="CREATE TRIGGER "+expected.substring("CREATE TRIGGER IF NOT EXISTS ".length());return normalized.equals(actual);}}
        return false;
    }
    public static StorageGcR31.State storedState(SQLiteDatabase db,String artifact){
        hash(artifact);try(Cursor c=db.rawQuery("SELECT state FROM r31_gc_artifact WHERE artifact_hash=?",new String[]{artifact})){return c.moveToFirst()?StorageGcR31.State.valueOf(c.getString(0)):null;}
    }
    public StorageGcR31.State state(String artifact){return storedState(db,artifact);}
    public long version(String artifact){hash(artifact);try(Cursor c=db.rawQuery("SELECT version FROM r31_gc_artifact WHERE artifact_hash=?",new String[]{artifact})){if(!c.moveToFirst())throw new IllegalStateException("R31_GC_ARTIFACT_UNREGISTERED");return c.getLong(0);}}
    public long bytes(String artifact){hash(artifact);try(Cursor c=db.rawQuery("SELECT bytes FROM r31_gc_artifact WHERE artifact_hash=?",new String[]{artifact})){if(!c.moveToFirst())throw new IllegalStateException("R31_GC_ARTIFACT_UNREGISTERED");return c.getLong(0);}}
    /** The supplied bytes/edges came from an actual hash-verified immutable file outside the writer.
     * A DELETED content identity may be published again only after its new file was verified. */
    public void registerVerified(String artifact,long bytes,long modified,Set<String> children,long at){
        writer();hash(artifact);if(bytes<1||bytes>CheckpointArtifactsR26.MAX_BYTES||children.size()>MAX_NODES)throw new IllegalArgumentException("R31_GC_VERIFIED_FILE_BOUND");
        StorageGcR31.State prior=state(artifact);if(prior==StorageGcR31.State.DELETING)throw new IllegalStateException("R31_GC_PREPARED_ARTIFACT_DELETING");
        if(prior==null)db.execSQL("INSERT INTO r31_gc_artifact VALUES(?,?,?,'LIVE',0,?)",new Object[]{artifact,bytes,modified,at});
        else if(prior==StorageGcR31.State.DELETED){db.execSQL("UPDATE r31_gc_artifact SET state='LIVE',version=version+1,bytes=?,modified_at=?,verified_at=? WHERE artifact_hash=? AND state='DELETED'",new Object[]{bytes,modified,at,artifact});if(changes()!=1)throw new IllegalStateException("R31_GC_REVIVE_CAS");}
        else if(bytes(artifact)!=bytes)throw new IllegalStateException("R31_GC_VERIFIED_SIZE_CHANGED");
        else db.execSQL("UPDATE r31_gc_artifact SET modified_at=?,verified_at=? WHERE artifact_hash=? AND state='LIVE'",new Object[]{modified,at,artifact});
        Set<String> old=edgesFrom(artifact);if(prior==StorageGcR31.State.LIVE&&!old.equals(children))throw new IllegalStateException("R31_GC_IMMUTABLE_EDGES_CHANGED");
        for(String child:children){hash(child);db.execSQL("INSERT OR IGNORE INTO r31_gc_edge VALUES(?,?)",new Object[]{artifact,child});}
    }
    public boolean compareAndSet(String artifact,StorageGcR31.State expected,long version,StorageGcR31.State next){
        writer();hash(artifact);db.execSQL("UPDATE r31_gc_artifact SET state=?,version=version+1 WHERE artifact_hash=? AND state=? AND version=?",new Object[]{next.name(),artifact,expected.name(),version});return changes()==1;
    }
    public boolean addEdgeIfLive(String from,String to){
        writer();hash(to);if(from==null||from.isEmpty()||from.length()>256)throw new IllegalArgumentException("R31_GC_EDGE_OWNER");
        hash(from);if(state(from)!=StorageGcR31.State.LIVE)return false;
        db.execSQL("UPDATE r31_gc_artifact SET version=version+1 WHERE artifact_hash=? AND state='LIVE'",new Object[]{to});if(changes()!=1)return false;
        db.execSQL("INSERT OR IGNORE INTO r31_gc_edge VALUES(?,?)",new Object[]{from,to});return true;
    }
    public void admitReference(String artifact){writer();hash(artifact);db.execSQL("UPDATE r31_gc_artifact SET version=version+1 WHERE artifact_hash=? AND state='LIVE'",new Object[]{artifact});if(changes()!=1)throw new IllegalStateException("R31_GC_REFERENCE_NOT_LIVE");}
    public boolean pin(String id,String artifact,EvidenceReachabilityR31.RootKind kind){
        writer();hash(artifact);try(Cursor c=db.rawQuery("SELECT artifact_hash,root_kind FROM r31_gc_pin WHERE pin_id=?",new String[]{id})){if(c.moveToFirst()){if(!artifact.equals(c.getString(0))||!kind.name().equals(c.getString(1)))throw new IllegalStateException("R31_GC_PIN_IDENTITY");return state(artifact)==StorageGcR31.State.LIVE;}}
        db.execSQL("UPDATE r31_gc_artifact SET version=version+1 WHERE artifact_hash=? AND state='LIVE'",new Object[]{artifact});if(changes()!=1)return false;
        db.execSQL("INSERT INTO r31_gc_pin VALUES(?,?,?)",new Object[]{id,artifact,kind.name()});return true;
    }
    public void releasePin(String id,String artifact){writer();hash(artifact);db.execSQL("DELETE FROM r31_gc_pin WHERE pin_id=? AND artifact_hash=?",new Object[]{id,artifact});}
    private static Set<String> publicationReferences(ModelPublicationR31.Plan plan){
        Set<String> references=new TreeSet<>();for(ModelPublicationR31.Item item:plan.items.values())for(Object value:item.values){if(value instanceof byte[]&&CheckpointArtifactsR26.reference((byte[])value))references.add(CheckpointArtifactsR26.referenceHash((byte[])value));if(references.size()>64)throw new ModelPublicationR31.PublicationDependencyR31("GC_PUBLICATION_PIN_SLICE_BOUND");}return references;
    }
    private static String publicationPin(String id,String hash){return "publication:"+id+":"+hash;}
    /** The complete immutable plan, including items not staged yet, is pinned
     * with OPEN (or before recovery resumes). A partial staged prefix is insufficient. */
    public static void pinPublicationWithin(SQLiteDatabase db,ModelPublicationR31.Plan plan){
        if(!ready(db))return;if(!db.inTransaction())throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");
        try(Cursor c=db.rawQuery("SELECT manifest_hash,status FROM r31_model_publication WHERE publication_id=?",new String[]{plan.id})){if(!c.moveToFirst()||!Objects.equals(c.getString(0),plan.manifestHash)||!"PREPARING".equals(c.getString(1)))throw new IllegalStateException("R31_GC_PUBLICATION_IDENTITY");}
        Set<String> references=publicationReferences(plan);CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());
        for(String artifact:references)if(!store.pin(publicationPin(plan.id,artifact),artifact,EvidenceReachabilityR31.RootKind.OPEN_TASK))throw new ModelPublicationR31.PublicationDependencyR31("GC_PUBLICATION_ARTIFACT_NOT_LIVE");
        try(Cursor c=db.rawQuery("SELECT manifest_hash,reference_count,status FROM r31_gc_publication WHERE publication_id=?",new String[]{plan.id})){if(c.moveToFirst()&&(!Objects.equals(c.getString(0),plan.manifestHash)||c.getInt(1)!=references.size()||!"PINNED".equals(c.getString(2))))throw new IllegalStateException("R31_GC_PUBLICATION_COVERAGE_CHANGED");}
        db.execSQL("INSERT OR IGNORE INTO r31_gc_publication VALUES(?,?,?,'PINNED',?)",new Object[]{plan.id,plan.manifestHash,references.size(),System.currentTimeMillis()});
    }
    public static void releasePublicationWithin(SQLiteDatabase db,ModelPublicationR31.Plan plan){
        if(!ready(db))return;if(!db.inTransaction())throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");
        try(Cursor c=db.rawQuery("SELECT manifest_hash,status FROM r31_model_publication WHERE publication_id=?",new String[]{plan.id})){if(!c.moveToFirst()||!Objects.equals(c.getString(0),plan.manifestHash)||!"PUBLISHED".equals(c.getString(1)))throw new IllegalStateException("R31_GC_PUBLICATION_NOT_COMMITTED");}
        CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());for(String hash:publicationReferences(plan))store.releasePin(publicationPin(plan.id,hash),hash);
        db.execSQL("UPDATE r31_gc_publication SET status='PUBLISHED' WHERE publication_id=? AND manifest_hash=?",new Object[]{plan.id,plan.manifestHash});
    }
    public static void requirePublicationCoverage(SQLiteDatabase db){
        try(Cursor present=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name='r31_model_publication'",null)){if(!present.moveToFirst())return;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_model_publication p LEFT JOIN r31_gc_publication g ON g.publication_id=p.publication_id WHERE p.status='PREPARING' AND (g.publication_id IS NULL OR g.manifest_hash IS NOT p.manifest_hash OR g.status<>'PINNED' OR g.reference_count<>(SELECT count(*) FROM r31_gc_pin pin WHERE substr(pin.pin_id,1,length('publication:'||p.publication_id||':'))='publication:'||p.publication_id||':' AND pin.root_kind='OPEN_TASK')) LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("DEPENDENCY:R31_GC_PENDING_PUBLICATION_PIN_COVERAGE");}
    }
    public void detach(String artifact){writer();hash(artifact);if(state(artifact)!=StorageGcR31.State.DELETING)throw new IllegalStateException("R31_GC_DETACH_WITHOUT_CLAIM");db.execSQL("DELETE FROM r31_gc_edge WHERE from_hash=? OR to_hash=?",new Object[]{artifact,artifact});}
    public void receipt(String artifact,String phase){
        writer();hash(artifact);if(!Arrays.asList("CLAIMED_DELETING","TOMBSTONE_RETRY","UNLINKED").contains(phase))throw new IllegalArgumentException("R31_GC_RECEIPT_PHASE");
        long version=version(artifact),size=bytes(artifact);db.execSQL("INSERT OR IGNORE INTO r31_gc_receipt(artifact_hash,phase,version,bytes,updated_at) VALUES(?,?,?,?,?)",new Object[]{artifact,phase,version,size,System.currentTimeMillis()});
        db.execSQL("UPDATE r31_gc_receipt SET phase=?,version=?,bytes=?,retry_count=retry_count+?,released_bytes=CASE WHEN ?='UNLINKED' AND version<>? THEN released_bytes+? ELSE released_bytes END,updated_at=? WHERE artifact_hash=?",new Object[]{phase,version,size,phase.equals("TOMBSTONE_RETRY")?1:0,phase,version,size,System.currentTimeMillis(),artifact});
    }
    public Map<String,EvidenceReachabilityR31.RootKind> roots(){
        Map<String,EvidenceReachabilityR31.RootKind> roots=new TreeMap<>(currentRoots);
        try(Cursor c=db.rawQuery("SELECT artifact_hash,root_kind FROM r31_gc_pin LIMIT "+(MAX_NODES+1),null)){while(c.moveToNext()){hash(c.getString(0));roots.put(c.getString(0),EvidenceReachabilityR31.RootKind.valueOf(c.getString(1)));if(roots.size()>MAX_NODES)throw new IllegalStateException("R31_GC_ROOT_BOUND");}}
        for(String artifact:roots.keySet())if(state(artifact)!=StorageGcR31.State.LIVE)throw new IllegalStateException("R31_GC_ROOT_NOT_LIVE_OR_UNREGISTERED");
        return roots;
    }
    public Set<String> artifacts(){Set<String> values=new TreeSet<>();try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r31_gc_artifact WHERE state<>'DELETED' LIMIT "+(MAX_NODES+1),null)){while(c.moveToNext()){values.add(c.getString(0));if(values.size()>MAX_NODES)throw new IllegalStateException("R31_GC_ARTIFACT_BOUND");}}return values;}
    public Set<String> edgesFrom(String artifact){
        hash(artifact);if(state(artifact)!=StorageGcR31.State.LIVE)throw new IllegalStateException("R31_GC_ROOT_OR_CHILD_NOT_LIVE");
        Set<String> values=new TreeSet<>();try(Cursor c=db.rawQuery("SELECT to_hash FROM r31_gc_edge WHERE from_hash=? LIMIT "+(MAX_NODES+1),new String[]{artifact})){while(c.moveToNext()){values.add(c.getString(0));if(values.size()>MAX_NODES)throw new IllegalStateException("R31_GC_EDGE_BOUND");}}return values;
    }
    public static List<String> deleting(SQLiteDatabase db){List<String> result=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r31_gc_artifact WHERE state='DELETING' ORDER BY artifact_hash LIMIT "+PAGE,null)){while(c.moveToNext())result.add(c.getString(0));}return result;}
    public static Map<String,long[]> inventory(SQLiteDatabase db){
        Map<String,long[]> result=new HashMap<>();try(Cursor c=db.rawQuery("SELECT artifact_hash,bytes,modified_at FROM r31_gc_artifact WHERE state='LIVE' LIMIT "+(MAX_NODES+1),null)){while(c.moveToNext()){result.put(c.getString(0),new long[]{c.getLong(1),c.getLong(2)});if(result.size()>MAX_NODES)throw new IllegalStateException("R31_GC_ARTIFACT_BOUND");}}return result;
    }
    /** Report reader: bounded aggregate facts only; no census, DDL, filesystem IO or GC. */
    public static Map<String,Object> report(SQLiteDatabase db){
        Map<String,Object> out=new LinkedHashMap<>();out.put("owner","CheckpointGcStoreR31");out.put("scope","HASH_VERIFIED_R26_CHECKPOINT_CAS; KEEP_ORIGINALS_HAVE_THEIR_OWN_OWNER");
        if(!ready(db)){out.put("state","SCHEMA_PENDING");return out;}
        try(Cursor c=db.rawQuery("SELECT state,count(*),coalesce(sum(bytes),0) FROM r31_gc_artifact GROUP BY state LIMIT 4",null)){while(c.moveToNext()){out.put(c.getString(0)+"Count",c.getLong(1));out.put(c.getString(0)+"Bytes",c.getLong(2));}}
        try(Cursor c=db.rawQuery("SELECT count(*),coalesce(sum(retry_count),0),coalesce(sum(released_bytes),0) FROM r31_gc_receipt",null)){if(c.moveToFirst()){out.put("receipts",c.getLong(0));out.put("unlinkRetries",c.getLong(1));out.put("verifiedHistoricalReleasedBytes",c.getLong(2));}}
        try(Cursor c=db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1 AND length(CAST(report_json AS BLOB))<=65536",null)){if(c.moveToFirst())out.put("lastActualCollection",JsonR27.parse(c.getString(0)));}
        out.put("state","DURABLE_STATE_AVAILABLE_NOT_A_HEALTH_VERDICT");return out;
    }
}
