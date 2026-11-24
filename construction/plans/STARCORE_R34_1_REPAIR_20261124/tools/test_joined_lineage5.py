"""Offline controlled-run join probes. These use the existing v3 and v4 checkers as prerequisites."""
import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from continuity4 import IDENTITY, verify as verify_continuity
from test_continuity4 import fixture as stream_fixture
from test_learning_trace import fixture as trace_fixture
from verify_learning_trace import verify as verify_lineage
from verify_receipt import Originals
from joined_lineage5 import FIELDS, check_joined_lineage


def sha(b):
    return hashlib.sha256(b).hexdigest()


def dump(value):
    return json.dumps(value, ensure_ascii=False, sort_keys=True, allow_nan=False, separators=(",", ":")).encode()


def put(base, name, value):
    raw = value if isinstance(value, bytes) else dump(value)
    (base/name).write_bytes(raw)
    return {"path": name, "sha256":sha(raw)}


def get(base, ref):
    return json.loads((base/ref["path"]).read_bytes())


def make(base, fault=None):
    s, l = base/"stream", base/"learning"
    s.mkdir()
    l.mkdir()
    ci, li = stream_fixture(s), trace_fixture(l)
    if fault != "old_separate_runs":
        identity = {k:ci[k] for k in IDENTITY}
        spec, ledger, restart = (get(s,ci[k]) for k in ("scenario","ledger","restart"))
        planned = [1000,1002,1004]
        spec["lineage_target_ordinals"] = planned if fault != "changed_targets" else [1001,1002,1004]
        ci["scenario"] = put(s,"scenario",spec)
        for doc in (ledger,restart):
            doc["scenario_sha256"]=ci["scenario"]["sha256"]
        li.update(identity)
        ci["case_id"] = li["case_id"] = ledger["case_id"] = "A-B12-03b"
        if fault=="foreign_case":li["case_id"]="OTHER"
        li["database_instance"] = "another" if fault=="foreign_db" else "db1"
        ledger["database_instance"]="db1"
        stream_rows = {}
        for i,row in enumerate(ledger["rows"]):
            for event in row["events"]:
                if event["kind"]=="COMMIT":
                    doc=get(s,event["original"])
                    doc["scenario_sha256"]=ci["scenario"]["sha256"]
                    stream_rows[(i,event["stage"],event["issue"])]=doc
        trace_docs, old_sha, source_by_sha = {},{},{}
        for f in l.iterdir():
            if f.name.startswith("weights"):
                continue
            doc = json.loads(f.read_bytes())
            old_sha[f.name]=sha(f.read_bytes())
            source_by_sha[old_sha[f.name]]=f.name
            doc.update(identity)
            trace_docs[f.name]=doc
        pairs=[]
        for i,target in enumerate(planned):
            slot=spec["slots"][target-1000]
            ordinal=target-1000
            issue=slot["issue"]
            for stage in ["train","publish","freeze","settle","feedback","research"]+(
                    ["mutation"] if i==0 else ["fusion"] if i==1 else []):
                key=stage+str(i) if stage not in ("mutation","fusion") else "birth"+str(i)
                data=trace_docs[key]
                obs=stream_rows[ordinal,stage,issue]
                if stage in FIELDS:
                    obs=dict(obs,source_table="candidate_"+stage,durable_key="candidate:"+issue+stage)
                    ledger["rows"][ordinal].setdefault("lineage_candidate_rows",[])
                obs.update(database_instance="db1",target_ordinal=target,policy_hash=data.get("policy_hash",data.get("child_hash")))
                data["service_origin"]={k:obs[k] for k in ("database_instance","source_table","durable_key","process")}
                data["service_origin"]["work_issue"]=issue
                if stage=="train":
                    data["started_at"]=200*ordinal+15
                    data["committed_at"]=obs["committed_ms"]
                    data["trained_through"]=target-1
                if stage=="publish":data["published_at"]=obs["committed_ms"]
                if stage=="freeze":
                    data.update(target_ordinal=target,returned_at=obs["committed_ms"],deadline_at=slot["draw_ms"]-1)
                if stage=="settle":
                    data.update(target_ordinal=target,draw_at=slot["draw_ms"],settled_at=obs["committed_ms"])
                if stage=="feedback":data.update(target_ordinal=target,committed_at=obs["committed_ms"])
                if stage=="research":data.update(target_ordinal=target,completed_at=obs["committed_ms"],artifact_hash=obs["effect"]["sha256"])
                if stage in ("mutation","fusion"):data["committed_at"]=obs["committed_ms"]
                pairs.append((ordinal,i,stage,key,data,obs))
        outputs={}
        def replace(v):
            if isinstance(v,str) and v in source_by_sha:return rebuild(source_by_sha[v])["sha256"]
            if isinstance(v,dict):return {k:replace(x) for k,x in v.items()}
            if isinstance(v,list):return [replace(x) for x in v]
            return v
        def rebuild(name):
            if name not in outputs:outputs[name]=put(l,name,replace(trace_docs[name]))
            return outputs[name]
        for name in trace_docs:rebuild(name)
        li=replace(li)
        for ordinal,i,stage,key,data,obs in pairs:
            data=get(l,outputs[key])
            obs["learning_original_sha256"]=outputs[key]["sha256"]
            if stage in ("train","publish"):
                raw=(l/f"weights{i}").read_bytes()
                if fault=="other_weights" and i==1 and stage=="train":raw=b"other"
            elif stage in FIELDS:
                raw=dump({k:data[k] for k in FIELDS[stage]})
            elif stage=="research":raw=(s/obs["effect"]["path"]).read_bytes()
            else:raw=dump(get(l,data["child_original"])["program"])
            obs["effect"]=put(s,key+".payload",raw)
            if stage in FIELDS:
                if fault=="alias_basic_shadow" and i==0 and stage=="freeze":
                    basic=stream_rows[ordinal,stage,ledger["rows"][ordinal]["issue"]]
                    obs["source_table"],obs["durable_key"]=basic["source_table"],basic["durable_key"]
                if fault=="cross_issue" and i==0 and stage=="feedback":obs["issue"]=spec["slots"][1]["issue"]
                if fault=="wrong_process" and i==2 and stage=="settle":obs["process"]="proc-A"
                if fault=="early" and i==1 and stage=="freeze":obs["committed_ms"]=59
                if fault=="wrong_key" and i==2 and stage=="feedback":obs["durable_key"]="other"
                if fault=="no_candidate_settle" and i==2 and stage=="settle":continue
                ledger["rows"][ordinal]["lineage_candidate_rows"].append(put(s,key+".candidate",obs))
        refs={}
        for i,row in enumerate(ledger["rows"]):
            for ev in row["events"]:
                k=(ev["stage"],ev["issue"])
                if ev["kind"]=="REPLAY":ev["original"]=refs[k];continue
                doc=stream_rows[i,*k]
                if ev["stage"] in ("feedback","settle"):
                    parent="settle" if ev["stage"]=="feedback" else "freeze"
                    doc["parent_ref"]=refs[parent,ev["issue"]]["sha256"]
                refs[k]=ev["original"]=put(s,ev["original"]["path"],doc)
        ci["ledger"]=put(s,"ledger",ledger)
        ci["restart"]=put(s,"restart",restart)
    put(s,"index",ci);put(l,"index",li)
    entries=[{"path":f.relative_to(base).as_posix(),"sha256":sha(f.read_bytes())} for f in base.rglob("*") if f.is_file()]
    orig=Originals(base,entries)
    run={"id":"A-B12-03b","learning_trace_index_artifact":"learning/index","continuity_index_artifact":"stream/index"}
    return ci, li, run, {"source_after":ci["source_commit"]}, orig


class Joined(unittest.TestCase):
    def test_previous_checks_pass_two_unrelated_fixtures(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);ci,li,run,rec,orig=make(p,"old_separate_runs")
            self.assertEqual(ci["source_commit"],li["source_commit"])
            self.assertEqual(ci["apk_sha256"],li["apk_sha256"])
            self.assertEqual(verify_continuity(ci,p/"stream")["periods"],8)
            self.assertEqual(verify_lineage(li,p/"learning")["cycles"],3)
            with self.assertRaisesRegex(ValueError,"same service run"):check_joined_lineage(run,rec,orig)

    def test_three_generations_actually_on_service(self):
        with tempfile.TemporaryDirectory() as tmp:
            p=Path(tmp);ci,li,run,rec,orig=make(p)
            self.assertEqual(verify_continuity(ci,p/"stream")["periods"],8)
            self.assertEqual(verify_lineage(li,p/"learning")["cycles"],3)
            self.assertEqual(check_joined_lineage(run,rec,orig)["linked_commits"],20)

    def test_join_faults_that_old_checkers_cannot_detect(self):
        for fault in ("foreign_case","changed_targets","foreign_db","other_weights","wrong_key","early","wrong_process",
                      "cross_issue","no_candidate_settle","alias_basic_shadow"):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as tmp:
                p=Path(tmp);ci,li,run,rec,orig=make(p,fault)
                verify_continuity(ci,p/"stream")
                verify_lineage(li,p/"learning")
                with self.assertRaises(ValueError):check_joined_lineage(run,rec,orig)


if __name__=="__main__":
    unittest.main()
