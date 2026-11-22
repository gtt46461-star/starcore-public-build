package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Installs additive definitions using the same SQL as the legacy research initializer. */
final class ResearchSchemaDdlR335 {
    private static final Pattern CREATE = Pattern.compile(
            "^CREATE (?:UNIQUE )?(?:TABLE|INDEX) IF NOT EXISTS ([A-Za-z_][A-Za-z0-9_]*)(?:\\s|\\()");

    private final String[] definitions;
    private final String[] names;
    private final String lookup;

    ResearchSchemaDdlR335(String[] definitions) {
        this.definitions = definitions;
        names = new String[definitions.length];
        StringBuilder marks = new StringBuilder();
        for (int i = 0; i < definitions.length; i++) {
            Matcher match = CREATE.matcher(definitions[i]);
            if (!match.find()) throw new IllegalArgumentException("R335_UNRECOGNIZED_RESEARCH_DDL");
            names[i] = match.group(1);
            if (i > 0) marks.append(',');
            marks.append('?');
        }
        lookup = "SELECT name FROM sqlite_master WHERE name IN (" + marks + ")";
    }

    void installAll(SQLiteDatabase db) {
        for (String definition : definitions) db.execSQL(definition);
    }

    boolean installNext(SQLiteDatabase db) {
        Set<String> installed = new HashSet<>();
        try (Cursor c = db.rawQuery(lookup, names)) {
            while (c.moveToNext()) installed.add(c.getString(0));
        }
        for (int i = 0; i < names.length; i++) {
            if (!installed.contains(names[i])) {
                ResearchWorkBudgetR927.checkpoint();
                db.execSQL(definitions[i]);
                return false;
            }
        }
        return true;
    }
}
