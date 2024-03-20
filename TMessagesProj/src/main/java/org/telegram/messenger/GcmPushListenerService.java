/*
 * This is the source code of Telegram for Android v. 5.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import androidx.annotation.NonNull;

import com.google.android.exoplayer2.util.Log;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.flexatar.FlexatarServerAccess;
import org.flexatar.FlexatarServiceAuth;
import org.json.JSONException;

import java.util.Arrays;
import java.util.Map;

public class GcmPushListenerService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage message) {
        String from = message.getFrom();
        Map<String, String> data = message.getData();
        long time = message.getSentTime();

        if (BuildVars.LOGS_ENABLED) {
            FileLog.d("FCM received data: " + data + " from: " + from);
        }
//        Log.d("FLX_INJECT","cloud message " + Arrays.toString(data.keySet().toArray()));
        if (data.containsKey("flexatar")){
//            verifyListener.onVerifyAnswer(data.get("flexatar"));
/*            try {
                FlexatarServerAccess.StdResponse response = new FlexatarServerAccess.StdResponse(data.get("flexatar"));
                String key = response.tgid == null ? ""+UserConfig.getInstance(UserConfig.selectedAccount).clientUserId :""+response.tgid;
                if (FlexatarServiceAuth.verifyProcesses.containsKey(key)) {
                    FlexatarServiceAuth.verifyProcesses.get(key).verify(response,null);
//                    FlexatarServiceAuth.verifyProcesses.get(key).verifyListener.onReady(response);
                }
            } catch (JSONException e) {
                Log.d("FLX_INJECT","incorrect flexatar auth gcm message");
            }
//            verifyServiceListener.onReady(data.get("flexatar"));
            Log.d("FLX_INJECT","flexatar cloud message " + data.get("flexatar"));*/
        }else {
            PushListenerController.processRemoteMessage(PushListenerController.PUSH_TYPE_FIREBASE, data.get("p"), time);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        AndroidUtilities.runOnUIThread(() -> {
            if (BuildVars.LOGS_ENABLED) {
                FileLog.d("Refreshed FCM token: " + token);
            }
            ApplicationLoader.postInitApplication();
            PushListenerController.sendRegistrationToServer(PushListenerController.PUSH_TYPE_FIREBASE, token);
        });
    }
}
