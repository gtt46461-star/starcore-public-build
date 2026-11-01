"""Actual compiled matrix/schema and calibration task on original native SQLite shim. No device/quality claim."""
import pathlib,os,subprocess,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
sdk=pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=pathlib.Path(os.environ.get('SQLITE_JDBC','/workspace/work/r29/deps/sqlite-jdbc.jar'))
def run(args):subprocess.run(list(map(str,args)),cwd=root,check=True)
with tempfile.TemporaryDirectory(prefix='repair-matrix-host-') as temporary:
 out=pathlib.Path(temporary);shim=out/'shim';shim.mkdir();test=out/'test';test.mkdir();log=out/'Log.java';log.write_text('package android.util;public final class Log{public static int w(String t,String v){return 0;}public static int w(String t,String v,Throwable e){return 0;}public static int e(String t,String v){return 0;}}')
 extra=out/'extra';extra.mkdir();values=extra/'ContentValues.java';values.write_text('package android.content;public class ContentValues{public final java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();'+''.join('public void put(String k,'+t+' v){data.put(k,v);}' for t in ['String','Integer','Long','Double','Float','Boolean','byte[]'])+'public void putNull(String k){data.put(k,null);}}')
 database=extra/'SQLiteDatabase.java';base=(root/'tools/r29/qual_host/shim/android/database/sqlite/SQLiteDatabase.java').read_text();where=base.index('    public Cursor rawQuery(');base=base[:where]+"""    public long insertWithOnConflict(String table,String empty,android.content.ContentValues values,int conflict){java.util.StringJoiner names=new java.util.StringJoiner(","),slots=new java.util.StringJoiner(",");java.util.List<Object> args=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.data.entrySet()){names.add(e.getKey());slots.add("?");args.add(e.getValue());}execSQL("INSERT "+(conflict==4?"OR IGNORE ":conflict==5?"OR REPLACE ":"")+"INTO "+table+"("+names+") VALUES("+slots+")",args.toArray());try(Cursor c=rawQuery("SELECT changes(),last_insert_rowid()",null)){return c.moveToFirst()&&c.getLong(0)>0?c.getLong(1):-1;}}
"""+base[where:];database.write_text(base)
 run(['javac','-nowarn','-encoding','UTF-8','-d',shim,*[p for p in sorted((root/'tools/r29/qual_host/shim').rglob('*.java')) if p.name!='SQLiteDatabase.java'],log,values,database])
 cp=os.pathsep.join(map(str,[shim,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r30-repair/qual_host/RepairMatrixR30HostTest.java',root/'tools/r30/qual_host/CalibrationR30HostTest.java'])
 cp=os.pathsep.join(map(str,[shim,test,classes,jdbc,sdk]))
 for name in ['RepairMatrixR30HostTest','CalibrationR30HostTest']:run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.'+name])
