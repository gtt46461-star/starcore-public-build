package android.system;
/** SDK-compatible typed host failure injection; not an actual full Android volume. */
public final class ErrnoException extends Exception {
 public final int errno;
 public ErrnoException(String function,int errno){super(function+" failed: "+(errno==28?"ENOSPC (No space left on device)":"EIO (I/O error)"));this.errno=errno;}
 public ErrnoException(String function,int errno,Throwable cause){this(function,errno);initCause(cause);}
}
