package org.flexatar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;

import java.io.File;

public class FlexatarPreviewOld extends FrameLayout {
    private final FlxDrawerNew drawer;
    private final CardView cardview;
    private final LayoutParams flexatarViewLayoutParameters;
    private final LayoutParams openMouthButtonLayoutParameters;
    private final TextView openMouthButton;
    private final LayoutParams seekBarLayoutParameters;
    private final SeekBarView seekBar;
    private final LayoutParams parameterToEditLayoutParameters;
    private final ImageView saveButton;
    private final LinearLayout parameterToEditLayout;
    private boolean isMouthOpened = false;

    public FlexatarPreviewOld(@NonNull Context context, File flexatarFile) {
        super(context);
        byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
        Log.d("FLX_INJECT",""+flxBytes);
        FlexatarData flexatarData = new FlexatarData(new LengthBasedFlxUnpack(flxBytes));
//        FlexatarRenderer.currentFlxData = flexatarData;
        View overlayView = new View(context);
//        overlayView.setLayoutParams(new ViewGroup.LayoutParams(
//                ViewGroup.LayoutParams.MATCH_PARENT,
//                ViewGroup.LayoutParams.MATCH_PARENT));
        overlayView.setBackgroundColor(Color.BLACK);
        overlayView.setAlpha(0.8f);
        overlayView.setEnabled(true);
        overlayView.setClickable(true);
        addView(overlayView,LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT, Gravity.CENTER));
        GLSurfaceView surfaceView = new GLSurfaceView(context);
        surfaceView.setEGLContextClientVersion(2);
        FlexatarRenderer.speechState = new float[]{0,0,-1,0,0};
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        drawer = new FlxDrawerNew();
        renderer.drawer = drawer;
        drawer.setFlexatarData(flexatarData);
//        renderer.drawer.setFlexatarDataAlt(FlexatarRenderer.currentFlxData);
//        renderer.drawer.setEffect(true,1);
//        renderer.drawer.setMixWeight(0.5f);
        surfaceView.setRenderer(renderer);

        cardview = new CardView(context);
        cardview.setClickable(false);
//        cardview.setContextClickable(false);
        cardview.setRadius(AndroidUtilities.dp(10));

        cardview.addView(surfaceView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT, Gravity.CENTER));

//        addView(cardview,LayoutHelper.createFrame(AndroidUtilities.dp(80),AndroidUtilities.dp(100),Gravity.CENTER_HORIZONTAL | Gravity.TOP,0,20,0,0));
        VoIPBackgroundProvider backgroundProvider = new VoIPBackgroundProvider();
        backgroundProvider.setTotalSize(200,200);
//        backgroundProvider.setHasVideo(true);
        openMouthButton = new TextView(context);
        openMouthButton.setText("Calibrate mouth");
        openMouthButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        openMouthButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        openMouthButton.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(4),AndroidUtilities.dp(24),AndroidUtilities.dp(4));
        openMouthButton.setTextColor(Color.WHITE);
        openMouthButton.setOnClickListener((v) -> {
            isMouthOpened = !isMouthOpened;
            if (isMouthOpened)
                openMouth();
            else
                closeMouth();
            toggleMouthEdit(isMouthOpened);
            openMouthButton.setText(isMouthOpened ? "Close mouth":"Calibrate mouth");
        });

        seekBar = new SeekBarView(context);
        seekBar.setReportChanges(true);
        int stepCount = 30;
        seekBar.setSeparatorsCount(stepCount);

        seekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {

            }

            @Override
            public void onSeekBarPressed(boolean pressed) {
            }

            @Override
            public CharSequence getContentDescription() {
                return "No";
            }

            @Override
            public int getStepsCount() {
                return stepCount;
            }
        });

        parameterToEditLayout = new LinearLayout(context);
        String[] buttonNames = {"Top X", "Top Y","Bottom X","Bottom Y"};
        LinearLayout.LayoutParams switchEditButtonLayoutParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        switchEditButtonLayoutParams.leftMargin = AndroidUtilities.dp(3);
        switchEditButtonLayoutParams.rightMargin = AndroidUtilities.dp(3);
        for (int i = 0; i < buttonNames.length; i++) {

            String name = buttonNames[i];

            TextView tv = new HideEmojiTextView(context, backgroundProvider);

//            if (name.equals(chosenEffect)) {
                tv.setTextColor(Color.parseColor("#f7d26c"));
//            }

            tv.setLayoutParams(switchEditButtonLayoutParams);
            tv.setText(name);
            tv.setGravity(Gravity.CENTER);
            parameterToEditLayout.addView(tv);
        }
        parameterToEditLayout.setPadding(AndroidUtilities.dp(6),AndroidUtilities.dp(12),AndroidUtilities.dp(6),AndroidUtilities.dp(12));
        flexatarViewLayoutParameters = new LayoutParams(-1,-1);
        openMouthButtonLayoutParameters = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        seekBarLayoutParameters = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,ViewGroup.LayoutParams.WRAP_CONTENT);
        parameterToEditLayoutParameters = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.WRAP_CONTENT);

        openMouthButton.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
//                int height = bottom - top;
                parameterToEditLayoutParameters.topMargin = bottom ;
                parameterToEditLayoutParameters.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                parameterToEditLayoutParameters.width = ViewGroup.LayoutParams.MATCH_PARENT;
                parameterToEditLayoutParameters.leftMargin = 0;
                if (layoutHeight<layoutWidth){
                    parameterToEditLayoutParameters.gravity = Gravity.TOP | Gravity.LEFT;
                    parameterToEditLayoutParameters.width = layoutWidth - left;
                    parameterToEditLayoutParameters.leftMargin = left;
                }
//                seekBarLayoutParameters.topMargin = bottom ;
                if (width !=0){
                    post(()->{
                        if (indexOfChild(parameterToEditLayout) == -1) {
                            addView(parameterToEditLayout, parameterToEditLayoutParameters);
//                            addView(seekBar, seekBarLayoutParameters);
                        }
                        else {
                            parameterToEditLayout.setLayoutParams(parameterToEditLayoutParameters);
                        }
                    });
                }
            }
        });

        parameterToEditLayout.addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom, int oldLeft, int oldTop, int oldRight, int oldBottom) {
                int width = right - left;
//                int height = bottom - top;

                seekBarLayoutParameters.topMargin = bottom;
                seekBarLayoutParameters.width = (int) (layoutWidth * 0.8f);
                seekBarLayoutParameters.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
                seekBarLayoutParameters.leftMargin = 0;
                if (layoutWidth>layoutHeight){
                    seekBarLayoutParameters.gravity = Gravity.TOP | Gravity.LEFT;
                    seekBarLayoutParameters.width = (int) ((layoutWidth-left) * 0.8f);
                    seekBarLayoutParameters.leftMargin = left;
                }
                if (width !=0){
                    post(()->{
                        if (indexOfChild(seekBar) == -1) {
//                            addView(parameterToEditLayout, parameterToEditLayoutParameters);
                            addView(seekBar, seekBarLayoutParameters);
                        }
                        else {
                            seekBar.setLayoutParams(seekBarLayoutParameters);
                        }
                    });
                }
            }
        });

        ImageView closeButton = new ImageView(context);
        closeButton.setImageResource(R.drawable.msg_cancel);
        closeButton.setOnClickListener((v)->{
           ((FrameLayout)getParent()).removeView(this);
        });
        int buttonSize = 64;
        addView(closeButton,LayoutHelper.createFrame(buttonSize,buttonSize,Gravity.BOTTOM |Gravity.RIGHT,0,0,24,24));

        saveButton = new ImageView(context);
        saveButton.setImageResource(R.drawable.checkbig);
        saveButton.setOnClickListener((v)->{
            ((FrameLayout)getParent()).removeView(this);
        });
        addView(saveButton,LayoutHelper.createFrame(buttonSize,buttonSize,Gravity.BOTTOM |Gravity.RIGHT,0,0,24*2+buttonSize,24));
        toggleMouthEdit(false);
        //        addView(tv,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT,LayoutHelper.WRAP_CONTENT,Gravity.TOP | Gravity.LEFT,20,AndroidUtilities.dp(120),0,0));

    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

    }
    private void toggleMouthEdit(boolean visible){
            int visibility = visible ? View.VISIBLE :View.GONE;
            saveButton.setVisibility(visibility);
            saveButton.setEnabled(visible);
            seekBar.setVisibility(visibility);
            seekBar.setEnabled(visible);
            parameterToEditLayout.setVisibility(visibility);
            parameterToEditLayout.setEnabled(visible);



    }

    private int layoutWidth;
    private int layoutHeight;
    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom){
        super.onLayout(changed, left, top, right, bottom);
        int width = right - left;
        int height = bottom - top;
        layoutWidth = width;
        layoutHeight = height;
        int flxWidth = (int) ((float)width * 0.6f);
        int flxHeight = (int) ((float)width * 0.6f *1.5f);
        int topMargin = (int) ((float)width * 0.1f);
        flexatarViewLayoutParameters.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        openMouthButtonLayoutParameters.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        openMouthButtonLayoutParameters.setMargins(0, (int) (1.2f*topMargin+flxHeight),0,0);
//        openMouthButtonLayoutParameters.setMargins((width - flxWidth)/2, (int) (1.2f*topMargin+flxHeight),0,0);

        seekBarLayoutParameters.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        seekBarLayoutParameters.setMargins(0, (int) (1.2f*topMargin+flxHeight)+AndroidUtilities.dp(40),0,0);
        seekBarLayoutParameters.width = flxWidth;
        seekBarLayoutParameters.height = topMargin;

        int leftMargin = 0;
        if (width>height){
            flxHeight = (int) ((float)height * 0.8f);
            flxWidth = (int) ((float)flxHeight / 1.5f);
            topMargin = (int) ((float)height * 0.1f);
            leftMargin = (int) ((float)flxWidth * 0.3f);
//            leftMargin = (int) ((float)flxWidth / 0.3f);
            flexatarViewLayoutParameters.gravity = Gravity.TOP | Gravity.LEFT;
            openMouthButtonLayoutParameters.gravity = Gravity.TOP | Gravity.LEFT;
//            openMouthButtonLayoutParameters.setMargins(0, topMargin,0,0);
            openMouthButtonLayoutParameters.setMargins((int) (flxWidth+leftMargin+leftMargin*0.1f), topMargin,0,0);

        }
        flexatarViewLayoutParameters.width = flxWidth;
        flexatarViewLayoutParameters.height = flxHeight;
        flexatarViewLayoutParameters.setMargins(leftMargin,topMargin,0,0);



        if (width!=0)
            post(()->{
                if (indexOfChild(cardview) == -1) {
                    addView(cardview, flexatarViewLayoutParameters);
                    addView(openMouthButton, openMouthButtonLayoutParameters);
//                    addView(seekBar, seekBarLayoutParameters);
                }
                else {
                    cardview.setLayoutParams(flexatarViewLayoutParameters);
                    openMouthButton.setLayoutParams(openMouthButtonLayoutParameters);
//                    seekBar.setLayoutParams(seekBarLayoutParameters);
                }
            });
    }
    public void openMouth(){
        drawer.setSpeechState(new float[]{0,0,-0.8f,0,0});
    }
    public void closeMouth(){
        drawer.setSpeechState(new float[]{0,0,0.05f,0,0});
    }
}
