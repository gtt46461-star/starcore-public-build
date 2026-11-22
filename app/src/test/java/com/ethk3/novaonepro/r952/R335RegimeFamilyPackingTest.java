package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.ReleaseManifestR926;
import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class R335RegimeFamilyPackingTest {
    private static Map<String, Object> fields(Object... pairs) {
        Map<String, Object> result = new TreeMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    private static CandidateAstR952 parentAt(int position) {
        List<String> scores = Collections.nCopies(10, "0x1.0p0");
        List<?> empty = Collections.emptyList();
        Map<String, Object> score = fields("id", "n0", "operator", "CONSTANT", "atomId", "",
                "outputType", "SCORE_VECTOR10", "stage", "FROZEN_PREDRAW", "unit", "dimensionless",
                "parameters", scores, "parents", empty);
        Map<String, Object> root = fields("version", 3, "compilerRevision", "AST3_CP4_1",
                "position", position, "game", ReleaseManifestR926.GAME_CODE, "family", "FREQUENCY",
                "modelKind", "FIXED_GRAPH", "normalizerPolicy", "TRAIN_ONLY_ZSCORE_V1",
                "nodes", Collections.singletonList(score), "featureOutputIds", empty,
                "outputId", "n0", "inputDimension", 10, "experts", empty);
        root.put("featureManifest", fields("revision", "CP4_FEATURE_1",
                "graphHash", CandidateAstR952.hashGraphKernelWithoutManifest(root),
                "orderedFeatureIds", empty, "featureTypes", empty, "requiredHistory", 0,
                "atomSpecHashes", empty, "bindingHashes", empty, "matrixSpecRefs", empty,
                "normalizerPolicy", "TRAIN_ONLY_ZSCORE_V1", "missingPolicyPerFeature", empty,
                "inputDimension", 10, "estimatedStateBytes", 4096));
        return CandidateAstR952.parse("AST3:" + StrictCanonicalJson.write(root));
    }

    @Test public void registeredRegimeGraphsCompileAndReopenForAllPositionsAndWindows() throws Exception {
        Map<Integer, List<AtomSpecCP4>> byPosition = new TreeMap<>();
        try (BufferedReader r = Files.newBufferedReader(Paths.get("src/main/assets/cp4_atom_registry.jsonl"), StandardCharsets.UTF_8)) {
            String line;
            while ((line = r.readLine()) != null) {
                AtomSpecCP4 spec = AtomSpecCP4.fromAssetLine(line);
                if (Arrays.asList("MARKOV_SCORE", "FREQUENCY", "REGIME_SCORE", "OMISSION").contains(spec.family))
                    byPosition.computeIfAbsent(spec.position, key -> new ArrayList<>()).add(spec);
            }
        }
        int[] windows = {50, 120, 300, 500};
        for (int position = 1; position <= 10; position++) {
            CandidateAstR952 parent = parentAt(position);
            List<AtomSpecCP4> registry = byPosition.get(position);
            for (int i = 0; i < windows.length; i++) {
                int window = windows[i];
                CandidateAstR952 child = ModernFamilyFactoryCP12.newFamilyFromParentCP12(
                        parent, CandidateAstR952.Family.REGIME, registry, Collections.emptyList(), i * 10 + position - 1);
                assertTrue(child.canonical.getBytes(StandardCharsets.UTF_8).length <= 32768);
                CandidateAstR952 reopened = CandidateAstR952.parse(child.canonical);
                List<AtomSpecCP4> used = MutationGraphCP12.usedSpecs(reopened, registry);
                CandidateCompilerR952.compile(reopened, reopened.graphSpec.manifest, used);
                assertEquals(child.structuralHash, reopened.structuralHash);
                assertEquals(5, used.stream().filter(a -> a.family.equals("REGIME_SCORE")).count());
                assertEquals(10, used.stream().filter(a -> a.family.equals("MARKOV_SCORE") && a.mathSpec.w == window).count());
                assertEquals(10, reopened.graphSpec.manifest.physicalDimensionsR82.size());
                Set<String> partitions = new HashSet<>();
                for (FeatureManifestR952.MatrixRef ref : reopened.graphSpec.manifest.matrixSpecRefs) {
                    assertTrue(ref.partitionRule.startsWith("CP12/" + window + "/CROSSNEXT/"));
                    assertTrue(partitions.add(ref.partitionRule));
                }
                assertEquals(10, partitions.size());
            }
        }
    }

    @Test public void probabilityPackingPreservesEveryValueAndMissingFlag() {
        TypedOperationsCP4.Value[] raw = new TypedOperationsCP4.Value[10];
        TypedOperationsCP4.Value[] cast = new TypedOperationsCP4.Value[10];
        for (int mask = 0; mask < 1024; mask++) {
            for (int d = 0; d < 10; d++) {
                raw[d] = new TypedOperationsCP4.Value(new double[]{d / 9.0}, new boolean[]{(mask & (1 << d)) != 0});
                cast[d] = TypedOperationsCP4.apply("NUMERIC_TO_DOUBLE", 1, new TypedOperationsCP4.Value[]{raw[d]}, new double[0]);
            }
            TypedOperationsCP4.Value before = TypedOperationsCP4.apply("PACK10", 10, cast, new double[0]);
            TypedOperationsCP4.Value after = TypedOperationsCP4.apply("PACK10", 10, raw, new double[0]);
            assertArrayEquals(before.x, after.x, 0.0);
            assertArrayEquals(before.present, after.present);
        }
    }
}
