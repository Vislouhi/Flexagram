package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
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
        public static final String RESULT_RETRY = "RETRY";
        public static final String RESULT_OK = "OK";
        public String ftars;
        public StdResponse(String json) throws JSONException {
            formJson(json);
        }

        public StdResponse() {

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
                    if (completion != null) {
                        if (jsonResponse.length()>0)
                            completion.onReady(new StdResponse(jsonResponse));
                        else
                            completion.onReady(new StdResponse());
                    }
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
                FlexatarServiceAuth.getVerification().verify(new FlexatarServiceAuth.OnAuthListener() {
                    @Override
                    public void onReady() {
                            requestDataRecursive(FlexatarServiceAuth.getVerification(), path, part, new ByteArrayOutputStream(), completion);
                    }

                    @Override
                    public void onError() {
                        completion.onError();
                    }
                });
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



                }else if(responseCode == HttpURLConnection.HTTP_UNAUTHORIZED){
                    completion.onUnauthorized();
                }else if (responseCode == HttpURLConnection.HTTP_NOT_FOUND){
                    if (completion!=null) completion.onNotFoundError();
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );

                    if (completion!=null)
                        completion.onError();
                }
            } catch (IOException e) {
                if (completion!=null) completion.onError();
                Log.d("FLX_INJECT","connection failed by exception");
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
                                    if (!savedFids.contains(element.id) && element.ftar!=null){
                                        linksToDownload.add(new String[]{element.ftar,element.id,prefix,element.err,element.meta});
                                    }else if(element.err != null){
                                        Log.d("FLX_INJECT","flexatar entry contains error, deleting it");
                                        String deleteRout = ServerDataProc.genDeleteRout(element.err);
                                        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(), deleteRout, "DELETE",null);
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
        FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(), links.get(position)[0], 0, new ByteArrayOutputStream(),
                new FlexatarServerAccess.OnDataDownloaded() {
                    @Override
                    public void onReady(boolean finished, ByteArrayOutputStream byteArrayOutputStream) {
                        Log.d("FLX_INJECT","downloaded "+links.get(position)[1]);
                        byte[] downloadedData = byteArrayOutputStream.toByteArray();
                        if (!new LengthBasedFlxUnpack(downloadedData).validate()){

                            try {
                                byteArrayOutputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                            loadNext();
                            return;
                        }

                        if (links.get(position)[4]==null){


                            File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,downloadedData,links.get(position)[1],links.get(position)[2]);
                            if (downloadBuiltinObserver != null){
                                downloadBuiltinObserver.downloaded(downloadedFile);
                            }
                            try {
                                byteArrayOutputStream.close();
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }else{
                            FlexatarServerAccess.requestDataRecursive(FlexatarServiceAuth.getVerification(), links.get(position)[4], 0, new ByteArrayOutputStream(), new OnDataDownloaded() {
                                @Override
                                public void onReady(boolean finished, ByteArrayOutputStream metaByteArray) {
                                    FlexatarStorageManager.FlexatarMetaData metaData = new FlexatarStorageManager.FlexatarMetaData();
                                    metaData.data = new Data(metaByteArray.toByteArray());
                                    File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,byteArrayOutputStream.toByteArray(),links.get(position)[1],links.get(position)[2]);
                                    FlexatarStorageManager.rewriteFlexatarHeader(downloadedFile,metaData);
                                    if (downloadBuiltinObserver != null){
                                        downloadBuiltinObserver.downloaded(downloadedFile);
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
                            downloadFlexatarListRecursive1(links,position+1,onFinish);
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
