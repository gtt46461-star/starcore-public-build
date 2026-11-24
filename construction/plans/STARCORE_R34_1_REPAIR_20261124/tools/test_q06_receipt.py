"""Synthetic evidence tests of the offline Q06 receipt reader."""
from datetime import datetime, timedelta, timezone
import math
import unittest
from q06_receipt import digest, METHOD, UTILITY, verify_q06


class Originals:
    def __init__(self, files):
        self.files=files

    @property
    def hashes(self):
        return {k:digest(v) for k,v in self.files.items()}

    def json(self,k):
        return self.files[k]


def fixture():
    zero=datetime(2026,1,1,tzinfo=timezone.utc)
    def at(x):
        return (zero+timedelta(seconds=x)).isoformat()
    child, parent, source, study = 'a'*64,'b'*64,'c'*40,'d'*64
    context=dict(candidate_hash=child,source_commit=source,protocol_hash=study,role='PREREGISTERED_FUTURE',fixture=False)
    ca=[]; cf=[]; pf=[]; labels=[]; pairs=[]; acts=[]; deltas=[]
    for i in range(16):
        t=5*i+5
        k=str(i+1)
        ca.append(dict(target_key=k,opportunity_at=at(t),deadline_at=at(t+2)))
        def forecast(on,hit):
            f=dict(target_key=k,freeze_return_at=at(t+1),act=on)
            if on:
                f.update(position=1,numbers=[1,2,3] if hit else [2,3,4])
            else:
                f['reason']='SCORE_BELOW_THRESHOLD'
            return f
        a,b=forecast(i!=0, True),forecast(i%3==0,False)
        d=dict(target_key=k,draw_at=at(t+3),numbers=list(range(1,11)))
        ha=True if a['act'] else None
        hb=False if b['act'] else None
        s=dict(target_key=k,settled_at=at(t+4),child_hit=ha,parent_hit=hb,child_freeze_sha256=digest(a),
               parent_freeze_sha256=digest(b),draw_sha256=digest(d))
        cf.append(a);pf.append(b);labels.append(d);pairs.append(s)
        deltas.append((.70 if ha is True else 0)-(-.30 if hb is False else 0))
        if a['act']:
            acts.append(dict(target_key=k,freeze_return_at=a['freeze_return_at'],draw_at=d['draw_at'],settled_at=s['settled_at'],hit=ha))
    m=sum(deltas)/len(deltas)
    bound=m-1.6448536269514722*math.sqrt(sum((v-m)**2 for v in deltas)/(len(deltas)-1)/len(deltas))
    files=Originals({'child':dict(context,predictions=cf), 'parent':dict(context,candidate_hash=parent,predictions=pf),
                     'draws':dict(draws=labels,fixture=False,source_id='draw-feed'),
                     'settle':dict(context,pairs=pairs,parent_hash=parent),'acts':dict(context,settled_acts=acts),
                     'birth':dict(birth_committed_at=at(-1),child_hash=child,parent_hashes=[parent])})
    rule=dict(method=METHOD, utility=UTILITY, parent_hash=parent, target_keys_sha256=digest([r['target_key'] for r in ca]),
              lineage_artifact='birth',lineage_sha256=files.hashes['birth'])
    pre=dict(candidate_hash=child,protocol_hash=study,draw_source_id='draw-feed',freeze_at=at(0),q06_parent_comparison=rule)
    calendar=dict(candidate_hash=child,protocol_hash=study,targets=ca)
    receipt=dict(source_after=source,window_end=at(90))
    result=dict(context,frozen_predictions_artifact='child',authoritative_draws_artifact='draws',future_act_ledger_artifact='acts')
    comp=dict(context,values={'paired_lcb_vs_parent':bound},method=METHOD,parent_hash=parent,
              interval_scope='ONE_SIDED_95_PAIRED_SAME_OPPORTUNITY',parent_freezes_artifact='parent',pair_settlements_artifact='settle')
    return pre, calendar, receipt, result, comp, files


class Q06Tests(unittest.TestCase):
    def test_genuine_mixed_actions(self):
        out=verify_q06(*fixture())
        self.assertEqual(out['support'],16)
        self.assertGreater(out['paired_lcb_vs_parent'],0)

    def test_no_measured_advantage(self):
        pre,cal,rc,run,q,orig=fixture()
        files=orig.files
        for i,forecast in enumerate(files['child']['predictions']):
            match=dict(forecast)
            files['parent']['predictions'][i]=match
            record=files['settle']['pairs'][i]
            record['parent_hit']=record['child_hit']
            record['parent_freeze_sha256']=digest(match)
        q['values']['paired_lcb_vs_parent']=0
        with self.assertRaisesRegex(ValueError,'lacks improvement'):
            verify_q06(pre,cal,rc,run,q,orig)

    def test_no_summary_or_unlinked_original_can_pass(self):
        for fault in ['self_report','undefined_method','other_parent','discard_abstain','parent_late_abstain','wrong_draw',
                      'fake_settlement','negative_opportunities_cut','child_act_missing','duplicate','wrong_role','birth_after_registration',
                      'abstain_carries_act','false_scope','link_to_shared_parent_forecasts','shift_child_freeze']:
            with self.subTest(fault=fault):
                pre,cal,rc,run,q,orig=fixture()
                f=orig.files
                if fault=='self_report': q['values']['paired_lcb_vs_parent']=.99
                if fault=='undefined_method': pre['q06_parent_comparison']['method']='BEST_OF_METHODS'
                if fault=='other_parent':q['parent_hash']='e'*64
                if fault=='discard_abstain': f['parent']['predictions'].pop(1)
                if fault=='parent_late_abstain':
                    f['parent']['predictions'][1]['freeze_return_at']=cal['targets'][1]['deadline_at']
                    f['settle']['pairs'][1]['parent_freeze_sha256']=digest(f['parent']['predictions'][1])
                if fault=='wrong_draw': f['draws']['draws'][3]['numbers'].reverse()
                if fault=='fake_settlement': f['settle']['pairs'][1]['parent_hit']=False
                if fault=='negative_opportunities_cut':cal['targets'].pop(0)
                if fault=='child_act_missing':f['acts']['settled_acts'].pop()
                if fault=='duplicate':f['parent']['predictions'][1]['target_key']='1'
                if fault=='wrong_role':f['parent']['role']='TRAIN'
                if fault=='birth_after_registration':
                    f['birth']['birth_committed_at']=cal['targets'][1]['opportunity_at']
                    pre['q06_parent_comparison']['lineage_sha256']=orig.hashes['birth']
                if fault=='abstain_carries_act':
                    f['parent']['predictions'][1]['numbers']=[1,2,3]
                    f['settle']['pairs'][1]['parent_freeze_sha256']=digest(f['parent']['predictions'][1])
                if fault=='false_scope':run['fixture']=True
                if fault=='link_to_shared_parent_forecasts':q['parent_freezes_artifact']='child'
                if fault=='shift_child_freeze':f['acts']['settled_acts'][0]['freeze_return_at']=pre['freeze_at']
                with self.assertRaises((KeyError,ValueError)):
                    verify_q06(pre,cal,rc,run,q,orig)


if __name__=='__main__':
    unittest.main()
