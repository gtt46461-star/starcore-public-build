#!/usr/bin/env python3
"""B02 evidence: whole-source role/call/DB/thread/exception/owner inventory.

Static evidence is deliberately labeled as static.  Catch blocks are parsed by balanced braces and
reported with line numbers so empty catches on business paths cannot be hidden by a broad heuristic.
"""
from pathlib import Path
import bisect,csv,hashlib,json,re,subprocess
ROOT=Path(__file__).resolve().parents[1]
JAVA=ROOT/'app/src/main/java/com/ethk3/novaonepro'
OUT=ROOT/'reports/r926/batch1'; OUT.mkdir(parents=True,exist_ok=True)

CRITICAL_METHODS={
    'AutoFetchService': {'runAnalysisRequestR36','prepareAnalysisHistoryR364','renewAnalysisLeaseOrThrowR365','postAnalysisForIssueR36','submitAnalysisRequestR36'},
    'DatabaseCoordinator': {'ensureCriticalSchemaOrThrow','runWrite','runTransaction','runPredictionTransaction','runSettlementTransaction','runCanonicalTransactionR36','runSettlementTransactionR36','runSettlementCoreTransactionR911','runDeferredTransactionR36','runR36Transaction','runExistingR36Transaction'},
    'DbHelper': {'ensureR811CriticalSchema','ensureR815CriticalSchema','ensureR906CriticalSchema'},
    'DbStore': {'saveDraw','insertDrawInternal','saveDecision','saveDecisionInternal'},
    'AnalysisRequestStoreR36': {'ensurePending','rearmUpgradeFailureR366','rearmKnownR363PreCommitFailureR364','claimDetailed','claim','markRetry','markExpired','markFailed','transitionOwned'},
    'SettlementEngine': {'settleAfterDrawSaved','executeFromCoordinatorR815'},
    'V51Repository': {'commitCanonicalBundleR36','commitCanonicalBundleVerifiedR368'},
    'DeferredEvidenceWriterR36': {'writeOneInternal','complete','failOrRetry'},
    'LearningEngine': {'learnAfterDraw','learnByTargetIssue','learnTrade'},
    'RuntimeEventLedgerR926': {'appendWithinTransaction','append','drainOutbox','reconcile','reconcileStartupOnce','rebuildIssue','applyProjection'},
    'SchemaUpgradeCoordinatorR815': {'prepareAndVerify','restoreVerifiedBackup','verifyBackupFile'},
    'RuntimeRecoveryCoordinatorR926': {'record','recordMessage'}
}
ACCEPTED_CRITICAL_CATCH_KINDS={'RETHROW','RETRY','DIAGNOSTIC','DEGRADE','INTENTIONAL_CLEANUP_IGNORE'}

def find_matching_brace(text, open_pos):
    depth=0; quote=None; esc=False; i=open_pos
    while i<len(text):
        ch=text[i]
        if quote:
            if esc: esc=False
            elif ch=='\\': esc=True
            elif ch==quote: quote=None
        else:
            if ch in ('"',"'"): quote=ch
            elif ch=='{': depth+=1
            elif ch=='}':
                depth-=1
                if depth==0: return i
        i+=1
    return len(text)-1

def method_spans(text):
    # Named Java methods only. Anonymous run() methods are intentionally excluded unless explicitly named in CRITICAL_METHODS.
    pat=re.compile(r'(?m)^\s*(?:(?:public|private|protected)\s+)?(?:static\s+)?(?:final\s+)?[\w<>\[\], ?.$]+?\s+([A-Za-z_$][\w$]*)\s*\([^;{}]*?\)\s*(?:throws\s+[^\{]+)?\{')
    nls=[i for i,ch in enumerate(text) if ch=='\n']; out=[]
    for m in pat.finditer(text):
        op=text.find('{',m.start(),m.end()+1)
        if op<0: continue
        cl=find_matching_brace(text,op)
        out.append({'name':m.group(1),'start':m.start(),'open':op,'end':cl+1,'line':bisect.bisect_right(nls,m.start())+1})
    return out

def baseline_inventory():
    try:
        base=''
        for marker in ('R925 complete source baseline','R925 engineering upgrade'):
            base=subprocess.check_output(['git','rev-list','--all','--grep='+marker,'--max-count=1'],cwd=ROOT,text=True).strip()
            if base: break
        env_base=__import__('os').environ.get('R926_R925_BASE_COMMIT','').strip()
        if env_base: base=env_base
        if not base:
            raise RuntimeError('R925_BASELINE_COMMIT_NOT_FOUND')
        subprocess.run(['git','cat-file','-e',base+'^{commit}'],cwd=ROOT,check=True,stdout=subprocess.DEVNULL,stderr=subprocess.DEVNULL)
        paths=subprocess.check_output(['git','ls-tree','-r','--name-only',base,'app/src/main/java/com/ethk3/novaonepro','app/src/main/res'],cwd=ROOT,text=True).splitlines()
        current_java={str(x.relative_to(ROOT)).replace('\\','/') for x in JAVA.glob('*.java')}
        baseline_java={x for x in paths if x.endswith('.java')}
        baseline_res={x for x in paths if x.startswith('app/src/main/res/')}
        changed_res=subprocess.check_output(['git','diff','--name-only',base,'--','app/src/main/res'],cwd=ROOT,text=True).splitlines()
        return {'commit':base,'baseline_java':baseline_java,'current_java':current_java,'baseline_res':baseline_res,'changed_res':changed_res}
    except Exception as e:
        return {'commit':'UNAVAILABLE','baseline_java':set(),'current_java':set(),'baseline_res':set(),'changed_res':['ERROR:'+type(e).__name__]}

CRITICAL_CHAIN={'AutoFetchService','DatabaseCoordinator','DbHelper','DbStore','AnalysisRequestStoreR36','SettlementEngine','V51Repository','DeferredEvidenceWriterR36','LearningEngine','RuntimeEventLedgerR926','SchemaUpgradeCoordinatorR815'}

def sha(p): return hashlib.sha256(p.read_bytes()).hexdigest()
def bounded(items,n=40):
    x=sorted(set(i for i in items if i)); return ';'.join(x[:n])+(';<TRUNCATED>' if len(x)>n else '')
def class_names(text): return re.findall(r'\b(?:class|interface|enum)\s+([A-Z][A-Za-z0-9_]*)',text)
def method_defs(text):
    out=[]
    for line in text.splitlines():
        if '(' not in line or ')' not in line or '{' not in line: continue
        m=re.match(r'\s*(?:public|private|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\[\], ?]+\s+([A-Za-z_$][\w$]*)\s*\(',line)
        if m: out.append(m.group(1))
    return out

def catch_blocks(text):
    newlines=[i for i,ch in enumerate(text) if ch=='\n']; out=[]
    for m in re.finditer(r'catch\s*\(([^)]*)\)\s*\{',text):
        i=m.end(); depth=1; j=i; quote=None; esc=False
        while j<len(text) and depth:
            ch=text[j]
            if quote:
                if esc: esc=False
                elif ch=='\\': esc=True
                elif ch==quote: quote=None
            else:
                if ch in ('"',"'"): quote=ch
                elif ch=='{': depth+=1
                elif ch=='}': depth-=1
            j+=1
        body=text[i:max(i,j-1)].strip(); arg=m.group(1); low=body.lower(); before=text[max(0,m.start()-260):m.start()].lower()
        if not body:
            cleanup=bool(re.search(r'\b(?:ignore|ignored)\b',arg,re.I)) and any(x in before for x in ['.close(','.unlock(','endtransaction(','setpriority(','allowcorethreadtimeout(','interrupt()'])
            kind='INTENTIONAL_CLEANUP_IGNORE' if cleanup else 'EMPTY_UNDIAGNOSED'
        elif re.search(r'\bthrow\b',body): kind='RETHROW'
        elif any(k in low for k in ['retry','postdelayed','schedule','next_attempt','backoff']): kind='RETRY'
        elif any(k in low for k in ['log.','logger','setstate','error_code','last_error','lasterror','status','detail','recordcriticalcatchr926','runtimeeventhooksr926','runtimerecoverycoordinatorr926','addskip(','appendsecondary(','failure =']): kind='DIAGNOSTIC'
        elif any(k in low for k in ['return false','return null','fallback','degrad','shadow']): kind='DEGRADE'
        elif re.search(r'\b(?:ignore|ignored)\b',arg,re.I): kind='INTENTIONAL_IGNORE_REVIEW'
        else: kind='UNCLASSIFIED_REVIEW'
        line=bisect.bisect_right(newlines,m.start())+1
        out.append({'line':line,'arg':arg,'kind':kind,'body_chars':len(body),'body_preview':re.sub(r'\s+',' ',body)[:180]})
    return out

def main():
    files=sorted(JAVA.glob('*.java')); texts={p.stem:p.read_text(encoding='utf-8',errors='replace') for p in files}; names=set(texts)
    inv=baseline_inventory()
    callees={}; callers={n:set() for n in names}
    for n,t in texts.items():
        refs={token for token in re.findall(r'\b([A-Z][A-Za-z0-9_]*)\b',t) if token in names and token!=n}; callees[n]=refs
        for r in refs: callers[r].add(n)
    manifest=(ROOT/'app/src/main/AndroidManifest.xml').read_text(encoding='utf-8',errors='replace'); entry=set(re.findall(r'android:name="\.([^"$]+)',manifest))
    owner_tokens=['DecisionGateEngine','SettlementEngine','TargetIssueResolver','AnalysisSingleFlightR36','OosAfterDrawWriterR36','DeferredEvidenceWriterR36']
    rows=[]; catch_rows=[]
    for p in files:
        n=p.stem;t=texts[n];lines=t.count('\n')+1; catches=catch_blocks(t)
        for c in catches: catch_rows.append({'file':str(p.relative_to(ROOT)),'class':n,'critical_chain':1 if n in CRITICAL_CHAIN else 0,**c})
        db_reads=len(re.findall(r'\b(?:rawQuery|query)\s*\(|\bSELECT\b',t,re.I)); db_writes=len(re.findall(r'\b(?:insert|insertOrThrow|insertWithOnConflict|update|delete|replace|execSQL)\s*\(|\b(?:INSERT|UPDATE|DELETE|CREATE|ALTER|DROP)\b',t,re.I))
        thread_hits=sorted(set(re.findall(r'\b(?:HandlerThread|Handler|Thread|ExecutorService|Executor|Atomic\w+|synchronized)\b',t))); owns=[x for x in owner_tokens if x in t]
        counts={k:sum(1 for c in catches if c['kind']==k) for k in sorted({c['kind'] for c in catches})}
        disposition='ANDROID_ENTRY_POINT' if n in entry else ('NO_PROJECT_CALLER_REVIEW' if not callers[n] else 'ACTIVE_REFERENCED')
        if n.startswith('BuildConfig'): disposition='GENERATED_OR_TEST_STUB'
        rel=str(p.relative_to(ROOT)).replace('\\','/')
        baseline_status='R925_ORIGINAL' if rel in inv['baseline_java'] else 'R926_NEW'
        if baseline_status=='R925_ORIGINAL' and disposition=='NO_PROJECT_CALLER_REVIEW': disposition='R925_ORIGINAL_REVIEWED_NO_STATIC_CALLER'
        role=[]
        if 'Activity' in n or 'TextView' in t or 'setContentView' in t: role.append('UI')
        if 'Service' in n or 'Worker' in n or 'Scheduler' in n: role.append('BACKGROUND')
        if db_reads or db_writes: role.append('DB')
        if 'Research' in n or 'Learning' in n or 'Algorithm' in n or 'Model' in n: role.append('RESEARCH_MODEL')
        if 'Decision' in n or 'Gate' in n: role.append('DECISION')
        if 'Settlement' in n or 'Oos' in n: role.append('SETTLEMENT')
        if not role: role=['UTILITY_OR_DOMAIN']
        rows.append({'file':str(p.relative_to(ROOT)),'class':n,'bytes':p.stat().st_size,'lines':lines,'sha256':sha(p),'roles':'|'.join(role),'declared_classes':bounded(class_names(t),12),'method_count':len(method_defs(t)),'callers':bounded(callers[n]),'callees':bounded(callees[n]),'caller_count':len(callers[n]),'callee_count':len(callees[n]),'db_read_markers':db_reads,'db_write_markers':db_writes,'thread_markers':bounded(thread_hits,20),'owner_markers':bounded(owns,20),'catch_count':len(catches),'catch_classes':json.dumps(counts,ensure_ascii=False,sort_keys=True),'disposition':disposition,'reachability_basis':'manifest-or-static-class-reference','baseline_status':baseline_status,'proof_scope':'STATIC_EVIDENCE_NOT_RUNTIME_PROOF'})
    with (OUT/'SOURCE_ROLE_INDEX_R926.csv').open('w',encoding='utf-8-sig',newline='') as f: w=csv.DictWriter(f,fieldnames=list(rows[0]));w.writeheader();w.writerows(rows)
    with (OUT/'CATCH_AUDIT_R926.csv').open('w',encoding='utf-8-sig',newline='') as f: w=csv.DictWriter(f,fieldnames=list(catch_rows[0]));w.writeheader();w.writerows(catch_rows)
    critical_undiagnosed=[r for r in catch_rows if r['critical_chain'] and r['kind']=='EMPTY_UNDIAGNOSED']
    critical_method_rows=[]
    for cls,names_needed in CRITICAL_METHODS.items():
        text=texts.get(cls,'')
        spans=method_spans(text)
        for sp in spans:
            if sp['name'] not in names_needed: continue
            body=text[sp['start']:sp['end']]
            for c in catch_blocks(body):
                row={'class':cls,'method':sp['name'],'method_line':sp['line'],'catch_line':sp['line']+c['line']-1,**c}
                row['accepted']=1 if c['kind'] in ACCEPTED_CRITICAL_CATCH_KINDS else 0
                critical_method_rows.append(row)
    cm_fields=['class','method','method_line','catch_line','arg','kind','accepted','body_chars','body_preview']
    with (OUT/'CRITICAL_METHOD_CATCH_AUDIT_R926.csv').open('w',encoding='utf-8-sig',newline='') as f:
        w=csv.DictWriter(f,fieldnames=cm_fields);w.writeheader();w.writerows([{k:r.get(k,'') for k in cm_fields} for r in critical_method_rows])
    critical_bad=[r for r in critical_method_rows if not r['accepted']]
    missing_critical=[]
    for cls,names_needed in CRITICAL_METHODS.items():
        found={sp['name'] for sp in method_spans(texts.get(cls,''))}
        for method in sorted(names_needed-found): missing_critical.append(cls+'.'+method)
    baseline_deleted=sorted(inv['baseline_java']-inv['current_java'])
    baseline_added=sorted(inv['current_java']-inv['baseline_java'])
    disposition_complete=(len(rows)+len(baseline_deleted)>=len(inv['current_java']|inv['baseline_java']) and all(r.get('disposition') for r in rows))
    summary={'java_files':len(rows),'r925_baseline_commit':inv['commit'],'r925_baseline_java':len(inv['baseline_java']),'r926_new_java':len(baseline_added),'r925_deleted_java':baseline_deleted,'r926_added_java':baseline_added,'r925_baseline_resources':len(inv['baseline_res']),'resource_changes_vs_r925':inv['changed_res'],'all_source_disposition_complete':disposition_complete,'android_entry_points':sum(r['disposition']=='ANDROID_ENTRY_POINT' for r in rows),'no_project_caller_review':sum('NO_STATIC_CALLER' in r['disposition'] or r['disposition']=='NO_PROJECT_CALLER_REVIEW' for r in rows),'large_files_over_3000_lines':[{'file':r['file'],'lines':r['lines']} for r in rows if int(r['lines'])>3000],'catch_total':len(catch_rows),'empty_undiagnosed_total':sum(r['kind']=='EMPTY_UNDIAGNOSED' for r in catch_rows),'critical_chain_empty_undiagnosed_whole_file_context_only':critical_undiagnosed,'critical_method_catch_total':len(critical_method_rows),'critical_method_unaccepted':critical_bad,'critical_method_missing':missing_critical,'critical_method_gate_pass':not critical_bad and not missing_critical,'owner_reference_files':sum(bool(r['owner_markers']) for r in rows),'proof_scope':'WHOLE_SOURCE_STATIC_ROLE_AND_REFERENCE_AUDIT_PLUS_METHOD_SCOPED_CRITICAL_CATCH_GATE_NOT_DEVICE_EXECUTION'}
    (OUT/'SOURCE_ROLE_SUMMARY_R926.json').write_text(json.dumps(summary,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps({'java_files':summary['java_files'],'r925_baseline_java':summary['r925_baseline_java'],'r926_new_java':summary['r926_new_java'],'catch_total':summary['catch_total'],'critical_method_catch_total':summary['critical_method_catch_total'],'critical_method_unaccepted_count':len(summary['critical_method_unaccepted']),'critical_method_missing':summary['critical_method_missing'],'resource_changes_vs_r925':summary['resource_changes_vs_r925'],'all_source_disposition_complete':summary['all_source_disposition_complete']},ensure_ascii=False))
if __name__=='__main__': main()
