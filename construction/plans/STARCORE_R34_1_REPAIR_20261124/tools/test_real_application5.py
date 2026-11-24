"""Isolated host contracts for the natural deployment reader; no Android or natural results."""
import hashlib
import json
import sqlite3
import tempfile
import unittest
from datetime import datetime, timedelta, timezone
from pathlib import Path

from real_application5 import TABLES, verify_deployment, verify_q02_q07
from verify_receipt import Originals


def h(text):
    return hashlib.sha256(text.encode()).hexdigest()


def fixture(root, fault="", periods=250):
    files = []
    def put(name, doc):
        raw = json.dumps(doc, sort_keys=True).encode()
        (root/name).write_bytes(raw)
        sig = hashlib.sha256(raw).hexdigest()
        files.append(dict(path=name,sha256=sig))
        return sig
    start = datetime(2026, 11, 23,23,50,tzinfo=timezone.utc)
    def t(i):
        return (start+timedelta(seconds=i)).isoformat()
    def key(i):
        stamp = start+timedelta(minutes=i)
        return stamp.strftime("%Y%m%d")+f"{1 + stamp.hour*60+stamp.minute:04d}"
    def slot(i):
        return dict(target_key=key(i),opportunity_at=t(i*60),deadline_at=t(i*60+52))
    end = periods*60
    scope = dict(source_commit="f"*40,apk_sha256=h("apk"),device_identity="unit-contract",candidate_hash=h("child"),
                 protocol_hash=h("protocol"),fixture=False)
    receipt = dict(source_after=scope['source_commit'],apk_sha256=scope['apk_sha256'],device_identity=scope['device_identity'],
                   window_start=t(-80000),window_end=t(end+70))
    result = dict(candidate_hash=scope['candidate_hash'],protocol_hash=scope['protocol_hash'],certificate_artifact="cert",
                  deployment_evidence_artifact="index",fixture=False,issuance_status="ACTIVE",verdict="QUALIFIED")
    policy = scope["candidate_hash"]
    model=h("model")
    cert_hash = put("gates",dict(authority="R27_GATE_V1",gateVersion="R27_GATE_V1",
        gateEvidenceR31=dict(policyHash=policy),failing=[],modelStateBindingR31=dict(policyHash=policy,state="BOUND",bindingSha256=h("substitute") if fault=="issuer_model" else model),
        calibrationBindingR31=dict(policyHash=policy,state="BOUND",calibrationSha256=h("other-cal") if fault=="issuer_cal" else h("cal"))))
    cert_hash_view=put("cert",dict(candidate_hash=policy,protocol_hash=scope['protocol_hash'],certificate_hash=cert_hash,
                                   model_hash=model,gate_bytes_artifact='gates',issued_at=t(-60),expires_at=t(86000)))
    cal=dict(scope, source=TABLES["calendar"],before_window=slot(-1),after_window=slot(periods),
             window_start=t(0),window_end=t(end),registered_at=t(-2),targets=[slot(i) for i in range(periods)],
             draw_source_id="PK10fixture")
    if fault=="cut_calendar":del cal['targets'][3]
    ch=put("calendar",cal)
    raw={k:[] for k in TABLES if k!="calendar"};hold=set(range(38,min(periods,249)))
    native = []
    for i in range(periods):
        k=key(i);a=[1,1,2,3];kid=i+1; other=i>=249
        candidate=(h("other") if other else policy)
        trade = i not in hold
        bound=dict(policy=candidate,certificate_hash=cert_hash,model_hash=model,action=a,proof_hash=h("proof"+k))
        common=dict(target_key=k)
        take=dict(common,selected_policy=candidate if trade else None,terminal=("OTHER_WINNER" if other else "SELECTED") if trade else "ABSTAIN")
        if trade:take.update(bound,frozen_at=t(i*60+10),user_enabled=True)
        raw["choices"].append(take)
        if not trade:continue
        auth, did = h("auth"+k),"d"+k
        raw["decisions"].append(dict(common,**bound,lane="CONTROLLED_PROBE",action_type="BET",decision_id=did,
                                     authorization=auth,authorized_at=t(i*60+20)))
        booking=dict(common,policy=candidate,role="REAL_TRADE",trade_id=kid,action=a,authorization=auth,
                     decision_id=did,committed_at=t(i*60+22))
        raw["trades"].append(booking)
        native.append((k,kid,*a,auth,"BET","REAL_TRADE",1,1))
        if not other:
            record=dict(common,**bound,authorization=auth,trade_id=kid)
            raw["exact"].append(dict(record))
            raw["probabilities"].append(dict(record,selected_probability=.38,calibration_hash=h('cal')))
        raw["draws"].append(dict(common,source_id=cal["draw_source_id"],draw_id="dr"+k,draw_at=t(i*60+60),numbers=list(range(1,11))))
        raw["settlements"].append(dict(common,role="REAL_TRADE",policy=candidate,trade_id=kid,authorization=auth,
                                       hit=True,draw_id="dr"+k,settled_at=t(i*60+64)))
    if fault in ("authorized_no_trade", "selected_no_decision", "false_abstain_with_bet"):
        for name in ("trades", "exact", "probabilities", "settlements"):
            raw[name].pop(0)
        native.pop(0)
        if fault == "false_abstain_with_bet":
            raw["choices"][0] = dict(target_key=key(0), terminal="ABSTAIN", selected_policy=None)
        if fault == "selected_no_decision":
            raw["decisions"].pop(0)
    if fault in ("missing_draw", "arrived_not_settled"):
        raw["settlements"].pop(0)
        if fault == "missing_draw":
            raw["draws"].pop(0)
        native[0]=(*native[0][:-2],0,0)
    if fault=="late_freeze":raw['choices'][0]['frozen_at']=t(-2)
    if fault=="foreign_book": raw['choices'][0]['selected_policy']=h('other')
    if fault=="shadow":raw['trades'][0]['role']='SHADOW'
    if fault=="hidden_native":raw['trades'].pop(0)
    if fault=="invented_native":native.pop(0)
    if fault=="wrong_hit":raw['draws'][0]['numbers'].reverse()
    if fault=="wrong_exact":raw['exact'][0]['proof_hash']=h('otherproof')
    if fault=="stale_cert":raw["revocations"].append(dict(certificate_hash=cert_hash,revoked_at=t(1)))
    if fault=="duplicate":raw['trades'].append(raw['trades'][0])
    if fault=="cross_source":raw['draws'][0]['source_id']="other"
    db=sqlite3.connect(str(root/'original.db'))
    db.execute("CREATE TABLE trades(target_issue TEXT,_id INTEGER,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,authorization_id TEXT,action TEXT,stat_scope TEXT,settled INTEGER,hit INTEGER)")
    db.executemany("INSERT INTO trades VALUES(?,?,?,?,?,?,?,?,?,?,?)",native)
    db.commit();db.close()
    dbhash=hashlib.sha256((root/'original.db').read_bytes()).hexdigest()
    files.append(dict(path='original.db',sha256=dbhash))
    for n, rows in raw.items():
        put(n,dict(scope,source=TABLES[n],calendar_sha256=ch,snapshot_at=t(end+65),rows=rows,trading_database_sha256=dbhash))
    put("index",dict(scope,schema="R341_DEPLOYMENT_BOOK_5",snapshot_at=t(end+65),issuance_artifact_sha256=cert_hash_view,
                     trading_database_artifact='original.db',trading_database_sha256=dbhash,originals={k:k for k in TABLES}))
    return receipt,result,Originals(root,files)


class RealCheck(unittest.TestCase):
    def test_legal_first_use_and_completed_window(self):
        for n,expected in [(1,1),(250,38)]:
            with self.subTest(n=n), tempfile.TemporaryDirectory() as tmp:
                rc,r,o=fixture(Path(tmp),periods=n)
                summary=verify_deployment(rc,r,o)
                self.assertEqual((summary['real_acts'],summary['settled'],summary['hits']),(expected,expected,expected))
                q02=dict(deployed_opportunities=summary['rows'],values=dict(real_act_rate=expected/n))
                q07=dict(scope="ONE_REGISTERED_CANDIDATE",qualified_candidate_hashes=[r['candidate_hash']],
                         values=dict(independently_qualified_assets=1))
                if n==250:
                    verify_q02_q07(r,q02,q07,summary)
                    q07['values']['independently_qualified_assets']=99
                    with self.assertRaises(ValueError):verify_q02_q07(r,q02,q07,summary)
                    q07['values']['independently_qualified_assets']=1
                    q02['deployed_opportunities'] = [dict(x) for x in summary['rows']]
                    q02['deployed_opportunities'][0]['real_act']=False
                    q02['deployed_opportunities'][-1]['real_act']=True
                    with self.assertRaises(ValueError):verify_q02_q07(r,q02,q07,summary)
                del r["deployment_evidence_artifact"]
                self.assertIsNone(verify_deployment(rc,r,o))
    def test_block_bad_sources_and_early_claims(self):
        for fault in ("issuer_model","issuer_cal","cut_calendar","late_freeze","foreign_book","shadow","hidden_native","invented_native","wrong_hit",
                      "wrong_exact","stale_cert","duplicate","cross_source"):
            with self.subTest(fault=fault),tempfile.TemporaryDirectory() as tmp:
                rc,r,o=fixture(Path(tmp),fault=fault,periods=4)
                with self.assertRaises(ValueError):verify_deployment(rc,r,o)
    def test_open_trade_is_real_and_unsettled_until_the_source_arrives(self):
        with tempfile.TemporaryDirectory() as tmp:
            rc,r,o=fixture(Path(tmp),fault='missing_draw',periods=1)
            s=verify_deployment(rc,r,o)
            self.assertEqual((s['real_acts'],s['settled'],len(s['pending_settlement'])),(1,0,1))
            self.assertEqual(s['pending_draw'],s['pending_settlement'])
            self.assertFalse(s['unsettled_after_draw'])
        with tempfile.TemporaryDirectory() as tmp:
            rc,r,o=fixture(Path(tmp),fault='arrived_not_settled',periods=1)
            s=verify_deployment(rc,r,o)
            self.assertEqual((s['real_acts'],s['settled'],len(s['pending_settlement'])),(1,0,1))
            self.assertEqual(s['unsettled_after_draw'],s['pending_settlement'])
            self.assertFalse(s['pending_draw'])

    def test_selected_and_authorized_are_separate_missing_stages(self):
        for fault in ('false_abstain_with_bet','authorized_no_trade','selected_no_decision'):
            with self.subTest(fault=fault), tempfile.TemporaryDirectory() as tmp:
                rc,r,o=fixture(Path(tmp),fault=fault,periods=1)
                if fault=='false_abstain_with_bet':
                    with self.assertRaisesRegex(ValueError,'BET conflicts'):
                        verify_deployment(rc,r,o)
                    continue
                s=verify_deployment(rc,r,o)
                target=s['rows'][0]['target_key']
                self.assertEqual(s['real_acts'],0)
                self.assertEqual(s['missing_selected'],[target])
                self.assertEqual(s['missing_authorized'],[target] if fault=='authorized_no_trade' else [])



if __name__=='__main__':
    unittest.main()
