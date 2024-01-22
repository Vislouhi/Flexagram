package org.flexatar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;

import androidx.core.app.ActivityCompat;

import com.google.android.exoplayer2.util.Log;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.Data;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.voip.NativeInstance;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.ui.LaunchActivity;
import org.webrtc.EglBase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlexatarRenderer {
    private static final int SAMPLE_RATE = 16000;
    public static FlxDrawer drawer;
    public static FlexatarData data;
    public static FlexatarAnimator animator;
    public static Bitmap icon;
    public static List<Bitmap> icons;
    public static List<String> flexatarLinks;
    public static FlexatarData currentFlxData;
    public static FlexatarData altFlxData;
    public static float[] speechState = {0,0,0,0,0};
    private static AudioRecord audioRecord;
    private static ExecutorService executor;
    private static boolean isRecording;

    public static boolean isFrontFaceCamera;
    public static float effectsMixWeight = 0.5f;
    public static float chosenMixWeight = 0.5f;
    public static boolean isMorphEffect = false;
    public static int effectID = 0;
    public static boolean isEffectsOn = false;



    public static void makeIcons() {

        String[] flxFileNames = {"android.p","leo.p","leo1.p","auth2.p","char1t.p", "char2t.p", "char3t.p", "char4t.p", "char5t.p", "char6t.p", "char7t.p"};
        String[] flxFileTypes = {"user","user","user","user","builtin", "builtin", "builtin", "builtin", "builtin", "builtin", "builtin"};

        icons = new ArrayList<>();
//        flexatarLinks = new ArrayList<>();
//        FlexatarStorageManager.clearStorage(AssetAccess.context);
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];
            String fType = flxFileTypes[flxFileNames.length - i - 1];
//        }
//        for (String fName : flxFileNames) {
            String flexatarLink = "flexatar/" + fName;
//            flexatarLinks.add(flexatarLink);
            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(AssetAccess.context,flxRaw,fType+"___"+fName.split("\\.")[0]);
//            LengthBasedFlxUnpack packages = new LengthBasedFlxUnpack(flxRaw);
//            FlexatarData flxData = new FlexatarData(packages);
//            byte[] previewImageData = flxData.flxData.get("exp0").get("PreviewImage").get(0);
//            byte[] previewImageData = Data.unpackPreviewImage(AssetAccess.context,flexatarLink);
//            InputStream inputStream = new ByteArrayInputStream(previewImageData);
//            icons.add(BitmapFactory.decodeStream(inputStream));
        }
    }

    public static FlexatarData loadFlexatarByLink(String link) {
        byte[] flxRaw = AssetAccess.dataFromFile(link);
        LengthBasedFlxUnpack packages = new LengthBasedFlxUnpack(flxRaw);

        return new FlexatarData(packages);
    }

    public static void init() {
        animator = new FlexatarAnimator();
        FlexatarCommon.prepare();
        makeIcons();
        File[] flexatarFiles = FlexatarStorageManager.getFlexatarFileList(AssetAccess.context);
        if (FlexatarUI.chosenFirst == null){
            FlexatarUI.chosenFirst = flexatarFiles[0];
            FlexatarUI.chosenSecond = flexatarFiles[1];
        }
        currentFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenFirst)));
        altFlxData = new FlexatarData(new LengthBasedFlxUnpack(FlexatarStorageManager.dataFromFile(FlexatarUI.chosenSecond)));
//        currentFlxData = loadFlexatarByLink(flexatarLinks.get(FlexatarUI.chosenFirst));
//        altFlxData = loadFlexatarByLink(flexatarLinks.get(FlexatarUI.chosenSecond));
    }

//    public static boolean isFlexatarRendering = false;
    public static boolean isVoiceProcessingNeed = false;
    private static Timer checkTmer;
    private static final Object mutexObject = new Object();
    public static void startVoiceProcessingIfNotRunning(){
        synchronized (mutexObject) {
            isVoiceProcessingNeed = true;
            if (checkTmer == null) {
                checkTmer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        if (!isVoiceProcessingNeed) {
                            stopAnimateSpeech();
                            if (checkTmer != null) {
                                checkTmer.cancel();
                                checkTmer.purge();
                                checkTmer = null;
                            }
                        }
                        isVoiceProcessingNeed = false;
                    }
                };
                checkTmer.scheduleAtFixedRate(task, 0, 1000);
            }
            startVoiceProcessing();
        }

    }
    private static boolean isVoiceProcessingOn = false;
    public static void stopAnimateSpeech(){
        synchronized (mutexObject) {
            if (audioRecord != null) {
                try {
                    audioRecord.stop();
                    audioRecord.release();
                }catch (IllegalStateException ignored){

                }
            }
            if (executor != null)
                executor.shutdown();
            audioRecord = null;
            executor = null;
            isVoiceProcessingOn = false;
            isRecording = false;
//            isFlexatarRendering = false;
        }
    }
    private static List<float[]> audioToTF = new ArrayList<>();

    private static float[] concatenateFloatArrays(List<float[]> arrays) {
        // Calculate the total length of the concatenated array
        int totalLength = 0;
        for (float[] array : arrays) {
            totalLength += array.length;
        }

        // Create the concatenated array
        float[] resultArray = new float[totalLength];

        // Copy individual arrays to the concatenated array
        int currentIndex = 0;
        for (float[] array : arrays) {
            System.arraycopy(array, 0, resultArray, currentIndex, array.length);
            currentIndex += array.length;
        }

        return resultArray;
    }
    private static Object processingMutex = new Object();
    public static void processSpeechAnimation(float[] audioBuffer){
//        Log.d("FLX_INJECT", "processSpeechAnimation");
        synchronized (processingMutex) {
            LaunchActivity context = LaunchActivity.instance;
            SpeechAnimation.loadModels(context);
            audioToTF.add(audioBuffer);
            if (audioToTF.size() == 5) {
                float[] buffer = concatenateFloatArrays(audioToTF);
                if (buffer.length == 800)
                    FlexatarRenderer.speechState = SpeechAnimation.processAudio(concatenateFloatArrays(audioToTF));
                else
                    Log.d("processSpeechAnimation", "incorect size");
                audioToTF.clear();
            }
        }



    }

    public static void startVoiceProcessing() {
//        VoIPService.getSharedInstance().setMicMute();
        if(true) return;
        synchronized (mutexObject) {
            if (isVoiceProcessingOn) return;
            isVoiceProcessingOn = true;

            LaunchActivity context = LaunchActivity.instance;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            SpeechAnimation.loadModels(context);

            int bufferSizeInBytes = 800 * 2;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
//            AcousticEchoCanceler canceler =  AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
//            canceler.setEnabled(true);

            executor = Executors.newSingleThreadExecutor();

            short[] audioBuffer = new short[800];
            audioRecord.startRecording();



            executor.execute(() -> {
                while (true) {
                    if (audioRecord == null)
                        break;
                    int bytesRead = audioRecord.read(audioBuffer, 0, 800, AudioRecord.READ_BLOCKING);
                    if (bytesRead < 0) {
                        break;
                    }

//                    if (VoIPService.getSharedInstance()!=null && !VoIPService.getSharedInstance().isMicMute()) {
                        FlexatarRenderer.speechState = SpeechAnimation.processAudio(VPUtil.shortToFloat(audioBuffer));
//                    }else{
//                        FlexatarRenderer.speechState = new float[]{0,0,0.05f,0,0};
//                    }
//                  Log.d("FLX_ANIM", Arrays.toString(FlexatarRenderer.speechState));


                }

            });
        }

    }
    public FlexatarRenderer(EglBase.Context glContext, Context context) {


    }
}
