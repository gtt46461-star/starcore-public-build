package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Bounded, verified facts copied from the original immutable Root learning ledger. The original event table
 * is never created in research. Capture closes every Root cursor before a separate RP transaction applies it. */
public final class LegacyLearnedInputR29 {
    private LegacyLearnedInputR29(){}
    public static final int PAGE=128;
    public static final String TABLE="r29_legacy_learned_input",STATE="r29_legacy_learned_state",LOCAL="r29_legacy_local_learned";
    private static final String GATE="r29_legacy_learned_apply_gate";
    private static final String TINY="ResearchTinyDeepLearnerR909",FTRL="ResearchAdaptiveLearnerStoreR811";
    private static final String ROOT="v57_runtime_event_r926";
    private static final java.util.concurrent.ConcurrentHashMap<String,Long> INSTALLED=new java.util.concurrent.ConcurrentHashMap<>();
    private static String key(SQLiteDatabase db){String path=db.getPath();return path.equals(":memory:")||path.isEmpty()?path+"#"+System.identityHashCode(db):path;}
    private static long schema(SQLiteDatabase db){return MigrationCopyR29.scalar(db,"PRAGMA schema_version",null);}
    private static boolean installed(SQLiteDatabase db){
        long version=schema(db);Long cached=INSTALLED.get(key(db));if(cached!=null&&cached==version)return true;
        // The version invalidates exactly as before, but unrelated legacy tables and
        // their large trigger SQL do not participate in this guard contract.
        Set<String> names=new LinkedHashSet<>(Arrays.asList(TABLE,STATE,LOCAL,GATE,"r29_research_owner",
                "r29_legacy_guard_migration",DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL));
        names.addAll(guards(true).keySet());
        StringJoiner marks=new StringJoiner(",");for(String name:names)marks.add("?");
        Map<String,String> objects=new HashMap<>();try(Cursor c=db.rawQuery("SELECT name,sql FROM sqlite_master WHERE type IN('table','trigger') AND name IN ("+marks+")",names.toArray(new String[0]))){while(c.moveToNext())objects.put(c.getString(0),c.getString(1));}
        for(String name:new String[]{TABLE,STATE,LOCAL,GATE,"r29_research_owner","r29_legacy_guard_migration"})if(!objects.containsKey(name))return false;
        for(Map.Entry<String,String> guard:guards(objects.containsKey(DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL)).entrySet())if(!guard.getValue().equals(objects.get(guard.getKey())))return false;
        INSTALLED.put(key(db),version);return true;
    }
    private static final String FILTER="game_code=? AND event_type='LEARNED' AND stage='LEARNED_EXACTLY_ONCE' AND source IN('"+TINY+"','"+FTRL+"')";
    private static String[] args(long after,long head){return new String[]{String.valueOf(after),String.valueOf(head),ReleaseManifestR926.GAME_CODE};}
    public static String readySql(){return "EXISTS(SELECT 1 FROM "+STATE+" WHERE singleton=1 AND ready=1)";}
    public static String learnedExistsSql(String issueExpression,String source){
        if(!TINY.equals(source)&&!FTRL.equals(source))throw new IllegalArgumentException("R29_LEARNED_FREEZE_SOURCE");
        String predicate="game_code='"+ReleaseManifestR926.GAME_CODE+"' AND target_issue="+issueExpression+" AND source='"+source+"'";
        return "(EXISTS(SELECT 1 FROM "+TABLE+" WHERE "+predicate+") OR EXISTS(SELECT 1 FROM "+LOCAL+" WHERE "+predicate+" AND root_acked_at>0))";
    }
    public static boolean ready(SQLiteDatabase research){
        if(!table(research,STATE))return false;return MigrationCopyR29.scalar(research,"SELECT count(*) FROM "+STATE+" WHERE singleton=1 AND ready=1",null)==1;
    }
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void research(SQLiteDatabase db){if(!ResearchRouteR29.isResearchDatabase(db))throw new IllegalStateException("R29_LEARNED_RESEARCH_DOMAIN_REQUIRED");}
    public static void installResearch(SQLiteDatabase db){
        if(installed(db))return;research(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(source_seq INTEGER PRIMARY KEY,event_id TEXT NOT NULL UNIQUE,dedup_key TEXT NOT NULL,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,epoch_id TEXT NOT NULL,attempt_id TEXT NOT NULL,event_type TEXT NOT NULL,stage TEXT NOT NULL,state_rank INTEGER NOT NULL,payload_text TEXT NOT NULL,payload_hash TEXT NOT NULL,source TEXT NOT NULL,created_at INTEGER NOT NULL,identity_hash TEXT NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS r29_legacy_learned_target ON "+TABLE+"(game_code,target_issue,source)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+STATE+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),source_seq INTEGER NOT NULL,source_head INTEGER NOT NULL,bootstrap_head INTEGER NOT NULL,ready INTEGER NOT NULL CHECK(ready IN(0,1)),updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+GATE+"(singleton INTEGER PRIMARY KEY CHECK(singleton=1),active INTEGER NOT NULL CHECK(active IN(0,1)))");db.execSQL("INSERT OR IGNORE INTO "+GATE+" VALUES(1,0)");
        if(MigrationCopyR29.scalar(db,"SELECT count(*) FROM "+STATE,null)==0)db.execSQL("INSERT INTO "+STATE+" VALUES(1,0,0,-1,0,0)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+LOCAL+"(learning_identity TEXT PRIMARY KEY,game_code TEXT NOT NULL,target_issue TEXT NOT NULL,source TEXT NOT NULL,pre_state_hash TEXT NOT NULL,post_state_hash TEXT NOT NULL,trained_until TEXT NOT NULL,payload_hash TEXT NOT NULL,recorded_at INTEGER NOT NULL,root_acked_at INTEGER NOT NULL DEFAULT 0)");
        db.execSQL("CREATE INDEX IF NOT EXISTS r29_local_learned_target ON "+LOCAL+"(game_code,target_issue,source,root_acked_at)");
        db.execSQL("CREATE TABLE IF NOT EXISTS r29_legacy_guard_migration(trigger_name TEXT PRIMARY KEY,original_sql TEXT NOT NULL,original_sha256 TEXT NOT NULL,replaced_at INTEGER NOT NULL)");
        for(Map.Entry<String,String> guard:guards(table(db,DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL)).entrySet())replaceGuard(db,guard.getKey(),guard.getValue());
        INSTALLED.put(key(db),schema(db));
    }
    private static Map<String,String> guards(boolean tiny){
        Map<String,String> out=new LinkedHashMap<>();
        out.put("r29_learned_input_insert_owner","CREATE TRIGGER r29_learned_input_insert_owner BEFORE INSERT ON "+TABLE+" WHEN NOT EXISTS(SELECT 1 FROM "+GATE+" WHERE singleton=1 AND active=1) BEGIN SELECT RAISE(ABORT,'R29_ROOT_LEARNED_CAPTURE_REQUIRED'); END");
        for(String event:new String[]{"UPDATE","DELETE"}){String name="r29_learned_input_no_"+event.toLowerCase(java.util.Locale.ROOT);out.put(name,"CREATE TRIGGER "+name+" BEFORE "+event+" ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R29_ROOT_LEARNED_FACT_IMMUTABLE'); END");}
        out.put("r29_learned_state_owner","CREATE TRIGGER r29_learned_state_owner BEFORE UPDATE ON "+STATE+" WHEN NOT EXISTS(SELECT 1 FROM "+GATE+" WHERE singleton=1 AND active=1) BEGIN SELECT RAISE(ABORT,'R29_ROOT_LEARNED_CAPTURE_REQUIRED'); END");
        out.put("r29_learned_state_no_delete","CREATE TRIGGER r29_learned_state_no_delete BEFORE DELETE ON "+STATE+" BEGIN SELECT RAISE(ABORT,'R29_LEARNED_CURSOR_IMMUTABLE'); END");
        out.put("r29_local_learned_immutable","CREATE TRIGGER r29_local_learned_immutable BEFORE UPDATE OF learning_identity,game_code,target_issue,source,pre_state_hash,post_state_hash,trained_until,payload_hash,recorded_at ON "+LOCAL+" BEGIN SELECT RAISE(ABORT,'R29_LOCAL_LEARNED_IMMUTABLE'); END");
        out.put("r29_local_learned_no_delete","CREATE TRIGGER r29_local_learned_no_delete BEFORE DELETE ON "+LOCAL+" BEGIN SELECT RAISE(ABORT,'R29_LOCAL_LEARNED_IMMUTABLE'); END");
        out.put("r29_local_learned_first_ack","CREATE TRIGGER r29_local_learned_first_ack BEFORE UPDATE OF root_acked_at ON "+LOCAL+" WHEN OLD.root_acked_at>0 OR NEW.root_acked_at<=0 BEGIN SELECT RAISE(ABORT,'R29_LOCAL_LEARNED_ACK_IMMUTABLE'); END");
        if(tiny){String old="OLD.asset_id='PRODUCTION_SELECTED'",next="NEW.asset_id='PRODUCTION_SELECTED'";
            out.put("trg_r29_tiny_label_bootstrap_update","CREATE TRIGGER trg_r29_tiny_label_bootstrap_update BEFORE UPDATE ON "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHEN ("+old+" OR "+next+") AND NOT("+readySql()+") BEGIN SELECT RAISE(ABORT,'R29_TINY_LABEL_BOOTSTRAP_PENDING'); END");
            out.put("trg_r927_e02c_tiny_label_update_freeze","CREATE TRIGGER trg_r927_e02c_tiny_label_update_freeze BEFORE UPDATE OF asset_id,target_issue,feature_blob,hit_label ON "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHEN ("+old+" AND (NOT("+readySql()+") OR "+learnedExistsSql("OLD.target_issue",TINY)+")) OR ("+next+" AND (NOT("+readySql()+") OR "+learnedExistsSql("NEW.target_issue",TINY)+")) BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END");
            out.put("trg_r927_e02c_tiny_label_delete_freeze","CREATE TRIGGER trg_r927_e02c_tiny_label_delete_freeze BEFORE DELETE ON "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHEN "+old+" AND (NOT("+readySql()+") OR "+learnedExistsSql("OLD.target_issue",TINY)+") BEGIN SELECT RAISE(ABORT,'E02C_TINY_LABEL_FROZEN_AFTER_LEARN'); END");
            out.put("trg_r29_tiny_label_insert_guard","CREATE TRIGGER trg_r29_tiny_label_insert_guard BEFORE INSERT ON "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHEN "+next+" AND (NOT("+readySql()+") OR "+learnedExistsSql("NEW.target_issue",TINY)+") BEGIN SELECT RAISE(ABORT,'R29_TINY_LABEL_BOOTSTRAP_OR_LEARNED'); END");
        }return out;
    }
    private static void replaceGuard(SQLiteDatabase db,String name,String expected){
        String old=null;try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type='trigger' AND name=?",new String[]{name})){if(c.moveToFirst())old=c.getString(0);}
        if(expected.equals(old))return;
        if(old!=null){db.execSQL("INSERT OR IGNORE INTO r29_legacy_guard_migration VALUES(?,?,?,?)",new Object[]{name,old,StatsR27.sha256(old),System.currentTimeMillis()});db.execSQL("DROP TRIGGER "+MigrationCopyR29.id(name));}db.execSQL(expected);
    }
    public static final class Fact {
        public final long seq,createdAt;public final int rank;public final String id,dedup,game,issue,epoch,attempt,type,stage,payload,payloadHash,source,identityHash;
        private Fact(Cursor c){seq=c.getLong(0);id=c.getString(1);dedup=c.getString(2);game=c.getString(3);issue=c.getString(4);epoch=c.getString(5);attempt=c.getString(6);type=c.getString(7);stage=c.getString(8);rank=c.getInt(9);payload=c.getString(10);payloadHash=c.getString(11);source=c.getString(12);createdAt=c.getLong(13);
            if(seq<=0||createdAt<=0||!sha(id)||!sha(payloadHash)||!StatsR27.sha256(payload).equals(payloadHash)||!ReleaseManifestR926.GAME_CODE.equals(game)||!TargetIssueResolver.isValid(issue)||!"LEARNED".equals(type)||!"LEARNED_EXACTLY_ONCE".equals(stage)||(!TINY.equals(source)&&!FTRL.equals(source))||rank!=90||dedup==null||dedup.isEmpty())throw new IllegalStateException("R29_ROOT_LEARNED_FACT_INVALID");
            Map<String,Object> map=new LinkedHashMap<>();map.put("seq",seq);map.put("eventId",id);map.put("dedup",dedup);map.put("game",game);map.put("issue",issue);map.put("epoch",epoch);map.put("attempt",attempt);map.put("type",type);map.put("stage",stage);map.put("rank",rank);map.put("payload",payload);map.put("payloadHash",payloadHash);map.put("source",source);map.put("createdAt",createdAt);identityHash=StatsR27.sha256(JsonR27.write(map));
        }
    }
    public static final class Capture {
        public final long previousSeq,head,nextSeq,bootstrapHead;public final boolean ready,noChange;public final List<Fact> facts;
        private Capture(long previous,long head,long next,long bootstrap,boolean ready,boolean noChange,List<Fact> facts){previousSeq=previous;this.head=head;nextSeq=next;bootstrapHead=bootstrap;this.ready=ready;this.noChange=noChange;this.facts=Collections.unmodifiableList(facts);}
    }
    public static Capture capture(SQLiteDatabase root,SQLiteDatabase research){
        if(ResearchRouteR29.isResearchDatabase(root)||root.inTransaction())throw new IllegalStateException("R29_COMMITTED_ROOT_READ_ONLY_CAPTURE_REQUIRED");
        if(!table(root,ROOT))throw new IllegalStateException("R29_ROOT_LEARNED_LEDGER_REQUIRED_BOOTSTRAP_PENDING");
        long previous=0,bootstrap=-1;boolean wasReady=false;
        if(table(research,STATE))try(Cursor c=research.rawQuery("SELECT source_seq,bootstrap_head,ready FROM "+STATE+" WHERE singleton=1",null)){if(c.moveToFirst()){previous=c.getLong(0);bootstrap=c.getLong(1);wasReady=c.getInt(2)==1;}}
        long head=MigrationCopyR29.scalar(root,"SELECT coalesce(max(seq),0) FROM "+ROOT,null);if(head<previous)throw new IllegalStateException("R29_ROOT_LEARNED_LEDGER_REWOUND");if(bootstrap<0)bootstrap=head;
        String range="seq>CAST(? AS INTEGER) AND seq<=CAST(? AS INTEGER) AND "+FILTER;
        List<Long> page=new ArrayList<>();
        try(Cursor c=root.rawQuery("SELECT seq,length(payload_text),length(dedup_key),length(epoch_id),length(attempt_id) FROM "+ROOT+" WHERE "+range+" ORDER BY seq LIMIT "+PAGE,args(previous,head))){
            while(c.moveToNext()){for(int i=1;i<=4;i++)if(c.getLong(i)>(i==1?16384:4096))throw new IllegalStateException("R29_ROOT_LEARNED_FACT_BUDGET");page.add(c.getLong(0));}
        }
        List<Fact> facts=new ArrayList<>();long last=previous;
        if(!page.isEmpty())try(Cursor c=root.rawQuery("SELECT seq,event_id,dedup_key,game_code,target_issue,epoch_id,attempt_id,event_type,stage,state_rank,payload_text,payload_hash,source,created_at FROM "+ROOT+" WHERE "+range+" ORDER BY seq LIMIT "+PAGE,args(previous,page.get(page.size()-1)))){
            while(c.moveToNext()){Fact f=new Fact(c);if(f.seq<=last)throw new IllegalStateException("R29_ROOT_LEARNED_SEQUENCE");facts.add(f);last=f.seq;}
        }
        if(facts.size()!=page.size())throw new IllegalStateException("R29_ROOT_LEARNED_PAGE_CHANGED");
        boolean exhausted=MigrationCopyR29.scalar(root,"SELECT count(*) FROM(SELECT 1 FROM "+ROOT+" WHERE "+range+" LIMIT 1)",args(last,head))==0;
        return new Capture(previous,head,exhausted?head:last,bootstrap,exhausted&&head>=bootstrap,wasReady&&previous==head,facts);
    }
    public static void apply(SQLiteDatabase research,Capture capture){
        if(!research.inTransaction())throw new IllegalStateException("R29_LEARNED_CAPTURE_RESEARCH_TRANSACTION");installResearch(research);
        long actual=MigrationCopyR29.scalar(research,"SELECT source_seq FROM "+STATE+" WHERE singleton=1",null);if(actual!=capture.previousSeq)throw new IllegalStateException("CP3_DB_BUSY_R29_STALE_LEARNED_CAPTURE");
        research.execSQL("UPDATE "+GATE+" SET active=1 WHERE singleton=1");
        try{
            for(Fact f:capture.facts){
                research.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{f.seq,f.id,f.dedup,f.game,f.issue,f.epoch,f.attempt,f.type,f.stage,f.rank,f.payload,f.payloadHash,f.source,f.createdAt,f.identityHash});
                try(Cursor c=research.rawQuery("SELECT event_id,identity_hash FROM "+TABLE+" WHERE source_seq=?",new String[]{String.valueOf(f.seq)})){if(!c.moveToFirst()||!f.id.equals(c.getString(0))||!f.identityHash.equals(c.getString(1)))throw new IllegalStateException("R29_LEARNED_FACT_IDENTITY_CONFLICT");}
                acknowledgeCaptured(research,f);
            }
            research.execSQL("UPDATE "+STATE+" SET source_seq=?,source_head=?,bootstrap_head=?,ready=?,updated_at=? WHERE singleton=1",new Object[]{capture.nextSeq,capture.head,capture.bootstrapHead,capture.ready?1:0,System.currentTimeMillis()});
        }finally{research.execSQL("UPDATE "+GATE+" SET active=0 WHERE singleton=1");}
    }
    public static void refresh(DbStore store){
        SQLiteDatabase authority=LegacyResearchScopeR29.rawRealtime(store);if(!ResearchRouteR29.active(authority))return;
        Capture captured=capture(ResearchRouteR29.realtimeReadOnly(authority),ResearchRouteR29.reader(authority));
        if(captured.noChange&&installed(ResearchRouteR29.reader(authority)))return;
        ResearchRouteR29.write(store,"R29_LEGACY_LEARNED_FACT_CAPTURE",db->{apply(db,captured);return 0;});
    }
    private static void acknowledgeCaptured(SQLiteDatabase db,Fact fact){
        String[] fields=fact.payload.split("[|]",5);String[] names={"learning_identity=","pre_state_hash=","post_state_hash=","trained_until="};String[] values=new String[4];
        if(fields.length<4)return;for(int i=0;i<4;i++){if(!fields[i].startsWith(names[i]))return;values[i]=fields[i].substring(names[i].length());}
        if(!sha(values[0])||!sha(values[1])||!sha(values[2])||!fact.issue.equals(values[3]))return;
        try(Cursor c=db.rawQuery("SELECT game_code,target_issue,source,pre_state_hash,post_state_hash,trained_until,root_acked_at FROM "+LOCAL+" WHERE learning_identity=?",new String[]{values[0]})){
            if(!c.moveToFirst())return;
            if(!fact.game.equals(c.getString(0))||!fact.issue.equals(c.getString(1))||!fact.source.equals(c.getString(2))||!values[1].equals(c.getString(3))||!values[2].equals(c.getString(4))||!values[3].equals(c.getString(5)))throw new IllegalStateException("R29_ROOT_LOCAL_LEARNED_IDENTITY_CONFLICT");
            if(c.getLong(6)>0)return;
        }
        acknowledgeLocalWithin(db,values[0],fact.createdAt);
    }
    public static void appendLocal(SQLiteDatabase db,String issue,String source,String identity,String pre,String post,String through,String detail){recordLocalWithin(db,issue,source,identity,pre,post,through,detail);}
    public static void recordLocalWithin(SQLiteDatabase db,String issue,String source,String identity,String pre,String post,String through,String detail){
        if(!db.inTransaction()||!TargetIssueResolver.isValid(issue)||!issue.equals(through)||source==null||source.isEmpty()||!sha(identity)||!sha(pre)||!sha(post))throw new IllegalArgumentException("R29_LOCAL_LEARNED_INPUT");installResearch(db);
        String hash=StatsR27.sha256(JsonR27.write(Arrays.asList(issue,source,identity,pre,post,through,detail==null?"":detail)));
        db.execSQL("INSERT OR IGNORE INTO "+LOCAL+" VALUES(?,?,?,?,?,?,?,?,?,0)",new Object[]{identity,ReleaseManifestR926.GAME_CODE,issue,source,pre,post,through,hash,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT payload_hash FROM "+LOCAL+" WHERE learning_identity=?",new String[]{identity})){if(!c.moveToFirst()||!hash.equals(c.getString(0)))throw new IllegalStateException("R29_LOCAL_LEARNED_IDENTITY_CONFLICT");}
    }
    public static void acknowledgeLocalWithin(SQLiteDatabase db,String identity,long rootAckedAt){
        if(!db.inTransaction()||!sha(identity)||rootAckedAt<=0)throw new IllegalArgumentException("R29_LOCAL_LEARNED_ACK");research(db);
        db.execSQL("UPDATE "+LOCAL+" SET root_acked_at=? WHERE learning_identity=? AND root_acked_at=0",new Object[]{rootAckedAt,identity});
    }
    private static boolean sha(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
}
