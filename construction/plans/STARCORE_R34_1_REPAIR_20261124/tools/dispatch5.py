"""Resolve the Android isolation argument and verify the selected test actually ran."""
import argparse
import json
import re
import secrets
import sys
import zipfile
from collections import defaultdict
from pathlib import Path
from xml.etree import ElementTree

RUNNER = "com.ethk3.novaonepro.R30FaultAndroidRunner"
PACKAGE = "com.starcore.research.r13"
SCOPE_KEY = "-Pandroid.testInstrumentationRunnerArguments.r30Sandbox="
REAL_KEY = "-Pandroid.testInstrumentationRunnerArguments.r31qRealService="
SELECT_CLASS = "-Pandroid.testInstrumentationRunnerArguments.class="
MARKER = "<R341_SCOPE>"
ANDROID_MODE = "ANDROID_PRODUCTION_OWNER_AND_SQLITE"


def need(ok, why):
    if not ok:
        raise ValueError(why)


def read(path):
    return json.loads(path.read_bytes())


def prefix(case_id):
    return "r341_" + case_id.replace("-", "_") + "_"


def actual_scope(case_id, value):
    return isinstance(value, str) and bool(re.fullmatch(re.escape(prefix(case_id)) + r"[0-9a-f]{32}", value)) and len(value) <= 80


def value_for(command, gradle_key, adb_name):
    values = [x[len(gradle_key):] for x in command if x.startswith(gradle_key)]
    values += [command[i+2] for i in range(len(command)-2) if command[i:i+2] == ["-e", adb_name]]
    return values


def write_scope(template, case_id, scope):
    need(actual_scope(case_id, scope), "Android case needs a fresh case-scoped nonce")
    need(value_for(template, SCOPE_KEY, "r30Sandbox") == [MARKER], "Runner mapping lacks the unique sealed scope slot")
    return [word.replace(MARKER, scope) if word in (MARKER,SCOPE_KEY + MARKER) else word for word in template]


def checked_android_command(case, run, originals, template):
    """Supply the resolved template to the existing exact check_command verifier."""
    contract = case["test_contract"]
    if not contract.get("android_scope_contract"):
        return template
    cid = case["id"]
    argv = run["command"]
    scopes = value_for(argv, SCOPE_KEY, "r30Sandbox")
    need(len(scopes) == 1 and actual_scope(cid, scopes[0]), "Missing, foreign or invalid Android case sandbox")
    scope = scopes[0]
    resolved = write_scope(template, cid, scope)
    selectors = value_for(argv, SELECT_CLASS, "class")
    need(len(selectors)==1 and selectors[0].count("#")==1, "Android case selector must select one method")
    cls, method = selectors[0].split("#")
    need(method == run["runner_method"] and cls, "Android selected method differs from checked runner")
    configs = value_for(argv, REAL_KEY, "r31qRealService")
    need(configs in ([],["false"],["true"]), "Conflicting real-service test routing")
    shared = configs == ["true"]
    if shared:
        need(run.get("equivalent_mapping_path"), "Shared application root needs an explicit committed case mapping")
    observed = originals.json(run["instrumentation_artifact"])
    expected = dict(case_id=cid, source_commit=run["source_after"], variant="debug", package=PACKAGE,
                    runner=RUNNER, selected_class=cls, selected_method=method, r30Sandbox=scope,
                    r31qRealService=shared)
    need(all(k in observed and type(observed[k]) is type(v) and observed[k] == v for k,v in expected.items()),
         "Android runner invocation, target, scope or test identity differs")
    need(isinstance(observed.get("device_serial"),str) and observed["device_serial"].strip(),
         "Android test result lacks its device identity")
    root = observed["target_data_dir"]
    need(isinstance(root,str) and root.startswith("/") and ".." not in Path(root).parts, "Invalid observed data directory")
    if shared:
        need(root.endswith("/" + PACKAGE) and "/r30-instrumentation/" not in root and
             observed.get("disposable_emulator") is True and observed.get("qemu_property") == "1",
             "Shared cross-process fixture lacks a disposable emulator and actual common application root")
    else:
        need(root.endswith("/r30-instrumentation/" + scope + "/" + PACKAGE),
             "Runner selected but target data root does not belong to this invocation")
    logname = observed["device_log_artifact"]
    need(logname in originals.files and originals.hashes[logname] == observed["device_log_sha256"] and
         logname != run["instrumentation_artifact"], "Missing original device invocation log")
    events = []
    entry = None
    with originals.files[logname].open(encoding="utf-8", errors="replace") as stream:
        for line in stream:
            if "R341_INSTRUMENTATION " not in line:
                continue
            event = json.loads(line.split("R341_INSTRUMENTATION ",1)[1])
            if event.get("r30Sandbox") == scope:
                need(all(k in event and type(event[k]) is type(observed[k]) and event[k] == observed[k] for k in
                         ("case_id", "source_commit", "runner", "selected_class", "selected_method",
                          "r30Sandbox", "r31qRealService", "target_data_dir", "package")),
                     "Target process runtime observation differs from requested scope")
                events.append(event["event"])
                if event["event"] == "CASE_ENTER":
                    entry = event
    need(events == ["CASE_ENTER","CASE_ASSERTIONS_COMPLETE"], "Missing, duplicated or stale Android test observation")
    artifact = observed["junit_xml_artifact"]
    need(artifact in originals.files and originals.hashes[artifact] == observed["junit_xml_sha256"] and
         artifact != run["instrumentation_artifact"], "Missing linked per-device native JUnit result")
    try:
        root_xml = ElementTree.parse(originals.files[artifact]).getroot()
    except (ElementTree.ParseError, OSError) as exc:
        raise ValueError("Invalid Android XML test result") from exc
    tests = list(root_xml.iter("testcase"))
    need(root_xml.tag in ("testsuite","testsuites") and len(tests) == 1 and tests[0].get("classname") == cls and tests[0].get("name") == method and
         all(root_xml.find(".//"+tag) is None for tag in ("skipped","failure","error")) and
         all("tests" not in node.attrib or node.get("tests") == str(len(list(node.iter("testcase"))))
             for node in root_xml.iter() if node.tag in ("testsuite","testsuites")) and
         all(node.get(key,"0") == "0" for node in root_xml.iter() if node.tag in ("testsuite","testsuites")
             for key in ("errors","failures","skipped")),
         "Android result lacks a unique successful invocation of the selected method")
    if contract.get("external_process_transition"):
        captured, last = process_transition(case, run, originals, scope)
        need(captured["device_serial"] == observed["device_serial"] and entry["boot_id"] == captured["boot_id"] and
             entry["installed_apk_sha256"] == captured["installed_apk_sha256"] and
             type(entry["elapsed_ms"]) is int and last <= entry["elapsed_ms"],
             "JUnit verification must follow the original host campaign on the same device, boot and APK")
    return resolved


def process_transition(case, run, originals, scope):
    """Validate the host observations captured outside the main app that was killed."""
    name = run["process_transition_artifact"]
    index = originals.json(name)
    need(index["schema"] == "R341_HOST_RESTART_1" and index["case_id"] == case["id"] and
         index["source_commit"] == run["source_after"] and index["r30Sandbox"] == scope and
         index["package"] == PACKAGE and index["qemu_property"] == "1" and index["disposable_emulator"] is True,
         "Wrong host restart fixture or source")
    apk = index["installed_apk_artifact"]
    need(apk in originals.files and originals.hashes[apk] == index["installed_apk_sha256"],
         "Missing exact APK used during process observations")
    member = index["apk_identity_member"]
    need(isinstance(member,str) and member.startswith("assets/") and ".." not in Path(member).parts,
         "Invalid process-campaign APK identity member")
    try:
        with zipfile.ZipFile(originals.files[apk]) as archive:
            names = archive.namelist()
            need("AndroidManifest.xml" in names and "classes.dex" in names and names.count(member) == 1,
                 "Campaign APK lacks unique program identity")
            identity = json.loads(archive.read(member))
    except zipfile.BadZipFile as exc:
        raise ValueError("Invalid campaign APK") from exc
    need(identity["sourceCommit"] == run["source_after"] and identity["planSha256"] == run["plan_manifest_sha256"],
         "Process campaign APK belongs to another source or plan")
    wanted = ("WAITING_COMMITTED","TARGET_EXITED","SERVICE_RESTARTED","WORK_COMMITTED")
    refs = index["event_artifacts"]
    need(isinstance(refs,list) and len(refs) == len(wanted) and
         len({originals.hashes[f] for f in [name,apk,*refs]}) == len(refs)+2, "Repeated process observation")
    events = [originals.json(f) for f in refs]
    keys = ("case_id","package","source_commit","r30Sandbox","device_serial","boot_id","observer_session",
            "db_identity","work_key","installed_apk_sha256")
    need(all(isinstance(index.get(k),str) and index[k] for k in keys) and
         all(e.get(k) == index[k] for e in events for k in keys), "Mixed host or work identity")
    seen=[]
    for e,kind in zip(events,wanted):
        need(e["kind"] == kind and type(e["elapsed_ms"]) is int and e["elapsed_ms"] >= 0 and
             e["activity_starts_since_start"] == 0 and e["observer"] == "EXTERNAL_HOST",
             "Host restart phases require service-only source observations")
        seen.append(e["elapsed_ms"])
    need(all(a<b for a,b in zip(seen,seen[1:])), "Restart measurement order differs")
    a,dead,b,after = events
    def process(e):
        need(e["process_name"] == PACKAGE and type(e["pid"]) is int and e["pid"] > 0 and
             type(e["start_ticks"]) is int and e["start_ticks"] > 0, "Missing actual main-process identity")
        return e["pid"], e["start_ticks"]
    old, new = process(a), process(b)
    need(process(after) == new and old != new and old[1] < new[1] and
         dead["retired_pid"] == old[0] and dead["retired_start_ticks"] == old[1] and
         dead["old_process_present"] is False and b["launcher"] in ("SERVICE","OS_SERVICE_RESTART","JOB","ALARM"),
         "Main process neither actually left nor resumed through service")
    need(a["committed_work_count"] == 0 and type(after["committed_work_count"]) is int and
         after["committed_work_count"] == 1 and after["commit_origin"] == "PRODUCTION_SERVICE" and
         after["pending_count_for_key"] == 0, "Recovered work has no unique committed result")
    need(isinstance(after["business_result_artifact"],str) and after["business_result_artifact"] in originals.files and
         after["business_result_sha256"] == originals.hashes[after["business_result_artifact"]] and
         after["business_result_artifact"] not in [name,apk,*refs], "Missing recovered business original")
    raw = originals.json(after["business_result_artifact"])
    need(raw["work_key"] == index["work_key"] and raw["source_commit"] == index["source_commit"] and
         raw["db_identity"] == index["db_identity"] and raw["service_pid"] == new[0] and
         raw["service_start_ticks"] == new[1] and raw["commit_count"] == 1,
         "Wrong service or business original after restart")
    return index, seen[-1]


def check_construction_order(plan):
    """Check declared physical edits and command inputs in issue order without treating old traceability as runtime."""
    work = read(plan/"WORK_ORDERS.json")
    cards = {x["production_path"]:x for x in read(plan/"FILE_CARDS.json")["files"]}
    cases = read(plan/"ACCEPTANCE_MATRIX.json")["cases"]
    declared = defaultdict(list)
    positions = {}
    for b in work["batches"]:
        for i in b["issues"]:
            positions[i["id"]] = len(positions)
            for f in i["files"]:
                declared[f["path"]].append((i["id"],b["id"],f["action"]))
    available = {}
    new_count = 0
    for path, card in cards.items():
        uses = declared[path]
        need(bool(uses), "Missing file schedule " + path)
        history = [(r["issue_id"], r["batch"], r["action"]) for r in card["changes_in_order"]]
        need(history == uses, "Physical issue order differs from file card: "+path)
        if card["before_state"] == "NEW_REQUIRED":
            new_count += 1
            need(card["primary_owner_batch"] == uses[0][1] and uses[0][2] == "NEW" and
                 all(a in {"MODIFY","VERIFY_ONLY"} for _,_,a in uses[1:]), "Missing or repeated physical creation: "+path)
            available[path] = positions[uses[0][0]]
        else:
            need(all(a != "NEW" for _,_,a in uses), "Cannot recreate a baseline or superseded file: "+path)
    android_count = 0
    for c in cases:
        if c["level"] != "ENGINEERING":
            continue
        contract = c["test_contract"]
        links = [contract["path"]] + [r["effective_path"] for r in c.get("inherited_path_probes", [])]
        need(all(available.get(f,-1) <= positions[c["issue_id"]] for f in links),
             "Case references a file introduced by a later issue: "+c["id"])
        if contract["harness"] == ANDROID_MODE:
            android_count+=1
            cmd = contract["command_argv"]
            need(contract.get("android_scope_contract") == 1 and "-Pr30Instrumentation=true" in cmd and
                 ":app:connectedDebugAndroidTest" in cmd and value_for(cmd, SCOPE_KEY,"r30Sandbox") == [MARKER] and
                 len(value_for(cmd, SELECT_CLASS,"class"))==1,
                 "Android plan command cannot start the designated runner: "+c["id"])
    return {"single_creation_files":new_count,"android_scoped_cases":android_count}


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--plan", type=Path, default=Path(__file__).resolve().parents[1])
    p.add_argument("--case", help="Print one runnable argv as a JSON array with a fresh sandbox token.")
    args = p.parse_args()
    if args.case:
        case = next((c for c in read(args.plan/"ACCEPTANCE_MATRIX.json")["cases"] if c["id"] == args.case), None)
        need(case is not None and case["test_contract"].get("android_scope_contract") == 1,
             "Expected a scoped Android engineering case")
        token = prefix(args.case) + secrets.token_hex(16)
        print(json.dumps(write_scope(case["test_contract"]["command_argv"],args.case,token)))
    else:
        print(json.dumps(check_construction_order(args.plan)))


if __name__ == "__main__":
    try:
        main()
    except (KeyError, ValueError, OSError) as e:
        sys.exit("FAIL_DISPATCH: " + str(e))
