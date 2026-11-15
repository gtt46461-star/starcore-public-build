from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def text(p): return (ROOT/p).read_text(encoding="utf-8")
def require(ok,msg):
    if not ok: raise AssertionError(msg)
g=text("app/build.gradle"); a=text("app/src/main/java/com/ethk3/novaonepro/AppConfig.java"); b=text("app/src/main/java/com/ethk3/novaonepro/BuildIdentity.java")
require("V46.0.46072130" in g and "V46.0.46072130" in a,"V46 version mismatch")
require("V46_BUILD_IDENTITY_46072130" in b,"V46 identity owner missing")
print("PASS BuildIdentityRegressionHarness")
