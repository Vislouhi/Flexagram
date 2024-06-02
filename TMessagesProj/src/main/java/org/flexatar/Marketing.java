package org.flexatar;

import android.util.Log;

import org.telegram.messenger.LocaleController;

public class Marketing {
    public static final String FIRST_FLEXATAR_USAGE_EVENT = "first_flexatar_usage";
    public static final String FIRST_EFFECT_USAGE_EVENT = "first_effect_usage";
    public static void sendEvent(int account,String event){
        String langCode = LocaleController.getInstance().getCurrentLocaleInfo().getLangCode();
        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), "marketing/"+langCode+"/"+event, "POST", null, "application/json", new FlexatarServerAccess.OnRequestJsonReady() {

            @Override
            public void onReady(FlexatarServerAccess.StdResponse response) {
                Log.d("FLX_INJECT","flx marketing request success ");
            }

            @Override
            public void onError() {
                Log.d("FLX_INJECT","flx marketing request fail ");
            }
        });
    }
}
