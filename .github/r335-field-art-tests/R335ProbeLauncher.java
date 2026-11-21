package com.ethk3.novaonepro.strategyv2;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
public final class R335ProbeLauncher {
    public static void main(String[] args) {
        try {
            String name = args[0];
            switch (name) {
                case "AtomNetSessionNumericalProbe":
                case "AtomNetSessionResumeReviewTest":
                case "ResearchConsumerR335Host":
                case "R335EpochReceiptProbe":
                case "R33NaturalQualificationHost":
                    break;
                default: throw new IllegalArgumentException();
            }
            String classname = "com.ethk3.novaonepro.strategyv2." + name;
            Class.forName(classname).getMethod("main", String[].class)
                    .invoke(null, (Object) Arrays.copyOfRange(args, 1, args.length));
        } catch (Throwable failure) {
            Throwable cause = failure instanceof InvocationTargetException ? failure.getCause() : failure;
            for (int depth = 0; cause != null && depth < 6; depth++, cause = cause.getCause()) {
                System.out.println("CONTROLLED_FAILURE_TYPE " + cause.getClass().getName());
                for (StackTraceElement frame : cause.getStackTrace()) {
                    if (frame.getClassName().startsWith("com.ethk3.novaonepro."))
                        System.out.println("CONTROLLED_FRAME " + frame.getClassName() + "." + frame.getMethodName() + ":" + frame.getLineNumber());
                }
            }
            System.out.flush();
            System.exit(1);
        }
    }
}
