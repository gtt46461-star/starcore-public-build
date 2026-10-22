from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/CandidateDecisionFunnelLogEngine.java').read_text('utf-8')
    assert 'OosWriteAuditEngine.persist' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/OosWriteAuditEngine.java').read_text('utf-8')
    assert 'PredictionContractEngine.stampCandidate(c,null)' in s
    print("PASS Final40PersistenceHarness")
if __name__=="__main__": main()
