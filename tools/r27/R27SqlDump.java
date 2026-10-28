import com.ethk3.novaonepro.strategyv2.JsonR27;
import com.ethk3.novaonepro.strategyv2.MainlineBridgeSU2;
import com.ethk3.novaonepro.strategyv2.SchemaR27;
import com.ethk3.novaonepro.strategyv2.SequentialQualificationR27;
import com.ethk3.novaonepro.strategyv2.StoreR27;
import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Prints the exact production R27 SQL (migration DDL, guarded columns, intent query) as JSON for
 * tools/r27/test_r27_migration.py, so the sqlite test runs the statements the app runs. */
public final class R27SqlDump {
    public static void main(String[] a) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        List<Object> ddl = new ArrayList<>(); for (String s : SchemaR27.DDL) ddl.add(s); m.put("ddl", ddl);
        Field f = SchemaR27.class.getDeclaredField("COLUMNS"); f.setAccessible(true);
        List<Object> cols = new ArrayList<>(); for (String[] c : (String[][]) f.get(null)) { List<Object> x = new ArrayList<>(); for (String s : c) x.add(s); cols.add(x); }
        m.put("columns", cols); m.put("revision", (long) SchemaR27.REVISION);
        m.put("intentSql", MainlineBridgeSU2.R27_INTENT_SQL); m.put("qualified", SequentialQualificationR27.QUALIFIED);
        Map<String, Object> evo = new LinkedHashMap<>();
        evo.put("insert", StoreR27.R26_EVOLUTION_INSERT); evo.put("accepted", StoreR27.R26_EVOLUTION_ACCEPTED);
        evo.put("verified", StoreR27.R26_EVOLUTION_VERIFIED); evo.put("qualified", StoreR27.R26_EVOLUTION_QUALIFIED);
        Method q = Class.forName("com.ethk3.novaonepro.strategyv2.RuntimeReportR26").getDeclaredMethod("query", String.class); q.setAccessible(true);
        evo.put("reportQuery", q.invoke(null, "EVOLUTION"));
        m.put("r26Evolution", evo);
        System.out.println(JsonR27.write(m));
    }
}
