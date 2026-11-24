"""Measure exact planned work-order coverage using original per-level receipts."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
from verify_integrated_chain import verify_level


def validate(plan,repo,root):
    orders=json.loads((plan/"WORK_ORDERS.json").read_bytes())
    batches=[]
    for batch in orders["batches"]:
        key=batch["id"]
        row={"batch":key,"issues":[order["id"] for order in batch["issues"]]}
        for level in ("ENGINEERING","DEVICE","NATURAL"):
            directory=root/key
            receipt=directory/(level+".json")
            if not receipt.is_file():
                row[level]={"status":"NOT_RUN","reason":"ORIGINAL_RECEIPT_MISSING"}
                continue
            record=json.loads(receipt.read_bytes())
            if record.get("batch")!=key or record.get("level")!=level:
                raise ValueError("FOREIGN_RECEIPT:"+key+":"+level)
            row[level]=verify_level(plan,repo,receipt,directory,level)
            row["sourceCommit"]=record.get("source_after")
        batches.append(row)
    passed=sum(row["ENGINEERING"].get("exitCode")==0 for row in batches)
    return {"status":"ENGINEERING_COMPLETE" if passed==12 else "ENGINEERING_INCOMPLETE",
            "engineeringBatchesPassed":passed,"batchesRequired":12,
            "workOrdersInPlan":sum(len(row["issues"]) for row in batches),"batches":batches}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("plan","repo","receipts"):
        p.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    try:
        result=validate(a.plan,a.repo,a.receipts)
        print(json.dumps(result,indent=2))
        return 0 if result["status"]=="ENGINEERING_COMPLETE" else 2
    except (OSError,ValueError,KeyError,subprocess.SubprocessError) as error:
        print(json.dumps({"status":"RECEIPT_INTEGRITY_FAILURE","reason":str(error)}))
        return 1


if __name__=="__main__":
    sys.exit(main())
