#!/usr/bin/env python3
"""R32.5 device verdicts from independent opportunities and observed originals.

No frozen-row denominator, no newest-snapshot shortcut, no backdated row as a
predraw witness. Exact policy/source gates and all numerical targets are unchanged.
This evaluator cannot grant a strategy certificate or authorise REAL.
"""
import argparse, json, math, re, sys
from pathlib import Path
import collect_device_receipts as collect

ROOT = Path(__file__).resolve().parents[2]
CONFIG = ROOT / 'construction/STARCORE_R31_FINAL_20261104/ledgers/acceptance_config.json'
CONDITIONS = collect.CONDITIONS


def p95(values):
    values = sorted(x for x in values if isinstance(x, (int, float)) and not isinstance(x, bool) and math.isfinite(x) and x > 0)
    return values[min(len(values) - 1, int(math.ceil(.95 * len(values))) - 1)] if values else None


def gate(status, **facts): return dict(status=status, **facts)


def stable_snapshot(value, sample):
    if not isinstance(value, dict) or value.get('format') != collect.SNAPSHOT_FORMAT or value.get('pulled') is not True or value.get('complete') is not True: return False
    proof = value.get('consistentCopy')
    if not isinstance(proof, dict): return False
    before, after, copied = (proof.get(x) for x in ('before', 'after', 'copied'))
    if not isinstance(before, dict) or set(before) != {collect.DB, collect.DB + '-wal'} or before != after or before != copied: return False
    if not re.fullmatch('[0-9a-f]{64}', str(before[collect.DB])): return False
    wal = before[collect.DB + '-wal']
    if wal is not None and not re.fullmatch('[0-9a-f]{64}', str(wal)): return False
    observed = value.get('observedAtMs')
    return collect.positive_int(observed) and observed <= sample.get('deviceWallMs', 0)


def existing_ledger_witnesses(db, samples, margin):
    """Accept an existing, fully joined typed-mainline post-COMMIT observation.

    Timestamp-only frozen rows, same-issue unrelated predictions, a missing
    event flush and PROCESS_ONLY clocks cannot take this evidence path.
    """
    evidence = db.get('causalEvidence', {}); accepted, rejected = {}, []
    if not evidence.get('available'): return accepted, [evidence.get('reason', 'NO_EXISTING_CAUSAL_EVIDENCE')]
    expected_guards = {
        'r31_mainline_freeze_no_update': "CREATE TRIGGER r31_mainline_freeze_no_update BEFORE UPDATE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END",
        'r31_mainline_freeze_no_delete': "CREATE TRIGGER r31_mainline_freeze_no_delete BEFORE DELETE ON r31_mainline_freeze BEGIN SELECT RAISE(ABORT,'R31_MAINLINE_FREEZE_IMMUTABLE'); END",
        'r974_real_no_update': "CREATE TRIGGER r974_real_no_update BEFORE UPDATE ON r974_exact_real_receipt BEGIN SELECT RAISE(ABORT,'R974_REAL_RECEIPT_IMMUTABLE'); END"}
    normalize = lambda s: re.sub(r'\s+', ' ', str(s).strip())
    guards = {r.get('name'): normalize(r.get('sql')) for r in evidence.get('guards', [])}
    if any(guards.get(k) != normalize(v) for k,v in expected_guards.items()): return accepted, ['EXISTING_LEDGER_IMMUTABILITY_GUARD_MISMATCH']
    frozen = {(r.get('policy_hash'),r.get('target_ordinal')):r for r in db.get('freezeRows', [])}
    receipts = {r.get('target_issue'):r for r in evidence.get('realReceipts', [])}
    originals = {r.get('target_issue'):r for r in evidence.get('canonicalPredictions', [])}
    events = {r.get('key'):r.get('payload_json') for r in evidence.get('predictionEvents', [])}
    for typed in evidence.get('typedFreezes', []):
        key = (typed.get('policy_hash'),typed.get('target_ordinal')); issue=typed.get('target_issue')
        try:
            raw=typed['proof_json']; proof=json.loads(raw); f=frozen[key]; r=receipts[issue]; o=originals[issue]
            if collect.sha_bytes(raw.encode()) != typed['proof_hash'] or proof.get('format') != 'R31_R27_TYPED_FREEZE_V1': raise ValueError('TYPED_PROOF_HASH')
            for pkey,fkey in (('policyHash','policy_hash'),('targetOrdinal','target_ordinal'),('targetIssue','target_issue'),('position','position'),('n1','n1'),('n2','n2'),('n3','n3'),('probabilityHash','probabilities_sha256'),('frozenAt','frozen_at'),('deadlineAt','deadline_at')):
                if proof.get(pkey) != f.get(fkey): raise ValueError('ORIGINAL_FREEZE_BINDING')
            for pkey,tkey in (('choiceId','choice_id'),('certificateDigest','certificate_digest'),('modelStateHash','model_state_hash'),('sourceRevision','source_revision'),('sourceSchema','source_schema'),('selectionFence','selection_fence')):
                if proof.get(pkey) != typed.get(tkey): raise ValueError('TYPED_IDENTITY_BINDING')
            if f.get('act') != 1 or r['candidate_id'] != 'R31-R27-'+f['policy_hash'] or r['recipe_hash'] != typed['proof_hash'] or r['certificate_hash'] != typed['certificate_digest']: raise ValueError('EXACT_REAL_RECIPE_BINDING')
            if o['game_code']!='Binance1-PK10' or o['pre_draw_frozen']!=1 or o['authorization_hash']!=r['authorization_hash'] or o['deadline_at']!=f['deadline_at']: raise ValueError('CANONICAL_TRANSACTION_BINDING')
            if any(o[k]!=f[k] for k in ('position','n1','n2','n3')): raise ValueError('CANONICAL_ACTION_BINDING')
            if proof.get('baseIssue')!=o['base_issue'] or collect.issue_offset(o['base_issue'],1)!=issue: raise ValueError('PIT_PREDECESSOR_BINDING')
            attempt=o['analysis_attempt_id']; event_id=collect.sha_bytes(('PREDICTION|'+issue+'|'+attempt+'|').encode()); event=json.loads(events[event_id])
            if event.get('event_id')!=event_id or event.get('issue')!=issue or event.get('source_trace')!=attempt or event.get('source_issue')!=o['base_issue']: raise ValueError('EVENT_ORIGINAL_BINDING')
            if event.get('version')!='ISSUE_EVENT_R30_REPAIR_1' or event.get('event_role')!='LIVE' or event.get('event_kind')!='PREDICTION' or event.get('outcome')=='EVENT_IDENTITY_CONFLICT': raise ValueError('EVENT_ROLE_OR_CONFLICT')
            if event.get('durable_stage')!='NEXT_FROZEN' or event.get('timing_boundary')!='CALLER_OBSERVED_SUCCESS_AFTER_SQLITE_COMMIT': raise ValueError('POST_COMMIT_OBSERVATION_MISSING')
            wall,mono,boot=(event.get(k) for k in ('durable_wall','durable_mono','durable_boot'))
            if not all(collect.positive_int(x) for x in (wall,mono,event.get('wall_next_frozen'),event.get('mono_next_frozen'))): raise ValueError('DURABLE_CLOCK_MISSING')
            if not re.fullmatch(r'KERNEL_BOOT:[0-9a-f-]{36}',str(boot)) or event.get('boot_next_frozen')!=boot: raise ValueError('KERNEL_CLOCK_SCOPE_MISSING')
            if not f['frozen_at']<=typed['created_at']<=r['committed_at']<=wall<=f['deadline_at']-margin or not f['frozen_at']<=o['commit_at']<=wall: raise ValueError('COMMIT_MARGIN_OR_ORDER')
            if event['wall_next_frozen']>wall or event['mono_next_frozen']>mono: raise ValueError('POST_COMMIT_CLOCK_ORDER')
            # Compare the original wall/monotonic pair with an independently
            # collected anchor on that same kernel boot. Allow 1 s adb skew.
            anchors=[s for s in samples if 'KERNEL_BOOT:'+str(s.get('bootId'))==boot and collect.positive_int(s.get('deviceElapsedMs')) and s['deviceElapsedMs']>=mono and abs((s['deviceWallMs']-wall)-(s['deviceElapsedMs']-mono))<=1000]
            if not anchors: raise ValueError('SAME_BOOT_MONOTONIC_CLOCK_NOT_CORROBORATED')
            for hashed in (typed['proof_hash'],r['proof_hash'],r['action_hash'],o['record_hash']):
                if not re.fullmatch('[0-9a-f]{64}',str(hashed)): raise ValueError('RECEIPT_HASH_INVALID')
            accepted[key]={'committedAt':wall,'eventId':event_id,'typedProofHash':typed['proof_hash'],'source':'EXISTING_TYPED_MAINLINE_CANONICAL_POST_COMMIT_LEDGER'}
        except (KeyError,ValueError,TypeError,OverflowError) as error: rejected.append(str(issue)+':'+str(error))
    return accepted, rejected


def opportunity_gates(merged, cfg):
    calendar = merged.get('calendar'); errors = collect.validate_calendar(calendar)
    if errors: return gate('BLOCKED', reason='OPPORTUNITY_DENOMINATOR_UNKNOWN', problems=errors), gate('BLOCKED', reason='OPPORTUNITY_DENOMINATOR_UNKNOWN')
    rows = merged.get('samples', []); scope_problems = collect.verify_scope(merged)
    scope_problems.extend(merged.get('scopeProblems', []))
    entries = {x['targetIssue']: x for x in calendar['entries']}; eligible = {k: v for k, v in entries.items() if v['eligible']}
    if not eligible: return gate('BLOCKED', reason='NO_ELIGIBLE_OPPORTUNITIES', excluded=len(entries)), gate('BLOCKED', reason='NO_ELIGIBLE_OPPORTUNITIES')
    snapshots, ignored = [], []
    for sample in rows:
        db = sample.get('db')
        if db is None: continue
        if stable_snapshot(db, sample) and db.get('windowStartMs', 1 << 62) <= calendar['windowStartMs'] and db.get('windowEndMs', 0) >= calendar['windowEndMs']:
            snapshots.append(db)
        else: ignored.append({'sampleAt': sample.get('deviceWallMs'), 'reason': db.get('reason', 'UNVERIFIED_OR_PARTIAL_DATABASE_SNAPSHOT') if isinstance(db, dict) else 'INVALID_SNAPSHOT_TYPE'})
    if not snapshots: return gate('BLOCKED', reason='NO_COMPLETE_COHERENT_DATABASE_WINDOW', ignoredSnapshots=ignored), gate('BLOCKED', reason='NO_COMPLETE_COHERENT_DATABASE_WINDOW')
    latest_observed = max(x['observedAtMs'] for x in snapshots)
    draws, freezes, conflicts, ledger_witnesses, ledger_rejections, ledger_originals = {}, {}, set(), {}, [], {}
    immutable = ('policy_hash','target_ordinal','target_issue','position','n1','n2','n3','act','status','frozen_at','deadline_at','probabilities_sha256')
    for db in sorted(snapshots, key=lambda x: x['observedAtMs']):
        causal=db.get('causalEvidence',{})
        for kind in ('typedFreezes','realReceipts','canonicalPredictions'):
            for original in causal.get(kind,[]):
                identity=(kind,original.get('target_issue'),original.get('policy_hash'),original.get('target_ordinal'))
                signature=collect.canonical(original)
                if identity in ledger_originals and ledger_originals[identity]!=signature: conflicts.add(original.get('target_issue'))
                else: ledger_originals[identity]=signature
        witnessed,rejected=existing_ledger_witnesses(db,rows,cfg['minimum_predraw_margin_ms'])
        ledger_rejections.extend(rejected)
        for key,witness in witnessed.items():
            if key not in ledger_witnesses or witness['committedAt']<ledger_witnesses[key]['committedAt']: ledger_witnesses[key]=witness
        for draw in db.get('drawRows', []):
            issue = draw.get('issue')
            if issue not in entries: continue
            if draw.get('dirty') != 0 or draw.get('open_time') != entries[issue]['deadlineAt']: conflicts.add(issue); continue
            if issue in draws and draws[issue].get('open_time') != draw.get('open_time'): conflicts.add(issue)
            draws[issue] = draw
        for freeze in db.get('freezeRows', []):
            issue = freeze.get('target_issue')
            if issue not in entries: continue
            key = (freeze.get('policy_hash'), freeze.get('target_ordinal'))
            if not isinstance(key[0], str) or not key[0] or not collect.positive_int(key[1]): conflicts.add(issue); continue
            if freeze.get('deadline_at') != entries[issue]['deadlineAt']: conflicts.add(issue); continue
            signature = tuple(freeze.get(k) for k in immutable)
            old = freezes.get(key)
            if old and old['signature'] != signature: conflicts.add(issue); conflicts.add(old['row']['target_issue']); continue
            if old is None:
                old = {'row': dict(freeze), 'signature': signature, 'firstObservedAt': db['observedAtMs']}; freezes[key] = old
            old['firstObservedAt'] = min(old['firstObservedAt'], db['observedAtMs'])
            previous = old['row']; settled, hit = freeze.get('settled_at'), freeze.get('hit')
            if previous.get('settled_at', 0) > 0:
                if settled != previous.get('settled_at') or hit != previous.get('hit'): conflicts.add(issue)
            elif collect.positive_int(settled): old['row'] = dict(freeze)
    by_issue = {}
    for key, value in freezes.items(): by_issue.setdefault(value['row']['target_issue'], []).append((key, value))
    good, missed, unknown, late, valid_records = [], [], [], [], []
    margin = cfg['minimum_predraw_margin_ms']
    for issue, opportunity in eligible.items():
        deadline = opportunity['deadlineAt']; values = by_issue.get(issue, [])
        if issue in conflicts: unknown.append(issue); continue
        if not values:
            (missed if latest_observed >= deadline else unknown).append(issue); continue
        timely = False; ambiguous = False
        for key, value in values:
            row, observed = value['row'], value['firstObservedAt']; at = row.get('frozen_at')
            if key in ledger_witnesses: observed=min(observed,ledger_witnesses[key]['committedAt'])
            value['effectiveWitnessAt']=observed
            if row.get('status') != 'FROZEN': continue
            numbers = [row.get('n1'), row.get('n2'), row.get('n3')]
            legal = (type(row.get('position')) is int and 1 <= row['position'] <= 10
                     and all(type(x) is int and 1 <= x <= 10 for x in numbers) and len(set(numbers)) == 3
                     and row.get('act') in (0, 1) and re.fullmatch('[0-9a-f]{64}', str(row.get('probabilities_sha256')))
                     and collect.positive_int(at))
            if not legal: conflicts.add(issue); ambiguous = True; continue
            if at > deadline - margin: late.append(issue); continue
            valid_records.append((key, value))
            if issue not in draws or observed > deadline - margin:
                ambiguous = True  # A timestamp first seen after the deadline is not a predraw witness.
            elif observed < at:
                conflicts.add(issue); ambiguous = True
            elif row.get('settled_at', 0) > 0 and row['settled_at'] < deadline:
                conflicts.add(issue); ambiguous = True
            else: timely = True
        if issue in conflicts: unknown.append(issue)
        elif timely: good.append(issue)
        elif ambiguous: unknown.append(issue)
        elif latest_observed >= deadline: missed.append(issue)
        else: unknown.append(issue)
    denominator = len(eligible); lower, upper = len(good) / denominator, (len(good) + len(unknown)) / denominator
    required = cfg['eligible_realtime_freeze_coverage_min']
    freeze_state = 'FAIL' if upper < required else ('BLOCKED' if scope_problems or conflicts or unknown or lower < required else 'PASS')
    frozen_gate = gate(freeze_state, eligible=denominator, frozenWithMargin=len(good), coverage=lower if not unknown else None,
                       coverageLowerBound=lower, coverageUpperBound=upper, requiredCoverage=required,
                       excluded=[{'targetIssue': x['targetIssue'], 'reason': x['exclusionReason'], 'evidenceSha256': x['exclusionEvidenceSha256']} for x in entries.values() if not x['eligible']],
                       missingOrInvalid=sorted(set(missed)), late=sorted(set(late)), timingOrOutcomeUnknown=sorted(set(unknown)),
                       conflicts=sorted(conflicts), scopeProblems=sorted(set(scope_problems)), ignoredSnapshots=ignored,
                       denominatorSource='PREREGISTERED_COMPLETE_MINUTE_CALENDAR',
                       witnessRule='EXISTING_BOUND_POST_COMMIT_LEDGER_OR_SAME_ORIGINAL_OBSERVED_EARLY',
                       existingLedgerWitnesses=list(ledger_witnesses.values()), existingLedgerUnavailable=sorted(set(ledger_rejections)))
    # Count original policy/target records separately; do not combine one policy's
    # earliest freeze with a different policy's final label or settlement.
    valid, settled, unsettled, settlement_unknown = [], [], [], []
    for key, value in valid_records:
        row = value['row']; issue = row['target_issue']; identity = {'policy': key[0], 'targetOrdinal': key[1], 'targetIssue': issue}
        if issue in conflicts or issue not in draws or value.get('effectiveWitnessAt',value['firstObservedAt']) > row['deadline_at'] - margin:
            settlement_unknown.append(identity); continue
        valid.append(identity)
        when, hit = row.get('settled_at'), row.get('hit')
        if collect.positive_int(when) and when >= max(row['frozen_at'], draws[issue]['open_time']) and when <= latest_observed and type(hit) is int and hit in (0, 1): settled.append(identity)
        else: unsettled.append(identity)
    sc = len(settled) / len(valid) if valid else None
    if valid and sc < cfg['settlement_coverage_of_valid_frozen_records_min']: settlement_state = 'FAIL'
    elif not valid or settlement_unknown or scope_problems or conflicts: settlement_state = 'BLOCKED'
    else: settlement_state = 'PASS'
    settlement_gate = gate(settlement_state, validFrozenWithDraw=len(valid), settled=len(settled), coverage=sc,
                           unsettled=unsettled, unprovenOriginals=settlement_unknown,
                           requiredCoverage=cfg['settlement_coverage_of_valid_frozen_records_min'],
                           key='policy_hash,target_ordinal', scopeProblems=sorted(set(scope_problems)))
    return frozen_gate, settlement_gate


def evaluate_gate(merged, phase='24h', config=None):
    cfg = config or json.loads(CONFIG.read_text()); rows = merged.get('samples', []); gates = {}
    names = ('DURATION', 'CONDITIONS', 'SCOPE', 'FREEZE', 'SETTLEMENT', 'MEMORY', 'STORAGE', 'TASKS')
    if not rows: return {name: gate('BLOCKED', reason='NO_SAMPLES') for name in names}
    t0, t1 = rows[0]['deviceWallMs'], rows[-1]['deviceWallMs']; hours = (t1 - t0) / 3.6e6
    need_h = cfg['initial_device_hours'] if phase == '24h' else cfg['final_device_hours']
    gates['DURATION'] = gate('PASS' if hours >= need_h else 'BLOCKED', observedHours=round(hours, 3), requiredHours=need_h)
    seen = {c: sum(1 for r in rows if r.get('condition') == c) for c in CONDITIONS}
    gates['CONDITIONS'] = gate('PASS' if all(seen.values()) else 'BLOCKED', samplesPerCondition=seen, missing=[c for c, n in seen.items() if not n])
    problems = sorted(set(collect.verify_scope(merged) + merged.get('scopeProblems', [])))
    gates['SCOPE'] = gate('BLOCKED' if problems else 'PASS', problems=problems, gaps=merged.get('gaps', []))
    gates['FREEZE'], gates['SETTLEMENT'] = opportunity_gates(merged, cfg)
    warm = t0 + cfg['memory_warmup_hours'] * 3.6e6; span = cfg['memory_comparison_window_hours'] * 3.6e6
    first = [r.get('pssKb') for r in rows if warm <= r['deviceWallMs'] < warm + span and r.get('condition') != 'USER_FORCE_STOP']
    last = [r.get('pssKb') for r in rows if r['deviceWallMs'] >= t1 - span and r.get('condition') != 'USER_FORCE_STOP']
    pfirst, plast = p95(first), p95(last)
    incomplete_memory = any(not isinstance(x, (int, float)) or isinstance(x, bool) or not math.isfinite(x) or x <= 0 for x in first + last)
    if not first or not last or t1 - warm < 2 * span or incomplete_memory or not pfirst or not plast:
        gates['MEMORY'] = gate('BLOCKED', reason='COMPARISON_WINDOWS_OR_VALID_SAMPLES_MISSING', firstSamples=len(first), lastSamples=len(last))
    else:
        ratio = plast / pfirst
        gates['MEMORY'] = gate('PASS' if ratio <= cfg['memory_constant_workload_last_vs_first_p95_pss_ratio_max'] else 'FAIL', firstP95Kb=pfirst, lastP95Kb=plast, ratio=ratio)
    required_dirs = set(collect.DIRS) | {'UNCLASSIFIED'}; totals = []; missing_storage = []
    for row in rows:
        ds = row.get('dirBytes', {})
        if not required_dirs.issubset(ds) or any(type(ds.get(k)) is not int or ds[k] < 0 for k in required_dirs): missing_storage.append(row['deviceWallMs']); continue
        totals.append((row['deviceWallMs'], sum(ds[k] for k in required_dirs)))
    mib = 1024 * 1024
    if not totals or missing_storage:
        gates['STORAGE'] = gate('BLOCKED', reason='STORAGE_DIRECTORY_BYTES_UNKNOWN', missingSamples=missing_storage)
    else:
        peak = max(v for _, v in totals) / mib; window = [(t, v) for t, v in totals if t >= t1 - 2 * 86400000]
        duration_days = (window[-1][0] - window[0][0]) / 86400000 if len(window) > 1 else 0
        growth = (window[-1][1] - window[0][1]) / mib / duration_days if duration_days >= 2 - merged.get('intervalSeconds', 60) / 86400 else None
        state = 'FAIL' if peak > cfg['disk_normal_hard_admission_mib'] else ('BLOCKED' if growth is None else ('PASS' if growth <= cfg['post_warmup_daily_growth_target_mib'] else 'FAIL'))
        gates['STORAGE'] = gate(state, peakMib=round(peak, 2), dailyGrowthLastTwoDaysMib=growth,
                                unclassifiedPeakMib=max(r['dirBytes']['UNCLASSIFIED'] for r in rows) / mib,
                                protected='included in physical total; never subtracted')
    dbs = [r['db'] for r in rows if stable_snapshot(r.get('db'), r)]
    tasks = dbs[-1].get('tasks', []) if dbs else []
    silent = [task[0] for task in tasks if len(task) == 5 and task[1] in ('BLOCKED', 'YIELDED', 'READY', 'RUNNING') and not task[4] and (not task[3] or task[3] < t1 - 6 * 3.6e6)]
    gates['TASKS'] = gate('BLOCKED' if not tasks else ('FAIL' if silent else 'PASS'), tasks=len(tasks), silentWithoutReason=silent)
    return gates


def write_verdict(path, gates, phase):
    states = [v.get('status') for v in gates.values()]
    overall = 'FAIL' if 'FAIL' in states else ('PASS' if states and all(s == 'PASS' for s in states) else 'BLOCKED')
    verdict = {'format': 'R325_DEVICE_VERDICT_V2', 'phase': phase, 'overall': overall, 'gates': gates,
               'naturalQuality': 'NOT_PROVEN', 'realAuthority': 'UNCHANGED_NO_AUTHORIZATION',
               'rule': 'All declared opportunities and original conflicts retained; no path/group-only acceptance.'}
    Path(path).write_text(json.dumps(verdict, ensure_ascii=False, indent=2) + '\n'); return verdict


def main():
    parser = argparse.ArgumentParser(description=__doc__); parser.add_argument('merged'); parser.add_argument('--phase', choices=['24h', '72h'], default='24h'); parser.add_argument('--out', required=True)
    args = parser.parse_args(); verdict = write_verdict(args.out, evaluate_gate(json.loads(Path(args.merged).read_text()), args.phase), args.phase)
    print(json.dumps({'overall': verdict['overall'], 'gates': {k: v['status'] for k, v in verdict['gates'].items()}}))
    return 0 if verdict['overall'] == 'PASS' else 1


if __name__ == '__main__': sys.exit(main())
