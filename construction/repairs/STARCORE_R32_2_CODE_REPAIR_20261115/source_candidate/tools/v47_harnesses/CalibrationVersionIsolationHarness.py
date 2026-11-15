from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/ConditionalCalibrationEngine.java').read_text('utf-8')
    assert 'AND version=?' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/VersionScopedCalibrationEngine.java').read_text('utf-8')
    assert 'build_id=? AND cohort_id=?' in s
    print("PASS CalibrationVersionIsolationHarness")
if __name__=="__main__": main()
