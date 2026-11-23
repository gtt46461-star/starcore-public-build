import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.util.JavacTask;

/** Syntax parsing only; does not resolve Android or production dependency types. */
public final class JavaParseR34 {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler=ToolProvider.getSystemJavaCompiler();
        if(compiler==null)throw new IllegalStateException("HOST_COMPILER_UNAVAILABLE");
        DiagnosticCollector<JavaFileObject> diagnostics=new DiagnosticCollector<>();
        try(StandardJavaFileManager files=compiler.getStandardFileManager(diagnostics,null,null)) {
            List<Path> paths=new ArrayList<>();for(String arg:args)paths.add(Path.of(arg));
            JavacTask task=(JavacTask)compiler.getTask(null,files,diagnostics,
                    List.of("-proc:none","-source","17","-Xlint:-options"),null,files.getJavaFileObjectsFromPaths(paths));
            int count=0;for(CompilationUnitTree tree:task.parse())count++;
            boolean failed=false;for(Diagnostic<? extends JavaFileObject> d:diagnostics.getDiagnostics())if(d.getKind()==Diagnostic.Kind.ERROR){failed=true;System.err.println(d);}
            if(failed||count!=paths.size())throw new AssertionError("R34_SOURCE_PARSE_FAILED");
            System.out.println("R34_JAVA_PARSE_PASS files="+count+" scope=SYNTAX_ONLY_NOT_ANDROID_TYPECHECK_OR_EXECUTION");
        }
    }
}
