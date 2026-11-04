package com.ethk3.novaonepro;
public final class AtomNativeFaultsR31 {
 public static void clear(){DatabaseCoordinator.diagnosticFaultR31.set(null);}
 public static void failAck(){DatabaseCoordinator.diagnosticFaultR31.set(phase->{if(phase.equals("AFTER_COMMIT"))throw new IllegalStateException("HOST_ACTUAL_COMMITTED_ACK_FAILURE");});}
 public static void haltAfterCommit(){DatabaseCoordinator.diagnosticFaultR31.set(phase->{if(phase.equals("AFTER_COMMIT"))java.lang.Runtime.getRuntime().halt(73);});}
}
