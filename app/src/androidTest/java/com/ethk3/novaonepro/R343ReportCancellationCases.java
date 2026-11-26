package com.ethk3.novaonepro;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Locale;

/** Tests the original restart drain ordering after a synthetic interruption at the durable job boundary. */
public final class R343ReportCancellationCases {
    private R343ReportCancellationCases() {}

    public static JSONObject resumeCancelledReport(File files) throws Exception {
        if (!files.isDirectory() && !files.mkdirs()) throw new IOException("REPORT_CANCELLATION_FIXTURE_ROOT");
        File source = new File(files, "source-original.db");
        String sourceContent = "original database bytes";
        write(source, sourceContent);
        JSONArray outcomes = new JSONArray();
        ReportJobJournalCP12.Stage[] phases = {
                ReportJobJournalCP12.Stage.BACKUP,
                ReportJobJournalCP12.Stage.COMPRESSING,
                ReportJobJournalCP12.Stage.VERIFYING};
        int assertions = 0;
        for (int index = 0; index < phases.length; index++) {
            File working = new File(files, "phase-" + index);
            if (!working.mkdir()) throw new IOException("REPORT_CANCELLATION_FIXTURE_PHASE");
            ReportJobJournalCP12.Job cancelled = job(100 + index, 200 + index);
            ReportJobJournalCP12.Job other = job(300 + index, 400 + index);
            File cancelledDirectory;
            File protectedPrior;
            File marker;
            long requested;
            JSONObject originalMarker;
            try (ReportJobJournalCP12 owner = ReportJobJournalCP12.tryClaim(working)) {
                require(owner != null, "Initial export owner claimed", index);
                owner.create(other);
                advance(owner, other, ReportJobJournalCP12.Stage.READY_TO_SHARE);
                protectedPrior = new File(owner.directory(other), "published.json");
                write(protectedPrior, "prior published report");

                owner.create(cancelled);
                advance(owner, cancelled, phases[index]);
                cancelledDirectory = owner.directory(cancelled);
                write(new File(cancelledDirectory, "phase.partial"), "derived report bytes");
                String command = ReportTaskControlR69.request(working, false);
                require(!command.isEmpty(), "Initial durable control command", index);
                marker = new File(owner.root(), "control-r69/cancel-" + cancelled.requestId + ".json");
                originalMarker = new JSONObject(ReportJobJournalCP12.readAtomic(marker, 4096));
                requested = originalMarker.getLong("requestedNanosR30");
                require(requested > 0 && requested <= System.nanoTime(), "Original monotonic timestamp persisted", index);
                cancelled.lastReason = "REPORT_USER_CANCEL";
                owner.transition(cancelled, ReportJobJournalCP12.Stage.CANCELLED, System.currentTimeMillis());
                // Do not finish cancellation here. This is the persistence boundary
                // reached if the process dies after journal+ACK fsync.
            }
            File receipt = new File(cancelledDirectory, "cancel-receipt-r30.json");
            require(!receipt.exists() && marker.isFile(), "Restart image keeps the original timestamp", index);
            try (ReportJobJournalCP12 restarted = ReportJobJournalCP12.tryClaim(working)) {
                require(restarted != null, "Restart reacquires sole export owner", index);
                restarted.reconcileAcknowledgementsR31();
                ReportTaskControlR69.applyPending(restarted);
                ReportJobJournalCP12.Job current = restarted.current();
                require(current != null && cancelled.jobId.equals(current.jobId), "Same exact cancelled job read back", index);
                ReportTaskControlR69.finishCancellationR30(restarted.root(), restarted.directory(current), current);
                require(!marker.exists(), "Completed command releases its marker", index);

                JSONObject row = new JSONObject(ReportJobJournalCP12.readAtomic(receipt, 4096));
                require(current.jobId.equals(row.getString("jobId")), "Receipt belongs to the original job", index);
                require(row.getBoolean("resourcesReleased"), "Private resources released", index);
                require(row.getBoolean("privateTemporaryFilesCleaned"), "Private temporary cleanup recorded", index);
                require(!new File(cancelledDirectory, "phase.partial").exists(), "Task's private temporary file removed", index);
                require("prior published report".equals(read(protectedPrior)), "Published neighbor report immutable", index);
                require(sourceContent.equals(read(source)), "Source file immutable", index);
                assertions += 13;

                if ("UNKNOWN".equals(originalMarker.getString("bootR30"))) {
                    require(row.isNull("cancelRequestToReleaseMs") && row.isNull("targetMet"),
                            "Unavailable boot identity remains UNKNOWN", index);
                    assertions++;
                } else {
                    require(!row.isNull("cancelRequestToReleaseMs"),
                            "Same-boot original cancellation timestamp was lost across restart", index);
                    long elapsed = row.getLong("cancelRequestToReleaseMs");
                    long observedMax = (System.nanoTime() - requested) / 1_000_000;
                    require(elapsed >= 0 && elapsed <= observedMax, "Receipt interval follows same-boot clock", index);
                    require(row.getBoolean("targetMet") == (elapsed <= 2000), "Threshold is derived from observed interval", index);
                    assertions += 3;
                }
                File ack = new File(restarted.root(), "requests/" + current.requestId + ".ack.json");
                JSONObject acknowledgement = new JSONObject(ReportJobJournalCP12.readAtomic(ack, 8192));
                require(current.jobId.equals(acknowledgement.optString("jobId")), "Recovered ACK retains job identity", index);
                require("CANCELLED".equals(acknowledgement.getString("stage")), "Recovered ACK retains terminal stage", index);
                File before = new File(cancelledDirectory, "cancel-receipt-r30.json");
                String first = read(before);
                ReportTaskControlR69.finishCancellationR30(restarted.root(), cancelledDirectory, current);
                require(first.equals(read(before)), "Repeated recovery preserves receipt bytes", index);
                assertions += 3;
                ReportJobJournalCP12.Job retry = job(500 + index, 600 + index);
                restarted.create(retry);
                require(retry.jobId.equals(restarted.current().jobId), "Recovered owner can accept a new export", index);
                assertions++;
                outcomes.put(row.put("initialPhase", phases[index].name()).put("nextJobClaim", retry.jobId)
                        .put("ackRevision", acknowledgement.getLong("revision")));
            }
        }
        return new JSONObject().put("scope", "SYNTHETIC_DURABLE_JOB_REOPEN")
                .put("assertions", assertions).put("phases", outcomes);
    }

    private static ReportJobJournalCP12.Job job(int id, int request) {
        return new ReportJobJournalCP12.Job(hash(id), hash(request), hash(901), "FULL_REPORT", System.currentTimeMillis());
    }

    private static String hash(int value) {
        return String.format(Locale.ROOT, "%064x", value);
    }

    private static void advance(ReportJobJournalCP12 owner, ReportJobJournalCP12.Job job,
                                ReportJobJournalCP12.Stage target) throws Exception {
        for (ReportJobJournalCP12.Stage stage : new ReportJobJournalCP12.Stage[]{
                ReportJobJournalCP12.Stage.BACKUP, ReportJobJournalCP12.Stage.SNAPSHOT_DURABLE,
                ReportJobJournalCP12.Stage.INDEXING, ReportJobJournalCP12.Stage.INDEX_DURABLE,
                ReportJobJournalCP12.Stage.COMPRESSING, ReportJobJournalCP12.Stage.VERIFYING,
                ReportJobJournalCP12.Stage.READY_TO_SHARE}) {
            owner.transition(job, stage, System.currentTimeMillis());
            if (stage == target) return;
        }
        throw new IllegalArgumentException(target.name());
    }

    private static void require(boolean passed, String assertion, int stage) {
        if (!passed) throw new AssertionError("stageIndex=" + stage + " " + assertion);
    }

    private static void write(File path, String content) throws IOException {
        Files.write(path.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File path) throws IOException {
        return new String(Files.readAllBytes(path.toPath()), StandardCharsets.UTF_8);
    }
}
