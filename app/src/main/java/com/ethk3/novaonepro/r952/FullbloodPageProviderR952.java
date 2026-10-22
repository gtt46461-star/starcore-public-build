package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;

public final class FullbloodPageProviderR952 {
    private FullbloodPageProviderR952(){}
    public static String render(DbStore store){return "STARCORE 5.4.2\n"+BatchBuildIdentityR952.canonical()+"\n"+FullbloodDiagnosticsProviderR952.render(store);}
    public static android.content.Intent intent(android.content.Context context,String page,String snapshot,String candidate){
        android.content.Intent intent;String game=com.ethk3.novaonepro.V51Repository.GAME_CODE;
        if("deep".equals(page))intent=com.ethk3.novaonepro.DeepLearningActivity.createIntent(context,game,candidate);
        else if("evolution".equals(page))intent=com.ethk3.novaonepro.EvolutionActivity.createIntent(context,game,candidate);
        else if("creation".equals(page))intent=com.ethk3.novaonepro.CreationActivity.createIntent(context,game,candidate);
        else if("atoms".equals(page))intent=com.ethk3.novaonepro.AtomQualityActivity.createIntent(context,game,candidate);
        else throw new IllegalArgumentException("PAGE_ROUTE_MISSING:"+page);
        return intent.putExtra("snapshot",snapshot==null?"":snapshot);
    }
}
