from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text('utf-8')
    assert 'Final40ContractAuditor.rank' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/FinalCandidateRankingEngine.java').read_text('utf-8')
    assert 'Double.compare(ev(b),ev(a))' not in s
    print("PASS FinalRankSingleOwnerHarness")
if __name__=="__main__": main()
