#!/usr/bin/env python3
"""Hard-close the lifecycle pause invariant after recovery integration.

A lifecycle PAUSED state is a system/dependency hold. Ordinary review, a stale/open window row,
or an otherwise-valid old certificate must not restore SHADOW/INFLUENCE/CHAMPION. The only
recovery owner is ObservationGenerationR932.recover(), which explicitly transitions PAUSED ->
OBSERVING after registering a new paid generation and revoking old influence atomically.
"""
from pathlib import Path

R = Path(__file__).resolve().parents[2]
p = R / 'app/src/main/java/com/ethk3/novaonepro/ResearchLifecycleR932.java'
s = p.read_text(encoding='utf-8')

anchor = '''        // System/dependency pauses win over any old ACTIVE row. A positive state is a projection\n        // of the same frozen-request authority used by production, not a second certificate gate.\n        if(store!=null&&next!=State.PAUSED&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED){'''
replacement = '''        // PAUSED is sticky under review. Only ObservationGenerationR932.recover() may reopen it\n        // after a new paid generation is durably registered and old influence is revoked.\n        if(old.equals("PAUSED")&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED){\n            next=State.PAUSED;\n            reason="WAIT_EXPLICIT_NEW_OBSERVATION_RECOVERY";\n        }\n        // System/dependency pauses win over any old ACTIVE row. A positive state is a projection\n        // of the same frozen-request authority used by production, not a second certificate gate.\n        if(store!=null&&next!=State.PAUSED&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED){'''
if s.count(anchor) != 1:
    raise SystemExit('R932_PAUSE_STICKY_ANCHOR_COUNT=' + str(s.count(anchor)))
s = s.replace(anchor, replacement, 1)

elite = '''        if(n>=30&&hits/(double)n>=.35&&brier<=.24&&ece<=.08&&next!=State.QUARANTINED){'''
elite_replacement = '''        if(n>=30&&hits/(double)n>=.35&&brier<=.24&&ece<=.08&&next!=State.QUARANTINED&&next!=State.QUALITY_PRUNED&&next!=State.PAUSED){'''
if s.count(elite) != 1:
    raise SystemExit('R932_PAUSE_ELITE_ANCHOR_COUNT=' + str(s.count(elite)))
s = s.replace(elite, elite_replacement, 1)

p.write_text(s, encoding='utf-8')
print('R932_PAUSE_STICKY_GUARD_APPLIED')

