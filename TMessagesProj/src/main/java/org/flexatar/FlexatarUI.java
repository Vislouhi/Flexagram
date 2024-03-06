package org.flexatar;

import static android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO;
import static org.telegram.messenger.AndroidUtilities.dp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.opengl.GLSurfaceView;
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
import org.telegram.ui.Components.ViewPagerFixed;
import org.telegram.ui.Components.voip.HideEmojiTextView;
import org.telegram.ui.Components.voip.VoIPBackgroundProvider;
import org.telegram.ui.LaunchActivity;

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
        /*public void resetEffects(){
            for (int j = 0; j < effectTextViews.length; j++) {
                effectTextViews[j].setTextColor(Color.WHITE);
            }
            effectTextViews[0].setTextColor(Color.parseColor("#f7d26c"));
            FlexatarRenderer.isEffectsOn = false;
            FlexatarRenderer.effectID = 0;
            FlexatarRenderer.isMorphEffect = false;
        }*/
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
            Bitmap iconBitmap1 = FlexatarStorageManager.getFlexatarMetaData(FlexatarStorageManager.callFlexatarChooser.getChosenFirst(),true).previewImage;
            RoundedBitmapDrawable dr1 = RoundedBitmapDrawableFactory.create(getContext().getResources(), iconBitmap1);
            dr1.setCornerRadius(AndroidUtilities.dp(8));
            img1.setImageDrawable(dr1);
            Bitmap iconBitmap2 = FlexatarStorageManager.getFlexatarMetaData(FlexatarStorageManager.callFlexatarChooser.getChosenSecond(),true).previewImage;
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
        bkgProvider.setTotalSize(200, 400);
        bkgProvider.setHasVideo(true);
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

//        View popupView = inflater.inflate(R.layout.wait_popup_window, null);
        FlexatarControlPanelLayout popupView = new FlexatarControlPanelLayout(context,false, FlexatarStorageManager.callFlexatarChooser);
//        FlexatarPanelLayout popupView = makeFlexatarEffectsPanel(context, bkgProvider);
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
        popupView.setOnCancelListener(c->{
            popupWindow.dismiss();
            Log.d("FLX_INJECT","popup dismissed");
        });
//        popupView.setOnCloseListener(() -> {
//            popupWindow.dismiss();
//            Log.d("FLX_INJECT","popup dismissed");
//        });
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
    public static LinearLayout makeFlexatarChoosePanel(Context context, VoIPBackgroundProvider backgroundProvider){




        FlexatarStorageManager.FlexatarChooser currentFlexatarChooser = FlexatarStorageManager.callFlexatarChooser;
        currentFlexatarChooser.resetEffects();
        RectF bgRect = new RectF();
        Paint paint = new Paint();
        paint.setARGB(200, 0, 0, 0);
        LinearLayout linearLayout = new LinearLayout(context){
            @Override
            protected void dispatchDraw(Canvas canvas) {
                bgRect.set(0, 0, getWidth(), getHeight());
//        backgroundProvider.setDarkTranslation(getX(), getY());
                canvas.drawRoundRect(bgRect, dp(20), dp(20), paint);
                super.dispatchDraw(canvas);
            }
        };
//        FlexatarPanelLayout linearLayout = new FlexatarPanelLayout(context,backgroundProvider);

        ViewPagerFixed.TabsView tabsView = new ViewPagerFixed.TabsView(context, true, 3, LaunchActivity.getLastFragment().getResourceProvider()) {
            @Override
            public void selectTab(int currentPosition, int nextPosition, float progress) {
                super.selectTab(currentPosition, nextPosition, progress);

            }
        };
//        tabsView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        tabsView.tabMarginDp = 16;
        tabsView.addTab(0, LocaleController.getString("VideoTab",R.string.VideoTab));
        tabsView.addTab(1, LocaleController.getString("PhotoTab",R.string.PhotoTab));
        tabsView.selectTabWithId(currentFlexatarChooser.getFlxType(),1f);
        tabsView.setPadding(0,6,0,6);
        tabsView.setDelegate(new ViewPagerFixed.TabsView.TabsViewDelegate() {
            @Override
            public void onPageSelected(int page, boolean forward) {
                currentFlexatarChooser.setFlxType(page);

                FlexatarHorizontalRecycleView recyclerView = new FlexatarHorizontalRecycleView(context,currentFlexatarChooser.getFlxType(), null);
                ((FlexatarHorizontalRecycleView.Adapter)recyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
                    if (tabsView.getCurrentTabId() == 1) {
                        currentFlexatarChooser.setChosenFlexatar(file.getAbsolutePath());
                    }else if (tabsView.getCurrentTabId() == 0){
                        if (file.equals(currentFlexatarChooser.getChosenVideo()) ) return;
                        currentFlexatarChooser.setChosenVideoFlexatar(file.getAbsolutePath());
                    }
                });
                linearLayout.removeViewAt(1);
                linearLayout.addView(recyclerView);
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
        linearLayout.addView(tabsView);
        FlexatarHorizontalRecycleView recyclerView = new FlexatarHorizontalRecycleView(context,currentFlexatarChooser.getFlxType(), null);
        ((FlexatarHorizontalRecycleView.Adapter)recyclerView.getAdapter()).setAndOverrideOnItemClickListener(file->{
            if (tabsView.getCurrentTabId() == 1) {
                currentFlexatarChooser.setChosenFlexatar(file.getAbsolutePath());
            }else if (tabsView.getCurrentTabId() == 0){
                if (file.equals(currentFlexatarChooser.getChosenVideo()) ) return;
                currentFlexatarChooser.setChosenVideoFlexatar(file.getAbsolutePath());
            }
        });
        linearLayout.setGravity(Gravity.CENTER);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(12);
        linearLayout.setPadding(pad, pad, pad, pad);


        linearLayout.addView(recyclerView);
//        linearLayout.addView(flexatarScrollView(context,null));

        return linearLayout;
    }


}
