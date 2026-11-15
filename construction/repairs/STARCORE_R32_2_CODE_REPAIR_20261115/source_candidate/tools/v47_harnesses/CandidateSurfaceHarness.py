from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text('utf-8')
    assert 'DYNAMIC_CAUSAL_REALTIME_CANDIDATE_CAP = AppConfig.V47_PROBABILITY_CANDIDATE_COUNT' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text('utf-8')
    assert 'Final40ContractAuditor.rank' in s
    print("PASS CandidateSurfaceHarness")
if __name__=="__main__": main()
