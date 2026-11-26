package com.ethk3.novaonepro;

/** Conservative pre-admission for AtomicFile's physical envelope.
 * Logical stream size and physical allocation are different quantities.
 * The existing StorageBudget still measures and enforces the actual delta.
 * This bound is NOT a claim about every filesystem's allocation behavior. */
final class PreparedCanonicalAdmissionR343 {
    static final long MAX_LOGICAL_BYTES = 16L * 1024 * 1024;
    static final long ALLOCATION_SLAB_BYTES = 64L * 1024;
    private PreparedCanonicalAdmissionR343() {}

    static long physicalGrowthBound(long logicalUpperBound) {
        if (logicalUpperBound < 1 || logicalUpperBound > MAX_LOGICAL_BYTES)
            throw new IllegalArgumentException("R343_PREPARED_LOGICAL_BOUND");
        long slabs = Math.addExact(logicalUpperBound, ALLOCATION_SLAB_BYTES - 1)
                / ALLOCATION_SLAB_BYTES;
        long rounded = Math.multiplyExact(slabs, ALLOCATION_SLAB_BYTES);
        // Reserve for at most two atomic file images plus directory allocation.
        // No existing file is deleted or accepted merely because this is granted.
        return Math.addExact(Math.multiplyExact(rounded, 2L), ALLOCATION_SLAB_BYTES);
    }
}
