# Portable AtomNet whole-artifact validation

The new runner is `tools/r31/run_atomnet_publication_host.py`. It requires only
the downloaded repository files, an actual compiled whole-app jar, and standard
JDK/SDK/JDBC/JUnit dependencies. It does not load an old COMPILE_SETUP file or
require Git history. It does not compile any production business source.

CI invocation, with `JAVA_HOME`, `ANDROID_HOME` and `SQLITE_JDBC` already set:

```sh
python3 tools/r31/run_atomnet_publication_host.py --classes "$R31_WHOLE_CLASSES_JAR" --out "$RUNNER_TEMP/atomnet-publication"
```

JUnit/Hamcrest can be supplied through `JUNIT_JAR` and `HAMCREST_JAR`, or resolved
from the normal `GRADLE_USER_HOME` cache. The output directory must be new; the
actual existing-output rejection and unchanged receipt hash are recorded in
`EXISTING_OUTPUT_GUARD.json`.

`PORTABILITY_RECEIPT.json` summarizes two completed actual runs against the same
4475-class jar, SHA `e46f65ea8e53b8982e1077a6908600cfab66d690f9c7bc11f501285fc85405c9`:

- `whole4475/`: normal checkout, 543 original native consumer assertions, all
  29 original model/state JUnit tests in one JVM, actual Runtime step/submit
  producer, cursor-loss rejection, and both halt-before-final / halt-after-commit
  cases with distinct fresh recovery JVM PIDs.
- `source-download-no-git/`: the same full suite rerun from a copied source
  directory containing neither `.git` nor COMPILE_SETUP.json. All 47 repository
  fixture/boundary/asset/resource/CSV inputs stayed unchanged.

The real CSV used by R27TestSupport is explicitly included in the input hashes.
An initial incomplete minimal export omitted this CSV; its genuine failure is
retained in `initial-missing-csv-failure/`. It was corrected by copying the actual
input, without changing any numerical assertion, source dataset or model code.

All 22 checked production classes, including DbHelper, DbStore, coordinator,
AtomNet, Runtime, checkpoint, memory and AST classes, have actual CodeSource
locations in the supplied whole jar. There are zero business stubs and zero
production source overrides. Only Android HOST boundaries and the original tests
are compiled. The output-only native fixture copy replaces its obsolete
`DbHelper(db)` convenience call with actual `DbHelper(Context)`, using an explicit
SQLiteOpenHelper boundary binding to the same native TraceDb. The original/effective
HOST source hashes and exact connection hunk are recorded; assertions are unchanged.

Each run retains its commands, effective HOST/platform sources, original test
hashes, actual class-entry hashes, raw logs, model/consumer receipts and process
halt/recovery facts. Generated classes, SQLite databases and dependency jars are
not committed. The earlier AtomNet implementation receipts are untouched.

These are native SQLite/HOST JVM results. Android integration, physical-device
process death, 24/72-hour running and natural future quality remain NOT_RUN or
NOT_EVALUATED here. The producer's measured wall-bounded materialization and
actual small-history split are recorded separately from the consumer's full
800/400/160 engineering training plan.
