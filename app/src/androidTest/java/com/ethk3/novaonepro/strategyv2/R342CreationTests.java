package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.LegacyTeacherOwnerR26;
import java.util.List;

public final class R342CreationTests {
    private R342CreationTests() {}

    private static final String DISCOVERY=StatsR27.sha256("R342 original discovery fixture");
    private static final String TRANSFER=StatsR27.sha256("R342 independent transfer fixture");
    private static final String PAIRED_A=StatsR27.sha256("R342 parent A paired receipt");
    private static final String PAIRED_B=StatsR27.sha256("R342 parent B paired receipt");

    private static PolicySpecR27 policy(String ast) {
        return new PolicySpecR27(PolicyAstR27.parse(ast),1,PolicySpecR27.Rule.QUANTILE,
                0d,.15d,1d,true,.01d,.01d,
                IdeaFamilyR27.meta("I09","R342_PROBE",1,18L,"Program in an isolated development fixture"));
    }

    public static void preservedOriginal(Context app) throws Exception {
        R342StartupTests.isolatedSchema(app);
        new R26PublicationDeviceTest().missingWarmCheckpointCannotSilentlyBecomeColdTraining();
    }

    public static void independentStructures(Context app) throws Exception {
        R341FoundryTestCases.distinguishTopologyAndParameterVersions(app);
        R341FoundryTestCases.freshInputMayRetryExactlyOneBirth(app);
    }

    public static void typedRuleArtifact(Context ignored) {
        PolicySpecR27 a=policy("(add (zscore (freq 1 8)) (zscore (trans1)))");
        PolicySpecR27 b=policy("(add (zscore (freqnb 1 16)) (zscore (trans2)))");
        TypedGenerationRuleR342 rule=TypedGenerationRuleR342.induct(a,PAIRED_A,b,PAIRED_B,DISCOVERY,
                4L,1000L,1999L,FrozenConfirmationR30.currentSemantics());
        assertEquals(rule.ruleId,TypedGenerationRuleR342.induct(b,PAIRED_B,a,PAIRED_A,DISCOVERY,
                4L,1000L,1999L,FrozenConfirmationR30.currentSemantics()).ruleId);
        assertThrows(IllegalArgumentException.class,()->rule.instantiate(TRANSFER,1500L,2100L,2));
        assertThrows(IllegalArgumentException.class,()->rule.instantiate(DISCOVERY,2000L,2900L,2));
        assertThrows(IllegalArgumentException.class,()->rule.instantiate(TRANSFER,0L,900L,2));
        assertThrows(IllegalArgumentException.class,()->TypedGenerationRuleR342.induct(a,PAIRED_A,
                policy("(add (zscore (freq 1 16)) (zscore (trans1)))"),PAIRED_B,
                DISCOVERY,4L,1000L,1999L,rule.abi));
        try (SQLiteDatabase db=SQLiteDatabase.create(null)) {
            for(String ddl:AtomPublicationStoreR31.ruleDdlR342())db.execSQL(ddl);
            db.execSQL("INSERT INTO "+AtomPublicationStoreR31.RULES_R342+" VALUES(?,?,?,?,?,?,?,?,?)",
                    new Object[]{rule.ruleId,rule.shapeHash,rule.dataset,rule.revision,rule.abi,rule.from,rule.through,
                            rule.encodeCanonical(),System.currentTimeMillis()});
            assertThrows(android.database.sqlite.SQLiteConstraintException.class, () -> db.execSQL(
                    "UPDATE " + AtomPublicationStoreR31.RULES_R342+" SET canonical='{}'"));
            try (android.database.Cursor stored=db.rawQuery(
                    "SELECT canonical FROM "+AtomPublicationStoreR31.RULES_R342+" WHERE rule_id=?",
                    new String[]{rule.ruleId})) {
                assertTrue(stored.moveToFirst());
                TypedGenerationRuleR342 reopened=TypedGenerationRuleR342.decode(stored.getString(0));
                assertEquals(rule.ruleId,reopened.ruleId);
                List<PolicySpecR27> generated=reopened.instantiate(TRANSFER,2000L,2999L,2);
                assertEquals(2,generated.size());
                assertNotEquals(FormulaIdentityR341.structure(generated.get(0)),FormulaIdentityR341.structure(generated.get(1)));
                for (PolicySpecR27 child:generated) {
                    assertNotEquals(a.policyHash(),child.policyHash());
                    assertNotEquals(b.policyHash(),child.policyHash());
                    assertTrue(BirthGateR27.candidateContract(child).ok);
                    assertEquals(new java.util.HashSet<>(java.util.Arrays.asList(a.policyHash(),b.policyHash())),
                            new java.util.HashSet<>(child.meta.parents));
                }
            }
        }
    }

    public static void candidateIdentity(Context ignored) {
        PolicySpecR27 frequency=policy("(zscore (freq 1 8))");
        PolicySpecR27 transition=policy("(zscore (trans1))");
        PolicySpecR27 retriever=policy("(zscore (knn 3 20))");
        List<PolicySpecR27> candidates=java.util.Arrays.asList(frequency,transition,retriever);
        java.util.Set<String> identities=new java.util.HashSet<>();
        for (PolicySpecR27 candidate:candidates) {
            assertTrue(BirthGateR27.candidateContract(candidate).ok);
            assertTrue(identities.add(FormulaIdentityR341.structure(candidate)));
        }
    }

    public static void atomAndMoleculeEvidence(Context app) throws Exception {
        R341ResearchTestCases.selectedProgramsUseOnlyEffectiveInputs(app);
        R341ResearchTestCases.onlyActualTopologyEditGetsCounterfactual(app);
        PolicyAstR27.Node molecule=PolicyAstR27.op("mix",.5d,
                PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,8)),
                PolicyAstR27.op("zscore",PolicyAstR27.leaf("trans1")));
        assertEquals(PolicyAstR27.Type.SCORE,PolicyAstR27.type(molecule));
        assertNotEquals(FormulaIdentityR341.structure(policy(molecule.toString())),
                FormulaIdentityR341.structure(policy(molecule.kids[0].toString())));
        assertNotEquals(FormulaIdentityR341.structure(policy(molecule.toString())),
                FormulaIdentityR341.structure(policy(molecule.kids[1].toString())));
    }

    public static void originalTeacher(Context app) throws Exception {
        LegacyTeacherOwnerR26.register(app);
        assertEquals(com.ethk3.novaonepro.HistoricalGenomeImportR967.SOURCE_SHA,FamilyRegistryR26.legacyBinding().get("asset"));
        assertEquals("",FamilyRegistryR26.dependencyR31("F13"));
        assertEquals("",FamilyRegistryR26.dependencyR31("F23"));
        new R26DeviceTest().originalPublicAssetTeacherBindsStateAndCannotReadTargetLabel();
    }
}
