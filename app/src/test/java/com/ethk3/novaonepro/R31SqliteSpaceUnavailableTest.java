package com.ethk3.novaonepro;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Map;
import org.junit.Test;

/** R31-B05-C01#A3: when dbstat (and even PRAGMA page metadata) is unavailable, the census reports
 * UNSUPPORTED/UNKNOWN with the reason and never derives bytes from row counts or reports zero. */
public final class R31SqliteSpaceUnavailableTest {
    @Test public void unavailableDbstatIsExplicitUnknownNotZeroOrRowEstimate() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe"); unsafeField.setAccessible(true);
        sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
        // The JVM stub database answers no query (rawQuery yields no cursor): every probe is unavailable.
        android.database.sqlite.SQLiteDatabase db = (android.database.sqlite.SQLiteDatabase) unsafe.allocateInstance(android.database.sqlite.SQLiteDatabase.class);
        Map<String, Object> space = StorageCensusSU2.sqliteSpace(db);
        assertEquals("UNSUPPORTED_OR_UNAVAILABLE", space.get("dbstatStatus"));
        assertNotNull("the unavailability reason is reported", space.get("dbstatReason"));
        assertNull("no per-object bytes are invented", space.get("dbstatObjects"));
        for (String key : new String[]{"page_count", "page_size", "freelist_count"}) {
            assertTrue(key + " key present", space.containsKey(key));
            assertNull(key + " unknown, never zero", space.get(key));
            assertNotNull(key + " error recorded", space.get(key + "Error"));
        }
        assertFalse("no logical bytes without page metadata", space.containsKey("logicalPageBytes"));
        assertFalse("no free bytes without page metadata", space.containsKey("freePageBytes"));
        assertTrue(String.valueOf(space.get("scope")).contains("DBSTAT_EXCLUDES_FREELIST_AND_SIDECARS"));
    }
}
