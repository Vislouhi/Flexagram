package org.flexatar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.FileObserver;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.video.FallbackStrategy;
import androidx.camera.video.FileOutputOptions;
import androidx.camera.video.Quality;
import androidx.camera.video.QualitySelector;
import androidx.camera.video.Recorder;
import androidx.camera.video.Recording;
import androidx.camera.video.VideoCapture;
import androidx.camera.video.VideoRecordEvent;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.util.Consumer;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.common.util.concurrent.ListenableFuture;

import org.flexatar.DataOps.Data;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileStreamLoadOperation;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.messenger.video.VideoPlayerHolderBase;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.VideoPlayer;
import org.telegram.ui.LaunchActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutionException;

public class FlexatarVideoCapFragment extends BaseFragment implements LifecycleOwner {
    private LifecycleRegistry lifecycleRegistry;
    private FrameLayout frameLayout;
    private PreviewView mPreviewView;
    private ProcessCameraProvider cameraProvider;
    private ImageView takePhotoButton;
    private VideoCapture<Recorder> videoCapture;
    private Recording recording;
    private TextView timeView;
    private VideoPlayer videoPlayer;
    private TextureView textureView;
    private FileObserver fileObserver;

    public void createNewLifecycle(){
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);
    }
    @SuppressLint("AppCompatCustomView")
    public View createView(Context context) {
        createNewLifecycle();

        LaunchActivity.instance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("FlexatarCapture", R.string.FlexatarCapture));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
//                if (id == 10){
//                    showStartUpAlert(true);
//                }
            }
        });
//        ActionBarMenu menu = actionBar.createMenu();
//        ActionBarMenuItem otherItem = menu.addItem(10, R.drawable.msg_help);
//        otherItem.setContentDescription("Help");

        fragmentView = new FrameLayout(context);
        frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Color.BLACK);

        makeRecordLayout();

        return fragmentView;
    }
    private void makeRecordLayout(){
        mPreviewView = new PreviewView(getContext());
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));
        mPreviewView.setLayoutParams(layoutParams);

        frameLayout.addView(mPreviewView);

        TextView helpTextView = new TextView(getContext());
        helpTextView.setTextColor(Color.WHITE);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        helpTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        helpTextView.setText(LocaleController.getString("TakeShortVideo",R.string.TakeShortVideo));
        frameLayout.addView(helpTextView,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT,Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,0,0,0,50+72+12+20));



        startCamera();
        final RectF bgRect = new RectF();
        Paint paint = new Paint();
        paint.setARGB(255, 255, 0, 0);
        float circleSize = 0.7f;
        takePhotoButton = new ImageView(getContext()){
            @Override
            protected void onDraw(Canvas canvas) {
                if (recording!=null) {
                    float width = circleSize * getWidth();
                    float height = circleSize * getHeight();
                    float left = getWidth() * (1f - circleSize) / 2;
                    float top = getWidth() * (1f - circleSize) / 2;
                    bgRect.set(left, top, left + width, top + height);
//                bgRect.set(getWidth()*(1f - circleSize)/2, getHeight()*(1f - circleSize)/2, width, height);
                    canvas.drawRoundRect(bgRect, width / 2, height / 2, paint);
                }
                super.onDraw(canvas);
            }
        };
        takePhotoButton.setImageResource(R.drawable.camera_btn);
        FrameLayout.LayoutParams takePhotoParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(72),AndroidUtilities.dp(72));
        takePhotoParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        takePhotoParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(50));
        takePhotoButton.setLayoutParams(takePhotoParams);
        takePhotoButton.setOnClickListener((l)->{
            if (recording == null) {
                AndroidUtilities.runOnUIThread(()->frameLayout.removeView(helpTextView));
                timeView.setVisibility(View.VISIBLE);
                timeView.setText("00 : 00 / (00 : 15)");
                Log.d("FLX_INJECT","start video record");
                File videoFile = new File(FlexatarStorageManager.createTmpVideoStorage(), "saved_video.mp4");
                fileObserver = new FileObserver(videoFile) {

                    @Override
                    public void onEvent(int event, String path) {
//                        Log.d("FLX_INJECT","video file ready" + event);
                        if (event == FileObserver.CLOSE_WRITE) {
                            stopWatching();
                            AndroidUtilities.runOnUIThread(()-> {
                                        Log.d("FLX_INJECT","video file ready");
                                        startPlayback();
                                    }
                            );
                        }
                    }
                };

                fileObserver.startWatching();


                FileOutputOptions options = new FileOutputOptions.Builder(videoFile)

                        .build();

                recording = videoCapture.getOutput().prepareRecording(getContext(), options).start(ContextCompat.getMainExecutor(getContext()), new Consumer<VideoRecordEvent>() {
                    private long seconds = -1;

                    @Override
                    public void accept(VideoRecordEvent videoRecordEvent) {
                        long cSeconds = videoRecordEvent.getRecordingStats().getRecordedDurationNanos()/1_000_000_000L;
                        if (seconds!=cSeconds){
                            seconds = cSeconds;
                            AndroidUtilities.runOnUIThread(()-> {
                                {
                                    String time = (seconds<10?"0":"")+seconds;
                                    timeView.setText("00 : "+time+" / (00 : 15)");
                                }
                            });
                            if (seconds == 15){
                                recording.stop();
                                recording.close();
                                recording = null;
                                AndroidUtilities.runOnUIThread(()-> {
                                    {
                                        takePhotoButton.invalidate();
                                        timeView.setVisibility(View.INVISIBLE);
                                    }
                                });

                            }
                            Log.d("FLX_INJECT","recorded seconds : "+seconds);
                        }
                    }

                });
                takePhotoButton.invalidate();
            }else{
                Log.d("FLX_INJECT","stop video record");

                recording.stop();
                recording.close();
                recording = null;
                timeView.setVisibility(View.INVISIBLE);
                takePhotoButton.invalidate();
            }

        });
        frameLayout.addView(takePhotoButton);


        timeView = new TextView(getContext());
        timeView.setTextColor(Color.WHITE);

        timeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        timeView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        timeView.setVisibility(View.INVISIBLE);

        FrameLayout.LayoutParams timeViewParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        timeViewParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        timeViewParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(50+72+12));
        frameLayout.addView(timeView,timeViewParams);

    }
    private void startPlayback(){

        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        cameraProvider.unbindAll();
        frameLayout.removeAllViews();
        makePlaybackLayout();
    }
    private void recreateRecordLayout(){

        frameLayout.removeAllViews();
        createNewLifecycle();
        makeRecordLayout();
    }
    private void switchToRecordLayout(){
        videoPlayer.pause();
        videoPlayer.releasePlayer(true);
        videoPlayer = null;
        frameLayout.removeAllViews();
        createNewLifecycle();
        makeRecordLayout();
    }
    public void makePlaybackLayout(){

        textureView = new TextureView(getContext());
        textureView.setOpaque(true);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));
        textureView.setLayoutParams(layoutParams);
        frameLayout.addView(textureView);
        videoPlayer = new VideoPlayer();
        videoPlayer.setDelegate(new VideoPlayer.VideoPlayerDelegate() {
            @Override
            public void onStateChanged(boolean playWhenReady, int playbackState) {
                if (videoPlayer == null) {
                    return;
                }
//                if (videoPlayer.isPlaying() && playbackState == ExoPlayer.STATE_ENDED) {
//                    videoPlayer.seekTo(videoEditedInfo.startTime > 0 ? videoEditedInfo.startTime : 0);
//                }
            }

            @Override
            public void onError(VideoPlayer player, Exception e) {
                FileLog.e(e);
            }

            @Override
            public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
                Log.d("FLX_INJECT","video size ratio "+width + " height "+height);
                int viewWidth = frameLayout.getWidth()-AndroidUtilities.dp(48);
                int viewHeight = (int) ((float)viewWidth/width*height);
                FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(viewWidth, viewHeight);
                layoutParams.setMargins(AndroidUtilities.dp(24),AndroidUtilities.dp(12),AndroidUtilities.dp(24),AndroidUtilities.dp(12));
                textureView.setLayoutParams(layoutParams);
                textureView.invalidate();
            }

            @Override
            public void onRenderedFirstFrame() {

            }

            @Override
            public boolean onSurfaceDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });
        File videoFile = new File(FlexatarStorageManager.createTmpVideoStorage(), "saved_video.mp4");
        videoPlayer.setTextureView(textureView);
        videoPlayer.preparePlayer(Uri.fromFile(videoFile), "other");
        videoPlayer.play();
        videoPlayer.setMute(true);
        videoPlayer.setLooping(true);
        ImageView cancelIcon = new ImageView(getContext());
        cancelIcon.setImageResource(R.drawable.ic_close_white);
        cancelIcon.setOnClickListener(v->{
            switchToRecordLayout();
        });

        frameLayout.addView(cancelIcon,LayoutHelper.createFrame(36,36,Gravity.LEFT | Gravity.BOTTOM,66,0,0,66));

        ImageView okIcon = new ImageView(getContext());
        okIcon.setImageResource(R.drawable.floating_check);
        okIcon.setOnClickListener(v->{
            askFlexatarName();

            /*MediaMetadataRetriever retriever = new MediaMetadataRetriever();
            retriever.setDataSource(getContext(), Uri.fromFile(videoFile));

            String rotationString = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION);
            Log.d("FLX_INJECT","imageRotation "+rotationString);
            try {
                retriever.release();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }*/



        });
        frameLayout.addView(okIcon,LayoutHelper.createFrame(36,36,Gravity.RIGHT | Gravity.BOTTOM,0,0,66,66));


        TextView helpTextView = new TextView(getContext());
        helpTextView.setTextColor(Color.WHITE);
        helpTextView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        helpTextView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        helpTextView.setText(LocaleController.getString("UseVideoForFlexatar",R.string.UseVideoForFlexatar));
        frameLayout.addView(helpTextView,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT,Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL,0,0,0,50+72+12+20));


    }
    private void askFlexatarName(){

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


        builder.setTitle(LocaleController.getString("FlexatarName", R.string.FlexatarName));

        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText editText = new EditText(getContext());
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint(LocaleController.getString("EnterFlexatarsName", R.string.EnterFlexatarsName));
        int pad = AndroidUtilities.dp(12);
        linearLayout.setPadding(pad, pad, pad, pad);
        linearLayout.addView(editText,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,Gravity.CENTER));
        builder.setView(linearLayout);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), (dialogInterface, i) -> {
            packVideoDataToMakeFlexatar(editText.getText().toString());
        });
        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog,true,false,null);
    }
    public void packVideoDataToMakeFlexatar(String flxName){
        File videoFile = new File(FlexatarStorageManager.createTmpVideoStorage(), "saved_video.mp4");
        byte[] videoBytes = FlexatarStorageManager.dataFromFile(videoFile);

        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("flx_type", "video");
            jsonObject.put("name", flxName);
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            jsonObject.put("date", currentDateTime.format(formatter));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        Data sendData = new Data(jsonObject.toString());
        sendData = sendData.encodeLengthHeader().add(sendData);
        Data cData = new Data(videoBytes);
        cData = cData.encodeLengthHeader().add(cData);
        sendData = sendData.add(cData);

        FlexatarServerAccess.requestJson(FlexatarServiceAuth.getVerification(UserConfig.selectedAccount), "data", "POST", sendData.value, "application/octet-stream", new FlexatarServerAccess.OnRequestJsonReady() {
            @Override
            public void onReady(FlexatarServerAccess.StdResponse response) {
                Log.d("FLX_INJECT", "make video flx data response: " + response.toJson().toString());
//                ticket.status = "in_process";
//                ticket.formJson(FlexatarServerAccess.ListElement.listFactory(response.ftars).get("private").get(0).toJson());
//                TicketStorage.setTicket(lfid,ticket);
//                TicketsController.flexatarTaskStart(lfid,ticket);
            }

            @Override
            public void onError() {
//                TicketStorage.removeTicket(lfid);
                FlexatarCabinetActivity.makeFlexatarFailAction.run();
                Log.d("FLX_INJECT", "make flx data error " );
            }
        });
//        FlexatarStorageManager.dataToFile(sendData.value,makeFlxFile);
//        File makeFlxFile = new File(FlexatarStorageManager.createTmpVideoStorage(), "make_flx_by_video.pack");
        FlexatarCabinetActivity.needShowMakeFlexatarAlert = true;
        finishFragment();
    }
    private void startCamera() {
        Log.d("FLX_INJECT","startCamera");
        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());

        cameraProviderFuture.addListener(() -> {
            try {

                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);



            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }
    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {


        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        /*ImageAnalysis   imageAnalysis = new ImageAnalysis.Builder()
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(getContext()), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {

                image.close();
            }
        });*/

//        ImageCapture.Builder builder = new ImageCapture.Builder();


//        ImageCapture imageCapture = builder
//                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
//                .setTargetRotation(AndroidUtilities.findActivity(getContext()).getWindowManager().getDefaultDisplay().getRotation())
////                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
//                .setTargetResolution(new Size(720, 720 * 4 / 3))
//                .build();


        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());
        Recorder.Builder builder = new Recorder.Builder();
        builder.setExecutor(ContextCompat.getMainExecutor(getContext()));
        builder.setQualitySelector(QualitySelector.from(Quality.SD));
        //        builder.setTargetAspectRatio(AspectRatio.RATIO_4_3);
       videoCapture = VideoCapture.withOutput(builder.build());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview,  videoCapture);



        //        isTakePhotoAvailable = true;

    }
    private boolean needRecreateCamera = false;
    @Override
    public void onResume() {
        super.onResume();
        if (needRecreateCamera)
            recreateRecordLayout();
    }

    @Override
    public void onPause() {
        super.onPause();
        needRecreateCamera = videoPlayer == null;
        if (lifecycleRegistry!=null) lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        if (cameraProvider!=null) cameraProvider.unbindAll();
        if (fileObserver!=null) {
            fileObserver.stopWatching();
            fileObserver = null;
        };
        if (recording!=null) {
            recording.stop();
            recording.close();
            recording = null;
        }
    }

    @Override
    public void onFragmentDestroy() {
        super.onFragmentDestroy();
        Log.d("FLX_INJECT", "destroy videocap fragment");
        if (videoPlayer!=null) {
            videoPlayer.pause();
            videoPlayer.releasePlayer(true);
            videoPlayer = null;
        }
        if (lifecycleRegistry!=null) lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        if (cameraProvider!=null) cameraProvider.unbindAll();
        if (fileObserver!=null) {
            fileObserver.stopWatching();
            fileObserver = null;
        };
        if (recording!=null) {
            recording.stop();
            recording.close();
            recording = null;
        }
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }
}
