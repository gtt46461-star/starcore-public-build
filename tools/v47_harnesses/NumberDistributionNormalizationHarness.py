from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/CrossFittedNumberModelEngine.java').read_text('utf-8')
    assert 'normalize(s.probability[p-1])' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/NumberFeatureSnapshotV47.java').read_text('utf-8')
    assert '10x10' in s
    print("PASS NumberDistributionNormalizationHarness")
if __name__=="__main__": main()
