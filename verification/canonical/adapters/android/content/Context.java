package android.content;
/** Narrow host transport only; product DbStore is compiled against the real API jar. */
public class Context {private final android.content.res.AssetManager assets;public Context(){this(null);}public Context(android.content.res.AssetManager a){assets=a;}public android.content.res.AssetManager getAssets(){return assets;}public Context getApplicationContext(){return this;}public java.io.File getFilesDir(){return new java.io.File(System.getProperty("java.io.tmpdir"),"cp12-host-assets");}}
