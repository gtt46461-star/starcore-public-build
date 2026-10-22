package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
public final class CertificateMainlineProbeR982 {
 public static void main(String[] a)throws Exception{SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
  DbStore store=HostProductR974.store(db);long at=System.currentTimeMillis();try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(store,"202609230361",at)){
   String id,structure;try(Cursor c=db.rawQuery("SELECT candidate_id,structural_hash FROM research_certificate_r929 WHERE state='ACTIVE'",null)){if(!c.moveToFirst())throw new AssertionError("No fixture certificate");id=c.getString(0);structure=c.getString(1);}
   ResearchPlatformStoreR929.Authorization auth=ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,id,structure,"202609230361",.05);
   System.out.println("AUTHORIZE authorized="+auth.authorized+" reason="+auth.reason+" weight="+auth.grantedWeight);
   if(!auth.authorized)throw new AssertionError("Complete identity fixture not authorized: "+auth.reason);
   if(!UnifiedResearchChoiceR932.freeze(store,"202609230361"))throw new AssertionError("Choice freeze unavailable");
   UnifiedResearchChoiceR932.Evidence selected=UnifiedResearchChoiceR932.selected(store,"202609230361",CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
   System.out.println("MAINLINE_SELECTED="+(selected==null?"NONE":selected.id));if(selected==null||!id.equals(selected.id))throw new AssertionError("Published qualified certificate not consumed");
  }
 }finally{db.shutdown();}}
}
