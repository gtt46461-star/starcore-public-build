from collections import Counter
from contextlib import ExitStack
from pathlib import Path
import hashlib
import io
import json
import re
import sys
import zipfile


def emit(label, obj):
    print(label, json.dumps(obj, ensure_ascii=False, sort_keys=True), flush=True)


def selected(obj, keys):
    return {key: obj.get(key) for key in keys if key in obj}


def status_table(row):
    counts = {}
    for state in row.get("states", []):
        value = str(state.get("value"))
        col = state.get("column")
        if col in {"status","state","kind","stage","terminal_code","failure_class"} and re.fullmatch(r"[A-Z0-9_:/|.-]{1,100}", value):
            counts.setdefault(col, {})[value] = state.get("rows")
    return {"table": row["table"], "rows": row["rows"], "groups": counts}


path=Path(sys.argv[1])
with ExitStack() as stack:
    archive=stack.enter_context(zipfile.ZipFile(path))
    if "DIAGNOSTIC_SCOPE.json" not in archive.namelist():
        matches=[item for item in archive.infolist() if item.filename.rsplit("/",1)[-1]=="STARCORE_FULL_REPORT.zip"]
        assert len(matches)==1 and matches[0].file_size <= 160_000_000
        archive=stack.enter_context(zipfile.ZipFile(io.BytesIO(archive.read(matches[0]))))
    members=archive.infolist()
    assert len(members)==len({m.filename for m in members})
    assert sum(m.file_size for m in members)<=750_000_000
    assert archive.testzip() is None
    def doc(name):
        return json.loads(archive.read(name))
    src=doc("source-manifest.json")
    sem=doc("R31_SEMANTIC_RECEIPT.json")
    scope=doc("DIAGNOSTIC_SCOPE.json")
    r26=doc("R26_REPORT.json")
    r30=doc("R30_REPORT.json")
    assert src["sourceCommit"]==scope["sourceCommit"]==sem["sourceCommit"]
    assert src["snapshotSha256"]==scope["snapshotSha256"]==sem["snapshotSha256"]
    assert hashlib.sha256(archive.read("R30_REPORT.json")).hexdigest()==sem["reportSha256"]
    emit("REPORT_IDENTITY",selected(src,["sourceCommit","snapshotSha256","snapshotBytes","apkSha256","versionCode","appVersionCode","reportProfile"]))
    emit("CHECK_OUTCOMES",Counter((str(x.get("executionState",x.get("execution_state","UNSPECIFIED")))) for x in sem["checks"].values()) if isinstance(sem["checks"],dict) else list(selected(x,["id","execution_state","executionState"]) for x in sem["checks"] if isinstance(x,dict)))
    tables={t["table"]:t for t in scope["tables"]}
    table_groups={
        "CREATE":r"family_creation|ablation|creation|genome|r952_candidate$|research_action_v2|r27_(generation|policy|lineage)$|r975_evolution_review$",
        "TRAIN":r"training_|r952_artifact|r952_learning_cursor|r27_atomnet_epoch|r26_training_step|research_rolling_(feature|version|event|head)|research_task_r967",
        "CAPTURE":r"selected_evidence|deferred_evidence|settlement_task|canonical|v57_shadow_observation|analysis_request",
        "QUALIFICATION":r"research_certificate|calibration|r958_frozen_predraw|r967_frozen_evaluation|fixed_window|mainline_contribution",
        "CHART":r"chart_cache|r30r1_(input_head|chart|research_applied)|r27_brier_pair",
    }
    for tag, pattern in table_groups.items():
        rows=[status_table(t) for name,t in tables.items() if re.search(pattern,name)]
        emit(tag, rows)
    emit("R30_CHAPTERS",[selected(x,["sectionId","status","reason","evidenceLevel"]) for x in r30["chapters"]])
    emit("R30_METRICS",[selected(x,["id","metricId","kind","value","status","sampleCount","denominator","missingReason","evidenceLevel"]) for x in r30["metricResults"]])
    emit("R30_TABLES",[selected(x,["id","totalMatchingRows","status","shown","sourceVersionStatus","readOnly"]) for x in r30["researchTables"]])
    idx=r30.get("objectIndex",{})
    if isinstance(idx,dict):
        for k,value in idx.items():
            if re.search(r"chart|research",k,re.I):
                if isinstance(value,dict):
                    emit("R30_INDEX",{"key":k,"summary":selected(value,["totalRows","totalObjects","status","inlineByteBudgetExceeded","displayIsFullDenominator","availableRows","exportedRows","fullOriginal"]),"keys":list(value)})
                elif isinstance(value,list):
                    emit("R30_INDEX",{"key":k,"count":len(value)})
    diag=doc("UPGRADE_DIAGNOSIS.json")
    findings=diag.get("findings",diag.get("diagnosis",{}).get("findings",[]))
    if isinstance(findings,list):
        emit("UPGRADE_FINDINGS",[selected(x,["id","status","priority","severity","action","conclusion"]) for x in findings])
    assess=r26.get("strategyAssessment",{})
    emit("R26_STRATEGY",selected(assess,["totalPolicies","exportedPolicies","omittedPolicies","evolution"]))
    for x in r26["sections"]:
        if x.get("sectionId") in {"TRAINING","PERFORMANCE","RECOVERY","BACKGROUND","CONTINUOUS_RUNTIME","R27_03_CONTINUOUS_RUNTIME","R29_12_ACCEPTANCE_GAPS"}:
            emit("R26_TRACK",selected(x,["sectionId","elapsedMs","availableRows","exportedRows","status","reason"]))
