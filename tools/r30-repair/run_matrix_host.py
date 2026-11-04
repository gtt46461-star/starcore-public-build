"""Actual compiled matrix/schema and calibration task on original native SQLite shim. No device/quality claim."""
import pathlib,os,subprocess,tempfile,sys
root=pathlib.Path(__file__).resolve().parents[2];sys.path.insert(0,str(root/'tools/r31'))
from host_runtime_fixture import compile_owned_platform
from host_tool_support import dependencies, run as host_run
classes,sdk,resources,jdbc=dependencies(root)
def run(args):host_run(root,args)
with tempfile.TemporaryDirectory(prefix='repair-matrix-host-') as temporary:
 out=pathlib.Path(temporary);test=out/'test';test.mkdir()
 owned=compile_owned_platform(root,out/'owned-platform',classes,sdk)
 extra=out/'extra';extra.mkdir();values=extra/'ContentValues.java';values.write_text('package android.content;public class ContentValues{public final java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();'+''.join('public void put(String k,'+t+' v){data.put(k,v);}' for t in ['String','Integer','Long','Double','Float','Boolean','byte[]'])+'public void putNull(String k){data.put(k,null);}}')
 # Extend only the HOST JDBC DML boundary; production migration/claim/write code stays real.
 database=extra/'SQLiteDatabase.java';base=(out/'owned-platform/sources/android/database/sqlite/SQLiteDatabase.java').read_text();where=base.index('    public Cursor rawQuery(');base=base[:where]+"""    public long insertWithOnConflict(String table,String empty,android.content.ContentValues values,int conflict){java.util.StringJoiner names=new java.util.StringJoiner(","),slots=new java.util.StringJoiner(",");java.util.List<Object> args=new java.util.ArrayList<>();for(java.util.Map.Entry<String,Object> e:values.data.entrySet()){names.add(e.getKey());slots.add("?");args.add(e.getValue());}execSQL("INSERT "+(conflict==4?"OR IGNORE ":conflict==5?"OR REPLACE ":"")+"INTO "+table+"("+names+") VALUES("+slots+")",args.toArray());try(Cursor c=rawQuery("SELECT changes(),last_insert_rowid()",null)){return c.moveToFirst()&&c.getLong(0)>0?c.getLong(1):-1;}}
"""+base[where:];database.write_text(base)
 process=extra/'Process.java';base=(out/'owned-platform/sources/android/os/Process.java').read_text();base=base.replace('public final class Process{','public final class Process{public static int myPid(){long pid=java.lang.ProcessHandle.current().pid();if(pid<=0||pid>Integer.MAX_VALUE)throw new IllegalStateException("HOST_PID_OUT_OF_ANDROID_INT_RANGE");return(int)pid;}',1);process.write_text(base)
 cp=os.pathsep.join(map(str,[owned,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',owned,values,database,process])
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r31/host/R31OwnedRuntimeFixture.java',root/'tools/r30-repair/qual_host/RepairMatrixR30HostTest.java'])
 cp=os.pathsep.join(map(str,[owned,test,classes,jdbc,sdk]))
 run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.RepairMatrixR30HostTest'])
 cp=os.pathsep.join(map(str,[owned,classes,sdk]))
 run(['javac','-nowarn','-encoding','UTF-8','-cp',cp,'-d',test,root/'tools/r31/host/R31OwnedRuntimeFixture.java',root/'tools/r30/qual_host/CalibrationR30HostTest.java'])
 cp=os.pathsep.join(map(str,[owned,test,classes,jdbc,sdk]))
 run(['java','-Xmx768m','-cp',cp,'com.ethk3.novaonepro.strategyv2.CalibrationR30HostTest'])
