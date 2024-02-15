package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.Data;
import org.json.JSONArray;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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

    /*public static class VerifyResponse extends SrializerJson{
        public String result;
        public String token;
        public String route;
        public static final String RESULT_RETRY = "RETRY";
        public static final String RESULT_OK = "OK";
        public VerifyResponse(){

        }
        public VerifyResponse(String json) throws JSONException, IllegalAccessException {
            formJson(json);
        }
        public boolean isRetry(){
            if (result!=null) return result.equals(RESULT_RETRY);
            return false;
        }
        public boolean isOk(){
            if (result!=null) return result.equals(RESULT_OK);
            return false;
        }
    }*/

    public static class StdResponse extends SrializerJson{
        public String result;
        public String tgid;

        public String token;
        public String st;
        public String route;
        public static final String RESULT_RETRY = "RETRY";
        public static final String RESULT_OK = "OK";
        public String ftars;
        public StdResponse(String json) throws JSONException {
            formJson(json);
        }
        public boolean isRetry(){
            if (result!=null) return result.equals(RESULT_RETRY);
            return false;
        }
        public boolean isOk(){
            if (result!=null) return result.equals(RESULT_OK);
            return false;
        }
        public Map<String,List<ListElement>>  getFtars(){
            return ListElement.listFactory(ftars);
        }
    }
    public static class ListElement extends SrializerJson{
        public String id;
        public String f_class;
        public String ftar;
        public String err;
        public String meta;
        public static Map<String,List<ListElement>> listFactory(String jsonString){
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Map<String,List<ListElement>> map = new HashMap<>();
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONArray jsonArray = jsonObject.getJSONArray(key);
                    List<ListElement> list = new ArrayList<>();
                    for (int i = 0; i < jsonArray.length(); i++) {
                        list.add(new ListElement(jsonArray.getString(i)));
                    }
                    map.put(key,list);
                }
                return map;
            } catch (JSONException e) {
                return null;
            }
        }
        public ListElement(){

        }
        public ListElement(String json) throws JSONException {
            formJson(json);
        }
    }

    public interface OnVerifyResultListener{
        void onResult(StdResponse response);
        void onError();
    }
    /*public static void verify(String rout,String token,OnVerifyResultListener listener){
        Log.d("FLX_INJECT","Requesting : "+rout );
        try{
        URL url = new URL(rout);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Authorization", "Bearer "+token);
            JSONObject output = new JSONObject();
//            output.put("token",SharedConfig.pushString);
//            long telegramID = UserConfig.getInstance(UserConfig.selectedAccount).getCurrentUser().id;
//            output.put("tid",""+telegramID);
            output.put("android_ver",Config.version);
            output.put("ios_ver","");
            output.put("ext_ver","");
            output.put("token",token);
            Data outputData = new Data(output.toString());
            connection.setDoOutput(true);
            OutputStream outputStream = connection.getOutputStream();
            outputStream.write(outputData.value);
            outputStream.close();
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK){
                Log.d("FLX_INJECT","verify request sent" );
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String jsonResponse = response.toString();
                Log.d("FLX_INJECT","Received response " + jsonResponse );
                listener.onResult(new StdResponse(jsonResponse));

            }else{
                listener.onError();
                Log.d("FLX_INJECT","connection fail with code: " + responseCode);
            }
        } catch (IOException | JSONException | IllegalAccessException e) {
            throw new RuntimeException(e);
//            listener.onError();
        }
    }*/
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


    public interface OnRequestJsonReady{
        void onReady(StdResponse response);
        void onError();
    }
    public static void requestJson( FlexatarServiceAuth.FlexatarVerifyProcess verify,String path, String method,OnRequestJsonReady completion){
        requestJson(verify,path, method, null, null,completion);
    }
    public static void requestJson(FlexatarServiceAuth.FlexatarVerifyProcess verify,String path, String method,byte[] sendData,String contentType,OnRequestJsonReady completion){

        requestJsonInternal(verify.getRoute(),path, verify.getToken(), method, sendData, contentType, new OnRequestJsonReady() {
            @Override
            public void onReady(StdResponse response) {
                if(response.isRetry()){
                    android.util.Log.d("FLX_INJECT","apigw response retry");
                    verify.save(response);
                    requestJson(verify,path, method, sendData, contentType, completion);

                } else if (response.isOk()) {
                    android.util.Log.d("FLX_INJECT","apigw response ok");
                    if (completion!=null) completion.onReady(response);
                }else{
                    if (completion!=null) completion.onError();
                }
            }

            @Override
            public void onError() {

            }
        });
    }
    public static void requestJsonInternal(String rout,String path, String token, String method, byte[] sendData, String contentType, OnRequestJsonReady completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {

//                if (FlexatarServiceAuth.getVerifyData() == null){completion.onError();return;}
                String urlString = rout+"/"+path;
                Log.d("FLX_INJECT","urlString "+urlString);
                Log.d("FLX_INJECT","token "+token);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");

                connection.setRequestProperty("Authorization","Bearer "+ token);
                if (sendData!=null) {
                    connection.setRequestProperty("Content-Type", contentType);

                    connection.setDoOutput(true);
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(sendData);
                    outputStream.close();
                }


                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK ) {

                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    String jsonResponse = response.toString();
                    Log.d("FLX_INJECT","received string: "+jsonResponse);
                    if (completion != null)
                        completion.onReady(new StdResponse(jsonResponse));
//                        Log.d("FLX_INJECT", "Received JSON: " + jsonResponse);
                }else{
                    Log.d("FLX_INJECT","connection failed err code "+responseCode);
                    if (completion!=null) completion.onError();
                }
            } catch (IOException e) {
                if (completion!=null) completion.onError();
                Log.d("FLX_INJECT","connection failed by exception");
                //            return null;
//                throw new RuntimeException(e);
            } catch (JSONException e) {
                if (completion!=null) completion.onError();
            }
        });
    }


    public interface OnDataDownloaded{
        void onReady(boolean finished,ByteArrayOutputStream byteArrayOutputStream);
        void onError();
        default void onUnauthorized(){};
    }
    public static void requestDataRecursive(FlexatarServiceAuth.FlexatarVerifyProcess verify,String path,int part,ByteArrayOutputStream byteArrayOutputStream,OnDataDownloaded completion ){
        requestDataInternal(verify.getRoute(), path+"?part="+part, verify.getToken(), byteArrayOutputStream, new OnDataDownloaded() {
            @Override
            public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                if (!finished){
                    Log.d("FLX_INJECT","requesting additional part");
                    requestDataRecursive(verify, path, part+1, byteArrayOutputStream,completion);
                }else{
                    completion.onReady(true,byteArrayOutputStream);
                }
            }

            @Override
            public void onError() {
                completion.onError();
            }

            @Override
            public void onUnauthorized() {
                /*FlexatarServiceAuth.verify(FlexatarServiceAuth.getVerifyData(), new FlexatarServiceAuth.OnAuthListener() {
                    @Override
                    public void onReady() {
                        StdResponse vd = FlexatarServiceAuth.getVerifyData();
                        if (vd!=null){
                            requestDataRecursive(vd.route, path, part, vd.token, byteArrayOutputStream,completion);
                        }else{
                            completion.onError();
                        }

                    }

                    @Override
                    public void onError() {
                        completion.onError();
                    }
                });*/
            }
        });
    }

    public static void requestDataInternal(String rout,String path,String token,ByteArrayOutputStream byteArrayOutputStream,OnDataDownloaded completion ){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = rout +"/"+ path;

                Log.d("FLX_INJECT","urlString "+urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setRequestProperty("Accept", "application/octet-stream");
                connection.setRequestProperty("Authorization","Bearer "+ token);

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {
//                    Log.d("FLX_INJECT", "getContentType: " + connection.getContentType().toLowerCase());

                        InputStream inputStream = new BufferedInputStream(connection.getInputStream());
//                    Log.d("FLX_INJECT","inputStream :"+inputStream.available() );
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            byteArrayOutputStream.write(buffer, 0, bytesRead);
                        }
                    inputStream.close();
                    connection.disconnect();
                    if (completion!=null)
                        completion.onReady(responseCode == HttpURLConnection.HTTP_OK,byteArrayOutputStream);



                }else if(responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    completion.onUnauthorized();
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );

                    if (completion!=null)
                        completion.onError();
                }
            } catch (IOException e) {
                if (completion!=null) completion.onError();
                Log.d("FLX_INJECT","connection failed by exception");
                //            return null;
//                throw new RuntimeException(e);
            }
        });
    }
    public static void lambdaRequest(String path, String method,byte[] sendData,ByteArrayOutputStream byteArrayOutputStream,CompletionListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = path;
                Log.d("FLX_INJECT","urlString "+urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);

                connection.setRequestProperty("Content-Type", "application/octet-stream");
                if (byteArrayOutputStream!=null){
                    connection.setRequestProperty("Accept", "application/octet-stream");
                }
                connection.setRequestProperty("Authorization","Bearer "+ FlexatarServiceAuth.verifyData.token);
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
    public static void downloadCloudFlexatars1(Runnable onFinish) {
        isDownloadingFlexatars = true;
        /*StdResponse vd = FlexatarServiceAuth.getVerifyData();
        if (vd == null){
            onFinish.run();
            return;
        }*/
        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(),"list/1.00","GET",
                new OnRequestJsonReady() {
                    @Override
                    public void onReady(StdResponse response) {
                        Map<String, List<FlexatarServerAccess.ListElement>> ftars = response.getFtars();
                        List<String[]> linksToDownload = new ArrayList<>();

                        List<String> savedFids = FlexatarStorageManager.getSavedFids(null);
//                        Log.d("FLX_INJECT","savedFids " + Arrays.toString(savedFids.toArray(new String[0])));
                        String[] keys = {"public","private"};
                        for (String key : keys){
                            String prefix = FlexatarStorageManager.FLEXATAR_PREFIX;
                            if (key.equals("public")) prefix = FlexatarStorageManager.PUBLIC_PREFIX;
                            if (ftars.containsKey(key)){
                                List<ListElement> elements = ftars.get(key);
                                if (elements == null) continue;
                                for (ListElement element : elements){
                                    if (!savedFids.contains(element.id)){
                                        linksToDownload.add(new String[]{element.ftar,element.id,prefix});
                                    }
//                                    Log.d("FLX_INJECT","ftar id " + element.id);
                                }
                            }
                        }
                        Log.d("FLX_INJECT","linksToDownload " + linksToDownload.size());
                        if (linksToDownload.size()>0)
                            downloadFlexatarListRecursive1(linksToDownload,0,onFinish);
                        else
                            onFinish.run();
                    }

                    @Override
                    public void onError() {
                        onFinish.run();
                    }
                }
        );

    }
    public static void downloadFlexatarListRecursive1(List<String[]> links, int position,Runnable onFinish){
        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.start();
        /*StdResponse vd = FlexatarServiceAuth.getVerifyData();
        if (vd == null){
            onFinish.run();
            return;
        }*/
        FlexatarServerAccess.requestDataRecursive(
                FlexatarServiceAuth.getVerification(),
                links.get(position)[0], 0,
                new ByteArrayOutputStream(),
                new FlexatarServerAccess.OnDataDownloaded() {
                    @Override
                    public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                        Log.d("FLX_INJECT","downloaded "+links.get(position)[1]);
                        File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,byteArrayOutputStream.toByteArray(),links.get(position)[1],links.get(position)[2]);

                        if (downloadBuiltinObserver != null){
                            downloadBuiltinObserver.downloaded(downloadedFile);
                        }
                        if (position+1<links.size()){
                            downloadFlexatarListRecursive1(links,position+1,onFinish);
                        }else {
//                    isDownloadingFlexatars = false;
                            if (onFinish!=null) onFinish.run();

                        }
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                    @Override
                    public void onError() {
                        if (position+1<links.size()){
                            downloadFlexatarListRecursive1(links,position+1,onFinish);
                        }else {
                            if (onFinish!=null) onFinish.run();
                        }
                        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.onError();
                        Log.d("FLX_INJECT","failed to download flexatar");
                    }
                }
        );


    }
    /*public static void downloadCloudFlexatars(Runnable onFinish){
        isDownloadingFlexatars = true;
        FlexatarServerAccess.lambdaRequest("/list/1.00", "GET", null, null, new FlexatarServerAccess.CompletionListener() {
            @Override
            public void onReady(String response) {
                ServerDataProc.FlexatarListResponse listResponse = new ServerDataProc.FlexatarListResponse(response);

                if (listResponse.hasPublic()) {
                    FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.PUBLIC_PREFIX, listResponse.getPublicLinksToDownload(), listResponse.getPublicIdsToDownload(), 0, ()->{
                        if(listResponse.hasPrivate()) {
                            FlexatarServerAccess.downloadFlexatarListRecursive(FlexatarStorageManager.FLEXATAR_PREFIX, listResponse.getPrivateLinksToDownload(), listResponse.getPrivateIdsToDownload(), 0, onFinish);
                        }else{
                            if (onFinish!=null) onFinish.run();
                        }
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
    }*/
    /*public static void downloadFlexatarListRecursive(String prefix,List<String> flexatarList, List<String> idList, int position,Runnable onFinish){
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

    }*/



    public interface DownloadBuiltinObserver{
        void start();
        void onError();
        void downloaded(File file);
    }
    public static DownloadBuiltinObserver downloadBuiltinObserver = null;

}
