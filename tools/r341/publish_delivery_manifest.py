"""Re-download the authorized files and build a byte-verified delivery manifest."""
import argparse
import hashlib
import json
from pathlib import Path
import re
import sys
import urllib.parse
import urllib.request
import zipfile

PREFIX="/gtt46461-star/starcore-public-build/releases/download/"


def collect(candidate,output):
    output.mkdir(parents=True,exist_ok=False)
    artifacts=[]
    for artifact in candidate["artifacts"]:
        name=artifact["filename"]
        if re.fullmatch(r"[A-Za-z0-9_][A-Za-z0-9_.-]{1,180}",name) is None:
            raise ValueError("DELIVERY_NAME_INVALID")
        url=urllib.parse.urlsplit(artifact["url"])
        if url.scheme!="https" or url.netloc!="github.com" or not url.path.startswith(PREFIX) or url.path.split("/")[-1]!=name:
            raise ValueError("UNAUTHORIZED_PUBLICATION_ADDRESS")
        expected=artifact["bytes"]; wanted=artifact["sha256"]
        if type(expected) is not int or expected<1 or re.fullmatch(r"[0-9a-f]{64}",wanted) is None:
            raise ValueError("DELIVERY_EXPECTED_IDENTITY_MISSING")
        path=output/name
        digest=hashlib.sha256();copied=0
        with urllib.request.urlopen(artifact["url"],timeout=45) as source,path.open("xb") as target:
            while block:=source.read(1024*1024):
                copied+=len(block)
                if copied>expected:
                    raise ValueError("DELIVERY_RESPONSE_EXCEEDS_EXPECTED_BYTES:"+name)
                target.write(block);digest.update(block)
        if copied!=expected or digest.hexdigest()!=wanted:
            raise ValueError("DELIVERY_DOWNLOAD_BYTE_MISMATCH:"+name)
        if path.suffix in (".apk",".zip"):
            with zipfile.ZipFile(path) as archive:
                if len(set(archive.namelist()))!=len(archive.namelist()) or archive.testzip() is not None:
                    raise ValueError("DELIVERY_ARCHIVE_CONTENT_INVALID:"+name)
                if any(re.search(r"\.(jks|p12|key|keystore|pfx)$",p,re.I) for p in archive.namelist()):
                    raise ValueError("PRIVATE_SIGNING_FILE_REJECTED")
        artifacts.append({**artifact,"downloadState":"READBACK_BYTES_MATCH"})
    return {"status":"DOWNLOAD_BYTES_VERIFIED","sourceCommit":candidate["sourceCommit"],"artifacts":artifacts}


def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument("--index",type=Path,required=True)
    p.add_argument("--out",type=Path,required=True)
    a=p.parse_args()
    try:
        report=collect(json.loads(a.index.read_bytes()),a.out)
        (a.out/"DELIVERY_R341.json").write_text(json.dumps(report,indent=2)+"\n")
        print(json.dumps(report,indent=2))
        return 0
    except (OSError,ValueError,KeyError,zipfile.BadZipFile) as error:
        print(json.dumps({"status":"DELIVERY_READBACK_FAILED","reason":str(error)}))
        return 1


if __name__=="__main__":
    sys.exit(main())
