package com.ethk3.novaonepro;
import java.nio.file.*;
import java.sql.*;
public final class OriginalSqliteFullFixtureR30HostTest {
    static long n(Connection db,String sql)throws Exception{try(Statement s=db.createStatement();ResultSet c=s.executeQuery(sql)){if(!c.next())throw new AssertionError(sql);return c.getLong(1);}}
    static void execute(Connection db,String sql)throws Exception{try(Statement s=db.createStatement()){s.execute(sql);}}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path path=Files.createTempFile("r30-real-sqlite-full-",".db");
        try(Connection primary=DriverManager.getConnection("jdbc:sqlite:"+path);Connection other=DriverManager.getConnection("jdbc:sqlite:"+path)){
            execute(primary,"PRAGMA journal_mode=WAL");execute(primary,"CREATE TABLE synthetic_fixture(id INTEGER PRIMARY KEY,payload BLOB)");
            long before=n(primary,"PRAGMA page_count");n(other,"PRAGMA max_page_count="+before);
            if(n(primary,"PRAGMA max_page_count")==before)throw new AssertionError("EXPECTED_CONNECTION_LOCAL_LIMIT_SCOPE");
            execute(primary,"INSERT INTO synthetic_fixture VALUES(1,zeroblob(16777216))");
            long pages=n(primary,"PRAGMA page_count"),pageSize=n(primary,"PRAGMA page_size");
            primary.setAutoCommit(false);
            if(n(primary,"PRAGMA max_page_count="+pages)!=pages)throw new AssertionError("PINNED_PRIMARY_LIMIT_NOT_APPLIED");
            try(PreparedStatement insert=primary.prepareStatement("INSERT INTO synthetic_fixture VALUES(2,zeroblob(?))")){
                insert.setLong(1,(pages+1)*pageSize);
                try{insert.execute();throw new AssertionError("ACTUAL_SQLITE_FULL_NOT_TRIGGERED");}
                catch(SQLException expected){if(expected.getErrorCode()!=13)throw expected;}
            }
            try{primary.rollback();throw new AssertionError("FULL_AUTO_ROLLBACK_NOT_OBSERVED");}
            catch(SQLException cleanup){if(cleanup.getErrorCode()!=1||!cleanup.getMessage().contains("no transaction is active"))throw cleanup;}
        }
        try(Connection reopened=DriverManager.getConnection("jdbc:sqlite:"+path)){
            if(n(reopened,"SELECT count(*) FROM synthetic_fixture")!=1||n(reopened,"SELECT count(*) FROM synthetic_fixture WHERE id=2")!=0)throw new AssertionError("FULL_LEFT_PARTIAL_COMMITTED_ROW");
        }
        System.out.println("R30_SQLITE_FULL_FIXTURE|REAL_TWO_CONNECTION_SQLITE|OTHER_CONNECTION_LIMIT_DOES_NOT_LIMIT_PRIMARY|PINNED_PRIMARY_REAL_ERROR13|AUTO_ROLLBACK_CLEANUP_ERROR1_CANNOT_REPLACE_ORIGINAL_FULL|NO_PARTIAL_ROW_AFTER_REOPEN|HOST_NOT_ANDROID|PASS");
    }
}
