package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.nio.file.*;
/** Current mathematical family constructor supplies an explicit isolated AST input. */
public final class PrepareCertificateFixtureR984 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   ModernFamilyFactoryCP12.Proposal p=ModernFamilyFactoryCP12.prepare(db,CandidateAstR952.Family.FREQUENCY,0);
   Files.writeString(Path.of(a[2]),p.ast.canonical);
   if(p.ast.formatVersion!=3||p.ast.graphSpec.modelKind!=CandidateCompilerR952.ModelKind.FIXED_GRAPH)throw new AssertionError("Actual AST3 family required");
   System.out.println("ACTUAL_FAMILY_AST3_FIXTURE atoms="+p.specs.size()+" hash="+p.ast.structuralHash+" scope=TEST_ONLY");
  }
 }
}
