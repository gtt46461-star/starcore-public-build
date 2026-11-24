"""Revalidate B12 against sealed source-owner and live lineage witness contracts."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
from build_identity import verify_seal


def verify_level(plan,repo,receipt,originals,level):
    repo,plan=Path(repo).resolve(),Path(plan).resolve()
    head=subprocess.check_output(["git","-C",repo,"rev-parse","HEAD"],text=True).strip()
    verify_seal(repo,head,plan)
    completed=subprocess.run([sys.executable,"-B",plan/"tools/verify_receipt.py","--plan",plan,
        "--receipt",receipt,"--evidence-root",originals,"--repo",repo,"--level",level],
        capture_output=True,text=True)
    if completed.returncode not in (0,2):
        return {"status":"INVALID_OR_INCOMPLETE_ORIGINALS","reason":completed.stderr.strip(),
                "exitCode":completed.returncode}
    output=json.loads(completed.stdout)
    output["exitCode"]=completed.returncode
    return output


def verify_chain(plan,repo,receipt,originals):
    record=json.loads(Path(receipt).read_bytes())
    if record.get("batch")!="B12":
        raise ValueError("B12_SOURCE_OWNER_RECEIPT_REQUIRED")
    if record.get("status")=="PASS" and not any(
            test.get("id")=="A-B12-03b" for test in record.get("test_results",[])):
        raise ValueError("B12_LIVE_LINEAGE_CASE_REQUIRED")
    return verify_level(plan,repo,receipt,originals,"ENGINEERING")


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("plan","repo","receipt","originals"):
        p.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    try:
        result=verify_chain(a.plan,a.repo,a.receipt,a.originals)
    except (OSError,ValueError,subprocess.SubprocessError) as error:
        result={"status":"BLOCKED_ORIGINALS","reason":str(error),"exitCode":2}
    print(json.dumps(result,indent=2))
    return result["exitCode"]


if __name__=="__main__":
    sys.exit(main())
