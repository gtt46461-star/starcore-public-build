package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Pure lifecycle contract. It does not create predictions, labels or REAL authority. */
public final class RealtimeContractR33 {
    public static final long POLL_PERIOD_MS=1000L, PREDICTION_P95_LIMIT_MS=5000L;
    public static final long DRAW_TO_SETTLEMENT_LIMIT_MS=1000L;
    private RealtimeContractR33() {}

    public enum Coverage { NOT_DUE, FROZEN_ON_TIME, MISSED_PREDRAW, INVALID_PIT, UNKNOWN_CLOCK }
    public static Coverage coverage(long targetOrdinal,long historyMaxOrdinal,long deadlineWall,
                                    long frozenWall,boolean immutableFreezePresent,long nowWall,
                                    boolean scheduleVerified) {
        if(!scheduleVerified||historyMaxOrdinal<0||targetOrdinal<0||deadlineWall<=0||nowWall<=0)return Coverage.UNKNOWN_CLOCK;
        if(historyMaxOrdinal>=targetOrdinal)return Coverage.INVALID_PIT;
        if(immutableFreezePresent&&frozenWall>0&&frozenWall<deadlineWall)return Coverage.FROZEN_ON_TIME;
        // A freeze after deadline is a missed opportunity, never a later repair of the denominator.
        return nowWall<deadlineWall?Coverage.NOT_DUE:Coverage.MISSED_PREDRAW;
    }
    /** Only a timestamp span measured on one boot is a latency measurement. */
    public static long monotonicSpan(String originBoot,String completionBoot,long origin,long completion) {
        return originBoot==null||!originBoot.equals(completionBoot)||origin<0||completion<origin?-1:completion-origin;
    }
    public static String performance(long measuredMs,int samples,long limitMs) {
        if(measuredMs<0||samples<=0)return "UNKNOWN";
        return measuredMs<=limitMs?"PASS":"FAIL";
    }
    public static final class WindowCounts {
        public final long expected,due,onTime,missed,unknown,explicitlyDisabled;
        public WindowCounts(long expected,long due,long onTime,long missed,long unknown,long disabled) {
            if(expected<0||due<0||onTime<0||missed<0||unknown<0||disabled<0||due>expected
                    ||onTime>due||missed>due||unknown>due||onTime>due-missed||unknown!=due-onTime-missed)
                throw new IllegalArgumentException("R33_OPPORTUNITY_DENOMINATOR");
            this.expected=expected;this.due=due;this.onTime=onTime;this.missed=missed;this.unknown=unknown;explicitlyDisabled=disabled;
        }
        public double onTimeRate(){return due==0?Double.NaN:(double)onTime/(double)due;}
        public boolean complete(){return due>0&&unknown==0&&missed==0;}
    }
    /** Immutable handoff identity. Natural evidence is not produced by this envelope. */
    public static final class FeedbackEnvelope {
        public final String eventId,targetIssue,settlementHash,inputHash,policyHash,sourceCommit,role;
        public FeedbackEnvelope(String target,String settlement,String input,String policy,String source,String role) {
            if(target==null||!target.matches("[0-9]{12}")||!hash(settlement)||!hash(input)||!hash(policy)
                    ||source==null||!source.matches("[0-9a-f]{40}")
                    ||!("SHADOW".equals(role)||"REAL".equals(role)||"FUTURE".equals(role)))
                throw new IllegalArgumentException("R33_FEEDBACK_IDENTITY");
            targetIssue=target;settlementHash=settlement;inputHash=input;policyHash=policy;sourceCommit=source;this.role=role;
            eventId=digest(fields("R33_FEEDBACK_V1",target,settlement,input,policy,source,role));
        }
        public void requireSame(FeedbackEnvelope other) {
            if(other==null||!eventId.equals(other.eventId))throw new IllegalStateException("R33_FEEDBACK_CONFLICT");
        }
    }
    /** ACK means durable inbox receipt of the exact envelope; it never means training complete. */
    public static void requireInboxAck(FeedbackEnvelope event,String ackEvent,String ackPayload,boolean committed) {
        if(event==null||!committed||!event.eventId.equals(ackEvent)||!digest(fields(event.eventId,event.settlementHash,event.inputHash)).equals(ackPayload))
            throw new IllegalStateException("R33_FEEDBACK_ACK_NOT_DURABLE");
    }
    public static String inboxPayload(FeedbackEnvelope event){return digest(fields(event.eventId,event.settlementHash,event.inputHash));}
    public static String fields(String... values) {
        StringBuilder b=new StringBuilder();for(String s:values){if(s==null)throw new IllegalArgumentException("R33_NULL_FIELD");b.append(s.length()).append(':').append(s);}return b.toString();
    }
    private static boolean hash(String s){return s!=null&&s.matches("[0-9a-f]{64}");}
    public static String digest(String text) {
        try{byte[] v=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));StringBuilder b=new StringBuilder(64);
            for(byte x:v){int n=x&255;b.append("0123456789abcdef".charAt(n>>>4)).append("0123456789abcdef".charAt(n&15));}return b.toString();
        }catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
}
