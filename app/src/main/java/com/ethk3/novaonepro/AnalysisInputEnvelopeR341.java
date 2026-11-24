package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** One PIT input per durable analysis attempt. Its numerical hash stays compatible with V51. */
final class AnalysisInputEnvelopeR341 {
    static final String TABLE = "r341_analysis_input";
    static final int MAX_BYTES = 48 * 1024;
    private static final int FORMAT = 1;
    private final String requestHash, database, captureOwner, boot;
    private final long generation, deadlineWall, deadlineElapsed;
    final V51RuntimeInputR36 input;

    private AnalysisInputEnvelopeR341(AnalysisSingleFlightR36.Request request, String database,
                                     String captureOwner, String boot, long deadlineElapsed, V51RuntimeInputR36 input) {
        this.requestHash = request.requestHash;
        this.database = database;
        this.captureOwner = captureOwner;
        this.boot = boot;
        this.generation = request.generation;
        this.deadlineWall = request.deadlineAt;
        this.deadlineElapsed = deadlineElapsed;
        this.input = input;
    }

    static void ensureSchema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE + "(target_issue TEXT NOT NULL,generation INTEGER NOT NULL,"
                + "request_hash TEXT NOT NULL,history_hash TEXT NOT NULL,encoded_hash TEXT NOT NULL,"
                + "body BLOB NOT NULL CHECK(length(body) BETWEEN 1 AND " + MAX_BYTES + "),"
                + "PRIMARY KEY(target_issue,generation,request_hash)) WITHOUT ROWID");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r341_analysis_input_no_update BEFORE UPDATE ON " + TABLE
                + " BEGIN SELECT RAISE(ABORT,'R341_ANALYSIS_INPUT_IMMUTABLE'); END");
    }

    static AnalysisInputEnvelopeR341 create(List<Models.Draw> history, AnalysisSingleFlightR36.Request request,
                                            String owner, String database) {
        if (owner == null || owner.isEmpty() || database == null || !database.matches("[a-f0-9-]{36}"))
            throw new IllegalArgumentException("R341_ANALYSIS_INPUT_SCOPE_REQUIRED");
        AnalysisInputEnvelopeR341 result = new AnalysisInputEnvelopeR341(request, database, owner,
                IssueTimelineWriterR30.bootId(), request.deadlineMonoAt, V51RuntimeInputR36.freeze(history, request));
        result.verify(request, database);
        return result;
    }

    void verify(AnalysisSingleFlightR36.Request request, String expectedDatabase) {
        if (!input.baseIssue.equals(request.baseIssue) || !input.targetIssue.equals(request.targetIssue)
                || generation != request.generation || !requestHash.equals(request.requestHash)
                || !database.equals(expectedDatabase) || deadlineWall != request.deadlineAt
                || (boot.equals(IssueTimelineWriterR30.bootId()) && request.deadlineMonoAt > deadlineElapsed)
                || (!request.historyHash.isEmpty() && !request.historyHash.equals(input.historyHash)))
            throw new IllegalStateException("R341_PIT_ANALYSIS_INPUT_IDENTITY_CONFLICT:" + request.targetIssue);
    }

    byte[] encode() {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(bytes)) {
                out.writeInt(FORMAT);
                text(out, input.baseIssue);
                text(out, input.targetIssue);
                text(out, requestHash);
                text(out, input.historyHash);
                text(out, database);
                text(out, captureOwner);
                text(out, boot);
                out.writeLong(generation);
                out.writeLong(deadlineWall);
                out.writeLong(deadlineElapsed);
                out.writeInt(input.history.size());
                for (Models.Draw draw : input.history) {
                    text(out, draw.issue);
                    out.writeLong(draw.openTime);
                    out.writeLong(draw.fetchTime);
                    out.writeLong(draw.id);
                    for (int number : draw.numbers) out.writeByte(number);
                }
            }
            byte[] result = bytes.toByteArray();
            if (result.length > MAX_BYTES) throw new IllegalArgumentException("R341_ANALYSIS_INPUT_BOUND");
            return result;
        } catch (IOException impossible) { throw new IllegalStateException("R341_ANALYSIS_INPUT_ENCODE", impossible); }
    }

    static AnalysisInputEnvelopeR341 decode(byte[] bytes, AnalysisSingleFlightR36.Request request, String database) {
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_BYTES) throw new IllegalArgumentException("R341_ANALYSIS_INPUT_BOUND");
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            if (in.readInt() != FORMAT) throw new IOException("FORMAT");
            String base = text(in), target = text(in), requestHash = text(in), historyHash = text(in);
            String source = text(in), owner = text(in), boot = text(in);
            long generation = in.readLong(), wall = in.readLong(), elapsed = in.readLong();
            if (!base.equals(request.baseIssue) || !target.equals(request.targetIssue)
                    || !requestHash.equals(request.requestHash) || generation != request.generation
                    || wall != request.deadlineAt || !source.equals(database) || owner.isEmpty() || boot.isEmpty())
                throw new IOException("IDENTITY");
            int count = in.readInt();
            if (count < R36AnalysisHistoryPolicyR364.MIN_LIVE_HISTORY || count > R36AnalysisHistoryPolicyR364.MAX_FROZEN_HISTORY)
                throw new IOException("ROW_COUNT");
            List<Models.Draw> rows = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                Models.Draw draw = new Models.Draw();
                draw.issue = text(in);
                if (!TargetIssueResolver.isValid(draw.issue) || (i > 0 && TargetIssueResolver.distance(rows.get(i - 1).issue, draw.issue) != 1))
                    throw new IOException("ISSUE_ORDER");
                draw.openTime = in.readLong();
                draw.fetchTime = in.readLong();
                draw.id = in.readLong();
                for (int j = 0; j < draw.numbers.length; j++) draw.numbers[j] = in.readUnsignedByte();
                if (!draw.isValid()) throw new IOException("DRAW_PERMUTATION");
                rows.add(draw);
            }
            if (in.read() != -1) throw new IOException("TRAILING_BYTES");
            V51RuntimeInputR36 frozen = V51RuntimeInputR36.freeze(rows, request);
            if (frozen.history.size() != count || !historyHash.equals(frozen.historyHash)) throw new IOException("HISTORY_HASH");
            AnalysisInputEnvelopeR341 result = new AnalysisInputEnvelopeR341(request, source, owner, boot, elapsed, frozen);
            result.verify(request, database);
            return result;
        } catch (IOException failure) { throw new IllegalStateException("R341_INTEGRITY_ANALYSIS_INPUT:" + failure.getMessage(), failure); }
    }

    private static void text(DataOutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length > 256) throw new IOException("FIELD_LENGTH");
        out.writeShort(bytes.length);
        out.write(bytes);
    }

    private static String text(DataInputStream in) throws IOException {
        int count = in.readUnsignedShort();
        if (count > 256) throw new IOException("FIELD_LENGTH");
        byte[] bytes = new byte[count];
        in.readFully(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
