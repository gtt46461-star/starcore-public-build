package com.ethk3.novaonepro;
import java.util.*;import java.lang.reflect.*;import java.nio.file.*;
public final class ResearchFeedbackMetricsContractCP12 {
 static int assertions;static void ok(boolean x){assertions++;if(!x)throw new AssertionError("assertion "+assertions);}
 static Constructor<?> row;static Method metrics;
 static Object observation(String issue,int actual,int hit,double p)throws Exception{return row.newInstance(issue,V51ChecksumEngine.sha256(issue),V51ChecksumEngine.sha256("receipt"+issue),7,1,actual,hit,p);}
 static ResearchFeedbackCP12.Metrics of(List<Object> rows)throws Exception{return (ResearchFeedbackCP12.Metrics)metrics.invoke(null,rows,"algorithm:R958-F-V3-TEST");}
 public static void main(String[] args)throws Exception{
  Class<?> type=Class.forName("com.ethk3.novaonepro.ResearchFeedbackCP12$Observation");row=type.getDeclaredConstructors()[0];row.setAccessible(true);metrics=ResearchFeedbackCP12.Metrics.class.getDeclaredMethod("of",List.class,String.class);metrics.setAccessible(true);
  List<Object> rows=new ArrayList<>();rows.add(observation("202609221440",1,0,.3));rows.add(observation("202609230001",10,0,.3));ResearchFeedbackCP12.Metrics m=of(rows);ok(m.n==2&&m.missStreak==2&&m.episodeStart.equals("202609221440"));ok(m.n<20);ok(Math.abs(m.residualMean+.3)<1e-12);ok(Double.isNaN(m.js));
  rows.add(observation("202609230003",10,0,.3));m=of(rows);ok(m.missStreak==1&&m.episodeStart.equals("202609230003"));rows.add(observation("202609230004",1,1,.3));m=of(rows);ok(m.missStreak==0&&m.episodeStart.isEmpty());
  rows.clear();String issue="202609230001";for(int i=0;i<200;i++){rows.add(observation(issue,i<100?1:10,i<150?1:0,.3));issue=TargetIssueResolver.next(issue).targetIssue;}m=of(rows);ok(m.n==200&&m.missStreak==50);ok(m.episodeStart.equals("202609230151"));ok(Math.abs(m.residualMean+.3)<1e-12);ok(m.js>.5);ok(Math.abs(m.ece-(70-36)/120.0)<1e-12);
  // Whole lifetime episode survives a bounded recent-window threshold.
  rows.clear();issue="202609230001";for(int i=0;i<1200;i++){rows.add(observation(issue,10,0,.3));issue=TargetIssueResolver.next(issue).targetIssue;}m=of(rows);ok(m.missStreak==1200&&m.episodeStart.equals("202609230001"));ok(m.n==1200);
  String out="{\"status\":\"PASS\",\"assertions\":"+assertions+",\"scope\":\"actual formal feedback kernel: second miss below20, rollover, gap UNKNOWN, reset, rolling50/120/100+100, lifetime1200 bounded episode\"}";Files.writeString(Path.of(args[0]),out+"\n");System.out.println(out);
 }
}
