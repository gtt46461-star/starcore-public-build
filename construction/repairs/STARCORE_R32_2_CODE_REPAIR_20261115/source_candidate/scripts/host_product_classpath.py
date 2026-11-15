"""Dependencies for unchanged independent kernel recompiles; Android compilation must run first."""
import os,pathlib
def product_classpath(root,output=None):
 root=pathlib.Path(root);classes=root/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'
 sdk=pathlib.Path(os.environ.get('ANDROID_HOME',os.environ.get('ANDROID_SDK_ROOT','/nonexistent')))/'platforms/android-36/android.jar'
 if not (classes/'com/ethk3/novaonepro/strategyv2/EvolutionLoopR27.class').is_file() or not sdk.is_file():
  raise SystemExit('HOST_COMPILED_PRODUCT_DEPENDENCIES_MISSING: activate JDK17/SDK36 and compile current Android source first')
 return os.pathsep.join(map(str,([output] if output else [])+[classes,sdk]))
