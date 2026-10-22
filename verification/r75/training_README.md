# R75 training continuity verification

## Report evidence and repair

The supplied `DIAGNOSTIC_SCOPE.json` contains an R952 learner with 421 settled
future observations but only 413 numeric updates. Eight outbox deliveries remain
unconsumed. Delivery `202610130091` is still `RUNNING`, attempt 1, with lease expiry
`1791868901669`; the report timestamp is `1791868997937` (96,268 ms later).
An expired lease is recoverable by the existing outbox owner, but requires another
training pulse. The old snapshot by itself does not establish that a model rejected
those samples.

The same report has 11 modern family proposals from issues `202610130021`–`025`
still awaiting registration at issue `202610130323`, and no training protocols.
Replaying all 11 observed ASTs through the actual product `prepare` owner succeeds.
The first replay takes 248 ms on this host; subsequent ones take 46–94 ms. These
figures are host observations, not Android timing guarantees.

The Director used to advance the durable owner before checking whether the remaining
callback budget could start that owner. For example IMPORT can repeatedly receive
only a short tail, advance back to TRAIN, and yield during input preparation.
The training sub-wheel has the same failure mode after the legacy outbox owner.

R75 preserves the next owner if there is insufficient local or parent budget:

- Director TRAIN / RESEARCH / EVOLVE / IMPORT: at least 350 ms, including the existing commit
  allowance; other Director owners: at least 125 ms.
- Training legacy outbox: at least 125 ms; candidate training and freezing: 250 ms.

An admitted owner still advances the cursor before executing. A failing owner thus
cannot reserve the entire wheel. No deadline is extended and no numeric, predictive
quality, qualification, or transaction threshold changes. The outer callback must
provide a useful fresh slice; an indefinitely exhausted parent cannot train. The
separate AutoFetch callback fairness and database-cost repairs address that cause.

## Reproduce against the final compiled product

Use the existing complete SQLite host schema and final compiled release classes:

```sh
python3 verification/r75/training_run_all.py \
  --schema /workspace/scratch/7a6032548233/r72-final-schema \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --report /workspace/scratch/e2544c9906f6/r75-input/DIAGNOSTIC_SCOPE.json \
  --out /workspace/scratch/e2544c9906f6/r75-training-final
```

The runner saves exact commands, source hashes, report hash, result JSON, logs,
SQLite fixtures, and real development input files. It compiles test harnesses and
the host monotonic clock, not product code. `--overlay` is available for an explicitly
recorded pre-build product snapshot; omit it for release acceptance.

The focused before/after runner `training_run_budget.py` also accepts
`--compile-product` to compile the two scheduler source files as an explicit overlay.
Without that flag, the old R74 bytecode fails the first assertion
`SHORT_PARENT_NOT_ADMITTED_TRAIN_0`. The original failure is retained at
`/workspace/scratch/e2544c9906f6/r75-training-repro/baseline-director.log`.

## Assertions and limits

1. **66 durable scheduler checks:** each of six Director owners receives repeated
   unusable parent slices without losing its place, then a fresh slice; IMPORT's
   local tail and RESEARCH's insufficient 200 ms parent are tested explicitly. All three training sub-owners preserve their
   cursor and resume after reconstructing the caller store. Admitted owners rotate.
2. **10 outbox/feedback checks:** actual `ResearchDirectorR952.pulse` consumes 13
   valid synthetic labels, reclaims the simulated crashed lease, advances a model
   through 12 chronological numeric updates, and commits 13 post-training feedback
   receipts. A broken candidate cannot create an artifact or block an independent
   healthy candidate. Repeated pulses do not train twice. No certificate or trade
   is created. Labels are test fixtures, never predictive-quality evidence.
3. **Observed NEURAL development:** the report's actual AST, atom definitions,
   history, action, source issue and evidence hash are passed through product
   preparation, registration, and incremental training owners. Across short
   850 ms scopes, 1,320 real feature rows yield one `MLP_D32_16_10` model, 1,200
   training rows, 120 validation rows, and one development receipt. The old report
   omitted action-input boundary rows; the fixture reconstructs those deterministic
   rows from the observed source/action/evidence and labels this fact. It does not
   fabricate Future, calibration, certificates, or trades.

Each training pulse reconstructs `DbStore` from the same SQLite database. This
tests durable store recovery and a simulated prior-process lease, not actual
Android process death or device scheduling. Host throughput does not guarantee
provider publication time, on-device latency, per-period completion, predictive
improvement, or live qualification. Those require their own evidence.

## Calibration repair continuation

`training_protocol_identity.py` accepts the same schema/classes/jdk/report/out
arguments as `training_run_all.py` and compares all 11 ordinary report family
protocols against the old `0dba780` DevelopmentProduction source. Both versions
run with the same current build identity. Canonical protocol bytes, input hash
and seed must be exactly equal. Product overlays are optional and explicitly
recorded. The actual training and freeze checks above cover the downstream path.

`training_run_repair.py` copies the final calibration-repair fixture's SQLite DB
and published SCDEV files, then invokes only the public training/freeze owner.
It does not insert feature rows, quality decisions, artifacts or frozen models.
Run it after the calibration-repair fixture's `resume` stage has registered its
child and TRAIN task:

```sh
python3 verification/r75/training_run_repair.py \
  --fixture /workspace/scratch/e2544c9906f6/r75-calrepair-test6 \
  --schema /workspace/scratch/7a6032548233/r72-final-schema \
  --classes app/build/intermediates/javac/release/compileReleaseJavaWithJavac/classes \
  --jdk /workspace/scratch/cd9274eff1c6/jdk/jdk-17.0.20.1+1 \
  --out /workspace/scratch/e2544c9906f6/r75-repair-continuation-final
```

Seventeen assertions require a real child artifact, development receipt, all
600 feature rows (480 TRAIN + 120 VALIDATION), and a newly loaded FIXED_GRAPH
freeze. Input, validation and model cutoff must remain `202609230421`, before
parent calibration starts at `202609230442`; selection uses `202609230682`
and the new freeze begins at `202609230683`. Replays must not duplicate models.
The 241 calibration observations and original registry states (2,522 DISCOVERED
and 13 REQUIRES_BINDING) remain unchanged; Future, formal observations,
certificates and trades stay zero. The fixture's calibration decisions come from
the actual capture/fitting owners, while added pre-calibration raw draws are
explicitly synthetic host test inputs. This proves software execution, not live
predictive quality.
