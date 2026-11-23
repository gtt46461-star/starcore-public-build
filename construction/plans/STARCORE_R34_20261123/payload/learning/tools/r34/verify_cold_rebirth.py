#!/usr/bin/env python3
"""Executes supplied Java pure contract and owner SQL in native SQLite.
Does not claim Android/scheduler/writer integration was executed.
"""
import argparse, hashlib, json, os, pathlib, re, sqlite3, subprocess, sys, tempfile

PAYLOAD = pathlib.Path(__file__).resolve().parents[2]
PACKAGE = pathlib.Path('app/src/main/java/com/ethk3/novaonepro/strategyv2')

def strings(path):
    return [json.loads(m.group(0)) for m in re.finditer(r'"(?:[^"\\]|\\.)*"',path.read_text())]

def constants(path):
    return {m.group(1):json.loads(m.group(2)) for m in re.finditer(r'public static final String (\w+) = ("(?:[^"\\]|\\.)*");',path.read_text())}

def digest(*parts):
    d=hashlib.sha256()
    for x in parts:
        b=x.encode();d.update(str(len(b)).encode()+b':'+b)
    return d.hexdigest()

def insert(db,sql,suffix=''):
    abi='a'*64;source='b'*64;campaign='child-campaign'+suffix;child='child-policy'+suffix
    db.execute(sql['INSERT_SOURCE'],(campaign,source,5095,b'new independent raw source','RUNNING',1,abi,1000))
    db.execute(sql['INSERT_CANDIDATE'],(campaign,child,'FRESH_PROGRAM','old-policy','R34_COLD_RETRAIN_ONLY_1'))
    return (campaign,child)

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source-root',type=pathlib.Path,default=PAYLOAD);parser.add_argument('--output',type=pathlib.Path);parser.add_argument('--crash-child',type=pathlib.Path)
    args=parser.parse_args();sql=constants(PAYLOAD/PACKAGE/'ColdRebirthContractR34.java')
    if args.crash_child:
        db=sqlite3.connect(args.crash_child,isolation_level=None);db.execute('BEGIN IMMEDIATE');insert(db,sql,'-killed');os._exit(74)
    out=args.output or PAYLOAD/'verification';out.mkdir(parents=True,exist_ok=True)
    result={'scope':'ACTUAL_JAVA_PURE_CONTRACT_AND_EXACT_OWNER_SQL_ON_NATIVE_SQLITE; NOT_ANDROID_OR_SCHEDULER_INTEGRATION','status':'FAILED_OR_INCOMPLETE','checks':[]}
    def check(value,label):
        if not value:raise AssertionError(label)
        result['checks'].append(label)
    with tempfile.TemporaryDirectory(prefix='r34-cold-') as temporary:
        work=pathlib.Path(temporary);classes=work/'classes';classes.mkdir()
        # This installed runtime exposes jdk.compiler but omits ct.sym; --release
        # is unavailable. Source/bytecode target 17, against the host JDK 21 APIs.
        # This is deliberately not an Android or Java 17 API compatibility claim.
        command=['java','com.sun.tools.javac.Main','-source','17','-target','17','-Xlint:-options','-encoding','UTF-8','-d',str(classes),str(PAYLOAD/PACKAGE/'ColdRebirthContractR34.java'),str(PAYLOAD/'tools/r34/ColdRebirthContractR34Test.java'),str(PAYLOAD/'tools/r34/JavaParseR34.java')]
        run=subprocess.run(command,capture_output=True,text=True);(out/'java-compile.log').write_text(run.stdout+run.stderr);run.check_returncode()
        parse_sources=sorted((PAYLOAD/PACKAGE).glob('ColdRebirth*R34.java'))+sorted((PAYLOAD/'integration_postimages').rglob('*.java'))
        syntax=subprocess.run(['java','-cp',str(classes),'JavaParseR34']+[str(p) for p in parse_sources],capture_output=True,text=True)
        (out/'java-syntax-only.log').write_text(syntax.stdout+syntax.stderr);syntax.check_returncode()
        result['syntax_only_files']=len(parse_sources)
        run=subprocess.run(['java','-cp',str(classes),'com.ethk3.novaonepro.strategyv2.ColdRebirthContractR34Test'],capture_output=True,text=True);(out/'java-contract.log').write_text(run.stdout+run.stderr);run.check_returncode()
        source=digest('R34_COLD_RETRAIN_ONLY_1','CANDIDATE','old-campaign','old-policy')
        check('sourceKey='+source in run.stdout,'Java/Python identity byte agreement')
        check('campaign='+digest('R34_COLD_RETRAIN_ONLY_1','CAMPAIGN',source,'a'*64,'b'*64) in run.stdout,'Java/Python campaign identity agreement')
        owner_sql=dict(line.split('=',1) for line in run.stdout.splitlines() if line.startswith(('protectCandidateSQL=','protectExpertSQL=','pruneSourceSQL=','pruneExpertSQL=')))
        runnable_sql=[s for s in strings(PAYLOAD/PACKAGE/'ColdRebirthR34.java') if s.startswith('SELECT count(*) FROM r34_cold_rebirth b JOIN r26_foundry f')]
        check(len(runnable_sql)==1,'actual runnable-admission query identified')
        path=work/'live.db';db=sqlite3.connect(path,isolation_level=None)
        base=strings(args.source_root/PACKAGE/'RuntimeSchemaR26.java')
        for table in ['r26_foundry','r26_candidate','r26_expert_experiment']:
            candidates=[s for s in base if s.startswith('CREATE TABLE IF NOT EXISTS '+table+'(')]
            check(len(candidates)==1,'exact 095 base DDL '+table);db.execute(candidates[0])
        ddls=[s for s in strings(PAYLOAD/PACKAGE/'ColdRebirthSchemaR34.java') if s.startswith('CREATE ')]
        check(len(ddls)==7,'all seven additive schema objects enumerated')
        for ddl in ddls:db.execute(ddl)
        for ddl in ddls:db.execute(ddl)
        for ddl in ddls:
            name=ddl.split(' ')[5].split('(',1)[0]
            actual=db.execute('SELECT sql FROM sqlite_master WHERE name=?',(name,)).fetchone()[0]
            check(actual.replace('IF NOT EXISTS ','')==ddl.replace('IF NOT EXISTS ',''),'exact persisted schema definition '+name)
        db.execute(sql['INSERT_SOURCE'],('old-campaign','c'*64,4095,b'old exact snapshot','ABANDONED_NUMERICAL_ABI',0,'d'*64,0))
        db.execute("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,cursor,state,state_hash,checkpoint_hash,dev_n,dev_k,val_n,val_k,status,operation) VALUES(?,?,?,100,?,?,?,70,25,30,10,'BLOCKED_WARM_NUMERICAL_ABI_PRESERVED','OLD')",('old-campaign','old-policy','OLD_PROGRAM',b'old learned weights','e'*64,'f'*64))
        before=db.execute("SELECT * FROM r26_candidate WHERE policy_hash='old-policy'").fetchall()
        db.execute('BEGIN IMMEDIATE');insert(db,sql,'-rollback');db.execute('ROLLBACK')
        check(db.execute("SELECT count(*) FROM r26_candidate WHERE policy_hash='child-policy-rollback'").fetchone()[0]==0,'failure before receipt rolls child back')
        check(db.execute("SELECT count(*) FROM r26_foundry WHERE campaign_id='child-campaign-rollback'").fetchone()[0]==0,'failure before receipt rolls source back')
        run=subprocess.run([sys.executable,__file__,'--crash-child',str(path)],capture_output=True,text=True)
        check(run.returncode==74,'actual separate process exited during transaction')
        check(db.execute("SELECT count(*) FROM r26_candidate WHERE policy_hash='child-policy-killed'").fetchone()[0]==0,'process death rolls candidate back')
        db.execute('BEGIN IMMEDIATE');campaign,child=insert(db,sql)
        receipt=('CANDIDATE','old-campaign','old-policy',source,'e'*64,'f'*64,100,'d'*64,'a'*64,campaign,child,'b'*64,1000,5095,'R34_COLD_RETRAIN_ONLY_1',1)
        db.execute(sql['INSERT_RECEIPT'],receipt);db.execute('COMMIT')
        check(db.execute("SELECT cursor,dev_n,dev_k,val_n,val_k,coalesce(length(state),0),length(checkpoint_hash) FROM r26_candidate WHERE policy_hash=?",(child,)).fetchone()==(0,0,0,0,0,0,0),'new candidate has exactly zero trained/selection state')
        check(db.execute("SELECT * FROM r26_candidate WHERE policy_hash='old-policy'").fetchall()==before,'old parent remains byte identical')
        for statement,params in [
            (sql['INSERT_RECEIPT'],receipt),
            ('UPDATE r34_cold_rebirth SET child_id=?',('other',)),
            ('DELETE FROM r34_cold_rebirth',()),
            ('INSERT OR REPLACE INTO r34_cold_rebirth SELECT * FROM r34_cold_rebirth',()),
            (sql['INSERT_RECEIPT'].replace('INSERT INTO','INSERT OR REPLACE INTO'),('CANDIDATE','different-parent-campaign','different-parent-id')+receipt[3:])]:
            try:db.execute(statement,params)
            except sqlite3.IntegrityError as e:check('R34_REBIRTH_RECEIPT_IMMUTABLE' in str(e),'duplicate/rewrite rejected: '+statement[:30])
            else:raise AssertionError('receipt mutated')
        check(db.execute('SELECT count(*) FROM r34_cold_rebirth').fetchone()[0]==1,'repeat cannot create another receipt')
        check(not any(r[0].startswith('r27_') for r in db.execute("SELECT name FROM sqlite_master WHERE type='table'")),'no natural/real/certificate table created or inherited')
        db.execute(sql['INSERT_SOURCE'],('expert-campaign','1'*64,5095,b'expert source','EXPERT_CONTINUATION_SOURCE',2,'a'*64,1000))
        db.execute(sql['INSERT_EXPERT'],('expert-child','E03','expert-campaign',b'new pair reference','2'*64,'a'*64,'{"inheritedEvidence":false}',2))
        check(db.execute("SELECT cursor,work_nanos,checkpoint_hash,parent_hash,result_json,status FROM r26_expert_experiment WHERE experiment_id='expert-child'").fetchone()==(0,0,'','','{}','RUNNING'),'new expert uses actual zero/default running columns')
        expert_source=digest('R34_COLD_RETRAIN_ONLY_1','EXPERT','old-expert-campaign','old-expert')
        expert_receipt=('EXPERT','old-expert-campaign','old-expert',expert_source,'e'*64,'f'*64,100,'d'*64,'a'*64,'expert-campaign','expert-child','1'*64,1000,5095,'R34_COLD_RETRAIN_ONLY_1',2)
        db.execute(sql['INSERT_RECEIPT'],expert_receipt)
        check(db.execute(runnable_sql[0],('a'*64,'a'*64)).fetchone()[0]==2,'capacity counts only the two actual ready children')
        for n in (3,4):db.execute(sql['INSERT_SOURCE'],('newer-'+str(n),'3'*64,6000,b'newer raw source','DONE',n,'a'*64,2000))
        db.execute(owner_sql['pruneSourceSQL'])
        check(db.execute("SELECT count(*) FROM r26_foundry WHERE campaign_id='expert-campaign'").fetchone()[0]==1,'actual prune SQL preserves active expert source without candidates')
        db.execute("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id='expert-campaign'")
        db.execute("UPDATE r26_expert_experiment SET status='EXPIRED_SOURCE_RETENTION' WHERE experiment_id='expert-child'")
        db.execute(owner_sql['pruneSourceSQL'])
        check(db.execute("SELECT length(snapshot) FROM r26_foundry WHERE campaign_id='expert-campaign'").fetchone()==(0,),'zero-trained recovery preserves source metadata but not expired raw bytes')
        check(db.execute('SELECT count(*) FROM r26_expert_experiment e WHERE '+owner_sql['protectExpertSQL']).fetchone()[0]==1,'expired zero-trained expert is a protected successor input')
        db.execute("DELETE FROM r26_foundry WHERE campaign_id='expert-campaign'")
        db.execute(owner_sql['pruneExpertSQL'])
        check(db.execute("SELECT count(*) FROM r26_expert_experiment WHERE experiment_id='expert-child'").fetchone()[0]==1,'actual retention SQL preserves zero child even if source was removed')
        db.execute("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id='child-campaign'")
        db.execute('DELETE FROM r26_candidate WHERE cursor=0 AND NOT '+owner_sql['protectCandidateSQL'])
        check(db.execute("SELECT count(*) FROM r26_candidate WHERE policy_hash='child-policy'").fetchone()[0]==1,'shared warm/foundry guard preserves expired zero-trained candidate')
        check(db.execute(runnable_sql[0],('a'*64,'a'*64)).fetchone()[0]==0,'expired/missing sources cannot deadlock admission as runnable children')
        db.execute('BEGIN IMMEDIATE')
        db.execute(sql['INSERT_SOURCE'],('expert-successor-campaign','4'*64,6095,b'new retained source','EXPERT_CONTINUATION_SOURCE',5,'a'*64,2000))
        db.execute(sql['INSERT_EXPERT'],('expert-successor','E03','expert-successor-campaign',b'fresh pair reference','5'*64,'a'*64,'{"inheritedEvidence":false}',5))
        successor_source=digest('R34_COLD_RETRAIN_ONLY_1','EXPERT','expert-campaign','expert-child')
        successor_receipt=('EXPERT','expert-campaign','expert-child',successor_source,'2'*64,'',0,'a'*64,'a'*64,'expert-successor-campaign','expert-successor','4'*64,2000,6095,'R34_COLD_RETRAIN_ONLY_1',5)
        db.execute(sql['INSERT_RECEIPT'],successor_receipt)
        db.execute("UPDATE r26_expert_experiment SET status='COLD_UNTRAINED_SUPERSEDED' WHERE experiment_id='expert-child' AND cursor=0 AND checkpoint_hash='' AND status='EXPIRED_SOURCE_RETENTION'")
        check(db.execute('SELECT changes()').fetchone()[0]==1,'zero-trained parent status CAS links a new source successor')
        db.execute('COMMIT')
        check(db.execute("SELECT source_cursor,source_abi=target_abi,child_id FROM r34_cold_rebirth WHERE source_id='expert-child'").fetchone()==(0,1,'expert-successor'),'same-ABI recovery is explicit fresh successor, not mutation of old receipt')
        check(db.execute("SELECT cursor,work_nanos,checkpoint_hash,result_json FROM r26_expert_experiment WHERE experiment_id='expert-successor'").fetchone()==(0,0,'','{}'),'successor again starts with zero training/evidence')
        db.execute(owner_sql['pruneExpertSQL'])
        check(db.execute("SELECT count(*) FROM r26_expert_experiment WHERE experiment_id='expert-child'").fetchone()[0]==0,'metadata guard releases only after successor receipt exists')
        check(db.execute("SELECT count(*) FROM r34_cold_rebirth WHERE child_id='expert-child'").fetchone()[0]==1,'original immutable birth receipt remains after safe metadata pruning')
        try:db.execute(sql['INSERT_RECEIPT'],successor_receipt)
        except sqlite3.IntegrityError:check(True,'one successor per expired child and target ABI')
        else:raise AssertionError('duplicate successor admitted')
        db.execute('SAVEPOINT capacity_fixture')
        for n in range(15):
            campaign,child=insert(db,sql,'-capacity-'+str(n))
            cap_source=digest('R34_COLD_RETRAIN_ONLY_1','CANDIDATE','capacity-parent',str(n))
            db.execute(sql['INSERT_RECEIPT'],('CANDIDATE','capacity-parent',str(n),cap_source,'e'*64,'f'*64,100,'d'*64,'a'*64,campaign,child,'b'*64,1000,5095,'R34_COLD_RETRAIN_ONLY_1',10+n))
        check(db.execute(runnable_sql[0],('a'*64,'a'*64)).fetchone()[0]==16,'actual bounded runnable queue reaches exactly sixteen')
        db.execute("UPDATE r26_foundry SET status='EXPIRED_RETENTION',snapshot=X'' WHERE campaign_id LIKE 'child-campaign-capacity-%' OR campaign_id='expert-successor-campaign'")
        check(db.execute(runnable_sql[0],('a'*64,'a'*64)).fetchone()[0]==0,'all-sixteen-expired case releases capacity for controlled successors')
        db.execute('ROLLBACK TO capacity_fixture');db.execute('RELEASE capacity_fixture')
        db.close()
        with sqlite3.connect(path) as reopened:
            check(reopened.execute('SELECT count(*) FROM r34_cold_rebirth').fetchone()[0]==3,'all immutable lineage receipts survive fresh connection')
    sources=sorted((PAYLOAD/PACKAGE).glob('ColdRebirth*R34.java'))
    result.update(status='PASS_LIMITED_HOST_SCOPE',source_sha256={str(p.relative_to(PAYLOAD)):hashlib.sha256(p.read_bytes()).hexdigest() for p in sources},checks_passed=len(result['checks']))
    (out/'cold_rebirth_verification.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps({'status':result['status'],'checks':len(result['checks']),'scope':result['scope']},ensure_ascii=False))
if __name__=='__main__':main()
