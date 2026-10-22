from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
n=text("app/src/main/java/com/ethk3/novaonepro/NumberDistributionEngineV46.java"); p=text("app/src/main/java/com/ethk3/novaonepro/PredictionEnvelope.java")
require("p1 + p2 + p3" in n,"K3 event decomposition missing")
require("v46EventProbability" in p and "v46ActionLcb" in p,"EventP/ActionLCB fields missing")
print("PASS ProbabilityDecompositionHarness")
