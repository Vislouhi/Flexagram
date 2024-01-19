package org.flexatar;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FlexatarCameraCaptureFragment extends BaseFragment implements LifecycleOwner{

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

    public void finishPage(){
        lifecycleRegistry.setCurrentState(Lifecycle.State.DESTROYED);
        cameraProvider.unbindAll();
        finishFragment();
    }
    private void makeUI(){
        FrameLayout frameLayout = (FrameLayout) fragmentView;
        frameLayout.setBackgroundColor(Color.BLACK);
//        frameLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        mPreviewView = new PreviewView(context);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        layoutParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12));
        mPreviewView.setLayoutParams(layoutParams);

        frameLayout.addView(mPreviewView);
        lifecycleRegistry = new LifecycleRegistry(this);
        lifecycleRegistry.setCurrentState(Lifecycle.State.CREATED);
        lifecycleRegistry.setCurrentState(Lifecycle.State.STARTED);

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
        FrameLayout.LayoutParams faceHintViewParams = new FrameLayout.LayoutParams(AndroidUtilities.dp(200),AndroidUtilities.dp(200));
        faceHintViewParams.gravity = Gravity.LEFT | Gravity.BOTTOM;
        faceHintViewParams.setMargins(AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(12),AndroidUtilities.dp(120));
        faceHintView.setLayoutParams(faceHintViewParams);
        frameLayout.addView(faceHintView);

        overlayView = new View(context);
        overlayView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        overlayView.setBackgroundColor(Color.WHITE);
        overlayView.setAlpha(0.0f);
        overlayView.setEnabled(false);
//        overlayView.setVisibility(View.GONE);
        frameLayout.addView(overlayView);
        showStartUpAlert(false);
    }
    @Override
    public View createView(Context context) {
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

        startCamera();

        return fragmentView;
    }
    private void startCamera() {

        final ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(context);

        cameraProviderFuture.addListener(() -> {
            try {

                cameraProvider = cameraProviderFuture.get();
                makeUI();
                bindPreview(cameraProvider);



            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(context));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {

        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                .build();

        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), new ImageAnalysis.Analyzer() {
            @Override
            public void analyze(@NonNull ImageProxy image) {
                image.close();
            }
        });

        ImageCapture.Builder builder = new ImageCapture.Builder();



        imageCapture = builder
                .setTargetRotation(AndroidUtilities.findActivity(context).getWindowManager().getDefaultDisplay().getRotation())
                .build();

        preview.setSurfaceProvider(mPreviewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);


    }

    private int hintCounter = 0;
    private List<byte[]> imagesCollector = new ArrayList<>();
    private void takePhoto() {
//        findViewById(R.id.photo_frame).setVisibility(View.VISIBLE);
//        findViewById(R.id.image_capture_button).setEnabled(false);
        overlayView.setEnabled(true);
        overlayView.animate().alpha(0.5f).setDuration(250).start();

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

//                        new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(FlxCaptureActivity.this, "Image Saved successfully", Toast.LENGTH_SHORT).show());

                    } catch (Exception e) {
                        e.printStackTrace();
                    }


                }
                if (hintCounter == faceHints.size()) {
                    sendImagesToMakeFlexatar();
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
                if (checks[0]) ValueStorage.setfDontShowFlexatarPhotoInstructions(context);
            });
        }else{
            builder.setPositiveButton("OK", (dialogInterface, i) -> {

            });
        }


        AlertDialog alertDialog = builder.create();
        showDialog(alertDialog);
        TextView button = (TextView) alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if (button != null) {
            button.setTextColor(Theme.getColor(Theme.key_text_RedBold));
        }
    }

    public void sendImagesToMakeFlexatar(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("rotation", imageRotation);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

//        Data sendData = new Data(imagesCollector.get(0));
        Data sendData = new Data(jsonObject.toString());
        sendData = sendData.encodeLengthHeader().add(sendData);
        for (int i = 0; i < 5; i++) {
            Data cData = new Data(imagesCollector.get(i));
            cData = cData.encodeLengthHeader().add(cData);
            sendData = sendData.add(cData);
        }

        ValueStorage.addTicket(context, UUID.randomUUID().toString());

        JSONArray tickets = ValueStorage.getTickets(context);
        Log.d("FLX_INJECT", "addTicket : " + tickets + " " + tickets.length());
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//        Data finalSendData = sendData;
//        executor.execute(() -> {
//                    FlexatarServerAccess.makeFlexatarRequest(finalSendData.value);
//                });
        finishPage();
    }
}
