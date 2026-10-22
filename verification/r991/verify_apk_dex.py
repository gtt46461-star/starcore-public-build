#!/usr/bin/env python3
"""Actual APK dexdump provenance, independent of host runtime/device verification."""
import argparse,hashlib,json,re,subprocess,zipfile
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--apk',type=Path,required=True);p.add_argument('--dexdump',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
o=a.out.resolve();assert not o.exists() or not any(o.iterdir());o.mkdir(parents=True,exist_ok=True)
wanted=['ReportExportServiceCP4','UnifiedResearchChoiceR932','MatrixSnapshotBusR952','BuildConfig'];parts={n:[]for n in wanted};records=[]
with zipfile.ZipFile(a.apk)as z:
 for name in z.namelist():
  if not re.fullmatch(r'classes\d*\.dex',name):continue
  dex=o/name;dex.write_bytes(z.read(name));log=o/(name+'.dump');cmd=[str(a.dexdump.resolve()),'-d',str(dex)]
  with log.open('w')as f:q=subprocess.run(cmd,stdout=f,stderr=subprocess.STDOUT)
  records.append({'command':cmd,'exit_code':q.returncode,'dex_sha256':hashlib.sha256(dex.read_bytes()).hexdigest()});assert q.returncode==0
  active=None
  for line in log.open(errors='replace'):
   if line.startswith('Class #'):active=None
   if 'Class descriptor' in line:active=next((n for n in wanted if '/'+n+';'in line),None)
   if active:parts[active].append(line)
for name,lines in parts.items():assert lines,name;(o/(name+'.dexdump.txt')).write_text(''.join(lines))
service=''.join(parts['ReportExportServiceCP4'])
assert 'REPORT_STORAGE_FULL'in service and'FREE_DESTINATION_SPACE_AND_START_NEW_SAF_TRANSFER'in service
assert len(re.findall(r'invoke-static.*ReportExportServiceCP4;\.recordSafStorageFailure:',service))==2,'BOTH_SAF_CATCH_PATHS_CALL_STORAGE_HANDLER'
assert 'prepareChoiceCP12'in ''.join(parts['UnifiedResearchChoiceR932'])
assert 'CP12_MATRIX_SCHEMA_MISMATCH'in ''.join(parts['MatrixSnapshotBusR952'])
build=''.join(parts['BuildConfig'])
for field in ['DUOQU_API_KEY_A','DUOQU_API_KEY_B']:
 part=build.split("name          : '"+field+"'",1)[1].split('    #',1)[0];assert 'value         : ""'in part
receipt={'executions':records,'apk_sha256':hashlib.sha256(a.apk.read_bytes()).hexdigest(),'both_saf_catch_paths_call_actual_storage_handler':True,'prior_mainline_and_matrix_repairs_preserved':True,'BuildConfig_private_api_fields':'EMPTY','scope':'Actual signed APK DEX provenance. Host118 runtime proof separate; DEVICE_NOT_VERIFIED','excerpts':{n:hashlib.sha256((o/(n+'.dexdump.txt')).read_bytes()).hexdigest()for n in wanted}}
(o/'receipt.json').write_text(json.dumps(receipt,indent=2));print('CURRENT_SIGNED_APK_PRODUCT_BINDING_PASS')
