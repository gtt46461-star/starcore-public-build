"""Recompute the preregistered Q06 parent comparison. This never authorizes REAL."""
from datetime import datetime
import hashlib
import json
import math
import re


METHOD = "R27_PAIRED_MOMENTS_ONE_SIDED_95_FULL_CALENDAR_V1"
UTILITY = "FROZEN_ACT_HIT_MINUS_0_30_OR_ZERO_ON_COMMITTED_ABSTAIN"


def need(ok, message):
    if not ok:
        raise ValueError(message)


def digest(value):
    raw = json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False).encode()
    return hashlib.sha256(raw).hexdigest()


def time(value):
    result = datetime.fromisoformat(value)
    need(result.utcoffset() is not None, "Q06 timestamp lacks timezone")
    return result


def keyed(rows):
    result = {row['target_key']: row for row in rows}
    need(len(rows) == len(result), "Q06 duplicate opportunity")
    return result


def valid_context(doc, subject, result):
    need(doc['candidate_hash'] == subject and doc['source_commit'] == result['source_commit'] and
         doc['protocol_hash'] == result['protocol_hash'] and doc['role'] == 'PREREGISTERED_FUTURE' and
         doc['fixture'] is False, "Q06 evidence belongs to another policy, source, protocol or role")


def utility(freeze, draw):
    need(type(freeze['act']) is bool, "Q06 ACT or abstention must be explicit")
    if not freeze['act']:
        need(freeze.get('position') is None and freeze.get('numbers') in (None,[]) and freeze.get('reason'),
             "Q06 abstention lacks its terminal reason or has a prediction")
        return 0.0, None
    position, numbers = freeze['position'], freeze['numbers']
    need(type(position) is int and 1 <= position <= 10 and len(numbers) == 3 and
         all(type(n) is int and 1 <= n <= 10 for n in numbers) and len(set(numbers)) == 3,
         "Q06 invalid single-position K3 forecast")
    actual = draw['numbers'][position-1] in numbers
    return int(actual) - 0.30, actual


def verify_q06(pre, calendar, receipt, result, src, originals):
    protocol = pre['q06_parent_comparison']
    child, parent = pre['candidate_hash'], protocol['parent_hash']
    need(all(isinstance(h,str) and re.fullmatch('[0-9a-f]{64}',h) for h in (child,parent)) and child != parent,
         "Q06 parent not independently identified")
    need(src['interval_scope'] == 'ONE_SIDED_95_PAIRED_SAME_OPPORTUNITY' and
         protocol['method'] == METHOD and protocol['utility'] == UTILITY and src['method'] == METHOD and
         src['parent_hash'] == parent and src['candidate_hash'] == child == result['candidate_hash'] and
         src['protocol_hash'] == pre['protocol_hash'] == result['protocol_hash'] and
         src['source_commit'] == result['source_commit'] == receipt['source_after'] and result['fixture'] is False,
         "Q06 unsupported method or mismatched scope")
    targets = keyed(calendar['targets'])
    check_keys = list(targets)
    need(len(targets) >= 2 and calendar['candidate_hash'] == child and
         calendar['protocol_hash'] == pre['protocol_hash'] and
         digest(check_keys) == protocol['target_keys_sha256'], "Q06 unregistered or shortened calendar")
    names = [result['frozen_predictions_artifact'], src['parent_freezes_artifact'], result['authoritative_draws_artifact'],
             src['pair_settlements_artifact'], result['future_act_ledger_artifact'], protocol['lineage_artifact']]
    need(len({originals.hashes[n] for n in names}) == len(names), "Q06 originals alias")
    child_doc,parent_doc,draw_doc,settle_doc,act_doc,lineage = [originals.json(n) for n in names]
    need(originals.hashes[names[-1]] == protocol['lineage_sha256'] and lineage['child_hash'] == child and
         isinstance(lineage['parent_hashes'],list) and parent in lineage['parent_hashes'] and
         time(lineage['birth_committed_at']) <= time(pre['freeze_at']), "Q06 comparator is not a registered parent")
    for doc, subject in ((child_doc,child),(parent_doc,parent),(settle_doc,child),(act_doc,child)):
        valid_context(doc,subject,result)
    need(settle_doc['parent_hash']==parent and draw_doc['source_id']==pre['draw_source_id'] and
         draw_doc['fixture'] is False, "Q06 foreign draw or pair")
    a,b = keyed(child_doc['predictions']),keyed(parent_doc['predictions'])
    outcomes, draws, child_acts = keyed(settle_doc['pairs']),keyed(draw_doc['draws']),keyed(act_doc['settled_acts'])
    need(all(set(r)==set(targets) for r in (a,b,outcomes)) and set(targets)<=set(draws) and
         {k for k,r in a.items() if r['act'] is True}==set(child_acts), "Q06 missing, extra or discarded opportunities")
    mean, m2 = 0.0, 0.0
    for i,k in enumerate(check_keys,1):
        target, af, bf, d, out = targets[k],a[k],b[k],draws[k],outcomes[k]
        opened, end, drawn, settled = (time(t) for t in (target['opportunity_at'],target['deadline_at'],d['draw_at'],out['settled_at']))
        need(time(pre['freeze_at']) < opened and all(opened <= time(f['freeze_return_at']) < min(end,drawn) for f in (af,bf))
             and drawn <= settled <= time(receipt['window_end']), "Q06 late freeze, abstention or settlement")
        need(len(d['numbers'])==10 and all(type(v) is int for v in d['numbers']) and set(d['numbers'])==set(range(1,11)),
             "Q06 invalid draw")
        need([out[x] for x in ('child_freeze_sha256','parent_freeze_sha256','draw_sha256')]==
             [digest(af),digest(bf),digest(d)], "Q06 settlement bound to different inputs")
        ua,ha=utility(af,d)
        ub,hb=utility(bf,d)
        need(out['child_hit'] is ha and out['parent_hit'] is hb, "Q06 settlement disagrees with frozen action")
        if af['act']:
            raw=child_acts[k]
            need(raw['hit'] is ha and time(raw['freeze_return_at'])==time(af['freeze_return_at'])
                 and time(raw['draw_at'])==drawn and drawn<=time(raw['settled_at'])<=time(receipt['window_end']),
                 "Q06 child ACT differs from the natural ACT ledger")
        delta=ua-ub
        change=delta-mean
        mean+=change/i
        m2+=change*(delta-mean)
    n=len(targets)
    lcb=mean-1.6448536269514722*math.sqrt(m2/(n-1)/n)
    claimed=src['values']['paired_lcb_vs_parent']
    need(type(claimed) in (int,float) and math.isfinite(claimed) and math.isclose(claimed,lcb,rel_tol=1e-10,abs_tol=1e-10)
         and lcb>0, "Q06 recomputed paired bound differs or lacks improvement")
    return {'support':n,'paired_mean':mean,'paired_lcb_vs_parent':lcb}
