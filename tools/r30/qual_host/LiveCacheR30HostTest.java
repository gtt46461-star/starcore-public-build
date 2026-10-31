package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import sun.misc.Unsafe;

/** Reproduces the real runtime's backlog transition using its production method and SQLite. */
public final class LiveCacheR30HostTest {
    static String issue(int n) { return String.format(java.util.Locale.ROOT,"20261030%04d",n); }
    static void add(Connection c,int first,int last) throws Exception {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,0)")) {
            for(int n=first;n<=last;n++) { p.setString(1,issue(n));for(int k=1;k<=10;k++)p.setInt(k+1,k);p.executeUpdate(); }
        }
    }
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try(Statement s=c.createStatement()){s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,dirty INT)");}
            add(c,1,100);
            Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);Unsafe u=(Unsafe)field.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);
            ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);
            ResearchPoolR27.Worker worker=pool.new Worker(new SQLiteDatabase(c),"test");
            FeatureCacheR27 initial=rt.liveCacheWithPlaceholder(worker,TargetIssueResolver.sequenceOrdinalSU1(issue(100)));
            if(initial==null)throw new AssertionError("INITIAL_SOURCE_MISSING");
            initial.family(2,12);
            if(args.length>0 && args[0].equals("correction")) {
                try(Statement s=c.createStatement()) { s.executeUpdate("UPDATE draws SET n1=2,n2=1 WHERE issue='"+issue(10)+"'"); }
                FeatureCacheR27 next=rt.liveCacheWithPlaceholder(worker,TargetIssueResolver.sequenceOrdinalSU1(issue(100)));
                int actual=next.data.draws[9][0];
                System.out.println("CORRECTED_DRAW expected=2 actual="+actual);
                if(actual!=2)throw new AssertionError("LIVE_FEATURE_SOURCE_STALE_CORRECTION");
                double[] got=next.family(2,12), want=new FeatureCacheR27(next.data).family(2,12);
                for(int i=0;i<got.length;i++)if(Double.doubleToLongBits(got[i])!=Double.doubleToLongBits(want[i]))throw new AssertionError("FEATURE_MISMATCH_AT_"+i);
                System.out.println("CORRECTION_FEATURES_MATCH_FULL_RECOMPUTATION");
                return;
            }
            add(c,101,190);
            long expected=TargetIssueResolver.sequenceOrdinalSU1(issue(190));
            FeatureCacheR27 next=rt.liveCacheWithPlaceholder(worker,expected);
            long actual=next.data.ordinals[next.data.size()-2];
            System.out.println("BACKLOG_SOURCE expected="+expected+" actual="+actual+" behind="+(expected-actual));
            if(actual!=expected)throw new AssertionError("LIVE_FEATURE_SOURCE_INCOMPLETE_BACKLOG");
            double[] got=next.family(2,12), want=new FeatureCacheR27(next.data).family(2,12);
            for(int i=0;i<got.length;i++)if(Double.doubleToLongBits(got[i])!=Double.doubleToLongBits(want[i]))throw new AssertionError("BACKLOG_FEATURE_MISMATCH_AT_"+i);
            if(rt.liveCacheWithPlaceholder(worker,expected+1)!=null)throw new AssertionError("STALE_HEAD_MUST_NOT_FREEZE");
            System.out.println("BACKLOG_FEATURES_MATCH_FULL_RECOMPUTATION_AND_STALE_HEAD_REJECTED");
        }
    }
}
