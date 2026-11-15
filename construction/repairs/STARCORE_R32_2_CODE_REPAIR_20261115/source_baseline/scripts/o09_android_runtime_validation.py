#!/usr/bin/env python3
"""Run scoped production contracts on one disposable emulator, never a user's phone.

APK files stay on the runner. Only logs/receipts are CI artifacts. Synthetic
runtime tests do not establish natural Future-OOS, model quality or O01-O09 closure.
"""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "com.ethk3.novaonepro"
CLASSES = (
    "R79RecoveryAndroidTest",
    "SqliteBusyPoolR77Test",
    "FeedbackBootstrapAndroidTest",
    "SqlNumericAffinityAndroidTest",
    "FoundationImportAndroidCP12Test",
    "ResearchContinuationAndroidTest",
    "R36RuntimeChainTest", "R36DatabaseAtomicityTest",
    "C07AndroidRuntimeScheduleTest",
    "R939MainChainTest", "ShadowCycleAndroidTest", "R940UiStabilityTest", "E03AAndroidParityTest",
)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--d04-parity-dir", type=Path)
    args = parser.parse_args()
    output = args.output.resolve()
    output.mkdir(parents=True, exist_ok=True)
    d04 = args.d04_parity_dir.resolve() if args.d04_parity_dir else None
    classes = CLASSES + (("D04AndroidRuntimeParityTest",) if d04 else ())
    sdk = Path(os.environ["ANDROID_HOME"])
    avd_home = Path(os.environ.get("RUNNER_TEMP", str(ROOT / "build"))) / "o09-avd"
    os.environ["ANDROID_AVD_HOME"] = str(avd_home)
    adb = str(sdk / "platform-tools/adb")
    device = [adb, "-s", "emulator-5554"]
    report = {"scope": "SYNTHETIC_ANDROID_RUNTIME_NOT_NATURAL_NOT_PHYSICAL_PHONE",
              "overall_1_9": "NOT_CLOSED", "natural": "NOT_PROVEN", "tests": [],
              "not_run": {"D04AndroidRuntimeParityTest": "requires independently produced host parity assets"}}
    if d04:
        prepared = json.loads((d04 / "prepare-receipt.json").read_text())
        if prepared.get("status") != "PASS":
            raise RuntimeError("D04 host preparation did not pass")
        report["not_run"] = {}

    def save():
        (output / "receipt.json").write_text(json.dumps(report, indent=2) + "\n")

    def command(argv, name, timeout=120, check=True):
        start = time.monotonic()
        with (output / (name + ".log")).open("w") as log:
            result = subprocess.run(argv, cwd=ROOT, stdout=log,
                                    stderr=subprocess.STDOUT, timeout=timeout)
        if check and result.returncode:
            raise RuntimeError(f"{name}: exit={result.returncode}")
        return result.returncode, time.monotonic() - start

    def one_apk(folder):
        files = sorted((ROOT / folder).glob("*.apk"))
        if len(files) != 1:
            raise RuntimeError(f"Expected exactly one APK in {folder}: {len(files)}")
        return files[0]

    def wait_package_manager(name):
        # sys.boot_completed alone can precede PackageManager/Binder readiness.
        # Keep raw probes so a system-service restart is not reported as an app test.
        deadline = time.monotonic() + 90
        stable = 0
        probes = []
        while time.monotonic() < deadline:
            probe = subprocess.run(device + ["shell", "cmd", "package", "list", "packages", "android"],
                stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, timeout=15)
            probes.append({"exit": probe.returncode, "output": probe.stdout})
            (output / (name + ".log")).write_text(json.dumps(probes, indent=2) + "\n")
            stable = stable + 1 if probe.returncode == 0 and "package:android" in probe.stdout.splitlines() else 0
            if stable == 3:
                return
            time.sleep(2)
        raise RuntimeError(name + ": package manager did not become ready")

    def install_apk(apk, name):
        for attempt in range(2):
            wait_package_manager(name + "-ready-" + str(attempt))
            # File transfer followed by installation avoids the fragile streamed
            # Binder pipe observed on the second cold boot of the disposable AVD.
            log_name = name if attempt == 0 else name + "-retry"
            code, _ = command(device + ["install", "--no-streaming", "-r", "-t", str(apk)],
                log_name, timeout=180, check=False)
            detail = (output / (log_name + ".log")).read_text()
            if code == 0 and "Success" in detail:
                return
            command(device + ["logcat", "-d", "-b", "crash"], log_name + "-system-crash", check=False)
            # Never retry away a genuine package rejection or accept a failed install.
            if "INSTALL_FAILED" in detail or not any(x in detail for x in ("Broken pipe", "device offline", "Can't find service")):
                break
        raise RuntimeError(name + ": installation failed; see exact attempt logs")

    # No adb operation below is allowed to target an attached physical device.
    if subprocess.check_output([adb, "devices"], text=True).count("emulator-5554"):
        raise RuntimeError("Refuse to reuse an existing emulator")
    process = None
    try:
        report["commit"] = subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip()
        app = one_apk("app/build/outputs/apk/debug")
        test = one_apk("app/build/outputs/apk/androidTest/debug")
        report["inputs"] = [{"path": str(p.relative_to(ROOT)), "sha256": hashlib.sha256(p.read_bytes()).hexdigest()}
                            for p in (app, test)]
        with (output / "emulator.log").open("w") as log:
            process = subprocess.Popen([str(sdk / "emulator/emulator"), "-avd", "o09-validation",
                "-port", "5554", "-no-window", "-no-audio", "-no-boot-anim", "-no-snapshot",
                "-gpu", "swiftshader_indirect", "-no-metrics", "-partition-size", "6144"],
                stdout=log, stderr=subprocess.STDOUT)
            limit = time.monotonic() + 300
            while time.monotonic() < limit:
                if process.poll() is not None:
                    raise RuntimeError("Emulator exited before boot")
                probe = subprocess.run(device + ["shell", "getprop", "sys.boot_completed"],
                    stdout=subprocess.PIPE, stderr=subprocess.DEVNULL, text=True, timeout=15)
                if probe.stdout.strip() == "1":
                    break
                time.sleep(2)
            else:
                raise RuntimeError("Emulator boot timeout")
            command(device + ["shell", "getprop"], "android-properties")
            install_apk(app, "install-app")
            install_apk(test, "install-test")
            for test_class in classes:
                print("O09_ANDROID_CLASS_START|" + test_class, flush=True)
                # New application process/database for every test class.
                command(device + ["shell", "am", "force-stop", PACKAGE], test_class + "-stop")
                command(device + ["shell", "pm", "clear", PACKAGE], test_class + "-clear")
                command(device + ["logcat", "-c"], test_class + "-logcat-clear")
                try:
                    code, seconds = command(device + ["shell", "am", "instrument", "-w", "-r",
                        "-e", "class", PACKAGE + "." + test_class,
                        PACKAGE + ".test/androidx.test.runner.AndroidJUnitRunner"],
                        test_class, timeout=900, check=False)
                    text = (output / (test_class + ".log")).read_text()
                    passed = code == 0 and re.search(r"OK \([1-9]\d* tests?\)", text) is not None
                    passed = passed and not any(token in text for token in
                        ("FAILURES!!!", "INSTRUMENTATION_FAILED", "Process crashed", "INSTRUMENTATION_ABORTED"))
                    if passed and test_class == "D04AndroidRuntimeParityTest":
                        # Extract before another pm clear. Never use device output as its own oracle.
                        try:
                            for name in ("d04_upstream_android.bin", "d04_online_android.bin", "d04_android_metrics.json"):
                                with (d04 / name).open("wb") as file:
                                    subprocess.run(device + ["exec-out", "run-as", PACKAGE, "cat", "files/" + name],
                                        stdout=file, stderr=subprocess.PIPE, check=True, timeout=60)
                            command([sys.executable, str(ROOT / "scripts/o09_d04_parity.py"), "compare", "--output", str(d04)],
                                    "D04-host-android-compare")
                        except (subprocess.SubprocessError, RuntimeError) as error:
                            passed = False
                            report["d04_error"] = str(error)
                            print("O09_D04_PARITY_FAILED|" + str(error), flush=True)
                    report["tests"].append({"class": test_class, "passed": passed,
                                            "exit": code, "seconds": round(seconds, 3)})
                    print("O09_ANDROID_CLASS_RESULT|" + json.dumps(report["tests"][-1]), flush=True)
                    if not passed:
                        print(text[-12000:], flush=True)
                except subprocess.TimeoutExpired:
                    report["tests"].append({"class": test_class, "passed": False, "reason": "TIMEOUT"})
                finally:
                    command(device + ["logcat", "-d", "-v", "threadtime"], test_class + "-logcat", check=False)
                    save()
            report["runtime_contracts"] = "PASS" if all(x["passed"] for x in report["tests"]) else "FAIL"
            save()
            print("O09_ANDROID_RUNTIME_RECEIPT|" + json.dumps(report), flush=True)
    except Exception as error:
        report["error"] = f"{type(error).__name__}: {error}"
        report["runtime_contracts"] = "FAIL"
        save()
        raise
    finally:
        if process is not None:
            if process.poll() is None:
                subprocess.run(device + ["emu", "kill"], stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, timeout=20)
            try:
                process.wait(timeout=30)
            except subprocess.TimeoutExpired:
                process.kill()
                process.wait(timeout=15)
    if report["runtime_contracts"] != "PASS":
        raise SystemExit(1)


if __name__ == "__main__":
    main()


