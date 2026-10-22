package com.ethk3.novaonepro;
import org.json.JSONObject;
public final class ParserMissingTimeR992 {
 public static void main(String[]args)throws Exception{
  JSONObject missing=new JSONObject("{\"issue\":\"202610040259\",\"numbers\":\"1,2,3,4,5,6,7,8,9,10\"}");
  if(ApiParser.parseDrawObject(missing,"PRIMARY",null)!=null)throw new AssertionError("missing time became fresh");
  missing.put("openTime",System.currentTimeMillis());Models.Draw current=ApiParser.parseDrawObject(missing,"PRIMARY",null);
  if(current==null||!current.isValid())throw new AssertionError("explicit timestamp lost");
  JSONObject duoqu=new JSONObject("{\"betNo\":\"202610040259\",\"openNumber\":\"1,2,3,4,5,6,7,8,9,10\"}");
  if(ApiParser.parseDrawObject(duoqu,"PRIMARY",null)!=null)throw new AssertionError("duoqu missing time became fresh");
  duoqu.put("openTime",System.currentTimeMillis()-3600000L);Models.Draw old=ApiParser.parseDrawObject(duoqu,"PRIMARY",null);
  if(old==null||ProviderFreshnessGateR3611.evaluate(old,"202610040260",System.currentTimeMillis()).accepted)throw new AssertionError("old time admitted");
  System.out.println("PASS missing timestamps rejected, valid explicit timestamp parsed, stale explicit draw rejected");
 }
}
