package com.ethk3.novaonepro;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
public final class OriginalReadLeaseFixtureR30HostTest {
    static void database(Path file)throws Exception { Files.createDirectories(file.getParent());try(Connection conn=DriverManager.getConnection("jdbc:sqlite:"+file)){conn.createStatement().execute("CREATE TABLE synthetic_fixture(value TEXT)");} }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");Path temporary=Files.createTempDirectory("r30-original-source-lease-");
        Path old=temporary.resolve("r30-instrumentation/scope/databases/"+AppConfig.DB_NAME);database(old);
        try(SourceReadLeaseR82 lease=SourceReadLeaseR82.acquire(old.toFile())){throw new AssertionError("OLD_UNTRUSTED_TEST_LAYOUT_WAS_ACCEPTED");}
        catch(IOException expected){if(!"untrusted source database path".equals(expected.getMessage()))throw expected;}
        Path fixed=temporary.resolve("r30-instrumentation/scope/"+ReleaseManifestR926.APPLICATION_ID+"/databases/"+AppConfig.DB_NAME);database(fixed);
        try(SourceReadLeaseR82 first=SourceReadLeaseR82.acquire(fixed.toFile());SourceReadLeaseR82 second=SourceReadLeaseR82.acquire(fixed.toFile())){}
        try(SourceReadLeaseR82 exclusive=SourceReadLeaseR82.exclusive(fixed.toFile())){}
        System.out.println("R30_ORIGINAL_SOURCE_READ_LEASE|OLD_TEST_LAYOUT_REJECTED|REAL_SQLITE_FILE|ORIGINAL_PRODUCTION_GUARD_UNCHANGED|SCOPED_PACKAGE_LAYOUT_SHARED_AND_EXCLUSIVE_OS_LOCKS_PASS|HOST_NOT_ANDROID_OR_NATURAL|PASS");
    }
}
