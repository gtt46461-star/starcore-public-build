from pathlib import Path
import gzip,hashlib,json,re,xml.etree.ElementTree as ET
r=Path(__file__).resolve().parents[3];o=Path(__file__).resolve().parent;sha=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
archive=r/'release/r982_source_calls.tsv.gz';raw=gzip.decompress(archive.read_bytes());pin=json.loads((r/'release/r982_source_calls.manifest.json').read_text());assert hashlib.sha256(raw).hexdigest()==pin['sha256']
rows=[x.split('\t') for x in raw.decode().splitlines() if x.startswith('METHOD\t')];sources={x[1]:x[2] for x in rows};assert all(sha(r/p)==h for p,h in sources.items())
def info(x):return {'file':x[1],'class':x[3],'method':x[4],'descriptor':x[5],'lines':[x[6],x[7]],'calls':x[9]}
reflect=[info(x) for x in rows if any(z in x[9] for z in ['java/lang/reflect/Method.invoke','java/lang/Class.forName','java/lang/reflect/Constructor.newInstance'])]
native=[info(x) for x in rows if x[8]=='NATIVE' or 'ReportNativeCP4' in x[3]]
# Candidate names are source literals matched to real compiled DbStore declarations, not assumed dynamic edges.
methods={x[4] for x in rows if x[3]=='com/ethk3/novaonepro/DbStore'};candidates=[]
for file in sorted({x['file'] for x in reflect}):
 text=(r/file).read_text();names=sorted(set(re.findall(r'"([A-Za-z][A-Za-z0-9_]+)"',text))&methods)
 candidates.append({'file':file,'compiled_DbStore_name_candidates':names,'qualification':'Name match only; receiver, overload and reachability need manual review; unmatched calls are not a writer.'})
ns='{http://schemas.android.com/apk/res/android}';manifest=r/'app/src/main/AndroidManifest.xml';root=ET.parse(manifest).getroot();components=[]
for node in root.find('application'):
 if node.tag in ['service','receiver','provider']:
  name=node.get(ns+'name');cls='com/ethk3/novaonepro/'+name.lstrip('.')
  components.append({'kind':node.tag,'attributes':node.attrib,'compiled_entry_methods':[info(x) for x in rows if x[3]==cls and x[4].startswith('on')]})
xmlclicks=[]
for p in (r/'app/src/main/res').rglob('*.xml'):
 for i,line in enumerate(p.read_text().splitlines(),1):
  if 'android:onClick=' in line:xmlclicks.append({'file':str(p.relative_to(r)),'line':i,'text':line.strip()})
result={'scope':'SOURCE_BOUND_ACTUAL_COMPILED_STATIC_CALL_INDEX_PLUS_XML_NATIVE_SOURCE_REVIEW_NOT_DYNAMIC_RUNTIME_COVERAGE','archive_sha256':sha(archive),'archive_content_sha256':pin['sha256'],'all_index_source_hashes_match':True,'reflection_sites':reflect,'candidate_dynamic_DbStore_names':candidates,'manifest_sha256':sha(manifest),'components':components,'xml_onclick':xmlclicks,'native_interface':native,'native_source_sha256':sha(r/'app/src/main/cpp/report_native_cp4.c'),'service_chain':[info(x) for x in rows if (x[3].endswith('/AutoFetchService') and x[4] in ['runAnalysisWatchdogR36','postAnalysisForLatest','submitAnalysisRequestR36']) or x[3].endswith('/BusinessReconcilerR36')]}
(o/'entry_points.json').write_text(json.dumps(result,indent=2)+'\n');print('SOURCE_BOUND_ENTRY_REVIEW_PASS',len(rows),'methods',len(reflect),'reflective sites',len(components),'components',len(xmlclicks),'XML onClick')
