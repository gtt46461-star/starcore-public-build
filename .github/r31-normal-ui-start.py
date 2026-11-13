#!/usr/bin/env python3
"""Normal production start control before original report readiness acceptance.

No DB, preferences, schema, service IPC or predictions are injected. This is a
fixture precondition after the preceding live test explicitly stops its service.
The original export, completeness and both schema READY assertions stay intact.
"""
import importlib.util
import json
import pathlib
import subprocess
import sys
import time

assert len(sys.argv)==2
out=pathlib.Path(sys.argv[1]);out.mkdir(parents=True,exist_ok=True)
helper=pathlib.Path(__file__).with_name('r31-final-current-android.py')
spec=importlib.util.spec_from_file_location('actual_ui',helper)
ui=importlib.util.module_from_spec(spec);spec.loader.exec_module(ui)
assert ui.adb('shell','getprop','ro.kernel.qemu')=='1' and ui.adb('shell','id','-u')=='0'
ui.adb('shell','am','start','-W','-n',ui.PKG+'/com.ethk3.novaonepro.MainActivity')
ui.click(lambda n:n.get('resource-id','').endswith('/btn_start'))
for _ in range(120):time.sleep(1)
receipt={'sourceCommit':subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip(),'precondition':'ACTUAL_MAIN_ACTIVITY_START_BUTTON_AFTER_PREVIOUS_TEST_STOPPED_SERVICE','normalStartupObservationSeconds':120,'readiness':'NOT_INFERRED; ORIGINAL_REPORT_ASSERTS_CRITICAL_AND_RESEARCH_READY','databaseOrPreferenceInjection':False,'physicalDevice':'NOT_RUN'}
(out/'NORMAL_UI_START_PRECONDITION.json').write_text(json.dumps(receipt,indent=2)+'\n')
print('NORMAL_UI_START_PRECONDITION|ACTUAL_BUTTON|ORIGINAL_READY_ASSERTIONS_UNCHANGED')
