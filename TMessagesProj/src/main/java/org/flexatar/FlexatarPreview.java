package org.flexatar;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.opengl.GLSurfaceView;
import android.util.DisplayMetrics;
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

import org.checkerframework.checker.units.qual.A;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;

import java.io.File;

public class FlexatarPreview extends FrameLayout {
    private final FlxDrawerNew drawer;
    private final CardView cardview;
    private final LengthBasedFlxUnpack unpackedFlexatar;
    private final BaseFragment parentFragment;
    private final byte[] flxData;

    private boolean isMouthOpened = false;
    private LinearLayout layout;
    private LinearLayout.LayoutParams cardVieParameters;
    private int selectedEdit;
    private LinearLayout parameterToEditLayout;
    private SeekBarView seekBar;
    private ImageView saveButton;
    private boolean isCalibrateMode = false;

    public FlexatarPreview(@NonNull Context context, File flexatarFile, BaseFragment parentFragment) {
        super(context);
        this.parentFragment = parentFragment;
        int currentOrientation = getResources().getConfiguration().orientation;

        byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
        flxData = flxBytes;
        Log.d("FLX_INJECT",""+flxBytes);
        unpackedFlexatar = new LengthBasedFlxUnpack(flxBytes);
        FlexatarData flexatarData = new FlexatarData(unpackedFlexatar);
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
        makeLayout(currentOrientation);
//        addView(cardview);

    }
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        makeCardviewParameters(newConfig.orientation);
        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        layout.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
//        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
//
//        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//
//        }
    }

    private void makeCardviewParameters(int orientation){
        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        int cardWidth = (int) (screenWidth * 0.5f);
        int cardHeight = (int) (cardWidth * 1.5f);
        if (!isPortrait){
            cardHeight = (int) (screenHeight * 0.7f);
            cardWidth = (int) (cardHeight / 1.5f);
        }

        cardVieParameters = new LinearLayout.LayoutParams(cardWidth,cardHeight);
        cardVieParameters.gravity = isPortrait ? Gravity.TOP | Gravity.CENTER_HORIZONTAL :  Gravity.LEFT | Gravity.CENTER_VERTICAL;
        cardVieParameters.topMargin = isPortrait ? AndroidUtilities.dp(24) : 0;
        cardVieParameters.leftMargin = isPortrait ? 0:AndroidUtilities.dp(24) ;
        cardview.setLayoutParams(cardVieParameters);

    }
    private void makeLayout(int orientation){
        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;

        layout = new LinearLayout(getContext());
        layout.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        addView(layout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));

        makeCardviewParameters(orientation);
        layout.addView(cardview);

        LinearLayout controlsLayout = new LinearLayout(getContext());
        controlsLayout.setOrientation(LinearLayout.VERTICAL);
        layout.addView(controlsLayout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));

        LinearLayout mouthActionsButtons = new LinearLayout(getContext());
        mouthActionsButtons.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.addView(mouthActionsButtons,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,26,0,0));

        LinearLayout.LayoutParams mouthActionsButtonsLayoutParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        mouthActionsButtonsLayoutParams.leftMargin = AndroidUtilities.dp(3);
        mouthActionsButtonsLayoutParams.rightMargin = AndroidUtilities.dp(3);

        TextView calibrateMouthButton = new TextView(getContext());
        calibrateMouthButton.setText("Calibrate mouth");
        calibrateMouthButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        calibrateMouthButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        calibrateMouthButton.setGravity(Gravity.CENTER);
//        calibrateMouthButton.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(4),AndroidUtilities.dp(24),AndroidUtilities.dp(4));
        calibrateMouthButton.setTextColor(Color.WHITE);
        mouthActionsButtons.addView(calibrateMouthButton,mouthActionsButtonsLayoutParams);
        calibrateMouthButton.setOnClickListener((v)->{
            isCalibrateMode = !isCalibrateMode;
            toggleMouthEdit(isCalibrateMode);
            calibrateMouthButton.setText(isCalibrateMode ? "Close mouth" : "Calibrate mouth");
            if (isCalibrateMode)
                openMouth();
            else
                closeMouth();
        });

        TextView makeMouthButton = new TextView(getContext());
        makeMouthButton.setText("Make mouth");
        makeMouthButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        makeMouthButton.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        makeMouthButton.setGravity(Gravity.CENTER);
//        makeMouthButton.setPadding(AndroidUtilities.dp(24),AndroidUtilities.dp(4),AndroidUtilities.dp(24),AndroidUtilities.dp(4));
        makeMouthButton.setTextColor(Color.WHITE);

        mouthActionsButtons.addView(makeMouthButton,mouthActionsButtonsLayoutParams);
        mouthActionsButtons.setOnClickListener((v) ->{
            ((FrameLayout)getParent()).removeView(this);
//            this.parentFragment.presentFragment(new FlexatarCameraCaptureFragment(flxData));
            this.parentFragment.presentFragment(new FlexatarCameraCaptureFragment(FlexatarData.removeMouth(unpackedFlexatar)));

        });


        parameterToEditLayout = new LinearLayout(getContext());
        String[] buttonNames = {"Top X", "Top Y","Bottom X","Bottom Y"};
        LinearLayout.LayoutParams switchEditButtonLayoutParams = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        switchEditButtonLayoutParams.leftMargin = AndroidUtilities.dp(3);
        switchEditButtonLayoutParams.rightMargin = AndroidUtilities.dp(3);
        selectedEdit = 0;
        TextView[] selects = new TextView[buttonNames.length];
        for (int i = 0; i < buttonNames.length; i++) {

            String name = buttonNames[i];

            TextView tv = new TextView(getContext());
            selects[i] = tv;
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
            tv.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            tv.setTextColor(Color.WHITE);
            if (i == 0)
                tv.setTextColor(Color.parseColor("#f7d26c"));

            tv.setLayoutParams(switchEditButtonLayoutParams);
            tv.setText(name);
            tv.setGravity(Gravity.CENTER);
            parameterToEditLayout.addView(tv);

            int finalI = i;
            tv.setOnClickListener((v)->{
                selects[selectedEdit].setTextColor(Color.WHITE);
                selectedEdit = finalI;
                selects[selectedEdit].setTextColor(Color.parseColor("#f7d26c"));
            });
        }
        controlsLayout.addView(parameterToEditLayout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,26,0,0));

        seekBar = new SeekBarView(getContext());
        seekBar.setReportChanges(true);
        int stepCount = 30;
        seekBar.setSeparatorsCount(stepCount);
        seekBar.setProgress(0.5f);

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
        LinearLayout.LayoutParams seekBarLayoutParameters = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,AndroidUtilities.dp(36) );
        seekBarLayoutParameters.gravity = Gravity.CENTER_HORIZONTAL;
        seekBarLayoutParameters.topMargin = AndroidUtilities.dp(26);
        controlsLayout.addView(seekBar,seekBarLayoutParameters);

        LinearLayout saveExitLayout = new LinearLayout(getContext());
        saveExitLayout.setOrientation(LinearLayout.HORIZONTAL);
        controlsLayout.addView(saveExitLayout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.BOTTOM,0,26,0,26));

        int buttonSize = 48;
        LinearLayout.LayoutParams saveExitLayoutParams = new LinearLayout.LayoutParams(
                0,
                AndroidUtilities.dp(buttonSize),
                1
        );

        saveButton = new ImageView(getContext());
        saveButton.setImageResource(R.drawable.background_selected);
        saveButton.setOnClickListener((v)->{
            ((FrameLayout)getParent()).removeView(this);
        });
        saveExitLayout.addView(saveButton,saveExitLayoutParams);

        ImageView closeButton = new ImageView(getContext());
        closeButton.setImageResource(R.drawable.msg_cancel);
        closeButton.setOnClickListener((v)->{
            ((FrameLayout)getParent()).removeView(this);
        });

        saveExitLayout.addView(closeButton,saveExitLayoutParams);
        toggleMouthEdit(isCalibrateMode);


    }
    private void toggleMouthEdit(boolean visible){
            int visibility = visible ? View.VISIBLE :View.GONE;
            saveButton.setVisibility(visibility);
            saveButton.setEnabled(visible);
            seekBar.setVisibility(visibility);
            seekBar.setEnabled(visible);
            parameterToEditLayout.setVisibility(visibility);
//            parameterToEditLayout.setEnabled(visible);
            setEnabledRecursive(parameterToEditLayout,visible);



    }


    public void openMouth(){
        drawer.setSpeechState(new float[]{0,0,-0.8f,0,0});
    }
    public void closeMouth(){
        drawer.setSpeechState(new float[]{0,0,0.05f,0,0});
    }
    private static void setEnabledRecursive(View view, boolean enabled) {
        view.setEnabled(enabled);

        if (view instanceof ViewGroup) {
            ViewGroup viewGroup = (ViewGroup) view;
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View child = viewGroup.getChildAt(i);
                setEnabledRecursive(child, enabled);
            }
        }
    }
}
