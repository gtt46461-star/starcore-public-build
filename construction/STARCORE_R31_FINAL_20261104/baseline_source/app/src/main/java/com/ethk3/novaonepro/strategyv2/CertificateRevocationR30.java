package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Source corrections revoke authority in their existing writer transaction; evidence and models stay intact. */
public final class CertificateRevocationR30 {
    private CertificateRevocationR30() {}
    public static final String SCOPE="CONSERVATIVE_CURRENT_CERTIFICATES_DEPENDENCY_GRAPH_UNAVAILABLE";
    static final String NOW="CAST((julianday('now')-2440587.5)*86400000 AS INTEGER)";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r30_certificate_revocation_event(protocol TEXT NOT NULL,issue TEXT NOT NULL,cause TEXT NOT NULL,event_key TEXT NOT NULL,scope TEXT NOT NULL,affected INTEGER NOT NULL,detected_at INTEGER NOT NULL,PRIMARY KEY(protocol,issue,cause,event_key))",
        "CREATE TABLE IF NOT EXISTS r30_revoked_policy(protocol TEXT NOT NULL,policy_key TEXT NOT NULL,issue TEXT NOT NULL,cause TEXT NOT NULL,revoked_at INTEGER NOT NULL,PRIMARY KEY(protocol,policy_key))",
        "CREATE TABLE IF NOT EXISTS r30_revoked_certificate(protocol TEXT NOT NULL,certificate_key TEXT NOT NULL,policy_key TEXT NOT NULL,issue TEXT NOT NULL,cause TEXT NOT NULL,evidence_digest TEXT NOT NULL,gates_json TEXT NOT NULL,gate_version TEXT NOT NULL,issued_at INTEGER NOT NULL,expires_at INTEGER NOT NULL,old_status TEXT NOT NULL,revoked_at INTEGER NOT NULL,PRIMARY KEY(protocol,certificate_key,issue,cause,evidence_digest))"
    };
    /** Called at R27 schema setup and again when the independently scheduled SU2 schema becomes available. */
    public static void ensure(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("R30_REVOCATION_WRITER_REQUIRED");
        for(String ddl:DDL)db.execSQL(ddl);
        if(table(db,"r27_certificate"))install(db,"R27");
        if(table(db,"su2_certificate")) {
            db.execSQL("CREATE TABLE IF NOT EXISTS r26_certificate_revocation(certificate_id TEXT PRIMARY KEY,reason TEXT NOT NULL,revoked_at INTEGER NOT NULL)");
            install(db,"SU2");
        }
    }
    static void install(SQLiteDatabase db,String protocol) {
        String prefix="r30_revoke_"+protocol.toLowerCase(Locale.ROOT);
        if(table(db,"draws")) {
            Set<String> columns=columns(db,"draws");List<String> semantic=new ArrayList<>();
            for(String name:new String[]{"issue","dirty","open_time","n1","n2","n3","n4","n5","n6","n7","n8","n9","n10"})if(columns.contains(name))semantic.add(name);
            if(!semantic.isEmpty())sourceTrigger(db,protocol,prefix+"_draw_semantics",semantic,"DRAW_NUMBERS_TIME_OR_STATUS_CHANGED");
            List<String> hashes=new ArrayList<>();for(String name:new String[]{"draw_hash_v2","draw_hash_version"})if(columns.contains(name))hashes.add(name);
            if(!hashes.isEmpty())sourceTrigger(db,protocol,prefix+"_draw_hash",hashes,"DRAW_HASH_CHANGED");
        }
        if(protocol.equals("R27")) {
            for(String operation:new String[]{"INSERT","UPDATE OF status"}) {
                String suffix=operation.equals("INSERT")?"insert":"update";
                db.execSQL("CREATE TRIGGER IF NOT EXISTS "+prefix+"_reissue_"+suffix+" AFTER "+operation+" ON r27_certificate WHEN NEW.status<>'REVOKED' AND EXISTS(SELECT 1 FROM r30_revoked_policy p WHERE p.protocol='R27' AND p.policy_key=NEW.policy_hash) BEGIN "
                        +archiveR27(db,"NEW.policy_hash","(SELECT issue FROM r30_revoked_policy WHERE protocol='R27' AND policy_key=NEW.policy_hash)","'REISSUE_WITH_REVOKED_SOURCE'",NOW)
                        +" UPDATE r27_certificate SET status='REVOKED' WHERE policy_hash=NEW.policy_hash; END");
            }
        }else db.execSQL("CREATE TRIGGER IF NOT EXISTS "+prefix+"_reissue AFTER INSERT ON su2_certificate WHEN EXISTS(SELECT 1 FROM r30_revoked_policy p WHERE p.protocol='SU2' AND p.policy_key=NEW.genome_key) BEGIN INSERT OR IGNORE INTO r26_certificate_revocation(certificate_id,reason,revoked_at) SELECT NEW.certificate_id,'R30_SOURCE_REVOKED_POLICY:'||p.cause,"+NOW+" FROM r30_revoked_policy p WHERE p.protocol='SU2' AND p.policy_key=NEW.genome_key; END");
        db.execSQL("CREATE INDEX IF NOT EXISTS "+prefix+"_expires ON "+(protocol.equals("R27")?"r27_certificate(status,expires_at)":"su2_certificate(expires_at)"));
    }
    static void sourceTrigger(SQLiteDatabase db,String protocol,String name,List<String> fields,String cause) {
        StringJoiner changed=new StringJoiner(" OR "),key=new StringJoiner("||'|'||");
        for(String field:fields){changed.add("NEW."+field+" IS NOT OLD."+field);key.add("coalesce(CAST(OLD."+field+" AS TEXT),'NULL')||'>'||coalesce(CAST(NEW."+field+" AS TEXT),'NULL')");}
        String body=statements(db,protocol,"OLD.issue",quote(cause),key.toString(),NOW);
        db.execSQL("CREATE TRIGGER IF NOT EXISTS "+name+" AFTER UPDATE OF "+String.join(",",fields)+" ON draws WHEN "+changed+" BEGIN "+body+" END");
    }
    /** Invoked by SettlementEngine's hook from the canonicalizer's already active write transaction. */
    public static void revokeWithin(SQLiteDatabase db,String issue,String cause,String eventKey) {
        if(!db.inTransaction())throw new IllegalStateException("R30_REVOCATION_TRANSACTION_REQUIRED");
        ensure(db);
        for(String protocol:new String[]{"R27","SU2"})if(table(db,protocol.equals("R27")?"r27_certificate":"su2_certificate")) {
            for(String statement:statementParts(db,protocol,quote(issue),quote(cause),quote(eventKey),String.valueOf(System.currentTimeMillis())))db.execSQL(statement);
        }
    }
    static String statements(SQLiteDatabase db,String protocol,String issue,String cause,String event,String now) {
        return String.join("",statementParts(db,protocol,issue,cause,event,now));
    }
    static List<String> statementParts(SQLiteDatabase db,String protocol,String issue,String cause,String event,String now) {
        String live=protocol.equals("R27")?"status='ACTIVE' AND expires_at>"+now:"expires_at>"+now+" AND NOT EXISTS(SELECT 1 FROM r26_certificate_revocation r WHERE r.certificate_id=su2_certificate.certificate_id)";
        String table=protocol.equals("R27")?"r27_certificate":"su2_certificate",policy=protocol.equals("R27")?"policy_hash":"genome_key";
        String journal="INSERT OR IGNORE INTO r30_certificate_revocation_event(protocol,issue,cause,event_key,scope,affected,detected_at) SELECT "+quote(protocol)+","+issue+","+cause+","+event+","+quote(SCOPE)+",count(*),"+now+" FROM "+table+" WHERE "+live+";";
        String blocked="INSERT OR IGNORE INTO r30_revoked_policy(protocol,policy_key,issue,cause,revoked_at) SELECT DISTINCT "+quote(protocol)+","+policy+","+issue+","+cause+","+now+" FROM "+table+" WHERE "+live+";";
        if(protocol.equals("R27"))return Arrays.asList(journal,blocked,archiveR27(db,null,issue,cause,now),"UPDATE r27_certificate SET status='REVOKED' WHERE "+live+";");
        return Arrays.asList(journal,blocked,"INSERT OR IGNORE INTO r26_certificate_revocation(certificate_id,reason,revoked_at) SELECT certificate_id,'R30_SOURCE_REVISION:'||"+cause+","+now+" FROM su2_certificate WHERE "+live+";");
    }
    static String archiveR27(SQLiteDatabase db,String onlyPolicy,String issue,String cause,String now) {
        String version=columns(db,"r27_certificate").contains("gate_version")?"gate_version":"'LEGACY_UNVERSIONED'";
        return "INSERT OR IGNORE INTO r30_revoked_certificate(protocol,certificate_key,policy_key,issue,cause,evidence_digest,gates_json,gate_version,issued_at,expires_at,old_status,revoked_at) SELECT 'R27',policy_hash,policy_hash,"+issue+","+cause+",evidence_digest,gates_json,"+version+",issued_at,expires_at,status,"+now+" FROM r27_certificate WHERE "+(onlyPolicy==null?"status='ACTIVE' AND expires_at>"+now:"policy_hash="+onlyPolicy)+";";
    }
    static String quote(String text){if(text==null)throw new IllegalArgumentException("R30_REVOCATION_FACT_REQUIRED");return "'"+text.replace("'","''")+"'";}
    public static boolean blockedPolicy(SQLiteDatabase db,String protocol,String policy) {
        if(!table(db,"r30_revoked_policy"))return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM r30_revoked_policy WHERE protocol=? AND policy_key=?",new String[]{protocol,policy})){return c.moveToFirst();}
    }
    static Set<String> columns(SQLiteDatabase db,String table){Set<String> names=new HashSet<>();try(Cursor c=db.rawQuery("PRAGMA table_info("+table+")",null)){while(c.moveToNext())names.add(c.getString(1));}return names;}
    static boolean table(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}}
    public static Map<String,Object> report(SQLiteDatabase db) {
        Map<String,Object> out=new LinkedHashMap<>();out.put("scope",SCOPE);out.put("deviceRevokeToRejectMs",null);
        if(!table(db,"r30_certificate_revocation_event")){out.put("status","UNKNOWN_SOURCE_MISSING");out.put("events",null);return out;}
        long n=count(db,"SELECT count(*) FROM r30_certificate_revocation_event");out.put("status",n==0?"NOT_RUN":"RECORDED");out.put("events",n);out.put("revokedPolicies",count(db,"SELECT count(*) FROM r30_revoked_policy"));out.put("retainedOriginalR27Certificates",count(db,"SELECT count(*) FROM r30_revoked_certificate"));
        out.put("recovery","Original evidence remains archived; a changed source requires a new pre-registered FUTURE binding, never an alpha index reset or immediate reissue from old evidence.");
        return out;
    }
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
}
