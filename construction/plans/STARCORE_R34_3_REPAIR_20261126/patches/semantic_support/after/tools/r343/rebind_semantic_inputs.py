#!/usr/bin/env python3
"""Rebind only reviewed 173/PATCHSET bytes to the unchanged R967/R82 contracts.

Default: read-only preflight. --apply writes exactly two current identity files
through an external journal. --resume accepts only that journal's before/after
bytes. It never changes policy, historical plans, source, qualification or APKs.
"""
from pathlib import Path
import argparse, contextlib, hashlib, importlib.util, json, os, re, shutil, subprocess, tempfile, types
from datetime import date

BASE = '4d1de9d83692635b5c5ce39144fece1c8a31c8ac'
PLAN_PATH = 'construction/plans/STARCORE_R34_3_REPAIR_20261126'
OUTPUTS = ('release/research_dependencies_r967.json', 'release/r82_training_invalidation.json')
VERSION_FILES = ('release/release_manifest.json', 'release/release_identity.properties',
 'app/src/main/java/com/ethk3/novaonepro/ReleaseManifestR926.java',
 'verification/r36-stubs/com/ethk3/novaonepro/BuildConfig.java')
JAVA = 'app/src/main/java/com/ethk3/novaonepro/'
# Explicit review of every new production leaf. Existing roles are never changed.
NEW_ROLES = {
 JAVA+'WriterFaultSnapshotViewR343.java': ('PRESENTATION', 'Read-only historical writer observation; cannot produce predictions, train models or grant qualification.'),
 JAVA+'strategyv2/StartupProbePolicyR343.java': ('NUMERIC', 'Bounded research startup admission affects which training work runs; preserve the conservative numeric identity boundary.'),
 JAVA+'strategyv2/StartupTempSchemaR343.java': ('NUMERIC', 'Exact research migration workspace contracts govern checkpoint admission and subsequent training startup.'),
 JAVA+'PreparedCanonicalAdmissionR343.java': ('NUMERIC', 'Physical growth admission gates canonical prediction persistence; no bypass of actual storage accounting.'),
 JAVA+'CoverageSettlementR343.java': ('PRESENTATION', 'Read-only settlement evidence join for reporting; does not modify freezes, outcomes or settlement production.'),
 JAVA+'r952/TrainingInfrastructurePolicyR343.java': ('NUMERIC', 'Typed infrastructure failure classification controls training retry and recovery state transitions.'),
 JAVA+'r952/TrainingRecoveryR343.java': ('NUMERIC', 'Evidence-bound successor recovery resumes original development training inputs without rewriting terminal originals.'),
 JAVA+'r952/RollingLearnerR343.java': ('NUMERIC', 'Incremental numerical training, checkpoints and learning-head compare-and-swap are part of the model rule.'),
 JAVA+'r952/RollingFrozenPublisherR343.java': ('NUMERIC', 'Immutable trained model publication and independent frozen evaluation identity are numerical development dependencies.'),
 JAVA+'r952/CreationChangeKindR343.java': ('NUMERIC', 'Typed structural change classification participates in the original evolution child transaction and retained lineage.'),
 JAVA+'strategyv2/ReportInlineBudgetR343.java': ('PRESENTATION', 'Bounded report projection allocation only; reserves graph identity visibility without changing producer data.'),
 JAVA+'strategyv2/ReportResearchConsumptionR343.java': ('PRESENTATION', 'Read-only original research receipt validation; separates consumption counts and grants no quality or REAL authority.'),
}
# Existing roles are retained verbatim. Each edited semantic leaf is reviewed
# separately; a future sealed patch still needs a new explicit entry here.
REVIEWED_EXISTING = {
 JAVA+'AutoFetchService.java': ('NUMERIC', 'Original background owner resumes research initialization through bounded persistent probes; fetch and main-chain authority stay with the same owner.'),
 JAVA+'DatabaseCoordinator.java': ('NUMERIC', 'Original database coordinator distinguishes CONTROL ticks from actual main-writer attempts, preserving transaction ownership and error evidence.'),
 JAVA+'MainWriterFaultProjectionR342.java': ('PRESENTATION', 'Historical writer projection delegates read-only snapshot display while preserving frozen evidence and marking missing independent time anchors UNKNOWN.'),
 JAVA+'ReportAttachmentClosureCP12.java': ('NUMERIC', 'Original report closure carries the bounded writer snapshot view; retain its existing conservative numeric classification without granting live authority.'),
 JAVA+'StartupStageJournalR341.java': ('NUMERIC', 'Durable bounded research-startup probe reservation and retention alter admission scheduling while preserving successful predecessor ownership.'),
 JAVA+'WriteBudgetR341.java': ('NUMERIC', 'Correct the persisted queue failure category and preserve the original timeout and single-writer resource budget.'),
 JAVA+'strategyv2/StartupCheckpointIdentityR31.java': ('NUMERIC', 'Checkpoint identity admits only the four owned exact temporary definitions and qualifies main schema reads, retaining shadow-table rejection.'),
 JAVA+'strategyv2/WriterHoldFaultsR31.java': ('NUMERIC', 'Original writer fault record distinguishes attempted work from control ticks and records phase timing without changing commit or readback receipts.'),
 JAVA+'PreparedCanonicalStoreR335.java': ('NUMERIC', 'Original prepared canonical persistence accounts for both atomic-file images, records save phases and preserves prior cache/source identity.'),
 JAVA+'StorageBudgetR31.java': ('NUMERIC', 'Physical growth allowance remains measured by the original storage budget, with exact canonical pending bytes passed to admission.'),
 JAVA+'V51RuntimeOrchestrator.java': ('NUMERIC', 'Original main-chain orchestrator emits canonical preparation stage observations and retains the existing unique prediction and settlement owner.'),
 JAVA+'RuntimeHealthLedgerR36.java': ('NUMERIC', 'Read-only coverage diagnostics join settlement evidence in the correct hash domain; retain the original numeric role and production freeze semantics.'),
 JAVA+'ResearchTaskQueueR967.java': ('NUMERIC', 'Original task queue separates infrastructure retry from deterministic failure and publishes qualified frozen evaluation successors only through existing typed transactions.'),
 JAVA+'r952/ModernFamilyFactoryCP12.java': ('NUMERIC', 'Existing family production isolates per-item failures so valid independently typed children can proceed through original admission contracts.'),
 JAVA+'r952/ModernSeedFactoryCP12.java': ('NUMERIC', 'Existing seed production preserves executable typed formula construction and bounded per-item failure isolation in the original foundry.'),
 JAVA+'r952/ResearchMigrationRegistryR952.java': ('NUMERIC', 'Original migration registry adds only the reviewed research recovery, learning and publication storage contracts under original migration ownership.'),
 JAVA+'r952/ResearchTrainingHookRegistryR952.java': ('NUMERIC', 'Original training hooks connect typed recovery and immutable rolling model publication to original task and receipt owners.'),
 JAVA+'DrawDependencyInputCP12.java': ('NUMERIC', 'Resolved draw input exposes the exact causally available dependency prefix needed by the existing learning owner without importing future labels.'),
 JAVA+'ReportDiagnosticEvidenceR64.java': ('NUMERIC', 'Original diagnostic exports add learning and frozen-publication evidence; preserve its conservative numeric classification and bounded export scope.'),
 JAVA+'ResearchCaptureQueueR946.java': ('NUMERIC', 'Original capture completion triggers learning work through the existing serialized queue and exact draw dependency identity.'),
 JAVA+'r952/BoundAtomLifecycleR80.java': ('NUMERIC', 'Existing bound-atom lifecycle feeds reviewed current development training without changing atom identity or granting historical quality.'),
 JAVA+'r952/DevelopmentProductionCP12.java': ('NUMERIC', 'Original development production schedules rolling work from validated inputs through existing quotas and original lifecycle ownership.'),
 JAVA+'r952/HistoricalTrainingReceiptCP12.java': ('NUMERIC', 'Original historical receipt binds the reviewed checkpoint and training rule so later publication retains exact causal and terminal evidence.'),
 JAVA+'r952/ResearchDirectorR952.java': ('NUMERIC', 'Original director routes typed rolling-training successors and frozen publications while retaining a single research task authority.'),
 JAVA+'FrozenCandidateAdapterR958.java': ('NUMERIC', 'Existing immutable candidate adapter consumes the exact reviewed frozen model and retained training identity, not a mutable learning head.'),
 JAVA+'GlobalResearchScheduleR932.java': ('NUMERIC', 'Original schedule accepts reviewed frozen candidates for independent evaluation under existing statistical gates and allocation ownership.'),
 JAVA+'ResearchEvaluationRunR967.java': ('NUMERIC', 'Existing evaluation run binds exact frozen model, source and training-rule identities without inheriting predecessor qualification.'),
 JAVA+'ResearchFunnelR967.java': ('PRESENTATION', 'Read-only funnel reports exact frozen publication stages and evidence without altering numerical acceptance or REAL authority.'),
 JAVA+'r952/EvolutionControllerR952.java': ('NUMERIC', 'Original evolution child transaction records typed structural change kind and retained lineage while preserving existing executable child admission.'),
 JAVA+'strategyv2/ReportObjectIndexR30.java': ('NUMERIC', 'Original object index distinguishes real produced, consumed, applied and rendered evidence; preserve its existing conservative numeric role.'),
 JAVA+'strategyv2/ReportSnapshotR30.java': ('NUMERIC', 'Original report snapshot exports actual input heads and source-bound consumption receipts within bounded inline allocation, retaining original numeric classification.'),
}


def need(ok, code):
    if not ok: raise ValueError(code)
def sha(body): return hashlib.sha256(body).hexdigest()
def encode(value): return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2)+'\n').encode()
def canonical(value): return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(',', ':'), allow_nan=False)
def unique(raw):
    def pairs(items):
        value={}
        for key,item in items:
            need(key not in value, 'FIELD173_DUPLICATE_JSON_KEY:'+key); value[key]=item
        return value
    return json.loads(raw, object_pairs_hook=pairs)
def regular(root, name):
    rel=Path(name);need(not rel.is_absolute() and '..' not in rel.parts, 'FIELD173_UNSAFE_PATH')
    path=Path(root)/rel
    for item in (path,*path.parents):
        if item==Path(root):break
        need(not item.is_symlink(), 'FIELD173_SYMLINK:'+name)
    need(path.is_file(), 'FIELD173_MISSING_FILE:'+name);return path
def git_env():
    env=dict(os.environ,GIT_NO_REPLACE_OBJECTS='1',GIT_OPTIONAL_LOCKS='0',GIT_TERMINAL_PROMPT='0')
    for key in ('GIT_DIR','GIT_WORK_TREE','GIT_COMMON_DIR','GIT_INDEX_FILE','GIT_OBJECT_DIRECTORY','GIT_ALTERNATE_OBJECT_DIRECTORIES','GIT_PREFIX'):env.pop(key,None)
    return env
def git(repo,*args): return subprocess.check_output(['git','-c','core.hooksPath=/dev/null','-C',str(repo),*args],stderr=subprocess.PIPE,env=git_env())
def module(path,name):
    spec=importlib.util.spec_from_file_location(name,path);value=importlib.util.module_from_spec(spec);spec.loader.exec_module(value);return value
def reviewed_module(raw,path,name):
    value=types.ModuleType(name);value.__file__=str(path)
    exec(compile(raw,str(path),'exec'),value.__dict__);return value


class Baseline:
    """Exact immutable commit bytes, independent of the current mutable manifests."""
    def __init__(self, repository, patchset):
        self.repository=Path(repository).resolve()
        self.manifest_raw=git(repository,'show',BASE+':'+OUTPUTS[0])
        self.manifest=unique(self.manifest_raw)
        need(self.manifest.get('revision')=='R967_EXPLICIT_DEPENDENCIES_V1','FIELD173_BASE_MANIFEST')
        self.classified={r['path']:r for r in self.manifest['files']}
        need(len(self.classified)==len(self.manifest['files']),'FIELD173_BASE_DUPLICATE_PATH')
        wanted=set(self.classified)|set(OUTPUTS)|set(VERSION_FILES)|{'tools/r33/advance_release_version.py'}
        for group in patchset['groups']:
            for row in group['files']:
                if row['before_sha256'] is not None:wanted.add(row['path'])
        names=sorted(wanted)
        for name in names:need('\n' not in name and '\r' not in name,'FIELD173_INVALID_BASE_PATH')
        raw=subprocess.run(['git','-c','core.hooksPath=/dev/null','-C',str(repository),'cat-file','--batch'],
            input=''.join(BASE+':'+p+'\n' for p in names).encode(),stdout=subprocess.PIPE,stderr=subprocess.PIPE,env=git_env(),check=True).stdout
        self.bodies={};offset=0
        for name in names:
            end=raw.index(b'\n',offset);header=raw[offset:end].split();offset=end+1
            need(len(header)==3 and header[1]==b'blob','FIELD173_BASE_BLOB_MISSING:'+name)
            size=int(header[2]);self.bodies[name]=raw[offset:offset+size];offset+=size
            need(raw[offset:offset+1]==b'\n','FIELD173_BASE_BATCH_TRUNCATED');offset+=1
        need(offset==len(raw),'FIELD173_BASE_BATCH_TRAILING')
        self.proof=unique(self.bodies[OUTPUTS[1]])
        self.old=unique(self.bodies[self.proof['controlled_baseline_file']])
        self.old={r['path']:r for r in self.old['files']}


def reviewed_states(plan, baseline, batch):
    patchset=unique(regular(plan,'PATCHSET.json').read_bytes())
    need(patchset.get('source_commit')==BASE,'FIELD173_PATCH_BASE')
    need(re.fullmatch(r'B(?:0[1-9]|1[0-2])',batch),'FIELD173_BATCH')
    bodies=dict(baseline.bodies);chain=dict(baseline.bodies);states=[dict(bodies)];selected=[];last_batch=0
    for group in patchset['groups']:
        number=int(group['batch'][1:]);need(number>=last_batch,'FIELD173_GROUP_BATCH_ORDER');last_batch=number
        take=number<=int(batch[1:])
        for row in group['files']:
            name=row['path'];prior=chain.get(name)
            need((sha(prior) if prior is not None else None)==row['before_sha256'], 'FIELD173_PATCH_CHAIN:'+name)
            need(row['action'] in ('ADD','MODIFY'),'FIELD173_UNREVIEWED_PATCH_ACTION')
            after=regular(plan,group['full_source_directory']+'/'+name).read_bytes()
            need(len(after)==row['after_bytes'] and sha(after)==row['after_sha256'],'FIELD173_PATCH_AFTER:'+name)
            chain[name]=after
            if take:bodies[name]=after
        if take:selected.append(group['id']);states.append(dict(bodies))
    return bodies,states,selected


def check_version(repo, baseline, batch):
    original=unique(baseline.bodies[VERSION_FILES[0]]);current=unique(regular(repo,VERSION_FILES[0]).read_bytes())
    need(type(current.get('versionCode')) is int and 577311173<current['versionCode']<=2100000000,'FIELD173_ADVANCED_VERSION_REQUIRED')
    match=re.fullmatch(r'R34\.2-'+batch+r'-(\d{4}-\d\d-\d\d)-'+str(current['versionCode']),current.get('versionName',''))
    need(match,'FIELD173_BATCH_VERSION');date.fromisoformat(match[1])
    need({k:v for k,v in current.items() if k not in ('versionName','versionCode')}==
         {k:v for k,v in original.items() if k not in ('versionName','versionCode')},'FIELD173_UNREVIEWED_RELEASE_RUNTIME_CHANGE')
    generator_path=regular(repo,'scripts/r926_release_manifest.py')
    need(generator_path.read_bytes()==baseline.bodies['scripts/r926_release_manifest.py'],'FIELD173_VERSION_GENERATOR_CHANGED')
    generator=module(generator_path,'field173_release_generator')
    body=regular(repo,VERSION_FILES[0]).read_bytes();digest=sha(body)
    expected=(body,generator.prop_text(current,digest).encode(),generator.java_text(current,digest).encode(),generator.host_build_config_text(current,digest).encode())
    for name,value in zip(VERSION_FILES,expected):need(regular(repo,name).read_bytes()==value,'FIELD173_FOUR_FILE_VERSION_MISMATCH:'+name)
    return current


def candidate_pair(bodies, baseline):
    manifest=unique(baseline.manifest_raw);classified={r['path']:r for r in manifest['files']}
    for name,raw in bodies.items():
        if name.startswith('app/src/main/') and name not in classified:
            need(name in NEW_ROLES,'FIELD173_NEW_PATH_REVIEW_REQUIRED:'+name)
            role,rationale=NEW_ROLES[name];classified[name]={'path':name,'role':role,'rationale':rationale}
    semantics=reviewed_module(baseline.bodies['scripts/r967_semantic_identity.py'],baseline.repository/'scripts/r967_semantic_identity.py','field173_semantics_schema')
    for name,row in classified.items():
        raw=bodies[name]
        if name in baseline.classified and raw!=baseline.bodies[name] and row['role']!='PROVENANCE':
            need(name in REVIEWED_EXISTING and REVIEWED_EXISTING[name][0]==row['role'], 'FIELD173_EDITED_LEAF_REVIEW_REQUIRED:'+name)
        if row['role'] in ('NUMERIC','BUILD_CONTRACT') and name.endswith('.java') and semantics.DYNAMIC.search(raw.decode('utf-8')):
            # This body was already matched to the baseline or an exact sealed patch.
            row['dynamic_review_sha256']=sha(raw)
    manifest['files']=[classified[name] for name in sorted(classified)]
    proof=unique(baseline.bodies[OUTPUTS[1]])
    prior_changes={r['path']:r for r in proof['controlled_changes']};changes=[]
    for name in sorted(set(baseline.old)|set(classified)):
        old=baseline.old.get(name,{});new=classified.get(name,{})
        br,ar=old.get('role',''),new.get('role','')
        if br=='PROVENANCE' and ar=='PROVENANCE' or not br and ar=='PROVENANCE':continue
        before=old.get('sha256','');after=sha(bodies[name]) if new else ''
        if (before,br)==(after,ar):continue
        prior=prior_changes.get(name)
        fields={'path':name,'before_sha256':before,'after_sha256':after,'before_role':br,'after_role':ar}
        if prior and all(prior.get(k)==v for k,v in fields.items()):changes.append(prior)
        else:
            review=NEW_ROLES.get(name) or REVIEWED_EXISTING.get(name)
            need(review is not None and review[0]==ar,'FIELD173_DIFF_REVIEW_REQUIRED:'+name)
            changes.append({**fields,'reviewer':'STARCORE R34.3-173 exact PATCHSET review','rationale':review[1]})
    old_reviews={r['path']:r for r in proof['new_role_reviews']};role_reviews=[]
    for name in sorted(set(classified)-set(baseline.old)):
        prior=old_reviews.get(name);role=classified[name]['role']
        if prior and prior['role']==role:role_reviews.append(prior)
        else:
            need(name in NEW_ROLES,'FIELD173_NEW_ROLE_REVIEW_REQUIRED:'+name)
            role_reviews.append({'path':name,'role':role,'reviewer':'STARCORE R34.3-173 explicit production leaf review','rationale':NEW_ROLES[name][1]})
    proof['controlled_changes']=changes;proof['new_role_reviews']=role_reviews;proof['predecessors']=[]
    proof['reviewed_input_provenance_sha256']=sha(canonical([[name,sha(bodies[name])] for name in sorted(classified) if classified[name]['role']!='PROVENANCE']).encode())
    release=unique(baseline.bodies[VERSION_FILES[0]])
    runtime={'applicationId':release['applicationId'],'gameCode':release['gameCode'],'databaseName':release['databaseName'],'databaseUserVersion':release['schema']['userVersion'],'minSdk':release['minSdk'],'runtimeContract':release['runtimeContract']}
    proof['runtime_contract_sha256']=sha(canonical(runtime).encode())
    proof['current_controlled_roles_sha256']=sha(canonical(sorted([name,row['role']] for name,row in classified.items())).encode())
    # Tool-only batches and the original four-file version update do not change
    # numerical semantics. Preserve exact original proof bytes in that case.
    return (baseline.manifest_raw if manifest==baseline.manifest else encode(manifest),
            baseline.bodies[OUTPUTS[1]] if proof==baseline.proof else encode(proof))


def evaluate(repo, plan, baseline, batch, allow_mixed=False):
    """Shared read-only evaluator. No identity file is changed on rejection."""
    repo=Path(repo).resolve();plan=Path(plan).resolve()
    expected,states,selected=reviewed_states(plan,baseline,batch)
    version=check_version(repo,baseline,batch)
    for name in VERSION_FILES:expected[name]=regular(repo,name).read_bytes()
    semantics_path=regular(repo,'scripts/r967_semantic_identity.py')
    need(semantics_path.read_bytes()==baseline.bodies['scripts/r967_semantic_identity.py'],'FIELD173_SEMANTIC_VERIFIER_CHANGED')
    semantics=module(semantics_path,'field173_actual_semantics')
    actual=semantics.controlled_paths(repo)
    required=set(baseline.classified)|{name for name in expected if name.startswith('app/src/main/')}
    need(actual==required,'FIELD173_CONTROLLED_SET_CHANGED:'+canonical({'new':sorted(actual-required),'missing':sorted(required-actual)}))
    for name in actual-set(OUTPUTS):
        need(regular(repo,name).read_bytes()==expected[name],'FIELD173_UNREVIEWED_SOURCE_BYTES:'+name)
    patchset=unique(regular(plan,'PATCHSET.json').read_bytes())
    for group in patchset['groups']:
        for row in group['files']:
            name=row['path'];raw=expected.get(name);target=repo/name
            if raw is None:need(not target.exists() and not target.is_symlink(),'FIELD173_LATER_GROUP_PRESENT:'+name)
            else:need(regular(repo,name).read_bytes()==raw,'FIELD173_PATCH_PREFIX_MISMATCH:'+name)
    pair=candidate_pair(expected,baseline)
    allowed={(sha(baseline.manifest_raw),sha(baseline.bodies[OUTPUTS[1]]))}
    for state in states:allowed.add(tuple(sha(x) for x in candidate_pair(state,baseline)))
    current=tuple(regular(repo,name).read_bytes() for name in OUTPUTS)
    if not allow_mixed:need(tuple(sha(x) for x in current) in allowed,'FIELD173_CURRENT_IDENTITY_NOT_REVIEWED_PREFIX')
    # Invoke the unchanged verifier against actual source bytes and proposed files
    # before any mutation. Ordinary files are hard-linked, outputs are fresh bytes.
    with tempfile.TemporaryDirectory(prefix='field173-semantic-check-') as temp:
        shadow=Path(temp)
        for name in sorted(actual|set(OUTPUTS)|{VERSION_FILES[0]}):
            dst=shadow/name;dst.parent.mkdir(parents=True,exist_ok=True)
            if name in OUTPUTS:dst.write_bytes(pair[OUTPUTS.index(name)])
            else:
                try:os.link(regular(repo,name),dst)
                except OSError:shutil.copyfile(regular(repo,name),dst)
        result=semantics.build(root=shadow)
    need(not result['compatible_predecessor_rules_sha256'],'FIELD173_QUALIFICATION_INHERITANCE_FORBIDDEN')
    return pair,{'schema':'STARCORE_FIELD173_SEMANTIC_PREFLIGHT_1','sourceBaseline':BASE,'batch':batch,
        'selectedPatchGroups':selected,'versionCode':version['versionCode'],
        'controlledFileCount':len(actual),'newProductionLeaves':sorted(actual-set(baseline.classified)),
        'trainingRuleSha256':result['training_rule_sha256'],'policyHash':result['policy_hash'],
        'reviewedIdentityPairsSha256':sorted([list(item) for item in allowed]),
        'qualificationInheritance':False,'historicalSu1Review':'PRESERVED_NOT_USED_AS_CURRENT_REBINDER',
        'androidBuild':'NOT_RUN','deviceAcceptance':'NOT_INFERRED','naturalQuality':'NOT_INFERRED'}


def atomic(path, body):
    path=Path(path);mode=path.stat().st_mode & 0o777 if path.exists() else 0o600
    fd,tmp=tempfile.mkstemp(prefix=path.name+'.',suffix='.partial',dir=path.parent)
    try:
        with os.fdopen(fd,'wb') as stream:stream.write(body);stream.flush();os.fsync(stream.fileno())
        os.chmod(tmp,mode);os.replace(tmp,path)
        fd=os.open(path.parent,os.O_RDONLY)
        try:os.fsync(fd)
        finally:os.close(fd)
    finally:
        if os.path.exists(tmp):os.unlink(tmp)


def apply_journal(repo, journal, pair, identity, plan_seal, resume=False):
    repo=Path(repo).resolve();journal=Path(journal).absolute()
    need(not journal.is_symlink() and not journal.resolve().is_relative_to(repo),'FIELD173_EXTERNAL_JOURNAL_REQUIRED')
    for parent in journal.parents:need(not parent.is_symlink(),'FIELD173_JOURNAL_SYMLINK')
    header={'schema':'STARCORE_FIELD173_SEMANTIC_JOURNAL_1','repo':str(repo),'sourceBaseline':BASE,'batch':identity['batch'],'versionCode':identity['versionCode'],'planSha256':plan_seal,'trainingRuleSha256':identity['trainingRuleSha256']}
    if not resume:
        need(not journal.exists(),'FIELD173_FRESH_JOURNAL_REQUIRED');journal.mkdir(parents=True,mode=0o700)
        rows=[]
        for name,after in zip(OUTPUTS,pair):
            before=regular(repo,name).read_bytes()
            for folder,body in (('before',before),('after',after)):
                p=journal/folder/name;p.parent.mkdir(parents=True,exist_ok=True);atomic(p,body)
            rows.append({'path':name,'beforeSha256':sha(before),'afterSha256':sha(after)})
        atomic(journal/'JOURNAL.json',encode({**header,'files':rows}))
    receipt=unique(regular(journal,'JOURNAL.json').read_bytes())
    need({k:receipt.get(k) for k in header}==header,'FIELD173_JOURNAL_IDENTITY_MISMATCH')
    rows=receipt.get('files',[]);need(len(rows)==2 and tuple(r.get('path') for r in rows)==OUTPUTS,'FIELD173_JOURNAL_EXACT_OUTPUTS')
    need([row.get('beforeSha256') for row in rows] in identity['reviewedIdentityPairsSha256'], 'FIELD173_JOURNAL_BEFORE_NOT_REVIEWED_PREFIX')
    # Validate every member and current state before the first replacement.
    for row,after in zip(rows,pair):
        name=row['path'];before=regular(journal,'before/'+name).read_bytes();saved=regular(journal,'after/'+name).read_bytes()
        need(sha(before)==row['beforeSha256'] and sha(saved)==row['afterSha256'] and saved==after,'FIELD173_JOURNAL_BODY_MISMATCH')
        need(sha(regular(repo,name).read_bytes()) in (row['beforeSha256'],row['afterSha256']),'FIELD173_IDENTITY_DIVERGED_RETAIN_USER_EDIT:'+name)
    for name,body in zip(OUTPUTS,pair):
        if regular(repo,name).read_bytes()!=body:atomic(repo/name,body)
    atomic(journal/'APPLIED.json',encode({**header,'status':'APPLIED_EXACT_CURRENT_IDENTITIES','outputs':[{'path':name,'sha256':sha(body)} for name,body in zip(OUTPUTS,pair)]}))


def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('--repo',type=Path,required=True);p.add_argument('--plan',type=Path,required=True);p.add_argument('--batch',required=True)
    p.add_argument('--journal',type=Path);m=p.add_mutually_exclusive_group();m.add_argument('--apply',action='store_true');m.add_argument('--resume',action='store_true');m.add_argument('--check',action='store_true')
    a=p.parse_args();repo=a.repo.resolve();plan=a.plan.resolve()
    need(not a.repo.is_symlink() and not a.plan.is_symlink(),'FIELD173_ROOT_SYMLINK')
    git(repo,'merge-base','--is-ancestor',BASE,'HEAD')
    # The published plan and local copy must be the exact already-pinned plan.
    pin=unique(regular(repo,'release/field173_plan_binding.json').read_bytes());seal=sha(regular(plan,'MANIFEST_SHA256.json').read_bytes())
    need(set(pin)=={'schema','planCommit','planSha256','sourceBaseline','reportSha256'} and
         pin.get('schema')=='STARCORE_FIELD173_PLAN_BINDING_1' and
         pin.get('reportSha256')=='3fb241b3181dccd4d149cf91471d86532919f77d943847ff8ce348cce5ec4d86' and
         pin.get('sourceBaseline')==BASE and pin.get('planSha256')==seal and re.fullmatch('[a-f0-9]{40}',pin.get('planCommit','')),'FIELD173_PLAN_BINDING')
    need(sha(git(repo,'show',pin['planCommit']+':'+PLAN_PATH+'/MANIFEST_SHA256.json'))==seal,'FIELD173_PUBLISHED_PLAN_PIN')
    import sys
    sys.path.insert(0,str(repo/'tools/r33'));sys.path.insert(0,str(repo/'tools/r343'))
    import build_identity
    build_identity.verify_plan(plan,seal);build_identity.verify_plan(repo/PLAN_PATH,seal)
    patchset=unique(regular(plan,'PATCHSET.json').read_bytes());baseline=Baseline(repo,patchset)
    lock_path=regular(repo,'tools/r33/advance_release_version.py')
    need(lock_path.read_bytes()==baseline.bodies['tools/r33/advance_release_version.py'],'FIELD173_ORIGINAL_VERSION_LOCK_CHANGED')
    advance=reviewed_module(baseline.bodies['tools/r33/advance_release_version.py'],lock_path,'field173_original_version_lock')
    # Use the same existing Git-administration lock as the four-file version
    # transaction. Source bytes are checked inside the lock and again after apply.
    with advance.repo_version_lock(repo) if a.apply or a.resume else contextlib.nullcontext():
        pair,result=evaluate(repo,plan,baseline,a.batch,allow_mixed=a.resume)
        if a.check:need(all(regular(repo,name).read_bytes()==body for name,body in zip(OUTPUTS,pair)),'FIELD173_CURRENT_SEMANTIC_REBIND_REQUIRED')
        if a.apply or a.resume:
            need(a.journal is not None,'FIELD173_EXTERNAL_JOURNAL_REQUIRED')
            need(not a.journal.resolve().is_relative_to(plan),'FIELD173_PLAN_IS_READ_ONLY')
            apply_journal(repo,a.journal,pair,result,seal,a.resume)
            evaluate(repo,plan,baseline,a.batch)
    result['status']='CURRENT_SEMANTIC_IDENTITIES_VERIFIED' if a.check or a.apply or a.resume else 'REVIEWED_REBIND_PREFLIGHT_PASSED'
    result['identityFiles']=[{'path':name,'sha256':sha(body)} for name,body in zip(OUTPUTS,pair)]
    print(json.dumps(result,ensure_ascii=False,sort_keys=True))

if __name__=='__main__':main()
