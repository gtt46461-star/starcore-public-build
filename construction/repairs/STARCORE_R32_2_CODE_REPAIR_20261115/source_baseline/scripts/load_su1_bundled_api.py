#!/usr/bin/env python3
"""Load an authorized, RSA-OAEP sealed API credential into the build environment.

Only ciphertext is tracked. The matching private transport key stays in the CI
cache; neither the source credential nor the private key is exported as evidence.
"""
import argparse
import base64
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys


def bundled_credential(envelope_path, private_key):
    if not envelope_path.is_file():
        candidate = os.environ.get("DUOQU_API_KEY", "").strip()
    else:
        envelope = json.loads(envelope_path.read_text(encoding="utf-8"))
        expected = {
            "schema", "public_key_sha256", "ciphertext_base64",
            "source_repository", "source_commit", "source_path", "source_blob",
        }
        if set(envelope) != expected or envelope["schema"] != "SU1_RSA_OAEP_SHA256_V1":
            raise ValueError("ENVELOPE_INVALID")
        if not re.fullmatch(r"[0-9a-f]{64}", envelope["public_key_sha256"]):
            raise ValueError("PUBLIC_KEY_BINDING_INVALID")
        if not private_key.is_file():
            raise ValueError("PRIVATE_TRANSPORT_KEY_UNAVAILABLE")
        public = subprocess.run(
            ["openssl", "pkey", "-in", str(private_key), "-pubout", "-outform", "DER"],
            check=True, capture_output=True,
        ).stdout
        if hashlib.sha256(public).hexdigest() != envelope["public_key_sha256"]:
            raise ValueError("PRIVATE_TRANSPORT_KEY_MISMATCH")
        ciphertext = base64.b64decode(envelope["ciphertext_base64"], validate=True)
        if not 256 <= len(ciphertext) <= 512:
            raise ValueError("CIPHERTEXT_LENGTH_INVALID")
        candidate = subprocess.run(
            ["openssl", "pkeyutl", "-decrypt", "-inkey", str(private_key),
             "-pkeyopt", "rsa_padding_mode:oaep",
             "-pkeyopt", "rsa_oaep_md:sha256",
             "-pkeyopt", "rsa_mgf1_md:sha256"],
            input=ciphertext, check=True, capture_output=True,
        ).stdout.decode("ascii")
    if candidate and not re.fullmatch(r"[A-Za-z0-9]{8,128}", candidate):
        raise ValueError("CREDENTIAL_INVALID")
    return candidate


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--envelope", type=Path, default=Path(".github/su1-api-credential.sealed.json"))
    parser.add_argument("--private-key", type=Path,
                        default=Path.home() / ".starcore/su1-api-transport/private.pem")
    args = parser.parse_args()
    try:
        credential = bundled_credential(args.envelope, args.private_key)
        if credential:
            # GitHub consumes this control command and masks subsequent output.
            print("::add-mask::" + credential, flush=True)
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
            output.write("DUOQU_API_KEY=" + credential + "\n")
        print("Bundled API credential loaded: " + str(bool(credential)))
    except Exception:
        # Do not include exception text, subprocess buffers or credential values.
        print("SU1_API_CREDENTIAL_LOAD_FAILED: sealed credential or CI key unavailable/invalid", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
