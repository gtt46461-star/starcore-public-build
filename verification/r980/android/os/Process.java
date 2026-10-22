package android.os;
/** Host-only process API adapter. Real JVM PID; does not emulate Android lifecycle. */
public final class Process {
    private Process() {}
    public static int myPid() { return Math.toIntExact(ProcessHandle.current().pid()); }
}
