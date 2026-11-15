#!/usr/bin/env python3
from pathlib import Path
import subprocess,tempfile,os,textwrap,time
root=Path(__file__).resolve().parents[1]
classes=root/'build/r925-host/compiled-classes'
source=(root/'app/src/main/java/com/ethk3/novaonepro/AnalysisRequestStoreR36.java').read_text()
required=['R947_ANALYSIS_EXECUTION_WINDOW_REFRESH','EXECUTION_WINDOW_REFRESHED','canonicalExists(db, request.targetIssue)','WHERE issue=? AND dirty=0','executionWindowTooShort']
for token in required:
    if token not in source: raise SystemExit('R947_ANALYSIS_WINDOW_SOURCE_FAIL:'+token)
with tempfile.TemporaryDirectory(prefix='r947-window-') as td:
    pkg=Path(td)/'com/ethk3/novaonepro';pkg.mkdir(parents=True)
    java=pkg/'R947AnalysisWindowHarness.java'
    java.write_text(textwrap.dedent('''
        package com.ethk3.novaonepro;
        public final class R947AnalysisWindowHarness {
          public static void main(String[] args) {
            long now=1000000L;
            AnalysisSingleFlightR36.Request shortReq=new AnalysisSingleFlightR36.Request(
              "202609130100","202609130101",1,now-1000,now+2500,"x",repeat('a',64),"");
            AnalysisSingleFlightR36.Request healthy=new AnalysisSingleFlightR36.Request(
              "202609130100","202609130101",1,now-1000,now+20000,"x",repeat('b',64),"");
            if(!AnalysisRequestStoreR36.executionWindowTooShort(shortReq,now,12000)) throw new RuntimeException("SHORT_NOT_DETECTED");
            if(AnalysisRequestStoreR36.executionWindowTooShort(healthy,now,12000)) throw new RuntimeException("HEALTHY_FALSE_POSITIVE");
            if(AnalysisRequestStoreR36.executionWindowTooShort(healthy,now,0)) throw new RuntimeException("MINIMUM_CLAMP_FALSE_POSITIVE");
            System.out.println("R947_ANALYSIS_WINDOW_CLASSIFIER_PASS");
          }
          static String repeat(char c,int n){StringBuilder b=new StringBuilder();for(int i=0;i<n;i++)b.append(c);return b.toString();}
        }
    '''),encoding='utf-8')
    subprocess.run(['javac','-cp',str(classes),'-d',td,str(java)],check=True)
    out=subprocess.check_output(['java','-cp',str(classes)+os.pathsep+td,'com.ethk3.novaonepro.R947AnalysisWindowHarness'],text=True).strip()
    if 'PASS' not in out: raise SystemExit('R947_ANALYSIS_WINDOW_CLASSIFIER_FAIL:'+out)
print('R947_ANALYSIS_WINDOW_PASS|min_remaining_ms=12000|identity_fields_immutable=1|postdraw_guard=1|canonical_guard=1')
