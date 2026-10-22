package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.*;import java.util.*;
/** Real SQLite source fairness and fixed bootstrap bounds; pure full-history parity uses real projection code. */
public final class MatrixRegistrationContractR75 {
 static int checks;static void ok(boolean b,String m){checks++;if(!b)throw new AssertionError(m);}static String H(String s){return MatrixProjectionCP12.hash(s);}
 static String issue(int n){String x="202609230001";for(int i=1;i<n;i++)x=TargetIssueResolver.next(x).targetIssue;return x;}
 static void tx(SQLiteDatabase db,Runnable body){db.beginTransaction();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
 static void draw(SQLiteDatabase db,int n){db.execSQL("INSERT INTO draws(issue,open_time,fetch_time,dirty,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10) VALUES(?,?,?,0,1,2,3,4,5,6,7,8,9,10)",new Object[]{issue(n),n*1000L,n*1000L+1});}
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){DbStore store=HostProductR974.store(db);
  if(args[2].equals("fair")){
   tx(db,()->{draw(db,1);draw(db,2);for(int i=0;i<128;i++)MatrixSnapshotBusR952.registerSpecCP12(db,new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FrequencyMatrix,"position:1",300,H("fair-source-"+i)));});
   Set<String> seen=new HashSet<>();for(int i=0;i<128;i++){MatrixSourceRouterCP12.PreparedInput p=MatrixSourceRouterCP12.prepareNext(store,System.nanoTime()+1000000000L);ok(p!=null,"source available "+i);ok(store.getState("cp12_matrix_source_cursor","").equals(p.spec.matrixId),"cursor equals last actual attempt");seen.add(p.spec.matrixId);tx(db,()->MatrixSourceRouterCP12.commitPrepared(db,p,1));}ok(seen.size()==128,"all 128 sustained sources get turn");
   String before=store.getState("cp12_matrix_source_cursor","");ok(MatrixSourceRouterCP12.prepareNext(store,System.nanoTime())==null,"zero budget yields");ok(before.equals(store.getState("cp12_matrix_source_cursor","")),"zero budget preserves cursor");
   for(int i=0;i<128;i++){MatrixSourceRouterCP12.PreparedInput p=MatrixSourceRouterCP12.prepareNext(store,System.nanoTime()+1000000000L);ok(p!=null,"second backlog source available");tx(db,()->MatrixSourceRouterCP12.commitPrepared(db,p,1));}
   Set<String> positions=new HashSet<>();for(int i=0;i<3;i++){ok(MatrixSourceRouterCP12.prepareNext(store,System.nanoTime()+1000000000L)==null,"empty source page");positions.add(store.getState("cp12_matrix_source_cursor",""));}ok(positions.size()==2,"empty pages move around 128 specs");
   store.setState("r75_matrix_registration_phase","2");String cursor=store.getState("r75_matrix_registration_2","");MemoryBudgetCP4 budget=RuntimeMemoryGuardR811.algorithmModelBudget();try(MemoryBudgetCP4.Lease full=budget.acquire(budget.limit-budget.used())){ok(!MatrixRegistrationR75.drain(store,System.nanoTime()+1000000000L),"memory pressure yields");}ok(store.getState("r75_matrix_registration_phase","").equals("2")&&store.getState("r75_matrix_registration_2","").equals(cursor),"transient pressure does not consume cursor");
  }else{
   tx(db,()->{for(int n=1;n<=650;n++)draw(db,n);});
   for(int i=0;i<60;i++)MatrixRegistrationR75.drain(store,System.nanoTime()+1000000000L);
   MatrixProjectionCP12.Spec frequency=MatrixRegistrationR75.spec(MatrixProjectionCP12.Type.FrequencyMatrix,"position:1"),drift=MatrixRegistrationR75.spec(MatrixProjectionCP12.Type.DriftMatrix,"position:1"),transition=MatrixRegistrationR75.spec(MatrixProjectionCP12.Type.TransitionMatrix,"position:1|lag:1");
   ok(MatrixSourceRouterCP12.prepareHistoryInputCP12(db,frequency).issue.equals(issue(351)),"frequency real tail W");ok(MatrixSourceRouterCP12.prepareHistoryInputCP12(db,drift).issue.equals(issue(51)),"drift real tail 2W");ok(MatrixSourceRouterCP12.prepareHistoryInputCP12(db,transition).issue.equals(issue(351)),"transition retains real preceding lag");
   tx(db,()->{for(int n=651;n<=700;n++)draw(db,n);});for(int i=0;i<15;i++)MatrixRegistrationR75.drain(store,System.nanoTime()+1000000000L);ok(MatrixSourceRouterCP12.prepareHistoryInputCP12(db,frequency).issue.equals(issue(351)),"later draws cannot move startup lower bound");
   MatrixProjectionCP12.Spec old=new MatrixProjectionCP12.Spec(MatrixProjectionCP12.Type.FrequencyMatrix,"position:1",300,H("old spec"));ok(MatrixSourceRouterCP12.prepareHistoryInputCP12(db,old).issue.equals(issue(1)),"old manifest behavior retained");
   for(MatrixProjectionCP12.Type type:new MatrixProjectionCP12.Type[]{MatrixProjectionCP12.Type.FrequencyMatrix,MatrixProjectionCP12.Type.OmissionMatrix,MatrixProjectionCP12.Type.TransitionMatrix,MatrixProjectionCP12.Type.ConditionalTransitionMatrix,MatrixProjectionCP12.Type.DriftMatrix}){
    String part="position:1"+(type==MatrixProjectionCP12.Type.TransitionMatrix?"|lag:1":type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix?"|lag:1|regime:0|regimePolicy:"+AtomRegimePolicyCP12.POLICY_HASH:"");MatrixProjectionCP12.Spec spec=MatrixRegistrationR75.spec(type,part);
    MatrixProjectionCP12.State full=MatrixProjectionCP12.empty(spec),tail=MatrixProjectionCP12.empty(spec);List<MatrixProjectionCP12.Sample> rows=new ArrayList<>();Random random=new Random(314159);long ordinal=1;
    for(int i=0;i<2200;i++){ordinal+=i%37==0?3:1;int a=random.nextInt(10),b=random.nextInt(10);if(type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix&&i%7!=0)continue;rows.add(new MatrixProjectionCP12.Sample(ordinal,0,a,b,type==MatrixProjectionCP12.Type.ConditionalTransitionMatrix?0:a,0,0,0,0,0));}
    int retain=type==MatrixProjectionCP12.Type.DriftMatrix?600:300;for(MatrixProjectionCP12.Sample r:rows)MatrixProjectionCP12.apply(full,r.ordinal,r,null);for(int i=Math.max(0,rows.size()-retain);i<rows.size();i++){MatrixProjectionCP12.Sample r=rows.get(i);MatrixProjectionCP12.apply(tail,r.ordinal,r,null);}
    ok(Arrays.equals(MatrixProjectionCP12.encode(full),MatrixProjectionCP12.encode(tail)),"full history and finite source tail encode identical "+type);Map<String,double[]> x=MatrixProjectionCP12.metrics(full),y=MatrixProjectionCP12.metrics(tail);ok(x.keySet().equals(y.keySet()),"consumer metric keys "+type);for(String k:x.keySet())ok(Arrays.equals(x.get(k),y.get(k)),"consumer value "+type+":"+k);
   }
  }
  System.out.println("R75_MATRIX_CONTRACT_PASS mode="+args[2]+" checks="+checks);
 }}
}
