from pathlib import Path
import csv, hashlib, json, os, re, subprocess, sys, xml.etree.ElementTree as ET
ROOT=Path('.').resolve(); OUT=ROOT/'reports'; OUT.mkdir(exist_ok=True)
BASE=os.environ.get('B05_ACCEPTED_SHA','c98278c1d1df465a72f2af736d7ce49a30d7a3ba')
SRC=ROOT/'app/src/main/java/com/ethk3/novaonepro'
files=['PatternStateEngine.java','ShapeStateEngine.java','TransitionRelationEngine.java','SimilarShapeEngine.java','ShapeMemoryEngine.java','NumberBehaviorEngine.java','AttributionEngine.java']
ranges={'PatternStateEngine.java':(1482,1659),'ShapeStateEngine.java':(1067,1667),'TransitionRelationEngine.java':(1224,1370),'SimilarShapeEngine.java':(1000,1600),'ShapeMemoryEngine.java':(1000,1600),'NumberBehaviorEngine.java':(1406,1574),'AttributionEngine.java':(1373,1537)}
checks=[]
def ck(name,ok,detail=''): checks.append((name,bool(ok),str(detail)))
texts={f:(SRC/f).read_text(encoding='utf-8') for f in files}; joined='\n'.join(texts.values())
changed=subprocess.check_output(['git','diff','--name-only',BASE],text=True).splitlines()
business=sorted('app/src/main/java/com/ethk3/novaonepro/'+f for f in files)
support=sorted(['app/build.gradle','app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java','app/src/main/java/com/ethk3/novaonepro/V51BuildManifest.java'])
ck('BUSINESS_DIFF_SCOPE',sorted(x for x in changed if x not in support and not x.startswith('.github/'))==business,changed)
ck('IDENTITY_POINT_SCOPE',sorted(x for x in changed if x in support)==support,changed)
for f in files:
 n=len(texts[f].splitlines()); lo,hi=ranges[f]; ck('LINE_RANGE_'+f,lo<=n<=hi,'%d in [%d,%d]'%(n,lo,hi))
ck('NO_RAWP_WRITE',not re.search(r'\bcandidate\s*\.\s*rawP\s*=',joined),'')
ck('NO_FINALP_WRITE',not re.search(r'\bcandidate\s*\.\s*finalP\s*=',joined),'')
ck('NO_GATE_OR_BLOCK_WRITE',not re.search(r'\bcandidate\s*\.\s*(?:dynamicGate|blockCode)\s*=',joined),'')
ck('NO_BET_CONSTRUCTION',not re.search(r'new\s+(?:Models\.)?(?:Bet|Trade|Decision)\s*\(',joined,re.I),'')
ck('NO_TODO_PLACEHOLDER',not re.search(r'\bTODO\b|PLACEHOLDER|UnsupportedOperationException',joined,re.I),'')
ck('B06_OWNER_ALL',all('B06_OWNER' in texts[f] for f in files),'7/7')
ck('HASH_ALL',all('fingerprint' in texts[f].lower() and 'sha256' in texts[f].lower() for f in files),'7/7')
ck('DECISION_GATE_DECLARED',sum('DecisionGateEngine' in texts[f] for f in files)>=3,sum('DecisionGateEngine' in texts[f] for f in files))
sim=texts['SimilarShapeEngine.java']; mem=texts['ShapeMemoryEngine.java']; attr=texts['AttributionEngine.java']
ck('SIMILAR_PIT_COMPARE','PredictionContractEngine.compareIssue' in sim and 'futureExcludedCount' in sim and 'futureVisibleCount' in sim,'strict issue boundary')
ck('SIMILAR_MULTI_HORIZON',all(x in sim for x in ['horizon24','horizon48','horizon96','horizon240','ablationFragility']),'24/48/96/240')
ck('MEMORY_REAL_CALIBRATION_GUARD',all(x in mem for x in ['AppConfig.isRealTradeScope(trade.statScope)','DECISION_BET','trade.settled == 1','store.updateCalibration']),'REAL_TRADE+BET+settled')
ck('MEMORY_CALIBRATION_SINGLE_CALL',mem.count('store.updateCalibration(')==1,mem.count('store.updateCalibration('))
ck('MEMORY_MULTI_HORIZON',all(x in mem for x in ['rate64','rate128','rate240','rate480','crossHorizonStability','realCalibrationEligible']),'64/128/240/480')
ck('ATTRIBUTION_PROOF_GATES',all(x in attr for x in ['wilsonLower95','pass39','pass40','pass50','pass60','frequency15','frequency20','frequency30','frequency40']),'proof/frequency gates')
ck('ATTRIBUTION_NO_FALSE_CLAIM','NOT_PROVEN' in attr and '不得宣称40%、50%或60%' in attr,'honest status')
for f in files:
 cls=f[:-5]; refs=[]
 for p in SRC.glob('*.java'):
  if p.name!=f and cls in p.read_text(encoding='utf-8',errors='ignore'): refs.append(p.name)
 ck('PRODUCTION_REF_'+f,len(refs)>0,','.join(refs[:8]))
for f in files:
 s=texts[f]; ck('BALANCE_'+f,s.count('{')==s.count('}') and s.count('(')==s.count(')') and s.count('[')==s.count(']'),'%d/%d %d/%d %d/%d'%(s.count('{'),s.count('}'),s.count('('),s.count(')'),s.count('['),s.count(']')))
xml_errors=[]
for p in (ROOT/'app/src/main').rglob('*.xml'):
 try: ET.parse(str(p))
 except Exception as e: xml_errors.append('%s:%s'%(p,e))
ck('XML_PARSE',not xml_errors,len(xml_errors))
build=(ROOT/'app/build.gradle').read_text(encoding='utf-8'); manifest=(SRC/'V51BuildManifest.java').read_text(encoding='utf-8'); identity=(SRC/'BuildIdentity.java').read_text(encoding='utf-8')
ck('B06_VERSION_IDENTITY',all(x in build+manifest+identity for x in ['V57.6.0-tianshu-b06-shadow-engineering','576010806','B06']),'B06')
ck('SHADOW_ONLY_LOCK','SHADOW_ONLY = true' in manifest and 'REAL_BET_HARD_DISABLED = true' in manifest and 'MODEL_PROVEN = false' in manifest,'locked')
rows=[]
for f in files:
 rel='app/src/main/java/com/ethk3/novaonepro/'+f; old=subprocess.check_output(['git','show',BASE+':'+rel]); new=(ROOT/rel).read_bytes()
 rows.append({'file':rel,'kind':'BUSINESS','old_lines':len(old.decode('utf-8').splitlines()),'new_lines':len(new.decode('utf-8').splitlines()),'delta':len(new.decode('utf-8').splitlines())-len(old.decode('utf-8').splitlines()),'sha256':hashlib.sha256(new).hexdigest(),'role':'Evidence-only; DecisionGateEngine remains BET owner','production_referenced':'YES'})
for rel in support:
 old=subprocess.check_output(['git','show',BASE+':'+rel]); new=(ROOT/rel).read_bytes()
 rows.append({'file':rel,'kind':'IDENTITY_POINT_REPAIR','old_lines':len(old.decode('utf-8').splitlines()),'new_lines':len(new.decode('utf-8').splitlines()),'delta':len(new.decode('utf-8').splitlines())-len(old.decode('utf-8').splitlines()),'sha256':hashlib.sha256(new).hexdigest(),'role':'B06 build identity only','production_referenced':'YES'})
with (OUT/'B06_FILE_AUDIT.csv').open('w',encoding='utf-8-sig',newline='') as fh:
 w=csv.DictWriter(fh,fieldnames=rows[0].keys()); w.writeheader(); w.writerows(rows)
(OUT/'B06_FILE_AUDIT.json').write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding='utf-8')
(OUT/'B06_FULL_PARSE.log').write_text('java=%d\nxml=%d\nxml_errors=%d\n%s\n'%(len(list((ROOT/'app/src/main/java').rglob('*.java'))),len(list((ROOT/'app/src/main').rglob('*.xml'))),len(xml_errors),'\n'.join(xml_errors)),encoding='utf-8')
with (OUT/'B06_HARNESS.log').open('w',encoding='utf-8') as fh:
 for n,o,d in checks: fh.write(('%s %s | %s\n'%('PASS' if o else 'FAIL',n,d)))
 fh.write('TOTAL=%d PASS=%d FAIL=%d\n'%(len(checks),sum(1 for _,o,_ in checks if o),sum(1 for _,o,_ in checks if not o)))
print((OUT/'B06_HARNESS.log').read_text(encoding='utf-8'))
if not all(o for _,o,_ in checks): sys.exit(1)
