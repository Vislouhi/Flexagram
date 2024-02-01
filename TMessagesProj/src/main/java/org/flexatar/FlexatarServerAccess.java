package org.flexatar;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.ApplicationLoader;

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
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlexatarServerAccess {
    private static String auth = "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJleHAiOjIwMjA1OTY2MjIsIm5iZiI6MTcwNTIzNjYyMiwiVGFnIjoibm9uZSIsIlVzZXIiOiJkZWZhdWx0IiwiQXBwIjoidGctYW5yb2lkIiwiVmVyIjoiMS4wIn0._PxtlSUHWk0TDmz7Q84V-c4oxfDuSYXiWCDkwqwJXE4";
    private static String url_lambda = "https://mhpblvwwrb.execute-api.us-east-1.amazonaws.com/test1/";

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
    /*public static void operation(String path, String method, CompletionListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = url_lambda + path;
//                Log.d("FLX_INJECT",urlString);
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);
//                connection.setRequestProperty("Content-Type", "application/octet-stream");
                connection.setRequestProperty("Authorization",auth);
//                connection.setRequestProperty("Range","4000000");
                int responseCode = connection.getResponseCode();
                *//*Map<String, List<String>> headers = connection.getHeaderFields();
                for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                    String key = entry.getKey();
                    List<String> values = entry.getValue();
                    if (key != null) {
                        Log.d("FLX_INJECT",key + ": " + String.join(", ", values));
                    }
                }*//*
                if (responseCode == HttpURLConnection.HTTP_OK || responseCode == HttpURLConnection.HTTP_PARTIAL) {


                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    InputStream inputStream = new BufferedInputStream(connection.getInputStream());
//                    Log.d("FLX_INJECT","inputStream :"+inputStream.available() );
                    byte[] buffer = new byte[1024];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    if (completion!=null)
                        completion.onReadyData(byteArrayOutputStream.toByteArray());
                    byteArrayOutputStream.close();
                    inputStream.close();
                    connection.disconnect();

                    // Do something with the JSON data
//                    Log.d("FLX_INJECT","Received JSON: " + jsonResponse);
                }else{
                    Log.d("FLX_INJECT","Server responce error :"+responseCode );
                    if (completion!=null)
                        completion.onFail();
                }
            } catch (IOException e) {
                //            return null;
                throw new RuntimeException(e);
            }
        });
    }*/

    public static void lambdaRequest(String path, String method,byte[] sendData,ByteArrayOutputStream byteArrayOutputStream,CompletionListener completion){
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                String urlString = url_lambda + path;

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method);

                connection.setRequestProperty("Content-Type", "application/octet-stream");
                if (byteArrayOutputStream!=null){
                    connection.setRequestProperty("Accept", "application/octet-stream");
                }
                connection.setRequestProperty("Authorization",auth);
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
                //            return null;
                throw new RuntimeException(e);
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
    public static void downloadFlexatarListRecursive(String prefix,List<String> flexatarList, List<String> idList, int position){
        ByteArrayOutputStream flxOutputStream = new ByteArrayOutputStream();
        downloadFlexatarRecursive(flexatarList.get(position), 0, flxOutputStream, new CompletionListener() {
            @Override
            public void onReady(boolean isComplete) {
                Log.d("FLX_INJECT","downloaded "+idList.get(position));
                File downloadedFile = FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,flxOutputStream.toByteArray(),flexatarList.get(position));
                String[] fids = FlexatarStorageManager.getRecords(ApplicationLoader.applicationContext);
                Log.d("FLX_INJECT","fids "+ Arrays.toString(fids));
                if (downloadBuiltinObserver != null){
                    downloadBuiltinObserver.downloaded(downloadedFile);
                }
                if (position+1<flexatarList.size()){
                    downloadFlexatarListRecursive(prefix,flexatarList,idList,position+1);
                }

            }

            @Override
            public void onFail() {
                CompletionListener.super.onFail();
            }

        });

    }

    public interface DownloadBuiltinObserver{
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
