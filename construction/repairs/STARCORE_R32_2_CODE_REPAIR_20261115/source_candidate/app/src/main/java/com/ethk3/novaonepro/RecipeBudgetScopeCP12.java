package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.MemoryBudgetCP4;
/** Whole frozen recipe allocation lifetime: before PIT/root/decode through durablecommit or replay end. */
public final class RecipeBudgetScopeCP12 implements AutoCloseable {
 public static final long MODEL_BYTES=5L<<20,ATOM_BYTES=4L<<20;
 private static final ThreadLocal<RecipeBudgetScopeCP12> CURRENT=new ThreadLocal<>();
 private final Thread thread=Thread.currentThread();private final MemoryBudgetCP4.Lease model,atom;
 private boolean closed;private int recipes;
 private RecipeBudgetScopeCP12(MemoryBudgetCP4 modelBudget,MemoryBudgetCP4 atomBudget){
  if(CURRENT.get()!=null)throw new IllegalStateException("RECIPE_SCOPE_NESTED");
  MemoryBudgetCP4.Lease a=modelBudget.acquire(MODEL_BYTES),b=null;
  try{b=atomBudget.acquire(ATOM_BYTES);model=a;atom=b;CURRENT.set(this);}catch(RuntimeException|Error failure){if(b!=null)b.close();a.close();throw failure;}
 }
 public static RecipeBudgetScopeCP12 open(){return open(RuntimeMemoryGuardR811.algorithmModelBudget(),RuntimeMemoryGuardR811.algorithmAtomMatrixBudget());}
 public static RecipeBudgetScopeCP12 open(MemoryBudgetCP4 model,MemoryBudgetCP4 atom){if(model==null||atom==null)throw new IllegalArgumentException("RECIPE_BUDGET");return new RecipeBudgetScopeCP12(model,atom);}
 public static void requireActive(){RecipeBudgetScopeCP12 scope=CURRENT.get();if(scope==null||scope.closed||scope.thread!=Thread.currentThread())throw new IllegalStateException("RECIPE_ALLOCATION_WITHOUT_LEASE");}
 public static void beginRecipe(){requireActive();RecipeBudgetScopeCP12 scope=CURRENT.get();if(scope.recipes++!=0)throw new IllegalStateException("RECIPE_TWO_CONSUMERS");}
 public void close(){if(Thread.currentThread()!=thread)throw new IllegalStateException("RECIPE_SCOPE_THREAD");if(closed)return;closed=true;CURRENT.remove();try{atom.close();}finally{model.close();}}
}
