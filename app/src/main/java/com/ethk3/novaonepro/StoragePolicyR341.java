package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Admission rules for additional on-disk growth. Existing originals take physical space. */
final class StoragePolicyR341 {
    static final long ORDINARY_REQUEST_BYTES = 128 * StorageBudgetR31.MiB;
    static final long ORDINARY_WORKING_BYTES = 256 * StorageBudgetR31.MiB;
    static final String HASH = StorageBudgetR31.sha(("R341_DISK_GROWTH_V1|" + ORDINARY_REQUEST_BYTES
            + "|" + ORDINARY_WORKING_BYTES + "|DURABLE_OR_SNAPSHOT_PHYSICAL_ADMISSION")
            .getBytes(StandardCharsets.UTF_8));

    static boolean ordinary(StorageBudgetR31.Category category) {
        return category == StorageBudgetR31.Category.CACHE;
    }

    static final class Admission {
        final String decision, reason;
        final long remainingGrowth;

        Admission(String reason, long remainingGrowth) {
            this.decision = reason.isEmpty() ? "ALLOW" : "DEFER";
            this.reason = reason;
            this.remainingGrowth = Math.max(0, remainingGrowth);
        }

        Map<String,Object> facts() {
            Map<String,Object> facts = new LinkedHashMap<>();
            facts.put("decision", decision);
            facts.put("reason", reason);
            facts.put("remainingGrowthBytes", remainingGrowth);
            facts.put("policyHash", HASH);
            return facts;
        }
    }

    static Admission evaluate(StorageBudgetR31.Category category, long physicalFree, long physicalFloor,
            long promised, long ordinaryOccupiedAndPromised, long requested, long resultingOwnerBound) {
        long physicalCapacity = Math.max(0, physicalFree - physicalFloor);
        long physicalRemaining = Math.max(0, physicalCapacity - promised);
        if (requested > physicalRemaining || promised > physicalCapacity || physicalFree < physicalFloor) {
            return new Admission("FREE_SPACE_RESERVE", physicalRemaining);
        }
        if (!ordinary(category)) return new Admission("", physicalRemaining - requested);
        long ordinaryRemaining = Math.max(0, ORDINARY_WORKING_BYTES - ordinaryOccupiedAndPromised);
        if (resultingOwnerBound > ORDINARY_REQUEST_BYTES || requested > ordinaryRemaining) {
            return new Admission("ORDINARY_WORKING_GROWTH_LIMIT", Math.min(physicalRemaining, ordinaryRemaining));
        }
        return new Admission("", Math.min(physicalRemaining - requested, ordinaryRemaining - requested));
    }

    static Map<String,Object> reconcile(long baseline, long actual, long promised) {
        long used = Math.max(0, actual - baseline);
        Map<String,Object> receipt = new LinkedHashMap<>();
        receipt.put("actualAdditionalBytes", used);
        receipt.put("promisedGrowthBytes", promised);
        receipt.put("unusedGrowthBytes", Math.max(0, promised - used));
        receipt.put("policyHash", HASH);
        return receipt;
    }

    private StoragePolicyR341() {}
}
