#!/usr/bin/env python3
"""Parse existing Java declarations for plan linkage. Not Android compilation or testing."""
import argparse,hashlib,json,re,subprocess,sys
from pathlib import Path

def main():
 ap=argparse.ArgumentParser(description=__doc__);ap.add_argument('--plan',type=Path,default=Path(__file__).resolve().parents[1]);ap.add_argument('--source-repo',type=Path,required=True);ap.add_argument('--out',type=Path,required=True);args=ap.parse_args()
 plan=args.plan.resolve();source=args.source_repo.resolve()
 cards=json.loads((plan/'FILE_CARDS.json').read_text())['files']
 cards=[c for c in cards if c['exists_at_baseline'] and c['path'].endswith('.java')]
 result=subprocess.run(['java',str(plan/'tools/RuntimeMethodExtractor.java')]+[str(source/c['path']) for c in cards],capture_output=True,text=True,timeout=120)
 decl=[json.loads(line) for line in result.stdout.splitlines() if line.strip()];errors=[];verified=[];planned=[];field_checks=[]
 if result.returncode or result.stderr.strip():errors.append({'error':'JAVA_PARSE_DIAGNOSTICS','exit_code':result.returncode,'diagnostics':result.stderr})
 for c in cards:
  own=[d for d in decl if Path(d['file']).resolve()==source/c['path']]
  available={d['name'] for d in own if d['kind']=='method'}
  ctors={d['owner'].split('.')[-1] for d in own if d['kind']=='constructor'}
  contracts=[m for m in c.get('method_contracts',[]) if isinstance(m,dict)]
  future={m.get('name',m.get('method')) for m in contracts if 'PLANNED' in m.get('state',m.get('status',''))}
  for name in c['methods']:
   simple=name.split('.')[-1]
   if name in future or simple in future:planned.append({'file':c['path'],'method':name,'status':'PLANNED_NOT_IMPLEMENTED'});continue
   found=simple in available or simple in ctors
   if not found:errors.append({'error':'METHOD_DECLARATION_NOT_FOUND','file':c['path'],'method':name})
   else:verified.append({'file':c['path'],'method':name,'kind':'constructor' if simple in ctors and simple not in available else 'method'})
  source_text=(source/c['path']).read_text()
  for field in c.get('fields',[]):
   found=bool(re.search(r'\b'+re.escape(field)+r'\s*(?:=|;)',source_text))
   field_checks.append({'file':c['path'],'field':field,'found':found})
   if not found:errors.append({'error':'FIELD_NOT_FOUND','file':c['path'],'field':field})
 output={'schema':'R342_JAVA_METHOD_LINKAGE_1','source_commit':'eb8f82a8bda8db22f1f122095217132f07053bcb','scope':'JAVA_AST_PARSE_AND_SYMBOL_LINKAGE_ONLY','android_compile_executed':False,'app_test_executed':False,'device_test_executed':False,'status':'PASS' if not errors else 'FAIL','file_cards_sha256':hashlib.sha256((plan/'FILE_CARDS.json').read_bytes()).hexdigest(),'existing_java_files':len(cards),'all_parsed_declarations':len(decl),'verified_plan_symbols':len(verified),'planned_methods_separate':len(planned),'fields_checked':len(field_checks),'parser_diagnostics':result.stderr,'errors':errors,'verified':verified,'planned':planned,'field_checks':field_checks}
 args.out.write_text(json.dumps(output,ensure_ascii=False,indent=2)+'\n')
 print(json.dumps({k:v for k,v in output.items() if k not in ['verified','planned','field_checks']},ensure_ascii=False))
 return bool(errors)
if __name__=='__main__':sys.exit(main())
