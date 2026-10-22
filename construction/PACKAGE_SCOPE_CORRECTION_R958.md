# R958 full-package scope correction

## Finding

The first R957 delivery archive was a compact active-engineering package, not the
fully inherited total package required by the construction plan.  Its APK and
active application source were valid, but the archive label
`FULLBLOOD_TOTAL` overstated its packaging scope.

Measured evidence:

| Archive/source | Files | Uncompressed bytes | ZIP bytes |
| --- | ---: | ---: | ---: |
| User R926 full baseline package | 5,749 | 550,200,574 | 307,017,254 |
| First compact R957 total | 1,985 | 49,598,336 | 20,699,364 |
| Corrected inherited R957 total | at least 6,249 | about 579 MB | about 318 MB |
| R926 complete-source tree | 1,626 | 27,100,561 | n/a |
| First compact R957 source ZIP | 1,824 | 34,157,173 | 7,788,788 |
| Corrected inherited R957 source ZIP | at least 2,416 | about 40.5 MB | about 10.2 MB |

The corrected-package rows are scope measurements rather than immutable byte
identities: the embedded Git bundle and final receipts change when the sealing
commit changes.  The outer `SHA256SUMS.txt` is authoritative for the exact final
byte size and digest.

The missing weight was primarily the original package snapshot, Android build
toolchain, synthetic verification fixtures, previous delivery archive, baseline
snapshot, rollback material, historical reports, `.github` harnesses, tools and
research data.  It was not missing current `app/` code: all 649 R926 application
paths were present in R957, with 169 additional application paths.

## Correction

`scripts/r952_release_pack.sh` now supports `R952_LEGACY_PAYLOAD_DIR` and builds a
full inherited package as follows:

1. Copy the R926 complete-source tree.
2. Overlay the sealed R957 Git tree; R957 always wins on matching paths.
3. Retain the full inherited non-source payload under
   `16_INHERITED_R926_FULL_PAYLOAD`.
4. Exclude `06_PRIVATE_SIGNING` from the distributable package.
5. Emit `SOURCE_INHERITANCE_RECEIPT.json` with source and payload counts.
6. Recompute all package SHA-256 manifests and test both ZIP archives.

The correction changes packaging only.  The signed APK bytes remain unchanged:

`5ce3e14bfcf8b132a9cd759e325e9917f04524937e989d363610ef6b10acc4f4`

## Acceptance boundary

The corrected archive resolves the total-package completeness defect.  It does
not convert physical-device or future-live-draw gates from `NOT_RUN` to `PASS`.
