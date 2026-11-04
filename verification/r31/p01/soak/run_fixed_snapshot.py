"""Run or detach the explicitly scoped frozen HOST contention fixture, never a device soak."""
import argparse
import datetime
import hashlib
import json
import pathlib
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("snapshot", type=pathlib.Path)
parser.add_argument("output", type=pathlib.Path)
parser.add_argument("--seconds", type=int, default=3600)
parser.add_argument("--wait", action="store_true")
args = parser.parse_args()
if args.seconds < 1:
    parser.error("seconds must be positive")
snapshot = args.snapshot.resolve()
manifest = json.loads((snapshot / "snapshot.json").read_text())
for name, expected in manifest["compiled_class_sha256"].items():
    if hashlib.sha256((snapshot / name).read_bytes()).hexdigest() != expected:
        raise SystemExit("frozen class changed: " + name)
for name, field in [("android.jar", "android_jar_sha256"), ("sqlite-jdbc.jar", "sqlite_jdbc_sha256")]:
    if hashlib.sha256((snapshot / name).read_bytes()).hexdigest() != manifest[field]:
        raise SystemExit("frozen runtime dependency changed: " + name)
output = args.output.resolve()
output.mkdir(parents=True, exist_ok=False)
database = output / "fixture.db"
command = manifest["command"] + [str(database), str(output), str(args.seconds)]
with (output / "process.log").open("w") as log:
    process = subprocess.Popen(command, stdin=subprocess.DEVNULL, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
receipt = {"format": "R31_HOST_CONTENTION_LAUNCH_V1", "status": "IN_PROGRESS", "pid": process.pid,
           "requested_at": datetime.datetime.now(datetime.timezone.utc).isoformat(), "requested_seconds": args.seconds,
           "snapshot": str(snapshot), "snapshot_manifest_sha256": hashlib.sha256((snapshot / "snapshot.json").read_bytes()).hexdigest(),
           "command": command, "production_source_sha256": manifest["source_sha256"],
           "scope": "HOST synthetic writer/native SQLite fixture; not Android or 24/72-hour device acceptance"}
(output / "launch.json").write_text(json.dumps(receipt, indent=2) + "\n")
print(json.dumps({"pid": process.pid, "status": "IN_PROGRESS", "output": str(output)}), flush=True)
if args.wait:
    result = process.wait()
    print((output / "process.log").read_text(), end="")
    raise SystemExit(result)
