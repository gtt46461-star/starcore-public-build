from pathlib import Path
ROOT=Path(__file__).resolve().parents[2]
def main():
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AppConfig.java').read_text('utf-8')
    assert 'STRICT_DECISION_GATE_ONLY_CAN_BET = true' in s
    s=(ROOT/'app/src/main/java/com/ethk3/novaonepro/AutoFetchService.java').read_text('utf-8')
    assert 'SIGN_ONLY_AUTH' in s
    print("PASS SingleBetAuthorizationHarness")
if __name__=="__main__": main()
