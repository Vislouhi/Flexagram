package org.flexatar;

import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BaseController;
import org.telegram.messenger.DownloadController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.LayoutHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

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
            synchronized (FlexatarMessageController.class) {
                localInstance = Instance[num];
                if (localInstance == null) {
                    Instance[num] = localInstance = new FlexatarMessageController(num);
                }
            }
        }
        return localInstance;
    }
    public interface FlexatarAddToGalleryListener{
        void onAddToGallery(File flexatarFile,int flexatarType);
    }
    private FlexatarAddToGalleryListener flexatarAddToGalleryListener;
    public void setOnAddToGalleryListener(FlexatarAddToGalleryListener listener){
        flexatarAddToGalleryListener = listener;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            long did = (Long) args[0];
            Log.d("FLX_INJECT","msg dialog id is "+did);
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
                        ServerDataProc.FlexatarChatCellInfo ftarInfo = ServerDataProc.parseFlexatarCellUrl(targetUrl);
                        if (ftarInfo == null) continue;
//                        TODO for debug purpose
//                        ftarInfo.download = true;
                        if (ftarInfo.ftar!=null && ftarInfo.download){
                            File galleryFile = new File(FlexatarStorageManager.getFlexatarStorage(ApplicationLoader.applicationContext,account),ftarInfo.getFileName());
                            if (!galleryFile.exists()){
                                FlexatarServerAccess.downloadFlexatar(account,null, ftarInfo.ftar, new FlexatarServerAccess.OnReadyOrErrorListener() {
                                    @Override
                                    public void onReady(File flexatarFile,int flexatarType) {
                                        Log.d("FLX_INJECT","Flexatar loaded to gallery");
                                        if (flexatarAddToGalleryListener!=null){
                                            flexatarAddToGalleryListener.onAddToGallery(flexatarFile,flexatarType);
                                        }
                                    }

                                    @Override
                                    public void onError() {
                                        Log.d("FLX_INJECT","Flexatar loaded error");
                                    }
                                });
                            }
                        }
                        if (ftarInfo.verify!=null){
                            FlexatarServiceAuth.getVerification(account).verify(ftarInfo.verify);
                        }
                        Log.d("FLX_INJECT", "received flexatar message " + targetUrl);
                    }
                }

            }
        }
    }
}
