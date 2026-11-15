from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
a=text("app/src/main/java/com/ethk3/novaonepro/AppConfig.java"); f=text("app/src/main/java/com/ethk3/novaonepro/FullCandidateScanEngine.java"); r=text("README.md")
require("V46_FULL_SCAN_CANDIDATE_COUNT = 1200" in a,"1200 full scan constant missing")
require("GLOBAL_DECISION_CANDIDATE_LIMIT = AppConfig.V46_PROBABILITY_CANDIDATE_COUNT" in f,"Top60 cut missing")
require("PRESELECT160" not in r and "160个结构预选" not in r,"hidden 160 stage remains")
print("PASS CandidateFunnelCompletenessHarness")
