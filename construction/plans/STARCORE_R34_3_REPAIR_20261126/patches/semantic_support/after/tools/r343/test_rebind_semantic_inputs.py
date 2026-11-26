#!/usr/bin/env python3
"""Exercise exact cumulative PATCHSET identities using the original R967 verifier.

Requires the 173 Git object and sealed-plan source payloads; operates only inside
an external temporary directory. This is a host contract test, not an APK,
Android-device, statistical-quality or field-acceptance result.
"""
from pathlib import Path
import argparse, hashlib, json, os, shutil, subprocess, sys, tempfile
import rebind_semantic_inputs as r


def run(source, plan):
    source=Path(source).resolve();plan=Path(plan).resolve()
    patchset=r.unique(r.regular(plan,'PATCHSET.json').read_bytes())
    baseline=r.Baseline(source,patchset)
    original=r.reviewed_module(baseline.bodies['scripts/r967_semantic_identity.py'],source/'scripts/r967_semantic_identity.py','original_r967_test')
    cases=[];batches=[]
    def check(name, ok):
        if not ok:raise AssertionError(name)
        cases.append(name)
    def reject(name, action, code):
        try:action()
        except (ValueError, AssertionError) as error:
            check(name, code in str(error))
        else:raise AssertionError(name+': unexpectedly accepted')
    def write(root,name,raw):
        dst=root/name;dst.parent.mkdir(parents=True,exist_ok=True);dst.write_bytes(raw)
    with tempfile.TemporaryDirectory(prefix='field173-semantic-test-') as tmp:
        top=Path(tmp);repo=top/'repo';repo.mkdir()
        for name,raw in baseline.bodies.items():write(repo,name,raw)
        # This creates only a local lock owner, not a fabricated baseline commit.
        subprocess.run(['git','init','-q',str(repo)],check=True)
        initial=original.build(root=repo)
        check('exact_173_rule_matches_exported_report',initial['training_rule_sha256']=='f4ee2a3513d7e10608c99465d909ba7538aebc9d2a762685b2c490e36a110a55')
        check('new_leaves_12_existing_reviews_31',len(r.NEW_ROLES)==12 and len(r.REVIEWED_EXISTING)==31)
        check('tool_and_tests_not_R967_production_leaves',not any(p.startswith('tools/r343/') for p in original.controlled_paths(repo)))
        untouched=('release/strategy_su1_reviewed_inputs.json','release/r81_semantic_baseline.json','release/r81_controlled_source_baseline.json','release/research_gate_plan_r967.json')
        retained={name:r.sha((repo/name).read_bytes()) for name in untouched}
        generator=r.reviewed_module(baseline.bodies['scripts/r926_release_manifest.py'],repo/'scripts/r926_release_manifest.py','original_release_test')
        def set_version(batch,code=None):
            value=r.unique(baseline.bodies[r.VERSION_FILES[0]])
            value['versionCode']=code or 577311173+int(batch[1:]);value['versionName']='R34.2-'+batch+'-2026-11-26-'+str(value['versionCode'])
            body=r.encode(value);digest=r.sha(body)
            bodies=(body,generator.prop_text(value,digest).encode(),generator.java_text(value,digest).encode(),generator.host_build_config_text(value,digest).encode())
            for name,raw in zip(r.VERSION_FILES,bodies):write(repo,name,raw)
        def apply_source(batch):
            expected,states,selected=r.reviewed_states(plan,baseline,batch)
            for name,body in expected.items():
                if name not in r.OUTPUTS and name not in r.VERSION_FILES:write(repo,name,body)
            set_version(batch)
        def preserved():return retained=={name:r.sha((repo/name).read_bytes()) for name in untouched}
        apply_source('B01');pair,identity=r.evaluate(repo,plan,baseline,'B01')
        check('B01_tool_only_rule_unchanged',identity['trainingRuleSha256']==initial['training_rule_sha256'])
        check('B01_preserves_original_identity_bytes',pair==tuple(baseline.bodies[n] for n in r.OUTPUTS))
        r.apply_journal(repo,top/'journal-b01',pair,identity,'a'*64)
        r.apply_journal(repo,top/'journal-b01',pair,identity,'a'*64,resume=True)
        check('B01_idempotent_resume',tuple((repo/n).read_bytes() for n in r.OUTPUTS)==pair)
        batches.append({'batch':'B01','controlledFiles':identity['controlledFileCount'],'trainingRuleSha256':identity['trainingRuleSha256']})

        apply_source('B02')
        reject('existing_release_fails_without_rebind',lambda:original.build(root=repo),'R967_UNCLASSIFIED_OR_MISSING_PATHS')
        pair,identity=r.evaluate(repo,plan,baseline,'B02')
        modes={n:(repo/n).stat().st_mode & 0o777 for n in r.OUTPUTS}
        # Interrupt after the first durable identity replacement, not while
        # constructing a synthetic final receipt.
        original_atomic=r.atomic;writes=[]
        def interrupted(path,body):
            original_atomic(path,body)
            if str(path)==str(repo/r.OUTPUTS[0]):
                writes.append(str(path));raise OSError('SIMULATED_PROCESS_LOSS_AFTER_FIRST_REPLACE')
        r.atomic=interrupted
        try:
            try:r.apply_journal(repo,top/'journal-b02',pair,identity,'a'*64)
            except OSError as error:check('interruption_after_first_identity_write','SIMULATED_PROCESS_LOSS' in str(error))
            else:raise AssertionError('interruption_not_executed')
        finally:r.atomic=original_atomic
        check('partial_identity_has_no_APPLIED_claim',len(writes)==1 and not (top/'journal-b02/APPLIED.json').exists())
        reject('normal_preflight_rejects_partial_pair',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_CURRENT_IDENTITY_NOT_REVIEWED_PREFIX')
        recovered_pair,recovered_identity=r.evaluate(repo,plan,baseline,'B02',allow_mixed=True)
        r.apply_journal(repo,top/'journal-b02',recovered_pair,recovered_identity,'a'*64,resume=True)
        check('resume_matches_exact_two_outputs',tuple((repo/n).read_bytes() for n in r.OUTPUTS)==pair)
        check('identity_file_modes_preserved',modes=={n:(repo/n).stat().st_mode & 0o777 for n in r.OUTPUTS})
        checked=original.build(root=repo)
        check('original_verifier_accepts_B02',checked['training_rule_sha256']==identity['trainingRuleSha256'])
        check('prior_quality_not_inherited_B02',checked['compatible_predecessor_rules_sha256']==[])
        batches.append({'batch':'B02','controlledFiles':identity['controlledFileCount'],'trainingRuleSha256':identity['trainingRuleSha256']})

        def with_change(name,body,action):
            before=(repo/name).read_bytes();write(repo,name,body)
            try:action()
            finally:write(repo,name,before)
        probe=r.JAVA+'UnreviewedExtraR343.java';write(repo,probe,b'package com.ethk3.novaonepro; class UnreviewedExtraR343 {}\n')
        try:reject('unknown_production_leaf_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_CONTROLLED_SET_CHANGED')
        finally:(repo/probe).unlink()
        leaf=r.JAVA+'AutoFetchService.java'
        with_change(leaf,(repo/leaf).read_bytes()+b'\n',lambda:reject('reviewed_path_unreviewed_bytes_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_UNREVIEWED_SOURCE_BYTES'))
        with_change('release/research_gate_plan_r967.json',b'{}\n',lambda:reject('policy_change_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_UNREVIEWED_SOURCE_BYTES'))
        with_change('release/r81_controlled_source_baseline.json',b'{}\n',lambda:reject('immutable_R81_change_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_UNREVIEWED_SOURCE_BYTES'))
        manifest=r.unique((repo/r.OUTPUTS[0]).read_bytes());manifest['files'][0]['role']='PROVENANCE'
        with_change(r.OUTPUTS[0],r.encode(manifest),lambda:reject('existing_role_edit_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_CURRENT_IDENTITY_NOT_REVIEWED_PREFIX'))
        proof=r.unique((repo/r.OUTPUTS[1]).read_bytes());proof['predecessors']=['f'*64]
        with_change(r.OUTPUTS[1],r.encode(proof),lambda:reject('qualification_predecessor_edit_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_CURRENT_IDENTITY_NOT_REVIEWED_PREFIX'))
        with_change(r.VERSION_FILES[1],(repo/r.VERSION_FILES[1]).read_bytes()+b'\n',lambda:reject('four_version_files_must_match',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_FOUR_FILE_VERSION_MISMATCH'))
        with_change(r.VERSION_FILES[0],b'{"versionCode":577311175,"versionCode":577311176}\n',lambda:reject('duplicate_identity_key_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_DUPLICATE_JSON_KEY'))
        set_version('B02',577311173)
        reject('installed_173_version_cannot_ship',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_ADVANCED_VERSION_REQUIRED');set_version('B02')
        future=next(row for g in patchset['groups'] if g['id']=='prepared_storage' for row in g['files'] if row['action']=='ADD')
        group=next(g for g in patchset['groups'] if g['id']=='prepared_storage')
        write(repo,future['path'],(plan/group['full_source_directory']/future['path']).read_bytes())
        try:reject('future_batch_leaf_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'FIELD173_CONTROLLED_SET_CHANGED')
        finally:(repo/future['path']).unlink()
        saved=(repo/leaf).read_bytes();(repo/leaf).unlink();(repo/leaf).symlink_to(source/leaf)
        try:reject('controlled_symlink_rejected',lambda:r.evaluate(repo,plan,baseline,'B02'),'R967_UNREVIEWED_SYMLINK')
        finally:(repo/leaf).unlink();write(repo,leaf,saved)

        # A user edit, even with a syntactically sound persisted journal, is not
        # overwritten. A forged before snapshot must also be a reviewed prefix.
        with_change(r.OUTPUTS[0],b'{"user":"retained"}\n',lambda:reject('resume_retains_user_identity_edit',lambda:r.apply_journal(repo,top/'journal-b02',pair,identity,'a'*64,resume=True),'FIELD173_IDENTITY_DIVERGED_RETAIN_USER_EDIT'))
        journal_body=top/'journal-b02/after'/r.OUTPUTS[0];original_journal_body=journal_body.read_bytes();journal_body.write_bytes(original_journal_body+b'\n')
        try:reject('tampered_journal_body_rejected',lambda:r.apply_journal(repo,top/'journal-b02',pair,identity,'a'*64,resume=True),'FIELD173_JOURNAL_BODY_MISMATCH')
        finally:journal_body.write_bytes(original_journal_body)
        receipt_path=top/'journal-b02/JOURNAL.json';receipt_raw=receipt_path.read_bytes();receipt=r.unique(receipt_raw)
        before_path=top/'journal-b02/before'/r.OUTPUTS[0];before_raw=before_path.read_bytes();before_path.write_bytes(b'{}\n');receipt['files'][0]['beforeSha256']=r.sha(b'{}\n');receipt_path.write_bytes(r.encode(receipt))
        try:reject('forged_unreviewed_journal_before_rejected',lambda:r.apply_journal(repo,top/'journal-b02',pair,identity,'a'*64,resume=True),'FIELD173_JOURNAL_BEFORE_NOT_REVIEWED_PREFIX')
        finally:before_path.write_bytes(before_raw);receipt_path.write_bytes(receipt_raw)
        reject('journal_cannot_write_inside_repo',lambda:r.apply_journal(repo,repo/'journal',pair,identity,'a'*64),'FIELD173_EXTERNAL_JOURNAL_REQUIRED')
        # The exact existing lock blocks a second version/semantic writer.
        advance=r.reviewed_module(baseline.bodies['tools/r33/advance_release_version.py'],repo/'tools/r33/advance_release_version.py','original_lock_test')
        with advance.repo_version_lock(repo):
            def acquire():
                with advance.repo_version_lock(repo):pass
            reject('original_version_transaction_lock_shared',acquire,'VERSION_TRANSACTION_BUSY_RETRY_SAME_JOURNAL')
        prior_rule=identity['trainingRuleSha256']
        for number in range(3,13):
            batch=f'B{number:02d}';apply_source(batch);pair,identity=r.evaluate(repo,plan,baseline,batch)
            r.apply_journal(repo,top/('journal-'+batch.lower()),pair,identity,'a'*64)
            check(batch+'_original_verifier',original.build(root=repo)['training_rule_sha256']==identity['trainingRuleSha256'])
            check(batch+'_old_seals_and_policy_preserved',preserved())
            check(batch+'_no_qualification_inheritance',not identity['qualificationInheritance'] and not r.unique(pair[1])['predecessors'])
            if batch in ('B04','B08','B09','B11','B12'):check(batch+'_version_only_rule_stable',identity['trainingRuleSha256']==prior_rule)
            batches.append({'batch':batch,'controlledFiles':identity['controlledFileCount'],'trainingRuleSha256':identity['trainingRuleSha256']});prior_rule=identity['trainingRuleSha256']
        check('all_12_new_production_leaves_classified',set(identity['newProductionLeaves'])==set(r.NEW_ROLES))
        final_roles={row['path']:row['role'] for row in r.unique(pair[0])['files']}
        check('all_existing_1589_roles_unchanged',all(final_roles[name]==row['role'] for name,row in baseline.classified.items()))
        check('original_readonly_rule_verifier_remains_unchanged',(repo/'scripts/r967_semantic_identity.py').read_bytes()==baseline.bodies['scripts/r967_semantic_identity.py'])
        check('historical_SU1_and_R81_retained',preserved())
        set_version('B12',577311299);next_pair,next_identity=r.evaluate(repo,plan,baseline,'B12')
        check('newer_version_same_semantics_stable',next_pair==pair and next_identity['trainingRuleSha256']==prior_rule)
    return {'schema':'STARCORE_FIELD173_SEMANTIC_HOST_TEST_1','status':'PASS','assertionCount':len(cases),'cases':cases,'batches':batches,
            'scope':'ORIGINAL_R967_HOST_CONTRACT_ONLY','androidBuild':'NOT_RUN','deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'}


def run_cli(source,plan):
    """Exercise the real CLI with a local, explicitly test-only plan commit.

    The commit has the actual immutable 173 parent and complete sealed plan
    bytes. It is never pushed, shipped or described as user acceptance.
    """
    source=Path(source).resolve();plan=Path(plan).resolve()
    baseline=r.Baseline(source,r.unique((plan/'PATCHSET.json').read_bytes()))
    cases=[]
    def check(name,ok):
        if not ok:raise AssertionError(name)
        cases.append(name)
    with tempfile.TemporaryDirectory(prefix='field173-semantic-cli-test-') as tmp:
        top=Path(tmp);repo=top/'repo';repo.mkdir();external=top/'external-plan'
        shutil.copytree(plan,external,ignore=shutil.ignore_patterns('__pycache__','*.pyc','*.pyo'))
        expected,states,selected=r.reviewed_states(external,baseline,'B12')
        for name,raw in expected.items():
            leaf=repo/name;leaf.parent.mkdir(parents=True,exist_ok=True);leaf.write_bytes(raw)
        shutil.copytree(external,repo/r.PLAN_PATH)
        generator=r.reviewed_module(baseline.bodies['scripts/r926_release_manifest.py'],repo/'scripts/r926_release_manifest.py','cli_release_generator')
        release=r.unique(baseline.bodies[r.VERSION_FILES[0]]);release.update(versionCode=577311185,versionName='R34.2-B12-2026-11-26-577311185')
        body=r.encode(release);digest=r.sha(body)
        values=(body,generator.prop_text(release,digest).encode(),generator.java_text(release,digest).encode(),generator.host_build_config_text(release,digest).encode())
        for name,raw in zip(r.VERSION_FILES,values):(repo/name).write_bytes(raw)
        subprocess.run(['git','init','-q',str(repo)],check=True)
        objects=r.git(source,'rev-parse','--path-format=absolute','--git-path','objects').decode().strip()
        (repo/'.git/objects/info/alternates').write_text(objects+'\n')
        shallow=Path(r.git(source,'rev-parse','--path-format=absolute','--git-path','shallow').decode().strip())
        if shallow.is_file():shutil.copyfile(shallow,repo/'.git/shallow')
        r.git(repo,'update-ref','refs/heads/host-semantic-contract',r.BASE);r.git(repo,'symbolic-ref','HEAD','refs/heads/host-semantic-contract')
        r.git(repo,'read-tree',r.BASE);r.git(repo,'add','--force','--',r.PLAN_PATH);tree=r.git(repo,'write-tree').decode().strip()
        env=r.git_env();env.update(GIT_AUTHOR_NAME='Local host contract test',GIT_AUTHOR_EMAIL='host-test@example.invalid',GIT_COMMITTER_NAME='Local host contract test',GIT_COMMITTER_EMAIL='host-test@example.invalid')
        plan_commit=subprocess.run(['git','-c','core.hooksPath=/dev/null','-C',str(repo),'commit-tree',tree,'-p',r.BASE],
            input=b'TEST FIXTURE ONLY: semantic CLI contract\n',stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=env,check=True).stdout.decode().strip()
        r.git(repo,'update-ref','refs/heads/host-semantic-contract',plan_commit)
        seal=r.sha((external/'MANIFEST_SHA256.json').read_bytes())
        pin={'schema':'STARCORE_FIELD173_PLAN_BINDING_1','sourceBaseline':r.BASE,'planCommit':plan_commit,'planSha256':seal,'reportSha256':'3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86'}
        location=repo/'release/field173_plan_binding.json';location.write_bytes(r.encode(pin));pin_raw=location.read_bytes()
        command=[sys.executable,'-B',str(repo/'tools/r343/rebind_semantic_inputs.py'),'--repo',str(repo),'--plan',str(external),'--batch','B12']
        def invoke(args=()):return subprocess.run(command+list(args),stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=r.git_env(),timeout=120)
        before=tuple((repo/name).read_bytes() for name in r.OUTPUTS)
        preflight=invoke();check('cli_full_pinned_plan_preflight:'+preflight.stderr.decode('utf-8','replace')[-4000:] if preflight.returncode else 'cli_full_pinned_plan_preflight',preflight.returncode==0)
        check('cli_preflight_does_not_write',before==tuple((repo/name).read_bytes() for name in r.OUTPUTS))
        not_current=invoke(('--check',));check('cli_check_requires_current_generated_identity',not_current.returncode!=0 and b'FIELD173_CURRENT_SEMANTIC_REBIND_REQUIRED' in not_current.stderr)
        journal=top/'semantic-journal';applied=invoke(('--apply','--journal',str(journal)))
        check('cli_exact_two_identity_apply',applied.returncode==0)
        checked=invoke(('--check',));check('cli_current_identity_check',checked.returncode==0)
        resumed=invoke(('--resume','--journal',str(journal)));check('cli_idempotent_resume',resumed.returncode==0)
        check('cli_preserves_historical_SU1_review',(repo/'release/strategy_su1_reviewed_inputs.json').read_bytes()==baseline.bodies['release/strategy_su1_reviewed_inputs.json'])
        applied_bytes=tuple((repo/name).read_bytes() for name in r.OUTPUTS)
        bad=dict(pin,planSha256='0'*64);location.write_bytes(r.encode(bad))
        wrong=invoke();check('cli_wrong_seal_rejected',wrong.returncode!=0 and b'FIELD173_PLAN_BINDING' in wrong.stderr);location.write_bytes(pin_raw)
        member=repo/r.PLAN_PATH/'PATCHSET.json';member_raw=member.read_bytes();member.write_bytes(member_raw+b'\n')
        wrong=invoke();check('cli_local_plan_divergence_rejected',wrong.returncode!=0 and b'FIELD173_PLAN_MEMBER_MISMATCH' in wrong.stderr);member.write_bytes(member_raw)
        member=external/'PATCHSET.json';member_raw=member.read_bytes();member.write_bytes(member_raw+b'\n')
        wrong=invoke();check('cli_external_plan_divergence_rejected',wrong.returncode!=0 and b'FIELD173_PLAN_MEMBER_MISMATCH' in wrong.stderr);member.write_bytes(member_raw)
        check('cli_failed_preflights_preserve_output_bytes',applied_bytes==tuple((repo/name).read_bytes() for name in r.OUTPUTS))
    return {'status':'PASS','assertionCount':len(cases),'cases':cases,'gitCommitScope':'LOCAL_TEST_FIXTURE_ONLY_NOT_PUSHED','androidBuild':'NOT_RUN','deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'}


def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--source-repo',type=Path,required=True);parser.add_argument('--plan',type=Path,required=True);parser.add_argument('--out',type=Path);parser.add_argument('--cli-only',action='store_true')
    args=parser.parse_args();result=run_cli(args.source_repo,args.plan) if args.cli_only else run(args.source_repo,args.plan)
    if args.out:
        target=args.out.absolute();r.need(not target.resolve().is_relative_to(args.source_repo.resolve()) and not target.resolve().is_relative_to(args.plan.resolve()),'EXTERNAL_TEST_OUTPUT_REQUIRED')
        target.parent.mkdir(parents=True,exist_ok=True)
        with target.open('xb') as out:out.write(r.encode(result))
    print(json.dumps({'status':result['status'],'assertionCount':result['assertionCount'],'batchCount':len(result.get('batches',[])),
                      'androidBuild':'NOT_RUN','deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'},sort_keys=True))


if __name__=='__main__':main()
