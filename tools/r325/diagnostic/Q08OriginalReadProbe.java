package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.sql.*;
import java.util.*;

/** Executes actual Q08EvidenceReader SQL against native SQLite; fixture outcomes
 * are not experimental results. Android cursor cancellation remains device-only. */
public final class Q08OriginalReadProbe {
    static int checks,failures;
    static void check(boolean ok,String name){checks++;if(!ok){failures++;System.out.println("FAIL "+name);}}
    static Map<String,Object> map(Object x){return Q08ReportBoundaryProbe.map(x);}
    static void execute(Connection c,String sql)throws Exception{try(Statement s=c.createStatement()){s.execute(sql);}}
    static long scalar(Connection c,String sql)throws Exception{try(Statement s=c.createStatement();ResultSet r=s.executeQuery(sql)){r.next();return r.getLong(1);}}
    static void store(Connection c,String panel,String key,String text,long at)throws Exception{
        execute(c,"PRAGMA query_only=OFF");
        try(PreparedStatement p=c.prepareStatement("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)")){p.setString(1,panel);p.setString(2,key);p.setString(3,text);p.setLong(4,at);p.executeUpdate();}
        execute(c,"PRAGMA query_only=ON");
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")){
            execute(c,"CREATE TABLE r27_chart_cache(panel_id TEXT NOT NULL,key TEXT NOT NULL,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");
            SQLiteDatabase db=new SQLiteDatabase(c);Q08ReportBoundaryProbe.Fixture f=new Q08ReportBoundaryProbe.Fixture();
            for(Map.Entry<String,String> r:f.rows.entrySet())store(c,"r31-risk-contract-trial",r.getKey(),r.getValue(),2000L);
            f.nulls.put("retainedOriginalDetail","x".repeat(20000));f.seal();String nullText=JsonR27.write(f.nulls);
            store(c,"r30","q08",nullText,10000L);
            Map<String,Object> factory=new LinkedHashMap<>(f.factory);factory.put("largeOriginalDetails","x".repeat(20000));
            store(c,"r30","q08-factory-r31",JsonR27.write(factory),10000L);
            for(int i=0;i<100;i++)store(c,"r30-charts","noise-"+i,"{}",20000L+i);
            long changes=scalar(c,"SELECT total_changes()"),count=scalar(c,"SELECT count(*) FROM r27_chart_cache");
            Q08EvidenceReaderR325.capture(db,f.data,true,null);
            Map<String,Object> originals=map(f.data.get("q08FactoryOriginalsR325")),nullView=map(f.data.get("syntheticControl")),factoryView=map(f.data.get("factoryCapabilityExperiment"));
            check("READ_OK_NOT_A_HEALTH_VERDICT".equals(originals.get("status")),"compact originals read with Android string-bind SQL");
            check(((Number)originals.get("rowCount")).intValue()==120,"all 120 compact originals retained");
            check(Objects.equals(originals.get("originalsSha256"),f.registry.get("validationOriginalsSha256")),"native row set hash exactly matches registered producer validation");
            check(((Number)originals.get("originalBytes")).longValue()<=262144,"original trial set retains the existing 256KiB resource bound");
            check("READ_OK_NOT_A_HEALTH_VERDICT".equals(nullView.get("status"))&&nullText.length()>16384,"legacy null and power original over UI 16KiB limit is read");
            check(StatsR27.sha256(nullText).equals(nullView.get("originalSha256")),"legacy original hash preserves exact stored bytes");
            check("REFERENCE_ONLY_ORIGINAL_BYTE_BOUND".equals(factoryView.get("status"))&&factoryView.get("payload")==null,"large factory display is a reference and never truncated JSON");
            check("PASS".equals(f.row().get("status")),"real dedicated read reaches real report consumer despite 100 newer UI caches");
            check(scalar(c,"SELECT total_changes()")==changes&&scalar(c,"SELECT count(*) FROM r27_chart_cache")==count,"read adapter changes no database rows under SQLite query_only");
            check(db.queries.size()==3&&db.queries.stream().allMatch(q->q.startsWith("SELECT ")),"exactly three read-only source queries");
            check(db.queries.get(0).contains("LIMIT 121"),"one excess original is detected before accepting complete set");
            Q08EvidenceReaderR325.capture(db,f.data,false,null);check("SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK".equals(map(f.data.get("q08FactoryOriginalsR325")).get("sourceScope")),"pre-35 live read scope is explicit rather than falsely pinned");
            store(c,"r31-risk-contract-trial",f.registration.binding+":GAPS:710099",f.rows.get(f.rows.firstKey()),2000L);
            Q08EvidenceReaderR325.capture(db,f.data,true,null);check("UNKNOWN_Q08_ORIGINAL_READ".equals(map(f.data.get("q08FactoryOriginalsR325")).get("status")),"121st original is refused rather than silently cut to 120");
            execute(c,"PRAGMA query_only=OFF");try(PreparedStatement p=c.prepareStatement("DELETE FROM r27_chart_cache WHERE panel_id='r31-risk-contract-trial' AND key=?")){p.setString(1,f.registration.binding+":GAPS:710099");p.executeUpdate();}execute(c,"PRAGMA query_only=ON");
            String first=f.rows.firstKey();store(c,"r31-risk-contract-trial",first,"x".repeat(16385),2000L);Q08EvidenceReaderR325.capture(db,f.data,true,null);
            check("UNKNOWN_Q08_ORIGINAL_READ".equals(map(f.data.get("q08FactoryOriginalsR325")).get("status")),"oversized compact trial record cannot become a missing zero");
            store(c,"r31-risk-contract-trial",first,f.rows.get(first)+" ",2000L);Q08EvidenceReaderR325.capture(db,f.data,true,null);
            check("UNKNOWN_SYNTHETIC_READ_SCOPE_MISMATCH".equals(f.row().get("status")),"changed original bytes between reads cannot be combined into PASS");
            store(c,"r31-risk-contract-trial",first,f.rows.get(first),2000L);
            store(c,"r30","q08","x".repeat(262145),10000L);Q08EvidenceReaderR325.capture(db,f.data,true,null);
            check("REFERENCE_ONLY_ORIGINAL_BYTE_BOUND".equals(map(f.data.get("syntheticControl")).get("status"))&&!"PASS".equals(f.row().get("status")),"oversized legacy evidence is explicit and blocks Q08");
            execute(c,"PRAGMA query_only=OFF");execute(c,"DELETE FROM r27_chart_cache WHERE panel_id='r30' AND key='q08'");execute(c,"PRAGMA query_only=ON");Q08EvidenceReaderR325.capture(db,f.data,true,null);
            check("UNKNOWN_SOURCE_MISSING".equals(map(f.data.get("syntheticControl")).get("status"))&&!"PASS".equals(f.row().get("status")),"absent fixed key cannot be filled from unrelated caches");
            execute(c,"PRAGMA query_only=OFF");execute(c,"DROP TABLE r27_chart_cache");execute(c,"PRAGMA query_only=ON");Q08EvidenceReaderR325.capture(db,f.data,true,null);
            check("UNKNOWN_Q08_ORIGINAL_READ".equals(map(f.data.get("q08FactoryOriginalsR325")).get("status"))&&!"PASS".equals(f.row().get("status")),"native SQL source failure remains UNKNOWN without successful fallback");
        }
        System.out.println("RESULT checks="+checks+" failures="+failures);if(failures>0)System.exit(1);
    }
}
