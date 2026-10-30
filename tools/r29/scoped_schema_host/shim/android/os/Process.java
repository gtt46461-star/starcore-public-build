package android.os;

/** HOST identity/Java thread priority only. Does not model Android process or scheduler behavior. */
public final class Process {
    public static final int THREAD_PRIORITY_DEFAULT=0,THREAD_PRIORITY_LOWEST=19,THREAD_PRIORITY_BACKGROUND=10,THREAD_PRIORITY_FOREGROUND=-2,THREAD_PRIORITY_MORE_FAVORABLE=-1,THREAD_PRIORITY_LESS_FAVORABLE=1;
    private Process(){}
    public static int myPid(){return (int)java.lang.ProcessHandle.current().pid();}
    public static int myTid(){return (int)Thread.currentThread().getId();}
    public static int myUid(){throw new UnsupportedOperationException("HOST_ANDROID_UID_UNAVAILABLE");}
    public static void setThreadPriority(int priority){priority(Thread.currentThread(),priority);}
    public static void setThreadPriority(int tid,int priority){for(Thread thread:Thread.getAllStackTraces().keySet())if((int)thread.getId()==tid){priority(thread,priority);return;}throw new IllegalArgumentException("HOST_THREAD_MISSING:"+tid);}
    private static void priority(Thread thread,int priority){if(priority<-20||priority>19)throw new IllegalArgumentException("HOST_ANDROID_PRIORITY_RANGE");int javaPriority=priority>=10?Thread.MIN_PRIORITY:priority<0?Math.min(Thread.MAX_PRIORITY,Thread.NORM_PRIORITY+1):Thread.NORM_PRIORITY;thread.setPriority(javaPriority);}
}
