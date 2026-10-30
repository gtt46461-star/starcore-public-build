"""Extract the three checked-in verifiers' unchanged source-list expressions and really javac each set."""
from pathlib import Path
import ast
import concurrent.futures
import hashlib
import json
import shutil
import subprocess
import sys
import time

ROOT = Path('/workspace/starcore-public-build')
OUT = Path('/workspace/work/r29/pure-selector-fix')
sys.path.insert(0, str(ROOT / 'scripts'))
from pure_java_sources import select_pure_strategy_sources


def compile_set(label, name):
    script = ROOT / 'scripts' / name
    parsed = ast.parse(script.read_text())
    main = next(node for node in parsed.body if isinstance(node, ast.FunctionDef) and node.name == 'main')
    nodes = []
    for node in main.body:
        if isinstance(node, ast.Assign):
            assigned = {item.id for target in node.targets for item in ast.walk(target) if isinstance(item, ast.Name)}
            if 'sources' in assigned:
                nodes.append(node)
        elif isinstance(node, ast.AugAssign) and isinstance(node.target, ast.Name) and node.target.id == 'sources':
            nodes.append(node)
    namespace = {'ROOT': ROOT, 'HOST': ROOT / 'tools/strategy_su1', 'BASE': ROOT / 'tools/strategy_su2',
                 'r952': ROOT / 'app/src/main/java/com/ethk3/novaonepro/r952',
                 'prod': ROOT / 'app/src/main/java/com/ethk3/novaonepro' / ('strategyv2' if label == 'su1' else ''),
                 'select_pure_strategy_sources': select_pure_strategy_sources}
    exec(compile(ast.Module(body=nodes, type_ignores=[]), str(script), 'exec'), namespace)
    sources = namespace['sources']
    classes = OUT / label / 'classes'
    if classes.exists():
        shutil.rmtree(classes)
    classes.mkdir(parents=True)
    argv = ['java', 'com.sun.tools.javac.Main', '-source', '17', '-target', '17', '-encoding', 'UTF-8', '-d', str(classes)] + [str(source) for source in sources]
    start = time.monotonic()
    run = subprocess.run(argv, cwd=ROOT, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    (OUT / label / 'javac.log').write_text(run.stdout)
    result = {'script': name, 'returncode': run.returncode, 'seconds': round(time.monotonic() - start, 3),
              'source_count': len(sources), 'selected_pure_strategy_count': namespace['source_selection']['selected_count'],
              'test_source_count': sum(source.name.endswith('Test.java') for source in sources),
              'source_sha256': {str(source.relative_to(ROOT)): hashlib.sha256(source.read_bytes()).hexdigest() for source in sources}}
    (OUT / label / 'compile.json').write_text(json.dumps(result, indent=2) + '\n')
    print('ACTUAL_SOURCE_SET_JAVAC', label, 'PASS' if run.returncode == 0 else 'FAIL', result['source_count'], 'sources', flush=True)
    if run.returncode:
        print(run.stdout)
    return result


with concurrent.futures.ThreadPoolExecutor(max_workers=3) as pool:
    results = list(pool.map(lambda pair: compile_set(*pair), [('su1', 'verify_strategy_su1.py'), ('su2', 'verify_plan1025.py'), ('r26', 'verify_r26.py')]))
(OUT / 'compile-summary.json').write_text(json.dumps(results, indent=2) + '\n')
raise SystemExit(0 if all(result['returncode'] == 0 for result in results) else 1)
