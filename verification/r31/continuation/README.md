# R31 B01-C04 continuation / required REPLAY caller evidence

State: **CODE_COMPLETE**, pending root Android build and shared protocol integration. No device or natural quality pass is claimed.

Reproduce from the complete Android project after its production Java classes exist:

```sh
python3 tools/r31/verify_continuation.py
```

The command compiles the production pure-Java continuation sources, then runs:

- 8 lifecycle JUnit tests, including cancel versus actual termination, real helper finally/exit after cancellation without any installed continuation or token, retained permits under a never-poll worker, preserved interrupt status, and writer-critical yield rejection.
- 4 production `EvolutionLoopR27.stepReplay` JUnit tests: all 128 already-admitted synthetic candidates finish with at most two helpers; every temperature and trace array matches serial replay exactly; capacity refusal retains candidates for retry; parent cancellation stops both actual helper bodies; helpers inherit the H_SELECT boundary and cannot read H_CONFIRM.
- 2 controlled overlap races using the production TaskContinuation / LegacyReplayContinuation wrappers and clearly labelled controlled numerical entry shims. These do not simulate SQL fencing.
- 10,000 sequential owner/target changes using the production task map, including repeated owner names. This stress is distinct from the two overlapped races, and does not claim 10,000 real database fence races.
- Original R26 incumbent-budget and model/replay recovery suites against the changed continuation core.
- A controlled reproduction of baseline defects from exact commit `5237b265a27fe5ad27fd842901c445d5f8c27749`, and baseline/current synthetic incumbent cost profiles with exact output digest comparison.

`RECEIPT.json` records all source/test/tool hashes, commands, results, scope extensions and remaining acceptance work. Timing/allocation profiles are individual host observations. They do not prove device performance or prediction quality.

The extra production wrappers `LegacyGenerationR26`, `LegacyReplayContinuationR26` and the REPLAY portion of `EvolutionLoopR27` were explicitly assigned by root after discovering that premature worker replacement and submitting 128 candidates into a two-helper controller would invalidate the shared B01-C04 protocol. No other evolution algorithm was changed.

Production integration requires ResearchPoolR27's single static permit controller to be installed before any Android continuation is created. That controller owns helper task/fence groups; its cancellation, resource closing and immutable-source invalidation must wait for helper bodies to exit. DiagnosticSnapshot.asMap supplies bounded execution/checkpoint/thread observations for actual shutdown/report consumers.

Android build, real owner/fence SQL races, writer rollback injection, shared ResearchPool helper-group tests, Android 10-second draining, device memory/thermal/screen-off behavior, and 24/72-hour acceptance remain outside these host receipts. APK identity and device evidence remain unassigned until the root release step.
