"""Verify frozen independent future evidence using the sealed original R34.1 protocol."""
import argparse
import json
from pathlib import Path
import subprocess
import sys
from verify_integrated_chain import verify_level


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("plan","repo","receipt","originals"):
        p.add_argument("--"+name,type=Path,required=True)
    a=p.parse_args()
    try:
        result=verify_level(a.plan,a.repo,a.receipt,a.originals,"NATURAL")
    except (OSError,ValueError,subprocess.SubprocessError) as error:
        result={"status":"NATURAL_PENDING","reason":str(error),"exitCode":2}
    print(json.dumps(result,indent=2))
    return result["exitCode"]


if __name__=="__main__":
    sys.exit(main())
