package android.content;
/** Android intent envelope only; product report request/journal/queue logic is unchanged. */
public final class Intent {
 private String action;private final java.util.Map<String,String> extras=new java.util.HashMap<>();
 public Intent(Context context,Class<?> target){}
 public Intent setAction(String value){action=value;return this;}
 public String getAction(){return action;}
 public Intent putExtra(String key,String value){extras.put(key,value);return this;}
 public String getStringExtra(String key){return extras.get(key);}
}
