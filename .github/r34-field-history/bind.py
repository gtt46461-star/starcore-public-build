"""Bind the existing signed-package/SAF import exercise to its exact R34 release."""
from pathlib import Path
source = '5fae0728cd461a5d8185ca5a8df3772ed07f13e5'
p = Path('.github/r335-095-research-import/observe.py')
s = p.read_text().replace('273641e838e0b8dd842b42a67a674e184942a30a',source).replace('577311095','577311113').replace('R335-095-research-import-', 'R34-field-research-import-')
p.write_text(s)
p = Path('.github/r335-independent-window/observe.py')
s = p.read_text()
before = "'/releases/download/r335-'+stage+'-'+commit[:12]"
assert s.count(before)==1 and s.count("name='STARCORE-R33-'")==1
s = s.replace(before, "('/releases/download/r34-B12-' if version==577311113 else '/releases/download/r335-'+stage+'-')+commit[:12]")
s = s.replace("name='STARCORE-R33-'", "name=('STARCORE-R34-' if version==577311113 else 'STARCORE-R33-')")
p.write_text(s)
