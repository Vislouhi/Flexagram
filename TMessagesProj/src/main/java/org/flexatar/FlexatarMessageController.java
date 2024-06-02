package org.flexatar;

import android.util.Log;
import android.view.Gravity;
import android.widget.FrameLayout;

import org.flexatar.DataOps.Data;
import org.json.JSONException;
import org.json.JSONObject;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class FlexatarMessageController extends BaseController implements NotificationCenter.NotificationCenterDelegate{
    private static FlexatarMessageController[] Instance = new FlexatarMessageController[UserConfig.MAX_ACCOUNT_COUNT];

    public FlexatarMessageController(int num) {
        super(num);
        AndroidUtilities.runOnUIThread(() -> {
            getNotificationCenter().addObserver(FlexatarMessageController.this, NotificationCenter.didReceiveNewMessages);
            getNotificationCenter().addObserver(FlexatarMessageController.this, NotificationCenter.replaceMessagesObjects);
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
    public FlexatarAddToGalleryListener flexatarAddToGalleryListener;
    public void setOnAddToGalleryListener(FlexatarAddToGalleryListener listener){
        flexatarAddToGalleryListener = listener;
    }
    private ServerDataProc.FlexatarChatCellInfo processFlxLink(String link,int account){
        ServerDataProc.FlexatarChatCellInfo ftarInfo = ServerDataProc.parseFlexatarCellUrl(link);
//        Log.d("FLX_INJECT","ftarInfo processFlxLink ftar "+ ftarInfo.ftar + "active" + ftarInfo.active);
        if (ftarInfo!=null){
            if (ftarInfo.active!=null && ftarInfo.ftar!=null){
                Log.d("FLX_INJECT","ftar "+ ftarInfo.ftar + "active" + ftarInfo.active);
                if (ftarInfo.active){
                    FlxDownloadTaskQueue.addTask(account,ftarInfo.ftar, FlxDownloadTaskQueue.Action.ADD);
//                    File ftarCacheFile = new File(CacheController.cacheDir(), ServerDataProc.flxFileNameByRoute(ftarInfo.ftar));
                    Log.d("FLX_INJECT","Task add flx "+ftarInfo.ftar);
                }else{
                    FlxDownloadTaskQueue.addTask(account,ftarInfo.ftar, FlxDownloadTaskQueue.Action.DELETE);
                    Log.d("FLX_INJECT","Remove flx by link "+ftarInfo.ftar);
                }
            }
        }
        return  ftarInfo;
    }
    @SuppressWarnings("unchecked")
    @Override
    public void didReceivedNotification(int id, int account, Object... args) {
        if (id == NotificationCenter.didReceiveNewMessages) {
            long did = (Long) args[0];
//            Log.d("FLX_INJECT","msg dialog id is "+did);
            MessageObject messageObject;
            ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
            for (int a = 0; a < arr.size(); a++) {
                messageObject = arr.get(a);
                if (messageObject.isForwarded() && messageObject.isPhoto() && !messageObject.isOut()){

                    if (messageObject.messageOwner.fwd_from.from_id.user_id == Config.authBotId){
                        Log.d("FLX_INJECT","msg forwarded from bot ");
                        for (int i = 0; i < messageObject.messageOwner.entities.size(); i++) {
                            TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(i);
                            if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                                String targetUrl = entity.url;
                                ServerDataProc.FlexatarChatCellInfo ftarInfo = ServerDataProc.parseFlexatarCellUrl(targetUrl);
                                if (ftarInfo!=null && ftarInfo.ftar!=null){
                                    TLRPC.Photo photo = messageObject.messageOwner.media.photo;
                                    String photoId = TgPackFileId.packFileId(photo.id,
                                            photo.dc_id,photo.access_hash,photo.file_reference,"m");
                                    JSONObject body = new JSONObject();
                                    try {
                                        body.put("name",messageObject.caption.toString());
                                        body.put("photoid",photoId);
                                    } catch (JSONException e) {
                                        throw new RuntimeException(e);
                                    }
                                    Data data = new Data(body.toString());
                                    boolean isPrivate = ftarInfo.ftar.startsWith("private");
                                    String type = isPrivate ? "private":"public";
                                    String[] ftarSplit = ftarInfo.ftar.split("/");
                                    String ver;
                                    String tag;
                                    String owner;
                                    String flxId;
                                    ver = ftarSplit[1];
                                    if (isPrivate){
                                        tag = ftarSplit[2];
                                        owner = ftarSplit[3];
                                        flxId = ftarSplit[4];
                                    }else{
                                        tag = "tg";
                                        owner = "0";
                                        flxId = ftarSplit[2];
                                    }
                                    String route = "forward/"+type+"/"+ver+"/"+tag+"/"+owner+"/"+flxId;

                                    FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), route, "POST", data.value, "application/json", new FlexatarServerAccess.OnRequestJsonReady() {

                                        @Override
                                        public void onReady(FlexatarServerAccess.StdResponse response) {
                                            Log.d("FLX_INJECT","flx forward request success ");
                                        }

                                        @Override
                                        public void onError() {
                                            Log.d("FLX_INJECT","flx forward request fail ");
                                        }
                                    });
//                                    Log.d("FLX_INJECT","request body : "+body.toString());
                                }

                            }
                        }
                    }
                }

                /*if (messageObject.messageOwner.media!=null && messageObject.messageOwner.media.photo!=null) {
                    TLRPC.Photo photo = messageObject.messageOwner.media.photo;
                    Log.d("FLX_INJECT","msg photo id "+photo.id);
                    Log.d("FLX_INJECT","msg photo dc id "+photo.dc_id);
                    Log.d("FLX_INJECT","msg photo access_hash "+photo.access_hash);
                    String fileRef = "[";
                    for (byte b : photo.file_reference){
                        int unsignedInt = b & 0xFF;
                        fileRef += unsignedInt+",";
                    }
                    fileRef+="]";
                    Log.d("FLX_INJECT","msg photo file ref "+ fileRef);

                    for (TLRPC.PhotoSize size:photo.sizes){
                        Log.d("FLX_INJECT","msg photo size.type "+ size.type);
                    }
                }*/
            }
            if (did != Config.authBotId){
//                Log.d("FLX_INJECT","msg not from bot, ignoring");
                return;
            }
            Log.d("FLX_INJECT","msg from bot");
            for (int a = 0; a < arr.size(); a++) {
                messageObject = arr.get(a);
                for (int i = 0; i < messageObject.messageOwner.entities.size(); i++) {
                    TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(i);
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        String targetUrl = entity.url;
//                        Log.d("FLX_INJECT","msg targetUrl: "+targetUrl);
                        ServerDataProc.FlexatarChatCellInfo ftarInfo = processFlxLink(targetUrl,account);

//                        ServerDataProc.FlexatarChatCellInfo ftarInfo = ServerDataProc.parseFlexatarCellUrl(targetUrl);
                        if (ftarInfo == null) continue;

                        /*if (ftarInfo.ftar!=null && ftarInfo.download){
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
                        }*/
                        if (ftarInfo.verify!=null){
                            FlexatarServiceAuth.getVerification(account).saveBotToken(ftarInfo.verify);
                            FlexatarServiceAuth.getVerification(account).botLockRelease();
                            FlexatarServiceAuth.getVerification(account).start();
//                            FlexatarServiceAuth.getVerification(account).verify(ftarInfo.verify);
                        }
//                        Log.d("FLX_INJECT", "received flexatar message " + targetUrl);
                    }
                }

            }
        } else if (id ==  NotificationCenter.replaceMessagesObjects) {
            Log.d("FLX_INJECT", "replaceMessagesObjects ");
            long did = (Long) args[0];
            if (did != Config.authBotId){
//                Log.d("FLX_INJECT","msg not from bot, ignoring");
                return;
            }
            MessageObject messageObject;
            ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
            for (int a = 0; a < arr.size(); a++) {
                messageObject = arr.get(a);
                for (int i = 0; i < messageObject.messageOwner.entities.size(); i++) {
                    TLRPC.MessageEntity entity = messageObject.messageOwner.entities.get(i);
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        String targetUrl = entity.url;
                        processFlxLink(targetUrl,account);
                        Log.d("FLX_INJECT", "change msg targetUrl: " + targetUrl);
                    }
                }
            }
            Log.d("FLX_INJECT","bot message changed");
        }
    }
}
