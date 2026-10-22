package com.ethk3.novaonepro.r952;
import java.util.concurrent.atomic.AtomicInteger;
public final class ExclusiveStageClockCP12Test {
 static int checks;static void ok(boolean b){checks++;if(!b)throw new AssertionError("clock "+checks);}
 static final class C implements ExclusiveStageClockCP12.Clock {long wall,cpu;public long wallNanos(){return wall;}public long cpuNanos(){return cpu;}void at(long w,long c){wall=w;cpu=c;}}
 public static void main(String[] args)throws Exception {C c=new C();ExclusiveStageClockCP12 x=new ExclusiveStageClockCP12(c);
  c.at(100,10);x.begin("PARENT");c.at(110,12);x.begin("CHILD_A");c.at(120,15);x.begin("GRANDCHILD");c.at(130,18);ExclusiveStageClockCP12.Span g=x.end("GRANDCHILD");ok(g.wallExclusiveNanos==10&&g.cpuExclusiveNanos==3);c.at(140,21);ExclusiveStageClockCP12.Span a=x.end("CHILD_A");ok(a.wallTotalNanos==30&&a.wallExclusiveNanos==20&&a.cpuTotalNanos==9&&a.cpuExclusiveNanos==6);
  c.at(150,23);x.begin("CHILD_B");c.at(170,28);ExclusiveStageClockCP12.Span b=x.end("CHILD_B");ok(b.wallExclusiveNanos==20&&b.cpuExclusiveNanos==5);c.at(200,35);ExclusiveStageClockCP12.Span p=x.end("PARENT");ok(p.wallTotalNanos==100&&p.wallExclusiveNanos==50&&p.cpuTotalNanos==25&&p.cpuExclusiveNanos==11);ok(p.wallExclusiveNanos+a.wallExclusiveNanos+b.wallExclusiveNanos+g.wallExclusiveNanos==p.wallTotalNanos);ok(p.canonical().startsWith("CP12EX1|PARENT|50|11|"));ok(x.depth()==0);
  c.at(210,40);x.begin("MISSING_PARENT");c.at(220,-1);x.begin("MISSING_CHILD");c.at(230,-1);ok(x.end("MISSING_CHILD").cpuExclusiveNanos==-1);c.at(240,50);ok(x.end("MISSING_PARENT").cpuExclusiveNanos==-1);
  c.at(250,55);x.begin("ORDER");try{x.end("OTHER");throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("STAGE_STACK_MISMATCH"));}ok(x.abandon()==1&&x.depth()==0);
  c.at(300,60);x.begin("MAIN");AtomicInteger childDepth=new AtomicInteger(-1);Thread t=new Thread(()->{childDepth.set(x.depth());x.begin("OTHER_THREAD");ok(x.depth()==1);x.abandon();});t.start();t.join();ok(childDepth.get()==0&&x.depth()==1);x.abandon();
  c.at(400,70);x.begin("TIME");c.at(399,71);try{x.end("TIME");throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("STAGE_MONOTONIC_WALL"));}x.abandon();
  for(int i=0;i<64;i++)x.begin("D"+i);try{x.begin("OVER");throw new AssertionError();}catch(IllegalStateException e){ok(e.getMessage().equals("STAGE_DEPTH"));}ok(x.abandon()==64);
  System.out.println("CP12_EXCLUSIVE_STAGE_CLOCK_PASS checks="+checks+" nestedUnion=PASS cpuUnavailable=PASS threadIsolation=PASS");
 }
}
