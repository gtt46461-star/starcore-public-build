package android.content.res;
import java.io.*;
/** Host transport for exact extracted APK asset bytes, not simulated data. */
public class AssetManager {private final File root;public AssetManager(File root){this.root=root;}public InputStream open(String name)throws IOException{return new FileInputStream(new File(root,name));}public AssetFileDescriptor openFd(String name)throws IOException{File f=new File(root,name);return new AssetFileDescriptor(f,0,f.length());}}
