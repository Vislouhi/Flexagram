package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.flexatar.DataOps.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.GcmPushListenerService;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class FlexatarServiceAuth {
    private static final String VERIFY_COMMAND = "/verify";
    private static final String PREF_STORAGE_NAME = "flexatar_pref_verify";
    private static final String VERIFY_KEY = "verify_response";

    private static long currentUserId = -1;
    public static FlexatarServerAccess.StdResponse verifyData;


    public static void clearVerificationData(){
        verifyData = null;
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + userId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }
    public static void saveVerificationData(FlexatarServerAccess.StdResponse response){


        verifyData = response;
        response.result = null;
        response.ftars = null;
        Log.d("FLX_INJECT","saving verify "+response.toJson().toString());
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + currentUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(VERIFY_KEY, response.toJson().toString());
        editor.apply();
    }
    public static FlexatarServerAccess.StdResponse getVerifyData(){
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        if (userId == currentUserId && verifyData!=null) return verifyData;
        if (loadVerificationData()){
            return verifyData;
        }else{
            return null;
        }
    }
    public static void updateVerifyData(){
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        if (userId == currentUserId && verifyData!=null) return;
        loadVerificationData();
    }
    public static boolean loadVerificationData(){
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String storedString = sharedPreferences.getString(VERIFY_KEY, null);
        Log.d("FLX_INJECT","got stored verify "+storedString);
        if (storedString == null ) {
            verifyData = null;
            return false;
        };
        try {
            verifyData = new FlexatarServerAccess.StdResponse(storedString);
            return true;
        } catch (JSONException | IllegalAccessException e) {
            throw new RuntimeException(e);
//            verifyData = null;
//            return false;

        }
    }

    public interface VerifyListener{
        void onReady(String verifyJson);

    }

    public interface OnAuthListener{
        void onReady();
        void onError();
    }


    public static void auth(OnAuthListener listener){

            AccountInstance accountInstance = AccountInstance.getInstance(UserConfig.selectedAccount);
            currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
            GcmPushListenerService.verifyServiceListener = json -> {
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.execute(() -> {
                    try {
                        FlexatarServerAccess.StdResponse verifyResponse = new FlexatarServerAccess.StdResponse(json);
                        verify(verifyResponse, listener);
                    } catch (JSONException | IllegalAccessException e) {
                        listener.onError();
                    }
                });
            };

            AndroidUtilities.runOnUIThread(() -> {
                Config.getAuthBotUser(new Config.AuthBotUserObtainedListener() {
                    @Override
                    public void userReady(TLRPC.User user) {
                        Log.d("FLX_INJECT","bot user ready "+user);
                        AndroidUtilities.runOnUIThread(() -> {
                            accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(VERIFY_COMMAND + " " + SharedConfig.pushString, Config.authBotId, null, null, null, false, null, null, null, true, 0, null, false));
                        });
                    }
                });

            });
//        }

    }
    public static void verify(FlexatarServerAccess.StdResponse verifyResponse,OnAuthListener listener){

        JSONObject output = new JSONObject();
        try {
            output.put("android_ver",Config.version);
            output.put("ios_ver","");
            output.put("ext_ver","");
            output.put("token","");
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        Data outputData = new Data(output.toString());
        currentUserId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        FlexatarServerAccess.requestJson(
                verifyResponse.route , "verify",
                verifyResponse.token,
                "POST",
                outputData.value,
                "application/json",
                new FlexatarServerAccess.OnRequestJsonReady() {
                    @Override
                    public void onReady(FlexatarServerAccess.StdResponse response) {
                        if (response.isOk()) {
                            listener.onReady();
                        }else{
                            listener.onError();
                        }
                    }

                    @Override
                    public void onError() {
                        listener.onError();
                    }
                }
        );
       /* FlexatarServerAccess.requestJsonInternal(
                verifyResponse.route + "/verify",
                verifyResponse.token,
                "POST",
                outputData.value,
                "application/json",
                new FlexatarServerAccess.OnRequestJsonReady() {
                    @Override
                    public void onReady(FlexatarServerAccess.StdResponse response) {
                        if(response.isRetry()){
                            Log.d("FLX_INJECT","auth response retry");
                            saveVerificationData(response);
                            FlexatarServiceAuth.verify(response,listener);
                        } else if (response.isOk()) {
                            Log.d("FLX_INJECT","auth response ok");


                            listener.onReady();
                        }else{
                            listener.onError();
                        }
                    }

                    @Override
                    public void onError() {
                        listener.onError();
                    }
                }
        );

        FlexatarServerAccess.verify(verifyResponse.route + "/verify", verifyResponse.token, new FlexatarServerAccess.OnVerifyResultListener() {
            @Override
            public void onResult(FlexatarServerAccess.StdResponse response) {
                if(response.isRetry()){
                    Log.d("FLX_INJECT","auth response retry");
                    saveVerificationData(response);
                    FlexatarServiceAuth.verify(response,listener);
                } else if (response.isOk()) {
                    Log.d("FLX_INJECT","auth response ok");


                    listener.onReady();
                }else{
                    listener.onError();
                }
            }

            @Override
            public void onError() {
                listener.onError();
            }
        });*/
    }
    public static AtomicBoolean hasRunAuth = new AtomicBoolean(false);
    List<Integer> atomicList = new CopyOnWriteArrayList<>();
    public static class FlexatarVerifyProcess{
        private final long userId;
        private ScheduledExecutorService executorService;
        private int counter = 0;
        public FlexatarVerifyProcess(long userId){
            Log.d("FLX_INJECT","Start flexatar verify of user "+userId);
            this.userId=userId;
//            clearVerificationData();
            if (loadVerificationData()) return;
            start();
        }
        private void start(){
            executorService = Executors.newScheduledThreadPool(1);

            executorService.scheduleAtFixedRate(()->{
                Log.d("FLX_INJECT", "Attempt to verify of user " + userId);
                if (loadVerificationData()){
                    executorService.shutdown();
                    return;
                }
                auth(new OnAuthListener() {
                    @Override
                    public void onReady() {
                        Log.d("FLX_INJECT", "Success to verify of user " + userId);
                        executorService.shutdown();
                    }

                    @Override
                    public void onError() {
                        Log.d("FLX_INJECT", "Err to verify of user " + userId + "trying again");
                        executorService.shutdown();
                        start();
                    }
                });
                counter+=1;
                if (counter>5){
                    executorService.shutdown();
                }
                executorService.shutdown();
            }, 0, 15, TimeUnit.SECONDS);
        }
        public void stop(){
            if (!executorService.isShutdown())
                executorService.shutdown();
        }
    }
    public static Map<String,FlexatarVerifyProcess> verifyProcesses = new ConcurrentHashMap<>();

    public static void startVerification(int account){
        long userId = UserConfig.getInstance(account).clientUserId;
        verifyProcesses.compute(""+userId, (existingKey, existingValue) -> {

            if (existingValue == null) {
                return new FlexatarVerifyProcess(userId);
            } else {
                return existingValue;
            }
        });
    }
    public static void resetVerification(){

        for (Map.Entry<String,FlexatarVerifyProcess> ent : verifyProcesses.entrySet()){
            ent.getValue().stop();
        }
        verifyProcesses.clear();
    }
    /*public static NotificationCenter.NotificationCenterDelegate authConnectionObserver = new NotificationCenter.NotificationCenterDelegate() {
        @Override
        public void didReceivedNotification(int id, int account, Object... args) {
            if (id == NotificationCenter.didUpdateConnectionState){
                int state = ConnectionsManager.getInstance(account).getConnectionState();
                if (state == 3){
                    long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
                    verifyProcesses.putIfAbsent(""+userId, new FlexatarVerifyProcess(userId));
                }
            }
        }
    };*/
}
