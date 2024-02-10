package org.flexatar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.flexatar.DataOps.FlexatarData;
import org.flexatar.DataOps.LengthBasedFlxUnpack;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;

import java.io.File;
import java.util.Timer;
import java.util.TimerTask;

public class FlexatarControlPanelLayout  extends LinearLayout{
    private final ImageView icnFlx2;
    private final ImageView icnFlx1;
    private final LinearLayout panelLayout;
    private final LinearLayout previewLayout;
    private final VoIPBackgroundProvider backgroundProvider;
    private final RectF bgRect = new RectF();
    private final Paint paint;
    private final SeekBarView seekBar;
    private String chosenEffect = "No";
    private GLSurfaceView surfaceView;
    private final FrameLayout bottomButtonsLayout;
//    public final LinearLayout mainLayout;
    private OnCancelListener onCancelListener;
    private OnSendListener onSendListener;
    private FlxDrawer drawer;
    private Timer morphTimer;

    public void cancel() {
        if (onCancelListener != null) onCancelListener.onCancel(this);
    }

    public interface OnCancelListener {
        void onCancel(FlexatarControlPanelLayout layout);
    }
    public void setOnCancelListener(OnCancelListener onCancelListener){
        this.onCancelListener=onCancelListener;
    }

    public interface OnSendListener {
        void onSend(FlexatarControlPanelLayout layout);
    }
    public void setOnSendListener(OnSendListener onSendListener){
        this.onSendListener=onSendListener;
    }
    private File chosenFirst;
    private File chosenSecond;
    public FlexatarControlPanelLayout(Context context){
        super(context);
        File[] files = FlexatarStorageManager.getFlexatarFileList(context);
        chosenFirst = files[0];
        chosenSecond = files[1];
        icnFlx1= new ImageView(context);
        icnFlx2 = new ImageView(context);

        setClickable(true);
        backgroundProvider = new VoIPBackgroundProvider();
        backgroundProvider.setHasVideo(true);
        backgroundProvider.setTotalSize(100,100);
        LinearLayout mainLayout = this;
        int currentOrientation = getResources().getConfiguration().orientation;
        boolean isPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
        mainLayout.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
//      ======  Effect Panel Layout ====
        panelLayout = new LinearLayout(context);
        previewLayout = new LinearLayout(context);
        surfaceView = new GLSurfaceView(context);
        bottomButtonsLayout = new FrameLayout(context);
        configureLayoutOrientation(isPortrait);

        mainLayout.addView(panelLayout);
        panelLayout.setGravity(Gravity.CENTER);
        panelLayout.setOrientation(LinearLayout.VERTICAL);
        FlexatarHorizontalRecycleView flexatarRecyclerView = new FlexatarHorizontalRecycleView(context, (icnFlx) -> {

        });
        ((FlexatarHorizontalRecycleView.Adapter)flexatarRecyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
            if (file.equals(chosenFirst) ) return;
            chosenSecond = chosenFirst;
            chosenFirst = file;
            if (drawer!=null){
                FlexatarData flexatarData = FlexatarData.factory(file);
                flexatarData.getPreviewImage();
                Bitmap iconBitmap = flexatarData.getPreviewImage();
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);
                dr.setCornerRadius(AndroidUtilities.dp(8));
                icnFlx2.setImageDrawable(icnFlx1.getDrawable());
                icnFlx1.setImageDrawable(dr);
                drawer.changeFlexatar(flexatarData);
            }
        });
        panelLayout.addView(flexatarRecyclerView);
//============= Image Pair Layout ================
        LinearLayout imgPairLayout = new LinearLayout(context);


        imgPairLayout.setOrientation(LinearLayout.HORIZONTAL);
        panelLayout.addView(imgPairLayout, LayoutHelper.createLinear( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_HORIZONTAL));

        {
            ImageView icnFlx = icnFlx1;
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(chosenFirst,true).previewImage;
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));
            icnFlx.setImageDrawable(dr);
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            float imageWidth = 40;
            icnFlx.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth*ratio)));

            imgPairLayout.addView(icnFlx);
        }
        {
            ImageView icnFlx = icnFlx2;
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));
            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(chosenSecond,true).previewImage;
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));
            icnFlx.setImageDrawable(dr);
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            float imageWidth = 40;
            icnFlx.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth*ratio)));

            imgPairLayout.addView(icnFlx);
        }
        //-------------------EFFECT BUTTONS LAYOUT-------------
        LinearLayout effectLayout = new LinearLayout(context);
        effectLayout.setOrientation(LinearLayout.HORIZONTAL);
        effectLayout.setPadding(0, AndroidUtilities.dp(6), 0, 0);

        panelLayout.addView(effectLayout);
        String[] effectNames = {"No","Mix","Morph","Hybrid"};
        String[] effectCaptions = {
                LocaleController.getString("NoEffectButton", R.string.NoEffectButton),
                LocaleController.getString("MixEffectButton", R.string.MixEffectButton),
                LocaleController.getString("MorphEffectButton", R.string.MorphEffectButton),
                LocaleController.getString("HybridEffectButton", R.string.HybridEffectButton),

        };
        LinearLayout.LayoutParams layoutParams1 = new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1
        );
        layoutParams1.leftMargin = AndroidUtilities.dp(3);
        layoutParams1.rightMargin = AndroidUtilities.dp(3);
        TextView[] effectTextViews = new TextView[effectNames.length];
        seekBar = new SeekBarView(context);
        for (int i = 0; i < effectNames.length; i++) {

            String name = effectNames[i];

            TextView tv = new HideEmojiTextView(context, backgroundProvider);
            effectTextViews[i] = tv;
            if(name.equals(chosenEffect)) {
                tv.setTextColor(Color.parseColor("#f7d26c"));
            }

            tv.setLayoutParams(layoutParams1);
            tv.setText(effectCaptions[i]);
            tv.setGravity(Gravity.CENTER);
//            tv.setPadding(AndroidUtilities.dp(3), AndroidUtilities.dp(0), AndroidUtilities.dp(3), AndroidUtilities.dp(0));
            int finalI = i;
            tv.setOnClickListener((v) -> {

                for (int j = 0; j < effectNames.length; j++) {
                    effectTextViews[j].setTextColor(Color.WHITE);
                }
                effectTextViews[finalI].setTextColor(Color.parseColor("#f7d26c"));
                chosenEffect = effectNames[finalI];
                setupEffects();

                if (chosenEffect.equals("Mix")){
                    seekBar.setVisibility(View.VISIBLE);
                }else{
                    seekBar.setVisibility(View.GONE);
                }
            });
            effectLayout.addView(tv);
        }
//        linearLayout.setEffectTextViews(effectTextViews);

//        SeekBarView seekBar = new SeekBarView(context);
        seekBar.setReportChanges(true);
        int stepCount = 30;
        seekBar.setSeparatorsCount(stepCount);
        seekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                mixWeight = 1f-seekBar.getProgress();
                drawer.setMixWeightVal(mixWeight);
//                FlexatarRenderer.chosenMixWeight = 1-progress;
//                FlexatarRenderer.effectsMixWeight = FlexatarRenderer.chosenMixWeight;
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

        seekBar.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        seekBar.setProgress(0.5f);
        if (!chosenEffect.equals("Mix")){
            seekBar.setVisibility(View.GONE);
        }
        panelLayout.addView(seekBar,LayoutHelper.createFrame(200, 20,Gravity.CENTER,0,6,0,0));
// ============ Preview Layout =======

        previewLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(previewLayout);
        createFlexatarView();
//        FrameLayout frameWarper = new FrameLayout(context);
//        frameWarper.addView(surfaceView,LayoutHelper.createLinear(150,200));
        previewLayout.addView(surfaceView);



        previewLayout.addView(bottomButtonsLayout);




        ImageView closePanelIcon = new ImageView(context){
            @Override
            protected void onDraw(Canvas canvas) {
                bgRect.set(0, 0, getWidth(), getHeight());
                canvas.drawRoundRect(bgRect, dp(getWidth()/2), dp(getHeight()/2), paint);
                super.onDraw(canvas);
            }
        };
        closePanelIcon.setImageResource(R.drawable.input_clear);
        closePanelIcon.setOnClickListener((v) -> {
            if (onCancelListener != null) onCancelListener.onCancel(this);
//            pre.setVisibility(View.GONE);
//            linearLayout.fulfillClose();
        });
        bottomButtonsLayout.addView(closePanelIcon,LayoutHelper.createFrame(46, 46, Gravity.RIGHT, 0, 0, 12, 0));

        ImageView sendIcon = new ImageView(context){
            @Override
            protected void onDraw(Canvas canvas) {
                bgRect.set(0, 0, getWidth(), getHeight());
                canvas.drawRoundRect(bgRect, dp(getWidth()/2), dp(getHeight()/2), paint);

//                Drawable drawable = getDrawable();
//                int drawableWidth = getWidth();
//                int drawableHeight = getHeight();
//                float scale = 0.8f;
//                drawable.setBounds((int) (drawableWidth*(1f-scale)/2f), (int) (drawableHeight*(1f-scale)/2f), (int) (drawableWidth * scale), (int) (drawableHeight * scale));
//                drawable.draw(canvas);
                super.onDraw(canvas);
            }
        };
        sendIcon.setImageResource(R.drawable.attach_send);
        sendIcon.setOnClickListener((v) -> {
            FlexatarNotificator.chosenStateForRoundVideo = new FlexatarNotificator.ChosenStateForRoundVideo();
            FlexatarNotificator.chosenStateForRoundVideo.effect = effectIndex;
            FlexatarNotificator.chosenStateForRoundVideo.firstFile = chosenFirst;
            FlexatarNotificator.chosenStateForRoundVideo.secondFile = chosenSecond;
            FlexatarNotificator.chosenStateForRoundVideo.mixWeight = mixWeight;

            if (onSendListener!=null)onSendListener.onSend(this);
//            linearLayout.setVisibility(View.GONE);
//            linearLayout.fulfillClose();
        });
        bottomButtonsLayout.addView(sendIcon,LayoutHelper.createFrame(46, 46, Gravity.LEFT, 12, 0, 12, 0));

        bottomButtonsLayout.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));

        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        paint = new Paint();
        paint.setARGB(200, 0, 0, 0);
    }
    private int effectIndex = 0;
    private void setupEffects(){

        if (seekBar == null)return;
        if (chosenEffect.equals("No")){
            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.NO;
            invalidateMorphTimer();
            drawer.setisEffectOnVal(false);
//                    FlexatarRenderer.isEffectsOn = false;
//                    FlexatarRenderer.isMorphEffect = false;

        } else if (chosenEffect.equals("Mix")){
            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.MIX;

            invalidateMorphTimer();
            drawer.setisEffectOnVal(true);
            drawer.setEffectIdVal(0);
            drawer.setMixWeightVal(1f-seekBar.getProgress());


//                    FlexatarRenderer.isEffectsOn = true;
//                    FlexatarRenderer.effectID = 0;
//                    FlexatarRenderer.effectsMixWeight = FlexatarRenderer.chosenMixWeight;
//                    FlexatarRenderer.isMorphEffect = false;
        }else if (chosenEffect.equals("Morph")){
            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.MORPH;
            invalidateMorphTimer();
            createMorphTimer();
            drawer.setisEffectOnVal(true);
            drawer.setEffectIdVal(0);
            drawer.setMixWeightVal(1f-seekBar.getProgress());

//                    FlexatarRenderer.isEffectsOn = true;
//                    FlexatarRenderer.effectID = 0;
//                    FlexatarRenderer.isMorphEffect = true;
//                    FlexatarRenderer.effectsMixWeight = 0;

        }else if (chosenEffect.equals("Hybrid")){
            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.HYBRID;
            invalidateMorphTimer();
            createHybridTimer();
            drawer.setisEffectOnVal(true);
            drawer.setEffectIdVal(1);
            drawer.setMixWeightVal(1f-seekBar.getProgress());
//                    FlexatarRenderer.isEffectsOn = true;
//                    FlexatarRenderer.effectID = 1;
//                    FlexatarRenderer.isMorphEffect = false;

        }
    }
    private float mixWeight = 0.5f;
    private void createMorphTimer(){
        morphTimer = new Timer();

        TimerTask task = new TimerTask() {
            private float counter = mixWeight;
            private float delta = 0.02f;

            @Override
            public void run() {
                counter += delta;
                if (counter>1) {counter = 1f;delta=-delta;}else if (counter<0) {counter = 0f;delta=-delta;}
                drawer.setMixWeightVal(counter);
            }
        };


        morphTimer.scheduleAtFixedRate(task, 0, 40);
    }
    private void invalidateMorphTimer(){
        if (morphTimer!=null) {
            morphTimer.cancel();
            morphTimer.purge();
            morphTimer = null;
        }
    }

    private void createHybridTimer(){
        morphTimer = new Timer();

        TimerTask task = new TimerTask() {
            private float counter = mixWeight;
            private float delta = 0.005f;

            @Override
            public void run() {
                counter += delta;
                if (counter>1) {counter -= 1f;}
                drawer.setMixWeightVal(counter);
            }
        };


        morphTimer.scheduleAtFixedRate(task, 0, 40);
    }

    public void createFlexatarView(){


        if (surfaceView == null) surfaceView = new GLSurfaceView(getContext());
        surfaceView.setEGLContextClientVersion(2);
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        renderer.isRounded = true;
        drawer = new FlxDrawer();

        renderer.drawer = drawer;
        drawer.setFlexatarData(FlexatarData.factory(chosenFirst));
        drawer.setFlexatarDataAlt(FlexatarData.factory(chosenSecond));
        surfaceView.setZOrderOnTop(true);
        surfaceView.setBackgroundColor(Color.TRANSPARENT);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);

        surfaceView.setRenderer(renderer);
        setupEffects();
    }
    private void configureLayoutOrientation(boolean isPortrait){
        if (isPortrait){
            panelLayout.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT));
            previewLayout.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.WRAP_CONTENT));
            bottomButtonsLayout.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,LayoutHelper.MATCH_PARENT));
            surfaceView.setLayoutParams(LayoutHelper.createLinear(150,200,Gravity.CENTER_HORIZONTAL,12,12,12,12));
        }else{
            panelLayout.setLayoutParams(LayoutHelper.createLinear(0,LayoutHelper.WRAP_CONTENT,1f));
            previewLayout.setLayoutParams(LayoutHelper.createLinear(0,LayoutHelper.WRAP_CONTENT,1f));
            bottomButtonsLayout.setLayoutParams(LayoutHelper.createLinear(0,LayoutHelper.WRAP_CONTENT,1f));
            surfaceView.setLayoutParams(LayoutHelper.createLinear(150,200,0f,Gravity.CENTER_HORIZONTAL,36,4,36,0));
        }
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {

        boolean isPortrait = newConfig.orientation == Configuration.ORIENTATION_PORTRAIT;
        this.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        previewLayout.setOrientation(isPortrait ? LinearLayout.VERTICAL : LinearLayout.HORIZONTAL);
        configureLayoutOrientation(isPortrait);
        this.forceLayout();
    }
    @Override
    protected void dispatchDraw(Canvas canvas) {
        bgRect.set(0, 0, getWidth(), getHeight());
//        backgroundProvider.setDarkTranslation(getX(), getY());
        canvas.drawRoundRect(bgRect, dp(20), dp(20), paint);
        super.dispatchDraw(canvas);
    }

    private int surfaceViewLayoutIndex = -1;
    @Override
    protected void onDetachedFromWindow() {
        invalidateMorphTimer();
        surfaceViewLayoutIndex = previewLayout.indexOfChild(surfaceView);
        previewLayout.removeView(surfaceView);
        surfaceView = null;
        super.onDetachedFromWindow();
        Log.d("FLX_INJECT","flexatar panel onDetachedFromWindow");
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (surfaceViewLayoutIndex!=-1){
            createFlexatarView();
            int currentOrientation = getResources().getConfiguration().orientation;
            boolean isPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
            configureLayoutOrientation(isPortrait);
            previewLayout.addView(surfaceView,surfaceViewLayoutIndex);

        }
    }
}
