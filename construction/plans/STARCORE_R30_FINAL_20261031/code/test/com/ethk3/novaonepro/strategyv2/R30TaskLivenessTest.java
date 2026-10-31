package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

/** R30-B01-05 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30TaskLivenessTest {
    @Test public void livenessClassification() {
        assertEquals(TaskLivenessR30.State.UNKNOWN, TaskLivenessR30.classify(-1, 0, false, false, false));
        assertEquals(TaskLivenessR30.State.BLOCKED_DEPENDENCY, TaskLivenessR30.classify(3, 9, true, false, false));
        assertEquals(TaskLivenessR30.State.IDLE_WAITING_DRAW, TaskLivenessR30.classify(0, 9, false, true, false));
        assertEquals(TaskLivenessR30.State.STALLED, TaskLivenessR30.classify(3, 5, false, false, true));
        assertEquals(TaskLivenessR30.State.RUNNING, TaskLivenessR30.classify(3, 4, false, false, true));
        assertEquals(TaskLivenessR30.State.BACKLOG, TaskLivenessR30.classify(3, 4, false, false, false));
        Map<String, Object> m = TaskLivenessR30.summarize(new TaskLivenessR30.State[] {TaskLivenessR30.State.RUNNING, TaskLivenessR30.State.UNKNOWN});
        assertNull("UNKNOWN 存在时 M12 不能写成0", m.get("m12"));
        m = TaskLivenessR30.summarize(new TaskLivenessR30.State[] {TaskLivenessR30.State.RUNNING, TaskLivenessR30.State.STALLED});
        assertEquals(1, ((Integer) m.get("m12")).intValue());
        assertEquals(Boolean.FALSE, m.get("m12Pass"));
    }
}
