# Current product repair: certificate admission and publication

The complete paired Future fixture exposed an actual product blocker. The certificate
cap required ATOM_BINDING_ADMISSION, but cp4ExternalGates did not call the actual atom
admission reader. Consequently an otherwise eligible certificate could never publish.

CertificateBundleServiceR929 now prepares AtomAdmissionCP12.readForLifetimeCP12 outside
the writer, under a bounded identity read and preparation deadline. It checks the
actual frozen structure/model against the certificate lifetime and retains the
reader's immutable gate proof. Missing or invalid evidence produces a rejecting gate
with the raw typed reason. Resource yield propagates for resumable scheduling.

CertificateDependenciesR932 now checks the certificate's validFromIssue during
publication. The prior ThreadLocal request target was empty in the asynchronous
publisher and rejected valid atom PIT evidence. Publication also fences the paired
matrix proofs through the existing ProspectiveDiversityCP12 owner before committing.

Actual consumers: CertificatePublisherR967 preparation -> cp4ExternalGates ->
prepared certificate -> CertificatePublisherR967 writer -> validateCp4Publication ->
certificate/MCE persistence. No admission threshold or REAL authority was relaxed.
No schema change is necessary; the existing immutable atom and matrix owners persist
and fence the proofs.

Verification: CertificateCommonFutureR982 retains the supplied certificate test's
assertions and adds explicit synthetic paired and frozen atom producer fixtures.
17 assertions exercise missing peers, successful publication, changed source,
degraded atom admission and changed paired matrix rejection with rollback. All
synthetic evidence stays in isolated host databases; it is not production Future
performance or authority to bet. See the current delivery's execution receipts and
failure logs, including the original missing-gate and empty-request-target failures.

Extended helper provenance: ModernModelContractTest, DevelopmentTrainerContractCP9
and TrainingFixtureCP11 are newly written test-only helpers, because the original
helpers are absent from the supplied plan. Supplied assertion sources remain
unchanged. Host API/JNI adapters do not verify Android service/device scheduling.
