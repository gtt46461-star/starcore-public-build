#!/usr/bin/env python3
"""Run actual CAL implementation against isolated time-boundary SQLite fixtures."""
import argparse,json,subprocess,hashlib
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--jdk',type=Path,required=True);p.add_argument('--classes',type=Path,required=True);p.add_argument('--adapters',type=Path,required=True);p.add_argument('--android-runtime',type=Path,required=True);p.add_argument('--out',type=Path,required=True);p.add_argument('--overlay',action='store_true');p.add_argument('--expect-baseline-failure',action='store_true');a=p.parse_args()
r=Path(__file__).resolve().parents[2];o=a.out.resolve();o.mkdir(parents=True,exist_ok=False);c=o/'classes';c.mkdir();cp=':'.join(map(str,[c,a.adapters,a.classes,a.android_runtime]));sources=[r/'verification/r77/CalStrictTimeR77.java']
sources.append(r/'verification/canonical/adapters/android/os/SystemClock.java')
if a.overlay:sources += [r/('app/src/main/java/com/ethk3/novaonepro/'+x+'.java') for x in ['CandidateCalibrationR932','DrawDependencyInputCP12']]
with (o/'compile.log').open('w') as f:subprocess.run([str(a.jdk/'bin/javac'),'-cp',cp,'-d',str(c),*map(str,sources)],stdout=f,stderr=subprocess.STDOUT,check=True)
with (o/'run.log').open('w') as f:z=subprocess.run([str(a.jdk/'bin/java'),'-cp',cp,'com.ethk3.novaonepro.CalStrictTimeR77',str(r/'verification/canonical/schema/sqlite_bridge.py'),str(o/'test.db'),str(o/'result.json')],stdout=f,stderr=subprocess.STDOUT)
log=(o/'run.log').read_text()
if a.expect_baseline_failure:
 assert z.returncode!=0 and 'equal/late/zero capture times rejected' in log,log
 result={'status':'EXPECTED_BASELINE_FAILURE','reason':'late calibration observations incorrectly received hit labels and counts'}
else:
 assert z.returncode==0,log;result=json.loads((o/'result.json').read_text())
result.update({'implementation':'development-source-overlay' if a.overlay else 'supplied-product-bytecode','sources':{str(s.relative_to(r)):hashlib.sha256(s.read_bytes()).hexdigest() for s in sources},'baseline_classes':str(a.classes),'overlay_cal_class':hashlib.sha256((c/'com/ethk3/novaonepro/CandidateCalibrationR932.class').read_bytes()).hexdigest() if a.overlay else None})
(o/'summary.json').write_text(json.dumps(result,indent=2)+'\n');print(json.dumps(result))
