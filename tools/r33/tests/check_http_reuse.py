#!/usr/bin/env python3
"""Exercise the installed release's HTTPS polling and cancellation on a disposable Android emulator."""
import argparse
import hashlib
import http.server
import json
import os
from pathlib import Path
import re
import ssl
import subprocess
import tempfile
import threading


class PollOrigin(http.server.BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def respond(self):
        if self.path != "/draw":
            self.send_error(404)
            return
        self.rfile.read(int(self.headers.get("Content-Length", "0")))
        self.server.requests.append((self.command, self.client_address[1], self.headers.get("Connection", "")))
        body = b'{"acknowledged":true}'
        self.send_response(200)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    do_GET = do_POST = respond

    def log_message(self, *_):
        pass


def run(args):
    def adb(*parts):
        return subprocess.check_output(["adb", "-s", args.serial, *map(str, parts)], text=True, timeout=150).strip()

    if adb("shell", "getprop", "ro.kernel.qemu") != "1":
        raise ValueError("Select the disposable emulator used to run the transport check")
    paths = adb("shell", "pm", "path", "com.starcore.research.r13").splitlines()
    apk = next(line.removeprefix("package:") for line in paths if line.endswith("/base.apk"))
    digest = adb("shell", "sha256sum", apk).split()[0]
    if not re.fullmatch(r"[a-f0-9]{64}", args.apk_sha256) or digest != args.apk_sha256:
        raise ValueError("Installed APK SHA-256 mismatch")

    source = Path(__file__).with_name("PoolProbe.java")
    sdk = Path(os.environ["ANDROID_HOME"])
    android = sdk / "platforms/android-36/android.jar"
    with tempfile.TemporaryDirectory(prefix="starcore-http-") as work:
        temp = Path(work)
        key, cert = temp / "origin.key", temp / "origin.pem"
        subprocess.run(["openssl", "req", "-x509", "-newkey", "rsa:2048", "-nodes",
                        "-keyout", key, "-out", cert, "-days", "1", "-subj", "/CN=localhost",
                        "-addext", "subjectAltName=IP:127.0.0.1,DNS:localhost"], check=True, capture_output=True)
        classes = temp / "classes"
        classes.mkdir()
        subprocess.run(["javac", "--release", "11", "-cp", os.pathsep.join(map(str, (android, args.classes))),
                        "-d", classes, source], check=True)
        subprocess.run(["jar", "cf", temp / "classes.jar", "-C", classes, "."], check=True)
        dex = temp / "probe.jar"
        subprocess.run([sdk / "build-tools/36.0.0/d8", "--min-api", "24", "--lib", android,
                        "--classpath", args.classes, "--output", dex, temp / "classes.jar"], check=True)
        with http.server.ThreadingHTTPServer(("127.0.0.1", 0), PollOrigin) as server:
            server.requests = []
            tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
            tls.load_cert_chain(cert, key)
            server.socket = tls.wrap_socket(server.socket, server_side=True)
            thread = threading.Thread(target=server.serve_forever, daemon=True)
            thread.start()
            remote = adb("shell", "mktemp", "-d", "/data/local/tmp/starcore-http.XXXXXX")
            device_port = None
            try:
                for file in (dex, cert):
                    location = remote + "/" + file.name
                    adb("push", file, location)
                    adb("shell", "chmod", "0444", location)
                    if adb("shell", "sha256sum", location).split()[0] != hashlib.sha256(file.read_bytes()).hexdigest():
                        raise ValueError("Device fixture differs")
                device_port = int(adb("reverse", "tcp:0", "tcp:" + str(server.server_port)))
                client = adb("shell", "env", "CLASSPATH=" + apk + ":" + remote + "/probe.jar", "app_process",
                             "/system/bin", "com.ethk3.novaonepro.PoolProbe",
                             f"https://127.0.0.1:{device_port}/draw", remote + "/origin.pem")
            finally:
                server.shutdown()
                thread.join()
                if device_port is not None:
                    adb("reverse", "--remove", f"tcp:{device_port}")
                adb("shell", "rm", "-f", remote + "/probe.jar", remote + "/origin.pem")
                adb("shell", "rmdir", remote)

            calls = [row[0] for row in server.requests]
            connections = len({row[1] for row in server.requests})
            print(json.dumps({"apkSha256": digest, "requests": calls, "serverConnections": connections,
                              "connectionHeaders": [row[2] for row in server.requests], "client": client}, indent=2))
            if calls != ["GET", "POST", "GET", "GET"] or connections != 1:
                raise AssertionError("Fully consumed polls did not reuse one TLS connection")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--apk-sha256", required=True)
    parser.add_argument("--classes", type=Path, required=True, help="Compiled STARCORE Java classes")
    run(parser.parse_args())
