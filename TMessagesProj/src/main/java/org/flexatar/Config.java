package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class Config {
    public static long authBotId = 6818271084L;
    public static final boolean debugMode = true;

    public static final String version = "pre-historic";
    private static final String PREF_STORAGE_NAME = "flexatar_config_pref";
    private static final String TOKEN_FIELD = "token";
    private static final String VERIFY_FIELD = "verify";
    private static final String STORAGE_FIELD = "storage";
    private static final String STAT_FIELD = "stat";

    public static CountDownLatch stopRecordingAudioSemaphore = null;
    public static Runnable sendAudioCallback = null;
    public static Runnable chooseFlexatarForAudioCallback = null;

    public static CountDownLatch startSendFlexatarRoundSemaphore = null;
    public static boolean chosenAudioWithFlexatar;
    public static boolean chosenSendFlexatarRoundVideo;
    public static boolean sendFlexatarRoundVideoCanceled;

    public static void runAudioCallback(){
        if (sendAudioCallback != null) sendAudioCallback.run();
    }
    public static void runChooseFlexatarForAudioCallback(){
        if (chooseFlexatarForAudioCallback != null) chooseFlexatarForAudioCallback.run();
    }
    public static void signalRecordAudioSemaphore(){
        if (Config.stopRecordingAudioSemaphore !=null)
            Config.stopRecordingAudioSemaphore.countDown();
    }
    public static void signalSendFlexatarSemaphore(){
        if (Config.startSendFlexatarRoundSemaphore !=null)
            Config.startSendFlexatarRoundSemaphore.countDown();
    }

    public static String token = null;
    public static String verify = null;
    public static String storage = null;
    public static String stat = null;
    private static final Object mutex = new Object();
    public static void reset(){
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    private static boolean verifyInProgress = false;
    private static final Object verifyMutex = new Object();
    public static void init(){
        synchronized (verifyMutex) {
            if (verifyInProgress) return;
            verifyInProgress = true;
        }

//        reset();
//        FlexatarStorageManager.clearStorage();
        addDefaultFlexatars();
        loadConfig();
        if (token!=null) return;
        startVerifyRequest();
    }
    public static boolean isVerified(){
        return token!=null;
    }
    public static void addDefaultFlexatars(){
        if (FlexatarUI.chosenFirst !=null) return;
        String[] flxFileNames = { "char6t", "char7t"};
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName+".p";

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,flxRaw,fName,"builtin_");

        }
        File[] flexatarFiles = FlexatarStorageManager.getFlexatarFileList(ApplicationLoader.applicationContext);
//        if (FlexatarUI.chosenFirst == null){
            FlexatarUI.chosenFirst = flexatarFiles[0];
            FlexatarUI.chosenSecond = flexatarFiles[1];
//        }
        Log.d("FLX_INJECT","FlexatarUI.chosenFirst" + FlexatarUI.chosenFirst);
        Log.d("FLX_INJECT","FlexatarUI.chosenSecond " + FlexatarUI.chosenSecond);
        FlexatarRenderer.currentFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenFirst)));
        FlexatarRenderer.altFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenSecond)));

    }
    private static void startVerifyRequest(){
        FlexatarServerAccess.lambdaVerify(new FlexatarServerAccess.VerifyListener() {
            @Override
            public void onVerifyAnswer(String token1, String verifyUrl, String storageUrl, String statUrl) {
                synchronized (mutex) {
                    String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

                    SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
//                    storageUrl = "https://mhpblvwwrb.execute-api.us-east-1.amazonaws.com/test1/";
                    if (token1 != null) editor.putString(TOKEN_FIELD, token1);
                    if (verifyUrl != null) editor.putString(VERIFY_FIELD, verifyUrl);
                    if (storageUrl != null) editor.putString(STORAGE_FIELD, storageUrl);
                    if (statUrl != null) editor.putString(STAT_FIELD, statUrl);
                    token = token1;
                    verify = verifyUrl;
                    storage = storageUrl;
                    stat = statUrl;
                    editor.apply();
                    synchronized (verifyMutex) {
                        verifyInProgress = false;
                    }
                }
            }
            @Override
            public void onError(){
                Log.d("FLX_INJECT","verify failed restart");
                synchronized (verifyMutex) {
                    verifyInProgress = false;
                }
                startVerifyRequest();
            }
        });
    }

    private static long oldUserId = -1;
    private static void loadConfig(){
        synchronized (mutex) {
            long currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
            if (currentUserId == oldUserId && token!=null) {


                synchronized (verifyMutex) {
                    verifyInProgress = false;
                }
                return;
            }
            oldUserId = currentUserId;
            String storageName = PREF_STORAGE_NAME + currentUserId;
            SharedPreferences sharedPreferences = ApplicationLoader.applicationContext.getSharedPreferences(storageName, Context.MODE_PRIVATE);
            token = sharedPreferences.getString(TOKEN_FIELD, null);
            verify = sharedPreferences.getString(VERIFY_FIELD, null);
            storage = sharedPreferences.getString(STORAGE_FIELD, null);
            stat = sharedPreferences.getString(STAT_FIELD, null);
            Log.d("FLX_INJECT","init flexatar config");
            OpusToAacConverter.testConverter();
            synchronized (verifyMutex) {
                if (token!=null)
                    verifyInProgress = false;
            }
        }
    }

    private interface AuthBotUserObtainedListener{
        void userReady(TLRPC.User user);
    }

    private static void getAuthBotUser(AuthBotUserObtainedListener listener) {
        AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
        MessagesController messageController = accountInstance.getMessagesController();
        TLRPC.User user = messageController.getUser(authBotId);
        if (user!=null){
            listener.userReady(user);
            return;
        }
        final MessagesStorage messagesStorage = accountInstance.getMessagesStorage();

        messagesStorage.getStorageQueue().postRunnable(() -> {
            TLRPC.User user1 = messagesStorage.getUser(authBotId);


            messageController.putUser(user1, true);
            listener.userReady(user1);
        });
    }
    private static final int  VERIFY_TIMEOUT_SECONDS = 10;
    private static final String VERIFY_COMMAND = "/verify";
    public interface BotAuthCompletionListener{
        void onReady(String token);
        void onFail();
    }
    public static void botAuth(BotAuthCompletionListener completionListener){
        ScheduledExecutorService timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        AtomicReference timeoutFutureRef = new AtomicReference<ScheduledFuture<?>>();

        NotificationCenter.NotificationCenterDelegate observer = new NotificationCenter.NotificationCenterDelegate() {
            @Override
            public void didReceivedNotification(int id, int account, Object... args) {
                if (id == NotificationCenter.didReceiveNewMessages){
//                    Log.d("FLX_INJECT","didReceiveNewMessages " + id);
                    long did = (Long) args[0];

                    ArrayList<MessageObject> arr = (ArrayList<MessageObject>) args[1];
                    for (int a = 0, N = arr.size(); a < N; a++) {
                        MessageObject messageObject = arr.get(a);
//                        if ( !messageObject.isOut()) {
//                            Log.d("FLX_INJECT", "msg text " + messageObject.messageText);
//                            Log.d("FLX_INJECT", "account " + did);
//                        }
                        if (!messageObject.isOut() && did == authBotId){
                            Log.d("FLX_INJECT", "auth observer removed");
                            ((ScheduledFuture<?>)timeoutFutureRef.get()).cancel(false);
                            String token = (String) messageObject.messageText;
                            AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
                            completionListener.onReady(token);
                            AndroidUtilities.runOnUIThread(()-> {
                                accountInstance.getNotificationCenter().removeObserver(this, NotificationCenter.didReceiveNewMessages);

                            });
                        }
                    }
                }
            }
        };
        ScheduledFuture<?> timeoutFuture = timeoutExecutor.schedule(() -> {
            AndroidUtilities.runOnUIThread(()-> {
                AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
                accountInstance.getNotificationCenter().removeObserver(observer, NotificationCenter.didReceiveNewMessages);
            });
            completionListener.onFail();
            Log.d("FLX_INJECT", "observer removed by timeout " );
            timeoutExecutor.shutdown();
        }, VERIFY_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        timeoutFutureRef.set(timeoutFuture);

        getAuthBotUser(user->{
//            Log.d("FLX_INJECT","usr obtained " +user);
            AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
            AndroidUtilities.runOnUIThread(()-> {
                accountInstance.getNotificationCenter().addObserver(observer, NotificationCenter.didReceiveNewMessages);
                accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(VERIFY_COMMAND,authBotId , null, null, null, false, null, null, null, true, 0, null, false));

            });
        });


    }
}
