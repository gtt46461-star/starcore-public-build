# R988 private API credential producer and consumer verification

The release build no longer reads or embeds DUOQU_API_KEY. Both legacy BuildConfig fields are empty. The actual MainActivity API status row opens ApiCredentialSettingsCP12; KeyProvider's bounded worker writes an encrypted device-private credential, rereads its immutable identity and decrypted value, then makes it available to the existing ApiClient. Application startup and unlocked boot/package replacement schedule asynchronous reload. Getters on the UI are memory-only. Missing/corrupt credentials retain KEY_NOT_READY with the original cause, missing condition and HOME_API_LINE_CONFIGURE recovery path; no HTTP is attempted without a usable key.

Run against freshly compiled complete current product classes:

    python3 verification/r988/run_credential_checks.py --classes CURRENT_CLASSES --out NEW_EMPTY_DIRECTORY

The 26 checks use synthetic alphanumeric keys and a JDK RSA/file Storage port. They execute the actual PrivateApiCredentialCP12, KeyProvider async configure/reload, fingerprint rotation and ApiClient URL/guard consumers. The driver uses reflection only to bind the platform storage port, not to replace product methods or enable certificate/REAL authority. No HTTP, production database, natural Future or model-performance evidence is generated. AndroidKeyStore, the foreground dialog, actual boot/device process recovery and update installation are DEVICE_NOT_VERIFIED.

For the first update from an old embedded-key APK, tap the home API status row and enter your own API key once. The encrypted file is stored in noBackupFilesDir and the RSA private key is in AndroidKeyStore. Later compatible updates are designed to retain them. Device overwrite/retention still requires real-device verification. Do not uninstall or erase app data as an update workaround. No production credential, signing password or private key belongs in source, report, test fixtures or APK.

The current signed APK was scanned across all uncompressed members for the known private construction credential, its build fragments, signing password and private keystore files: zero matches. This is an actual artifact check, not proof of AndroidKeyStore runtime or a claim that the complete construction plan is accepted.
