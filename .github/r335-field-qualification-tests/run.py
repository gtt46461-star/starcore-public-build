"""Run the original qualification and authority tests against the already installed signed candidate."""
import datetime
import hashlib
import json
import os
import re
import subprocess
import time
from pathlib import Path

temp = Path(os.environ['RUNNER_TEMP'])
evidence = temp/'upgrade-evidence'
originals = temp/'qualification-test-originals'
originals.mkdir(exist_ok=True)
source = Path.cwd()
commit = subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
apk = (evidence/'target-source-apk-sha256.txt').read_text().strip()
installed = next(line[8:] for line in subprocess.check_output(['adb','shell','pm','path','com.starcore.research.r13'],text=True).splitlines() if line.startswith('package:') and line.endswith('/base.apk'))
def hash_installed():return subprocess.check_output(['adb','shell','sha256sum',installed],text=True).split()[0]
assert hash_installed() == apk
assert subprocess.check_output(['adb','shell','getprop','ro.kernel.qemu']).strip() == b'1'
cases = [
    ('SU2','strategyv2.R26EvidenceDeviceTest','completeSyntheticAggregateCanIssueAndReadExactlyOneBoundCertificate'),
    ('SU2','strategyv2.R26EvidenceDeviceTest','aggregateCalendarRejectsMissingOrRevisedBlocks'),
    ('SU2','strategyv2.R26PublicationDeviceTest','forgedCompletionCannotAllocateFutureQualification'),
    ('SU2','strategyv2.R26PublicationDeviceTest','completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback'),
    ('DECISION_GATE','R30DecisionChainAndroidTest','correctionRevokesAndNextAuthorityReadRejectsWithinFiveSeconds'),
    ('DECISION_GATE','R30DecisionChainAndroidTest','originalOwnerPositiveAndIndependentNegativeAuthorities'),
]
items=[]
for which,cls,method in cases:
    class_name='com.ethk3.novaonepro.'+cls
    original=source/'app/src/androidTest/java'/(class_name.replace('.','/')+'.java')
    assert re.search(r'@Test\s+public void\s+'+re.escape(method)+r'\s*\(',original.read_text())
    target='com.starcore.research.r13.test/com.ethk3.novaonepro.R30FaultAndroidRunner'
    start=time.time()
    command=['adb','shell','am','instrument','-w','-r','-e','r30Sandbox','r335-qualification-'+str(len(items)),
             '-e','class',class_name+'#'+method,target]
    timedout=False
    try:
        proc=subprocess.run(command,capture_output=True,timeout=600)
        raw=proc.stdout+b'\n'+proc.stderr;rc=proc.returncode
    except subprocess.TimeoutExpired as err:
        raw=(err.stdout or b'')+b'\n'+(err.stderr or b'');rc=124;timedout=True
    f=originals/(method+'.txt');f.write_bytes(raw)
    passed=rc==0 and re.search(rb'OK\s*\(1 test\)',raw) is not None and b'FAILURES!!!' not in raw
    items.append({'route':which,'class':class_name,'method':method,'directOriginalJavaSha256':hashlib.sha256(original.read_bytes()).hexdigest(),
                  'rawSha256':hashlib.sha256(raw).hexdigest(),'exit':rc,'oneNamedTestPassed':passed,'timeout':timedout,'elapsedMs':round((time.time()-start)*1000)})
doc={'sourceCommit':commit,'apkSha256':apk,'sameInstalledBytesAfter':hash_installed()==apk,'api':int(subprocess.check_output(['adb','shell','getprop','ro.build.version.sdk'])),
     'scope':'ORIGINAL_TESTS_CONTROLLED_INPUTS_NO_NATURAL_CERTIFICATE','cases':items,'passed':sum(i['oneNamedTestPassed'] for i in items),'total':len(cases)}
(evidence/'qualification-routes.json').write_text(json.dumps(doc,indent=2)+'\n')
print('Six original controlled qualification and authority tests',doc['passed'],'out of',doc['total'],flush=True)
