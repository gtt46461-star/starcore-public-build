package android.os;

/** Real host PID for process-generation replay; no Android/device identity claim. */
public final class Process {
    private Process() {}
    public static int myPid() { return Math.toIntExact(ProcessHandle.current().pid()); }
}
