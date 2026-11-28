package com.ethk3.novaonepro.r952;

import org.junit.Test;
import static org.junit.Assert.*;

/** Whitelist admission only. Original CAS/AST/protocol verification still runs in verifyAndResume. */
public final class TrainingRecoveryR343AdmissionTest {
    private static String[] originalRuleTask() {
        String[] row = new String[22];
        String[] identity = {"678d059b7207c6f2ec712c0cdd906dbc201357a61632236b8019af0c67ce740f",
                "CP12-FAMILY-ebfcc38e32ef9155cf4cbae7",
                "ab1473934a4326bf05eeef4ed8e5dd015a0b4dddff7aeed7791e52e6dc5594da",
                "129c749b58ccfe9884070607d83b7ec51e2f742481495c0d24d2050c7b012f01",
                "1b75dcb6745bbff2bbc0d669159523e34fae306e1f2ce6b1304cfa8c9eb1370d"};
        System.arraycopy(identity, 0, row, 0, identity.length);
        row[6] = TrainingRecoveryR343.REASON;
        row[7] = "FAILED_TERMINAL";
        row[10] = "QUARANTINED";
        row[11] = "REJECTED";
        row[12] = TrainingRecoveryR343.REASON;
        return row;
    }
    @Test public void auditedFifthTaskIsEligibleForOriginalProofVerification() {
        assertTrue(TrainingRecoveryR343.known(originalRuleTask()));
    }
    @Test public void alteredIdentityOrLifecycleCannotEnterRecovery() {
        for (int column : new int[]{0, 1, 2, 3, 4, 6, 7, 10, 11, 12}) {
            String[] changed = originalRuleTask();
            changed[column] += "-CHANGED";
            assertFalse("column " + column, TrainingRecoveryR343.known(changed));
        }
        assertFalse(TrainingRecoveryR343.known(null));
        assertFalse(TrainingRecoveryR343.known(new String[12]));
    }
}
