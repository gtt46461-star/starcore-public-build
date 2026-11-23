package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Pure protocol core for bounded retention-schema work. It does not open SQLite,
 * run owner DDL, declare an Android migration complete, or replace R31 guards.
 * Every Cursor/ReadBack supplied to it MUST come from the durable adapter contract.
 * See INTEGRATION.md for the required production owners and transaction protocol.
 */
public final class SchemaUnitPlanR34 {
    private SchemaUnitPlanR34() {}
    public static final String BASELINE = "273641e838e0b8dd842b42a67a674e184942a30a";
    public static final int PROTOCOL_VERSION = 1;
    // Generated verbatim from ResearchRetentionR30.PROTECTED_TABLES at BASELINE.
    public static final List<String> PROTECTED_TABLES_095 = Collections.unmodifiableList(java.util.Arrays.asList(
        "r26_incumbent_frozen", "r26_frozen", "r26_feedback", "r26_consumption", "r26_policy_identity", "r26_checkpoint_receipt", "r26_gap_receipt", "r26_expert_audit", "r26_evolution", "r26_foundry", "r26_legacy_campaign", "r26_expert_experiment", "r26_live_model", "r26_candidate",
        "r27_policy", "r27_lineage", "r27_generation", "r27_live_frozen", "r27_live_shadow", "r27_eprocess", "r27_certificate", "r27_holdout_exposure", "r27_atomnet_state", "r29_qual_receipt", "r29_model_checkpoint", "r29_legacy_model_checkpoint",
        "r30_confirm_read", "r30_frozen_confirm", "r30_model_artifact", "r30_model_surface", "r30_model_call", "r30_decision_explain", "r30_decision_mismatch", "r30_certificate_revocation_event", "r30_revoked_policy", "r30_revoked_certificate",
        "r30r1_analysis_run", "r30r1_research_measure", "r30r1_proposal", "r30r1_consume", "research_matrix_snapshot_r930",
        "su2_certificate", "su2_mainline_choice", "su2_mainline_binding", "su2_live_state", "su2_matrix_snapshot", "su2_diag_snapshot", "su2_live_decision", "su2_model_genome", "su2_lineage",
        "su1_model_ablation", "su1_campaign_audit", "su1_live_prediction", "su1_live_result", "su1_sealed_plan", "su1_sealed_result", "su1_certificate", "su1_alpha_ledger"));

    public enum Phase { INSTALL, VERIFY, DONE }
    public enum Outcome { ROLLED_BACK, COMMITTED, UNKNOWN }
    public enum Resolution { ACKNOWLEDGED, RETRY_SAME_UNIT, WAIT_DURABLE_RECEIPT }
    public enum Wait { WRITER_BUSY, DEADLINE_BEFORE_BEGIN, INPUT_CHANGED }

    public static final class ProtocolViolation extends IllegalStateException {
        public final String code;
        ProtocolViolation(String code) { super("R34_SCHEMA_" + code); this.code = code; }
    }
    private static void need(boolean ok, String code) { if (!ok) throw new ProtocolViolation(code); }
    private static String text(String value, String field) {
        need(value != null && !value.isEmpty(), "MISSING_" + field); return value;
    }
    private static String hash(String value, String field) {
        need(value != null && value.matches("[0-9a-f]{64}"), "BAD_HASH_" + field); return value;
    }
    public static String sha256(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(64);
            for (byte b : bytes) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }
    private static String framed(String... values) {
        StringBuilder out = new StringBuilder();
        for (String value : values) out.append(value.length()).append(':').append(value);
        return out.toString();
    }

    /** Exact owner definitions, not observed arbitrary sqlite_master SQL used as authority. */
    public static final class Definition {
        public final String sourceDefinitionHash, protectionDefinitionHash;
        public final boolean sourceExpected;
        public Definition(String sourceDefinitionHash, String protectionDefinitionHash, boolean sourceExpected) {
            this.sourceDefinitionHash = hash(sourceDefinitionHash, "SOURCE_DEFINITION");
            this.protectionDefinitionHash = hash(protectionDefinitionHash, "PROTECTION_DEFINITION");
            this.sourceExpected = sourceExpected;
        }
    }
    public static final class Unit {
        public final int index;
        public final String source, archive;
        public final Definition definition;
        private Unit(int index, String source, Definition definition) {
            this.index = index; this.source = source; this.archive = "r30_keep_" + source; this.definition = definition;
        }
    }
    public static final class Plan {
        public final String databaseId, migrationId, definitionRevision, planHash;
        public final int schemaRevision;
        public final List<Unit> units;
        public Plan(String databaseId, String migrationId, int schemaRevision, String definitionRevision,
                    Map<String, Definition> definitions) {
            this.databaseId = text(databaseId, "DATABASE_ID"); this.migrationId = text(migrationId, "MIGRATION_ID");
            need(schemaRevision > 0, "BAD_SCHEMA_REVISION"); this.schemaRevision = schemaRevision;
            this.definitionRevision = text(definitionRevision, "DEFINITION_REVISION");
            need(definitions != null && definitions.size() == PROTECTED_TABLES_095.size()
                && definitions.keySet().containsAll(PROTECTED_TABLES_095), "PROTECTED_REGISTRY_COVERAGE");
            List<Unit> ordered = new ArrayList<>();
            StringBuilder identity = new StringBuilder(framed("R34_SCHEMA_UNIT_PLAN_V1", databaseId,
                migrationId, String.valueOf(schemaRevision), definitionRevision));
            for (String table : PROTECTED_TABLES_095) {
                Definition definition = Objects.requireNonNull(definitions.get(table), "definition " + table);
                ordered.add(new Unit(ordered.size(), table, definition));
                identity.append(framed(table, definition.sourceDefinitionHash, definition.protectionDefinitionHash,
                    String.valueOf(definition.sourceExpected)));
            }
            units = Collections.unmodifiableList(ordered); planHash = sha256(identity.toString());
        }
    }

    /** Durable cursor. A proposed cursor is not evidence until the adapter commits and reads it. */
    public static final class Cursor {
        public final String planHash, runId, ownerEpoch;
        public final Phase phase;
        public final int nextIndex;
        public final long revision, fence, auditRound, auditSchemaCookie;
        public Cursor(String planHash, String runId, String ownerEpoch, Phase phase, int nextIndex,
                      long revision, long fence, long auditRound, long auditSchemaCookie) {
            this.planHash = hash(planHash, "CURSOR_PLAN"); this.runId = text(runId, "RUN_ID");
            this.ownerEpoch = text(ownerEpoch, "OWNER_EPOCH"); this.phase = Objects.requireNonNull(phase);
            need(nextIndex >= 0 && revision >= 0 && fence > 0 && auditRound >= 0 && auditSchemaCookie >= -1,
                "BAD_CURSOR");
            this.nextIndex = nextIndex; this.revision = revision; this.fence = fence;
            this.auditRound = auditRound; this.auditSchemaCookie = auditSchemaCookie;
        }
        private String signature() {
            return framed(planHash, runId, ownerEpoch, phase.name(), String.valueOf(nextIndex),
                String.valueOf(revision), String.valueOf(fence), String.valueOf(auditRound), String.valueOf(auditSchemaCookie));
        }
    }
    public static Cursor initial(Plan plan, String runId, String ownerEpoch, long fence) {
        return new Cursor(plan.planHash, runId, ownerEpoch, Phase.INSTALL, 0, 0, fence, 0, -1);
    }
    private static void cursor(Plan plan, Cursor value) {
        need(plan.planHash.equals(value.planHash), "CURSOR_PLAN_CHANGED");
        need(value.phase == Phase.DONE ? value.nextIndex == plan.units.size() : value.nextIndex < plan.units.size(),
            "CURSOR_RANGE");
        if (value.phase == Phase.DONE) need(value.auditSchemaCookie >= 0, "DONE_WITHOUT_VERIFICATION");
    }

    /** Costs must come from the exact prepared metadata/DDL. No protected-row copying is allowed here. */
    public static final class Cost {
        public final int ddlStatements, columns, attachments;
        public final long definitionBytes;
        public Cost(int ddlStatements, int columns, int attachments, long definitionBytes) {
            need(ddlStatements >= 0 && columns >= 0 && attachments >= 0 && definitionBytes >= 0, "BAD_COST");
            this.ddlStatements = ddlStatements; this.columns = columns;
            this.attachments = attachments; this.definitionBytes = definitionBytes;
        }
    }
    public static final class Budget {
        public final int maxDdlStatements, maxColumns, maxAttachments;
        public final long maxDefinitionBytes;
        public Budget(int statements, int columns, int attachments, long bytes) {
            need(statements > 0 && columns > 0 && attachments > 0 && bytes > 0, "BAD_BUDGET");
            maxDdlStatements = statements; maxColumns = columns; maxAttachments = attachments; maxDefinitionBytes = bytes;
        }
        private void require(Cost cost) {
            need(cost.ddlStatements <= maxDdlStatements && cost.columns <= maxColumns
                && cost.attachments <= maxAttachments && cost.definitionBytes <= maxDefinitionBytes,
                "UNIT_REQUIRES_SMALLER_OWNER_SLICE");
        }
    }
    /** A consistent preflight view; re-read the identity/version inside the writer before mutation. */
    public static final class View {
        public final String databaseId, definitionRevision, sourceDefinitionHash, originalContentRoot;
        public final int schemaRevision;
        public final long schemaCookie;
        public final boolean sourcePresent;
        public View(String databaseId, int schemaRevision, String definitionRevision, long schemaCookie,
                    String sourceDefinitionHash, String originalContentRoot, boolean sourcePresent) {
            this.databaseId = text(databaseId, "VIEW_DB"); this.schemaRevision = schemaRevision;
            this.definitionRevision = text(definitionRevision, "VIEW_REVISION");
            need(schemaCookie >= 0, "BAD_SCHEMA_COOKIE"); this.schemaCookie = schemaCookie;
            this.sourceDefinitionHash = hash(sourceDefinitionHash, "VIEW_SOURCE");
            this.originalContentRoot = hash(originalContentRoot, "VIEW_ORIGINAL_ROOT"); this.sourcePresent = sourcePresent;
        }
    }
    public static final class Ticket {
        public final Cursor before;
        public final Unit unit;
        public final View prepared;
        public final Cost plannedCost;
        public final Budget budget;
        public final String commitKey;
        public final long deadlineMonoMs;
        private Ticket(Cursor before, Unit unit, View prepared, Cost cost, Budget budget, long deadline) {
            this.before = before; this.unit = unit; this.prepared = prepared; plannedCost = cost;
            this.budget = budget; deadlineMonoMs = deadline;
            commitKey = sha256(framed("R34_SCHEMA_UNIT_COMMIT_V1", before.planHash, before.runId,
                before.phase.name(), String.valueOf(before.nextIndex), String.valueOf(before.auditRound)));
        }
    }
    private static void identity(Plan plan, Unit unit, View view) {
        need(plan.databaseId.equals(view.databaseId), "DATABASE_REPLACED");
        need(plan.schemaRevision == view.schemaRevision, "SCHEMA_REVISION_CHANGED");
        need(plan.definitionRevision.equals(view.definitionRevision), "DEFINITION_REVISION_CHANGED");
        need(unit.definition.sourceExpected == view.sourcePresent, "SOURCE_PRESENCE_CHANGED");
        need(unit.definition.sourceDefinitionHash.equals(view.sourceDefinitionHash), "SOURCE_DEFINITION_CHANGED");
    }
    public static Ticket prepare(Plan plan, Cursor cursor, View view, Cost cost, Budget budget, long deadlineMonoMs) {
        cursor(plan, cursor); need(cursor.phase != Phase.DONE, "ALREADY_DONE_REVALIDATE_CERTIFICATE");
        need(deadlineMonoMs > 0, "DEADLINE_REQUIRED"); Unit unit = plan.units.get(cursor.nextIndex);
        identity(plan, unit, view); budget.require(cost);
        if (cursor.phase == Phase.VERIFY) {
            need(cost.ddlStatements == 0, "VERIFY_MAY_NOT_RUN_DDL");
            need(cursor.auditSchemaCookie < 0 || cursor.auditSchemaCookie == view.schemaCookie,
                "RESTART_VERIFY_REQUIRED");
        }
        return new Ticket(cursor, unit, view, cost, budget, deadlineMonoMs);
    }
    public static final class Admission {
        private final Plan plan;
        private final Ticket ticket;
        private Admission(Plan plan, Ticket ticket) { this.plan = plan; this.ticket = ticket; }
    }
    /** Must run inside the same serialized transaction before DDL, cursor CAS, and receipt insertion. */
    public static Admission beforeWrite(Plan plan, Ticket ticket, Cursor durableCursor, View fresh, long nowMonoMs) {
        cursor(plan, durableCursor);
        need(ticket.before.signature().equals(durableCursor.signature()), "STALE_OWNER_OR_CURSOR");
        need(nowMonoMs < ticket.deadlineMonoMs, "DEADLINE_BEFORE_BEGIN");
        identity(plan, ticket.unit, fresh);
        need(fresh.schemaCookie == ticket.prepared.schemaCookie, "REPREPARE_SCHEMA_COOKIE");
        need(fresh.originalContentRoot.equals(ticket.prepared.originalContentRoot), "REPREPARE_ORIGINAL_INPUT");
        return new Admission(plan, ticket);
    }
    public static final class Effect {
        public final String installedDefinitionHash, originalContentRootAfter;
        public final long schemaCookieAfter;
        public final Cost actualCost;
        public Effect(String installedDefinitionHash, String originalContentRootAfter, long schemaCookieAfter, Cost actualCost) {
            this.installedDefinitionHash = hash(installedDefinitionHash, "INSTALLED_DEFINITION");
            this.originalContentRootAfter = hash(originalContentRootAfter, "ORIGINAL_AFTER");
            need(schemaCookieAfter >= 0, "BAD_AFTER_COOKIE"); this.schemaCookieAfter = schemaCookieAfter;
            this.actualCost = Objects.requireNonNull(actualCost);
        }
    }
    /** Proposed receipt payload; constructing this object is NOT a COMMIT acknowledgement. */
    public static final class ReceiptPayload {
        public final String commitKey, payloadHash, installedDefinitionHash;
        private final String beforeCursorSignature;
        public final Cursor next;
        public final long schemaCookieBefore, schemaCookieAfter;
        private ReceiptPayload(Ticket ticket, Effect effect, Cursor next) {
            commitKey = ticket.commitKey; this.next = next;
            beforeCursorSignature = ticket.before.signature();
            schemaCookieBefore = ticket.prepared.schemaCookie; schemaCookieAfter = effect.schemaCookieAfter;
            installedDefinitionHash = effect.installedDefinitionHash;
            payloadHash = sha256(framed(commitKey, ticket.before.signature(), next.signature(),
                installedDefinitionHash, ticket.prepared.originalContentRoot, effect.originalContentRootAfter,
                String.valueOf(schemaCookieBefore), String.valueOf(schemaCookieAfter),
                String.valueOf(effect.actualCost.ddlStatements), String.valueOf(effect.actualCost.columns),
                String.valueOf(effect.actualCost.attachments), String.valueOf(effect.actualCost.definitionBytes)));
        }
    }
    public static ReceiptPayload receiptPayload(Admission admission, Effect effect) {
        Plan plan = admission.plan; Ticket ticket = admission.ticket;
        cursor(plan, ticket.before); ticket.budget.require(effect.actualCost);
        need(ticket.unit.definition.protectionDefinitionHash.equals(effect.installedDefinitionHash), "EXACT_DEFINITION_MISMATCH");
        need(ticket.prepared.originalContentRoot.equals(effect.originalContentRootAfter), "PROTECTED_ORIGINAL_CHANGED");
        Cursor before = ticket.before; Phase phase = before.phase; int nextIndex = before.nextIndex + 1;
        long auditCookie = before.auditSchemaCookie;
        if (phase == Phase.VERIFY) {
            need(effect.actualCost.ddlStatements == 0 && effect.schemaCookieAfter == ticket.prepared.schemaCookie,
                "VERIFY_MUTATED_SCHEMA");
            auditCookie = ticket.prepared.schemaCookie;
            if (nextIndex == plan.units.size()) phase = Phase.DONE;
        } else if (nextIndex == plan.units.size()) { phase = Phase.VERIFY; nextIndex = 0; auditCookie = -1; }
        Cursor next = new Cursor(before.planHash, before.runId, before.ownerEpoch, phase, nextIndex,
            Math.addExact(before.revision, 1), before.fence, before.auditRound, auditCookie);
        return new ReceiptPayload(ticket, effect, next);
    }
    /** Read receipt and cursor together from a committed snapshot after the writer transaction ends. */
    public static final class ReadBack {
        public final String commitKey, payloadHash;
        public final Cursor cursor;
        public ReadBack(String commitKey, String payloadHash, Cursor cursor) {
            this.commitKey = hash(commitKey, "READBACK_COMMIT"); this.payloadHash = hash(payloadHash, "READBACK_PAYLOAD");
            this.cursor = Objects.requireNonNull(cursor);
        }
    }
    public static final class Ack {
        public final Resolution resolution;
        public final Cursor cursor;
        private Ack(Resolution resolution, Cursor cursor) { this.resolution = resolution; this.cursor = cursor; }
    }
    public static Ack resolve(Ticket ticket, ReceiptPayload payload, Outcome outcome, ReadBack readBack) {
        Objects.requireNonNull(outcome);
        need(ticket.commitKey.equals(payload.commitKey) && ticket.before.signature().equals(payload.beforeCursorSignature),
            "TICKET_PAYLOAD_MISMATCH");
        if (readBack != null) {
            need(payload.commitKey.equals(readBack.commitKey) && payload.payloadHash.equals(readBack.payloadHash),
                "DURABLE_RECEIPT_CONFLICT");
            need(payload.next.signature().equals(readBack.cursor.signature()), "RELOAD_DURABLE_CURSOR");
            return new Ack(Resolution.ACKNOWLEDGED, readBack.cursor);
        }
        if (outcome == Outcome.ROLLED_BACK) return new Ack(Resolution.RETRY_SAME_UNIT, ticket.before);
        if (outcome == Outcome.COMMITTED) throw new ProtocolViolation("COMMITTED_RECEIPT_MISSING");
        return new Ack(Resolution.WAIT_DURABLE_RECEIPT, ticket.before);
    }
    /** Proposal only: the adapter must CAS this cursor under the current lease and read it back. */
    public static Cursor restartVerification(Plan plan, Cursor current) {
        cursor(plan, current); need(current.phase != Phase.INSTALL, "INSTALL_NOT_COMPLETE");
        return new Cursor(current.planHash, current.runId, current.ownerEpoch, Phase.VERIFY, 0,
            Math.addExact(current.revision, 1), current.fence, Math.addExact(current.auditRound, 1), -1);
    }
    /** Requires a durable DONE cursor and a current DB identity/schema cookie, not table-exists ready(). */
    public static boolean certificateCurrent(Plan plan, Cursor durable, String databaseId, int schemaRevision,
                                             String definitionRevision, long schemaCookie) {
        cursor(plan, durable);
        return durable.phase == Phase.DONE && plan.databaseId.equals(databaseId) && plan.schemaRevision == schemaRevision
            && plan.definitionRevision.equals(definitionRevision) && durable.auditSchemaCookie == schemaCookie;
    }
    /** Only use after definite rollback; UNKNOWN must first resolve the original receipt. */
    public static long retryDelayMs(Wait reason, int resourceWaits) {
        Objects.requireNonNull(reason); need(resourceWaits >= 0, "NEGATIVE_WAITS");
        return Math.min(8000L, 250L << Math.min(resourceWaits, 5));
    }
}
