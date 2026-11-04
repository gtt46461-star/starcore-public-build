package com.ethk3.novaonepro;

import org.junit.Test;
import static org.junit.Assert.*;
import static com.ethk3.novaonepro.BackgroundRuntimeCapabilityR811.ObservationR31.*;

/** State mapping and interruption semantics only; host execution is not Android OS evidence. */
public final class R31BackgroundCapabilityStateTest {
    @Test public void configuredCapabilitiesAreNeverDeviceSoakEvidence(){
        assertEquals("CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,YES,YES,YES,NO,NO,NO,YES));
    }
    @Test public void onlyObservedRestrictionsAreOsRestricted(){
        assertEquals("OS_RESTRICTED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,NO,YES,YES,YES,NO,NO,YES));
        assertEquals("OS_RESTRICTED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,YES,YES,YES,NO,NO,YES,YES));
        assertEquals("DEGRADED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,NO,YES,YES,NO,NO,NO,YES));
        assertEquals("DEGRADED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,YES,YES,YES,NO,YES,NO,YES));
    }
    @Test public void unknownAndUnsupportedArePreservedSeparately(){
        assertEquals("UNKNOWN",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,UNKNOWN,YES,YES,NO,NO,NO,YES));
        assertEquals("CAPABILITIES_READY_DEVICE_SOAK_NOT_VERIFIED",BackgroundRuntimeCapabilityR811.operatingStatusR31(true,UNSUPPORTED,UNSUPPORTED,UNSUPPORTED,UNSUPPORTED,NO,UNSUPPORTED,YES));
        assertEquals("UNKNOWN",BackgroundRuntimeCapabilityR811.operatingStatusR31(null,YES,YES,YES,NO,NO,NO,YES));
    }
    @Test public void userDisabledIsNotPromisedAutomaticRecovery(){
        assertEquals("USER_DISABLED",BackgroundRuntimeCapabilityR811.operatingStatusR31(false,NO,NO,NO,YES,YES,YES,NO));
    }
    @Test public void forceStopRequiresActualStartEvidenceAndIntervalsAreNotUptime(){
        String unsupported=LifecycleHealthR963.interruptionSummaryR31(100,0,null,"UNSUPPORTED_API_BELOW_35","NO_EXIT");assertTrue(unsupported.contains("force-stop=UNSUPPORTED"));assertTrue(unsupported.contains("不代表无中断"));
        String known=LifecycleHealthR963.interruptionSummaryR31(200,100,true,"ApplicationStartInfo.wasForceStopped/current_pid_and_process","USER_REQUESTED");assertTrue(known.contains("force-stop=true"));assertTrue(known.contains("UNOBSERVED_INTERVAL[100,200]"));assertTrue(known.contains("实际运行未知"));assertTrue(known.contains("不补造过去赛前预测"));
        assertTrue(LifecycleHealthR963.interruptionSummaryR31(100,200,false,"ACTUAL_CURRENT_START","EXIT").contains("UNKNOWN_WALL_CLOCK_ORDER"));
    }
    @Test public void detailsCarrySourceClockUnknownsAndUnverifiedOem(){
        BackgroundRuntimeCapabilityR811.RuntimeObservationR31 state=new BackgroundRuntimeCapabilityR811.RuntimeObservationR31(100,200,28,36,true,YES,YES,YES,NO,NO,UNKNOWN,YES,java.util.Collections.singletonMap("ActivityManager.isBackgroundRestricted","SecurityException"));
        assertEquals("UNKNOWN",state.status);String detail=state.userSummary();assertTrue(detail.contains("OEM自启动=未核实"));assertTrue(detail.contains("sampleElapsedMs=200"));assertTrue(detail.contains("24小时=NOT_RUN"));assertTrue(detail.contains("72小时=NOT_RUN"));assertTrue(detail.contains("SecurityException"));assertFalse(detail.contains("DEVICE_PASS"));
        try{state.errors.put("fake","PASS");fail("mutable observation");}catch(UnsupportedOperationException expected){}
    }
}
