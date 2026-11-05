#!/usr/bin/env python3
"""Provide the same development environment to the unchanged local/CI R31.2 Gate."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import urllib.request

ROOT = Path(__file__).resolve().parents[2]
GATE = ROOT / "construction/STARCORE_R31_FINAL_20261104/tools/r312_gate.py"
JDBC_SHA256 = "4a4832720a65eaf7f4d6fd7ede52087b994dc5633c076f9e994dc0c8b4b0b4fa"
ANDROID_SHA256 = "d9eb9da824d9e247a352f570f01e1169e725b2954bca9e283a71786c59b59f9a"
JDBC_URL = "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar"


def digest(path):
    result = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1048576), b""):
            result.update(block)
    return result.hexdigest()


def prepare():
    java_home = os.environ.get("JAVA_HOME")
    local_jdk = Path("/workspace/starcore-environment/tools/jdk-17.0.17+10")
    if not java_home:
        java_home = str(local_jdk if local_jdk.is_dir() else Path(shutil.which("java") or "java").resolve().parent.parent)
    java_home = Path(java_home).resolve()
    version = subprocess.run([str(java_home / "bin/java"), "-version"], capture_output=True, text=True, check=True)
    if not re.search(r'version "17\.', version.stderr + version.stdout):
        raise ValueError("R312_JAVA17_REQUIRED")
    sdk = Path(os.environ.get("ANDROID_HOME") or os.environ.get("ANDROID_SDK_ROOT")
               or "/workspace/starcore-environment/android-sdk").resolve()
    android = sdk / "platforms/android-36/android.jar"
    if not android.is_file() or digest(android) != ANDROID_SHA256:
        raise ValueError("R312_ANDROID36_MISSING_OR_SHA256_MISMATCH")
    explicit = os.environ.get("R312_SQLITE_JDBC")
    existing = Path(explicit) if explicit else Path("/tmp/r31-baseline-deps/sqlite-jdbc.jar")
    if existing.is_file():
        jdbc = existing.resolve()
        if digest(jdbc) != JDBC_SHA256:
            raise ValueError("R312_JDBC_SHA256_MISMATCH")
    elif explicit:
        raise ValueError("R312_EXPLICIT_JDBC_FILE_MISSING")
    else:
        cache = Path(os.environ.get("RUNNER_TEMP") or tempfile.gettempdir()) / "r312-channel"
        cache.mkdir(parents=True, exist_ok=True)
        jdbc = cache / "sqlite-jdbc-3.46.1.3.jar"
        if not jdbc.is_file() or digest(jdbc) != JDBC_SHA256:
            partial = jdbc.with_suffix(".download")
            try:
                with urllib.request.urlopen(JDBC_URL, timeout=30) as response, partial.open("wb") as output:
                    size = 0
                    while True:
                        block = response.read(1048576)
                        if not block:
                            break
                        size += len(block)
                        if size > 32 * 1024 * 1024:
                            raise ValueError("R312_JDBC_DOWNLOAD_SIZE_LIMIT")
                        output.write(block)
                if digest(partial) != JDBC_SHA256:
                    raise ValueError("R312_JDBC_DOWNLOAD_SHA256_MISMATCH")
                partial.replace(jdbc)
            finally:
                partial.unlink(missing_ok=True)
    return {"JAVA_HOME": str(java_home), "ANDROID_HOME": str(sdk), "ANDROID_SDK_ROOT": str(sdk),
            "R312_ANDROID_JAR": str(android), "R312_SQLITE_JDBC": str(jdbc),
            "ORG_GRADLE_PROJECT_r312Development": "true"}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    prep = sub.add_parser("prepare")
    prep.add_argument("--github-env", type=Path)
    gate = sub.add_parser("gate")
    gate.add_argument("arguments", nargs=argparse.REMAINDER)
    args = parser.parse_args()
    if args.command == "prepare":
        selected = prepare()
        if args.github_env:
            with args.github_env.open("a") as output:
                for name, value in selected.items():
                    if "\n" in value or "\r" in value:
                        raise ValueError("R312_INVALID_ENVIRONMENT_PATH")
                    output.write(name + "=" + value + "\n")
        print(json.dumps({"scope": "DEVELOPMENT_CHANNEL_ONLY_NOT_BUILD_PASS", "java": "17",
                          "androidJarSha256": ANDROID_SHA256, "jdbcSha256": JDBC_SHA256,
                          "selectedPaths": selected}, indent=2))
        return
    arguments = args.arguments
    if not arguments:
        parser.error("gate requires the original Gate command and arguments")
    selected = prepare() if arguments[0] == "build" and "--import" not in arguments else {
        "ORG_GRADLE_PROJECT_r312Development": "true"}
    env = dict(os.environ)
    env.update(selected)
    if "--batch" in arguments:
        batch = arguments[arguments.index("--batch") + 1]
    else:
        status = subprocess.check_output([sys.executable, str(GATE), "status"], cwd=ROOT, env=env, text=True)
        batch = json.loads(status)["open_batch"]
    if not re.fullmatch(r"S(?:0[0-9]|1[0-9]|2[01])", batch or ""):
        raise ValueError("R312_BATCH_ID_INVALID")
    env["R312_CURRENT_BATCH"] = batch
    os.chdir(ROOT)
    os.execve(sys.executable, [sys.executable, str(GATE), *arguments], env)


if __name__ == "__main__":
    main()
