package org.flexatar;

import static org.flexatar.DataOps.Data.dataToIntArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class FlexatarStorageManager {
    private static String PREF_STORAGE_NAME = "flexatar_storage_pref";
    private static String FLEXATAR_STORAGE_FOLDER = "flexatar_storage";
    private static String FLEXATAR_FILES = "flexatar_files";

    public static void createFlexatarStorage(Context context){
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }
    }
    public static void addToStorage(Context context, byte[] flexatarData,String fId){

        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        File flexataFile = new File(flexatarStorageFolder,fId + ".flx");
        if (!flexataFile.exists()){
            addStorageRecord(context,fId);
            dataToFile(flexatarData,flexataFile);
        }
    }
    private static synchronized void addStorageRecord(Context context,String fId){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            jsonArray.put(fId);
            flexatarFilesString = jsonArray.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FLEXATAR_FILES, flexatarFilesString);
            editor.apply();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private static synchronized void removeRecord(Context context,String fid){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.getString(i).equals(fid)){
                    jsonArray.remove(i);
                    break;
                }
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FLEXATAR_FILES, jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    private static synchronized String[] getRecords(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            String[] result = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                result[i] = jsonArray.getString(jsonArray.length() - i - 1);

            }
            return result;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static void deleteFromStorage(Context context,File flexatarFile){
        if (flexatarFile.exists()){
            removeRecord(context,flexatarFile.getName().split("\\.")[0]);
            flexatarFile.delete();
        }

    }
    public static class FlexatarMetaData {
        public Bitmap previewImage;
        public String date;
        public String name;

    }
    public static FlexatarMetaData getFlexatarMetaData(File file){
        try {
            boolean isBuiltin = file.getName().startsWith("builtin");
            FileInputStream fileInputStream = new FileInputStream(file);


            boolean isHeader = true;
            String currentType = "";
            String name = "";
            String date = "";

            while (true) {
                byte[] buffer = new byte[8];
                int bytesRead = fileInputStream.read(buffer, 0, 8);
                if (bytesRead<=0) break;

                int packetLength = dataToIntArray(buffer)[0];
                buffer = new byte[packetLength];
                bytesRead = fileInputStream.read(buffer, 0, packetLength);
                if(isHeader) {
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(str);
                    currentType = jsonObject.getString("type");
//                    Log.d("unpackPreviewImage", jsonObject.toString());
                }
                if (currentType.equals("Info")&&!isHeader){
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(str);
                    name = jsonObject.has("name") ? jsonObject.getString("name") : "No Name";

                    String noDate = "";
                    if (!isBuiltin) {
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        noDate = currentDateTime.format(formatter);
                    }
                    date = jsonObject.has("date") ? jsonObject.getString("date") : noDate;
                    Log.d("FLX_INJECT",jsonObject.toString());
                }
                if (currentType.equals("PreviewImage")&&!isHeader){
                    fileInputStream.close();
                    Bitmap bitmapOrig = BitmapFactory.decodeStream(new ByteArrayInputStream(buffer));
                    Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOrig, (int)(bitmapOrig.getWidth()*0.5f), (int)(bitmapOrig.getHeight()*0.5f), false);
                    FlexatarMetaData flexatarMetaData = new FlexatarMetaData();
                    flexatarMetaData.previewImage = bitmap;
                    flexatarMetaData.name = name;
                    flexatarMetaData.date = date;
                    bitmapOrig.recycle();
                    return flexatarMetaData;
                }
                isHeader = !isHeader;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
        return null;
    }

    public static File[] getFlexatarFileList(Context context){
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        String[] fids = getRecords(context);
        File[] files = new File[fids.length];
//        Log.d("FLX_INJECT", "length files "+files.length);
        for (int i = 0; i < fids.length; i++) {
            files[i] = new File(flexatarStorageFolder,fids[i]+".flx");

            Log.d("FLX_INJECT", files[i].getAbsolutePath());
//            Log.d("FLX_INJECT", "lastModified "+files[i].lastModified());
        }

        return files;
    }
    public static File makeFileInFlexatarStorage(Context context,String fName){
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        return new File(flexatarStorageFolder,fName);
    }
    public static void clearStorage(Context context){
        SharedPreferences sharedPreferences = context.getSharedPreferences(PREF_STORAGE_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FLEXATAR_FILES, "[]");
        editor.apply();

        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        for(File f : flexatarStorageFolder.listFiles()) {
            if (f.getName().startsWith("user") || f.getName().startsWith("builtin"))
                f.delete();
        }
    }

    public static byte[] dataFromFile(File trgFile){

        if (!trgFile.exists()){
            return null;
        }
        int size = (int) trgFile.length();
        byte[] bytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(trgFile));
            buf.read(bytes, 0, bytes.length);
            buf.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
        Log.d("IntStorManager","file size " + size);
        return bytes;
    }

    public static void dataToFile( byte[] byteArray, File file) {
        if(file.exists()){
            file.delete();
        }
        try (FileOutputStream fos = new FileOutputStream(file)) {
            // Write the byte array to the file
            fos.write(byteArray);
            fos.close();
            Log.d("FLX_INJECT","Byte array written to file successfully.");
        } catch (IOException e) {
//            System.err.println("Error writing byte array to file: " + e.getMessage());
        }
    }
}
