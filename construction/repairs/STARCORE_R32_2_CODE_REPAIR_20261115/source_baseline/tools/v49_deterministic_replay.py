from pathlib import Path
import hashlib,json,sys,re
paths=['FeatureContractRegistry.java','V49NumberFeatureEngine.java','V49WindowStateEngine.java','MixtureOfExpertsGateEngine.java','V49DirichletCalibrationEngine.java','V49RealtimePipelineEngine.java','V49ContractEngine.java']
base=Path('app/src/main/java/com/ethk3/novaonepro')
h=[]
for name in paths:
    b=(base/name).read_bytes();h.append(hashlib.sha256(b).hexdigest())
text=(base/'V49RealtimePipelineEngine.java').read_text('utf-8')
checks={'no_random':not re.search(r'Math\.random|new\s+Random|UUID\.randomUUID',text),'stable_sort':'return x != 0 ? x : a.intValue() - b.intValue()' in text,'checksum':'candidateHash' in text and 'featureChecksum' in (base/'V49ContractEngine.java').read_text('utf-8')}
out={'checks':checks,'pipeline_fingerprint':hashlib.sha256('|'.join(h).encode()).hexdigest()};Path('reports/V49_DETERMINISTIC_REPLAY_49072201.json').write_text(json.dumps(out,indent=2),'utf-8');print(out);sys.exit(0 if all(checks.values()) else 1)
