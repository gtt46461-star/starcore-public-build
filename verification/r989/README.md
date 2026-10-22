# Actual report sort failure cleanup

The real ReportExportServiceCP4 calls ReportMemberPlanCP12.sort and reads its returned sorted file for the native archive. Paused indexing resumes from its immutable source facts and creates a fresh sort nonce. A partial failed nonce has no persistent downstream consumer; keeping it indefinitely leaks temporary space on retries/cancellation.

The actual sort now reclaims only the failed attempt's generated workspace and partial result, keeps the unsorted source and other tasks, preserves the original exception/interrupt, and attaches any cleanup failure as a suppressed exception. Existing success cleanup, duplicate-identity rejection, stream/row limits and cancellation fences remain. Cleanup traversal is bounded to the generated directory shape and rejects symlinks; it never follows a path outside its attempt. Process kill during cleanup remains DEVICE_NOT_VERIFIED.

Run against complete fresh current classes:

    python3 verification/r989/run_sort_cleanup.py --classes CURRENT_CLASSES --out NEW_EMPTY_DIRECTORY

The original real R988 sorter failed cancellation cleanup assertion7. Current tests retain that assertion, add exact original reason/type and guarded cleanup failure assertions, check source and unrelated-task hashes, actual successful ordered row values and interrupt state. No sort/file implementation or product method is replaced. Android provider/device/process/performance coverage is DEVICE_NOT_VERIFIED. The earlier R988 aggregate REPORT_SORT_CLEANUP root cause remains UNKNOWN; this proven retry/cancellation leak is separately fixed.
