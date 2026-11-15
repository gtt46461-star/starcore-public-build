package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;

/** Chunked report store with byte-level hashes. It refuses partial/corrupt reads. */
public final class LongTextStoreR815 {
    public static final int MAX_CHUNK_BYTES = 16 * 1024;
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final SecureRandom RANDOM = new SecureRandom();

    public interface SectionSource {
        String sectionId();

        CharSequence content();
    }

    public interface ChunkConsumer {
        void onChunk(int index, String text) throws Exception;
    }

    public static final class Token {
        public final String value;
        public final String reportSha256;
        public final long expiresAt;

        Token(String value, String reportSha256, long expiresAt) {
            this.value = safe(value);
            this.reportSha256 = safe(reportSha256);
            this.expiresAt = expiresAt;
        }
    }

    public static final class ReadResult {
        public final boolean success;
        public final String status;
        public final int chunks;
        public final String reportSha256;

        ReadResult(boolean success, String status, int chunks, String reportSha256) {
            this.success = success;
            this.status = safe(status);
            this.chunks = Math.max(0, chunks);
            this.reportSha256 = safe(reportSha256);
        }
    }

    private LongTextStoreR815() {}

    public static Token put(final DbStore store, final SectionSource source) {
        if (store == null || source == null || safe(source.sectionId()).length() == 0)
            throw new IllegalArgumentException("R815_LONG_TEXT_ARGUMENT");
        CharSequence content = source.content();
        if (content == null) throw new IllegalArgumentException("R815_LONG_TEXT_CONTENT_NULL");
        final ArrayList<String> chunks = splitUtf8(content.toString());
        if (chunks.isEmpty()) chunks.add("");
        final String reportHash = digestChunks(chunks);
        final long now = System.currentTimeMillis();
        final long expiresAt = now + 7L * 24L * 60L * 60L * 1000L;
        final String token =
                "r815_"
                        + V51ChecksumEngine.sha256(
                                safe(source.sectionId())
                                        + '|'
                                        + reportHash
                                        + '|'
                                        + now
                                        + '|'
                                        + RANDOM.nextLong());
        Boolean written =
                store.coordinatorR36()
                        .runWrite(
                                "R815_LONG_TEXT_PUT",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        boolean owns = !db.inTransaction();
                                        if (owns) db.beginTransaction();
                                        try {
                                            for (int index = 0; index < chunks.size(); index++) {
                                                String text = chunks.get(index);
                                                ContentValues values = new ContentValues();
                                                values.put("token", token);
                                                values.put("chunk_index", index);
                                                values.put("chunk_text", text);
                                                values.put("chunk_sha256", sha256(text.getBytes(UTF8)));
                                                values.put("report_sha256", reportHash);
                                                values.put("created_at", now);
                                                values.put("expires_at", expiresAt);
                                                long row =
                                                        db.insertWithOnConflict(
                                                                DbHelper.TABLE_R815_LONG_TEXT,
                                                                null,
                                                                values,
                                                                SQLiteDatabase.CONFLICT_IGNORE);
                                                if (row < 0L && !chunkMatches(db, token, index, values))
                                                    throw new IllegalStateException("R815_LONG_TEXT_FIRST_WRITE_CONFLICT");
                                            }
                                            if (owns) db.setTransactionSuccessful();
                                            return Boolean.TRUE;
                                        } finally {
                                            if (owns && db.inTransaction()) db.endTransaction();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        if (written == null || !written.booleanValue())
            throw new IllegalStateException("R815_LONG_TEXT_WRITE_FAILED");
        return new Token(token, reportHash, expiresAt);
    }

    public static ReadResult read(DbStore store, Token token, ChunkConsumer consumer) {
        if (store == null || token == null || consumer == null || !validToken(token.value))
            return new ReadResult(false, "ARGUMENT_INVALID", 0, "");
        Cursor cursor = null;
        MessageDigest digest = newDigest();
        String expectedReport = "";
        int expectedIndex = 0;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT chunk_index,chunk_text,chunk_sha256,report_sha256,expires_at FROM "
                                            + DbHelper.TABLE_R815_LONG_TEXT
                                            + " WHERE token=? ORDER BY chunk_index ASC",
                                    new String[] {token.value});
            while (cursor.moveToNext()) {
                int index = cursor.getInt(0);
                String text = cursor.getString(1);
                String chunkHash = safe(cursor.getString(2));
                String reportHash = safe(cursor.getString(3));
                long expires = cursor.getLong(4);
                if (expires < System.currentTimeMillis()) return new ReadResult(false, "EXPIRED", expectedIndex, expectedReport);
                if (index != expectedIndex
                        || !sha256(safe(text).getBytes(UTF8)).equals(chunkHash)
                        || (expectedReport.length() > 0 && !expectedReport.equals(reportHash)))
                    return new ReadResult(false, "CORRUPT", expectedIndex, expectedReport);
                if (expectedReport.length() == 0) expectedReport = reportHash;
                byte[] bytes = safe(text).getBytes(UTF8);
                digest.update(bytes, 0, bytes.length);
                consumer.onChunk(index, safe(text));
                expectedIndex++;
            }
            if (expectedIndex == 0 || !expectedReport.equals(token.reportSha256))
                return new ReadResult(false, "NOT_FOUND_OR_BINDING_MISMATCH", expectedIndex, expectedReport);
            String computed = hex(digest.digest());
            if (!computed.equals(expectedReport)) return new ReadResult(false, "REPORT_HASH_MISMATCH", expectedIndex, expectedReport);
            return new ReadResult(true, "OK", expectedIndex, expectedReport);
        } catch (Exception failure) {
            return new ReadResult(false, "READ_FAILED:" + failure.getClass().getSimpleName(), expectedIndex, expectedReport);
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    public static boolean verify(Token token, String expectedHash) {
        return token != null && token.reportSha256.equals(safe(expectedHash)) && hash(token.reportSha256);
    }

    public static int prune(final DbStore store, final long now) {
        if (store == null) return 0;
        Integer deleted =
                store.coordinatorR36()
                        .runWrite(
                                "R815_LONG_TEXT_PRUNE",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        return Integer.valueOf(
                                                db.delete(
                                                        DbHelper.TABLE_R815_LONG_TEXT,
                                                        "rowid IN (SELECT rowid FROM "
                                                                + DbHelper.TABLE_R815_LONG_TEXT
                                                                + " WHERE expires_at<? LIMIT 200)",
                                                        new String[] {String.valueOf(now)}));
                                    }
                                },
                                Integer.valueOf(0));
        return deleted == null ? 0 : Math.max(0, deleted.intValue());
    }

    private static boolean chunkMatches(SQLiteDatabase db, String token, int index, ContentValues values) {
        Cursor cursor = null;
        try {
            cursor =
                    db.rawQuery(
                            "SELECT chunk_sha256,report_sha256 FROM "
                                    + DbHelper.TABLE_R815_LONG_TEXT
                                    + " WHERE token=? AND chunk_index=? LIMIT 1",
                            new String[] {token, String.valueOf(index)});
            return cursor.moveToFirst()
                    && safe(values.getAsString("chunk_sha256")).equals(safe(cursor.getString(0)))
                    && safe(values.getAsString("report_sha256")).equals(safe(cursor.getString(1)));
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static ArrayList<String> splitUtf8(String text) {
        ArrayList<String> out = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        int bytes = 0;
        for (int offset = 0; offset < text.length();) {
            int codePoint = text.codePointAt(offset);
            String part = new String(Character.toChars(codePoint));
            int partBytes = part.getBytes(UTF8).length;
            if (bytes > 0 && bytes + partBytes > MAX_CHUNK_BYTES) {
                out.add(current.toString());
                current.setLength(0);
                bytes = 0;
            }
            current.append(part);
            bytes += partBytes;
            offset += Character.charCount(codePoint);
        }
        if (current.length() > 0) out.add(current.toString());
        return out;
    }

    private static String digestChunks(ArrayList<String> chunks) {
        MessageDigest digest = newDigest();
        for (int i = 0; i < chunks.size(); i++) {
            byte[] bytes = chunks.get(i).getBytes(UTF8);
            digest.update(bytes, 0, bytes.length);
        }
        return hex(digest.digest());
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (Exception failure) {
            throw new IllegalStateException("R815_SHA256_UNAVAILABLE", failure);
        }
    }

    private static String sha256(byte[] bytes) {
        MessageDigest digest = newDigest();
        digest.update(bytes, 0, bytes.length);
        return hex(digest.digest());
    }

    private static String hex(byte[] bytes) {
        StringBuilder out = new StringBuilder(bytes.length * 2);
        for (int i = 0; i < bytes.length; i++) out.append(String.format(java.util.Locale.US, "%02x", bytes[i] & 0xff));
        return out.toString();
    }

    private static boolean validToken(String value) {
        return value != null && value.startsWith("r815_") && value.length() == 69;
    }

    private static boolean hash(String value) {
        if (value == null || value.length() != 64) return false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) return false;
        }
        return true;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
