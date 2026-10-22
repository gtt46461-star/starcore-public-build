from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/TimeBlockChampionEvaluator.java').read_text('utf-8')
    assert 'CHALLENGER_SHADOW' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text('utf-8')
    assert 'V47_CHAMPION_MIN_BRIER_GAIN' in s
    print("PASS ChampionPromotionHarness")
if __name__=="__main__": main()
