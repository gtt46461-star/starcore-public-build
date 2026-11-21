package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;

public final class EnabledWindowPreferenceR335Test {
    private static final String PROCESS = "123456789abcdef0123456789abcdef0";
    private static final String SOURCE = "123456789abcdef0123456789abcdef012345678";

    @Test public void frameworkClosingTagIndentationPreservesExactJournalAndSubsequentAssignment() {
        String before = EnabledWindowJournalR335.append("", true, 10000, 500, PROCESS, SOURCE);
        String disk = before + "    ";
        assertEquals(before, EnabledWindowJournalR335.preferenceText(before));
        assertEquals(before, EnabledWindowJournalR335.preferenceText(disk));
        String continued = EnabledWindowJournalR335.appendState(EnabledWindowJournalR335.preferenceText(disk),
                "0", 10100, 600, PROCESS, SOURCE);
        EnabledWindowJournalR335.Journal journal = EnabledWindowJournalR335.parse(continued);
        assertEquals(2, journal.events.size());
        assertTrue(journal.events.get(0).enabled);
        assertFalse(journal.events.get(1).enabled);
        assertEquals(journal.events.get(0).hash, journal.events.get(1).previousHash);
        assertEquals(EnabledWindowJournalR335.append(before, false, 10100, 600, PROCESS, SOURCE), continued);
    }

    @Test public void truncationHashDamageAndUnrecognizedSuffixesRemainInvalid() {
        String good = EnabledWindowJournalR335.append("", true, 10000, 500, PROCESS, SOURCE);
        String noLf = good.substring(0, good.length() - 1);
        String damaged = good.replace("|10000|", "|10001|");
        for (String bad : new String[]{noLf, noLf + "    ", good + " ", good + "     ",
                good + "\t", good + "   x", damaged + "    "}) {
            assertThrows(IllegalArgumentException.class,
                    () -> EnabledWindowJournalR335.parse(EnabledWindowJournalR335.preferenceText(bad)));
        }
        assertThrows(IllegalArgumentException.class, () -> EnabledWindowJournalR335.parse(good + "    "));
    }
}
