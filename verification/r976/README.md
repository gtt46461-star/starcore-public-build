# R976 tests

`run.py` compiles and tests actual product Java with explicit Android API adapters
and a real Python SQLite bridge. Java 17 and Python 3 are required. Supply the
Android 36 jar from the user's retained NO_KEYS toolchain.

Use a fresh output directory with `--step all`, or execute STEPS in the order shown
in run.py. `compile` must precede all product tests. V3 stages share a SQLite file.
Do not run the old `base` fixture suite into this output; it can shadow classes.
The script deliberately exposes no `base` shortcut.

New tests:
- ChainBlockerHarnessR976: 47 checks; exact same regression fails 10 on supplied R975.
- PostRetryHarnessR976: 10 fenced rollback-retry checks.
- ContinuationHarnessR976: 13 finite predecessor/run-selection checks.

Existing R974/R975 suites are retained and exercised, not claimed as new features.
`report` uses bundled old R975 fixtures, NOT the user's current full report/database.
V3 uses independent synthetic data and the real publisher/gate/writer, not live draws.
No handset, power management, network, app installation, or real win rate is tested.
