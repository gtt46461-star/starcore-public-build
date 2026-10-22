# R66 runtime continuity regression

The new regression runs the actual Gradle-compiled release classes against real SQLite with Android transport adapters. Its fixtures are synthetic. It verifies cancellation-safe task lease release and immediate fenced reclaim, preservation of certificate-owned lifecycle state and paired evidence, exact R65 numerical training continuation, rejection of unknown rules, complete metadata export beyond the former 200-row/2,048-character limits, and immutable source snapshots.

`run.py` also executes the existing 234-check numerical suite, including gradients, prospective model continuation, both fusion parents, ordering, gap rejection and fixed K3 output. It does not assert future predictive improvement, device liveness, successful installation or live certificate/mainline eligibility.

Run after the normal Android release compilation:

```sh
python3 verification/r66/run.py \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --android-runtime /absolute/path/android-all.jar \
  --jdk /absolute/path/jdk17 \
  --out /absolute/path/new-evidence-directory
```

The output directory must be new because its database is intentionally durable between close/reopen assertions. API credentials and signing material are external build inputs and are never included in this test.
