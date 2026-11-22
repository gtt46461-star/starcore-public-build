"""Count the released scheduler's task receipts, public fault sites and cursor steps during a normal import."""
from pathlib import Path
import re
import subprocess
import sys

original = Path('.github/r335-093-research-import/observe.py').resolve()
sys.path.insert(0, str(original.parent))
source = original.read_text()
cutoff = source.index('try:main()')
program = source[:cutoff].replace('R335-093-research-import-', 'R335-093-publication-cause-')
program = program.replace('(30,180,600,1200,2400,3600)', '(30,180,480,900)')
program = program.replace('if at in (600,2400,3600):read_panels()', 'if at==900:read_panels()')
namespace = {'__name__': 'original_research_observation'}
exec(compile(program, str(original), 'exec'), namespace)
query = namespace['query']
record = namespace['record']
original_sample = namespace['sample']
owners = ('r27-evolution', 'r27-replay-dataset', 'r27-atomnet', 'r27-chart-cache')
symbols = (
    'ACTIVE_SCHEMA_SESSION_TASK_LEASE_OR_RECOVERY','COOPERATIVE_PREPARED_STEP','DEPENDENCY(r27-replay-dataset)',
    'CORRECTED_REPLAY_DATASET','DRAINING_NOT_TERMINATED','RESULT_CLEANUP_PENDING','RUNNING','HALF_OPEN',
    'RESOURCE_WAIT','WRITER_HOLD_FAULT_BACKLOG','R31_COMPUTE_CAPACITY_WAIT','R26_UNSLICED_COMPUTATION_CAPACITY',
    'CP3_DB_BUSY_DEADLINE','CLAIM_WAIT','CLAIMED','READY','DRAIN','STALE','CANCEL','EXPIRE','RESEARCH',
    'QUARANTINE','UncertainCommit','StaleResearch','DatabaseLockedException','SQLiteException','Integrity','RetryLater',
    'ResearchStoreR30R1','ResearchInputPinR30R1','EvolutionPublicationStoreR31','EvolutionPublicationSqlR31',
    'BirthGateR27','FeatureCacheR27','RuntimeR27','CreationBudgetR31','CooperativeContinuationR26',
    'IllegalStateException','IllegalArgumentException','OutOfMemoryError','InterruptedException',
    'RuntimeCoordinatorR26.claimWithinR31','ResearchPoolR27.claimPrepared','revalidateResearchAdmissions','commitResearchAdmissions',
    'commitWithin','preparePublicationR31','submit','requireFence','CooperativeContinuationR26.resume','CONTINUATION',
    'NO_NEW_EVENT','EVOLUTION_STEP_','EVOLUTION_STAGING_','CLAIM','MEMORY','RESERV','LEASE','HEALTH')
source_dir = Path('app-source/app/src/main/java')
files = {f.name: f for f in source_dir.rglob('*.java')}
sources = {}
for name in ['RuntimeR27','RuntimeCoordinatorR26','CooperativeContinuationR26','ResearchPoolR27','ResearchStoreR30R1','BirthGateR27','EvolutionLoopR27','DatabaseCoordinator','ResearchWriteQueueR27','EvolutionPublicationStoreR31','PreparedTaskR30R1','ResearchInputPinR30R1','FeatureCacheR27','ReplayEvaluatorR27','ResearchContextR30R1','MemoryBudgetR31']:
    path = files.get(name+'.java')
    if path:
        sources[path.name] = len(path.read_text().splitlines())

symbols += ('BIND_CAPACITY_TOO_SMALL', 'BIND_SHAPE:', 'BLOCKED_PROTECTED_BYTES:', 'BLOCKED_PROTECTED_MANIFEST_DEBT', 'CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN:', 'INACTIVE_RECURSIVE_TRIGGERS_OFF_CHECKED', 'INSERT_SHAPE:', 'LIVE_TRANSITION_NOT_OWNER_EVOLUTION', 'LOCAL_NEW_EVOLUTION_OWNER_ENCODED_PAYLOAD_NOT_PHYSICAL_DB_OR_SHARED_GLOBAL_QUOTA', 'MISSING_INSERT_KEY:', 'NONCOMMUTING_OWNER_STATEMENT_CYCLE', 'NON_LITERAL_OWNER_SQL:', 'NON_POINT_UPDATE:', 'NON_TERMINAL_PROPOSAL_UPDATE', 'NULL_OR_BLOB_KEY', 'OWNER_LOGICAL_ENVELOPE:', 'OWNER_POINT_KEY_ARITY', 'OWNER_SQL_SHAPES:', 'PREPARED_MEMORY_ENVELOPE', 'PUBLICATION_BATCH_ENVELOPE', 'R31_EVOLUTION_CAPACITY:', 'R31_EVOLUTION_CURSOR_REQUIRED', 'R31_EVOLUTION_IMMUTABILITY_TRIGGER_MISMATCH:', 'R31_EVOLUTION_IMMUTABILITY_TRIGGER_MISSING:', 'R31_EVOLUTION_MAINTENANCE_OWNER', 'R31_EVOLUTION_MIGRATION_TRANSACTION_REQUIRED', 'R31_EVOLUTION_OWNER_INPUT', 'R31_EVOLUTION_OWNER_PLAN_REQUIRED:', 'R31_EVOLUTION_SNAPSHOT_CAPACITY', 'R31_EVOLUTION_STAGE_KEY_CORRUPT', 'R31_EVOLUTION_STALE_OWNER_OR_CURSOR', 'RECURSIVE_TRIGGERS_REQUIRE_EXPLICIT_OWNER_PLAN', 'SQL_LIST', 'STALE_OWNER', 'STATEFUL_UPDATE_REQUIRES_EXPLICIT_OWNER_PLAN:', 'TABLE_OR_PRIMARY_KEY:', 'UNBALANCED_SQL')

def numbers(statement):
    try:
        result = query(statement)
        if result == '':return []
        return [[int(n) for n in l.split('|')] for l in result.splitlines()]
    except (ValueError,subprocess.TimeoutExpired):
        return None

def aggregate_counts(expr, source):
    out={}
    for pos in range(0,len(symbols),24):
        batch=symbols[pos:pos+24]
        fields=','.join("coalesce(sum(case when instr("+expr+",'"+s+"')>0 then 1 else 0 end),0)" for s in batch)
        counts=numbers('select '+fields+' from '+source)
        if counts is not None and len(counts)==1:
            out.update({s:v for s,v in zip(batch,counts[0]) if v>0})
    return out

def threads():
    observed={}
    try:
        pid=subprocess.check_output(['adb','shell','pidof','-s',namespace['f'].PKG],text=True,timeout=10).strip()
        if not re.fullmatch(r'[1-9][0-9]*',pid):return {'count':0}
        trace=subprocess.run(['adb','shell','debuggerd','-j',pid],capture_output=True,text=True,timeout=15)
        target=None
        for line in trace.stdout.splitlines():
            match=re.search(r'^\s*"([^"]+)"',line)
            if match:
                name=match[1]
                target=name if name in ('starcore-r30-prepared-r27-evolution','starcore-r30-prepared-r27-replay-dataset','starcore-r27-research-0','starcore-r27-research-1') else None
                if target:observed.setdefault(target,[])
            elif target:
                frame=re.search(r'\(([A-Za-z0-9_]+\.java):(\d+)\)',line)
                if frame and 0<int(frame[2])<=sources.get(frame[1],0):
                    observed[target].append([frame[1],int(frame[2])])
        return {'commandExit':trace.returncode,'publicSourceLines':observed}
    except (subprocess.TimeoutExpired,ValueError):return {'sampleAvailable':False}

def sampled(elapsed):
    original_sample(elapsed)
    result={}
    for task in owners:
        pred=" where task_id='"+task+"'"
        result[task]={
            'taskFenceStepsCursorCpu':numbers('select fence,steps,cursor,last_cpu_ms from r26_task'+pred),
            'observedAttemptsCpuUnitsCommittedFailed':numbers('select attempts,work_cpu_ms,observed_work_units,task_receipts_committed,task_receipts_failed from r31_task_observation'+pred),
            'claimsLevelGenerationFaultRounds':numbers('select level,generation,fault_rounds from r31_task_control'+pred),
            'progressReceiptsMovedCpu':numbers('select count(*),coalesce(sum(cursor_after!=cursor_before),0),coalesce(sum(cpu_ms),0) from r26_progress'+pred),
            'latestBlockedSymbols':aggregate_counts("coalesce(blocked_reason,'')",'r26_task'+pred),
            'faultSymbols':aggregate_counts("coalesce(last_stack,'')||' '||coalesce(last_cause_chain_json,'')||' '||coalesce(phase,'')||' '||coalesce(category,'')",'r31_runtime_fault'+pred),
            'lastProgressSymbols':aggregate_counts("coalesce(blocked_reason,'')",'(select blocked_reason from r26_progress'+pred+' order by receipt_id desc limit 25)'),
        }
    row=record['observations'][-1]
    row['schedulerEvidence']=result
    row['evolutionCursor']=numbers('select generation,step from r27_evolution_cursor order by generation desc limit 3')
    row['sourceLines']=threads()
    # Return only locations in the exact public implementation and known table names.
    fault_rows=query("select coalesce(last_stack,'')||' '||coalesce(last_cause_chain_json,'') from r31_runtime_fault where task_id='r27-evolution' and (instr(last_stack,'EvolutionPublicationSqlR31')>0 or instr(last_cause_chain_json,'EvolutionPublicationSqlR31')>0) limit 24")
    sites={}
    for name,line in re.findall(r'\b(EvolutionPublicationSqlR31|EvolutionPublicationStoreR31|RuntimeR27)[.]java:(\d{1,5})\b',str(fault_rows)):
        key=name+':'+line
        sites[key]=sites.get(key,0)+1
    row['publicationFaultSites']=sites
    row['observedTablesMentioned']=[name for name in ('r27_policy','r27_lineage','r30r1_proposal','r30r1_consume','r27_replay_dataset','r27_evolution_cursor','r33_research_applied') if name in str(fault_rows)]
    # Only immutable identifiers from the published application schema are returned.
    # The fault text and database values stay in the disposable guest.
    diagnostic_tables = ('r26_evolution','r27_evolution_cursor','r27_expert_proposal','r27_policy','r27_lineage','r27_replay_score','r27_generation','r30r1_analysis_run','r30r1_proposal','r30r1_consume','r30_confirm_dataset','r30_frozen_confirm','r33_research_applied','r27_chart_cache','r27_eprocess','r27_holdout_exposure','r31_evolution_publication')
    fault = "coalesce(last_stack,'')||' '||coalesce(last_cause_chain_json,'')||' '||coalesce(stack,'')||' '||coalesce(cause_chain_json,'')"
    row['failedPublicationTargets'] = {}
    for table in diagnostic_tables:
        n = query("SELECT coalesce(sum(occurrences),0) FROM r31_runtime_fault WHERE task_id='r27-evolution' AND instr("+fault+",'CANONICAL_TRIGGER_REQUIRES_EXPLICIT_OWNER_PLAN:"+table+"')>0")
        if str(n).strip() not in ('0','', '[]', "['0']"):row['failedPublicationTargets'][table]=str(n)[:70]
    # Record the installed trigger identities on the actual published-source tables.
    # Hashes and known first operation tokens identify DDL, without disclosing any stored values.
    import hashlib
    table_set = ",".join("'"+t+"'" for t in diagnostic_tables)
    definitions=query("SELECT lower(tbl_name)||'|'||name||'|'||replace(replace(sql,char(10),' '),char(13),' ') FROM sqlite_master WHERE type='trigger' AND lower(tbl_name) IN ("+table_set+") ORDER BY tbl_name,name")
    selected=[]
    for definition in definitions.splitlines() if isinstance(definitions,str) else definitions:
        fields=definition.split('|',2)
        if len(fields)!=3:continue
        table,name,ddl=fields
        if table not in diagnostic_tables or not re.fullmatch(r'[A-Za-z0-9_]{1,120}',name):continue
        event=re.search(r'\b(?:BEFORE|AFTER)\s+(INSERT|UPDATE|DELETE)\b',ddl,re.I)
        selected.append({'table':table,'name':name,'ddlSha256':hashlib.sha256(ddl.encode()).hexdigest(),'event':event[1].upper() if event else 'OTHER'})
    row['knownTargetTriggerDefinitions']=selected
    row['cliConnectionRecursiveTriggersOnly']=numbers('PRAGMA recursive_triggers')
    namespace['save']()
namespace['sample']=sampled
try:
    namespace['main']()
except Exception as error:
    record.update(completed=False,failureType=type(error).__name__)
    namespace['save']()
    raise
