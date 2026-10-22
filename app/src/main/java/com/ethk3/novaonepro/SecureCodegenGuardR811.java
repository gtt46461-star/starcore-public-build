package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Fail-closed whitelist guard for audit-only Research code generation. */
public final class SecureCodegenGuardR811 {
    private static final Set<String> ALLOWED_IMPORTS =
            new HashSet<String>(
                    Arrays.asList(
                            "java.util.Arrays",
                            "java.util.List",
                            "java.util.Map",
                            "java.util.Collections",
                            "com.ethk3.novaonepro.ResearchDslCandidateRuntimeR811",
                            "com.ethk3.novaonepro.ResearchModelCandidateRuntimeR811",
                            "com.ethk3.novaonepro.ResearchArtifactCodecR811"));
    private static final String[] FORBIDDEN =
            new String[] {
                "java.lang.reflect",
                "Class.forName",
                "ClassLoader",
                "Runtime.getRuntime",
                "ProcessBuilder",
                "System.exit",
                "java.io.File",
                "java.net.",
                "android.database",
                "DecisionGateEngine",
                "SettlementEngine",
                "CanonicalObservationWriter",
                "REAL_BET_UNLOCK",
                "DexClassLoader",
                "loadClass"
            };

    private SecureCodegenGuardR811() {}

    public static Validation validateAst(ResearchStrategyDslEngine.Node node) {
        if (node == null) return Validation.reject("AST_NULL");
        try {
            String canonical = ResearchStrategyDslEngine.canonical(node);
            return validateTokens(canonical);
        } catch (Throwable e) {
            return Validation.reject("AST_INVALID:" + e.getClass().getSimpleName());
        }
    }

    public static Validation validateImports(String source) {
        String[] lines = safe(source).split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.startsWith("import ")) continue;
            String name = line.substring(7).replace(";", "").trim();
            if (!ALLOWED_IMPORTS.contains(name)) return Validation.reject("IMPORT:" + name);
        }
        return Validation.pass();
    }

    public static Validation validateTokens(String source) {
        String value = safe(source);
        for (int i = 0; i < FORBIDDEN.length; i++)
            if (value.indexOf(FORBIDDEN[i]) >= 0)
                return Validation.reject("TOKEN:" + FORBIDDEN[i]);
        return Validation.pass();
    }

    public static boolean withinQuota(int files, long bytes) {
        return files >= 0 && files <= 32 && bytes >= 0L && bytes <= 4L * 1024L * 1024L;
    }

    public static boolean researchPrivatePath(String relativePath) {
        String path = safe(relativePath).replace('\\', '/');
        return path.startsWith("research/generated/")
                && path.indexOf("..") < 0
                && !path.startsWith("/");
    }

    public static final class Validation {
        public final boolean accepted;
        public final String code;

        private Validation(boolean accepted, String code) {
            this.accepted = accepted;
            this.code = code;
        }

        static Validation pass() {
            return new Validation(true, "PASS");
        }

        static Validation reject(String code) {
            return new Validation(false, code);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}
