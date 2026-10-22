package android.os;
/** Host adapter: real cancellation state; no Android device timing claim. */
public final class CancellationSignal { private volatile boolean cancelled; public boolean isCanceled(){return cancelled;} public void cancel(){cancelled=true;} }
