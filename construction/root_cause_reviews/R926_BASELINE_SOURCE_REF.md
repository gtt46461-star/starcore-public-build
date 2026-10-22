# R926 predecessor-source reference

- Symptom: the first final signing build stopped at `BASELINE_SOURCE_ARCHIVE`.
- Root cause: `r926_build_release.sh` retained a repository-specific default commit `997457c`, which does not exist in the verified R956 source checkpoint repository.
- Resolution: the release build was rerun with `STARCORE_BASE_COMMIT=552a512aa8a0805c2bedf4a9e5fee74eb46aed60`, the immutable imported R956 baseline.
- Result: signed release build, predecessor resource compatibility, signature continuity, and both APK acceptance suites passed.
- Product impact: none; failure happened before product Java compilation and no partial APK was accepted.

