import org.objectweb.asm.*;
import java.io.*;import java.util.*;import java.util.zip.*;
/** mode=stub: every android.* method body throws RuntimeException("Stub!") (SDK android.jar semantics).
 *  mode=mockable: every method returns the default value (AGP unitTests.returnDefaultValues=true semantics).
 *  Only android/, com/android/, dalvik/, org/apache/http are kept; java.* etc. are dropped. */
public class JarTransform {
  static Map<String,String> SUPERS;static Map<String,List<String>> CTORS;
  static String pickCtor(String owner){
    List<String> c=CTORS.get(owner);
    if(c==null){try{Class<?> k=Class.forName(owner.replace('/','.'),false,JarTransform.class.getClassLoader());c=new ArrayList<>();for(java.lang.reflect.Constructor<?> x:k.getDeclaredConstructors())if(!java.lang.reflect.Modifier.isPrivate(x.getModifiers()))c.add(Type.getConstructorDescriptor(x));}catch(Throwable t){return "()V";}}
    String best=null;for(String d:c){if(d.equals("()V"))return d;if(best==null||Type.getArgumentTypes(d).length<Type.getArgumentTypes(best).length)best=d;}return best==null?"()V":best;}
  static void pushDefault(MethodVisitor mv,Type t){switch(t.getSort()){case Type.LONG:mv.visitInsn(Opcodes.LCONST_0);break;case Type.FLOAT:mv.visitInsn(Opcodes.FCONST_0);break;case Type.DOUBLE:mv.visitInsn(Opcodes.DCONST_0);break;case Type.OBJECT:case Type.ARRAY:mv.visitInsn(Opcodes.ACONST_NULL);break;default:mv.visitInsn(Opcodes.ICONST_0);}}
  public static void main(String[] a) throws Exception {
    String mode=a[2];
    final Map<String,String> supers=new HashMap<>();final Map<String,List<String>> ctors=new HashMap<>();
    try(ZipInputStream in=new ZipInputStream(new BufferedInputStream(new FileInputStream(a[0])))){ZipEntry e;
      while((e=in.getNextEntry())!=null){if(!e.getName().endsWith(".class"))continue;ClassReader cr=new ClassReader(in.readAllBytes());
        final String cn=cr.getClassName();supers.put(cn,cr.getSuperName());
        cr.accept(new ClassVisitor(Opcodes.ASM9){public MethodVisitor visitMethod(int acc,String name,String desc,String sig,String[] ex){if("<init>".equals(name)&&(acc&Opcodes.ACC_PRIVATE)==0)ctors.computeIfAbsent(cn,k->new ArrayList<>()).add(desc);return null;}},ClassReader.SKIP_CODE);}}
    SUPERS=supers;CTORS=ctors;
    try(ZipInputStream in=new ZipInputStream(new BufferedInputStream(new FileInputStream(a[0])));ZipOutputStream out=new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(a[1])))){
      ZipEntry e;Set<String> seen=new HashSet<>();
      while((e=in.getNextEntry())!=null){
        String n=e.getName();if(e.isDirectory()||!n.endsWith(".class")||!seen.add(n))continue;
        if(!(n.startsWith("android/")||n.startsWith("com/android/")||n.startsWith("dalvik/")||n.startsWith("org/apache/http/")||n.startsWith("org/json/")||n.startsWith("org/xmlpull/")))continue;
        byte[] b=in.readAllBytes();ClassReader cr=new ClassReader(b);ClassWriter cw=new ClassWriter(0);
        cr.accept(new ClassVisitor(Opcodes.ASM9,cw){
          String owner;
          public MethodVisitor visitMethod(int acc,String name,String desc,String sig,String[] ex){
            MethodVisitor mv=super.visitMethod(acc&~Opcodes.ACC_NATIVE,name,desc,sig,ex);
            if((acc&Opcodes.ACC_ABSTRACT)!=0)return mv;
            if("<clinit>".equals(name)){mv.visitCode();mv.visitInsn(Opcodes.RETURN);mv.visitMaxs(0,0);mv.visitEnd();return null;}
            mv.visitCode();
            if(mode.equals("stub")){mv.visitTypeInsn(Opcodes.NEW,"java/lang/RuntimeException");mv.visitInsn(Opcodes.DUP);mv.visitLdcInsn("Stub!");mv.visitMethodInsn(Opcodes.INVOKESPECIAL,"java/lang/RuntimeException","<init>","(Ljava/lang/String;)V",false);mv.visitInsn(Opcodes.ATHROW);mv.visitMaxs(3,(Type.getArgumentsAndReturnSizes(desc)>>2)+1);}
            else{
              int locals=(Type.getArgumentsAndReturnSizes(desc)>>2)+1;
              if("<init>".equals(name)){ // call Object.<init> is invalid if super differs; call real super no-arg? Use Object-safe: skip, JVM requires super call -> call java/lang/Object only when direct super is Object.
                String sup=SUPERS.getOrDefault(owner,"java/lang/Object");if(sup==null)sup="java/lang/Object";String cd=sup.equals("java/lang/Object")?"()V":pickCtor(sup);
                mv.visitVarInsn(Opcodes.ALOAD,0);for(Type t:Type.getArgumentTypes(cd))pushDefault(mv,t);mv.visitMethodInsn(Opcodes.INVOKESPECIAL,sup,"<init>",cd,false);mv.visitInsn(Opcodes.RETURN);mv.visitMaxs(1+(Type.getArgumentsAndReturnSizes(cd)>>2),locals);}
              else{Type r=Type.getReturnType(desc);
                switch(r.getSort()){case Type.VOID:mv.visitInsn(Opcodes.RETURN);break;case Type.LONG:mv.visitInsn(Opcodes.LCONST_0);mv.visitInsn(Opcodes.LRETURN);break;case Type.FLOAT:mv.visitInsn(Opcodes.FCONST_0);mv.visitInsn(Opcodes.FRETURN);break;case Type.DOUBLE:mv.visitInsn(Opcodes.DCONST_0);mv.visitInsn(Opcodes.DRETURN);break;case Type.OBJECT:case Type.ARRAY:mv.visitInsn(Opcodes.ACONST_NULL);mv.visitInsn(Opcodes.ARETURN);break;default:mv.visitInsn(Opcodes.ICONST_0);mv.visitInsn(Opcodes.IRETURN);}
                mv.visitMaxs(2,locals);}
            }
            mv.visitEnd();return null;
          }
          public void visit(int v,int acc,String name,String sig,String sup,String[] itf){owner=name;super.visit(v,acc&~Opcodes.ACC_FINAL,name,sig,sup,itf);}
        },0);
        out.putNextEntry(new ZipEntry(n));out.write(cw.toByteArray());out.closeEntry();
      }
    }
  }
}
