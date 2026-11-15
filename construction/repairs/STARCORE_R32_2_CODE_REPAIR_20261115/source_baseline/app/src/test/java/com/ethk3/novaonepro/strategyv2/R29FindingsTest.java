package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** R29-040: findings carry evidence, strength, refutation, code, card and verification; missing data is UNKNOWN. */
public class R29FindingsTest {
    @Test public void unmeasuredIsUnknownNotPass() {
        List<Map<String, Object>> f = FindingsR29.fromQualification(new LinkedHashMap<String, Object>());
        assertEquals(1, f.size()); assertEquals("UNKNOWN", f.get(0).get("severity"));
        assertEquals("UNKNOWN", FindingsR29.fromQualification(null).get(0).get("severity"));
    }
    @Test public void measuredGapsBecomeActionableFindings() {
        Map<String, Object> q = new LinkedHashMap<>();
        q.put("receipts", 10L); q.put("receiptStateMismatch", 1L); q.put("conflictsOpen", 2L); q.put("legacyHitMinus2Remaining", 0L);
        q.put("pendingDrawOpen", 3L); q.put("pendingDrawOpenActRows", 1L); q.put("pendingDrawOldestOrdinal", 77L); q.put("gateV2Evaluated", 4L); q.put("gateV2WouldIssue", 0L);
        List<Map<String, Object>> f = FindingsR29.fromQualification(q);
        String ids = ""; for (Map<String, Object> x : f) { ids += x.get("id") + ";"; for (String k : new String[] {"evidence", "strength", "refutedBy", "code", "card", "verification"}) assertNotNull(k, x.get(k)); }
        assertTrue(ids, ids.contains("RECEIPT-MISMATCH") && ids.contains("CONFLICT") && ids.contains("PENDING-DRAW") && ids.contains("NO-QUALIFIED"));
        assertFalse(ids.contains("MINUS2"));
    }
}
