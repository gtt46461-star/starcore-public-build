"""Regression probes for file-level execution receipts. Uses a real temporary Git commit."""
import hashlib
import json
import subprocess
import tempfile
import unittest
from pathlib import Path

from owner_receipt4 import check_owner_probes


class Evidence:
    def __init__(self, root, documents, payload):
        self.docs = documents
        self.files, self.hashes = {}, {}
        for key, data in {**{k: json.dumps(v).encode() for k, v in documents.items()}, "payload": payload}.items():
            self.files[key] = root / key
            self.files[key].write_bytes(data)
            self.hashes[key] = hashlib.sha256(data).hexdigest()

    def json(self, name):
        return self.docs[name]


class OwnerProbeTest(unittest.TestCase):
    def setUp(self):
        temp = tempfile.TemporaryDirectory()
        self.addCleanup(temp.cleanup)
        self.root = Path(temp.name)
        def git(*args):
            return subprocess.check_output(["git", "-C", str(self.root), *args],
                                           stderr=subprocess.PIPE, text=True).strip()
        git("init", "-q")
        (self.root / "current.py").write_text("def submit(x): return x\n")
        git("add", "current.py")
        git("-c","user.name=Unit","-c","user.email=test@invalid","commit","-qm","baseline")
        self.commit, self.blob = git("rev-parse","HEAD"), git("rev-parse","HEAD:current.py")
        self.case = {"id":"case", "inherited_path_probes":[{"key":"old.py", "effective_path":"current.py",
                     "contract_sha256":"a"*64, "probe":"runner consumed the released bytes"}]}
        self.run = {"source_after":self.commit, "owner_probe_results":[{"key":"old.py", "contract_sha256":"a"*64,
                    "positive_metric":"ok", "negative_metric":"fail"}]}
        self.observations = [{"metric":m,"evidence_artifact":"readout","json_pointer":f"/{i}/count"}
                             for i,m in enumerate(("ok","fail"))]
        common = {"case_id":"case","source_commit":self.commit,"business_id":"service/42"}
        self.payload = b"frozen result #42"
        sha = hashlib.sha256(self.payload).hexdigest()
        self.documents = {"readout":[{**common, "polarity":p,"direction_key":"old.py",
                          "contract_sha256":"a"*64, "count":i} for i,p in enumerate(("positive","negative"))],
                          "events":[{**common,"event_id":"p","output_digest":sha,"effective_path":"current.py",
                                     "effective_blob_sha":self.blob},{**common,"event_id":"q","input_digest":sha}]}
        self.documents["readout"][0].update(effective_path="current.py", effective_blob_sha=self.blob,
             producer={"artifact":"events","json_pointer":"/0/event_id"},
             consumer={"artifact":"events","json_pointer":"/1/event_id"},payload_artifact="payload")

    def check(self):
        e = Evidence(self.root,self.documents,self.payload)
        check_owner_probes(self.case,self.run,self.observations,e,self.root)

    def test_effective_replacement_and_separate_events(self):
        self.check()

    def test_source_mismatch(self):
        self.documents["readout"][0]["effective_blob_sha"] = "f"*40
        with self.assertRaises(ValueError): self.check()

    def test_metadata_and_direction_metric_reuse(self):
        self.run["direction_results"] = [{"positive_metric":"ok","negative_metric":"other"}]
        with self.assertRaises(ValueError): self.check()
        self.run.pop("direction_results")
        self.observations[0]["json_pointer"] = "/0/effective_blob_sha"
        with self.assertRaises(ValueError): self.check()

    def test_wrong_flow_digest_or_source(self):
        for field,value in (("business_id","z"),("input_digest","c"*64),("source_commit","0"*40),("event_id","p")):
            with self.subTest(field=field):
                rec = self.documents["events"][1]
                old = rec[field]
                rec[field] = value
                with self.assertRaises(ValueError): self.check()
                rec[field] = old

    def test_missing_measured_consumer(self):
        self.documents["readout"][0]["consumer"] = self.documents["readout"][0]["producer"]
        with self.assertRaises(ValueError): self.check()

    def test_payload_matches_bytes_and_endpoint_runs_owner(self):
        self.payload = b"altered"
        with self.assertRaises(ValueError): self.check()
        self.payload = b"frozen result #42"
        del self.documents["events"][0]["effective_path"]
        with self.assertRaises(ValueError): self.check()

    def test_missing_negative_and_extraneous_historical_result(self):
        self.observations.pop()
        with self.assertRaises(ValueError): self.check()
        check_owner_probes({"id":"old"}, {}, [], None, None)
        with self.assertRaises(ValueError): check_owner_probes({"id":"old"}, self.run, [], None, None)


if __name__ == "__main__":
    unittest.main()
