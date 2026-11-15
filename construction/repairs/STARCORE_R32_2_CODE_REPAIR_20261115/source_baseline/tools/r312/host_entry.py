#!/usr/bin/env python3
"""Run one registered HOST command on this gate's actual support, without changing tests/heaps."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import hashlib
import os
from pathlib import Path
import re
import subprocess
import sys

from prepare_host_support import BUILD, ROOT, current_batch, prepare, source_identity


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--id", required=True)
    parser.add_argument("--source-only", action="store_true", help="For explicit pure Python/source validators only; does not claim an Android class input")
    parser.add_argument("command", nargs=argparse.REMAINDER, help="Original python/script argv after --; support placeholders are resolved from the actual gate build")
    args = parser.parse_args()
    if not re.fullmatch(r"[A-Za-z0-9_-]+", args.id):
        raise ValueError("INVALID_HOST_ID")
    template = args.command[1:] if args.command[:1] == ["--"] else args.command
    if not template:
        raise ValueError("ORIGINAL_HOST_COMMAND_REQUIRED")
    started = datetime.now(timezone.utc).isoformat()
    identity = source_identity()
    batch = current_batch()
    context_path = ROOT / f"verification/r31/r312/{batch}/GATE_CONTEXT.json"
    context = json.loads(context_path.read_text())
    if context["source_head"] != identity["source_head"]:
        raise ValueError("CURRENT_GATE_PREBUILD_CONTEXT_SOURCE_MISMATCH")
    journal = context_path.with_name("HOST_JOURNAL.jsonl")
    journal.parent.mkdir(parents=True, exist_ok=True)
    suite = json.loads((ROOT / "construction/r31/r312/HOST_SUITE.json").read_text())
    registered = next(entry["command"] for entry in suite.get("host", []) + suite.get("post_build", []) if entry["id"] == args.id)
    row = dict(identity, run_id=context["run_id"], entry_pid=os.getpid(), host_id=args.id, id=args.id,
               event="START", started_at=started, status="PREPARING_NOT_PASS", original_command=template,
               registered_command=registered, registered_command_sha256=hashlib.sha256(registered.encode()).hexdigest())

    def record():
        with journal.open("a") as stream:
            stream.write(json.dumps(row, sort_keys=True) + "\n")

    record()
    try:
        support = None if args.source_only else prepare()
        out = BUILD / "host-runs" / batch / context["run_id"] / args.id
        out.parent.mkdir(parents=True, exist_ok=True)
        substitutions = {"repo": str(ROOT), "out": str(out)}
        if support:
            substitutions.update({"classes_jar": support["classes_jar"], "classes_dir": support["classes_dir"],
                                  "r_jar": support["r_jar"], "jdk": support["java_home"], "sdk": support["sdk"],
                                  "jdbc": support["jdbc"], "platform_manifest": support["platform_manifest"],
                                  "ahead_receipt": support["receipt"],
                                  "writer_outcomes_ahead": support["writer_outcomes_ahead"]})
        command = []
        for argument in template:
            for key, value in substitutions.items():
                argument = argument.replace("{" + key + "}", value)
            if re.search(r"\{[A-Za-z_]+\}", argument):
                raise ValueError("UNKNOWN_HOST_ARGUMENT_PLACEHOLDER:" + argument)
            command.append(argument)
        env = dict(os.environ)
        temporary = out.parent / (args.id + "-tmp")
        temporary.mkdir(parents=True, exist_ok=True)
        env.update({"TMPDIR": str(temporary), "R312_HOST_OUTPUT": str(out),
                    "R31_MEMORY_OUTPUT": str(out), "R31_MEMORY_CONTROL_OUT": str(out)})
        env["JAVA_TOOL_OPTIONS"] = (env.get("JAVA_TOOL_OPTIONS", "") + " -Djava.io.tmpdir=" + str(temporary)).strip()
        if support:
            env.update({"R31_HOST_CLASSES": support["classes_jar"], "R31_HOST_SUPPORT_CLASSES": support["classes_jar"],
                    "R31_CLASSES": support["classes_dir"], "R31_MEMORY_CLASSES": support["classes_dir"],
                    "R31_MEMORY_CONTROL_SUPPORT": support["classes_jar"], "R31_HOST_RJAR": support["r_jar"],
                    "R31_HOST_ANDROID_JAR": support["sdk"], "SQLITE_JDBC": support["jdbc"],
                        "R31_RUNTIME_OUTPUT": str(out), "R312_ACTUAL_HOST_SUPPORT_RECEIPT": support["receipt"]})
        env.update({"R312_GATE_CONTEXT": str(context_path), "R312_HOST_JOURNAL": str(journal),
                    "R312_HOST_ENTRY_ID": args.id, "R312_HOST_RUN_ID": context["run_id"],
                    "R312_HOST_ENTRY_PID": str(os.getpid())})
        row.update(event="RUNNER_START", status="RUNNING_NOT_PASS", command=command,
                   command_sha256=hashlib.sha256(json.dumps(command, separators=(",", ":")).encode()).hexdigest())
        if support:
            row.update(support_receipt=support["receipt"], classes_jar_sha256=support["classes_jar_sha256"], r_jar_sha256=support["r_jar_sha256"])
        if len(command) < 2 or Path(command[0]).name not in {"python", "python3", "python3.12"}:
            raise ValueError("REGISTERED_PYTHON_HOST_REQUIRED_FOR_OUTPUT_ADAPTER")
        execution = [sys.executable, str(ROOT / "tools/r312/host_io.py"), "--script", command[1], "--output", str(out), "--", *command[2:]]
        row["executed_command"] = execution
        raw_log = out.parent / (args.id + "-RAW.log")
        print("R312_HOST_ACTUAL_COMMAND " + json.dumps(command), flush=True)
        with raw_log.open("wb") as target:
            process = subprocess.Popen(execution, cwd=ROOT, env=env, stdout=target, stderr=subprocess.STDOUT)
            row["child_pid"] = process.pid
            record()
            raw_exit = process.wait()
        raw_bytes = raw_log.read_bytes()
        row.update(raw_log=str(raw_log), raw_log_sha256=hashlib.sha256(raw_bytes).hexdigest(), raw_log_bytes=len(raw_bytes))
        sys.stdout.buffer.write(raw_bytes[-65536:])
        sys.stdout.flush()
        print("R312_HOST_RAW_LOG " + json.dumps({key: row[key] for key in ("raw_log", "raw_log_sha256", "raw_log_bytes")}), flush=True)
        stable = source_identity() == identity
        row.update(event="END", status="PASS" if raw_exit == 0 and stable else "FAIL", raw_exit=raw_exit, exit=raw_exit,
                   source_stable=stable, finished_at=datetime.now(timezone.utc).isoformat())
        record()
        return raw_exit if stable else 1
    except Exception as failure:
        row.update(event="END", status="FAIL_PRECONDITION_OR_RUNNER", exit=1, raw_exit=None, error=type(failure).__name__ + ":" + str(failure),
                   finished_at=datetime.now(timezone.utc).isoformat())
        record()
        print("R312_HOST_ENTRY_FAILED " + row["error"], file=sys.stderr, flush=True)
        return 1


if __name__ == "__main__":
    raise SystemExit(main())
