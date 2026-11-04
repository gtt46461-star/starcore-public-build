# R31 B00-C01 baseline and build identity evidence

Fixed source baseline: `5237b265a27fe5ad27fd842901c445d5f8c27749`.
Plan manifest SHA-256: `13f34908b624c74e2852ce8f944b99f8978348820aab23f83043fab1d72753ce`.

`test-receipts.json` records actual command times, exit codes and logs. Twelve Python verifier tests passed. They cover missing/changed/sealed members, unsafe paths, exact Git blobs, an unrelated branch, retained follow-up changes, dirty publication rejection, and APK inspection adapter rejection. The two APK adapter fixtures mock tool output; they are not installable APKs and do not establish cryptographic or device acceptance.

`baseline-verification.json` verifies all 405 sealed members and the 190 source anchors against the complete repository's original Git objects. The original 1832 requirements, 299 historical file cards, and 1832 preservation mappings retain their pinned bytes and original statuses. This proves preservation only; it does not accept those requirements as implemented.

Current source differences are reported rather than replaced. `--strict-baseline` rejects applying original line anchors to changed files. `--require-clean` rejects dirty publication source. The rejection receipts are expected negative test results, not a successful release build.

The Gradle integration adds committed tree, actual product input digest, dirty flag, fixed baseline, plan manifest digest and migration contract digest to BuildConfig and a generated diagnostic asset. The actual-input digest excludes generated build output, evidence logs, the separately sealed plan and private signing inputs. It includes new source files. Final assembly checks that those bytes did not change during the build. Release assembly requires a clean checkout.

Run integration after source writers have stopped:

```sh
source /workspace/starcore-environment/activate.sh
bash ./gradlew --no-daemon :app:generateR31BuildIdentity :app:generateDebugBuildConfig :app:verifyFinalR31BuildIdentity
python3 tools/r31/verify_baseline.py --require-clean --output /tmp/r31-clean-baseline.json
```

To verify a real build output without reading private signing material:

```sh
python3 tools/r31/verify_baseline.py --require-clean --apk /path/to/output.apk \
  --apksigner "$ANDROID_HOME/build-tools/36.0.0/apksigner" \
  --aapt "$ANDROID_HOME/build-tools/36.0.0/aapt" \
  --expected-cert-sha256 PUBLIC_EXPECTED_CERTIFICATE_SHA256 \
  --output /tmp/r31-apk-identity.json
```

This agent did not run Gradle, sign/build an APK, or access an Android device. Those fields remain `NOT_RUN`/`NOT_VERIFIED_HERE`. These initial dirty worktree receipts are construction observations; regenerate a clean release receipt at the actual delivery commit. Natural strategy quality remains `NOT_PROVEN`.
