package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;

/** Actual router + persistent SQLite, with synthetic predraw market states. */
public final class DomainRouteHarnessR932 {
    static V51Models.StateTrajectorySnapshot state(String issue,int position,String domain){return new V51Models.StateTrajectorySnapshot(issue,position,"PROTO_BALANCED",domain,V51Enums.Lifecycle.UNKNOWN,4,.8,12,.01,.9,V51ChecksumEngine.sha256(issue+"|"+position+"|"+domain));}
    static DomainRouterR929.DomainEvidence evidence(String domain,String schema){return new DomainRouterR929.DomainEvidence(domain,schema,V51ChecksumEngine.sha256(domain+schema),160,360,true,.02,.04);}
    static void check(boolean ok,String code){if(!ok)throw new AssertionError(code);System.out.println(code+"=PASS");}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try{
            DbStore store=new DbStore(db);String schema=DomainRouterR929.installedSchemaHash();DomainRouterR929.DomainEvidence a=evidence("A",schema),b=evidence("B",schema);
            DomainRouterR929.Decision first=DomainRouterR929.route(store,"200",state("200",1,"A"),a,true);
            check(first.weightMultiplier==0,"NEW_DOMAIN_WAITS_FROZEN_HYSTERESIS");
            DomainRouterR929.Decision retry=DomainRouterR929.route(store,"200",state("200",1,"A"),a,true);
            check(retry.routeHash.equals(first.routeHash),"SAME_ISSUE_REPLAY_IDENTICAL_NO_STREAK_INCREMENT");
            check(DomainRouterR929.route(store,"201",state("201",1,"A"),a,true).weightMultiplier==0,"SECOND_DISTINCT_ISSUE_STILL_WAITING");
            check(DomainRouterR929.route(store,"202",state("202",1,"A"),a,true).weightMultiplier==1,"THIRD_DISTINCT_ISSUE_ACTIVATES");
            check(DomainRouterR929.route(new DbStore(db),"202",state("202",1,"A"),a,true).weightMultiplier==1,"RESTART_PRESERVES_ROUTE");
            check(DomainRouterR929.route(store,"203",state("203",2,"A"),a,true).weightMultiplier==0,"POSITIONS_DO_NOT_SHARE_STREAKS");
            check(DomainRouterR929.route(store,"203",state("203",1,"B"),b,true).weightMultiplier==0,"EXIT_DOMAIN_ZERO_CONTRIBUTION");
            check(DomainRouterR929.route(store,"204",state("204",1,"B"),b,true).weightMultiplier==0,"SWITCH_SECOND_ISSUE_WAIT");
            check(DomainRouterR929.route(store,"205",state("205",1,"B"),b,true).weightMultiplier==1,"SWITCH_STREAK_CAN_REACH_THRESHOLD");
            check(DomainRouterR929.route(store,"205",state("205",1,"A"),a,true).route==DomainRouterR929.Route.PAUSE,"SAME_ISSUE_CHANGED_STATE_REJECTED");
            check(DomainRouterR929.route(store,"199",state("199",1,"A"),a,true).route==DomainRouterR929.Route.PAUSE,"OUT_OF_ORDER_STATE_CANNOT_REWIND");
            check(DomainRouterR929.route(store,"206",state("206",1,"B"),evidence("B",V51ChecksumEngine.sha256("NEW_BOUNDARIES")),true).weightMultiplier==0,"NEW_SCHEMA_STARTS_NEW_STREAK");
            check(DomainRouterR929.route(store,"207",state("207",1,"B"),b,false).route==DomainRouterR929.Route.PAUSE,"RUNTIME_HEALTH_SEPARATE_FROM_MARKET");
            check(DomainRouterR929.route(store,"208",state("209",1,"B"),b,true).route==DomainRouterR929.Route.PAUSE,"FUTURE_STATE_TARGET_REJECTED");
            DomainRouterR929.DomainEvidence small=new DomainRouterR929.DomainEvidence("B",schema,V51ChecksumEngine.sha256("SMALL"),119,360,true,.02,.04);
            check(DomainRouterR929.route(store,"209",state("209",1,"B"),small,true).weightMultiplier==0,"CAL_SUPPORT_BELOW_120_FALLBACK");
            System.out.println("R932_DOMAIN_ROUTE_PASS|assertions=15|scope=REAL_SQLITE_SYNTHETIC_STATES");
        }finally{db.shutdown();}
    }
}
