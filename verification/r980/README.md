# R980 verification boundary

The current product sources are the uploaded R979 baseline plus this branch's diff, not a remote
substitution. Source provenance is generated once before compilation. `run.py` records real exit
codes, unmodified stdout, final-product source digests and test/fixture digests; cached Python bytecode
is deliberately excluded. Tests use real Java product owners and a real SQLite process with explicit
Android API adapters. They do not run the Android application process or render its views.

## Commands

From the repository root, after extracting the supplied no-key toolchain:

```sh
python verification/r980/run.py --step all --out /absolute/test-output \
  --android-jar /absolute/toolchain/android-platform-36/android.jar
```

There are 27 ordered steps, including clean full-product compilation. The V3 steps share only their
own disposable synthetic SQLite database: 120 prequential numeric updates, a frozen model, then 601
separate synthetic future draws (241 calibration, 120 short-window, 240 long-window). The publisher
must issue a certificate through actual product gates; this path does not inject a certificate.
`business-closure` copies that database and verifies two further actual canonical/REAL commits,
canonical-view reads, draw persistence, sole settlement/OOS commit, the coordinator's automatic
training callback, real next numeric updates, and duplicate idempotency. Canonical DTO construction
and Android process APIs are host test adapters. Old warm-training terminal records were deliberately
minimal; the extension preserves them under a fixture-only table name and invokes actual base,
column and additive-schema owners before new settlement. No such table rename runs in the APK.

`exact` and older fixtures include separately seeded synthetic authority inputs; they must not be
misrepresented as the unseeded V3 publication test. `report` in the inherited runner uses an older
bundled R975 fixture. Current user's R979 report is specifically covered by `resource-closure`,
`backlog` and `full-population`, with exact five table snapshots in `fixtures/`.

Re-extract those five snapshots without manufacturing labels:

```sh
python verification/r980/extract_report.py --report /absolute/5.4.2.22检测报告.zip \
  --out /absolute/re-extracted-fixtures
```

Compare to `fixtures/manifest.json`; per-table timestamps are not an atomic phone database backup.
Backlog tests explicitly generate synthetic draw labels in disposable databases. The 7,618 original
settled labels are checked unchanged. Full-population tests use the 64 original genomes, run 240
historical VM updates, inject an actual late transaction abort, verify whole-operation rollback,
and then retry publication. Historical updates never become future/OOS samples.

## Artifact build

Use the delivered `REBUILD_R980.sh` wrapper or `scripts/build_current.py`; older versioned scripts
remain historical references, not current release entrypoints. The current build compiles all main
Java classes, replaces every predecessor DEX, recompiles current Manifest/resources/assets, checks
resource IDs, signs with the owner's original external signing kit, and verifies certificate equality.
The baseline APK is a packaging/credential reference only; no old DEX is shipped as current code.
No signing private key is distributed in this repair delivery. Keep the original private signing kit
from the user's R979 input privately when rebuilding; never put it into a public repository.

`verify_delivery.py` compares final source manifests, app/version identity, signer certificate,
ZIP integrity, and every final DEX against the newly generated D8 files. It does not prove an
installation on the user's phone. Its final detached output contains the actual Git commit and APK
hash; no impossible self-referential APK hash is embedded in the APK.

## Mandatory NOT RUN boundaries

Phone overwrite installation, real MainActivity rendering, service/OS startup, screen-off operation,
process kill/reboot, live network timing, full physical ~3 GB database performance, prospective
accuracy and profit are NOT RUN. The 91-item historical plan and whole-project old-code retirement
are not declared fully re-audited by this scoped repair. Do not turn host PASS into a phone PASS.
