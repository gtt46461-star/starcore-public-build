"""Keep the historical collector and its P0 calendar. Bind its two APK inputs to R34."""
from pathlib import Path
import os

kit = Path(".github/r335-independent-window")
previous = "273641e838e0b8dd842b42a67a674e184942a30a"
source = os.environ["R34_WINDOW_SOURCE"]
version = int(os.environ["R34_WINDOW_VERSION"])
old = "a724adbfb31dd96f1459aa2e5cbd94582dd6e82d"
def replace_once(value, before, after):
    if value.count(before) != 1:
        raise ValueError("unexpected original collector structure: "+before)
    return value.replace(before, after)
p=kit/"field.py"
s=replace_once(p.read_text(),'("B08", 577311065, "'+old+'")','("B08", '+str(version)+', "'+source+'")')
p.write_text(s)
p=kit/"observe.py"
s=replace_once(p.read_text(),"""    a,b=[get_apk(*s) for s in (B07,B08)]""",'''    prior=get_apk("prior",577311095,"'''+previous+'''")
    a=prior
    b=get_apk(*B08)''')
s=replace_once(s,"'/releases/download/r335-'+stage+'-'+commit[:12]","('/releases/download/r335-B08-' if stage=='prior' else '/releases/download/r34-B12-')+commit[:12]")
s=replace_once(s,"name='STARCORE-R33-'","name=('STARCORE-R33-' if stage=='prior' else 'STARCORE-R34-')")
s=s.replace('B07[1]', '577311095')
p.write_text(s)
p=kit/"public_metrics.py"
s=replace_once(p.read_text(),old,source)
s=replace_once(s,"apk='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'","apk=os.environ['REPAIRED_APK_SHA']")
s=replace_once(s,'R335-independent-window-','R34-field-window-')
p.write_text(s)
