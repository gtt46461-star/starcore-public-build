"""Read committed transport clocks from one freshly upgraded, enabled Android guest."""
from pathlib import Path

observe = Path('.github/r335-095-research-import/observe.py')
original = observe.read_text()
start = original.index('    from import_ui import import_history')
end = original.index("    assert f.installed(f.EVID/'end.txt'", start)
body = '''    from transport_clocks import observe_transport
    observe_transport(query,record,save)
'''
import sys
sys.path.insert(0,str(Path(__file__).resolve().parent))
exec(compile(original[:start]+body+original[end:],str(observe),'exec'))
