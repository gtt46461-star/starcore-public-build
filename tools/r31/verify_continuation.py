#!/usr/bin/env python3
"""Run production continuation lifecycle code and separately labelled synthetic host orchestration.

Requires the complete Android project's compiled classes for the numerical dependency closure.
Does not run Gradle, simulate a database, or produce device/natural quality evidence.
"""
import argparse
from datetime import datetime, timezone
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "app/src/main/java/com/ethk3/novaonepro/strategyv2"
TEST_SOURCE = ROOT / "app/src/test/java/com/ethk3/novaonepro/strategyv2/R31ContinuationLifecycleTest.java"
REPLAY_TEST_SOURCE = TEST_SOURCE.with_name("R31BoundedReplayTest.java")
HOST = ROOT / "tools/r31/continuation_host"
BASELINE = "5237b265a27fe5ad27fd842901c445d5f8c27749"
OWNED = ["CooperativeContinuationR26", "TaskContinuationR26", "ExecutionBudgetR26", "IncumbentLoopSU2", "XiIncumbentSU2", "LegacyGenerationR26", "LegacyReplayContinuationR26", "EvolutionLoopR27"]
PURE = ["CooperativeContinuationR26", "ExecutionBudgetR26", "BudgetTokenSU1"]
NUMERICAL = [*PURE, "XiIncumbentSU2", "LogisticRidgeSU2", "HashingSU1"]


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--classes", type=Path, default=ROOT / "app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes")
    parser.add_argument("--out", type=Path, default=ROOT / "verification/r31/continuation")
    parser.add_argument("--phase", choices=["P01","P02"], default="P01")
    args = parser.parse_args()
    out = args.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    classes = args.classes.resolve()
    dependency = classes / "com/ethk3/novaonepro/strategyv2/ModelReplaySU2.class"
    if not dependency.is_file():
        raise RuntimeError("Complete Android project must compile first; ModelReplaySU2.class is missing")
    gradle_home = Path(os.environ.get("GRADLE_USER_HOME", "/workspace/starcore-environment/gradle-home"))
    junit = next(gradle_home.glob("caches/modules-2/files-2.1/junit/junit/4.13.2/*/junit-4.13.2.jar"))
    hamcrest = next(gradle_home.glob("caches/modules-2/files-2.1/org.hamcrest/hamcrest-core/1.3/*/hamcrest-core-1.3.jar"))
    java, javac = shutil.which("java"), shutil.which("javac")
    if not java or not javac:
        raise RuntimeError("java/javac missing; activate the repository's configured JDK")
    commands = []
    compiled_hashes={name:digest(SOURCE/(name+".java")) for name in OWNED}
    source_commit=subprocess.check_output(["git","rev-parse","HEAD"],cwd=ROOT,text=True).strip()

    def run(command, log, timeout=90):
        result = subprocess.run(list(map(str, command)), cwd=ROOT, capture_output=True, text=True, timeout=timeout)
        (out / log).write_text(result.stdout + result.stderr)
        commands.append(dict(command=list(map(str, command)), log=log, exitCode=result.returncode))
        if result.returncode:
            (out / "FAILED_COMMAND.json").write_text(json.dumps(commands[-1], indent=2) + "\n")
            raise RuntimeError(f"command failed: {log}")
        return result.stdout

    with tempfile.TemporaryDirectory(prefix="r31-continuation-") as temporary:
        temp = Path(temporary)
        core, race, baseline, current, replay = [temp / name for name in ("core", "race", "baseline", "current", "replay")]
        for target in (core, race, baseline, current, replay):
            target.mkdir()
        test_cp = os.pathsep.join(map(str, (junit, hamcrest)))
        run([javac, "-cp", test_cp, "-d", core, *[SOURCE / (name + ".java") for name in PURE], TEST_SOURCE], "core-javac.log")
        junit_log = run([java, "-cp", str(core) + os.pathsep + test_cp, "org.junit.runner.JUnitCore", "com.ethk3.novaonepro.strategyv2.R31ContinuationLifecycleTest"], "core-junit.log")
        junit_tests = int(re.search(r"OK \((\d+) tests\)", junit_log).group(1))
        sdk = Path(os.environ.get("ANDROID_HOME", "/workspace/starcore-environment/android-sdk"))
        android_jar = sdk / "platforms/android-36/android.jar"
        replay_cp = os.pathsep.join(map(str, (classes, android_jar, junit, hamcrest)))
        run([javac, "-cp", replay_cp, "-d", replay, *[SOURCE / (name + ".java") for name in [*PURE, "EvolutionLoopR27"]], REPLAY_TEST_SOURCE], "bounded-replay-javac.log")
        replay_log = run([java, "-Xmx768m", "-cp", str(replay) + os.pathsep + replay_cp, "org.junit.runner.JUnitCore", "com.ethk3.novaonepro.strategyv2.R31BoundedReplayTest"], "bounded-replay-junit.log")
        replay_tests = int(re.search(r"OK \((\d+) tests\)", replay_log).group(1))
        run([javac, "-d", race, *[SOURCE / (name + ".java") for name in [*PURE, "TaskContinuationR26", "LegacyReplayContinuationR26"]], *sorted((HOST / "shim").rglob("*.java")), HOST / "ContinuationOwnerRaceR31HostTest.java"], "owner-race-javac.log")
        race_log = run([java, "-cp", race, "com.ethk3.novaonepro.strategyv2.ContinuationOwnerRaceR31HostTest"], "owner-race.log")
        assert "switches=10000|bodyEntries=10000|maxBodyConcurrency=1|heldPermits=0" in race_log

        baseline_sources = []
        for name in NUMERICAL:
            path = temp / "baseline-source/com/ethk3/novaonepro/strategyv2" / (name + ".java")
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_bytes(subprocess.check_output(["git", "show", BASELINE + ":" + str((SOURCE / (name + ".java")).relative_to(ROOT))], cwd=ROOT))
            baseline_sources.append(path)
        run([javac, "-cp", classes, "-d", baseline, *baseline_sources, HOST / "B01C04BaselineRepro.java", HOST / "IncumbentProfileR31HostTest.java"], "baseline-javac.log")
        baseline_cp = str(baseline) + os.pathsep + str(classes)
        baseline_repro = run([java, "-cp", baseline_cp, "com.ethk3.novaonepro.strategyv2.B01C04BaselineRepro"], "baseline-repro.log")
        assert "doneBeforeWorkerExit=true" in baseline_repro and "awaitOwnerStoppedInterruptPreserved=false" in baseline_repro
        before = json.loads(run([java, "-Xmx768m", "-cp", baseline_cp, "com.ethk3.novaonepro.strategyv2.IncumbentProfileR31HostTest"], "baseline-profile.log").strip())
        run([javac, "-cp", classes, "-d", current, *[SOURCE / (name + ".java") for name in NUMERICAL], HOST / "IncumbentProfileR31HostTest.java", ROOT / "tools/strategy_su2/tests/R26IncumbentBudgetTest.java", ROOT / "tools/strategy_su2/tests/R26RecoveryTest.java"], "current-numerical-javac.log")
        current_cp = str(current) + os.pathsep + str(classes)
        after = json.loads(run([java, "-Xmx768m", "-cp", current_cp, "com.ethk3.novaonepro.strategyv2.IncumbentProfileR31HostTest"], "current-profile.log").strip())
        assert before["continuousAndSlicedSha256"] == after["continuousAndSlicedSha256"]
        run([java, "-Xmx768m", "-cp", current_cp, "com.ethk3.novaonepro.strategyv2.R26IncumbentBudgetTest"], "original-incumbent-budget.log")
        run([java, "-Xmx768m", "-cp", current_cp, "com.ethk3.novaonepro.strategyv2.R26RecoveryTest"], "original-recovery.log", timeout=180)

    if compiled_hashes!={name:digest(SOURCE/(name+".java")) for name in OWNED}:raise RuntimeError("production continuation source changed during verification")
    receipt = dict(
        card="B01-C04" if args.phase=="P01" else "B04_CONTROL_LIFECYCLE_WITH_B01_C04_REGRESSION", phase=args.phase, state="CODE_COMPLETE", baseline=BASELINE,
        inheritedP00Commit=subprocess.check_output(["git", "rev-parse", "8faabc2c"], cwd=ROOT, text=True).strip(), sourceCommit=source_commit, dirtyAtRun=bool(subprocess.check_output(["git","status","--porcelain"],cwd=ROOT,text=True).strip()),compiledSourceSHA256=compiled_hashes,evaluatedAt=datetime.now(timezone.utc).isoformat(),
        actualChanges={
            "CooperativeContinuationR26": ["State/isTerminated/awaitTermination", "helpers without CURRENT honor interrupt at all existing poll sites", "bounded owner waits preserve interrupt", "slice rejects concurrent resume and never resumes cancellation", "permit ownership reaped after actual thread exit", "startup/finally cleanup", "bounded phase/checkpoint/thread diagnostics", "idempotent critical close"],
            "TaskContinuationR26": ["map lock excludes slice/join", "inFlight and context identity", "old owner/target drains before replacement", "commit chunk inheritance for same task/owner"],
            "ExecutionBudgetR26": ["bounded phase observer only"],
            "IncumbentLoopSU2": ["capture/encode/freeze phase markers", "checkpoint marker only after durable freeze returned success"],
            "XiIncumbentSU2": ["phase markers only; original polls and arithmetic preserved"],
            "LegacyGenerationR26": ["authorized scope extension: cancel retains active worker", "nonblocking single step reservation", "old owner/source/ABI replacement waits for termination", "same-owner chunk inheritance"],
            "EvolutionLoopR27": ["authorized required caller extension: REPLAY only", "at most two in-flight helpers", "20ms Future timeout with parent polling", "capacity refusal yields explicit resource wait after bounded 50ms admission retry", "apply complete results in original candidate order", "cancel every remaining Future on failure", "inherit H_SELECT evolution guard on helpers"],
            "LegacyReplayContinuationR26": ["authorized scope extension: lock excludes slice/join", "inFlight/context identity", "do not clear mutable replay state under live worker", "same-owner chunk inheritance"],
        },
        sourceSha256={str((SOURCE / (name + ".java")).relative_to(ROOT)): digest(SOURCE / (name + ".java")) for name in OWNED},
        testAndToolSha256={str(path.relative_to(ROOT)): digest(path) for path in [TEST_SOURCE, REPLAY_TEST_SOURCE, Path(__file__).resolve(), *sorted(HOST.rglob("*.java"))]},
        androidCompiledDependency=dict(path=str(dependency.relative_to(ROOT)), sha256=digest(dependency), use="production ModelReplaySU2.quantileLinear numerical dependency; not a standalone Android build"),
        tests=dict(coreJUnit=junit_tests, productionBoundedReplayJUnit=replay_tests, realReplayCandidates=128, maximumReplayHelpers=2, exactSerialParallelReplayParity=True, actualHelperFinallyObservedAfterCancel=True, controlledOverlapRaces=2, sequentialOwnerTargetSwitches=10000,
                   maxSameTaskBodyConcurrency=1, finalHeldPermits=0, originalIncumbentBudget="PASS",
                   originalRecovery="PASS", baselineFaultReproduction="REPRODUCED"),
        profiles=dict(before=before, after=after, conclusion="Exact synthetic arithmetic parity; individual host timing/allocation observations are not device performance or quality claims"),
        integration=dict(sharedProvider="ResearchPoolR27 single static controller installed before Android workers", sharedClaim="inheritCommitChunksR31(previous)", androidBuild="PENDING_ROOT_JOINT_BUILD", databaseMigration="NO_DDL_IN_OWNED_CHANGES", apk=None),
        requiredRemaining=["root Android compile and shared task/writer integration", "device owner/fence race and connection lifetime", "writer-internal rejected yield database rollback", "never-poll 10-second shutdown on Android", "memory/thermal/screen-off fault injection", "24h and 72h duration acceptance"],
        notEvidenceFor=["SQL fence integration by this host shim", "complete LegacyGeneration end-to-end DB recovery", "real ResearchPool helper task/fence group integration by this bounded executor fixture", "Android device scheduling or resource consumption", "natural future quality or promotion"],
        commands=commands,
    )
    if args.phase=="P02":receipt["p02AdditionalEvidence"]={"sameOwnerCancelledTerminatedContextReloads":2,"lastOldResultDiscarded":True,"namedCancellationDoesNotProveTermination":True,"controlledLifecycleBody":"SYNTHETIC_ORCHESTRATION_NOT_QUALITY_OR_FULL_DB_MODEL_RECOVERY"}
    (out / "RECEIPT.json").write_text(json.dumps(receipt, ensure_ascii=False, indent=2) + "\n")
    (out / "FAILED_COMMAND.json").unlink(missing_ok=True)
    print(json.dumps(dict(status="HOST_PASS", junit=receipt["tests"]["coreJUnit"], productionReplayJUnit=replay_tests, realReplayCandidates=128, controlledOverlapRaces=2, sequentialSwitches=10000, exactOriginalNumericalParity=True, androidDevice="NOT_RUN", naturalQuality="NOT_EVALUATED")))


if __name__ == "__main__":
    main()
