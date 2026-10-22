from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/LanePolicyCompiler.java').read_text('utf-8')
    assert 'StartupContextEngine.resolve(store, c)' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/V47CandidateSurfaceContext.java').read_text('utf-8')
    assert 'STATE_VISIBLE_HISTORY' in s
    print("PASS StartupContextPersistenceHarness")
if __name__=="__main__": main()
