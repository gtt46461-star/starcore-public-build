package com.ethk3.novaonepro;

import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ethk3.novaonepro.strategyv2.RealAuthoritySU2;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Exercises the normal signed analysis owner on an engineering-only original SU2 lane fixture. */
@RunWith(AndroidJUnit4.class)
public final class R343RealAuthorityInstrumentedTest {
    @Test public void r343_034_user_disabled_rejects_real() throws Exception { reject("USER_DISABLED"); }
    @Test public void r343_034_expired_original_rejects_real() throws Exception { reject("EXPIRED_CERTIFICATE"); }

    private static void reject(String boundary) throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            try (R30DecisionChainAndroidTest.Fixture fixture = new R30DecisionChainAndroidTest.Fixture()) {
                checks.require(RealAuthoritySU2.allows(fixture.store, fixture.candidate(), fixture.certificate,
                        fixture.target, fixture.deadline), "precondition is a valid engineering SU2 authority");
                if ("USER_DISABLED".equals(boundary)) {
                    fixture.store.setState(RealAuthoritySU2.USER_SWITCH, "0");
                    checks.equal("0", fixture.store.getState(RealAuthoritySU2.USER_SWITCH, "1"), "persisted REAL setting is disabled");
                } else {
                    long now = System.currentTimeMillis();
                    fixture.store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R343_SYNTHETIC_EXPIRE_CERTIFICATE", db -> {
                        db.execSQL("UPDATE su2_certificate SET issued_at=?,expires_at=? WHERE certificate_id=?",
                                new Object[]{now - 4000, now - 1000, fixture.certificate});
                        return 0;
                    });
                }
                checks.require(!RealAuthoritySU2.allows(fixture.store, fixture.candidate(), fixture.certificate,
                        fixture.target, fixture.deadline), "original SU2 authority denies this target");
                V51RuntimeOrchestrator.OutcomeR36 outcome = R30DecisionChainAndroidTest.originalOwnerAttempt(fixture, "R343_034_" + boundary);
                V51Models.AuthorizedDecision authorized = outcome.runtime.authorized;
                checks.require(authorized == null || authorized.action != V51Enums.DecisionAction.BET, "the normal analysis owner cannot authorize REAL");
                checks.equal(null, fixture.store.getTradeByTargetIssue(fixture.target), "no REAL trade exists for the target");
                checks.equal(0L, count(fixture, "SELECT count(*) FROM " + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                        + " WHERE target_issue=? AND decision_action='BET'", fixture.target), "no BET publication");
                checks.equal(0L, count(fixture, "SELECT count(*) FROM r974_exact_real_receipt WHERE target_issue=?", fixture.target), "no exact REAL receipt");
                checks.equal(1L, count(fixture, "SELECT count(*) FROM su2_certificate WHERE certificate_id=?", fixture.certificate), "prior synthetic certificate original retained");
                checks.require(count(fixture, "SELECT count(*) FROM " + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                        + " WHERE target_issue=?", fixture.target) > 0, "actual normal analysis request persisted");
                checks.database("SYNTHETIC_SU2_ORIGINAL_AUTHORITY", fixture.target, fixture.deadline,
                        RuntimeObservationScopeR341.databaseIdentity(fixture.db));
                checks.record("injectedBoundary", boundary);
                checks.record("retainedSyntheticCertificate", fixture.certificate);
                checks.record("action", authorized == null ? "NONE" : authorized.action.name());
                checks.record("rejectCode", authorized == null ? "NO_AUTHORIZED_DECISION" : authorized.rejectCode.name());
                checks.record("normalOwnerRuntimeSuccess", outcome.runtime.success);
                checks.record("normalOwnerCommitSuccess", outcome.commit != null && outcome.commit.success);
                checks.record("canonicalRealReceipts", 0L);
            }
        });
    }

    private static long count(R30DecisionChainAndroidTest.Fixture fixture, String sql, String argument) {
        try (Cursor rows = fixture.db.rawQuery(sql, new String[]{argument})) {
            if (!rows.moveToFirst()) throw new AssertionError("No database readback for " + sql);
            return rows.getLong(0);
        }
    }
}
