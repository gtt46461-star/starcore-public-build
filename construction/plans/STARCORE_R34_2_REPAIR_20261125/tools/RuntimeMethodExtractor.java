import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;
import com.sun.source.tree.*;
import com.sun.source.util.*;

class RuntimeMethodExtractor {
  static String q(String s) {
    StringBuilder b=new StringBuilder("\"");
    for(char c:s.toCharArray()) { switch(c) {
      case '\\': b.append("\\\\"); break; case '"':b.append("\\\"");break;
      case '\n':b.append("\\n");break; case '\r':b.append("\\r");break;
      case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);
    }} return b.append('"').toString();
  }
  public static void main(String[] args) throws Exception {
    JavaCompiler c=ToolProvider.getSystemJavaCompiler();
    if(c==null)throw new IllegalStateException("No compiler available");
    DiagnosticCollector<JavaFileObject> diags=new DiagnosticCollector<>();
    StandardJavaFileManager fm=c.getStandardFileManager(diags,null,null);
    JavacTask task=(JavacTask)c.getTask(null,fm,diags,List.of("-proc:none"),null,fm.getJavaFileObjects(args));
    Iterable<? extends CompilationUnitTree> units=task.parse();
    Trees trees=Trees.instance(task);
    SourcePositions pos=trees.getSourcePositions();
    for(CompilationUnitTree u:units) {
      String text=u.getSourceFile().getCharContent(true).toString();
      String file=Paths.get(u.getSourceFile().toUri()).toString();
      new TreePathScanner<Void,Void>() {
        Deque<String> classes=new ArrayDeque<>();
        public Void visitClass(ClassTree tree,Void v) {
          String name=tree.getSimpleName().toString();
          if(name.isEmpty())name="<anonymous@"+u.getLineMap().getLineNumber(pos.getStartPosition(u,tree))+">";
          classes.addLast(name);super.visitClass(tree,v);classes.removeLast();return null;
        }
        public Void visitMethod(MethodTree m,Void v) {
          long start=pos.getStartPosition(u,m), end=pos.getEndPosition(u,m);
          long body=m.getBody()==null?end:pos.getStartPosition(u,m.getBody());
          if(start>=0&&end>=start) {
            String name=m.getName().toString();boolean ctor=m.getReturnType()==null;
            String sig=text.substring((int)start,(int)body).trim();
            String j="{\"file\":"+q(file)+",\"owner\":"+q(String.join(".",classes))+
              ",\"name\":"+q(name)+",\"kind\":"+q(ctor?"constructor":"method")+
              ",\"has_body\":"+(m.getBody()!=null)+
              ",\"start_line\":"+u.getLineMap().getLineNumber(start)+
              ",\"end_line\":"+u.getLineMap().getLineNumber(end-1)+
              ",\"signature\":"+q(sig)+"}";
            System.out.println(j);
          }
          return super.visitMethod(m,v);
        }
      }.scan(u,null);
    }
    for(Diagnostic<? extends JavaFileObject> d:diags.getDiagnostics()){
      System.err.println(d.getKind()+" "+d.getSource()+":"+d.getLineNumber()+" "+d.getMessage(null));
    }
    fm.close();
  }
}
