package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.TargetIssueResolver;
import com.ethk3.novaonepro.V51ChecksumEngine;
import com.ethk3.novaonepro.strategyv2.ExecutionBudgetR26;
import java.util.*;

/** Fixed history identity. The public legacy List API returns defensive Draw copies. */
public final class PITSnapshotContractR952 {
    public final String targetIssue,snapshotHash,firstIssue,lastIssue;
    public final List<Models.Draw> history;
    private PITSnapshotContractR952(String target,ArrayList<Models.Draw> rows,String hash) {
        targetIssue=target;snapshotHash=hash;
        firstIssue=rows.get(rows.size()-1).issue;lastIssue=rows.get(0).issue;
        final int count=rows.size();final String[] issues=new String[count];final long[] opens=new long[count],fetches=new long[count];final byte[] numbers=new byte[count*10];
        for(int i=0;i<count;i++){ExecutionBudgetR26.poll();Models.Draw row=rows.get(i);issues[i]=row.issue;opens[i]=row.openTime;fetches[i]=row.fetchTime;for(int n=0;n<10;n++)numbers[i*10+n]=(byte)row.numbers[n];}
        history=Collections.unmodifiableList(new AbstractList<Models.Draw>() {
            public Models.Draw get(int i){if(i<0||i>=count)throw new IndexOutOfBoundsException();Models.Draw row=new Models.Draw();row.issue=issues[i];row.openTime=opens[i];row.fetchTime=fetches[i];row.sourceLine="";for(int n=0;n<10;n++)row.numbers[n]=numbers[i*10+n];return row;}
            public int size(){return count;}
        });
    }
    public static PITSnapshotContractR952 capture(DbStore store,String target,int lookback) {
        ExecutionBudgetR26.poll();
        if(store==null||!TargetIssueResolver.isValid(target))throw new IllegalArgumentException("R952_PIT_INPUT");
        if(store.getDrawByIssue(target)!=null)throw new IllegalStateException("R952_POST_DRAW_FREEZE_REJECTED");
        ExecutionBudgetR26.poll();
        List<Models.Draw> loaded=store.getDrawsBeforeIssue(target,Math.max(20,Math.min(5000,lookback)));
        ExecutionBudgetR26.poll();
        if(loaded==null)throw new IllegalStateException("R952_PIT_HISTORY_UNAVAILABLE");
        ArrayList<Models.Draw> valid=new ArrayList<Models.Draw>();
        for(Models.Draw row:loaded) {
            ExecutionBudgetR26.poll();
            if(row==null||!row.isValid()||row.dirty!=0)continue;
            if(row.issue.compareTo(target)>=0)throw new IllegalStateException("R952_PIT_SOURCE_NOT_BEFORE_TARGET");
            valid.add(row);
        }
        return prepare(target,valid,null);
    }
    /** Replay checks the original frozen hash; it never asks the current DB whether target is drawn. */
    public static PITSnapshotContractR952 fromFrozenHistory(String target,List<Models.Draw> source,String expectedHash) {
        if(expectedHash==null||!expectedHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("FROZEN_PIT_HASH");
        return prepare(target,source,expectedHash);
    }
    private static PITSnapshotContractR952 prepare(String target,List<Models.Draw> source,String expectedHash) {
        if(!TargetIssueResolver.isValid(target)||source==null||source.size()<20||source.size()>5000)
            throw new IllegalArgumentException("FROZEN_PIT_BOUND");
        ArrayList<Models.Draw> rows=new ArrayList<Models.Draw>(source.size());
        for(Models.Draw sourceRow:source) {
            ExecutionBudgetR26.poll();
            if(sourceRow==null)throw new IllegalArgumentException("FROZEN_PIT_INVALID");
            Models.Draw row=copyDraw(sourceRow);
            if(!row.isValid()||row.dirty!=0||!TargetIssueResolver.isValid(row.issue)||row.issue.compareTo(target)>=0)
                throw new IllegalArgumentException("FROZEN_PIT_INVALID");
            rows.add(row);
        }
        Collections.sort(rows,new Comparator<Models.Draw>(){public int compare(Models.Draw a,Models.Draw b){return b.issue.compareTo(a.issue);}});
        ExecutionBudgetR26.poll();
        StringBuilder text=new StringBuilder("R958_PIT_V2|").append(target);String previous=null;
        for(Models.Draw row:rows){
            ExecutionBudgetR26.poll();
            if(row.issue.equals(previous))throw new IllegalArgumentException("R958_PIT_DUPLICATE_SOURCE");
            text.append('|').append(row.issue).append(':').append(row.numbersText());previous=row.issue;
        }
        String hash=V51ChecksumEngine.sha256(text.toString());
        if(expectedHash!=null&&!hash.equals(expectedHash))throw new IllegalArgumentException("FROZEN_PIT_HASH");
        return new PITSnapshotContractR952(target,rows,hash);
    }
    /** R27.3: the newest run of consecutive draws (at least 20), newest first. A fresh install back-fills history with
     * provider gaps; requiring all 5000 rows to be consecutive failed every SU1 frame (HISTORY_SEQUENCE_GAP) and
     * quarantined incumbent-forecast on device. Deterministic for a given history, so preparation and freeze agree. */
    public PITSnapshotContractR952 newestContiguous() {
        int k=1;
        while(k<history.size()){ExecutionBudgetR26.poll();if(!TargetIssueResolver.exact(history.get(k).issue,history.get(k-1).issue).valid)break;k++;}
        if(k==history.size()||k<20)return this;
        return prepare(targetIssue,new ArrayList<Models.Draw>(history.subList(0,k)),null);
    }
    /** Modern temporal atoms require contiguous support and the immediately previous draw. */
    public void requireConsecutive() {
        if(!TargetIssueResolver.exact(lastIssue,targetIssue).valid)throw new IllegalArgumentException("PIT_TARGET_GAP");
        for(int i=history.size()-1;i>0;i--){ExecutionBudgetR26.poll();
            if(!TargetIssueResolver.exact(history.get(i).issue,history.get(i-1).issue).valid)
                throw new IllegalArgumentException("HISTORY_SEQUENCE_GAP");
        }
    }
    private static Models.Draw copyDraw(Models.Draw source){
        int[] numbers=source.numbers;
        if(numbers==null||numbers.length!=10)throw new IllegalArgumentException("FROZEN_PIT_INVALID");
        Models.Draw copy=new Models.Draw();copy.issue=source.issue;copy.openTime=source.openTime;
        copy.fetchTime=source.fetchTime;copy.dirty=source.dirty;copy.numbers=Arrays.copyOf(numbers,10);
        // Provider rawJson/sourceLine are not PIT features and must not pin unbounded payloads.
        copy.rawJson="";copy.sourceLine="";return copy;
    }
}
