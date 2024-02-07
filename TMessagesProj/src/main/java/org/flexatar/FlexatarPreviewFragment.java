package org.flexatar;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.ColorUtils;

import com.google.android.exoplayer2.util.Log;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.ActionBarMenu;
import org.telegram.ui.ActionBar.ActionBarMenuItem;
import org.telegram.ui.ActionBar.BackDrawable;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BitmapShaderTools;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.MotionBackgroundDrawable;

public class FlexatarPreviewFragment extends BaseFragment{
    private final MotionBackgroundDrawable bgGreen = new MotionBackgroundDrawable(0xFF5FD051, 0xFF00B48E, 0xFFA9CC66, 0xFF5AB147, 0, false, true);

    private final BitmapShaderTools bgGreenShaderTools = new BitmapShaderTools(80, 80);
    private final BitmapShaderTools bgBlueVioletShaderTools = new BitmapShaderTools(80, 80);
    private final MotionBackgroundDrawable bgBlueViolet = new MotionBackgroundDrawable(0xFF00A3E6, 0xFF296EF7, 0xFF18CEE2, 0xFF3FB2FF, 0, false, true);
    private FlexatarCellNew cell;
    private TextView positiveButton;
    private View.OnClickListener onViewInstructionsChosenListener = null;
    private FlexatarPreview flexatarPreview;
    private FrameLayout frameLayout;

    public FlexatarPreviewFragment(){
        super();
    }

    FlexatarCabinetActivity parentFragment;
    public  FlexatarPreviewFragment(FlexatarCellNew cell,FlexatarCabinetActivity parentFragment){
        super();
        this.cell=cell;
        this.parentFragment=parentFragment;
    }

    public void finishPage(){
        if (flexatarPreview.isMakeMouthSelected()) {
            String filexatarId = cell.getFlexatarFile().getName().replace(".flx", "").replace("flexatar_", "");
            parentFragment.presentFragment(new FlexatarCameraCaptureFragment(filexatarId));
        }
//        finishFragment();
    }



    @Override
    public View createView(Context context) {


        actionBar.setBackButtonDrawable(new BackDrawable(false));
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle(LocaleController.getString("FlexatarPreview", R.string.FlexatarPreview));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }else if (id == 10) {

                        finishFragment();
                        FlexatarStorageManager.FlexatarMetaData metaData = flexatarPreview.getNewMetaData();
//                        Log.d("FLX_INJECT","meta "+FlexatarStorageManager.metaDataToJson(metaData));
                        if (metaData != null) {
                            FlexatarStorageManager.FlexatarMetaData oldMetData = flexatarPreview.getFlexatarCell().getMetaData();
                            if (metaData.name != null) {
                                oldMetData.name = metaData.name;
                                flexatarPreview.getFlexatarCell().setName(metaData.name);
                            }
                            if (metaData.mouthCalibration != null) {
                                oldMetData.mouthCalibration = metaData.mouthCalibration;
                            }
                            if (metaData.amplitude != null) {
                                oldMetData.amplitude = metaData.amplitude;
                            }
//                            Log.d("FLX_INJECT","write meta "+FlexatarStorageManager.metaDataToJson(oldMetData));
                            byte[] metaSend = FlexatarStorageManager.rewriteFlexatarHeader(flexatarPreview.getFlexatarCell().getFlexatarFile(), oldMetData);
                            String putRout = ServerDataProc.genDeleteRout(ServerDataProc.fileNameToRout(flexatarPreview.getFlexatarCell().getFlexatarFile().getName()));
//                            String putRout = flexatarPreview.getFlexatarCell().getFlexatarFile().getName().replace(FlexatarStorageManager.FLEXATAR_PREFIX,"").replace(".flx","");
                            FlexatarServerAccess.lambdaRequest("/"+putRout, "PUT", metaSend, null, new FlexatarServerAccess.CompletionListener() {
                                @Override
                                public void onReady(String string) {
                                    Log.d("FLX_INJECT","meta ready string");
                                }

                                @Override
                                public void onReady(boolean isComplete) {
                                    Log.d("FLX_INJECT","meta ready data");
                                }

                                @Override
                                public void onFail() {
                                    Log.d("FLX_INJECT","meta fail");
                                }
                            });
//                            String metaStr = FlexatarStorageManager.metaDataToJson( FlexatarStorageManager.getFlexatarMetaData(flexatarPreview.getFlexatarCell().getFlexatarFile(),false)).toString();
//                            Log.d("FLX_INJECT","read meta "+metaStr);
                        }
                    }

            }
        });
        ActionBarMenu menu = actionBar.createMenu();
        menu.addItem(10, R.drawable.background_selected);

        initLayout();

        return fragmentView;
    }
    private boolean finishCalled = false;
    public void initLayout(){
        fragmentView = new FrameLayout(getContext());
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        frameLayout = (FrameLayout) fragmentView;
        flexatarPreview = new FlexatarPreview(getContext(), cell, this);
        flexatarPreview.setClickable(false);
        frameLayout.addView(flexatarPreview, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

    }
    @Override
    public void onResume() {
        super.onResume();
        flexatarPreview.reinitFlexatar();
//            if (frameLayout == null) initLayout();
        /*if (flexatarPreview.isMakeMouthSelected()){
            if (!finishCalled) {
                finishCalled = true;
                finishFragment();

            }
        }*/

    }

    @Override
    public void onPause() {
        super.onPause();
        flexatarPreview.destroyFlexatarView();
//        finishFragment();
    }

    @Override
    public void onFragmentDestroy() {

        super.onFragmentDestroy();
        finishPage();
    }







}
