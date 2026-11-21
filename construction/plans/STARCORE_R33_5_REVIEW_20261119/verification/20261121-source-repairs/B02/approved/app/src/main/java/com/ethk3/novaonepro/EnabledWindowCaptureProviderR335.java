package com.ethk3.novaonepro;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.util.Xml;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/** Read the three persisted assignment fields for a live, attached adb capture. */
public final class EnabledWindowCaptureProviderR335 extends ContentProvider {
    private static final String PATH = "/assignment";

    @Override public boolean onCreate() { return true; }

    static boolean allowedRead(int caller, String mode, String path, boolean canonicalUri) {
        return caller == Process.SHELL_UID && "r".equals(mode) && PATH.equals(path) && canonicalUri;
    }

    @Override public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        String authority = getContext().getPackageName() + ".r335.enabled";
        boolean exact = ("content://" + authority + PATH).equals(uri.toString());
        if (!allowedRead(Binder.getCallingUid(), mode, uri.getPath(), exact))
            throw new SecurityException("R335_ASSIGNMENT_READ_ONLY_SHELL");

        File prefs = new File(getContext().getApplicationInfo().dataDir, "shared_prefs/v20_service_runtime_light.xml");
        File backup = new File(prefs.getPath() + ".bak");
        if (!prefs.isFile() || backup.exists()) throw new FileNotFoundException("R335_ASSIGNMENT_NOT_STABLE");
        final byte[] bytes;
        try (FileInputStream input = new FileInputStream(prefs)) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(input, "UTF-8");
            bytes = projection(parser);
        } catch (IOException | XmlPullParserException | IllegalArgumentException failure) {
            FileNotFoundException unavailable = new FileNotFoundException("R335_ASSIGNMENT_UNREADABLE");
            unavailable.initCause(failure);
            throw unavailable;
        }
        if (backup.exists()) throw new FileNotFoundException("R335_ASSIGNMENT_WRITE_IN_PROGRESS");
        return openPipeHelper(uri, "text/xml", null, bytes, (fd, name, mime, opts, data) -> {
            try (ParcelFileDescriptor.AutoCloseOutputStream output = new ParcelFileDescriptor.AutoCloseOutputStream(fd)) {
                output.write(data);
            } catch (IOException disconnected) {
                // A partial read cannot pass the receiver's XML, journal and repeated-read checks.
            }
        });
    }

    static byte[] projection(XmlPullParser parser) throws IOException, XmlPullParserException {
        String journal = null, status = null, enabled = null;
        boolean map = false;
        for (int event = parser.next(); event != XmlPullParser.END_DOCUMENT; event = parser.next()) {
            if (event != XmlPullParser.START_TAG) continue;
            if (parser.getDepth() == 1) {
                if (map || !"map".equals(parser.getName())) throw new IOException("PREF_ROOT");
                map = true;
            }
            if (parser.getDepth() != 2) continue;
            String name = parser.getAttributeValue(null, "name");
            String tag = parser.getName();
            if (EnabledWindowJournalR335.PREF_KEY.equals(name)) {
                if (journal != null || !"string".equals(tag)) throw new IOException("JOURNAL_TYPE_OR_DUPLICATE");
                journal = parser.nextText();
            } else if (EnabledWindowJournalR335.STATUS_KEY.equals(name)) {
                if (status != null || !"string".equals(tag)) throw new IOException("STATUS_TYPE_OR_DUPLICATE");
                status = parser.nextText();
            } else if ("user_enabled".equals(name)) {
                if (enabled != null || !"boolean".equals(tag)) throw new IOException("ASSIGNMENT_TYPE_OR_DUPLICATE");
                enabled = parser.getAttributeValue(null, "value");
                if (!"true".equals(enabled) && !"false".equals(enabled)) throw new IOException("ASSIGNMENT_VALUE");
            }
        }
        if (!map || journal == null || !"RECORDED".equals(status) || enabled == null) throw new IOException("ASSIGNMENT_INCOMPLETE");
        EnabledWindowJournalR335.Journal parsed = EnabledWindowJournalR335.parse(journal);
        if (parsed.events.isEmpty() || parsed.events.get(parsed.events.size()-1).enabled != Boolean.parseBoolean(enabled))
            throw new IOException("ASSIGNMENT_HISTORY_MISMATCH");
        String escaped = journal.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
        String xml = "<map><string name=\"" + EnabledWindowJournalR335.PREF_KEY + "\">" + escaped
                + "</string><string name=\"" + EnabledWindowJournalR335.STATUS_KEY + "\">RECORDED</string>"
                + "<boolean name=\"user_enabled\" value=\"" + enabled + "\"/></map>";
        return xml.getBytes(StandardCharsets.UTF_8);
    }

    @Override public String getType(Uri uri) { return "text/xml"; }
    @Override public Cursor query(Uri u, String[] p, String s, String[] a, String o) { throw new UnsupportedOperationException(); }
    @Override public Uri insert(Uri u, ContentValues v) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri u, ContentValues v, String s, String[] a) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri u, String s, String[] a) { throw new UnsupportedOperationException(); }
}
