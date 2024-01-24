package org.flexatar;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;

import com.google.common.util.concurrent.ListenableFuture;

import org.flexatar.DataOps.Data;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.CheckBoxCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.LaunchActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class FlexatarCameraCaptureFragment extends BaseFragment implements LifecycleOwner{

    private byte[] flexatarBody;
    private Context context;
    private PreviewView mPreviewView;
    private ImageCapture imageCapture;
    private LifecycleRegistry lifecycleRegistry;
    private ProcessCameraProvider cameraProvider;
    private ImageView takePhotoButton;

    List<Integer> faceHints = new ArrayList<>();
    private ImageView faceHintView;
    private View overlayView;
    private int imageRotation = 0;
    private int initialOrientationMode;
    private View orangeBarView;
    private FrameLayout frameLayout;

    private float barTopMargin = 0f;
    private float barBottomMargin = 0f;

    public FlexatarCameraCaptureFragment(){
        super();
    }


    public FlexatarCameraCaptureFragment(byte[] flexatarBody) {
        super();
        this.flexatarBody=flexatarBody;
    }

    public void finishPage(){
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        cameraProvider.unbindAll();
        finishFragment();
    }
    private void makeUI(){
        frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Color.BLACK);
//        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        mPreviewView = new PreviewView(context);
        mPreviewView.setScaleType(PreviewView.ScaleType.FIT_CENTER);
        mPreviewView.setVisibility(View.INVISIBLE);
        orangeBarView = new View(context);
        mPreviewView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(
                    View view, int left, int top, int right, int bottom,
                    int oldLeft, int oldTop, int oldRight, int oldBottom) {

                int width = right - left;
                int height = bottom - top;

                Log.d("FLX_INJECT","New width: " + width + ", New height: " + height +" left " +left + " top " + top);
                if (width != 0 && height != 0){
//                    Log.d("FLX_INJECT","add view bar");
                    frameLayout.post(new Runnable() {
                        @Override
                        public void run() {
//                            orangeBarView.setLayoutParams(new ViewGroup.LayoutParams(
//                                    width,
//                                    height));
                            int width1 = (int)(0.3f * (float)width);
                            int height1 = (int)(0.02f * (float)width);

                            barTopMargin = 2f/3f - (float)height1/(float)height/2f;
                            barBottomMargin = 2f/3f + (float)height1/(float)height/2f;

                            int left1 = left + width/2 - width1/2;
                            int top1 = top + (int)(barTopMargin * (float)height);
                            int h = (int)(barBottomMargin * (float)height)-(int)(barTopMargin * (float)height);

                            frameLayout.removeView(orangeBarView);
                            FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                                    width1,
                                    h);
                            p.gravity = Gravity.TOP | Gravity.LEFT;
                            p.setMargins(left1,top1,0,0);
                            orangeBarView.setBackgroundColor(Color.YELLOW);
                            orangeBarView.setEnabled(false);
                            if (flexatarBody == null)
                                orangeBarView.setVisibility(View.INVISIBLE);
                            frameLayout.addView(orangeBarView,p);
//                            mPreviewView.setVisibility(View.VISIBLE);
                        }
                    });
                    frameLayout.postDelayed(()->{mPreviewView.setVisibility(View.VISIBLE);},500);
                }
            }
        });
        /*{
            @Override
            protected void onLayout(boolean changed, int left, int top, int right, int bottom){
                super.onLayout(changed,left,top,right,bottom);
//                Log.d()
            }
        };*/
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));
        mPreviewView.setLayoutParams(layoutParams);

        frameLayout.addView(mPreviewView);


        takePhotoButton = new ImageView(context);
        takePhotoButton.setImageDrawable(context.getResources().getDrawable(R.drawable.camera_btn));
        FrameLayout.LayoutParams takePhotoParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(72),AndroidUtilities.dp(72));
        takePhotoParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        takePhotoParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(50));
        takePhotoButton.setLayoutParams(takePhotoParams);
        takePhotoButton.setOnClickListener((l)->{
            takePhoto();
        });
        frameLayout.addView(takePhotoButton);



        faceHints.add(R.drawable.face_front);
        faceHints.add(R.drawable.face_left);
        faceHints.add(R.drawable.face_front);
        faceHints.add(R.drawable.face_right);
        faceHints.add(R.drawable.face_front);
        faceHints.add(R.drawable.face_up);
        faceHints.add(R.drawable.face_front);
        faceHints.add(R.drawable.face_down);

        faceHintView = new ImageView(context);
        faceHintView.setImageResource(faceHints.get(0));
        FrameLayout.LayoutParams faceHintViewParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(120),AndroidUtilities.dp(150));
        faceHintViewParams.gravity = Gravity.LEFT | Gravity.TOP;
        faceHintViewParams.setMargins(AndroidUtilities.dp(6),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(120));
        faceHintView.setLayoutParams(faceHintViewParams);
        frameLayout.addView(faceHintView);

        overlayView = new View(context);
        overlayView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlayView.setBackgroundColor(Color.WHITE);
        overlayView.setAlpha(0.0f);
        overlayView.setEnabled(false);


        frameLayout.addView(overlayView);

        if (flexatarBody == null) {
            showStartUpAlert(false);
        }else{

            showHelpMouthCaptureAlert();
            isMouthDone = true;
            orangeBarView.setVisibility(View.VISIBLE);
        }
    }
    @SuppressLint("SourceLockedOrientationActivity")
    @Override
    public View createView(Context context) {
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);

        initialOrientationMode = LaunchActivity.instance.getRequestedOrientation();
        LaunchActivity.instance.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        this.context=context;
        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Flexatar Capture");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishPage();
                }
                if (id == 10){
                    showStartUpAlert(true);
                }
            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        ActionBarMenuItem otherItem = menu.addItem(10, R.drawable.msg_help);
        otherItem.setContentDescription("Help");

        fragmentView = new FrameLayout(context);
        makeUI();
        startCamera();

        return fragmentView;
    }
    @Override
    public void onFragmentDestroy() {

        super.onFragmentDestroy();
        LaunchActivity.instance.setRequestedOrientation(initialOrientationMode);
        finishPage();
    }
    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {

                cameraProvider = cameraProviderFuture.get();

                bindPreview(cameraProvider);



            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(context));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                if (needTakePhoto) {
                    needTakePhoto = false;
                    Log.d("FLX_INJECT", "ImageProxy width " + image.getWidth() + " height " + image.getHeight());
                }
                image.close();
            }
        });

        ImageCapture.Builder builder = new ImageCapture.Builder();



        imageCapture = builder
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetRotation(AndroidUtilities.findActivity(context).getWindowManager().getDefaultDisplay().getRotation())
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)

                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);


    }

    private int hintCounter = 0;
    private List<byte[]> imagesCollector = new ArrayList<>();
    private boolean needTakePhoto = false;
    private boolean isMouthDone = false;
    private void takePhoto() {
        needTakePhoto = true;
//        findViewById(R.id.photo_frame).setVisibility(View.VISIBLE);
//        findViewById(R.id.image_capture_button).setEnabled(false);
        overlayView.setEnabled(true);
        overlayView.animate().alpha(0.5f).setDuration(250).start();
        orangeBarView.setVisibility(View.INVISIBLE);
//        orangeBarView.setVisibility(View.VISIBLE);
//        overlayView.setVisibility(View.VISIBLE);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(outputStream).build();

        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(context), new ImageCapture.OnImageSavedCallback() {

            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                overlayView.setEnabled(false);
                overlayView.animate().alpha(0f).setDuration(250).start();
//                findViewById(R.id.image_capture_button).setEnabled(true);
//                findViewById(R.id.photo_frame).setVisibility(View.INVISIBLE);
                hintCounter += 1;
                if (hintCounter<faceHints.size()) {
                    faceHintView.setImageResource(faceHints.get(hintCounter));
                }
                if (hintCounter%2 == 0 || hintCounter == 1) {
                    try {
                        // Decode the JPEG file into a Bitmap
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Specify the bitmap configuration
                        byte[] imgAsBytes = outputStream.toByteArray();
                        ExifInterface exif = new ExifInterface(new ByteArrayInputStream(imgAsBytes));
                        imageRotation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);

                        ByteArrayInputStream inputStream = new ByteArrayInputStream(imgAsBytes);
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        Log.d("FLX_INJECT", "photo width " + bitmap.getWidth() + " height " + bitmap.getHeight());

                        int w = bitmap.getWidth();
                        int h = bitmap.getHeight();
                        int nw = 720;
                        int nh = h * nw / w;
                        if (w>h){
                            nh = 720;
                            nw = w * nh / h;

                        }


                        bitmap = Bitmap.createScaledBitmap(bitmap, nw, nh, false);
                        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                        FlexatarCameraCaptureFragment.this.imagesCollector.add(byteArrayOutputStream.toByteArray());

//                        int pos = (int) ((float)bitmap.getHeight() * barTopMargin);
//                        Bitmap bitmap1 = drawRectangleOverBitmap(bitmap,0,pos,bitmap.getWidth(),pos+20);
//                        faceHintView.setImageBitmap(bitmap1);
//                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(FlxCaptureActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                if (hintCounter == faceHints.size()) {
                    if (isMouthDone) {
                        askFlexatarName();
                    }else{
                        showStartMouthCaptureAlert();
                    }
//
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
//                        Toast.makeText(FlexatarCameraCaptureFragment.this, "Image Saved Error", Toast.LENGTH_SHORT).show();
                    }
                });
                exception.printStackTrace();
            }
        });
    }

    @NonNull
    @Override
    public Lifecycle getLifecycle() {
        return lifecycleRegistry;
    }

    public void showStartUpAlert(boolean force){
        if (!force) if(ValueStorage.checkIfDontShowFlexatarPhotoInstructions(context)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


            builder.setTitle("Instructions");
            builder.setMessage("1. Take photos turning your head as assistant shows.\n" +
                               "2. Don't turn your head too mach. \n" +
                               "3. Keep your mouth closed.");


        if (!force) {
            final boolean[] checks = new boolean[]{false};
            FrameLayout frameLayout = new FrameLayout(getParentActivity());

            CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
            cell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
            cell.setText("Don't show again.", "", false, false);
            cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(8) : 0, 0, LocaleController.isRTL ? 0 : AndroidUtilities.dp(8), 0);
            frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.LEFT, 8, 0, 8, 0));
            cell.setOnClickListener(v -> {
                CheckBoxCell cell1 = (CheckBoxCell) v;
                checks[0] = !checks[0];
                cell1.setChecked(checks[0], true);
            });
            builder.setView(frameLayout);
            builder.setPositiveButton("OK", (dialogInterface, i) -> {
                if (checks[0]) ValueStorage.setDontShowFlexatarPhotoInstructions(context);
            });
        }else{
            builder.setPositiveButton("OK", (dialogInterface, i) -> {

            });
        }


        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);

    }

    public void showStartMouthCaptureAlert(){


        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


        builder.setTitle("Make mouth (optional)");
        builder.setMessage("1. Bite the orange bar taking first picture.\n" +
                "2. Keep mouth opened. \n" +
                "3. Take photos turning your head as assistant shows.");




            builder.setPositiveButton("Continue", (dialogInterface, i) -> {
                hintCounter = 0;
                faceHintView.setImageResource(faceHints.get(hintCounter));
                isMouthDone = true;
                orangeBarView.setVisibility(View.VISIBLE);
            });

            builder.setNegativeButton("Skip", (dialogInterface, i) -> {
                askFlexatarName();
            });

        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog,true,false,null);

    }

    public void showHelpMouthCaptureAlert(){
//        Log.d("FLX_INJECT","capture mouth mode" + ValueStorage.checkIfDontShowMouthPhotoInstructions(context));
        if(ValueStorage.checkIfDontShowMouthPhotoInstructions(context)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


        builder.setTitle("Make Mouth");
        builder.setMessage("1. Bite the orange bar taking first picture.\n" +
                "2. Keep mouth opened. \n" +
                "3. Take photos turning your head as assistant shows.");


        final boolean[] checks = new boolean[]{false};
        FrameLayout frameLayout = new FrameLayout(getParentActivity());

        CheckBoxCell cell = new CheckBoxCell(getParentActivity(), 1);
        cell.setBackgroundDrawable(Theme.getSelectorDrawable(false));
        cell.setText("Don't show again.", "", false, false);
        cell.setPadding(LocaleController.isRTL ? AndroidUtilities.dp(8) : 0, 0, LocaleController.isRTL ? 0 : AndroidUtilities.dp(8), 0);
        frameLayout.addView(cell, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, 48, Gravity.TOP | Gravity.LEFT, 8, 0, 8, 0));
        cell.setOnClickListener(v -> {
            CheckBoxCell cell1 = (CheckBoxCell) v;
            checks[0] = !checks[0];
            cell1.setChecked(checks[0], true);
        });
        builder.setView(frameLayout);

        builder.setPositiveButton("OK", (dialogInterface, i) -> {
            if (checks[0]) ValueStorage.setDontShowMouthPhotoInstructions(context);
//            hintCounter = 0;
//            faceHintView.setImageResource(faceHints.get(hintCounter));
//            isMouthDone = true;
//            orangeBarView.setVisibility(View.VISIBLE);

        });



        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);

    }
    private void askFlexatarName(){
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());


        builder.setTitle("Flexatar Name");
//        builder.setMessage("Enter flexatar name");





        LinearLayout linearLayout = new LinearLayout(getContext());
        linearLayout.setOrientation(LinearLayout.VERTICAL);

        final EditText editText = new EditText(getContext());
        editText.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        editText.setHint("Enter flexatars name");
        int pad = AndroidUtilities.dp(12);
        linearLayout.setPadding(pad, pad, pad, pad);
        linearLayout.addView(editText,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,Gravity.CENTER));
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", (dialogInterface, i) -> {
            sendImagesToMakeFlexatar(editText.getText().toString());
        });
        /*builder.setNegativeButton("Skip", (dialogInterface, i) -> {
            sendImagesToMakeFlexatar();
        });*/

        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog,true,false,null);
    }

    public void sendImagesToMakeFlexatar(String flexatarName){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("rotation", imageRotation);
            jsonObject.put("teeth_top", barTopMargin);
            jsonObject.put("teeth_bottom", barBottomMargin);
            jsonObject.put("name", flexatarName);
            jsonObject.put("mouth_only", flexatarBody != null);
            LocalDateTime currentDateTime = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            jsonObject.put("date", currentDateTime.format(formatter));

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

//        Data sendData = new Data(imagesCollector.get(0));
        Data sendData = new Data(jsonObject.toString());
        sendData = sendData.encodeLengthHeader().add(sendData);
        if (flexatarBody != null) {
            Data flxBodyData = new Data(flexatarBody);
            flxBodyData = flxBodyData.encodeLengthHeader().add(flxBodyData);
            sendData = sendData.add(flxBodyData);
//            Log.d("FLX_INJECT", "add flx body of size " +flxBodyData.value.length);
//            Log.d("FLX_INJECT", "sendData length " +sendData.value.length);
        }
        for (int i = 0; i < imagesCollector.size(); i++) {
            Data cData = new Data(imagesCollector.get(i));
            cData = cData.encodeLengthHeader().add(cData);
            sendData = sendData.add(cData);
        }
//        Log.d("FLX_INJECT", "full send data size " +sendData.value.length);
        ValueStorage.addTicket(context, UUID.randomUUID().toString());

        JSONArray tickets = ValueStorage.getTickets(context);
        FlexatarStorageManager.dataToFile(sendData.value,FlexatarStorageManager.makeFileInFlexatarStorage(context,"input_face.bin"));
        Log.d("FLX_INJECT", "addTicket : " + tickets + " " + tickets.length());
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Data finalSendData = sendData;
//        executor.execute(() -> {
//                    FlexatarServerAccess.makeFlexatarRequest(finalSendData.value);
//                });
        finishPage();
    }


    public static Bitmap drawRectangleOverBitmap(Bitmap originalBitmap, int left, int top, int right, int bottom) {
        // Create a copy of the original bitmap to avoid modifying it directly
        Bitmap bitmapWithRectangle = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);

        // Create a Canvas to draw on the new bitmap
        Canvas canvas = new Canvas(bitmapWithRectangle);
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        // Create a Paint for drawing the rectangle
        Paint paint = new Paint();
        paint.setColor(Color.RED); // Set the color of the rectangle
        paint.setStyle(Paint.Style.STROKE); // Set the style to stroke (outline)
        paint.setStrokeWidth(5); // Set the width of the stroke

        // Draw the rectangle on the Canvas
        canvas.drawRect(left, top, right, bottom, paint);

        return bitmapWithRectangle;
    }
}
