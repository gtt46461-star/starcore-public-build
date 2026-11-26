import com.ethk3.novaonepro.strategyv2.StartupTempSchemaR343;
import com.ethk3.novaonepro.strategyv2.StartupProbePolicyR343;

public final class RuntimePolicyHarness {
    private static void check(boolean value, String name) {
        if (!value) throw new AssertionError(name);
    }
    public static void main(String[] args) {
        if (args.length == 4) {
            System.out.println(StartupTempSchemaR343.allowed(args[0],args[1],args[2],args[3]));
            return;
        }
        check(StartupTempSchemaR343.allowed("table","r31q_ddl_cache_flush","r31q_ddl_cache_flush",
                "CREATE TABLE r31q_ddl_cache_flush(x INTEGER)"), "legacy cache-flush table");
        check(!StartupTempSchemaR343.allowed("table","r31q_ddl_cache_flush","r31q_ddl_cache_flush",
                "CREATE TABLE r31q_ddl_cache_flush(x TEXT)"), "wrong column shape");
        check(!StartupTempSchemaR343.allowed("table","r29_model_checkpoint","r29_model_checkpoint",
                "CREATE TABLE r29_model_checkpoint(owner TEXT)"), "checkpoint shadow rejected");
        check(!StartupTempSchemaR343.allowed("trigger","r31q_ddl_cache_flush","r31q_ddl_cache_flush",
                "CREATE TABLE r31q_ddl_cache_flush(x INTEGER)"), "non-table rejected");
        check(!StartupProbePolicyR343.due("boot-a",300_999L,"boot-a",1000L), "cooldown");
        check(StartupProbePolicyR343.due("boot-a",301_000L,"boot-a",1000L), "bounded half-open");
        check(!StartupProbePolicyR343.due("boot-a",500L,"boot-a",1000L), "reversed clock");
        check(StartupProbePolicyR343.due("boot-b",500L,"boot-a",1000L), "new boot has own clock");
        check(!StartupProbePolicyR343.due("UNRESOLVED",500L,"boot-a",1000L), "unknown current boot");
        check(StartupProbePolicyR343.hasProbeBudget(0), "first recovery reservation");
        check(StartupProbePolicyR343.hasProbeBudget(2), "third recovery reservation");
        check(!StartupProbePolicyR343.hasProbeBudget(3), "exhausted stable-dependency probe budget");
        check(!StartupProbePolicyR343.hasProbeBudget(-1), "invalid counter cannot open circuit");
        System.out.println("PASS: 13 exact-contract, monotonic cooldown and finite probe-budget checks");
    }
}
