package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

public class VersionController {
    private static VersionController[] Instance = new VersionController[UserConfig.MAX_ACCOUNT_COUNT];
    public static VersionController getInstance(int num){
        VersionController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (VersionController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new VersionController();
                }
            }
        }
        return localInstance;
    }
    private static final String PREF_STORAGE_NAME = "flexatar_storage_version";

    public synchronized void setVersion(long tgId){
        int buildVersion = SharedConfig.buildVersion() / 10;
        Log.d("FLX_INJECT","buildVersion "+buildVersion);
        Context context = ApplicationLoader.applicationContext;
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(""+tgId, buildVersion);
        editor.apply();
        version = buildVersion;
    }
    private int version = -1;
    public synchronized int getVersion(long tgId){
        if (version!=-1) return version;
        Context context = ApplicationLoader.applicationContext;
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        version = sharedPreferences.getInt(""+tgId,-1);
        return version;
    }
    public boolean isVersionChanged(int currentAccount){
        int flxVersion = VersionController.getInstance(currentAccount).getVersion(UserConfig.getInstance(currentAccount).getClientUserId());
        if (flxVersion==-1){
            VersionController.getInstance(currentAccount).setVersion(UserConfig.getInstance(currentAccount).getClientUserId());
            return false;
        }
        int buildVersion = SharedConfig.buildVersion() / 10;
        return flxVersion!=buildVersion;
    }

    public static void updateTokenForNewVersion(int currentAccount,Runnable ready,Runnable error){
        if (UserConfig.getInstance(currentAccount).isClientActivated()) {
            if (VersionController.getInstance(currentAccount).isVersionChanged(currentAccount)){
//                TODO flexatar request new verify token
                Log.d("FLX_INJECT","version of app changed need token update for id "+UserConfig.getInstance(currentAccount).getClientUserId());
                FlexatarServiceAuth.FlexatarVerifyProcess verifyProcess = FlexatarServiceAuth.getVerification(currentAccount);
                FlexatarServerAccess.StdResponse vData = verifyProcess.getVerifyData();
                if (vData!=null && vData.token!=null /*&& vData.isFail()*/) {
                    verifyProcess.renewToken(FlexatarServiceAuth.getVerification(currentAccount).getToken(),ready,error);
                    return;
                }
            }else{
                Log.d("FLX_INJECT","version of app not changed for id "+UserConfig.getInstance(currentAccount).getClientUserId());
                if (ready!=null) ready.run();
                return;
            }
        }
        if (error!=null) error.run();
    }



}
