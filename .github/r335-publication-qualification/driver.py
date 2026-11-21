"""Reopen two original publication tests under their configured Android runners on the signed B08."""
import hashlib
import json
import os
import re
import subprocess
from pathlib import Path

base=Path(os.environ['RUNNER_TEMP'])
app=Path('app-source').resolve()
candidate=base/'protocol-candidate'
private=base/'qualification-replay-raw'
private.mkdir(exist_ok=True)
source='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
apk='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
pkg='com.starcore.research.r13'
cls='com.ethk3.novaonepro.strategyv2.R26PublicationDeviceTest'
names=('forgedCompletionCannotAllocateFutureQualification','completedCandidatePublishesBeforePeersAndFutureLabelRequiresTerminalFeedback')
files=('R26PublicationDeviceTest','R26RuntimeDeviceTest','DatabaseCoordinator','DbStore','DbHelper','FoundryCampaignR26','LivePoolR26','RuntimeSchemaR26','ModelStoreSU2','StrategySchemaSU2','R31FixtureSchemaOwners','R26CandidateCheckpoint','SQLiteConnection','SQLiteDatabase','Assertions','Assert')
codes={'FRESH_FIXTURE_OWNER':'Each fixture must start in a fresh instrumentation process','INSTRUMENTATION_FAILURE':'INSTRUMENTATION_FAILED','SQLITE_SCHEMA_MISSING':'no such table','SQLITE_COLUMN_MISSING':'no column named','SQLITE_NO_SUCH_COLUMN':'no such column','SQLITE_UNIQUE':'UNIQUE constraint','SQLITE_NOT_NULL':'NOT NULL constraint','SQLITE_FOREIGN_KEY':'FOREIGN KEY constraint','ASSERTION':'AssertionError','EXPECTED_ACTUAL':'expected:<','ILLEGAL_STATE':'IllegalStateException','CLASS_MISSING':'ClassNotFoundException','SQLITE_EXCEPTION':'SQLiteException','SQLITE_CONSTRAINT':'SQLiteConstraintException','MISSING_METHOD':'NoSuchMethod','SHORT_BLOCK':'ArrayIndexOutOfBoundsException'}
def adb(*a):
    return subprocess.check_output(['adb',*map(str,a)],stderr=subprocess.STDOUT,timeout=180).strip()
def sha(path):
    with path.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def inspect_text(text):
    known=[key for key,needle in codes.items() if needle in text]
    hits=[]
    for filename in files:
        for n in re.findall(r'\('+re.escape(filename)+r'\.java:(\d{1,7})\)',text):
            frame={'sourceFile':filename+'.java','line':int(n)}
            if frame not in hits and len(hits)<16:hits.append(frame)
    expected=[]
    pairs=re.findall(r'expected:<(-?[0-9]{1,12})> but was:<(-?[0-9]{1,12})>',text)
    for a,b in pairs[:2]:expected.append({'expected':int(a),'actual':int(b)})
    return dict(categories=known,sourceFrames=hits,integerAssertionValues=expected)
def main():
    assert subprocess.check_output(['git','-C',app,'rev-parse','HEAD'],text=True).strip()==source
    java=app/'app/src/androidTest/java'/(cls.replace('.','/')+'.java')
    target=next(candidate.glob('*-release.apk'))
    assert sha(target)==apk and adb('shell','getprop','ro.kernel.qemu')==b'1'
    assert b'Success' in adb('install','-r',target)
    installed=next(line[8:] for line in adb('shell','pm','path',pkg).decode().splitlines() if line.startswith('package:') and line.endswith('base.apk'))
    assert adb('shell','sha256sum',installed).decode().split()[0]==apk
    out=[]
    for mode,test_apk,runner in [('R30','instrument-androidTest.apk','com.ethk3.novaonepro.R30FaultAndroidRunner'),('SU1','original-su1-androidTest.apk','com.ethk3.novaonepro.strategyv2.Su1AndroidTestRunner')]:
        assert b'Success' in adb('install','-r','-t',candidate/test_apk)
        assert (pkg+'.test/'+runner).encode() in adb('shell','pm','list','instrumentation')
        for n,name in enumerate(names):
            assert re.search(r'@Test\s+public void\s+'+re.escape(name)+r'\s*\(',java.read_text())
            args=['adb','shell','am','instrument','-w','-r']
            if mode=='R30':args+=['-e','r30Sandbox','publication-replay-'+str(n)]
            args+=['-e','class',cls+'#'+name,pkg+'.test/'+runner]
            try:
                result=subprocess.run(args,capture_output=True,timeout=600);raw=result.stdout+b'\n'+result.stderr;exitcode=result.returncode
            except subprocess.TimeoutExpired as ex:
                raw=(ex.stdout or b'')+b'\n'+(ex.stderr or b'');exitcode=124
            (private/(mode+'-'+name+'.txt')).write_bytes(raw)
            text=raw.decode(errors='replace')
            passed=exitcode==0 and re.search(r'OK\s*\(1 test\)',text) is not None and 'FAILURES!!!' not in text
            out.append(dict(runner=mode,originalMethod=name,exit=exitcode,passed=passed,originalOutputSha256=hashlib.sha256(raw).hexdigest(),**inspect_text(text)))
    assert adb('shell','sha256sum',installed).decode().split()[0]==apk
    doc=dict(sourceCommit=source,apkSha256=apk,originalTestJavaSha256=sha(java),api=int(adb('shell','getprop','ro.build.version.sdk')),runId=int(os.environ['GITHUB_RUN_ID']),role='CONTROLLED_DISPOSABLE_ANDROID_ORIGINAL_PUBLICATION_TESTS',observations=out)
    (base/('R335-publication-qualification-'+str(doc['runId'])+'.json')).write_text(json.dumps(doc,indent=2)+'\n')
if __name__=='__main__':main()
