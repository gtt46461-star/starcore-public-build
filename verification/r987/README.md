# Independent durable report cancellation

The original R986 actual ReportExportService.requestCancel overwrote a shared cancel.json. A second report cancellation erased the first report cancellation, and the unchanged behavioral assertion failed at assertion 5. Only Android file/Intent transport is adapted; the product Service and journal implementations are executed.

Current product stores one immutable-target command per existing report job or SAF transfer. Request and notification paths call the same journal command producer; ActiveControl calls its exact command consumer. A separate file lock serializes readers and writers across processes. Original single-file commands remain readable. Terminal request cleanup removes only its exact command identities. Missing tasks, malformed or mismatched command identity fail closed.

Run run_cancellation_checks.py against newly compiled complete product classes. The driver checks exact request kicks, independent report and SAF commands, repeat commands, unknown-task rejection, legacy compatibility, malformed identity, concurrent process writers, fresh JVM recovery and independent cleanup. Host AtomicFile is explicitly a filesystem transport port. Android lifecycle, device installation and provider behavior remain DEVICE_NOT_VERIFIED.
