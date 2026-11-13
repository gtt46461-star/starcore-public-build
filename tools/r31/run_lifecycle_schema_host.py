#!/usr/bin/env python3
"""Native actual one-object migration owner, with declaration-only fixture adapters.
This proves owner semantics; it does not claim the four production Stores ran.
"""
from pathlib import Path
import argparse,datetime,hashlib,json,os,subprocess,tempfile
ROOT=Path(__file__).resolve().parents[2]
def main():
 p=argparse.ArgumentParser();p.add_argument('--out',type=Path,required=True);a=p.parse_args();out=a.out.resolve();out.mkdir(parents=True,exist_ok=False)
 classes=Path(os.environ.get('R31_COMPILED_SUPPORT',ROOT/'app/build/intermediates/javac/debug/compileDebugJavaWithJavac/classes'))
 sdk=Path(os.environ['ANDROID_HOME'])/'platforms/android-36/android.jar';jdbc=Path(os.environ.get('SQLITE_JDBC','/tmp/r31-baseline-deps/sqlite-jdbc.jar'));slf=next(Path(os.environ['GRADLE_USER_HOME']).glob('caches/modules-2/files-2.1/org.slf4j/slf4j-api/*/*/*.jar'))
 source=ROOT/'app/src/main/java/com/ethk3/novaonepro';sources=[source/'LifecycleSchemaR31.java',source/'SchemaDomainCoordinatorR926.java',source/'LegacyDdlR31.java',ROOT/'tools/r31/unified_host/LifecycleSchemaR31Host.java',Path(__file__).resolve()]
 hashes={str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources};commands=[]
 def run(command,name):
  r=subprocess.run(list(map(str,command)),cwd=ROOT,capture_output=True,text=True,timeout=120);(out/name).write_text(r.stdout+r.stderr);commands.append({'command':list(map(str,command)),'exitCode':r.returncode,'log':name})
  if r.returncode:raise RuntimeError(name+'\n'+(r.stdout+r.stderr)[-5000:])
  return r.stdout
 with tempfile.TemporaryDirectory(prefix='r31-lifecycle-owner-') as temp:
  work=Path(temp);platform=work/'platform';production=work/'production';test=work/'test'
  for d in [platform,production,test]:d.mkdir()
  shims=list((ROOT/'tools/r31/ddl_host').rglob('*.java'));shims.extend((ROOT/'tools/r29/qual_host/shim/android/database/sqlite').glob('SQLite*Exception.java'))
  run(['javac','-cp',os.pathsep.join(map(str,[sdk,classes])),'-d',platform,*shims],'platform.log')
  # Only module declarations are fixture adapters. The migration owner, actual
  # SQLite and journal are compiled unmodified from the production sources.
  fixtures={
   'KeepMigrationStoreR31':'''public static final String[] DDL={"CREATE TABLE IF NOT EXISTS r31_keep_owner_fixture(id INTEGER PRIMARY KEY,value TEXT NOT NULL DEFAULT 'Ready ; IF NOT EXISTS X')","CREATE INDEX IF NOT EXISTS r31_keep_owner_index ON r31_keep_owner_fixture(value) WHERE value='Ready ; IF NOT EXISTS X'","CREATE TRIGGER IF NOT EXISTS r31_keep_owner_guard BEFORE UPDATE ON r31_keep_owner_fixture WHEN OLD.value='Original owner' BEGIN SELECT RAISE(ABORT,'Owner Literal ; IF NOT EXISTS'); END"};''',
   'KeepMismatchReferenceR31':'public static final String[] DDL={};public static String[] cutoverDefinitionsR31(android.database.sqlite.SQLiteDatabase db){return new String[0];}public static boolean advanceCutoverWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){return false;}',
   'MainlineFreezeProofR31':'public static final String[] DDL={};',
   'ImprovementStoreR31':'public static final String[] DDL={};public static String[] sourceGuardsR31(android.database.sqlite.SQLiteDatabase db){return new String[0];}public static boolean ready(android.database.sqlite.SQLiteDatabase db){return true;}public static void seedWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){}',
   'StartupCheckpointIdentityR31':'public static String[] ddlR31(){return new String[0];}public static boolean ready(android.database.sqlite.SQLiteDatabase db){return true;}public static void seedWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){}',
   'GapReferenceR31':'public static final String[] DDL={};public static String[] guardDefinitionsR31(android.database.sqlite.SQLiteDatabase db){return new String[0];}public static boolean advanceCutoverWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){return false;}',
   'KeepArchiveRetirementR31':'public static final String[] DDL={};public static String[] guardDefinitionsR31(android.database.sqlite.SQLiteDatabase db){return new String[0];}public static boolean advanceWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){return false;}',
   'CheckpointGcStoreR31':'public static final String[] DDL={};public static String[] referenceGuards(android.database.sqlite.SQLiteDatabase db){return new String[0];}',
   'HypothesisStoreR31':'public static String[] ddlR31(){return new String[0];}public static void requireSchemaR31(android.database.sqlite.SQLiteDatabase db){}public static void seedQuotaWithinMigrationR31(android.database.sqlite.SQLiteDatabase db){}'}
  adapters=[]
  for name,body in fixtures.items():
   path=work/(name+'.java');path.write_text('package com.ethk3.novaonepro.strategyv2;public final class '+name+'{'+body+'}');adapters.append(path)
  cp=os.pathsep.join(map(str,[platform,production,test,classes,sdk,jdbc,slf]))
  run(['javac','-sourcepath','','-implicit:none','-cp',os.pathsep.join(map(str,[sdk,platform,production,test,classes,jdbc,slf])),'-d',production,*sources[:3],*adapters],'production.log')
  run(['javac','-cp',cp,'-d',test,sources[3]],'host.log')
  result=run(['java','-Xmx256m','-cp',cp,'com.ethk3.novaonepro.LifecycleSchemaR31Host','run',work/'owner.db'],'run.log')
 if hashes!={str(x.relative_to(ROOT)):hashlib.sha256(x.read_bytes()).hexdigest() for x in sources}:raise RuntimeError('SOURCE_CHANGED_DURING_EXECUTION')
 print(result)
 (out/'RECEIPT.json').write_text(json.dumps({'state':'PASS_HOST_NATIVE_OWNER_ONLY','scope':'ACTUAL_PRODUCTION_MIGRATION_OWNER_AND_JOURNAL; DECLARATION_FIXTURE_ADAPTERS_NOT_PRODUCTION_STORES','capturedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),'sourceHead':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),'workingTreeDirty':bool(subprocess.check_output(['git','status','--porcelain'],cwd=ROOT,text=True).strip()),'sources':hashes,'commands':commands,'maxActualDdlPerTransaction':1,'realKillBoundaries':3,'criticalCommitsBetweenUnits':3,'businessReadinessDml':0,'Android':'NOT_RUN','physicalDevice':'NOT_RUN','naturalQuality':'NOT_PROVEN'},indent=2)+'\n')
if __name__=='__main__':main()
