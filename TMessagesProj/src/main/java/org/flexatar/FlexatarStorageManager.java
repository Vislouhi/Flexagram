package org.flexatar;

import static org.flexatar.DataOps.Data.dataToIntArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.flexatar.DataOps.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.UserConfig;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;


public class FlexatarStorageManager {
    private static String PREF_STORAGE_NAME = "flexatar_storage_pref";
    public static String FLEXATAR_STORAGE_FOLDER = "flexatar_storage";
    public static String FLEXATAR_PREVIEW_STORAGE_FOLDER = "flexatar_preview_storage";
    public static String FLEXATAR_TMP_VIDEO_STORAGE_FOLDER = "flexatar_tmp_video_storage";
    public static String PUBLIC_PREFIX = "public_";
    public static String FLEXATAR_PREFIX = "flexatar_";
    public static String BUILTIN_PREFIX = "builtin_";
    private static String FLEXATAR_FILES = "flexatar_files";

    public static File getFlexatarStorage(Context context){
        File rootDir = context.getFilesDir();
        String userFolderName = "tg_" + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        File userFolder = new File(rootDir,userFolderName);
        if (!userFolder.exists()) userFolder.mkdir();

        File flexatarStorageFolder = new File(userFolder,FLEXATAR_STORAGE_FOLDER);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }
        return flexatarStorageFolder;
    }
    public static File createFlexatarPreviewStorage(Context context){
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_PREVIEW_STORAGE_FOLDER);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }
        return flexatarStorageFolder;
    }
    public static File createTmpVideoStorage(){
        Context context = ApplicationLoader.applicationContext;
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_TMP_VIDEO_STORAGE_FOLDER);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }
        return flexatarStorageFolder;
    }
    public static File addToStorage(Context context, byte[] flexatarData,String fId){


        return addToStorage(context,  flexatarData,fId, "flexatar_");

    }
    public static File addToStorage(Context context, byte[] flexatarData,String fId,String prefix){
        File flexatarStorageFolder = getFlexatarStorage(context);

//        File rootDir = context.getFilesDir();
//        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        fId = prefix+fId;
        String fileName = ServerDataProc.routToFileName(fId,"");
        File flexataFile = new File(flexatarStorageFolder,fileName);
        if (!flexataFile.exists()){

            addStorageRecord(context,fId);
            dataToFile(flexatarData,flexataFile);
        }
        return flexataFile;

    }

    public static File addToStorage(Context context, File srcFile,String fId){


        File flexatarStorageFolder = getFlexatarStorage(context);
        String fileName = ServerDataProc.routToFileName(fId,"flexatar_");
        File flexataFile = new File(flexatarStorageFolder,fileName);
        if (!flexataFile.exists()){

            addStorageRecord(context,fId);
            try {
                copy(srcFile, flexataFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return flexataFile;
    }
    public static void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        try {
            OutputStream out = new FileOutputStream(dst);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

    public static synchronized void addStorageRecord(Context context,String fId){
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            Log.d("FLX_INJECT","addStorageRecord fid "+ fId);
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
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
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
    public static synchronized String[] getRecords(Context context){
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
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

    public static synchronized List<String> getSavedFids(String prefix){
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String fid = jsonArray.getString(jsonArray.length() - i - 1);
                if (fid.startsWith(prefix)){

                    result.add(fid.replace(prefix,""));
                }


            }
            return result;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static void deleteFromStorage(Context context,File flexatarFile){
        deleteFromStorage(context,flexatarFile,true);


    }
    public static void deleteFromStorage(Context context,File flexatarFile,boolean deleteOnCloaud){
        if (flexatarFile.exists()){
            removeRecord(context,flexatarFile.getName().replace(".flx",""));
            if (deleteOnCloaud) {
                String ftarRout = ServerDataProc.fileNameToRout(flexatarFile.getName());
                if (ftarRout != null) {
                    String deleteRout = ServerDataProc.genDeleteRout(ftarRout);

                    FlexatarServerAccess.lambdaRequest("/" + deleteRout, "DELETE", null, null, null);
                }
            }
            flexatarFile.delete();
        }

    }
    public static File storePreviewImage(File file){
        File imageFile = null;
//        if (imageFile.exists()) return imageFile;
        try {
//            FlexatarMetaData flexatarMetaData = new FlexatarMetaData();

            FileInputStream fileInputStream = new FileInputStream(file);


            boolean isHeader = true;
            String currentType = "";

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
                }
                if (currentType.equals("Info")&&!isHeader) {
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    imageFile = new File(createFlexatarPreviewStorage(ApplicationLoader.applicationContext),FlexatarStorageManager.jsonToMetaData(str).name+".jpg");

                }
                if ( currentType.equals("PreviewImage")&&!isHeader){
                    fileInputStream.close();
//                    imageFile = new File(createFlexatarPreviewStorage(ApplicationLoader.applicationContext),file.getName()+".jpg");
                    dataToFile(buffer,imageFile);

                    return imageFile;
                }
                isHeader = !isHeader;
            }
        } catch (IOException | JSONException e) {
            return null;
        }
        return null;
    }
    public static class FlexatarMetaData {
        public Bitmap previewImage;
        public String date;
        public String name;
        public Float amplitude;
        public float[] mouthCalibration;

        public Data toHeaderAsData(){
            Data infoHeader = new Data("{\"type\":\"Info\"}");
            infoHeader = infoHeader.encodeLengthHeader().add(infoHeader);
            Data headerData = new Data(metaDataToJson(this).toString());
            headerData = headerData.encodeLengthHeader().add(headerData);
            headerData = infoHeader.add(headerData);
            return headerData;
        }

    }
    public static byte[] rewriteFlexatarHeader(File flexatarFile, FlexatarMetaData metaData){
        byte[] remainingBytes;
        try (RandomAccessFile file = new RandomAccessFile(flexatarFile, "rw")) {
            file.seek(0);
            byte[] lengthHeaderBytes = new byte[8];
            file.read(lengthHeaderBytes);
            long lengthHeader1 = Data.decodeLengthHeader(lengthHeaderBytes);
            file.seek(8+lengthHeader1);

            file.read(lengthHeaderBytes);
            long lengthHeader2 = Data.decodeLengthHeader(lengthHeaderBytes);

            int bytesToDelete = (int) (8+lengthHeader1+8+lengthHeader2);

            long fileLength = file.length();

            remainingBytes = new byte[(int) (fileLength - bytesToDelete)];
            file.seek(bytesToDelete);
            file.read(remainingBytes);

            Data newMetaData = metaData.toHeaderAsData();
            int newLength = newMetaData.value.length + remainingBytes.length;
            file.setLength(newLength);
            file.seek(0);
            file.write(newMetaData.value);
            file.seek(newMetaData.value.length);
            file.write(remainingBytes);
            return newMetaData.value;

        } catch (IOException e) {
            return null;
        }
       /* flexatarFile.delete();
        Data newMetaData = metaData.toHeaderAsData();

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(flexatarFile);
            fos.write(newMetaData.value);
            fos.write(remainingBytes);
            fos.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
*/
    }
    public static JSONObject metaDataToJson(FlexatarMetaData md){

        try {
            JSONObject mdJSON = new JSONObject();
            mdJSON.put("name",md.name);
            mdJSON.put("date",md.date);
            if (md.mouthCalibration != null){
                JSONArray mouthCalibrationJson = new JSONArray();
                for (float v:md.mouthCalibration)
                    mouthCalibrationJson.put(v);
                mdJSON.put("mouth_calibration",mouthCalibrationJson);

            }
            if (md.amplitude != null)
                mdJSON.put("amplitude",md.amplitude);
            Log.d("FLX_INJECT", "meta json "+mdJSON.toString());
            return mdJSON;
        } catch (JSONException e) {
            return null;
        }
    }
    public static FlexatarMetaData jsonToMetaData(String json){
        FlexatarMetaData md = new FlexatarMetaData();
        try {
            JSONObject jsonObject = new JSONObject(json);
            md.name = jsonObject.has("name") ? jsonObject.getString("name") : "No Name";

            String noDate = "";

            md.date = jsonObject.has("date") ? jsonObject.getString("date") : noDate;

            if (jsonObject.has("mouth_calibration")) {
                JSONArray jsonArr = jsonObject.getJSONArray("mouth_calibration");
                md.mouthCalibration = new float[jsonArr.length()];
                for (int i = 0; i < jsonArr.length(); i++) {
                    md.mouthCalibration[i] = (float) jsonArr.getDouble(i);
                }
            }
            if (jsonObject.has("amplitude")) {
                md.amplitude = (float) jsonObject.getDouble("amplitude");
            }
            return md;
        } catch (JSONException e) {
            return null;
        }
    }
    public static FlexatarMetaData getFlexatarMetaData(File file,boolean loadPreviewImage){
        if (!file.exists()) return null;
        try {
            FlexatarMetaData flexatarMetaData = new FlexatarMetaData();
            boolean isBuiltin = file.getName().startsWith("builtin");
            FileInputStream fileInputStream = new FileInputStream(file);


            boolean isHeader = true;
            String currentType = "";
            String name = "";
            String date = "";
            float[] mouthCalibration = null;
            Float amplitude = null;
//            int headerLength = 0;
            while (true) {
                byte[] buffer = new byte[8];
                int bytesRead = fileInputStream.read(buffer, 0, 8);
//                headerLength+=bytesRead;
                if (bytesRead<=0) break;

                int packetLength = dataToIntArray(buffer)[0];
                buffer = new byte[packetLength];
                bytesRead = fileInputStream.read(buffer, 0, packetLength);
//                headerLength+=bytesRead;
                if(isHeader) {
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    JSONObject jsonObject = new JSONObject(str);
                    currentType = jsonObject.getString("type");
//                    Log.d("unpackPreviewImage", jsonObject.toString());
                }
                if (currentType.equals("Info")&&!isHeader){
                    String str = new String(buffer, StandardCharsets.UTF_8);
                    flexatarMetaData = FlexatarStorageManager.jsonToMetaData(str);

                    /*JSONObject jsonObject = new JSONObject(str);
                    name = jsonObject.has("name") ? jsonObject.getString("name") : "No Name";

                    String noDate = "";
                    if (!isBuiltin) {
                        LocalDateTime currentDateTime = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                        noDate = currentDateTime.format(formatter);
                    }
                    date = jsonObject.has("date") ? jsonObject.getString("date") : noDate;

                    if (jsonObject.has("mouth_calibration")) {
                        JSONArray jsonArr = jsonObject.getJSONArray("mouth_calibration");
                        mouthCalibration = new float[jsonArr.length()];
                        for (int i = 0; i < jsonArr.length(); i++) {
                            mouthCalibration[i] = (float) jsonArr.getDouble(i);
                        }
                    }
                    if (jsonObject.has("amplitude")) {
                        amplitude = (float) jsonObject.getDouble("amplitude");
                    }*/


//                    Log.d("FLX_INJECT",jsonObject.toString());
                }
                if ( currentType.equals(loadPreviewImage ? "PreviewImage":"Info")&&!isHeader){
                    fileInputStream.close();

                    if (loadPreviewImage) {
                        Bitmap bitmapOrig = BitmapFactory.decodeStream(new ByteArrayInputStream(buffer));
                        Bitmap bitmap = Bitmap.createScaledBitmap(bitmapOrig, (int) (bitmapOrig.getWidth() * 0.5f), (int) (bitmapOrig.getHeight() * 0.5f), false);
                        flexatarMetaData.previewImage = bitmap;
                        bitmapOrig.recycle();
                    }
//                    flexatarMetaData.name = name;
//                    flexatarMetaData.date = date;
//                    flexatarMetaData.mouthCalibration = mouthCalibration;
//                    flexatarMetaData.amplitude = amplitude;

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
        File flexatarStorageFolder = getFlexatarStorage(context);
        String[] fids = getRecords(context);
        File[] files = new File[fids.length];
//        Log.d("FLX_INJECT", "length files "+files.length);
        for (int i = 0; i < fids.length; i++) {
            files[i] = new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],""));

//            Log.d("FLX_INJECT", files[i].getAbsolutePath());
//            Log.d("FLX_INJECT", "lastModified "+files[i].lastModified());
        }


        return files;
    }
    public static File[] getFlexatarFileList(Context context,String prefix){
        File flexatarStorageFolder = getFlexatarStorage(context);
        String[] fids = getRecords(context);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fids.length; i++) {
            if (fids[i].startsWith(prefix))
                files.add(new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],"")));

        }


        return files.toArray(new File[0]);
    }
    /*public static String getFlexatarNameByPreviewFileName(String fileName){
        File rootDir = ApplicationLoader.applicationContext.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        fileName = fileName.substring(0, fileName.length() - 4);
        File flexatarFile = new File(flexatarStorageFolder,fileName);
        return getFlexatarMetaData(flexatarFile,false).name;

    }*/
    /*public static File[] getFlexatarFileList(String prefix){
        Context context = ApplicationLoader.applicationContext;
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        String[] fids = getRecords(context,prefix);
        File[] files = new File[fids.length];
//        Log.d("FLX_INJECT", "length files "+files.length);
        for (int i = 0; i < fids.length; i++) {
            files[i] = new File(flexatarStorageFolder,fids[i]+".flx");
        }

        return files;
    }*/
    /*public static File makeFileInFlexatarStorage(Context context,String fName){
        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        return new File(flexatarStorageFolder,fName);
    }*/
    public static void clearStorage(){
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(UserConfig.selectedAccount).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FLEXATAR_FILES, "[]");
        editor.apply();

//        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = getFlexatarStorage(context);
        File[] allStroageFiles = flexatarStorageFolder.listFiles();
        if (allStroageFiles == null) return;
        if (allStroageFiles.length == 0) return;
        for(File f : allStroageFiles) {
            if (f.getName().startsWith("user") || f.getName().startsWith("builtin") || f.getName().startsWith("public")|| f.getName().startsWith("private"))
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
