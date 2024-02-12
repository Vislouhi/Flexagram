package org.flexatar;

import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.GcmPushListenerService;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlexatarServiceAuth {
    private static final String VERIFY_COMMAND = "/verify";

    public interface VerifyListener{
        void onReady(String verifyJson);

    }
    public static void auth(){
        AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
        GcmPushListenerService.verifyServiceListener = json -> {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                try {
                    JSONObject verifyInfo = new JSONObject(json);
                    FlexatarServerAccess.verify(verifyInfo.getString("route") + "/verify", verifyInfo.getString("token"));
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            });
        };

        AndroidUtilities.runOnUIThread(()-> {
//                accountInstance.getNotificationCenter().addObserver(observer, NotificationCenter.didReceiveNewMessages);
            accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(VERIFY_COMMAND+" "+ SharedConfig.pushString,Config.authBotId , null, null, null, false, null, null, null, true, 0, null, false));

        });

    }
}
