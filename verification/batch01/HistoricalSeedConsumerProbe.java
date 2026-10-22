package com.ethk3.novaonepro;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/** Decode the actual packaged CSV with release bytecode; grants no DB/model proof. */
public final class HistoricalSeedConsumerProbe {
    public static void main(String[] args) throws Exception {
        Method parse = HistoricalDrawImportEngine.class.getDeclaredMethod("parse", String.class);
        parse.setAccessible(true);
        Set<String> issues = new HashSet<>();
        String first = null, last = null;
        try (BufferedReader reader = HistoricalDrawImportEngine.seedReader(new FileInputStream(args[0]))) {
            if (reader.readLine() == null) throw new AssertionError("SEED_HEADER_MISSING");
            String line;
            while ((line = reader.readLine()) != null) {
                Models.Draw draw = (Models.Draw) parse.invoke(null, line);
                if (draw == null || !draw.isValid() || !issues.add(draw.issue))
                    throw new AssertionError("SEED_INVALID_OR_DUPLICATE");
                if (first == null) first = draw.issue;
                last = draw.issue;
            }
        }
        if (issues.size() != HistoricalDrawImportEngine.EXPECTED_SEED_ROWS
                || !HistoricalDrawImportEngine.FIRST_SEED_ISSUE.equals(first)
                || !HistoricalDrawImportEngine.LAST_SEED_ISSUE.equals(last))
            throw new AssertionError("SEED_COUNT_OR_BOUNDARY_DRIFT");
        System.out.println("PASS|ACTUAL_APK_HISTORY_SEED|rows=" + issues.size()
                + "|asset=" + HistoricalDrawImportEngine.SEED_ASSET_PATH + "|NO_DATABASE_AUTHORITY");
    }
}
