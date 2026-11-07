package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.Test;

/** R31-B07-C01#A2: F23 is not executable without its teacher; binding a teacher with the exact ABI/scope makes it
 * executable as the distillation model of that teacher (no silent fallback to another family). The binding is
 * process-global, so the bound half runs in a child JVM and cannot leak into other tests. */
public final class R31F23BoundTeacherTest {
    public static void main(String[] args) throws Exception {
        if (!FamilyRegistryR26.legacyBinding().isEmpty()) throw new AssertionError("child JVM started bound");
        FamilyRegistryR26.AcceptanceR31 unbound = FamilyRegistryR26.acceptR31("F23", R31FamilyCapabilityAcceptanceTest.stream(32, 5));
        if (unbound.status != FamilyRegistryR26.AcceptanceStatusR31.DEPENDENCY_NOT_READY) throw new AssertionError("unbound F23 ran: " + unbound.status);
        Map<String, String> binding = new TreeMap<>();
        binding.put("abi", "1"); binding.put("scope", "LEGACY_REPORTED_PREFERENCE_PROJECTION_V1");
        for (String key : new String[]{"asset", "teacher", "state"}) binding.put(key, StatsR27.sha256("R31_TEST_TEACHER_" + key));
        Map<String, String> wrongAbi = new TreeMap<>(binding); wrongAbi.put("abi", "2");
        try { FamilyRegistryR26.bindLegacyTeacher(wrongAbi, p -> null); throw new AssertionError("wrong ABI bound"); }
        catch (IllegalArgumentException expected) { if (!"F23_BINDING_ABI_OR_SCOPE".equals(expected.getMessage())) throw expected; }
        if (!FamilyRegistryR26.legacyBinding().isEmpty() || FamilyRegistryR26.EXECUTABLE.contains("F23")) throw new AssertionError("wrong ABI changed state");
        DistillationFamilyR26.Teacher teacher = new DistillationFamilyR26.Teacher() {
            public String identity() { return "R31_TEST_TEACHER"; }
            public double[][] marginal() { double[][] p = new double[10][10]; for (int q = 0; q < 10; q++) for (int k = 0; k < 10; k++) p[q][k] = k == q ? .55 : .05; return p; }
        };
        FamilyRegistryR26.bindLegacyTeacher(binding, params -> new DistillationFamilyR26(teacher));
        if (!FamilyRegistryR26.EXECUTABLE.contains("F23") || FamilyRegistryR26.implementation("F23").isEmpty()) throw new AssertionError("bound F23 not registered");
        OnlineModelsSU2.Model built = FamilyRegistryR26.build("F23");
        if (!(built instanceof DistillationFamilyR26)) throw new AssertionError("F23 fell back to " + built.getClass().getName());
        FamilyRegistryR26.AcceptanceR31 bound = FamilyRegistryR26.acceptR31("F23", R31FamilyCapabilityAcceptanceTest.stream(32, 5));
        if (!bound.executed()) throw new AssertionError("bound F23 did not execute: " + bound.reason);
        System.out.println("R31_F23_BOUND_PASS steps=" + bound.steps + " state=" + bound.stateSha256 + " dependency=" + FamilyRegistryR26.dependencyR31("F23").isEmpty());
    }
    @Test public void f23ExecutesOnlyAfterBindingItsTeacher() throws Exception {
        String java = new File(System.getProperty("java.home"), "bin/java").getPath();
        Process child = new ProcessBuilder(java, "-Xmx512m", "-cp", System.getProperty("java.class.path"), R31F23BoundTeacherTest.class.getName()).redirectErrorStream(true).start();
        String output; try (InputStream in = child.getInputStream()) { output = new String(in.readAllBytes(), StandardCharsets.UTF_8); }
        assertEquals(output, 0, child.waitFor());
        assertTrue(output, output.contains("R31_F23_BOUND_PASS steps=32 ") && output.contains("dependency=true"));
    }
}
