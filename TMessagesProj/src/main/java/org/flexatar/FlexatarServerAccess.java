package org.flexatar;


import android.util.Log;

import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.flexatar.DataOps.LengthBasedUnpack;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.GcmPushListenerService;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.Components.LayoutHelper;

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
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class FlexatarServerAccess {


    /*public interface CompletionListener{
        default void onReady(String string){

        }
        default void onReadyData(byte[] data){

        }
        default void onReady(boolean isComplete){

        }
        default void onFail(){

        }
    }*/
    /*public interface VerifyListener{
        default void onVerifyAnswer(String verifyJson){};
        default void onVerifyAnswer(String token,String verifyUrl,String storageUrl,String statUrl){};
        default void onError(){};
    }*/

    public static class StdResponse extends SrializerJson{
        public String result;
        public String tgid;

        public String token;
        public String st;
        public String route;
        public String interval;
        public static final String RESULT_RETRY = "RETRY";
        public static final String RESULT_OK = "OK";
        public static final String RESULT_FAIL = "FAIL";
        public String ftars;
        public StdResponse(String json) throws JSONException {
            formJson(json);
        }

        public StdResponse() {

        }
        public boolean equal(StdResponse response){
            if (result == null) return false;
            if (!result.equals(response.result)) return false;
            if (token == null) return false;
            if (!token.equals(response.token)) return false;
            if (route == null) return false;
            if (!route.equals(response.route)) return false;
            return true;
        }

        public boolean overwrite(StdResponse response){
            boolean needOverwrite = false;
            if (result == null && response.result!=null) {
                needOverwrite = true;
                result = response.result;
            }
            if (result != null && response.result!=null && !result.equals(response.result)) {
                needOverwrite = true;
                result = response.result;
            }

            if (token == null && response.token!=null) {
                needOverwrite = true;
                token = response.token;
            }
            if (token != null && response.token!=null && !token.equals(response.token)) {
                needOverwrite = true;
                token = response.token;
            }

            if (route == null && response.route!=null) {
                needOverwrite = true;
                route = response.route;
            }
            if (route != null && response.route!=null && !route.equals(response.route)) {
                needOverwrite = true;
                route = response.token;
            }

            if (interval == null && response.interval!=null) {
                needOverwrite = true;
                interval = response.interval;
            }
            if (interval != null && response.interval!=null && !interval.equals(response.interval)) {
                needOverwrite = true;
                interval = response.interval;
            }
//            Log.d("FLX_INJECT", "interval " + response.interval);
            return needOverwrite;

        }
        public boolean isRetry(){
            if (result!=null) return result.equals(RESULT_RETRY);
            return false;
        }
        public boolean isOk(){
            if (result!=null) return result.equals(RESULT_OK);
            return false;
        }
        public boolean isFail(){
//            return true;
            if (result==null) return true;
            return result.equals(RESULT_FAIL);

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

        public ListElement(String json) throws JSONException {
            formJson(json);
        }
    }


    public interface OnRequestJsonReady{
        void onReady(StdResponse response);
        void onError();

    }
    public static void requestJson( FlexatarServiceAuth.FlexatarVerifyProcess verify,String path, String method,OnRequestJsonReady completion){
        requestJson(verify,path, method, null, null,completion);
    }
    public static void requestJson(FlexatarServiceAuth.FlexatarVerifyProcess verify,String path, String method,byte[] sendData,String contentType,OnRequestJsonReady completion){
        VersionController.updateTokenForNewVersion(verify.getAccount(),()->{
            requestJsonInternal(verify.getRoute(),path, verify.getToken(), method, sendData, contentType, new OnRequestJsonReady() {
                @Override
                public void onReady(StdResponse response) {
                    if (verify.getVerifyData().overwrite(response)){
                        verify.save();
                    }
                    if(response.isRetry()){
                        android.util.Log.d("FLX_INJECT","apigw response retry");
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
                    if (completion!=null) completion.onError();
                }
            });
        },null);

    }
    public static void debugLog(String tag,String value,String token){
        String rout = "https://ijye3k3hz3aebtwi3mmuxb2zmy0ahijw.lambda-url.us-east-1.on.aws";
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("tag", tag);
            jsonObject.put("value", value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Data sendData = new Data(jsonObject.toString());
        requestJsonInternal(rout, null, token, "POST", sendData.value, "application/json", new OnRequestJsonReady() {
            @Override
            public void onReady(StdResponse response) {
                Log.d("FLX_INJECT","log sent");
            }

            @Override
            public void onError() {
                Log.d("FLX_INJECT","log sent error");
            }
        });
    }
    public static void requestJsonInternal(String rout,String path, String token, String method, byte[] sendData, String contentType, OnRequestJsonReady completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
//                if (FlexatarServiceAuth.getVerifyData() == null){completion.onError();return;}
                String urlString = rout;
                if (path!=null)
                    urlString += "/"+path;
                Log.d("FLX_INJECT","urlString "+urlString);
                Log.d("FLX_INJECT","token "+token);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
                if (token!=null)
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
                    InputStream is = connection.getInputStream();
                    if (is == null ){
                        Log.d("FLX_INJECT", "input is null" );
                        completion.onReady(new StdResponse());
                    }
                    /*else if (is.available()==0){
                        Log.d("FLX_INJECT", "available 0" );
                        completion.onReady(new StdResponse());
                    }*/else {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        String jsonResponse = response.toString();
                        Log.d("FLX_INJECT", "received string: " + jsonResponse);
                        if (completion != null) {
                            if (jsonResponse.length() > 0)
                                completion.onReady(new StdResponse(jsonResponse));
                            else
                                completion.onReady(new StdResponse());
                        }
                    }
//                        Log.d("FLX_INJECT", "Received JSON: " + jsonResponse);
                }else{

                    Log.d("FLX_INJECT","connection failed err code "+responseCode);
                    if (completion!=null) completion.onError();
                }
            } catch (IOException e) {
                if (completion!=null) completion.onError();
                Log.d("FLX_INJECT","connection failed by exception");
                e.printStackTrace();
                //            return null;
//                throw new RuntimeException(e);
            } catch (JSONException e) {
                Log.d("FLX_INJECT","connection failed by json exception");
//                if (completion!=null) completion.onReady(new StdResponse());
                if (completion!=null) completion.onError();
            }
        });
    }
    public interface VerifyReadyListener{
        void onReady(String token);
        void onError();
    }
    public static void requestVerifyTokenString(String route, String method, String token,byte[] sendData,  VerifyReadyListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {

//                if (FlexatarServiceAuth.getVerifyData() == null){completion.onError();return;}
//                String urlString = route;
                Log.d("FLX_INJECT","urlString "+route);
                Log.d("FLX_INJECT","token "+token);
                URL url = new URL(route);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                connection.setRequestMethod(method);
                connection.setRequestProperty("Accept", "application/json");
//                connection.setRequestProperty("Authorization","Bearer "+ token);
                connection.setRequestProperty("Content-Type","text/plain");

                    connection.setDoOutput(true);
                    OutputStream outputStream = connection.getOutputStream();
                    outputStream.write(sendData);
                    outputStream.close();



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
                    if (completion != null) {
                        if (jsonResponse.length()>0)
                            completion.onReady(jsonResponse);
                        else
                            completion.onReady(null);
                    }
//                        Log.d("FLX_INJECT", "Received JSON: " + jsonResponse);
                }else{
                    Log.d("FLX_INJECT","connection failed err code "+responseCode);
                    if (completion!=null) completion.onError();
                }
            } catch (IOException e) {
                if (completion != null) completion.onError();
                Log.d("FLX_INJECT", "connection failed by exception");
            }
        });
    }

    public interface OnDataDownloaded{
        void onReady(boolean finished,ByteArrayOutputStream byteArrayOutputStream);
        void onError();
        default void onUnauthorized(){};
        default void onNotFoundError(){};
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
            public void onNotFoundError() {
                completion.onNotFoundError();
            }

            @Override
            public void onUnauthorized() {
                FlexatarServerAccess.requestJson(verify, "verify", "POST",
                        new FlexatarServerAccess.OnRequestJsonReady() {
                            @Override
                            public void onReady(FlexatarServerAccess.StdResponse response) {
                                Log.d("FLX_INJECT", "check accomplished");
                            }

                            @Override
                            public void onError() {

                            }
                        }
                );
                /*FlexatarServiceAuth.getVerification().verify(new FlexatarServiceAuth.OnAuthListener() {
                    @Override
                    public void onReady() {
                            requestDataRecursive(FlexatarServiceAuth.getVerification(), path, part, new ByteArrayOutputStream(), completion);
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
                    else{
                        if (byteArrayOutputStream!=null)byteArrayOutputStream.close();

                    }


                }else if(responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    if (byteArrayOutputStream!=null)byteArrayOutputStream.close();
                    completion.onUnauthorized();
                }else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND){
                    if (byteArrayOutputStream!=null)byteArrayOutputStream.close();
                    if (completion!=null) completion.onNotFoundError();
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );
                    if (byteArrayOutputStream!=null)byteArrayOutputStream.close();
                    if (completion!=null)
                        completion.onError();
                }
            } catch (IOException e) {
                if (byteArrayOutputStream!=null) {
                    try {
                        byteArrayOutputStream.close();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                if (completion!=null) completion.onError();
                Log.d("FLX_INJECT","connection failed by exception");
            }
        });
    }

    public static boolean isDownloadingFlexatars = false;
    public static void downloadCloudFlexatars1(int account,Runnable onFinish,Runnable onError) {
        isDownloadingFlexatars = true;
        /*StdResponse vd = FlexatarServiceAuth.getVerifyData();
        if (vd == null){
            onFinish.run();
            return;
        }*/
        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account),"list/1.00","GET",
//        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account),"verify","POST",
                new OnRequestJsonReady() {
                    @Override
                    public void onReady(StdResponse response) {
                        Map<String, List<FlexatarServerAccess.ListElement>> ftars = response.getFtars();
                        List<String[]> linksToDownload = new ArrayList<>();

                        List<String> savedFids = FlexatarStorageManager.getSavedFids(null,account);
//                        Log.d("FLX_INJECT","savedFids " + Arrays.toString(savedFids.toArray(new String[0])));
                        String[] keys = {"public"};
//                        String[] keys = {"public","private"};
                        for (String key : keys){
                            String prefix = FlexatarStorageManager.FLEXATAR_PREFIX;
                            if (key.equals("public"))
                                prefix = FlexatarStorageManager.PUBLIC_PREFIX;

                            if (ftars.containsKey(key)){
                                List<ListElement> elements = ftars.get(key);
                                if (elements == null) continue;
                                for (ListElement element : elements){
                                    if (!savedFids.contains(element.id) && element.ftar!=null){
                                        linksToDownload.add(new String[]{element.ftar,element.id,prefix,element.err,element.meta});
                                    }else if(element.err != null){
                                        Log.d("FLX_INJECT","flexatar entry contains error, deleting it");
                                        String deleteRout = ServerDataProc.genDeleteRout(element.err);
                                        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), deleteRout, "DELETE",null);
                                    }
//                                    Log.d("FLX_INJECT","ftar id " + element.id);
                                }
                            }
                        }
                        Log.d("FLX_INJECT","linksToDownload " + linksToDownload.size());
//                        if (linksToDownload.size()>0)
                            downloadFlexatarListRecursive1(UserConfig.selectedAccount,linksToDownload,0,onFinish);
//                        else
                            onFinish.run();
                    }

                    @Override
                    public void onError() {
                        onError.run();
                    }
                }
        );

    }
    public static void downloadFlexatarListRecursive1(int account,List<String[]> links, int position,Runnable onFinish){
        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.start();
        StdResponse vd = FlexatarServiceAuth.getVerification(account).getVerifyData();
        if (vd == null){
            onFinish.run();
            return;
        }
        FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(account), links.get(position)[0], 0, new ByteArrayOutputStream(),
                new FlexatarServerAccess.OnDataDownloaded() {
                    @Override
                    public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                        Log.d("FLX_INJECT","downloaded "+links.get(position)[1]);
                        byte[] downloadedData = byteArrayOutputStream.toByteArray();
                        int flexatarType = new LengthBasedUnpack(downloadedData,true).getFlexatarType() == FlexatarData.FlxDataType.PHOTO
                                ? 1 : 0;
                        if (!new LengthBasedFlxUnpack(downloadedData).validate(flexatarType)){

                            try {
                                byteArrayOutputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            loadNext();
                            return;
                        }

                        if (links.get(position)[4]==null){

                            File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,account, downloadedData, links.get(position)[1], links.get(position)[2],flexatarType);

                            if (downloadBuiltinObserver != null){
                                downloadBuiltinObserver.downloaded(downloadedFile,flexatarType);
                            }

                            try {
                                byteArrayOutputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(account), links.get(position)[4], 0, new ByteArrayOutputStream(), new OnDataDownloaded() {
                                @Override
                                public void onReady(boolean finished, ByteArrayOutputStream metaByteArray) {
                                    FlexatarStorageManager.FlexatarMetaData metaData = new FlexatarStorageManager.FlexatarMetaData();
                                    metaData.data = new Data(metaByteArray.toByteArray());
                                    File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,account,byteArrayOutputStream.toByteArray(),links.get(position)[1],links.get(position)[2],flexatarType);
                                    FlexatarStorageManager.rewriteFlexatarHeader(downloadedFile,metaData);
                                    if (downloadBuiltinObserver != null){
                                        downloadBuiltinObserver.downloaded(downloadedFile,flexatarType);
                                    }
                                    try {
                                        byteArrayOutputStream.close();
                                        metaByteArray.close();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }

                                @Override
                                public void onError() {

                                }
                            });
                        }


                        loadNext();

                    }

                    private void loadNext(){
                        if (position+1<links.size()){
                            downloadFlexatarListRecursive1(account,links,position+1,onFinish);
                        }else {
                            if (onFinish!=null) onFinish.run();
                        }
                    }

                    @Override
                    public void onNotFoundError() {
                        Log.d("FLX_INJECT","flexatar file not found, deleting entry");

//                        String deleteRout = ServerDataProc.genDeleteRout(links.get(position)[3]);
//                        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(), deleteRout, "DELETE",null);
                        loadNext();
                        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.onError();
                    }

                    @Override
                    public void onError() {
                        loadNext();
                        if (downloadBuiltinObserver!=null) downloadBuiltinObserver.onError();
                        Log.d("FLX_INJECT","failed to download flexatar");
                    }
                }
        );
    }
    public static Map<String,OnReadyOrErrorListener> activeDownloads = new HashMap<>();
    interface OnReadyOrErrorListener{
        void onReady(File flexatarFile,int flexatarType);
        void onError();
    }

    private static final Object downloadsLock = new Object();

    public static void downloadFlexatar(int account,File flexatarFile,String servPath,OnReadyOrErrorListener listener){
        synchronized (downloadsLock) {
            if (activeDownloads.containsKey(servPath)) {
                activeDownloads.put(servPath, listener);
                return;
            }
            activeDownloads.put(servPath, listener);
        }
        String servPathMeta = servPath.replace(".p", ".m");
        AtomicReference<byte[]> metaData = new AtomicReference<>(null);
        AtomicReference<byte[]> body = new AtomicReference<>(null);

        CountDownLatch latch = new CountDownLatch(2);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                latch.await();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            byte[] meta = metaData.get();
            byte[] downloadedData = body.get();
            if (downloadedData == null) {
                synchronized (downloadsLock) {
                    OnReadyOrErrorListener list = activeDownloads.get(servPath);
                    activeDownloads.remove(servPath);
                    if (list != null) list.onError();
                }

            } else {
                try{
                    int flexatarType = new LengthBasedUnpack(downloadedData, true).getFlexatarType() == FlexatarData.FlxDataType.PHOTO
                            ? 1 : 0;
                    if (!new LengthBasedFlxUnpack(downloadedData).validate(flexatarType)) {
                        return;
                    }
                    File resultFile = null;
                    if (flexatarFile != null) {
                        if (flexatarType == 1) {

                            FlexatarStorageManager.dataToFile(downloadedData, flexatarFile);

                        } else {
                            File videoFile = new File(flexatarFile.getAbsolutePath().replace(".flx", ".mp4"));
                            byte[][] extractResult = FlexatarStorageManager.extractVideo(downloadedData);
                            FlexatarStorageManager.dataToFile(extractResult[0], flexatarFile);
                            FlexatarStorageManager.dataToFile(extractResult[1], videoFile);
                        }
                        if (meta != null) {
                            FlexatarStorageManager.FlexatarMetaData mD = new FlexatarStorageManager.FlexatarMetaData();
                            mD.data = new Data(meta);
                            FlexatarStorageManager.rewriteFlexatarHeader(flexatarFile, mD);
                        }
                        resultFile = flexatarFile;
                    } else {
                        String[] pathSplit = servPath.split("/");
                        String prefix = servPath.startsWith("public") ? FlexatarStorageManager.PUBLIC_PREFIX : FlexatarStorageManager.FLEXATAR_PREFIX;

                        File flxFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext, account, downloadedData, pathSplit[pathSplit.length - 2], prefix, flexatarType);
                        if (meta != null) {
                            FlexatarStorageManager.FlexatarMetaData mD = new FlexatarStorageManager.FlexatarMetaData();
                            mD.data = new Data(meta);
                            FlexatarStorageManager.rewriteFlexatarHeader(flxFile, mD);
                        }
                        resultFile = flxFile;
                    }
                    synchronized (downloadsLock) {
                        OnReadyOrErrorListener list = activeDownloads.get(servPath);
                        activeDownloads.remove(servPath);
                        if (list != null) list.onReady(resultFile, flexatarType);
                    }
                }catch (Exception ignored){
//                    String resp = new String(downloadedData, StandardCharsets.UTF_8);
//                    Log.d("FLX_INJECT","incorrect flexatar data: "+resp);
                    OnReadyOrErrorListener list = activeDownloads.get(servPath);
                    activeDownloads.remove(servPath);
                    if (list != null) list.onError();

                    FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), "verify", "POST",
                            new FlexatarServerAccess.OnRequestJsonReady() {
                                @Override
                                public void onReady(FlexatarServerAccess.StdResponse response) {
                                    Log.d("FLX_INJECT", "check accomplished");
                                }

                                @Override
                                public void onError() {

                                }
                            }
                    );
                }
            }

        });
        FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(account), servPathMeta, 0, new ByteArrayOutputStream(), new OnDataDownloaded() {
                    @Override
                    public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                        metaData.set(byteArrayOutputStream.toByteArray());
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        latch.countDown();
                        Log.d("FLX_INJECT", "meta data loaded " + servPath);

                    }

                    @Override
                    public void onError() {
                        latch.countDown();
                        Log.d("FLX_INJECT", "no meta data " + servPath);
                    }
                }
        );

        FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(account), servPath, 0, new ByteArrayOutputStream(),
                new FlexatarServerAccess.OnDataDownloaded() {
                    @Override
                    public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                        Log.d("FLX_INJECT", "downloaded " + servPath);
                        body.set(byteArrayOutputStream.toByteArray());
                        try {
                            byteArrayOutputStream.close();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        latch.countDown();


                    }


                    @Override
                    public void onNotFoundError() {
                        Log.d("FLX_INJECT", "flexatar file not found, deleting entry");
                        latch.countDown();


                    }

                    @Override
                    public void onError() {
                        latch.countDown();
//                        OnReadyOrErrorListener list = activeDownloads.get(servPath);
//                        activeDownloads.remove(servPath);
//                        if (list != null) list.onError();
                        Log.d("FLX_INJECT", "failed to download flexatar");
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
        void downloaded(File file,int flexatarType);
    }
    public static DownloadBuiltinObserver downloadBuiltinObserver = null;

}
