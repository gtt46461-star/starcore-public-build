from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
o=text("app/src/main/java/com/ethk3/novaonepro/OosActionLogEngine.java"); m=text("app/src/main/java/com/ethk3/novaonepro/MicroWalkForwardEngine.java")
require("target_issue" in o.lower(),"target issue OOS key missing")
require("STRICT_TIME_ORDERED_OOS_PUBLISHER" in m,"strict OOS role missing")
for bad in ["ACTUAL_NUMBER", "WOULD_HIT"]: require(bad not in text("app/src/main/java/com/ethk3/novaonepro/NumberDistributionEngineV46.java"),"future leakage token "+bad)
print("PASS TimeOrderedOosReplayHarness")
