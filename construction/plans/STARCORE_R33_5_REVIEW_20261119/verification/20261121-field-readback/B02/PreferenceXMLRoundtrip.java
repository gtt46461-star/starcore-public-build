package com.ethk3.novaonepro;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
public class PreferenceXMLRoundtrip {
 public static void main(String[] args) throws Exception {
  String journal=EnabledWindowJournalR335.append("",true,10000,500,"123456789abcdef0123456789abcdef0","123456789abcdef0123456789abcdef012345678");
  String xml="<map><string name='"+EnabledWindowJournalR335.PREF_KEY+"'>"+journal+"    </string><string name='"+EnabledWindowJournalR335.STATUS_KEY+"'>RECORDED</string><boolean name='user_enabled' value='true'/></map>";
  byte[] parsed=SourceClockProbe.project(xml);String projection=new String(parsed, StandardCharsets.UTF_8);
  if(!projection.contains(journal+"</string>")||!Arrays.equals(parsed,SourceClockProbe.project(projection)))throw new AssertionError("cumulative changes on reread");
  for(String invalid:new String[]{xml.replace(journal,journal.trim()),xml.replace("|10000|","|10001|"),xml.replace("    </string>","   x</string>")}) {
   boolean refused=false;try{SourceClockProbe.project(invalid);}catch(IllegalArgumentException ex){refused=true;}
   if(!refused)throw new AssertionError("lost original integrity");
  }
  System.out.println("ONE_EXACT_JOURNAL_AND_REPEATED_PROJECTION_MATCH; THREE_INTEGRITY_NEGATIVES_REJECTED");
 }
}
