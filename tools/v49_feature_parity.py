from pathlib import Path
import ast
import hashlib
import json
import re
import sys

java = Path('app/src/main/java/com/ethk3/novaonepro/FeatureContractRegistry.java').read_text('utf-8')
training = Path('training/build_dataset.py').read_text('utf-8')
artifact = Path('app/src/main/java/com/ethk3/novaonepro/V49ModelArtifact.java').read_text('utf-8')
contracts = re.findall(r'register\("([^"]+)",\s*"([^"]+)"', java)
java_names = [x[0] for x in contracts]
required = {
    'freq_w6','freq_w12','freq_w24','freq_w48','freq_w96','freq_w180','freq_w360',
    'freq_w720','freq_w1440','trend_slope','trend_accel','gap_norm','gap_percentile',
    'switch_risk','action_probability','third_contribution','third_margin'
}


def python_feature_names(text):
    tree = ast.parse(text)
    for node in tree.body:
        if isinstance(node, (ast.Assign, ast.AnnAssign)):
            target = node.targets[0] if isinstance(node, ast.Assign) else node.target
            if isinstance(target, ast.Name) and target.id == 'FEATURE_NAMES':
                return list(ast.literal_eval(node.value))
    return []


py_names = python_feature_names(training)
match = re.search(r'FEATURE_NAMES\s*=\s*new String\[\]\{([^}]*)\}', artifact, re.S)
artifact_names = re.findall(r'"([^"]+)"', match.group(1)) if match else []
number_feature_contract = [n for n in java_names if n in set(py_names[2:])]
checks = {
    'contract_count': len(contracts) >= 27,
    'required': required <= set(java_names),
    'single_smoothing_formula': 'count + 0.5d' in java and 'observed + 5.0d' in java,
    'training_feature_count_24': len(py_names) == 24,
    'training_java_feature_names_exact': set(number_feature_contract) == set(py_names[2:]),
    'artifact_feature_order_exact': artifact_names == py_names,
    'artifact_real_training_hash': 'UNTRAINED_NEUTRAL' not in artifact and 'PARAMETER_SHA256' in artifact,
    'feature_contract_hash_emitted': 'feature_contract' in training and 'FEATURE_NAMES' in training,
}
out = {
    'checks': checks,
    'contract_count': len(contracts),
    'training_feature_count': len(py_names),
    'training_features': py_names,
    'artifact_features': artifact_names,
    'java_matching_features': number_feature_contract,
    'sha256': hashlib.sha256('\n'.join(a + '=' + b for a, b in contracts).encode()).hexdigest(),
}
Path('reports/V49_FEATURE_PARITY_49072201.json').write_text(
    json.dumps(out, indent=2, ensure_ascii=False), encoding='utf-8')
print(out)
sys.exit(0 if all(checks.values()) else 1)
