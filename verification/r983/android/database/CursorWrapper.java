package android.database;
/** Host test API delegation only; no product class is replaced. */
public class CursorWrapper implements Cursor {
 private final Cursor cursor; public CursorWrapper(Cursor c){cursor=c;}
 public void close(){cursor.close();}
 public void copyStringToBuffer(int a0,android.database.CharArrayBuffer a1){cursor.copyStringToBuffer(a0,a1);}
 public void deactivate(){cursor.deactivate();}
 public byte[] getBlob(int a0){return cursor.getBlob(a0);}
 public int getColumnCount(){return cursor.getColumnCount();}
 public int getColumnIndex(java.lang.String a0){return cursor.getColumnIndex(a0);}
 public int getColumnIndexOrThrow(java.lang.String a0){return cursor.getColumnIndexOrThrow(a0);}
 public java.lang.String getColumnName(int a0){return cursor.getColumnName(a0);}
 public java.lang.String[] getColumnNames(){return cursor.getColumnNames();}
 public int getCount(){return cursor.getCount();}
 public double getDouble(int a0){return cursor.getDouble(a0);}
 public android.os.Bundle getExtras(){return cursor.getExtras();}
 public float getFloat(int a0){return cursor.getFloat(a0);}
 public int getInt(int a0){return cursor.getInt(a0);}
 public long getLong(int a0){return cursor.getLong(a0);}
 public android.net.Uri getNotificationUri(){return cursor.getNotificationUri();}
 public java.util.List<android.net.Uri> getNotificationUris(){return cursor.getNotificationUris();}
 public int getPosition(){return cursor.getPosition();}
 public short getShort(int a0){return cursor.getShort(a0);}
 public java.lang.String getString(int a0){return cursor.getString(a0);}
 public int getType(int a0){return cursor.getType(a0);}
 public boolean getWantsAllOnMoveCalls(){return cursor.getWantsAllOnMoveCalls();}
 public boolean isAfterLast(){return cursor.isAfterLast();}
 public boolean isBeforeFirst(){return cursor.isBeforeFirst();}
 public boolean isClosed(){return cursor.isClosed();}
 public boolean isFirst(){return cursor.isFirst();}
 public boolean isLast(){return cursor.isLast();}
 public boolean isNull(int a0){return cursor.isNull(a0);}
 public boolean move(int a0){return cursor.move(a0);}
 public boolean moveToFirst(){return cursor.moveToFirst();}
 public boolean moveToLast(){return cursor.moveToLast();}
 public boolean moveToNext(){return cursor.moveToNext();}
 public boolean moveToPosition(int a0){return cursor.moveToPosition(a0);}
 public boolean moveToPrevious(){return cursor.moveToPrevious();}
 public void registerContentObserver(android.database.ContentObserver a0){cursor.registerContentObserver(a0);}
 public void registerDataSetObserver(android.database.DataSetObserver a0){cursor.registerDataSetObserver(a0);}
 public boolean requery(){return cursor.requery();}
 public android.os.Bundle respond(android.os.Bundle a0){return cursor.respond(a0);}
 public void setExtras(android.os.Bundle a0){cursor.setExtras(a0);}
 public void setNotificationUri(android.content.ContentResolver a0,android.net.Uri a1){cursor.setNotificationUri(a0,a1);}
 public void setNotificationUris(android.content.ContentResolver a0,java.util.List<android.net.Uri> a1){cursor.setNotificationUris(a0,a1);}
 public void unregisterContentObserver(android.database.ContentObserver a0){cursor.unregisterContentObserver(a0);}
 public void unregisterDataSetObserver(android.database.DataSetObserver a0){cursor.unregisterDataSetObserver(a0);}
}
