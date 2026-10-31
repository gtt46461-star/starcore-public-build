Run: https://github.com/gtt46461-star/starcore-public-build/actions/runs/41176346773

Both public, already-published CP06 and CP16 baselines installed on disposable API 34 emulators. The published B00 R13 release installed over each baseline, and the filesystem marker remained. Both jobs failed during the first 180-second READY/heartbeat probe; the database comparison and second startup were not reached. This is not an upgrade PASS.

The probe reads application preferences with `run-as` and discards its stderr; the installed release is not debuggable. Every sampled preference is absent despite a live process, so the probe cannot measure readiness using that access method. Separate recheck 41176921680 uses emulator root to read the same files and applies the same readiness conditions. Database writer contention also appears in logcat; its impact has not been determined. No physical device or natural quality was tested.
