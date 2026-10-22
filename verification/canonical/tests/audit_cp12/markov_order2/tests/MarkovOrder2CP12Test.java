import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;import java.nio.charset.StandardCharsets;import java.io.*;import java.util.*;
public final class MarkovOrder2CP12Test {
    static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("check"+checks);}
    static void fail(Runnable run,String expected){checks++;try{run.run();}catch(RuntimeException e){if(expected.equals(e.getMessage()))return;throw new AssertionError(e);}throw new AssertionError("accepted "+expected);}
    static AtomEvaluatorCP4.Spec spec(int w,int d,double alpha){return new AtomEvaluatorCP4.Spec("MARKOV_SCORE",w,0,d,0,0,0,0,0,alpha,"ALL",2,8);}
    static int[] batch(int[] input,int end,int w){int[] out=new int[1000];for(int t=end-w+1;t<=end;t++)out[(input[t-2]*10+input[t-1])*10+input[t]]++;return out;}
    public static void main(String[] args)throws Exception {
        MemoryBudgetCP4 budget=new MemoryBudgetCP4(4L<<20);
        try(AtomWindowCP4 data=new AtomWindowCP4(budget)) {
            for(int i=0;i<51;i++)data.append(i,0);
            AtomEvaluatorCP4.Result missing=AtomEvaluatorCP4.evaluate(spec(50,0,.5),data,null,51);ok(!missing.present&&missing.reason.equals("INSUFFICIENT_HISTORY"));
            data.append(51,0);double sum=0;
            for(int d=0;d<10;d++){AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec(50,d,.5),data,null,52);ok(r.present);ok(Math.abs(r.value-(d==0?50.5:.5)/55)<1e-15);sum+=r.value;}
            ok(Math.abs(sum-1)<1e-15);ok(Math.abs(AtomEvaluatorCP4.evaluate(spec(50,0,1),data,null,52).value-51.0/60)<1e-15);
            fail(()->AtomEvaluatorCP4.evaluate(spec(50,0,.5),data,null,51),"PIT_INVALID");
            data.append(52,9);fail(()->AtomEvaluatorCP4.evaluate(spec(50,0,.5),data,null,52),"PIT_INVALID");
            ok(!AtomEvaluatorCP4.evaluate(spec(50,0,.5),data,null,53).present); // last context(0,9) has no successor evidence.
        }
        ok(budget.used()==0);
        try(AtomWindowCP4 sparse=new AtomWindowCP4(budget)){
            for(int i=0;i<52;i++)sparse.append(i,i%10);
            AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec(50,3,.5),sparse,null,52);
            ok(!r.present&&r.reason.equals("MARKOV_CONTEXT_SUPPORT"));
        }
        for(int support:new int[]{7,8})try(AtomWindowCP4 edge=new AtomWindowCP4(budget)){
            int[] a=new int[52];for(int j=0;j<support;j++){a[j*3]=1;a[j*3+1]=2;a[j*3+2]=3;}a[50]=1;a[51]=2;for(int i=0;i<a.length;i++)edge.append(i,a[i]);
            AtomEvaluatorCP4.Result r=AtomEvaluatorCP4.evaluate(spec(50,3,.5),edge,null,52);ok(r.present==(support==8));if(support==8)ok(Math.abs(r.value-8.5/13)<1e-15);else ok(r.reason.equals("MARKOV_CONTEXT_SUPPORT"));
        }
        int[] input=new int[5600];Random random=new Random(912367L);int[] windows={50,120,300,500};
        try(AtomWindowCP4 rolling=new AtomWindowCP4(budget)) {
            for(int w:windows)rolling.markov2(w);
            for(int end=0;end<input.length;end++){
                input[end]=(end%13<8)?0:random.nextInt(10);rolling.append(end,input[end]);
                for(int w:windows)if(end>=w+1){int[] expected=batch(input,end,w);ok(Arrays.equals(expected,rolling.markov2(w).counts()));
                    int context=(input[end-1]*10+input[end])*10,n=0;for(int d=0;d<10;d++)n+=expected[context+d];
                    AtomEvaluatorCP4.Result result=AtomEvaluatorCP4.evaluate(spec(w,2,.5),rolling,null,end+1);
                    ok(result.present==(n>=8));if(n>=8)ok(Double.doubleToLongBits(result.value)==Double.doubleToLongBits((expected[context+2]+.5)/(n+5.0)));
                }
            }
            ok(rolling.size()==5000); // verifies prefix eviction across the physical ring wrap.
            rolling.releaseUnused(new HashSet<>(Arrays.asList("M2:50")));ok(budget.used()==16384);
            ok(Arrays.equals(batch(input,input.length-1,50),rolling.markov2(50).counts()));
        }
        ok(budget.used()==0);
        try(AtomWindowCP4 cap=new AtomWindowCP4(budget)){
            cap.window(1,0);for(int n=1;n<256;n++)cap.markov2(n);
            fail(()->cap.markov2(256),"ATOM_STATE_BUDGET");
            fail(()->cap.markov2(4999),"MARKOV_WINDOW_BOUND");
        }
        ok(budget.used()==0);
        MemoryBudgetCP4 minimal=new MemoryBudgetCP4(8192);try(AtomWindowCP4 low=new AtomWindowCP4(minimal)){fail(()->low.markov2(50),"ATOM_MEMORY_BUDGET");ok(minimal.used()==8192);}ok(minimal.used()==0);
        int count=0,order2=0;Set<String> families=new HashSet<>(),hashes=new HashSet<>();String firstNew=null;
        try(BufferedReader reader=Files.newBufferedReader(Path.of(args[0]),StandardCharsets.UTF_8)){
            for(String line;(line=reader.readLine())!=null;){AtomSpecCP4 parsed=AtomSpecCP4.fromAssetLine(line);count++;ok(hashes.add(parsed.specHash));families.add(parsed.family);
                if(parsed.family.equals("MARKOV_SCORE")&&parsed.mathSpec.order==2){order2++;ok(parsed.mathSpec.minContext==8&&parsed.minimumHistory==parsed.mathSpec.w+2);if(firstNew==null)firstNew=line;}
            }
        }
        ok(count==25350&&order2==800&&families.size()==28);
        final String row=firstNew;fail(()->AtomSpecCP4.fromAssetLine(row.replace("\"minContext\":8","\"minContext\":7")),"ATOM_SPEC_HASH_MISMATCH");
        fail(()->new AtomEvaluatorCP4.Spec("MARKOV_SCORE",50,0,0,0,0,0,0,0,.5,"ALL",2,7),"MARKOV_ORDER_OR_CONTEXT");
        fail(()->new AtomEvaluatorCP4.Spec("MARKOV_SCORE",50,0,0,0,0,0,0,0,.5,"ALL",3,0),"MARKOV_ORDER_OR_CONTEXT");
        System.out.println("{\"status\":\"PASS\",\"assertions\":"+checks+",\"catalog_rows\":"+count+",\"order2_rows\":"+order2+",\"families\":"+families.size()+",\"scope\":\"Actual AFTER evaluator/window/spec: sparse context MASK, known Dirichlet counts, strict cutoff/no label t, every rolling step vs independent100x10 batch, physical5000 ring wrap/prefix eviction, shared cache/M2 release/memory/state caps and every25350 actual asset row strict parser/hash identity. No fabricated device timing.\"}");
    }
}
