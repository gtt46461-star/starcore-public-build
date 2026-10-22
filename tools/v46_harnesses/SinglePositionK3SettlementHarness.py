from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
s=text("app/src/main/java/com/ethk3/novaonepro/SettlementEngine.java")
require("position" in s and "n1" in s and "n2" in s and "n3" in s,"single-position K3 settlement fields missing")
require("K4" not in s,"K4 settlement path found")
print("PASS SinglePositionK3SettlementHarness")
