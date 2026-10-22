# R925 host-compile output publication race

- Symptom: all compilation work completed, but `cp -R` failed while replacing `build/r925-host/compiled-classes` because a destination subdirectory appeared concurrently.
- Root cause: the script removed the live directory and copied directly into the final path, exposing a partially published tree to overlapping verification activity.
- Fix: copy into a PID-unique sibling directory, then replace the final directory with a single rename.
- Regression: the complete R952 batch acceptance is rerun by the final packaging command.
- Product impact: none; this path is host verification evidence only and is excluded from the APK.
