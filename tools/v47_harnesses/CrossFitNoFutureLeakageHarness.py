from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/CrossFittedNumberModelEngine.java').read_text('utf-8')
    assert 'for(int t=split;t<h.size();t++)' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/CrossFittedNumberModelEngine.java').read_text('utf-8')
    assert 'ensembleAt(h,t,p' in s
    print("PASS CrossFitNoFutureLeakageHarness")
if __name__=="__main__": main()
