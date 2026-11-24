"""Host regression probes of Android command/receipt and physical file ordering."""
import copy
import hashlib
import json
import tempfile
import unittest
from pathlib import Path
from dispatch5 import (ANDROID_MODE,MARKER,PACKAGE,RUNNER,SCOPE_KEY,SELECT_CLASS,actual_scope,
                       checked_android_command,check_construction_order,prefix,write_scope)


class Originals:
    def __init__(self, directory, records):
        self.files, self.hashes = {}, {}
        for key, payload in records.items():
            raw = payload.encode() if isinstance(payload,str) else json.dumps(payload).encode()
            self.files[key] = directory / key
            self.files[key].write_bytes(raw)
            self.hashes[key] = hashlib.sha256(raw).hexdigest()

    def json(self, name):
        return json.loads(self.files[name].read_bytes())


def sample(version="",case="R341-B01-02-T01"):
    cls="com.ethk3.novaonepro.r341.R341B01ContractTest"
    method="test_r341_b01_02_t01"
    template=["./gradlew",":app:connectedDebugAndroidTest","-Pr30Instrumentation=true",
              SELECT_CLASS+cls+"#"+method,SCOPE_KEY+MARKER]
    scope=prefix(case)+"b"*32
    command=write_scope(template,case,scope)
    run={"command":command,"source_after":"b"*40,"runner_method":method,"instrumentation_artifact":"invoke"}
    expected=dict(case_id=case,package=PACKAGE,source_commit=run["source_after"],runner=RUNNER,selected_class=cls,
                  selected_method=method,r30Sandbox=scope,r31qRealService=False,
                  target_data_dir="/data/no_backup/r30-instrumentation/"+scope+"/"+PACKAGE)
    observed=dict(expected,variant="debug",device_serial="emulator-5554",junit_xml_artifact="suite.xml",device_log_artifact="events")
    node='<testcase classname="'+cls+'" name="'+method+'"/>'
    if version=="skipped":node=node.replace("/>"," ><skipped/></testcase>")
    if version=="zero-tests":node=""
    if version=="wrong-method":node=node.replace(method,"other_method")
    if version=="two-methods":node=node+node
    modes=["CASE_ENTER","CASE_ASSERTIONS_COMPLETE"]
    if version=="never-finished":modes=modes[:1]
    if version=="duplicate-start":modes.insert(0,modes[0])
    if version=="wrong-root":observed["target_data_dir"]="/data/user/0/"+PACKAGE
    if version=="release":observed["variant"]="release"
    if version=="foreign-scope":expected["r30Sandbox"]="wrong"
    if version=="literal-slot":command[-1]=SCOPE_KEY+MARKER
    if version=="no-scope":command.pop()
    if version=="duplicate-scope":command.append(command[-1])
    if version=="unisolated":command.append("-Pandroid.testInstrumentationRunnerArguments.r31qRealService=true")
    if version=="stale-log":observed["source_commit"]="c"*40
    expected_log='\n'.join('R341_INSTRUMENTATION '+json.dumps(dict(expected,event=e)) for e in modes)
    data={"suite.xml":"<testsuite>"+node+"</testsuite>","events":expected_log}
    if version=="fake-xml-root":data["suite.xml"]=data["suite.xml"].replace("testsuite","summary")
    if version=="zero-aggregate":data["suite.xml"]=data["suite.xml"].replace("<testsuite>",'<testsuite tests="0">')
    for k in ["device_log","junit_xml"]:
        observed[k+"_sha256"]=hashlib.sha256(data[observed[k+"_artifact"]].encode()).hexdigest()
    return {"id":case,"test_contract":{"android_scope_contract":1}},run,{"invoke":observed,**data},template


class CommandContracts(unittest.TestCase):
    def test_real_argv_template_can_be_resolved_before_exact_check(self):
        case,run,records,template=sample()
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(checked_android_command(case,run,Originals(Path(tmp),records),template),run["command"])
        self.assertFalse(actual_scope(case["id"],prefix("R341-B02-01-T01")+"b"*32))
        self.assertEqual(checked_android_command({"test_contract":{}},{},None,["old"]),["old"])
        with self.assertRaises(ValueError):
            write_scope(template[:-1],case["id"],prefix(case["id"])+"b"*32)

    def test_wrong_routing_unrun_case_and_reused_native_results_are_rejected(self):
        with tempfile.TemporaryDirectory() as tmp:
            for error in ("skipped","zero-tests","wrong-method","two-methods","never-finished",
                          "duplicate-start","wrong-root","release","foreign-scope","literal-slot",
                          "no-scope","duplicate-scope","unisolated","stale-log","fake-xml-root","zero-aggregate"):
                with self.subTest(error=error):
                    case,run,records,template=sample(error)
                    with self.assertRaises(ValueError):
                        checked_android_command(case,run,Originals(Path(tmp),records),template)

    def test_shared_application_data_requires_committed_mapping_and_disposable_emulator(self):
        c,r,files,template=sample()
        r["equivalent_mapping_path"]="tools/test-map.json"
        flag="-Pandroid.testInstrumentationRunnerArguments.r31qRealService=true"
        template.append(flag);r["command"].append(flag)
        old=files["invoke"]; base="/data/user/0/"+PACKAGE
        events=[json.loads(line.split(" ",1)[1]) for line in files["events"].splitlines()]
        for e in events:e.update(r31qRealService=True,target_data_dir=base)
        files["events"]='\n'.join("R341_INSTRUMENTATION "+json.dumps(e) for e in events)
        old.update(r31qRealService=True,target_data_dir=base,disposable_emulator=True,qemu_property="1",
                   device_log_sha256=hashlib.sha256(files["events"].encode()).hexdigest())
        with tempfile.TemporaryDirectory() as tmp:
            self.assertEqual(checked_android_command(c,r,Originals(Path(tmp),files),template),r["command"])
            old["qemu_property"]="0"
            with self.assertRaises(ValueError):
                checked_android_command(c,r,Originals(Path(tmp),files),template)

    def test_external_observer_survives_main_process_death(self):
        import io, zipfile
        from dispatch5 import process_transition
        cid="R341-B05-01-T02"; case,run,native,template=sample(case=cid); sc=prefix(cid)+"b"*32
        case["test_contract"]["external_process_transition"]=True
        ph="a"*64; run.update(process_transition_artifact="campaign",plan_manifest_sha256=ph)
        b=io.BytesIO()
        with zipfile.ZipFile(b,"w") as z:
            for path,data in [("AndroidManifest.xml",b"mf"),("classes.dex",b"dex"),
                              ("assets/build.json",json.dumps(dict(sourceCommit=run["source_after"],planSha256=ph)))]:
                z.writestr(path,data)
        apk=b.getvalue();digest=hashlib.sha256(apk).hexdigest()
        ident=dict(case_id=cid,package=PACKAGE,source_commit=run["source_after"],r30Sandbox=sc,device_serial="emulator-5554",
                   observer_session="external",boot_id="boot",db_identity="run-db",work_key="row-key",installed_apk_sha256=digest)
        names=["wait","gone","restart","commit"]
        data={"campaign":dict(ident,schema="R341_HOST_RESTART_1",installed_apk_artifact="app.apk",
                              apk_identity_member="assets/build.json",qemu_property="1",disposable_emulator=True,event_artifacts=names)}
        kinds=("WAITING_COMMITTED","TARGET_EXITED","SERVICE_RESTARTED","WORK_COMMITTED")
        for i,(name,kind) in enumerate(zip(names,kinds)):
            data[name]=dict(ident,kind=kind,elapsed_ms=100+i,activity_starts_since_start=0,observer="EXTERNAL_HOST",process_name=PACKAGE)
        data["wait"].update(pid=91,start_ticks=55,committed_work_count=0)
        data["gone"].update(retired_pid=91,retired_start_ticks=55,old_process_present=False)
        data["restart"].update(pid=91,start_ticks=66,launcher="SERVICE")
        raw=dict(work_key="row-key",source_commit=run["source_after"],db_identity="run-db",service_pid=91,service_start_ticks=66,commit_count=1)
        data["row"]=raw
        data["commit"].update(pid=91,start_ticks=66,pending_count_for_key=0,committed_work_count=1,commit_origin="PRODUCTION_SERVICE",
                              business_result_artifact="row",business_result_sha256=hashlib.sha256(json.dumps(raw).encode()).hexdigest())
        with tempfile.TemporaryDirectory() as tmp:
            def original(records):
                orig=Originals(Path(tmp),records);f=Path(tmp)/"app.apk";f.write_bytes(apk)
                orig.files["app.apk"]=f;orig.hashes["app.apk"]=digest
                return orig
            idx,end=process_transition(case,run,original(data),sc)
            self.assertEqual(end,103)
            def joined(entered):
                raw = copy.deepcopy(native)
                events=[json.loads(s.split(" ",1)[1]) for s in raw["events"].splitlines()]
                events[0].update(boot_id="boot",installed_apk_sha256=digest,elapsed_ms=entered)
                raw["events"]='\n'.join("R341_INSTRUMENTATION "+json.dumps(e) for e in events)
                raw["invoke"]["device_log_sha256"]=hashlib.sha256(raw["events"].encode()).hexdigest()
                return original({**data,**raw})
            self.assertEqual(checked_android_command(case,run,joined(104),template), run["command"])
            with self.assertRaises(ValueError):checked_android_command(case,run,joined(99),template)
            for n,k,v in [("restart","start_ticks",55),("gone","old_process_present",True),("commit","db_identity","new-db"),
                          ("wait","activity_starts_since_start",1),("commit","elapsed_ms",99),("restart","launcher","ACTIVITY"),
                          ("commit","committed_work_count",2),("row","work_key","not-same"),("campaign","qemu_property","0")]:
                wrong=copy.deepcopy(data);wrong[n][k]=v
                with self.subTest(n=n,k=k),self.assertRaises(ValueError):process_transition(case,run,original(wrong),sc)

    def test_creation_order_detects_recreation_and_future_suite(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);issues=["R341-B01-01","R341-B02-01"]
            def setup(first_case,actions):
                batches=[];changes=[]
                for idx,(issue,action) in enumerate(zip(issues,actions)):
                    b="B0"+str(idx+1)
                    batches.append({"id":b,"issues":[{"id":issue,"files":[{"path":"NewRunner.java","action":action}] if action else []}]})
                    if action:changes.append({"issue_id":issue,"batch":b,"action":action})
                for n,o in [("WORK_ORDERS",{"batches":batches}),("FILE_CARDS",{"files":[{"production_path":"NewRunner.java",
                           "primary_owner_batch":changes[0]["batch"],"before_state":"NEW_REQUIRED","changes_in_order":changes}]}),
                           ("ACCEPTANCE_MATRIX",{"cases":[{"issue_id":issues[first_case],"id":"test","level":"ENGINEERING",
                            "test_contract":{"harness":"HOST","path":"NewRunner.java"}}]})]:
                    (p/(n+".json")).write_text(json.dumps(o))
            setup(1,["NEW","MODIFY"]);self.assertEqual(check_construction_order(p)["single_creation_files"],1)
            setup(0,["NEW","NEW"])
            with self.assertRaises(ValueError):check_construction_order(p)
            setup(0,[None,"NEW"])
            with self.assertRaises(ValueError):check_construction_order(p)


if __name__ == "__main__":
    unittest.main()
