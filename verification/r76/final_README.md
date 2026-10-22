# R76 final host replay

`final_run.py` accepts the JDK, complete schema/host adapters, final release classes, Android host runtime, compiled host native library, current report, prior report, repaired-model fixture, untouched current-report queue fixture, strict build receipt and a fresh output directory. It runs twelve independent suites with three workers and records all commands, exit codes and release class hashes before/after. No production source is compiled or substituted in final mode. The continuation suite separately runs three explicitly invalid generated-list mutations; its normal path loads release identities unchanged.

The two reports have different purposes and cannot be interchanged:

- `--report`: current R75 phone report supplied for R76 repair, ZIP SHA `a4acc60904ecef8b88fcd5e5b4c3b5707a057652c4fdf7ee2bb6d694eaf68825`. The current recovery test authenticates six SCDEV input hashes and two original feature-prefix hashes, then resumes existing protocols across JVM restarts.
- `--prior-report`: earlier R74 report used by the R75 compatibility test, DIAGNOSTIC_SCOPE SHA `7c27e983942d809aaeb26ea708af6ec3bd731248adf4403e8410a9697d52ca94`. Its NEURAL action is still PROPOSED; the old test registers it at its original boundary. In the newer report it is already WAIT_FUTURE and the product correctly rejects duplicate preparation.

The first aggregate attempt supplied the new report to both suites. Eleven suites passed; the compatibility suite failed at `TRAIN_SELECTION_ACTION_CAS`. Only that failed suite was rerun against its original input. No product code, gate or assertion changed. `evidence/final/first-suite-attempt.json`, the original failed logs and `suite.json` retain this correction; `final_run.py` now requires the separate prior report argument for future complete replays.

Final result: twelve groups passed using source commit `ede9746f9172033d27cf6ed15456780d4a92ca4d`; all 3,419 release classes remained unchanged, including after the compatibility rerun. The complete class hash inventory is `evidence/final/release-class-hashes.json.gz`. The test runner and extra bytecode guards are verification-only changes recorded after the source build; they do not alter the APK.

`evidence/final/build-build-receipt.json` binds APK SHA, original signer, version, ABI set, catalog assets and generated identities to the source commit. `evidence/final/unit-summary.json` records 51 passing unit tests and lint under the unchanged baseline. Existing baseline-filtered lint findings are not claimed fixed.

Host adapters and synthetic inputs are explicit. These tests do not establish phone latency, full Android UI lifecycle, continuous 55-second capture, natural quality or qualified REAL. See `ACCEPTANCE.md` for the remaining obligations and the separate Android CI status.
