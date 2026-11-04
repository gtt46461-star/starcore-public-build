#!/usr/bin/env python3
"""Run the original-report quarantine regression against baseline and current Java."""
import json
from pathlib import Path
import subprocess
import tempfile

ROOT=Path(__file__).resolve().parents[2]
DEST=ROOT/'verification/r31/p00/task-status-regression'
PACKAGE='com/ethk3/novaonepro/strategyv2'
BASELINE='5237b265a27fe5ad27fd842901c445d5f8c27749'
SOURCE='''package com.ethk3.novaonepro.strategyv2;
import java.util.*; import java.nio.file.*;
public class R31OriginalReportStatusRegression {
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception {
  Map<String,Object> report=(Map<String,Object>)JsonR27.parse(Files.readString(Path.of(args[0])));
  Map<String,Object> data=(Map<String,Object>)((Map<?,?>)((List<?>)report.get("chapters")).get(3)).get("data");
  List<Object> facts=new ArrayList<>();
  for(Object o:(List<?>)((Map<?,?>)data.get("taskLiveness")).get("tasks")){
    Object f=((Map<?,?>)o).get("committedFacts");if(f!=null)facts.add(f);
  }
  Map<String,Object> result=TaskProgressR30R1.summarize(data.get("tasks"),facts,3597129L);
  Map<String,Map<?,?>> rows=new HashMap<>();for(Object o:(List<?>)result.get("tasks"))rows.put(String.valueOf(((Map<?,?>)o).get("taskId")),(Map<?,?>)o);
  int quarantined=0,hidden=0;
  for(Object o:(List<?>)data.get("tasks")){
   Map<?,?> task=(Map<?,?>)o;if(!"QUARANTINED".equals(task.get("status")))continue;
   quarantined++;Map<?,?> row=rows.get(task.get("task_id"));
   if(row==null||!"QUARANTINED".equals(row.get("state"))){hidden++;System.out.println("HIDDEN "+task.get("task_id")+" projected="+(row==null?"MISSING":row.get("state")));}
  }
  System.out.println("originalQuarantined="+quarantined+" hiddenQuarantines="+hidden);
  if(quarantined!=14||hidden!=0)throw new AssertionError("SCHEDULER_FAULT_HIDDEN_BY_OLD_PROGRESS");
 }
}'''


def run(mode, workspace):
    src=workspace/mode/PACKAGE;src.mkdir(parents=True)
    for name in ('JsonR27','ResearchTaskRegistryR30R1','TaskProgressR30R1'):
        relative=f'app/src/main/java/{PACKAGE}/{name}.java'
        data=(subprocess.check_output(['git','show',BASELINE+':'+relative],cwd=ROOT)
              if mode=='baseline' else (ROOT/relative).read_bytes())
        (src/(name+'.java')).write_bytes(data)
    (src/'R31OriginalReportStatusRegression.java').write_text(SOURCE)
    classes=workspace/mode/'classes';classes.mkdir()
    c=subprocess.run(['javac','-d',str(classes),*map(str,src.glob('*.java'))],capture_output=True,text=True)
    (DEST/(mode+'-compile.log')).write_text(c.stdout+c.stderr)
    c.check_returncode()
    r=subprocess.run(['java','-cp',str(classes),'com.ethk3.novaonepro.strategyv2.R31OriginalReportStatusRegression',
                      str(ROOT/'construction/STARCORE_R31_FINAL_20261104/evidence/original_report/R30_REPORT.json')],capture_output=True,text=True)
    (DEST/(mode+'.log')).write_text(r.stdout+r.stderr)
    return {'exit_code':r.returncode,'output':r.stdout+r.stderr}


if __name__=='__main__':
    DEST.mkdir(parents=True,exist_ok=True)
    with tempfile.TemporaryDirectory(prefix='r31-status-') as scratch:
        before=run('baseline',Path(scratch));after=run('current',Path(scratch))
    passed=(before['exit_code']!=0 and 'SCHEDULER_FAULT_HIDDEN_BY_OLD_PROGRESS' in before['output'] and after['exit_code']==0)
    result={'scope':'ORIGINAL_REPORT_THROUGH_PRODUCTION_JAVA_PROJECTION_HOST_NOT_DEVICE',
            'status':'PASS' if passed else 'FAIL','baseline':BASELINE,'before':before,'after':after}
    (DEST/'receipt.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n')
    print(json.dumps(result,ensure_ascii=False,indent=2));raise SystemExit(0 if passed else 1)
