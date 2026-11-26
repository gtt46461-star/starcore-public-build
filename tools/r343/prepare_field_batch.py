#!/usr/bin/env python3
"""Pin a reviewed field plan and prepare one batch version; does not commit or sign."""
import argparse
from datetime import date, datetime
import hashlib
import json
from pathlib import Path
import re
import sys
from zoneinfo import ZoneInfo
sys.path.insert(1,str(Path(__file__).resolve().parents[1]/'r33'))
from verify_plan_binding import git,read,require,safe,sha
import build_identity as field
import advance_release_version as advance


def journal_preflight(repo,journal,batch,installed,highest):
    """Read-only wrapper validation; the original advance API owns all four writes."""
    journal=Path(journal).absolute()
    require(not journal.is_symlink() and not journal.resolve().is_relative_to(repo),
            'FRESH_EXTERNAL_VERSION_JOURNAL_REQUIRED')
    receipt=read(advance.regular(journal,'JOURNAL.json'))
    code=receipt.get('versionCode'); name=receipt.get('versionName','')
    require(receipt.get('schema')=='STARCORE_R335_VERSION_JOURNAL_1'
            and receipt.get('repo')==str(repo) and receipt.get('stage')==batch
            and type(code) is int and max(installed,highest,field.BASELINE_CODE)<code<=2100000000,
            'FIELD173_RESUME_VERSION_OR_BATCH_CONFLICT')
    match=re.fullmatch(r'R34\.2-'+re.escape(batch)+r'-(\d{4}-\d\d-\d\d)-'+str(code),name)
    require(match,'FIELD173_JOURNAL_RELEASE_SERIES_MISMATCH'); date.fromisoformat(match[1])
    rows=receipt.get('files',[])
    require(len(rows)==4 and tuple(row.get('path') for row in rows)==advance.FILES,
            'EXACT_FOUR_VERSION_FILES_REQUIRED')
    all_before=True; bodies=[]
    for row in rows:
        name=row['path']; target=advance.regular(repo,name)
        after=advance.regular(journal,'after/'+name).read_bytes()
        before=advance.regular(journal,'before/'+name).read_bytes()
        require(advance.sha(after)==row['afterSha256'] and advance.sha(before)==row['beforeSha256'],
                'JOURNAL_BYTES_MISMATCH')
        actual=advance.sha(target.read_bytes())
        require(actual in (row['beforeSha256'],row['afterSha256']),
                'IDENTITY_DIVERGED_RETAIN_USER_EDIT:'+name)
        all_before=all_before and actual==row['beforeSha256']; bodies.append(after)
    version=json.loads(bodies[0]); before_version=read(advance.regular(journal,'before/'+advance.FILES[0]))
    require(type(version.get('versionCode')) is int and version['versionCode']==code
            and version.get('versionName')==receipt['versionName']
            and version.get('applicationId')=='com.starcore.research.r13'
            and before_version.get('versionCode')==receipt.get('previousVersionCode'),
            'FIELD173_JOURNAL_MANIFEST_IDENTITY')
    # Validate the complete proposed four-file set before any binding/version write.
    # Use the original generator; this wrapper creates no alternate version format.
    generator=advance.generator(repo); digest=advance.sha(bodies[0])
    expected=(bodies[0],generator.prop_text(version,digest).encode(),
              generator.java_text(version,digest).encode(),generator.host_build_config_text(version,digest).encode())
    require(tuple(bodies)==expected,'FIELD173_JOURNAL_GENERATED_IDENTITIES_MISMATCH')
    return receipt,all_before


def apply_field_batch(repo,journal,pin,batch,installed,highest,day,resume=False):
    """Reuse the original lock, prepare and apply_locked; never discard user edits."""
    repo=Path(repo).resolve(); journal=Path(journal).absolute()
    require(re.fullmatch('B(?:0[1-9]|1[0-2])',batch)
            and type(installed) is int and installed>=field.BASELINE_CODE
            and type(highest) is int and highest>=field.BASELINE_CODE,
            'FIELD173_VERSION_OBSERVATIONS_REQUIRED')
    with advance.repo_version_lock(repo):
        location=safe(repo,field.BINDING)
        if location.exists(): require(read(location)==pin,'FIELD173_EXISTING_PIN_CONFLICT')
        current=read(advance.regular(repo,advance.FILES[0]))
        require(type(current['versionCode']) is int and current['versionCode']>=field.BASELINE_CODE,
                'FIELD173_VERSION_OBSERVATIONS_REQUIRED')
        if not resume:
            advance.prepare(repo,journal,installed,batch,day,'R34.2',highest)
        receipt,all_before=journal_preflight(repo,journal,batch,installed,highest)
        plan_record=journal/'FIELD173_PLAN.json'
        if plan_record.exists() or plan_record.is_symlink():
            require(read(advance.regular(journal,'FIELD173_PLAN.json'))==pin,
                    'FIELD173_JOURNAL_PLAN_MISMATCH')
        else:
            # Crash after prepare() sealed JOURNAL but before this wrapper sealed
            # its plan. Recovery is allowed only while every controlled identity
            # remains exactly BEFORE and there is no completed-apply claim.
            # A pre-existing identical pin from an earlier batch is valid.
            require(all_before and not (journal/'APPLIED.json').exists(),
                    'FIELD173_MISSING_PLAN_REQUIRES_UNAPPLIED_JOURNAL')
            advance.atomic(plan_record,(json.dumps(pin,ensure_ascii=False,indent=2)+'\n').encode())
        if not location.exists():
            advance.atomic(location,(json.dumps(pin,ensure_ascii=False,indent=2)+'\n').encode())
        result=advance.apply_locked(repo,journal)
        actual=read(advance.regular(repo,advance.FILES[0]))
        require(result['versionCode']==receipt['versionCode']==actual['versionCode']
                and result['versionName']==receipt['versionName']==actual['versionName'],
                'FIELD173_APPLIED_IDENTITY_MISMATCH')
        return result


def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--repo',type=Path,default=Path(__file__).resolve().parents[2])
    parser.add_argument('--plan',type=Path,required=True)
    parser.add_argument('--plan-commit',required=True)
    parser.add_argument('--batch',required=True)
    parser.add_argument('--installed-version',type=int,required=True)
    parser.add_argument('--highest-known-version-code',type=int,required=True)
    parser.add_argument('--journal',type=Path,required=True)
    parser.add_argument('--resume',action='store_true')
    args=parser.parse_args(); repo=args.repo.resolve()
    require(re.fullmatch('[0-9a-f]{40}',args.plan_commit) and re.fullmatch('B(?:0[1-9]|1[0-2])',args.batch),
            'FIELD173_PREPARATION_ARGUMENT')
    head=git(repo,'rev-parse','HEAD').decode().strip()
    git(repo,'merge-base','--is-ancestor',field.BASELINE,head)
    seal=sha(args.plan/'MANIFEST_SHA256.json'); field.verify_plan(args.plan,seal)
    raw=git(repo,'show',args.plan_commit+':'+field.PLAN_PATH+'/MANIFEST_SHA256.json')
    require(hashlib.sha256(raw).hexdigest()==seal,'FIELD173_GIT_PLAN_PIN_MISMATCH')
    # The complete plan is an explicit source input. Copy with git restore before this command.
    field.verify_plan(repo/field.PLAN_PATH,seal)
    pin={'schema':field.SCHEMA,'planCommit':args.plan_commit,'planSha256':seal,
         'sourceBaseline':field.BASELINE,'reportSha256':field.REPORT_SHA256}
    day=datetime.now(ZoneInfo('Asia/Shanghai')).date().isoformat()
    result=apply_field_batch(repo,args.journal,pin,args.batch,args.installed_version,
                             args.highest_known_version_code,day,args.resume)
    print('FIELD173_BATCH_PREPARED:'+args.batch+':'+str(result['versionCode'])+'; COMMIT_AND_CURRENT_TESTS_REQUIRED')


if __name__=='__main__':main()
