"""Recompute build identity from a checkout and check the original signed APK."""
import argparse
import json
from pathlib import Path
import sys

sys.path.insert(0,str(Path(__file__).resolve().parents[1]/"r33"))
import package_ci_candidate as package
import verify_plan_binding as binding


def verify(repo, r33_plan, r34_plan, r341_plan, apk, aapt, apksigner, build=None):
    repo=Path(repo).resolve()
    identity=binding.identity(repo,Path(r33_plan),repo/"release/r33_plan_binding.json",
                              require_clean=True,r34_plan=r34_plan,r341_plan=r341_plan)
    actual, _, _=package.inspect(apk,aapt,apksigner)
    package.embedded(apk,identity)
    owner=identity["r341"]
    binding.require(actual["package"]==package.PACKAGE and actual["versionCode"]==owner["versionCode"]
                    and actual["versionName"]==owner["versionName"],"R341_ACTUAL_APK_MANIFEST_MISMATCH")
    if build is not None:
        record=json.loads(Path(build).read_bytes())
        binding.require(record.get("sourceCommit")==owner["sourceCommit"] and record.get("sourceTree")==owner["sourceTree"]
                        and record.get("r341PlanManifestSha256")==owner["planSha256"]
                        and record.get("apkSha256")==actual["sha256"] and record.get("exitCode")==0
                        and record.get("sourceUnchangedDuringBuild") is True,
                        "R341_BUILD_RECEIPT_BYTE_IDENTITY")
    return {"status":"ORIGINAL_SIGNED_SOURCE_IDENTITY_VERIFIED","apk":actual,"buildIdentity":owner}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    for name in ("repo","r33-plan","r34-plan","r341-plan","apk","aapt","apksigner"):
        p.add_argument("--"+name,type=Path,required=True)
    p.add_argument("--build",type=Path)
    a=p.parse_args()
    try:
        print(json.dumps(verify(a.repo,a.r33_plan,a.r34_plan,a.r341_plan,a.apk,a.aapt,a.apksigner,a.build),indent=2))
        return 0
    except (OSError,ValueError,KeyError) as error:
        print(json.dumps({"status":"IDENTITY_FAILED","reason":str(error)}))
        return 1


if __name__=="__main__":
    sys.exit(main())
