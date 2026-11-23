package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.IssueTimelineWriterR30;
import java.util.Arrays;

/** Fixed-position K3 comparison recorded directly against revealed canonical labels. */
final class BaselineShadowR34 {
    private BaselineShadowR34() {}
    static final String ROLE="S0_BASELINE";
    static final String TABLE="r34_s0_shadow";
    private static final int LOOKBACK=3;
    private static final String HISTORY_SQL="SELECT issue,open_time,n1,draw_hash_v2 FROM draws WHERE dirty=0 AND issue<=? ORDER BY issue DESC LIMIT "+LOOKBACK;

    static void schema(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(game_code TEXT NOT NULL,target_issue TEXT NOT NULL,"
            +"base_issue TEXT NOT NULL,role TEXT NOT NULL CHECK(role='S0_BASELINE'),position INTEGER NOT NULL CHECK(position=1),"
            +"mask INTEGER NOT NULL,input_hash TEXT NOT NULL,numerical_abi TEXT NOT NULL,source_commit TEXT NOT NULL,deadline_wall INTEGER NOT NULL,deadline_elapsed INTEGER NOT NULL,"
            +"frozen_wall INTEGER NOT NULL,frozen_elapsed INTEGER NOT NULL,boot_id TEXT NOT NULL,returned_wall INTEGER,returned_elapsed INTEGER,"
            +"label_hash TEXT,settled_wall INTEGER,hit INTEGER CHECK(hit IN (0,1)),state TEXT NOT NULL,"
            +"PRIMARY KEY(game_code,target_issue))");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r34_s0_frozen_immutable BEFORE UPDATE OF game_code,target_issue,base_issue,role,position,mask,input_hash,numerical_abi,source_commit,deadline_wall,deadline_elapsed,frozen_wall,frozen_elapsed,boot_id "
            +"ON "+TABLE+" BEGIN SELECT RAISE(ABORT,'R34_S0_ORIGINAL_IMMUTABLE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r34_s0_label_immutable BEFORE UPDATE OF label_hash,settled_wall,hit ON "+TABLE+" WHEN OLD.label_hash IS NOT NULL"
            +" BEGIN SELECT RAISE(ABORT,'R34_S0_LABEL_IMMUTABLE'); END");
    }

    private static final class Input {
        final String hash;
        final int mask;
        Input(String hash,int mask){this.hash=hash;this.mask=mask;}
    }

    private static Input input(SQLiteDatabase db,String base) {
        int[] counts=new int[11];
        StringBuilder identity=new StringBuilder(ROLE).append("|FREQ3_POS1_V1");
        long expected=TargetIssueResolver.sequenceOrdinalSU1(base);
        try(Cursor c=db.rawQuery(HISTORY_SQL,new String[]{base})){
            for(int i=0;i<LOOKBACK;i++){
                if(!c.moveToNext()||!TargetIssueResolver.isValid(c.getString(0))
                        ||TargetIssueResolver.sequenceOrdinalSU1(c.getString(0))!=expected-i
                        ||c.getLong(1)<=0L)return null;
                int n=c.getInt(2);
                String hash=c.getString(3);
                if(n<1||n>10||hash==null||!hash.matches("[a-f0-9]{64}"))return null;
                counts[n]++;
                identity.append('|').append(c.getString(0)).append('|').append(c.getLong(1)).append('|').append(hash);
            }
        }
        Integer[] ranked={1,2,3,4,5,6,7,8,9,10};
        Arrays.sort(ranked,(a,b)->counts[a]==counts[b]?Integer.compare(a,b):Integer.compare(counts[b],counts[a]));
        return new Input(V51ChecksumEngine.sha256(identity.toString()),(1<<ranked[0])|(1<<ranked[1])|(1<<ranked[2]));
    }

    private static boolean targetObserved(SQLiteDatabase db,String target) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R811_DRAW_PROVENANCE+" WHERE parsed_issue=? LIMIT 1",
                new String[]{target})){
            return c.moveToFirst();
        }
    }

    static void freeze(DbStore store,Models.Draw base) {
        if(!TargetIssueResolver.isValid(base.issue)||TimeSourceR811.detectSkew().excessive)return;
        final String target=TargetIssueResolver.next(base.issue).targetIssue;
        final long deadline=TargetIssueResolver.predrawDeadlineWallR20261008(base);
        long wall=TimeSourceR811.wallNowMs(),elapsed=TimeSourceR811.monoNowMs();
        if(deadline<=wall)return;
        long deadlineMono=elapsed+(deadline-wall);
        Input prepared=input(store.readable(),base.issue);
        if(prepared==null)return;
        final String boot=IssueTimelineWriterR30.bootId();
        DatabaseCoordinator owner=store.coordinatorR36();
        boolean frozen=owner.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(
                DatabaseCoordinator.WriteClass.LIVE_FREEZE,"R34_S0_FREEZE").withDeadlineMonoMs(deadlineMono),db->{
            if(!DrawCanonicalizerR811.isConfirmedWithinRead(db,base)||targetObserved(db,target))return false;
            Input exact=input(db,base.issue);
            if(exact==null||!prepared.hash.equals(exact.hash)||prepared.mask!=exact.mask)return false;
            long freezeWall=TimeSourceR811.wallNowMs(),freezeElapsed=TimeSourceR811.monoNowMs();
            if(freezeWall>=deadline||freezeElapsed>=deadlineMono||TimeSourceR811.detectSkew().excessive)return false;
            db.execSQL("INSERT OR IGNORE INTO "+TABLE+"(game_code,target_issue,base_issue,role,position,mask,input_hash,numerical_abi,source_commit,"
                        +"deadline_wall,deadline_elapsed,frozen_wall,frozen_elapsed,boot_id,state) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'RETURN_NOT_OBSERVED')",
                    new Object[]{DatabaseCoordinator.GAME_CODE,target,base.issue,ROLE,1,exact.mask,exact.hash,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,BuildConfig.SOURCE_COMMIT_R26,deadline,deadlineMono,freezeWall,freezeElapsed,boot});
            try(Cursor changed=db.rawQuery("SELECT changes()",null)){return changed.moveToFirst()&&changed.getInt(0)==1;}
        });
        if(!frozen)return;
        final long returnedWall=TimeSourceR811.wallNowMs(),returnedElapsed=TimeSourceR811.monoNowMs();
        boolean before=returnedWall<deadline && returnedElapsed<deadlineMono && !TimeSourceR811.detectSkew().excessive;
        owner.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.LIVE_FREEZE,"R34_S0_FREEZE_RETURN"),db->{
            String state=before&&!targetObserved(db,target)?"FROZEN":"DEADLINE_OR_OBSERVATION_CONFLICT";
            db.execSQL("UPDATE "+TABLE+" SET returned_wall=?,returned_elapsed=?,state=? WHERE game_code=? AND target_issue=?"
                +" AND input_hash=? AND boot_id=? AND state='RETURN_NOT_OBSERVED'",
                new Object[]{returnedWall,returnedElapsed,state,DatabaseCoordinator.GAME_CODE,target,prepared.hash,boot});
            return 0;
        });
    }

    static void settleWithin(SQLiteDatabase db,Models.Draw draw,long labelWall) {
        try(Cursor schema=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){
            if(!schema.moveToFirst())return;
        }
        String target=draw.issue;
        try(Cursor row=db.rawQuery("SELECT mask,deadline_wall,returned_wall,state FROM "+TABLE+" WHERE game_code=? AND target_issue=?",
                new String[]{DatabaseCoordinator.GAME_CODE,target})){
            if(!row.moveToFirst())return;
            String state=row.getString(3);
            if(!"FROZEN".equals(state)&&!"RETURN_NOT_OBSERVED".equals(state))return;
            long observed=draw.fetchTime>0?draw.fetchTime:labelWall;
            boolean valid="FROZEN".equals(state)&&row.getLong(2)<row.getLong(1)&&row.getLong(2)<observed;
            Integer hit=valid?((row.getInt(0)>>draw.numbers[0])&1):null;
            db.execSQL("UPDATE "+TABLE+" SET label_hash=?,settled_wall=?,hit=?,state=? WHERE game_code=? AND target_issue=? AND state=?",
                new Object[]{DrawHashR36.sha256V2(draw),labelWall,hit,valid?"SETTLED":"UNVERIFIED_PIT",DatabaseCoordinator.GAME_CODE,target,state});
        }
    }
}
