package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Same-database source changes. A canonical write records one event, never one row per affected pin.
 * The event sequence is independent of the legacy source-generation trigger execution order. */
public final class ResearchSourceJournalR31 {
    private ResearchSourceJournalR31(){}
    public static final String EVENT="r31_source_event",MEMBER="r31_source_member",FENCE="r31_pin_source_fence";
    public static final int REVISION=2;
    private static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_source_event(event_seq INTEGER PRIMARY KEY AUTOINCREMENT,event_key TEXT UNIQUE,source_name TEXT NOT NULL,operation TEXT NOT NULL CHECK(operation IN ('INSERT','UPDATE','DELETE','IMPORT_INSERT','IMPORT_REPLACE')),old_ordinal INTEGER,new_ordinal INTEGER,first_ordinal INTEGER,through_ordinal INTEGER,member_set TEXT,recorded_at INTEGER NOT NULL,CHECK(first_ordinal IS NULL OR through_ordinal IS NULL OR first_ordinal<=through_ordinal))",
        "CREATE TABLE IF NOT EXISTS r31_source_member(member_set TEXT NOT NULL,operation TEXT NOT NULL CHECK(operation IN ('INSERT','REPLACE')),ordinal INTEGER NOT NULL,PRIMARY KEY(member_set,operation,ordinal))",
        "CREATE TABLE IF NOT EXISTS r31_pin_source_fence(run_id TEXT PRIMARY KEY,event_seq INTEGER NOT NULL CHECK(event_seq>=0),FOREIGN KEY(run_id) REFERENCES r30r1_analysis_run(run_id))",
        "CREATE INDEX IF NOT EXISTS r31_source_event_new ON r31_source_event(operation,new_ordinal,event_seq)",
        "CREATE INDEX IF NOT EXISTS r31_source_event_old ON r31_source_event(operation,old_ordinal,event_seq)",
        "CREATE INDEX IF NOT EXISTS r31_source_event_members ON r31_source_event(member_set,event_seq)",
        "CREATE INDEX IF NOT EXISTS r31_source_event_import ON r31_source_event(source_name,event_seq,operation)",
        "CREATE TRIGGER IF NOT EXISTS r31_source_member_no_insert AFTER INSERT ON r31_source_member WHEN EXISTS(SELECT 1 FROM r31_source_event WHERE member_set=NEW.member_set) BEGIN SELECT RAISE(ABORT,'R31_PUBLISHED_SOURCE_MEMBERS_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_source_member_no_update BEFORE UPDATE ON r31_source_member WHEN EXISTS(SELECT 1 FROM r31_source_event WHERE member_set=OLD.member_set OR member_set=NEW.member_set) BEGIN SELECT RAISE(ABORT,'R31_PUBLISHED_SOURCE_MEMBERS_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_source_member_no_delete BEFORE DELETE ON r31_source_member WHEN EXISTS(SELECT 1 FROM r31_source_event WHERE member_set=OLD.member_set) BEGIN SELECT RAISE(ABORT,'R31_PUBLISHED_SOURCE_MEMBERS_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_source_event_no_delete BEFORE DELETE ON r31_source_event BEGIN SELECT RAISE(ABORT,'R31_SOURCE_EVENT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_source_event_no_update BEFORE UPDATE ON r31_source_event BEGIN SELECT RAISE(ABORT,'R31_SOURCE_EVENT_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_source_import_revision AFTER INSERT ON r31_source_event WHEN NEW.source_name='CSV_IMPORT' BEGIN UPDATE r30_live_source_revision SET generation=generation+1 WHERE singleton=1; END",
        "CREATE TRIGGER IF NOT EXISTS r31_pin_source_capture AFTER INSERT ON r30r1_analysis_run WHEN NEW.status IN ('INPUT_DATASET_PINNED','ONLINE_MODEL_ROLE_REGISTERED') BEGIN INSERT OR IGNORE INTO r31_pin_source_fence VALUES(NEW.run_id,COALESCE((SELECT seq FROM sqlite_sequence WHERE name='r31_source_event'),0)); END",
        "CREATE TRIGGER IF NOT EXISTS r31_pin_source_fence_no_update BEFORE UPDATE ON r31_pin_source_fence BEGIN SELECT RAISE(ABORT,'R31_PIN_SOURCE_FENCE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_pin_source_fence_no_delete BEFORE DELETE ON r31_pin_source_fence BEGIN SELECT RAISE(ABORT,'R31_PIN_SOURCE_FENCE_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r31_input_pin_no_delete BEFORE DELETE ON r30r1_analysis_run WHEN OLD.status='INPUT_DATASET_PINNED' BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_PIN_IMMUTABLE'); END",
        "CREATE TRIGGER IF NOT EXISTS r30r1_pin_immutable BEFORE UPDATE ON r30r1_analysis_run WHEN OLD.status='INPUT_DATASET_PINNED' BEGIN SELECT RAISE(ABORT,'RESEARCH_INPUT_PIN_IMMUTABLE'); END"
    };
    private static String name(String sql){String start=sql.substring(0,sql.indexOf(" IF NOT EXISTS "));String tail=sql.substring(start.length()+" IF NOT EXISTS ".length());int at=tail.indexOf(' '),paren=tail.indexOf('(');return tail.substring(0,at<0?paren:paren<0?at:Math.min(at,paren));}
    static String canonical(String sql){
        if(sql==null)return "";StringBuilder out=new StringBuilder();char quote=0;
        for(int i=0;i<sql.length();i++){char value=sql.charAt(i);
            if(quote!=0){out.append(value);if(value==quote){if(i+1<sql.length()&&sql.charAt(i+1)==quote)out.append(sql.charAt(++i));else quote=0;}}
            else if(value=='\''||value=='"'||value=='`'||value=='['){quote=value=='['?']':value;out.append(value);}
            else if(!Character.isWhitespace(value)&&value!=';')out.append(Character.toLowerCase(value));
        }return out.toString().replaceFirst("^(create(?:unique)?(?:table|index|trigger))ifnotexists","$1");
    }
    private static String actual(SQLiteDatabase db,String name){try(Cursor q=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})){return q.moveToFirst()?q.getString(0):null;}}
    private static String[] legacySql(){List<String> out=new ArrayList<>();for(String table:new String[]{"draws","r27_history_import"})for(String op:new String[]{"INSERT","UPDATE"}){
        String changed="NEW.issue IS NOT OLD.issue";
        if(table.equals("draws")){changed+=" OR NEW.open_time IS NOT OLD.open_time OR NEW.dirty IS NOT OLD.dirty";for(int n=1;n<=10;n++)changed+=" OR NEW.n"+n+" IS NOT OLD.n"+n;}
        else changed+=" OR NEW.ordinal IS NOT OLD.ordinal OR NEW.numbers IS NOT OLD.numbers OR NEW.status IS NOT OLD.status";
        String affected=ResearchInputPinR30R1.ordinal("NEW")+" BETWEEN p.from_ordinal AND p.through_ordinal";
        if(op.equals("UPDATE"))affected+=" OR "+ResearchInputPinR30R1.ordinal("OLD")+" BETWEEN p.from_ordinal AND p.through_ordinal";
        out.add("CREATE TRIGGER IF NOT EXISTS r30r1_pin_"+table+'_'+op.toLowerCase(Locale.ROOT)+" AFTER "+op+" ON "+table+(op.equals("UPDATE")?" WHEN ("+changed+")":"")+" BEGIN INSERT OR IGNORE INTO r30r1_research_measure SELECT p.run_id,'SOURCE_CHANGED:"+table+":"+op+":'||NEW.issue||':'||(SELECT generation FROM r30_live_source_revision WHERE singleton=1),'INPUT_INVALIDATION',p.policy_hash,'ORIGINAL_CANONICAL_INPUT',p.from_ordinal,p.through_ordinal,0,NULL,NULL,NULL,'{\"reason\":\"CANONICAL_CORRECTION_OR_IN_RANGE_BACKFILL\",\"table\":\""+table+"\"}' FROM r30r1_analysis_run p WHERE (p.status='"+ResearchInputPinR30R1.STATUS+"'"+(op.equals("UPDATE")?" OR p.status='ONLINE_MODEL_ROLE_REGISTERED'":"")+") AND ("+affected+"); END");
    }return out.toArray(new String[0]);}
    public static String[] legacyDrawTriggerSqlR31(){List<String> out=new ArrayList<>();for(String sql:legacySql())if(sql.contains(" ON draws"))out.add(sql);return out.toArray(new String[0]);}
    public static String[] sourceDrawTriggerSqlR31(){List<String> out=new ArrayList<>();for(String sql:sourceTriggerSqlR31())if(sql.contains(" ON draws"))out.add(sql);return out.toArray(new String[0]);}
    private static String[] previousTriggerSqlR31(){List<String> out=new ArrayList<>();for(String table:new String[]{"draws","r27_history_import"})for(String op:new String[]{"INSERT","UPDATE","DELETE"}){
        String changed="NEW.issue IS NOT OLD.issue";if(table.equals("draws")){changed+=" OR NEW.open_time IS NOT OLD.open_time OR NEW.dirty IS NOT OLD.dirty";for(int n=1;n<=10;n++)changed+=" OR NEW.n"+n+" IS NOT OLD.n"+n;}else changed+=" OR NEW.ordinal IS NOT OLD.ordinal OR NEW.numbers IS NOT OLD.numbers OR NEW.status IS NOT OLD.status";
        out.add("CREATE TRIGGER IF NOT EXISTS r30r1_pin_"+table+'_'+op.toLowerCase(Locale.ROOT)+" AFTER "+op+" ON "+table+(op.equals("UPDATE")?" WHEN ("+changed+")":"")+" BEGIN INSERT INTO r31_source_event(source_name,operation,old_ordinal,new_ordinal,recorded_at) VALUES('"+table+"','"+op+"',"+(op.equals("INSERT")?"NULL":ResearchInputPinR30R1.ordinal("OLD"))+","+(op.equals("DELETE")?"NULL":ResearchInputPinR30R1.ordinal("NEW"))+",CAST(strftime('%s','now') AS INTEGER)*1000); END");
    }return out.toArray(new String[0]);}
    private static String knownAtTriggerSqlR31(){return "CREATE TRIGGER IF NOT EXISTS r31_pin_draw_known_at_update AFTER UPDATE OF first_seen_at ON draws WHEN NEW.first_seen_at IS NOT OLD.first_seen_at BEGIN UPDATE r30_live_source_revision SET generation=generation+1 WHERE singleton=1; INSERT INTO r31_source_event(source_name,operation,old_ordinal,new_ordinal,recorded_at) VALUES('draws','UPDATE',"+ResearchInputPinR30R1.ordinal("OLD")+","+ResearchInputPinR30R1.ordinal("NEW")+",CAST(strftime('%s','now') AS INTEGER)*1000); END";}
    static String[] sourceTriggerSqlR31(){List<String> out=new ArrayList<>(Arrays.asList(previousTriggerSqlR31()));out.add(knownAtTriggerSqlR31());return out.toArray(new String[0]);}
    public static String[] schemaObjectsR31(){List<String> out=new ArrayList<>();for(String sql:DDL)out.add(name(sql));for(String sql:sourceTriggerSqlR31())out.add(name(sql));return out.toArray(new String[0]);}
    public static String inspectUpgradeR31(SQLiteDatabase db){
        int old=0,fresh=0,missing=0;Map<String,String> prior=new LinkedHashMap<>();for(String sql:legacySql())prior.put(name(sql),sql);
        for(String expected:previousTriggerSqlR31()){String observed=actual(db,name(expected));if(observed==null){missing++;continue;}if(canonical(expected).equals(canonical(observed)))fresh++;else if(prior.containsKey(name(expected))&&canonical(prior.get(name(expected))).equals(canonical(observed)))old++;else return "CONFLICT";}
        String known=actual(db,name(knownAtTriggerSqlR31()));boolean exact=known!=null&&canonical(knownAtTriggerSqlR31()).equals(canonical(known));if(known!=null&&!exact)return "CONFLICT";
        if(old==4&&missing==2&&known==null)return "OLD_EXACT";if(fresh==6)return exact?"R31_EXACT":"R31_V1_EXACT";if(missing==6&&known==null)return "EMPTY";return "CONFLICT";
    }
    /** Only the existing schema writer/Domain cutover wrapper calls this. No callback into Pin.ensure. */
    public static void ensureSchemaR31(SQLiteDatabase db){if(!db.inTransaction())throw new IllegalStateException("R31_SOURCE_SCHEMA_WRITER_REQUIRED");String shape=inspectUpgradeR31(db);if(shape.equals("R31_EXACT")||shape.equals("R31_V1_EXACT"))for(String table:new String[]{EVENT,MEMBER,FENCE})if(actual(db,table)==null)throw new IllegalStateException("R31_SOURCE_EVIDENCE_SCHEMA_MISSING:"+table);if(shape.equals("CONFLICT"))throw new IllegalStateException("R31_SOURCE_TRIGGER_CONFLICT");for(String expected:DDL){String observed=actual(db,name(expected));if(observed!=null&&!canonical(expected).equals(canonical(observed)))throw new IllegalStateException("R31_SOURCE_SCHEMA_CONFLICT:"+name(expected));}for(String sql:DDL)db.execSQL(sql);if(shape.equals("OLD_EXACT"))for(String sql:legacySql())db.execSQL("DROP TRIGGER "+name(sql));for(String sql:sourceTriggerSqlR31())db.execSQL(sql);if(!readySchemaR31(db))throw new IllegalStateException("R31_SOURCE_SCHEMA_NOT_READY");}
    public static boolean readySchemaR31(SQLiteDatabase db){if(com.ethk3.novaonepro.TransactionSchemaCacheR941.readyCommitted(db,"R31_SOURCE_JOURNAL2_KNOWN_AT"))return true;for(String sql:DDL)if(!canonical(sql).equals(canonical(actual(db,name(sql)))))return false;if(!"R31_EXACT".equals(inspectUpgradeR31(db)))return false;com.ethk3.novaonepro.TransactionSchemaCacheR941.markCommitted(db,"R31_SOURCE_JOURNAL2_KNOWN_AT");return true;}
    public static long currentFence(SQLiteDatabase db){ResearchInputPinR30R1.requireReady(db);try(Cursor q=db.rawQuery("SELECT COALESCE((SELECT seq FROM sqlite_sequence WHERE name='r31_source_event'),0)",null)){return q.moveToFirst()?q.getLong(0):-1;}}
    public static void requireFence(SQLiteDatabase db,long expected){ResearchInputPinR30R1.requireReady(db);if(expected<0||!SchemaR27.liveSourceTriggersValid(db)||currentFence(db)!=expected)throw new ResearchStoreR30R1.StaleResearch();}
    private static String key(String value){if(value==null||value.isEmpty()||value.length()>512)throw new IllegalArgumentException("R31_SOURCE_IDENTITY");return value;}
    private static String operation(String value){if(!"INSERT".equals(value)&&!"REPLACE".equals(value))throw new IllegalArgumentException("R31_IMPORT_CHANGE_OPERATION");return value;}
    public static void appendImportMembers(ResearchWriteQueueR27.Batch batch,String memberSet,String op,List<Long> ordinals){key(memberSet);operation(op);if(ordinals==null||ordinals.size()>32)throw new IllegalArgumentException("R31_SOURCE_MEMBER_CHUNK");for(Long ordinal:ordinals){if(ordinal==null)throw new IllegalArgumentException("R31_SOURCE_MEMBER_ORDINAL");batch.add("INSERT OR IGNORE INTO r31_source_member(member_set,operation,ordinal) VALUES(?,?,?)",memberSet,op,ordinal);}}
    public static void appendPublishedImport(ResearchWriteQueueR27.Batch batch,String event,String memberSet,String op,long first,long through,long now){key(event);key(memberSet);operation(op);if(first>through)throw new IllegalArgumentException("R31_SOURCE_RANGE");String eventOp="IMPORT_"+op;
        batch.add("INSERT INTO r31_source_event(event_key,source_name,operation,first_ordinal,through_ordinal,member_set,recorded_at) SELECT ?,'CSV_IMPORT',?,?,?,?,? WHERE EXISTS(SELECT 1 FROM r31_source_member WHERE member_set=? AND operation=? LIMIT 1) AND NOT EXISTS(SELECT 1 FROM r31_source_event WHERE event_key=?)",event,eventOp,first,through,memberSet,now,memberSet,op,event);
        batch.guardAfter(db->{try(Cursor q=db.rawQuery("SELECT operation,member_set,first_ordinal,through_ordinal FROM r31_source_event WHERE event_key=?",new String[]{event})){if(!q.moveToFirst()||!eventOp.equals(q.getString(0))||!memberSet.equals(q.getString(1))||q.getLong(2)!=first||q.getLong(3)!=through)throw new IllegalStateException("R31_SOURCE_EVENT_IDENTITY_OR_MEMBERS");}return 0;});
    }
    static String alias(String value){if(value==null||!value.matches("[A-Za-z][A-Za-z0-9_]*"))throw new IllegalArgumentException("R31_SOURCE_SQL_ALIAS");return value;}
    static String captureSql(String a){return "COALESCE((SELECT f.event_seq FROM r31_pin_source_fence f WHERE f.run_id="+a+".run_id),0)";}
    static String affectedSql(String a){alias(a);String fence=captureSql(a),range=" BETWEEN "+a+".from_ordinal AND "+a+".through_ordinal",pin=a+".status='INPUT_DATASET_PINNED'",online=a+".status='ONLINE_MODEL_ROLE_REGISTERED'";
        return "("+pin+" AND EXISTS(SELECT 1 FROM r31_source_event e WHERE e.operation='INSERT' AND e.new_ordinal"+range+" AND e.event_seq>"+fence+")) OR (("+pin+" OR "+online+") AND (EXISTS(SELECT 1 FROM r31_source_event e WHERE e.operation='UPDATE' AND e.new_ordinal"+range+" AND e.event_seq>"+fence+") OR EXISTS(SELECT 1 FROM r31_source_event e WHERE e.operation='UPDATE' AND e.old_ordinal"+range+" AND e.event_seq>"+fence+"))) OR ("+online+" AND EXISTS(SELECT 1 FROM r31_source_event e WHERE e.operation='DELETE' AND e.old_ordinal"+range+" AND e.event_seq>"+fence+")) OR EXISTS(SELECT 1 FROM r31_source_event e WHERE e.source_name='CSV_IMPORT' AND e.event_seq>"+fence+" AND ("+pin+" OR ("+online+" AND e.operation='IMPORT_REPLACE')) AND EXISTS(SELECT 1 FROM r31_source_member m WHERE m.member_set=e.member_set AND m.operation=CASE e.operation WHEN 'IMPORT_INSERT' THEN 'INSERT' ELSE 'REPLACE' END AND m.ordinal"+range+"))";
    }
    static String deleteTouchesSql(String a){alias(a);return "EXISTS(SELECT 1 FROM r31_source_event e WHERE e.operation='DELETE' AND e.old_ordinal BETWEEN "+a+".from_ordinal AND "+a+".through_ordinal AND e.event_seq>"+captureSql(a)+")";}
}
