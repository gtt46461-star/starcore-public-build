package com.ethk3.novaonepro;
import android.content.Context;
import android.content.Intent;
import com.ethk3.novaonepro.r952.*;
/** Independent manifest route sharing the read-only evidence view lifecycle. */
public final class CreationActivity extends DeepLearningActivity {
    @Override protected String pageTitle(){return "创造能力与可执行算法";}
    @Override protected DeepLearningPresenterR952 createPresenter(){return new CreationPresenterR952(this);}
    public static Intent createIntent(Context context,String game,String candidate){return new Intent(context,CreationActivity.class).putExtra("game",game).putExtra("candidate",candidate);}
}
