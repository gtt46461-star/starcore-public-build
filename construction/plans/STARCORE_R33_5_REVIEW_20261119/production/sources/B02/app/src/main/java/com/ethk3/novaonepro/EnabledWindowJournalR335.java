package com.ethk3.novaonepro;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Original permission assignments only. No SQLite, calendar, prediction or qualification authority. */
public final class EnabledWindowJournalR335 {
    public static final String PREF_KEY = "r335_enabled_assignment_journal";
    public static final String STATUS_KEY = "r335_enabled_assignment_status";
    public static final String FORMAT = "R335_ENABLED_ASSIGNMENTS_V1";
    public static final int MAX_EVENTS = 512;
    private static final String ZERO = "0000000000000000000000000000000000000000000000000000000000000000";
    private EnabledWindowJournalR335() {}

    public static final class Event {
        public final long sequence, wallMs, elapsedMs;
        public final String processEpoch, sourceCommit, previousHash, hash, assignment;
        public final boolean enabled;
        private Event(long sequence, long wall, long elapsed, String process, String assignment,
                      String source, String previous, String hash) {
            this.sequence=sequence;wallMs=wall;elapsedMs=elapsed;processEpoch=process;
            this.assignment=assignment;enabled="1".equals(assignment);sourceCommit=source;previousHash=previous;this.hash=hash;
        }
        private String body() {
            return sequence+"|"+wallMs+"|"+elapsedMs+"|"+processEpoch+"|"+assignment
                    +"|"+sourceCommit+"|"+previousHash;
        }
        public String line() { return body()+"|"+hash; }
    }
    public static final class Journal {
        public final long dropped;
        public final String prefixHash;
        public final List<Event> events;
        private Journal(long dropped,String prefix,List<Event> events) {
            this.dropped=dropped;prefixHash=prefix;
            this.events=Collections.unmodifiableList(new ArrayList<Event>(events));
        }
        public String encode() {
            StringBuilder b=new StringBuilder(FORMAT+"|"+dropped+"|"+prefixHash+"\n");
            for(Event e:events)b.append(e.line()).append('\n');
            return b.toString();
        }
    }
    /**
     * Android's indented shared-preference XML can indent the closing string tag after the
     * journal's own final LF. Remove precisely the four serializer spaces following that LF.
     * A missing LF, modified event, or other suffix remains subject to the strict journal parser.
     */
    static String preferenceText(String value) {
        return value != null && value.endsWith("\n    ") ? value.substring(0, value.length() - 4) : value;
    }

    public static Journal parse(String value) {
        if(value==null||value.isEmpty())return new Journal(0,ZERO,new ArrayList<Event>());
        require(value.length()<=200000,"JOURNAL_BYTE_BUDGET");
        String[] lines=value.split("\n",-1);
        require(lines.length>=2&&lines[lines.length-1].isEmpty(),"JOURNAL_TRUNCATED");
        String[] h=lines[0].split("\\|",-1);
        require(h.length==3&&FORMAT.equals(h[0])&&sha(h[2]),"JOURNAL_HEADER");
        long dropped=number(h[1]);require(dropped>=0,"JOURNAL_DROPPED");
        require(dropped!=0||ZERO.equals(h[2]),"JOURNAL_PREFIX");
        ArrayList<Event> events=new ArrayList<Event>();String previous=h[2];long seq=dropped,wall=0;
        for(int i=1;i<lines.length-1;i++) {
            String[] p=lines[i].split("\\|",-1);
            require(p.length==8,"JOURNAL_EVENT_FIELDS");
            long next=number(p[0]),at=number(p[1]),elapsed=number(p[2]);
            require(seq<Long.MAX_VALUE&&next==seq+1&&at>0&&at>=wall&&elapsed>=0,"JOURNAL_ORDER");
            require(p[3].matches("[0-9a-f]{32}")&&(p[4].equals("0")||p[4].equals("1")||p[4].equals("R"))
                    &&p[5].matches("[0-9a-f]{40}")&&previous.equals(p[6])&&sha(p[7]),"JOURNAL_IDENTITY");
            Event e=new Event(next,at,elapsed,p[3],p[4],p[5],p[6],p[7]);
            require(digest(e.body()).equals(e.hash),"JOURNAL_HASH");
            if(!events.isEmpty()) {
                Event old=events.get(events.size()-1);
                if(old.processEpoch.equals(e.processEpoch)) {
                    require(e.elapsedMs>=old.elapsedMs,"JOURNAL_ELAPSED_REVERSED");
                    long wd=e.wallMs-old.wallMs,md=e.elapsedMs-old.elapsedMs;
                    require(Math.abs(wd-md)<=2000,"JOURNAL_CLOCK_DISCONTINUITY");
                }
            }
            events.add(e);require(events.size()<=MAX_EVENTS,"JOURNAL_EVENT_BUDGET");
            seq=next;wall=at;previous=e.hash;
        }
        require(dropped==0||!events.isEmpty(),"JOURNAL_EMPTY_ROTATION");
        return new Journal(dropped,h[2],events);
    }
    public static String append(String original,boolean enabled,long wall,long elapsed,String process,String source) {
        return appendState(original,enabled?"1":"0",wall,elapsed,process,source);
    }
    /** R means an attempted enable was refused; it is not evidence of an explicit user stop. */
    public static String appendState(String original,String assignment,long wall,long elapsed,String process,String source) {
        require("0".equals(assignment)||"1".equals(assignment)||"R".equals(assignment),"JOURNAL_ASSIGNMENT");
        Journal old=parse(original);long seq=old.events.isEmpty()?old.dropped:old.events.get(old.events.size()-1).sequence;
        require(seq<Long.MAX_VALUE&&wall>0&&elapsed>=0&&process!=null&&process.matches("[0-9a-f]{32}")
                &&source!=null&&source.matches("[0-9a-f]{40}"),"JOURNAL_APPEND_IDENTITY");
        String previous=old.events.isEmpty()?old.prefixHash:old.events.get(old.events.size()-1).hash;
        Event provisional=new Event(seq+1,wall,elapsed,process,assignment,source,previous,"");
        Event event=new Event(seq+1,wall,elapsed,process,assignment,source,previous,digest(provisional.body()));
        ArrayList<Event> all=new ArrayList<Event>(old.events);all.add(event);
        // Rotation is explicit and preserves the removed prefix digest. A verifier must find its
        // pre-window witness in retained originals; otherwise the interval remains UNKNOWN.
        long dropped=old.dropped;String prefix=old.prefixHash;
        if(all.size()>MAX_EVENTS) { Event removed=all.remove(0);dropped=removed.sequence;prefix=removed.hash; }
        String encoded=new Journal(dropped,prefix,all).encode();parse(encoded);return encoded;
    }
    private static long number(String value) {
        require(value!=null&&value.matches("0|[1-9][0-9]*"),"JOURNAL_INTEGER");
        try{return Long.parseLong(value);}catch(NumberFormatException e){throw new IllegalArgumentException("JOURNAL_INTEGER_RANGE",e);}
    }
    private static boolean sha(String value){return value!=null&&value.matches("[0-9a-f]{64}");}
    private static void require(boolean ok,String reason){if(!ok)throw new IllegalArgumentException(reason);}
    private static String digest(String value) {
        try {
            byte[] bytes=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            char[] chars=new char[bytes.length*2];char[] digits="0123456789abcdef".toCharArray();
            for(int i=0;i<bytes.length;i++){chars[i*2]=digits[(bytes[i]&255)>>>4];chars[i*2+1]=digits[bytes[i]&15];}
            return new String(chars);
        }catch(java.security.NoSuchAlgorithmException impossible){throw new AssertionError(impossible);}
    }
}
