#!/usr/bin/env python3
"""Execute committed new-field cases and retain exact inputs, commands and runner logs."""
import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import uuid

ROOT=Path(__file__).resolve().parents[2]
sys.path.insert(1,str(ROOT/'tools/r33'))
import verify_plan_binding as binding
import field_delivery_contract as contract


def execute(repo, destination, build, apk):
    repo=Path(repo).resolve(); destination=Path(destination).resolve()
    field=contract.bound_field(repo,build=build)
    binding.require(field is not None and build.get('gradleRelease') == 'PASS'
                    and build.get('apkSha256') == binding.sha(apk), 'FIELD173_CURRENT_BUILD_REQUIRED')
    binding.require(not destination.is_relative_to(repo), 'FIELD173_EXTERNAL_ORIGINALS_REQUIRED')
    binding.verify_checkout_bytes(repo,field['sourceCommit'])
    cases,catalog_sha=contract.catalog(repo,field)
    specs,runner_raw=contract.runner_catalog(repo,field,cases)
    destination.mkdir(parents=True,exist_ok=True)
    binding.require(not (destination/contract.RECEIPT).exists(), 'FIELD173_FRESH_RECEIPT_REQUIRED')
    (destination/contract.RUNNER_ORIGINAL).write_bytes(runner_raw)
    observations={}
    def save(name,data):
        path=destination/name
        with path.open('xb') as out:out.write(data)
        return {'file':name,'sha256':contract.digest(data)}
    for case_id in sorted(cases):
        spec=specs[case_id]; stem='field173-'+case_id
        inputs={}; hashes={}
        for index,name in enumerate(spec['inputFiles']):
            data=contract.committed_bytes(repo,field['sourceCommit'],name)
            inputs[name]=save(stem+'-input-'+str(index)+'.bin',data); hashes[name]=contract.digest(data)
        context={'schema':'STARCORE_FIELD173_CASE_CONTEXT_1',
                 **contract.expected_scope(field,case_id,build['apkSha256'],build['ciRun']),
                 'executionId':uuid.uuid4().hex,'inputHashes':hashes}
        context_ref=save(stem+'-context.json',contract.canonical(context))
        argv_ref=save(stem+'-argv.json',contract.canonical(spec['argv']))
        env=dict(os.environ)
        output_manifest=destination/(stem+'-output-manifest.json')
        binding.require(not output_manifest.exists(), 'FIELD173_FRESH_CASE_OUTPUT_REQUIRED')
        env.update(STARCORE_FIELD173_CASE_CONTEXT=str(destination/context_ref['file']),
                   STARCORE_FIELD173_CANDIDATE_APK=str(Path(apk).resolve()),
                   STARCORE_FIELD173_CASE_OUTPUT=str(output_manifest))
        stdout_name=stem+'-stdout.log'; stderr_name=stem+'-stderr.log'
        with (destination/stdout_name).open('xb') as stdout,(destination/stderr_name).open('xb') as stderr:
            try:
                result=subprocess.run(spec['argv'],cwd=repo,env=env,stdout=stdout,stderr=stderr,
                                      timeout=spec['timeoutSeconds'],shell=False)
                code=result.returncode
            except subprocess.TimeoutExpired:
                code=124
        reader=contract.directory_reader(destination)
        out=reader(stdout_name); err=reader(stderr_name)
        binding.require(code == 0, 'FIELD173_RUNNER_NONZERO:'+case_id)
        count=contract.events(out,context,len(cases[case_id]['assertions']))
        output_ref={'file':output_manifest.name,'sha256':contract.digest(reader(output_manifest.name))}
        contract.verify_case_output(case_id,context,output_ref,reader)
        observations[case_id]={'argv':spec['argv'],'exitCode':code,'status':'PASS','assertionCount':count,
                              'inputOriginals':inputs,'contextOriginal':context_ref,'argvOriginal':argv_ref,
                              'outputManifestOriginal':output_ref,
                              'stdoutOriginal':{'file':stdout_name,'sha256':contract.digest(out)},
                              'stderrOriginal':{'file':stderr_name,'sha256':contract.digest(err)}}
    binding.verify_checkout_bytes(repo,field['sourceCommit'])
    binding.require(contract.bound_field(repo,build=build) == field, 'FIELD173_SOURCE_CHANGED_DURING_TEST')
    receipt={'schema':'STARCORE_FIELD173_BATCH_ENGINEERING_1','fieldRepair173':field,
             'sourceCommit':field['sourceCommit'],'sourceTree':field['sourceTree'],
             'planCommit':field['planCommit'],'planSha256':field['planSha256'],'batch':field['batch'],
             'buildRun':build['ciRun'],'apkSha256':build['apkSha256'],'catalogSha256':catalog_sha,
             'runnerManifestSha256':contract.digest(runner_raw),'status':'ENGINEERING_CHECKS_PASSED',
             'physicalDevice':'NOT_INFERRED','naturalQuality':'NOT_PROVEN',
             'pendingDeviceAndNatural':contract.pending_device_and_natural(repo),'cases':observations}
    contract.verify_receipt(repo,field,receipt,contract.directory_reader(destination),
                            apk_sha=build['apkSha256'],run_id=build['ciRun'])
    save(contract.RECEIPT,contract.canonical(receipt))
    return receipt


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--repo',type=Path,default=ROOT);p.add_argument('--out',type=Path,required=True)
    p.add_argument('--build-receipt',type=Path,required=True);p.add_argument('--apk',type=Path,required=True)
    a=p.parse_args(); result=execute(a.repo,a.out,binding.read(a.build_receipt),a.apk)
    print(json.dumps({'fieldEngineering':'PASS','cases':len(result['cases']),
                      'sourceCommit':result['sourceCommit'],'batch':result['batch'],
                      'physicalDevice':'NOT_INFERRED','naturalQuality':'NOT_PROVEN'}))


if __name__ == '__main__':main()
