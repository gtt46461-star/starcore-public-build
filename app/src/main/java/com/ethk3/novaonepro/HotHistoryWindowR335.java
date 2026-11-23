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
    final List<String> gapRanges;

    private HotHistoryWindowR335(String first, String last, List<String> dates, int missing,List<String> ranges) {
        firstIssue = first;
        lastIssue = last;
        this.dates = Collections.unmodifiableList(dates);
        missingPeriods = missing;
        gapRanges = Collections.unmodifiableList(ranges);
    }

    static HotHistoryWindowR335 read(DbStore store) {
        Models.Draw head = store.getLatestDraw();
        return head == null || !head.isValid() ? null : read(store.readable(), head.issue);
    }

    static HotHistoryWindowR335 read(SQLiteDatabase db, String last) {
        long end = TargetIssueResolver.sequenceOrdinalSU1(last);
        long begin = Math.max(1L, end - AppConfig.MAX_LOCAL_HISTORY_SAVE + 1L);
        String first = TargetIssueResolver.fromSequenceOrdinalSU1(begin);
        Map<String,Integer> counts = new HashMap<>();
        List<String> ranges=new ArrayList<>();
        long cursor=begin;
        try (Cursor c = db.rawQuery("SELECT issue FROM draws "
                + "WHERE dirty=0 AND issue BETWEEN ? AND ? AND length(issue)=12 ORDER BY issue",
                new String[]{first,last})) {
            while(c.moveToNext()) {
                String issue=c.getString(0);
                if(!TargetIssueResolver.isValid(issue))continue;
                long position=TargetIssueResolver.sequenceOrdinalSU1(issue);
                if(position<cursor||position>end)continue;
                if(position>cursor)ranges.add(range(cursor,position-1));
                cursor=position+1;
                String date=issue.substring(0,8);
                Integer count=counts.get(date);
                counts.put(date,count==null?1:count+1);
            }
        }
        if(cursor<=end)ranges.add(range(cursor,end));
        HotHistoryWindowR335 summary=coverage(begin,end,first,last,counts);
        return new HotHistoryWindowR335(first,last,summary.dates,summary.missingPeriods,ranges);
    }

    private static String range(long from,long through) {
        return TargetIssueResolver.fromSequenceOrdinalSU1(from)+"–"+TargetIssueResolver.fromSequenceOrdinalSU1(through);
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
        return new HotHistoryWindowR335(first,last,missingDates,missing,Collections.<String>emptyList());
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
