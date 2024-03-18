package org.flexatar;



import android.content.Context;

import com.google.android.exoplayer2.util.Log;
import com.google.firebase.FirebaseApp;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

import org.flexatar.DataOps.FlexatarData;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FlexatarRenderer {
//    private static final int SAMPLE_RATE = 16000;

//    public static FlexatarData data;
    public static FlexatarAnimator animator;
//    public static Bitmap icon;
//    public static List<Bitmap> icons;
//    public static List<String> flexatarLinks;
//    public static FlexatarData currentFlxData;
//    public static FlexatarData altFlxData;
    public static float[] speechState = {0,0,0,0,0};
    public static boolean isFlexatarCamera = true;
//    private static AudioRecord audioRecord;
//    private static ExecutorService executor;
//    private static boolean isRecording;

    public static boolean isFrontFaceCamera;
//    public static float effectsMixWeight = 0.5f;
//    public static float chosenMixWeight = 0.5f;
//    public static boolean isMorphEffect = false;
//    public static int effectID = 0;
//    public static boolean isEffectsOn = false;



    public static void makeIcons() {

   //        TicketStorage.clearTickets();
//        icons = new ArrayList<>();


//        flexatarLinks = new ArrayList<>();
//        FlexatarStorageManager.clearStorage(AssetAccess.context);
       /* FlexatarServerAccess.lambdaRequest("list/1.00", "GET", null, null, new FlexatarServerAccess.CompletionListener() {
            @Override
            public void onReady(String string) {
                String[] links = ServerDataProc.getFlexatarLinkList(string, "public");
                String[] ids = ServerDataProc.getFlexatarIdList(string, "public");
//                String[] fids = FlexatarStorageManager.getRecords(ApplicationLoader.applicationContext);
//                Log.d("FLX_INJECT","fids "+Arrays.toString(fids));
                List<String> fids = FlexatarStorageManager.getSavedFids("public");

//                Log.d("FLX_INJECT","fids "+Arrays.toString(fids.toArray(new String[0])));
                List<String> linksToDownload = new ArrayList<>();
                List<String> idsToDownload = new ArrayList<>();
                for (int i = 0; i < ids.length; i++) {
                    if (!fids.contains(links[i])){
                        linksToDownload.add(links[i]);
                        idsToDownload.add(ids[i]);
                    }
                }
                Log.d("FLX_INJECT", "links count "+linksToDownload.size());
                if (linksToDownload.size()>0){
                    FlexatarServerAccess.downloadFlexatarListRecursive("builtin",linksToDownload,idsToDownload,0);
                }
//                fids.contains
//                FlexatarServerAccess.downloadFlexatarListRecursive(links,ids,0);
            }

            @Override
            public void onFail() {

            }
        });*/

//        String[] flxFileNames = {"android.p","android1.p","android2.p","android3.p","leo.p","leo1.p","auth2.p","char1t.p", "char2t.p", "char3t.p", "char4t.p", "char5t.p", "char6t.p", "char7t.p"};
//        String[] flxFileTypes = {"user","user","user","user","user","user","user","builtin", "builtin", "builtin", "builtin", "builtin", "builtin", "builtin"};

        /*for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];
            String fType = flxFileTypes[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName;

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(AssetAccess.context,flxRaw,fType+"___"+fName);

        }*/

       /* String[] flxFileNames = {"Geralt","Kianu","Kirk","Monica","Morgan","ObiVan","Oprah","Samuel", "Snoop", "Tony", "Valdemar"};
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName+".p";

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(AssetAccess.context,flxRaw,fName);

        }*/
        /*String[] flxFileNames = { "char6t", "char7t"};
        for (int i = 0; i < flxFileNames.length; i++) {
            String fName = flxFileNames[flxFileNames.length - i - 1];

            String flexatarLink = "flexatar/" + fName+".p";

            byte[] flxRaw = AssetAccess.dataFromFile(flexatarLink);
            FlexatarStorageManager.addToStorage(AssetAccess.context,flxRaw,fName,"builtin_");

        }*/
    }

   /* public static FlexatarData loadFlexatarByLink(String link) {
        byte[] flxRaw = AssetAccess.dataFromFile(link);
        LengthBasedFlxUnpack packages = new LengthBasedFlxUnpack(flxRaw);

        return new FlexatarData(packages);
    }*/

    public static void init() {
        FirebaseApp.initializeApp(ApplicationLoader.applicationContext);
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!Config.debugMode);
        animator = new FlexatarAnimator();
        FlexatarCommon.prepare();
//        VideoToTextureArray.decode();
//        FlexatarServiceAuth.integrityCheck();
//        Context context = ApplicationLoader.applicationContext;
//        FlexatarStorageManager.clearStorage();

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
    private static final Object processingMutex = new Object();
    public static void processSpeechAnimation(float[] audioBuffer){

        if(FlexatarNotificator.isMakingFlexatarRoundVideo) return;
//        Log.d("FLX_INJECT", "processSpeechAnimation");
        synchronized (processingMutex) {
            LaunchActivity context = LaunchActivity.instance;
            SpeechAnimation.loadModels(context);
            audioToTF.add(audioBuffer);
            if (audioToTF.size() == 5) {
                float[] buffer = concatenateFloatArrays(audioToTF);
                if (buffer.length == 800) {
                    FlexatarRenderer.speechState = SpeechAnimation.processAudio(concatenateFloatArrays(audioToTF));
//                    Log.d("FLX_INJECT", "anim voice call " + Arrays.toString(FlexatarRenderer.speechState));
                }else
                    Log.d("processSpeechAnimation", "incorect size");
                audioToTF.clear();
            }
        }



    }

    /*public static void startVoiceProcessing() {
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

    }*/
    /*public FlexatarRenderer(EglBase.Context glContext, Context context) {


    }*/
}
