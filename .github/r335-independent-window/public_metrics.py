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
commit='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
apk='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'
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
native_full=False
p=None
if native.is_file():
    with zipfile.ZipFile(native) as candidate:
        declared=json.loads(candidate.read('source-manifest.json'))
        flags=json.loads(candidate.read('R31_SEMANTIC_RECEIPT.json'))
        assert declared['sourceCommit']==commit and declared['apkSha256']==apk
        native_full=declared.get('rawEvidenceIncluded') is True and flags.get('rawEvidence') is True and 'snapshot.db' in candidate.namelist()
if native_full:
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
location=root/'r335-private-post-window'
meta=location/'measurement.json'
if p is None and meta.is_file():
    acquisition=read(meta)
    if acquisition['status']=='CONSISTENT_POST_WINDOW_DB':
        assert acquisition['sourceCommit']==commit and acquisition['apkSha256']==apk and acquisition['calendarSha256']==hashfile(cal)
        p=location/'measurements.sqlite';sh=hashfile(p)
        assert sh==acquisition['snapshotSha256'] and p.stat().st_size==acquisition['bytes']
        out['separatePostWindowSnapshot']={'sha256':sh,'bytes':int(p.stat().st_size),'acquisition':'STABLE_ANDROID_COPY_SQLITE_BACKUP_AFTER_END','nativeReport':False}
if p is not None:
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
            from measure_independent_returns import partial_clock_rows
            out['additionalObservedClocks']=partial_clock_rows(db,cal)
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
"""Calculate the independent assignment count from genuine, bracketed Android capture pairs."""
def registered_assignment_tally(raw, calendar, commit, apk):
    from periods import load_collector
    from collections import Counter
    from pathlib import Path
    import json
    reader = load_collector()
    if not calendar.is_file():
        return {'status':'MISSING_REGISTERED_CALENDAR'}
    doc = json.loads(calendar.read_bytes())
    paths = [raw / 'enable-start.json', raw / 'enable-end.json']
    if not all(path.is_file() for path in paths):
        return {'status':'INCOMPLETE_APP_CAPTURE_PAIR'}
    try:
        reader.require(doc['sourceCommit'] == commit and doc['apkSha256'] == apk and not reader.trace.calendar.validate_calendar(doc), 'CALENDAR_SOURCE')
        reader.require(all(entry['eligible'] is True for entry in doc['entries']), 'CALENDAR_FILTERED')
        first, initial_journal = reader.read_capture(paths[0], calendar, 'START')
        last, end_journal = reader.read_capture(paths[1], calendar, 'END')
        reader.require(all(x['captureSource'] == 'APP_OWNED' and x['sourceCommit'] == commit and x['apkSha256'] == apk for x in (first,last)), 'CAPTURE_SOURCE')
        reader.require(first['serial'] == last['serial'] and first['after']['wallMs'] <= doc['windowStartMs'] and last['before']['wallMs'] >= doc['windowEndMs'], 'CAPTURE_BOUNDARY')
        before, after = first['after'], last['before']
        clocks = before['bootId'] == after['bootId'] and abs((after['wallMs']-before['wallMs'])-(after['elapsedMs']-before['elapsedMs'])) <= 2000
        anchor = initial_journal['events'][-1]
        retained = {e['sequence']: e for e in end_journal['events']}
        reader.require(retained.get(anchor['sequence']) == anchor or (end_journal['dropped'] == anchor['sequence'] and end_journal['prefixHash'] == anchor['hash']), 'UNLINKED_ASSIGNMENT')
        later = [event for event in end_journal['events'] if event['sequence'] > anchor['sequence']]
        reader.require(all(event['sourceCommit'] == commit for event in [anchor,*later]), 'MIXED_ASSIGNMENT_SOURCE')
        tally = Counter()
        for entry in doc['entries']:
            state = reader.interval_enabled(anchor,later,max(doc['windowStartMs'],entry['deadlineAt']-60000),entry['deadlineAt']) if clocks else 'UNKNOWN_CLOCK'
            reader.require(state in ('ENABLED','EXPLICITLY_DISABLED','UNKNOWN_ASSIGNMENT','UNKNOWN_CLOCK'), 'BAD_STATE')
            tally[state] += 1
        return {'status':'VERIFIED_CAPTURE_PAIR','calendarCount':len(doc['entries']),'enabled':tally['ENABLED'],'explicitlyDisabled':tally['EXPLICITLY_DISABLED'],'unknown':tally['UNKNOWN_ASSIGNMENT']+tally['UNKNOWN_CLOCK'],'wallAndMonotonicContinuous':clocks,'expectedEnabledDue':len(doc['entries'])-tally['EXPLICITLY_DISABLED']}
    except (ValueError,KeyError,IndexError,OSError,TypeError):
        return {'status':'ORIGINAL_CAPTURE_OR_CHAIN_REFUSED'}

out['assignmentCaptureCounts']=registered_assignment_tally(raw,cal,commit,apk)
gate=raw/'window-result.json'
if gate.is_file():
    try:
        original=read(gate)
    except (ValueError, OSError):
        original={}
    out['reviewedWindowCheck']={'rawSha256':hashfile(gate),'exit':next((int(e['exit']) for e in events if e['kind']=='hundred-window-verifier'),None),'outcome':original.get('outcome') if original.get('outcome') in ('PASS','FAIL','INCOMPLETE') else None,'sourceBound':original.get('sourceCommit')==commit and original.get('apkSha256')==apk}
for phase in ('start','end'):
    capture=raw/('enable-'+phase+'.json')
    if capture.is_file():
        d=read(capture)
        assert d.get('phase')==phase.upper() and d.get('sourceCommit')==commit and d.get('apkSha256')==apk
        digest=d['preferences']['sha256']
        assert re.fullmatch('[0-9a-f]{64}',digest)
        out.setdefault('phaseReceipts',{})[phase]={'sha256':hashfile(capture),'calendarSha256':d['calendarSha256'],'projectionSha256':digest}
out['outcome']='MEASURED_ON_RUNNER' if 'periods' in out else 'INCOMPLETE'
path=root/('R335-independent-window-'+str(int(os.environ['GITHUB_RUN_ID']))+'.json')
path.write_text(json.dumps(out,indent=2)+'\n')
