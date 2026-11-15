package com.ethk3.novaonepro;
import android.content.Context;
import android.content.Intent;
import com.ethk3.novaonepro.r952.*;
/** Independent manifest route sharing the read-only evidence view lifecycle. */
public final class AtomQualityActivity extends DeepLearningActivity {
    @Override protected String pageTitle(){return "原子与分子质量";}
    @Override protected DeepLearningPresenterR952 createPresenter(){return new AtomQualityPresenterR952(this);}
    public static Intent createIntent(Context context,String game,String candidate){return new Intent(context,AtomQualityActivity.class).putExtra("game",game).putExtra("candidate",candidate);}
}
