package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Actual immutable registry and p-bound; simulations explicitly separate null assumptions. */
public final class MultiplicityHarnessR932 {
    static void check(boolean b,String s){if(!b)throw new AssertionError(s);System.out.println(s+"=PASS");}
    static double number(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getDouble(0):0;}}
    static String hash(String s){return V51ChecksumEngine.sha256(s);}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            ResearchMultiplicityR932.schema(db);db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,dirty INTEGER)");db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES('202609100120',0)");
            double[][] alpha=new double[32][5];int candidate=0;
            db.beginTransaction();try{
                for(int family=0;family<4;family++)for(int member=0;member<8;member++){
                    String key="SYNTHETIC_"+candidate;ResearchMultiplicityR932.registerCandidate(db,key,"F"+family,hash(key),"202609100001");
                    for(int look=1;look<=5;look++)alpha[candidate][look-1]=ResearchMultiplicityR932.registerLook(db,key,key+"_"+look,look,"202609100001","202609100120");candidate++;
                }
                double before=number(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS);
                ResearchMultiplicityR932.registerCandidate(db,"RENAMED_ALIAS","F0",hash("SYNTHETIC_0"),"202609100001");
                check(number(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==before,"COPY_RENAME_DOES_NOT_RESET_HYPOTHESIS_ALPHA");
                boolean rejected=false;try{ResearchMultiplicityR932.registerCandidate(db,"SPLIT_FAMILY","NEW_FAMILY",hash("SYNTHETIC_0"),"202609100001");}catch(IllegalStateException e){rejected=true;}check(rejected,"SPLITTING_EXISTING_STRUCTURE_INTO_NEW_FAMILY_REJECTED");
                rejected=false;try{ResearchMultiplicityR932.registerLook(db,"SYNTHETIC_0","RENAMED_LOOK",1,"202609100001","202609100120");}catch(IllegalStateException e){rejected=true;}check(rejected,"RENAMED_LOOK_CANNOT_REFUND_OLD_WINDOW");
                for(int i=4;i<64;i++)ResearchMultiplicityR932.registerCandidate(db,"APPENDED_"+i,"F"+i,hash("APPENDED_"+i),"202609100001");
                check(number(db,"SELECT SUM(alpha) FROM "+ResearchMultiplicityR932.FAMILY)<=.05&&number(db,"SELECT SUM(alpha) FROM "+ResearchMultiplicityR932.HYPOTHESIS)<=.05,"CONTINUOUS_FAMILY_AND_HYPOTHESIS_ADDITION_STAYS_WITHIN_GLOBAL_ALPHA");
                check(number(db,"SELECT SUM(alpha) FROM "+ResearchMultiplicityR932.LOOK)<=.05,"ALL_FIVE_LOOK_ALLOCATIONS_STAY_WITHIN_REGISTERED_BUDGET");
                ResearchMultiplicityR932.close(db,"SYNTHETIC_0_1","202609100120","FAIL_EFFECT",hash("FAILED"),1d);
                ResearchMultiplicityR932.close(db,"SYNTHETIC_0_1","202609100120","FAIL_EFFECT",hash("FAILED"),1d);
                rejected=false;try{ResearchMultiplicityR932.close(db,"SYNTHETIC_0_1","202609100120","PASS",hash("RETRY_WIN"),.00000001);}catch(IllegalStateException e){rejected=true;}check(rejected,"REPEATED_LOOK_CANNOT_REPLACE_FAILURE_WITH_LATER_WIN");
                check(number(db,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==92,"FAILED_OR_ALIASED_CANDIDATES_REMAIN_IN_DENOMINATOR");
                db.setTransactionSuccessful();
            }finally{db.endTransaction();}
            JSONArray simulations=new JSONArray();int[] sizes={120,240,240,240,360};int repetitions=2000;
            for(int mode=0;mode<4;mode++){
                Random random=new Random(9320600+mode);int falsePositive=0;
                for(int rep=0;rep<repetitions;rep++){
                    boolean rejected=false;int persistent=random.nextBoolean()?1:-1,history=1;
                    for(int look=0;look<5;look++){
                        int[] sums=new int[32];
                        for(int t=0;t<sizes[look];t++){
                            int shared=random.nextBoolean()?1:-1;
                            for(int c=0;c<32;c++){
                                int delta=mode==0?(random.nextBoolean()?1:-1):mode==1?shared:mode==2?shared*history:persistent;sums[c]+=delta;
                            }
                            history=shared;
                        }
                        for(int c=0;c<32;c++)rejected|=ResearchMultiplicityR932.pairedP(sizes[look],sums[c])<=alpha[c][look];
                    }
                    if(rejected)falsePositive++;
                }
                double rate=falsePositive/(double)repetitions,z=1.959963984540054,upper=(rate+z*z/(2*repetitions)+z*Math.sqrt(rate*(1-rate)/repetitions+z*z/(4d*repetitions*repetitions)))/(1+z*z/repetitions);
                JSONObject result=new JSONObject();result.put("scenario",new String[]{"INDEPENDENT_NOISE","PERFECTLY_CORRELATED_CANDIDATES","PAST_DEPENDENT_CONDITIONAL_NULL","INVALID_PERSISTENT_SIGN_NEGATIVE_CONTROL"}[mode]);result.put("repetitions",repetitions);result.put("any_false_positive",falsePositive);result.put("empirical_fwer",rate);result.put("binomial_monte_carlo_95_upper",upper);result.put("conditional_null_satisfied",mode<3);simulations.put(result);
                if(mode<3)check(upper<.05,"MONTE_CARLO_GLOBAL_BOUND_SCENARIO_"+mode);else check(rate>.2,"INVALID_DEPENDENCE_NEGATIVE_CONTROL_HAS_NO_FALSE_GUARANTEE");
            }
            JSONObject report=new JSONObject();report.put("protocol","FIXED_FIVE_LOOK_HIERARCHICAL_FWER_NOT_BH_FDR");report.put("global_alpha",.05);report.put("simulations",simulations);report.put("scope","SYNTHETIC_PAIRED_NULLS_NO_REAL_EFFECT_CLAIM");report.put("assumption","E[D_t|past]<=0 and D_t in [-1,1]; arbitrary temporal dependence is not covered");System.out.println(report.toString());
            System.out.println("R932_MULTIPLICITY_PASS|scope=REAL_SQLITE_REGISTRY_AND_PRODUCTION_P_BOUND");
        }finally{db.shutdown();}
    }
}
