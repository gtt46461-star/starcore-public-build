# R927 engineering-closure report directory

- Symptom: every production/SQLite assertion passed, then the harness exited non-zero while writing its JSON result.
- Root cause: the script created `build/r927-engineering` but assumed `reports/r927` already existed.
- Fix: create the report directory before running or persisting evidence.
- Regression: rerun the complete R927 engineering-closure harness and require exit code 0 plus `R927_ENGINEERING_CLOSURE_PASS`.
- Product impact: none; failure occurred after all product assertions and before evidence persistence.
