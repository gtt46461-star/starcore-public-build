package android.database;
public class CursorWrapper implements Cursor {
  protected final Cursor c; public CursorWrapper(Cursor c){this.c=c;}
  public boolean moveToPosition(int position){return c.moveToPosition(position);} public boolean moveToFirst(){return c.moveToFirst();} public boolean moveToNext(){return c.moveToNext();}
  public int getColumnIndex(String n){return c.getColumnIndex(n);} public int getColumnIndexOrThrow(String n){return c.getColumnIndexOrThrow(n);}
  public String[] getColumnNames(){return c.getColumnNames();} public int getColumnCount(){return c.getColumnCount();}
  public String getColumnName(int i){return c.getColumnName(i);} public String getString(int i){return c.getString(i);}
  public int getInt(int i){return c.getInt(i);} public long getLong(int i){return c.getLong(i);} public double getDouble(int i){return c.getDouble(i);}
  public byte[] getBlob(int i){return c.getBlob(i);} public int getType(int i){return c.getType(i);} public boolean isNull(int i){return c.isNull(i);}
  public int getCount(){return c.getCount();} public void close(){c.close();}
}
