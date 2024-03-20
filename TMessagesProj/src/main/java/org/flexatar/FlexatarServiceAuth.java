package org.flexatar;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AccountInstance;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.ContactsController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.SendMessagesHelper;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import com.google.android.gms.tasks.Task;
import com.google.android.play.core.integrity.IntegrityManagerFactory;
import com.google.android.play.core.integrity.StandardIntegrityManager;

public class FlexatarServiceAuth {
    private static final String VERIFY_COMMAND = "/verify";
    private static final String PREF_STORAGE_NAME = "flexatar_pref_verify";
    private static final String VERIFY_KEY = "verify_response";

    private static long currentUserId = -1;
    public static FlexatarServerAccess.StdResponse verifyData;
    private static StandardIntegrityManager.StandardIntegrityTokenProvider integrityTokenProvider;
    private static CountDownLatch integrityTokenProviderLatch = new CountDownLatch(1);
    public static void integrityCheck(){
        if (integrityTokenProvider!=null) return;
        StandardIntegrityManager standardIntegrityManager =
                IntegrityManagerFactory.createStandard(ApplicationLoader.applicationContext);
        long cloudProjectNumber = 642143043531L;
        standardIntegrityManager.prepareIntegrityToken(
                        StandardIntegrityManager.PrepareIntegrityTokenRequest.builder()
                                .setCloudProjectNumber(cloudProjectNumber)
                                .build())
                .addOnSuccessListener(tokenProvider -> {
                    Log.d("FLX_INJECT","token provider obtained success ");
                    integrityTokenProvider = tokenProvider;
                    integrityTokenProviderLatch.countDown();
//                    getIntegrityToken(integrityTokenProvider);
                })
                .addOnFailureListener(exception -> {
                    integrityTokenProviderLatch.countDown();
                    Log.d("FLX_INJECT","integrity warmup error");
                });
    }
    private interface IntegrityTokenListener{
        void onTokenReady(String token);
    }
    public static void getIntegrityToken(String requestHash,IntegrityTokenListener integrityTokenListener){
//        String requestHash = UUID.randomUUID().toString();
        try {
            integrityTokenProviderLatch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        if (integrityTokenProvider==null){
            integrityTokenListener.onTokenReady(null);
            return;
        }
        Task<StandardIntegrityManager.StandardIntegrityToken> integrityTokenResponse =
                integrityTokenProvider.request(
                        StandardIntegrityManager.StandardIntegrityTokenRequest.builder()
                                .setRequestHash(requestHash)
                                .build());
        integrityTokenResponse
                .addOnSuccessListener(response -> {
                    Log.d("FLX_INJECT","integrity token obtained:"+response.token());
                    integrityTokenListener.onTokenReady(response.token());
                })
                .addOnFailureListener(exception -> {
                    Log.d("FLX_INJECT","integrity token obtained");
                    integrityTokenListener.onTokenReady(null);

                });

    }
//    public static void


    /*public static void clearVerificationData(){
        verifyData = null;
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + userId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();
    }*/
    /*public static void saveVerificationData(FlexatarServerAccess.StdResponse response){


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
    }*/
    /*public static FlexatarServerAccess.StdResponse getVerifyData(){
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        if (userId == currentUserId && verifyData!=null) return verifyData;
        if (loadVerificationData()){
            return verifyData;
        }else{
            return null;
        }
    }*/
    /*public static void updateVerifyData(){
        long userId = UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        if (userId == currentUserId && verifyData!=null) return;
        loadVerificationData();
    }*/
    /*public static boolean loadVerificationData(){
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
        } catch (JSONException e) {
            throw new RuntimeException(e);
//            verifyData = null;
//            return false;

        }
    }*/

   /* public interface VerifyListener{
        void onReady(FlexatarServerAccess.StdResponse verifyData);

    }*/

    public interface OnAuthListener{
        void onReady();
        void onError();
    }

    public static class FlexatarVerifyProcess{
        private final long userId;
        private final int account;
        private final Runnable timeoutCallback;
        private ScheduledExecutorService executorService;
        private int counter = 0;
        private boolean verifyInProgress = false;

        private FlexatarServerAccess.StdResponse verifyData;

        public FlexatarVerifyProcess(int account,Runnable timeoutCallback){
            this.account=account;
            this.timeoutCallback=timeoutCallback;
            this.userId = UserConfig.getInstance(account).clientUserId;
            start();

//            clear();
            /*load();
            if (isVerified()) {
                Log.d("FLX_INJECT","Verify of account  "+account + " with userid "+userId +" is ready");
                return;
            }
            Log.d("FLX_INJECT","Start flexatar verify of account  "+account + " with userid "+userId );
            start();*/
        }

        public boolean isVerified(){

            return verifyData!=null;
        }
//        private final static int = 0
//        private int status = 0;
        private void start(){
            if (verifyInProgress) return;
            verifyInProgress = true;
            integrityCheck();
            authTgBot();
            /*if (isVerified()) return;
            if (!(counter>5 || counter == 0)) return;
            counter = 0;
            executorService = Executors.newScheduledThreadPool(1);

            executorService.scheduleAtFixedRate(()->{
                Log.d("FLX_INJECT", "Attempt to verify of user " + userId);
                authTgBot();

                counter+=1;
                if (counter>5){
                    executorService.shutdown();
                    timeoutCallback.run();
                }
//                executorService.shutdown();
            }, 0, 15, TimeUnit.SECONDS);*/
        }
        public void authTgBot(){
           ContactsController.getInstance(account).requestFlexatarBot(()->{
                AccountInstance accountInstance = AccountInstance.getInstance(account);
                final MessagesStorage messagesStorage = accountInstance.getMessagesStorage();
                messagesStorage.getStorageQueue().postRunnable(() -> {
                    AndroidUtilities.runOnUIThread(() -> {
                        accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(VERIFY_COMMAND, Config.authBotId, null, null, null, false, null, null, null, true, 0, null, false));
//                        accountInstance.getSendMessagesHelper().sendMessage(SendMessagesHelper.SendMessageParams.of(VERIFY_COMMAND + " " + SharedConfig.pushString, Config.authBotId, null, null, null, false, null, null, null, true, 0, null, false));
                    });
                });
            });
        }
        public void stop(){
            counter = 0;
            if (executorService == null) return;
            if (!executorService.isShutdown())
                executorService.shutdown();
        }
        public final Object storageSync = new Object();
        public void save(FlexatarServerAccess.StdResponse response){
//            if (verifyData == null) return;
            synchronized (storageSync) {
                verifyData = response;
                Log.d("FLX_INJECT", "saving verify " + verifyData.toJson().toString());
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME + userId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(VERIFY_KEY, verifyData.toJson().toString());
                editor.apply();
            }
        }

        public void load(){
            synchronized (storageSync) {
                if (verifyData != null) return;

                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME + userId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                String storedString = sharedPreferences.getString(VERIFY_KEY, null);
                if (storedString == null) return;
                try {
                    verifyData = new FlexatarServerAccess.StdResponse(storedString);
                } catch (JSONException e) {
                    verifyData = null;
                }
            }
        }
        public void clear(){
            Context context = ApplicationLoader.applicationContext;
            String storageName = PREF_STORAGE_NAME + userId;
            SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.clear();
            editor.apply();
        }
        /*
        Verification

App -> Bot : /verify
Bot -> App : url?verify=<flexatar_tmp_token>
App -> Google Api : <flexatar_tmp_token>
Google Api -> App : <google_token>
App -> lambda.url/verify : <google_token>
lambda.url/verify -> Google Api : <google_token>
Google Api -> lambda.url/verify : <google_token_decoded>
From <google_token_decoded> obtain <flexatar_tmp_token>, app version
lambda.url/verify -> App : <access_token>
        */
        public void verify(String token) {
            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.execute(() -> {
                getIntegrityToken(token, integrityToken -> {
                    JSONObject output = new JSONObject();
                    try {
                        output.put("android_ver",Config.version);
                        output.put("ios_ver","");
                        output.put("ext_ver","");
                        output.put("token",integrityToken);
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                    Data outputData = new Data(output.toString());
                    FlexatarServerAccess.requestJson(
                            this , "verify",
                            "POST",
                            outputData.value,
                            "application/json",
                            new FlexatarServerAccess.OnRequestJsonReady() {
                                @Override
                                public void onReady(FlexatarServerAccess.StdResponse response) {
                                    if (response.isOk()) {

                                    }else{

                                    }
                                }
                                @Override
                                public void onError() {

                                }
                            }
                    );
                });
            });

        }

        public void verify(OnAuthListener listener) {
            verify(verifyData,listener);
        }
        public void verify(FlexatarServerAccess.StdResponse verifyResponse,OnAuthListener listener){
            verifyData = verifyResponse;
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
            

            FlexatarServerAccess.requestJson(
                    this , "verify",

                    "POST",
                    outputData.value,
                    "application/json",
                    new FlexatarServerAccess.OnRequestJsonReady() {
                        @Override
                        public void onReady(FlexatarServerAccess.StdResponse response) {
                            if (response.isOk()) {
                                verifyData = response;
                                stop();
//                                save();
                                Log.d("FLX_INJECT","flexatar verify success");
                                if (listener!=null) listener.onReady();
                            }else{
                                if (listener!=null) listener.onError();
                            }
                        }
                        @Override
                        public void onError() {
                            if (listener!=null) listener.onError();
                        }
                    }
            );
        }

        public String getRoute() {
            return verifyData.route;
        }
        public String getToken() {
            if (verifyData == null) return null;
            return verifyData.token;
        }
    }
    public static Map<Integer,FlexatarVerifyProcess> verifyProcesses = new ConcurrentHashMap<>();

    public static void startVerification(int account,Runnable timeoutCallback){
//        long userId = UserConfig.getInstance(account).clientUserId;
        verifyProcesses.compute(account, (existingKey, existingValue) -> {

            if (existingValue == null) {
                return new FlexatarVerifyProcess(account,timeoutCallback);
            } else {
                existingValue.start();
                return existingValue;
            }
        });
    }
    public static FlexatarVerifyProcess getVerification(int account){
//        long userId = UserConfig.getInstance(account).clientUserId;
        if (!verifyProcesses.containsKey(account)){
            FlexatarServiceAuth.startVerification(account, () -> {

            });
        }
        return verifyProcesses.get(account);
    }
//    public static void resetVerification(){
//
//        for (Map.Entry<String,FlexatarVerifyProcess> ent : verifyProcesses.entrySet()){
//            ent.getValue().stop();
//        }
//    }

}
