"""Keep report data private and surface a typed location if the verification process fails."""
from contextlib import redirect_stderr, redirect_stdout
from pathlib import Path
import json
import os
import runpy
import sys
import traceback

folder=Path(__file__).parent
temp=Path(os.environ['RUNNER_TEMP'])
try:
    with (temp/'fullraw-private-console.txt').open('w') as log, redirect_stderr(log), redirect_stdout(log):
        runpy.run_path(str(folder/'driver.py'),run_name='__main__')
except Exception as error:
    names={'run_private.py','driver.py','observe.py','field.py','phone_full_backup.py','verify_export_artifacts_r334.py','RawClosure.java'}
    frames=[[Path(f.filename).name,f.lineno] for f in traceback.extract_tb(error.__traceback__) if Path(f.filename).name in names]
    codes={'CANDIDATE','PREVIOUS_INSTALL','PRIOR_APK','CURRENT_INSTALL','INITIAL_PACKAGE','RAW_BODY_IDENTITY','SNAPSHOT_DESCENDANTS','ORIGINAL_APK'}
    label=next((c for c in codes if c==str(error)),'UNCLASSIFIED')
    print(json.dumps(dict(scope='READER_OR_HARNESS_FAILED',reason=label,kind=type(error).__name__,frames=frames)),file=sys.stderr,flush=True)
    sys.exit(1)
