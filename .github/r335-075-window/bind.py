"""Bind the existing prospective calendar collector to the two public upgrade versions."""
from pathlib import Path
import os
kit=Path('.github/r335-independent-window')
old='a724adbfb31dd96f1459aa2e5cbd94582dd6e82d'
current='d48b6bcd54d05250b1382e977c65832d3622b1c8'
f=kit/'field.py';s=f.read_text().replace('("B08", 577311065, "'+old+'")','("B08", 577311075, "'+current+'")')
f.write_text(s)
f=kit/'observe.py';s=f.read_text().replace('    a,b=[get_apk(*s) for s in (B07,B08)]','''    prior=get_apk('B08',577311065,"a724adbfb31dd96f1459aa2e5cbd94582dd6e82d")
    a=prior.with_name('prior-065.apk');prior.rename(a)
    b=get_apk(*B08)''').replace('B07[1]','577311065')
f.write_text(s)
f=kit/'public_metrics.py';s=f.read_text().replace(old,current).replace("apk='09af709a6dc462f0f883156d20a723c0eb60a63aa6a43896e2c65ffa1905cced'","apk=next(line.split()[0] for line in (root/'B08-SUMS').read_text().splitlines() if line.endswith('-release.apk'))").replace('R335-independent-window-','R335-075-window-');f.write_text(s)
