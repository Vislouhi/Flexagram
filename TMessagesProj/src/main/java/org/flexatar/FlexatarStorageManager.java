package org.flexatar;

import static org.flexatar.DataOps.Data.dataToIntArray;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedUnpack;
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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;


public class FlexatarStorageManager {
    private static final String PREF_STORAGE_NAME = "flexatar_storage_pref";
    private static final String PREF_STORAGE_NAME_GROUP = "flexatar_storage_group_pref";
    private static final String PREF_STORAGE_NAME_CHOSEN = "flexatar_storage_pref";
    public static final String FLEXATAR_STORAGE_FOLDER = "flexatar_storage";
    public static final String FLEXATAR_PREVIEW_STORAGE_FOLDER = "flexatar_preview_storage";
    public static final String FLEXATAR_TMP_VIDEO_STORAGE_FOLDER = "flexatar_tmp_video_storage";
    public static final String PUBLIC_PREFIX = "public_";
    public static final String FLEXATAR_PREFIX = "flexatar_";
    public static final String BUILTIN_PREFIX = "builtin_";
    private static final String FLEXATAR_FILES = "flexatar_files";
    private static final String FLEXATAR_VIDEO_FILES = "flexatar_video_files";
    private static final String HIDDEN_FILES = "hidden_files";

//    public static final FlexatarChooser callFlexatarChooser = new FlexatarChooser("call",0.005f,0.0025f);
//    public static final FlexatarChooser roundFlexatarChooser = new FlexatarChooser("round",0.02f,0.0025f);
    public static FlexatarChooser[] callFlexatarChooser;

    public static FlexatarChooser[] roundFlexatarChooser;

    public static class FlexatarChooser {
        private static final String VIDEO = "video";
        private static final String FIRST = "first";
        private static final String SECOND = "second";
        private static final String FLX_TYPE = "flexatar_type";

        private final String tag;
        private final float d2;
        private final float d3;
        private final TimerAutoDestroy<FlxDrawer.GroupMorphState> groupTimer;
        private final int account;

        private File chosenVideoFile;
        private File first;
        private File second;

        private FlexatarData firstFlxData;
        private FlexatarData secondFlxData;
        private FlexatarData videoFlxData;
        private final Object flexatarDataLoadMutex = new Object();
        private final Object flexatarFileLoadMutex = new Object();
        private int effectIndex = -1;
        private float mixWeight = -1;
        private List<File> groupFiles = new ArrayList<>();
        private int flxType = -1;
        public int getFlxType(){
            if (flxType!=-1) return flxType;
            synchronized (flexatarFileLoadMutex) {

                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                flxType = sharedPreferences.getInt(FLX_TYPE, 0);
            }
            return flxType;
        }
        public FlexatarChooser(int account,String tag,float d2,float d3){
            this.account=account;
            this.tag=tag;
            this.d2=d2;
            this.d3=d3;
            groupTimer = new TimerAutoDestroy<FlxDrawer.GroupMorphState>();
            groupTimer.setValue(
                    new FlxDrawer.GroupMorphState()
            );
            groupTimer.onTimerListener = x ->{
                if (x.flexatarData == null){
                    x.flexatarData = getFirstFlxData();
                }

                if (!x.morphStage)
                    x.counter+=1;
                if (x.counter>x.changeDelta){
                    x.counter = 0;

                    if (x.flexatarCounter>=groupFiles.size()){
                        x.flexatarCounter=0;
                    }
//                    Log.d("FLX_INJECT","flx chooser file" + groupFiles.get(x.flexatarCounter));
                    FlexatarData.asyncFactory(groupFiles.get(x.flexatarCounter),fData->{
                        x.morphStage = true;
                        x.mixWeight = 0;
                        x.effectID = 0;
                        x.isEffectsOn = true;
                        x.flexatarDataAlt = x.flexatarData;
                        x.flexatarData = fData;

                        x.flexatarCounter+=1;
                    });
                }
                if (x.morphStage){
                    x.morphCounter+=1;
                    double w = (1d + Math.cos(Math.PI + Math.PI * (double) x.morphCounter / x.morphDelta)) / 2;
                    x.mixWeight = (float)w;
                    x.effectID = 0;
                    x.isEffectsOn = true;
                    if (x.morphCounter>x.morphDelta){
                        x.morphCounter = 0;
                        x.morphStage =false;
                        x.mixWeight = 1;
                        x.effectID = 0;
                        x.isEffectsOn = false;

                    }
                }

                return x;
            };
        }

        boolean newFlexatarLoaded = false;
        public FlexatarData getFirstFlxData() {
            synchronized (flexatarDataLoadMutex) {
                if (firstFlxData != null) return firstFlxData;

                firstFlxData = FlexatarData.syncFactory(getChosenFirst());
                newFlexatarLoaded = true;
                return firstFlxData;
            }
        }

        public FlexatarData getSecondFlxData() {
            synchronized (flexatarDataLoadMutex) {
                if (secondFlxData != null) return secondFlxData;
                secondFlxData = FlexatarData.syncFactory(getChosenSecond());
                return secondFlxData;
            }
        }
        public FlexatarData getVideoFlxData() {
            synchronized (flexatarDataLoadMutex) {
                if (videoFlxData != null) return videoFlxData;
                Log.d("FLX_INJECT","get video flexatar " + getChosenVideo());
                videoFlxData = FlexatarData.syncFactory(getChosenVideo());
                return videoFlxData;
            }
        }

        public void setEffectIndex(int effectIndex){
            effectId = effectIndex == 3 ? 1:0;
            synchronized (flexatarFileLoadMutex) {
                this.effectIndex = effectIndex;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt("EffectIndex", effectIndex);
                editor.apply();
            }
        }
        public int getEffectIndex(){
            if (effectIndex!=-1) return effectIndex;
            synchronized (flexatarFileLoadMutex) {
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                this.effectIndex = sharedPreferences.getInt("EffectIndex", 0);
                effectId = effectIndex == 3 ? 1:0;
                return this.effectIndex;
            }
        }
        public void setMixWeight(float mixWeight){
            this.mixWeight = mixWeight;
        }
        public void saveMixWeight(float mixWeight){
            synchronized (flexatarFileLoadMutex) {
                this.mixWeight = mixWeight;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat("MixWeight", mixWeight);
                editor.apply();
            }
        }
        public float getMixWeight(){
            if (mixWeight>=0) return mixWeight;
            synchronized (flexatarFileLoadMutex) {
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                this.mixWeight = sharedPreferences.getFloat("MixWeight", 0.5f);
                return this.mixWeight;
            }
        }
        private Timer mixWeightTimer = null;
        private float animatedMixWeight = 1f;
        private int effectId = 0;
        public int getEffectID(){
            return effectId;
        }
        public boolean isEffectOn(){
            return effectIndex != 0;
        }
        private final Object timerCreationSync = new Object();
        private int usageCounter = 10;
        public float getAnimatedMixWeight(){

            int effectIndex = getEffectIndex();
            if (effectIndex == 0 || effectIndex == 1) return mixWeight;
            usageCounter++;
            synchronized (timerCreationSync) {
                if (mixWeightTimer == null) {
                    mixWeightTimer = new Timer();
                    TimerTask task = new TimerTask() {
                        private float delta2 = d2;
                        private float delta3 = d3;
                        private int counter5 = 0;


                        @Override
                        public void run() {

                            counter5++;
                            if (counter5 > 10) {
                                counter5 = 0;
                                if (usageCounter == 0) {
                                    mixWeightTimer.cancel();
                                    mixWeightTimer.purge();
                                    mixWeightTimer = null;
                                }
                                usageCounter = 0;
                            }
                            if (getEffectIndex() == 2) {
                                animatedMixWeight += delta2;
                                if (animatedMixWeight > 1) {
                                    animatedMixWeight = 1f;
                                    delta2 = -delta2;
                                } else if (animatedMixWeight < 0) {
                                    animatedMixWeight = 0f;
                                    delta2 = -delta2;
                                }
                            } else if (getEffectIndex() == 3) {
                                animatedMixWeight += delta3;
                                if (delta3 < 0) delta3 = -delta3;
                                animatedMixWeight += delta3;
                                if (animatedMixWeight > 1) {

                                    animatedMixWeight -= 1f;
                                }
                            }
//                            Log.d("FLX_INJECT", "animatedMixWeight timer " + animatedMixWeight);
                        }

                    };


                    mixWeightTimer.scheduleAtFixedRate(task, 0, 40);
                }
            }

            return animatedMixWeight;
        }
        public void resetEffects(){
            setEffectIndex(0);
        }
        public void setChosenFlexatar(String path) {
            synchronized (flexatarFileLoadMutex) {
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                String oldFirstPath = sharedPreferences.getString(FIRST, null);

                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(FIRST, path);
                editor.putString(SECOND, oldFirstPath);
                editor.apply();
                first = new File(path);
                if (oldFirstPath != null)
                    second = new File(oldFirstPath);
                secondFlxData = firstFlxData;
                firstFlxData = null;
                setFlexatarGroup();
                Log.d("FLX_INJECT","change flexatar group size "+groupFiles.size());
            }
        }
        public void setChosenVideoFlexatar(String path) {
            synchronized (flexatarFileLoadMutex) {
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(VIDEO, path);
                editor.apply();
                chosenVideoFile = new File(path);
                videoFlxData = null;
                Log.d("FLX_INJECT","selected video flexatar: "+chosenVideoFile.getName());


            }
        }
        public void setFlexatarGroup(){
            String groupId = getChosenFirst().getName().replace(".flx", "");
            groupFiles = FlexatarStorageManager.getFlexatarGroupFileList(ApplicationLoader.applicationContext,account, groupId);
            if (groupFiles.size() != 0) {
                groupFiles.add(getChosenFirst());
            }
        }
        public File getChosenVideo() {
            synchronized (flexatarFileLoadMutex) {
                if (chosenVideoFile != null && chosenVideoFile.exists()) return chosenVideoFile;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                String videoPath = sharedPreferences.getString(VIDEO, null);

                if (videoPath == null  || !(new File(videoPath).exists()) ) {
                    File file = getVideoFlexatarFileList(context,account)[0];
                    chosenVideoFile = file;
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString(VIDEO, file.getAbsolutePath());
                    editor.apply();
                    return file;
                }
                chosenVideoFile = new File(videoPath);
                return chosenVideoFile;
            }
        }
        public File getChosenFirst() {
            synchronized (flexatarFileLoadMutex) {
                if (first != null && first.exists()) return first;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                String firstPath = sharedPreferences.getString(FIRST, null);
                if (firstPath == null) {
                    for (File file : getFlexatarFileList(context,account)) {
                        if (second == null) {
                            first = file;
                            return first;
                        }
                        if (!file.equals(second)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(FIRST, file.getAbsolutePath());
                            editor.apply();
                            first = file;
                            return first;
                        }
                    }
                    return first;
                }
                File firstFile = new File(firstPath);
                if (firstFile.exists()) {
                    first = firstFile;
                } else {
                    for (File file : getFlexatarFileList(context,account)) {
                        if (second == null) {
                            first = file;
                            break;
                        }
                        if (!file.equals(second)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(FIRST, file.getAbsolutePath());
                            editor.apply();
                            first = file;
                            break;
                        }
                    }
                }
                return first;
            }
        }

        public File getChosenSecond() {
            synchronized (flexatarFileLoadMutex) {
                if (second != null && second.exists()) return second;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                String firstPath = sharedPreferences.getString(SECOND, null);

                if (firstPath == null) {
                    for (File file : getFlexatarFileList(context,account)) {
                        if (first == null) {
                            second = file;
                            return second;
                        }
                        if (!file.equals(first)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SECOND, file.getAbsolutePath());
                            editor.apply();
                            second = file;
                            return second;
                        }
                    }
                    return second;
                }
                File firstFile = new File(firstPath);
                if (firstFile.exists()) {
                    second = firstFile;
                } else {
                    for (File file : getFlexatarFileList(context,account)) {
                        if (first == null) {
                            second = file;
                            break;
                        }
                        if (!file.equals(first)) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString(SECOND, file.getAbsolutePath());
                            editor.apply();
                            second = file;
                            break;
                        }
                    }
                }
                return second;
            }
        }

//        private List<WeakReference<FlxDrawer>> subscribers = new LinkedList<>();
        public void subscribe(FlxDrawer drawer){
           /* WeakReference<FlxDrawer> weakDrawer = new WeakReference<>(drawer);

            weakDrawer.get()*/
            setFlexatarGroup();
            newFlexatarLoaded = true;
            drawer.onFrameStartListener.set( ()-> new FlxDrawer.RenderParams(){{
                flexatarType = getFlxType();
//                Log.d("FLX_INJECT","flxTyrp: "+flexatarType);
                if (flexatarType == 0){
                    flexatarDataVideo = getVideoFlxData();
                }else {
                    mixWeight = getAnimatedMixWeight();
                    effectID = getEffectID();
                    isEffectsOn = isEffectOn();

                    if (!isEffectsOn) {
                        if (groupFiles.size() > 0) {
                            FlxDrawer.GroupMorphState timerVal = groupTimer.getValue();
                            mixWeight = timerVal.mixWeight;
                            effectID = timerVal.effectID;
                            isEffectsOn = timerVal.isEffectsOn;
                            flexatarData = timerVal.flexatarData;
                            flexatarDataAlt = timerVal.flexatarDataAlt;
//                        Log.d("FLX_INJECT"," flexatarData "+flexatarData);
//                        if (flexatarDataAlt == null) flexatarDataAlt = flexatarData;
                        } else {
                            flexatarData = getFirstFlxData();
                            flexatarDataAlt = getSecondFlxData();
                        }
                    } else {
                        flexatarData = getFirstFlxData();
                        flexatarDataAlt = getSecondFlxData();
                    }
                }

            }});
//            subscribers.add(weakDrawer);
        }


        public void setFlxType(int page) {
            synchronized (flexatarFileLoadMutex) {
                flxType = page;
                Context context = ApplicationLoader.applicationContext;
                String storageName = PREF_STORAGE_NAME_CHOSEN + tag + UserConfig.getInstance(account).clientUserId;
                SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putInt(FLX_TYPE, flxType);
                editor.apply();
//                String firstPath = sharedPreferences.getInt(FLX_TYPE, null);
            }
        }
    }

    public static File getFlexatarStorage(Context context,int account){
        File rootDir = context.getFilesDir();
        if (account == -1) account = UserConfig.selectedAccount;
        String userFolderName = "tg_" + UserConfig.getInstance(account).clientUserId;
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
    private final static String FLEXATAR_SEND_IMAGE_STORAGE_FOLDER = "send_image_storage";
    private final static String FLEXATAR_TMP_LOAD = "tmp_load";
    public static File createFlexatarSendImageStorage(Context context,int account){
        if (account==-1)account = UserConfig.selectedAccount;
        File rootDir = context.getFilesDir();
        String userFolderName = "tg_" + UserConfig.getInstance(account).clientUserId;
        File userFolder = new File(rootDir,userFolderName);
        if (!userFolder.exists()) userFolder.mkdir();

        File flexatarStorageFolder = new File(userFolder,FLEXATAR_SEND_IMAGE_STORAGE_FOLDER);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }

        return flexatarStorageFolder;
    }
    public static File createTmpLoadFlexatarStorage(Context context,int account){
        if (account==-1) account = UserConfig.selectedAccount;
        File rootDir = context.getFilesDir();
        String userFolderName = "tg_" + UserConfig.getInstance(account).clientUserId;
        File userFolder = new File(rootDir,userFolderName);
        if (!userFolder.exists()) userFolder.mkdir();

        File flexatarStorageFolder = new File(userFolder,FLEXATAR_TMP_LOAD);
        if (!flexatarStorageFolder.exists()){
            flexatarStorageFolder.mkdir();
        }

        return flexatarStorageFolder;
    }
    public static boolean checkIfTmpFileExists(String name,int account){
        return new File(createTmpLoadFlexatarStorage(ApplicationLoader.applicationContext,account),name).exists();
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
    public static File addToStorage(Context context,int account, byte[] flexatarData,String fId){


        return addToStorage(context,account,  flexatarData,fId, "flexatar_");

    }
    public static File addToStorage(Context context,int account, byte[] flexatarData,String fId,String prefix) {
        return addToStorage(context,account, flexatarData, fId, prefix, 1);
    }

    public static File addToStorage(Context context,int account, byte[] flexatarData,String fId,String prefix,int flexatarType){
        File flexatarStorageFolder = getFlexatarStorage(context,account);

//        File rootDir = context.getFilesDir();
//        File flexatarStorageFolder = new File(rootDir,FLEXATAR_STORAGE_FOLDER);
        fId = prefix+fId;
        String fileName = ServerDataProc.routToFileName(fId,"");
        File flexataFile = new File(flexatarStorageFolder,fileName);
        if (!flexataFile.exists()){
            if (flexatarType == 1) {
                addStorageRecord(context,account, fId);
                dataToFile(flexatarData,flexataFile);
            }else if (flexatarType == 0) {
                String videoFileName = fileName.replace(".flx",".mp4");
                File videoFile = new File(flexatarStorageFolder,videoFileName);
                byte[][] extractResult = extractVideo(flexatarData);
                Log.d("FLX_INJECT","flxPack size "+extractResult[0].length);
                Log.d("FLX_INJECT","flxVideo size "+extractResult[1].length);
                addStorageRecordVideo(context,account, fId);
                dataToFile(extractResult[0],flexataFile);
                dataToFile(extractResult[1],videoFile);

            }

        }
        return flexataFile;

    }

    public static File addToStorage(Context context,int account, File srcFile,String fId){


        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String fileName = ServerDataProc.routToFileName(fId,"flexatar_");
        File flexataFile = new File(flexatarStorageFolder,fileName);
        if (!flexataFile.exists()){

            addStorageRecord(context,account,fId);
            try {
                copy(srcFile, flexataFile);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }
        return flexataFile;
    }
    public static File addToStorage(Context context,int account, File srcFile){
        File videoFile = new File(srcFile.getAbsolutePath().replace(".flx", ".mp4"));
        boolean isVideo = videoFile.exists();
        String fid = srcFile.getName().replace(".flx","");
        Log.d("FLX_INJECT","add to gallery "+fid);
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String fileName = srcFile.getName();
        File flexataFile = new File(flexatarStorageFolder,fileName);

        if (!flexataFile.exists()){
            if (isVideo)
                addStorageRecordVideo(context,account,fid);
            else
                addStorageRecord(context,account,fid);
            try {
                copy(srcFile, flexataFile);
                if (isVideo){
                    File videoDstFile = new File(flexatarStorageFolder,videoFile.getName());
                    copy(videoFile, videoDstFile);
                }
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

    public static synchronized void addStorageRecord(Context context,int account,String fId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
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
    public static synchronized void addStorageRecordVideo(Context context,int account,String fId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_VIDEO_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            Log.d("FLX_INJECT","addStorageRecord fid "+ fId);
            jsonArray.put(fId);
            flexatarFilesString = jsonArray.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FLEXATAR_VIDEO_FILES, flexatarFilesString);
            editor.apply();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized void addStorageHiddenRecord(Context context,int account,String fId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(HIDDEN_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            Log.d("FLX_INJECT","addStorageRecord fid "+ fId);
            jsonArray.put(fId);
            flexatarFilesString = jsonArray.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(HIDDEN_FILES, flexatarFilesString);
            editor.apply();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized List<String> getGroups(Context context,int account){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPreferences.getAll();
        Set<String> keys = allEntries.keySet();

        return new ArrayList<String>(){{addAll(keys);}}.stream().sequential().map(x->x.replace("groupId_","")).collect(Collectors.toList());
    }
    public static synchronized void addGroupRecord(Context context,int account,String groupId,String fId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String groupKey = "groupId_"+groupId;
        String flexatarFilesString = sharedPreferences.getString(groupKey, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            Log.d("FLX_INJECT","addStorageRecord fid "+ fId);
            jsonArray.put(fId);
            flexatarFilesString = jsonArray.toString();
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(groupKey, flexatarFilesString);
            editor.apply();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized void moveGroupRecord(Context context,int account,String groupId,String fId,int direction){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String groupKey = "groupId_"+groupId;
        String flexatarFilesString = sharedPreferences.getString(groupKey, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.getString(i).equals(fId)){
                    String tmp = jsonArray.getString(i);
                    jsonArray.put(i,jsonArray.getString(i+direction));
                    jsonArray.put(i+direction,tmp);

                    break;
                }
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(groupKey, jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized int getGroupSize(Context context,int account,String groupId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String groupKey = "groupId_"+groupId;

        String flexatarFilesString = sharedPreferences.getString(groupKey, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            return jsonArray.length();
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static synchronized String[] getGroupRecords(Context context,int account,String groupId){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String groupKey = "groupId_"+groupId;

        String flexatarFilesString = sharedPreferences.getString(groupKey, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            String[] result = new String[jsonArray.length()];
            for (int i = 0; i < jsonArray.length(); i++) {
                result[i] = jsonArray.getString(i);

            }
            return result;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static synchronized void removeGroupRecord(Context context,int account,String groupId,String fid){
        String groupKey = "groupId_"+groupId;
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME_GROUP + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(groupKey, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.getString(i).equals(fid)){
                    jsonArray.remove(i);
                    break;
                }
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            if (jsonArray.length() == 0){
                editor.remove(groupKey);
            }else{
                editor.putString(groupKey, jsonArray.toString());
            }
            editor.apply();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static List<File> getFlexatarGroupFileList(Context context,int account,String groupId){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getGroupRecords(context,account,groupId);

        List<File> files = new ArrayList<>();
        for (int i = 0; i < fids.length; i++) {
            files.add(new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],"")));
        }
        return files;
    }
    private static synchronized void removeRecord(Context context,int account,String fid){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
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
    private static synchronized void removeVideoRecord(Context context,int account,String fid){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_VIDEO_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.getString(i).equals(fid)){
                    jsonArray.remove(i);
                    break;
                }
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(FLEXATAR_VIDEO_FILES, jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized void removeHiddenRecord(Context context,int account,String fid){
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(HIDDEN_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            for (int i = 0; i < jsonArray.length(); i++) {
                if(jsonArray.getString(i).equals(fid)){
                    jsonArray.remove(i);
                    break;
                }
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(HIDDEN_FILES, jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
    public static synchronized void clearHiddenRecord(Context context,int account){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(HIDDEN_FILES, "[]");
        editor.apply();
    }
    public static synchronized String[] getRecords(Context context,int account){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;

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
    public static synchronized String[] getVideoRecords(Context context,int account){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_VIDEO_FILES, "[]");
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
    public static synchronized List<String> getHiddenRecords(Context context,int account){
        if (account == -1) account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(HIDDEN_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                result.add(jsonArray.getString(jsonArray.length() - i - 1));

            }
            return result;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }
    public static synchronized List<String> getRecordsExcept(Context context,int account,List<String> except){
        if (account==-1)account=UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);
            List<String> result = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                String entry = jsonArray.getString(jsonArray.length() - i - 1);
                if (!except.contains(entry))
                    result.add(entry);

            }
            return result;

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

    }

    public static synchronized List<String> getSavedFids(String prefix,int account){
        Context context = ApplicationLoader.applicationContext;
        if (account==-1)account = UserConfig.selectedAccount;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;
        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        String flexatarFilesString = sharedPreferences.getString(FLEXATAR_FILES, "[]");
        String flexatarVideoFilesString = sharedPreferences.getString(FLEXATAR_VIDEO_FILES, "[]");
        flexatarFilesString = (flexatarFilesString+flexatarVideoFilesString).replace("][",",");
//        Log.d("FLX_INJECT","flexatarFilesString "+flexatarFilesString);
        try {
            JSONArray jsonArray =  new JSONArray(flexatarFilesString);

            List<String> result = new ArrayList<>();

            for (int i = 0; i < jsonArray.length(); i++) {
                String fid = jsonArray.getString(jsonArray.length() - i - 1);
                if (prefix == null){
                    if (fid.startsWith(PUBLIC_PREFIX)){
                        result.add(fid.replace(PUBLIC_PREFIX,""));
                    }
                    if (fid.startsWith(FLEXATAR_PREFIX)){
                        result.add(fid.replace(FLEXATAR_PREFIX,""));
                    }

                }else if (fid.startsWith(prefix)){

                    result.add(fid.replace(prefix,""));
                }


            }
            return result;

        } catch (JSONException e) {
            return new ArrayList<>();
        }

    }
    public static void deleteFromStorage(Context context,int account,File flexatarFile){
        deleteFromStorage(context,account,flexatarFile,false);
    }
    public static void deleteFromStorage(Context context,int account,File flexatarFile,boolean deleteOnCloaud){
        if (flexatarFile.exists()){
            File videoFile = new File(flexatarFile.getAbsolutePath().replace(".flx", ".mp4"));
            if (videoFile.exists()){
                removeVideoRecord(context, account, flexatarFile.getName().replace(".flx", ""));
                videoFile.delete();
            }else {
                removeRecord(context,account, flexatarFile.getName().replace(".flx", ""));
            }
            flexatarFile.delete();
        }
        if (deleteOnCloaud) {
            String ftarRout = ServerDataProc.fileNameToRout(flexatarFile.getName());
            if (ftarRout != null) {
                String deleteRout = ServerDataProc.genDeleteRout(ftarRout);
                FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), deleteRout, "DELETE", new FlexatarServerAccess.OnRequestJsonReady() {
                    @Override
                    public void onReady(FlexatarServerAccess.StdResponse response) {
                        Log.d("FLX_INJECT", "flexatar remote deletion success");
                    }

                    @Override
                    public void onError() {
                        Log.d("FLX_INJECT", "flexatar remote deletion error");
                    }
                });
//                    FlexatarServerAccess.lambdaRequest("/" + deleteRout, "DELETE", null, null, null);
            }
        }
    }
    public static void deleteVideoFromStorage(Context context,int account,File flexatarFile,boolean deleteOnCloaud){
        if (flexatarFile.exists()){
            removeVideoRecord(context,account,flexatarFile.getName().replace(".flx",""));
            if (deleteOnCloaud) {
                String ftarRout = ServerDataProc.fileNameToRout(flexatarFile.getName());
                if (ftarRout != null) {
                    String deleteRout = ServerDataProc.genDeleteRout(ftarRout);
                    FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(account), deleteRout, "DELETE", new FlexatarServerAccess.OnRequestJsonReady() {
                        @Override
                        public void onReady(FlexatarServerAccess.StdResponse response) {
                            Log.d("FLX_INJECT", "flexatar remote deletion success");
                        }

                        @Override
                        public void onError() {
                            Log.d("FLX_INJECT", "flexatar remote deletion error");
                        }
                    });
//                    FlexatarServerAccess.lambdaRequest("/" + deleteRout, "DELETE", null, null, null);
                }
            }
            File videoFile = new File(flexatarFile.getAbsolutePath().replace(".flx","mp4"));
            if (videoFile.exists()) videoFile.delete();
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
        public Data data;
        public String type;

        public Data toHeaderAsData(){
            if (data!=null) return data;
            Data infoHeader = new Data("{\"type\":\"Info\"}");
            infoHeader = infoHeader.encodeLengthHeader().add(infoHeader);
            Data headerData = new Data(metaDataToJson(this).toString());
            headerData = headerData.encodeLengthHeader().add(headerData);
            headerData = infoHeader.add(headerData);
            return headerData;
        }
        public int isVideo(){
            if (type == null) return 1;
            if (type.equals("photo")) return 1;
            return 0;
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
    public static byte[][] extractVideo(byte[] data){
        LengthBasedUnpack unpackedData = new LengthBasedUnpack(data);

        byte[] videoFileData = null;
        Data flxPackData = new Data(new byte[0]);
        for (int i = 0; i < unpackedData.bPacks.size()/2; i++) {
            try {
                String packType = unpackedData.asJson(i * 2).getString("type");

                if (packType.equals("video")){
                    videoFileData = unpackedData.bPacks.get(i * 2 + 1);
                }else{
                    Data pData1 = new Data(unpackedData.bPacks.get(i * 2));
                    pData1 = pData1.encodeLengthHeader().add(pData1);
                    Data pData2 = new Data(unpackedData.bPacks.get(i * 2 + 1));
                    pData2 = pData2.encodeLengthHeader().add(pData2);
                    flxPackData = flxPackData.add(pData1);
                    flxPackData = flxPackData.add(pData2);
                }
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        }
        return new byte[][]{flxPackData.value, videoFileData};
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
            if (md.type != null)
                mdJSON.put("type",md.type);
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
            if (jsonObject.has("type")) {
                md.type = jsonObject.getString("type");
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
//            boolean isBuiltin = file.getName().startsWith("builtin");
            FileInputStream fileInputStream = new FileInputStream(file);


            boolean isHeader = true;
            String currentType = "";
//            String name = "";
//            String date = "";
//            float[] mouthCalibration = null;
//            Float amplitude = null;
////            int headerLength = 0;
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

    public static void addDefaultFlexatars(int account){

        String[] flxFileNames = { "char6t", "char7t"};
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName+".p";

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,account,flxRaw,fName,"builtin_");

        }
    }
    public static void addDefaultVideoFlexatars(int account){
//        for(File f :getVideoFlexatarFileList(ApplicationLoader.applicationContext)){
//            deleteVideoFromStorage(ApplicationLoader.applicationContext,f,false);
//        }
//        deleteVideoFromStorage(ApplicationLoader.applicationContext,new File(getFlexatarStorage(ApplicationLoader.applicationContext),"builtin_test_flx.flx"),false);
        String[] flxFileNames = { "test_flx"};
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName+".p";

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(ApplicationLoader.applicationContext,account,flxRaw,fName,"builtin_",0);

        }
    }
    public static File[] getFlexatarFileList(Context context,int account){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getRecords(context,account);
        if (fids.length == 0){
            addDefaultFlexatars(account);
            fids = getRecords(context,account);
        }
        File[] files = new File[fids.length];
//        Log.d("FLX_INJECT", "length files "+files.length);
        for (int i = 0; i < fids.length; i++) {
            files[i] = new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],""));

//            Log.d("FLX_INJECT", files[i].getAbsolutePath());
//            Log.d("FLX_INJECT", "lastModified "+files[i].lastModified());
        }


        return files;
    }

    public static File[] getVideoFlexatarFileList(Context context,int account){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getVideoRecords(context,account);
        if (fids.length == 0){
            addDefaultVideoFlexatars(account);
            fids = getVideoRecords(context,account);
        }
        File[] files = new File[fids.length];
        for (int i = 0; i < fids.length; i++) {
            files[i] = new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],""));

        }


        return files;
    }

    public static List<File> getFlexatarFileListExcept(Context context,int account,List<String> except){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getRecordsExcept(context,account, except).toArray(new String[0]);
        if (fids.length == 0){
            addDefaultFlexatars(account);
            fids = getRecords(context,account);
        }
        List<File> files = new ArrayList<>();
//        Log.d("FLX_INJECT", "length files "+files.length);
        for (int i = 0; i < fids.length; i++) {
            files.add(new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],"")));

//            Log.d("FLX_INJECT", files[i].getAbsolutePath());
//            Log.d("FLX_INJECT", "lastModified "+files[i].lastModified());
        }


        return files;
    }

    public static File[] getFlexatarFileList(Context context,int account,String prefix){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getRecords(context,account);
        List<File> files = new ArrayList<>();
        for (int i = 0; i < fids.length; i++) {
            if (fids[i].startsWith(prefix))
                files.add(new File(flexatarStorageFolder,ServerDataProc.routToFileName(fids[i],"")));

        }


        return files.toArray(new File[0]);
    }

    public static File[] getVideoFlexatarFileList(Context context,int account,String prefix){
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        String[] fids = getVideoRecords(context,account);

        if (fids.length == 0){
            addDefaultVideoFlexatars(account);
            fids = getVideoRecords(context,account);
        }
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
    public static void clearStorage(int account){
        if (account==-1)account = UserConfig.selectedAccount;
        Context context = ApplicationLoader.applicationContext;
        String storageName = PREF_STORAGE_NAME + UserConfig.getInstance(account).clientUserId;

        SharedPreferences sharedPreferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FLEXATAR_FILES, "[]");
        editor.putString(FLEXATAR_VIDEO_FILES, "[]");
        editor.apply();

//        File rootDir = context.getFilesDir();
        File flexatarStorageFolder = getFlexatarStorage(context,account);
        File[] allStroageFiles = flexatarStorageFolder.listFiles();
        if (allStroageFiles == null) return;
        if (allStroageFiles.length == 0) return;
        for(File f : allStroageFiles) {
//            if (f.getName().startsWith("user") || f.getName().startsWith("builtin") || f.getName().startsWith("public")|| f.getName().startsWith("private"))
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
