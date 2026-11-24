"""Run B12 engineering probes on a clean application checkout and explicit originals."""
import argparse
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
from build_identity import verify_seal
from verify_delivery_identity import binding
from verify_integrated_chain import verify_chain
from verify_batch_ledger import validate
from verify_upgrade_install import adb
from publish_delivery_manifest import collect

CASE_IDS={f"A-B12-{number:02d}{letter}" for number,letters in
          ((1,"ab"),(2,"b"),(3,"ab"),(4,"b"),(5,"ab"),(6,"ab")) for letter in letters}


def head(repo):
    return binding.git(repo,"rev-parse","HEAD").decode().strip()


def refuse(call,reason=None):
    try:
        call()
    except (OSError,ValueError) as error:
        if reason is not None and reason not in str(error):
            raise
        return str(error)
    raise ValueError("EXPECTED_REJECTION_NOT_OBSERVED")


def run(case,repo,plan,originals):
    if case=="A-B12-01a":
        seal=verify_seal(repo,head(repo),plan)
        rejections=[]
        with tempfile.TemporaryDirectory(prefix="r341-identity-") as directory:
            for name in ("MANIFEST_SHA256.json","WORK_ORDERS.json","control/PLAN_IDENTITY.json"):
                tested=Path(directory)/str(len(rejections))
                shutil.copytree(plan,tested,ignore=shutil.ignore_patterns("__pycache__"))
                damaged=tested/name
                damaged.write_bytes(damaged.read_bytes()+b"\n")
                rejections.append(refuse(lambda:verify_seal(repo,head(repo),tested),"R341_PLAN_"))
        inherited={}
        if os.getenv("STARCORE_R33_PLAN_ROOT") and os.getenv("STARCORE_R34_PLAN_ROOT"):
            inherited=binding.identity(repo,Path(os.environ["STARCORE_R33_PLAN_ROOT"]),repo/"release/r33_plan_binding.json",
                                       require_clean=True,r34_plan=Path(os.environ["STARCORE_R34_PLAN_ROOT"]),r341_plan=plan)["r341"]["legacyPlanSeals"]
        return {"verified":seal,"inheritedSeals":inherited,"tamperedPlansRejected":rejections}
    if case=="A-B12-01b":
        with tempfile.TemporaryDirectory(prefix="r341-build-bytes-") as directory:
            root=Path(directory)
            subprocess.run(["git","init","-q",root],check=True)
            file=root/"input";file.write_text("committed\n")
            binding.git(root,"add","input")
            binding.git(root,"-c","user.name=Fixture","-c","user.email=fixture@example.invalid","commit","-qm","fixture")
            binding.verify_checkout_bytes(root,head(root))
            file.write_text("changed during build\n")
            error=refuse(lambda:binding.verify_checkout_bytes(root,head(root)))
            return {"worktreeMutationRejected":error,"boundSource":head(repo)}
    if case in {"A-B12-02b","A-B12-04b"}:
        try:
            devices=adb(None,"devices","-l")
        except (OSError,subprocess.SubprocessError) as error:
            return {"physicalDevice":"NOT_RUN","reason":type(error).__name__}
        attached=[row for row in devices.splitlines()[1:] if len(row.split())>=2 and row.split()[1]=="device"]
        return {"physicalDevice":"FIELD_PENDING","adbAuthorizedDevices":len(attached),"hardwareListing":devices}
    if case=="A-B12-03a":
        return validate(plan,repo,originals)
    if case=="A-B12-03b":
        required=originals/"B12"/"ENGINEERING.json"
        return verify_chain(plan,repo,required,required.parent) if required.is_file() else {
            "status":"BLOCKED_ORIGINALS","reason":"SAME_SERVICE_DATASET_APK_RESTART_AND_DIVERSITY_WITNESSES_MISSING"}
    if case=="A-B12-05a":
        quality=originals/"B09"/"quality-experiment-receipt-137.json"
        coverage=json.loads(quality.read_bytes()) if quality.is_file() else None
        return {"status":"NATURAL_PENDING","independentFutureActs":None,"syntheticOnly":coverage}
    if case=="A-B12-05b":
        result=subprocess.run([sys.executable,"-B","-m","unittest","test_q06_receipt","test_q08_receipt","test_real_application5"],
                              cwd=plan/"tools",capture_output=True,text=True)
        return {"status":"SEALED_CONTRACT_REGRESSION","exitCode":result.returncode,"log":result.stdout+result.stderr}
    if case=="A-B12-06a":
        receipt=originals/"B12"/"DELIVERY_R341.json"
        return json.loads(receipt.read_bytes()) if receipt.is_file() else {
            "status":"NOT_RUN","reason":"FINAL_AUTHORIZED_DOWNLOAD_READBACK_MISSING"}
    if case=="A-B12-06b":
        invalid={"sourceCommit":head(repo),"artifacts":[{"filename":"old.apk","url":"https://example.invalid/old.apk",
                    "sha256":"0"*64,"bytes":10}]}
        with tempfile.TemporaryDirectory(prefix="r341-download-negative-") as directory:
            error=refuse(lambda:collect(invalid,Path(directory)/"download"),"UNAUTHORIZED_PUBLICATION_ADDRESS")
        return {"invalidPublicationRejected":error}
    raise ValueError("UNKNOWN_B12_CASE")


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--case",choices=sorted(CASE_IDS),required=True)
    p.add_argument("--repo",type=Path,required=True)
    p.add_argument("--out",type=Path,required=True)
    p.add_argument("--plan",type=Path,default=os.getenv("STARCORE_R341_PLAN_ROOT"))
    p.add_argument("--originals",type=Path,default=os.getenv("STARCORE_R341_ORIGINALS"))
    a=p.parse_args()
    a.repo=a.repo.resolve()
    plan=a.plan or a.repo/"construction/plans/STARCORE_R34_1_REPAIR_20261124"
    originals=a.originals or a.out.parent
    try:
        outcome=run(a.case,a.repo,plan,originals)
        result={"case":a.case,"sourceCommit":head(a.repo),"evidenceScope":"ENGINEERING_PROBE",
                "outcome":outcome}
        a.out.parent.mkdir(parents=True,exist_ok=True)
        with a.out.open("x") as handle:
            json.dump(result,handle,indent=2)
            handle.write("\n")
        print(json.dumps(result))
        incomplete = {"NOT_RUN","NATURAL_PENDING","FIELD_PENDING","BLOCKED_ORIGINALS","ENGINEERING_INCOMPLETE","RECORDED_NONPASS"}
        return 2 if outcome.get("status") in incomplete or outcome.get("physicalDevice") in incomplete else int(outcome.get("exitCode",0))
    except (OSError,ValueError,KeyError,subprocess.SubprocessError) as error:
        print(json.dumps({"status":"PROBE_FAILED","reason":str(error)}))
        return 1


if __name__=="__main__":
    sys.exit(main())
