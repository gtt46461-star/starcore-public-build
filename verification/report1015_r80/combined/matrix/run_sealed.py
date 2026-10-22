"""Log the unchanged inherited matrix runner with a stricter JVM heap ceiling.

No product, fixture assertions, formulas, SQL cardinalities, or timeouts change.
The original -Xmx512m request and effective -Xmx256m request are both recorded.
"""
from pathlib import Path
import datetime
import hashlib
import json
import os
import runpy
import subprocess
import sys
import time
import traceback

ROOT = Path(__file__).resolve().parents[4]
RUNNER = ROOT / "verification/report1015/r79_matrix/run.py"
OUT = Path(os.environ["STARCORE_R79_MATRIX_OUT"])
META = Path(str(OUT) + "-invocation")
META.mkdir(parents=True, exist_ok=False)
CONFIG = Path(os.environ["STARCORE_HOST_CONFIG"])
SCHEMA = Path(os.environ["STARCORE_HOST_SCHEMA"])
AST = Path(os.environ["STARCORE_TEST_AST"])
config = json.loads(CONFIG.read_text())
sha = lambda p: hashlib.sha256(Path(p).read_bytes()).hexdigest()
tracked = [RUNNER, RUNNER.with_name("sql_scale.py"), *RUNNER.parent.glob("*.java"),
           ROOT / "verification/report1015/MatrixSourcesR78.java"]
tracked += [ROOT / "verification/canonical/extended" / n for n in (
    "ModernModelContractTest.java", "MatrixBindingsCP12Test.java", "MatrixHistoricalDrawCP12Test.java")]
source_before = {str(p.relative_to(ROOT)): sha(p) for p in tracked}
product = Path(config["product"])
product_before = {str(p.relative_to(product)): sha(p) for p in sorted(product.rglob("*.class"))}
manifest = CONFIG.parent / "product_hashes.json"
assert product_before == json.loads(manifest.read_text()), "SEALED_PRODUCT_MANIFEST_MISMATCH"
for name in ("JAVA_TOOL_OPTIONS", "JDK_JAVA_OPTIONS", "_JAVA_OPTIONS"):
    assert not os.environ.get(name), "UNRECORDED_JAVA_OPTION_ENVIRONMENT:" + name
receipt = {
    "started_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
    "command": [sys.executable, str(Path(__file__).resolve())],
    "cwd": str(Path.cwd()),
    "environment": {k: os.environ[k] for k in (
        "STARCORE_HOST_CONFIG", "STARCORE_HOST_SCHEMA", "STARCORE_TEST_AST", "STARCORE_R79_MATRIX_OUT")},
    "config_sha256": sha(CONFIG), "schema_sha256": sha(SCHEMA), "input_ast_sha256": sha(AST),
    "prepared_product_manifest_sha256": sha(manifest), "product_classes": len(product_before),
    "test_sources_before": source_before, "wrapper_sha256": sha(__file__),
    "heap_policy": {"inherited_java_max_heap_mib": 512, "effective_java_max_heap_mib": 256,
                    "javac_max_heap_mib": 256, "reason": "stricter resource ceiling for concurrent audit workloads",
                    "expectation_formula_timeout_sql_load_changes": False},
}
(META / "invocation.json").write_text(json.dumps(receipt, indent=2) + "\n")
executions = []
original_run = subprocess.run

def logged_run(args, *positional, **kwargs):
    original = list(map(str, args))
    effective = list(original)
    if effective[0] == config["java"]:
        effective = ["-Xmx256m" if a == "-Xmx512m" else a for a in effective]
        assert "-Xmx256m" in effective, "JVM_HEAP_CEILING_MISSING"
    elif effective[0] == config["javac"]:
        effective.insert(1, "-J-Xmx256m")
    record = {"sequence": len(executions) + 1, "original_command": original,
              "effective_command": effective, "timeout_seconds": kwargs.get("timeout"),
              "stdout_path": str(getattr(kwargs.get("stdout"), "name", "captured")),
              "started_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    start = time.monotonic()
    try:
        result = original_run(effective, *positional, **kwargs)
        record["exit_code"] = result.returncode
        return result
    except BaseException as error:
        record["error_type"] = type(error).__name__
        record["error"] = str(error)
        record["exit_code"] = getattr(error, "returncode", None)
        raise
    finally:
        record["elapsed_seconds"] = time.monotonic() - start
        executions.append(record)
        (META / "executions.json").write_text(json.dumps(executions, indent=2) + "\n")

subprocess.run = logged_run
status = 1
try:
    with (META / "heap-verification.log").open("w") as handle:
        logged_run([config["java"], "-Xmx256m", "-XshowSettings:vm", "-version"],
                   stdout=handle, stderr=subprocess.STDOUT, check=True, timeout=30)
    sys.path.insert(0, str(RUNNER.parent))
    runpy.run_path(str(RUNNER), run_name="__main__")
    status = 0
except BaseException:
    traceback.print_exc()
finally:
    subprocess.run = original_run
    source_after = {str(p.relative_to(ROOT)): sha(p) for p in tracked}
    product_after = {str(p.relative_to(product)): sha(p) for p in sorted(product.rglob("*.class"))}
    receipt.update({
        "finished_at_utc": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "runner_exit_code": status,
        "test_sources_unchanged": source_before == source_after,
        "product_class_bytes_unchanged": product_before == product_after,
        "prepared_manifest_unchanged": sha(manifest) == receipt["prepared_product_manifest_sha256"],
        "schema_bytes_unchanged": sha(SCHEMA) == receipt["schema_sha256"],
        "input_ast_bytes_unchanged": sha(AST) == receipt["input_ast_sha256"],
        "test_sources_after": source_after,
    })
    (META / "invocation.json").write_text(json.dumps(receipt, indent=2) + "\n")
    (META / "product_hashes.json").write_text(json.dumps(product_before, indent=2) + "\n")
    assert all(receipt[k] for k in ("test_sources_unchanged", "product_class_bytes_unchanged",
                                  "prepared_manifest_unchanged", "schema_bytes_unchanged", "input_ast_bytes_unchanged"))
sys.exit(status)
