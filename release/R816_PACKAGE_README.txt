STARCORE TIANSHU V57.15.3-R3.6.13-816 — ONE-PACKAGE DELIVERY

START HERE

This archive contains the complete upgraded project source and assets, the signed installable APK,
all build/verification evidence, complete repair documentation, and deterministic file hashes.
Nothing required for reviewing the application source has been replaced by a placeholder.

TOP-LEVEL CONTENTS

1. APK/
   Signed R816 release APK. Install it as an in-place upgrade. Do not uninstall or clear application
   data first; R816 includes a metadata-only repair path for the existing R815 database.

2. SOURCE_ASSETS/
   Complete project tree: all Java source, Android Manifest/resources/assets, Gradle files/wrapper,
   verification harnesses, release scripts, training/research utilities, historical reports and
   documentation. Generated build directories are intentionally not duplicated here; the final APK
   and complete build evidence are provided in their own top-level folders.

3. BUILD_EVIDENCE/
   Official Android API 36 Java 17 compilation logs, regression/runtime/performance harness results,
   D8/AAPT2/zipalign/signature/ZIP checks, release manifest, and APK/source/resource/toolchain hashes.

4. DOCUMENTATION/
   R816 root-cause analysis, exact file-level change list, upgrade behavior, acceptance conditions,
   rollback policy, and physical-device T29–T34 retest procedure.

5. TOOLCHAIN_USED/
   The exact Android 36 platform, R8/D8, AAPT2, APK signing/zipalign and APK inspection binaries
   used by the supplied offline build scripts, together with their upstream notices/metadata. No
   signing key or password is present.

6. MANIFESTS/
   Sorted complete file list and SHA-256 for every delivered file, plus the outer archive hash next
   to the ZIP after packaging.

INSTALLATION

- APK file: APK/Starcore-Tianshu-V57.15.3-R3.6.13-816-release.apk
- applicationId: com.ethk3.novaonepro
- versionName: V57.15.3-R3.6.13-816
- versionCode: 577190825
- APK SHA-256: 602c937773078cb11040d123ca290468c5c71d7e98712832240fca64d751650b
- Signing certificate SHA-256: ae7e1b4a5a71f1f299d4a0b2472c6a36513873514e78190d42d6cfba3340116f

TRUTH BOUNDARY

This release passed source/static/host/Android-36/D8/APK/signature verification. Physical-device
screen-on/off, process recovery, network switching and 24-hour soak still require execution on the
target phone. MODEL_PROVEN remains false, SHADOW_ONLY remains true, and REAL BET remains hard
disabled. This is deliberate and must not be changed merely to make a report look successful.

SECURITY EXCLUSION

Private signing keys, PKCS#12/JKS files, passwords, and live API secrets are not distributable source
or assets and are not included. The delivered APK is already signed. Build scripts perform hard
credential validation and read secrets only from the controlled build environment.
