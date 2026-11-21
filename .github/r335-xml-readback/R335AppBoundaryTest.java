package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.CancellationSignal;
import android.util.Xml;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ethk3.novaonepro.strategyv2.RepairAnalysisAndroidFixture;
import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

/** Disposable signed integration test. Emit predefined categories only, with no preference content. */
public final class R335AppBoundaryTest {
    private static void record(String k, String v) {
        System.out.println("R335_BOUNDED " + k + " " + v);
    }

    @Test public void explicitStartAndFrameworkPersistence() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        ServiceHeartbeatStore service = new ServiceHeartbeatStore(context);
        service.setUserEnabled(true);
        service.markUiStartRequested("r335_disposable_probe");
        SharedPreferences prefs = context.getSharedPreferences("v20_service_runtime_light", Context.MODE_PRIVATE);
        String status = prefs.getString(EnabledWindowJournalR335.STATUS_KEY, "");
        record("recorded", "RECORDED".equals(status) ? "YES" : status.isEmpty() ? "ABSENT" : "OTHER");
        record("enabled", prefs.getBoolean("user_enabled", false) ? "YES" : "NO");
        String original = prefs.getString(EnabledWindowJournalR335.PREF_KEY, "");
        try {
            boolean valid = !EnabledWindowJournalR335.parse(original).events.isEmpty();
            record("memoryJournal", valid ? "VALID" : "EMPTY");
        } catch (IllegalArgumentException bad) {
            record("memoryJournal", "INVALID");
        }
        File file = new File(context.getApplicationInfo().dataDir, "shared_prefs/v20_service_runtime_light.xml");
        try (FileInputStream f = new FileInputStream(file)) {
            boolean binary = f.read() == 'A' && f.read() == 'B' && f.read() == 'X' && f.read() == 0;
            record("frameworkFormat", binary ? "ANDROID_BINARY_XML" : "TEXT_OR_OTHER");
        }
        record("memoryLength", Integer.toString(original.length()));
        record("memoryLines", Integer.toString(count(original, '\n')));
        try (FileInputStream stream = new FileInputStream(file)) {
            XmlPullParser test=Xml.newPullParser();
            test.setInput(stream, "UTF-8");
            for(int event=test.next();event!=XmlPullParser.END_DOCUMENT;event=test.next()) {
                if(event==XmlPullParser.START_TAG && test.getDepth()==2 && EnabledWindowJournalR335.PREF_KEY.equals(test.getAttributeValue(null,"name"))) {
                    String value=test.nextText();
                    compare("pull",original,value);
                }
            }
        }
        try (FileInputStream stream = new FileInputStream(file)) {
            final StringBuilder selected = new StringBuilder();
            org.xml.sax.helpers.DefaultHandler handler = new org.xml.sax.helpers.DefaultHandler() {
                boolean journal; int depth;
                @Override public void startElement(String uri,String local,String q,org.xml.sax.Attributes a) {
                    depth++;
                    if(depth==2)journal=EnabledWindowJournalR335.PREF_KEY.equals(a.getValue("name"));
                }
                @Override public void characters(char[] c,int o,int n) { if(depth==2 && journal)selected.append(c,o,n); }
                @Override public void endElement(String u,String l,String q) { if(depth==2) journal=false;depth--; }
            };
            Xml.parse(stream,Xml.Encoding.UTF_8,handler);
            compare("sax",original,selected.toString());
        }
        try (FileInputStream f = new FileInputStream(file)) {
            XmlPullParser xml = Xml.newPullParser();
            xml.setInput(f, "UTF-8");
            EnabledWindowCaptureProviderR335.projection(xml);
            record("projection", "PASS");
        } catch (Exception error) {
            String reason = String.valueOf(error.getMessage());
            String category = error instanceof org.xmlpull.v1.XmlPullParserException ? "XML_PARSE" : "OTHER";
            for (String known : new String[]{"PREF_ROOT","JOURNAL_TYPE_OR_DUPLICATE","STATUS_TYPE_OR_DUPLICATE","ASSIGNMENT_TYPE_OR_DUPLICATE","ASSIGNMENT_VALUE","ASSIGNMENT_INCOMPLETE","ASSIGNMENT_HISTORY_MISMATCH","JOURNAL_TRUNCATED","JOURNAL_HASH","JOURNAL_HEADER","JOURNAL_IDENTITY","JOURNAL_ELAPSED_REVERSED","JOURNAL_CLOCK_DISCONTINUITY"}) if (known.equals(reason)) category = known;
            record("projection", category);
        }
    }

    private static int count(String s, char chr) {
        int n=0;for(int i=0;i<s.length();i++)if(s.charAt(i)==chr)n++;return n;
    }
    private static void compare(String kind,String original,String value){
        record(kind+"Length",Integer.toString(value.length()));
        record(kind+"Lines",Integer.toString(count(value,'\n')));
        record(kind+"Carriage",Integer.toString(count(value,'\r')));
        record(kind+"Exact",original.equals(value)?"YES":"NO");
        record(kind+"Escaped",value.contains("\\n")?"YES":"NO");
        record(kind+"LiteralEntity",value.contains("&#")?"YES":"NO");
        record(kind+"Trimmed",value.equals(original.trim())?"YES":"NO");
        record(kind+"Header",value.startsWith(EnabledWindowJournalR335.FORMAT)?"YES":"NO");
        record(kind+"AppendLfExact",(value+"\n").equals(original)?"YES":"NO");
    }
}
