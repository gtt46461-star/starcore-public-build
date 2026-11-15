from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text('utf-8')
    assert 'V47_MAX_ANALYSIS_LAG_ISSUES' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/LanePolicyCompiler.java').read_text('utf-8')
    assert 'RuntimeFreshnessGuard.evaluate' in s
    print("PASS RuntimeFreshnessHarness")
if __name__=="__main__": main()
