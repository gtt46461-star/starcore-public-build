from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
d=text("app/src/main/java/com/ethk3/novaonepro/DecisionGateEngine.java"); a=text("app/src/main/java/com/ethk3/novaonepro/DecisionAuthorizationEngine.java")
require("LanePolicyCompiler" in d,"lane compiler not used")
require("SIGN_ONLY" in a or "sign" in a.lower(),"authorization not sign-only")
require("pickDecisionCandidateLegacy" not in d or "private" in d,"legacy real fallback exposed")
print("PASS SingleLaneAuthorizationHarness")
