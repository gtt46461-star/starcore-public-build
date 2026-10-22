package android.content;
/** Captures service kicks without replacing any product implementation. */
public class Intent {
 private String action,id;public Intent(Context context,Class<?> owner){}
 public Intent setAction(String action){this.action=action;return this;}
 public Intent putExtra(String key,String value){if("id".equals(key))id=value;return this;}
 public String getAction(){return action;}public String getStringExtra(String key){return "id".equals(key)?id:null;}
}
