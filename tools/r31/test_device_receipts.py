#!/usr/bin/env python3
"""Synthetic evaluator regressions only: no physical-device or natural claim.

Retains the original short-window/gap/condition/source/threshold negative checks,
adds missing-opportunity, postdated-observation, paging, conflict and stream tests.
"""
import copy, json, sqlite3, sys, tempfile, unittest
from pathlib import Path
from unittest import mock
sys.path.insert(0, str(Path(__file__).resolve().parent))
import collect_device_receipts as col
import accept_device_receipts as acc

T = 1800000000000
CFG = {'minimum_predraw_margin_ms': 5000, 'eligible_realtime_freeze_coverage_min': .99,
       'settlement_coverage_of_valid_frozen_records_min': 1.0, 'initial_device_hours': 24,
       'final_device_hours': 72, 'memory_warmup_hours': 2, 'memory_comparison_window_hours': 24,
       'memory_constant_workload_last_vs_first_p95_pss_ratio_max': 1.1,
       'disk_normal_hard_admission_mib': 2048, 'post_warmup_daily_growth_target_mib': 50}

def proof():
    row = {col.DB: 'd' * 64, col.DB + '-wal': None}
    return {'before': dict(row), 'copied': dict(row), 'after': dict(row)}

def calendar(count=2): return col.make_calendar('202611151439', T, count, T+1, 'c'*40, 'a'*64, 'e'*64, window_start_ms=T+1)

def freeze(entry, index, settled=False, policy='policy-A'):
    return {'policy_hash': policy, 'target_ordinal': 1000+index, 'target_issue': entry['targetIssue'], 'position': 1,
            'n1': 1, 'n2': 2, 'n3': 3, 'act': 1, 'status': 'FROZEN', 'frozen_at': entry['deadlineAt']-20000,
            'deadline_at': entry['deadlineAt'], 'probabilities_sha256': 'b'*64,
            'settled_at': entry['deadlineAt']+1000 if settled else 0, 'hit': 1 if settled else -1}

def snapshot(cal, observed, freezes, draws):
    return {'pulled': True, 'format': col.SNAPSHOT_FORMAT, 'complete': True, 'consistentCopy': proof(),
            'windowStartMs': cal['windowStartMs'], 'windowEndMs': cal['windowEndMs'], 'observedAtMs': observed,
            'freezeRows': copy.deepcopy(freezes), 'drawRows': copy.deepcopy(draws), 'tasks': [['task', 'RUNNING', 1, observed, '']]}

def sample(at, db=None, condition='FOREGROUND'):
    row={'deviceWallMs':at, 'bootId':'boot-1', 'sourceCommit':'c'*40, 'apkSha256':'a'*64, 'condition':condition,
         'pid':'12', 'pssKb':200000, 'dirBytes':{k:1024 for k in (*col.DIRS, 'UNCLASSIFIED')}}
    if db is not None: row['db']=db
    return row

def fixture(count=2):
    cal = calendar(count); rows = [sample(cal['windowStartMs'])]; live=[]; final=[]; draws=[]
    for i,e in enumerate(cal['entries']):
        live.append(freeze(e,i)); final.append(freeze(e,i,True)); draws.append({'issue':e['targetIssue'],'open_time':e['deadlineAt'],'first_seen_at':e['deadlineAt']+500,'dirty':0})
        at=e['deadlineAt']-10000
        rows.append(sample(at,snapshot(cal,at,live,draws[:-1])))
    at=cal['windowEndMs']+2000; rows.append(sample(at,snapshot(cal,at,final,draws)))
    return {'samples':rows,'calendar':cal,'gaps':[],'collectionProblems':[],'intervalSeconds':60,'scopeProblems':[]}

def evaluate(m): return acc.evaluate_gate(m,config=CFG)


def ledger_fixture():
    m=fixture(1);boot='12345678-abcd-4567-abcd-123456789012';policy='d'*64
    for s in m['samples']:
        s['bootId']=boot;s['deviceElapsedMs']=s['deviceWallMs']-T+100000
        if 'db' in s:
            for f in s['db']['freezeRows']:f['policy_hash']=policy
    for s in m['samples'][:-1]:s.pop('db',None)
    db=m['samples'][-1]['db'];f=db['freezeRows'][0];issue=f['target_issue'];deadline=f['deadline_at'];attempt='actual-attempt'
    p={'format':'R31_R27_TYPED_FREEZE_V1','policyHash':policy,'targetOrdinal':f['target_ordinal'],'targetIssue':issue,'position':f['position'],'n1':1,'n2':2,'n3':3,'probabilityHash':f['probabilities_sha256'],'frozenAt':f['frozen_at'],'deadlineAt':deadline,'baseIssue':col.issue_offset(issue,-1),'choiceId':'choice-1','certificateDigest':'e'*64,'modelStateHash':'f'*64,'sourceRevision':7,'sourceSchema':8,'selectionFence':9}
    raw=json.dumps(p,separators=(',',':'));ph=col.sha_bytes(raw.encode())
    typed={'policy_hash':policy,'target_ordinal':f['target_ordinal'],'target_issue':issue,'choice_id':'choice-1','certificate_digest':'e'*64,'model_state_hash':'f'*64,'source_revision':7,'source_schema':8,'selection_fence':9,'proof_json':raw,'proof_hash':ph,'created_at':deadline-18000}
    real={'target_issue':issue,'trade_id':1,'candidate_id':'R31-R27-'+policy,'model_hash':'f'*64,'certificate_hash':'e'*64,'proof_hash':'1'*64,'recipe_hash':ph,'action_hash':'2'*64,'authorization_hash':'authorization-1','committed_at':deadline-9000}
    original={'target_issue':issue,'game_code':'Binance1-PK10','base_issue':p['baseIssue'],'position':1,'n1':1,'n2':2,'n3':3,'authorization_hash':'authorization-1','record_hash':'3'*64,'pre_draw_frozen':1,'analysis_attempt_id':attempt,'deadline_at':deadline,'commit_at':deadline-9500}
    event_id=col.sha_bytes(('PREDICTION|'+issue+'|'+attempt+'|').encode())
    event={'event_id':event_id,'issue':issue,'source_trace':attempt,'source_issue':p['baseIssue'],'version':'ISSUE_EVENT_R30_REPAIR_1','event_role':'LIVE','event_kind':'PREDICTION','outcome':'OPEN','durable_stage':'NEXT_FROZEN','timing_boundary':'CALLER_OBSERVED_SUCCESS_AFTER_SQLITE_COMMIT','durable_wall':deadline-8000,'durable_mono':deadline-8000-T+100000,'durable_boot':'KERNEL_BOOT:'+boot,'wall_next_frozen':deadline-9000,'mono_next_frozen':deadline-9000-T+100000,'boot_next_frozen':'KERNEL_BOOT:'+boot}
    guards=[{'name':'r31_mainline_freeze_no_update','sql':"CREATE TRIGGER r31_mainline_freeze_no_update BEFORE UPDATE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END"},{'name':'r31_mainline_freeze_no_delete','sql':"CREATE TRIGGER r31_mainline_freeze_no_delete BEFORE DELETE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END"},{'name':'r974_real_no_update','sql':"CREATE TRIGGER r974_real_no_update BEFORE UPDATE ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'R974_REAL_RECEIPT_IMMUTABLE'); END"}]
    db['causalEvidence']={'available':True,'guards':guards,'typedFreezes':[typed],'realReceipts':[real],'canonicalPredictions':[original],'predictionEvents':[{'key':event_id,'payload_json':json.dumps(event)}]}
    return m


class Device(unittest.TestCase):
    def test_missing_freeze_is_counted_in_independent_denominator(self):
        m=fixture(); issue=m['calendar']['entries'][1]['targetIssue']
        for row in m['samples']:
            if 'db' in row: row['db']['freezeRows']=[f for f in row['db']['freezeRows'] if f['target_issue']!=issue]
        g=evaluate(m)['FREEZE']; self.assertEqual((g['eligible'],g['frozenWithMargin'],g['coverage'],g['status']),(2,1,.5,'FAIL'))

    def test_postfill_early_timestamp_is_not_predraw_witness(self):
        m=fixture()
        for row in m['samples'][:-1]: row.pop('db',None)
        g=evaluate(m)['FREEZE']; self.assertEqual(g['status'],'BLOCKED'); self.assertEqual(g['frozenWithMargin'],0); self.assertEqual(len(g['timingOrOutcomeUnknown']),2)

    def test_existing_bound_post_commit_ledger_needs_no_per_period_external_copy(self):
        m=ledger_fixture();g=evaluate(m);self.assertEqual(g['FREEZE']['status'],'PASS');self.assertEqual(g['SETTLEMENT']['status'],'PASS')
        self.assertEqual(len(g['FREEZE']['existingLedgerWitnesses']),1)

    def test_existing_ledger_rejects_wrong_policy_recipe_clock_guard_or_attempt(self):
        for field in ('recipe','clock','guard','attempt','margin','proof','settlement_stage'):
            m=ledger_fixture();e=m['samples'][-1]['db']['causalEvidence']
            if field=='recipe':e['realReceipts'][0]['recipe_hash']='0'*64
            if field=='clock':m['samples'][-1]['deviceElapsedMs']+=3000
            if field=='guard':e['guards'].pop()
            if field=='attempt':e['canonicalPredictions'][0]['analysis_attempt_id']='unrelated'
            if field=='proof':e['typedFreezes'][0]['proof_hash']='0'*64
            if field in ('margin','settlement_stage'):
                event=json.loads(e['predictionEvents'][0]['payload_json'])
                if field=='margin':event['durable_wall']=m['calendar']['windowEndMs']-1000
                else:event['durable_stage']='SETTLED'
                e['predictionEvents'][0]['payload_json']=json.dumps(event)
            self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED',field)

    def test_existing_ledger_conflict_in_later_snapshot_does_not_erase_earlier_original(self):
        m=ledger_fixture();later=copy.deepcopy(m['samples'][-1]);later['deviceWallMs']+=1000;later['deviceElapsedMs']+=1000;later['db']['observedAtMs']+=1000
        later['db']['causalEvidence']['realReceipts'][0]['recipe_hash']='0'*64;m['samples'].append(later)
        self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED');self.assertTrue(evaluate(m)['FREEZE']['conflicts'])

    def test_default_calendar_has_collection_lead_and_future_anchor_rejected(self):
        c=col.make_calendar('202611151439',T,10,T+1,'c'*40,'a'*64,'e'*64)
        self.assertEqual(c['windowStartMs'],T+120001);self.assertFalse(col.validate_calendar(c))
        with self.assertRaisesRegex(ValueError,'FUTURE_ANCHOR'):col.make_calendar('202611151439',T+2,10,T+1,'c'*40,'a'*64,'e'*64)

    def test_valid_current_witnesses_and_final_labels_pass_two_specific_gates(self):
        g=evaluate(fixture()); self.assertEqual(g['FREEZE']['status'],'PASS'); self.assertEqual(g['SETTLEMENT']['status'],'PASS')
        self.assertEqual(g['DURATION']['status'],'BLOCKED')

    def test_no_calendar_or_legacy_snapshot_cannot_pass(self):
        m=fixture(); m.pop('calendar'); self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED')
        m=fixture()
        for row in m['samples']:
            if 'db' in row: row['db']={'pulled':True,'frozen':[['x',1,2,3,1]],'draws':[['x',2]]}
        self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED')

    def test_calendar_omission_duplicate_late_registration_rejected(self):
        for change in ('missing','duplicate','registered'):
            c=calendar(4)
            if change=='missing': c['entries'].pop(1)
            elif change=='duplicate': c['entries'][1]=copy.deepcopy(c['entries'][0])
            else: c['registeredAtMs']=c['windowStartMs']+1
            self.assertTrue(col.validate_calendar(c),change)

    def test_4320_calendar_and_cross_day_rollover_are_exact(self):
        c=calendar(4320); self.assertFalse(col.validate_calendar(c)); self.assertEqual(len(c['entries']),4320)
        self.assertEqual(c['entries'][0]['targetIssue'],'202611151440'); self.assertEqual(c['entries'][1]['targetIssue'],'202611160001')
        self.assertEqual(c['entries'][-1]['deadlineAt'],T+4320*60000)

    def test_calendar_exclusion_requires_preregistered_evidence_and_is_visible(self):
        m=fixture(); e=m['calendar']['entries'][1]; e.update(eligible=False,exclusionReason='USER_FORCE_STOP_EXPECTED')
        self.assertTrue(col.validate_calendar(m['calendar']))
        e['exclusionEvidenceSha256']='e'*64
        g=evaluate(m)['FREEZE']; self.assertEqual(g['eligible'],1); self.assertEqual(len(g['excluded']),1)

    def test_immutable_conflict_cannot_be_hidden_by_latest_row(self):
        m=fixture(); m['samples'][-1]['db']['freezeRows'][0]['n1']=4
        g=evaluate(m)['FREEZE']; self.assertEqual(g['status'],'BLOCKED'); self.assertTrue(g['conflicts'])

    def test_policies_never_mix_freeze_and_settlement_fields(self):
        m=fixture(1); e=m['calendar']['entries'][0]
        m['samples'][-1]['db']['freezeRows'][0]['settled_at']=0; m['samples'][-1]['db']['freezeRows'][0]['hit']=-1
        other=freeze(e,0,True,'policy-B'); other['frozen_at']=e['deadlineAt']+1
        m['samples'][-1]['db']['freezeRows'].append(other)
        g=evaluate(m)['SETTLEMENT']; self.assertEqual(g['status'],'FAIL'); self.assertEqual(g['settled'],0)

    def test_missing_draw_is_unknown_not_dropped_from_denominator(self):
        m=fixture(1); m['samples'][-1]['db']['drawRows']=[]
        g=evaluate(m); self.assertEqual(g['FREEZE']['eligible'],1); self.assertEqual(g['FREEZE']['status'],'BLOCKED'); self.assertEqual(g['SETTLEMENT']['status'],'BLOCKED')

    def test_earlier_settled_original_survives_later_retention_snapshot(self):
        m=fixture(2); first=m['calendar']['entries'][0]; extra_at=first['deadlineAt']+2000
        extra=snapshot(m['calendar'],extra_at,[freeze(first,0,True)],[{'issue':first['targetIssue'],'open_time':first['deadlineAt'],'first_seen_at':first['deadlineAt']+500,'dirty':0}])
        # Later snapshots intentionally contain only the newer target.
        for row in m['samples']:
            if row['deviceWallMs']>extra_at and 'db' in row:
                row['db']['freezeRows']=[f for f in row['db']['freezeRows'] if f['target_issue']!=first['targetIssue']]
                row['db']['drawRows']=[d for d in row['db']['drawRows'] if d['issue']!=first['targetIssue']]
        m['samples'].append(sample(extra_at,extra));m['samples'].sort(key=lambda x:x['deviceWallMs'])
        g=evaluate(m); self.assertEqual(g['FREEZE']['status'],'PASS');self.assertEqual(g['SETTLEMENT']['status'],'PASS');self.assertEqual(g['SETTLEMENT']['settled'],2)

    def test_partial_unstable_or_future_observed_snapshot_rejected(self):
        for field in ('partial','hash','future'):
            m=fixture()
            for row in m['samples']:
                if 'db' not in row:continue
                if field=='partial':row['db']['complete']=False
                elif field=='hash':row['db']['consistentCopy']['after'][col.DB]='f'*64
                else:row['db']['observedAtMs']=row['deviceWallMs']+1
            self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED',field)

    def test_db_full_window_reads_over_2000_and_hashes_probability_bytes(self):
        db=sqlite3.connect(':memory:')
        db.executescript('CREATE TABLE draws(issue TEXT,open_time INTEGER,first_seen_at INTEGER,dirty INTEGER); CREATE TABLE r27_live_frozen(policy_hash TEXT,target_ordinal INTEGER,target_issue TEXT,position INTEGER,probabilities BLOB,n1 INTEGER,n2 INTEGER,n3 INTEGER,act INTEGER,status TEXT,frozen_at INTEGER,deadline_at INTEGER,hit INTEGER,settled_at INTEGER); CREATE TABLE r26_task(task_id TEXT,status TEXT,cursor INTEGER,last_progress INTEGER,blocked_reason TEXT);')
        c=calendar(3001)
        for i,e in enumerate(c['entries']):
            db.execute('INSERT INTO draws VALUES(?,?,?,0)',(e['targetIssue'],e['deadlineAt'],e['deadlineAt']+100))
            db.execute('INSERT INTO r27_live_frozen VALUES(?,?,?,1,?,1,2,3,1,\'FROZEN\',?,?,1,?)',('p',1000+i,e['targetIssue'],b'probabilities',e['deadlineAt']-10000,e['deadlineAt'],e['deadlineAt']+1000))
        db.commit();out=col.database_facts(db,c['windowStartMs'],c['windowEndMs'],c['windowEndMs']+2000,proof())
        self.assertTrue(out['complete']);self.assertEqual(len(out['drawRows']),3001);self.assertEqual(len(out['freezeRows']),3001);self.assertEqual(out['freezeRows'][0]['probabilities_sha256'],col.sha_bytes(b'probabilities'))
        self.assertNotIn('probabilities',out['freezeRows'][0]);db.close()

    def test_db_budget_exhaustion_is_failure_not_partial_complete(self):
        db=sqlite3.connect(':memory:');db.execute('CREATE TABLE t(i INTEGER)');db.executemany('INSERT INTO t VALUES(?)',[(1,),(2,)])
        with mock.patch.object(col,'MAX_SNAPSHOT_ROWS',1):
            with self.assertRaisesRegex(RuntimeError,'NOT_COMPLETE'):list(col._query_rows(db,'SELECT * FROM t'))
        db.close()

    def test_existing_causal_adapter_reads_real_sql_table_shapes_and_raw_json(self):
        m=ledger_fixture();source=m['samples'][-1]['db'];e=source['causalEvidence'];db=sqlite3.connect(':memory:')
        def table(name,rows):
            columns=list(rows[0]);ddl=','.join(k+(' INTEGER' if isinstance(rows[0][k],int) else ' TEXT') for k in columns)
            db.execute('CREATE TABLE '+name+'('+ddl+')')
            for row in rows:db.execute('INSERT INTO '+name+' VALUES('+','.join('?' for _ in columns)+')',[row[k] for k in columns])
        f=copy.deepcopy(source['freezeRows'][0]);f['probabilities']=b'original';f.pop('probabilities_sha256')
        table('r27_live_frozen',[f]);table('draws',source['drawRows']);table('r26_task',[{'task_id':'task','status':'RUNNING','cursor':1,'last_progress':T,'blocked_reason':''}])
        table('r31_mainline_freeze',e['typedFreezes']);table('r974_exact_real_receipt',e['realReceipts']);table('v57_shadow_observation_r35',e['canonicalPredictions'])
        event=e['predictionEvents'][0];table('r27_chart_cache',[{'panel_id':'r30-timeline-r1:PRED','key':event['key'],'payload_json':event['payload_json'],'updated_at':T+1000}])
        for guard in e['guards']:db.execute(guard['sql'])
        db.commit();got=col.database_facts(db,m['calendar']['windowStartMs'],m['calendar']['windowEndMs'],m['samples'][-1]['deviceWallMs'],proof())
        self.assertTrue(got['complete']);self.assertTrue(got['causalEvidence']['available']);self.assertEqual(got['causalEvidence']['predictionEvents'][0]['payload_json'],event['payload_json']);self.assertEqual(len(got['causalEvidence']['guards']),3);db.close()

    def test_collection_always_attempts_full_final_database_snapshot(self):
        with tempfile.TemporaryDirectory() as d:
            with mock.patch.object(col,'adb',return_value=str(T)),mock.patch.object(col.time,'monotonic',side_effect=[0,0,1,2]),mock.patch.object(col.time,'sleep'),mock.patch.object(col,'sample',return_value=sample(T)) as take:
                col.collect(str(Path(d)/'samples.jsonl'),'FOREGROUND',60,1,'c'*40,'a'*64,10)
                self.assertEqual(take.call_count,2);self.assertIs(take.call_args_list[-1].args[3],True)

    def test_stable_copy_verifies_before_bytes_and_after_without_shell_input(self):
        with tempfile.TemporaryDirectory() as d:
            def run(argv,stdout,stderr,timeout):stdout.write(b'db');return type('R',(),{'returncode':0})()
            h=col.sha_bytes(b'db')
            with mock.patch.object(col,'remote_hash',side_effect=[h,None,h,None]),mock.patch.object(col.subprocess,'run',side_effect=run):
                self.assertEqual(col._copy_stable_database(Path(d))['copied'][col.DB],h)
        with tempfile.TemporaryDirectory() as d:
            with mock.patch.object(col,'remote_hash',side_effect=[h,None,'f'*64,None]),mock.patch.object(col.subprocess,'run',side_effect=run):
                with self.assertRaisesRegex(RuntimeError,'CHANGED'):col._copy_stable_database(Path(d))

    def test_merge_deduplicates_repeated_files_and_retains_conflict(self):
        m=fixture();h={'header':True,'model':'V2353A','intervalSeconds':60,'calendar':m['calendar'],'calendarSha256':col.sha_bytes(col.canonical(m['calendar']))}
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'samples.jsonl';p.write_text('\n'.join(json.dumps(x) for x in [h,*m['samples']])+'\n')
            got=col.merge_windows([p,p]);self.assertEqual(len(got['samples']),len(m['samples']));self.assertFalse(got['collectionProblems'])
            bad=copy.deepcopy(m['samples'][1]);bad['pssKb']=1
            q=Path(d)/'bad.jsonl';q.write_text(json.dumps(h)+'\n'+json.dumps(bad)+'\n')
            got=col.merge_windows([p,q]);self.assertTrue(any('DUPLICATE_SAMPLE_CONFLICT' in x for x in got['collectionProblems']))

    def test_source_calendar_window_and_sampling_gaps_block(self):
        m=fixture();m['samples'][1]['sourceCommit']='d'*40;self.assertTrue(any('MIXED_SOURCE:' in x for x in col.verify_scope(m)));self.assertEqual(evaluate(m)['FREEZE']['status'],'BLOCKED')
        m=fixture();m['samples'].pop();self.assertIn('CALENDAR_OBSERVATION_WINDOW_INCOMPLETE',col.verify_scope(m))
        m=fixture();m['gaps']=[{'fromMs':T,'toMs':T+600000}];self.assertEqual(evaluate(m)['SCOPE']['status'],'BLOCKED')

    def test_empty_all_failed_memory_and_missing_storage_do_not_crash_or_pass(self):
        self.assertEqual(evaluate({'samples':[]})['MEMORY']['status'],'BLOCKED')
        m=fixture();m['samples'][0]['dirBytes']['databases']=None;self.assertEqual(evaluate(m)['STORAGE']['status'],'BLOCKED')
        self.assertIsNone(acc.p95([None,0,float('nan')]));self.assertEqual(evaluate(fixture())['MEMORY']['status'],'BLOCKED')

    def test_original_memory_and_physical_storage_thresholds_remain_enforced(self):
        m=fixture();m['samples']=[sample(T+hour*3600000) for hour in range(73)]
        for s in m['samples']:s['dirBytes']={k:0 for k in (*col.DIRS,'UNCLASSIFIED')};s['dirBytes']['databases']=100*1024*1024
        g=evaluate(m);self.assertEqual(g['MEMORY']['status'],'PASS');self.assertEqual(g['STORAGE']['status'],'PASS')
        for s in m['samples'][49:]:s['pssKb']=230000
        self.assertEqual(evaluate(m)['MEMORY']['status'],'FAIL')
        for hour,s in enumerate(m['samples']):s['dirBytes']['databases']=(100+hour*3)*1024*1024
        self.assertEqual(evaluate(m)['STORAGE']['status'],'FAIL')
        for s in m['samples']:s['dirBytes']['databases']=0;s['dirBytes']['UNCLASSIFIED']=2049*1024*1024
        self.assertEqual(evaluate(m)['STORAGE']['status'],'FAIL')

    def test_missing_condition_short_duration_and_reverse_clock_remain_explicit(self):
        g=evaluate(fixture());self.assertEqual(g['DURATION']['status'],'BLOCKED');self.assertEqual(g['CONDITIONS']['status'],'BLOCKED')
        with tempfile.TemporaryDirectory() as d:
            p=Path(d)/'reverse.jsonl';p.write_text(json.dumps(sample(T+1000))+'\n'+json.dumps(sample(T))+'\n')
            _,_,problems=col.load([p]);self.assertTrue(any(x.startswith('WALL_CLOCK_REVERSED:') for x in problems))

    def test_late_settlement_wrong_label_or_time_cannot_pass(self):
        for field,value in [('settled_at',0),('hit',-1),('settled_at',T)]:
            m=fixture(1);m['samples'][-1]['db']['freezeRows'][0][field]=value
            self.assertNotEqual(evaluate(m)['SETTLEMENT']['status'],'PASS')

    def test_actual_force_stop_may_have_no_pss_but_remains_separate(self):
        m=fixture();m['samples'][0]['condition']='USER_FORCE_STOP';m['samples'][0]['pssKb']=None
        self.assertFalse(any(x.startswith('MISSING_pssKb') for x in col.verify_scope(m)))
        self.assertEqual(evaluate(m)['CONDITIONS']['samplesPerCondition']['USER_FORCE_STOP'],1)

    def test_silent_running_task_is_not_exempt(self):
        m=fixture();m['samples'][-1]['db']['tasks']=[['task','RUNNING',1,T-7*3600000,'']]
        self.assertEqual(evaluate(m)['TASKS']['status'],'FAIL')

    def test_write_verdict_never_turns_a_blocked_gate_into_global_pass(self):
        with tempfile.TemporaryDirectory() as d:
            v=acc.write_verdict(Path(d)/'v.json',evaluate(fixture()),'24h');self.assertNotEqual(v['overall'],'PASS');self.assertEqual(v['naturalQuality'],'NOT_PROVEN')

if __name__=='__main__':unittest.main(verbosity=2)
