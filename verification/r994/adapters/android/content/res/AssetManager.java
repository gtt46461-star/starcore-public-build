package android.content.res;
import java.io.*;import java.util.zip.*;
public class AssetManager {private final File apk;public AssetManager(File apk){this.apk=apk;}public InputStream open(String name)throws IOException{final ZipFile z=new ZipFile(apk);ZipEntry e=z.getEntry("assets/"+name);if(e==null){z.close();throw new FileNotFoundException(name);}return new FilterInputStream(z.getInputStream(e)){public void close()throws IOException{try{super.close();}finally{z.close();}}};}}
