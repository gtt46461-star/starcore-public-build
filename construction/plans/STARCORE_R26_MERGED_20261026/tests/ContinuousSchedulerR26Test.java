import java.util.*;
public final class ContinuousSchedulerR26Test {
    static int checks;
    static void ok(boolean condition,String label){checks++;if(!condition)throw new AssertionError(label);}
    public static void main(String[] args){
        ContinuousSchedulerR26 s=new ContinuousSchedulerR26();
        List<ContinuousSchedulerR26.Task> research=new ArrayList<>();
        for(int i=1;i<=24;i++)research.add(s.add("F"+i,ContinuousSchedulerR26.Lane.P2));
        for(int i=1;i<=16;i++)research.add(s.add("E"+i,ContinuousSchedulerR26.Lane.P2));
        ContinuousSchedulerR26.Task feed=s.add("feedback",ContinuousSchedulerR26.Lane.P1);
        ContinuousSchedulerR26.Task report=s.add("report",ContinuousSchedulerR26.Lane.P3);
        // 80 slots serve all 40 mechanisms once, with concurrent feedback/report.
        for(int i=1;i<=80;i++){ContinuousSchedulerR26.Task t=s.next(true,true,false,false,true);ok(t!=null,"work conserving");s.committed(t,t.cursor+1,i);}
        for(ContinuousSchedulerR26.Task t:research)ok(t.steps==1,"every mechanism served "+t.id);
        ok(feed.steps==20&&report.steps==20,"non-starved other lanes");
        ok(s.next(false,true,false,false,true)==null,"user stop");
        ok(s.next(true,false,false,false,true)==null,"OS restriction");
        ok(s.next(true,true,true,false,true)==null,"critical transaction yields research");
        ok(s.next(true,true,false,true,true)==null,"thermal checkpoint barrier");
        ok(s.next(true,true,false,false,false)==null,"memory barrier");
        ok(s.next(true,true,false,false,true)!=null,"automatic resumption");
        ContinuousSchedulerR26.Task p0=s.add("freeze",ContinuousSchedulerR26.Lane.P0);
        ok(s.next(true,true,false,false,true)==p0,"critical first");p0.ready=false;
        ok(s.next(true,true,false,false,true)!=p0,"after critical work resumes");
        boolean rejected=false;try{s.committed(feed,feed.cursor,90);}catch(IllegalArgumentException e){rejected=true;}
        ok(rejected,"heartbeat is not progress");
        ok(s.starved(100000,60000).size()==42,"starvation is observable");
        // Telemetry change cannot change selection: power/idle are not eligibility inputs.
        ContinuousSchedulerR26 a=new ContinuousSchedulerR26(),b=new ContinuousSchedulerR26();
        for(int i=0;i<40;i++){a.add("t"+i,ContinuousSchedulerR26.Lane.P2);b.add("t"+i,ContinuousSchedulerR26.Lane.P2);}
        for(int i=0;i<120;i++)ok(a.next(true,true,false,false,true).id.equals(b.next(true,true,false,false,true).id),"power-independent dispatch");
        System.out.println("R26 scheduler assertions="+checks+" PASS HOST_ONLY");
    }
}
