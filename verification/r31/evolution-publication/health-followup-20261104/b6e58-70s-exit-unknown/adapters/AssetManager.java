package android.content.res;
/** Actual source asset reads; missing generated APK assets remain missing, never fabricated. */
public class AssetManager {
 public java.io.InputStream open(String name)throws java.io.IOException{java.nio.file.Path root=java.nio.file.Path.of("/workspace/r31-evolution-publication-de9/app/src/main/assets").toRealPath();java.nio.file.Path file=root.resolve(name).normalize();if(!file.startsWith(root))throw new java.io.IOException("HOST_ASSET_PATH");return java.nio.file.Files.newInputStream(file);}
}
