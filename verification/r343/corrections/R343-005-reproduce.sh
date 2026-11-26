#!/usr/bin/env bash
set -euo pipefail
plan="${1:?fixed R343 plan directory}"
app="${2:?173 application source checkout}"
repair="${3:-}"
scratch="$(mktemp -d /tmp/r343-circuit-reproducer-XXXXXX)"
trap 'rm -rf "$scratch"' EXIT
cd "$scratch"
mkdir -p source/android/{content,system} source/com/ethk3/novaonepro/strategyv2 bin
runtime="$plan/patches/runtime/after/app/src/main/java/com/ethk3/novaonepro"
appsrc="$app/app/src/main/java/com/ethk3/novaonepro"
sha256sum "$runtime/StartupStageJournalR341.java" "$runtime/strategyv2/StartupProbePolicyR343.java"
journal="${repair:-$runtime/StartupStageJournalR341.java}"
sha256sum "$journal"
cp "$journal" "$appsrc/V51ChecksumEngine.java" source/com/ethk3/novaonepro/
cp "$runtime/strategyv2/StartupProbePolicyR343.java" "$appsrc"/strategyv2/{FailureDispositionR31,JsonR27,RuntimeFaultEvidenceR31,StatsR27}.java source/com/ethk3/novaonepro/strategyv2/
cat > source/android/content/Context.java <<'JAVA'
package android.content;
import java.io.File;
public abstract class Context {
    public abstract File getNoBackupFilesDir();
}
JAVA
cat > source/android/system/ErrnoException.java <<'JAVA'
package android.system;
public final class ErrnoException extends Exception {}
JAVA
cat > source/android/system/OsConstants.java <<'JAVA'
package android.system;
public final class OsConstants { public static final int O_RDONLY = 0; }
JAVA
cat > source/android/system/Os.java <<'JAVA'
package android.system;
import java.io.FileDescriptor;
// Host adapter only. Real journal file lock, file sync, rename and CRC remain in production class.
public final class Os {
    public static FileDescriptor open(String path, int flags, int mode) throws ErrnoException {return new FileDescriptor();}
    public static void fsync(FileDescriptor fd) throws ErrnoException {}
    public static void close(FileDescriptor fd) throws ErrnoException {}
}
JAVA
cat > source/com/ethk3/novaonepro/RuntimeObservationScopeR341.java <<'JAVA'
package com.ethk3.novaonepro;
import android.content.Context;
import java.util.Map;
public final class RuntimeObservationScopeR341 {
    interface Clock { long wall(); long monotonic(); }
    static final Clock SYSTEM = null;
    final String sourceCommit = "test-source";
    final String bootId = "known-boot";
    static RuntimeObservationScopeR341 capture(Context ctx,Object db,Object version,Clock clock) {
        throw new UnsupportedOperationException();
    }
    Map<String,Object> exportEnvelope() {return Map.of("sourceCommit",sourceCommit,"bootId",bootId);}
}
JAVA
cat > source/com/ethk3/novaonepro/ReportJobJournalCP12.java <<'JAVA'
package com.ethk3.novaonepro;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
public final class ReportJobJournalCP12 {
    static void writeAtomic(File file,String text) throws IOException {Files.writeString(file.toPath(),text);}
    static String readAtomic(File file,int max) throws IOException {
        if (file.length()>max) throw new IOException("BOUND");
        return Files.readString(file.toPath());
    }
}
JAVA
cat > source/com/ethk3/novaonepro/ProbeRotationProof.java <<'JAVA'
package com.ethk3.novaonepro;
import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.StartupProbePolicyR343;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProbeRotationProof {
    private static final String A = "a".repeat(64), B = "b".repeat(64);
    private static final class Clock implements RuntimeObservationScopeR341.Clock {
        long mono = 10L;
        public long monotonic() { return mono; }
        public long wall() { return 1_795_656_000_000L + mono; }
        void waitCooldown() { mono += StartupProbePolicyR343.RETRY_MS; }
    }
    private static void require(boolean ok,String message) {
        if (!ok) throw new AssertionError(message);
    }
    private static void failed(StartupStageJournalR341 journal,String identity) {
        StartupStageJournalR341.Token token = journal.begin("owner",UUID.randomUUID().toString(),
                StartupStageJournalR341.Stage.OWNER_BEGIN,identity);
        require(token.beginReceipt.durable, "failure began durably");
        require(journal.recordUncaught(token,new IllegalStateException("REPRODUCIBLE_BUG")).durable,
                "failure retained durably");
    }
    private static void repeat(StartupStageJournalR341 journal,String identity) {
        for (int i=0;i<3;i++) failed(journal,identity);
        require(journal.hasDeterministicFailureLimit(identity),"failure circuit engaged "+identity.charAt(0));
    }
    private static long reservations(StartupStageJournalR341 journal,String identity) {
        Object snapshot = journal.snapshot().get("frames");
        return ((List<?>)snapshot).stream().map(row -> (Map<?,?>)row)
                .filter(row -> identity.equals(row.get("inputHash"))
                        && "CIRCUIT_PROBE".equals(row.get("stage")) && "END".equals(row.get("record"))).count();
    }
    private static void verifyCapacity(File root) throws Exception {
        Clock clock = new Clock();
        clock.waitCooldown();
        File frames = new File(root,"research.frames");
        byte[] before = Files.readAllBytes(frames.toPath());
        StartupStageJournalR341 journal = StartupStageJournalR341.open(root,"research",clock,
                new RuntimeObservationScopeR341());
        require(journal.hasDeterministicFailureLimit(A),"packed journal has the established failure run");
        require(!journal.reserveDeterministicProbeR343(A),"protected journal capacity must fail closed");
        require(Arrays.equals(before,Files.readAllBytes(frames.toPath())),"existing protected frames retained");
        require("PROBE_JOURNAL_PROTECTED_CAPACITY".equals(journal.deterministicProbeStatusR343()),
                "failed capacity admission exposed the exact reason");
        Map<String,Object> snapshot=journal.snapshot();
        require("R341_JOURNAL_PROTECTED_CAPACITY".equals(snapshot.get("writeError")),
                "journal retains specific write failure");
        require(String.valueOf(snapshot.get("emergency")).contains("R341_JOURNAL_PROTECTED_CAPACITY"),
                "emergency reserve contains the specific failure");
        System.out.println(JsonR27.write(Map.of("protectedBytes",before.length,"preserved",true,
                "probeReserved",false,"status",journal.deterministicProbeStatusR343(),
                "writeError",snapshot.get("writeError"))));
    }
    public static void main(String[] args) throws Exception {
        Clock clock = new Clock();
        RuntimeObservationScopeR341 identity = new RuntimeObservationScopeR341();
        File root = new File(args[0]);
        StartupStageJournalR341 journal = StartupStageJournalR341.open(root,"research",clock,identity);
        repeat(journal,A);
        for (int i=0;i<3;i++) {
            clock.waitCooldown();
            require(journal.reserveDeterministicProbeR343(A),"initial probe "+i);
        }
        clock.waitCooldown();
        require(!journal.reserveDeterministicProbeR343(A),"limit before competing dependency");
        long before = reservations(journal,A);
        repeat(journal,B);
        clock.waitCooldown();
        require(journal.reserveDeterministicProbeR343(B),"B independent probe");
        for (int i=0;i<470;i++) {
            StartupStageJournalR341.Token token = journal.begin("unrelated","rotation-"+i,
                    StartupStageJournalR341.Stage.SERVICE_INITIALIZE,null);
            require(token.beginReceipt.durable, "rotation begin is durable");
            require(journal.end(token,StartupStageJournalR341.Outcome.VERIFIED,"rotation",0).durable,
                    "rotation end is durable");
        }
        StartupStageJournalR341 reopened = StartupStageJournalR341.open(root,"research",clock,identity);
        long after = reservations(reopened,A), bAfter = reservations(reopened,B);
        repeat(reopened,A);
        clock.waitCooldown();
        boolean fourth = reopened.reserveDeterministicProbeR343(A);
        System.out.println(JsonR27.write(Map.of("aReservationsBeforeRotation",before,
                "aReservationsAfterRotation",after,"bReservationsAfterRotation",bAfter,
                "aFourthReservationSucceeded",fourth,"lastStatus",reopened.deterministicProbeStatusR343(),
                "scope","HOST_REAL_SEALED_JOURNAL_CONTROL_FLOW_WITH_ANDROID_FILE_ADAPTER")));
        if ("fixed".equals(args[1])) {
            require(before==3 && after==1 && bAfter==1 && !fourth,
                    "Keep most recent A and B receipts and deny the fourth grant");
            require("PROBE_LIMIT_REACHED_DEPENDENCY_CHANGE_REQUIRED".equals(reopened.deterministicProbeStatusR343()),
                    "Expose the retained probe budget");
            verifyCapacity(new File(args[2]));
        } else {
            require(before==3 && after==0 && bAfter==1 && fourth,"Bug requires 3→0 A receipts and a fourth grant");
        }
    }
}
JAVA
javac -d bin $(find source -name '*.java')
expect=sealed
if [[ -n "$repair" ]]; then
    expect=fixed
    python3 - "$scratch/full-journal" <<'PY'
import json
import pathlib
import struct
import sys
import zlib

root = pathlib.Path(sys.argv[1])
root.mkdir()
limit = 256 * 1024 - 4096
target_size = limit - 64

def frame(stage, record, dependency, token):
    row = dict(schema="R341_STARTUP_FRAME_1", stage=stage, record=record, inputHash=dependency,
               sourceCommit="test-source", bootId="known-boot", wallAt=1795656000001, monoAt=1,
               attempt=token, token=token, owner="capacity-fixture")
    if stage == "CIRCUIT_PROBE":
        row["probeAttempt"] = 1
    if record == "END":
        row["outcome"] = "VERIFIED" if stage == "CIRCUIT_PROBE" else "BUG"
        if stage == "OWNER_BEGIN":
            row["causeChainHash"] = "same-deterministic-bug"
    return row

def encode(frame, sequence):
    data = json.dumps(dict(frame, sequence=sequence), sort_keys=True, separators=(",", ":")).encode()
    assert 0 < len(data) <= 2048
    return struct.pack(">II", len(data), zlib.crc32(data)) + data

tail = [frame("OWNER_BEGIN", role, "a" * 64, f"failed-{n}")
        for n in range(3) for role in ("BEGIN", "END")]
prefix = []
occupied = 0
for n in range(1, 1000):
    dependency = f"{n:064x}"
    pair = [frame("CIRCUIT_PROBE", role, dependency, f"probe-{n}") for role in ("BEGIN", "END")]
    width = sum(len(encode(row, len(prefix) + k)) for k, row in enumerate(pair, start=1))
    tail_width = sum(len(encode(row, len(prefix) + len(pair) + k))
                     for k, row in enumerate(tail, start=1))
    if occupied + width + tail_width + 32 > target_size:
        break
    prefix.extend(pair)
    occupied += width

rows = prefix + tail
rows[-1]["fixturePad"] = ""
unpadded = sum(len(encode(row, sequence)) for sequence, row in enumerate(rows, start=1))
rows[-1]["fixturePad"] = "x" * (target_size - unpadded)
value = b"".join(encode(row, sequence) for sequence, row in enumerate(rows, start=1))
assert len(value) == target_size
(root / "research.frames").write_bytes(value)
PY
fi
java -cp bin com.ethk3.novaonepro.ProbeRotationProof "$scratch/live-journal" "$expect" "$scratch/full-journal"
