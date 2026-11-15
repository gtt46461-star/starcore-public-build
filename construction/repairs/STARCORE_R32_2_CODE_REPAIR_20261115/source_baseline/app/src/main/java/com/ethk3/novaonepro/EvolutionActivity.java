package com.ethk3.novaonepro;
import android.content.Context;
import android.content.Intent;
import com.ethk3.novaonepro.r952.*;
/** Independent manifest route sharing the read-only evidence view lifecycle. */
public final class EvolutionActivity extends DeepLearningActivity {
    @Override protected String pageTitle(){return "动态迭代、挑战者与证书";}
    @Override protected DeepLearningPresenterR952 createPresenter(){return new EvolutionPresenterR952(this);}
    public static Intent createIntent(Context context,String game,String candidate){return new Intent(context,EvolutionActivity.class).putExtra("game",game).putExtra("candidate",candidate);}
}
