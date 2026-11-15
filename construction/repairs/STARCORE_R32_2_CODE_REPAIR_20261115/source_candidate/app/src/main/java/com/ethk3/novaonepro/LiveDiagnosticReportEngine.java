package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

/**
 * B15 complete report and copy owner. The full channel is never shortened; the summary is bounded
 * independently. Every chapter, chunk, copy payload and persisted snapshot carries a deterministic
 * SHA-256. Sensitive-value scanning happens before clipboard material is returned.
 */
public final class LiveDiagnosticReportEngine {
    public static final String OWNER = "B15_LIVE_DIAGNOSTIC_REPORT_AND_COPY_ONLY";
    public static final String VERSION = AppConfig.R36_RUNTIME_REPAIR_VERSION + "-B15-FULL-REPORT-R911";
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String END_MARKER = "【END_OF_LIVE_DIAGNOSTIC_REPORT】";
    public static final int DEFAULT_SUMMARY_MAX = 12000;
    public static final int DEFAULT_CHUNK_SIZE = 16000;
    public static final int MIN_CHUNK_SIZE = 2048;
    public static final int MAX_CHUNK_SIZE = 64000;

    private static final String STATE_VERSION = "b15_live_report_version";
    private static final String STATE_CREATED_AT = "b15_live_report_created_at";
    private static final String STATE_DIAGNOSIS_HASH = "b15_live_report_diagnosis_hash";
    private static final String STATE_BODY_HASH = "b15_live_report_body_hash";
    private static final String STATE_FULL_HASH = "b15_live_report_full_hash";
    private static final String STATE_FULL_LENGTH = "b15_live_report_full_length";
    private static final String STATE_SUMMARY = "b15_live_report_summary";
    private static final String STATE_FULL_TEXT = "b15_live_report_full_text";
    private static final String STATE_JSON = "b15_live_report_json";
    private static final String STATE_INDEX = "b15_live_report_index";
    private static final String STATE_VALIDATION = "b15_live_report_validation";
    private static final String STATE_PERSIST_HASH = "b15_live_report_persist_hash";

    public enum Channel {
        SUMMARY,
        FULL_TEXT,
        JSON_EVIDENCE,
        COPY_PAYLOAD,
        CHUNK
    }

    public enum CopyMode {
        EXACT_FULL_IF_CLEAN,
        ALWAYS_REDACT_SENSITIVE,
        INTERNAL_EXACT_NO_REDACTION
    }

    public enum CopyStatus {
        EXACT_FULL,
        REDACTED_FULL,
        BLOCKED_EMPTY,
        INVALID_REPORT
    }

    public enum PersistStatus {
        NOT_REQUESTED,
        STORED,
        STORE_NULL,
        WRITE_ERROR,
        HASH_MISMATCH
    }

    public enum ValidationCode {
        BODY_HASH,
        FULL_HASH,
        END_MARKER,
        CHAPTER_INDEX,
        CHUNK_ROUND_TRIP,
        JSON_LINK,
        COPY_POLICY,
        FULL_NOT_TRUNCATED,
        BUNDLE_VALID,
        REDLINE_TEXT,
        ROOT_CAUSE_CONSISTENCY,
        NO_FALSE_HEALTHY
    }

    public static final class ReportRequest {
        public final int summaryMaximum;
        public final int chunkSize;
        public final boolean persist;
        public final boolean includeJsonInFullText;
        public final boolean includeCanonicalBundle;
        public final CopyMode copyMode;

        public ReportRequest(
                int summaryMaximum,
                int chunkSize,
                boolean persist,
                boolean includeJsonInFullText,
                boolean includeCanonicalBundle,
                CopyMode copyMode) {
            this.summaryMaximum = Math.max(1000, summaryMaximum);
            this.chunkSize = Math.max(MIN_CHUNK_SIZE, Math.min(MAX_CHUNK_SIZE, chunkSize));
            this.persist = persist;
            this.includeJsonInFullText = includeJsonInFullText;
            this.includeCanonicalBundle = includeCanonicalBundle;
            this.copyMode = copyMode == null ? CopyMode.EXACT_FULL_IF_CLEAN : copyMode;
        }

        public static ReportRequest production() {
            return new ReportRequest(
                    DEFAULT_SUMMARY_MAX,
                    DEFAULT_CHUNK_SIZE,
                    true,
                    false,
                    false,
                    CopyMode.EXACT_FULL_IF_CLEAN);
        }

        public static ReportRequest replay() {
            return new ReportRequest(
                    DEFAULT_SUMMARY_MAX, 4096, false, true, true, CopyMode.EXACT_FULL_IF_CLEAN);
        }

        public String canonical() {
            return summaryMaximum
                    + "|"
                    + chunkSize
                    + "|"
                    + persist
                    + "|"
                    + includeJsonInFullText
                    + "|"
                    + includeCanonicalBundle
                    + "|"
                    + copyMode.name();
        }
    }

    public static final class ChapterSource {
        public final String id;
        public final String title;
        public final String owner;
        public final LiveResearchDiagnosisOrchestrator.Status status;
        public final LiveResearchDiagnosisOrchestrator.EvidenceGrade grade;
        public final int sampleSize;
        public final String interval;
        public final String conclusion;
        public final String counterEvidence;
        public final String action;
        public final String detail;
        public final String sourceHash;

        ChapterSource(
                String id,
                String title,
                String owner,
                LiveResearchDiagnosisOrchestrator.Status status,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade grade,
                int sampleSize,
                String interval,
                String conclusion,
                String counterEvidence,
                String action,
                String detail,
                String sourceHash) {
            this.id = safe(id);
            this.title = safe(title);
            this.owner = safe(owner);
            this.status =
                    status == null ? LiveResearchDiagnosisOrchestrator.Status.READ_ERROR : status;
            this.grade =
                    grade == null
                            ? LiveResearchDiagnosisOrchestrator.EvidenceGrade.READ_ERROR
                            : grade;
            this.sampleSize = sampleSize;
            this.interval = safe(interval);
            this.conclusion = safe(conclusion);
            this.counterEvidence = safe(counterEvidence);
            this.action = safe(action);
            this.detail = safePreserve(detail);
            this.sourceHash = safe(sourceHash);
        }

        public String canonical() {
            return escape(id)
                    + '|'
                    + escape(title)
                    + '|'
                    + escape(owner)
                    + '|'
                    + status.name()
                    + '|'
                    + grade.name()
                    + '|'
                    + sampleSize
                    + '|'
                    + escape(interval)
                    + '|'
                    + escape(conclusion)
                    + '|'
                    + escape(counterEvidence)
                    + '|'
                    + escape(action)
                    + '|'
                    + escape(detail)
                    + '|'
                    + sourceHash;
        }
    }

    public static final class ChapterIndex {
        public final int ordinal;
        public final String id;
        public final String title;
        public final String owner;
        public final LiveResearchDiagnosisOrchestrator.Status status;
        public final int relativeStart;
        public final int relativeEnd;
        public final int length;
        public final String payloadHash;
        public final String checksum;

        ChapterIndex(
                int ordinal,
                ChapterSource source,
                int relativeStart,
                int relativeEnd,
                String payloadHash) {
            this.ordinal = ordinal;
            id = source.id;
            title = source.title;
            owner = source.owner;
            status = source.status;
            this.relativeStart = relativeStart;
            this.relativeEnd = relativeEnd;
            length = Math.max(0, relativeEnd - relativeStart);
            this.payloadHash = safe(payloadHash);
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return ordinal
                    + "|"
                    + escape(id)
                    + "|"
                    + escape(title)
                    + "|"
                    + escape(owner)
                    + "|"
                    + status.name()
                    + "|"
                    + relativeStart
                    + "|"
                    + relativeEnd
                    + "|"
                    + length
                    + "|"
                    + payloadHash;
        }

        public String render() {
            return ordinal
                    + "|id="
                    + id
                    + "|title="
                    + title
                    + "|status="
                    + status.name()
                    + "|owner="
                    + owner
                    + "|chapterRegionOffset="
                    + relativeStart
                    + ".."
                    + relativeEnd
                    + "|length="
                    + length
                    + "|sha256="
                    + payloadHash;
        }
    }

    public static final class ReportChunk {
        public final int index;
        public final int count;
        public final int start;
        public final int end;
        public final String text;
        public final String sha256;

        ReportChunk(int index, int count, int start, int end, String text) {
            this.index = index;
            this.count = count;
            this.start = start;
            this.end = end;
            this.text = safePreserve(text);
            this.sha256 = V51ChecksumEngine.sha256(this.text);
        }

        public String canonical() {
            return index
                    + "|"
                    + count
                    + "|"
                    + start
                    + "|"
                    + end
                    + "|"
                    + text.length()
                    + "|"
                    + sha256;
        }
    }

    public static final class RedactionEvent {
        public final int line;
        public final String rule;
        public final String beforeHash;
        public final String afterHash;
        public final String checksum;

        RedactionEvent(int line, String rule, String before, String after) {
            this.line = line;
            this.rule = safe(rule);
            beforeHash = V51ChecksumEngine.sha256(before);
            afterHash = V51ChecksumEngine.sha256(after);
            checksum =
                    V51ChecksumEngine.sha256(
                            line + "|" + this.rule + "|" + beforeHash + "|" + afterHash);
        }
    }

    public static final class RedactionResult {
        public final String text;
        public final List<RedactionEvent> events;
        public final boolean changed;
        public final String sha256;

        RedactionResult(String text, List<RedactionEvent> events) {
            this.text = safePreserve(text);
            this.events = immutableRedactions(events);
            changed = !this.events.isEmpty();
            sha256 = V51ChecksumEngine.sha256(this.text);
        }
    }

    public static final class CopyPayload {
        public final CopyMode mode;
        public final CopyStatus status;
        public final String label;
        public final String text;
        public final int characterCount;
        public final String sourceFullHash;
        public final String payloadHash;
        public final List<RedactionEvent> redactions;
        public final boolean complete;
        public final String checksum;

        CopyPayload(
                CopyMode mode,
                CopyStatus status,
                String label,
                String text,
                String sourceFullHash,
                List<RedactionEvent> redactions,
                boolean complete) {
            this.mode = mode;
            this.status = status;
            this.label = safe(label);
            this.text = safePreserve(text);
            characterCount = this.text.length();
            this.sourceFullHash = safe(sourceFullHash);
            payloadHash = V51ChecksumEngine.sha256(this.text);
            this.redactions = immutableRedactions(redactions);
            this.complete = complete;
            checksum = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            StringBuilder out = new StringBuilder();
            out.append(mode.name())
                    .append('|')
                    .append(status.name())
                    .append('|')
                    .append(escape(label))
                    .append('|')
                    .append(characterCount)
                    .append('|')
                    .append(sourceFullHash)
                    .append('|')
                    .append(payloadHash)
                    .append('|')
                    .append(complete ? 1 : 0);
            for (int i = 0; i < redactions.size(); i++) {
                out.append('|').append(redactions.get(i).checksum);
            }
            return out.toString();
        }
    }

    public static final class ValidationFinding {
        public final ValidationCode code;
        public final boolean pass;
        public final String expected;
        public final String actual;
        public final String checksum;

        ValidationFinding(ValidationCode code, boolean pass, String expected, String actual) {
            this.code = code;
            this.pass = pass;
            this.expected = safe(expected);
            this.actual = safe(actual);
            checksum =
                    V51ChecksumEngine.sha256(
                            code.name()
                                    + '|'
                                    + pass
                                    + '|'
                                    + escape(this.expected)
                                    + '|'
                                    + escape(this.actual));
        }
    }

    public static final class ReportValidation {
        public final boolean valid;
        public final List<ValidationFinding> findings;
        public final String checksum;

        ReportValidation(List<ValidationFinding> findings) {
            this.findings = immutableValidation(findings);
            boolean all = true;
            StringBuilder canonical = new StringBuilder();
            for (int i = 0; i < this.findings.size(); i++) {
                ValidationFinding finding = this.findings.get(i);
                all &= finding.pass;
                canonical.append('|').append(finding.checksum);
            }
            valid = all;
            checksum = V51ChecksumEngine.sha256((valid ? "PASS" : "FAIL") + canonical.toString());
        }

        public String render() {
            StringBuilder out = new StringBuilder();
            out.append("valid=")
                    .append(valid)
                    .append("|validationHash=")
                    .append(checksum)
                    .append('\n');
            for (int i = 0; i < findings.size(); i++) {
                ValidationFinding finding = findings.get(i);
                out.append(finding.code.name())
                        .append('=')
                        .append(finding.pass ? "PASS" : "FAIL")
                        .append("|expected=")
                        .append(finding.expected)
                        .append("|actual=")
                        .append(finding.actual)
                        .append("|hash=")
                        .append(finding.checksum)
                        .append('\n');
            }
            return out.toString();
        }
    }

    public static final class PersistReceipt {
        public final PersistStatus status;
        public final long storedAt;
        public final String bodyHash;
        public final String fullHash;
        public final int fullLength;
        public final String error;
        public final String persistHash;

        PersistReceipt(
                PersistStatus status,
                long storedAt,
                String bodyHash,
                String fullHash,
                int fullLength,
                String error) {
            this.status = status;
            this.storedAt = Math.max(0L, storedAt);
            this.bodyHash = safe(bodyHash);
            this.fullHash = safe(fullHash);
            this.fullLength = Math.max(0, fullLength);
            this.error = safe(error);
            persistHash = V51ChecksumEngine.sha256(canonical());
        }

        public String canonical() {
            return status.name()
                    + "|"
                    + storedAt
                    + "|"
                    + bodyHash
                    + "|"
                    + fullHash
                    + "|"
                    + fullLength
                    + "|"
                    + escape(error);
        }
    }

    public static final class PersistedSnapshot {
        public final PersistStatus status;
        public final String version;
        public final long createdAt;
        public final String diagnosisHash;
        public final String bodyHash;
        public final String fullHash;
        public final int fullLength;
        public final String summary;
        public final String fullText;
        public final String json;
        public final String chapterIndex;
        public final String validation;
        public final String persistHash;
        public final String error;
        public final boolean hashVerified;

        PersistedSnapshot(
                PersistStatus status,
                String version,
                long createdAt,
                String diagnosisHash,
                String bodyHash,
                String fullHash,
                int fullLength,
                String summary,
                String fullText,
                String json,
                String chapterIndex,
                String validation,
                String persistHash,
                String error) {
            this.status = status;
            this.version = safe(version);
            this.createdAt = createdAt;
            this.diagnosisHash = safe(diagnosisHash);
            this.bodyHash = safe(bodyHash);
            this.fullHash = safe(fullHash);
            this.fullLength = fullLength;
            this.summary = safePreserve(summary);
            this.fullText = safePreserve(fullText);
            this.json = safePreserve(json);
            this.chapterIndex = safePreserve(chapterIndex);
            this.validation = safePreserve(validation);
            this.persistHash = safe(persistHash);
            this.error = safe(error);
            hashVerified =
                    status == PersistStatus.STORED
                            && this.fullLength == this.fullText.length()
                            && this.fullHash.equals(V51ChecksumEngine.sha256(this.fullText));
        }
    }

    public static final class ReportArtifact {
        public final long generatedAt;
        public final ReportRequest request;
        public final LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle;
        public final List<ChapterSource> chapters;
        public final List<ChapterIndex> chapterIndex;
        public final String chapterRegion;
        public final String body;
        public final String bodyHash;
        public final String fullText;
        public final String fullTextHash;
        public final String summary;
        public final String jsonEvidence;
        public final CopyPayload copyPayload;
        public final List<ReportChunk> chunks;
        public final ReportValidation validation;
        public final PersistReceipt persistence;

        ReportArtifact(
                long generatedAt,
                ReportRequest request,
                LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
                List<ChapterSource> chapters,
                List<ChapterIndex> chapterIndex,
                String chapterRegion,
                String body,
                String bodyHash,
                String fullText,
                String fullTextHash,
                String summary,
                String jsonEvidence,
                CopyPayload copyPayload,
                List<ReportChunk> chunks,
                ReportValidation validation,
                PersistReceipt persistence) {
            this.generatedAt = generatedAt;
            this.request = request;
            this.bundle = bundle;
            this.chapters = immutableChapters(chapters);
            this.chapterIndex = immutableIndex(chapterIndex);
            this.chapterRegion = safePreserve(chapterRegion);
            this.body = safePreserve(body);
            this.bodyHash = safe(bodyHash);
            this.fullText = safePreserve(fullText);
            this.fullTextHash = safe(fullTextHash);
            this.summary = safePreserve(summary);
            this.jsonEvidence = safePreserve(jsonEvidence);
            this.copyPayload = copyPayload;
            this.chunks = immutableChunks(chunks);
            this.validation = validation;
            this.persistence = persistence;
        }

        ReportArtifact withPersistence(PersistReceipt receipt) {
            return new ReportArtifact(
                    generatedAt,
                    request,
                    bundle,
                    chapters,
                    chapterIndex,
                    chapterRegion,
                    body,
                    bodyHash,
                    fullText,
                    fullTextHash,
                    summary,
                    jsonEvidence,
                    copyPayload,
                    chunks,
                    validation,
                    receipt);
        }

        public String channel(Channel channel) {
            if (channel == null) return fullText;
            switch (channel) {
                case SUMMARY:
                    return summary;
                case JSON_EVIDENCE:
                    return jsonEvidence;
                case COPY_PAYLOAD:
                    return copyPayload.text;
                case FULL_TEXT:
                default:
                    return fullText;
            }
        }

        public ReportChunk chunk(int oneBasedIndex) {
            int index = oneBasedIndex - 1;
            if (index < 0 || index >= chunks.size()) return null;
            return chunks.get(index);
        }
    }

    public static final class ReplayResult {
        public final ReportArtifact first;
        public final ReportArtifact second;
        public final boolean deterministicBody;
        public final boolean deterministicFull;
        public final boolean deterministicIndex;
        public final boolean chunksRoundTrip;
        public final boolean valid;
        public final String checksum;

        ReplayResult(ReportArtifact first, ReportArtifact second) {
            this.first = first;
            this.second = second;
            deterministicBody = first.bodyHash.equals(second.bodyHash);
            deterministicFull = first.fullTextHash.equals(second.fullTextHash);
            deterministicIndex =
                    indexHash(first.chapterIndex).equals(indexHash(second.chapterIndex));
            chunksRoundTrip = reconstruct(first.chunks).equals(first.fullText);
            valid =
                    deterministicBody
                            && deterministicFull
                            && deterministicIndex
                            && chunksRoundTrip
                            && first.validation.valid
                            && second.validation.valid;
            checksum =
                    V51ChecksumEngine.sha256(
                            first.bodyHash
                                    + '|'
                                    + second.bodyHash
                                    + '|'
                                    + first.fullTextHash
                                    + '|'
                                    + second.fullTextHash
                                    + '|'
                                    + valid);
        }
    }

    public String render(DbStore store) {
        return generate(store);
    }

    public String generate(DbStore store) { return "BOUNDED_SUMMARY_ONLY|full_report_owner=ReportExportServiceCP4|no_live_DB_evaluation|no_report_persistence"; }

    public ReportArtifact generateArtifact(DbStore store) { throw new IllegalStateException("REPORT_EXPORT_OWNER_SERVICE_ONLY"); }

    public ReportArtifact generateArtifact(DbStore store, ReportRequest request) { throw new IllegalStateException("REPORT_EXPORT_OWNER_SERVICE_ONLY"); }

    public ReportArtifact build(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            ReportRequest request,
            long generatedAt) { throw new IllegalStateException("REPORT_EXPORT_OWNER_SERVICE_ONLY"); }

    private ReportArtifact buildInternal(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            ReportRequest request,
            long generatedAt,
            DbStore store) {
        if (bundle == null) throw new IllegalArgumentException("B15_BUNDLE_NULL");
        ReportRequest effective = request == null ? ReportRequest.production() : request;
        List<ChapterSource> chapters = chapters(bundle);
        if (store != null) chapters.add(r811TruthChapter(store));
        ChapterBuild chapterBuild = buildChapterRegion(chapters);
        String indexText = renderIndex(chapterBuild.index);
        String json = buildJsonEvidence(bundle, chapters, chapterBuild.index, generatedAt);
        String body =
                buildBody(bundle, effective, generatedAt, indexText, chapterBuild.region, json);
        String bodyHash = V51ChecksumEngine.sha256(body);
        String fullText =
                body
                        + "\n\n【报告哈希与完整结束标志】\n"
                        + "CanonicalBodySHA256="
                        + bodyHash
                        + "\n"
                        + "DiagnosisBundleSHA256="
                        + bundle.diagnosisHash
                        + "\n"
                        + "HashAlgorithm="
                        + HASH_ALGORITHM
                        + "\n"
                        + END_MARKER
                        + "\n";
        String fullHash = V51ChecksumEngine.sha256(fullText);
        String summary =
                buildSummary(bundle, chapters, body, fullText, bodyHash, fullHash, effective);
        CopyPayload copy = buildCopyPayload(fullText, fullHash, effective.copyMode, true);
        List<ReportChunk> chunks = chunks(fullText, effective.chunkSize);
        ReportValidation validation =
                validate(
                        bundle,
                        chapters,
                        chapterBuild,
                        body,
                        bodyHash,
                        fullText,
                        fullHash,
                        json,
                        copy,
                        chunks);
        PersistReceipt notRequested =
                new PersistReceipt(
                        PersistStatus.NOT_REQUESTED, 0L, bodyHash, fullHash, fullText.length(), "");
        return new ReportArtifact(
                generatedAt,
                effective,
                bundle,
                chapters,
                chapterBuild.index,
                chapterBuild.region,
                body,
                bodyHash,
                fullText,
                fullHash,
                summary,
                json,
                copy,
                chunks,
                validation,
                notRequested);
    }

    private List<ChapterSource> chapters(LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        ArrayList<ChapterSource> chapters = new ArrayList<ChapterSource>();
        for (int i = 0; i < bundle.stages.size(); i++) {
            LiveResearchDiagnosisOrchestrator.StageArtifact stage = bundle.stages.get(i);
            LiveResearchDiagnosisOrchestrator.EvidenceGrade grade = strongestGrade(stage.evidence);
            int sample = largestSample(stage.evidence);
            String interval = interval(stage.evidence);
            String counter = counterEvidence(stage.evidence);
            String action = action(bundle, stage.stage);
            chapters.add(
                    new ChapterSource(
                            String.format(
                                    Locale.US,
                                    "C%02d-%s",
                                    Integer.valueOf(i + 1),
                                    stage.stage.name()),
                            stage.stage.title,
                            stage.owner,
                            stage.status,
                            grade,
                            sample,
                            interval,
                            stage.summary,
                            counter,
                            action,
                            renderStageDetail(stage),
                            stage.checksum));
        }
        chapters.add(findingChapter(bundle));
        chapters.add(edgeChapter(bundle));
        chapters.add(redlineChapter(bundle));
        chapters.add(validationChapter(bundle));
        return chapters;
    }

    private ChapterSource findingChapter(LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        StringBuilder detail = new StringBuilder(32768);
        for (int i = 0; i < bundle.findings.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Finding finding = bundle.findings.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(finding.id)
                    .append('|')
                    .append(finding.severity.name())
                    .append('|')
                    .append(finding.status.name())
                    .append("|confidence=")
                    .append(finding.confidence)
                    .append("|owner=")
                    .append(finding.owner)
                    .append("|file=")
                    .append(finding.codeLocation)
                    .append('\n')
                    .append("结论=")
                    .append(finding.conclusion)
                    .append('\n')
                    .append("原因=")
                    .append(finding.cause)
                    .append('\n')
                    .append("影响=")
                    .append(finding.impact)
                    .append('\n')
                    .append("施工=")
                    .append(finding.action)
                    .append('\n')
                    .append("验证=")
                    .append(finding.validation)
                    .append('\n')
                    .append("期望=")
                    .append(finding.expected)
                    .append('\n')
                    .append("回滚=")
                    .append(finding.rollback)
                    .append("\nFindingHash=")
                    .append(finding.checksum)
                    .append("\n\n");
        }
        return new ChapterSource(
                "C15-FINDING-DAG",
                "Finding完整施工账本",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.overallStatus,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.DATABASE_OBSERVATION,
                bundle.findings.size(),
                "N/A",
                "每个结论均绑定证据、反证、Owner、代码位置、施工、验证和回滚",
                "诊断结论不是模型效果证明，也不得直接解锁REAL",
                "按P0→P1→P2顺序关闭；READ_ERROR优先于模型调参",
                detail.toString(),
                hashFindings(bundle.findings));
    }

    private ChapterSource edgeChapter(LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        StringBuilder detail = new StringBuilder(16384);
        for (int i = 0; i < bundle.edges.size(); i++) {
            LiveResearchDiagnosisOrchestrator.DependencyEdge edge = bundle.edges.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(edge.fromFinding)
                    .append(" -> ")
                    .append(edge.toFinding)
                    .append('|')
                    .append(edge.kind.name())
                    .append('|')
                    .append(edge.rationale)
                    .append("|hash=")
                    .append(edge.checksum)
                    .append('\n');
        }
        return new ChapterSource(
                "C16-DEPENDENCY-DAG",
                "根因依赖DAG",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.validation.acyclic
                        ? LiveResearchDiagnosisOrchestrator.Status.PASS
                        : LiveResearchDiagnosisOrchestrator.Status.FAIL,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.STATIC_BUILD,
                bundle.edges.size(),
                "N/A",
                bundle.validation.acyclic ? "依赖图无环且引用完整" : "依赖图校验失败",
                "DAG表达诊断顺序，不代表因果关系已经由实验识别",
                "先修上游阻断点，再执行下游模型与Challenger验证",
                detail.toString(),
                hashEdges(bundle.edges));
    }

    private ChapterSource redlineChapter(LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        String detail =
                "Package="
                        + AppConfig.PACKAGE_NAME
                        + "\nGame="
                        + LiveResearchDiagnosisOrchestrator.GAME_CODE
                        + "\n"
                        + "Play=single-position|fixed-K3|max-one-REAL-per-issue\n"
                        + "Forbidden=K4|dynamicK|multi-position-REAL|chase|martingale|doubling|frequency-debt\n"
                        + "DecisionGateOwner=DecisionGateEngine_ONLY\n"
                        + "SettlementOwner=SettlementEngine_ONLY\n"
                        + "AnalysisOwner=AnalysisSingleFlightR36_ONLY\n"
                        + "IssueOwner=TargetIssueResolver_ONLY\n"
                        + "MODEL_PROVEN="
                        + V51BuildManifest.MODEL_PROVEN
                        + "\nSHADOW_ONLY="
                        + V51BuildManifest.SHADOW_ONLY
                        + "\nREAL_BET_HARD_DISABLED="
                        + V51BuildManifest.REAL_BET_HARD_DISABLED
                        + "\nREAL_AUTO_UNLOCK=FALSE\n"
                        + "DiagnosisMutation=FORBIDDEN\n";
        boolean safe =
                !V51BuildManifest.MODEL_PROVEN
                        && !V51BuildManifest.REALTIME_PROVEN
                        && !V51BuildManifest.SHADOW_ONLY
                        && !V51BuildManifest.REAL_BET_HARD_DISABLED
                        && V51CandidateRealGateR915.releaseBoundary()
                        && !AppConfig.R36_REAL_BET_UNLOCK;
        return new ChapterSource(
                "C17-IMMUTABLE-REDLINES",
                "身份、玩法与REAL不可变红线",
                "DecisionGateEngine_ONLY",
                safe
                        ? LiveResearchDiagnosisOrchestrator.Status.PASS
                        : LiveResearchDiagnosisOrchestrator.Status.FAIL,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.CONTRACT,
                1,
                "N/A",
                safe ? "未证明模型仅允许R915精确受控候选门" : "REAL安全红线被破坏",
                "工程完成、文件数量或APK大小均不能代替严格OOS与设备运行证明",
                "任一红线失败立即阻断受控候选；Research始终无直接REAL权限",
                detail,
                V51ChecksumEngine.sha256(detail));
    }

    private ChapterSource validationChapter(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        StringBuilder detail = new StringBuilder();
        detail.append("bundleValid=")
                .append(bundle.validation.valid)
                .append("\nfixedOrder=")
                .append(bundle.validation.fixedOrder)
                .append("\nuniqueFindingIds=")
                .append(bundle.validation.uniqueFindingIds)
                .append("\nevidenceResolved=")
                .append(bundle.validation.evidenceResolved)
                .append("\nedgesResolved=")
                .append(bundle.validation.edgesResolved)
                .append("\nacyclic=")
                .append(bundle.validation.acyclic)
                .append("\nhashStable=")
                .append(bundle.validation.hashStable)
                .append("\nreadOnlyContract=")
                .append(bundle.validation.readOnlyContract)
                .append("\nviolations=");
        for (int i = 0; i < bundle.validation.violations.size(); i++) {
            if (i > 0) detail.append(',');
            detail.append(bundle.validation.violations.get(i));
        }
        detail.append("\nValidationHash=").append(bundle.validation.checksum).append('\n');
        return new ChapterSource(
                "C18-BUNDLE-VALIDATION",
                "诊断编排确定性与完整性校验",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.validation.valid
                        ? LiveResearchDiagnosisOrchestrator.Status.PASS
                        : LiveResearchDiagnosisOrchestrator.Status.FAIL,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.STATIC_BUILD,
                7,
                "N/A",
                bundle.validation.valid ? "诊断Bundle校验通过" : "诊断Bundle校验失败",
                "Bundle校验通过仍不等于物理设备运行通过",
                "运行主机回放、APK ABI门和物理设备矩阵",
                detail.toString(),
                bundle.validation.checksum);
    }

    private ChapterSource r811TruthChapter(DbStore store) {
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruth(store);
        DiagnosticSnapshotR811 snapshot = DiagnosticSnapshotR811.capture(store);
        SettlementTaskStoreR36.Terminal terminal =
                SettlementTaskStoreR36.terminal(store, snapshot.latestDraw);
        String detail =
                "Build="
                        + V51BuildManifest.VERSION_NAME
                        + "\nDB_VERSION="
                        + AppConfig.DB_VERSION
                        + "\nR811_SCHEMA_REVISION="
                        + snapshot.schemaRevision
                        + "\nBusinessTruth="
                        + truth.canonical()
                        + "\nCORE="
                        + snapshot.coreStatus
                        + "\nFULL="
                        + snapshot.fullStatus
                        + "\nSHADOW predicted/settled/pending/hit/miss="
                        + snapshot.shadowPredicted
                        + '/'
                        + snapshot.shadowSettled
                        + '/'
                        + Math.max(0, snapshot.shadowPredicted - snapshot.shadowSettled)
                        + '/'
                        + snapshot.shadowHit
                        + '/'
                        + snapshot.shadowMiss
                        + "\nProvider primary issue/age/code="
                        + store.getState("r3611_provider_primary_issue", "")
                        + '/'
                        + store.getState("r3611_provider_primary_age_ms", "-1")
                        + '/'
                        + store.getState("r3611_provider_primary_code", "NO_SAMPLE")
                        + "\nProvider backup issue/age/code="
                        + store.getState("r3611_provider_backup_issue", "")
                        + '/'
                        + store.getState("r3611_provider_backup_age_ms", "-1")
                        + '/'
                        + store.getState("r3611_provider_backup_code", "NO_SAMPLE")
                        + "\nProvider selected issue/line/age="
                        + store.getState("r3611_provider_selected_issue", "")
                        + '/'
                        + store.getState("r3611_provider_selected_line", "")
                        + '/'
                        + store.getState("r3611_provider_selected_age_ms", "-1")
                        + "\nShadow backlog total/ready/waitDraw/lease/pitInvalid/corrupt="
                        + store.getState("r3611_shadow_backlog_total", "0")
                        + '/'
                        + store.getState("r3611_shadow_ready_to_settle", "0")
                        + '/'
                        + store.getState("r3611_shadow_wait_draw", "0")
                        + '/'
                        + store.getState("r3611_shadow_lease_active", "0")
                        + '/'
                        + store.getState("r3611_shadow_pit_invalid", "0")
                        + '/'
                        + store.getState("r3611_shadow_data_corrupt", "0")
                        + "\nShadow oldest pending="
                        + store.getState("r3611_shadow_oldest_pending_issue", "")
                        + "\nLatest settlement task="
                        + (terminal == null
                                ? "PENDING_OR_MISSING"
                                : terminal.taskClass
                                        + '/'
                                        + terminal.status
                                        + '/'
                                        + terminal.terminalCode
                                        + "/attempts="
                                        + terminal.attempts
                                        + "/error="
                                        + terminal.error)
                        + "\nResearch state="
                        + store.getResearchStateR811("r811_research_state", "IDLE")
                        + "\nResearch candidate="
                        + store.getResearchStateR811("r811_research_candidate", "NONE")
                        + "\nR908 settlement recovery="
                        + store.getState("r906_supervisor_last_result", "NOT_STARTED")
                        + "\nR908 Research settlement="
                        + store.getState("r908_research_settlement_status", "NOT_STARTED")
                        + "\nR908 autonomous tick="
                        + store.getState("r908_research_tick_status", "NOT_STARTED")
                        + "\nR908 historical invention="
                        + store.getState("r908_historical_invention_tick", "NOT_STARTED")
                        + "\nR908 invention OOS/walk-forward="
                        + store.getState("r908_invention_last_reliability", "NOT_AVAILABLE")
                        + "\nResearch boundary=CERTIFICATE_BOUNDED_SCORE_ONLY|DIRECT_REAL_AUTH=0|AUTO_UNLOCK=0\n"
                        + r811ResearchTruth(store, snapshot);
        LiveResearchDiagnosisOrchestrator.Status status;
        if ("READ_ERROR".equals(truth.rootCause))
            status = LiveResearchDiagnosisOrchestrator.Status.READ_ERROR;
        else if ("FAIL".equals(truth.status)) status = LiveResearchDiagnosisOrchestrator.Status.FAIL;
        else if ("DEGRADED".equals(truth.status))
            status = LiveResearchDiagnosisOrchestrator.Status.RETEST_REQUIRED;
        else status = LiveResearchDiagnosisOrchestrator.Status.PASS;
        return new ChapterSource(
                "C19-R811-RUNTIME-TRUTH",
                "R811结算、证据、SHADOW与Research统一真值",
                "RuntimeHealthLedgerR36/DiagnosticSnapshotR811",
                status,
                "READ_ERROR".equals(truth.rootCause)
                        ? LiveResearchDiagnosisOrchestrator.EvidenceGrade.READ_ERROR
                        : LiveResearchDiagnosisOrchestrator.EvidenceGrade.DATABASE_OBSERVATION,
                snapshot.shadowSettled,
                "recent30/60/120",
                "rootCause=" + truth.rootCause + "|status=" + truth.status,
                "服务进程存活不能反证issue lag；Deferred PENDING不能污染CORE",
                "按READ_ERROR→RUNTIME→SETTLEMENT/OOS→EVIDENCE→MODEL顺序处理",
                detail,
                V51ChecksumEngine.sha256(detail));
    }

    private String r811ResearchTruth(DbStore store, DiagnosticSnapshotR811 snapshot) {
        boolean schemaReady =
                store != null
                        && store.coordinatorR36() != null
                        && store.coordinatorR36().isR811ResearchSchemaReady();
        int strictOosIssues =
                researchCount(
                        store,
                        "SELECT COUNT(DISTINCT target_issue) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1");
        int strictOosHits =
                researchCount(
                        store,
                        "SELECT COUNT(DISTINCT target_issue) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1 AND hit_label=1");
        double strictOosHitRate = strictOosIssues <= 0 || strictOosHits < 0
                ? Double.NaN : strictOosHits / (double) strictOosIssues;
        double strictOosWilson = strictOosIssues <= 0 || strictOosHits < 0
                ? Double.NaN : wilsonLower(strictOosHits, strictOosIssues);
        int actionLabelIssues =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM v57_research_action_label_r811")
                        : -1;
        int actionRows =
                schemaReady
                        ? researchCount(
                                store, "SELECT COUNT(1) FROM v57_research_action_label_r811")
                        : -1;
        int expertSnapshotIssues =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM v57_research_expert_predraw_r811")
                        : -1;
        int livePredrawIssues =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM "
                                        + DbHelper.TABLE_R817_RESEARCH_PREDRAW)
                        : -1;
        int livePredrawSettled =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(1) FROM "
                                        + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                        + " WHERE hit_label IN (0,1)")
                        : -1;
        String livePredrawAdaptive =
                schemaReady ? latestResearchPredrawAdaptive(store) : "SCHEMA_NOT_READY";
        int generated =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                        + " WHERE run_id=(SELECT run_id FROM v57_research_run_r811"
                                        + " ORDER BY updated_at DESC LIMIT 1)")
                        : -1;
        int shadowEligible =
                schemaReady
                        ? researchCount(
                                store,
                                "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                        + " WHERE status IN ('SHADOW_ELIGIBLE','SHADOW_CHALLENGER')"
                                        + " AND run_id=(SELECT run_id FROM v57_research_run_r811"
                                        + " ORDER BY updated_at DESC LIMIT 1)")
                        : -1;
        String observed =
                snapshot.recent120.sample == 0
                        ? "N/A_NO_SETTLED_FUTURE_LIVE_OOS"
                        : "N="
                                + snapshot.recent120.sample
                                + "|Hit="
                                + snapshot.recent120.hit
                                + "|HitRate="
                                + snapshot.recent120.hitRate
                                + "|Wilson="
                                + snapshot.recent120.wilsonLower
                                + "|Brier="
                                + snapshot.recent120.brier;
        /* Grow with observed content instead of reserving a contiguous 256 KiB char array while
         * the business loop may already be close to its heap ceiling. */
        StringBuilder out = new StringBuilder(32768);
        out.append("\n【自主策略发现/深度学习/动态进化】\n")
                .append("R908CreationDefinition=RAW_HISTORY->PRIMITIVE_DISCOVERY->FORMULA_SYNTHESIS + ALGORITHM_GENOME_PROGRAM_SEARCH->PREDRAW_FREEZE->OOS->KEEP_MUTATE_PRUNE\n")
                .append("R908FixedMLAsMother=NO|FixedMLRole=OPTIONAL_BASELINE_OR_EXECUTOR_ONLY\n")
                .append("R908AlgorithmDefinition=NEW_ALGORITHM_REQUIRES_OWN_STATE+OWN_SCORE_PROGRAM+OWN_SETTLEMENT_UPDATE_PROGRAM+OWN_FAILURE_DOMAIN\n")
                .append("R908AlgorithmPrimitiveContract=").append(AlgorithmGenomeVmR908.explainInputs()).append('\n')
                .append("R908AlgorithmInvention=").append(store.getState("r908_algorithm_invention_tick", "NOT_STARTED")).append('\n')
                .append("R908AlgorithmSurvivors=").append(store.getState("r908_algorithm_invention_survivors", "[]")).append('\n')
                .append("R908AlgorithmMechanismCells=").append(store.getState("r908_algorithm_invention_mechanism_cells", "[]")).append('\n')
                .append("R908AlgorithmPredrawFrozen=").append(store.getState("r908_algorithm_async_frozen", "0")).append('\n')
                .append("R908AlgorithmSettlement=").append(store.getState("r908_algorithm_settlement_updates", "0")).append('\n')
                .append("R908HistoricalInvention=").append(store.getState("r908_historical_invention_tick", "NOT_STARTED")).append('\n')
                .append("R908HistoricalReliability=").append(store.getState("r908_invention_last_reliability", "NOT_AVAILABLE")).append('\n')
                .append("R908HistoricalAssets=").append(store.getState("r908_invention_last_assets", "[]")).append('\n')
                .append("R908BackgroundResearch=").append(store.getState("r908_research_tick_status", "NOT_STARTED")).append('\n')
                .append("R908ResearchPhase=").append(store.getState("r908_research_phase", "0")).append('\n')
                .append("R908CompactLearner=").append(store.getState("r906_research_orchestrator_status", "NOT_STARTED")).append('\n')
                .append("R908FtrlCompact=").append(store.getState("r906_ftrl_compact_status", "NOT_STARTED")).append('\n')
                .append("R908BackgroundLoops=").append(store.getState("r908_background_loops", "NOT_ARMED")).append("|at=").append(store.getState("r908_background_loops_at", "0")).append('\n')
                .append("R908SettlementLoop=").append(store.getState("r908_settlement_loop_status", "NOT_STARTED")).append("|at=").append(store.getState("r908_settlement_loop_at", "0")).append("|currentExact=").append(store.getState("r908_current_exact_retry_status", "NONE")).append('\n')
                .append("R908AlgorithmFutureOos=").append(store.getState("r908_algorithm_future_summary", "NO_FUTURE_OOS_YET")).append('\n')
                .append("R911ResearchSqlGate=").append(store.getState("r911_research_sql_gate", "NOT_RUN")).append('\n')
                .append("R911SettlementRuntimeTruth=").append(SettlementRuntimeTruthR911.snapshot(store)).append('\n')
                .append("ResearchSchema=")
                .append(schemaReady ? "READY" : "NOT_READY_NO_FALSE_ZERO")
                .append("\n[A Production Champion]\n")
                .append("ChampionId=")
                .append(V51BuildManifest.COHORT_ID)
                .append("\nCodeHash=")
                .append(V51ChecksumEngine.sha256(V51BuildManifest.COHORT_ID))
                .append("|HashMeaning=COHORT_IDENTITY_NOT_SOURCE_FILE\n")
                .append("ArtifactHash=N/A_PRODUCTION_ARTIFACTS_UNPROVEN\n")
                .append("DatasetHash=N/A_NO_RESEARCH_PROMOTION\nTrainedUntilIssue=N/A\n")
                .append("ModelFamily=FROZEN_B01_B15_R366_PRODUCTION\nStrictOosN=")
                .append(strictOosIssues)
                .append("\nStrictOosHit=").append(strictOosHits)
                .append("\nOverallHitRate=").append(finiteText(strictOosHitRate))
                .append("\nOverallWilsonLower95=").append(finiteText(strictOosWilson))
                .append("\nRecent120N=").append(snapshot.recent120.sample)
                .append("\nRecent120Hit=").append(snapshot.recent120.hit)
                .append("\nRecent120HitRate=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.hitRate)
                .append("\nRecent120WilsonLower95=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.wilsonLower)
                .append("\nCoverage=N/A_ELIGIBLE_DENOMINATOR_NOT_CERTIFIED")
                .append("\nBetaLower95=N/A\nECE=N/A_NO_PROVEN_PROBABILITY\nRecent120Brier=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.brier)
                .append("\nCoverageCV=N/A\nBlockMetrics=N/A\nRegimeMetrics=N/A\nLongestLose=N/A\n")
                .append("P50/P95/P99=N/A_DEVICE_RETEST_REQUIRED\nModelProof45=NOT_PROVEN\n")
                .append("[B Failure Distribution]\n");
        appendFailureDistribution(out, store);
        out.append("[C Hypotheses]\n");
        appendHypotheses(out, store, schemaReady);
        out.append("[D Newly Created Formula]\n");
        appendFormulaCandidates(out, store, schemaReady);
        out.append("[E Candidate Strategy Chain]\n")
                .append("RawHistory -> HistoricalStrategyInventionR908[atomic empirical facts] -> formula/strategy synthesis; AND ")
                .append("RawHistory -> AlgorithmInventionLabR908[primitive instruction population] -> AlgorithmGenomeR908[state+scoreProgram+updateProgram] -> ")
                .append("structural mutation + behavioral novelty -> chronological development walk-forward -> Chinese mechanism identity -> ")
                .append("exact1200 Research-only scoring -> pre-draw frozen feature/state -> exact-draw state update -> future strict OOS -> ")
                .append("block/regime/ablation/complexity evidence -> keep/mutate/prune -> SHADOW_CHALLENGER. ")
                .append("Markov/forest/neural/bandit are benchmark opponents only, never mandatory parents.\n")
                .append("ChainContractHash=")
                .append(
                        V51ChecksumEngine.sha256(
                                "R908_CHAIN|RAW_HISTORY|DISCOVERY|WF|SYMBOLIC_SYNTHESIS|1200|PREDRAW|OOS|ABLATION|EVOLVE|SHADOW"))
                .append("|ProductionOwnerMutation=0\n")
                .append("[F Search Statistics]\nGenerated=")
                .append(generated)
                .append("|StaticRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_STATIC%") : -1)
                .append("|LeakageRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_LEAKAGE%") : -1)
                .append("|WalkForwardRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_WALK_FORWARD%") : -1)
                .append("|OverfitRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_OVERFIT%") : -1)
                .append("|CalibrationRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_CALIBRATION%") : -1)
                .append("|FDRRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_FDR%") : -1)
                .append("|FrozenOosRejected=")
                .append(schemaReady ? candidateStatusCount(store, "REJECT_FROZEN%") : -1)
                .append("|ShadowEligible=")
                .append(shadowEligible)
                .append("|Promoted=0_PRODUCTION\n")
                .append("[G Top Challenger Table]\n")
                .append("CandidateId|FormulaOrModel|EligibleN|SelectedN|HitN|HitRate|Coverage|")
                .append("Wilson|Beta|ECE|Brier|CoverageCV|")
                .append("Blocks|Regimes|LongestLose|LatencyP95|RAM|CodeHash|FDR|Status\n");
        appendTopChallengers(out, store, schemaReady);
        out.append("[H Code Generation]\n")
                .append("Policy=COMPLETE_TEXT_NO_TRUNCATION|RuntimeJavacD8SignInstall=FORBIDDEN|")
                .append("OwnerAudit=NO_PRODUCTION_OWNER|StrategyBoundaryAudit=LOCKED\n");
        appendCodeGeneration(out, store, snapshot, schemaReady);
        out.append("[Deep Learning Exact Runtime/Trainer]\n")
                .append("JavaRuntimeFile=ResearchModelCandidateRuntimeR811.java\n")
                .append("OfflineTrainerFile=training/r811_autonomous_research.py\n")
                .append("Architecture=x[96];h1=ReLU(W1*x+b1)[64];h2=ReLU(W2*h1+b2)[32];")
                .append("z=W3*h2+b3[100];Q=log_domain_Sinkhorn(reshape10x10(z))\n")
                .append("Loss=L_ce+lambda2*sum(W^2);L_ce=-sum_p(log(max(eps,Q[p,actual_p])))\n")
                .append("Adam=m=.9m+.1g;v=.999v+.001g^2;bias_correct;")
                .append("w-=lr*mhat/(sqrt(vhat)+1e-8);globalGradientClip=5.0\n")
                .append("AllowedLR=1e-4,3e-4,1e-3|AllowedLambda2=0,1e-6,1e-5,1e-4|")
                .append("Init=XavierUniform(limit=sqrt(6/(fanIn+fanOut)))|Seed=xorshift64star\n")
                .append("Split=chronological train->validation->sealedFinal;shuffle=false;")
                .append("earlyStop=validation_only;sealedFinalConsumed=false\n")
                .append("AndroidPolicy=frozen_artifact_inference_and_light_FTRL_only;")
                .append("large_network_backprop=OFFLINE_ONLY\n");
        appendResearchArtifacts(out, store, schemaReady);
        out.append("[Dynamic Iteration Exact Runtime]\n")
                .append("Order=draw(t)->SettlementEngine(t)->strictOos(t)->exact1200Labels(t)->")
                .append("FTRL.update(t)->artifact.trainedUntil=t->earliestUse(t+1)\n")
                .append("FTRL=p=sigmoid(w*x);g=(p-y)x;sigma=(sqrt(n+g^2)-sqrt(n))/alpha;")
                .append("z+=g-sigma*w;n+=g^2;w=0 if abs(z)<=l1 else ")
                .append("-(z-sign(z)l1)/((beta+sqrt(n))/alpha+l2)\n")
                .append("Parameters=alpha0.05,beta1,l1=0.001,l2=0.01|")
                .append("PersistentStore=v57_research_artifact_r811|")
                .append("FutureBackfillLearning=FORBIDDEN\n")
                .append("ResearchRunState=")
                .append(store == null ? "STORE_NULL" : store.getState("r811_research_tick_status", "NOT_STARTED"))
                .append("|FtrlStatus=")
                .append(store == null ? "STORE_NULL" : store.getState("r811_adaptive_learner_status", "NOT_STARTED"))
                .append("|FtrlTrainedUntil=")
                .append(store == null ? "" : store.getState("r811_ftrl_trained_until", ""))
                .append("|PredrawStatus=")
                .append(store == null ? "STORE_NULL" : store.getState("r817_research_predraw_status", "NOT_STARTED"))
                .append("|PredrawIssues=")
                .append(livePredrawIssues)
                .append("|PredrawSettled=")
                .append(livePredrawSettled)
                .append("|AdaptivePredraw=")
                .append(livePredrawAdaptive)
                .append("\n[I Decision]\nKEEP_CHAMPION=YES|PROMOTE_TO_SHADOW=")
                .append(shadowEligible > 0 ? "CERTIFICATE_AND_FUTURE_LIVE_DEPENDENT" : "NO")
                .append("|PROMOTE_TO_PRODUCTION=FORBIDDEN|")
                .append("Reason=R811_ZERO_DRIFT_MODEL_UNPROVEN_PHYSICAL_RETEST_REQUIRED\n")
                .append("[J Truth boundary]\nResearchTarget45=0.45|Recent120Observed=")
                .append(observed)
                .append("|ModelProof45=NOT_PROVEN\n")
                .append("ProofRule=Nselected>=480;hit>=0.45;Wilson>=0.40;Beta>=0.40;")
                .append("coverage>=0.15;ECE<=0.04;stableBlocks>=3/4;FDR=true;")
                .append("futureLiveAfterFreeze=true\n")
                .append("[Production Gate Truth]\n")
                .append("ProductionExpertGateArtifactProven=false|")
                .append("ProductionRegretObserveCallSites=0_SOURCE_AUDITED|")
                .append("GateStatus=FROZEN_TELEMETRY_ONLY|GateEffectiveExperts[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateFallbackUsed[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateFallbackExpert[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateWeightSummary=")
                .append("DIAGNOSTIC_ONLY_NOT_STRATEGY_HASH\n")
                .append("[Grouped Research Dataset]\nStrictSelectedOosIssues=")
                .append(strictOosIssues)
                .append("|ResearchActionLabelIssues=")
                .append(actionLabelIssues)
                .append("|ResearchActionRows=")
                .append(actionRows)
                .append("|ResearchExpertSnapshotIssues=")
                .append(expertSnapshotIssues)
                .append("|ProofIndependentN=")
                .append(strictOosIssues)
                .append("|ActionRowsNeverCountAsProofN=true\n")
                .append("[Cross-position Assignment Research]\n")
                .append("LocationEntropy=AVAILABLE|PositionExclusivity=AVAILABLE|")
                .append("LocationVote=AVAILABLE|AssignmentEnsembleSupport=AVAILABLE|")
                .append("CurrentBestAssignmentCandidate=")
                .append(shadowEligible <= 0 ? "NONE_CERTIFIED" : "SEE_TOP_CHALLENGERS")
                .append("|GlobalOccurrencePrediction=FORBIDDEN\n")
                .append("[Candidate Executability]\n")
                .append("RUNTIME_EXECUTABLE_DSL=YES_RESEARCH_SHADOW|")
                .append("RUNTIME_EXECUTABLE_GENERIC_MODEL=YES_AFTER_VALID_FROZEN_ARTIFACT|")
                .append("BUILD_REQUIRED_JAVA=YES_NO_DYNAMIC_LOAD|")
                .append("GeneratedJavaIsAuditableSource=true|")
                .append("CurrentApkGenericDslPredrawExecution=R817_ACTIVE_WHEN_CANDIDATE_EXISTS|")
                .append("MayAuthorizeReal=false\n")
                .append("[Runtime Memory Repair Truth]\n")
                .append("HistoryProjectionExcludesRawJson=")
                .append(DrawHistoryProjectionR817.excludesRawPayload())
                .append("|OversizeRawPayloadPending=")
                .append(store == null ? -1 : DrawRawPayloadCompactorR817.pending(store))
                .append("|DeferredChunk=")
                .append(AppConfig.R36_DEFERRED_ACTION_CHUNK)
                .append("|DuplicateHistoryConversion=REMOVED|StreamingSnapshot=ENABLED|")
                .append("RankSort=STABLE_PRIMITIVE_MERGESORT|ActionHashValidation=ONE_PASS|")
                .append("OOMLedgerTruth=OOM_COUNTS_AS_OOM|RepairVersion=")
                .append(AppConfig.R36_RUNTIME_REPAIR_VERSION)
                .append("|MemoryPreflight=")
                .append(store == null
                        ? "STORE_NULL"
                        : store.getState("r811_runtime_memory_preflight", "NOT_OBSERVED"))
                .append("\n");
        return out.toString();
    }

    private void appendFailureDistribution(StringBuilder out, DbStore store) {
        ResearchFailureHypothesisEngineR811.Diagnosis diagnosis =
                ResearchFailureHypothesisEngineR811.diagnose(store);
        out.append("Status=")
                .append(diagnosis.status)
                .append("|Primary=")
                .append(diagnosis.primaryLayer)
                .append("|Checksum=")
                .append(diagnosis.checksum)
                .append('\n');
        for (int i = 0; i < diagnosis.layers.size(); i++) {
            ResearchFailureHypothesisEngineR811.LayerEvidence row = diagnosis.layers.get(i);
            out.append(row.layer)
                    .append("|count=")
                    .append(row.failureCount)
                    .append("|share=")
                    .append(row.failureShare)
                    .append("|confidenceLower95=")
                    .append(row.confidenceLower95)
                    .append("|sample=")
                    .append(row.sampleN)
                    .append("|counterEvidenceCount=")
                    .append(row.counterEvidenceCount)
                    .append('\n');
        }
        out.append("MechanisticHypothesis=")
                .append(diagnosis.hypothesis)
                .append("\nCounterEvidence=")
                .append(diagnosis.counterEvidence)
                .append('\n');
    }

    private void appendHypotheses(StringBuilder out, DbStore store, boolean ready) {
        if (!ready) {
            out.append("READ_ERROR=RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT hypothesis_id,failure_layer,mechanism,positive_evidence,"
                                            + "counter_evidence,affected_regime,candidate_family,acceptance_json,status"
                                            + " FROM v57_research_hypothesis_r811"
                                            + " WHERE run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY updated_at DESC",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append("Hypothesis#")
                        .append(count)
                        .append("|Id=")
                        .append(cursor.getString(0))
                        .append("|FailureLayer=")
                        .append(cursor.getString(1))
                        .append("|Mechanism=")
                        .append(cursor.getString(2))
                        .append("|PositiveEvidence=")
                        .append(cursor.getString(3))
                        .append("|CounterEvidence=")
                        .append(cursor.getString(4))
                        .append("|AffectedRegime=")
                        .append(cursor.getString(5))
                        .append("|CandidateFamily=")
                        .append(cursor.getString(6))
                        .append("|RequiredTestAndAcceptance=")
                        .append(cursor.getString(7))
                        .append("|Status=")
                        .append(cursor.getString(8))
                        .append('\n');
            }
            if (count == 0) out.append("NONE_YET_STRICT_OOS_GATE_NOT_MET\n");
        } catch (Throwable error) {
            out.append("READ_ERROR=").append(error.getClass().getSimpleName()).append('\n');
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void appendFormulaCandidates(StringBuilder out, DbStore store, boolean ready) {
        if (!ready) {
            out.append("NONE_PERSISTED_RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT candidate_id,formula_ast,parent_formula_ids,generation,"
                                            + "genetic_operator,complexity,feature_dependencies,dataset_hash,seed,status"
                                            + " FROM v57_research_candidate_r811 WHERE candidate_type='FORMULA'"
                                            + " AND run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY generation ASC,candidate_id ASC",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append("Formula#")
                        .append(count)
                        .append("|FormulaId=")
                        .append(cursor.getString(0))
                        .append("|CanonicalAST=")
                        .append(cursor.getString(1))
                        .append("|HumanReadableFormula=")
                        .append(cursor.getString(1))
                        .append("|ParentFormulaIds=")
                        .append(cursor.getString(2))
                        .append("|Generation=")
                        .append(cursor.getInt(3))
                        .append("|Mutation/Crossover=")
                        .append(cursor.getString(4))
                        .append("|Complexity=")
                        .append(cursor.getDouble(5))
                        .append("|FeatureDependencies=")
                        .append(cursor.getString(6))
                        .append("|PITAudit=PREDRAW_FEATURES_ONLY_GROUP_BY_TARGET_ISSUE")
                        .append("|DatasetHash=")
                        .append(cursor.getString(7))
                        .append("|Seed=")
                        .append(cursor.getLong(8))
                        .append("|Status=")
                        .append(cursor.getString(9))
                        .append('\n');
            }
            if (count == 0) out.append("NONE_YET_SCREENING_REQUIRES_STRICT_OOS_N_120\n");
        } catch (Throwable error) {
            out.append("READ_ERROR=").append(error.getClass().getSimpleName()).append('\n');
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void appendTopChallengers(StringBuilder out, DbStore store, boolean ready) {
        if (!ready) {
            out.append("READ_ERROR_RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT c.candidate_id,c.candidate_type,c.canonical_spec,"
                                            + "e.eligible_n,e.selected_n,e.hit_n,e.hit_rate,"
                                            + "e.coverage,e.wilson_lower,e.beta_lower,e.ece,e.brier,e.coverage_cv,"
                                            + "e.longest_lose,e.latency_p95_ms,e.ram_peak_bytes,c.code_hash,e.fdr_pass,"
                                            + "c.status,(SELECT fold_id FROM v57_research_experiment_r811 b"
                                            + " WHERE b.candidate_id=c.candidate_id AND b.split_role='BLOCK_STRESS' LIMIT 1),"
                                            + "(SELECT fold_id FROM v57_research_experiment_r811 r"
                                            + " WHERE r.candidate_id=c.candidate_id AND r.split_role='REGIME_STRESS' LIMIT 1)"
                                            + " FROM v57_research_candidate_r811 c LEFT JOIN"
                                            + " v57_research_experiment_r811 e ON e.candidate_id=c.candidate_id"
                                            + " AND e.split_role='FROZEN_TEST'"
                                            + " WHERE c.run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY COALESCE(e.wilson_lower,-1) DESC,"
                                            + "c.candidate_id ASC LIMIT 10",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append(cursor.getString(0))
                        .append('|')
                        .append(cursor.getString(1))
                        .append(':')
                        .append(cursor.getString(2))
                        .append("|EligibleN=")
                        .append(cursor.getInt(3))
                        .append("|SelectedN=")
                        .append(cursor.getInt(4))
                        .append("|HitN=")
                        .append(cursor.getInt(5))
                        .append("|HitRate=")
                        .append(cursor.getDouble(6))
                        .append("|Coverage=")
                        .append(cursor.getDouble(7))
                        .append("|Wilson=")
                        .append(cursor.getDouble(8))
                        .append("|Beta=")
                        .append(cursor.getDouble(9))
                        .append("|ECE=")
                        .append(cursor.getDouble(10))
                        .append("|Brier=")
                        .append(cursor.getDouble(11))
                        .append("|CoverageCV=")
                        .append(cursor.getDouble(12))
                        .append("|Blocks=")
                        .append(cursor.getString(19))
                        .append("|Regimes=")
                        .append(cursor.getString(20))
                        .append("|LongestLose=")
                        .append(cursor.getInt(13))
                        .append("|LatencyP95=")
                        .append(cursor.getDouble(14))
                        .append("|RAM=")
                        .append(cursor.getLong(15))
                        .append("|CodeHash=")
                        .append(cursor.getString(16))
                        .append("|FDR=")
                        .append(cursor.getInt(17))
                        .append("|Status=")
                        .append(cursor.getString(18))
                        .append('\n');
            }
            if (count == 0) out.append("NO_CANDIDATE_ROWS\n");
        } catch (Throwable error) {
            out.append("READ_ERROR=").append(error.getClass().getSimpleName()).append('\n');
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private void appendCodeGeneration(
            StringBuilder out, DbStore store, DiagnosticSnapshotR811 snapshot, boolean ready) {
        int count = 0;
        Cursor cursor = null;
        if (ready) {
            try {
                cursor =
                        store.readable()
                                .rawQuery(
                                        "SELECT g.candidate_id,g.bundle_path,g.patch_sha256,"
                                                + "g.manifest_sha256,g.compile_status,g.owner_audit_status,"
                                                + "g.bundle_hash,g.java_source,g.patch_text,g.manifest_text,"
                                                + "g.fixtures_text FROM v57_research_codegen_r811 g"
                                                + " JOIN v57_research_candidate_r811 c"
                                                + " ON c.candidate_id=g.candidate_id"
                                                + " WHERE c.run_id=(SELECT run_id FROM"
                                                + " v57_research_run_r811 ORDER BY updated_at DESC LIMIT 1)"
                                                + " ORDER BY g.candidate_id ASC",
                                        null);
                while (cursor.moveToNext()) {
                    count++;
                    appendCompleteCodeBundle(
                            out,
                            "PERSISTED_" + count,
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getString(9),
                            cursor.getString(10));
                }
            } catch (Throwable error) {
                out.append("CodegenReadError=")
                        .append(error.getClass().getSimpleName())
                        .append('\n');
            } finally {
                if (cursor != null) cursor.close();
            }
        }
        if (count == 0) appendUntrainedReferenceCode(out, snapshot);
    }

    private void appendCompleteCodeBundle(
            StringBuilder out,
            String ordinal,
            String candidateId,
            String path,
            String patchHash,
            String manifestHash,
            String compileStatus,
            String ownerAudit,
            String bundleHash,
            String javaSource,
            String patch,
            String manifest,
            String fixtures) {
        out.append("--- CODE_BUNDLE_BEGIN ")
                .append(ordinal)
                .append(" ---\nCandidateId=")
                .append(candidateId)
                .append("\nPath=")
                .append(path)
                .append("\nPatchHash=")
                .append(patchHash)
                .append("\nManifestHash=")
                .append(manifestHash)
                .append("\nCompileStatus=")
                .append(compileStatus)
                .append("\nOwnerAudit=")
                .append(ownerAudit)
                .append("\nStrategyBoundaryAudit=RESEARCH_ONLY_NO_REAL_AUTHORITY")
                .append("\nBundleHash=")
                .append(bundleHash)
                .append("\n--- GeneratedJava BEGIN ---\n")
                .append(safePreserve(javaSource))
                .append("\n--- GeneratedJava END ---\n--- PATCH.diff BEGIN ---\n")
                .append(safePreserve(patch))
                .append("\n--- PATCH.diff END ---\n--- manifest BEGIN ---\n")
                .append(safePreserve(manifest))
                .append("\n--- manifest END ---\n--- fixtures BEGIN ---\n")
                .append(safePreserve(fixtures))
                .append("\n--- fixtures END ---\n--- CODE_BUNDLE_END ")
                .append(ordinal)
                .append(" ---\n");
    }

    private void appendUntrainedReferenceCode(
            StringBuilder out, DiagnosticSnapshotR811 snapshot) {
        String until =
                TargetIssueResolver.isValid(snapshot.latestOos)
                        ? snapshot.latestOos
                        : "202608170001";
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "UNTRAINED_REFERENCE",
                        "TinyMLP architecture is buildable but has no learned weights or proof",
                        "Future frozen live OOS may reject every candidate",
                        "PRODUCTION_FROZEN",
                        "ResearchModelCandidateRuntimeR811",
                        "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");
        StrategyChallengerFactoryEngine.CandidateSpec candidate =
                StrategyChallengerFactoryEngine.fromHypothesis(
                        finding,
                        new StrategyChallengerFactoryEngine.ResearchDataset(
                                V51ChecksumEngine.sha256("R811_UNTRAINED_REFERENCE_DATASET"),
                                until,
                                0),
                        StrategyChallengerFactoryEngine.CandidateType.MODEL,
                        "TINY_MLP[D=96,H1=64,H2=32,O=100,loss=PERMUTATION_CE_SINKHORN,"
                                + "adam=0.9/0.999/1e-8,clip=5,lr=1e-4|3e-4|1e-3,"
                                + "lambda2=0|1e-6|1e-5|1e-4,early_stop=validation_only]",
                        false,
                        811L);
        CandidateCodeSynthesisEngine.Bundle bundle =
                CandidateCodeSynthesisEngine.synthesize(candidate);
        appendCompleteCodeBundle(
                out,
                "UNTRAINED_DESIGN_REFERENCE_NOT_OBSERVED_CANDIDATE",
                candidate.candidateId,
                "research/generated/"
                        + candidate.candidateId.replaceAll("[^A-Za-z0-9_]", "_")
                        + ".java",
                V51ChecksumEngine.sha256(bundle.patch),
                V51ChecksumEngine.sha256(bundle.manifest),
                "DESIGN_ONLY_BUILD_REQUIRED_UNTRAINED",
                "PASS_RESEARCH_ONLY_NO_OWNER",
                bundle.bundleHash,
                bundle.javaSource,
                bundle.patch,
                bundle.manifest,
                bundle.fixtures);
    }

    private void appendResearchArtifacts(StringBuilder out, DbStore store, boolean ready) {
        if (!ready) {
            out.append("LatestResearchArtifact=NONE_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT artifact_id,artifact_type,trained_until_issue,"
                                            + "feature_schema_hash,artifact_hash,status FROM"
                                            + " v57_research_artifact_r811 ORDER BY trained_until_issue DESC LIMIT 1",
                                    null);
            if (cursor.moveToFirst())
                out.append("LatestResearchArtifact=")
                        .append(cursor.getString(0))
                        .append("|Type=")
                        .append(cursor.getString(1))
                        .append("|TrainedUntil=")
                        .append(cursor.getString(2))
                        .append("|FeatureSchemaHash=")
                        .append(cursor.getString(3))
                        .append("|ArtifactHash=")
                        .append(cursor.getString(4))
                        .append("|Status=")
                        .append(cursor.getString(5))
                        .append('\n');
            else out.append("LatestResearchArtifact=NONE_UNTRAINED\n");
        } catch (Throwable error) {
            out.append("LatestResearchArtifact=READ_ERROR:")
                    .append(error.getClass().getSimpleName())
                    .append('\n');
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private int candidateStatusCount(DbStore store, String like) {
        if (store == null) return -1;
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                            + " WHERE status LIKE ? AND run_id=(SELECT run_id FROM"
                                            + " v57_research_run_r811 ORDER BY updated_at DESC LIMIT 1)",
                                    new String[] {like});
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Throwable ignored) {
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private int researchCount(DbStore store, String sql) {
        if (store == null || sql == null) return -1;
        Cursor cursor = null;
        try {
            cursor = store.readable().rawQuery(sql, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Throwable ignored) {
            return -1;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static String finiteText(double value) {
        return Double.isNaN(value) || Double.isInfinite(value)
                ? "N/A" : String.format(Locale.US, "%.8f", value);
    }

    private static double wilsonLower(int hit, int n) {
        if (n <= 0) return Double.NaN;
        double z = 1.959963984540054d;
        double p = hit / (double) n;
        double denominator = 1.0d + z * z / n;
        double center = p + z * z / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z * z / (4.0d * n)) / n);
        return Math.max(0.0d, Math.min(1.0d, (center - margin) / denominator));
    }

    private String latestResearchPredrawAdaptive(DbStore store) {
        if (store == null) return "STORE_NULL";
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT adaptive_artifact_hash,adaptive_trained_until,"
                                            + "adaptive_updates,adaptive_weight,formula_probability,adaptive_probability"
                                            + " FROM "
                                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                            + " ORDER BY frozen_at DESC,candidate_id ASC LIMIT 1",
                                    null);
            if (!cursor.moveToFirst()) return "NONE_YET";
            String artifact = cursor.getString(0) == null ? "" : cursor.getString(0);
            return "artifact="
                    + (artifact.length() == 0 ? "NONE" : artifact)
                    + "/trainedUntil="
                    + cursor.getString(1)
                    + "/updates="
                    + cursor.getLong(2)
                    + "/weight="
                    + cursor.getDouble(3)
                    + "/formulaP="
                    + cursor.getDouble(4)
                    + "/adaptiveP="
                    + cursor.getDouble(5);
        } catch (Throwable error) {
            return "READ_ERROR:" + error.getClass().getSimpleName();
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private String renderStageDetail(LiveResearchDiagnosisOrchestrator.StageArtifact stage) {
        StringBuilder detail = new StringBuilder(stage.detail.length() + 16384);
        detail.append("Stage=")
                .append(stage.stage.name())
                .append("\nOrder=")
                .append(stage.stage.order)
                .append("\nStatus=")
                .append(stage.status.name())
                .append("\nOwner=")
                .append(stage.owner)
                .append("\nSource=")
                .append(stage.source)
                .append("\nCollectedAt=")
                .append(stage.collectedAt)
                .append("\nReadError=")
                .append(stage.readError.length() == 0 ? "NONE" : stage.readError)
                .append("\nStageHash=")
                .append(stage.checksum)
                .append("\n\n【指标】\n");
        for (int i = 0; i < stage.metrics.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Metric metric = stage.metrics.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(metric.key)
                    .append('=')
                    .append(metric.value)
                    .append('|')
                    .append(metric.unit)
                    .append("|source=")
                    .append(metric.source)
                    .append("|available=")
                    .append(metric.available)
                    .append("|hash=")
                    .append(metric.checksum)
                    .append('\n');
        }
        detail.append("\n【证据与反证】\n");
        for (int i = 0; i < stage.evidence.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Evidence evidence = stage.evidence.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(evidence.id)
                    .append('|')
                    .append(evidence.grade.name())
                    .append("|source=")
                    .append(evidence.source)
                    .append("|sample=")
                    .append(evidence.sampleSize)
                    .append("|interval=")
                    .append(evidence.lowerBound)
                    .append("..")
                    .append(evidence.upperBound)
                    .append("|readSuccessful=")
                    .append(evidence.readSuccessful)
                    .append('\n')
                    .append("观察=")
                    .append(evidence.observation)
                    .append('\n')
                    .append("反证=")
                    .append(evidence.counterEvidence)
                    .append("\nEvidenceHash=")
                    .append(evidence.checksum)
                    .append("\n\n");
        }
        detail.append("【完整阶段正文】\n").append(stage.detail).append('\n');
        return detail.toString();
    }

    private ChapterBuild buildChapterRegion(List<ChapterSource> chapters) {
        StringBuilder region = new StringBuilder(524288);
        ArrayList<ChapterIndex> index = new ArrayList<ChapterIndex>();
        for (int i = 0; i < chapters.size(); i++) {
            ChapterSource chapter = chapters.get(i);
            String payload = renderChapter(i + 1, chapter);
            int start = region.length();
            region.append(payload);
            int end = region.length();
            index.add(
                    new ChapterIndex(
                            i + 1, chapter, start, end, V51ChecksumEngine.sha256(payload)));
        }
        return new ChapterBuild(region.toString(), index);
    }

    private String renderChapter(int ordinal, ChapterSource chapter) {
        StringBuilder out = new StringBuilder(chapter.detail.length() + 4096);
        out.append("\n============================================================\n")
                .append("【第")
                .append(ordinal)
                .append("章 ")
                .append(chapter.title)
                .append("】\n")
                .append("ChapterId=")
                .append(chapter.id)
                .append("\nStatus=")
                .append(chapter.status.name())
                .append("\nEvidenceGrade=")
                .append(chapter.grade.name())
                .append("\nOwner=")
                .append(chapter.owner)
                .append("\nSample=")
                .append(chapter.sampleSize)
                .append("\nInterval=")
                .append(chapter.interval)
                .append("\nSourceHash=")
                .append(chapter.sourceHash)
                .append("\nConclusion=")
                .append(chapter.conclusion)
                .append("\nCounterEvidence=")
                .append(chapter.counterEvidence)
                .append("\nExecutableAction=")
                .append(chapter.action)
                .append("\n\n")
                .append(chapter.detail)
                .append('\n');
        return out.toString();
    }

    private String renderIndex(List<ChapterIndex> index) {
        StringBuilder out = new StringBuilder(index.size() * 400);
        out.append("OffsetBasis=CHAPTER_REGION_UTF16_CHAR_INDEX\n")
                .append("EndOffset=EXCLUSIVE\n")
                .append("ChapterCount=")
                .append(index.size())
                .append('\n');
        for (int i = 0; i < index.size(); i++) {
            out.append(index.get(i).render()).append('\n');
        }
        out.append("ChapterIndexSHA256=").append(indexHash(index)).append('\n');
        return out.toString();
    }

    private String buildBody(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            ReportRequest request,
            long generatedAt,
            String index,
            String chapterRegion,
            String json) {
        StringBuilder out = new StringBuilder(chapterRegion.length() + json.length() + 32768);
        out.append("STARCORE_TIANSHU ").append(AppConfig.R36_RUNTIME_REPAIR_VERSION).append(" | B14-B15 FULL DIAGNOSTIC EVIDENCE\n")
                .append("ReportVersion=")
                .append(VERSION)
                .append("\nGeneratedAt=")
                .append(generatedAt)
                .append("\nPackage=")
                .append(AppConfig.PACKAGE_NAME)
                .append("\nAppVersion=")
                .append(V51BuildManifest.VERSION_NAME)
                .append("\nVersionCode=")
                .append(V51BuildManifest.VERSION_CODE)
                .append("\nGame=")
                .append(LiveResearchDiagnosisOrchestrator.GAME_CODE)
                .append("\nDatabase=")
                .append(AppConfig.DB_NAME)
                .append("|DB_VERSION=")
                .append(AppConfig.DB_VERSION)
                .append("|R811_SCHEMA_REVISION=")
                .append(AppConfig.R811_SCHEMA_REVISION)
                .append("\nTargetIssue=")
                .append(bundle.targetIssue)
                .append("\nOverallStatus=")
                .append(bundle.overallStatus.name())
                .append("\nPrimaryFinding=")
                .append(bundle.primaryFindingId)
                .append("\nDiagnosisBundleSHA256=")
                .append(bundle.diagnosisHash)
                .append("\nBundleValidation=")
                .append(bundle.validation.valid)
                .append("\nMODEL_PROVEN=")
                .append(V51BuildManifest.MODEL_PROVEN)
                .append("\nSHADOW_ONLY=")
                .append(V51BuildManifest.SHADOW_ONLY)
                .append("\nDIRECT_REAL_AUTH=0")
                .append("\nPHYSICAL_DEVICE_RUNTIME=RETEST_REQUIRED")
                .append("\nPhysicalRuntime=RETEST_REQUIRED\n")
                .append("FullTextPolicy=NO_TRUNCATION\n")
                .append("SummaryPolicy=BOUNDED_INDEPENDENT_CHANNEL\n")
                .append("CopyPolicy=SENSITIVE_SCAN_THEN_COMPLETE_PAYLOAD\n")
                .append("ReportMutationAuthority=NONE\n")
                .append("RequestHash=")
                .append(V51ChecksumEngine.sha256(request.canonical()))
                .append("\n\n【章节索引】\n")
                .append(index)
                .append("\n【完整章节区域开始】\n")
                .append(chapterRegion)
                .append("\n【完整章节区域结束】\n");
        if (request.includeCanonicalBundle) {
            out.append("\n【诊断Bundle规范载荷】\n")
                    .append(bundle.canonicalPayload)
                    .append("\n【诊断Bundle规范载荷结束】\n");
        }
        if (request.includeJsonInFullText) {
            out.append("\n【JSON证据包】\n").append(json).append("\n【JSON证据包结束】\n");
        } else {
            out.append("\nJSONEvidenceChannel=AVAILABLE_SEPARATELY\n")
                    .append("JSONEvidenceSHA256=")
                    .append(V51ChecksumEngine.sha256(json))
                    .append('\n');
        }
        out.append("\n【真实性边界】\n")
                .append("PlanAcceptance=NOT_ESTABLISHED_BY_THIS_RUNTIME_REPORT\n")
                .append("EngineeringBuild=THIS_RUNNING_APK\n")
                .append("PhysicalRuntime=RETEST_REQUIRED\n")
                .append("ModelProof=SEE_MODEL_AND_TRAINING_EVIDENCE_CHAPTERS\n")
                .append("RealUnlock=PER_TARGET_CERTIFICATE_AND_RUNTIME_GATE\n")
                .append("TinySampleNeverRenderedAsProof=TRUE\n")
                .append("ReadErrorNeverRenderedAsZeroOrPass=TRUE\n");
        return out.toString();
    }

    private String buildSummary(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            List<ChapterSource> chapters,
            String body,
            String fullText,
            String bodyHash,
            String fullHash,
            ReportRequest request) {
        StringBuilder out = new StringBuilder(16384);
        out.append("【星核天枢").append(AppConfig.R36_RUNTIME_REPAIR_VERSION).append("完整诊断摘要】\n")
                .append("Overall=")
                .append(bundle.overallStatus.name())
                .append("|Primary=")
                .append(bundle.primaryFindingId)
                .append("\nTargetIssue=")
                .append(bundle.targetIssue)
                .append("\nChapters=")
                .append(chapters.size())
                .append("|Findings=")
                .append(bundle.findings.size())
                .append("|Edges=")
                .append(bundle.edges.size())
                .append("\nREAD_ERROR=")
                .append(bundle.count(LiveResearchDiagnosisOrchestrator.Status.READ_ERROR))
                .append("|FAIL=")
                .append(bundle.count(LiveResearchDiagnosisOrchestrator.Status.FAIL))
                .append("|NOT_PROVEN=")
                .append(bundle.count(LiveResearchDiagnosisOrchestrator.Status.NOT_PROVEN))
                .append("|RETEST_REQUIRED=")
                .append(bundle.count(LiveResearchDiagnosisOrchestrator.Status.RETEST_REQUIRED))
                .append("\nBundleValid=")
                .append(bundle.validation.valid)
                .append("|DiagnosisHash=")
                .append(bundle.diagnosisHash)
                .append("\nBodyChars=")
                .append(body.length())
                .append("|FullChars=")
                .append(fullText.length())
                .append("\nBodySHA256=")
                .append(bodyHash)
                .append("\nFullTextSHA256=")
                .append(fullHash)
                .append("\nMODEL_PROVEN=")
                .append(V51BuildManifest.MODEL_PROVEN)
                .append("|SHADOW_ONLY=")
                .append(V51BuildManifest.SHADOW_ONLY)
                .append("|PhysicalDevice=RETEST_REQUIRED\n")
                .append("说明：摘要可以受界面长度限制；完整正文、JSON证据和分段通道独立保存，正文不截断。\n\n")
                .append("【章节状态】\n");
        for (int i = 0; i < chapters.size(); i++) {
            ChapterSource chapter = chapters.get(i);
            out.append(i + 1)
                    .append('|')
                    .append(chapter.id)
                    .append('|')
                    .append(chapter.status.name())
                    .append('|')
                    .append(chapter.title)
                    .append('|')
                    .append(chapter.conclusion)
                    .append('\n');
        }
        String text = out.toString();
        if (text.length() <= request.summaryMaximum) return text;
        String suffix =
                "\n【摘要长度保护】仅摘要通道缩短；完整正文字符数=" + fullText.length() + "，SHA256=" + fullHash + "。\n";
        int keep = Math.max(0, request.summaryMaximum - suffix.length());
        return text.substring(0, keep) + suffix;
    }

    private String buildJsonEvidence(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            List<ChapterSource> chapters,
            List<ChapterIndex> index,
            long generatedAt) {
        LiveResearchDiagnosisOrchestrator orchestrator = new LiveResearchDiagnosisOrchestrator();
        String diagnosisJson = orchestrator.toJson(bundle);
        StringBuilder out = new StringBuilder(diagnosisJson.length() + 32768);
        out.append('{');
        jsonField(out, "reportVersion", VERSION, true);
        jsonNumber(out, "generatedAt", generatedAt, true);
        jsonField(out, "package", AppConfig.PACKAGE_NAME, true);
        jsonField(out, "appVersion", V51BuildManifest.VERSION_NAME, true);
        jsonNumber(out, "versionCode", V51BuildManifest.VERSION_CODE, true);
        jsonField(out, "gameCode", LiveResearchDiagnosisOrchestrator.GAME_CODE, true);
        jsonField(out, "diagnosisHash", bundle.diagnosisHash, true);
        out.append("\"chapters\":[");
        for (int i = 0; i < chapters.size(); i++) {
            if (i > 0) out.append(',');
            ChapterSource chapter = chapters.get(i);
            ChapterIndex pointer = index.get(i);
            out.append('{');
            jsonNumber(out, "ordinal", i + 1, true);
            jsonField(out, "id", chapter.id, true);
            jsonField(out, "title", chapter.title, true);
            jsonField(out, "owner", chapter.owner, true);
            jsonField(out, "status", chapter.status.name(), true);
            jsonField(out, "evidenceGrade", chapter.grade.name(), true);
            jsonNumber(out, "sampleSize", chapter.sampleSize, true);
            jsonField(out, "interval", chapter.interval, true);
            jsonField(out, "conclusion", chapter.conclusion, true);
            jsonField(out, "counterEvidence", chapter.counterEvidence, true);
            jsonField(out, "action", chapter.action, true);
            jsonField(out, "sourceHash", chapter.sourceHash, true);
            jsonNumber(out, "relativeStart", pointer.relativeStart, true);
            jsonNumber(out, "relativeEnd", pointer.relativeEnd, true);
            jsonNumber(out, "length", pointer.length, true);
            jsonField(out, "payloadHash", pointer.payloadHash, false);
            out.append('}');
        }
        out.append("],\"diagnosis\":").append(diagnosisJson).append('}');
        return out.toString();
    }

    private CopyPayload buildCopyPayload(
            String fullText, String fullHash, CopyMode mode, boolean reportValid) {
        if (!reportValid) {
            return new CopyPayload(
                    mode,
                    CopyStatus.INVALID_REPORT,
                    "星核天枢完整诊断报告-校验失败",
                    "",
                    fullHash,
                    Collections.<RedactionEvent>emptyList(),
                    false);
        }
        if (fullText == null || fullText.length() == 0) {
            return new CopyPayload(
                    mode,
                    CopyStatus.BLOCKED_EMPTY,
                    "星核天枢完整诊断报告-空",
                    "",
                    fullHash,
                    Collections.<RedactionEvent>emptyList(),
                    false);
        }
        if (mode == CopyMode.INTERNAL_EXACT_NO_REDACTION) {
            return new CopyPayload(
                    mode,
                    CopyStatus.EXACT_FULL,
                    "星核天枢" + AppConfig.R36_RUNTIME_REPAIR_VERSION + "完整检测报告",
                    fullText,
                    fullHash,
                    Collections.<RedactionEvent>emptyList(),
                    true);
        }
        RedactionResult redacted = redact(fullText);
        if (redacted.changed || mode == CopyMode.ALWAYS_REDACT_SENSITIVE) {
            return new CopyPayload(
                    mode,
                    redacted.changed ? CopyStatus.REDACTED_FULL : CopyStatus.EXACT_FULL,
                    "星核天枢" + AppConfig.R36_RUNTIME_REPAIR_VERSION + "完整检测报告",
                    redacted.text,
                    fullHash,
                    redacted.events,
                    true);
        }
        return new CopyPayload(
                mode,
                CopyStatus.EXACT_FULL,
                "星核天枢" + AppConfig.R36_RUNTIME_REPAIR_VERSION + "完整检测报告",
                fullText,
                fullHash,
                Collections.<RedactionEvent>emptyList(),
                true);
    }

    public RedactionResult redact(String value) {
        String text = safePreserve(value);
        String[] lines = text.split("\\n", -1);
        StringBuilder out = new StringBuilder(text.length());
        ArrayList<RedactionEvent> events = new ArrayList<RedactionEvent>();
        for (int i = 0; i < lines.length; i++) {
            String before = lines[i];
            RedactedLine redacted = redactLine(before);
            out.append(redacted.text);
            if (i + 1 < lines.length) out.append('\n');
            if (redacted.changed) {
                events.add(new RedactionEvent(i + 1, redacted.rule, before, redacted.text));
            }
        }
        return new RedactionResult(out.toString(), events);
    }

    private RedactedLine redactLine(String line) {
        String text = safePreserve(line);
        String[] assignmentKeys = {
            "DUOQU_API_KEY=",
            "API_KEY=",
            "APIKEY=",
            "ACCESS_TOKEN=",
            "REFRESH_TOKEN=",
            "CLIENT_SECRET=",
            "SIGNING_PASSWORD=",
            "KEYSTORE_PASSWORD="
        };
        for (int i = 0; i < assignmentKeys.length; i++) {
            int at = indexOfIgnoreCase(text, assignmentKeys[i]);
            if (at >= 0) {
                int valueStart = at + assignmentKeys[i].length();
                String after =
                        text.substring(0, valueStart)
                                + "[REDACTED_SHA256:"
                                + shortHash(text.substring(valueStart))
                                + "]";
                return new RedactedLine(after, true, assignmentKeys[i]);
            }
        }
        int authorization = indexOfIgnoreCase(text, "Authorization:");
        if (authorization >= 0) {
            int start = authorization + "Authorization:".length();
            String after =
                    text.substring(0, start)
                            + " [REDACTED_SHA256:"
                            + shortHash(text.substring(start))
                            + "]";
            return new RedactedLine(after, true, "AUTHORIZATION_HEADER");
        }
        int bearer = indexOfIgnoreCase(text, "Bearer ");
        if (bearer >= 0) {
            int start = bearer + "Bearer ".length();
            String after =
                    text.substring(0, start)
                            + "[REDACTED_SHA256:"
                            + shortHash(text.substring(start))
                            + "]";
            return new RedactedLine(after, true, "BEARER_TOKEN");
        }
        return new RedactedLine(text, false, "");
    }

    private static final class RedactedLine {
        final String text;
        final boolean changed;
        final String rule;

        RedactedLine(String text, boolean changed, String rule) {
            this.text = safePreserve(text);
            this.changed = changed;
            this.rule = safe(rule);
        }
    }

    private static final class ChapterBuild {
        final String region;
        final List<ChapterIndex> index;

        ChapterBuild(String region, List<ChapterIndex> index) {
            this.region = safePreserve(region);
            this.index = immutableIndex(index);
        }
    }

    public List<ReportChunk> chunks(String fullText, int requestedSize) {
        String text = safePreserve(fullText);
        int size = Math.max(MIN_CHUNK_SIZE, Math.min(MAX_CHUNK_SIZE, requestedSize));
        if (text.length() == 0) return Collections.emptyList();
        int count = (text.length() + size - 1) / size;
        ArrayList<ReportChunk> chunks = new ArrayList<ReportChunk>(count);
        for (int index = 0; index < count; index++) {
            int start = index * size;
            int end = Math.min(text.length(), start + size);
            if (end < text.length()
                    && end > start
                    && Character.isHighSurrogate(text.charAt(end - 1))
                    && Character.isLowSurrogate(text.charAt(end))) {
                end--;
            }
            if (!chunks.isEmpty()) start = chunks.get(chunks.size() - 1).end;
            if (end <= start) end = Math.min(text.length(), start + size);
            chunks.add(new ReportChunk(index + 1, count, start, end, text.substring(start, end)));
        }
        if (!chunks.isEmpty() && chunks.get(chunks.size() - 1).end < text.length()) {
            ReportChunk last = chunks.remove(chunks.size() - 1);
            chunks.add(
                    new ReportChunk(
                            last.index,
                            last.count,
                            last.start,
                            text.length(),
                            text.substring(last.start)));
        }
        return Collections.unmodifiableList(chunks);
    }

    private ReportValidation validate(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            List<ChapterSource> chapters,
            ChapterBuild chapterBuild,
            String body,
            String bodyHash,
            String fullText,
            String fullHash,
            String json,
            CopyPayload copy,
            List<ReportChunk> chunks) {
        ArrayList<ValidationFinding> findings = new ArrayList<ValidationFinding>();
        findings.add(
                check(
                        ValidationCode.BODY_HASH,
                        bodyHash.equals(V51ChecksumEngine.sha256(body)),
                        bodyHash,
                        V51ChecksumEngine.sha256(body)));
        findings.add(
                check(
                        ValidationCode.FULL_HASH,
                        fullHash.equals(V51ChecksumEngine.sha256(fullText)),
                        fullHash,
                        V51ChecksumEngine.sha256(fullText)));
        findings.add(
                check(
                        ValidationCode.END_MARKER,
                        fullText.endsWith(END_MARKER + "\n"),
                        END_MARKER,
                        tail(fullText, END_MARKER.length() + 16)));
        boolean indexValid = validateChapterIndex(chapters, chapterBuild);
        findings.add(
                check(
                        ValidationCode.CHAPTER_INDEX,
                        indexValid,
                        "contiguous exact payload offsets",
                        "chapters=" + chapters.size() + "|index=" + chapterBuild.index.size()));
        String reconstructed = reconstruct(chunks);
        findings.add(
                check(
                        ValidationCode.CHUNK_ROUND_TRIP,
                        reconstructed.equals(fullText),
                        fullHash,
                        V51ChecksumEngine.sha256(reconstructed)));
        boolean jsonLinked =
                json.indexOf(bundle.diagnosisHash) >= 0
                        && json.indexOf(VERSION) >= 0
                        && json.indexOf("\"chapters\"") >= 0;
        findings.add(
                check(
                        ValidationCode.JSON_LINK,
                        jsonLinked,
                        bundle.diagnosisHash,
                        V51ChecksumEngine.sha256(json)));
        boolean copyValid =
                copy.complete
                        && (copy.status == CopyStatus.EXACT_FULL
                                ? copy.text.equals(fullText)
                                : copy.status == CopyStatus.REDACTED_FULL
                                        && !copy.redactions.isEmpty());
        findings.add(
                check(
                        ValidationCode.COPY_POLICY,
                        copyValid,
                        "exact full or auditable redacted full",
                        copy.status.name() + "|redactions=" + copy.redactions.size()));
        findings.add(
                check(
                        ValidationCode.FULL_NOT_TRUNCATED,
                        body.length() < fullText.length() && fullText.indexOf("【完整章节区域结束】") >= 0,
                        "all chapter payloads plus hashes and end marker",
                        "body=" + body.length() + "|full=" + fullText.length()));
        findings.add(
                check(
                        ValidationCode.BUNDLE_VALID,
                        bundle.validation.valid,
                        "true",
                        String.valueOf(bundle.validation.valid)));
        boolean redlines =
                fullText.indexOf("SHADOW_ONLY=false") >= 0
                        && fullText.indexOf("MODEL_PROVEN=false") >= 0
                        && fullText.indexOf("DIRECT_REAL_AUTH=0") >= 0
                        && fullText.indexOf("PhysicalRuntime=RETEST_REQUIRED") >= 0;
        findings.add(
                check(
                        ValidationCode.REDLINE_TEXT,
                        redlines,
                        "controlled-candidate/model/device truth boundary",
                        String.valueOf(redlines)));
        LiveResearchDiagnosisOrchestrator.Finding primary =
                bundle.finding(bundle.primaryFindingId);
        boolean rootConsistent =
                primary != null
                        || ((bundle.primaryFindingId.length() == 0
                                        || "NONE".equals(bundle.primaryFindingId))
                                && bundle.overallStatus
                                        == LiveResearchDiagnosisOrchestrator.Status.PASS);
        findings.add(
                check(
                        ValidationCode.ROOT_CAUSE_CONSISTENCY,
                        rootConsistent,
                        bundle.primaryFindingId,
                        primary == null ? "MISSING" : primary.id + '/' + primary.stage.name()));
        boolean hardChapter = false;
        for (int i = 0; i < chapters.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Status status = chapters.get(i).status;
            if (status == LiveResearchDiagnosisOrchestrator.Status.FAIL
                    || status == LiveResearchDiagnosisOrchestrator.Status.READ_ERROR) {
                hardChapter = true;
                break;
            }
        }
        boolean noFalseHealthy =
                bundle.overallStatus != LiveResearchDiagnosisOrchestrator.Status.PASS
                        || !hardChapter;
        findings.add(
                check(
                        ValidationCode.NO_FALSE_HEALTHY,
                        noFalseHealthy,
                        "overall PASS only when no FAIL/READ_ERROR chapter exists",
                        "overall=" + bundle.overallStatus + "|hardChapter=" + hardChapter));
        return new ReportValidation(findings);
    }

    private boolean validateChapterIndex(List<ChapterSource> chapters, ChapterBuild chapterBuild) {
        if (chapters.size() != chapterBuild.index.size()) return false;
        int expectedStart = 0;
        for (int i = 0; i < chapters.size(); i++) {
            ChapterIndex pointer = chapterBuild.index.get(i);
            if (pointer.ordinal != i + 1) return false;
            if (!pointer.id.equals(chapters.get(i).id)) return false;
            if (pointer.relativeStart != expectedStart) return false;
            if (pointer.relativeEnd < pointer.relativeStart
                    || pointer.relativeEnd > chapterBuild.region.length()) return false;
            String payload =
                    chapterBuild.region.substring(pointer.relativeStart, pointer.relativeEnd);
            if (!pointer.payloadHash.equals(V51ChecksumEngine.sha256(payload))) return false;
            if (pointer.length != payload.length()) return false;
            expectedStart = pointer.relativeEnd;
        }
        return expectedStart == chapterBuild.region.length();
    }

    private ValidationFinding check(
            ValidationCode code, boolean pass, String expected, String actual) {
        return new ValidationFinding(code, pass, expected, actual);
    }

    private PersistReceipt persist(DbStore store, final ReportArtifact artifact) {
        if (store == null) {
            return new PersistReceipt(
                    PersistStatus.STORE_NULL,
                    0L,
                    artifact.bodyHash,
                    artifact.fullTextHash,
                    artifact.fullText.length(),
                    "STORE_NULL");
        }
        if (!artifact.validation.valid) {
            return new PersistReceipt(
                    PersistStatus.HASH_MISMATCH,
                    0L,
                    artifact.bodyHash,
                    artifact.fullTextHash,
                    artifact.fullText.length(),
                    "REPORT_VALIDATION_FAILED");
        }
        final long storedAt = System.currentTimeMillis();
        final String indexText = renderIndex(artifact.chapterIndex);
        final String validationText = artifact.validation.render();
        final String persistHash =
                V51ChecksumEngine.sha256(
                        VERSION
                                + '|'
                                + artifact.generatedAt
                                + '|'
                                + artifact.bundle.diagnosisHash
                                + '|'
                                + artifact.bodyHash
                                + '|'
                                + artifact.fullTextHash
                                + '|'
                                + artifact.fullText.length()
                                + '|'
                                + V51ChecksumEngine.sha256(artifact.summary)
                                + '|'
                                + V51ChecksumEngine.sha256(artifact.jsonEvidence)
                                + '|'
                                + V51ChecksumEngine.sha256(indexText)
                                + '|'
                                + artifact.validation.checksum);
        try {
            boolean wrote =
                    store.coordinatorR36()
                            .runWrite(
                                    "B15_REPORT_EVIDENCE",
                                    new DatabaseCoordinator.WriteRunnable() {
                                        public void run(SQLiteDatabase db) {
                                            LinkedHashMap<String, String> values =
                                                    new LinkedHashMap<String, String>();
                                            values.put(STATE_VERSION, VERSION);
                                            values.put(
                                                    STATE_CREATED_AT,
                                                    String.valueOf(artifact.generatedAt));
                                            values.put(
                                                    STATE_DIAGNOSIS_HASH,
                                                    artifact.bundle.diagnosisHash);
                                            values.put(STATE_BODY_HASH, artifact.bodyHash);
                                            values.put(STATE_FULL_HASH, artifact.fullTextHash);
                                            values.put(
                                                    STATE_FULL_LENGTH,
                                                    String.valueOf(artifact.fullText.length()));
                                            values.put(STATE_SUMMARY, artifact.summary);
                                            values.put(STATE_FULL_TEXT, artifact.fullText);
                                            values.put(STATE_JSON, artifact.jsonEvidence);
                                            values.put(STATE_INDEX, indexText);
                                            values.put(STATE_VALIDATION, validationText);
                                            values.put(STATE_PERSIST_HASH, persistHash);
                                            DbStore.putStatesR36(db, values);
                                        }
                                    });
            return new PersistReceipt(
                    wrote ? PersistStatus.STORED : PersistStatus.WRITE_ERROR,
                    wrote ? storedAt : 0L,
                    artifact.bodyHash,
                    artifact.fullTextHash,
                    artifact.fullText.length(),
                    wrote ? "" : "DATABASE_COORDINATOR_WRITE_FALSE");
        } catch (Throwable error) {
            return new PersistReceipt(
                    PersistStatus.WRITE_ERROR,
                    0L,
                    artifact.bodyHash,
                    artifact.fullTextHash,
                    artifact.fullText.length(),
                    errorText(error));
        }
    }

    public PersistedSnapshot readPersisted(DbStore store) {
        if (store == null) {
            return new PersistedSnapshot(
                    PersistStatus.STORE_NULL,
                    "",
                    0L,
                    "",
                    "",
                    "",
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "STORE_NULL");
        }
        try {
            String version = store.getState(STATE_VERSION, "");
            long createdAt = parseLong(store.getState(STATE_CREATED_AT, "0"), 0L);
            String diagnosisHash = store.getState(STATE_DIAGNOSIS_HASH, "");
            String bodyHash = store.getState(STATE_BODY_HASH, "");
            String fullHash = store.getState(STATE_FULL_HASH, "");
            int fullLength = parseInt(store.getState(STATE_FULL_LENGTH, "0"), 0);
            String summary = store.getState(STATE_SUMMARY, "");
            String fullText = store.getState(STATE_FULL_TEXT, "");
            String json = store.getState(STATE_JSON, "");
            String index = store.getState(STATE_INDEX, "");
            String validation = store.getState(STATE_VALIDATION, "");
            String persistHash = store.getState(STATE_PERSIST_HASH, "");
            PersistStatus status =
                    fullText.length() == 0 ? PersistStatus.WRITE_ERROR : PersistStatus.STORED;
            PersistedSnapshot snapshot =
                    new PersistedSnapshot(
                            status,
                            version,
                            createdAt,
                            diagnosisHash,
                            bodyHash,
                            fullHash,
                            fullLength,
                            summary,
                            fullText,
                            json,
                            index,
                            validation,
                            persistHash,
                            fullText.length() == 0 ? "NO_PERSISTED_REPORT" : "");
            if (status == PersistStatus.STORED && !snapshot.hashVerified) {
                return new PersistedSnapshot(
                        PersistStatus.HASH_MISMATCH,
                        version,
                        createdAt,
                        diagnosisHash,
                        bodyHash,
                        fullHash,
                        fullLength,
                        summary,
                        fullText,
                        json,
                        index,
                        validation,
                        persistHash,
                        "FULL_TEXT_HASH_OR_LENGTH_MISMATCH");
            }
            return snapshot;
        } catch (Throwable error) {
            return new PersistedSnapshot(
                    PersistStatus.WRITE_ERROR,
                    "",
                    0L,
                    "",
                    "",
                    "",
                    0,
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    errorText(error));
        }
    }

    public ReplayResult replayDeterminism() {
        LiveResearchDiagnosisOrchestrator orchestrator = new LiveResearchDiagnosisOrchestrator();
        LiveResearchDiagnosisOrchestrator.ReplayInput fixture =
                LiveResearchDiagnosisOrchestrator.deterministicFixture();
        LiveResearchDiagnosisOrchestrator.ReplayResult bundleReplay = orchestrator.replay(fixture);
        ReportRequest request = ReportRequest.replay();
        ReportArtifact first = build(bundleReplay.first, request, fixture.createdAt);
        ReportArtifact second = build(bundleReplay.second, request, fixture.createdAt);
        return new ReplayResult(first, second);
    }

    private LiveResearchDiagnosisOrchestrator.EvidenceGrade strongestGrade(
            List<LiveResearchDiagnosisOrchestrator.Evidence> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return LiveResearchDiagnosisOrchestrator.EvidenceGrade.READ_ERROR;
        }
        LiveResearchDiagnosisOrchestrator.EvidenceGrade best =
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.CONTRACT;
        int rank = gradeRank(best);
        for (int i = 0; i < evidence.size(); i++) {
            LiveResearchDiagnosisOrchestrator.EvidenceGrade current = evidence.get(i).grade;
            int currentRank = gradeRank(current);
            if (currentRank > rank) {
                best = current;
                rank = currentRank;
            }
        }
        return best;
    }

    private int gradeRank(LiveResearchDiagnosisOrchestrator.EvidenceGrade grade) {
        switch (grade) {
            case READ_ERROR:
                return 6;
            case PHYSICAL_DEVICE:
                return 5;
            case STRICT_OOS:
                return 4;
            case DATABASE_OBSERVATION:
                return 3;
            case STATIC_BUILD:
                return 2;
            case CONTRACT:
            default:
                return 1;
        }
    }

    private int largestSample(List<LiveResearchDiagnosisOrchestrator.Evidence> evidence) {
        int sample = -1;
        if (evidence != null) {
            for (int i = 0; i < evidence.size(); i++) {
                sample = Math.max(sample, evidence.get(i).sampleSize);
            }
        }
        return sample;
    }

    private String interval(List<LiveResearchDiagnosisOrchestrator.Evidence> evidence) {
        if (evidence != null) {
            for (int i = 0; i < evidence.size(); i++) {
                LiveResearchDiagnosisOrchestrator.Evidence item = evidence.get(i);
                if (item.lowerBound >= 0.0d || item.upperBound >= 0.0d) {
                    return item.lowerBound + ".." + item.upperBound;
                }
            }
        }
        return "NOT_AVAILABLE";
    }

    private String counterEvidence(List<LiveResearchDiagnosisOrchestrator.Evidence> evidence) {
        StringBuilder out = new StringBuilder();
        if (evidence != null) {
            for (int i = 0; i < evidence.size(); i++) {
                String text = safe(evidence.get(i).counterEvidence);
                if (text.length() == 0) continue;
                if (out.length() > 0) out.append("；");
                out.append(text);
            }
        }
        return out.length() == 0 ? "NO_COUNTER_EVIDENCE_RECORDED" : out.toString();
    }

    private String action(
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle,
            LiveResearchDiagnosisOrchestrator.Stage stage) {
        for (int i = 0; i < bundle.findings.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Finding finding = bundle.findings.get(i);
            if (finding.stage == stage) return finding.action + "；验证=" + finding.validation;
        }
        return "保持精确受控候选门并补齐对应证据；Research不直接授权REAL";
    }

    private static String hashFindings(List<LiveResearchDiagnosisOrchestrator.Finding> findings) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < findings.size(); i++) out.append('|').append(findings.get(i).checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String hashEdges(List<LiveResearchDiagnosisOrchestrator.DependencyEdge> edges) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < edges.size(); i++) out.append('|').append(edges.get(i).checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String indexHash(List<ChapterIndex> index) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < index.size(); i++) out.append('|').append(index.get(i).checksum);
        return V51ChecksumEngine.sha256(out.toString());
    }

    private static String reconstruct(List<ReportChunk> chunks) {
        StringBuilder out = new StringBuilder();
        if (chunks != null) {
            for (int i = 0; i < chunks.size(); i++) out.append(chunks.get(i).text);
        }
        return out.toString();
    }

    private static int indexOfIgnoreCase(String value, String token) {
        return safePreserve(value)
                .toUpperCase(Locale.US)
                .indexOf(safe(token).toUpperCase(Locale.US));
    }

    private static String shortHash(String value) {
        String hash = V51ChecksumEngine.sha256(value);
        return hash.substring(0, Math.min(16, hash.length()));
    }

    private static String tail(String value, int count) {
        String text = safePreserve(value);
        return text.length() <= count ? text : text.substring(text.length() - count);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(safe(value));
        } catch (Throwable error) {
            return fallback;
        }
    }

    private static long parseLong(String value, long fallback) {
        try {
            return Long.parseLong(safe(value));
        } catch (Throwable error) {
            return fallback;
        }
    }

    private static String errorText(Throwable error) {
        if (error == null) return "UNKNOWN";
        String message = error.getMessage();
        String text = error.getClass().getSimpleName();
        if (message != null && message.trim().length() > 0) text += ':' + message.trim();
        return text.length() <= 1000 ? text : text.substring(0, 1000);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static String safePreserve(String value) {
        return value == null ? "" : value;
    }

    private static String escape(String value) {
        String text = safePreserve(value);
        return text.replace("\\", "\\\\")
                .replace("|", "\\|")
                .replace("\r", "\\r")
                .replace("\n", "\\n");
    }

    private static List<ChapterSource> immutableChapters(List<ChapterSource> values) {
        return Collections.unmodifiableList(
                new ArrayList<ChapterSource>(
                        values == null ? Collections.<ChapterSource>emptyList() : values));
    }

    private static List<ChapterIndex> immutableIndex(List<ChapterIndex> values) {
        return Collections.unmodifiableList(
                new ArrayList<ChapterIndex>(
                        values == null ? Collections.<ChapterIndex>emptyList() : values));
    }

    private static List<ReportChunk> immutableChunks(List<ReportChunk> values) {
        return Collections.unmodifiableList(
                new ArrayList<ReportChunk>(
                        values == null ? Collections.<ReportChunk>emptyList() : values));
    }

    private static List<RedactionEvent> immutableRedactions(List<RedactionEvent> values) {
        return Collections.unmodifiableList(
                new ArrayList<RedactionEvent>(
                        values == null ? Collections.<RedactionEvent>emptyList() : values));
    }

    private static List<ValidationFinding> immutableValidation(List<ValidationFinding> values) {
        return Collections.unmodifiableList(
                new ArrayList<ValidationFinding>(
                        values == null ? Collections.<ValidationFinding>emptyList() : values));
    }

    private static void jsonField(StringBuilder out, String key, String value, boolean comma) {
        jsonString(out, key);
        out.append(':');
        jsonString(out, value);
        if (comma) out.append(',');
    }

    private static void jsonNumber(StringBuilder out, String key, long value, boolean comma) {
        jsonString(out, key);
        out.append(':').append(value);
        if (comma) out.append(',');
    }

    private static void jsonString(StringBuilder out, String value) {
        out.append('"');
        String text = safePreserve(value);
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c == '\n') {
                out.append("\\n");
            } else if (c == '\r') {
                out.append("\\r");
            } else if (c == '\t') {
                out.append("\\t");
            } else if (c < 0x20) {
                String hex = Integer.toHexString(c);
                out.append("\\u");
                for (int pad = hex.length(); pad < 4; pad++) out.append('0');
                out.append(hex);
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }

public String r811ResearchTruthSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosticSnapshotR811 snapshot) {
        boolean schemaReady =
                store != null && report.observationBoolean("researchSchemaReady");
        int strictOosIssues =
                researchCountSnapshotCP4(report, 
                        store,
                        "SELECT COUNT(DISTINCT target_issue) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1");
        int strictOosHits =
                researchCountSnapshotCP4(report, 
                        store,
                        "SELECT COUNT(DISTINCT target_issue) FROM "
                                + DbHelper.TABLE_R35_OOS_LABEL
                                + " WHERE strict_oos=1 AND hit_label=1");
        double strictOosHitRate = strictOosIssues <= 0 || strictOosHits < 0
                ? Double.NaN : strictOosHits / (double) strictOosIssues;
        double strictOosWilson = strictOosIssues <= 0 || strictOosHits < 0
                ? Double.NaN : wilsonLowerSnapshotCP4(report, strictOosHits, strictOosIssues);
        int actionLabelIssues =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM v57_research_action_label_r811")
                        : -1;
        int actionRows =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store, "SELECT COUNT(1) FROM v57_research_action_label_r811")
                        : -1;
        int expertSnapshotIssues =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM v57_research_expert_predraw_r811")
                        : -1;
        int livePredrawIssues =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(DISTINCT target_issue) FROM "
                                        + DbHelper.TABLE_R817_RESEARCH_PREDRAW)
                        : -1;
        int livePredrawSettled =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(1) FROM "
                                        + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                        + " WHERE hit_label IN (0,1)")
                        : -1;
        String livePredrawAdaptive =
                schemaReady ? latestResearchPredrawAdaptiveSnapshotCP4(report, store) : "SCHEMA_NOT_READY";
        int generated =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                        + " WHERE run_id=(SELECT run_id FROM v57_research_run_r811"
                                        + " ORDER BY updated_at DESC LIMIT 1)")
                        : -1;
        int shadowEligible =
                schemaReady
                        ? researchCountSnapshotCP4(report, 
                                store,
                                "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                        + " WHERE status IN ('SHADOW_ELIGIBLE','SHADOW_CHALLENGER')"
                                        + " AND run_id=(SELECT run_id FROM v57_research_run_r811"
                                        + " ORDER BY updated_at DESC LIMIT 1)")
                        : -1;
        String observed =
                snapshot.recent120.sample == 0
                        ? "N/A_NO_SETTLED_FUTURE_LIVE_OOS"
                        : "N="
                                + snapshot.recent120.sample
                                + "|Hit="
                                + snapshot.recent120.hit
                                + "|HitRate="
                                + snapshot.recent120.hitRate
                                + "|Wilson="
                                + snapshot.recent120.wilsonLower
                                + "|Brier="
                                + snapshot.recent120.brier;
        /* Grow with observed content instead of reserving a contiguous 256 KiB char array while
         * the business loop may already be close to its heap ceiling. */
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 32768);
        out.append("\n【自主策略发现/深度学习/动态进化】\n")
                .append("R908CreationDefinition=RAW_HISTORY->PRIMITIVE_DISCOVERY->FORMULA_SYNTHESIS + ALGORITHM_GENOME_PROGRAM_SEARCH->PREDRAW_FREEZE->OOS->KEEP_MUTATE_PRUNE\n")
                .append("R908FixedMLAsMother=NO|FixedMLRole=OPTIONAL_BASELINE_OR_EXECUTOR_ONLY\n")
                .append("R908AlgorithmDefinition=NEW_ALGORITHM_REQUIRES_OWN_STATE+OWN_SCORE_PROGRAM+OWN_SETTLEMENT_UPDATE_PROGRAM+OWN_FAILURE_DOMAIN\n")
                .append("R908AlgorithmPrimitiveContract=").append(AlgorithmGenomeVmR908.explainInputs()).append('\n')
                .append("R908AlgorithmInvention=").append(store.getState("r908_algorithm_invention_tick", "NOT_STARTED")).append('\n')
                .append("R908AlgorithmSurvivors=").append(store.getState("r908_algorithm_invention_survivors", "[]")).append('\n')
                .append("R908AlgorithmMechanismCells=").append(store.getState("r908_algorithm_invention_mechanism_cells", "[]")).append('\n')
                .append("R908AlgorithmPredrawFrozen=").append(store.getState("r908_algorithm_async_frozen", "0")).append('\n')
                .append("R908AlgorithmSettlement=").append(store.getState("r908_algorithm_settlement_updates", "0")).append('\n')
                .append("R908HistoricalInvention=").append(store.getState("r908_historical_invention_tick", "NOT_STARTED")).append('\n')
                .append("R908HistoricalReliability=").append(store.getState("r908_invention_last_reliability", "NOT_AVAILABLE")).append('\n')
                .append("R908HistoricalAssets=").append(store.getState("r908_invention_last_assets", "[]")).append('\n')
                .append("R908BackgroundResearch=").append(store.getState("r908_research_tick_status", "NOT_STARTED")).append('\n')
                .append("R908ResearchPhase=").append(store.getState("r908_research_phase", "0")).append('\n')
                .append("R908CompactLearner=").append(store.getState("r906_research_orchestrator_status", "NOT_STARTED")).append('\n')
                .append("R908FtrlCompact=").append(store.getState("r906_ftrl_compact_status", "NOT_STARTED")).append('\n')
                .append("R908BackgroundLoops=").append(store.getState("r908_background_loops", "NOT_ARMED")).append("|at=").append(store.getState("r908_background_loops_at", "0")).append('\n')
                .append("R908SettlementLoop=").append(store.getState("r908_settlement_loop_status", "NOT_STARTED")).append("|at=").append(store.getState("r908_settlement_loop_at", "0")).append("|currentExact=").append(store.getState("r908_current_exact_retry_status", "NONE")).append('\n')
                .append("R908AlgorithmFutureOos=").append(store.getState("r908_algorithm_future_summary", "NO_FUTURE_OOS_YET")).append('\n')
                .append("R911ResearchSqlGate=").append(store.getState("r911_research_sql_gate", "NOT_RUN")).append('\n')
                .append("R911SettlementRuntimeTruth=").append(SettlementRuntimeTruthR911.snapshotSnapshotCP4(report, store)).append('\n')
                .append("ResearchSchema=")
                .append(schemaReady ? "READY" : "NOT_READY_NO_FALSE_ZERO")
                .append("\n[A Production Champion]\n")
                .append("ChampionId=")
                .append(V51BuildManifest.COHORT_ID)
                .append("\nCodeHash=")
                .append(com.ethk3.novaonepro.ReportTextCP12.sha256(report, V51BuildManifest.COHORT_ID))
                .append("|HashMeaning=COHORT_IDENTITY_NOT_SOURCE_FILE\n")
                .append("ArtifactHash=N/A_PRODUCTION_ARTIFACTS_UNPROVEN\n")
                .append("DatasetHash=N/A_NO_RESEARCH_PROMOTION\nTrainedUntilIssue=N/A\n")
                .append("ModelFamily=FROZEN_B01_B15_R366_PRODUCTION\nStrictOosN=")
                .append(strictOosIssues)
                .append("\nStrictOosHit=").append(strictOosHits)
                .append("\nOverallHitRate=").append(finiteTextSnapshotCP4(report, strictOosHitRate))
                .append("\nOverallWilsonLower95=").append(finiteTextSnapshotCP4(report, strictOosWilson))
                .append("\nRecent120N=").append(snapshot.recent120.sample)
                .append("\nRecent120Hit=").append(snapshot.recent120.hit)
                .append("\nRecent120HitRate=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.hitRate)
                .append("\nRecent120WilsonLower95=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.wilsonLower)
                .append("\nCoverage=N/A_ELIGIBLE_DENOMINATOR_NOT_CERTIFIED")
                .append("\nBetaLower95=N/A\nECE=N/A_NO_PROVEN_PROBABILITY\nRecent120Brier=")
                .append(snapshot.recent120.sample == 0 ? "N/A" : snapshot.recent120.brier)
                .append("\nCoverageCV=N/A\nBlockMetrics=N/A\nRegimeMetrics=N/A\nLongestLose=N/A\n")
                .append("P50/P95/P99=N/A_DEVICE_RETEST_REQUIRED\nModelProof45=NOT_PROVEN\n")
                .append("[B Failure Distribution]\n");
        appendFailureDistributionSnapshotCP4(report, out, store);
        out.append("[C Hypotheses]\n");
        appendHypothesesSnapshotCP4(report, out, store, schemaReady);
        out.append("[D Newly Created Formula]\n");
        appendFormulaCandidatesSnapshotCP4(report, out, store, schemaReady);
        out.append("[E Candidate Strategy Chain]\n")
                .append("RawHistory -> HistoricalStrategyInventionR908[atomic empirical facts] -> formula/strategy synthesis; AND ")
                .append("RawHistory -> AlgorithmInventionLabR908[primitive instruction population] -> AlgorithmGenomeR908[state+scoreProgram+updateProgram] -> ")
                .append("structural mutation + behavioral novelty -> chronological development walk-forward -> Chinese mechanism identity -> ")
                .append("exact1200 Research-only scoring -> pre-draw frozen feature/state -> exact-draw state update -> future strict OOS -> ")
                .append("block/regime/ablation/complexity evidence -> keep/mutate/prune -> SHADOW_CHALLENGER. ")
                .append("Markov/forest/neural/bandit are benchmark opponents only, never mandatory parents.\n")
                .append("ChainContractHash=")
                .append(
                        com.ethk3.novaonepro.ReportTextCP12.sha256(report, 
                                "R908_CHAIN|RAW_HISTORY|DISCOVERY|WF|SYMBOLIC_SYNTHESIS|1200|PREDRAW|OOS|ABLATION|EVOLVE|SHADOW"))
                .append("|ProductionOwnerMutation=0\n")
                .append("[F Search Statistics]\nGenerated=")
                .append(generated)
                .append("|StaticRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_STATIC%") : -1)
                .append("|LeakageRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_LEAKAGE%") : -1)
                .append("|WalkForwardRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_WALK_FORWARD%") : -1)
                .append("|OverfitRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_OVERFIT%") : -1)
                .append("|CalibrationRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_CALIBRATION%") : -1)
                .append("|FDRRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_FDR%") : -1)
                .append("|FrozenOosRejected=")
                .append(schemaReady ? candidateStatusCountSnapshotCP4(report, store, "REJECT_FROZEN%") : -1)
                .append("|ShadowEligible=")
                .append(shadowEligible)
                .append("|Promoted=0_PRODUCTION\n")
                .append("[G Top Challenger Table]\n")
                .append("CandidateId|FormulaOrModel|EligibleN|SelectedN|HitN|HitRate|Coverage|")
                .append("Wilson|Beta|ECE|Brier|CoverageCV|")
                .append("Blocks|Regimes|LongestLose|LatencyP95|RAM|CodeHash|FDR|Status\n");
        appendTopChallengersSnapshotCP4(report, out, store, schemaReady);
        out.append("[H Code Generation]\n")
                .append("Policy=COMPLETE_TEXT_NO_TRUNCATION|RuntimeJavacD8SignInstall=FORBIDDEN|")
                .append("OwnerAudit=NO_PRODUCTION_OWNER|StrategyBoundaryAudit=LOCKED\n");
        appendCodeGenerationSnapshotCP4(report, out, store, snapshot, schemaReady);
        out.append("[Deep Learning Exact Runtime/Trainer]\n")
                .append("JavaRuntimeFile=ResearchModelCandidateRuntimeR811.java\n")
                .append("OfflineTrainerFile=training/r811_autonomous_research.py\n")
                .append("Architecture=x[96];h1=ReLU(W1*x+b1)[64];h2=ReLU(W2*h1+b2)[32];")
                .append("z=W3*h2+b3[100];Q=log_domain_Sinkhorn(reshape10x10(z))\n")
                .append("Loss=L_ce+lambda2*sum(W^2);L_ce=-sum_p(log(max(eps,Q[p,actual_p])))\n")
                .append("Adam=m=.9m+.1g;v=.999v+.001g^2;bias_correct;")
                .append("w-=lr*mhat/(sqrt(vhat)+1e-8);globalGradientClip=5.0\n")
                .append("AllowedLR=1e-4,3e-4,1e-3|AllowedLambda2=0,1e-6,1e-5,1e-4|")
                .append("Init=XavierUniform(limit=sqrt(6/(fanIn+fanOut)))|Seed=xorshift64star\n")
                .append("Split=chronological train->validation->sealedFinal;shuffle=false;")
                .append("earlyStop=validation_only;sealedFinalConsumed=false\n")
                .append("AndroidPolicy=frozen_artifact_inference_and_light_FTRL_only;")
                .append("large_network_backprop=OFFLINE_ONLY\n");
        appendResearchArtifactsSnapshotCP4(report, out, store, schemaReady);
        out.append("[Dynamic Iteration Exact Runtime]\n")
                .append("Order=draw(t)->SettlementEngine(t)->strictOos(t)->exact1200Labels(t)->")
                .append("FTRL.update(t)->artifact.trainedUntil=t->earliestUse(t+1)\n")
                .append("FTRL=p=sigmoid(w*x);g=(p-y)x;sigma=(sqrt(n+g^2)-sqrt(n))/alpha;")
                .append("z+=g-sigma*w;n+=g^2;w=0 if abs(z)<=l1 else ")
                .append("-(z-sign(z)l1)/((beta+sqrt(n))/alpha+l2)\n")
                .append("Parameters=alpha0.05,beta1,l1=0.001,l2=0.01|")
                .append("PersistentStore=v57_research_artifact_r811|")
                .append("FutureBackfillLearning=FORBIDDEN\n")
                .append("ResearchRunState=")
                .append(store == null ? "STORE_NULL" : store.getState("r811_research_tick_status", "NOT_STARTED"))
                .append("|FtrlStatus=")
                .append(store == null ? "STORE_NULL" : store.getState("r811_adaptive_learner_status", "NOT_STARTED"))
                .append("|FtrlTrainedUntil=")
                .append(store == null ? "" : store.getState("r811_ftrl_trained_until", ""))
                .append("|PredrawStatus=")
                .append(store == null ? "STORE_NULL" : store.getState("r817_research_predraw_status", "NOT_STARTED"))
                .append("|PredrawIssues=")
                .append(livePredrawIssues)
                .append("|PredrawSettled=")
                .append(livePredrawSettled)
                .append("|AdaptivePredraw=")
                .append(livePredrawAdaptive)
                .append("\n[I Decision]\nKEEP_CHAMPION=YES|PROMOTE_TO_SHADOW=")
                .append(shadowEligible > 0 ? "CERTIFICATE_AND_FUTURE_LIVE_DEPENDENT" : "NO")
                .append("|PROMOTE_TO_PRODUCTION=FORBIDDEN|")
                .append("Reason=R811_ZERO_DRIFT_MODEL_UNPROVEN_PHYSICAL_RETEST_REQUIRED\n")
                .append("[J Truth boundary]\nResearchTarget45=0.45|Recent120Observed=")
                .append(observed)
                .append("|ModelProof45=NOT_PROVEN\n")
                .append("ProofRule=Nselected>=480;hit>=0.45;Wilson>=0.40;Beta>=0.40;")
                .append("coverage>=0.15;ECE<=0.04;stableBlocks>=3/4;FDR=true;")
                .append("futureLiveAfterFreeze=true\n")
                .append("[Production Gate Truth]\n")
                .append("ProductionExpertGateArtifactProven=false|")
                .append("ProductionRegretObserveCallSites=0_SOURCE_AUDITED|")
                .append("GateStatus=FROZEN_TELEMETRY_ONLY|GateEffectiveExperts[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateFallbackUsed[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateFallbackExpert[10]=")
                .append("PREDRAW_SIDECAR_WHEN_AVAILABLE|GateWeightSummary=")
                .append("DIAGNOSTIC_ONLY_NOT_STRATEGY_HASH\n")
                .append("[Grouped Research Dataset]\nStrictSelectedOosIssues=")
                .append(strictOosIssues)
                .append("|ResearchActionLabelIssues=")
                .append(actionLabelIssues)
                .append("|ResearchActionRows=")
                .append(actionRows)
                .append("|ResearchExpertSnapshotIssues=")
                .append(expertSnapshotIssues)
                .append("|ProofIndependentN=")
                .append(strictOosIssues)
                .append("|ActionRowsNeverCountAsProofN=true\n")
                .append("[Cross-position Assignment Research]\n")
                .append("LocationEntropy=AVAILABLE|PositionExclusivity=AVAILABLE|")
                .append("LocationVote=AVAILABLE|AssignmentEnsembleSupport=AVAILABLE|")
                .append("CurrentBestAssignmentCandidate=")
                .append(shadowEligible <= 0 ? "NONE_CERTIFIED" : "SEE_TOP_CHALLENGERS")
                .append("|GlobalOccurrencePrediction=FORBIDDEN\n")
                .append("[Candidate Executability]\n")
                .append("RUNTIME_EXECUTABLE_DSL=YES_RESEARCH_SHADOW|")
                .append("RUNTIME_EXECUTABLE_GENERIC_MODEL=YES_AFTER_VALID_FROZEN_ARTIFACT|")
                .append("BUILD_REQUIRED_JAVA=YES_NO_DYNAMIC_LOAD|")
                .append("GeneratedJavaIsAuditableSource=true|")
                .append("CurrentApkGenericDslPredrawExecution=R817_ACTIVE_WHEN_CANDIDATE_EXISTS|")
                .append("MayAuthorizeReal=false\n")
                .append("[Runtime Memory Repair Truth]\n")
                .append("HistoryProjectionExcludesRawJson=")
                .append(DrawHistoryProjectionR817.excludesRawPayload())
                .append("|OversizeRawPayloadPending=")
                .append(store == null ? -1 : DrawRawPayloadCompactorR817.pendingSnapshotCP4(report, store))
                .append("|DeferredChunk=")
                .append(AppConfig.R36_DEFERRED_ACTION_CHUNK)
                .append("|DuplicateHistoryConversion=REMOVED|StreamingSnapshot=ENABLED|")
                .append("RankSort=STABLE_PRIMITIVE_MERGESORT|ActionHashValidation=ONE_PASS|")
                .append("OOMLedgerTruth=OOM_COUNTS_AS_OOM|RepairVersion=")
                .append(AppConfig.R36_RUNTIME_REPAIR_VERSION)
                .append("|MemoryPreflight=")
                .append(store == null
                        ? "STORE_NULL"
                        : store.getState("r811_runtime_memory_preflight", "NOT_OBSERVED"))
                .append("\n");
        return out.toString();
    }

public void appendResearchArtifactsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean ready) {
        if (!ready) {
            out.append("LatestResearchArtifact=NONE_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT artifact_id,artifact_type,trained_until_issue,"
                                            + "feature_schema_hash,artifact_hash,status FROM"
                                            + " v57_research_artifact_r811 ORDER BY trained_until_issue DESC LIMIT 1",
                                    null);
            if (cursor.moveToFirst())
                out.append("LatestResearchArtifact=")
                        .append(cursor.getString(0))
                        .append("|Type=")
                        .append(cursor.getString(1))
                        .append("|TrainedUntil=")
                        .append(cursor.getString(2))
                        .append("|FeatureSchemaHash=")
                        .append(cursor.getString(3))
                        .append("|ArtifactHash=")
                        .append(cursor.getString(4))
                        .append("|Status=")
                        .append(cursor.getString(5))
                        .append('\n');
            else out.append("LatestResearchArtifact=NONE_UNTRAINED\n");
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public void appendCodeGenerationSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store, DiagnosticSnapshotR811 snapshot, boolean ready) {
        int count = 0;
        Cursor cursor = null;
        if (ready) {
            try {
                cursor =
                        report.select(
                                        "SELECT g.candidate_id,g.bundle_path,g.patch_sha256,"
                                                + "g.manifest_sha256,g.compile_status,g.owner_audit_status,"
                                                + "g.bundle_hash,"+ReportBoundedSqlCP12.fieldReference(report,"v57_research_codegen_r811","g","java_source")+","+ReportBoundedSqlCP12.fieldReference(report,"v57_research_codegen_r811","g","patch_text")+","+ReportBoundedSqlCP12.fieldReference(report,"v57_research_codegen_r811","g","manifest_text")+","+ReportBoundedSqlCP12.fieldReference(report,"v57_research_codegen_r811","g","fixtures_text")+" FROM v57_research_codegen_r811 g"
                                                + " JOIN v57_research_candidate_r811 c"
                                                + " ON c.candidate_id=g.candidate_id"
                                                + " WHERE c.run_id=(SELECT run_id FROM"
                                                + " v57_research_run_r811 ORDER BY updated_at DESC LIMIT 1)"
                                                + " ORDER BY g.candidate_id ASC",
                                        null);
                while (cursor.moveToNext()) {
                    count++;
                    appendCompleteCodeBundleSnapshotCP4(report, 
                            out,
                            "PERSISTED_" + count,
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3),
                            cursor.getString(4),
                            cursor.getString(5),
                            cursor.getString(6),
                            cursor.getString(7),
                            cursor.getString(8),
                            cursor.getString(9),
                            cursor.getString(10));
                }
            } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
                if (cursor != null) cursor.close();
            }
        }
        if (count == 0) appendUntrainedReferenceCodeSnapshotCP4(report, out, snapshot);
    }

public void appendUntrainedReferenceCodeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportTextCP12 out, DiagnosticSnapshotR811 snapshot) {
        String until =
                TargetIssueResolver.isValid(snapshot.latestOos)
                        ? snapshot.latestOos
                        : "202608170001";
        StrategyChallengerFactoryEngine.FindingBundle finding =
                new StrategyChallengerFactoryEngine.FindingBundle(
                        "UNTRAINED_REFERENCE",
                        "TinyMLP architecture is buildable but has no learned weights or proof",
                        "Future frozen live OOS may reject every candidate",
                        "PRODUCTION_FROZEN",
                        "ResearchModelCandidateRuntimeR811",
                        "DecisionGateEngine,SettlementEngine,REAL_BET_UNLOCK");
        StrategyChallengerFactoryEngine.CandidateSpec candidate =
                StrategyChallengerFactoryEngine.fromHypothesis(
                        finding,
                        new StrategyChallengerFactoryEngine.ResearchDataset(
                                com.ethk3.novaonepro.ReportTextCP12.sha256(report, "R811_UNTRAINED_REFERENCE_DATASET"),
                                until,
                                0),
                        StrategyChallengerFactoryEngine.CandidateType.MODEL,
                        "TINY_MLP[D=96,H1=64,H2=32,O=100,loss=PERMUTATION_CE_SINKHORN,"
                                + "adam=0.9/0.999/1e-8,clip=5,lr=1e-4|3e-4|1e-3,"
                                + "lambda2=0|1e-6|1e-5|1e-4,early_stop=validation_only]",
                        false,
                        811L);
        CandidateCodeSynthesisEngine.Bundle bundle =
                CandidateCodeSynthesisEngine.synthesize(candidate);
        appendCompleteCodeBundleSnapshotCP4(report, 
                out,
                "UNTRAINED_DESIGN_REFERENCE_NOT_OBSERVED_CANDIDATE",
                candidate.candidateId,
                "research/generated/"
                        + candidate.candidateId.replaceAll("[^A-Za-z0-9_]", "_")
                        + ".java",
                com.ethk3.novaonepro.ReportTextCP12.sha256(report, bundle.patch),
                com.ethk3.novaonepro.ReportTextCP12.sha256(report, bundle.manifest),
                "DESIGN_ONLY_BUILD_REQUIRED_UNTRAINED",
                "PASS_RESEARCH_ONLY_NO_OWNER",
                bundle.bundleHash,
                bundle.javaSource,
                bundle.patch,
                bundle.manifest,
                bundle.fixtures);
    }

public void appendCompleteCodeBundleSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            com.ethk3.novaonepro.ReportTextCP12 out,
            String ordinal,
            String candidateId,
            String path,
            String patchHash,
            String manifestHash,
            String compileStatus,
            String ownerAudit,
            String bundleHash,
            String javaSource,
            String patch,
            String manifest,
            String fixtures) {
        out.append("--- CODE_BUNDLE_BEGIN ")
                .append(ordinal)
                .append(" ---\nCandidateId=")
                .append(candidateId)
                .append("\nPath=")
                .append(path)
                .append("\nPatchHash=")
                .append(patchHash)
                .append("\nManifestHash=")
                .append(manifestHash)
                .append("\nCompileStatus=")
                .append(compileStatus)
                .append("\nOwnerAudit=")
                .append(ownerAudit)
                .append("\nStrategyBoundaryAudit=RESEARCH_ONLY_NO_REAL_AUTHORITY")
                .append("\nBundleHash=")
                .append(bundleHash)
                .append("\n--- GeneratedJava BEGIN ---\n")
                .append(safePreserveSnapshotCP4(report, javaSource))
                .append("\n--- GeneratedJava END ---\n--- PATCH.diff BEGIN ---\n")
                .append(safePreserveSnapshotCP4(report, patch))
                .append("\n--- PATCH.diff END ---\n--- manifest BEGIN ---\n")
                .append(safePreserveSnapshotCP4(report, manifest))
                .append("\n--- manifest END ---\n--- fixtures BEGIN ---\n")
                .append(safePreserveSnapshotCP4(report, fixtures))
                .append("\n--- fixtures END ---\n--- CODE_BUNDLE_END ")
                .append(ordinal)
                .append(" ---\n");
    }

public static String safePreserveSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value;
    }

public void appendTopChallengersSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean ready) {
        if (!ready) {
            out.append("READ_ERROR_RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT c.candidate_id,c.candidate_type,c.canonical_spec,"
                                            + "e.eligible_n,e.selected_n,e.hit_n,e.hit_rate,"
                                            + "e.coverage,e.wilson_lower,e.beta_lower,e.ece,e.brier,e.coverage_cv,"
                                            + "e.longest_lose,e.latency_p95_ms,e.ram_peak_bytes,c.code_hash,e.fdr_pass,"
                                            + "c.status,(SELECT fold_id FROM v57_research_experiment_r811 b"
                                            + " WHERE b.candidate_id=c.candidate_id AND b.split_role='BLOCK_STRESS' LIMIT 1),"
                                            + "(SELECT fold_id FROM v57_research_experiment_r811 r"
                                            + " WHERE r.candidate_id=c.candidate_id AND r.split_role='REGIME_STRESS' LIMIT 1)"
                                            + " FROM v57_research_candidate_r811 c LEFT JOIN"
                                            + " v57_research_experiment_r811 e ON e.candidate_id=c.candidate_id"
                                            + " AND e.split_role='FROZEN_TEST'"
                                            + " WHERE c.run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY COALESCE(e.wilson_lower,-1) DESC,"
                                            + "c.candidate_id ASC LIMIT 10",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append(cursor.getString(0))
                        .append('|')
                        .append(cursor.getString(1))
                        .append(':')
                        .append(cursor.getString(2))
                        .append("|EligibleN=")
                        .append(cursor.getInt(3))
                        .append("|SelectedN=")
                        .append(cursor.getInt(4))
                        .append("|HitN=")
                        .append(cursor.getInt(5))
                        .append("|HitRate=")
                        .append(cursor.getDouble(6))
                        .append("|Coverage=")
                        .append(cursor.getDouble(7))
                        .append("|Wilson=")
                        .append(cursor.getDouble(8))
                        .append("|Beta=")
                        .append(cursor.getDouble(9))
                        .append("|ECE=")
                        .append(cursor.getDouble(10))
                        .append("|Brier=")
                        .append(cursor.getDouble(11))
                        .append("|CoverageCV=")
                        .append(cursor.getDouble(12))
                        .append("|Blocks=")
                        .append(cursor.getString(19))
                        .append("|Regimes=")
                        .append(cursor.getString(20))
                        .append("|LongestLose=")
                        .append(cursor.getInt(13))
                        .append("|LatencyP95=")
                        .append(cursor.getDouble(14))
                        .append("|RAM=")
                        .append(cursor.getLong(15))
                        .append("|CodeHash=")
                        .append(cursor.getString(16))
                        .append("|FDR=")
                        .append(cursor.getInt(17))
                        .append("|Status=")
                        .append(cursor.getString(18))
                        .append('\n');
            }
            if (count == 0) out.append("NO_CANDIDATE_ROWS\n");
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public int candidateStatusCountSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String like) {
        if (store == null) return -1;
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT COUNT(1) FROM v57_research_candidate_r811"
                                            + " WHERE status LIKE ? AND run_id=(SELECT run_id FROM"
                                            + " v57_research_run_r811 ORDER BY updated_at DESC LIMIT 1)",
                                    new String[] {like});
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally {
            if (cursor != null) cursor.close();
        }
    }

public void appendFormulaCandidatesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean ready) {
        if (!ready) {
            out.append("NONE_PERSISTED_RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT candidate_id,formula_ast,parent_formula_ids,generation,"
                                            + "genetic_operator,complexity,feature_dependencies,dataset_hash,seed,status"
                                            + " FROM v57_research_candidate_r811 WHERE candidate_type='FORMULA'"
                                            + " AND run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY generation ASC,candidate_id ASC",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append("Formula#")
                        .append(count)
                        .append("|FormulaId=")
                        .append(cursor.getString(0))
                        .append("|CanonicalAST=")
                        .append(cursor.getString(1))
                        .append("|HumanReadableFormula=")
                        .append(cursor.getString(1))
                        .append("|ParentFormulaIds=")
                        .append(cursor.getString(2))
                        .append("|Generation=")
                        .append(cursor.getInt(3))
                        .append("|Mutation/Crossover=")
                        .append(cursor.getString(4))
                        .append("|Complexity=")
                        .append(cursor.getDouble(5))
                        .append("|FeatureDependencies=")
                        .append(cursor.getString(6))
                        .append("|PITAudit=PREDRAW_FEATURES_ONLY_GROUP_BY_TARGET_ISSUE")
                        .append("|DatasetHash=")
                        .append(cursor.getString(7))
                        .append("|Seed=")
                        .append(cursor.getLong(8))
                        .append("|Status=")
                        .append(cursor.getString(9))
                        .append('\n');
            }
            if (count == 0) out.append("NONE_YET_SCREENING_REQUIRES_STRICT_OOS_N_120\n");
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public void appendHypothesesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean ready) {
        if (!ready) {
            out.append("READ_ERROR=RESEARCH_SCHEMA_NOT_READY\n");
            return;
        }
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT hypothesis_id,failure_layer,mechanism,positive_evidence,"
                                            + "counter_evidence,affected_regime,candidate_family,acceptance_json,status"
                                            + " FROM v57_research_hypothesis_r811"
                                            + " WHERE run_id=(SELECT run_id FROM v57_research_run_r811"
                                            + " ORDER BY updated_at DESC LIMIT 1)"
                                            + " ORDER BY updated_at DESC",
                                    null);
            int count = 0;
            while (cursor.moveToNext()) {
                count++;
                out.append("Hypothesis#")
                        .append(count)
                        .append("|Id=")
                        .append(cursor.getString(0))
                        .append("|FailureLayer=")
                        .append(cursor.getString(1))
                        .append("|Mechanism=")
                        .append(cursor.getString(2))
                        .append("|PositiveEvidence=")
                        .append(cursor.getString(3))
                        .append("|CounterEvidence=")
                        .append(cursor.getString(4))
                        .append("|AffectedRegime=")
                        .append(cursor.getString(5))
                        .append("|CandidateFamily=")
                        .append(cursor.getString(6))
                        .append("|RequiredTestAndAcceptance=")
                        .append(cursor.getString(7))
                        .append("|Status=")
                        .append(cursor.getString(8))
                        .append('\n');
            }
            if (count == 0) out.append("NONE_YET_STRICT_OOS_GATE_NOT_MET\n");
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public void appendFailureDistributionSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        ResearchFailureHypothesisEngineR811.Diagnosis diagnosis =
                ResearchFailureHypothesisEngineR811.diagnoseSnapshotCP4(report, store);
        out.append("Status=")
                .append(diagnosis.status)
                .append("|Primary=")
                .append(diagnosis.primaryLayer)
                .append("|Checksum=")
                .append(diagnosis.checksum)
                .append('\n');
        for (int i = 0; i < diagnosis.layers.size(); i++) {
            ResearchFailureHypothesisEngineR811.LayerEvidence row = diagnosis.layers.get(i);
            out.append(row.layer)
                    .append("|count=")
                    .append(row.failureCount)
                    .append("|share=")
                    .append(row.failureShare)
                    .append("|confidenceLower95=")
                    .append(row.confidenceLower95)
                    .append("|sample=")
                    .append(row.sampleN)
                    .append("|counterEvidenceCount=")
                    .append(row.counterEvidenceCount)
                    .append('\n');
        }
        out.append("MechanisticHypothesis=")
                .append(diagnosis.hypothesis)
                .append("\nCounterEvidence=")
                .append(diagnosis.counterEvidence)
                .append('\n');
    }

public static String finiteTextSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, double value) {
        return Double.isNaN(value) || Double.isInfinite(value)
                ? "N/A" : String.format(Locale.US, "%.8f", value);
    }

public int researchCountSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String sql) {
        if (store == null || sql == null) return -1;
        Cursor cursor = null;
        try {
            cursor = report.select(sql, null);
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        } catch (Throwable ignored) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",ignored);} finally {
            if (cursor != null) cursor.close();
        }
    }

public String latestResearchPredrawAdaptiveSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "STORE_NULL";
        Cursor cursor = null;
        try {
            cursor =
                    report.select(
                                    "SELECT adaptive_artifact_hash,adaptive_trained_until,"
                                            + "adaptive_updates,adaptive_weight,formula_probability,adaptive_probability"
                                            + " FROM "
                                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                            + " ORDER BY frozen_at DESC,candidate_id ASC LIMIT 1",
                                    null);
            if (!cursor.moveToFirst()) return "NONE_YET";
            String artifact = cursor.getString(0) == null ? "" : cursor.getString(0);
            return "artifact="
                    + (artifact.length() == 0 ? "NONE" : artifact)
                    + "/trainedUntil="
                    + cursor.getString(1)
                    + "/updates="
                    + cursor.getLong(2)
                    + "/weight="
                    + cursor.getDouble(3)
                    + "/formulaP="
                    + cursor.getDouble(4)
                    + "/adaptiveP="
                    + cursor.getDouble(5);
        } catch (Throwable error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally {
            if (cursor != null) cursor.close();
        }
    }

public static double wilsonLowerSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int hit, int n) {
        if (n <= 0) return Double.NaN;
        double z = 1.959963984540054d;
        double p = hit / (double) n;
        double denominator = 1.0d + z * z / n;
        double center = p + z * z / (2.0d * n);
        double margin = z * Math.sqrt((p * (1.0d - p) + z * z / (4.0d * n)) / n);
        return Math.max(0.0d, Math.min(1.0d, (center - margin) / denominator));
    }

public ChapterSource r811TruthChapterSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        RuntimeHealthLedgerR36.BusinessTruth truth =
                RuntimeHealthLedgerR36.businessTruthSnapshotCP4(report, store);
        DiagnosticSnapshotR811 snapshot = DiagnosticSnapshotR811.captureSnapshotCP4(report, store);
        SettlementTaskStoreR36.Terminal terminal =
                SettlementTaskStoreR36.terminalSnapshotCP4(report, store, snapshot.latestDraw);
        String detail =
                "Build="
                        + V51BuildManifest.VERSION_NAME
                        + "\nDB_VERSION="
                        + AppConfig.DB_VERSION
                        + "\nR811_SCHEMA_REVISION="
                        + snapshot.schemaRevision
                        + "\nBusinessTruth="
                        + truth.canonical()
                        + "\nCORE="
                        + snapshot.coreStatus
                        + "\nFULL="
                        + snapshot.fullStatus
                        + "\nSHADOW predicted/settled/pending/hit/miss="
                        + snapshot.shadowPredicted
                        + '/'
                        + snapshot.shadowSettled
                        + '/'
                        + Math.max(0, snapshot.shadowPredicted - snapshot.shadowSettled)
                        + '/'
                        + snapshot.shadowHit
                        + '/'
                        + snapshot.shadowMiss
                        + "\nProvider primary issue/age/code="
                        + store.getState("r3611_provider_primary_issue", "")
                        + '/'
                        + store.getState("r3611_provider_primary_age_ms", "-1")
                        + '/'
                        + store.getState("r3611_provider_primary_code", "NO_SAMPLE")
                        + "\nProvider backup issue/age/code="
                        + store.getState("r3611_provider_backup_issue", "")
                        + '/'
                        + store.getState("r3611_provider_backup_age_ms", "-1")
                        + '/'
                        + store.getState("r3611_provider_backup_code", "NO_SAMPLE")
                        + "\nProvider selected issue/line/age="
                        + store.getState("r3611_provider_selected_issue", "")
                        + '/'
                        + store.getState("r3611_provider_selected_line", "")
                        + '/'
                        + store.getState("r3611_provider_selected_age_ms", "-1")
                        + "\nShadow backlog total/ready/waitDraw/lease/pitInvalid/corrupt="
                        + store.getState("r3611_shadow_backlog_total", "0")
                        + '/'
                        + store.getState("r3611_shadow_ready_to_settle", "0")
                        + '/'
                        + store.getState("r3611_shadow_wait_draw", "0")
                        + '/'
                        + store.getState("r3611_shadow_lease_active", "0")
                        + '/'
                        + store.getState("r3611_shadow_pit_invalid", "0")
                        + '/'
                        + store.getState("r3611_shadow_data_corrupt", "0")
                        + "\nShadow oldest pending="
                        + store.getState("r3611_shadow_oldest_pending_issue", "")
                        + "\nLatest settlement task="
                        + (terminal == null
                                ? "PENDING_OR_MISSING"
                                : terminal.taskClass
                                        + '/'
                                        + terminal.status
                                        + '/'
                                        + terminal.terminalCode
                                        + "/attempts="
                                        + terminal.attempts
                                        + "/error="
                                        + terminal.error)
                        + "\nResearch state="
                        + store.getResearchStateR811("r811_research_state", "IDLE")
                        + "\nResearch candidate="
                        + store.getResearchStateR811("r811_research_candidate", "NONE")
                        + "\nR908 settlement recovery="
                        + store.getState("r906_supervisor_last_result", "NOT_STARTED")
                        + "\nR908 Research settlement="
                        + store.getState("r908_research_settlement_status", "NOT_STARTED")
                        + "\nR908 autonomous tick="
                        + store.getState("r908_research_tick_status", "NOT_STARTED")
                        + "\nR908 historical invention="
                        + store.getState("r908_historical_invention_tick", "NOT_STARTED")
                        + "\nR908 invention OOS/walk-forward="
                        + store.getState("r908_invention_last_reliability", "NOT_AVAILABLE")
                        + "\nResearch boundary=CERTIFICATE_BOUNDED_SCORE_ONLY|DIRECT_REAL_AUTH=0|AUTO_UNLOCK=0\n"
                        + r811ResearchTruthSnapshotCP4(report, store, snapshot);
        LiveResearchDiagnosisOrchestrator.Status status;
        if ("READ_ERROR".equals(truth.rootCause))
            status = LiveResearchDiagnosisOrchestrator.Status.READ_ERROR;
        else if ("FAIL".equals(truth.status)) status = LiveResearchDiagnosisOrchestrator.Status.FAIL;
        else if ("DEGRADED".equals(truth.status))
            status = LiveResearchDiagnosisOrchestrator.Status.RETEST_REQUIRED;
        else status = LiveResearchDiagnosisOrchestrator.Status.PASS;
        return new ChapterSource(
                "C19-R811-RUNTIME-TRUTH",
                "R811结算、证据、SHADOW与Research统一真值",
                "RuntimeHealthLedgerR36/DiagnosticSnapshotR811",
                status,
                "READ_ERROR".equals(truth.rootCause)
                        ? LiveResearchDiagnosisOrchestrator.EvidenceGrade.READ_ERROR
                        : LiveResearchDiagnosisOrchestrator.EvidenceGrade.DATABASE_OBSERVATION,
                snapshot.shadowSettled,
                "recent30/60/120",
                "rootCause=" + truth.rootCause + "|status=" + truth.status,
                "服务进程存活不能反证issue lag；Deferred PENDING不能污染CORE",
                "按READ_ERROR→RUNTIME→SETTLEMENT/OOS→EVIDENCE→MODEL顺序处理",
                detail,
                com.ethk3.novaonepro.ReportTextCP12.sha256(report, detail));
    }

public ChapterSource validationChapterSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, 
            LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        com.ethk3.novaonepro.ReportTextCP12 detail = new com.ethk3.novaonepro.ReportTextCP12(report);
        detail.append("bundleValid=")
                .append(bundle.validation.valid)
                .append("\nfixedOrder=")
                .append(bundle.validation.fixedOrder)
                .append("\nuniqueFindingIds=")
                .append(bundle.validation.uniqueFindingIds)
                .append("\nevidenceResolved=")
                .append(bundle.validation.evidenceResolved)
                .append("\nedgesResolved=")
                .append(bundle.validation.edgesResolved)
                .append("\nacyclic=")
                .append(bundle.validation.acyclic)
                .append("\nhashStable=")
                .append(bundle.validation.hashStable)
                .append("\nreadOnlyContract=")
                .append(bundle.validation.readOnlyContract)
                .append("\nviolations=");
        for (int i = 0; i < bundle.validation.violations.size(); i++) {
            if (i > 0) detail.append(',');
            detail.append(bundle.validation.violations.get(i));
        }
        detail.append("\nValidationHash=").append(bundle.validation.checksum).append('\n');
        return new ChapterSource(
                "C18-BUNDLE-VALIDATION",
                "诊断编排确定性与完整性校验",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.validation.valid
                        ? LiveResearchDiagnosisOrchestrator.Status.PASS
                        : LiveResearchDiagnosisOrchestrator.Status.FAIL,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.STATIC_BUILD,
                7,
                "N/A",
                bundle.validation.valid ? "诊断Bundle校验通过" : "诊断Bundle校验失败",
                "Bundle校验通过仍不等于物理设备运行通过",
                "运行主机回放、APK ABI门和物理设备矩阵",
                detail.toString(),
                bundle.validation.checksum);
    }

public ChapterSource edgeChapterSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        com.ethk3.novaonepro.ReportTextCP12 detail = new com.ethk3.novaonepro.ReportTextCP12(report, 16384);
        for (int i = 0; i < bundle.edges.size(); i++) {
            LiveResearchDiagnosisOrchestrator.DependencyEdge edge = bundle.edges.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(edge.fromFinding)
                    .append(" -> ")
                    .append(edge.toFinding)
                    .append('|')
                    .append(edge.kind.name())
                    .append('|')
                    .append(edge.rationale)
                    .append("|hash=")
                    .append(edge.checksum)
                    .append('\n');
        }
        return new ChapterSource(
                "C16-DEPENDENCY-DAG",
                "根因依赖DAG",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.validation.acyclic
                        ? LiveResearchDiagnosisOrchestrator.Status.PASS
                        : LiveResearchDiagnosisOrchestrator.Status.FAIL,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.STATIC_BUILD,
                bundle.edges.size(),
                "N/A",
                bundle.validation.acyclic ? "依赖图无环且引用完整" : "依赖图校验失败",
                "DAG表达诊断顺序，不代表因果关系已经由实验识别",
                "先修上游阻断点，再执行下游模型与Challenger验证",
                detail.toString(),
                hashEdgesSnapshotCP4(report, bundle.edges));
    }

public static String hashEdgesSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, List<LiveResearchDiagnosisOrchestrator.DependencyEdge> edges) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report);
        for (int i = 0; i < edges.size(); i++) out.append('|').append(edges.get(i).checksum);
        return com.ethk3.novaonepro.ReportTextCP12.sha256(report, out.toString());
    }

public ChapterSource findingChapterSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, LiveResearchDiagnosisOrchestrator.DiagnosisBundle bundle) {
        com.ethk3.novaonepro.ReportTextCP12 detail = new com.ethk3.novaonepro.ReportTextCP12(report, 32768);
        for (int i = 0; i < bundle.findings.size(); i++) {
            LiveResearchDiagnosisOrchestrator.Finding finding = bundle.findings.get(i);
            detail.append(i + 1)
                    .append('|')
                    .append(finding.id)
                    .append('|')
                    .append(finding.severity.name())
                    .append('|')
                    .append(finding.status.name())
                    .append("|confidence=")
                    .append(finding.confidence)
                    .append("|owner=")
                    .append(finding.owner)
                    .append("|file=")
                    .append(finding.codeLocation)
                    .append('\n')
                    .append("结论=")
                    .append(finding.conclusion)
                    .append('\n')
                    .append("原因=")
                    .append(finding.cause)
                    .append('\n')
                    .append("影响=")
                    .append(finding.impact)
                    .append('\n')
                    .append("施工=")
                    .append(finding.action)
                    .append('\n')
                    .append("验证=")
                    .append(finding.validation)
                    .append('\n')
                    .append("期望=")
                    .append(finding.expected)
                    .append('\n')
                    .append("回滚=")
                    .append(finding.rollback)
                    .append("\nFindingHash=")
                    .append(finding.checksum)
                    .append("\n\n");
        }
        return new ChapterSource(
                "C15-FINDING-DAG",
                "Finding完整施工账本",
                LiveResearchDiagnosisOrchestrator.OWNER,
                bundle.overallStatus,
                LiveResearchDiagnosisOrchestrator.EvidenceGrade.DATABASE_OBSERVATION,
                bundle.findings.size(),
                "N/A",
                "每个结论均绑定证据、反证、Owner、代码位置、施工、验证和回滚",
                "诊断结论不是模型效果证明，也不得直接解锁REAL",
                "按P0→P1→P2顺序关闭；READ_ERROR优先于模型调参",
                detail.toString(),
                hashFindingsSnapshotCP4(report, bundle.findings));
    }

public static String hashFindingsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, List<LiveResearchDiagnosisOrchestrator.Finding> findings) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report);
        for (int i = 0; i < findings.size(); i++) out.append('|').append(findings.get(i).checksum);
        return com.ethk3.novaonepro.ReportTextCP12.sha256(report, out.toString());
    }

public ChapterSource redlineChapterSnapshotCP12(com.ethk3.novaonepro.ReportReadContextCP4 report){return redlineChapter(null);}
}
