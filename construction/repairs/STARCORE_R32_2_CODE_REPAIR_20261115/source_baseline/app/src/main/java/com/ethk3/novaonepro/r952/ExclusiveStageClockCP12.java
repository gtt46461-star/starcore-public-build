package com.ethk3.novaonepro.r952;
import java.util.*;
/** Same-thread nested spans. Direct child inclusive intervals are disjoint, so subtraction is a union. */
public final class ExclusiveStageClockCP12 {
 public interface Clock { long wallNanos();long cpuNanos(); }
 public static final class Span {
  public final String stage;public final long wallTotalNanos,wallExclusiveNanos,cpuTotalNanos,cpuExclusiveNanos,threadId;
  Span(String stage,long total,long exclusive,long cpuTotal,long cpuExclusive,long thread){this.stage=stage;wallTotalNanos=total;wallExclusiveNanos=exclusive;cpuTotalNanos=cpuTotal;cpuExclusiveNanos=cpuExclusive;threadId=thread;}
  public String canonical(){return "CP12EX1|"+stage+'|'+wallExclusiveNanos+'|'+cpuExclusiveNanos+'|'+threadId+"|SYNC;";}
 }
 private static final class Frame {final String stage;final long wall,cpu,thread;long childWall,childCpu;boolean cpuMissing;Frame(String stage,long wall,long cpu){this.stage=stage;this.wall=wall;this.cpu=cpu;thread=Thread.currentThread().getId();cpuMissing=cpu<0;}}
 private final Clock clock;private final ThreadLocal<ArrayDeque<Frame>> stacks=new ThreadLocal<ArrayDeque<Frame>>(){protected ArrayDeque<Frame> initialValue(){return new ArrayDeque<>();}};
 public ExclusiveStageClockCP12(Clock clock){this.clock=Objects.requireNonNull(clock);}
 public void begin(String stage){need(stage!=null&&stage.matches("[A-Z][A-Z0-9_]{0,63}"),"STAGE_NAME");ArrayDeque<Frame> stack=stacks.get();need(stack.size()<64,"STAGE_DEPTH");stack.addLast(new Frame(stage,clock.wallNanos(),clock.cpuNanos()));}
 public Span end(String stage){ArrayDeque<Frame> stack=stacks.get();need(!stack.isEmpty()&&stack.peekLast().stage.equals(stage),"STAGE_STACK_MISMATCH");Frame frame=stack.removeLast();long wall=clock.wallNanos(),cpu=clock.cpuNanos();need(wall>=frame.wall&&frame.childWall<=wall-frame.wall,"STAGE_MONOTONIC_WALL");long total=wall-frame.wall,exclusive=total-frame.childWall;
  long cpuTotal=frame.cpu<0||cpu<frame.cpu?-1:cpu-frame.cpu,cpuExclusive=frame.cpuMissing||cpuTotal<frame.childCpu?-1:cpuTotal-frame.childCpu;
  if(!stack.isEmpty()){Frame parent=stack.peekLast();parent.childWall+=total;if(cpuTotal<0)parent.cpuMissing=true;else parent.childCpu+=cpuTotal;}
  return new Span(stage,total,exclusive,cpuTotal,cpuExclusive,frame.thread);
 }
 public int abandon(){ArrayDeque<Frame> stack=stacks.get();int count=stack.size();stack.clear();stacks.remove();return count;}
 public int depth(){return stacks.get().size();}
 private static void need(boolean b,String code){if(!b)throw new IllegalStateException(code);}
}
