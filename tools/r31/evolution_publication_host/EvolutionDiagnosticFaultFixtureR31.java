package com.ethk3.novaonepro;
import java.util.concurrent.atomic.AtomicInteger;
/** Same-package HOST access to the existing production diagnostic fault hook; no new app hook. */
public final class EvolutionDiagnosticFaultFixtureR31 {
 private EvolutionDiagnosticFaultFixtureR31(){}
 public static void installPostcommit(AtomicInteger count){DatabaseCoordinator.diagnosticFaultR31.set(phase->{if("AFTER_COMMIT".equals(phase)){count.incrementAndGet();throw new IllegalStateException("INJECTED_POSTCOMMIT_ACK_DIAGNOSTIC");}});}
 public static void clear(){DatabaseCoordinator.diagnosticFaultR31.remove();}
}
