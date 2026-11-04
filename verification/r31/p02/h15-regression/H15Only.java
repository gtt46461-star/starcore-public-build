import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
public final class H15Only {
    public static void main(String[] args)throws Exception{
        Result result=new JUnitCore().run(Request.method(Class.forName("com.ethk3.novaonepro.strategyv2.R27QualityTest"),"h15MainlineRedLines"));
        result.getFailures().forEach(f->System.out.println(f.getTrace()));
        System.out.println("R31_H15_ONLY run="+result.getRunCount()+" failed="+result.getFailureCount()+" ignored="+result.getIgnoreCount());
        if(!result.wasSuccessful()||result.getRunCount()!=1)System.exit(1);
    }
}
