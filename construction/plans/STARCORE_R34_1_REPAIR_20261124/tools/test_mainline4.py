"""Synthetic probes for the receipt checker, separate from the Android run."""
import hashlib
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path

from mainline4 import SCENARIOS, SOURCES, TIMING_RUNNER, check_mainline_receipt
from verify_receipt import Originals


def digest(tag):
    return hashlib.sha256(str(tag).encode()).hexdigest()


class MainlineTest(unittest.TestCase):
    def fixture(self, folder, fault=None):
        tmp = Path(folder)
        refs = {}
        def put(path, data):
            f = tmp/path
            f.parent.mkdir(exist_ok=True,parents=True)
            f.write_text(json.dumps(data,allow_nan=False,sort_keys=True))
            return register(path)
        def register(name):
            value = {"path":name,"sha256":hashlib.sha256((tmp/name).read_bytes()).hexdigest()}
            refs[name]=value
            return value
        plan = digest("plan")
        repo = tmp/"repo"
        runner = repo/TIMING_RUNNER
        runner.parent.mkdir(parents=True)
        rule = {"unit":"ms", "reserve_ms":20, "first_read_before_cutoff_ms":76}
        runner.write_text("// R341_MAINLINE_TIMING_V1 "+json.dumps(rule)+"\n")
        def git(*argv):
            return subprocess.run(["git","-C",str(repo),"-c","user.name=fixture","-c","user.email=fixture@example.invalid",
                                   *argv],capture_output=True,check=True,text=True).stdout.strip()
        git("init","-q");git("add",TIMING_RUNNER);git("commit","-qm","pre-register controlled interleavings")
        src = git("rev-parse","HEAD")
        member="assets/build.json"
        with zipfile.ZipFile(tmp/"fixture.apk","w") as z:
            for name,content in [("AndroidManifest.xml","fixture"),("classes.dex","fixture"),(member,json.dumps({"sourceCommit":src,"planSha256":plan}))]:
                z.writestr(name,content)
        apk=register("fixture.apk")
        scope=dict(source_commit=src,apk_sha256=apk["sha256"],scope="ENGINEERING_FIXTURE",run_id="fixture",time_scope="test-boot-clock")
        rule_for_run = dict(rule)
        if fault=="changed_source_budget": rule_for_run["reserve_ms"]=19
        timing=put("timing",dict(scope,runner=TIMING_RUNNER,rule=rule_for_run,registered_at=0))
        index=dict(scope,cases={},timing_registration=timing)
        for scenario in SCENARIOS:
            nxt=scenario=="NEXT_ELIGIBLE"
            late=scenario=="LATE_TERMINAL"
            db = "PROOF_FIRST" if nxt else scenario
            branch=f"{scenario}/"
            target=101+int(nxt)
            offset=200*int(nxt)
            action=[2,1,2,3]
            policy=digest("policy")
            target_input=put(f"input-{target}.json",dict(user_switch=True,action=action,policy=policy,target=target,deadline=100+offset))
            case={"input":target_input,"db_lineage":db};index["cases"][scenario]=case
            fields=dict(scope,target=target,db_lineage=db,input_sha256=target_input["sha256"],scenario=scenario)
            cert_ref = put(branch+"gate-bytes",dict(authority="R27_GATE_V1",failing=[],gateEvidenceR31={"policyHash":policy},
                                registeredFutureR34={"coveragePass":True,"expectedPayoffPositive":True}))
            cert=cert_ref["sha256"]
            base=dict(model=digest("model"),proof=digest("proof"+str(target)),policy=policy,certificate=cert,action=action)
            choice=digest(f"choice{scenario}");auth=digest(f"auth{target}");trade_id=target
            common=dict(trade_id=trade_id,target=target,authorization=auth)
            rec=dict(common,action_hash=digest("action"+str(target)),candidate="R31-R27-"+policy,certificate=cert,model=base["model"],proof=base["proof"])
            q=dict(policy=policy,status="ACTIVE",gate_version="R27_GATE_V1",gate_bytes=cert_ref,certificate=cert,issued_at=1,expires_at=900)
            a=dict(base,act=True,selected_hit_p=.37,calibration_hash=digest("calibration"),frozen_at=offset+8,deadline=offset+100)
            c=dict(policy=policy,certificate=cert,bundle=digest("bundle"),row_hash=choice,consumer="" if late else "INVENTED_ALGORITHM",candidate="" if late else rec["candidate"],committed_at=offset+30)
            d=dict(base,decision_id="D"+str(target),authorization=auth,action_hash=rec["action_hash"],selected_hit_p=.37,choice_row=choice,action_type="HOLD" if late else "BET",lane="CONTROLLED_PROBE",authorized_at=offset+32)
            le=dict(committed_at=offset+38,trade_rows=[] if late else [dict(common,role="REAL_TRADE",action=action,decision_id=d["decision_id"])],
                    exact_rows=[] if late else [rec.copy()],probability_rows=[] if late else [dict(rec,selected_hit_p=.37,calibration_hash=digest("calibration"))])
            se=dict(draw=list(range(1,11)),draw_at=offset+101,settled_at=offset+110,real_rows=[] if late else [dict(common,role="REAL_TRADE",hit=True)])
            delayed=scenario=="PENDING_THEN_PROOF"
            sc=dict(choice_rows=1,returned_choice=choice,first_read=offset+(4 if delayed or late else 13),
                    proof_visible_at=offset+(120 if late else 12),first_read_outcome="PENDING" if delayed else "READY",
                    final_rows_at_first_read=0,attempts=[dict(id=str(i),result=("COMMITTED","REPLAY_EXISTING")[i],start=offset+30+i,end=offset+41+i,choice_row=choice,trade_id=trade_id) for i in range(2)])
            sc.update(timing_registration_sha256=timing["sha256"],observed_execution_reserve_ms=20,
                      decision_cutoff=80+offset,selected_at=offset+28)
            if delayed or late:
                sc.update(first_read=4,first_read_outcome="PENDING",selected_at=80 if late else 79,
                          proof_visible_at=81 if late else 79,final_rows_before_cutoff=0,
                          after_late_delivery_choice=c["row_hash"])
                c["committed_at"]=80
                d["authorized_at"]=82
                le["committed_at"]=88
            if fault=="early_terminal" and late: sc["selected_at"]=c["committed_at"]=sc["first_read"]
            if fault=="hidden_terminal" and late: sc["final_rows_before_cutoff"]=1
            if fault=="changed_clock" and late: sc["decision_cutoff"]+=2
            if fault=="unbound_reserve" and late: sc["observed_execution_reserve_ms"]=15
            if fault=="premature_admission" and delayed: sc["proof_visible_at"]=78
            if fault=="changed_late_readback" and late: sc["after_late_delivery_choice"]=digest("overwritten")
            if fault=="false_terminal" and delayed: sc["final_rows_at_first_read"]=1
            if fault=="late_trades" and late: le["trade_rows"]=[{}]
            if fault=="sequential_retries" and scenario=="CONCURRENT_RETRY":sc["attempts"][1]["start"]=sc["attempts"][1]["end"]
            if fault=="wrong_retry" and scenario=="CONCURRENT_RETRY":sc["attempts"][1]["trade_id"]+=2
            if fault=="fake_source" and nxt: fields["source_commit"]="f"*40
            if fault=="wrong_k3" and nxt:d["action"]=[2,3,4,5]
            if fault=="uncorrelated_probability" and nxt:le["probability_rows"][0]["selected_hit_p"]=.4
            if fault=="both_committed" and scenario=="CONCURRENT_RETRY":sc["attempts"][1]["result"]="COMMITTED"
            if fault=="duplicate" and nxt:le["trade_rows"]*=2
            if fault=="wrong_hit" and nxt:se["real_rows"][0]["hit"]=False
            if fault=="different_certificate" and nxt:le["exact_rows"][0]["certificate"]=digest("other")
            if fault=="changed_input" and delayed:case["input"]=put("different",dict(user_switch=True,action=action,policy=policy,target=target,payload="other"))
            for (stage,producer),row in zip(SOURCES.items(),(q,a,c,d,le,se,sc)):
                case[stage]=put(branch+stage,dict(fields,producer=producer,**row))
        meta=put("index", index)
        if fault=="missing_original":refs.pop("PENDING_THEN_PROOF/schedule")
        r=dict(source_after=src,plan_manifest_sha256=plan,apk_sha256=None)
        run=dict(mainline_campaign=meta,apk_artifact="fixture.apk",apk_identity_member=member)
        if fault=="swapped_apk":r["apk_sha256"]=digest("other")
        return run,r,Originals(tmp,refs.values()),repo

    def test_positive(self):
        with tempfile.TemporaryDirectory() as d:
            self.assertEqual(check_mainline_receipt(*self.fixture(d))["scenarios"],5)

    def test_interleavings_and_identity(self):
        for fault in ("false_terminal","late_trades","sequential_retries","wrong_retry","fake_source","wrong_k3",
                      "early_terminal","hidden_terminal","changed_clock","unbound_reserve","premature_admission",
                      "changed_late_readback","changed_source_budget","both_committed","uncorrelated_probability","duplicate","wrong_hit","different_certificate","changed_input","missing_original","swapped_apk"):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as d, self.assertRaises(ValueError):
                check_mainline_receipt(*self.fixture(d,fault))


if __name__ == "__main__":
    unittest.main()
