# R3.6.10-813 Physical Device Retest Protocol

Use the new APK only. Before the first run, preserve/export any database needed for forensic comparison.

1. Cold start: launch the app 10 times. Expected: first screen remains responsive; no ~10 s freeze/process exit.
2. Background start: press 启动后台 once. Expected: foreground service remains alive and UI remains responsive.
3. UI stability: observe the probability/gate/runtime/stat cards for at least 10 issue refreshes. Expected: card heights stay stable and text truncates/ellipsizes rather than resizing the whole dashboard.
4. Probability truth: before a valid selected probability exists, expected display is 未生成/未证明 (or 未校准 with a valid upstream P), never a fabricated `0%` caused by missing data.
5. SHADOW creation: with `MODEL_PROVEN=false`, expected behavior is normal predraw canonical/DecisionGate processing with SHADOW observations; do not expect forced REAL bypass.
6. Settlement: after subsequent draws, check that valid predraw SHADOW/REAL observations leave pending state and are settled exactly once; legacy rows that truly lack a predraw canonical must remain explicitly marked rather than fabricated.
7. Screen-off: keep service enabled across screen off/on and verify fetch/analysis/settlement heartbeats remain fresh.
8. Task removal: swipe the Activity away and verify configured service/reconcile recovery without duplicate issue processing.
9. Network primary/backup: test primary failure/backup recovery without duplicate draw/canonical writes.
10. Soak: run the normal T29-T34 matrix; acceptance requires OOM=0 during the observation window and fresh business beats.

Any failure should be exported with complete diagnostic text + crash record + raw database state. Do not relax DecisionGate/model gates to make a runtime test pass.
