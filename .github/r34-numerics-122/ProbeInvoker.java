import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public final class ProbeInvoker {
    public static void main(String[] args) throws Exception {
        try {
            Class.forName("com.ethk3.novaonepro.strategyv2." + args[0])
                    .getMethod("main", String[].class)
                    .invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
            System.out.println("ORIGINAL_MAIN_RETURNED");
        } catch (InvocationTargetException cause) {
            cause.getTargetException().printStackTrace(System.err);
            System.err.flush();
            System.exit(3);
        }
    }
}
