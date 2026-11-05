# Warm owner protocol: frozen source and scoped evidence

Production commit: `1e0aafaf8d529b801609fa42f190ba1336020c24`, based on
`503efdc2c7abdb78b6e30fbd9d881d8dc772265e`. Only `WarmCandidateR26`,
`WarmModelSeedR26`, new `WarmPublicationR31`, and the approved closed-dispatch
extension of `ModelPublicationR31` changed. No shared Runtime/Schema source was edited.

The entry point for review is [BATCH_RECEIPT.json](BATCH_RECEIPT.json). It separates
the two pre-StatFs source groups from the final source. The last change replaces
`checkFreeFloorR31` with Android `StatFs.getAvailableBytes`, preserves the 256 MiB
floor, and rejects null/unknown capacity as a typed dependency. That method is the
only byte difference between the full regression source and the final ModelPublication
source; the other three source hashes are identical across all groups.

| Source group | Named assertions | Physical NEW transactions | Max SQL / bind bytes | Host P99 / maximum |
| --- | ---: | ---: | ---: | ---: |
| Original publication cases, pre-StatFs | 5,714 | 532 | 34 / 17,453 | 19.212227 / 42.052711 ms |
| Warm protocol, pre-StatFs | 9,197 | 446 | 33 / 17,292 | 29.697162 / 89.640661 ms |
| Final affected capacity and retention | 207 | 22 | 33 / 17,282 | 161.882120 / 161.882120 ms |

These denominators include all NEW-labelled recorded transactions, including
rollbacks and closed-callback Legacy controls. SQL counts include reads, writes,
fences and receipts; adding BEGIN/END also stays below 64. There are three >50 ms
records in the second group and one in the final group. Their causes are UNKNOWN.
**Timing acceptance remains OPEN**; the final small group's P99 exceeds 50 ms.
Android, natural quality, long-duration operation and unified root lint/build are
NOT_RUN here. A source freeze and named host checks do not close B02/B05 or P03.

Actual checks retained in the raw logs:

- Real Foundry preservation drained all 2,048 rows in 256 calls. All 1,024 learned
  rows remained; the 1,024 original removals were archived. All 17 original fields,
  SQL value types and BLOB bytes survived. Real rollback, source CAS and late-owner
  rejection did not advance the old work. Maximum observed drain transaction was
  23 SQL / 3,280 bind bytes / 22 changed rows including triggers.
- Actual Warm.step on 512 MiB and 64 MiB heaps kept the authenticated parent kernel
  and optimizer bytes after the original gap reset, reset the child's private cursor,
  transferred no cohort/alpha/qualification, and atomically committed the initializer,
  candidate, artifact, identity, parent archive, manifest, receipt and work total.
  The final publish observed 14 changed rows including triggers; staging observed 18.
- Final rollback, cancellation, stale source, late owner and post-commit diagnostics
  passed. One hundred reentries did not duplicate receipts or work. Actual process
  `halt(74)` left a durable private plan; a new JVM used the existing artifact pruner
  to remove uncommitted child files, then regenerated/re-leased them from the intact
  authenticated parent and completed the same numerical publication.
- A 100,000-candidate native database reproduced the old initializer writer scan:
  **3,210,145 VM steps** (intentional baseline failure). The new eight-key writer
  used **6,783 VM steps**. EXPLAIN reported an indexed policy_hash search.
- Six actual Runtime retention calls drained 17 candidates and 17 true orphan
  initializers in three calls each, retaining their durable phase until the last
  prefix. Existing candidate/live/HOF/retired/old champion/cohort owners kept their
  exact initializer values and bytes; existing cohort alpha/exposure and genomes
  remained unchanged. Missing-index business readiness created nothing; unknown
  same-name index startup was rejected and rolled back without overwriting it.
- Original publication cases still restore all five complete old/new artifacts
  equally, including POP24/HOF12, committee/stage/matrix and the actual full Foundry
  rosters/failure batches. The original 5,695 assertions were retained; additional
  caller fence assertions bring the same cases to 5,714.
- Unknown publication/item kinds were rejected. The old four-argument callback was
  never executed; the closed Legacy projector actually published all 36 fixture models.
- The original warm seed test passed all 24 family/composite cases, including kernel,
  optimizer/calibrator reset and corrupt/missing-initializer negatives. The scope is
  synthetic pure-model compatibility, not training quality.

[SOURCE_SCOPE_PROOF.json](SOURCE_SCOPE_PROOF.json) verifies 15 entire kernel/codec/
proposal sources plus the original warm numerical and identity method segments are
byte-identical to the isolated base. [SCHEMA_CONTRACT.json](SCHEMA_CONTRACT.json)
records the actual compiled API check: original ModelPublication REV1/24 objects/hash
unchanged; Warm REV1 adds exactly one partial index. It must be installed by the
existing startup owner after the canonical tables and R30 protected archives.

[ROOT_CALLERS.patch](ROOT_CALLERS.patch) is a proposed, tested temporary-source
overlay, **not applied to shared production**. SHA-256:
`fac234862215dba0da9aa9aa501b7a875a34ed2e705c669c9dd8c1dc2c79c03f`.
It applies to base `503efdc2c7abdb78b6e30fbd9d881d8dc772265e`; exact before/after
hashes are in [ROOT_CALLERS.json](ROOT_CALLERS.json). It switches the three existing
publication callers to the three-argument API, prepares Foundry preservation/ABI
keys outside the writer, and prepares Runtime retention phase 4/5 keys on the read
worker. A phase with a remaining prefix does not advance its durable checkpoint.
Compatibility methods remain bounded, but live callers need these integration hunks.

The first saved baseline (`20261104T211131Z`) retains the real 128/2,048 fanout
failures. Its 512 MiB warm birth fit the old bind cap; the actual 64 MiB run failed
179,938 > 131,072 bytes. The saved actual ABI task (`20261104T215254Z`) exercised
the real golden asset and shadow semantic path. Its separate Legacy roster-scope
negative remains OPEN and no Legacy ABI producer was modified. Failed fixture
setup (missing operation), an incorrect test main package and an attempted test
subclass of final DbStore are also retained with their original errors.

New staging uses the existing admitted ModelPublication pool (finite protected
logical bytes/parts/items/roots) and the existing admitted checkpoint CAS root.
Numerical preparation has a production MemoryBudget lease retained until actual
preparation/copies/fsync exit. Protected storage debt is rejected without deleting
old evidence. Global physical quota integration and other owners remain OPEN.

Raw traces, old/new artifacts and the failed private fixture database are losslessly
gzip-archived. [ARCHIVE_INDEX.json](ARCHIVE_INDEX.json) records original and gzip
hashes. `verify_receipt.py` checks decompressed bytes, command exits, source and
caller hashes, counters and all SQL budgets without rerunning numerical groups.
It does not count archiving host evidence as Android application GC.

Reproduction uses the copied fixed actual 4,459-class Android app jar SHA-256
`582f7475b43d218c50d34f8f24d4399ae4916b53579807c22c162bd25b7fea56`,
real native SQLite/JDBC and AOSP JSON. `run_native.py` runs the named Warm group;
`run_publication_regression.py` uses the frozen previous module's original tests.
For a fresh native run, pass its printed receipt path to
`run_followup.py --prior /absolute/path/to/receipt.json`. The default followup
receipt preserves the actually measured pre-StatFs/final split in this delivery.
The host StatFs transport queries actual Linux FileStore capacity and supplies no
fixed free-space values. No Gradle, mutable main classes, device database or APK
delivery was used by this module.
