package com.ethk3.novaonepro;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
public final class DbStore {
    private final SQLiteDatabase db;private final Context app;private final List<Models.Draw> draws;private final DatabaseCoordinator coordinator;
    public DbStore(SQLiteDatabase db,Context app,List<Models.Draw> draws){this.db=db;this.app=app;this.draws=draws;coordinator=new DatabaseCoordinator(db);}
    public SQLiteDatabase readable(){return db;}
    public Context contextForR26(){return app;}
    public DatabaseCoordinator coordinatorForResearchR952(){return coordinator;}
    public Models.Draw getLatestDraw(){return draws.get(draws.size()-1);}
    public List<Models.Draw> getRecentDraws(int count){return new ArrayList<>(draws.subList(Math.max(0,draws.size()-count),draws.size()));}
}
