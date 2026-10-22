from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
a=text("app/src/main/java/com/ethk3/novaonepro/AppConfig.java"); f=text("app/src/main/java/com/ethk3/novaonepro/FinalCandidateRankingEngine.java")
for token in ["V46_FULL_SCAN_CANDIDATE_COUNT = 1200","V46_PROBABILITY_CANDIDATE_COUNT = 60","V46_FORMAL_CANDIDATE_COUNT = 40","V46_UI_DISPLAY_CANDIDATE_COUNT = 12"]: require(token in a,token)
require("Collections.sort(selected, comparator())" in f,"final rerank missing")
print("PASS FinalRankingRegressionHarness")
