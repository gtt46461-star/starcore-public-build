from pathlib import Path
import re,json,sys
base=Path('app/src/main/java/com/ethk3/novaonepro')
moe=(base/'MixtureOfExpertsGateEngine.java').read_text('utf-8')
attr=(base/'V49CauseAttributionEngine.java').read_text('utf-8')
experts=re.findall(r'experts\.add\(new\s+([A-Za-z0-9_]+)',moe)
checks={'nine_unique_experts':len(experts)==9 and len(set(experts))==9,'ablation_groups':attr.count('ablation')>=4,'why_hit_miss':'STRUCTURAL_HIT' in attr and 'REGIME_SWITCH' in attr,'probability_not_mutated':'finalP =' not in attr}
out={'checks':checks,'experts':experts};Path('reports/V49_ABLATION_BENCHMARK_49072201.json').write_text(json.dumps(out,indent=2),'utf-8');print(out);sys.exit(0 if all(checks.values()) else 1)
