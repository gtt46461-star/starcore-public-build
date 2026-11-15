#!/usr/bin/env python3
import base64,hashlib,json,os,unittest
from datetime import datetime,timedelta,timezone
from cryptography import x509
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import hashes,serialization
from cryptography.hazmat.primitives.asymmetric import rsa,padding
from cryptography.hazmat.primitives.ciphers.aead import AESGCM
from cryptography.hazmat.primitives.serialization import pkcs12
from load_r13_signing import unseal,SCHEMA
class SigningTransportTest(unittest.TestCase):
    def test_roundtrip_and_tamper_rejection(self):
        transport=rsa.generate_private_key(public_exponent=65537,key_size=2048)
        private=transport.private_bytes(serialization.Encoding.PEM,serialization.PrivateFormat.PKCS8,serialization.NoEncryption())
        public=transport.public_key().public_bytes(serialization.Encoding.DER,serialization.PublicFormat.SubjectPublicKeyInfo)
        signing=rsa.generate_private_key(public_exponent=65537,key_size=2048);name=x509.Name([x509.NameAttribute(NameOID.COMMON_NAME,'R13 test only')]);now=datetime.now(timezone.utc)
        cert=x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(signing.public_key()).serial_number(x509.random_serial_number()).not_valid_before(now-timedelta(minutes=1)).not_valid_after(now+timedelta(days=1)).sign(signing,hashes.SHA256())
        password='fixture-password';blob=pkcs12.serialize_key_and_certificates(b'starcore-r13',signing,cert,None,serialization.BestAvailableEncryption(password.encode()));fingerprint=cert.fingerprint(hashes.SHA256()).hex()
        b64=lambda b:base64.b64encode(b).decode();key=AESGCM.generate_key(bit_length=256);nonce=os.urandom(12)
        payload=json.dumps(dict(keystore=b64(blob),password=password,alias='starcore-r13',certificate_sha256=fingerprint)).encode()
        envelope=dict(schema=SCHEMA,public_key_sha256=hashlib.sha256(public).hexdigest(),wrapped_key=b64(transport.public_key().encrypt(key,padding.OAEP(mgf=padding.MGF1(hashes.SHA256()),algorithm=hashes.SHA256(),label=None))),nonce=b64(nonce),ciphertext=b64(AESGCM(key).encrypt(nonce,payload,SCHEMA.encode())),certificate_sha256=fingerprint)
        self.assertEqual((blob,password,fingerprint),unseal(envelope,private))
        for field in ('ciphertext','public_key_sha256','certificate_sha256'):
            bad=dict(envelope)
            if field=='ciphertext':value=bytearray(base64.b64decode(bad[field]));value[0]^=1;bad[field]=b64(value)
            else:bad[field]='0'*64
            with self.assertRaises(Exception):unseal(bad,private)
if __name__=='__main__':unittest.main()
