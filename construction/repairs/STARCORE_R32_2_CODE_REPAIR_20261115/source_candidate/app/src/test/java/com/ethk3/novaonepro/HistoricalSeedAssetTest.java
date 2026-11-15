package com.ethk3.novaonepro;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.GZIPInputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/** Actual retained 10,000-row input and production decoder, with no DB authority. */
public final class HistoricalSeedAssetTest {
    private static Models.Draw parse(String line) throws Exception {
        Method method = HistoricalDrawImportEngine.class.getDeclaredMethod("parse", String.class);
        method.setAccessible(true);
        return (Models.Draw) method.invoke(null, line);
    }

    @Test public void mergedCsvIsDecodedByTheRealImporterWithoutGzip() throws Exception {
        Path source = Path.of("src/main/assets/history_seed.csv.gz");
        Set<String> issues = new HashSet<>();
        String first = null, last = null;
        // AGP's merged asset is the decompressed source. Do the same transformation here.
        try (InputStream csv = new GZIPInputStream(Files.newInputStream(source));
             BufferedReader reader = HistoricalDrawImportEngine.seedReader(csv)) {
            assertNotNull(reader.readLine());
            String line;
            while ((line = reader.readLine()) != null) {
                Models.Draw draw = parse(line);
                assertNotNull(draw);
                assertTrue(draw.isValid());
                assertTrue(issues.add(draw.issue));
                assertTrue(draw.openTime > 0 && draw.fetchTime > 0);
                if (first == null) first = draw.issue;
                last = draw.issue;
            }
        }
        assertEquals(HistoricalDrawImportEngine.EXPECTED_SEED_ROWS, issues.size());
        assertEquals(HistoricalDrawImportEngine.FIRST_SEED_ISSUE, first);
        assertEquals(HistoricalDrawImportEngine.LAST_SEED_ISSUE, last);
    }

    @Test public void malformedAndNonvalidRowsDoNotBecomeDraws() throws Exception {
        assertNull(parse(null));
        assertNull(parse(""));
        assertNull(parse("issue,1,source,1,other,other,1,1"));
        assertNull(parse("issue,1,source,\"1,2,3,4,5,6,7,8,9,9\",other,other,1,1"));
        assertNull(parse("issue,1,source,\"1,2,3,4,5,6,7,8,9,10\",other,other,0,1"));
    }

    @Test public void decoderPropagatesIoFailureAndClosesItsInput() throws Exception {
        final boolean[] closed = {false};
        InputStream failure = new InputStream() {
            @Override public int read() throws IOException { throw new IOException("seed-read-failure"); }
            @Override public void close() { closed[0] = true; }
        };
        try (BufferedReader reader = HistoricalDrawImportEngine.seedReader(failure)) {
            try { reader.readLine(); fail("Unreadable seed accepted"); }
            catch (IOException expected) { assertEquals("seed-read-failure", expected.getMessage()); }
        }
        assertTrue(closed[0]);
        try (BufferedReader reader = HistoricalDrawImportEngine.seedReader(
                new ByteArrayInputStream("header\n".getBytes(StandardCharsets.UTF_8)))) {
            assertEquals("header", reader.readLine());
            assertNull(reader.readLine());
        }
    }
}
