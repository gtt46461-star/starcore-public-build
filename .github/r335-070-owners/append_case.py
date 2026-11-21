"""Append a separately named coverage-failure test to the disposable instrumentation source only."""
from pathlib import Path
import sys
target = Path(sys.argv[1])
source = target.read_text().rstrip()
assert target.name == 'R335MigratedPublicationTest.java' and source.endswith('}')
assert 'failedLookCreatesFreshBoundedResearchAndAtomicCursorWithoutInheritedQuality' in source
case = Path(__file__).with_name('routed-research.java.inc').read_text()
target.write_text(source[:-1] + '\n' + case + '}\n')
assert target.read_text().startswith(source[:-1])
