#!/usr/bin/env python3
"""Restore only the R13 signing key from an authenticated encrypted envelope."""
import base64, hashlib, json, os
from pathlib import Path
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.serialization.pkcs12 import load_key_and_certificates
SCHEMA='STARCORE_R13_SIGNING_V1'
def unseal(envelope,private_pem):
    if envelope['schema']!=SCHEMA:raise ValueError('SCHEMA')
    private=serialization.load_pem_private_key(private_pem,password=None)
    public=private.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
    if hashlib.sha256(public).hexdigest()!=envelope['public_key_sha256']:raise ValueError('KEY_BINDING')
    decode=lambda x:base64.b64decode(x,validate=True)
    key=private.decrypt(decode(envelope['wrapped_key']),padding.OAEP(mgf=padding.MGF1(hashes.SHA256()),algorithm=hashes.SHA256(),label=None))
    payload=json.loads(AESGCM(key).decrypt(decode(envelope['nonce']),decode(envelope['ciphertext']),SCHEMA.encode()))
    blob=decode(payload['keystore']);password=payload['password']
    if not password or any(c in password for c in '\r\n') or payload['alias']!='starcore-r13':raise ValueError('PAYLOAD')
    _,cert,_=load_key_and_certificates(blob,password.encode())
    actual=cert.fingerprint(hashes.SHA256()).hex()
    if actual!=payload['certificate_sha256'] or actual!=envelope['certificate_sha256']:raise ValueError('CERT_BINDING')
    return blob,password,actual

def main():
    try:
        envelope=json.loads(Path('.github/r13-signing.sealed.json').read_text())
        blob,password,fingerprint=unseal(envelope,(Path.home()/'.starcore/su1-api-transport/private.pem').read_bytes())
        path=Path(os.environ['RUNNER_TEMP'])/'starcore-r13.p12';path.write_bytes(blob);path.chmod(0o600)
        print('::add-mask::'+password,flush=True)
        with open(os.environ['GITHUB_ENV'],'a') as out:
            out.write('R13_SIGNING_P12='+str(path)+'\nR13_SIGNING_PASSWORD='+password+'\nR13_EXPECTED_CERT_SHA256='+fingerprint+'\n')
        print('Fixed independent R13 signing identity restored: '+fingerprint)
        return 0
    except Exception:
        print('R13_SIGNING_RESTORE_FAILED: signing identity unavailable or invalid')
        return 1
if __name__=='__main__':raise SystemExit(main())
