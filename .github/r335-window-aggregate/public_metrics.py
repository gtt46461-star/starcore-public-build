"""Only aggregates, public scheduled periods, allow-listed codes and SHA-256 leave this machine."""
import hashlib,json,os,re,sqlite3,sys,zipfile
from pathlib import Path
from periods import period_observations
root=Path(os.environ['RUNNER_TEMP'])
raw=root/'report-originals/window'
report=root/'report-originals/B08/1'
digest=lambda x:hashlib.sha256(x).hexdigest()
def hashfile(p):
    with p.open('rb') as f:return hashlib.file_digest(f,'sha256').hexdigest()
def read(p):return json.loads(p.read_bytes())
commit='69eab914678bc988b71f2d7ce2287ccf89bd49eb'
apk='a8ce8f811af22cd29b14fe46774c32a12565b9a1e5b44b2c801c5412e71ae51f'
out={'sourceCommit':commit,'apkSha256':apk,'role':'DISPOSABLE_API34_ENGINEERING','runId':int(os.environ['GITHUB_RUN_ID'])}
codes={'READY','PROJECTION_INCOMPLETE','R335_ASSIGNMENT_UNREADABLE','R335_ASSIGNMENT_NOT_STABLE','R335_ASSIGNMENT_WRITE_IN_PROGRESS','R335_ASSIGNMENT_READ_ONLY_SHELL','R335_ASSIGNMENT_ACCESS','SecurityException','Permission Denial','Unknown URI','FileNotFoundException','No content provider','DeadObjectException','UNRECOGNIZED_PROVIDER_RESPONSE'}
obs=root/'upgrade-evidence/observations.jsonl'
events=[json.loads(i) for i in obs.read_text().splitlines()] if obs.exists() else []
out['readiness']=[v['category'] if v.get('category') in codes else 'UNKNOWN' for v in events if v.get('kind') in ('assignment-readiness','strict-start-unavailable')]
out['observedStartExit']=next((int(i['exit']) for i in events if i['kind']=='official-start-result'),None)
out['observedEndExit']=next((int(i['exit']) for i in events if i['kind']=='official-end'),None)
out['deviceElapsedThroughScheduledEnd']=any(i['kind']=='official-end' for i in events)
cal=raw/'calendar.json'
if cal.exists():
    v=read(cal);assert v['sourceCommit']==commit and v['apkSha256']==apk and len(v['entries'])==100
    out['calendar']={'sha256':hashfile(cal),'count':len(v['entries']),'eligible':sum(i['eligible'] is True for i in v['entries']),**{k:int(v[k]) for k in ('registeredAtMs','windowStartMs','windowEndMs')}}
for f in ['enable-start.json','enable-end.json','schedule.bin','trace.json']:
    p=raw/f
    if p.is_file():out.setdefault('artifactSha256',{})[f]=hashfile(p)
native=report/'UI_EXPORTED_FULL_REPORT.zip'
saf=report/'SAF_SAVED_FULL_REPORT.zip'
if native.is_file():
    with zipfile.ZipFile(native) as z:
        src=json.loads(z.read('source-manifest.json'));sem=json.loads(z.read('R31_SEMANTIC_RECEIPT.json'))
        assert src['sourceCommit']==commit and src['apkSha256']==apk and src['rawEvidenceIncluded'] is True and sem['rawEvidence'] is True
        assert not any('shared_prefs/' in n.lower() for n in z.namelist())
        assert re.fullmatch('[a-f0-9]{64}',sem['jobId'])
        snap=z.getinfo('snapshot.db')
        with z.open(snap) as f:sh=hashlib.file_digest(f,'sha256').hexdigest()
        assert sh==src['snapshotSha256'] and snap.file_size==src['snapshotBytes']
        out['fullReport']={'jobId':sem['jobId'],'nativeSha256':hashfile(native),'safReadback':saf.is_file() and hashfile(saf)==hashfile(native),'snapshotSha256':sh,'snapshotBytes':snap.file_size,'archiveCRC':z.testzip() is None,'sourceClosureVerified':None}
        formal=report/'strict-full-raw.json'
        if formal.is_file():out['fullReport']['sourceClosureVerified']=read(formal).get('sourceClosureVerified') is True
        p=raw/'snapshot.sqlite'
        if not p.exists():
            import shutil
            with z.open(snap) as f,p.open('xb') as sink:shutil.copyfileobj(f,sink,1<<20)
        assert hashfile(p)==sh
    with sqlite3.connect(p.resolve().as_uri()+'?mode=ro&immutable=1',uri=True) as db:
        db.row_factory=sqlite3.Row
        assert db.execute('PRAGMA integrity_check').fetchone()[0]=='ok'
        sources={item[0] for item in db.execute("SELECT name FROM sqlite_master WHERE type='table'")}
        eligible_tables=['draws','trades','r27_certificate','r27_live_frozen','su2_live_decision','su2_certificate','su2_mainline_binding']
        out['tableCounts']={k:int(db.execute('SELECT count(*) FROM '+k).fetchone()[0]) if k in sources else None for k in eligible_tables}
        if 'trades' in sources:
            row=db.execute("SELECT count(*),coalesce(sum(settled=1),0),coalesce(sum(settled=1 AND hit=1),0) FROM trades WHERE stat_scope='REAL_TRADE' AND action='BET'").fetchone()
            out['realTradesInSnapshot']=dict(zip(('acts','settled','hits'),map(int,row)))
        if 'r27_live_frozen' in sources:
            n=db.execute("SELECT max(n) FROM (SELECT count(*) n FROM r27_live_frozen WHERE act=1 AND status='FROZEN' AND frozen_at>0 AND frozen_at<deadline_at AND hit IN (0,1) AND settled_at>frozen_at GROUP BY policy_hash)").fetchone()[0]
            out['R27LargestCandidateSettledActs']=int(n or 0)
        if 'su2_live_decision' in sources:
            n=db.execute("SELECT max(n) FROM (SELECT count(DISTINCT sequence_ordinal) n FROM su2_live_decision WHERE frozen_at>0 AND frozen_at<target_open_time GROUP BY genome_key)").fetchone()[0]
            out['SU2LargestCandidateFrozenPeriods']=int(n or 0)
        if cal.is_file():
            result=period_observations(raw,commit,apk,db,sh)
            public=[]
            allowed={'FROZEN_ON_TIME','MISSED_PREDRAW','INVALID_OR_UNVERIFIABLE_FREEZE','UNKNOWN_FREEZE_SOURCE'}
            for a in result['rows']:
                target=str(a['target']);assert re.fullmatch('[0-9]{6,24}',target)
                states={'UNKNOWN_ASSIGNMENT_FROM_ORIGINALS','EXPLICITLY_DISABLED','FROZEN_ON_TIME','MISSED_PREDRAW','INVALID_OR_UNVERIFIABLE_FREEZE','UNKNOWN_CLOCK','UNKNOWN_ENABLE_REFUSED'}
                row={'target':target,'deadlineAt':int(a['deadlineAt']),'freeze':a['freeze'] if a['freeze'] in allowed else 'UNKNOWN','assignment':a['assignmentCoverage'] if a['assignmentCoverage'] in states else 'UNKNOWN','strictTrace':bool(a['canonicalClosure'])}
                # No app-provided text or individual policy/model identities are in this file.
                row['reasonCode']=a['reason'] if a['reason'] in ('NO_CANONICAL_FREEZE','FREEZE_CARDINALITY','SOURCE_DEADLINE_MISMATCH','CANONICAL_IDENTITY','LATE_FREEZE_CANNOT_REPAIR_OPPORTUNITY','LATE_OR_INVALID_PIT','FREEZE_HASH_OR_ACTION','') else 'SOURCE_JOIN_OR_IDENTITY_UNKNOWN'
                row['observedRows']={k:(int(v) if v is not None else None) for k,v in a['tableCounts'].items()}
                row['timingMs']={k:int(v) for k,v in a.get('elapsedFromOfficialOpenMs',{}).items()}
                public.append(row)
            out['periods']=public
            out['strictJoined']=result['strictJoined'];out['strictRealSettledActs']=result['joinedRealSettledActs'];out['strictRealHits']=result['joinedRealHits']
            if 'officialEnabledCounts' in result:out['enabledCounts']={k:int(v) for k,v in result['officialEnabledCounts'].items()}
            out['timing']={k:{'observed':v['observed'],'limitMs':v['limitMs'],'p95Ms':v['p95Ms'],'maxMs':v['maxMs'],'violations':len(v['violations'])} for k,v in result['timings'].items()}
out['outcome']='MEASURED_ON_RUNNER' if 'periods' in out else 'INCOMPLETE'
path=root/('R335-window-metrics-'+str(int(os.environ['GITHUB_RUN_ID']))+'.json')
path.write_text(json.dumps(out,indent=2)+'\n')
