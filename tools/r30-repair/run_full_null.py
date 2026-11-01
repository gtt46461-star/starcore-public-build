"""Run exactly the production 20-trial protocol; long-running but recoverable."""
import os,pathlib,subprocess,sys,tempfile
root=pathlib.Path(__file__).resolve().parents[2];classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
source=sys.argv[1] if len(sys.argv)>1 else subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
out=pathlib.Path(sys.argv[2]) if len(sys.argv)>2 else root/'evidence/r30-repair/q08-full'
with tempfile.TemporaryDirectory(prefix='repair-q08-') as tmp:
 cp=os.pathsep.join(map(str,[classes,pathlib.Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar']))
 subprocess.run(['javac','-nowarn','-cp',cp,'-d',tmp,str(root/'tools/r30-repair/qual_host/FullNullR30Host.java')],check=True)
 subprocess.run(['java','-Xmx1536m','-cp',tmp+os.pathsep+cp,'com.ethk3.novaonepro.strategyv2.FullNullR30Host',source,str(out)],cwd=root,check=True)
