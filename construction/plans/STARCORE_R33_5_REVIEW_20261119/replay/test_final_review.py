import contextlib
import copy
import subprocess
import io
import json
from pathlib import Path
import tempfile
import unittest
from types import SimpleNamespace
from unittest.mock import patch
import final_review as final


class Intake(unittest.TestCase):
    def test_old_score_alone_does_not_grant_review(self):
        with tempfile.TemporaryDirectory() as temp:
            old = Path(temp) / 'historical.json'
            old.write_text('{"engineeringGroups":{"verified":32,"denominator":32}}')
            output = io.StringIO()
            with contextlib.redirect_stdout(output):
                code = final.main(['--execution', str(old)])
            row = json.loads(output.getvalue())
            self.assertNotEqual(code, 0)
            self.assertIn('stage-receipt', row['missingInputs'])
            self.assertIn('deliveries', row['missingInputs'])
            self.assertEqual(row['checks'], {})
            self.assertEqual(row['status'], 'INCOMPLETE')

    def test_unrun_or_absent_trace_cannot_bind(self):
        with tempfile.TemporaryDirectory() as temp:
            for rows in ([], [{'id':'B08.2.TRACE','status':'NOT_RUN','proofs':[]}],
                          [{'id':'B08.2.TRACE','status':'PASS','proofs':[]}]):
                with self.assertRaises(ValueError):
                    final.proof_paths({'groups':[{'assertions':rows}]}, Path(temp), 'B08.2.TRACE')


class OriginalClosure(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.root = Path(temporary.name)

    def ref(self, name, obj):
        path = self.root / name
        path.write_text(json.dumps(obj))
        return {'path': name, 'sha256': final.sha(path)}

    def bundle(self, one_pending):
        envelope = {}
        score = {}
        for field, section, count in [('current222Ledger', 'currentOriginalClauses', 222),
                                      ('current1832Ledger', 'currentOriginalRequirements', 1832)]:
            rows = [{'id': str(i), 'status':'PASS', 'dispositionReason':'signed evidence'} for i in range(count)]
            if one_pending:
                rows[-1].update(status='BLOCKED', dispositionReason='awaiting device observation')
            envelope[field] = self.ref(field, {'rows':rows})
            score[section] = {'denominator':count, 'verified':count-int(one_pending)}
        proofs = [self.ref('envelope', envelope)]
        assertions = [{'id':'B08.3.'+role, 'status':'PASS', 'proofs':proofs} for role in ['POS','TRACE']]
        return {'groups':[{'assertions':assertions}]}, score

    def test_engineering_scope_and_incomplete_full_mode(self):
        exe, score = self.bundle(True)
        out = {'originalClosure':final.original_closure(exe, self.root, score)}
        final.record_completion(out, False)
        self.assertEqual(out['status'], 'VERIFIED_32_ENGINEERING_GROUPS_AND_REVIEW_CHECKS')
        final.record_completion(out, True)
        self.assertEqual(out['status'], 'INCOMPLETE_FULL_CLOSURE')
        self.assertEqual(out['originalClosure']['current1832Ledger']['pending'],
                         [{'id':'1831','status':'BLOCKED','reason':'awaiting device observation'}])

    def test_full_mode_accepts_all_observed_rows(self):
        exe, score = self.bundle(False)
        out = {'originalClosure':final.original_closure(exe, self.root, score)}
        final.record_completion(out, True)
        self.assertEqual(out['status'], 'VERIFIED_ENGINEERING_AND_ALL_ORIGINAL_LEDGERS')

    def test_summary_must_match_ledger(self):
        exe, score = self.bundle(True)
        score['currentOriginalClauses']['verified'] = 222
        with self.assertRaisesRegex(ValueError, 'CLOSURE_COUNT_DRIFT:current222Ledger'):
            final.original_closure(exe, self.root, score)

    def test_final_download_must_be_the_ninth_package(self):
        (self.root/'DELIVERY.json').write_text('original candidate')
        sources = dict(sourceCommit='a'*40, sourceTree='b'*40, sourceArchiveSha256='c'*64, gitBundleSha256='d'*64, receivedBuild={'downloadSha256':'9'*64})
        last = dict(sources, apkSha256='e'*64, deliveryZipSha256='9'*64, candidateReceiptSha256=final.sha(self.root/'DELIVERY.json'))
        release = {'stages':[last]}
        final.match_final_delivery(release, sources, sources['sourceCommit'], last['apkSha256'], self.root)
        for field in ('apkSha256','candidateReceiptSha256','sourceArchiveSha256','gitBundleSha256','sourceTree','sourceCommit','deliveryZipSha256'):
            with self.subTest(field=field), self.assertRaisesRegex(ValueError,'B08_DELIVERY_DIFFERS'):
                final.match_final_delivery({'stages':[dict(last, **{field:'0'*len(last[field])})]}, sources,
                                           sources['sourceCommit'], last['apkSha256'], self.root)


class FinalCommand(unittest.TestCase):
    """Exercise public command routing with explicit signed-build, Git and Android test boundaries."""
    setUp = OriginalClosure.setUp
    bundle = OriginalClosure.bundle
    ref = OriginalClosure.ref

    def test_full_cli_propagates_real_intake_arguments_and_partial_status(self):
        # Reuse just the ledger fixture. The gate-specific tests check the referenced data.
        exe, score = self.bundle(True)
        score['engineeringGroups'] = {'verified':32,'denominator':32}
        commit, apk, seal = 'a'*40, 'b'*64, 'c'*64
        exe.update(sourceCommit=commit,apkSha256=apk,versionCode=577311099,versionName='R33.5-B08-2026-11-20-577311099')
        source = dict(sourceCommit=commit,sourceTree='d'*40,sourceArchiveSha256='e'*64,gitBundleSha256='f'*64, receivedBuild={'downloadSha256':'9'*64})
        release = {'sourceCommit':commit,'apk':{'sha256':apk}}
        exe['finalBuild'] = {'releaseReceipt':self.ref('release-original.json',release)}
        trace = self.ref('trace.json',{'sample':'controlled'})
        chain = self.ref('chain.json',{'firstCase':{},'work':{},'nextCase':self.ref('next.json',{'commit':commit})})
        env_trace = self.ref('trace-envelope.json',{'rawTraceManifest':trace})
        env_chain = self.ref('chain-envelope.json',{'rawArtifacts':[chain]})
        for name,ref in [('B08.2.POS',env_trace),('B08.2.TRACE',env_trace),('B07.4.TRACE',env_chain)]:
            exe['groups'].append({'assertions':[{'id':name,'status':'PASS','proofs':[ref]}]})
        self.ref('execution',exe)
        self.ref('stage',{'stage':'B08','reviewManifestSha256':seal})
        plan = self.root/'plan'
        (plan/'verification').mkdir(parents=True)
        (plan/'verification/SHA256.json').write_text('{}')
        base_sha = final.sha(plan/'verification/SHA256.json')
        self.ref('DELIVERY.json',{'controlled':True})
        self.ref('raw',{})
        args = ['--plan',str(plan),'--repo',str(self.root),'--candidate',str(self.root),
                '--execution',str(self.root/'execution'),'--evidence',str(self.root),
                '--stage-receipt',str(self.root/'stage'),'--followup',str(self.root/'chain.json'),
                '--require-full-closure']
        for key in ('dumpsys','baseline-apk','aapt','apksigner','start','end','deliveries','delivery-zip'):
            args.extend(['--'+key,str(self.root/'raw')])
        def modules(path,name):
            return SimpleNamespace(SEAL=base_sha,verify=lambda review:seal)
        def git(command,text=False):
            return commit if 'rev-parse' in command else json.dumps(exe).encode()
        calls = {}
        def command(argv,**kwargs):
            script = Path(argv[2]).stem
            calls.setdefault(script,[]).append(argv)
            def output(key,doc):Path(argv[argv.index(key)+1]).write_text(json.dumps(doc))
            body = {}
            if script=='verify_delivery':
                output('--out',release)
                output('--source-out',source)
            if script=='verify_all_deliveries':
                output('--out',{'stages':[dict(source,apkSha256=apk,deliveryZipSha256='9'*64,candidateReceiptSha256=final.sha(self.root/'DELIVERY.json'))]})
            if script=='score_final':
                Path(argv[5]).write_text(json.dumps(score))
            if script=='check_research' and 'followup' in argv:
                body={'caseKey':'controlled','effect':'ADVANTAGE_UNPROVEN','reportWork':{'matched':True}}
            return SimpleNamespace(returncode=0,stdout=json.dumps(body).encode())
        output=io.StringIO()
        with patch.object(final,'load_module',side_effect=modules), patch.object(final,'verify_completed_source',return_value={}), \
             patch.object(final,'verify_b08_handoff'), patch.object(final.subprocess,'check_output',side_effect=git), \
             patch.object(final.subprocess,'run',side_effect=command),contextlib.redirect_stdout(output):
            status=final.main(args)
        result=json.loads(output.getvalue())
        self.assertEqual((status,result['status']),(1,'INCOMPLETE_FULL_CLOSURE'))
        self.assertTrue(result['researchFindings'][0]['reportWork']['matched'])
        joined=calls['check_research'][-1]
        self.assertEqual(joined[joined.index('--repo')+1],str(self.root))
        aggregate=calls['verify_all_deliveries'][0]
        self.assertEqual(aggregate[aggregate.index('--deliveries')+1],str(self.root/'raw'))
        self.assertIn('--delivery-zip',calls['verify_delivery'][0])


class HandoffHistory(unittest.TestCase):
    def setUp(self):
        temporary = tempfile.TemporaryDirectory()
        self.addCleanup(temporary.cleanup)
        self.repo = Path(temporary.name)
        self.git('init', '--quiet')
        self.git('config','user.name','host fixture')
        self.git('config','user.email','host@localhost')
        (self.repo/'release').mkdir()
        self.versions = []
        self.checks = []
        for index, tag in enumerate(['P00', *(f'B{i:02}' for i in range(1,9))]):
            path = self.repo/'release/release_manifest.json'
            path.write_text(json.dumps({'versionCode':index,'versionName':tag}))
            commit = self.commit(f'version {tag}')
            self.versions.append(dict(stage=tag,versionCode=index,commit=commit))
            self.checks.append(dict(stage=tag,commit=commit,versionCommit=commit,registeredProductionFiles=index))
            if tag == 'B07':
                (self.repo/'notes').write_text('committed followup after B07 release')
                self.handoff = self.commit('reviewed B07 input')
        self.complete = dict(completedVersions=self.versions,registeredPredecessorChecks=self.checks)
        self.stage = dict(sourceHead=self.handoff,
            orderedCommittedVersions=[*self.versions[:-2],dict(self.versions[-2],commit=self.handoff)],
            registeredPredecessorChecks=[*self.checks[:-2],dict(self.checks[-2],commit=self.handoff)])

    def git(self,*args):
        return subprocess.check_output(['git','-C',str(self.repo),*args],text=True).strip()

    def commit(self, message):
        self.git('add','.')
        self.git('commit','--quiet','-m',message)
        return self.git('rev-parse','HEAD')

    def test_handoff_with_intermediate_commits(self):
        final.verify_b08_handoff(self.repo,self.stage,self.complete)

    def test_any_ancestor_cannot_stand_in_for_the_transferred_head(self):
        bad = dict(self.stage,sourceHead=self.versions[0]['commit'])
        self.git('merge-base','--is-ancestor',bad['sourceHead'],self.versions[-1]['commit'])
        with self.assertRaisesRegex(ValueError,'WRONG_B07_VERSION'):
            final.verify_b08_handoff(self.repo,bad,self.complete)

    def test_b07_receipt_for_other_stage_lineage(self):
        bad = copy.deepcopy(self.stage)
        bad['orderedCommittedVersions'][2]['commit']=self.versions[3]['commit']
        with self.assertRaisesRegex(ValueError,'DIFFERS_FROM_NINE_STAGE_HISTORY'):
            final.verify_b08_handoff(self.repo,bad,self.complete)

    def test_registered_snapshot_counts_bound(self):
        bad = copy.deepcopy(self.stage)
        bad['registeredPredecessorChecks'][0]['registeredProductionFiles']+=1
        with self.assertRaisesRegex(ValueError,'DIFFERS_FROM_NINE_STAGE_HISTORY'):
            final.verify_b08_handoff(self.repo,bad,self.complete)


if __name__ == '__main__':
    unittest.main(verbosity=2)
