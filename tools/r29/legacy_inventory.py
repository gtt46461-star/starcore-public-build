#!/usr/bin/env python3
"""Recompute B01/R29-007's source/asset/evidence inventory, without opening app databases.

Reads only repository source, public packaged registries/assets and named engineering receipts.
Does not decode/print archived state, run models, build, access credentials, or grant acceptance.
"""
import argparse
import csv
import hashlib
import io
import json
import re
import subprocess
from collections import Counter
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
JAVA = "app/src/main/java/com/ethk3/novaonepro/"
V2 = JAVA + "strategyv2/"
PLAN = "construction/plans/STARCORE_R29_FINAL_20261029/"
LEGACY_REG = "construction/plans/STARCORE_R26_MERGED_20261026/sources/plan_b/baseline_R24/registries/"
ARCHIVE = "app/src/main/assets/research/r967_history/R908_125_algorithms.tsv"


def text(path):
    return (ROOT / path).read_text(encoding="utf-8")


def load(path):
    return json.loads(text(path))


def pin(path):
    p = ROOT / path
    return {"path": path, "present": p.is_file(),
            "bytes": p.stat().st_size if p.is_file() else None,
            "sha256": hashlib.sha256(p.read_bytes()).hexdigest() if p.is_file() else None}


def host_receipt(path):
    p = ROOT / path
    if not p.is_file():
        return {"status": "NOT_RUN", "path": path}
    lines = p.read_text().splitlines()
    passed = [x.split("|")[1] for x in lines if x.startswith("R29_LEGACY_STATE|") and x.endswith("|OK")]
    failed = [x for x in lines if x.startswith("R29_LEGACY_STATE|") and "|FAILED" in x]
    complete = any("R29_LEGACY_STATE|ALL_PASS|checks=12|" in x for x in lines)
    return {**pin(path), "status": "PASS_HOST_PROTOCOL_ONLY" if complete and len(passed) == 12 and not failed else "FAIL_OR_INCOMPLETE",
            "checks": passed, "actual_model_fixture": "one MSDIR model, 40 synthetic updates, then restore and next update equality",
            "not_proven": ["all 125 archived R908 programs", "all 24 families and 16 expert states", "Android scheduling endurance", "natural quality"]}


def archive_receipt():
    path = "evidence/r29/batch01/legacy-archive-host/legacy-archive-continuity.json"
    if not (ROOT / path).is_file():
        return {"status": "NOT_RUN", "path": path}
    r = load(path)
    return {**pin(path), "status": r["result"], "passed": r["passed"], "missing": r["missing"], "failed": r["failed"],
            "periods_per_asset": r["periodsPerAsset"], "actions_per_period": r["actionsPerPeriod"],
            "restarts_per_asset": r["restartsPerAsset"], "scope": r["scope"],
            "archive_hash_matches_current": r["archiveSha256"] == pin(ARCHIVE)["sha256"],
            "historical_external_device_golden_alignment": "NOT_RUN", "natural_evidence_transferred": False}


def inventory():
    family_defs = load(LEGACY_REG + "family_registry_r23.json")
    expert_defs = load(LEGACY_REG + "expert_registry_r23.json")
    registry = text(V2 + "FamilyRegistryR26.java")
    runtime = text(V2 + "RuntimeCoordinatorR26.java")
    adapter = text(V2 + "LegacyCapabilityAdapterR29.java")
    pool_runtime = text(V2 + "RuntimeR27.java")
    vm = text(JAVA + "AlgorithmGenomeR908.java")
    vm_ops = set(re.findall(r"\b[A-Z][A-Z_]+\b", vm.split("public enum Op {", 1)[1].split("}", 1)[0]))
    rows = list(csv.DictReader(io.StringIO(text(ARCHIVE)), delimiter="\t"))
    used_ops = set()
    for row in rows:
        used_ops.update(re.findall(r"(?:^|[=;])([A-Z][A-Z_]+),", row["genome_spec"]))
    snapshot = text(V2 + "LegacyCapabilitySnapshotR29.java")
    exporter = text(V2 + "LegacyPolicyExportR27.java")
    legacy_classification_gap = "VALUES(?,?,'LEGACY_NOT_REPRODUCIBLE'" in exporter
    full_state = text(V2 + "LegacyReplayStateR26.java")
    lanes = text(JAVA + "ResearchLearningContinuityR912.java")
    lane_switch = lanes.split("switch(lane)", 1)[1].split("default:", 1)[0]
    lane_rows = []
    for m in re.finditer(r"case (\d+):([\s\S]*?)(?=\n\s*case \d+:|$)", lane_switch):
        body = m.group(2)
        lane_rows.append({"lane": int(m.group(1)), "source": JAVA + "ResearchLearningContinuityR912.java",
                          "consumers": sorted(set(re.findall(r"\b([A-Z][A-Za-z0-9]*(?:R\d+|CP\d+))\.", body))),
                          "retired_duplicate_owner": "RETIRED_CP12_" in body,
                          "pool_adapter_wired": "pulseOnResearchPool" in adapter,
                          "golden_output_state_alignment": "NOT_RUN"})
    lane_rows.sort(key=lambda x: x["lane"])
    fam_rows = []
    for r in family_defs:
        fid = r["id"]
        fam_rows.append({"id": fid, "name": r["name"], "dispatch_source": V2 + "FamilyRegistryR26.java",
                         "source_dispatch_case_present": 'case "' + fid + '"' in registry,
                         "source_runtime_registration_present": 'i<=24' in runtime,
                         "unified_dispatcher_source_present": "runtime.runPoolLegacySlice()" in adapter,
                         "binding_condition": "original R908 teacher asset + exact state + R26F23 adapter" if fid == "F23" else "concrete kernel; unavailable implementation fails explicitly",
                         "all_family_golden_state_alignment": "NOT_RUN",
                         "natural_quality": "NOT_ESTABLISHED"})
    expert_rows = [{"id": r["id"], "name": r["name"], "dispatch_source": V2 + "ExpertWorkshopR26.java",
                   "runtime_registration_source_present": 'i<=16' in runtime,
                   "mode": "evidence audit" if r["id"] in {"E01", "E02", "E15", "E16"} else "resumable paired numerical experiment",
                   "full_expert_acceptance": "NOT_RUN", "natural_quality": "NOT_ESTABLISHED"} for r in expert_defs]
    receipts = []
    for p in sorted((ROOT / "evidence/r29/checkpoint07/failed").glob("android-*/r26-runtime-synthetic.json")):
        r = json.loads(p.read_text())
        receipts.append({**pin(str(p.relative_to(ROOT))), "overall_result": r.get("result"),
                         "families_with_actual_progress": r.get("familyMechanismsWithCommittedProgress"),
                         "experts_with_actual_receipts": r.get("expertsWithActualReceipts"),
                         "all_mechanisms_service_ms": r.get("allMechanismsServiceMillis"),
                         "retention_boundary_renewed": r.get("retentionBoundaryRenewed", False),
                         "export_receipts": r.get("retainedLegacyStateExports"),
                         "failure": r.get("error"), "scope": "SYNTHETIC_ANDROID; a failed run is not batch acceptance"})
    preservation = load("app/src/main/assets/r29/legacy_preservation.json")
    cards = load(PLAN + "registries/construction_cards.json")
    card = next(c for c in cards if c["id"] == "R29-007")
    archive_test = archive_receipt()
    source_paths = [ARCHIVE, V2 + "FamilyRegistryR26.java", V2 + "ExpertWorkshopR26.java",
                    V2 + "ExpertExperimentR26.java", V2 + "LegacyCapabilityAdapterR29.java",
                    V2 + "LegacyCapabilitySnapshotR29.java", V2 + "LegacyPolicyExportR27.java",
                    V2 + "LegacyContinuityR27.java", V2 + "LegacyReplayStateR26.java",
                    V2 + "LegacyReplayR26.java", V2 + "CheckpointReferencesR26.java",
                    V2 + "CheckpointArtifactsR26.java", V2 + "RuntimeCoordinatorR26.java",
                    V2 + "RuntimeR27.java", JAVA + "LegacyTeacherOwnerR26.java",
                    JAVA + "AlgorithmGenomeR908.java", JAVA + "AlgorithmGenomeVmR908.java",
                    JAVA + "ResearchLearningContinuityR912.java", JAVA + "AtomicToolWorldR943.java",
                    JAVA + "MolecularToolWorldR947.java", JAVA + "GenesisSchemaRegistryR915.java",
                    "app/src/main/assets/cp4_atom_registry.jsonl",
                    "app/src/main/assets/cp4_catalog_manifest.json",
                    "app/src/main/assets/r29/legacy_preservation.json"]
    genesis = text(JAVA + "GenesisSchemaRegistryR915.java")
    counts = {k: int(v) for k, v in re.findall(r"public static final int (EXPECTED_[A-Z_]+)\s*=\s*(\d+)", genesis)}
    return {
        "schema": "R29_B01_LEGACY_CAPABILITY_INVENTORY_1",
        "status": "SOURCE_AND_ARCHIVE_INVENTORY_COMPLETE_FULL_R29_007_ACCEPTANCE_NOT_RUN",
        "scope": "repository aggregate metadata only; no application database or private signing input read; source presence is not execution proof",
        "source_commit_observed": subprocess.check_output(["git", "rev-parse", "HEAD"], cwd=ROOT, text=True).strip(),
        "card": {"id": card["id"], "acceptance_test": card["acceptance_test"], "metrics": card["metrics"]},
        "family_mechanisms": fam_rows, "experts": expert_rows, "original_maintenance_lanes": lane_rows,
        "archive": {**pin(ARCHIVE), "programs": len(rows), "unique_genome_ids": len({r["genome_id"] for r in rows}),
                    "state_blobs_present": sum(bool(r["state_blob"]) for r in rows),
                    "nonzero_reported_state_updates": sum(int(r["state_updates"]) > 0 for r in rows),
                    "state_wire_prefix_counts": dict(sorted(Counter(r["state_blob"].split("|", 1)[0] for r in rows).items())),
                    "used_operator_count": len(used_ops), "operator_enum_count": len(vm_ops), "operators_missing_from_original_enum": sorted(used_ops - vm_ops),
                    "executable_original_vm_retained": (ROOT / (JAVA + "AlgorithmGenomeVmR908.java")).is_file(),
                    "F23_teacher_selection_scope": "ONE original archived program selected by historical dev_hit_rate; not all 125 teacher executions",
                    "all_125_program_current_original_VM_restart_continuity": archive_test["status"],
                    "historical_external_device_golden_alignment": "NOT_RUN", "natural_evidence_transferred": False},
        "complete_state_export": {"source": V2 + "LegacyCapabilitySnapshotR29.java", "source_table": "r26_legacy_runtime",
                                  "retained_state_codec": "R26_LEGACY_STATE_0x264c5301 + chunk CAS_0x26435302",
                                  "roster_member_capacity": int(re.search(r"MAX_MEMBERS=(\d+)", full_state).group(1)),
                                  "original_state_pin_source_present": "pinCurrentWithin" in snapshot,
                                  "CAS_restore_source_present": "artifacts.restoreLegacy" in snapshot,
                                  "separate_golden_equivalence_required": "SEPARATE_GOLDEN_TRAJECTORY_RECEIPT_REQUIRED" in snapshot,
                                  "pool_task_registered": "pool.register(legacySnapshot)" in pool_runtime,
                                  "natural_evidence_transferred": False,
                                  "limits": "only the current retained R26 model/committee/stage cohort; not an export receipt for every archived R908 program or historical private installation"},
        "atom_tool_registries": {"source": JAVA + "GenesisSchemaRegistryR915.java", "declared_cardinalities": counts,
                                 "cp4_atom_registry_lines": len([s for s in text("app/src/main/assets/cp4_atom_registry.jsonl").splitlines() if s.strip()]),
                                 "pool_consumer_source": [V2 + "LegacyCapabilityAdapterR29.java", JAVA + "ResearchLearningContinuityR912.java"],
                                 "complete_all_tools_golden_state_alignment": "NOT_RUN"},
        "preservation": {"source": "app/src/main/assets/r29/legacy_preservation.json", "requirements": len(preservation["requirements"]),
                         "file_cards": len(preservation["fileCards"]), "by_closure": dict(sorted(Counter(r["closure"] for r in preservation["requirements"]).items())),
                         "all_1832_requirements_regression": "NOT_RUN"},
        "host_state_protocol_receipt": host_receipt("evidence/r29/checkpoint08/host/legacy.log"),
        "all_125_original_archive_host_receipt": archive_test,
        "legacy_export_classification": {"source": V2 + "LegacyPolicyExportR27.java",
                                         "R27_AST_failure_used_as_original_executability_status": legacy_classification_gap,
                                         "status": "SOURCE_CLASSIFICATION_GAP" if legacy_classification_gap else "SOURCE_CLASSIFICATION_CHANGED_RUNTIME_RECEIPT_REQUIRED"},
        "retained_android_failures": receipts,
        "missing_evidence": [
            {"item": "historical external golden/ABI comparison", "missing": "archived old-device per-period output/state traces under old kernels; current-original-kernel host restart equality does not establish cross-build numerical ABI equivalence", "consumer": "LegacyPolicyExportR27 / I12 / LegacyTeacherOwnerR26", "status": "NOT_RUN"},
            {"item": "all family/expert/tool states", "missing": "complete per-capability golden state/alignment receipts; source registration and first service alone do not prove numerical equivalence", "consumer": "FamilyRegistryR26 / ExpertWorkshopR26 / ResearchLearningContinuityR912", "status": "NOT_RUN"},
            {"item": "original learned private installation states", "missing": "no user database or corresponding historical dependency bundle supplied to this read-only inventory; no recovery assumed", "consumer": "LegacyCapabilitySnapshotR29", "status": "UNKNOWN_NOT_READ"},
            {"item": "legacy export classification", "missing": "original-VM executability and R27 AST conversion need distinct classifications and runtime receipts; a source change alone is not a runtime receipt", "consumer": "LegacyPolicyExportR27", "status": "SOURCE_CLASSIFICATION_GAP" if legacy_classification_gap else "RUNTIME_RETEST_PENDING"},
            {"item": "unified scheduler sustained service", "missing": "latest retained Android run failed retention renewal despite 24 family/16 expert first-service receipts; repaired source needs an actual passing device receipt", "consumer": "LegacyCapabilityAdapterR29 / ResearchPoolR27", "status": "FAIL_RETEST_PENDING"}
        ],
        "pins": [pin(p) for p in sorted(source_paths)],
        "recompute": "python3 tools/r29/legacy_inventory.py --out evidence/r29/batch01/legacy_inventory.json"
    }


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--out", type=Path, default=Path("evidence/r29/batch01/legacy_inventory.json"))
    args = p.parse_args()
    result = inventory()
    target = ROOT / args.out
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n")
    print(json.dumps({"output": str(args.out), "status": result["status"], "families": len(result["family_mechanisms"]),
                      "experts": len(result["experts"]), "lanes": len(result["original_maintenance_lanes"]),
                      "archived_programs": result["archive"]["programs"], "missing_evidence_categories": len(result["missing_evidence"])}, ensure_ascii=False))


if __name__ == "__main__":
    main()
