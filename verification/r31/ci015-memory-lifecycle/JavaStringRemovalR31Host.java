package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Verifies the actual H15 test helper; no JVM stack-size override and no reduced owner-hash assertions. */
public final class JavaStringRemovalR31Host {
 static int assertions;static void need(boolean value,String why){assertions++;if(!value)throw new AssertionError(why);}
 static String original(String value){return value.replaceAll("\"(?:[^\"\\\\]|\\\\.)*\"","\"\"");}
 public static void main(String[] args){
  List<String> samples=Arrays.asList("", "outside", "\"\"", "\"abc\"", "before\"\\\"DecisionGateEngine\\\"\"after", "\"slash\\\\x\"+stake", "\"line\ntext\"", "\"dangling\\", "\"\\\"\" trailing\"", "one\"a\"two\"b\"three");
  for(String s:samples)need(original(s).equals(R27QualityTest.stripJavaStringsR31(s)),"legacy short literal grammar unchanged");
  Random random=new Random(0x31015);String alphabet="abc\"\\. /\n\r\tStake";
  for(int n=0;n<5000;n++){StringBuilder s=new StringBuilder();for(int k=random.nextInt(129);k>0;k--)s.append(alphabet.charAt(random.nextInt(alphabet.length())));String text=s.toString();need(original(text).equals(R27QualityTest.stripJavaStringsR31(text)),"arbitrary bounded text exact legacy output "+n);}
  String text="safe(\"DecisionGateEngine \\\"stake\\\"\"); SettlementEngine.authorize();";String code=R27QualityTest.stripJavaStringsR31(text);
  need(!code.contains("DecisionGateEngine")&&!code.contains("stake"),"literal data removed exactly");need(code.contains("SettlementEngine.authorize()"),"forbidden code outside literal still detectable");
  String body=String.join("",Collections.nCopies(1_000_000,"x"));String large="before(\""+body+"\");DecisionGateEngine.authorize();";boolean overflow=false;
  try{original(large);}catch(StackOverflowError real){overflow=true;}
  need(overflow,"actual original recursive regex overflows without Xss override");need("before(\"\");DecisionGateEngine.authorize();".equals(R27QualityTest.stripJavaStringsR31(large)),"million character literal has exact bounded-stack removal");
  String escaped="before(\""+String.join("",Collections.nCopies(100_000,"\\\"\\\\x"))+"\");placeBet();";
  need("before(\"\");placeBet();".equals(R27QualityTest.stripJavaStringsR31(escaped)),"large escaped literal exactoutput and forbidden call retained");
  Map<String,Object> out=new LinkedHashMap<>();out.put("state","PASS");out.put("assertions",assertions);out.put("boundedRandomLegacyEquivalence",5000);out.put("originalRealStackOverflowObserved",overflow);out.put("longLiteralCharacters",body.length());out.put("stackSizeOverride",false);out.put("heapOverride","512MiB");out.put("source","ACTUAL_R27QualityTest.stripJavaStringsR31");out.put("scope","H15_SCANNER_ENGINEERING_FIXTURE_NOT_ANDROID_OR_NATURAL_QUALITY");System.out.println(JsonR27.write(out));
 }
}
