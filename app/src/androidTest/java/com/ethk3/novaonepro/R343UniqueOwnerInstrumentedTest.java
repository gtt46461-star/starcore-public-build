package com.ethk3.novaonepro;

import android.database.Cursor;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ethk3.novaonepro.strategyv2.RealAuthoritySU2;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class R343UniqueOwnerInstrumentedTest {
    @Test
    public void existingCanonicalRealCannotBeIssuedByASecondGate() throws Exception {
        Field173NativeEvidence.run((app, checks) -> {
            try (R30DecisionChainAndroidTest.Fixture fixture = new R30DecisionChainAndroidTest.Fixture()) {
                ProductionOwnerCatalogR811.Validation owner = ProductionOwnerCatalogR811.validate();
                checks.require(owner.passed, "Static owner declaration validates");
                checks.require(!V51AutoFetchBridge.canCreateBet(), "Transport bridge cannot issue a decision");
                checks.require(!V51AutoFetchBridge.canSettle(), "Transport bridge cannot settle");
                V51RuntimeOrchestrator.OutcomeR36 first = R30DecisionChainAndroidTest.originalOwnerAttempt(
                        fixture, "R343_SYNTHETIC_UNIQUE_REAL_OWNER");
                checks.require(first.runtime.success && first.commit != null && first.commit.success,
                        "Existing single-flight runtime committed its decision");
                checks.equal(V51Enums.DecisionAction.BET, first.runtime.authorized.action,
                        "Valid synthetic certificate reaches original decision owner");
                checks.require(!RealAuthoritySU2.allows(fixture.store, fixture.candidate(), fixture.certificate,
                        fixture.target, fixture.deadline), "Existing REAL closes target authority");

                V51Models.AuthorizedDecision rejected = new DecisionGateEngine().decideV51B12(
                        fixture.store, first.runtime.contract);
                checks.require(rejected.action != V51Enums.DecisionAction.BET,
                        "Independent gate instance cannot issue a second REAL");
                try (Cursor actual = fixture.db.rawQuery("SELECT e.authorization_hash,t.position,t.target_issue,t.n1,t.n2,t.n3"
                        + " FROM r974_exact_real_receipt e JOIN trades t ON t._id=e.trade_id"
                        + " WHERE e.target_issue=? AND t.target_issue=e.target_issue", new String[]{fixture.target})) {
                    checks.require(actual.moveToFirst(), "Actual original REAL receipt exists");
                    checks.require(actual.getInt(1) >= 1 && actual.getInt(1) <= 10, "One legal position");
                    checks.equal(fixture.target, actual.getString(2), "Original target identity preserved");
                    int number1 = actual.getInt(3);
                    int number2 = actual.getInt(4);
                    int number3 = actual.getInt(5);
                    checks.require(AppConfig.isValidNumber(number1) && AppConfig.isValidNumber(number2)
                                    && AppConfig.isValidNumber(number3) && number1 != number2 && number1 != number3 && number2 != number3,
                            "Original authorization carries a distinct single-position K=3");
                    checks.record("originalAuthorization", actual.getString(0));
                    checks.require(!actual.moveToNext(), "Only one durable authorization survives");
                }
                String databaseIdentity = RuntimeObservationScopeR341.databaseIdentity(fixture.db);
                checks.require(databaseIdentity.matches("[a-f0-9-]{36}"), "Actual production DB identity");
                checks.record("ownerCatalogHash", owner.hash);
                checks.record("duplicateDecision", rejected.action.name());
                checks.record("duplicateReject", rejected.rejectCode.name());
                checks.record("certificateScope", "SYNTHETIC_MECHANICS");
                checks.database("ENGINEERING_SYNTHETIC", fixture.target, fixture.deadline, databaseIdentity);
            }
        });
    }
}
