package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ModernResearchProgressR78;
/** Read actual product progress on full schema, and confirm absent schema is not shown as zero. */
public final class ProgressReadR78 {
 public static void main(String[]a)throws Exception{try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
  String actual=ModernResearchProgressR78.render(d);System.out.println(actual);
  if(a[2].equals("schema")){if(actual.contains("UNKNOWN")||!actual.contains("历史训练完成／处理中／失败隔离=15/0/1"))throw new AssertionError("CURRENT_PROGRESS_NOT_READ");}
  else if(!actual.contains("UNKNOWN"))throw new AssertionError("UNKNOWN_CONVERTED_TO_ZERO");
  System.out.println("PASS PROGRESS_READ mode="+a[2]+" actual_product=TRUE");
 }}
}
