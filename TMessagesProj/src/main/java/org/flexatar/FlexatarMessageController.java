package org.flexatar;

import android.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

public class FlexatarMessageController extends BaseController implements NotificationCenter.NotificationCenterDelegate{
    private static FlexatarMessageController[] Instance = new FlexatarMessageController[UserConfig.MAX_ACCOUNT_COUNT];

    public FlexatarMessageController(int num) {
        super(num);
        AndroidUtilities.runOnUIThread(() -> {
            getNotificationCenter().addObserver(FlexatarMessageController.this, NotificationCenter.didReceiveNewMessages);
        });
    }

    public static FlexatarMessageController getInstance(int num){
        FlexatarMessageController localInstance = Instance[num];
        if (localInstance == null) {
            synchronized (DownloadController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FlexatarMessageController(num);
                }
            }
        }
        return localInstance;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            long did = (Long) args[0];
            if (did != Config.authBotId){
                Log.d("FLX_INJECT","msg not from bot, ignoring");
                return;
            }
            Log.d("FLX_INJECT","msg from bot");
            MessageObject messageObject;
            ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
            for (int a = 0; a < arr.size(); a++) {
                messageObject = arr.get(a);
                for (int i = 0; i < messageObject.messageOwner.entities.size(); i++) {
                    TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(i);
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        String targetUrl = entity.url;
                        Log.d("FLX_INJECT", "received flexatar message " + targetUrl);
                    }
                }

            }
        }
    }
}
