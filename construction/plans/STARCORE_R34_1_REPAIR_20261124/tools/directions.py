#!/usr/bin/env python3
"""Rebuild the field-to-construction direction register for job 4bef… from originals."""
import argparse
import hashlib
import json
from pathlib import Path


EXPECTED = {
    "UPGRADE_DIAGNOSIS.json": "c5020a250bff549d80770b826b2c8a641d428244fde0f801c2e468b50cc84999",
    "UPGRADE_DIAGNOSIS.md": "9e0e280c07d03aa1bb3d60ff275fa6f0ff2f7ba70f85dc04e9b7aa9281c536dd",
    "R31_SEMANTIC_RECEIPT.json": "e30f41c232f8fc3fd9dcd415d2fd24e66cddbe356b34e4c0c107bd9aed8ce832",
}
FAMILIES = ("MSDIR", "BOCPD", "HMM", "LOGLIN", "RELBAYES", "KALMAN", "REGMEM", "RELMEM", "CONTMAT")
# Real business owner for the prescription, including those reached from a read-only view.
TABLE_OWNER = (
    "07-01-A02", "08-07-A02", "09-02-A02", "08-05-A01", "08-03-A03", "08-06-A02",
    "06-03-A02", "06-04-A01", "08-07-A01", "08-06-A01", "07-03-A02", "09-03-A01",
    "09-01-A01", "02-03-T02", "08-04-A02", "04-04-T02",
)
GRAPH_OWNER = (
    "08-02-A01", "07-04-A02", "08-03-A03", "08-02-A01", "08-05-A01", "08-04-A01",
    "08-06-A02", "06-03-A02", "06-04-A01", "08-07-A01", "09-01-A01", "08-06-A02",
    "09-03-A01", "07-03-A01", "07-02-A02", "08-07-A01", "08-07-A02", "09-04-A02",
)
NEEDS = {
    "TRAINING_FAMILY_EXECUTION": ("06-01-A03", "06-02-A01", "06-04-A03"),
    "TEN_POSITION_TRAINING_SURFACE": ("07-01-A01", "07-02-A01", "07-03-A01", "07-05-A01"),
    "RESEARCH_ACTION_CONSUMPTION": ("08-02-A01", "08-04-A02", "08-07-A01"),
    "INDEPENDENT_QUALIFICATION": ("09-01-A02", "09-02-A02", "09-03-A01"),
    "DECISION_SETTLEMENT": ("04-04-T02", "09-04-A01"),
    "TABLE_SOURCE_AUDIT": ("A-B11-01b", "A-B11-04b"),
    "PERSISTED_FAULT_TRACE": ("02-02-T02", "A-B11-03b"),
    "CAPABILITY_NEED_DISCOVERY": ("A-B11-03b", "A-B11-05b"),
}
P50 = {
    "信息探针（P50）": ("08-01-A01", "08-07-A01"),
    "延续/转向矩阵（P49）": ("08-04-A01", "09-02-A03"),
    "阶段层（P49）": ("08-01-A02", "08-02-A01"),
    "区制记忆（P48）": ("08-01-A02", "08-07-A02"),
    "在线校准（P36）": ("09-03-A01",),
    "裁判与选择（P38/P40）": ("09-01-A02", "09-03-A01"),
    "委员会冠军（P37）": ("08-06-A02", "09-04-A01"),
    "进化与算子（P21/P41）": ("08-05-A01", "08-07-A01"),
    "研究记忆与原子（P44/P43）": ("08-04-A01", "08-07-A02"),
    "教师矩阵与原子蒸馏（P45/P47）": ("06-04-A03", "08-03-A03"),
    "主链 IPM（P30/P03）": ("09-04-A01", "04-04-T02"),
    "存储与内存（P31–P34/P50）": ("A-B10-01a", "A-B10-02c"),
}


def require(ok, reason):
    if not ok:
        raise ValueError(reason)


def digest(data):
    return hashlib.sha256(data).hexdigest()


def case_hash(*fields):
    # Match ReportRepairCaseIndexR335.key: byte length in ASCII, colon, UTF-8 field bytes.
    data=bytearray()
    for field in fields:
        raw=field.encode("utf-8")
        data.extend(str(len(raw)).encode("ascii") + b":" + raw)
    return digest(data)


def read(path):
    return json.loads(path.read_bytes())


def index(rows, field):
    result = {row[field]: row for row in rows}
    require(len(result) == len(rows), f"duplicate {field}")
    return result


def table(md, prefix, columns):
    """The selected historical Markdown tables have simple pipe-separated cells."""
    heading, *rest = md.split(prefix, 1)
    require(rest and heading, "missing section " + prefix)
    rows = []
    for line in rest[0].splitlines()[1:]:
        if line.startswith("##"):
            break
        if line.startswith("|"):
            cells = [x.strip() for x in line.strip().strip("|").split("|")]
            require(len(cells) == columns, "unexpected table structure in " + prefix)
            rows.append(cells)
    require(len(rows) >= 3, "empty table in " + prefix)
    return rows[2:]


def object_cases(group, ident):
    if group == "logicalTables":
        return ("A-B11-04a", TABLE_OWNER[int(ident[1:]) - 1])
    if group == "researchCharts":
        mode_case = "08-02-A03" if ident in {"R11", "R14", "R18"} else "08-02-A01"
        return (mode_case, GRAPH_OWNER[int(ident[1:]) - 1])
    if group == "legacyCharts":
        return ("A-B11-01b", "A-B11-04a")
    if group == "matrices" or ident.startswith("MATRIX:"):
        return ("08-03-A02", "08-03-A03")
    if ident == "ATOMNET":
        return ("07-02-A01", "07-03-A01", "07-05-A03")
    if ident == "F23":
        return ("06-02-A01", "06-04-A03")
    if ident.startswith("F"):
        return ("06-02-A01", "06-03-A03")
    if ident in {"E01", "E02", "E15", "E16"}:
        return ("06-04-A01",)
    if ident.startswith("E"):
        return ("06-04-A01", "08-04-A01")
    if ident == "I00":
        return ("09-05-A02", "09-05-A03")
    if ident.startswith("I"):
        return ("06-03-A01", "08-04-A01")
    raise ValueError(f"unmapped {group}/{ident}")


def build(report_dir, plan):
    report_dir, plan = Path(report_dir), Path(plan)
    originals = {}
    for name, expected in EXPECTED.items():
        blob = (report_dir / name).read_bytes()
        require(digest(blob) == expected, "report member identity mismatch: " + name)
        originals[name] = blob
    up = json.loads(originals["UPGRADE_DIAGNOSIS.json"])
    md = originals["UPGRADE_DIAGNOSIS.md"].decode()
    semantic = json.loads(originals["R31_SEMANTIC_RECEIPT.json"])
    scope = up["capabilityRepairR32"]
    group_cases = scope["improvementCasesR335"]
    cases, needs = group_cases["cases"], scope["capabilityNeedDiagnosticsR335"]
    old = index(read(plan / "specs/RESEARCH_OBJECT_CONTRACTS.json")["objects"], "key")
    order = index([x for b in read(plan / "WORK_ORDERS.json")["batches"] for x in b["issues"]], "id")
    tests = index(read(plan / "ACCEPTANCE_MATRIX.json")["cases"], "id")
    routes = {}

    def route(*short_ids):
        ids = list(dict.fromkeys(s if s.startswith("A-B") else "R341-B" + s for s in short_ids))
        for full in ids:
            entry = tests[full]
            require(entry["level"] == "ENGINEERING" and entry["issue_id"] in order, "invalid engineering route " + full)
            routes[full] = entry["issue_id"]
        return ids

    named_rows = {row["group"] + ":" + row["id"]: (i, row) for i, row in enumerate(scope["rows"])}
    require(len(named_rows) == len(scope["rows"]) == len(old) == len(cases) == group_cases["count"] == 165, "incomplete objects")
    require(set(named_rows) == set(old) == set(index(cases, "needKey")), "object key mismatch")
    require(len(index(cases, "caseKey")) == len(index(cases, "occurrenceId")), "case identity collision")
    objects = []
    for pos, c in enumerate(cases):
        k = c["needKey"]
        row_pos, r = named_rows[k]
        original = old[k]
        proof = r["evidenceContract"]
        require(r["nextAction"] == c["nextAction"] and r["verificationEntry"] == c["verificationEntry"]
                and proof["sourceObjectSha256"] == c["sourceObjectSha256"]
                and str(r["sourceSnapshot"]) == str(c["readWatermark"]) == str(scope["sourceSnapshot"])
                and c["sourceCommit"] == scope["sourceCommit"] == semantic["sourceCommit"]
                and c["mayAuthorizeReal"] is False, k + ": source observation differs")
        expected_key=case_hash("R335_CASE_V1", r["group"], r["id"])
        expected_occurrence=case_hash("R335_OCCURRENCE_V1", expected_key, c["sourceCommit"], c["readWatermark"],
                                      c["sourceObjectSha256"] or "", r["firstKnownGap"])
        require(c["caseKey"]==expected_key and c["occurrenceId"]==expected_occurrence, k + ": original key differs")
        require(all(x in order for x in original["work_orders"]), k + ": unknown owner")
        # Persist reference, not a second mutable copy, for the detailed product and reject contract.
        obj = dict(
            key=k, caseKey=c["caseKey"], occurrenceId=c["occurrenceId"],
            caseIndex=pos, observationIndex=row_pos, sourceObjectSha256=c["sourceObjectSha256"],
            sourceFile=c["sourceFile"], sourceMethod=c["sourceMethod"],
            observed={field: r[field] for field in ("implemented","callable","inputAvailable","produced","consumed","effective","notProducedReason","executionBlockedReason","firstKnownGap") if field in r},
            caseState=c["caseState"], engineeringStatus=c["engineeringStatus"], effectStatus=c["effectStatus"],
            rootCauseStatus=c["rootCauseStatus"], nextAction=c["nextAction"], verificationEntry=c["verificationEntry"],
            role=original["consumer_contract"]["mode"],
            implementation=original["work_orders"], tests=route(*object_cases(r["group"], r["id"])),
            sameDirectionAsFirstSnapshot=(c["nextAction"] == original["report_direction"]),
        )
        for source_field in ("implementationClass", "consumerFile", "consumerMethod", "producerClass", "producerMethod", "algorithmConsumer"):
            if r.get(source_field):
                obj[source_field] = r[source_field]
        objects.append(obj)

    need_rows = []
    require(set(index(needs["needResults"], "needKey")) == set(NEEDS), "need set mismatch")
    for n in needs["needResults"]:
        # Includes exact required signatures and inspection states. Source-window metadata is common.
        fields = ("needKey", "title", "sourceContract", "ownerBatch", "classification", "requiredInputs",
                  "producerPurpose", "consumerPurpose", "nextAction", "positiveVerification", "negativeVerification",
                  "requiredOwnerInspections", "closestExistingObjects", "registryEntrySha256", "ownerInspectionSha256",
                  "effectStatus", "proposalId", "runtimeBlockerApplicable", "mayAuthorizeReal")
        need_rows.append({**{k:n[k] for k in fields}, "tests":route(*NEEDS[n["needKey"]])})

    cap_rows = table(md, "## 5 各项能力动态迭代", 6)
    require({x[0] for x in cap_rows} == set(P50) and len(cap_rows) == len(P50), "P50 capability rows mismatch")
    cap_status = {x["name"]:x["status"] for x in up["capabilities"]}
    require(all(cap_status[n] == state for n,state,*_ in cap_rows), "markdown and structured P50 status disagree")
    caps = [dict(name=n, status=state, evidence=evidence, nextAction=action, projected=estimate, verify=verify,
                 tests=route(*P50[n])) for n,state,evidence,action,estimate,verify in cap_rows]
    family_table = table(md, "### 1.3 家族成效", 5)
    probe_table = table(md, "## 3 结构探测", 8)
    require(tuple(x[0] for x in family_table) == tuple(x[0] for x in probe_table) == FAMILIES,
            "probe or family row mismatch")
    probes = [dict(name=f[0], evolutionGain=f[1], forwardGain90=f[2], act15=f[3], recommendation=f[4],
                   probePeriods=t[1], probeForwardInterval=t[2], positiveBlocks=t[3], probeAct15=t[4],
                   prediction=t[5], projected=t[6], worstBlock=t[7], tests=route("08-01-A01","08-07-A02"))
              for f,t in zip(family_table,probe_table)]

    operating=scope["operatingSummary"]
    legacy=[]
    explanations=index(up["legacyFindingExplanationsR32"],"code")
    for finding in up["findings"]:
        code=finding["code"]
        require(code=="S4", "route new legacy finding before accepting it")
        legacy.append(dict(source=finding, explanation=explanations[code],
                           tests=route("08-04-A01","09-02-A03")))
    return {
        "schema":"R341_REPORT_DIRECTIONS_R3", "jobId":semantic["jobId"], "sourceCommit":semantic["sourceCommit"],
        "snapshot":scope["sourceSnapshot"], "memberSha256":EXPECTED,
        "objectContractFile":"specs/RESEARCH_OBJECT_CONTRACTS.json",
        "objectLink":"key connects to output_contract, consumer_contract, failure_contract and effect_contract. row/case indexes address capabilityRepairR32.rows and improvementCasesR335.cases in this job.",
        "routeLink":"tests lists the object-specific engineering runner(s). Each runner maps to one active work order in testOwners. A business producer is exercised by its owner, not by opening the linked view. Device and qualification evidence are additional, separate levels.",
        "priorityScope":"The source supplies severity for the stored finding. Processing order of remaining directions follows the work-order dependencies and input availability.",
        "sourceNeedScope":{k:needs[k] for k in ("needCount","registryHash","ownerManifestHash","coverageBoundary","candidateCapabilityProposalsR33","mayAuthorizeReal")},
        "coverageGaps":scope["coverageGaps"], "currentCapabilityProposals":scope["candidateCapabilityProposalsR33"],
        "objects":objects, "needs":need_rows,
        "storedP50":{"diagnosticSequence":up["legacySnapshotSequence"],"sourceScope":up["legacySnapshotScope"],
                     "findings":legacy,"capabilities":caps,"familyProbeRows":probes},
        "currentOperatingPrescriptions":{
            "foundryRepair":{"action":operating["foundryRepair"],"tests":route("06-01-A03","06-02-A01","09-01-A01")},
            "qualityCheck":{"action":operating["qualityCheck"],"tests":route("09-02-A02","09-03-A01","09-04-A02")},
            "atomNetArchiveMaintenance":{"source":operating["atomNetArchiveMaintenance"],"tests":route("07-05-A01","07-05-A03")}},
        "testOwners":dict(sorted(routes.items()))
    }


def main():
    cli=argparse.ArgumentParser(description=__doc__)
    cli.add_argument("--report-dir", type=Path, required=True)
    cli.add_argument("--plan", type=Path, required=True)
    dest=cli.add_mutually_exclusive_group(required=True)
    dest.add_argument("--out", type=Path)
    dest.add_argument("--check", type=Path)
    args=cli.parse_args()
    result=build(args.report_dir,args.plan)
    data=(json.dumps(result,ensure_ascii=False,indent=2,allow_nan=False)+"\n").encode()
    if args.check:
        if data != args.check.read_bytes():
            raise SystemExit("FAIL: direction register differs in source or construction mapping")
    else:
        args.out.write_bytes(data)
    print("PASS: 165 objects, 8 needs, 12 stored capability rows, 9 probe rows, "
          f"{len(result['storedP50']['findings'])} stored finding, 3 operating prescriptions")


if __name__=="__main__":
    main()
