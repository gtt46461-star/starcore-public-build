package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import java.nio.file.*;
import java.util.*;
/** Same native regression against unmodified production SQL on actual host SQLite. */
public final class ReportMetadataExportHost {
 public static void main(String[] args)throws Exception{Path root=Files.createTempDirectory("r26-report-export-");Path database=root.resolve("snapshot.db");try{
  try(SQLiteDatabase db=new SQLiteDatabase(args[0],database.toString())){ReportMetadataExportDeviceTest.fixture(db);}
  byte[] hash=java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(database));
  try(SQLiteDatabase db=SQLiteDatabase.openDatabase(database.toString(),null,SQLiteDatabase.OPEN_READONLY)){ReportMetadataExportDeviceTest.verify(db,root.toFile());}
  if(!Arrays.equals(hash,java.security.MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(database))))throw new AssertionError("Report mutated snapshot");
  Path aliases=root.resolve("aliases");Files.createDirectories(aliases);ReportMetadataExportDeviceTest.verifyDirectoryAliases(aliases.toFile());
  System.out.println("R26_REPORT_EXPORT_WITHOUT_ROWID_PASS|actualSU2CampaignSchema=true|compositeTextBlobPrimaryKey=true|longUnicodeNullAndBinaryPreserved=true|rowidShadowAndNoCaseGroups=true|last5000Of20005Exact=true|fullDiagnosticEvidenceGenerated=true|readonlySnapshotBytesUnchanged=true|androidAppAncestorAliasAccepted=true|filesNodeAndReportRootSymlinksStillRejected=true|durableReportJobAndAcknowledgement=true|cancellation=true|scope=HOST_PRODUCTION_SQLITE_NOT_USER_DEVICE_OR_ALL_REPORT_CHECKS");
 }finally{try(java.util.stream.Stream<Path> paths=Files.walk(root)){for(Path p:(Iterable<Path>)paths.sorted(Comparator.reverseOrder())::iterator)Files.delete(p);}}}
}
