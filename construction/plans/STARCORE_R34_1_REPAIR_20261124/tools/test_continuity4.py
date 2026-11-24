"""Exercise the controlled-stream continuity checker, including the restart boundary."""
import hashlib
import json
import tempfile
import subprocess
import unittest
from pathlib import Path

from continuity4 import SCENARIO_PATH, STAGES, check_continuity_receipt, referenced_files, verify


def fixture(root, fault=None, source=None, apk_sha=None):
    def save(name, data):
        raw = data if isinstance(data, bytes) else json.dumps(data, sort_keys=True).encode()
        (root / name).write_bytes(raw)
        return {"path":name, "sha256":hashlib.sha256(raw).hexdigest()}

    n, boundary = 8, 3
    identity = dict(scope="ENGINEERING_FIXTURE", run_id="stream-1", source_commit=source or "a"*40,
                    apk_sha256=apk_sha or "b"*64, time_scope="device1/boot1/elapsed-ms")
    slots = []
    for i in range(n):
        stages = STAGES if i % 2 == 0 else ("train", "freeze", "settle", "feedback", "research")
        due = {stage:i for stage in stages}
        if i == boundary:
            due["research"] += 1
        slots.append(dict(ordinal=i+1000, issue="20261124%04d"%(i+101), draw_ms=200*i+60,
                          close_ms=200*(i+1), work_due=due, draw_numbers=list(range(1,11)),
                          input_sha256=hashlib.sha256(("fixture-input-%d"%i).encode()).hexdigest()))
    scenario = save("scenario",dict(kind="CONTINUITY_CONTROLLED_STREAM_V1",start_ms=0,end_ms=n*200,
                    restart_after=boundary,cycle_ends=[1,3,5,7],slots=slots))
    common = dict(**identity, scenario_sha256=scenario["sha256"])
    carry_issue = slots[boundary]["issue"]
    carried = [["research",carry_issue]]
    restart = save("restart",dict(**common, old_process="proc-A",new_process="proc-B",after_ordinal=1003,
                   stopped_ms=801,resumed_ms=808,before_pending=carried,
                   after_pending=[] if fault=="lose_restart" else carried,
                   PROCESS_DIED=dict(kind="PROCESS_DIED",process_identity="proc-A",pid=9,process_start_ms=0,
                                     at_ms=801,record_key="observer/death"),
                   PROCESS_STARTED=dict(kind="PROCESS_STARTED",process_identity="proc-B",pid=10,
                                        process_start_ms=0 if fault=="not_restarted" else 804,
                                        at_ms=808,record_key="observer/start")))
    rows, originals = [], {}
    for i, slot in enumerate(slots):
        issue = slot["issue"]
        owner = "proc-A" if i <= boundary else "proc-B"
        draw = save("draw"+str(i),dict(**identity,issue=issue,ordinal=slot["ordinal"],
                    numbers=list(range(1,11)) if not (fault=="bad_draw" and i==2) else [1]*10))
        stimulus=save("stimulus"+str(i),("fixture-input-%d"%(0 if i==2 and fault=="wrong_input" else i)).encode())
        events = []
        def emit(stage,target,at):
            key = (stage,target)
            value = "same" if stage=="create" and fault=="stale_content" else target
            effect = save("value-"+stage+"-"+target,(stage+"/"+value).encode())
            product = dict(**common,stage=stage,issue=target,process=owner,source_table="fixture-"+stage,
                           durable_key=stage+target,committed_ms=at,effect=effect,
                           opportunity_input_sha256=next(x["input_sha256"] for x in slots if x["issue"]==target))
            if fault=="stale_key" and stage=="train":
                product["durable_key"]="old"
            if stage == "freeze" and i==2 and fault=="late_freeze":
                product["committed_ms"]=slot["draw_ms"]
            if stage == "settle":
                product["parent_ref"]=originals["freeze",target]["sha256"]
                product["draw_ref"]=draw["sha256"] if fault!="wrong_draw" else "f"*64
            if stage == "feedback":
                product["parent_ref"]=originals["settle",target]["sha256"] if fault!="wrong_settle" else "d"*64
            ref=save("product-"+stage+target,product)
            originals[key]=ref
            events.append(dict(stage=stage,issue=target,kind="COMMIT",original=ref,observed_ms=max(at,product["committed_ms"])))
        if i==boundary+1:
            old_target=slots[0]["issue"]
            if fault!="no_replay":
                events.append(dict(stage="train",issue=old_target,kind="REPLAY",original=originals["train",old_target],observed_ms=809))
            emit("research",carry_issue,812)
        rank = 0
        for stage in slot["work_due"]:
            if (i==boundary and stage=="research") or (fault=="wait" and stage=="fusion" and i==4):
                continue
            at=200*i+20+rank if stage not in {"settle","feedback","research","mutation","fusion"} else 200*i+70+rank
            emit(stage,issue,at)
            rank += 1
        row=dict(issue=issue,ordinal=slot["ordinal"],process=owner,activity_open=fault=="UI" and i==3,
                 origin="SERVICE",input_original=stimulus,draw_original=draw,events=events,pending_after=carried if i==boundary else [])
        if i==3 and fault=="wrong_pending":
            row["pending_after"]=[]
        if i==2 and fault=="duplicate":
            events.append(dict(events[-1]))
        rows.append(row)
    if fault=="omit_slot":
        rows.pop(2)
    ledger=save("ledger",dict(**common,started_ms=0,finished_ms=1000 if fault=="short" else 1600,rows=rows,
                lifecycle_listener_from_ms=0,lifecycle_listener_through_ms=1600,
                activity_events=["OPEN"] if fault=="brief_UI" else []))
    return dict(**identity, scenario=scenario,ledger=ledger,restart=restart,scenario_source_path=SCENARIO_PATH)


class Tests(unittest.TestCase):
    def test_positive(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            index=fixture(root)
            self.assertEqual(verify(index,root)["periods"],8)
            declared=referenced_files(index,root)
            self.assertEqual(set(declared),{p.name for p in root.iterdir()})

    def test_faults(self):
        for fault in ("omit_slot","lose_restart","wait","UI","brief_UI","not_restarted","stale_content","late_freeze","wrong_draw","wrong_settle",
                      "wrong_pending","duplicate","stale_key","no_replay","short","bad_draw","wrong_input"):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as d:
                root=Path(d)
                with self.assertRaises(ValueError):
                    verify(fixture(root,fault),root)

    def test_formal_receipt_and_rejections(self):
        with tempfile.TemporaryDirectory() as d:
            base = Path(d)
            root = base / "trace"
            root.mkdir()
            fixture(root)
            repo = base / "repo"
            repo.mkdir()
            def git(*args):
                return subprocess.run(["git","-C",str(repo),*args],check=True,capture_output=True).stdout.strip()
            git("init","-q")
            planned = repo / SCENARIO_PATH
            planned.parent.mkdir(parents=True)
            planned.write_bytes((root/"scenario").read_bytes())
            git("add",SCENARIO_PATH)
            git("-c","user.name=Test","-c","user.email=test@example.invalid","commit","-qm","register fixture")
            source = git("rev-parse","HEAD").decode()
            apk = base / "same.apk"
            apk.write_bytes(b"test fixture for already verified build identity")
            apk_sha = hashlib.sha256(apk.read_bytes()).hexdigest()
            idx = fixture(root,source=source,apk_sha=apk_sha)
            (root/"index").write_text(json.dumps(idx))
            (base/"learning").write_text(json.dumps(dict(source_commit=source,apk_sha256=apk_sha)))
            class Artifacts:
                def json(self, name):
                    return json.loads(self.files[name].read_bytes())
            original = Artifacts()
            original.files = {(Path("trace")/p.name).as_posix():p for p in root.iterdir()}
            original.files.update({"same.apk":apk, "learning":base/"learning"})
            original.hashes = {name:hashlib.sha256(p.read_bytes()).hexdigest() for name,p in original.files.items()}
            run = dict(continuity_index_artifact="trace/index",learning_trace_index_artifact="learning",
                       apk_artifact="same.apk")
            receipt = dict(source_after=source,apk_artifact=None,apk_sha256=None)
            self.assertEqual(check_continuity_receipt(run,receipt,original,repo)["periods"],8)
            with self.assertRaisesRegex(ValueError,"commit"):
                check_continuity_receipt(run,{**receipt,"source_after":"f"*40},original,repo)
            with self.assertRaisesRegex(ValueError,"APK digest"):
                check_continuity_receipt({**run,"apk_sha256":"f"*64},receipt,original,repo)
            (root/"index").write_text(json.dumps({**idx,"scenario_source_path":"elsewhere.json"}))
            with self.assertRaisesRegex(ValueError,"scenario source path"):
                check_continuity_receipt(run,receipt,original,repo)
            (root/"index").write_text(json.dumps({**idx,"scenario":{**idx["scenario"],"sha256":"f"*64}}))
            with self.assertRaisesRegex(ValueError,"scenario bytes"):
                check_continuity_receipt(run,receipt,original,repo)
            (root/"index").write_text(json.dumps(idx))
            del original.hashes["trace/stimulus2"]
            with self.assertRaisesRegex(ValueError,"undeclared continuity original"):
                check_continuity_receipt(run,receipt,original,repo)

    def test_wrong_scope_and_tamper(self):
        with tempfile.TemporaryDirectory() as d:
            root=Path(d)
            idx=fixture(root)
            with self.assertRaises(ValueError):
                verify({**idx,"scope":"NATURAL"},root)
            (root/"restart").write_bytes(b"{}")
            with self.assertRaisesRegex(ValueError,"digest mismatch"):
                verify(idx,root)


if __name__=="__main__":
    unittest.main()
