import com.sun.source.util.JavacTask;
import java.nio.file.*;
import java.util.*;
import javax.tools.*;

/** Syntax-only validation: deliberately does not claim Android symbol/type compilation. */
public final class ParseChangedJava {
    public static void main(String[] args) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler required");
        List<java.io.File> sources = new ArrayList<>();
        try (java.util.stream.Stream<Path> paths = Files.walk(Paths.get(args[0]))) {
            paths.filter(p -> p.toString().endsWith(".java")).sorted()
                    .forEach(p -> sources.add(p.toFile()));
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, null)) {
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    Arrays.asList("-proc:none"), null, manager.getJavaFileObjectsFromFiles(sources));
            task.parse();
        }
        for (Diagnostic<?> d : diagnostics.getDiagnostics())
            if (d.getKind() == Diagnostic.Kind.ERROR) throw new AssertionError(d.toString());
        System.out.println("PASS: " + sources.size() + " changed Java files parsed; Android type compilation NOT_RUN");
    }
}
