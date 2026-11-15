#!/usr/bin/env python3
"""Exercise real OpenSSL envelope interoperability and fail-closed build injection."""
import base64
import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest


SCRIPT = Path(__file__).with_name("load_su1_bundled_api.py")
SPEC = importlib.util.spec_from_file_location("su1_api_transport", SCRIPT)
MODULE = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(MODULE)


class ApiCredentialTransportTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory(prefix="su1-api-transport-")
        cls.root = Path(cls.temp.name)
        cls.private = cls.root / "private.pem"
        subprocess.run(["openssl", "genpkey", "-algorithm", "RSA", "-pkeyopt",
                        "rsa_keygen_bits:2048", "-out", str(cls.private)],
                       check=True, capture_output=True)
        cls.public = subprocess.run(
            ["openssl", "pkey", "-in", str(cls.private), "-pubout", "-outform", "DER"],
            check=True, capture_output=True).stdout
        cls.test_credential = "SyntheticContractOnly123"
        # Use an explicit public file so the plaintext and key have independent inputs.
        public_path = cls.root / "public.der"
        public_path.write_bytes(cls.public)
        encrypted = subprocess.run(
            ["openssl", "pkeyutl", "-encrypt", "-pubin", "-keyform", "DER",
             "-inkey", str(public_path), "-pkeyopt", "rsa_padding_mode:oaep",
             "-pkeyopt", "rsa_oaep_md:sha256", "-pkeyopt", "rsa_mgf1_md:sha256"],
            input=cls.test_credential.encode("ascii"), check=True, capture_output=True).stdout
        cls.envelope = {
            "schema": "SU1_RSA_OAEP_SHA256_V1",
            "public_key_sha256": hashlib.sha256(cls.public).hexdigest(),
            "ciphertext_base64": base64.b64encode(encrypted).decode("ascii"),
            "source_repository": "synthetic/test",
            "source_commit": "0" * 40,
            "source_path": "synthetic",
            "source_blob": "0" * 40,
        }

    @classmethod
    def tearDownClass(cls):
        cls.temp.cleanup()

    def write_envelope(self, name, value):
        path = self.root / name
        path.write_text(json.dumps(value), encoding="utf-8")
        return path

    def test_actual_oaep_round_trip_and_github_environment(self):
        path = self.write_envelope("valid.json", self.envelope)
        self.assertEqual(self.test_credential, MODULE.bundled_credential(path, self.private))
        output = self.root / "github-env"
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "--envelope", str(path), "--private-key", str(self.private)],
            env={**os.environ, "GITHUB_ENV": str(output)}, capture_output=True, text=True)
        self.assertEqual(0, result.returncode)
        self.assertEqual("DUOQU_API_KEY=" + self.test_credential + "\n", output.read_text())
        self.assertEqual("::add-mask::" + self.test_credential, result.stdout.splitlines()[0])

    def test_wrong_private_key_and_unknown_fields_are_rejected(self):
        value = {**self.envelope, "public_key_sha256": "0" * 64}
        with self.assertRaisesRegex(ValueError, "MISMATCH"):
            MODULE.bundled_credential(self.write_envelope("wrong-key.json", value), self.private)
        value = {**self.envelope, "plaintext": "unsupported"}
        with self.assertRaisesRegex(ValueError, "ENVELOPE_INVALID"):
            MODULE.bundled_credential(self.write_envelope("unknown.json", value), self.private)

    def test_corrupted_ciphertext_cannot_publish_a_build_credential(self):
        value = {**self.envelope, "ciphertext_base64": base64.b64encode(b"\0" * 256).decode("ascii")}
        path = self.write_envelope("corrupt.json", value)
        output = self.root / "must-not-exist-env"
        result = subprocess.run(
            [sys.executable, str(SCRIPT), "--envelope", str(path), "--private-key", str(self.private)],
            env={**os.environ, "GITHUB_ENV": str(output)}, capture_output=True, text=True)
        self.assertNotEqual(0, result.returncode)
        self.assertFalse(output.exists())
        self.assertNotIn(self.test_credential, result.stdout + result.stderr)
        self.assertNotIn("BEGIN PRIVATE KEY", result.stdout + result.stderr)


if __name__ == "__main__":
    unittest.main()
