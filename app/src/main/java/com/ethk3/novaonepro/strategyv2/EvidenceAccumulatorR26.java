package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/** P1 folds immutable frozen/settled calendars before raw retention, keeping only bounded sufficient statistics. */
public final class EvidenceAccumulatorR26 {
    private EvidenceAccumulatorR26() {}
    public static final int BLOCK=720;
    private static final class Row {int state;boolean hit;double phat;long frozen,deadline,settled;}
    private static Map<Long,Row> rows(SQLiteDatabase db,String policy,long from,long to){
        Map<Long,Row> result=new HashMap<>();
        try(Cursor c=db.rawQuery("SELECT sequence_ordinal,act,hit,phat,frozen_at,target_open_time,settled_at FROM su2_live_decision WHERE genome_key=? AND sequence_ordinal>=? AND sequence_ordinal<? ORDER BY sequence_ordinal",new String[]{policy,String.valueOf(from),String.valueOf(to)})){
            while(c.moveToNext()){Row r=new Row();r.frozen=c.getLong(4);r.deadline=c.getLong(5);r.settled=c.isNull(6)?0:c.getLong(6);r.state=r.frozen>=r.deadline?0:c.isNull(2)?1:c.getInt(1)==1?3:2;r.hit=!c.isNull(2)&&c.getInt(2)==1;r.phat=c.isNull(3)?Double.NaN:c.getDouble(3);if(r.state==3&&(!Double.isFinite(r.phat)||r.phat<0||r.phat>1))r.state=0;result.put(c.getLong(0),r);}
        }
        return result;
    }
    public static long stepOne(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget,long nextTarget)throws IOException {
        String policy=null,benchmark=null;long birth=0,through=0;byte[] partial=null;String chain="";
        try(Cursor c=store.readable().rawQuery("SELECT q.genome_key,q.incumbent,q.from_sequence,coalesce(a.through_ordinal,q.from_sequence-1),a.partial,a.source_chain FROM r26_cohort q LEFT JOIN r26_evidence_cursor a ON a.policy_hash=q.genome_key WHERE q.protocol=? AND coalesce(a.through_ordinal,q.from_sequence-1)<? ORDER BY coalesce(a.through_ordinal,q.from_sequence-1),q.genome_key LIMIT 1",new String[]{QualificationWindowR26.PROTOCOL,String.valueOf(nextTarget-1)})){
            if(c.moveToFirst()){policy=c.getString(0);benchmark=c.getString(1);birth=c.getLong(2);through=c.getLong(3);partial=c.isNull(4)?null:c.getBlob(4);chain=c.isNull(5)?"":c.getString(5);}
        }
        if(policy==null)return 0;final String key=policy,incumbent=benchmark;final long born=birth,oldThrough=through;EvidenceBlockR26 block=partial==null?new EvidenceBlockR26():EvidenceBlockR26.read(partial);
        if(block.calendar!=(through-birth+1)%BLOCK)throw new IOException("R26_EVIDENCE_CURSOR_SHAPE");
        long end=Math.min(nextTarget,Math.min(through+129,birth+((through-birth+1)/BLOCK+1)*BLOCK));
        Map<Long,Row> me=rows(store.readable(),policy,through+1,end),inc=benchmark.isEmpty()?Collections.emptyMap():rows(store.readable(),benchmark,through+1,end);int processed=0;
        for(long ordinal=through+1;ordinal<end;ordinal++){
            try{budget.checkpointOrYield();}catch(BudgetTokenSU1.Yield yield){if(processed==0)throw yield;break;}
            Row a=me.get(ordinal),b=inc.get(ordinal);
            // Revealed but not yet settled remains PENDING; it may be retried after the terminal owner commits.
            if(a!=null&&a.state==1||b!=null&&b.state==1)break;
            block.add(a==null?0:a.state,a!=null&&a.hit,a==null?Double.NaN:a.phat,b==null?0:b.state,b!=null&&b.hit);
            chain=HashingSU1.fields("R26_SETTLED_CALENDAR_FOLD_V1",chain,key,incumbent,String.valueOf(ordinal),a==null?"MISSING":a.state+":"+a.hit+":"+Double.toHexString(a.phat)+":"+a.frozen+":"+a.deadline+":"+a.settled,b==null?"MISSING":b.state+":"+b.hit+":"+b.frozen+":"+b.deadline+":"+b.settled);through=ordinal;processed++;
        }
        if(processed==0){claim.reason("EVIDENCE_WAITING_TERMINAL_SETTLEMENT");return 0;}
        final long completed=through,index=(through-birth)/BLOCK;final byte[] state=block.bytes(),empty=new EvidenceBlockR26().bytes();final boolean sealed=block.calendar==BLOCK;final String sourceChain=chain,hash=LiveStateR26.hash(state);
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_EVIDENCE_FOLD",db->{RuntimeCoordinatorR26.requireFence(db,claim);
            db.execSQL("INSERT OR IGNORE INTO r26_evidence_cursor VALUES(?,?,?,X'',?)",new Object[]{key,QualificationWindowR26.PROTOCOL,born-1,""});
            db.execSQL("UPDATE r26_evidence_cursor SET through_ordinal=?,partial=?,source_chain=? WHERE policy_hash=? AND protocol=? AND through_ordinal=?",new Object[]{completed,sealed?empty:state,sourceChain,key,QualificationWindowR26.PROTOCOL,oldThrough});
            try(Cursor c=db.rawQuery("SELECT changes()",null)){c.moveToFirst();if(c.getInt(0)!=1)throw new IllegalStateException("R26_EVIDENCE_FOLD_CAS");}
            if(sealed)db.execSQL("INSERT INTO r26_evidence_block VALUES(?,?,?,?,?,?)",new Object[]{key,index,born+index*BLOCK,born+(index+1)*BLOCK,state,HashingSU1.fields("R26_BLOCK_COMMIT_V1",hash,sourceChain)});
            db.execSQL("DELETE FROM r26_evidence_block WHERE policy_hash=? AND block_index<?",new Object[]{key,index-120});return 0;});return processed;
    }
    public static final class Window {
        public final EvidenceBlockR26[] quarters={new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26()};
        public final EvidenceBlockR26 fit=new EvidenceBlockR26(),validation=new EvidenceBlockR26();public final String digest;
        Window(String digest){this.digest=digest;}
    }
    public static Window read(SQLiteDatabase db,String policy,QualificationWindowR26.Window window)throws IOException {
        List<EvidenceBlockR26> blocks=new ArrayList<>();StringBuilder hashes=new StringBuilder(QualificationWindowR26.PROTOCOL).append('|').append(policy).append('|').append(window.from).append('|').append(window.to);long cursor=window.from;
        try(Cursor c=db.rawQuery("SELECT from_ordinal,to_ordinal,statistics,block_hash FROM r26_evidence_block WHERE policy_hash=? AND from_ordinal>=? AND to_ordinal<=? ORDER BY from_ordinal",new String[]{policy,String.valueOf(window.from),String.valueOf(window.to)})){
            while(c.moveToNext()){if(c.getLong(0)!=cursor||c.getLong(1)-cursor!=BLOCK)return null;EvidenceBlockR26 block=EvidenceBlockR26.read(c.getBlob(2));if(block.calendar!=BLOCK)throw new IOException("R26_EVIDENCE_SEALED_BLOCK_SHAPE");blocks.add(block);hashes.append('|').append(c.getString(3));cursor=c.getLong(1);}
        }
        if(cursor!=window.to||blocks.size()!=QualificationWindowR26.LENGTH/BLOCK)return null;
        Window result=new Window(PolicyIdentityR26.sha256(hashes.toString()));int quarter=blocks.size()/4;
        for(int i=0;i<blocks.size();i++){result.quarters[i/quarter].merge(blocks.get(i));if(i<quarter*3)result.fit.merge(blocks.get(i));else result.validation.merge(blocks.get(i));}return result;
    }
}
