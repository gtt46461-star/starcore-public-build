package android.content;
/** Only the file directory is transported by this host test. */
public final class Context {
    private final java.io.File files;
    public Context(java.io.File files){this.files=files;}
    public java.io.File getFilesDir(){return files;}
}
