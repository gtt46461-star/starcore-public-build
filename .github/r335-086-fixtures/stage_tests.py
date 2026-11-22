"""Prepare the four corrected Android test fixtures outside the verified app checkout."""
import hashlib
import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

repository = Path(sys.argv[1]).resolve()
workspace = Path(sys.argv[2]).resolve()
files = Path(__file__).resolve().parent
subtree = Path('app/src/androidTest/java')
checks = json.loads((files/'test-sources.json').read_bytes())

def digest(path):
    with path.open('rb') as contents:
        return hashlib.file_digest(contents, 'sha256').hexdigest()

if workspace.exists():
    raise FileExistsError(workspace)
workspace.mkdir(parents=True)
source = repository/subtree
destination = workspace/subtree
shutil.copytree(source, destination)
for path, hashes in checks.items():
    assert Path(path).is_relative_to(subtree)
    assert digest(workspace/path) == hashes['beforeSha256']
    committed = subprocess.check_output(['git', '-C', str(repository), 'show', 'HEAD:'+path])
    assert hashlib.sha256(committed).hexdigest() == hashes['beforeSha256']
patch = str(files/'android-fixtures.patch')
subprocess.run(['git', 'apply', '--no-index', '--check', patch], cwd=workspace, check=True)
subprocess.run(['git', 'apply', '--no-index', patch], cwd=workspace, check=True)
for src in source.rglob('*'):
    if src.is_file():
        key = str(src.relative_to(repository))
        expected = checks[key]['afterSha256'] if key in checks else digest(src)
        assert digest(workspace/key) == expected
shutil.copyfile(files/'WarmShadowCycleTest.java', destination/'com/ethk3/novaonepro/WarmShadowCycleTest.java')
print(destination)
