package android.app;
/** Host application transport: file/assets context only, not Android lifecycle emulation. */
public class Application extends android.content.Context {
 public Application(java.io.File files,java.io.File assets){super(files,assets);}
}
