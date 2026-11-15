package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.StorageGcR31;
import java.util.*;

/** Bounded live collection without materialising all historical publications.
 * Retire unrooted parent artifacts before their children. A live/deleting incoming
 * parent conservatively protects a child even when the parent is itself unrooted.
 * This preserves publication races without trusting an incomplete global mark.
 * Rootless cyclic reverse closures of at most 64 members retire atomically;
 * larger closures are preserved and reported without blocking other candidates. */
public final class CheckpointGcReachabilityR324 {
    private CheckpointGcReachabilityR324() {}
    static final String PHYSICAL_DDL="CREATE TABLE IF NOT EXISTS r31_gc_physical_claim_r324(artifact_hash TEXT PRIMARY KEY,claim_version INTEGER NOT NULL,physical_bytes INTEGER NOT NULL CHECK(physical_bytes>=0))";
    static final String EDGE_INDEX_DDL="CREATE INDEX IF NOT EXISTS idx_r31_gc_reverse_r324 ON r31_gc_edge(to_hash,from_hash)";
    public static boolean ready(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT count(*) FROM sqlite_master WHERE (type='table' AND name='r31_gc_physical_claim_r324') OR (type='index' AND name='idx_r31_gc_reverse_r324') OR (type='table' AND name='r31_gc_closed_component_r324') OR (type='index' AND name='idx_r31_gc_closed_component_r324')",null)) { return c.moveToFirst()&&c.getInt(0)==4; }
    }
    private static void hash(String value) { if(value==null||!value.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("R31_GC_ARTIFACT_HASH"); }
    public static boolean protectedNow(SQLiteDatabase db,String artifact) {
        hash(artifact);CheckpointGcStoreR31.requirePublicationCoverage(db);
        if(CheckpointReferencesR26.directlyReferencedR324(db,artifact)) return true;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_gc_edge e LEFT JOIN r31_gc_artifact p ON p.artifact_hash=e.from_hash WHERE e.to_hash=? AND (p.artifact_hash IS NULL OR p.state<>'DELETED') LIMIT 1",new String[]{artifact})) { return c.moveToFirst(); }
    }
    public static void requireClosedMetadata(SQLiteDatabase db) {
        if(!ready(db)) throw new IllegalStateException("DEPENDENCY:R324_GC_MIGRATION_PENDING");
        CheckpointGcStoreR31.requirePublicationCoverage(db);CheckpointReferencesR26.requireRegisteredR324(db);
    }
    /** Must run inside the existing serialized writer; physical bytes were
     * measured by the IO owner under the root admission/report lock. */
    public static boolean claim(SQLiteDatabase db,String artifact,long version,long physicalBytes) {
        if(!db.inTransaction()) throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");
        if(physicalBytes<0) throw new IllegalArgumentException("R324_GC_PHYSICAL_BYTES");
        CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());
        if(store.state(artifact)!=StorageGcR31.State.LIVE||protectedNow(db,artifact)) return false;
        if(!store.compareAndSet(artifact,StorageGcR31.State.LIVE,version,StorageGcR31.State.DELETING)) return false;
        db.execSQL("INSERT OR REPLACE INTO r31_gc_physical_claim_r324 VALUES(?,?,?)",new Object[]{artifact,store.version(artifact),physicalBytes});
        // Keep immutable edges until the parent is DELETED. Dropping them here
        // could let a child be collected while an unlinked parent is retried.
        store.receipt(artifact,"CLAIMED_DELETING");return true;
    }
    public static long physicalClaim(SQLiteDatabase db,String artifact,long version,long legacyRawBytes) {
        if(!ready(db)) return legacyRawBytes;
        try(Cursor c=db.rawQuery("SELECT physical_bytes,claim_version FROM r31_gc_physical_claim_r324 WHERE artifact_hash=?",new String[]{artifact})) {
            if(!c.moveToFirst()) return legacyRawBytes;
            long claim=c.getLong(1);if(claim!=version&&claim!=version-1) throw new IllegalStateException("R324_GC_PHYSICAL_CLAIM_VERSION");
            return c.getLong(0);
        }
    }
    public static List<String> candidatePage(SQLiteDatabase db,String after,long cutoff,int limit) {
        if(after==null) after="";if(!after.isEmpty()) hash(after);
        int page=Math.max(1,Math.min(CheckpointGcStoreR31.PAGE,limit));List<String> result=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r31_gc_artifact WHERE state='LIVE' AND modified_at<=? AND artifact_hash>? ORDER BY artifact_hash LIMIT "+page,new String[]{Long.toString(cutoff),after})) {
            while(c.moveToNext()) { hash(c.getString(0));result.add(c.getString(0)); }
        }
        return result;
    }
    public static List<String> deletingPage(SQLiteDatabase db,String after,int limit) {
        if(after==null) after="";if(!after.isEmpty()) hash(after);
        int page=Math.max(1,Math.min(CheckpointGcStoreR31.PAGE,limit));List<String> result=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r31_gc_artifact WHERE state='DELETING' AND artifact_hash>? ORDER BY artifact_hash LIMIT "+page,new String[]{after})) {
            while(c.moveToNext()) {hash(c.getString(0));result.add(c.getString(0));}
        }
        return result;
    }
    public static String cursor(SQLiteDatabase db) {return cursor(db,"candidateCursorR324");}
    public static String deletingCursor(SQLiteDatabase db) {return cursor(db,"deletingCursorR324");}
    private static String cursor(SQLiteDatabase db,String field) {
        try(Cursor c=db.rawQuery("SELECT report_json FROM r31_gc_status WHERE singleton=1 AND length(CAST(report_json AS BLOB))<=65536",null)) {
            if(c.moveToFirst()) { Object value=JsonR27.parse(c.getString(0));if(value instanceof Map) { Object cursor=((Map<?,?>)value).get(field);if(cursor instanceof String&&((String)cursor).matches("[0-9a-f]{64}")) return (String)cursor; } }
        }
        return "";
    }
    static final String COMPONENT_DDL="CREATE TABLE IF NOT EXISTS r31_gc_closed_component_r324(artifact_hash TEXT PRIMARY KEY,component_id TEXT NOT NULL,claim_version INTEGER NOT NULL)";
    static final String COMPONENT_INDEX_DDL="CREATE INDEX IF NOT EXISTS idx_r31_gc_closed_component_r324 ON r31_gc_closed_component_r324(component_id,artifact_hash)";
    /** A bounded reverse closure contains every nondeleted incoming parent.
     * All members retire in one writer transaction; no root can race into a
     * still-LIVE member after only part of a cycle was claimed. */
    public static final class ClosedComponent {
        public final Map<String,Long> versions;public final String disposition;
        private ClosedComponent(Map<String,Long> versions,String disposition){this.versions=Collections.unmodifiableMap(new LinkedHashMap<>(versions));this.disposition=disposition;}
        public boolean claimable(){return "ROOTLESS_CYCLIC_CLOSURE_READY".equals(disposition);}
    }
    private static List<String> incoming(SQLiteDatabase db,String artifact,int maximum){
        List<String> values=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT e.from_hash FROM r31_gc_edge e LEFT JOIN r31_gc_artifact p ON p.artifact_hash=e.from_hash WHERE e.to_hash=? AND (p.artifact_hash IS NULL OR p.state<>'DELETED') ORDER BY e.from_hash LIMIT "+(maximum+1),new String[]{artifact})){
            while(c.moveToNext()){hash(c.getString(0));values.add(c.getString(0));}
        }return values;
    }
    public static ClosedComponent closedComponent(SQLiteDatabase db,String start,int maximum)throws java.io.InterruptedIOException{
        hash(start);maximum=Math.max(1,Math.min(CheckpointGcStoreR31.PAGE,maximum));
        CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());
        Map<String,Long> versions=new LinkedHashMap<>();Map<String,Set<String>> forward=new LinkedHashMap<>();
        ArrayDeque<String> pending=new ArrayDeque<>();Set<String> scheduled=new LinkedHashSet<>();scheduled.add(start);pending.add(start);
        while(!pending.isEmpty()){
            ExecutionBudgetR26.poll();String member=pending.removeFirst();
            if(store.state(member)!=StorageGcR31.State.LIVE)return new ClosedComponent(versions,"NONLIVE_OR_MISSING_PARENT_PRESERVED");
            if(CheckpointReferencesR26.directlyReferencedR324(db,member))return new ClosedComponent(versions,"ROOTED_COMPONENT_PRESERVED");
            versions.put(member,store.version(member));
            for(String parent:incoming(db,member,maximum)){
                if(scheduled.add(parent)){if(scheduled.size()>maximum)return new ClosedComponent(versions,"COMPONENT_EXCEEDS_SLICE_PRESERVED");pending.addLast(parent);}
                forward.computeIfAbsent(parent,k->new LinkedHashSet<>()).add(member);
            }
        }
        // Acyclic closures use the ordinary parent-first protocol. Only a real
        // directed cycle needs the stronger whole-component retirement claim.
        Map<String,Integer> degree=new HashMap<>();for(String member:versions.keySet())degree.put(member,0);
        for(Set<String> children:forward.values())for(String child:children)degree.put(child,degree.get(child)+1);
        ArrayDeque<String> zero=new ArrayDeque<>();for(String member:degree.keySet())if(degree.get(member)==0)zero.add(member);
        int removed=0;while(!zero.isEmpty()){String member=zero.removeFirst();removed++;for(String child:forward.getOrDefault(member,Collections.emptySet())){int n=degree.get(child)-1;degree.put(child,n);if(n==0)zero.add(child);}}
        return new ClosedComponent(versions,removed==versions.size()?"ACYCLIC_USE_PARENT_FIRST":"ROOTLESS_CYCLIC_CLOSURE_READY");
    }
    public static boolean claimClosedComponent(SQLiteDatabase db,ClosedComponent component,Map<String,Long> physical){
        if(!db.inTransaction())throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");
        if(!component.claimable()||component.versions.isEmpty()||component.versions.size()>CheckpointGcStoreR31.PAGE||!component.versions.keySet().equals(physical.keySet()))throw new IllegalArgumentException("R324_GC_COMPONENT_CLAIM_SHAPE");
        CheckpointGcStoreR31.requirePublicationCoverage(db);CheckpointGcStoreR31 store=new CheckpointGcStoreR31(db,Collections.emptyMap());
        // Check every member and every external incoming edge before the first
        // mutation. The existing single writer serializes all pin/ref admission.
        for(Map.Entry<String,Long> item:component.versions.entrySet()){
            String member=item.getKey();if(physical.get(member)==null||physical.get(member)<0)throw new IllegalArgumentException("R324_GC_COMPONENT_PHYSICAL_BYTES");
            if(store.state(member)!=StorageGcR31.State.LIVE||store.version(member)!=item.getValue()||CheckpointReferencesR26.directlyReferencedR324(db,member))return false;
            for(String parent:incoming(db,member,CheckpointGcStoreR31.PAGE))if(!component.versions.containsKey(parent))return false;
            if(componentBusy(db,member))throw new IllegalStateException("R324_GC_LIVE_COMPONENT_PERMISSION_CONFLICT");
        }
        String id=UUID.randomUUID().toString();
        for(Map.Entry<String,Long> item:component.versions.entrySet()){
            String member=item.getKey();if(!store.compareAndSet(member,StorageGcR31.State.LIVE,item.getValue(),StorageGcR31.State.DELETING))throw new IllegalStateException("R324_GC_COMPONENT_CAS_ROLLBACK_REQUIRED");
            long version=store.version(member);
            db.execSQL("INSERT OR REPLACE INTO r31_gc_physical_claim_r324 VALUES(?,?,?)",new Object[]{member,version,physical.get(member)});
            db.execSQL("INSERT INTO r31_gc_closed_component_r324 VALUES(?,?,?)",new Object[]{member,id,version});
            store.receipt(member,"CLAIMED_DELETING");
        }return true;
    }
    private static String componentId(SQLiteDatabase db,String artifact){
        try(Cursor c=db.rawQuery("SELECT component_id FROM r31_gc_closed_component_r324 WHERE artifact_hash=?",new String[]{artifact})){return c.moveToFirst()?c.getString(0):null;}
    }
    /** Completed members cannot be recreated while another member of the same
     * retired cycle is still being unlinked/retried. Whole-group completion
     * removes this permission, after which normal verified revival is allowed. */
    public static boolean componentBusy(SQLiteDatabase db,String artifact){hash(artifact);return componentId(db,artifact)!=null;}
    public static boolean protectedForUnlink(SQLiteDatabase db,String artifact){
        String id=componentId(db,artifact);if(id==null)return protectedNow(db,artifact);
        CheckpointGcStoreR31.requirePublicationCoverage(db);Set<String> members=new LinkedHashSet<>();
        try(Cursor c=db.rawQuery("SELECT g.artifact_hash,g.claim_version,a.state,a.version FROM r31_gc_closed_component_r324 g LEFT JOIN r31_gc_artifact a ON a.artifact_hash=g.artifact_hash WHERE g.component_id=? ORDER BY g.artifact_hash LIMIT "+(CheckpointGcStoreR31.PAGE+1),new String[]{id})){
            while(c.moveToNext()){
                String member=c.getString(0),state=c.getString(2);long claim=c.getLong(1),version=c.getLong(3);hash(member);
                if(!("DELETING".equals(state)&&version==claim||"DELETED".equals(state)&&version==claim+1))throw new IllegalStateException("R324_GC_COMPONENT_PERMISSION_VERSION");
                members.add(member);if(members.size()>CheckpointGcStoreR31.PAGE)throw new IllegalStateException("R324_GC_COMPONENT_PERMISSION_BOUND");
            }
        }
        if(!members.contains(artifact))throw new IllegalStateException("R324_GC_COMPONENT_PERMISSION_MISSING");
        for(String member:members){
            if(CheckpointReferencesR26.directlyReferencedR324(db,member))return true;
            for(String parent:incoming(db,member,CheckpointGcStoreR31.PAGE))if(!members.contains(parent))return true;
        }
        return false;
    }
    public static void completeComponentMember(SQLiteDatabase db,String artifact){
        if(!db.inTransaction())throw new IllegalStateException("R31_GC_EXISTING_WRITER_REQUIRED");String id=componentId(db,artifact);if(id==null)return;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r31_gc_closed_component_r324 g LEFT JOIN r31_gc_artifact a ON a.artifact_hash=g.artifact_hash WHERE g.component_id=? AND (a.artifact_hash IS NULL OR a.state<>'DELETED' OR a.version<>g.claim_version+1) LIMIT 1",new String[]{id})){if(c.moveToFirst())return;}
        db.execSQL("DELETE FROM r31_gc_closed_component_r324 WHERE component_id=?",new Object[]{id});
    }
}
