package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;
import org.junit.Test;

/** R31-B07-C01#A1/#A2: every default family is executed on the same causal sample stream (predict before update),
 * serialized, restored into a fresh instance and must then continue bit-exactly. Every failing family is listed red;
 * a supported-family declaration is never counted as a pass. F23 is not executable without its bound teacher. */
public final class R31FamilyExecutionMatrixTest {
    private static int[][] draws(int n) {
        Random r = new Random(23_2026); int[][] d = new int[n][];
        for (int t = 0; t < n; t++) { int[] v = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}; for (int k = 9; k > 0; k--) { int j = r.nextInt(k + 1), x = v[k]; v[k] = v[j]; v[j] = x; } d[t] = v; }
        return d;
    }
    private static long[] step(OnlineModelsSU2.Model m, int[] draw, float[] st) {
        float[][] p = new float[10][10]; m.predict(p, st);
        long[] bits = new long[100];
        for (int q = 0; q < 10; q++) for (int k = 0; k < 10; k++) {
            bits[q * 10 + k] = Float.floatToIntBits(p[q][k]);
        }
        m.update(draw);
        return bits;
    }
    @Test public void everyDefaultFamilyExecutesAndRestoresBitExactly() throws Exception {
        int[][] d = draws(60);
        List<String> ids = new ArrayList<>(FamilyRegistryR26.EXECUTABLE); ids.remove("F23");
        assertEquals("23 default families", 23, ids.size());
        Map<String, String> red = new TreeMap<>();
        for (String id : ids) {
            try {
                OnlineModelsSU2.Model original = FamilyRegistryR26.build(id);
                float[] st = new float[Math.max(1, original.stateWidth())];
                for (int t = 0; t < 40; t++) step(original, d[t], st);
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                original.write(new DataOutputStream(bytes));
                OnlineModelsSU2.Model restored = FamilyRegistryR26.build(id);
                DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes.toByteArray()));
                restored.read(in);
                if (in.available() != 0) throw new IllegalStateException("TRAILING_STATE_BYTES");
                float[] st2 = st.clone();
                for (int t = 40; t < 50; t++)
                    if (!Arrays.equals(step(original, d[t], st), step(restored, d[t], st2))) throw new IllegalStateException("RESTORE_DIVERGED_AT_" + t);
            } catch (RuntimeException | IOException failure) {
                red.put(id, failure.getClass().getSimpleName() + ":" + failure.getMessage());
            }
        }
        assertEquals("families that failed execution or exact restore (red, never counted as supported)", Collections.emptyMap(), red);
    }
    @Test public void f23WithoutABoundTeacherIsNotExecutable() {
        if (!FamilyRegistryR26.legacyBinding().isEmpty()) return; // another test in this JVM bound the original teacher
        assertFalse(FamilyRegistryR26.EXECUTABLE.contains("F23"));
        assertEquals("", FamilyRegistryR26.implementation("F23"));
        try { FamilyRegistryR26.build("F23"); fail("F23 built without its teacher"); }
        catch (IllegalArgumentException expected) { assertEquals("F23_ORIGINAL_TEACHER_NOT_BOUND", expected.getMessage()); }
        try { FamilyRegistryR26.genome("F23"); fail("F23 genome without teacher"); }
        catch (IllegalArgumentException expected) { assertEquals("R26_BLOCKED_FAMILY", expected.getMessage()); }
    }
}
