package org.flexatar;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.RectF;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;


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


public class FlexatarUI {
    public static class FlexatarPanelLayout extends LinearLayout{
        private final RectF bgRect = new RectF();
        private final VoIPBackgroundProvider backgroundProvider;
        private Runnable onClose;
        public TextView switchToCameraButton;
        private ImageView img1;
        private ImageView img2;
        private TextView[] effectTextViews;
        public void setEffectTextViews(TextView[] effectTextViews){
            this.effectTextViews=effectTextViews;
        }
        public void resetEffects(){
            for (int j = 0; j < effectTextViews.length; j++) {
                effectTextViews[j].setTextColor(Color.WHITE);
            }
            effectTextViews[0].setTextColor(Color.parseColor("#f7d26c"));
            FlexatarRenderer.isEffectsOn = false;
            FlexatarRenderer.effectID = 0;
            FlexatarRenderer.isMorphEffect = false;
        }
        public void setImg1(ImageView img){
            img1=img;
        }
        public void setImg2(ImageView img){
            img2=img;
        }
        public ImageView getImg1(){
            return img1;
        }
        public ImageView getImg2(){
            return img2;
        }
        public void updateIcons(){
            Bitmap iconBitmap1 = FlexatarStorageManager.getFlexatarMetaData(chosenFirst,true).previewImage;
            RoundedBitmapDrawable dr1 = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap1);
            dr1.setCornerRadius(AndroidUtilities.dp(8));
            img1.setImageDrawable(dr1);
            Bitmap iconBitmap2 = FlexatarStorageManager.getFlexatarMetaData(chosenSecond,true).previewImage;
            RoundedBitmapDrawable dr2 = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap2);
            dr2.setCornerRadius(AndroidUtilities.dp(8));
            img2.setImageDrawable(dr2);
        }

        public void setOnCloseListener(Runnable onClose){
            this.onClose=onClose;
        }
        public void fulfillClose(){
            if (onClose!=null)
                onClose.run();
        }
        public FlexatarPanelLayout(Context context, VoIPBackgroundProvider backgroundProvider) {
            super(context);
            this.backgroundProvider = backgroundProvider;
            backgroundProvider.attach(this);
        }

        @Override
        protected void dispatchDraw(Canvas canvas) {
            bgRect.set(0, 0, getWidth(), getHeight());
            backgroundProvider.setDarkTranslation(getX(), getY());
            canvas.drawRoundRect(bgRect, dp(20), dp(20), backgroundProvider.getDarkPaint());
            super.dispatchDraw(canvas);
        }
    }

    static String chosenEffect = "No";
    public static File chosenFirst;
    public static File chosenSecond;


    public static PopupWindow panelPopup(Context context,View location){
        VoIPBackgroundProvider bkgProvider = new VoIPBackgroundProvider();
        bkgProvider.setTotalSize(200, 200);
        bkgProvider.setHasVideo(true);
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//        View popupView = inflater.inflate(R.layout.wait_popup_window, null);
        FlexatarPanelLayout popupView = makeFlexatarEffectsPanel(context, bkgProvider);
        popupView.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        PopupWindow popupWindow = new PopupWindow(
                popupView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT

        );
        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        Display display = windowManager.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = Math.min(size.x, size.y);

        int windowWidth = width - AndroidUtilities.dp(24);
        int xOffset = AndroidUtilities.dp(12);

        popupWindow.setWidth(windowWidth);
        popupView.setOnCloseListener(() -> {
            popupWindow.dismiss();
            Log.d("FLX_INJECT","popup dismissed");
        });
        popupWindow.showAsDropDown(location, xOffset, AndroidUtilities.dp(48));
//        popupWindow.showAtLocation(location, Gravity.CENTER_HORIZONTAL | Gravity.TOP, 0, AndroidUtilities.dp(46));
        return popupWindow;
    }
    public interface FlexatarChooseListener{
        void onChoose(ImageView icon);
    }
    /*public static HorizontalScrollView flexatarScrollView(Context context,FlexatarChooseListener onChooseListener){
//        ImageView icnFlx1= new ImageView(context);
//        ImageView icnFlx2 = new ImageView(context);
        HorizontalScrollView scrollView = new HorizontalScrollView(context);
        LinearLayout flxIconsLayout = new LinearLayout(context);
        flxIconsLayout.setOrientation(LinearLayout.HORIZONTAL);

        File[] flexatarsInLocalStorage = FlexatarStorageManager.getFlexatarFileList(context);
        for (int i = 0; i < flexatarsInLocalStorage.length; i++) {
            File flexatarFile = flexatarsInLocalStorage[i];
            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(flexatarFile,true).previewImage;;
            RoundedBitmapDrawable dr = RoundedBitmapDrawableFactory.create(context.getResources(), iconBitmap);
            dr.setCornerRadius(AndroidUtilities.dp(8));

            ImageView icnFlx = new ImageView(context);
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));
//            icnFlx.setImageResource(R.drawable.calls_flexatar);
//            Bitmap iconBitmap = FlexatarRenderer.icons.get(i);
            icnFlx.setImageDrawable(dr);
            float ratio = (float)iconBitmap.getHeight()/(float)iconBitmap.getWidth();
            float imageWidth = 70;
            icnFlx.setLayoutParams(new LinearLayout.LayoutParams(AndroidUtilities.dp(imageWidth), AndroidUtilities.dp(imageWidth*ratio)));
//            icnFlx.requestLayout();
            flxIconsLayout.addView(icnFlx);
            int finalI = i;
            icnFlx.setOnClickListener((v) -> {

                if (FlexatarUI.chosenFirst.getName().equals(flexatarFile.getName())) return;
                FlexatarUI.chosenSecond = FlexatarUI.chosenFirst;
                FlexatarUI.chosenFirst = flexatarFile;
                FlexatarRenderer.altFlxData = FlexatarRenderer.currentFlxData;
                byte[] flxBytes = FlexatarStorageManager.dataFromFile(flexatarFile);
//                FlexatarData flexatarData =
                FlexatarRenderer.currentFlxData = new FlexatarData(new LengthBasedFlxUnpack(flxBytes));
//                FlexatarRenderer.currentFlxData = FlexatarRenderer.loadFlexatarByLink(FlexatarRenderer.flexatarLinks.get(FlexatarUI.chosenFirst));

                if (chosenEffect.equals("Morph")){
                    FlexatarRenderer.effectsMixWeight = 0;
                }
                if (onChooseListener != null)
                    onChooseListener.onChoose(icnFlx);
                *//*
                icnFlx2.setImageDrawable(icnFlx1.getDrawable());
                icnFlx1.setImageDrawable(icnFlx.getDrawable());*//*
            });

        }
        scrollView.addView(flxIconsLayout);
        return scrollView;
    }
*/
    public static FlexatarPanelLayout makeFlexatarChoosePanel(Context context, VoIPBackgroundProvider backgroundProvider){
        FlexatarRenderer.isEffectsOn = false;
        FlexatarRenderer.effectID = 0;
        FlexatarRenderer.isMorphEffect = false;

        FlexatarPanelLayout linearLayout = new FlexatarPanelLayout(context,backgroundProvider);
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(12);
        linearLayout.setPadding(pad, pad, pad, pad);


        linearLayout.addView(new FlexatarHorizontalRecycleView(context,null));
//        linearLayout.addView(flexatarScrollView(context,null));

        return linearLayout;
    }
    public static FlexatarPanelLayout makeFlexatarEffectsPanel(Context context, VoIPBackgroundProvider backgroundProvider){
        ImageView icnFlx1= new ImageView(context);
        ImageView icnFlx2 = new ImageView(context);

        FlexatarPanelLayout linearLayout = new FlexatarPanelLayout(context,backgroundProvider);
        linearLayout.setImg1(icnFlx1);
        linearLayout.setImg2(icnFlx2);

        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(0, 0, 0, AndroidUtilities.dp(0));


        linearLayout.addView(new FlexatarHorizontalRecycleView(context,(icnFlx)->{
            icnFlx2.setImageDrawable(icnFlx1.getDrawable());
            icnFlx1.setImageDrawable(icnFlx.getDrawable());
        }));
//        linearLayout.addView(flexatarScrollView(context,(icnFlx)->{
//            icnFlx2.setImageDrawable(icnFlx1.getDrawable());
//            icnFlx1.setImageDrawable(icnFlx.getDrawable());
//        }));
//        linearLayout.addView(scrollView);
//-------------------IMAGE PAIR LAYOUT-------------
        LinearLayout imgPairLayout = new LinearLayout(context);
        imgPairLayout.setOrientation(LinearLayout.HORIZONTAL);
        linearLayout.addView(imgPairLayout,LayoutHelper.createLinear( LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT,Gravity.CENTER_HORIZONTAL));

        {
            ImageView icnFlx = icnFlx1;
            icnFlx.setContentDescription("flexatar button");
            icnFlx.setBackground(Theme.createSelectorDrawable(ColorUtils.setAlphaComponent(Color.WHITE, (int) (255 * 0.3f))));
            icnFlx.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(0), AndroidUtilities.dp(0), AndroidUtilities.dp(0));

            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(chosenFirst,true).previewImage;;
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
            Bitmap iconBitmap = FlexatarStorageManager.getFlexatarMetaData(chosenSecond,true).previewImage;;
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

        linearLayout.addView(effectLayout);
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
        SeekBarView seekBar = new SeekBarView(context);
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
                if (chosenEffect.equals("Mix")){
                    seekBar.setVisibility(View.VISIBLE);
                }else{
                    seekBar.setVisibility(View.GONE);
                }
            });
            effectLayout.addView(tv);
        }
        linearLayout.setEffectTextViews(effectTextViews);

//        SeekBarView seekBar = new SeekBarView(context);
        seekBar.setReportChanges(true);
        int stepCount = 30;
        seekBar.setSeparatorsCount(stepCount);
        seekBar.setDelegate(new SeekBarView.SeekBarViewDelegate() {
            @Override
            public void onSeekBarDrag(boolean stop, float progress) {
                FlexatarRenderer.chosenMixWeight = 1-progress;
                FlexatarRenderer.effectsMixWeight = FlexatarRenderer.chosenMixWeight;
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

        seekBar.setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        seekBar.setProgress(1-FlexatarRenderer.chosenMixWeight);
        if (!chosenEffect.equals("Mix")){
            seekBar.setVisibility(View.GONE);
        }
        linearLayout.addView(seekBar,LayoutHelper.createFrame(200, 20,Gravity.CENTER,0,6,0,0));

        FrameLayout bottomButtonsLayout = new FrameLayout(context);

        linearLayout.addView(bottomButtonsLayout);

//        TextView switchToCamera = new HideEmojiTextView(context, backgroundProvider);
//        switchToCamera.setText(FlexatarRenderer.isFlexatarRendering ? "Turn Off" : "Turn On");
//        switchToCamera.setLayoutParams(layoutParams1);
//        bottomButtonsLayout.addView(switchToCamera,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.LEFT, 12, 0, 0, 0));
//        linearLayout.switchToCameraButton = switchToCamera;
//        switchToCamera.setOnClickListener((v) -> {
//            FlexatarRenderer.isFlexatarRendering = !FlexatarRenderer.isFlexatarRendering;
//            VoIPService voipInstance = VoIPService.getSharedInstance();
//            if (voipInstance != null)
//                voipInstance.setFlexatarDelay(FlexatarRenderer.isFlexatarRendering);
//            switchToCamera.setText(FlexatarRenderer.isFlexatarRendering ? "Turn Off" : "Turn On");
//
//        });


        ImageView closePanelIcon = new ImageView(context);
        closePanelIcon.setImageResource(R.drawable.cancel_big);
        closePanelIcon.setOnClickListener((v) -> {
            linearLayout.setVisibility(View.GONE);
            linearLayout.fulfillClose();
        });
        bottomButtonsLayout.addView(closePanelIcon,LayoutHelper.createFrame(32, 32, Gravity.RIGHT, 0, 0, 12, 0));
        bottomButtonsLayout.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));


        /*TextView closePanelText = new HideEmojiTextView(context, backgroundProvider);
        closePanelText.setLayoutParams(layoutParams1);
        closePanelText.setText("Hide Panel");
        closePanelText.setOnClickListener((v) -> {
            linearLayout.setVisibility(View.GONE);
            linearLayout.fulfillClose();
        });
        bottomButtonsLayout.addView(closePanelText,LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.RIGHT, 0, 0, 12, 0));
        bottomButtonsLayout.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6), AndroidUtilities.dp(6));
*/
        linearLayout.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(12));
        return linearLayout;
    }


}
