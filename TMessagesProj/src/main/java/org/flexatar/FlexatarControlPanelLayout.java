package org.flexatar;

import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.graphics.ColorUtils;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.SeekBarView;
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;
import org.telegram.ui.LaunchActivity;

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
    private final int account;
    private  SeekBarView seekBar;
    private final FlexatarStorageManager.FlexatarChooser currentFlexatarChooser;
    private final boolean isWithPreview;
    private FlexatarHorizontalRecycleView flexatarRecyclerView;
    private FlexatarHorizontalRecycleView videoFlexatarRecyclerView;
    private LinearLayout imgPairLayout;
    private LinearLayout effectLayout;
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
//    private File chosenFirst;
//    private File chosenSecond;
    public void resetEffects(){
        currentFlexatarChooser.setEffectIndex(0);
        currentFlexatarChooser.saveMixWeight(0.5f);
    }

    public FlexatarControlPanelLayout(Context context,int account,boolean isWithPreview,FlexatarStorageManager.FlexatarChooser chooser){
        super(context);
        this.isWithPreview=isWithPreview;
        this.account=account;
       currentFlexatarChooser = chooser;
       mixWeight = currentFlexatarChooser.getMixWeight();
//        File[] files = FlexatarStorageManager.getFlexatarFileList(context);
//        chosenFirst = files[0];
//        chosenSecond = files[1];
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
//        ====== TAB SWITCHER ===
        ViewPagerFixed.TabsView tabsView = new ViewPagerFixed.TabsView(getContext(), true, 3, LaunchActivity.getLastFragment().getResourceProvider()) {
            @Override
            public void selectTab(int currentPosition, int nextPosition, float progress) {
                super.selectTab(currentPosition, nextPosition, progress);

            }
        };
        currentPage = currentFlexatarChooser.getFlxType();
//        tabsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tabsView.tabMarginDp = 16;
        tabsView.addTab(0, LocaleController.getString("VideoTab",R.string.VideoTab));
        tabsView.addTab(1, LocaleController.getString("PhotoTab",R.string.PhotoTab));
        tabsView.selectTabWithId(currentPage,1f);
        tabsView.setPadding(0,6,0,6);
        tabsView.setDelegate(new ViewPagerFixed.TabsView.TabsViewDelegate() {
            @Override
            public void onPageSelected(int page, boolean forward) {
                currentFlexatarChooser.setFlxType(page);
                switchTabs(page);

//                flexatarRecyclerView
//                imgPairLayout
//                effectLayout
//                seekBar
            }

            @Override
            public void onPageScrolled(float progress) {

            }

            @Override
            public void onSamePageSelected() {

            }

            @Override
            public boolean canPerformActions() {
                return true;
            }

            @Override
            public void invalidateBlur() {

            }
        });

        tabsView.finishAddingTabs();
        tabsView.setLayoutParams(LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT,32,0,0,0,0));
        panelLayout.addView(tabsView);

//        =====Flexatar chooser recycle view ==========
        /*flexatarRecyclerView = new FlexatarHorizontalRecycleView(context, (icnFlx) -> {

        });
        ((FlexatarHorizontalRecycleView.Adapter)flexatarRecyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
            if (file.equals(currentFlexatarChooser.getChosenFirst()) ) return;
            currentFlexatarChooser.setChosenFlexatar(file.getAbsolutePath());
//            chosenSecond = chosenFirst;
//            chosenFirst = file;
//            if (drawer!=null){
//                FlexatarData flexatarData = FlexatarData.factory(file);
//                flexatarData.getPreviewImage();
                Bitmap iconBitmap = currentFlexatarChooser.getFirstFlxData().getPreviewImage();
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);
                dr.setCornerRadius(AndroidUtilities.dp(8));
                icnFlx2.setImageDrawable(icnFlx1.getDrawable());
                icnFlx1.setImageDrawable(dr);
//                drawer.changeFlexatar(flexatarData);
//            }
        });*/
        if (currentFlexatarChooser.getFlxType() == 0){
            createVideoFlexatarRecyclerView();
            panelLayout.addView(videoFlexatarRecyclerView);
        }else if (currentFlexatarChooser.getFlxType() == 1){
            createFlexatarRecyclerView();
            addPhotoFlexatarViews();
        }


//============= Image Pair Layout ================
        // ============ Preview Layout =======

        previewLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.addView(previewLayout);
        if (isWithPreview) {
            createFlexatarView();
            previewLayout.addView(surfaceView);
        }



        previewLayout.addView(bottomButtonsLayout);
//        panelLayout.addView(seekBar,LayoutHelper.createFrame(200, 20,Gravity.CENTER,0,6,0,0));




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
        if (isWithPreview) {
            ImageView sendIcon = new ImageView(context) {
                @Override
                protected void onDraw(Canvas canvas) {
                    bgRect.set(0, 0, getWidth(), getHeight());
                    canvas.drawRoundRect(bgRect, dp(getWidth() / 2), dp(getHeight() / 2), paint);

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
                FlexatarNotificator.chosenStateForRoundVideo.firstFile = currentFlexatarChooser.getChosenFirst();
                FlexatarNotificator.chosenStateForRoundVideo.secondFile = currentFlexatarChooser.getChosenSecond();
                FlexatarNotificator.chosenStateForRoundVideo.mixWeight = mixWeight;

                if (onSendListener != null) onSendListener.onSend(this);
//            linearLayout.setVisibility(View.GONE);
//            linearLayout.fulfillClose();
            });
            bottomButtonsLayout.addView(sendIcon, LayoutHelper.createFrame(46, 46, Gravity.LEFT, 12, 0, 12, 0));
        }
        bottomButtonsLayout.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));

        setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        paint = new Paint();
        paint.setARGB(200, 0, 0, 0);
        this.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int visibility = FlexatarControlPanelLayout.this.getVisibility();
                if (visibility == View.VISIBLE) {

                    switchTabs(currentFlexatarChooser.getFlxType());

                }
            }
        });
    }
    int currentPage = -1;
    public void switchTabs(int page){
        if (currentPage == page) return;
        currentPage = page;
        if (page==0){
            for (View v : new View[]{flexatarRecyclerView,imgPairLayout,effectLayout,seekBar} ){
                if (v!=null && panelLayout.indexOfChild(v)!=-1){
                    panelLayout.removeView(v);
                }
            }
            if (videoFlexatarRecyclerView == null){
                createVideoFlexatarRecyclerView();
            }
            panelLayout.addView(videoFlexatarRecyclerView);
        }else  if (page==1){
            for (View v : new View[]{videoFlexatarRecyclerView} ){
                if (v!=null && panelLayout.indexOfChild(v)!=-1){
                    panelLayout.removeView(v);
                }
            }
            if (flexatarRecyclerView == null){
                createFlexatarRecyclerView();
                addPhotoFlexatarViews();
            }else {
                for (View v : new View[]{flexatarRecyclerView, imgPairLayout, effectLayout, seekBar}) {
                    if (v != null && panelLayout.indexOfChild(v) == -1) {
                        panelLayout.addView(v);
                    }
                }
            }
        }
    }
    private void addPhotoFlexatarViews(){
        panelLayout.addView(flexatarRecyclerView);
        panelLayout.addView(imgPairLayout, LayoutHelper.createLinear( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_HORIZONTAL));
        panelLayout.addView(effectLayout);
        panelLayout.addView(seekBar,LayoutHelper.createFrame(200, 20,Gravity.CENTER,0,6,0,0));


    }
    public void createFlexatarRecyclerView(){
        flexatarRecyclerView = new FlexatarHorizontalRecycleView(getContext(),account,1, (icnFlx) -> {

        });
        ((FlexatarHorizontalRecycleView.Adapter)flexatarRecyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
            if (file.equals(currentFlexatarChooser.getChosenFirst()) ) return;
            currentFlexatarChooser.setChosenFlexatar(file.getAbsolutePath());
//            chosenSecond = chosenFirst;
//            chosenFirst = file;
//            if (drawer!=null){
//                FlexatarData flexatarData = FlexatarData.factory(file);
//                flexatarData.getPreviewImage();
            Bitmap iconBitmap = currentFlexatarChooser.getFirstFlxData().getPreviewImage();
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));
            icnFlx2.setImageDrawable(icnFlx1.getDrawable());
            icnFlx1.setImageDrawable(dr);
//                drawer.changeFlexatar(flexatarData);
//            }
        });
        imgPairLayout = new LinearLayout(getContext());


        imgPairLayout.setOrientation(LinearLayout.HORIZONTAL);

        {
            ImageView icnFlx = icnFlx1;
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(currentFlexatarChooser.getChosenFirst(),true).previewImage;
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);
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
            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(currentFlexatarChooser.getChosenSecond(),true).previewImage;
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));
            icnFlx.setImageDrawable(dr);
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            float imageWidth = 40;
            icnFlx.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth*ratio)));

            imgPairLayout.addView(icnFlx);
        }
        //-------------------EFFECT BUTTONS LAYOUT-------------
        effectLayout = new LinearLayout(getContext());
        effectLayout.setOrientation(LinearLayout.HORIZONTAL);
        effectLayout.setPadding(0, AndroidUtilities.dp(6), 0, 0);

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
        seekBar = new SeekBarView(getContext());
        chosenEffect = effectNames[currentFlexatarChooser.getEffectIndex()];
        for (int i = 0; i < effectNames.length; i++) {

            String name = effectNames[i];

            TextView tv = new HideEmojiTextView(getContext(), backgroundProvider);
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
//                setupEffects();

                if (chosenEffect.equals("Mix")){
                    seekBar.setVisibility(View.VISIBLE);
                }else{
                    seekBar.setVisibility(View.GONE);
                }
                currentFlexatarChooser.setEffectIndex(finalI);
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
//                drawer.setMixWeightVal(mixWeight);
                if (stop){
                    Log.d("FLX_INJECT", "mix weight set to "+mixWeight);
                    currentFlexatarChooser.saveMixWeight(mixWeight);
                }else{
                    currentFlexatarChooser.setMixWeight(mixWeight);

                }
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
        seekBar.setProgress(1f - currentFlexatarChooser.getMixWeight());
        if (!chosenEffect.equals("Mix")){
            seekBar.setVisibility(View.GONE);
        }

    }
    public void createVideoFlexatarRecyclerView(){
        videoFlexatarRecyclerView = new FlexatarHorizontalRecycleView(getContext(),account,0, (icnFlx) -> {

        });
        ((FlexatarHorizontalRecycleView.Adapter)videoFlexatarRecyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
            if (file.equals(currentFlexatarChooser.getChosenVideo()) ) return;
            currentFlexatarChooser.setChosenVideoFlexatar(file.getAbsolutePath());

        });
    }
    private int effectIndex = 0;
    private void setupEffects(){

        if (seekBar == null)return;
        /*if (!isWithPreview) {
            if (chosenEffect.equals("No")){
                FlexatarRenderer.isEffectsOn = false;
                FlexatarRenderer.isMorphEffect = false;

            } else if (chosenEffect.equals("Mix")){
                FlexatarRenderer.isEffectsOn = true;
                FlexatarRenderer.effectID = 0;
                FlexatarRenderer.effectsMixWeight = FlexatarRenderer.chosenMixWeight;
                FlexatarRenderer.isMorphEffect = false;
            }else if (chosenEffect.equals("Morph")){
                FlexatarRenderer.isEffectsOn = true;
                FlexatarRenderer.effectID = 0;
                FlexatarRenderer.isMorphEffect = true;
                FlexatarRenderer.effectsMixWeight = 0;

            }else if (chosenEffect.equals("Hybrid")){
                FlexatarRenderer.isEffectsOn = true;
                FlexatarRenderer.effectID = 1;
                FlexatarRenderer.isMorphEffect = false;

            }
            return;
        }
*/
        if (chosenEffect.equals("No")){
//            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.NO;
//            invalidateMorphTimer();
//            drawer.setisEffectOnVal(false);

        } else if (chosenEffect.equals("Mix")){
//            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.MIX;

//            invalidateMorphTimer();
//            drawer.setisEffectOnVal(true);
//            drawer.setEffectIdVal(0);
//            drawer.setMixWeightVal(1f-seekBar.getProgress());
        }else if (chosenEffect.equals("Morph")){
//            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.MORPH;
//            invalidateMorphTimer();
//            createMorphTimer();
//            drawer.setisEffectOnVal(true);
//            drawer.setEffectIdVal(0);
//            drawer.setMixWeightVal(1f-seekBar.getProgress());

        }else if (chosenEffect.equals("Hybrid")){
//            effectIndex = FlexatarNotificator.ChosenStateForRoundVideo.HYBRID;
//            invalidateMorphTimer();
//            createHybridTimer();
//            drawer.setisEffectOnVal(true);
//            drawer.setEffectIdVal(1);
//            drawer.setMixWeightVal(1f-seekBar.getProgress());

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
        if (!isWithPreview) return;

        if (surfaceView == null) surfaceView = new GLSurfaceView(getContext());
        surfaceView.setEGLContextClientVersion(2);
        FlexatarViewRenderer renderer = new FlexatarViewRenderer();
        renderer.isRounded = true;
        drawer = new FlxDrawer();

        renderer.drawer = drawer;
        drawer.setFlexatarChooser(currentFlexatarChooser);
        drawer.setFlexatarData(currentFlexatarChooser.getFirstFlxData());
        drawer.setFlexatarDataAlt(currentFlexatarChooser.getSecondFlxData());

        surfaceView.setZOrderOnTop(true);
        surfaceView.setBackgroundColor(Color.TRANSPARENT);
        surfaceView.setEGLConfigChooser(8, 8, 8, 8, 0, 0);
        surfaceView.getHolder().setFormat(android.graphics.PixelFormat.RGBA_8888);

        surfaceView.setRenderer(renderer);
        setupEffects();
    }
    private void configureLayoutOrientation(boolean isPortrait){
        if (!isWithPreview) return;
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
        if (surfaceView!=null) {
            surfaceViewLayoutIndex = previewLayout.indexOfChild(surfaceView);
            previewLayout.removeView(surfaceView);
            surfaceView = null;
        }

        super.onDetachedFromWindow();
        Log.d("FLX_INJECT","flexatar panel onDetachedFromWindow");
    }

    @Override
    public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE){
            {
                ImageView icnFlx = icnFlx1;
                icnFlx.setContentDescription("flexatar button");
                icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
                icnFlx.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

                Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(currentFlexatarChooser.getChosenFirst(),true).previewImage;
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);
                dr.setCornerRadius(AndroidUtilities.dp(8));
                icnFlx.setImageDrawable(dr);

            }
            {
                ImageView icnFlx = icnFlx2;
                Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(currentFlexatarChooser.getChosenSecond(),true).previewImage;
                RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap);
                dr.setCornerRadius(AndroidUtilities.dp(8));
                icnFlx.setImageDrawable(dr);

            }
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        if (!isWithPreview) return;
        if (surfaceViewLayoutIndex!=-1){
            createFlexatarView();
            int currentOrientation = getResources().getConfiguration().orientation;
            boolean isPortrait = currentOrientation == Configuration.ORIENTATION_PORTRAIT;
            configureLayoutOrientation(isPortrait);
            previewLayout.addView(surfaceView,surfaceViewLayoutIndex);

        }
    }
}
