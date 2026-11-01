import com.sun.source.tree.*;
import com.sun.source.util.*;
import javax.tools.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
public final class SourceIndex {
 static String j(String s){StringBuilder b=new StringBuilder("\"");for(char c:s.toCharArray()){if(c=='"'||c=='\\')b.append('\\').append(c);else if(c=='\n')b.append("\\n");else if(c=='\r')b.append("\\r");else if(c=='\t')b.append("\\t");else if(c<32)b.append(String.format("\\u%04x",(int)c));else b.append(c);}return b.append('"').toString();}
 public static void main(String[] a)throws Exception{
  Path root=Paths.get(a[0]).toAbsolutePath();Path out=Paths.get(a[1]);Files.createDirectories(out);
  JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();if(compiler==null)throw new IllegalStateException("JDK_COMPILER_MISSING");
  List<File> files=new ArrayList<>();try(java.util.stream.Stream<Path>s=Files.walk(root.resolve("app/src/main/java"))){s.filter(p->p.toString().endsWith(".java")).sorted().forEach(p->files.add(p.toFile()));}
  DiagnosticCollector<JavaFileObject> diag=new DiagnosticCollector<>();StandardJavaFileManager fm=compiler.getStandardFileManager(diag,null,java.nio.charset.StandardCharsets.UTF_8);
  JavacTask task=(JavacTask)compiler.getTask(null,fm,diag,Arrays.asList("-proc:none","--release","17"),null,fm.getJavaFileObjectsFromFiles(files));
  Trees trees=Trees.instance(task);SourcePositions pos=trees.getSourcePositions();
  final long[] counters={0,0,0};
  try(PrintWriter mw=new PrintWriter(out.resolve("methods.jsonl").toFile(),"UTF-8");PrintWriter tw=new PrintWriter(out.resolve("transaction_sites.jsonl").toFile(),"UTF-8")){
   for(CompilationUnitTree unit:task.parse()){
    counters[0]++;String path=root.relativize(Paths.get(unit.getSourceFile().toUri())).toString();
    new TreePathScanner<Void,Void>(){
     Deque<String> types=new ArrayDeque<>();
     public Void visitClass(ClassTree t,Void v){types.addLast(t.getSimpleName().toString());super.visitClass(t,v);types.removeLast();return null;}
     public Void visitMethod(MethodTree t,Void v){
      long start=pos.getStartPosition(unit,t),end=pos.getEndPosition(unit,t);List<String> calls=new ArrayList<>();List<String> flags=new ArrayList<>();
      if(t.getModifiers().getFlags().contains(javax.lang.model.element.Modifier.SYNCHRONIZED))flags.add("synchronized_method");
      new TreeScanner<Void,Void>(){
       public Void visitMethodInvocation(MethodInvocationTree n,Void q){String name=n.getMethodSelect().toString();long line=unit.getLineMap().getLineNumber(pos.getStartPosition(unit,n));calls.add("{\"call\":"+j(name)+",\"line\":"+line+"}");
        if(name.matches(".*(?:runWrite|runAtomicWriteOrThrowF01|runTransaction|beginTransaction|beginTransactionNonExclusive|executeBatch).*")){
         counters[2]++;String body=n.toString();List<String> risks=new ArrayList<>();for(String term:new String[]{"Thread.sleep",".join(",".get(",".encode(",".compress(",".predict(",".train(","rawQuery(","ensureSchema(",".prune(",".foldDecisions("})if(body.contains(term))risks.add(j(term));
         tw.println("{\"path\":"+j(path)+",\"owner\":"+j(String.join("$",types))+",\"method\":"+j(t.getName().toString())+",\"line\":"+line+",\"call\":"+j(name)+",\"reviewSignals\":["+String.join(",",risks)+"],\"signalIsDefect\":false}");
        }return super.visitMethodInvocation(n,q);}
      }.scan(t.getBody(),null);
      counters[1]++;mw.println("{\"path\":"+j(path)+",\"type\":"+j(String.join("$",types))+",\"method\":"+j(t.getName().toString())+",\"signature\":"+j(t.getParameters().toString())+",\"startLine\":"+unit.getLineMap().getLineNumber(start)+",\"endLine\":"+(end<0?-1:unit.getLineMap().getLineNumber(end))+",\"calls\":["+String.join(",",calls)+"],\"binding\":\"SYNTAX_ONLY_NOT_TYPE_RESOLVED\"}");
      return super.visitMethod(t,v);
     }
    }.scan(unit,null);
   }
  }
  try(PrintWriter d=new PrintWriter(out.resolve("parse_diagnostics.jsonl").toFile(),"UTF-8")){for(Diagnostic<?> x:diag.getDiagnostics())d.println("{\"kind\":"+j(x.getKind().toString())+",\"line\":"+x.getLineNumber()+",\"message\":"+j(x.getMessage(Locale.ROOT))+"}");}
  System.out.println("{\"javaFilesParsed\":"+counters[0]+",\"methodsIndexed\":"+counters[1]+",\"transactionSites\":"+counters[2]+",\"parseErrors\":"+diag.getDiagnostics().stream().filter(d->d.getKind()==Diagnostic.Kind.ERROR).count()+",\"scope\":\"PARSE_ONLY_NOT_ANDROID_COMPILE_OR_FULL_SEMANTIC_AUDIT\"}");fm.close();
 }
}
