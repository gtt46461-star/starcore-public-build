# R986 actual SAF finalization and resumed identity

ReportSafFinalizationR986 reproduces cancellation accepted at final readback-stream close. Original R985 product returns success and fails the assertion. Current product checks cancellation/interruption after stream close. It retains exact byte/hash verification, bounded streams, write/readback failure and safe retry.

ReportSafResumeIdentityR986 uses actual ReportPublication prepare/delivery identities and the same ReportSafCopy verifySavedReceipt called by ReportExportService. Resumed part/name/hash/size must agree with the current immutable publication, and actual readback must agree. Service also rejects duplicate provider URIs and excessive receipt sets, and checks cancellation immediately before success persistence.

Compile the whole current product with verification/canonical/schema/run_schema_capture.py. Compile only these test drivers against those fresh classes plus Android36 jar; execute with -Xmx128m and empty isolated output directories. Existing ReportSaf, Publication, QueueRecovery and native report tests remain unchanged. File transport is host-backed. Actual Android provider/process/background/device/install remain DEVICE_NOT_VERIFIED. Fixture bytes are explicit synthetic publication transport data, not a natural report/model/Future claim.
