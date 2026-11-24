"""Exercise prefix rules using temporary real Git history; stub APK SDK and engineering only."""
import copy
import hashlib
import io
import json
import subprocess
import tempfile
import unittest
import zipfile
from pathlib import Path
from unittest.mock import patch
import verify_batch_builds as v


def put(path, value):
    path.parent.mkdir(parents=True,exist_ok=True)
    raw=value if isinstance(value,bytes) else json.dumps(value).encode()
    path.write_bytes(raw)
    return hashlib.sha256(raw).hexdigest()


class Response(io.BytesIO):
    status = 200
    def geturl(self):
        return "https://example.com/build.apk"


class Prefix(unittest.TestCase):
    def setUp(self):
        temp=tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        folder=Path(temp.name)
        self.repo,self.ev=folder/"git",folder/"artifacts"
        self.repo.mkdir()
        self.ev.mkdir()
        self.git("init","-q")
        self.git("config","user.email","fixture@example.invalid")
        self.git("config","user.name","fixture")
        put(self.repo/"release/r33_plan_binding.json",{"planRevision":"R33.5","planManifestSha256":"7"*64})
        self.baseline=self.commit("122")
        self.plan=self.repo/"plan"
        put(self.plan/"control/PLAN_IDENTITY.json",{"plan_path":"plan","application_baseline_commit":self.baseline,
                                                  "application_baseline_version_code":577311122})
        self.seal=put(self.plan/"MANIFEST_SHA256.json", {"local":"fixture"})
        self.plan_commit=self.commit("plan")
        self.latest=self.plan_commit
        self.rows=[]
        self.index={"schema":"STARCORE_R341_BUILD_PREFIX_1","planCommit":self.plan_commit,"batches":self.rows}
        self.addCleanup(patch.stopall)
        patch.object(v,"verify_seal",return_value={"manifest_sha256":self.seal}).start()
        self.engine=patch.object(v,"validate",return_value={"status":"PASS_RECEIPT_STRUCTURE_AND_DIGESTS"}).start()
        self.add("B01",577311123)
        self.add("B02",577311124)

    def git(self,*argv):
        return subprocess.check_output(["git","-C",str(self.repo),*argv],text=True).strip()

    def commit(self,msg):
        self.git("add",".")
        self.git("commit","--allow-empty","-qm",msg)
        return self.git("rev-parse","HEAD")

    def add(self,batch,version):
        vm=f"R34.1-{batch}-2026-11-24-{version}"
        put(self.repo/"release/release_manifest.json",{"versionCode":version,"versionName":vm,"applicationId":v.PACKAGE})
        binding=dict(planSha256=self.seal,planCommit=self.plan_commit,sourceBaseline=self.baseline)
        put(self.repo/"release/r341_plan_binding.json",binding)
        source=self.commit(batch);tree=self.git("rev-parse",source+"^{tree}")
        folder=self.ev/batch
        def doc(name,val):
            digest=put(folder/name,val)
            return {"path":batch+"/"+name,"sha256":digest}
        eng=dict(batch=batch,status="PASS",level="ENGINEERING",source_before=self.latest,source_after=source,
                 plan_manifest_sha256=self.seal)
        receipt=doc("receipt.json",eng)
        self.latest=source
        member="assets/r341.json"
        with zipfile.ZipFile(folder/"app.apk","w") as z:
            for name in ("classes.dex","AndroidManifest.xml"):
                z.writestr(name,b"TEST ONLY")
            z.writestr(member,json.dumps(dict(binding,batch=batch,sourceCommit=source,sourceTree=tree,
                                               versionCode=version,versionName=vm)))
        apk={"path":batch+"/app.apk","sha256":v.sha(folder/"app.apk")}
        log=doc("gradle.log",b"BUILD SUCCESSFUL\n")
        b=dict(sourceCommit=source,sourceTree=tree,r341Batch=batch,sourceClean=True,sourceCleanBeforeBuild=True,
               sourceUnchangedDuringBuild=True,gradleRelease="PASS",exitCode=0,wrapperExitCode=0,identityError=None,
               buildLaunchError=None,planManifestSha256="7"*64,gradleLogSha256=log["sha256"],apkSha256=apk["sha256"],r341PlanCommit=self.plan_commit,
               r341PlanManifestSha256=self.seal,command=["/build/gradlew",*v.TASKS],durationNs=10)
        self.rows.append(dict(batch=batch,engineering_receipt=receipt,build_receipt=doc("CI_BUILD_RECEIPT.json",b),
                              apk=apk,apk_identity_member=member,gradle_log=log,download_url="https://example.com/"+batch))

    def check(self,rows=None,last="B02",fetch=False):
        index=dict(self.index,batches=self.rows if rows is None else rows)
        return v.verify_prefix(self.plan,self.repo,self.plan_commit,last,index,self.ev,None,None,fetch)

    def test_incremental_prefix_and_signature_scope(self):
        early=self.check(self.rows[:1],"B01");self.assertEqual(early[0]["build"],"LOCAL_CANDIDATE_SDK_NOT_CHECKED")
        def fake_sdk(apk,*_):
            batch=apk.parent.name;code=577311122+int(batch[1:])
            return v.PACKAGE,code,f"R34.1-{batch}-2026-11-24-{code}",True
        def public(url,timeout):
            batch=url.rsplit("/",1)[-1]
            return Response((self.ev/batch/"app.apk").read_bytes())
        with patch.object(v,"sdk_identity",side_effect=fake_sdk),patch.object(v.urllib.request,"urlopen",side_effect=public):
            full=self.check(fetch=True)
            self.assertTrue(all(r["build"]=="ORIGINAL_SIGNED" and r["download"]=="READBACK_IDENTICAL" for r in full))
        row=copy.deepcopy(self.rows[0]);row.update(apk=None,build_receipt=None,download_url=None,reason="original key unavailable")
        self.assertEqual(self.check([row],"B01")[0]["build"],"BUILD_PENDING")
        self.assertGreater(self.engine.call_count,0)

    def test_sources_tasks_log_and_versions(self):
        for kind in ("skip","swap","different-apk","old-seal","forged-historical","wrapper","boolean-exit","task","wrong-source","fake-log","engine","same-version"):
            with self.subTest(kind=kind):
                rows=copy.deepcopy(self.rows);row=rows[-1]
                if kind=="skip":rows.pop(0)
                elif kind=="swap":rows.reverse()
                elif kind=="different-apk":row["apk"]=rows[0]["apk"]
                elif kind=="engine":row["engineering_receipt"]=rows[0]["engineering_receipt"]
                elif kind=="same-version":
                    with self.assertRaises(ValueError):
                        v.check_build(row,self.ev,self.repo,dict(sourceCommit=self.latest,sourceTree=self.git("rev-parse","HEAD^{tree}"),
                            sourceBaseline=self.baseline,planCommit=self.plan_commit,planSha256=self.seal,batch="B02"),577311124,None,None,False)
                    continue
                else:
                    obj=v.read(self.ev/row["build_receipt"]["path"])
                    if kind=="old-seal":obj["r341PlanManifestSha256"]="old"
                    if kind=="forged-historical":obj["planManifestSha256"]="e"*64
                    if kind=="wrapper":obj["wrapperExitCode"]=1
                    if kind=="boolean-exit":obj["exitCode"]=False
                    if kind=="task":obj["command"]=["gradlew",":app:tasks"]
                    if kind=="wrong-source":obj["sourceCommit"]=self.baseline
                    if kind=="fake-log":
                        row["gradle_log"]={"path":"fake.log","sha256":put(self.ev/"fake.log",b"not a build")}
                        obj["gradleLogSha256"]=row["gradle_log"]["sha256"]
                    row["build_receipt"]={"path":"changed.json","sha256":put(self.ev/"changed.json",obj)}
                with self.assertRaises(ValueError):self.check(rows)

    def test_android_tool_outputs_must_identify_exact_signer(self):
        from types import SimpleNamespace
        aapt = SimpleNamespace(stdout="package: name='pkg' versionCode='123' versionName='v'\n")
        signed = "Verifies\nSigner #1 certificate SHA-256 digest: " + v.CERT + "\n"
        for code, tail, okay in ((0,"",True),(1,"",False),(0,"Signer #2 certificate SHA-256 digest: "+"f"*64+"\n",False)):
            cert = SimpleNamespace(stdout=signed+tail,stderr="",returncode=code)
            with patch.object(v.subprocess,"run",side_effect=[aapt,cert]):
                self.assertEqual(v.sdk_identity(Path("fixture.apk"),"aapt","apksigner"),("pkg",123,"v",okay))

    def test_download_inside_published_zip(self):
        apk=self.ev/self.rows[0]["apk"]["path"]
        content=io.BytesIO()
        with zipfile.ZipFile(content,"w") as z:
            z.write(apk,"payload/app.apk")
        raw=content.getvalue()
        container=dict(bytes=len(raw),sha256=hashlib.sha256(raw).hexdigest(),apk_member="payload/app.apk")
        for member,okay in (("payload/app.apk",True),("another.apk",False)):
            with patch.object(v.urllib.request,"urlopen",return_value=Response(raw)):
                if okay:v.check_download("https://example.com/export.zip",apk,container)
                else:
                    with self.assertRaises(ValueError):v.check_download("https://example.com/export.zip",apk,dict(container,apk_member=member))

    def test_wrong_signer_smaller_download_missing_evidence(self):
        code=577311123
        with patch.object(v,"sdk_identity",return_value=(v.PACKAGE,code,f"R34.1-B01-2026-11-24-{code}",False)):
            self.assertEqual(self.check(self.rows[:1],"B01")[0]["build"],"SIGNATURE_NOT_ORIGINAL_OR_INVALID")
        with patch.object(v.urllib.request,"urlopen",return_value=Response(b"<html>error</html>")), self.assertRaises(ValueError):
            self.check(self.rows[:1],"B01",fetch=True)
        with patch.object(v,"validate",return_value={"status":"RECORDED_NONPASS"}),self.assertRaises(ValueError):
            self.check(self.rows[:1],"B01")


if __name__=="__main__":
    unittest.main()
