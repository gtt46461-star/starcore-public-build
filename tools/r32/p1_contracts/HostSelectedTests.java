import java.lang.reflect.*;
import java.util.*;
/** Execute selected original @Test methods with host assertions; not a JUnit platform claim. */
public final class HostSelectedTests {
    public static void main(String[] args) throws Exception {
        int pass=0, fail=0;
        for(String selector:args){
            String[] parts=selector.split("#",2);Class<?> type=Class.forName(parts[0]);
            int found=0;
            Method[] methods=type.getMethods();Arrays.sort(methods,Comparator.comparing(Method::getName));
            for(Method method:methods){
                org.junit.Test annotation=method.getAnnotation(org.junit.Test.class);
                if(annotation==null||parts.length==2&&!parts[1].equals("*")&&!method.getName().equals(parts[1]))continue;
                found++;
                Object instance=type.getDeclaredConstructor().newInstance();
                try{
                    for(Method setup:methods)if(setup.getAnnotation(org.junit.Before.class)!=null)setup.invoke(instance);
                    method.invoke(instance);
                    if(annotation.expected()!=org.junit.Test.None.class)throw new AssertionError("missing expected exception");
                    pass++;System.out.println("PASS "+type.getName()+"#"+method.getName());
                }catch(InvocationTargetException error){
                    Throwable cause=error.getCause();
                    if(annotation.expected().isInstance(cause)){pass++;System.out.println("PASS_EXPECTED "+type.getName()+"#"+method.getName());}
                    else{fail++;System.out.println("FAIL "+type.getName()+"#"+method.getName()+" "+cause);cause.printStackTrace(System.out);}
                }finally{for(Method teardown:methods)if(teardown.getAnnotation(org.junit.After.class)!=null)teardown.invoke(instance);}
            }
            if(found==0){fail++;System.out.println("FAIL MISSING_SELECTOR "+selector);}
        }
        System.out.println("HOST_ORIGINAL_SELECTED_TESTS pass="+pass+" fail="+fail+" platform=ASSERTION_API_ADAPTER_NOT_JUNIT_OR_ANDROID");
        if(fail>0||pass==0)System.exit(1);
    }
}
