package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;
import static com.ethk3.novaonepro.strategyv2.SchemaUnitPlanR34.*;

/** Host protocol tests; fixture hashes are explicitly synthetic, never production definitions. */
public final class SchemaUnitPlanR34Host {
    static int checks;
    static void check(boolean value, String reason) { if (!value) throw new AssertionError(reason); checks++; }
    static void rejected(String code, Runnable run) {
        try { run.run(); throw new AssertionError("accepted " + code); }
        catch (ProtocolViolation expected) { check(expected.code.equals(code), expected.code + " != " + code); }
    }
    static final Budget BUDGET = new Budget(16, 256, 32, 262144);
    static final Cost INSTALL = new Cost(5, 5, 4, 4096), VERIFY = new Cost(0, 5, 4, 4096);
    static Map<String, Definition> definitions() {
        Map<String, Definition> out = new LinkedHashMap<>();
        for (String table : PROTECTED_TABLES_095)
            out.put(table, new Definition(sha256("fixture source " + table), sha256("fixture guards " + table), true));
        return out;
    }
    static View view(Plan plan, Cursor c, long cookie) {
        Unit u = plan.units.get(c.nextIndex);
        return new View(plan.databaseId, plan.schemaRevision, plan.definitionRevision, cookie,
            u.definition.sourceDefinitionHash, sha256("fixture original bytes " + u.source), true);
    }
    public static void main(String[] args) {
        Map<String, Definition> definitions = definitions();
        Plan p = new Plan("fixture-db", "r34-retention", 34, "fixture-definition-v1", definitions);
        check(p.units.size() == PROTECTED_TABLES_095.size(), "all actual protected tables represented");
        definitions.remove(PROTECTED_TABLES_095.get(0));
        rejected("PROTECTED_REGISTRY_COVERAGE", () -> new Plan("db", "migration", 34, "rev", definitions));
        Cursor c = initial(p, "run-1", "boot-a", 1);
        View v = view(p, c, 10);
        Ticket t = prepare(p, c, v, INSTALL, BUDGET, 2000);
        rejected("UNIT_REQUIRES_SMALLER_OWNER_SLICE", () -> prepare(p, t.before, v,
            new Cost(1000, 5, 4, 4096), BUDGET, 2000));
        Cursor stale = new Cursor(c.planHash, c.runId, "boot-b", c.phase, c.nextIndex, c.revision, 2, 0, -1);
        rejected("STALE_OWNER_OR_CURSOR", () -> beforeWrite(p, t, stale, v, 1000));
        rejected("REPREPARE_SCHEMA_COOKIE", () -> beforeWrite(p, t, t.before, view(p, t.before, 11), 1000));
        rejected("DEADLINE_BEFORE_BEGIN", () -> beforeWrite(p, t, t.before, v, 2000));
        Admission a = beforeWrite(p, t, c, v, 1000);
        rejected("PROTECTED_ORIGINAL_CHANGED", () -> receiptPayload(a,
            new Effect(t.unit.definition.protectionDefinitionHash, sha256("lost original"), 15, INSTALL)));
        rejected("EXACT_DEFINITION_MISMATCH", () -> receiptPayload(a,
            new Effect(sha256("same name wrong SQL"), v.originalContentRoot, 15, INSTALL)));
        ReceiptPayload r = receiptPayload(a, new Effect(t.unit.definition.protectionDefinitionHash, v.originalContentRoot, 15, INSTALL));
        check(resolve(t, r, Outcome.UNKNOWN, null).resolution == Resolution.WAIT_DURABLE_RECEIPT, "unknown cannot replay");
        check(resolve(t, r, Outcome.ROLLED_BACK, null).cursor.nextIndex == 0, "rollback cannot advance");
        rejected("COMMITTED_RECEIPT_MISSING", () -> resolve(t, r, Outcome.COMMITTED, null));
        rejected("DURABLE_RECEIPT_CONFLICT", () -> resolve(t, r, Outcome.UNKNOWN,
            new ReadBack(r.commitKey, sha256("tampered receipt"), r.next)));
        c = resolve(t, r, Outcome.UNKNOWN, new ReadBack(r.commitKey, r.payloadHash, r.next)).cursor;
        check(c.nextIndex == 1, "commit before ACK recovered by original receipt");
        long cookie = 15;
        while (c.phase != Phase.DONE) {
            View prepared = view(p, c, cookie); Cost cost = c.phase == Phase.INSTALL ? INSTALL : VERIFY;
            Ticket ticket = prepare(p, c, prepared, cost, BUDGET, 2000);
            Admission admitted = beforeWrite(p, ticket, c, prepared, 1000);
            long afterCookie = c.phase == Phase.INSTALL ? cookie + 5 : cookie;
            ReceiptPayload payload = receiptPayload(admitted,
                new Effect(ticket.unit.definition.protectionDefinitionHash, prepared.originalContentRoot, afterCookie, cost));
            c = resolve(ticket, payload, Outcome.COMMITTED, new ReadBack(payload.commitKey, payload.payloadHash, payload.next)).cursor;
            cookie = afterCookie;
            if (c.phase == Phase.VERIFY && c.nextIndex == 1) {
                Cursor firstVerified = c; long changed = cookie + 1;
                rejected("RESTART_VERIFY_REQUIRED", () -> prepare(p, firstVerified, view(p, firstVerified, changed), VERIFY, BUDGET, 2000));
            }
        }
        check(c.revision == 2L * p.units.size(), "one committed install and verify per table");
        check(certificateCurrent(p, c, p.databaseId, p.schemaRevision, p.definitionRevision, cookie), "complete exact certificate");
        check(!certificateCurrent(p, c, p.databaseId, p.schemaRevision, p.definitionRevision, cookie + 1), "later DDL invalidates certificate");
        Cursor reset = restartVerification(p, c);
        check(reset.nextIndex == 0 && reset.auditRound == 1 && reset.phase == Phase.VERIFY, "bounded revalidation restart");
        check(!certificateCurrent(p, reset, p.databaseId, p.schemaRevision, p.definitionRevision, cookie), "proposal not ready");
        Ticket verify = prepare(p, reset, view(p, reset, cookie), VERIFY, BUDGET, 2000);
        Admission verifyAdmission = beforeWrite(p, verify, reset, verify.prepared, 1000);
        rejected("VERIFY_MUTATED_SCHEMA", () -> receiptPayload(verifyAdmission, new Effect(
            verify.unit.definition.protectionDefinitionHash, verify.prepared.originalContentRoot, verify.prepared.schemaCookie + 1, VERIFY)));
        check(retryDelayMs(Wait.WRITER_BUSY, 100) == 8000, "resource wait capped without business attempts");
        System.out.println("{\"javaProtocolChecks\":" + checks + ",\"protectedTables\":" + p.units.size()
            + ",\"committedUnitsInFixture\":" + c.revision + "}");
    }
}
