package com.ethk3.novaonepro.strategyv2;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/** Shared fixtures for the R27 HOST tests (docs/12 12.2). Paths are resolved from the repository root so the tests
 * run identically under ./gradlew test (working dir app/) and the standalone runner. */
final class R27TestSupport {
    private R27TestSupport() {}
    static File root() {
        File d = new File(System.getProperty("user.dir")).getAbsoluteFile();
        for (int i = 0; i < 5 && d != null; i++, d = d.getParentFile()) if (new File(d, "research_data").isDirectory() && new File(d, "app").isDirectory()) return d;
        throw new IllegalStateException("R27_TEST_ROOT_NOT_FOUND");
    }
    static ReplayDatasetR27 csv(int tail) throws Exception {
        try (InputStream in = new FileInputStream(new File(root(), "research_data/ethpk10_draws_20260316_201059.csv"))) { return ReplayDatasetR27.readCsv(in).tail(tail); }
    }
    static ReplayDatasetR27 golden() throws Exception {
        try (InputStream in = new FileInputStream(new File(root(), "app/src/main/assets/" + SemanticAbiR27.GOLDEN_ASSET))) { return SemanticAbiR27.readGolden(in, true); }
    }
    static void log(String s) { System.out.println("R27_HOST|" + s); }
}
