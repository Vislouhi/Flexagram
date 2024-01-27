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
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;

import org.checkerframework.checker.units.qual.A;
import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Cells.HeaderCell;
import org.telegram.ui.Cells.TextCell;
import org.telegram.ui.Cells.TextCheckCell;
import org.telegram.ui.Cells.TextDetailCell;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class FlexatarPreview extends FrameLayout {
    private final FlxDrawerNew drawer;
    private final CardView cardview;
    private final LengthBasedFlxUnpack unpackedFlexatar;
    private final BaseFragment parentFragment;
    private final byte[] flxData;
    private final FlexatarData flexatarData;
    private final FlexatarCell flexatarCell;

    private boolean isMouthOpened = false;
    private LinearLayout layout;
    private LinearLayout.LayoutParams cardVieParameters;
    private int selectedEdit;
    private LinearLayout parameterToEditLayout;
    private SeekBarView seekBar;
    private ImageView saveButton;
    private boolean isCalibrateMode = false;
    private Theme.ResourcesProvider resourcesProvider;
    private ScrollView controlsScrollView;
    private String newName = null;
    private boolean[] mouthCalibrationChanged = {false,false,false,false,false};
    private boolean isHeadAmplitudeChanged;
    public FlexatarCell getFlexatarCell(){
        return flexatarCell;
    }
    public FlexatarPreview(@NonNull Context context, FlexatarCell flexatarCell, BaseFragment parentFragment) {
        super(context);
        this.flexatarCell=flexatarCell;
        this.parentFragment = parentFragment;
        int currentOrientation = getResources().getConfiguration().orientation;

        byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarCell.getFlexatarFile());
        flxData = flxBytes;
//        Log.d("FLX_INJECT",""+flxBytes);
        unpackedFlexatar = new LengthBasedFlxUnpack(flxBytes);
        flexatarData = new FlexatarData(unpackedFlexatar);
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
        if (controlsScrollView == null) return;
        LinearLayout.LayoutParams layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, isPortrait ? Gravity.TOP : Gravity.LEFT, isPortrait ? 0 : 24, isPortrait ? 12 : 0, 0, 0);
        controlsScrollView.setLayoutParams(layoutParams);

    }
    private void makeLayout(int orientation){
        boolean isPortrait = orientation == Configuration.ORIENTATION_PORTRAIT;
        makeCardviewParameters(orientation);
        LinearLayout.LayoutParams layoutParams = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT, isPortrait ? Gravity.TOP : Gravity.LEFT, isPortrait ? 0 : 24, isPortrait ? 12 : 0, 0, 0);

        layout = new LinearLayout(getContext());
        layout.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        addView(layout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        layout.addView(cardview);
        controlsScrollView = new ScrollView(getContext());
        LinearLayout controlsLayout = new LinearLayout(getContext());
        controlsLayout.setOrientation(LinearLayout.VERTICAL);
        controlsLayout.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        controlsScrollView.addView(controlsLayout,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
        layout.addView(controlsScrollView,layoutParams);

        resourcesProvider = parentFragment.getResourceProvider();

        TextDetailCell changeNameCell = new TextDetailCell(getContext(),resourcesProvider, true);
        changeNameCell.setContentDescriptionValueFirst(true);
        changeNameCell.setTextAndValue(flexatarData.getName(), LocaleController.getString("FlexatarName", R.string.ViewInstructions), true);
        changeNameCell.valueTextView.setTextColor(getThemedColor(Theme.key_windowBackgroundWhiteGrayText2));
        controlsLayout.addView(changeNameCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
        changeNameCell.setOnClickListener(v->{
            parentFragment.showDialog(
                AlertDialogs.askFlexatarNameDialog(getContext(),name -> {
                    if (name.isEmpty()) name = "No Name";
                    newName = name;
                    changeNameCell.setTextAndValue(name, LocaleController.getString("FlexatarName", R.string.ViewInstructions), true);
                })
            );
        });

        TextCell makeMouthByPhotoCell = new TextCell(getContext());
        makeMouthByPhotoCell.setTextAndIcon(LocaleController.getString("MouthByPhoto", R.string.MouthByPhoto), R.drawable.msg_addphoto, true);
        controlsLayout.addView(makeMouthByPhotoCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
        makeMouthByPhotoCell.setOnClickListener(v->{
            ((FrameLayout)getParent()).removeView(this);
            this.parentFragment.presentFragment(new FlexatarCameraCaptureFragment(FlexatarData.removeMouth(unpackedFlexatar)));

        });

        FlexatarCalibrationCell headAmplitudeCell = new FlexatarCalibrationCell(getContext(), LocaleController.getString("HeadRotationAmp", R.string.HeadRotationAmp));
        controlsLayout.addView(headAmplitudeCell.getHeaderCell(),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
        controlsLayout.addView(headAmplitudeCell.getSeekBar(),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,38,Gravity.TOP|Gravity.CENTER_HORIZONTAL,54,12,54,0));
        headAmplitudeCell.setProgress(1f);
        headAmplitudeCell.setOnDragListener((progress) -> {
            Log.d("FLX_INJECT", "head amp progress" + (progress ));
            isHeadAmplitudeChanged = true;
            drawer.setHeadRotationAmplitude((3f -  2f * progress));
        });

        List<View> mouthCalibrationViews = new ArrayList<>();
        TextCheckCell isMouthCalibrationOnCell = new TextCheckCell(getContext());
        isMouthCalibrationOnCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        isMouthCalibrationOnCell.setTextAndValueAndCheck(LocaleController.getString("CalibrateTeeth", R.string.CalibrateTeeth), LocaleController.getString("CalibrateTeethInfo", R.string.CalibrateTeethInfo), false, true, true);
        controlsLayout.addView(isMouthCalibrationOnCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
        isMouthCalibrationOnCell.setOnClickListener((v)->{
            isMouthCalibrationOnCell.setChecked(!isMouthCalibrationOnCell.isChecked());
            if (isMouthCalibrationOnCell.isChecked()){
                openMouth();
                for(View view : mouthCalibrationViews){
                    view.setVisibility(View.VISIBLE);
                    view.setEnabled(true);
                }
            }else{
                closeMouth();
                for(View view : mouthCalibrationViews){
                    view.setVisibility(View.INVISIBLE);
                    view.setEnabled(false);
                }
            }
        });

        TextCheckCell isFlexatarSpeakingCell = new TextCheckCell(getContext());
        mouthCalibrationViews.add(isFlexatarSpeakingCell);
        isFlexatarSpeakingCell.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        isFlexatarSpeakingCell.setTextAndValueAndCheck(LocaleController.getString("ToggleSpeechAnimation", R.string.ToggleSpeechAnimation), LocaleController.getString("ToggleSpeechAnimationInfo", R.string.ToggleSpeechAnimationInfo), false, true, true);
        controlsLayout.addView(isFlexatarSpeakingCell,LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
        isFlexatarSpeakingCell.setOnClickListener((v)->{
            isFlexatarSpeakingCell.setChecked(!isFlexatarSpeakingCell.isChecked());
            if (isFlexatarSpeakingCell.isChecked()){
//                openMouth();
            }else{
//                closeMouth();
            }
        });


        String[] calibrationHeaderNames = {
                LocaleController.getString("TopTeethHorizontalPos", R.string.TopTeethHorizontalPos),
                LocaleController.getString("TopTeethVerticalPos", R.string.TopTeethVerticalPos),
                LocaleController.getString("BottomTeethHorizontalPos", R.string.BottomTeethHorizontalPos),
                LocaleController.getString("BottomTeethVerticalPos", R.string.BottomTeethVerticalPos),
                LocaleController.getString("TeethSize", R.string.TeethSize),

        };

        for (int i = 0; i < calibrationHeaderNames.length; i++) {
            FlexatarCalibrationCell calibrationCell = new FlexatarCalibrationCell(getContext(), calibrationHeaderNames[i]);
            mouthCalibrationViews.add(calibrationCell.getHeaderCell());
            mouthCalibrationViews.add(calibrationCell.getSeekBar());

            controlsLayout.addView(calibrationCell.getHeaderCell(),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT,0,Gravity.TOP,0,0,0,0));
            controlsLayout.addView(calibrationCell.getSeekBar(),LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,38,Gravity.TOP|Gravity.CENTER_HORIZONTAL,54,12,54,0));
            int finalI = i;
            calibrationCell.setOnDragListener((progress -> {
//                Log.d("FLX_INJECT", "mout cb "+ progress);
                mouthCalibrationChanged[finalI] = true;
                if (finalI == 3)
                    drawer.getFlexatarData().correctBotVerticalLipAnchor((progress - 0.5f) * 0.05f);
                else if (finalI == 0)
                    drawer.getFlexatarData().correctTopHorizontalLipAnchor((progress - 0.5f) * 0.15f);
                else if (finalI == 1)
                    drawer.getFlexatarData().correctTopVerticalLipAnchor((progress - 0.5f) * 0.05f);
                else if (finalI == 2)
                    drawer.getFlexatarData().correctBotHorizontalLipAnchor((progress - 0.5f) * 0.15f);
                else if (finalI == 4)
                    drawer.getFlexatarData().correctMouthSize(- (progress - 0.5f) * 0.2f);

            }));
        }

        for(View view : mouthCalibrationViews){
            view.setVisibility(View.INVISIBLE);
            view.setEnabled(false);
        }
    }

    public FlexatarStorageManager.FlexatarMetaData getNewMetaData(){
        FlexatarStorageManager.FlexatarMetaData metaData = new FlexatarStorageManager.FlexatarMetaData();

        boolean hasChanges = false;
        hasChanges = hasChanges || (newName != null);
        metaData.name = newName;

        if (atLeastOneTrue(mouthCalibrationChanged)){
            metaData.mouthCalibration = new float[]{
                drawer.getFlexatarData().topXCorrectionMouth,
                drawer.getFlexatarData().topYCorrectionMouth,
                drawer.getFlexatarData().botXCorrectionMouth,
                drawer.getFlexatarData().botYCorrectionMouth,
                drawer.getFlexatarData().sizeCorrectionMouth
            };
            hasChanges = true;
        }
        if (isHeadAmplitudeChanged){
            metaData.amplitude = drawer.getFlexatarData().headRotationAmplitude;
            hasChanges = true;
        }
        if (hasChanges)
            return metaData;
        else
            return null;
    }
    private static boolean atLeastOneTrue(boolean[] array) {
        for (boolean element : array) {
            if (element) {
                return true;
            }
        }
        return false;
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
    public int getThemedColor(int key) {
        return Theme.getColor(key, resourcesProvider);
    }
}
