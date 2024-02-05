package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.GcmPushListenerService;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class FlexatarServerAccess {
    private static String auth = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMjA1OTY2MjIsIm5iZiI6MTcwNTIzNjYyMiwiVGFnIjoibm9uZSIsIlVzZXIiOiJkZWZhdWx0IiwiQXBwIjoidGctYW5yb2lkIiwiVmVyIjoiMS4wIn0._PxtlSUHWk0TDmz7Q84V-c4oxfDuSYXiWCDkwqwJXE4";
    private static String url_lambda = "https://mhpblvwwrb.execute-api.us-east-1.amazonaws.com/test1/";
    private static ScheduledExecutorService timeoutExecutor = null;
    private static ScheduledFuture<?> timeoutFuture = null;

    public interface CompletionListener{
        default void onReady(String string){

        }
        default void onReadyData(byte[] data){

        }
        default void onReady(boolean isComplete){

        }
        default void onFail(){

        }
    }
    public static final String url_verify = "https://wjfk5fg7t7an6ypbmig7ydvvdm0nmubp.lambda-url.us-east-1.on.aws/";
    public interface VerifyListener{
        default void onVerifyAnswer(String verifyJson){};
        default void onVerifyAnswer(String token,String verifyUrl,String storageUrl,String statUrl){};
        default void onError(){};
    }
    public static void lambdaVerify(VerifyListener listener){
        if (timeoutExecutor!=null && !timeoutExecutor.isShutdown()){
            timeoutExecutor.shutdown();

        }
        if (timeoutFuture!=null && !timeoutFuture.isCancelled()){
            timeoutFuture.cancel(false);
        }
        timeoutExecutor = Executors.newSingleThreadScheduledExecutor();
        timeoutFuture = timeoutExecutor.schedule(()->{
            listener.onError();
            timeoutExecutor = null;
            timeoutFuture = null;
        }, 10, TimeUnit.SECONDS);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                GcmPushListenerService.verifyListener = new VerifyListener() {
                    @Override
                    public void onVerifyAnswer(String verifyJson) {
                        JSONObject verify;
                        try {
                            verify = new JSONObject(verifyJson);

                        } catch (JSONException e) {
                            listener.onError();
                            return;
                        }
                        String token = null;
                        String verifyUrl = null;
                        String storageUrl = null;
                        String statUrl = null;
                        try {
                            token = verify.getString("token");
                        } catch (JSONException ignored) {}
                        try {
                            verifyUrl = verify.getString("verify");
                        } catch (JSONException ignored) {}
                        try {
                            storageUrl = verify.getString("storage");
                        } catch (JSONException ignored) {}
                        try {
                            statUrl = verify.getString("stat");
                        } catch (JSONException ignored) {}
                        timeoutExecutor.shutdown();
                        timeoutExecutor = null;
                        timeoutFuture.cancel(false);
                        timeoutFuture=null;
                        listener.onVerifyAnswer(token,verifyUrl,storageUrl,statUrl);
                        Log.d("FLX_INJECT","verify result : " + verifyJson);
//                        Log.d("FLX_INJECT","verify result token: " + token);
                    }
                };
                URL url = new URL(url_verify);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");

                connection.setRequestProperty("Content-Type", "application/json");
                JSONObject output = new JSONObject();
                output.put("token",SharedConfig.pushString);
                long telegramID = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
                output.put("tid",""+telegramID);
                output.put("ver",Config.version);
                Data outputData = new Data(output.toString());

                connection.setDoOutput(true);
                OutputStream outputStream = connection.getOutputStream();
                outputStream.write(outputData.value);
                outputStream.close();
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK){
                    Log.d("FLX_INJECT","verify request sent" );
                }

            } catch (IOException e) {
                Log.d("FLX_INJECT","connection failed by exception");
                timeoutExecutor.shutdown();
                timeoutExecutor = null;
                timeoutFuture.cancel(false);
                timeoutFuture=null;
                listener.onError();
                //            return null;
//                throw new RuntimeException(e);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

    }


    public static void lambdaRequest(String path, String method,byte[] sendData,ByteArrayOutputStream byteArrayOutputStream,CompletionListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = Config.storage + path;
                Log.d("FLX_INJECT","urlString "+urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);

                connection.setRequestProperty("Content-Type", "application/octet-stream");
                if (byteArrayOutputStream!=null){
                    connection.setRequestProperty("Accept", "application/octet-stream");
                }
                connection.setRequestProperty("Authorization","Bearer "+ Config.token);
                if (sendData!=null) {
                    connection.setDoOutput(true);
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(sendData);
                    outputStream.close();
                }

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
//                    Log.d("FLX_INJECT", "getContentType: " + connection.getContentType().toLowerCase());
                    if (connection.getContentType().toLowerCase().contains("application/json") ||
                            connection.getContentType().toLowerCase().contains("text/plain")
                    ) {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        String jsonResponse = response.toString();
                        if (completion!=null)
                            completion.onReady(jsonResponse);
//                        Log.d("FLX_INJECT", "Received JSON: " + jsonResponse);
                    }else if (connection.getContentType().toLowerCase().contains("application/octet-stream")) {
                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
//                    Log.d("FLX_INJECT","inputStream :"+inputStream.available() );
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                        if (completion!=null)
                            completion.onReady(responseCode == HttpURLConnection.HTTP_OK);

                        inputStream.close();
                        connection.disconnect();
                    }
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );

                    if (completion!=null)
                        completion.onFail();
                }
            } catch (IOException e) {
                if (completion!=null) completion.onFail();
                Log.d("FLX_INJECT","connection failed by exception");
                //            return null;
//                throw new RuntimeException(e);
            }
        });
    }
    public static void downloadFlexatarRecursive(String flexatarLink,int part,ByteArrayOutputStream outputStream,FlexatarServerAccess.CompletionListener listener){
        FlexatarServerAccess.lambdaRequest(flexatarLink+"?part="+part, "GET", null, outputStream, new FlexatarServerAccess.CompletionListener() {
            @Override
            public void onReady(boolean isComplete) {
                if (isComplete){
                    listener.onReady(true);
                }else{
                    downloadFlexatarRecursive(flexatarLink,part+1,outputStream,listener);

                }
            }

            @Override
            public void onFail() {
                listener.onFail();
            }
        });

    }
    public static boolean isDownloadingFlexatars = false;

    public static void downloadCloudFlexatars(Runnable onFinish){
        isDownloadingFlexatars = true;
        FlexatarServerAccess.lambdaRequest("/list/1.00", "GET", null, null, new FlexatarServerAccess.CompletionListener() {
            @Override
            public void onReady(String response) {
                ServerDataProc.FlexatarListResponse listResponse = new ServerDataProc.FlexatarListResponse(response);

                if (listResponse.hasPublic()) {
                    FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.PUBLIC_PREFIX, listResponse.getPublicLinksToDownload(), listResponse.getPublicIdsToDownload(), 0, ()->{
                        FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.FLEXATAR_PREFIX, listResponse.getPrivateLinksToDownload(), listResponse.getPrivateIdsToDownload(), 0, onFinish);
                    });
                }
                else if(listResponse.hasPrivate()){
                    FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.FLEXATAR_PREFIX, listResponse.getPrivateLinksToDownload(), listResponse.getPrivateIdsToDownload(), 0, onFinish);

                }else{
//                    isDownloadingFlexatars = false;
                    if (onFinish!=null) onFinish.run();
                }
            }
            @Override
            public void onFail(){

                if (onFinish!=null) onFinish.run();
            }
        });
    }
    public static void downloadFlexatarListRecursive(String prefix,List<String> flexatarList, List<String> idList, int position,Runnable onFinish){
        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.start();
        ByteArrayOutputStream flxOutputStream = new ByteArrayOutputStream();
        downloadFlexatarRecursive("/"+flexatarList.get(position), 0, flxOutputStream, new CompletionListener() {
            @Override
            public void onReady(boolean isComplete) {
                Log.d("FLX_INJECT","downloaded "+idList.get(position));
                File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,flxOutputStream.toByteArray(),idList.get(position),prefix);
//                String[] fids = FlexatarStorageManager.getRecords(ApplicationLoader.applicationContext);
//                Log.d("FLX_INJECT","fids "+ Arrays.toString(fids));
                if (downloadBuiltinObserver != null){
                    downloadBuiltinObserver.downloaded(downloadedFile);
                }
                if (position+1<flexatarList.size()){
                    downloadFlexatarListRecursive(prefix,flexatarList,idList,position+1,onFinish);
                }else {
//                    isDownloadingFlexatars = false;
                    if (onFinish!=null) onFinish.run();

                }

            }

            @Override
            public void onFail() {
//                isDownloadingFlexatars = false;
                if (onFinish!=null) onFinish.run();
                if (downloadBuiltinObserver!=null) downloadBuiltinObserver.onError();
                Log.d("FLX_INJECT","failed to download flexatar");
            }

        });

    }



    public interface DownloadBuiltinObserver{
        void start();
        void onError();
        void downloaded(File file);
    }
    public static DownloadBuiltinObserver downloadBuiltinObserver = null;
    /*public static void getFlexatarList(CompletionListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = url_lambda + "list/1.00";
                Log.d("FLX_INJECT","getFlexatarList "+urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
    //            connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Authorization",auth);
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;

                    // Read the response line by line
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Close the reader
                    reader.close();

                    // Now 'response' contains the JSON data
                    String jsonResponse = response.toString();
                    completion.onReadyJsonSting(jsonResponse);
                    // Do something with the JSON data
                    Log.d("FLX_INJECT","Received JSON: " + jsonResponse);
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );
                    completion.onFail();
                }
            } catch (IOException e) {
    //            return null;
                throw new RuntimeException(e);
            }
        });
    }*/

    /*public static void makeFlexatarRequest(byte[] sendData){
        try {
            URL url = new URL("https://26ntp3aaifsmiiz2qxoi4ynboe0ncqby.lambda-url.us-east-1.on.aws/");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/octet-stream");
            connection.setRequestProperty("Authorization","Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMjA1OTY2MjIsIm5iZiI6MTcwNTIzNjYyMiwiVGFnIjoibm9uZSIsIlVzZXIiOiJkZWZhdWx0IiwiQXBwIjoidGctYW5yb2lkIiwiVmVyIjoiMS4wIn0._PxtlSUHWk0TDmz7Q84V-c4oxfDuSYXiWCDkwqwJXE4");
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(sendData);
            outputStream.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {

                *//*ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }

                byteArrayOutputStream.close();
                inputStream.close();
                connection.disconnect();*//*
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                // Read the response line by line
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                // Close the reader
                reader.close();

                // Now 'response' contains the JSON data
                String jsonResponse = response.toString();

                // Do something with the JSON data
                Log.d("FLX_INJECT","Received JSON: " + jsonResponse);
//                Log.d("FLX_INJECT","Server responce :" + new String(buffer, StandardCharsets.UTF_8));
            }else{
                Log.d("FLX_INJECT","Server responce error :"+responseCode );
            }
        } catch (IOException e) {
//            return null;
//            throw new RuntimeException(e);
        }
//        return null;
    }*/
}
