package com.ethk3.novaonepro.r952;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import com.ethk3.novaonepro.AppConfig;
import java.io.File;
import java.util.List;
import java.util.concurrent.*;

/** Async read/cancel/render lifecycle shared by the four evidence presenters. */
public class DeepLearningPresenterR952 implements AutoCloseable {
    public interface View { void render(ResearchPageRepositoryR952.PageResult result); }
    private final ResearchPageRepositoryR952 repository;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler ui=new Handler(Looper.getMainLooper());
    private View view;private long epoch;private CancellationSignal pending;private boolean closed;
    public DeepLearningPresenterR952(Context context){
        final File file=context.getApplicationContext().getDatabasePath(AppConfig.DB_NAME);
        repository=new ResearchPageRepositoryR952(new ResearchPageRepositoryR952.ReadPort(){
            public List<ResearchPageRepositoryR952.Row> read(CancellationSignal cancel){
                if(!file.isFile())throw new IllegalStateException("DB_NOT_READY");
                SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY);
                try{return ResearchPageRepositoryR952.readDatabase(db,cancel);}finally{db.close();}
            }
        });
    }
    protected String page(){return "deep";}
    public void attach(View value){if(closed)throw new IllegalStateException("PRESENTER_CLOSED");view=value;}
    public void load(final String snapshot,final String after,final String filter){
        if(closed||view==null)return;
        if(pending!=null)pending.cancel();
        final CancellationSignal cancel=new CancellationSignal();pending=cancel;final long generation=++epoch;
        final Runnable timeout=new Runnable(){public void run(){cancel.cancel();}};
        ui.postDelayed(timeout,2500L);
        worker.execute(new Runnable(){public void run(){
            final ResearchPageRepositoryR952.PageResult result=repository.load(page(),snapshot,after,filter,30,cancel);
            ui.removeCallbacks(timeout);
            ui.post(new Runnable(){public void run(){if(!closed&&view!=null&&generation==epoch)view.render(result);}});
        }});
    }
    public void detach(){view=null;epoch++;if(pending!=null)pending.cancel();}
    public void close(){detach();closed=true;worker.shutdownNow();ui.removeCallbacksAndMessages(null);}
}
