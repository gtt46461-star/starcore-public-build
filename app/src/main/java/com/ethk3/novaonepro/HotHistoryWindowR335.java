package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Calendar coverage for normal history recovery. Only the live fetch owner can advance the upper bound. */
final class HotHistoryWindowR335 {
    final String firstIssue;
    final String lastIssue;
    final List<String> dates;
    final int missingPeriods;

    private HotHistoryWindowR335(String first, String last, List<String> dates, int missing) {
        firstIssue = first;
        lastIssue = last;
        this.dates = Collections.unmodifiableList(dates);
        missingPeriods = missing;
    }

    static HotHistoryWindowR335 read(SQLiteDatabase db, String last) {
        long end = TargetIssueResolver.sequenceOrdinalSU1(last);
        long begin = Math.max(1L, end - AppConfig.MAX_LOCAL_HISTORY_SAVE + 1L);
        String first = TargetIssueResolver.fromSequenceOrdinalSU1(begin);
        Map<String,Integer> counts = new HashMap<>();
        try (Cursor c = db.rawQuery("SELECT substr(issue,1,8),count(*) FROM draws "
                + "WHERE dirty=0 AND issue BETWEEN ? AND ? AND length(issue)=12 "
                + "AND substr(issue,9,4) GLOB '[0-9][0-9][0-9][0-9]' "
                + "AND substr(issue,9,4) BETWEEN '0001' AND '1440' GROUP BY substr(issue,1,8)",
                new String[]{first,last})) {
            while (c.moveToNext()) counts.put(c.getString(0),c.getInt(1));
        }
        return coverage(begin,end,first,last,counts);
    }

    static HotHistoryWindowR335 coverage(long begin, long end, String first, String last, Map<String,Integer> counts) {
        List<String> missingDates = new ArrayList<>();
        int missing = 0;
        for (long through=end; through>=begin;) {
            String issue = TargetIssueResolver.fromSequenceOrdinalSU1(through);
            String day = issue.substring(0,8);
            long from = Math.max(begin,through-Integer.parseInt(issue.substring(8))+1);
            int deficit = (int)(through-from+1) - (counts.containsKey(day) ? counts.get(day) : 0);
            if (deficit>0) {
                missing += deficit;
                missingDates.add(day.substring(0,4)+"-"+day.substring(4,6)+"-"+day.substring(6,8));
            }
            through=from-1;
        }
        return new HotHistoryWindowR335(first,last,missingDates,missing);
    }

    List<Models.Draw> inRange(List<Models.Draw> page) {
        List<Models.Draw> accepted = new ArrayList<>();
        for (Models.Draw draw : page) {
            if (draw!=null && draw.issue!=null && draw.issue.compareTo(firstIssue)>=0 && draw.issue.compareTo(lastIssue)<=0)
                accepted.add(draw);
        }
        return accepted;
    }
}
