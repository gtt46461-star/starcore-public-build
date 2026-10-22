# Independent R75 matrix source and owner review

This review does not modify production sources. It checks the matrix registration changes against the existing source, PIT and qualification contracts, then runs the public `MatrixSnapshotBusR952.drainOneCP4` owner on copies of two databases already trained and frozen by the public development owners.

The fixtures are the completed calibration-repair continuation (`training_run_repair.py`) and the ordinary report-derived NEURAL continuation (`training_run_all.py`). No matrix source, model, certificate, prediction or quality row is inserted by this test. The runner copies each database and uses a fresh JVM for its second phase. It snapshots the product overlay before execution.

| Matrix family | Required evidence; registration alone provides none |
| --- | --- |
| Frequency, Omission, Transition, Drift | Actual clean draw rows. R75 defaults pin one initial lower bound; lag still requires real consecutive predecessors. |
| ConditionalTransition | Actual draw plus matching regime policy and regime receipt captured before the draw opens. |
| CandidateWindow, CandidateError, Confidence, Residual | The existing fixed formal window, lifetime binding, predraw prediction and authoritative settlement. |
| CoOccurrence, Similarity, Diversity | Both candidates' valid formal predictions for the same target and position; calibration observations are not formal pairs. |
| ParentChild | Real learner lineage and both immutable freezes, followed by common formal future evidence. |
| Teacher | The student's actual EXPERT_HEDGE AST and model expert references must match the teacher freeze, model, manifest and position. Formal source chronology still applies. |
| AtomTarget | A direct scalar atom in the model's actual feature outputs, with the exact frozen normalizer and binding. Intermediate atoms cannot borrow a later operator's feature normalizer. This is diagnostic evidence, not atom admission. |
| General AtomRegime study | The consumed atom node, exact frozen model/context and formal future pair with an actual ablation replay. Existing OOS admission requirements remain unchanged. |
| Latency | Actual exclusive `CP12EX1` audit spans. |
| Freeze, Failure | Actual source-owner events; no default fabricated input. |

The independent owner test checks budget exhaustion without cursor movement, fresh-JVM resumption, all 90 default registrations, actual checkpoints for all 40 unconditional history partitions, real lineage registration, unchanged source/quality/training counts, unchanged atom catalog states, and an actual `AtomAdmissionCP12.Group` rejection where formal OOS data is missing. Both public fixtures have no directly exposed scalar atom feature: the NEURAL feature is a threshold operator, while the repaired FIXED_GRAPH has no model feature array. Their AtomTarget count must therefore remain zero. Direct AtomTarget and real Teacher positive cases belong to the separate source-owner tests authored with the registration change.

Run the independent suite using the already produced fixture databases:

```sh
python verification/r75/matrix_registration_owner_run.py \
  --jdk JDK_DIRECTORY --schema HOST_SCHEMA_DIRECTORY \
  --classes FINAL_RELEASE_PRODUCT_CLASSES \
  --repair REPAIR_CONTINUATION_DATABASE --neural NEURAL_CONTINUATION_DATABASE \
  --out NEW_OUTPUT_DIRECTORY
```

Omit `--overlay` when validating the unified release. The runner copies only the three compiled matrix class families from `--classes`, verifies byte-for-byte equality with the originals, and compiles only the test and host clock adapter. The optional `--overlay` is for isolated development reproductions. Output contains copied fixture databases, four phase logs/results, class/source hashes, and `summary.json`. For release acceptance, `productOverlayUsed` and `productionSourceCompiledByRunner` must both be false; `snapshotExactlyMatchesOriginClasses`, `originClassesUnchangedDuringRun` and `sourcesUnchangedDuringRun` must be true.

The review reproduced a real unbounded query on the 5,000-draw fixture: a correlated `COUNT` over every earlier draw took about 1.8–2.0 seconds, exceeding the nominal 250 ms matrix slice and repeatedly preventing source-cursor persistence. `reproduction/matrix_lag_owner_before.log` contains the actual public-owner observations, and `reproduction/matrix_lag_sql_before_after.json` compares the same read-only query and result with its replacement. The final query computes the required prior-issue threshold once; the subsequent actual lag-continuity check is retained. The owner regression requires the empty conditional source queries to stay below 100 ms in this host fixture and verifies that all subsequent history partitions actually execute.

These are scoped host results. They do not establish physical-device timing, Android background longevity, natural future statistical performance, a certificate, REAL execution, or completion of all 299 construction cards / 1,832 requirements.
