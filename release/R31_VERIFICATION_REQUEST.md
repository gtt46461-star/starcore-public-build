# R31 verification request

Changing this file starts .github/workflows/r31-verification.yml on construction/r31-final-20261104
(a new workflow cannot be dispatched from a non-default branch). It is ignored by the construction gate.

- 2026-11-08: final K11 run (gate source 64db91b6 + this request)
- 2026-11-08: rerun after the Gradle distribution timeout (run 42464955166 built nothing); workflow now fetches it with retries
- 2026-11-08: rerun with the Gradle wrapper distribution cached (run 42465197650: contracts PASS, android_build failed on the download)
- 2026-11-08: gate green at 28d5affc (622/622, lint, all hosts); run with the temporary-CI-key debug build
