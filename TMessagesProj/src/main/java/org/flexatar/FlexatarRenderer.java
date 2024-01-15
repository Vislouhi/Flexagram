package org.flexatar;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.flexatar.DataOps.AssetAccess;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.voip.VoIPService;
import org.telegram.ui.LaunchActivity;
import org.webrtc.EglBase;

import java.io.ByteArrayInputStream;
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
        String[] flxFileNames = {"char1t.p", "char2t.p", "char3t.p", "char4t.p", "char5t.p", "char6t.p", "char7t.p"};
        icons = new ArrayList<>();
        flexatarLinks = new ArrayList<>();
        for (String fName : flxFileNames) {
            String flexatarLink = "flexatar/" + fName;
            flexatarLinks.add(flexatarLink);
            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            LengthBasedFlxUnpack packages = new LengthBasedFlxUnpack(flxRaw);
            FlexatarData flxData = new FlexatarData(packages);
            byte[] previewImageData = flxData.flxData.get("exp0").get("PreviewImage").get(0);
            InputStream inputStream = new ByteArrayInputStream(previewImageData);
            icons.add(BitmapFactory.decodeStream(inputStream));
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
        currentFlxData = loadFlexatarByLink(flexatarLinks.get(FlexatarUI.chosenFirst));
        altFlxData = loadFlexatarByLink(flexatarLinks.get(FlexatarUI.chosenSecond));
    }

    public static boolean isFlexatarRendering = false;
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
            isFlexatarRendering = false;
        }
    }

    public static void startVoiceProcessing() {
        synchronized (mutexObject) {
            if (isVoiceProcessingOn) return;
            isVoiceProcessingOn = true;

            LaunchActivity context = LaunchActivity.instance;
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            SpeechAnimation.loadModels(context);
//        SpeechAnimation.checkModel();
            int bufferSizeInBytes = 800 * 2;
            audioRecord = new AudioRecord(MediaRecorder.AudioSource.VOICE_COMMUNICATION, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSizeInBytes);
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

                    if (VoIPService.getSharedInstance()!=null && !VoIPService.getSharedInstance().isMicMute()) {
                        FlexatarRenderer.speechState = SpeechAnimation.processAudio(VPUtil.shortToFloat(audioBuffer));
                    }else{
                        FlexatarRenderer.speechState = new float[]{0,0,0.05f,0,0};
                    }
//                  Log.d("FLX_ANIM", Arrays.toString(FlexatarRenderer.speechState));


                }

            });
        }

    }
    public FlexatarRenderer(EglBase.Context glContext, Context context) {


    }
}
