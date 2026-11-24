"""Offline protocol tests only. Short traces below exercise the extra verifier, not device duration."""
import copy
import hashlib
import json
import unittest
from datetime import datetime, timedelta, timezone
from background4 import check_background4


class Originals:
    def __init__(self, docs):
        self.files = docs
        self.hashes = {k: hashlib.sha256(json.dumps(v,sort_keys=True).encode()).hexdigest() for k,v in docs.items()}

    def json(self, name):
        return self.files[name]


def fixture(fault=""):
    start = datetime(2026,11,24,23,59,tzinfo=timezone.utc)
    def t(seconds):
        return (start+timedelta(seconds=seconds)).isoformat()

    keys = ["202611241439","202611241440","202611250001","202611250002","202611250003"]
    scope = dict(apk_sha256="a"*64,installed_version_code=577311130,device_identity="field-test",
                 window_start=t(0),window_end=t(180),source_commit="a"*40)
    r = {**scope, "source_after":scope["source_commit"],"background_campaign_artifact":"run"}
    refs = dict(preregistered_calendar_artifact="cal",opportunity_ledger_artifact="ledger",
                boot_segments_artifact="boots",interventions_artifact="actions",raw_events_artifact="raw",
                coverage_evaluator_artifact="out",observer_artifact="observer")
    def point(i):
        return dict(target_key=keys[i],opportunity_at=t((i-1)*60))
    cal = dict(**scope,targets=[point(i) for i in (1,2,3)],registered_at=t(-90),clock_error_ms=1,
               before_window=point(0),after_window=point(4))
    def ob(id,kind,sec,boot="b1",elapsed=None):
        return dict(event_id=id,kind=kind,observed_at=t(sec),boot_id=boot,elapsed_ms=sec*1000 if elapsed is None else elapsed)
    observer = dict(**scope,complete=True,records=[ob("s","SEGMENT_START",0),ob("h","HOME",1),
        ob("e","SEGMENT_END",90),ob("s2","SEGMENT_START",90,"b2",0),ob("r","REBOOT",90,"b2",0),
        ob("e2","SEGMENT_END",180,"b2",90000)])
    def segment(boot,a,b):
        return dict(boot_id=boot,start_event_id=a,end_event_id=b,clock_error_ms=1)
    boots=dict(**scope,segments=[segment("b1","s","e"),segment("b2","s2","e2")])
    actions=dict(**scope,observer_event_ids=["h","r"])
    docs=dict(run=refs,cal=cal,observer=observer,boots=boots,actions=actions)
    raw=[];per=[];comm=[];inputs=[];freezes=[];settle=[];draws=[]
    source={k:scope[k] for k in ("device_identity","source_commit","apk_sha256","installed_version_code")}
    for j,key in enumerate(keys[1:4]):
        sec=j*60
        identity=dict(**source,target_key=key,producer="SERVICE",role="R35_PREDICTION_SHADOW")
        b="b1" if sec<90 else "b2"
        e=dict(target_key=key,sequence=1,observed_at=t(sec+15),terminal_state="PRIMARY_FREEZE_COMMITTED",
               reason="committed",producer="SERVICE",service_commit=True,role="R35_PREDICTION_SHADOW",
               boot_id=b,elapsed_ms=(sec+15-(0 if b=="b1" else 90))*1000)
        raw.append({**e,"commit_artifact":"commits","commit_record_index":j})
        comm.append({**e,**identity,"committed":True})
        inputs.append(dict(**source,target_key=key,origin="INDEPENDENT_INPUT_OBSERVER",input_state="HEALTHY",
                           opportunity_at=t(sec),deadline_at=t(sec+35),draw_due_at=t(sec+40)))
        freezes.append(dict(**identity,committed=True,returned_at=t(sec+15),freeze_key="f"+str(j)))
        draws.append(dict(**source,origin="AUTHORITATIVE_DRAW_INGRESS",target_key=key,provider_id="fixture_source",
                          draw_id="draw"+str(j),state="DRAW_RECEIVED",draw_at=t(sec+40),received_at=t(sec+41)))
        settle.append(dict(**identity,committed=True,draw_at=t(sec+40),draw_id="draw"+str(j),
                           committed_at=t(sec+41),freeze_key="f"+str(j)))
        per.append(dict(target_key=key,outcome="FROZEN_AND_SETTLED",input_artifact="inputs",input_record_index=j,
                        freeze_artifact="freezes",freeze_record_index=j,draw_artifact="draws",draw_record_index=j,settlement_artifact="settles",settlement_record_index=j))
    rows=copy.deepcopy(raw)
    out=dict(**scope,per_target_shadow=per,service_commit_targets=3)
    docs.update(raw=dict(**scope,opportunity_events=raw),ledger=dict(**scope,opportunities=rows),out=out,
                inputs=dict(records=inputs),freezes=dict(records=freezes),settles=dict(records=settle),
                commits=dict(records=comm),draws=dict(records=draws))
    if fault=="hide_action":
        observer["records"].append(ob("hidden","ACTIVITY_OPEN",16))
    if fault=="silent":
        observer["complete"]=False
    if fault=="clock":
        observer["records"][-1]["elapsed_ms"]=140
    if fault=="omit_boot":
        boots["segments"].pop()
    if fault=="skip_late":
        rows[2]["terminal_state"]="fabricated"
    if fault=="wrong_mono":
        raw[2]["elapsed_ms"]+=20000;rows[2]["elapsed_ms"]+=20000;comm[2]["elapsed_ms"]+=20000
    if fault=="cross_boot":
        raw[2]["boot_id"]="b1"
    if fault=="force_stop":
        observer["records"].append(ob("manual","USER_FORCE_STOP",12)); actions["observer_event_ids"].append("manual")
    if fault=="sparse":
        cal["targets"].pop(1);rows.pop(1)
    if fault=="calendar_late":
        cal["registered_at"]=t(70)
    if fault=="sparse_times":
        cal["targets"][1]["opportunity_at"]=t(61)
    if fault=="wrong_role":
        freezes[2]["role"]="S0"
    if fault=="late_freeze":
        freezes[2]["returned_at"]=t(166)
    if fault=="wrong_settle":
        settle[1]["freeze_key"]="other"
    if fault=="wrong_input":
        inputs[2]["target_key"]=keys[1]
    if fault=="late_replay":
        raw[2]["observed_at"]=t(181)
    if fault=="no_service":
        for i in range(3):
            rows[i]["service_commit"]=raw[i]["service_commit"]=False
    if fault=="network":
        i=1
        inputs[i].update(input_state="NETWORK_UNAVAILABLE",reason="offline",from_at=t(50),until_at=t(75))
        per[i]["outcome"]="OBSERVED_INPUT_CONSTRAINT"
        raw.pop(i)
        rows[i].update(terminal_state="NOT_OBSERVED_AS_OF_CUTOFF",observed_at=t(180),service_commit=False)
        out["service_commit_targets"]=2
    if fault=="boundary_freeze":
        freezes[1]["returned_at"]=t(60)
    if fault=="early_freeze":
        freezes[1]["returned_at"]=t(50)
    if fault=="early_draw":
        draws[1].update(draw_at=t(97),received_at=t(98));settle[1]["draw_at"]=t(97)
    if fault=="foreign_draw":
        draws[1]["target_key"]=keys[1]
    if fault=="authority_draw_time_differs":
        settle[1]["draw_at"]=t(99)
    if fault=="settled_before_received":
        draws[1]["received_at"]=t(103)
    if fault=="mixed_draw_id":
        settle[1]["draw_id"]="old"
    if fault=="future_due_hides_draw":
        inputs[2]["draw_due_at"]=t(185)
        per[2]["outcome"]="FROZEN_PENDING_DRAW"
    if fault=="right_edge":
        draws[2].update(state="NOT_YET_OBSERVED",as_of=t(180))
        for k in ("draw_id","draw_at","received_at"): draws[2].pop(k)
        inputs[2]["draw_due_at"]=t(185)
        per[2]["outcome"]="FROZEN_PENDING_DRAW"
    h=Originals(docs).hashes
    out["raw_events_sha256"]=h["raw"]
    for doc in (out,boots,actions):
        doc["observer_sha256"]=h["observer"]
    return r,Originals(docs)


class BackgroundTests(unittest.TestCase):
    def test_home_and_observed_reboot(self):
        self.assertEqual(check_background4(*fixture()),3)

    def test_observed_network_constraint_and_right_edge(self):
        self.assertEqual(check_background4(*fixture("network")),2)
        self.assertEqual(check_background4(*fixture("right_edge")),3)

    def test_primary_shadow_and_authority_order(self):
        self.assertEqual(check_background4(*fixture("boundary_freeze")),3)
        for fault in ("early_freeze","early_draw","foreign_draw","authority_draw_time_differs",
                      "settled_before_received","mixed_draw_id","future_due_hides_draw"):
            with self.subTest(fault=fault),self.assertRaises(ValueError):
                check_background4(*fixture(fault))

    def test_missing_or_assisted_observation(self):
        for fault in ["hide_action","silent","clock","omit_boot","cross_boot","wrong_mono","force_stop"]:
            with self.subTest(fault=fault),self.assertRaises(ValueError):
                check_background4(*fixture(fault))

    def test_missing_calendar_and_business_originals(self):
        for fault in ["skip_late","sparse","calendar_late","sparse_times","wrong_role","late_freeze",
                      "wrong_settle","wrong_input","late_replay","no_service"]:
            with self.subTest(fault=fault),self.assertRaises(ValueError):
                check_background4(*fixture(fault))


    def test_full_72_hour_denominator_and_both_verifiers(self):
        from verify_receipt import campaign
        r, originals = fixture()
        docs = originals.files
        run = docs["run"]
        start = datetime.fromisoformat(r["window_start"])
        def when(seconds):
            return (start+timedelta(seconds=seconds)).isoformat()
        def key(i):
            return (start.date()+timedelta(days=(1439+i)//1440)).strftime("%Y%m%d")+f"{(1439+i)%1440+1:04d}"
        def item(i):
            return dict(target_key=key(i),opportunity_at=when(i*60))
        r.update(fixture=False,window_end=when(72*3600))
        run.update({k:r[k] for k in ("apk_sha256","device_identity","window_start","window_end","installed_version_code","fixture")})
        run.update(source_commit=r["source_after"],checkpoint_24h_artifact="cut")
        for doc in ("cal","boots","out","ledger","observer","raw","actions"):
            docs[doc]["window_end"]=r["window_end"]
        docs["cal"].update(targets=[item(i) for i in range(4320)],before_window=item(-1),after_window=item(4320))
        obs=docs["observer"]["records"]
        endobs=dict(obs[0],event_id="e",kind="SEGMENT_END",observed_at=r["window_end"],elapsed_ms=72*3600*1000)
        docs["observer"]["records"]=[obs[0],obs[1],endobs]
        docs["boots"]["segments"]=docs["boots"]["segments"][:1]
        docs["actions"]["observer_event_ids"]=["h"]
        recs={k:[] for k in ("commits","freezes","settles","inputs","draws")}
        base={k:docs[k]["records"][0] for k in recs}
        raw=[];slo=[]
        for i in range(4320):
            sec=i*60
            target=key(i)
            e=dict(docs["raw"]["opportunity_events"][0],target_key=target,observed_at=when(sec+15),
                   elapsed_ms=(sec+15)*1000,commit_record_index=i)
            raw.append(e)
            recs["commits"].append(dict(base["commits"],**e))
            recs["inputs"].append(dict(base["inputs"],target_key=target,opportunity_at=when(sec),
                                       deadline_at=when(sec+35),draw_due_at=when(sec+40)))
            recs["freezes"].append(dict(base["freezes"],target_key=target,freeze_key=f"f{i}",returned_at=when(sec+15)))
            recs["draws"].append(dict(base["draws"],target_key=target,draw_id=f"draw{i}",
                                      draw_at=when(sec+40),received_at=when(sec+41)))
            recs["settles"].append(dict(base["settles"],target_key=target,freeze_key=f"f{i}",draw_id=f"draw{i}",
                                        draw_at=when(sec+40),committed_at=when(sec+41)))
            slo.append(dict(docs["out"]["per_target_shadow"][0],target_key=target,input_record_index=i,
                            freeze_record_index=i,settlement_record_index=i,draw_record_index=i))
        for k,v in recs.items():
            docs[k]={"records":v}
        docs["raw"]["opportunity_events"]=raw
        docs["ledger"]["opportunities"]=raw
        docs["out"].update(per_target_shadow=slo,service_commit_targets=4320,unattended=True,verdict="PASS",opportunity_count=4320)
        dig=Originals(docs).hashes
        for doc in ("boots","actions","out"):
            docs[doc]["observer_sha256"]=dig["observer"]
        docs["out"].update(raw_events_sha256=dig["raw"],ledger_sha256=dig["ledger"],calendar_sha256=dig["cal"])
        docs["cut"]=dict(window_start=r["window_start"],window_end=when(86400),exported_at=when(86401),
                         source_commit=r["source_after"],opportunities=raw[:1440],calendar_sha256=dig["cal"],
                         raw_events_sha256=dig["raw"],**{k:r[k] for k in ("device_identity","installed_version_code","apk_sha256")})
        final=Originals(docs)
        campaign(r,final)
        self.assertEqual(check_background4(r,final),4320)


if __name__=="__main__":
    unittest.main()
