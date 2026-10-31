package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * R30-B01-05 任务活性判定（纯Java，HOST可测），M12 唯一判定口径。
 *
 * 输入是一个任务在报告时刻的事实（全部来自 r26_task / ResearchPoolR27 统计，不估算）：
 *  pending        —— 已有数据但尚未处理的条数（期数/事件数），未知传 -1；
 *  periodsSinceCommit —— 自上次成功提交以来经过的开奖期数，未知传 -1；
 *  dependencyMissing —— 依赖任务未就绪（例如 schema 未完成）；
 *  waitingNewDraw  —— 已处理到最新期，等待下一期；
 *  running         —— 当前持有租约。
 *
 * 判定（按顺序，先命中先返回）：
 *  UNKNOWN            pending<0 或 periodsSinceCommit<0（不允许写成正常）；
 *  BLOCKED_DEPENDENCY dependencyMissing；
 *  IDLE_WAITING_DRAW  pending==0 且 waitingNewDraw；
 *  STALLED            pending>0 且 periodsSinceCommit>=5（M12 失败项，必须告警并进报告）；
 *  RUNNING            running；
 *  BACKLOG            pending>0（有积压但未超限）；
 *  IDLE_NO_WORK       其余。
 */
public final class TaskLivenessR30 {
    public static final int STALL_PERIODS = 5;
    private TaskLivenessR30() {}

    public enum State { UNKNOWN, BLOCKED_DEPENDENCY, IDLE_WAITING_DRAW, STALLED, RUNNING, BACKLOG, IDLE_NO_WORK }

    public static State classify(long pending, long periodsSinceCommit, boolean dependencyMissing,
                                 boolean waitingNewDraw, boolean running) {
        if (pending < 0 || periodsSinceCommit < 0) return State.UNKNOWN;
        if (dependencyMissing) return State.BLOCKED_DEPENDENCY;
        if (pending == 0 && waitingNewDraw) return State.IDLE_WAITING_DRAW;
        if (pending > 0 && periodsSinceCommit >= STALL_PERIODS) return State.STALLED;
        if (running) return State.RUNNING;
        if (pending > 0) return State.BACKLOG;
        return State.IDLE_NO_WORK;
    }

    /** M12 = STALLED 任务数；UNKNOWN 不计为 0，单列。 */
    public static Map<String, Object> summarize(State[] states) {
        int stalled = 0, unknown = 0, blocked = 0, total = 0;
        for (State s : states) {
            if (s == null) continue;
            total++;
            if (s == State.STALLED) stalled++;
            else if (s == State.UNKNOWN) unknown++;
            else if (s == State.BLOCKED_DEPENDENCY) blocked++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("tasks", total);
        m.put("stalled", stalled);
        m.put("unknown", unknown);
        m.put("blockedDependency", blocked);
        m.put("m12", unknown > 0 ? null : (Object) stalled);
        m.put("m12Pass", unknown > 0 ? null : (Object) (stalled == 0));
        return m;
    }
}
