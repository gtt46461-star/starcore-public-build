package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.xml.stream.*;
import org.xmlpull.v1.XmlPullParser;

public final class SourceClockProbe {
    static int checks;
    static void check(boolean ok, String name) {
        if (!ok) throw new AssertionError(name);
        checks++;
    }
    static Cursor cursor(Object... columns) {
        return (Cursor)Proxy.newProxyInstance(Cursor.class.getClassLoader(),new Class<?>[]{Cursor.class},(p,m,a)->{
            switch(m.getName()) {
                case "moveToFirst": return columns.length>0;
                case "moveToNext": return false;
                case "close": return null;
            }
            Object value=columns[(Integer)a[0]];
            switch(m.getName()) {
                case "getLong": return ((Number)value).longValue();
                case "getInt": return ((Number)value).intValue();
                case "getDouble": return ((Number)value).doubleValue();
                case "getString": return value==null?null:value.toString();
                case "isNull": return value==null;
                case "getType": return value instanceof Double?2:value instanceof Number?1:value==null?0:3;
                default: throw new AssertionError(m.toString());
            }
        });
    }

    static final String TARGET="202611181001", BASE="202611181000";
    static String hash(String s){return FrozenFeedbackProofR335.sha(s);}
    static final class Store extends SQLiteDatabase {
        final Models.Draw draw=new Models.Draw();
        final String record=hash("frozen"), history=hash("history"), snapshot=hash("snapshot"), action=hash("action");
        long seen=System.currentTimeMillis()-2000, frozen=seen-1000;
        ContentValues label, shadow;
        boolean missing, duplicate;
        int inserts, updates;
        Store() {
            draw.issue=TARGET; draw.openTime=seen-100;
            for(int i=0;i<10;i++)draw.numbers[i]=i+1;
        }
        public boolean inTransaction(){return true;}
        public Cursor rawQuery(String sql,String[] args) {
            if(sql.startsWith("SELECT first_seen_at FROM ")) {
                if(!sql.contains("draw_hash_v2=?")||args.length!=3||!DrawHashR36.sha256V2(draw).equals(args[2]))
                    throw new AssertionError("unbound original");
                return missing?cursor():cursor(seen);
            }
            if(sql.startsWith("SELECT base_issue,position"))
                return cursor(BASE,1,1,2,3,.4d,frozen-1,frozen,record,1,1,0,history,snapshot,action,V51Repository.GAME_CODE,BASE);
            if(sql.startsWith("SELECT record_hash,base_issue"))
                return cursor(label.get("record_hash"),BASE,1,1,2,3,1,1,label.get("settled_at"));
            throw new AssertionError(sql);
        }
        public long insertWithOnConflict(String table,String empty,ContentValues values,int conflict){
            inserts++;if(duplicate)return -1;label=values;return 1;
        }
        public int update(String table,ContentValues values,String predicate,String[] args){updates++;shadow=values;return 1;}
        OosAfterDrawWriterR36.LabelResult settle() throws Exception {
            Constructor<SettlementEngine.OwnerToken> c=SettlementEngine.OwnerToken.class.getDeclaredConstructor(String.class,long.class);
            c.setAccessible(true);
            return OosAfterDrawWriterR36.settleFrozenWithinTransaction(this,c.newInstance(TARGET,System.currentTimeMillis()),draw);
        }
        Object[] frozenRow(long settled,long completed){
            String drawHash=DrawHashR36.sha256V2(draw);
            String provenance=hash("R36_OOS_PROVENANCE|"+record+"|"+snapshot+"|"+action+"|"+history+"|"+drawHash+"|"+seen);
            Object[] row={TARGET,V51Repository.GAME_CODE,BASE,BASE,record,snapshot,action,history,frozen,draw.openTime,
                draw.openTime,seen,1,1,2,3,1,1,hash(provenance+"|1|1"),provenance,.4d,settled,drawHash,1,2,3,4,5,6,7,8,9,10,completed};
            return row;
        }
    }

    static void productionClocks() throws Exception {
        Store live=new Store();
        OosAfterDrawWriterR36.LabelResult written=live.settle();
        check(written.success&&!written.replay&&live.updates==1,"fresh label");
        check(written.labelSettledAt==(Long)live.label.get("settled_at"),"actual stored timestamp");
        live.duplicate=true;
        OosAfterDrawWriterR36.LabelResult replay=live.settle();
        check(replay.replay&&replay.labelSettledAt==written.labelSettledAt,"exact replay time");
        check(live.shadow.get("settled_at").equals(live.label.get("settled_at")),"shadow inherits original time");
        live.label.put("settled_at",live.seen-1);
        int count=live.updates;
        check("PIT_EXISTING_LABEL_TIME_INVALID".equals(live.settle().status)&&live.updates==count,"reject legacy early first-write");
        for(String test:List.of("open","seen","missing")){
            Store f=new Store();
            if(test.equals("open"))f.draw.openTime=System.currentTimeMillis()+60000;
            if(test.equals("seen"))f.seen=System.currentTimeMillis()+60000;
            f.missing=test.equals("missing");
            try{f.settle();throw new AssertionError(test);}
            catch(IllegalStateException e){check(e.getMessage().equals("R335_DRAW_NOT_READY_FOR_SETTLEMENT")&&f.inserts==0&&f.updates==0,test);}
        }
        Class<?> type=Class.forName("com.ethk3.novaonepro.strategyv2.RuntimeSchemaR26$FrozenR335");
        Constructor<?> reader=type.getDeclaredConstructor(Cursor.class);reader.setAccessible(true);
        Store f=new Store();
        check(reader.newInstance(cursor(f.frozenRow(f.seen,f.seen)))!=null,"inclusive native read");
        for(String name:List.of("before_seen","before_open","terminal_before_label","fraction","skipped_base")){
            Object[] row=f.frozenRow(f.seen,f.seen);
            switch(name){
                case "before_seen": row[21]=f.seen-1;break;
                case "before_open": row[10]=f.seen+1;break;
                case "terminal_before_label": row[33]=f.seen-1;break;
                case "fraction": row[21]=Double.valueOf(f.seen);break;
                default: row[2]="202611180999";row[3]=row[2];
            }
            try{reader.newInstance(cursor(row));throw new AssertionError(name);}
            catch(InvocationTargetException e){check(e.getCause() instanceof IllegalArgumentException,name);}
        }
    }

    // The JDK parser supplies the same pull events used by the production projection.
    static byte[] project(String input) throws Exception {
        XMLStreamReader stream=XMLInputFactory.newFactory().createXMLStreamReader(new StringReader(input));
        int[] depth={0},event={XmlPullParser.START_DOCUMENT};
        XmlPullParser parser=(XmlPullParser)Proxy.newProxyInstance(XmlPullParser.class.getClassLoader(),
            new Class<?>[]{XmlPullParser.class},(obj,method,args)->{
            switch(method.getName()){
                case "next":
                    if(event[0]==XmlPullParser.END_TAG)depth[0]--;
                    int t=stream.next();
                    if(t==XMLStreamConstants.START_ELEMENT){depth[0]++;event[0]=XmlPullParser.START_TAG;}
                    else if(t==XMLStreamConstants.END_ELEMENT)event[0]=XmlPullParser.END_TAG;
                    else if(t==XMLStreamConstants.END_DOCUMENT)event[0]=XmlPullParser.END_DOCUMENT;
                    else event[0]=XmlPullParser.TEXT;
                    return event[0];
                case "getDepth": return depth[0];
                case "getName": return stream.getLocalName();
                case "getAttributeValue": return stream.getAttributeValue(null,(String)args[1]);
                case "nextText": String result=stream.getElementText();event[0]=XmlPullParser.END_TAG;return result;
                default: throw new AssertionError(method.toString());
            }
        });
        return EnabledWindowCaptureProviderR335.projection(parser);
    }
    static void capture() throws Exception {
        String journal=EnabledWindowJournalR335.append("",true,1000,100,"a".repeat(32),"c".repeat(40));
        String xml="<map><string name='secret'>UNRELATED_VALUE</string><boolean name='user_enabled' value='true'/>"
            +"<string name='r335_enabled_assignment_journal'>"+journal+"</string>"
            +"<string name='r335_enabled_assignment_status'>RECORDED</string></map>";
        String projected=new String(project(xml),StandardCharsets.UTF_8);
        check(projected.contains(journal)&&!projected.contains("secret")&&!projected.contains("UNRELATED"),"three fields only");
        check(Arrays.equals(project(xml),project(projected)),"repeatable original projection");
        for(String bad:List.of(xml.replace("RECORDED","FAILED"),xml.replace("value='true'","value='false'"),
                xml.replace("</map>","<boolean name='user_enabled' value='true'/></map>"))){
            try{project(bad);throw new AssertionError("bad projection");}
            catch(IOException e){check(true,"reject incomplete, disagreement, duplicate");}
        }
        check(EnabledWindowCaptureProviderR335.allowedRead(2000,"r","/assignment",true),"shell read");
        check(!EnabledWindowCaptureProviderR335.allowedRead(10001,"r","/assignment",true),"ordinary uid");
        check(!EnabledWindowCaptureProviderR335.allowedRead(2000,"rw","/assignment",true),"write");
        check(!EnabledWindowCaptureProviderR335.allowedRead(2000,"r","/database",true),"alternate path");
        check(!EnabledWindowCaptureProviderR335.allowedRead(2000,"r","/assignment",false),"unrecognized uri");
    }
    public static void main(String[] args) throws Exception {
        productionClocks();capture();
        System.out.println("{\"status\":\"PASS\",\"productionAssertions\":"+checks+",\"scope\":\"JVM actual owners with cursor/ContentValues and XML adapters\"}");
    }
}
