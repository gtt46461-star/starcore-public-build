from pathlib import Path
from datetime import datetime,timezone
import hashlib,json,os,subprocess,time
root=Path('/workspace/r31-ba35-host-integration');work=Path('/tmp/r31-ba35-host-integration-mifthdj3')
matrix=json.loads((work/'MATRIX.json').read_text())
assert len(matrix['tests'])==24 and all(t['status']!='RUNNING' for t in matrix['tests']), 'WAIT_FOR_SEQUENTIAL_MATRIX_COMPLETION'
case=next(t for t in matrix['tests'] if t['name']=='r31-service_protocol')
assert case['exitCode']!=0 and 'java.net.SocketException: Operation not permitted' in (work/case['log']).read_text()
assert subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()==matrix['sourceCommit']
assert not subprocess.check_output(['git','diff','--name-only','--','app/src','tools','app/build.gradle'],cwd=root,text=True).strip(), 'NO_SOURCE_OR_TOOL_MUTATION_ALLOWED'
log=work/'logs/20-r31-service_protocol-attempt2-escalated.log'
assert not log.exists(), 'PRESERVE_EXISTING_RETRY_LOG'
env=os.environ.copy();env.update(matrix['dependencyEnvironment']);env['PYTHONUNBUFFERED']='1'
receipt={'name':'r31-service_protocol','attempt':2,'scope':'EXACT_SCRIPT_RETRY_AFTER_NATIVE_SOCKET_SANDBOX_DENIAL_NO_SOURCE_OR_TOOL_MUTATION','permission':'require_escalated','sourceCommit':matrix['sourceCommit'],'cwd':str(root),'command':case['command'],'dependencyEnvironment':matrix['dependencyEnvironment'],'startedAt':datetime.now(timezone.utc).isoformat(),'firstAttemptPreserved':case['log'],'firstAttemptLogSha256':case['logSha256']}
start=time.monotonic()
with log.open('wb') as output:run=subprocess.run(case['command'],cwd=root,env=env,stdout=output,stderr=subprocess.STDOUT)
receipt.update({'exitCode':run.returncode,'status':'HOST_PASS' if run.returncode==0 else 'HOST_FAILED','finishedAt':datetime.now(timezone.utc).isoformat(),'elapsedRealSeconds':round(time.monotonic()-start,3),'log':str(log.relative_to(work)),'logSha256':hashlib.sha256(log.read_bytes()).hexdigest(),'android':'NOT_RUN','naturalQuality':'NOT_PROVEN'})
(work/'SERVICE_SOCKET_RETRY.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
print('\n'.join(log.read_text(errors='replace').splitlines()[-25:]))
raise SystemExit(run.returncode)
