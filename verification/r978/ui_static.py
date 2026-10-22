"""Source-structure checks only. Does not claim Android view/lifecycle execution."""
from pathlib import Path
s=(Path(__file__).resolve().parents[2]/"app/src/main/java/com/ethk3/novaonepro/MainActivity.java").read_text()
a=s.index("private final Runnable uiTicker")
b=s.index("private final BroadcastReceiver serviceReceiver",a)
x=s[a:b]
checks={"lifecycle guard": "if (destroyed || !r941UiResumed) {" in x,
        "runtime failure logged": 'catch (RuntimeException' in x and 'R978_TICK_REFRESH_FAILED' in x,
        "finally rearm": 'finally {' in x and 'uiHandler.postDelayed(this, 1000L)' in x.split('finally {',1)[1],
        "rearm lifecycle guard": 'if (!destroyed && r941UiResumed && uiHandler != null)' in x.split('finally {',1)[1]}
for label,passed in checks.items():
 print(('PASS' if passed else 'FAIL')+' '+label)
if not all(checks.values()):raise SystemExit(1)
print('R978_UI_STATIC_PASS|checks='+str(len(checks))+'|scope=SOURCE_STRUCTURE_ONLY_NOT_ANDROID_UI_RUNTIME')
